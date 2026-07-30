package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player UI entrypoint for {@code /difficulty}.
 * Prefers CMI/CMILib inventory GUI, then chest companion, then chat.
 * DeluxeMenus is optional/legacy only.
 */
public final class DifficultyMenu {
    private DifficultyMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        if ("settings".equalsIgnoreCase(target)) {
            DifficultyChatMenu.open(player, "settings");
            return;
        }

        GuiBackend backend = GuiBackend.fromConfig();
        boolean opened = switch (backend) {
            case CMI -> CmiGuiBridge.open(player, target);
            case DELUXEMENUS -> DeluxeMenusBridge.open(player, target);
            case CHEST -> BukkitGuiBridge.open(player, target);
            case CHAT -> false;
            case AUTO -> {
                if (CmiGuiBridge.open(player, target)) {
                    yield true;
                }
                if (BukkitGuiBridge.open(player, target)) {
                    yield true;
                }
                // DeluxeMenus last — dispatchCommand often returns true even when menu fails.
                if (DeluxeMenusBridge.open(player, target)) {
                    yield true;
                }
                yield false;
            }
        };

        if (!opened) {
            if (backend != GuiBackend.AUTO && backend != GuiBackend.CHAT) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] guiBackend={} unavailable; falling back to chat GUI",
                        AdaptiveDifficultyMod.MOD_ID, backend.name().toLowerCase()
                );
            }
            DifficultyChatMenu.open(player, target);
        }
    }
}
