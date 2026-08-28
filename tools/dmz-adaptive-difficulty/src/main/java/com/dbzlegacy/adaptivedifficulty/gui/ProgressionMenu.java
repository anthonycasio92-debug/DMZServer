package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.server.level.ServerPlayer;

/**
 * Staff UI entrypoint for {@code /progression} / {@code /prog}.
 * Prefers CMI/CMILib inventory GUI, then Bukkit chest companion, then chat.
 */
public final class ProgressionMenu {
    private ProgressionMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        if (!StaffAccess.isStaff(player)) {
            DmzRewards.msg(player, "§cStaff only.");
            return;
        }
        if (!DifficultyConfig.get().enableProgression) {
            ProgressionChatMenu.open(player, page);
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;

        GuiBackend backend = GuiBackend.fromConfig();
        boolean opened = switch (backend) {
            case CMI -> openInventory(player, target);
            case CHEST -> BukkitGuiBridge.openProgression(player, target);
            case CHAT -> false;
            case AUTO -> openInventory(player, target);
        };

        if (!opened) {
            if (backend != GuiBackend.CHAT) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] progression guiBackend={} inventory open failed for {} — "
                                + "falling back to chat. Try /lm if this persists.",
                        AdaptiveDifficultyMod.MOD_ID,
                        backend.name().toLowerCase(),
                        player.m_6302_()
                );
            }
            ProgressionChatMenu.open(player, target);
        }
    }

    private static boolean openInventory(ServerPlayer player, String page) {
        return CmiGuiBridge.openProgression(player, page) || BukkitGuiBridge.openProgression(player, page);
    }
}
