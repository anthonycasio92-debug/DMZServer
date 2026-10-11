package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraGui;
import net.minecraft.server.level.ServerPlayer;

/** Player UI entrypoint for {@code /spar}. */
public final class SparMenu {
    private SparMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcUltraPreview.leave(player);
        GuiBackend backend = GuiBackend.fromConfig();
        if (backend == GuiBackend.ULTRA) {
            UltraGui.open(player, "spar", target);
            return;
        }
        if (backend == GuiBackend.CNPC) {
            com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.open(player, "spar", target);
            return;
        }
        SparChatMenu.open(player, target);
    }
}
