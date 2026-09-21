package com.dbzlegacy.adaptivedifficulty.gui;

import net.minecraft.server.level.ServerPlayer;

/** Player UI entrypoint for {@code /spar}. */
public final class SparMenu {
    private SparMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        if (GuiBackend.fromConfig() == GuiBackend.CNPC) {
            com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.open(player, "spar", target);
            return;
        }
        SparChatMenu.open(player, target);
    }
}
