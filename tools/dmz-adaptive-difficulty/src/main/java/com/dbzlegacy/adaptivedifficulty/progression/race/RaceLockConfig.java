package com.dbzlegacy.adaptivedifficulty.progression.race;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.ConfigPaths;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Race lock is retired. {@code race-lock.json} is no longer read.
 * A leftover file is deleted on load so it cannot be treated as a lock list.
 */
public final class RaceLockConfig {
    private RaceLockConfig() {}

    public static Path path() {
        return ConfigPaths.dataDir().resolve("race-lock.json");
    }

    public static boolean load() {
        try {
            DifficultyConfig cfg = DifficultyConfig.get();
            if (cfg != null && cfg.enableRaceLock) {
                cfg.enableRaceLock = false;
                DifficultyConfig.save();
            }
        } catch (Throwable ignored) {
        }
        Path file = path();
        try {
            if (Files.deleteIfExists(file)) {
                AdaptiveDifficultyMod.LOGGER.info(
                        "[{}] removed retired race-lock.json {}",
                        AdaptiveDifficultyMod.MOD_ID, file);
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    public static boolean reload() {
        return load();
    }
}
