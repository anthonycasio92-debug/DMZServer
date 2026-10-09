package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.server.level.ServerPlayer;

/** {@code /progression} — staff tools + player android remove. */
public final class ProgressionMenu {
    private ProgressionMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcUltraPreview.leave(player);
        boolean androidRemove = isAndroidRemovePage(target);
        if (!androidRemove && !StaffAccess.isStaff(player)) {
            DmzRewards.msg(player, "§cStaff only.");
            return;
        }
        if (!DifficultyConfig.get().enableProgression && !androidRemove) {
            ProgressionChatMenu.open(player, page);
            return;
        }

        GuiBackend backend = GuiBackend.fromConfig();
        if (backend == GuiBackend.CNPC) {
            if (androidRemove) {
                com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.open(player, "android_remove", "main");
            } else {
                com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.open(player, "progression", target);
            }
            return;
        }
        if (androidRemove) {
            DmzRewards.msg(player, "§cAndroid remove needs CNPC GUI (guiBackend=cnpc).");
            return;
        }
        ProgressionChatMenu.open(player, target);
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
}
