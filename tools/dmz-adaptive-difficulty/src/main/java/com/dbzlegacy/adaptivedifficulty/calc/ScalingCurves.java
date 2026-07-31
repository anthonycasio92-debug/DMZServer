package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;

/**
 * Separate curves for offense (damage/defense), health, and TP rewards.
 * <p>
 * Offense uses a steeper power curve so fights get meaner without ballooning HP.
 * Health uses a flat curve + lower caps so mobs stay killable.
 * Rewards use a soft log curve so TP does not explode at high difficulty.
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

    /**
     * Reward / TP multiplier curve.
     * Default: {@code 1 + gain * ln(1 + d / rewardScaling)}, soft-capped.
     */
    public static double rewardMultiplier(long activeDifficulty) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableRewardScaling || cfg.rewardScaling <= 0.0) {
            return 1.0;
        }
        if (activeDifficulty <= 0L) {
            return 1.0;
        }
        String mode = cfg.rewardCurve == null ? "log" : cfg.rewardCurve.trim().toLowerCase();
        double d = activeDifficulty;
        double scale = Math.max(1.0e-6, cfg.rewardScaling);
        double mult = switch (mode) {
            case "linear", "lin" -> 1.0 + (d / scale);
            case "sqrt", "root" -> 1.0 + Math.sqrt(d / scale) * Math.max(0.0, cfg.rewardCurveGain);
            case "power", "pow" -> {
                double exp = clamp(cfg.rewardCurveExponent, 0.05, 1.0);
                yield 1.0 + Math.pow(d / scale, exp) * Math.max(0.0, cfg.rewardCurveGain);
            }
            default -> { // log
                double gain = Math.max(0.0, cfg.rewardCurveGain);
                yield 1.0 + gain * Math.log1p(d / scale);
            }
        };
        double cap = cfg.maxRewardMultiplier;
        if (cap > 1.0) {
            mult = Math.min(mult, cap);
        }
        return Math.max(1.0, mult);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
