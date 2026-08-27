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

    public static boolean flight() {
        return masterEnabled() && DifficultyConfig.get().enableFlightProgression;
    }

    public static boolean sprintJump() {
        return masterEnabled() && DifficultyConfig.get().enableSprintJump;
    }

    public static boolean meditation() {
        return masterEnabled() && DifficultyConfig.get().enableMeditation;
    }

    public static boolean potential() {
        return masterEnabled() && DifficultyConfig.get().enablePotential;
    }

    public static boolean farmingTp() {
        return masterEnabled() && DifficultyConfig.get().enableFarmingTp;
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

    public static boolean fabledBridge() {
        return DifficultyConfig.get().enableFabledBridge;
    }

    public static boolean energyManaSync() {
        return fabledBridge() && DifficultyConfig.get().enableEnergyManaSync;
    }

    public static boolean statScreenSync() {
        return fabledBridge() && DifficultyConfig.get().enableStatScreenSync;
    }

    public static boolean tpSpMirror() {
        return fabledBridge() && DifficultyConfig.get().enableTpSpMirror;
    }

    public static boolean attrMultiBonus() {
        return fabledBridge() && DifficultyConfig.get().enableAttrMultiBonus;
    }

    public static boolean prestigeSkillSync() {
        return fabledBridge() && DifficultyConfig.get().enablePrestigeSkillSync;
    }

    public static boolean prestigeFactionSync() {
        return fabledBridge() && DifficultyConfig.get().enablePrestigeFactionSync;
    }

    public static boolean valueCleaner() {
        return fabledBridge() && DifficultyConfig.get().enableValueCleaner;
    }

    public static boolean raceClassSync() {
        return fabledBridge() && DifficultyConfig.get().enableRaceClassSync;
    }

    public static boolean classPermissionSync() {
        return fabledBridge() && DifficultyConfig.get().enableClassPermissionSync;
    }

    public static String statusSummary() {
        DifficultyConfig c = DifficultyConfig.get();
        return "master=" + onOff(c.enableProgression)
                + " fly=" + onOff(c.enableFlightProgression)
                + " sprint=" + onOff(c.enableSprintJump)
                + " med=" + onOff(c.enableMeditation)
                + " pot=" + onOff(c.enablePotential)
                + " farm=" + onOff(c.enableFarmingTp)
                + " boost=" + onOff(c.enableGlobalTpBoost)
                + " bio=" + onOff(c.enableBioAndroid)
                + " lock=" + onOff(c.enableRaceLock)
                + " yard=" + onOff(c.enableYardrat)
                + " spirit=" + onOff(c.enableSpiritualistKi)
                + " android=" + onOff(c.enableAndroidConversion)
                + " ki=" + onOff(c.enableKiWeapons)
                + " pierce=" + onOff(c.enablePiercingBonus)
                + " end=" + onOff(c.enableEndDimensionStrength)
                + " shadow=" + onOff(c.enableShadowDummyLimiter)
                + " skills=" + onOff(c.enableSkillUnlockService)
                + " prestige=" + onOff(c.enablePrestigeSystem)
                + " statcheck=" + onOff(c.enablePlayerStatChecker)
                + " fabled=" + onOff(c.enableFabledBridge);
    }

    private static String onOff(boolean v) {
        return v ? "ON" : "OFF";
    }
}
