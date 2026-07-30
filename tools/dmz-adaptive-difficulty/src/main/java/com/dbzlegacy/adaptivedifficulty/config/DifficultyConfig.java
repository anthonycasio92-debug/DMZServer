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
    public double rewardScaling = 1_000.0;
    public double healthPercentPerDifficulty = 1.0;
    public double damagePercentPerDifficulty = 1.0;
    public double defensePercentPerDifficulty = 3.0;
    public double movementPercentPer100Difficulty = 0.1;
    public double dmzExtraHealthPercent = 1.0;
    public double dmzExtraDamagePercent = 1.0;
    public double dmzExtraDefensePercent = 3.0;
    public double dmzExtraKiDamagePercent = 1.0;
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
    /** Caps so high difficulty cannot explode attributes / break spawns. */
    public double maxHealthMultiplier = 50.0;
    public double maxScaledHealth = 1024.0;
    public double maxMoveMultiplier = 1.75;
    public double maxArmorBonus = 20.0;
    public double maxDamageMultiplier = 50.0;
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

    public static void save() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(INSTANCE, writer);
            }
            com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier.invalidateThresholdCache();
            com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty.clearCache();
        } catch (IOException e) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] failed to save config: {}", AdaptiveDifficultyMod.MOD_ID, e.toString());
        }
    }

    public static void reload() {
        load();
        com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier.invalidateThresholdCache();
        com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty.clearCache();
    }
}
