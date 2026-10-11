package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesConfig;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import net.minecraft.server.level.ServerPlayer;

/** Player UI for Character Services ({@code /character} / hub). */
public final class CharacterServicesMenu {
    private CharacterServicesMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null || !CharacterServicesConfig.get().enabled) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        GuiBackend backend = GuiBackend.fromConfig();
        if (backend == GuiBackend.CNPC) {
            com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.open(player, "character", target);
            return;
        }
        // Ultra / chat: character screens are not ported to native Ultra yet.
        DmzRewards.msg(
                player,
                "§eCharacter Services: use §f/character §eor set §fguiBackend=cnpc §efor the full menu.");
        MechanicsChatMenu.open(player, "character");
    }
}
