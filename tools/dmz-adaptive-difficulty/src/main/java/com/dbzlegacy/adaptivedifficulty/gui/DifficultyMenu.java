package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import net.minecraft.server.level.ServerPlayer;

/**
 * Opens the real Adaptive Difficulty screen (S2C packet).
 * Chat fallback remains available via {@code /difficulty chat}.
 */
public final class DifficultyMenu {
    private DifficultyMenu() {}

    public static void open(ServerPlayer player, String page) {
        DifficultyActions.openGui(player, page);
    }
}
