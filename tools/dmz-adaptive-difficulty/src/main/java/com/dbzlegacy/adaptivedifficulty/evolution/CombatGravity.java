package com.dbzlegacy.adaptivedifficulty.evolution;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationType;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.util.DimensionGates;
import com.dragonminez.server.util.GravityDeviceManager;
import com.dragonminez.server.util.GravityStateSync;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * Stacks DMZ gravity-chamber pressure from Enderman / Warden kits.
 * <p>
 * Kits call {@link #contribute}; this class only applies the stacked value.
 * No per-player AABB entity scans (those crushed TPS).
 */
public final class CombatGravity {
    private static final Map<UUID, Map<UUID, Contribution>> BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> LAST_APPLIED = new ConcurrentHashMap<>();
    /** Apply / expire math cadence — not every player tick. */
    private static final int APPLY_INTERVAL_TICKS = 10;

    private CombatGravity() {}

    /**
     * Refresh a source's gravity contribution (e.g. one Enderman's UUID).
     * {@code ttlTicks} keeps the pressure alive between staggered mob ticks.
     */
    public static void contribute(ServerPlayer player, UUID sourceId, double gravity, int ttlTicks) {
        if (player == null || sourceId == null || gravity <= 0.0 || player.m_9236_().f_46443_) {
            return;
        }
        if (DimensionGates.isDisabled(player)) {
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

    /**
     * Apply gravity only for players who currently have (or recently had) contributions.
     * Avoids walking the full online player list every pulse.
     */
    public static void tickActive(MinecraftServer server) {
        if (server == null || (BY_PLAYER.isEmpty() && LAST_APPLIED.isEmpty())) {
            return;
        }
        // Copy keys — tickPlayer may remove empty maps.
        for (UUID id : new java.util.ArrayList<>(BY_PLAYER.keySet())) {
            ServerPlayer player = server.m_6846_().m_11259_(id); // getPlayer(UUID)
            if (player != null) {
                tickPlayer(player);
            } else {
                BY_PLAYER.remove(id);
                LAST_APPLIED.remove(id);
            }
        }
        // Clear residual applied gravity for players who dropped off the map.
        if (!LAST_APPLIED.isEmpty()) {
            for (UUID id : new java.util.ArrayList<>(LAST_APPLIED.keySet())) {
                if (BY_PLAYER.containsKey(id)) {
                    continue;
                }
                Double prev = LAST_APPLIED.get(id);
                if (prev == null || prev <= 0.05) {
                    LAST_APPLIED.remove(id);
                    continue;
                }
                ServerPlayer player = server.m_6846_().m_11259_(id);
                if (player != null) {
                    tickPlayer(player);
                } else {
                    LAST_APPLIED.remove(id);
                }
            }
        }
    }

    /** Apply / clear the player's combat gravity zone. Call from player tick. */
    public static void tickPlayer(ServerPlayer player) {
        if (player == null || player.m_9236_().f_46443_) {
            return;
        }
        if (!(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        // Fast path: nothing stacked and nothing previously applied.
        Map<UUID, Contribution> sources = BY_PLAYER.get(player.m_20148_());
        Double prev = LAST_APPLIED.get(player.m_20148_());
        if ((sources == null || sources.isEmpty()) && (prev == null || prev <= 0.05)) {
            return;
        }
        if (player.f_19797_ % APPLY_INTERVAL_TICKS != 0) {
            return;
        }

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

        double gravity = total <= 0.05 ? 0.0 : Math.min(400.0, total);
        if (prev != null && Math.abs(prev - gravity) < 2.0) {
            return;
        }
        LAST_APPLIED.put(player.m_20148_(), gravity);

        BlockPos key = keyFor(player);
        try {
            if (gravity <= 0.05) {
                GravityDeviceManager.unregister(level, key);
                GravityStateSync.sync(player);
                return;
            }
            AABB box = player.m_20191_().m_82377_(4.0, 3.0, 4.0); // inflate(x,y,z)
            GravityDeviceManager.register(level, key, box, gravity);
            GravityStateSync.sync(player);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] combat gravity failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    /** Same curve used by Enderman / Warden kits. */
    public static double gravityFor(Mob mob, DifficultyTier tier, long difficulty, double mult) {
        double base = 10.0 + tier.ordinalPower() * 8.0;
        // Soft proxy add — tier% below gates the final amount so T2 ≠ max chamber.
        base += Math.min(40.0, difficulty / 80.0);
        if (EliteSystem.isElite(mob)) {
            base *= 1.25;
        }
        if (MutationSystem.get(mob) == MutationType.GRAVITY_ENDERMAN) {
            base *= 1.20;
        }
        double tierPct = com.dbzlegacy.adaptivedifficulty.scaling.MobScaling.tierPercentOf(mob);
        // Full gravity force from ~50% ladder up; T2 20% ≈ 40% of base chamber.
        double tierScale = tierPct > 0.0
                ? Math.max(0.15, Math.min(1.0, tierPct / 0.50))
                : 0.35;
        return Math.max(4.0, base * mult * tierScale);
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        BY_PLAYER.remove(player.m_20148_());
        LAST_APPLIED.remove(player.m_20148_());
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
