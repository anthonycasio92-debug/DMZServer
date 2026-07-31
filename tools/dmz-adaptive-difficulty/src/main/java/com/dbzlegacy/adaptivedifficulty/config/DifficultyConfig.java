package com.dbzlegacy.adaptivedifficulty.config;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraftforge.fml.loading.FMLPaths;

/** Mirrors the concept doc admin settings. Saved at {@code config/dmz_adaptive_difficulty.json}. */
public final class DifficultyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DifficultyConfig INSTANCE = new DifficultyConfig();

    public double prestigeMultiplier = 10.0;
    public double levelMultiplier = 1.0;
    public double teamBonusPercent = 10.0;
    public double contributionPercent = 25.0;
    /**
     * Legacy fields kept for old configs; pricing now uses iron-coin fields below.
     * {@code baseCost}/{@code costScaling} are ignored for difficulty payments.
     */
    public long baseCost = 1L;
    public long costScaling = 100L;
    /**
     * Iron coins charged per difficulty level at active/purchased = 0.
     * Example: 1 → first +1 costs 1 iron coin.
     */
    public long baseCostIronCoins = 1L;
    /**
     * Extra cost growth per current difficulty level.
     * Formula: cost ≈ amount × base × (1 + scale × (from + (amount-1)/2)).
     * Default 0.01 → at difficulty 100, each +1 costs ~2× base.
     */
    public double costScalePerDifficulty = 0.01;
    /** Lightman's coin item used as the unit price (default iron coin). */
    public String costCoinItem = "lightmanscurrency:coin_iron";
    /**
     * Legacy reward-curve divisor. Unused — this mod no longer grants or multiplies TP.
     */
    public double rewardScaling = 2_500.0;
    /** Legacy reward curve mode. Unused — kept for config compatibility. */
    public String rewardCurve = "power";
    /** Legacy — forced to {@code 0} on load (TP multipliers removed). */
    public double rewardCurveGain = 0.0;
    /** Legacy power-curve exponent. Unused. */
    public double rewardCurveExponent = 0.38;
    /** Legacy — forced to {@code 1} on load (TP multipliers removed). */
    public double maxRewardMultiplier = 1.0;
    /** Legacy kill-TP fields — unused; this mod never grants Training Points. */
    public double killTpRefAmount = 0.0;
    public double killTpRefDifficulty = 10_000_000.0;
    public double killTpExponent = 0.70;
    /** Legacy — forced to {@code 0} on load (no kill TP grants). */
    public double killTpPerHealth = 0.0;
    /** Legacy — forced to {@code 0} on load. */
    public double killTpMinimum = 0.0;
    /** Legacy — forced to {@code 0} on load. */
    public double maxKillTp = 0.0;
    /** Legacy — forced to {@code 0} on load (TPGainEvent never modified). */
    public double maxTpGainEvent = 0.0;
    /**
     * Offense (damage/defense) curve exponent. Higher = steeper growth with difficulty.
     * Effective = {@code pow(d, exp) * pow(pivot, 1-exp)}.
     */
    public double combatCurveExponent = 0.96;
    /** Difficulty where the offense curve matches old linear rates. */
    public long combatCurvePivot = 450L;
    /**
     * Health curve exponent — kept low so mob HP does not become unkillable sponges.
     */
    public double healthCurveExponent = 0.40;
    /** Pivot for the health curve (usually same ballpark as combat pivot). */
    public long healthCurvePivot = 250L;
    /** Flat health % rate (applied through the flat health curve). */
    public double healthPercentPerDifficulty = 0.45;
    /**
     * Per-difficulty damage % (offense curve).
     * Tuned so at ~8M (level-100k band) a base-3 melee hit stays under ~800k raw —
     * what most players can tank from HP alone — while still biting through ~200k DEF.
     */
    public double damagePercentPerDifficulty = 0.62;
    /**
     * Per-difficulty armor points (offense curve).
     * Tuned so mob armor ≈ ~200k at 8M, matching typical endgame player DEF.
     */
    public double defensePercentPerDifficulty = 1.85;
    public double movementPercentPer100Difficulty = 0.15;
    /** DMZ-style extra health % (kept low; health uses its own flat curve). */
    public double dmzExtraHealthPercent = 0.45;
    /** DMZ-style extra damage % (offense curve). */
    public double dmzExtraDamagePercent = 0.62;
    /** DMZ-style extra armor points (offense curve). */
    public double dmzExtraDefensePercent = 1.85;
    /** DMZ-style extra ki damage % (offense curve). */
    public double dmzExtraKiDamagePercent = 0.62;
    public double mobScaleRadius = 64.0;
    /**
     * Area difficulty mode (Scaling Health-inspired):
     * {@code weighted} (default), {@code average}, or {@code max}.
     */
    public String areaDifficultyMode = "weighted";
    /** Extra area difficulty percent per additional nearby player (Scaling Health group bonus). */
    public double areaGroupBonusPercent = 5.0;
    /** Random variance applied to area difficulty when scaling a mob (percent, e.g. 5 => 0.95–1.05). */
    public double areaDifficultyVariancePercent = 5.0;
    /**
     * Absolute ceiling on difficulty values. {@code 0} = no hardcap
     * (max is only calculated from DMZ stats + purchased + team bonuses).
     */
    public long hardCapDifficulty = 0L;

    /**
     * Reference caps used to document the top tier (Zenith).
     * Theoretical max ≈ referenceMaxLevel × referenceMaxPrestige × prestigeMultiplier
     * = 100000 × 10 × 10 = 10,000,000 with defaults.
     */
    public long referenceMaxLevel = 100_000L;
    public int referenceMaxPrestige = 10;

    // Ability unlock tier thresholds (admin-editable)
    public long tierAwakened = 10L;
    public long tierEnhanced = 50L;
    public long tierElite = 100L;
    public long tierAdvanced = 500L;
    public long tierMaster = 1_000L;
    public long tierLegendary = 5_000L;
    public long tierGod = 10_000L;
    public long tierDivine = 50_000L;
    public long tierImpossible = 100_000L;
    public long tierTranscendent = 250_000L;
    public long tierEternal = 500_000L;
    public long tierMythic = 1_000_000L;
    public long tierOmega = 2_500_000L;
    public long tierAbsolute = 5_000_000L;
    public long tierApex = 7_500_000L;
    /** Top tier — theoretical max for level 100k @ 10 prestiges. */
    public long tierZenith = 10_000_000L;
    /**
     * Difficulty payments always use Lightman's Currency (iron coins).
     * Kept for config compatibility; non-lightmans values are forced back to lightmans.
     */
    public String purchaseCurrency = "lightmans";
    public boolean scaleHostileOnly = true;
    /**
     * Apply DMZ extra health/defense/damage/ki percents to <b>all</b> hostiles
     * (not only {@code dragonminez:} mobs). Default true so vanilla/modded
     * hostiles scale the same way as DMZ enemies.
     */
    public boolean applyDmzExtrasToAllHostiles = true;
    public boolean enableRewardScaling = true;
    public boolean enableMobScaling = true;

    // Phase 2 systems
    public boolean enableElites = true;
    public double eliteChancePercent = 3.0;
    public double eliteStatMultiplier = 1.75;
    public double eliteRewardBonus = 2.0;
    public boolean enableMutations = true;
    public double mutationChancePercent = 5.0;
    public boolean enableAdaptiveAi = true;
    public boolean enableEnemyEvolution = true;
    public boolean enableBossScaling = true;
    public double bossStatMultiplier = 1.5;
    /** Natural (pre-scale) max-health at/above this marks a boss. Default keeps wardens/etc. */
    public double bossHealthThreshold = 300.0;
    public List<String> bossIdContains = new ArrayList<>(Arrays.asList(
            "boss", "warden", "wither", "ender_dragon", "raid_boss", "raidboss"
    ));
    /**
     * Optional ceiling on the health multiplier from the health curve.
     * {@code 0} or {@code 1} = uncapped (default). Absolute HP still limited by {@link #maxScaledHealth}.
     */
    public double maxHealthMultiplier = 0.0;
    /** Absolute max HP after scaling (also clamped to vanilla 1024). {@code 0} = vanilla 1024 only. */
    public double maxScaledHealth = 1024.0;
    public double maxMoveMultiplier = 2.0;
    /**
     * Optional armor-point ceiling from defense scaling.
     * {@code 0} = uncapped (default). Only values {@code > 0} apply a ceiling.
     */
    public double maxArmorBonus = 0.0;
    /**
     * Optional damage multiplier ceiling.
     * {@code 0} or {@code 1} = uncapped (default). Only values {@code > 1} apply a ceiling.
     */
    public double maxDamageMultiplier = 0.0;
    /** Admin permission node (Forge PermissionAPI / LuckPerms). Ops always allowed. */
    public String adminPermission = "difficulty.admin";
    /**
     * Player UI backend for {@code /difficulty}:
     * {@code cmi} (default), {@code auto} (CMI → chest → chat), {@code chest}, or {@code chat}.
     */
    public String guiBackend = "cmi";
    /**
     * If the Minecraft world is on Peaceful (no hostile spawns), restore it on server start.
     * Peaceful prevents adaptive mob scaling from doing anything.
     * Null in JSON means enabled (default true).
     */
    public Boolean restoreVanillaDifficultyFromPeaceful = Boolean.TRUE;
    /** Target vanilla difficulty when restoring from Peaceful: easy / normal / hard. */
    public String vanillaDifficulty = "hard";
    /**
     * Dimensions where adaptive difficulty skips scaling / AI / evolution / gravity.
     * Default: The End — owned by CNPC {@code End Dimension Strength.js}; stacking both
     * systems crushed TPS when players visited.
     */
    public List<String> disabledDimensions = new ArrayList<>(Arrays.asList("minecraft:the_end"));

    public boolean shouldRestoreVanillaFromPeaceful() {
        return restoreVanillaDifficultyFromPeaceful == null || restoreVanillaDifficultyFromPeaceful;
    }

    /** Tier thresholds — editable via {@code /difficulty admin set tierX <n>}. */
    public long tierThreshold(com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier tier) {
        if (tier == null) {
            return 0L;
        }
        return switch (tier) {
            case NONE -> 0L;
            case AWAKENED -> Math.max(0L, tierAwakened);
            case ENHANCED -> Math.max(0L, tierEnhanced);
            case ELITE -> Math.max(0L, tierElite);
            case ADVANCED -> Math.max(0L, tierAdvanced);
            case MASTER -> Math.max(0L, tierMaster);
            case LEGENDARY -> Math.max(0L, tierLegendary);
            case GOD -> Math.max(0L, tierGod);
            case DIVINE -> Math.max(0L, tierDivine);
            case IMPOSSIBLE -> Math.max(0L, tierImpossible);
            case TRANSCENDENT -> Math.max(0L, tierTranscendent);
            case ETERNAL -> Math.max(0L, tierEternal);
            case MYTHIC -> Math.max(0L, tierMythic);
            case OMEGA -> Math.max(0L, tierOmega);
            case ABSOLUTE -> Math.max(0L, tierAbsolute);
            case APEX -> Math.max(0L, tierApex);
            case ZENITH -> Math.max(0L, tierZenith);
        };
    }

    /** Theoretical max at reference caps (level 100k × 10 prestiges by default). */
    public long theoreticalMaxAtReferenceCaps() {
        long levelPart = Math.round(Math.max(1L, referenceMaxLevel) * Math.max(0.0, levelMultiplier));
        int p = Math.max(0, referenceMaxPrestige);
        if (p <= 0) {
            return Math.max(0L, levelPart);
        }
        long prestigeFactor = Math.round(p * Math.max(0.0, prestigeMultiplier));
        try {
            return Math.multiplyExact(levelPart, Math.max(1L, prestigeFactor));
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE / 4L;
        }
    }

    private DifficultyConfig() {}

    public static DifficultyConfig get() {
        return INSTANCE;
    }

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("dmz_adaptive_difficulty.json");
    }

    public static void load() {
        Path file = path();
        try {
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    DifficultyConfig loaded = GSON.fromJson(reader, DifficultyConfig.class);
                    if (loaded != null) {
                        normalize(loaded);
                        INSTANCE = loaded;
                    }
                }
            }
            save();
        } catch (Exception e) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] failed to load config: {}", AdaptiveDifficultyMod.MOD_ID, e.toString());
            INSTANCE = new DifficultyConfig();
        }
    }

    private static void normalize(DifficultyConfig cfg) {
        if (cfg.bossIdContains == null) {
            cfg.bossIdContains = new ArrayList<>();
        }
        if (cfg.disabledDimensions == null) {
            cfg.disabledDimensions = new ArrayList<>(Arrays.asList("minecraft:the_end"));
        }
        if (cfg.guiBackend == null || cfg.guiBackend.isBlank()) {
            cfg.guiBackend = "cmi";
        } else {
            String gui = cfg.guiBackend.trim().toLowerCase();
            // Legacy DeluxeMenus configs → CMI
            if (gui.equals("deluxemenus") || gui.equals("deluxe") || gui.equals("dm")) {
                cfg.guiBackend = "cmi";
            }
        }
        if (cfg.vanillaDifficulty == null || cfg.vanillaDifficulty.isBlank()) {
            cfg.vanillaDifficulty = "hard";
        }
        if (cfg.adminPermission == null || cfg.adminPermission.isBlank()) {
            cfg.adminPermission = "difficulty.admin";
        }
        // Always Lightman's for difficulty payments
        cfg.purchaseCurrency = "lightmans";
        if (cfg.costCoinItem == null || cfg.costCoinItem.isBlank()) {
            cfg.costCoinItem = "lightmanscurrency:coin_iron";
        }
        if (cfg.baseCostIronCoins < 0L) {
            cfg.baseCostIronCoins = 1L;
        }
        if (cfg.costScalePerDifficulty < 0.0) {
            cfg.costScalePerDifficulty = 0.01;
        }
        if (cfg.rewardCurve == null || cfg.rewardCurve.isBlank()) {
            cfg.rewardCurve = "power";
        }
        if (cfg.rewardCurveGain < 0.0) {
            cfg.rewardCurveGain = 0.0;
        }
        if (cfg.rewardCurveExponent <= 0.0) {
            cfg.rewardCurveExponent = 0.38;
        }
        if (cfg.maxRewardMultiplier < 0.0) {
            cfg.maxRewardMultiplier = 1.0;
        }
        if (cfg.killTpRefDifficulty <= 0.0) {
            cfg.killTpRefDifficulty = 10_000_000.0;
        }
        if (cfg.killTpExponent <= 0.0) {
            cfg.killTpExponent = 0.70;
        }
        if (cfg.killTpRefAmount < 0.0) {
            cfg.killTpRefAmount = 0.0;
        }
        if (cfg.killTpPerHealth < 0.0) {
            cfg.killTpPerHealth = 0.0;
        }
        if (cfg.killTpMinimum < 0.0) {
            cfg.killTpMinimum = 0.0;
        }
        if (cfg.maxKillTp < 0.0) {
            cfg.maxKillTp = 0.0;
        }
        if (cfg.maxTpGainEvent < 0.0) {
            cfg.maxTpGainEvent = 0.0;
        }
        if (cfg.combatCurveExponent <= 0.0) {
            cfg.combatCurveExponent = 0.96;
        }
        if (cfg.combatCurvePivot < 1L) {
            cfg.combatCurvePivot = 450L;
        }
        if (cfg.healthCurveExponent <= 0.0) {
            cfg.healthCurveExponent = 0.40;
        }
        if (cfg.healthCurvePivot < 1L) {
            cfg.healthCurvePivot = 250L;
        }
        // Migrate stock offense values → endgame-anchored damage/defense.
        boolean retuned = false;
        if (nearly(cfg.combatCurveExponent, 0.70)
                || nearly(cfg.combatCurveExponent, 0.88)
                || nearly(cfg.combatCurveExponent, 0.93)) {
            cfg.combatCurveExponent = 0.96;
            retuned = true;
        }
        if (cfg.combatCurvePivot == 250L || cfg.combatCurvePivot == 500L) {
            cfg.combatCurvePivot = 450L;
            retuned = true;
        }
        // TP fully removed — force every legacy TP knob inert.
        if (!nearly(cfg.rewardCurveGain, 0.0)) {
            cfg.rewardCurveGain = 0.0;
            retuned = true;
        }
        if (!nearly(cfg.maxRewardMultiplier, 1.0)) {
            cfg.maxRewardMultiplier = 1.0;
            retuned = true;
        }
        if (nearly(cfg.rewardScaling, 1_000.0)) {
            cfg.rewardScaling = 2_500.0;
            retuned = true;
        }
        if (!nearly(cfg.killTpRefAmount, 0.0)) {
            cfg.killTpRefAmount = 0.0;
            retuned = true;
        }
        if (!nearly(cfg.killTpPerHealth, 0.0)) {
            cfg.killTpPerHealth = 0.0;
            retuned = true;
        }
        if (!nearly(cfg.killTpMinimum, 0.0)) {
            cfg.killTpMinimum = 0.0;
            retuned = true;
        }
        if (!nearly(cfg.maxKillTp, 0.0)) {
            cfg.maxKillTp = 0.0;
            retuned = true;
        }
        if (!nearly(cfg.maxTpGainEvent, 0.0)) {
            cfg.maxTpGainEvent = 0.0;
            retuned = true;
        }
        if (nearly(cfg.healthPercentPerDifficulty, 1.0)) {
            cfg.healthPercentPerDifficulty = 0.45;
            retuned = true;
        }
        if (nearly(cfg.dmzExtraHealthPercent, 1.0)) {
            cfg.dmzExtraHealthPercent = 0.45;
            retuned = true;
        }
        // Prior rates overshot endgame (~1.5M raw / ~811k armor at 8M vs ~800k HP / ~200k DEF).
        if (nearly(cfg.damagePercentPerDifficulty, 1.0)
                || nearly(cfg.damagePercentPerDifficulty, 1.5)
                || nearly(cfg.damagePercentPerDifficulty, 2.5)
                || nearly(cfg.damagePercentPerDifficulty, 3.2)) {
            cfg.damagePercentPerDifficulty = 0.62;
            retuned = true;
        }
        if (nearly(cfg.dmzExtraDamagePercent, 1.0)
                || nearly(cfg.dmzExtraDamagePercent, 1.5)
                || nearly(cfg.dmzExtraDamagePercent, 2.5)
                || nearly(cfg.dmzExtraDamagePercent, 3.2)) {
            cfg.dmzExtraDamagePercent = 0.62;
            retuned = true;
        }
        if (nearly(cfg.dmzExtraKiDamagePercent, 1.0)
                || nearly(cfg.dmzExtraKiDamagePercent, 1.5)
                || nearly(cfg.dmzExtraKiDamagePercent, 2.5)
                || nearly(cfg.dmzExtraKiDamagePercent, 3.2)) {
            cfg.dmzExtraKiDamagePercent = 0.62;
            retuned = true;
        }
        if (nearly(cfg.defensePercentPerDifficulty, 3.0)
                || nearly(cfg.defensePercentPerDifficulty, 4.5)
                || nearly(cfg.defensePercentPerDifficulty, 5.0)
                || nearly(cfg.defensePercentPerDifficulty, 7.5)) {
            cfg.defensePercentPerDifficulty = 1.85;
            retuned = true;
        }
        if (nearly(cfg.dmzExtraDefensePercent, 3.0)
                || nearly(cfg.dmzExtraDefensePercent, 4.5)
                || nearly(cfg.dmzExtraDefensePercent, 5.0)
                || nearly(cfg.dmzExtraDefensePercent, 7.5)) {
            cfg.dmzExtraDefensePercent = 1.85;
            retuned = true;
        }
        // Health: curve growth up to vanilla attribute ceiling (1024).
        if (nearly(cfg.maxHealthMultiplier, 50.0) || nearly(cfg.maxHealthMultiplier, 8.0)) {
            cfg.maxHealthMultiplier = 0.0;
            retuned = true;
        }
        if (nearly(cfg.maxScaledHealth, 400.0)) {
            cfg.maxScaledHealth = 1024.0;
            retuned = true;
        }
        // Remove stock damage/armor ceilings — offense grows with the curve only.
        if (nearly(cfg.maxDamageMultiplier, 50.0)
                || nearly(cfg.maxDamageMultiplier, 25_000.0)
                || nearly(cfg.maxDamageMultiplier, 50_000.0)) {
            cfg.maxDamageMultiplier = 0.0;
            retuned = true;
        }
        if (nearly(cfg.maxArmorBonus, 20.0)
                || nearly(cfg.maxArmorBonus, 30.0)
                || nearly(cfg.maxArmorBonus, 100.0)) {
            cfg.maxArmorBonus = 0.0;
            retuned = true;
        }
        if (cfg.maxDamageMultiplier < 0.0) {
            cfg.maxDamageMultiplier = 0.0;
        }
        if (cfg.maxArmorBonus < 0.0) {
            cfg.maxArmorBonus = 0.0;
        }
        if (retuned) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] retuned: kill/train TP grants removed; offense/health curves kept",
                    AdaptiveDifficultyMod.MOD_ID
            );
        }
        if (cfg.areaDifficultyMode == null || cfg.areaDifficultyMode.isBlank()) {
            cfg.areaDifficultyMode = "weighted";
        }
        if (cfg.hardCapDifficulty < 0L) {
            cfg.hardCapDifficulty = 0L;
        }
        // Legacy default was an artificial 1_000_000 ceiling — disable so max follows stats.
        if (cfg.hardCapDifficulty == 1_000_000L) {
            cfg.hardCapDifficulty = 0L;
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] hardCapDifficulty 1000000 → 0 (no hardcap; max from DMZ stats)",
                    AdaptiveDifficultyMod.MOD_ID
            );
        }
    }

    private static boolean nearly(double value, double expected) {
        return Math.abs(value - expected) < 1.0e-9;
    }

    public static void save() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(INSTANCE, writer);
            }
            com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier.invalidateThresholdCache();
            com.dbzlegacy.adaptivedifficulty.calc.ScalingCurves.invalidateLut();
            com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty.clearCache();
        } catch (IOException e) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] failed to save config: {}", AdaptiveDifficultyMod.MOD_ID, e.toString());
        }
    }

    public static void reload() {
        load();
        com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier.invalidateThresholdCache();
        com.dbzlegacy.adaptivedifficulty.calc.ScalingCurves.invalidateLut();
        com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty.clearCache();
    }
}
