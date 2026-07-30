package com.dbzlegacy.adaptivedifficulty.gui;

import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side clickable chat GUI (no client mod required).
 * Opened by {@code /difficulty}.
 */
public final class DifficultyMenu {
    private DifficultyMenu() {}

    public static void open(ServerPlayer player, String page) {
        DifficultyChatMenu.open(player, page);
    }
}
