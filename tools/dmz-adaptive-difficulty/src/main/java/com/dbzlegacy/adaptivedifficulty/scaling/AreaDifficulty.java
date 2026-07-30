package com.dbzlegacy.adaptivedifficulty.scaling;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/**
 * Area difficulty inspired by SilentChaos512 Scaling Health.
 * <p>
 * Modes (config {@code areaDifficultyMode}):
 * <ul>
 *   <li>{@code max} — highest nearby active difficulty (legacy DMZ default)</li>
 *   <li>{@code average} — mean of nearby players</li>
 *   <li>{@code weighted} — distance-weighted average (Scaling Health default)</li>
 * </ul>
 */
public final class AreaDifficulty {
    private AreaDifficulty() {}

    public static long at(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return 0L;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        double radius = Math.max(8.0, cfg.mobScaleRadius);
        AABB box = new AABB(
                pos.m_123341_() - radius, pos.m_123342_() - radius, pos.m_123343_() - radius,
                pos.m_123341_() + radius, pos.m_123342_() + radius, pos.m_123343_() + radius
        );
        List<ServerPlayer> players = level.m_45976_(ServerPlayer.class, box);
        if (players.isEmpty()) {
            return 0L;
        }

        String mode = cfg.areaDifficultyMode == null ? "weighted" : cfg.areaDifficultyMode.trim().toLowerCase();
        double raw = switch (mode) {
            case "max", "maxima", "extrema" -> maxMode(players);
            case "average", "avg", "mean" -> averageMode(players);
            default -> weightedMode(players, pos, radius);
        };

        // Group bonus like Scaling Health: 1 + groupBonusPercent * (count - 1)
        if (players.size() > 1 && cfg.areaGroupBonusPercent > 0) {
            raw *= 1.0 + (cfg.areaGroupBonusPercent / 100.0) * (players.size() - 1);
        }

        // Per-mob variance like Scaling Health (~0.95–1.05)
        if (cfg.areaDifficultyVariancePercent > 0) {
            double v = cfg.areaDifficultyVariancePercent / 100.0;
            double factor = (1.0 - v) + (level.f_46441_.m_188501_() * (2.0 * v)); // random float
            raw *= factor;
        }

        long value = Math.round(Math.max(0.0, raw));
        long cap = Math.max(0L, cfg.hardCapDifficulty);
        return cap > 0 ? Math.min(value, cap) : value;
    }

    private static double maxMode(List<ServerPlayer> players) {
        double best = 0;
        for (ServerPlayer player : players) {
            DifficultySnapshot snap = DifficultyCache.refresh(player);
            best = Math.max(best, snap.active);
        }
        return best;
    }

    private static double averageMode(List<ServerPlayer> players) {
        double total = 0;
        for (ServerPlayer player : players) {
            total += DifficultyCache.refresh(player).active;
        }
        return total / players.size();
    }

    /** Distance-weighted average — Scaling Health Average(weighted=true). */
    private static double weightedMode(List<ServerPlayer> players, BlockPos pos, double radius) {
        double total = 0;
        double totalWeight = 0;
        double rSq = radius * radius;
        for (ServerPlayer player : players) {
            double dx = player.m_20185_() - (pos.m_123341_() + 0.5);
            double dy = player.m_20186_() - (pos.m_123342_() + 0.5);
            double dz = player.m_20189_() - (pos.m_123343_() + 0.5);
            double distSq = dx * dx + dy * dy + dz * dz;
            double weight = Math.max(0.0, 1.0 - (distSq / rSq));
            if (weight <= 0) {
                continue;
            }
            total += weight * DifficultyCache.refresh(player).active;
            totalWeight += weight;
        }
        return totalWeight <= 0 ? 0 : total / totalWeight;
    }
}
