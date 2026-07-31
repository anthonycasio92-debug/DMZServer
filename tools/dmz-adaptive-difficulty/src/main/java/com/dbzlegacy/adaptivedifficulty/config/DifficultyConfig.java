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

    /**
     * Master switch. When {@code false}, scaling / rewards / AI / purchases are inert.
     * Toggle in-game: {@code /difficulty admin off|on|toggle}.
     */
    public boolean enabled = true;

    public double prestigeMultiplier = 10.0;
    public double levelMultiplier = 1.0;
    public double teamBonusPercent = 10.0;
    public double contributionPercent = 25.0;
    /** Soft divisor for kill XP multiplier. */
    public double rewardScaling = 2_500.0;
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
    /** Top internal ability ladder threshold (not a player Unlock Tier). */
    public long tierZenith = 10_000_000L;
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
    /**
     * Absolute max HP after scaling.
     * {@code 0} = uncapped (default). Vanilla's 1024 max_health attribute limit is raised at mod boot.
     */
    public double maxScaledHealth = 0.0;
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

    // ── V3 Combat Rating weights ───────────────────────────────────────────
    public double combatRatingDmzWeight = 1.0;
    public double combatRatingPrestigeWeight = 1000.0;
    public double combatRatingTransformWeight = 1.0;
    public double combatRatingDifficultyWeight = 1.0;

    // ── V3 Unlock tier requirements / ceilings / activation costs ───────────
    public long unlockTier1Level = 100L;
    public long unlockTier2Level = 500L;
    public long unlockTier3Level = 1_000L;
    public long unlockTier4Level = 5_000L;
    public long unlockTier5Level = 10_000L;
    public long unlockTier6Level = 50_000L;
    public long unlockTier7Level = 100_000L;
    public long unlockTier1Max = 1_000L;
    public long unlockTier2Max = 5_000L;
    public long unlockTier3Max = 10_000L;
    public long unlockTier4Max = 25_000L;
    public long unlockTier5Max = 50_000L;
    public long unlockTier6Max = 100_000L;
    public long unlockTier7Max = 250_000L;
    public long unlockTier1Cost = 100L;
    public long unlockTier2Cost = 500L;
    public long unlockTier3Cost = 1_500L;
    public long unlockTier4Cost = 5_000L;
    public long unlockTier5Cost = 15_000L;
    public long unlockTier6Cost = 50_000L;
    public long unlockTier7Cost = 150_000L;
    public double unlockTier1EnemyMult = 1.0;
    public double unlockTier2EnemyMult = 1.15;
    public double unlockTier3EnemyMult = 1.35;
    public double unlockTier4EnemyMult = 1.60;
    public double unlockTier5EnemyMult = 1.90;
    public double unlockTier6EnemyMult = 2.30;
    public double unlockTier7EnemyMult = 3.00;

    // ── V3 Ancient Coin economy ────────────────────────────────────────────
    public boolean enableAncientCoinDrops = true;
    public double ancientCoinDropMult = 1.0;
    public double ancientCoinRatingDivisor = 25_000.0;
    /** V3: reset active tier/level on player death (unlocks stay). */
    public boolean deathResetsActiveDifficulty = true;
    /** Minimum active unlock-tier for elites / mutations / full AI. */
    public int eliteMinUnlockTier = 4;
    public int mutationMinUnlockTier = 5;
    public int adaptiveAiMinUnlockTier = 3;
    public int enemyEvolutionMinUnlockTier = 2;
    public int bossMechanicsMinUnlockTier = 6;

    public boolean shouldRestoreVanillaFromPeaceful() {
        return restoreVanillaDifficultyFromPeaceful == null || restoreVanillaDifficultyFromPeaceful;
    }

    public long tierRequiredLevel(int tierId) {
        return switch (tierId) {
            case 1 -> Math.max(0L, unlockTier1Level);
            case 2 -> Math.max(0L, unlockTier2Level);
            case 3 -> Math.max(0L, unlockTier3Level);
            case 4 -> Math.max(0L, unlockTier4Level);
            case 5 -> Math.max(0L, unlockTier5Level);
            case 6 -> Math.max(0L, unlockTier6Level);
            case 7 -> Math.max(0L, unlockTier7Level);
            default -> Long.MAX_VALUE / 4L;
        };
    }

    public long tierMaxDifficulty(int tierId) {
        return switch (tierId) {
            case 1 -> Math.max(0L, unlockTier1Max);
            case 2 -> Math.max(0L, unlockTier2Max);
            case 3 -> Math.max(0L, unlockTier3Max);
            case 4 -> Math.max(0L, unlockTier4Max);
            case 5 -> Math.max(0L, unlockTier5Max);
            case 6 -> Math.max(0L, unlockTier6Max);
            case 7 -> Math.max(0L, unlockTier7Max);
            default -> 0L;
        };
    }

    public long tierActivationCost(int tierId) {
        return switch (tierId) {
            case 1 -> Math.max(0L, unlockTier1Cost);
            case 2 -> Math.max(0L, unlockTier2Cost);
            case 3 -> Math.max(0L, unlockTier3Cost);
            case 4 -> Math.max(0L, unlockTier4Cost);
            case 5 -> Math.max(0L, unlockTier5Cost);
            case 6 -> Math.max(0L, unlockTier6Cost);
            case 7 -> Math.max(0L, unlockTier7Cost);
            default -> Long.MAX_VALUE / 4L;
        };
    }

    public double tierEnemyMult(int tierId) {
        return switch (tierId) {
            case 1 -> Math.max(0.1, unlockTier1EnemyMult);
            case 2 -> Math.max(0.1, unlockTier2EnemyMult);
            case 3 -> Math.max(0.1, unlockTier3EnemyMult);
            case 4 -> Math.max(0.1, unlockTier4EnemyMult);
            case 5 -> Math.max(0.1, unlockTier5EnemyMult);
            case 6 -> Math.max(0.1, unlockTier6EnemyMult);
            case 7 -> Math.max(0.1, unlockTier7EnemyMult);
            default -> 1.0;
        };
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

    /** Master gate used by events / scaling / rewards / player purchases. */
    public static boolean isEnabled() {
        return INSTANCE != null && INSTANCE.enabled;
    }

    public static void setEnabled(boolean on) {
        if (INSTANCE == null) {
            INSTANCE = new DifficultyConfig();
        }
        INSTANCE.enabled = on;
        save();
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
        if (cfg.rewardScaling < 1.0) {
            cfg.rewardScaling = 2_500.0;
        }
        if (nearly(cfg.maxScaledHealth, 400.0) || nearly(cfg.maxScaledHealth, 1024.0)) {
            cfg.maxScaledHealth = 0.0;
        }
        if (cfg.maxDamageMultiplier < 0.0) {
            cfg.maxDamageMultiplier = 0.0;
        }
        if (cfg.maxArmorBonus < 0.0) {
            cfg.maxArmorBonus = 0.0;
        }
        if (cfg.areaDifficultyMode == null || cfg.areaDifficultyMode.isBlank()) {
            cfg.areaDifficultyMode = "weighted";
        }
        if (cfg.hardCapDifficulty < 0L || cfg.hardCapDifficulty == 1_000_000L) {
            cfg.hardCapDifficulty = 0L;
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
