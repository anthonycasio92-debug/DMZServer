package com.dbzlegacy.adaptivedifficulty.scaling;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Area difficulty inspired by SilentChaos512 Scaling Health.
 * <p>
 * Modes (config {@code areaDifficultyMode}):
 * <ul>
 *   <li>{@code max} — highest nearby active difficulty</li>
 *   <li>{@code average} — mean of nearby players</li>
 *   <li>{@code weighted} — distance-weighted average (default)</li>
 * </ul>
 * Uses cached player snapshots (no full refresh on spawn) and a short chunk TTL cache.
 */
public final class AreaDifficulty {
    /** Longer TTL — spawn bursts were thrashing chunk difficulty recomputes. */
    private static final long CACHE_TTL_MS = 1_000L;
    private static final Map<Long, Cached> CHUNK_CACHE = new ConcurrentHashMap<>();

    private AreaDifficulty() {}

    public static long at(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return 0L;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        double raw = rawAt(level, pos, cfg);

        // Per-mob variance like Scaling Health (~0.95–1.05) — outside chunk cache
        if (cfg.areaDifficultyVariancePercent > 0) {
            double v = cfg.areaDifficultyVariancePercent / 100.0;
            double factor = (1.0 - v) + (level.f_46441_.m_188501_() * (2.0 * v));
            raw *= factor;
        }

        long value = Math.round(Math.max(0.0, raw));
        long cap = Math.max(0L, cfg.hardCapDifficulty);
        return cap > 0 ? Math.min(value, cap) : value;
    }

    /** Base area difficulty without per-mob variance (chunk-cached). */
    public static double rawAt(ServerLevel level, BlockPos pos, DifficultyConfig cfg) {
        long key = chunkKey(level, pos);
        long now = System.currentTimeMillis();
        Cached hit = CHUNK_CACHE.get(key);
        if (hit != null && now - hit.atMs <= CACHE_TTL_MS) {
            return hit.value;
        }

        double radius = Math.max(8.0, cfg.mobScaleRadius);
        double value = compute(level, pos, cfg, radius);
        CHUNK_CACHE.put(key, new Cached(value, now));
        // Opportunistic prune when map grows
        if (CHUNK_CACHE.size() > 512) {
            CHUNK_CACHE.entrySet().removeIf(e -> now - e.getValue().atMs > CACHE_TTL_MS * 4);
        }
        return value;
    }

    public static void clearCache() {
        CHUNK_CACHE.clear();
    }

    private static double compute(ServerLevel level, BlockPos pos, DifficultyConfig cfg, double radius) {
        // Prefer iterating online players when the server is small — cheaper than big AABB scans.
        List<ServerPlayer> players = nearbyPlayers(level, pos, radius);
        if (players.isEmpty()) {
            return 0.0;
        }

        String mode = cfg.areaDifficultyMode == null ? "weighted" : cfg.areaDifficultyMode.trim().toLowerCase();
        double raw = switch (mode) {
            case "max", "maxima", "extrema" -> maxMode(players);
            case "average", "avg", "mean" -> averageMode(players);
            default -> weightedMode(players, pos, radius);
        };

        if (players.size() > 1 && cfg.areaGroupBonusPercent > 0) {
            raw *= 1.0 + (cfg.areaGroupBonusPercent / 100.0) * (players.size() - 1);
        }
        return Math.max(0.0, raw);
    }

    private static List<ServerPlayer> nearbyPlayers(ServerLevel level, BlockPos pos, double radius) {
        List<ServerPlayer> online = level.m_7654_().m_6846_().m_11314_(); // getServer().getPlayerList().getPlayers()
        if (online == null || online.isEmpty()) {
            return List.of();
        }
        // Small player counts: distance filter is cheaper than AABB entity query.
        if (online.size() <= 64) {
            double rSq = radius * radius;
            double cx = pos.m_123341_() + 0.5;
            double cy = pos.m_123342_() + 0.5;
            double cz = pos.m_123343_() + 0.5;
            java.util.ArrayList<ServerPlayer> out = new java.util.ArrayList<>(Math.min(8, online.size()));
            for (ServerPlayer player : online) {
                if (player == null || player.m_9236_() != level || !player.m_6084_()) {
                    continue;
                }
                double dx = player.m_20185_() - cx;
                double dy = player.m_20186_() - cy;
                double dz = player.m_20189_() - cz;
                if (dx * dx + dy * dy + dz * dz <= rSq) {
                    out.add(player);
                }
            }
            return out;
        }
        AABB box = new AABB(
                pos.m_123341_() - radius, pos.m_123342_() - radius, pos.m_123343_() - radius,
                pos.m_123341_() + radius, pos.m_123342_() + radius, pos.m_123343_() + radius
        );
        return level.m_45976_(ServerPlayer.class, box);
    }

    private static double maxMode(List<ServerPlayer> players) {
        double best = 0;
        for (ServerPlayer player : players) {
            best = Math.max(best, DifficultyCache.get(player).active);
        }
        return best;
    }

    private static double averageMode(List<ServerPlayer> players) {
        double total = 0;
        for (ServerPlayer player : players) {
            total += DifficultyCache.get(player).active;
        }
        return total / players.size();
    }

    private static double weightedMode(List<ServerPlayer> players, BlockPos pos, double radius) {
        double total = 0;
        double totalWeight = 0;
        double rSq = radius * radius;
        double cx = pos.m_123341_() + 0.5;
        double cy = pos.m_123342_() + 0.5;
        double cz = pos.m_123343_() + 0.5;
        for (ServerPlayer player : players) {
            double dx = player.m_20185_() - cx;
            double dy = player.m_20186_() - cy;
            double dz = player.m_20189_() - cz;
            double distSq = dx * dx + dy * dy + dz * dz;
            double weight = Math.max(0.0, 1.0 - (distSq / rSq));
            if (weight <= 0) {
                continue;
            }
            total += weight * DifficultyCache.get(player).active;
            totalWeight += weight;
        }
        return totalWeight <= 0 ? 0 : total / totalWeight;
    }

    private static long chunkKey(ServerLevel level, BlockPos pos) {
        ResourceKey<Level> dim = level.m_46472_();
        int dimHash = dim == null ? 0 : dim.hashCode();
        int cx = pos.m_123341_() >> 4;
        int cz = pos.m_123343_() >> 4;
        // Pack dim + chunk coords; collisions are acceptable (TTL is short).
        return (((long) dimHash) << 32) ^ (((long) cx) << 16) ^ (cz & 0xffffL);
    }

    private record Cached(double value, long atMs) {}
}
