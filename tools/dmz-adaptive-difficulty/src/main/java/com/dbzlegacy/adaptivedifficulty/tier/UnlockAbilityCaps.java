package com.dbzlegacy.adaptivedifficulty.tier;

/**
 * Maps Unlock Tier → AI / Enemy Evolution kit depth.
 * <p>
 * Each unlock tier has a distinct soft floor so mobs feel different as players
 * buy higher tiers. High player-stat proxies may raise a mob within the band
 * up to the hard ceiling — they cannot grant Zenith kits on a low unlock.
 * <pre>
 * Unlock  Soft floor   Hard ceiling
 * T1      Awakened     Enhanced
 * T2      Enhanced     Elite
 * T3      Elite        Advanced
 * T4      Advanced     Master
 * T5      Master       Divine
 * T6      Legendary    Mythic
 * T7      God          Zenith
 * </pre>
 */
public final class UnlockAbilityCaps {
    private UnlockAbilityCaps() {}

    /**
     * Soft floor — every scaled mob at this unlock gets at least this kit,
     * even when the CR/offense proxy is low.
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
     * Hard ceiling — high player stats cannot grant kits above this for the unlock.
     */
    public static DifficultyTier maxAbilityTier(int unlockTier) {
        return switch (Math.max(0, unlockTier)) {
            case 1 -> DifficultyTier.ENHANCED;
            case 2 -> DifficultyTier.ELITE;
            case 3 -> DifficultyTier.ADVANCED;
            case 4 -> DifficultyTier.MASTER;
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

    /**
     * Resolve the live AI/ability kit from unlock tier + difficulty proxy.
     * Unlock tier is the primary signal; proxy only adds headroom inside the band.
     */
    public static DifficultyTier resolve(long difficultyProxy, int unlockTier) {
        if (unlockTier <= 0) {
            return DifficultyTier.NONE;
        }
        return clamp(DifficultyTier.of(Math.max(0L, difficultyProxy)), unlockTier);
    }
}
