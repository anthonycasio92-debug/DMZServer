package com.dbzlegacy.adaptivedifficulty.rival;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import net.minecraft.server.level.ServerPlayer;

/**
 * Level-based rival TP multiplier from Rival System 4.7.10.
 * Applied on top of {@link RivalConstants#GLOBAL_TP_SCALE}.
 */
public final class RivalTpCurve {
    public static final boolean ENABLED = true;
    public static final double MAX_BURST = 3500.0;
    public static final double DRIP_POWER = 0.55;
    public static final double DRIP_MAX = 75.0;

    private static final int[] LEVEL_ANCHORS = {
            1, 10, 100, 1000, 5000, 10000, 25000, 50000, 75000, 100000
    };
    private static final double[] MULT_ANCHORS = {
            1.5, 2.5, 5.0, 14.0, 35.0, 70.0, 160.0, 320.0, 520.0, 850.0
    };

    private RivalTpCurve() {}

    public static float scale(ServerPlayer player, float rawAmount, String kind) {
        if (!(rawAmount > 0.0f)) {
            return 0.0f;
        }
        double base = Math.floor(rawAmount * RivalConstants.GLOBAL_TP_SCALE);
        if (!(base > 0.0)) {
            return 0.0f;
        }
        if (!ENABLED) {
            return (float) Math.max(1.0, base);
        }
        int level = 1;
        try {
            if (player != null) {
                level = Math.max(1, DmzProgression.dmzLevelForProgression(player));
            }
        } catch (Throwable ignored) {
            level = 1;
        }
        double mult = effectiveMultiplier(level, kind);
        return (float) Math.max(1.0, Math.floor(base * mult));
    }

    public static double burstMultiplier(int level) {
        if (!ENABLED) {
            return 1.0;
        }
        int lvl = Math.max(1, level);
        if (lvl <= LEVEL_ANCHORS[0]) {
            return MULT_ANCHORS[0];
        }
        int n = Math.min(LEVEL_ANCHORS.length, MULT_ANCHORS.length);
        for (int i = 0; i < n - 1; i++) {
            if (lvl <= LEVEL_ANCHORS[i + 1]) {
                double loLog = Math.log10(LEVEL_ANCHORS[i]);
                double hiLog = Math.log10(LEVEL_ANCHORS[i + 1]);
                double curLog = Math.log10(lvl);
                double progress = (curLog - loLog) / Math.max(0.0001, hiLog - loLog);
                progress = Math.max(0.0, Math.min(1.0, progress));
                return MULT_ANCHORS[i] + (MULT_ANCHORS[i + 1] - MULT_ANCHORS[i]) * progress;
            }
        }
        double lastL = LEVEL_ANCHORS[n - 1];
        double lastM = MULT_ANCHORS[n - 1];
        double extraDecades = (Math.log(lvl) - Math.log(lastL)) / Math.log(10.0);
        return Math.min(MAX_BURST, lastM + extraDecades * 220.0);
    }

    public static double effectiveMultiplier(int level, String kind) {
        double burst = burstMultiplier(level);
        if (!"drip".equalsIgnoreCase(kind)) {
            return burst;
        }
        double drip = Math.pow(Math.max(1.0, burst), DRIP_POWER);
        return Math.min(DRIP_MAX, drip);
    }
}
