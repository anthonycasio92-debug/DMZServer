package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.gui.MechanicsGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.RivalGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.SparGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

/** Legacy Mechanics main menu (CustomNPCs primary UI). */
public final class CnpcLmHubGui {
    private static final int ID_SECTION_COMBAT = 8;
    private static final int ID_SECTION_CHARACTER = 9;

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
                CnpcLmAdminGui.open(player, "main");
            } else {
                paintMain(player);
            }
            return;
        }
        paintMain(player);
    }

    private static void paintMain(ServerPlayer player) {
        int height = CnpcGuiSupport.suggestHeight(StaffAccess.isStaff(player) ? 360 : 340);
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_HUB, CnpcGuiSupport.W, height,
                (p, gui) -> paintMain(p, gui));
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = CnpcGuiSupport.target(player);
        Map<String, String> ph = MechanicsGuiApi.placeholders(who);
        boolean staff = StaffAccess.isStaff(player);
        boolean skillCheck = SkillCheckService.canUse(player);

        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§f§lLegacy Mechanics",
                "§7Scaling, rivals, sparring, prestige, and character tools");

        List<String> lines = hubSnapshot(who, ph, staff, skillCheck);
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 3));
        int gap = CnpcGuiSupport.ROW_STEP;

        if (!"true".equals(ph.get("bridge_ok"))) {
            gui.addLabel(CnpcGuiSupport.ID_STATUS_TAG, "§cLegacy Mechanics is not available on this server.",
                    CnpcGuiSupport.M, row, CnpcGuiSupport.textBandWidth(), 14);
            row += gap;
            CnpcGuiSupport.footerCloseRefresh(player, gui, row, () -> paintMain(player));
            return;
        }

        row = CnpcGuiSupport.paintSectionTag(gui, ID_SECTION_COMBAT, row + 4, "§8Combat & world scaling");
        systemBtn(gui, player, ph, "difficulty", row, CnpcGuiSupport.COL_L, "§aDifficulty",
                () -> CnpcLmGui.open(player, "difficulty", "main"));
        systemBtn(gui, player, ph, "rival", row, CnpcGuiSupport.COL_R, "§6Rival",
                () -> CnpcLmGui.open(player, "rival", "main"));
        row += gap;

        systemBtn(gui, player, ph, "spar", row, CnpcGuiSupport.COL_L, "§bSparring",
                () -> CnpcLmGui.open(player, "spar", "main"));
        systemBtn(gui, player, ph, "prestige", row, CnpcGuiSupport.COL_R, "§dPrestige",
                () -> CnpcLmGui.open(player, "prestige", "main"));
        row += gap + 4;

        row = CnpcGuiSupport.paintSectionTag(gui, ID_SECTION_CHARACTER, row, "§8Character & account");
        if (skillCheck) {
            CnpcGuiSupport.button(gui, 24, "§eSkill Check", CnpcGuiSupport.COL_L, row,
                    () -> CnpcLmGui.open(player, "skillcheck", "main"));
        } else if (staff) {
            CnpcGuiSupport.button(gui, 24, "§eSkills", CnpcGuiSupport.COL_L, row,
                    () -> CnpcLmGui.open(player, "skills", "core"));
        } else {
            CnpcGuiSupport.buttonSmall(gui, 24, "§8Skill Check", CnpcGuiSupport.COL_L, row, CnpcGuiSupport.BTN_W,
                    () -> {
                        CnpcGuiSupport.pushMenuMessage(player,
                                "§7Skill Check is a donator perk — ask staff if you want access.");
                        paintMain(player);
                    });
        }
        CnpcGuiSupport.button(gui, 25, "§fCharacter Services", CnpcGuiSupport.COL_R, row,
                () -> CnpcLmGui.open(player, "character", "main"));
        row += gap;

        CnpcGuiSupport.button(gui, 26, "§cRemove Android", CnpcGuiSupport.COL_L, row,
                () -> CnpcLmGui.open(player, "android_remove", "main"));
        if (staff) {
            CnpcGuiSupport.button(gui, 27, "§cStaff Admin", CnpcGuiSupport.COL_R, row,
                    () -> CnpcLmAdminGui.open(player, "main"));
        }
        row += gap;
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
        CnpcGuiSupport.buttonSmall(gui, id, "§8" + pretty, col, row, CnpcGuiSupport.BTN_W,
                () -> {
                    CnpcGuiSupport.pushMenuMessage(viewer,
                            "§7" + pretty + " is off on this server. Ask staff if you think that's wrong.");
                    paintMain(viewer);
                });
    }

    private static List<String> hubSnapshot(
            ServerPlayer who, Map<String, String> ph, boolean staff, boolean skillCheck) {
        List<String> lines = new ArrayList<>();
        lines.add("§7Hi §f" + who.m_7755_().getString() + "§7 — here is a quick snapshot.");

        try {
            int level = DmzProgression.guiDisplayDmzLevel(who);
            lines.add("§7Level §f" + level + " §8· §7Growth pace §f" + ph.getOrDefault("overhaul_scale", "x1"));
        } catch (Throwable ignored) {
        }

        try {
            if (DifficultyConfig.isEnabled()) {
                DifficultySnapshot snap = DifficultyCache.refresh(who);
                lines.add("§7Difficulty tier §f" + snap.activeTierName);
            }
        } catch (Throwable ignored) {
        }

        try {
            var rph = RivalGuiApi.placeholders(who);
            if ("true".equals(rph.get("system_enabled"))) {
                lines.add("§7Rivals §f" + rph.getOrDefault("mutual", "0") + " of "
                        + rph.getOrDefault("mutual_max", "3")
                        + " §8· §7Record §f" + rph.getOrDefault("wins", "0") + "W "
                        + rph.getOrDefault("losses", "0") + "L");
            }
        } catch (Throwable ignored) {
        }

        try {
            var sph = SparGuiApi.placeholders(who);
            if ("true".equals(sph.get("session_active"))) {
                String partner = sph.getOrDefault("partner", "");
                lines.add("§7Sparring §ais active"
                        + (partner.isBlank() ? "" : " §8· §7with §f" + partner));
            }
        } catch (Throwable ignored) {
        }

        if (skillCheck && "true".equals(ph.get("skillcheck_session"))) {
            lines.add("§7Skill Check session is open on this account.");
        } else if (staff) {
            lines.add("§7Staff: open §fStaff Admin §7for server tools.");
        }

        return lines;
    }
}
