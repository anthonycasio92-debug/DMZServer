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

public final class CnpcLmHubGui {
    private static final int HUB_H = 368;

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
        paintMain(player);
    }

    private static void paintMain(ServerPlayer player) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_HUB, CnpcGuiSupport.W, HUB_H, (p, gui) -> paintMain(p, gui));
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = CnpcGuiSupport.target(player);
        Map<String, String> ph = MechanicsGuiApi.placeholders(who);
        boolean staff = StaffAccess.isStaff(player);
        boolean skillCheck = SkillCheckService.canUse(player);

        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§f§lLegacy Mechanics",
                "§7Same menu as §f/lm §8— pick a system below");

        List<String> lines = hubSnapshot(who, ph, staff, skillCheck);
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 5);
        int gap = CnpcGuiSupport.ROW_STEP;

        if (!"true".equals(ph.get("bridge_ok"))) {
            gui.addLabel(CnpcGuiSupport.ID_STATUS_TAG, "§cLegacy Mechanics is not loaded on this server.",
                    CnpcGuiSupport.M, row, 400, 14);
            row += gap;
            CnpcGuiSupport.footerCloseRefresh(player, gui, row, () -> paintMain(player));
            return;
        }

        systemBtn(gui, player, ph, "difficulty", row, CnpcGuiSupport.COL_L,
                "§aDifficulty §8· tiers & world scaling",
                () -> CnpcLmGui.open(player, "difficulty", "main"));
        systemBtn(gui, player, ph, "rival", row, CnpcGuiSupport.COL_R,
                "§6Rival §8· rivalry & challenges",
                () -> CnpcLmGui.open(player, "rival", "main"));
        row += gap;

        systemBtn(gui, player, ph, "spar", row, CnpcGuiSupport.COL_L,
                "§bSparring §8· training teleports",
                () -> CnpcLmGui.open(player, "spar", "main"));
        systemBtn(gui, player, ph, "prestige", row, CnpcGuiSupport.COL_R,
                "§dPrestige §8· turn-ins & shop",
                () -> CnpcLmGui.open(player, "prestige", "main"));
        row += gap;

        if (skillCheck) {
            CnpcGuiSupport.button(gui, 24, "§eSkill Check §8· Natural & Saga", CnpcGuiSupport.COL_L, row,
                    () -> CnpcLmGui.open(player, "skillcheck", "main"));
        } else if (staff) {
            CnpcGuiSupport.button(gui, 24, "§eSkills §8· staff unlock browser", CnpcGuiSupport.COL_L, row,
                    () -> CnpcLmGui.open(player, "skills", "core"));
        } else {
            CnpcGuiSupport.buttonSmall(gui, 24, "§8Skill Check §7(permission)", CnpcGuiSupport.COL_L, row,
                    CnpcGuiSupport.BTN_W,
                    () -> CnpcGuiSupport.feedback(player,
                            "§7Ask staff about §fSkill Check §7access (donator perk)."));
        }
        CnpcGuiSupport.button(gui, 25, "§fCharacter §8· race, class, reskin", CnpcGuiSupport.COL_R, row,
                () -> CnpcLmGui.open(player, "character", "main"));
        row += gap;

        CnpcGuiSupport.button(gui, 26, "§cRemove Android §8· two-step confirm", CnpcGuiSupport.COL_L, row,
                () -> CnpcLmGui.open(player, "android_remove", "main"));

        if (staff) {
            CnpcGuiSupport.button(gui, 27, "§5Progression §8· staff tools", CnpcGuiSupport.COL_R, row,
                    () -> CnpcLmGui.open(player, "progression", "main"));
            row += gap;
            CnpcGuiSupport.button(gui, 28, "§cAdmin §8· reload & migrate", CnpcGuiSupport.COL_L, row,
                    () -> CnpcLmAdminGui.open(player, "main"));
            CnpcGuiSupport.button(gui, 29, "§8Event log §8· telemetry", CnpcGuiSupport.COL_R, row,
                    () -> CnpcLmLogsGui.open(player, "main"));
        }
        row += gap;
        CnpcGuiSupport.footerCloseRefresh(player, gui, row, () -> paintMain(player));
    }

    private static void systemBtn(
            ICustomGui gui,
            ServerPlayer viewer,
            Map<String, String> ph,
            String systemKey,
            int row,
            int col,
            String enabledLabel,
            Runnable open) {
        int id = switch (systemKey) {
            case "difficulty" -> 20;
            case "rival" -> 21;
            case "spar" -> 22;
            case "prestige" -> 23;
            default -> 30;
        };
        if ("difficulty".equals(systemKey) || "true".equals(ph.get(systemKey))) {
            CnpcGuiSupport.button(gui, id, enabledLabel, col, row, open);
            return;
        }
        String pretty = switch (systemKey) {
            case "rival" -> "Rival";
            case "spar" -> "Sparring";
            case "prestige" -> "Prestige";
            default -> systemKey;
        };
        CnpcGuiSupport.buttonSmall(gui, id, "§8" + pretty + " §7(unavailable)", col, row, CnpcGuiSupport.BTN_W,
                () -> CnpcGuiSupport.feedback(viewer,
                        "§7" + pretty + " is off on this server. Ask staff if you think that's wrong."));
    }

    private static List<String> hubSnapshot(
            ServerPlayer who, Map<String, String> ph, boolean staff, boolean skillCheck) {
        List<String> lines = new ArrayList<>();
        lines.add("§7Hey §f" + who.m_7755_().getString() + "§7 — pick a button below.");

        try {
            int level = DmzProgression.guiDisplayDmzLevel(who);
            lines.add("§7Level §f" + level + " §8· §7Prestige scale §f" + ph.getOrDefault("overhaul_scale", "x1"));
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
                lines.add("§7Rivals §f" + rph.getOrDefault("mutual", "0") + "/"
                        + rph.getOrDefault("mutual_max", "3")
                        + " §8· §7" + rph.getOrDefault("wins", "0") + "W "
                        + rph.getOrDefault("losses", "0") + "L");
            }
        } catch (Throwable ignored) {
        }

        try {
            var sph = SparGuiApi.placeholders(who);
            if ("true".equals(sph.get("session_active"))) {
                String partner = sph.getOrDefault("partner", "");
                lines.add("§7Spar session §aactive"
                        + (partner.isBlank() ? "" : " §8· §7with §f" + partner));
            }
        } catch (Throwable ignored) {
        }

        if (skillCheck && "true".equals(ph.get("skillcheck_session"))) {
            lines.add("§7You have a §eSkill Check §7session open.");
        } else if (staff) {
            lines.add("§7Staff: progression, admin, and logs are on the last rows.");
        }

        return lines;
    }
}
