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
    public long baseCost = 100_000L;
    public long costScaling = 1_000L;
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
    public long hardCapDifficulty = 1_000_000L;
    /**
     * {@code lightmans} (preferred when mod present), {@code training_points}, or {@code free}.
     */
    public String purchaseCurrency = "lightmans";
    public boolean scaleHostileOnly = true;
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
    public double bossHealthThreshold = 100.0;
    public List<String> bossIdContains = new ArrayList<>(Arrays.asList(
            "boss", "warden", "wither", "ender_dragon", "raid"
    ));
    /** Admin permission node (Forge PermissionAPI / LuckPerms). Ops always allowed. */
    public String adminPermission = "difficulty.admin";

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
                        if (loaded.bossIdContains == null) {
                            loaded.bossIdContains = new ArrayList<>();
                        }
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

    public static void save() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] failed to save config: {}", AdaptiveDifficultyMod.MOD_ID, e.toString());
        }
    }

    public static void reload() {
        load();
    }
}
