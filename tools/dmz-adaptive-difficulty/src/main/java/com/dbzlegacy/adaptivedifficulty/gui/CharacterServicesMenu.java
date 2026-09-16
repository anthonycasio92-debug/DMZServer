package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
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
        GuiBackend backend = GuiBackend.fromConfig();
        boolean opened = switch (backend) {
            case CMI -> CmiGuiBridge.openCharacterServices(player, target);
            case CHEST -> BukkitGuiBridge.openCharacterServices(player, target);
            case CHAT -> false;
            case AUTO -> CmiGuiBridge.openCharacterServices(player, target)
                    || BukkitGuiBridge.openCharacterServices(player, target);
        };
        if (!opened) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] character services GUI open failed for {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    player.m_6302_());
        }
    }
}
