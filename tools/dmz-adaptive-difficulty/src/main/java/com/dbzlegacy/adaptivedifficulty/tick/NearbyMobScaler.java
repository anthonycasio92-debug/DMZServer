package com.dbzlegacy.adaptivedifficulty.tick;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.scaling.HostileMobs;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.util.DimensionGates;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Rescales the closest hostiles near each player (hard cap: {@link ScaledMobTracker}).
 * Spawn does not bake final fight stats.
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
                continue;
            }
            if (DifficultyCache.get(player).activeTier <= 0) {
                continue;
            }
            ScaledMobTracker.prunePlayer(player);
            scaleAround(player, cfg);
        }
    }

    private static void scaleAround(ServerPlayer player, DifficultyConfig cfg) {
        if (!(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        double radius = Math.max(8.0, Math.min(48.0, cfg.mobScaleRadius));
        int max = ScaledMobTracker.maxSlots();
        AABB box = player.m_20191_().m_82377_(radius, Math.min(16.0, radius), radius);
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
        if (mobs == null || mobs.isEmpty()) {
            return;
        }
        double rSq = radius * radius;
        double px = player.m_20185_();
        double py = player.m_20186_();
        double pz = player.m_20189_();
        List<Mob> inRange = new ArrayList<>(Math.min(mobs.size(), 32));
        for (Mob mob : mobs) {
            double dx = mob.m_20185_() - px;
            double dy = mob.m_20186_() - py;
            double dz = mob.m_20189_() - pz;
            if (dx * dx + dy * dy + dz * dz <= rSq) {
                inRange.add(mob);
            }
        }
        if (inRange.isEmpty()) {
            return;
        }
        ScaledMobTracker.sortNearest(player, inRange);
        int scaled = 0;
        for (Mob mob : inRange) {
            if (scaled >= max) {
                break;
            }
            MobScaling.retargetToPlayer(mob, player);
            if (ScaledMobTracker.isClaimed(player, mob)) {
                CombatIndex.mark(mob);
                scaled++;
            }
        }
    }

    /** Revert mobs that lost their difficulty slot to a closer hostile. */
    public static void processEvictions() {
        Map<UUID, UUID> evicted = ScaledMobTracker.drainEvictions();
        if (evicted.isEmpty()) {
            return;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (UUID mobId : evicted.keySet()) {
            Mob mob = findMob(server, mobId);
            if (mob != null) {
                MobScaling.revertToBases(mob);
                CombatIndex.unmark(mobId);
            }
        }
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
