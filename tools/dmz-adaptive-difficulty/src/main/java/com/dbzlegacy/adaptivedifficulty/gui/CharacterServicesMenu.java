package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesConfig;
import net.minecraft.server.level.ServerPlayer;

/** Player UI for Character Services ({@code /character} / hub). */
public final class CharacterServicesMenu {
    private CharacterServicesMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null || !CharacterServicesConfig.get().enabled) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcUltraPreview.leave(player);
        if (GuiBackend.fromConfig() == GuiBackend.CNPC) {
            com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.open(player, "character", target);
        }
    }
}
