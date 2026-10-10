package com.dbzlegacy.adaptivedifficulty.quest;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.ConfigPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {@code config/legacymechanics/saga-reset.json}.
 * A saga id missing from {@link #costs} uses {@link #defaultBaseCost}, including datapack sagas
 * added after this file was written.
 */
public final class SagaResetConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile SagaResetConfig INSTANCE = new SagaResetConfig();

    public boolean enabled = true;
    /** Copper-value Ancient Coin cost used when {@link #costs} has no entry for that saga id. */
    public long defaultBaseCost = 100_000L;
    /** Optional per-saga overrides, keyed by the saga id from {@code QuestRegistry}. */
    public Map<String, Long> costs = new LinkedHashMap<>();

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
            try (Reader reader = Files.newBufferedReader(file)) {
                SagaResetConfig parsed = GSON.fromJson(reader, SagaResetConfig.class);
                INSTANCE = sanitize(parsed == null ? new SagaResetConfig() : parsed);
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] saga-reset config load failed: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
            INSTANCE = new SagaResetConfig();
        }
    }

    public long costFor(String sagaId) {
        if (sagaId != null && costs != null) {
            Long direct = costs.get(sagaId);
            if (direct == null) {
                for (Map.Entry<String, Long> entry : costs.entrySet()) {
                    if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(sagaId)) {
                        direct = entry.getValue();
                        break;
                    }
                }
            }
            if (direct != null) {
                return Math.max(0L, direct);
            }
        }
        return Math.max(0L, defaultBaseCost);
    }

    private static SagaResetConfig sanitize(SagaResetConfig config) {
        if (config.costs == null) {
            config.costs = new LinkedHashMap<>();
        }
        Map<String, Long> clean = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : config.costs.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            clean.put(entry.getKey().trim().toLowerCase(Locale.ROOT), Math.max(0L, entry.getValue()));
        }
        config.costs = clean;
        config.defaultBaseCost = Math.max(0L, config.defaultBaseCost);
        return config;
    }
}
