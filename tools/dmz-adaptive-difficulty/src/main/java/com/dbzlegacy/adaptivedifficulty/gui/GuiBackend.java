package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;

/** Which inventory/chat UI `/difficulty` should prefer. */
public enum GuiBackend {
    AUTO,
    DELUXEMENUS,
    CHEST,
    CHAT;

    public static GuiBackend fromConfig() {
        String raw = DifficultyConfig.get().guiBackend;
        if (raw == null || raw.isBlank()) {
            return AUTO;
        }
        return switch (raw.trim().toLowerCase()) {
            case "deluxemenus", "deluxe", "dm" -> DELUXEMENUS;
            case "chest", "bukkit", "inventory", "gui" -> CHEST;
            case "chat" -> CHAT;
            default -> AUTO;
        };
    }
}
