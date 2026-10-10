package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.gui.MechanicsGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

/** Legacy Mechanics main menu (CustomNPCs primary UI). */
public final class CnpcLmHubGui {
    private CnpcLmHubGui() {}

    public static void open(ServerPlayer player, String page) {
        if ("admin".equalsIgnoreCase(page)) {
            CnpcLmAdminGui.open(player, "main");
            return;
        }
        if ("logs".equalsIgnoreCase(page) || "syslog".equalsIgnoreCase(page)) {
            if (StaffAccess.isStaff(player)) {
                CnpcLmLogsGui.open(player, "main");
            } else {
                paintMain(player);
            }
            return;
        }
        if ("progression".equalsIgnoreCase(page) || "prog".equalsIgnoreCase(page)) {
            if (StaffAccess.isStaff(player)) {
                CnpcLmProgressionGui.open(player, "main");
            } else {
                paintMain(player);
            }
            return;
        }
        paintMain(player);
    }

    private static void paintMain(ServerPlayer player) {
        int height = CnpcGuiSupport.suggestHeight((StaffAccess.isStaff(player) ? 400 : 404) + 16);
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_HUB, CnpcGuiSupport.W, height,
                (p, gui) -> paintMain(p, gui));
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = CnpcGuiSupport.target(player);
        Map<String, String> ph = MechanicsGuiApi.placeholders(who);
        boolean staff = StaffAccess.isStaff(player);
        boolean skillCheck = SkillCheckService.canUse(player);

        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcUltraStyle.BODY + CnpcUltraStyle.BOLD + "Legacy Mechanics",
                CnpcUltraStyle.SUBTITLE + "Scaling, rivals, sparring, prestige, character tools, and more");

        List<String> lines = CnpcPlayerSnapshot.hubLines(who, ph, staff, skillCheck);
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(
                gui, infoY, lines, CnpcGuiStyle.INFO_INLINE_MAX));
        row = CnpcGuiSupport.paintSectionTag(gui, CnpcGuiSupport.ID_INLINE_NOTE, row, "Systems");
        int gap = CnpcGuiSupport.ROW_STEP;

        if (!"true".equals(ph.get("bridge_ok"))) {
            gui.addLabel(CnpcGuiSupport.ID_STATUS_TAG, CnpcUltraStyle.INFO + "Legacy Mechanics could not load your menu data. Try relogging.",
                    CnpcGuiSupport.M, row, CnpcGuiSupport.textBandWidth(), 14);
            row += gap;
            CnpcGuiSupport.footerCloseRefresh(player, gui, row, () -> paintMain(player));
            CnpcGuiSupport.paintSystemMainPreview(who, gui, player);
            return;
        }

        systemBtn(gui, player, ph, "difficulty", row, CnpcGuiSupport.COL_L, CnpcUltraStyle.CONFIRM + "Difficulty",
                () -> CnpcLmGui.open(player, "difficulty", "main"));
        systemBtn(gui, player, ph, "rival", row, CnpcGuiSupport.COL_R, CnpcUltraStyle.ACCENT + "Rival",
                () -> CnpcLmGui.open(player, "rival", "main"));
        row += gap;

        systemBtn(gui, player, ph, "spar", row, CnpcGuiSupport.COL_L, CnpcUltraStyle.ACCENT + "Sparring",
                () -> CnpcLmGui.open(player, "spar", "main"));
        systemBtn(gui, player, ph, "prestige", row, CnpcGuiSupport.COL_R, CnpcUltraStyle.ACCENT + "Prestige",
                () -> CnpcLmGui.open(player, "prestige", "main"));
        row += gap;

        if (skillCheck) {
            CnpcGuiSupport.button(gui, 24, CnpcUltraStyle.INFO + "Skill Check", CnpcGuiSupport.COL_L, row,
                    () -> CnpcLmGui.open(player, "skillcheck", "main"));
        } else if (staff) {
            CnpcGuiSupport.button(gui, 24, CnpcUltraStyle.INFO + "Skills", CnpcGuiSupport.COL_L, row,
                    () -> CnpcLmGui.open(player, "skills", "core"));
        } else {
            CnpcGuiSupport.buttonSmall(gui, 24, CnpcUltraStyle.DIM + "Skill Check", CnpcGuiSupport.COL_L, row, CnpcGuiSupport.BTN_W,
                    () -> {
                        CnpcGuiSupport.pushMenuMessage(player,
                                CnpcMenuFeedback.NOTICE_BODY
                                        + "Skill Check is a donator perk — ask staff if you want access.");
                        paintMain(player);
                    });
        }
        CnpcGuiSupport.button(gui, 25, CnpcUltraStyle.BODY + "Character Services", CnpcGuiSupport.COL_R, row,
                () -> CnpcLmGui.open(player, "character", "main"));
        row += gap;

        if (staff) {
            CnpcGuiSupport.button(gui, 27, CnpcUltraStyle.ACCENT + "Staff Admin", CnpcGuiSupport.COL_L, row,
                    () -> CnpcLmAdminGui.open(player, "main"));
            CnpcGuiSupport.button(gui, 28, CnpcUltraStyle.ACCENT + "Saga Reset", CnpcGuiSupport.COL_R, row,
                    () -> CnpcLmGui.open(player, "saga", "main"));
            row += gap;
        } else {
            CnpcGuiSupport.button(gui, 28, CnpcUltraStyle.ACCENT + "Saga Reset", CnpcGuiSupport.COL_L, row,
                    () -> CnpcLmGui.open(player, "saga", "main"));
            row += gap;
        }
        CnpcGuiSupport.footerCloseRefresh(player, gui, row, () -> paintMain(player));
        CnpcGuiSupport.paintSystemMainPreview(who, gui, player);
    }

    private static void systemBtn(
            ICustomGui gui,
            ServerPlayer viewer,
            Map<String, String> ph,
            String systemKey,
            int row,
            int col,
            String label,
            Runnable open) {
        int id = switch (systemKey) {
            case "difficulty" -> 20;
            case "rival" -> 21;
            case "spar" -> 22;
            case "prestige" -> 23;
            default -> 30;
        };
        if ("difficulty".equals(systemKey) || "true".equals(ph.get(systemKey))) {
            CnpcGuiSupport.button(gui, id, label, col, row, open);
            return;
        }
        String pretty = switch (systemKey) {
            case "rival" -> "Rival";
            case "spar" -> "Sparring";
            case "prestige" -> "Prestige";
            default -> systemKey;
        };
        CnpcGuiSupport.buttonSmall(gui, id, CnpcUltraStyle.DIM + pretty, col, row, CnpcGuiSupport.BTN_W,
                () -> {
                    CnpcGuiSupport.pushMenuMessage(viewer,
                            CnpcMenuFeedback.NOTICE_BODY + pretty
                                    + " is off on this server. Ask staff if you think that's wrong.");
                    paintMain(viewer);
                });
    }

}
