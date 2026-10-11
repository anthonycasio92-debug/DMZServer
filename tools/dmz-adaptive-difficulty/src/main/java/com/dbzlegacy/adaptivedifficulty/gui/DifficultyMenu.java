package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.server.level.ServerPlayer;

/** Player UI entrypoint for {@code /difficulty} — CustomNPCs, then chat fallback. */
public final class DifficultyMenu {
    private DifficultyMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        if ("settings".equalsIgnoreCase(target)) {
            if (!StaffAccess.isStaff(player)) {
                target = "main";
            } else {
                DifficultyChatMenu.open(player, "settings");
                return;
            }
        }

        GuiBackend backend = GuiBackend.fromConfig();
        if (backend == GuiBackend.CNPC) {
            com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.open(player, "difficulty", target);
            return;
        }
        // ultra / chat
        DifficultyChatMenu.open(player, target);
    }
}
