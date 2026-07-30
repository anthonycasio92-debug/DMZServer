package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player UI entrypoint for {@code /difficulty}.
 * Prefers DeluxeMenus, then the Bukkit chest companion, then clickable chat.
 * No client mod is required.
 */
public final class DifficultyMenu {
    private DifficultyMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        // Admin settings page stays chat-based (editable keys).
        if ("settings".equalsIgnoreCase(target)) {
            DifficultyChatMenu.open(player, "settings");
            return;
        }

        GuiBackend backend = GuiBackend.fromConfig();
        boolean opened = switch (backend) {
            case DELUXEMENUS -> DeluxeMenusBridge.open(player, target);
            case CHEST -> BukkitGuiBridge.open(player, target);
            case CHAT -> false;
            case AUTO -> {
                if (DeluxeMenusBridge.open(player, target)) {
                    yield true;
                }
                if (BukkitGuiBridge.open(player, target)) {
                    yield true;
                }
                yield false;
            }
        };

        if (!opened) {
            if (backend == GuiBackend.DELUXEMENUS || backend == GuiBackend.CHEST) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] guiBackend={} unavailable; falling back to chat GUI",
                        AdaptiveDifficultyMod.MOD_ID, backend.name().toLowerCase()
                );
            }
            DifficultyChatMenu.open(player, target);
        }
    }
}
