package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;

/** LM personal level cap → Overhaul-compatible max stat totals (matches dmzrevamp formula). */
public final class LmOverhaulCapMath {
    /** Stock Overhaul {@code levelUpPerPoints} when {@code LevelingRevamp.json} is default. */
    public static final int LEVEL_UP_PER_POINTS = 6;

    private LmOverhaulCapMath() {}

    public static int personalLevelCap(ServerPlayer player) {
        if (player == null) {
            return PrestigePointsSystem.BASE_LEVEL_CAP;
        }
        return PrestigePointsSystem.effectiveMaxLevel(player);
    }

    public static int maxAssignableTotal(StatsData data, int levelCap) {
        if (data == null || levelCap < 1) {
            return 0;
        }
        int initial = initialStatTotal(data);
        long max = (long) initial + (long) Math.max(0, levelCap - 1) * LEVEL_UP_PER_POINTS;
        return (int) Math.min(Integer.MAX_VALUE, max);
    }

    public static int maxAssignableTotal(StatsData data, ServerPlayer player) {
        return maxAssignableTotal(data, personalLevelCap(player));
    }

    private static int initialStatTotal(StatsData data) {
        try {
            Object v = data.getClass().getMethod("getInitialTotalStats").invoke(data);
            if (v instanceof Number n) {
                return Math.max(0, n.intValue());
            }
        } catch (Throwable ignored) {
        }
        try {
            var st = data.getStats();
            if (st != null) {
                return Math.max(0, st.getTotalStats());
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
