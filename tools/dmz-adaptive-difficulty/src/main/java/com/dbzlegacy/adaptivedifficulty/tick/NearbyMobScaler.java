package com.dbzlegacy.adaptivedifficulty.tick;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.scaling.HostileMobs;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.util.DimensionGates;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * Rescales hostiles <b>already near</b> players to that player's transformed /
 * limit-release stats × unlock-tier percent. Spawn no longer paints final stats.
 * <p>
 * Uses a tight per-player AABB with a hard budget — never a world-wide scan.
 */
public final class NearbyMobScaler {
    private NearbyMobScaler() {}

    public static void pulse(MinecraftServer server, int gameTick) {
        if (server == null) {
            return;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enabled || !cfg.enableMobScaling) {
            return;
        }
        int interval = Math.max(10, cfg.nearbyScaleIntervalTicks);
        List<ServerPlayer> online = server.m_6846_().m_11314_();
        if (online == null || online.isEmpty()) {
            return;
        }
        int budgetGlobal = Math.max(8, cfg.nearbyScaleBudgetPerPlayer * 4);
        for (ServerPlayer player : online) {
            if (budgetGlobal <= 0) {
                return;
            }
            if (player == null || !player.m_6084_() || player.m_5833_()) {
                continue;
            }
            // Stagger players across the interval window.
            if (Math.floorMod(gameTick + player.m_19879_(), interval) != 0) {
                continue;
            }
            if (!SystemGate.allows(player) || DimensionGates.isDisabled(player)) {
                continue;
            }
            if (DifficultyCache.get(player).activeTier <= 0) {
                continue;
            }
            budgetGlobal -= scaleAround(player, cfg);
        }
    }

    private static int scaleAround(ServerPlayer player, DifficultyConfig cfg) {
        if (!(player.m_9236_() instanceof ServerLevel level)) {
            return 0;
        }
        double radius = Math.max(8.0, Math.min(48.0, cfg.mobScaleRadius));
        AABB box = player.m_20191_().m_82377_(radius, Math.min(16.0, radius), radius);
        int budget = Math.max(1, cfg.nearbyScaleBudgetPerPlayer);
        int used = 0;
        List<Mob> mobs;
        try {
            mobs = level.m_6443_(Mob.class, box, mob ->
                    mob != null
                            && mob.m_6084_()
                            && HostileMobs.isHostile(mob)
                            && !DimensionGates.isDisabled(mob));
        } catch (Throwable t) {
            return 0;
        }
        if (mobs == null || mobs.isEmpty()) {
            return 0;
        }
        double rSq = radius * radius;
        double px = player.m_20185_();
        double py = player.m_20186_();
        double pz = player.m_20189_();
        for (Mob mob : mobs) {
            if (used >= budget) {
                break;
            }
            double dx = mob.m_20185_() - px;
            double dy = mob.m_20186_() - py;
            double dz = mob.m_20189_() - pz;
            if (dx * dx + dy * dy + dz * dz > rSq) {
                continue;
            }
            CombatIndex.mark(mob);
            MobScaling.retargetToPlayer(mob, player);
            used++;
        }
        return used;
    }
}
