package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;

/**
 * Separate curves for offense (damage/defense), health, and TP rewards.
 * <p>
 * Offense uses a steeper power curve so fights get meaner without ballooning HP.
 * Health uses a flat curve + lower caps so mobs stay killable.
 * Rewards use a diminishing power curve (uncapped by default) so TP keeps growing
 * at high difficulty without going linear.
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
     * Kill TP from the mob's max health (no difficulty / elite / boss TP multipliers).
     * Default: {@code maxHealth × killTpPerHealth} (5000) → ~5.1M at 1024 HP, capped by {@code maxKillTp}.
     */
    public static double killTrainingPointsFromHealth(double maxHealth) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableRewardScaling) {
            return 0.0;
        }
        double hp = Math.max(0.0, maxHealth);
        double tp = hp * Math.max(0.0, cfg.killTpPerHealth);
        tp = Math.max(cfg.killTpMinimum, tp);
        if (cfg.maxKillTp > 0.0) {
            tp = Math.min(tp, cfg.maxKillTp);
        }
        return tp;
    }

    /**
     * @deprecated Kill TP is health-based via {@link #killTrainingPointsFromHealth(double)}.
     * Kept for admin previews that still pass a difficulty id.
     */
    @Deprecated
    public static double killTrainingPoints(long difficulty) {
        // Preview only: estimate TP for a typical fully-scaled zombie-sized mob at this difficulty.
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableRewardScaling || difficulty <= 0L) {
            return Math.max(cfg.killTpMinimum, 20.0 * Math.max(0.0, cfg.killTpPerHealth));
        }
        double healthMult = 1.0 + healthBonus(difficulty, cfg.healthPercentPerDifficulty);
        healthMult += healthBonus(difficulty, cfg.dmzExtraHealthPercent);
        if (cfg.maxHealthMultiplier > 1.0) {
            healthMult = Math.min(healthMult, cfg.maxHealthMultiplier);
        }
        double cap = cfg.maxScaledHealth > 0.0 ? Math.min(cfg.maxScaledHealth, 1024.0) : 1024.0;
        double estHp = Math.min(cap, 20.0 * healthMult);
        return killTrainingPointsFromHealth(estHp);
    }

    /**
     * Legacy reward multiplier (XP / drops only). Always {@code 1.0} — TP multipliers
     * are fully removed. Kept so call sites compile without reintroducing TP scaling.
     */
    public static double rewardMultiplier(long activeDifficulty) {
        return 1.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
