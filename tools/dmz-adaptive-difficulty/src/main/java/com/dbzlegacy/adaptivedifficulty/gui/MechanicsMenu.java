package com.dbzlegacy.adaptivedifficulty.gui;

import net.minecraft.server.level.ServerPlayer;

/** Player UI entrypoint for {@code /lm} / {@code /legacymechanics}. */
public final class MechanicsMenu {
    private MechanicsMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;

        GuiBackend backend = GuiBackend.fromConfig();
        if (backend == GuiBackend.CNPC) {
            com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.openHub(player, target);
            return;
        }
        MechanicsChatMenu.open(player, target);
    }
}
