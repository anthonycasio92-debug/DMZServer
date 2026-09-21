package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

public final class CnpcLmProgressionGui {
    private static final int H = 340;

    private CnpcLmProgressionGui() {}

    public static void open(ServerPlayer player, String page) {
        if (!StaffAccess.isStaff(player) && !"android_remove".equalsIgnoreCase(page)) {
            player.m_213846_(net.minecraft.network.chat.Component.m_237113_("§cStaff only."));
            return;
        }
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_PROGRESSION, CnpcGuiSupport.W, H, (pl, gui) -> paint(pl, gui, p));
    }

    private static void paint(ServerPlayer player, ICustomGui gui, String page) {
        if ("android_remove".equals(page)) {
            CnpcGuiSupport.title(gui, 1, "§cRemove Android");
            CnpcGuiSupport.subtitle(gui, 2, "§7Confirm to restore prior forms");
            CnpcGuiSupport.button(gui, 20, "§cConfirm remove", CnpcGuiSupport.COL_L, 100, () -> CnpcGuiSupport.act(
                    player,
                    () -> ProgressionGuiApi.androidRemove(player, "confirm"),
                    () -> CnpcLmHubGui.open(player, "main")));
            CnpcGuiSupport.buttonSmall(gui, 96, "§7« Hub", CnpcGuiSupport.COL_L, 140, 95,
                    () -> CnpcLmHubGui.open(player, "main"));
            return;
        }

        if ("main".equals(page)) {
            paintMainHub(player, gui);
            return;
        }

        if ("admin".equals(page) || "flags".equals(page)) {
            paintFlagSection(player, gui, "admin", "§cAdmin flags", new String[] {
                    "flight", "sprint", "meditation", "potential", "farming", "building", "boost", "bio",
                    "racelock", "yardrat", "spiritualist", "android", "kiweapons", "piercing", "dot", "apothic",
                    "end", "endportal", "shadow", "statchecker", "fabled"
            });
            return;
        }

        if ("flags_fabled".equals(page) || "fabled_flags".equals(page)) {
            paintFlagSection(player, gui, "fabled", "§dFabled subflags", new String[] {
                    "fabled", "energy", "statscreen", "tpsp", "attr", "prestigeskill", "faction", "cleaner",
                    "raceclass", "classperm"
            });
            return;
        }

        if ("economy".equals(page) || "ancient_coins".equals(page) || "coins".equals(page)) {
            paintEconomy(player, gui);
            return;
        }

        String[] flags = flagsForSection(page);
        if (flags != null) {
            paintFlagSection(player, gui, page, sectionTitle(page), flags);
            return;
        }

        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§5Progression §8(staff)", "§7" + page);
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, ProgressionGuiApi.linesForPage(player, page), 5);
        footer(player, gui, row + 8, "main");
    }

    private static void paintMainHub(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§5Progression §8(staff)",
                "§7Server flags · economy · android tools");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, ProgressionGuiApi.linesForPage(player, "main"), 4);
        CnpcGuiSupport.button(gui, 20, "§eSkills flags", CnpcGuiSupport.COL_L, row, () -> open(player, "skills"));
        CnpcGuiSupport.button(gui, 21, "§6TP flags", CnpcGuiSupport.COL_R, row, () -> open(player, "tp"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§bRace flags", CnpcGuiSupport.COL_L, row, () -> open(player, "race"));
        CnpcGuiSupport.button(gui, 23, "§cCombat flags", CnpcGuiSupport.COL_R, row, () -> open(player, "combat"));
        row += 24;
        CnpcGuiSupport.button(gui, 24, "§5End flags", CnpcGuiSupport.COL_L, row, () -> open(player, "end"));
        CnpcGuiSupport.button(gui, 25, "§dFabled bridges", CnpcGuiSupport.COL_R, row, () -> open(player, "fabled"));
        row += 24;
        CnpcGuiSupport.button(gui, 26, "§7Utility", CnpcGuiSupport.COL_L, row, () -> open(player, "utility"));
        CnpcGuiSupport.button(gui, 27, "§eStatus snapshot", CnpcGuiSupport.COL_R, row, () -> open(player, "status"));
        row += 24;
        CnpcGuiSupport.button(gui, 28, "§6Ancient coin economy", CnpcGuiSupport.COL_L, row, () -> open(player, "economy"));
        CnpcGuiSupport.button(gui, 29, "§cAll flags", CnpcGuiSupport.COL_R, row, () -> open(player, "admin"));
        row += 24;
        CnpcGuiSupport.button(gui, 30, "§dFabled subflags", CnpcGuiSupport.COL_L, row, () -> open(player, "flags_fabled"));
        row += 24;
        footer(player, gui, row, "main");
    }

    private static void paintEconomy(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§6Ancient coin economy", "§7Staff economy tools");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, ProgressionGuiApi.linesForPage(player, "economy"), 5);
        CnpcGuiSupport.button(gui, 20, "§eToggle staff free costs", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> ProgressionGuiApi.handleDo(player, "toggle_staff_free_coins", "", "economy"),
                () -> open(player, "economy")));
        footer(player, gui, row + 28, "main");
    }

    private static void paintFlagSection(ServerPlayer player, ICustomGui gui, String page, String title, String[] keys) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, "§7Tap to toggle");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, ProgressionGuiApi.linesForPage(player, page), 3);
        int id = 40;
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            int col = (i % 2 == 0) ? CnpcGuiSupport.COL_L : CnpcGuiSupport.COL_R;
            if (i > 0 && i % 2 == 0) {
                row += 24;
            }
            if (row > 280) {
                break;
            }
            CnpcGuiSupport.buttonSmall(gui, id++, "§fToggle " + key, col, row, 195, () -> CnpcGuiSupport.act(
                    player,
                    () -> ProgressionGuiApi.handleDo(player, "flag", key, page),
                    () -> open(player, page)));
        }
        row += 32;
        footer(player, gui, row, "main");
    }

    private static String sectionTitle(String page) {
        return switch (page) {
            case "skills" -> "§eSkills flags";
            case "tp" -> "§6TP flags";
            case "race" -> "§bRace flags";
            case "combat" -> "§cCombat flags";
            case "end" -> "§5End flags";
            case "fabled" -> "§dFabled bridges";
            case "utility" -> "§7Utility flags";
            case "status" -> "§eStatus";
            case "economy", "ancient_coins", "coins" -> "§6Economy";
            default -> "§5Progression";
        };
    }

    private static String[] flagsForSection(String page) {
        return switch (page) {
            case "skills" -> new String[] {"flight", "sprint", "meditation", "potential"};
            case "tp" -> new String[] {"farming", "building", "boost", "bio"};
            case "race" -> new String[] {"racelock", "yardrat", "spiritualist", "android"};
            case "combat" -> new String[] {"kiweapons", "piercing", "dot", "apothic"};
            case "end" -> new String[] {"end", "endportal"};
            case "fabled" -> new String[] {"fabled"};
            case "utility" -> new String[] {"shadow", "statchecker"};
            case "status" -> new String[] {"flight", "sprint", "meditation", "potential", "farming", "building"};
            default -> null;
        };
    }

    private static void footer(ServerPlayer player, ICustomGui gui, int row, String backPage) {
        CnpcGuiSupport.navHubMain(player, gui, row, () -> open(player, backPage));
    }
}
