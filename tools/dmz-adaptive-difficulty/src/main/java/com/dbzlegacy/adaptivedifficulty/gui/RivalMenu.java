package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraGui;
import net.minecraft.server.level.ServerPlayer;

/** Player UI entrypoint for {@code /rival}. */
public final class RivalMenu {
    private RivalMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcUltraPreview.leave(player);
        GuiBackend backend = GuiBackend.fromConfig();
        if (backend == GuiBackend.ULTRA) {
            UltraGui.open(player, "rival", target);
            return;
        }
        if (backend == GuiBackend.CNPC) {
            com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.open(player, "rival", target);
            return;
        }
        RivalChatMenu.open(player, target);
    }
}
