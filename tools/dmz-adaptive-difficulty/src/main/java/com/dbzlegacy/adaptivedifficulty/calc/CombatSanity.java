package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;

/**
 * Race-agnostic clamps so future custom races / forms cannot NaN, explode, or
 * poison AdaptiveDifficulty when JSON multipliers or getters misbehave.
 * <p>
 * AD never hardcodes race ids — every race is handled through live DMZ stats +
 * these bounds.
 */
public final class CombatSanity {
    /** Absolute ceiling on detected form⊕stack (planned ×80 + headroom). */
    public static final double DEFAULT_MAX_FORM_BOOST = 100.0;
    /** Soft ceiling on a single live combat channel fed into blend math. */
    public static final double DEFAULT_MAX_LIVE_CHANNEL = 50_000_000.0;
    /** Reject baseline samples older than this (ms) after race/form chaos. */
    public static final long BASELINE_MAX_AGE_MS = 6 * 60 * 60 * 1000L;

    private CombatSanity() {}

    public static double maxFormBoost() {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (cfg == null || !(cfg.maxFormBoost > 1.0)) {
            return DEFAULT_MAX_FORM_BOOST;
        }
        return Math.max(8.0, Math.min(500.0, cfg.maxFormBoost));
    }

    public static double maxLiveChannel() {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (cfg == null || !(cfg.maxLiveCombatChannel > 1.0)) {
            return DEFAULT_MAX_LIVE_CHANNEL;
        }
        return Math.max(10_000.0, Math.min(1.0e12, cfg.maxLiveCombatChannel));
    }

    /** Finite form factor in {@code [1, maxFormBoost]} (invalid → 1). */
    public static double saneFormMult(double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            return 1.0;
        }
        return Math.max(1.0, Math.min(maxFormBoost(), value));
    }

    /** Finite positive live channel, clamped to the soft ceiling. */
    public static double saneLive(double value, double floor) {
        double f = floor > 0.0 && Double.isFinite(floor) ? floor : 1.0;
        if (!Double.isFinite(value) || value <= 0.0) {
            return f;
        }
        return Math.max(f, Math.min(maxLiveChannel(), value));
    }

    public static double sanePositive(double value, double fallback) {
        if (!Double.isFinite(value) || value <= 0.0) {
            return fallback;
        }
        return value;
    }

    public static boolean usableBaseline(long atMs, String baselineRace, String liveRace) {
        if (atMs <= 0L) {
            return false;
        }
        long age = System.currentTimeMillis() - atMs;
        if (age < 0L || age > BASELINE_MAX_AGE_MS) {
            return false;
        }
        String a = baselineRace == null ? "" : baselineRace.trim().toLowerCase();
        String b = liveRace == null ? "" : liveRace.trim().toLowerCase();
        // Empty live race = unknown; keep baseline. Race swap must clear elsewhere.
        if (!a.isEmpty() && !b.isEmpty() && !a.equals(b)) {
            return false;
        }
        return true;
    }
}
