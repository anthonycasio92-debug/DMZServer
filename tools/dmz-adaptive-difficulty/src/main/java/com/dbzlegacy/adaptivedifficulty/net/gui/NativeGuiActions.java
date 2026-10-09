package com.dbzlegacy.adaptivedifficulty.net.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.server.level.ServerPlayer;

/** Routes a native hub click to the same CNPC open calls the hub buttons already use. */
public final class NativeGuiActions {
    private NativeGuiActions() {}

    public static void dispatch(ServerPlayer player, String screenId, String actionId, String argsJson) {
        if (player == null || !StaffAccess.isStaff(player)) {
            return;
        }
        String screen = screenId == null ? "" : screenId.trim();
        String action = actionId == null ? "" : actionId.trim();
        String args = argsJson == null ? "{}" : argsJson.trim();
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] native gui action screen={} action={} args={}",
                AdaptiveDifficultyMod.MOD_ID,
                screen,
                action,
                args);
        String system = switch (action) {
            case "character" -> "character";
            case "difficulty" -> "difficulty";
            case "progression" -> "progression";
            case "prestige" -> "prestige";
            case "spar" -> "spar";
            case "rival" -> "rival";
            default -> "";
        };
        if (system.isEmpty()) {
            return;
        }
        CnpcLmGui.open(player, system, "main");
    }
}
