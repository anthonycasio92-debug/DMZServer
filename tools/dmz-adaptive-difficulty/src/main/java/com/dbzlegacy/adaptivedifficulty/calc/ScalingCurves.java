package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Offense / health curves with a small LUT so combat paths avoid {@code Math.pow}.
 * <p>
 * Training Points are never scaled or granted by this mod.
 * Reward multiplier follows concept §15 for XP / drops only.
 */
public final class ScalingCurves {
    /** Bucket size for LUT keys — balances precision vs cache size. */
    private static final long BUCKET = 25L;
    private static final Map<Long, Double> OFFENSE_LUT = new ConcurrentHashMap<>();
    private static final Map<Long, Double> HEALTH_LUT = new ConcurrentHashMap<>();
    private static volatile long offensePivot = -1L;
    private static volatile long healthPivot = -1L;
    private static volatile long offenseExpBits = 0L;
    private static volatile long healthExpBits = 0L;

    private ScalingCurves() {}

    /** Call when config reloads so LUT entries match new exponents/pivots. */
    public static void invalidateLut() {
        OFFENSE_LUT.clear();
        HEALTH_LUT.clear();
        offensePivot = -1L;
        healthPivot = -1L;
    }

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
        if (difficulty <= 0L) {
            return 0.0;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        ensureOffenseKey(cfg);
        long key = bucket(difficulty);
        return OFFENSE_LUT.computeIfAbsent(key, k ->
                curvedEffective(k, cfg.combatCurveExponent, cfg.combatCurvePivot));
    }

    /** Health curve (flatter — avoids unkillable sponge mobs). */
    public static double healthEffective(long difficulty) {
        if (difficulty <= 0L) {
            return 0.0;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        ensureHealthKey(cfg);
        long key = bucket(difficulty);
        return HEALTH_LUT.computeIfAbsent(key, k ->
                curvedEffective(k, cfg.healthCurveExponent, cfg.healthCurvePivot));
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
     * Concept §15: {@code 1 + Difficulty / Reward Scaling}.
     * Used for XP / rare-drop chance only — never Training Points.
     */
    public static double rewardMultiplier(long activeDifficulty) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableRewardScaling || activeDifficulty <= 0L) {
            return 1.0;
        }
        double scale = Math.max(1.0, cfg.rewardScaling);
        // Soft cap so drop odds stay sane at multi-million difficulty.
        return Math.min(25.0, 1.0 + (activeDifficulty / scale));
    }

    private static long bucket(long difficulty) {
        if (difficulty <= 0L) {
            return 0L;
        }
        return ((difficulty + BUCKET - 1L) / BUCKET) * BUCKET;
    }

    private static void ensureOffenseKey(DifficultyConfig cfg) {
        long expBits = Double.doubleToLongBits(cfg.combatCurveExponent);
        if (offensePivot != cfg.combatCurvePivot || offenseExpBits != expBits) {
            OFFENSE_LUT.clear();
            offensePivot = cfg.combatCurvePivot;
            offenseExpBits = expBits;
        }
        if (OFFENSE_LUT.size() > 2048) {
            OFFENSE_LUT.clear();
        }
    }

    private static void ensureHealthKey(DifficultyConfig cfg) {
        long expBits = Double.doubleToLongBits(cfg.healthCurveExponent);
        if (healthPivot != cfg.healthCurvePivot || healthExpBits != expBits) {
            HEALTH_LUT.clear();
            healthPivot = cfg.healthCurvePivot;
            healthExpBits = expBits;
        }
        if (HEALTH_LUT.size() > 2048) {
            HEALTH_LUT.clear();
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
