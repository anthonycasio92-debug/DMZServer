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
        String target = page == null || page.isBlank() ? "main" : page;
        boolean androidRemove = isAndroidRemovePage(target);
        if (!androidRemove && !StaffAccess.isStaff(player)) {
            DmzRewards.msg(player, "§cStaff only.");
            return;
        }
        if (!DifficultyConfig.get().enableProgression) {
            if (androidRemove) {
                DmzRewards.msg(player, "§cProgression system is disabled.");
                return;
            }
            ProgressionChatMenu.open(player, page);
            return;
        }

        GuiBackend backend = GuiBackend.fromConfig();
        // Android remove is player-facing inventory — skip staff-only chat menu.
        if (androidRemove && backend == GuiBackend.CHAT) {
            boolean opened = openInventory(player, "android_remove");
            if (!opened) {
                DmzRewards.msg(player, "§cAndroid remove GUI unavailable. Use §f/progression android remove§c.");
            }
            return;
        }
        boolean opened = switch (backend) {
            case CNPC -> {
                if (androidRemove) {
                    com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.open(player, "android_remove", "main");
                } else {
                    com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.open(player, "progression", target);
                }
                yield true;
            }
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
            if (androidRemove) {
                DmzRewards.msg(player, "§cAndroid remove GUI unavailable. Use §f/progression android remove§c.");
            } else {
                ProgressionChatMenu.open(player, target);
            }
        }
    }

    private static boolean isAndroidRemovePage(String page) {
        if (page == null) {
            return false;
        }
        return switch (page.toLowerCase()) {
            case "android_remove", "androidremove", "remove_android", "deandroid" -> true;
            default -> false;
        };
    }

    private static boolean openInventory(ServerPlayer player, String page) {
        return CmiGuiBridge.openProgression(player, page) || BukkitGuiBridge.openProgression(player, page);
    }
}
