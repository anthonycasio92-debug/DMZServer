package com.dbzlegacy.adaptivedifficulty.gui;

import net.minecraft.server.level.ServerPlayer;

/** Player UI entrypoint for {@code /prestige}. */
public final class PrestigeMenu {
    private PrestigeMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcUltraPreview.leave(player);
        if (GuiBackend.fromConfig() == GuiBackend.CNPC) {
            com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.open(player, "prestige", target);
        }
    }
}
