package com.dbzlegacy.adaptivedifficulty.net.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.gui.CharacterServicesMenu;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyChatMenu;
import com.dbzlegacy.adaptivedifficulty.gui.MechanicsChatMenu;
import com.dbzlegacy.adaptivedifficulty.gui.PrestigeMenu;
import com.dbzlegacy.adaptivedifficulty.gui.ProgressionChatMenu;
import com.dbzlegacy.adaptivedifficulty.gui.RivalChatMenu;
import com.dbzlegacy.adaptivedifficulty.gui.SparChatMenu;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import net.minecraft.server.level.ServerPlayer;

/**
 * Routes a native Ultra hub click to server menus. Does not open CustomNPCs.
 * Full Ultra screens for every subsystem can replace these chat fallbacks later.
 */
public final class NativeGuiActions {
    private NativeGuiActions() {}

    public static void dispatch(ServerPlayer player, String screenId, String actionId, String argsJson) {
        if (player == null) {
            return;
        }
        String screen = screenId == null ? "" : screenId.trim();
        String action = actionId == null ? "" : actionId.trim();
        String args = argsJson == null ? "{}" : argsJson.trim();
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] ultra gui action screen={} action={} args={}",
                AdaptiveDifficultyMod.MOD_ID,
                screen,
                action,
                args);
        if (!"hub".equalsIgnoreCase(screen) && !screen.isEmpty()) {
            // Future: nested Ultra screens send their own screen ids.
        }
        switch (action) {
            case "character" -> CharacterServicesMenu.open(player, "main");
            case "difficulty" -> DifficultyChatMenu.open(player, "main");
            case "progression" -> ProgressionChatMenu.open(player, "main");
            case "prestige" -> PrestigeMenu.open(player, "main");
            case "spar" -> SparChatMenu.open(player, "main");
            case "rival" -> RivalChatMenu.open(player, "main");
            case "hub" -> MechanicsChatMenu.open(player, "main");
            default -> DmzRewards.msg(player, "§7Unknown Ultra menu action.");
        }
    }
}
