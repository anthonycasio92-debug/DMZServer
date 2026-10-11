package com.dbzlegacy.adaptivedifficulty.gui;

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

    public static GuiBackend fromConfig() {
        String raw = DifficultyConfig.get().guiBackend;
        if (raw == null || raw.isBlank()) {
            return ULTRA;
        }
        return switch (raw.trim().toLowerCase()) {
            case "ultra", "kaipath", "dmzultra", "native" -> ULTRA;
            case "cnpc", "customnpcs", "customnpc", "noppes" -> CNPC;
            case "chat" -> CHAT;
            // Legacy chest/CMI/auto values → CNPC (LegacyMechanicsGUI plugin no longer required).
            case "cmi", "cmilib", "cmigui", "deluxemenus", "deluxe", "dm",
                    "chest", "bukkit", "inventory", "gui", "auto" -> CNPC;
            default -> ULTRA;
        };
    }
}
