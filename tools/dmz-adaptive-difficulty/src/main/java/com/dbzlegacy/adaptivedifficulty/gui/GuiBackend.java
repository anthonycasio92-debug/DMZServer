package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;

/** Which player UI {@code /lm} and subcommands use. Forge-only servers should use {@link #CNPC}. */
public enum GuiBackend {
    CHAT,
    CNPC;

    public static GuiBackend fromConfig() {
        String raw = DifficultyConfig.get().guiBackend;
        if (raw == null || raw.isBlank()) {
            return CNPC;
        }
        return switch (raw.trim().toLowerCase()) {
            case "cnpc", "customnpcs", "customnpc", "noppes" -> CNPC;
            case "chat" -> CHAT;
            // Legacy chest/CMI/auto values → CNPC (LegacyMechanicsGUI plugin no longer required).
            case "cmi", "cmilib", "cmigui", "deluxemenus", "deluxe", "dm",
                    "chest", "bukkit", "inventory", "gui", "auto" -> CNPC;
            default -> CNPC;
        };
    }
}
