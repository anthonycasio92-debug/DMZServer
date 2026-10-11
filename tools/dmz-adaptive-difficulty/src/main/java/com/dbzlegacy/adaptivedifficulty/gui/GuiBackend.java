package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;

/**
 * Which player UI {@code /lm} and subcommands use.
 * {@link #ULTRA} sends {@code legacymechanics:gui} packets for KaiPath clients;
 * {@link #CNPC} keeps CustomNPCs dialogs as fallback.
 */
public enum GuiBackend {
    CHAT,
    CNPC,
    ULTRA;

    /** Default when unset or unrecognized. */
    public static final GuiBackend DEFAULT = ULTRA;

    public static GuiBackend fromConfig() {
        String raw = DifficultyConfig.get().guiBackend;
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String key = raw.trim().toLowerCase();
        return switch (key) {
            case "ultra", "kaipath", "dmzultra", "native" -> ULTRA;
            case "cnpc", "customnpcs", "customnpc", "noppes" -> CNPC;
            case "chat" -> CHAT;
            default -> {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] unrecognized guiBackend \"{}\"; falling back to {}",
                        AdaptiveDifficultyMod.MOD_ID,
                        raw.trim(),
                        DEFAULT.name().toLowerCase());
                yield DEFAULT;
            }
        };
    }
}
