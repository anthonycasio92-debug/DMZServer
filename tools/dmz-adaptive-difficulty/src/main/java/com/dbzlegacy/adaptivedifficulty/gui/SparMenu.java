package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player UI entrypoint for {@code /spar}.
 * Prefers CMI/CMILib inventory GUI, then Bukkit chest companion, then chat.
 */
public final class SparMenu {
    private SparMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;

        GuiBackend backend = GuiBackend.fromConfig();
        boolean opened = switch (backend) {
            case CMI -> openInventory(player, target);
            case CHEST -> BukkitGuiBridge.openSpar(player, target);
            case CHAT -> false;
            case AUTO -> openInventory(player, target);
        };

        if (!opened) {
            if (backend != GuiBackend.CHAT) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] spar guiBackend={} inventory open failed for {} — "
                                + "falling back to chat. Try /lm if this persists.",
                        AdaptiveDifficultyMod.MOD_ID,
                        backend.name().toLowerCase(),
                        player.m_6302_()
                );
            }
            SparChatMenu.open(player, target);
        }
    }

    /** CMI first, then plain chest — never skip the companion plugin for a CMILib check. */
    private static boolean openInventory(ServerPlayer player, String page) {
        return CmiGuiBridge.openSpar(player, page) || BukkitGuiBridge.openSpar(player, page);
    }
}
