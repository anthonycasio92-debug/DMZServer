package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player UI entrypoint for {@code /difficulty}.
 * Prefers CMI/CMILib inventory GUI, then Bukkit chest companion, then chat.
 */
public final class DifficultyMenu {
    private DifficultyMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        if ("settings".equalsIgnoreCase(target)) {
            if (!StaffAccess.isStaff(player)) {
                target = "main";
            } else {
                DifficultyChatMenu.open(player, "settings");
                return;
            }
        }

        GuiBackend backend = GuiBackend.fromConfig();
        boolean opened = switch (backend) {
            case CMI -> CmiGuiBridge.open(player, target);
            case CHEST -> BukkitGuiBridge.open(player, target);
            case CHAT -> false;
            case AUTO -> CmiGuiBridge.open(player, target) || BukkitGuiBridge.open(player, target);
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
