package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;

/** Which inventory/chat UI {@code /difficulty} should prefer. */
public enum GuiBackend {
    AUTO,
    CMI,
    CHEST,
    CHAT,
    CNPC;

    public static GuiBackend fromConfig() {
        String raw = DifficultyConfig.get().guiBackend;
        if (raw == null || raw.isBlank()) {
            return CNPC;
        }
        return switch (raw.trim().toLowerCase()) {
            case "cnpc", "customnpcs", "customnpc", "noppes" -> CNPC;
            case "cmi", "cmilib", "cmigui" -> CMI;
            case "chest", "bukkit", "inventory", "gui" -> CHEST;
            case "chat" -> CHAT;
            // Legacy DeluxeMenus configs fall back to CMI
            case "deluxemenus", "deluxe", "dm" -> CMI;
            default -> AUTO;
        };
    }
}
