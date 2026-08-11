package com.dbzlegacy.adaptivedifficulty.world;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.storage.WorldData;

/**
 * Peaceful disables hostile mob spawns, so adaptive scaling never runs.
 * Restore a non-peaceful vanilla difficulty when configured.
 */
public final class VanillaDifficultyGuard {
    private VanillaDifficultyGuard() {}

    public static Difficulty current(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        WorldData data = server.m_129910_(); // getWorldData
        return data == null ? null : data.m_5472_(); // getDifficulty
    }

    public static boolean set(MinecraftServer server, Difficulty difficulty) {
        if (server == null || difficulty == null) {
            return false;
        }
        // setDifficulty(difficulty, forceUpdate)
        server.m_129827_(difficulty, true);
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] vanilla difficulty set to {}",
                AdaptiveDifficultyMod.MOD_ID,
                difficulty.m_19036_()
        );
        return true;
    }

    public static Difficulty parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Difficulty byName = Difficulty.m_19031_(raw.trim().toLowerCase());
        if (byName != null) {
            return byName;
        }
        return switch (raw.trim().toLowerCase()) {
            case "0", "p" -> Difficulty.PEACEFUL;
            case "1", "e" -> Difficulty.EASY;
            case "2", "n" -> Difficulty.NORMAL;
            case "3", "h" -> Difficulty.HARD;
            default -> null;
        };
    }

    /** If world is Peaceful and config asks for restore, set to configured target. */
    public static boolean restoreIfPeaceful(MinecraftServer server) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.shouldRestoreVanillaFromPeaceful() || server == null) {
            return false;
        }
        Difficulty now = current(server);
        if (now != Difficulty.PEACEFUL) {
            return false;
        }
        Difficulty target = parse(cfg.vanillaDifficulty);
        if (target == null || target == Difficulty.PEACEFUL) {
            target = Difficulty.HARD;
        }
        AdaptiveDifficultyMod.LOGGER.warn(
                "[{}] vanilla difficulty was PEACEFUL (hostile mobs disabled). Restoring to {} so adaptive scaling can run.",
                AdaptiveDifficultyMod.MOD_ID,
                target.m_19036_()
        );
        return set(server, target);
    }
}
