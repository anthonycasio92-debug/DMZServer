package com.dbzlegacy.adaptivedifficulty.quest;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.ConfigPaths;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code config/legacymechanics/saga-reset.json}.
 * Charge is {@code base + (level × perLevelCost)} Ancient Coins (copper units).
 * A saga id missing from {@link #baseCosts} uses {@link #defaultBaseCost}, including datapack sagas
 * added after this file was written.
 */
public final class SagaResetConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile SagaResetConfig INSTANCE = new SagaResetConfig();

    public boolean enabled = true;
    /** Added to the base once per Dragon Mine Z level ({@link PlayerDifficultyData#getHighestDmzLevel()}). */
    public long perLevelCost = 10L;
    /** Base used when {@link #baseCosts} has no entry for that saga id. */
    public long defaultBaseCost = 500L;
    /** Per-saga base costs, keyed by the saga id from {@code QuestRegistry}. */
    public Map<String, Long> baseCosts = defaultBaseCosts();

    public SagaResetConfig() {}

    public static SagaResetConfig get() {
        return INSTANCE;
    }

    public static Path path() {
        return ConfigPaths.dataDir().resolve("saga-reset.json");
    }

    public static void load() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            if (!Files.isRegularFile(file)) {
                SagaResetConfig fresh = new SagaResetConfig();
                try (Writer writer = Files.newBufferedWriter(file)) {
                    GSON.toJson(fresh, writer);
                }
            }
            SagaResetConfig parsed;
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonElement element = JsonParser.parseReader(reader);
                parsed = read(element);
            }
            INSTANCE = sanitize(parsed);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] saga-reset config load failed: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
            INSTANCE = new SagaResetConfig();
        }
    }

    /** Writes the live config, including a saga cost added from the staff menu. */
    public static boolean save() {
        INSTANCE = sanitize(INSTANCE == null ? new SagaResetConfig() : INSTANCE);
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(INSTANCE, writer);
            }
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] saga-reset config save failed: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
            return false;
        }
    }

    /** Base only. Unknown ids use {@link #defaultBaseCost}. */
    public long baseFor(String sagaId) {
        Long direct = lookup(baseCosts, sagaId);
        if (direct != null) {
            return direct;
        }
        return Math.max(0L, defaultBaseCost);
    }

    /**
     * Ancient Coins (copper units) for this player: {@code base + level × perLevelCost}.
     * Level is {@link PlayerDifficultyData#getHighestDmzLevel()}, a {@code long} with no arguments.
     */
    public long costFor(ServerPlayer player, String sagaId) {
        long base = baseFor(sagaId);
        long per = Math.max(0L, perLevelCost);
        long level = highestDmzLevel(player);
        return saturatingAdd(base, saturatingMul(per, level));
    }

    /** High-water Dragon Mine Z level. The shipped coin class does not expose this. */
    private static long highestDmzLevel(ServerPlayer player) {
        if (player == null) {
            return 0L;
        }
        try {
            return Math.max(0L, DifficultyCache.data(player).getHighestDmzLevel());
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    /** @deprecated flat lookup kept so older callers compile; level scaling is {@link #costFor(ServerPlayer, String)}. */
    public long costFor(String sagaId) {
        return baseFor(sagaId);
    }

    private static SagaResetConfig read(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return new SagaResetConfig();
        }
        JsonObject obj = element.getAsJsonObject();
        boolean legacyShape = !obj.has("perLevelCost") && !obj.has("baseCosts");
        SagaResetConfig parsed = GSON.fromJson(obj, SagaResetConfig.class);
        if (parsed == null) {
            parsed = new SagaResetConfig();
        }
        Map<String, Long> legacyCosts = mapFrom(obj.get("costs"));
        if (!obj.has("baseCosts") && legacyCosts != null && !legacyCosts.isEmpty()) {
            if (parsed.baseCosts == null) {
                parsed.baseCosts = new LinkedHashMap<>();
            }
            parsed.baseCosts.putAll(legacyCosts);
        } else if (obj.has("baseCosts") && legacyCosts != null) {
            if (parsed.baseCosts == null) {
                parsed.baseCosts = new LinkedHashMap<>();
            }
            for (Map.Entry<String, Long> entry : legacyCosts.entrySet()) {
                parsed.baseCosts.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        // The 4.6.63 file shipped defaultBaseCost 100000 as a flat copper price.
        // An untouched file picks up the level-scaled bases. A hand-edited default stays.
        if (legacyShape
                && parsed.defaultBaseCost == 100_000L
                && (legacyCosts == null || legacyCosts.isEmpty())) {
            parsed.defaultBaseCost = 500L;
            parsed.perLevelCost = 10L;
            parsed.baseCosts = defaultBaseCosts();
        }
        return parsed;
    }

    private static Map<String, Long> mapFrom(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        Map<String, Long> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || !entry.getValue().isJsonPrimitive()) {
                continue;
            }
            try {
                map.put(entry.getKey(), entry.getValue().getAsLong());
            } catch (RuntimeException ignored) {
            }
        }
        return map;
    }

    private static Long lookup(Map<String, Long> costs, String sagaId) {
        if (sagaId == null || costs == null || costs.isEmpty()) {
            return null;
        }
        Long direct = costs.get(sagaId);
        if (direct != null) {
            return Math.max(0L, direct);
        }
        for (Map.Entry<String, Long> entry : costs.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(sagaId) && entry.getValue() != null) {
                return Math.max(0L, entry.getValue());
            }
        }
        return null;
    }

    private static SagaResetConfig sanitize(SagaResetConfig config) {
        if (config.baseCosts == null) {
            config.baseCosts = defaultBaseCosts();
        }
        Map<String, Long> clean = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : config.baseCosts.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            clean.put(entry.getKey().trim().toLowerCase(Locale.ROOT), Math.max(0L, entry.getValue()));
        }
        config.baseCosts = clean;
        config.defaultBaseCost = Math.max(0L, config.defaultBaseCost);
        config.perLevelCost = Math.max(0L, config.perLevelCost);
        return config;
    }

    static Map<String, Long> defaultBaseCosts() {
        Map<String, Long> costs = new LinkedHashMap<>();
        costs.put("saiyan", 100L);
        costs.put("frieza", 250L);
        costs.put("android", 500L);
        costs.put("buu", 1000L);
        costs.put("future", 1500L);
        costs.put("movies", 2000L);
        costs.put("dragon_balls", 750L);
        return costs;
    }

    private static long saturatingMul(long a, long b) {
        if (a <= 0L || b <= 0L) {
            return 0L;
        }
        if (a > Long.MAX_VALUE / b) {
            return Long.MAX_VALUE;
        }
        return a * b;
    }

    private static long saturatingAdd(long a, long b) {
        if (b > Long.MAX_VALUE - a) {
            return Long.MAX_VALUE;
        }
        return a + b;
    }
}
