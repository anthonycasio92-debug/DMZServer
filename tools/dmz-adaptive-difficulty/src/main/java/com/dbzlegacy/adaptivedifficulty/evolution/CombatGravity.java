package com.dbzlegacy.adaptivedifficulty.evolution;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationType;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dragonminez.server.util.GravityDeviceManager;
import com.dragonminez.server.util.GravityStateSync;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;

/**
 * Stacks DMZ gravity-chamber pressure on players from nearby Endermen / Wardens /
 * Gravity-mutated hostiles — same path as a real {@code GravityDevice}.
 * <p>
 * {@link GravityDeviceManager#getGravityFor} takes the <b>max</b> of overlapping
 * zones, so we register <b>one zone per player</b> whose gravity equals the
 * sum of all active contributions (more Endermen → heavier gravity).
 * <p>
 * Contributions are refreshed every player tick from proximity (not only when a
 * mob currently has the player as AI target), so teleporting Endermen still apply
 * pressure while near the player.
 */
public final class CombatGravity {
    private static final Map<UUID, Map<UUID, Contribution>> BY_PLAYER = new ConcurrentHashMap<>();
    private static final double SCAN_RADIUS = 28.0;
    private static final int CONTRIB_TTL_TICKS = 40;

    private CombatGravity() {}

    /**
     * Refresh a source's gravity contribution (e.g. one Enderman's UUID).
     * {@code ttlTicks} keeps the pressure alive between staggered mob ticks.
     */
    public static void contribute(ServerPlayer player, UUID sourceId, double gravity, int ttlTicks) {
        if (player == null || sourceId == null || gravity <= 0.0 || player.m_9236_().f_46443_) {
            return;
        }
        if (!(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        long expire = level.m_46467_() + Math.max(10, ttlTicks); // getGameTime
        BY_PLAYER
                .computeIfAbsent(player.m_20148_(), id -> new ConcurrentHashMap<>())
                .put(sourceId, new Contribution(gravity, expire));
    }

    /** Apply / clear the player's combat gravity zone. Call every player tick. */
    public static void tickPlayer(ServerPlayer player) {
        if (player == null || player.m_9236_().f_46443_) {
            return;
        }
        if (!(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }

        // Proximity refresh — do not require mob AI target (Endermen teleport often).
        scanNearbySources(player, level);

        Map<UUID, Contribution> sources = BY_PLAYER.get(player.m_20148_());
        double total = 0.0;
        if (sources != null && !sources.isEmpty()) {
            long now = level.m_46467_();
            Iterator<Map.Entry<UUID, Contribution>> it = sources.entrySet().iterator();
            while (it.hasNext()) {
                Contribution c = it.next().getValue();
                if (c.expireTick < now) {
                    it.remove();
                } else {
                    total += c.gravity;
                }
            }
            if (sources.isEmpty()) {
                BY_PLAYER.remove(player.m_20148_(), sources);
            }
        }

        BlockPos key = keyFor(player);
        try {
            if (total <= 0.05) {
                GravityDeviceManager.unregister(level, key);
                GravityStateSync.sync(player);
                return;
            }
            // Cap below device max (1000) so combat pressure stays playable.
            double gravity = Math.min(400.0, total);
            // Wide box so feet/eyes stay inside while sprinting / flying.
            AABB box = player.m_20191_().m_82377_(4.0, 3.0, 4.0); // inflate(x,y,z)
            GravityDeviceManager.register(level, key, box, gravity);
            GravityStateSync.sync(player);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] combat gravity failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    /**
     * Find nearby gravity-capable hostiles and stack their pressure.
     * Works at Awakened+ for Endermen/Wardens; Gravity mutations work whenever present.
     */
    private static void scanNearbySources(ServerPlayer player, ServerLevel level) {
        AABB box = player.m_20191_().m_82400_(SCAN_RADIUS);
        List<Mob> mobs = level.m_45976_(Mob.class, box);
        for (Mob mob : mobs) {
            if (mob == null || !mob.m_6084_()) {
                continue;
            }
            if (!PersistentDataAccess.flag(mob, MobScaling.TAG_SCALED)
                    && !EliteSystem.isElite(mob)) {
                continue;
            }
            MutationType mutation = MutationSystem.get(mob);
            boolean enderman = mob instanceof EnderMan;
            boolean warden = mob instanceof Warden;
            boolean gravityMut = mutation == MutationType.GRAVITY_ENDERMAN;
            if (!enderman && !warden && !gravityMut) {
                continue;
            }

            long difficulty = MobScaling.difficultyOf(mob);
            DifficultyTier tier = DifficultyTier.of(difficulty);
            if (EliteSystem.isElite(mob) && tier.ordinalPower() < DifficultyTier.ELITE.ordinalPower()) {
                tier = DifficultyTier.ELITE;
            }
            if (!gravityMut && tier.ordinalPower() < DifficultyTier.AWAKENED.ordinalPower()) {
                continue;
            }
            if (difficulty <= 0L && !EliteSystem.isElite(mob) && !gravityMut) {
                continue;
            }

            float dist = mob.m_20270_(player);
            if (dist > SCAN_RADIUS) {
                continue;
            }

            double kitMult = warden ? 1.6 : 1.0;
            if (gravityMut) {
                kitMult *= 1.5;
            }
            double g = gravityFor(mob, tier, Math.max(1L, difficulty), kitMult);
            // Soft falloff past 16 blocks so distant mobs nudge instead of crush.
            if (dist > 16.0f) {
                g *= Math.max(0.35, 1.0 - (dist - 16.0) / (SCAN_RADIUS - 16.0) * 0.65);
            }
            contribute(player, mob.m_20148_(), g, CONTRIB_TTL_TICKS);

            // Keep them locked onto the player so kits/AI keep firing.
            if (mob.m_5448_() != player && dist < 22.0f) {
                mob.m_6710_(player);
            }
        }
    }

    /** Same curve used by Enderman / Warden kits. */
    public static double gravityFor(Mob mob, DifficultyTier tier, long difficulty, double mult) {
        double base = 10.0 + tier.ordinalPower() * 8.0;
        base += Math.min(80.0, difficulty / 40.0);
        if (EliteSystem.isElite(mob)) {
            base *= 1.35;
        }
        if (MutationSystem.get(mob) == MutationType.GRAVITY_ENDERMAN) {
            base *= 1.25;
        }
        return Math.max(8.0, base * mult);
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        BY_PLAYER.remove(player.m_20148_());
        try {
            if (player.m_9236_() instanceof ServerLevel level) {
                GravityDeviceManager.unregister(level, keyFor(player));
                GravityStateSync.sync(player);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Stable fake BlockPos so combat zones never collide with real gravity devices. */
    private static BlockPos keyFor(ServerPlayer player) {
        UUID id = player.m_20148_();
        int x = id.hashCode();
        int z = (int) (id.getMostSignificantBits() ^ id.getLeastSignificantBits());
        return new BlockPos(x, 512, z);
    }

    private record Contribution(double gravity, long expireTick) {}
}
