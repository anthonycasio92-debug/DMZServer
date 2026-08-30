package com.dbzlegacy.adaptivedifficulty.tick;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.scaling.HostileMobs;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.util.DimensionGates;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Rescales the closest hostiles near each player (hard cap: {@link ScaledMobTracker}).
 * When a player leaves range / turns personal off / has no tier, claimed mobs revert.
 *
 * <p>Mobs currently fighting this player (aggro target) stay scaled within a limited
 * pin radius (scan radius × 1.5) so knockback cannot mid-fight unscale them — but
 * dragging fights across the map no longer keeps infinite pin slots.
 */
public final class NearbyMobScaler {
    /** Extra reach for mid-fight pin beyond {@link DifficultyConfig#mobScaleRadius}. */
    private static final double COMBAT_PIN_RADIUS_MULT = 1.5;

    private NearbyMobScaler() {}

    public static void pulse(MinecraftServer server, int gameTick) {
        if (server == null) {
            return;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enabled || !cfg.enableMobScaling) {
            // Master / scaling OFF: strip every claim and revert what is loaded.
            ScaledMobTracker.releaseAllPlayers();
            processEvictions();
            return;
        }
        processEvictions();
        int interval = Math.max(10, cfg.nearbyScaleIntervalTicks);
        List<ServerPlayer> online = server.m_6846_().m_11314_();
        if (online == null || online.isEmpty()) {
            return;
        }
        for (ServerPlayer player : online) {
            if (player == null || !player.m_6084_() || player.m_5833_()) {
                continue;
            }
            if (Math.floorMod(gameTick + player.m_19879_(), interval) != 0) {
                continue;
            }
            if (!SystemGate.allows(player) || DimensionGates.isDisabled(player)) {
                ScaledMobTracker.releaseAndRevertPlayer(player);
                continue;
            }
            // Personal off or no active tier: revert every claimed mob to vanilla bases.
            if (!SystemGate.participates(player) || DifficultyCache.get(player).activeTier <= 0) {
                ScaledMobTracker.releaseAndRevertPlayer(player);
                continue;
            }
            // End Dragon fight owns AD pressure — suspend nearby mob scaling for the summoner.
            try {
                if (com.dbzlegacy.adaptivedifficulty.progression.end.EndDimensionStrength
                        .hasAliveSummonedDragon(player)) {
                    ScaledMobTracker.releaseAndRevertPlayer(player);
                    continue;
                }
            } catch (Throwable ignored) {
            }
            ScaledMobTracker.prunePlayer(player);
            scaleAround(player, cfg);
        }
        // Apply any leave-range / personal-off / slot-eviction reverts from this pulse.
        processEvictions();
    }

    private static void scaleAround(ServerPlayer player, DifficultyConfig cfg) {
        if (!(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        // Honor config radius (default 64); floor at 8 so tiny values still work.
        double radius = Math.max(8.0, cfg.mobScaleRadius);
        double pinRadius = Math.max(radius, radius * COMBAT_PIN_RADIUS_MULT);
        int max = ScaledMobTracker.maxSlots();
        // Match horizontal radius for vertical reach so tall caves / towers
        // don't leave combatants half-in / half-out of the retain AABB.
        AABB box = player.m_20191_().m_82377_(pinRadius, pinRadius, pinRadius);
        List<Mob> mobs;
        try {
            mobs = level.m_6443_(Mob.class, box, mob ->
                    mob != null
                            && mob.m_6084_()
                            && HostileMobs.isHostile(mob)
                            && !DimensionGates.isDisabled(mob)
                            && !MobScaling.isExemptFromConversion(mob));
        } catch (Throwable t) {
            return;
        }

        Set<UUID> kept = new HashSet<>(max);
        // Mid-fight pin first: claimed mobs currently targeting this player keep
        // their slot within pinRadius (not the whole dimension).
        pinCombatTargets(player, kept, max, pinRadius);

        if (mobs != null && !mobs.isEmpty()) {
            double rSq = radius * radius;
            double px = player.m_20185_();
            double py = player.m_20186_();
            double pz = player.m_20189_();
            List<Mob> inRange = new ArrayList<>(Math.min(mobs.size(), 32));
            for (Mob mob : mobs) {
                if (kept.contains(mob.m_20148_())) {
                    continue;
                }
                double dx = mob.m_20185_() - px;
                double dy = mob.m_20186_() - py;
                double dz = mob.m_20189_() - pz;
                if (dx * dx + dy * dy + dz * dz <= rSq) {
                    inRange.add(mob);
                }
            }
            if (!inRange.isEmpty()) {
                ScaledMobTracker.sortNearest(player, inRange);
                for (Mob mob : inRange) {
                    if (kept.size() >= max) {
                        break;
                    }
                    MobScaling.retargetToPlayer(mob, player);
                    if (ScaledMobTracker.isClaimed(player, mob)) {
                        CombatIndex.mark(mob);
                        kept.add(mob.m_20148_());
                    }
                }
            }
        }

        // Anything previously claimed but now out of the kept set → revert to normal.
        ScaledMobTracker.retainOnly(player, kept);
    }

    /**
     * Keep claimed hostiles that are actively fighting this player within
     * {@code pinRadius} so knockback / pathing / Y gaps cannot mid-fight unscale them.
     */
    private static void pinCombatTargets(ServerPlayer player, Set<UUID> kept, int max, double pinRadius) {
        if (player == null || kept == null || max <= 0) {
            return;
        }
        double pinSq = Math.max(8.0, pinRadius) * Math.max(8.0, pinRadius);
        double px = player.m_20185_();
        double py = player.m_20186_();
        double pz = player.m_20189_();
        ScaledMobTracker.forEachClaimedMob(player, mob -> {
            if (kept.size() >= max || kept.contains(mob.m_20148_())) {
                return;
            }
            LivingEntity target = mob.m_5448_();
            if (target != player) {
                return;
            }
            double dx = mob.m_20185_() - px;
            double dy = mob.m_20186_() - py;
            double dz = mob.m_20189_() - pz;
            if (dx * dx + dy * dy + dz * dz > pinSq) {
                return;
            }
            // Refresh paint while pinned so profile/config changes still apply.
            MobScaling.retargetToPlayer(mob, player);
            if (ScaledMobTracker.isClaimed(player, mob)) {
                CombatIndex.mark(mob);
                kept.add(mob.m_20148_());
            }
        });
    }

    /** Revert mobs that lost their difficulty slot (keeps unloaded UUIDs queued). */
    public static void processEvictions() {
        Map<UUID, UUID> evicted = ScaledMobTracker.peekEvictions();
        if (evicted.isEmpty()) {
            return;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (UUID mobId : evicted.keySet()) {
            Mob mob = findMob(server, mobId);
            if (mob == null) {
                // Chunk unloaded — retry when the mob is next found / joins.
                continue;
            }
            if (MobScaling.isExemptFromConversion(mob)) {
                MobScaling.ensureExempt(mob);
            } else {
                MobScaling.revertToBases(mob);
            }
            CombatIndex.unmark(mobId);
            ScaledMobTracker.clearPendingRevert(mobId);
        }
    }

    /** Force-release every claim and revert loaded scaled hostiles (admin off / scaling off). */
    public static void shutdownAllScaling() {
        ScaledMobTracker.releaseAllPlayers();
        processEvictions();
    }

    private static Mob findMob(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.m_129785_()) {
            Entity entity = level.m_8791_(id);
            if (entity instanceof Mob mob) {
                return mob;
            }
        }
        return null;
    }
}
