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
            case CMI -> openInventory(player, target);
            case CHEST -> BukkitGuiBridge.open(player, target);
            case CHAT -> false;
            case AUTO -> openInventory(player, target);
        };

        if (!opened) {
            // Companion plugin is up but open failed (Mohist player resolve flicker) —
            // do not dump to chat when inventory was requested; log and stop.
            if (backend != GuiBackend.CHAT
                    && (CmiGuiBridge.available() || BukkitGuiBridge.available())) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] guiBackend={} open failed for {} (companion present) — "
                                + "not falling back to chat. Try /dmzdiffgui.",
                        AdaptiveDifficultyMod.MOD_ID,
                        backend.name().toLowerCase(),
                        player.m_6302_()
                );
                return;
            }
            if (backend != GuiBackend.AUTO && backend != GuiBackend.CHAT) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] guiBackend={} unavailable; falling back to chat GUI "
                                + "(is plugins/dmz_adaptive_difficulty_gui-*.jar enabled?)",
                        AdaptiveDifficultyMod.MOD_ID, backend.name().toLowerCase()
                );
            }
            DifficultyChatMenu.open(player, target);
        }
    }

    /** CMI first, then plain chest — never skip the companion plugin for a CMILib check. */
    private static boolean openInventory(ServerPlayer player, String page) {
        return CmiGuiBridge.open(player, page) || BukkitGuiBridge.open(player, page);
    }
}
