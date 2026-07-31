package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;

/**
 * Separate curves for offense (damage/defense) and health.
 * <p>
 * Offense uses a steeper power curve so fights get meaner without ballooning HP.
 * Health uses a flat curve + lower caps so mobs stay killable.
 * <p>
 * Training Points are not scaled or granted by this mod.
 */
public final class ScalingCurves {
    private ScalingCurves() {}

    /**
     * Effective difficulty for a power curve.
     * {@code pow(d, exp) * pow(pivot, 1-exp)} — equals {@code d} when {@code d == pivot}.
     */
    public static double curvedEffective(long difficulty, double exponent, long pivot) {
        if (difficulty <= 0L) {
            return 0.0;
        }
        double exp = clamp(exponent, 0.05, 1.0);
        if (exp >= 0.999) {
            return difficulty; // linear
        }
        double p = Math.max(1.0, pivot);
        return Math.pow(difficulty, exp) * Math.pow(p, 1.0 - exp);
    }

    /** Damage / defense curve (steeper). */
    public static double offenseEffective(long difficulty) {
        DifficultyConfig cfg = DifficultyConfig.get();
        return curvedEffective(difficulty, cfg.combatCurveExponent, cfg.combatCurvePivot);
    }

    /** Health curve (flatter — avoids unkillable sponge mobs). */
    public static double healthEffective(long difficulty) {
        DifficultyConfig cfg = DifficultyConfig.get();
        return curvedEffective(difficulty, cfg.healthCurveExponent, cfg.healthCurvePivot);
    }

    /** @deprecated use {@link #offenseEffective(long)} */
    @Deprecated
    public static double combatEffective(long difficulty) {
        return offenseEffective(difficulty);
    }

    public static double offenseBonus(long difficulty, double percentPerDifficulty) {
        if (percentPerDifficulty <= 0.0) {
            return 0.0;
        }
        return offenseEffective(difficulty) * (percentPerDifficulty / 100.0);
    }

    public static double healthBonus(long difficulty, double percentPerDifficulty) {
        if (percentPerDifficulty <= 0.0) {
            return 0.0;
        }
        return healthEffective(difficulty) * (percentPerDifficulty / 100.0);
    }

    /** @deprecated use {@link #offenseBonus(long, double)} */
    @Deprecated
    public static double combatBonus(long difficulty, double percentPerDifficulty) {
        return offenseBonus(difficulty, percentPerDifficulty);
    }

    /** Always {@code 0} — this mod never grants kill TP. */
    public static double killTrainingPointsFromHealth(double maxHealth) {
        return 0.0;
    }

    /** Always {@code 0} — this mod never grants kill TP. */
    public static double killTrainingPoints(long difficulty) {
        return 0.0;
    }

    /**
     * Legacy reward multiplier stub. Always {@code 1.0} — this mod does not
     * multiply Training Points or other reward events by difficulty.
     */
    public static double rewardMultiplier(long activeDifficulty) {
        return 1.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
