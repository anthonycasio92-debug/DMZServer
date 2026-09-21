package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.server.level.ServerPlayer;

/** {@code /skills} (staff) and Skill Check CNPC entry. */
public final class SkillsMenu {
    private SkillsMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        if (!StaffAccess.isStaff(player)) {
            DmzRewards.msg(player, "§cStaff only.");
            return;
        }
        com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService.clearSession(player);
        openInternal(player, page, false);
    }

    public static void openSkillCheck(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        openInternal(player, page, true);
    }

    private static void openInternal(ServerPlayer player, String page, boolean skillCheck) {
        if (!DifficultyConfig.get().enableSkillUnlockService) {
            return;
        }
        if (skillCheck && !DifficultyConfig.get().enableSkillCheck) {
            return;
        }
        String target = page == null || page.isBlank() ? "core" : page;
        if (GuiBackend.fromConfig() == GuiBackend.CNPC) {
            if (skillCheck) {
                com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.open(player, "skillcheck", target);
            } else {
                com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.open(player, "skills", target);
            }
            return;
        }
        com.dbzlegacy.adaptivedifficulty.progression.shop.SkillUnlockService.open(
                player, target, skillCheck);
    }
}
