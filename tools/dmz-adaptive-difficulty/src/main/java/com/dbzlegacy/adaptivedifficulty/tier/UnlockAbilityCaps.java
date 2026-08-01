package com.dbzlegacy.adaptivedifficulty.tier;

/**
 * Maps Unlock Tier → AI / ability ladder depth.
 * Higher unlock tiers unlock smarter AI and more Enemy Evolution kits.
 */
public final class UnlockAbilityCaps {
    private UnlockAbilityCaps() {}

    /**
     * Soft floor — even a low CR proxy still gets at least this much kit for the unlock tier.
     */
    public static DifficultyTier minAbilityTier(int unlockTier) {
        return switch (Math.max(0, unlockTier)) {
            case 1 -> DifficultyTier.AWAKENED;
            case 2 -> DifficultyTier.ENHANCED;
            case 3 -> DifficultyTier.ELITE;
            case 4 -> DifficultyTier.ADVANCED;
            case 5 -> DifficultyTier.MASTER;
            case 6 -> DifficultyTier.LEGENDARY;
            case 7 -> DifficultyTier.GOD;
            default -> DifficultyTier.NONE;
        };
    }

    /**
     * Hard ceiling — high player stats cannot grant Zenith kits on a low Unlock Tier.
     */
    public static DifficultyTier maxAbilityTier(int unlockTier) {
        return switch (Math.max(0, unlockTier)) {
            case 1 -> DifficultyTier.ENHANCED;
            case 2 -> DifficultyTier.ELITE;
            case 3 -> DifficultyTier.ADVANCED;
            case 4 -> DifficultyTier.LEGENDARY;
            case 5 -> DifficultyTier.DIVINE;
            case 6 -> DifficultyTier.MYTHIC;
            case 7 -> DifficultyTier.ZENITH;
            default -> DifficultyTier.NONE;
        };
    }

    /** Clamp a difficulty-derived ability tier into the unlock-tier band. */
    public static DifficultyTier clamp(DifficultyTier fromDifficulty, int unlockTier) {
        if (unlockTier <= 0) {
            return DifficultyTier.NONE;
        }
        DifficultyTier min = minAbilityTier(unlockTier);
        DifficultyTier max = maxAbilityTier(unlockTier);
        DifficultyTier base = fromDifficulty == null ? DifficultyTier.NONE : fromDifficulty;
        if (base.ordinalPower() < min.ordinalPower()) {
            return min;
        }
        if (base.ordinalPower() > max.ordinalPower()) {
            return max;
        }
        return base;
    }
}
