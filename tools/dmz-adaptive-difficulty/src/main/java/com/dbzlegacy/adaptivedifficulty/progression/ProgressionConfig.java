package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;

/**
 * Typed accessors for natural-progression feature toggles stored on {@link DifficultyConfig}.
 */
public final class ProgressionConfig {
    private ProgressionConfig() {}

    public static boolean masterEnabled() {
        return DifficultyConfig.get().enableProgression;
    }

    public static boolean potential() {
        return masterEnabled() && DifficultyConfig.get().enablePotential;
    }

    public static boolean globalTpBoost() {
        return masterEnabled() && DifficultyConfig.get().enableGlobalTpBoost;
    }

    public static boolean bioAndroid() {
        return masterEnabled() && DifficultyConfig.get().enableBioAndroid;
    }

    public static boolean raceLock() {
        return masterEnabled() && DifficultyConfig.get().enableRaceLock;
    }

    public static boolean yardrat() {
        return masterEnabled() && DifficultyConfig.get().enableYardrat;
    }

    public static boolean spiritualistKi() {
        return masterEnabled() && DifficultyConfig.get().enableSpiritualistKi;
    }

    public static boolean androidConversion() {
        return masterEnabled() && DifficultyConfig.get().enableAndroidConversion;
    }

    public static boolean kiWeapons() {
        return masterEnabled() && DifficultyConfig.get().enableKiWeapons;
    }

    public static boolean piercingBonus() {
        return masterEnabled() && DifficultyConfig.get().enablePiercingBonus;
    }

    public static boolean dotExtraDamage() {
        return masterEnabled() && DifficultyConfig.get().enableDotExtraDamage;
    }

    public static boolean apothicElemental() {
        return masterEnabled() && DifficultyConfig.get().enableApothicElemental;
    }

    public static boolean endDimensionStrength() {
        return masterEnabled() && DifficultyConfig.get().enableEndDimensionStrength;
    }

    public static boolean endPortalGuard() {
        return masterEnabled() && DifficultyConfig.get().enableEndPortalGuard;
    }

    /** End mob HP/DEF scaling — off by default (script v2.11.0). */
    public static boolean endMobScaling() {
        return endDimensionStrength() && DifficultyConfig.get().enableEndMobScaling;
    }

    public static boolean shadowDummyLimiter() {
        return masterEnabled() && DifficultyConfig.get().enableShadowDummyLimiter;
    }

    public static boolean skillUnlockService() {
        return masterEnabled() && DifficultyConfig.get().enableSkillUnlockService;
    }

    public static boolean prestigeSystem() {
        return masterEnabled() && DifficultyConfig.get().enablePrestigeSystem;
    }

    public static boolean playerStatChecker() {
        return masterEnabled() && DifficultyConfig.get().enablePlayerStatChecker;
    }

    public static String statusSummary() {
        DifficultyConfig c = DifficultyConfig.get();
        return "§eSkills§7: master=" + onOff(c.enableProgression)
                + " pot=" + onOff(c.enablePotential)
                + "\n§6TP§7: boost=" + onOff(c.enableGlobalTpBoost)
                + " bio=" + onOff(c.enableBioAndroid)
                + "\n§bRace§7: lock=" + onOff(c.enableRaceLock)
                + " yard=" + onOff(c.enableYardrat)
                + " spirit=" + onOff(c.enableSpiritualistKi)
                + " android=" + onOff(c.enableAndroidConversion)
                + "\n§cCombat§7: ki=" + onOff(c.enableKiWeapons)
                + " pierce=" + onOff(c.enablePiercingBonus)
                + " dot=" + onOff(c.enableDotExtraDamage)
                + " apothic=" + onOff(c.enableApothicElemental)
                + "\n§5End§7: end=" + onOff(c.enableEndDimensionStrength)
                + " endmobs=" + onOff(c.enableEndMobScaling)
                + " endportal=" + onOff(c.enableEndPortalGuard)
                + "\n§aShop§7: prestige=" + onOff(c.enablePrestigeSystem)
                + " skills=" + onOff(c.enableSkillUnlockService)
                + "\n§7Utility: shadow=" + onOff(c.enableShadowDummyLimiter)
                + " statcheck=" + onOff(c.enablePlayerStatChecker);
    }

    private static String onOff(boolean v) {
        return v ? "ON" : "OFF";
    }
}
