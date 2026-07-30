package com.dbzlegacy.adaptivedifficulty.tier;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;

/** Concept §10 difficulty tiers that unlock AI / abilities. Thresholds are admin-configurable. */
public enum DifficultyTier {
    NONE(0, "None"),
    AWAKENED(10, "Awakened"),
    ENHANCED(50, "Enhanced"),
    ELITE(100, "Elite"),
    ADVANCED(500, "Advanced"),
    MASTER(1_000, "Master"),
    LEGENDARY(5_000, "Legendary"),
    GOD(10_000, "God"),
    DIVINE(50_000, "Divine"),
    IMPOSSIBLE(100_000, "Impossible");

    /** Concept default threshold (used when config is unavailable). */
    public final long threshold;
    public final String display;

    DifficultyTier(long threshold, String display) {
        this.threshold = threshold;
        this.display = display;
    }

    /** Live threshold from config (falls back to concept default). */
    public long threshold() {
        return DifficultyConfig.get().tierThreshold(this);
    }

    public static DifficultyTier of(long difficulty) {
        DifficultyTier best = NONE;
        for (DifficultyTier tier : values()) {
            if (difficulty >= tier.threshold()) {
                best = tier;
            }
        }
        return best;
    }

    public int ordinalPower() {
        return ordinal();
    }
}
