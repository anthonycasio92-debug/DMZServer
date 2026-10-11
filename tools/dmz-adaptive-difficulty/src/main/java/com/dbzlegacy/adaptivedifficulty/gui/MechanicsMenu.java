package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.net.gui.LmGuiNetwork;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
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
        if (backend == GuiBackend.ULTRA) {
            if (LmGuiNetwork.sendOpen(player, "hub")) {
                return;
            }
            DmzRewards.msg(
                    player,
                    "§eUltra menu needs the LegacyMechanicsUltra client jar and DMZUltra. Opening chat hub.");
            MechanicsChatMenu.open(player, target);
            return;
        }
        if (backend == GuiBackend.CNPC) {
            com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.openHub(player, target);
            return;
        }
        MechanicsChatMenu.open(player, target);
    }
}
