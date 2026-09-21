package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.gui.AdminInspectSessions;
import com.dbzlegacy.adaptivedifficulty.gui.MechanicsGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

public final class CnpcLmHubGui {
    private CnpcLmHubGui() {}

    public static void open(ServerPlayer player, String page) {
        if ("logs".equalsIgnoreCase(page) || "syslog".equalsIgnoreCase(page)) {
            if (StaffAccess.isStaff(player)) {
                CnpcLmLogsGui.open(player, "main");
            } else {
                paintMain(player);
            }
            return;
        }
        paintMain(player);
    }

    private static void paintMain(ServerPlayer player) {
        CnpcGuiSupport.show(player, CnpcLmGui.ID_HUB, (p, gui) -> paintMain(p, gui));
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = CnpcGuiSupport.target(player);
        Map<String, String> ph = MechanicsGuiApi.placeholders(who);
        CnpcGuiSupport.title(gui, 1, "§fLegacy Mechanics");
        CnpcGuiSupport.subtitle(gui, 2, "§7Your hub for difficulty, rivals, sparring & more");
        CnpcGuiSupport.inspectBanner(player, gui);
        CnpcGuiSupport.divider(gui, 3, AdminInspectSessions.isInspecting(player.m_20148_()) ? 52 : 38);

        List<String> lines = new ArrayList<>(MechanicsGuiApi.linesForPage(who, "main"));
        CnpcGuiSupport.bodyLines(gui, 10, 44, lines, 4);

        int row = 88;
        int gap = 24;
        if ("true".equals(ph.get("bridge_ok"))) {
            CnpcGuiSupport.button(gui, 20, "§aDifficulty", CnpcGuiSupport.COL_L, row, () -> CnpcLmGui.open(player, "difficulty", "main"));
            CnpcGuiSupport.button(gui, 21, "§6Rival", CnpcGuiSupport.COL_R, row, () -> CnpcLmGui.open(player, "rival", "main"));
            row += gap;
            CnpcGuiSupport.button(gui, 22, "§bSparring", CnpcGuiSupport.COL_L, row, () -> CnpcLmGui.open(player, "spar", "main"));
            if (DifficultyConfig.get().enablePrestigeSystem) {
                CnpcGuiSupport.button(gui, 23, "§dPrestige", CnpcGuiSupport.COL_R, row, () -> CnpcLmGui.open(player, "prestige", "main"));
            }
            row += gap;
            if (SkillCheckService.canUse(player)) {
                CnpcGuiSupport.button(gui, 24, "§eSkill Check", CnpcGuiSupport.COL_L, row, () -> CnpcLmGui.open(player, "skillcheck", "main"));
            } else if (StaffAccess.isStaff(player)) {
                CnpcGuiSupport.button(gui, 24, "§eSkills §8(staff)", CnpcGuiSupport.COL_L, row, () -> CnpcLmGui.open(player, "skills", "core"));
            }
            CnpcGuiSupport.button(gui, 25, "§fCharacter", CnpcGuiSupport.COL_R, row, () -> CnpcLmGui.open(player, "character", "main"));
            row += gap;
            CnpcGuiSupport.button(gui, 26, "§cRemove Android", CnpcGuiSupport.COL_L, row, () -> CnpcLmGui.open(player, "android_remove", "main"));
            if (StaffAccess.isStaff(player)) {
                CnpcGuiSupport.button(gui, 27, "§5Progression §8(staff)", CnpcGuiSupport.COL_R, row, () -> CnpcLmGui.open(player, "progression", "main"));
                row += gap;
                CnpcGuiSupport.button(gui, 28, "§8Event log", CnpcGuiSupport.COL_L, row, () -> CnpcLmLogsGui.open(player, "main"));
            }
        } else {
            gui.addLabel(3, "§cLegacyMechanics Forge mod not loaded.", CnpcGuiSupport.M, row, 400, 14);
        }
        row += gap;
        CnpcGuiSupport.buttonSmall(gui, 98, "§cClose", CnpcGuiSupport.COL_L, row, 95, () -> {});
        CnpcGuiSupport.buttonSmall(gui, 99, "§7Refresh", CnpcGuiSupport.COL_R, row, 95, () -> paintMain(player));
    }
}
