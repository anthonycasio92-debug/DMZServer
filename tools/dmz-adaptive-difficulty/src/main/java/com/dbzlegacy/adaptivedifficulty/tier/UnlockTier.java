package com.dbzlegacy.adaptivedifficulty.tier;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;

/**
 * V3 unlockable difficulty tiers (1–7).
 * Distinct from combat-AI progression — these are purchase/activation gates.
 */
public enum UnlockTier {
    T1(1, "Awakened", 1L, 1_000L, 1L),
    T2(2, "Enhanced", 500L, 5_000L, 5L),
    T3(3, "Elite", 1_000L, 10_000L, 15L),
    T4(4, "Advanced", 5_000L, 25_000L, 50L),
    T5(5, "Master", 10_000L, 50_000L, 150L),
    T6(6, "Legendary", 50_000L, 100_000L, 500L),
    T7(7, "God", 100_000L, 250_000L, 1_500L);

    public final int id;
    public final String display;
    public final long defaultRequiredLevel;
    public final long defaultMaxDifficulty;
    public final long defaultActivationCost;

    UnlockTier(int id, String display, long requiredLevel, long maxDifficulty, long activationCost) {
        this.id = id;
        this.display = display;
        this.defaultRequiredLevel = requiredLevel;
        this.defaultMaxDifficulty = maxDifficulty;
        this.defaultActivationCost = activationCost;
    }

    public static UnlockTier byId(int id) {
        for (UnlockTier t : values()) {
            if (t.id == id) {
                return t;
            }
        }
        return null;
    }

    public long requiredDmzLevel() {
        return DifficultyConfig.get().tierRequiredLevel(id);
    }

    /**
     * Prestige skill level that bypasses the DMZ level gate
     * ({@link UnlockSystem}: prestige ≥ tier id).
     */
    public int requiredPrestige() {
        return id;
    }

    /** Short unlock gate for GUI / chat: {@code DMZ 100000 or Prestige 7}. */
    public String requirementTip() {
        return "DMZ " + requiredDmzLevel() + " or Prestige " + requiredPrestige();
    }

    public long maxDifficulty() {
        return DifficultyConfig.get().tierMaxDifficulty(id);
    }

    public long activationCost() {
        return DifficultyConfig.get().tierActivationCost(id);
    }

    /** Base cost scaled by the player's current DMZ level. */
    public long activationCostForLevel(int dmzLevel) {
        return DifficultyConfig.get().tierActivationCostScaled(id, dmzLevel);
    }

    /**
     * Nearby-mob scale as a fraction of the player's post-transform / limit-release stats.
     * Defaults: T1 0.21 · T2 0.42 · T3 0.65 · T4 0.90 · T5 1.35 · T6 1.60 · T7 2.00.
     */
    public double enemyScalingMultiplier() {
        return DifficultyConfig.get().tierPlayerStatPercent(id);
    }

    /** Minimum active tier required for AI/evolution depth. */
    public int aiUnlockLevel() {
        return id;
    }
}
