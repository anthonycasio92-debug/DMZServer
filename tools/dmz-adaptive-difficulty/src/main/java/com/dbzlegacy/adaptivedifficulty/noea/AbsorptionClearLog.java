package com.dbzlegacy.adaptivedifficulty.noea;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;

/**
 * Logs a wipe failure through the mod logger so it respects log levels.
 *
 * <p>This class stays outside the mixin package. The dedicated-server
 * classloader does not resolve ordinary classes that live next to mixins.
 */
public final class AbsorptionClearLog {
    private AbsorptionClearLog() {}

    public static void failure(Throwable thrown) {
        AdaptiveDifficultyMod.LOGGER.warn(
                "[{}] Majin/GodKi clear() threw: {}", AdaptiveDifficultyMod.MOD_ID, thrown);
        Throwable cause = thrown;
        int depth = 0;
        while (cause != null && depth < 6) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] clear() cause[{}]: {}", AdaptiveDifficultyMod.MOD_ID, depth, cause.toString());
            StackTraceElement[] frames = cause.getStackTrace();
            int limit = frames == null ? 0 : Math.min(frames.length, 24);
            for (int i = 0; i < limit; i++) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}]   at {}", AdaptiveDifficultyMod.MOD_ID, frames[i]);
            }
            Throwable next = cause.getCause();
            if (next == cause) {
                break;
            }
            cause = next;
            depth++;
        }
        if (thrown != null) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] clear() stack", AdaptiveDifficultyMod.MOD_ID, thrown);
        }
    }
}
