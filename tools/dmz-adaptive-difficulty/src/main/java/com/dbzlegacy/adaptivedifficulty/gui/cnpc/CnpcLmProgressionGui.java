package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.gui.RivalGuiApi;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IScroll;

/** CustomNPCs progression UI — parity with {@code ProgressionChestGui}. */
public final class CnpcLmProgressionGui {
    private static final int H_MAIN = 380;

    private CnpcLmProgressionGui() {}

    public static void open(ServerPlayer player, String page) {
        String p = normalizePage(page);
        if (requiresStaff(p) && !StaffAccess.isStaff(player)) {
            CnpcGuiSupport.denyToHub(player, "§cStaff only — that progression page is for staff.");
            return;
        }
        int h = switch (p) {
            case "android_convert", "android_remove" -> 320;
            case "skills", "tp", "race", "combat", "end", "fabled", "utility", "status" -> sectionHeight(p);
            default -> H_MAIN;
        };
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_PROGRESSION, CnpcGuiSupport.W, h,
                (pl, gui) -> paint(pl, gui, p));
    }

    private static String normalizePage(String page) {
        if (page == null || page.isBlank()) {
            return "main";
        }
        String p = page.toLowerCase(Locale.ROOT).trim();
        return switch (p) {
            case "boost_panel", "tpboost" -> "boost_panel";
            case "android_panel", "android_tools", "androidtools" -> "android_panel";
            case "android_convert", "androidconvert", "convert_android" -> "android_convert";
            case "android_remove", "androidremove", "remove_android", "deandroid" -> "android_remove";
            case "admin", "flags", "disable", "flags_fabled", "fabled_flags" -> "main";
            case "ancient_coins", "coins" -> "economy";
            default -> p;
        };
    }

    private static boolean requiresStaff(String page) {
        return switch (page) {
            case "economy", "boost_panel" -> true;
            default -> false;
        };
    }

    private static void paint(ServerPlayer player, ICustomGui gui, String page) {
        switch (page) {
            case "main" -> paintMainHub(player, gui);
            case "skills", "tp", "race", "combat", "end", "fabled", "utility", "status" ->
                    paintSection(player, gui, page);
            case "boost_panel" -> paintBoostPanel(player, gui);
            case "android_panel" -> paintAndroidPanel(player, gui);
            case "android_convert" -> paintAndroidConvert(player, gui);
            case "android_remove" -> paintAndroidRemove(player, gui);
            case "economy" -> paintEconomy(player, gui);
            default -> paintMainHub(player, gui);
        }
    }

    private static void paintMainHub(ServerPlayer player, ICustomGui gui) {
        boolean staff = StaffAccess.isStaff(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§dProgression",
                staff ? "§7Staff: TP boost · Android tools · Ancient coins"
                        : "§7Module status and progression tools");
        List<String> info = new ArrayList<>(ProgressionGuiApi.linesForPage(player, "main"));
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, info, 3));

        row = placeRow(gui, player, row, 20, "§eSkills", CnpcGuiSupport.COL_L, () -> open(player, "skills"));
        placeRow(gui, player, row, 21, "§6TP gains", CnpcGuiSupport.COL_R, () -> open(player, "tp"));
        row += CnpcGuiSupport.ROW_STEP;
        placeRow(gui, player, row, 22, "§bRace & form", CnpcGuiSupport.COL_L, () -> open(player, "race"));
        placeRow(gui, player, row, 23, "§cCombat", CnpcGuiSupport.COL_R, () -> open(player, "combat"));
        row += CnpcGuiSupport.ROW_STEP;
        placeRow(gui, player, row, 24, "§5End", CnpcGuiSupport.COL_L, () -> open(player, "end"));
        placeRow(gui, player, row, 25, "§dFabled", CnpcGuiSupport.COL_R, () -> open(player, "fabled"));
        row += CnpcGuiSupport.ROW_STEP;
        placeRow(gui, player, row, 26, "§7Utility", CnpcGuiSupport.COL_L, () -> open(player, "utility"));
        placeRow(gui, player, row, 27, "§eStatus", CnpcGuiSupport.COL_R, () -> open(player, "status"));
        row += CnpcGuiSupport.ROW_STEP;

        if (staff) {
            placeRow(gui, player, row, 28, "§6Ancient coins", CnpcGuiSupport.COL_L, () -> open(player, "economy"));
        }
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.navSystemRoot(player, gui, row);
        CnpcGuiSupport.paintSystemMainPreview(CnpcGuiSupport.target(player), gui, player);
    }

    private static int placeRow(
            ICustomGui gui, ServerPlayer player, int row, int id, String label, int col, Runnable action) {
        CnpcGuiSupport.button(gui, id, label, col, row, action);
        return row;
    }

    private static void paintSection(ServerPlayer player, ICustomGui gui, String page) {
        boolean staff = StaffAccess.isStaff(player);
        String title = sectionTitle(page);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, CnpcGuiStyle.HINT_READ_ONLY);
        List<String> sectionLines = ProgressionGuiApi.linesForPage(player, page);
        int row = sectionLines.size() > 2
                ? CnpcGuiSupport.paintLongReadOnlyBody(gui, infoY, sectionLines)
                : CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, sectionLines, 2));

        Map<String, String> ph = ProgressionGuiApi.placeholders(player);
        List<CnpcGuiLayout.GridButton> grid = new ArrayList<>();
        if ("tp".equals(page)) {
            if (staff) {
                grid.add(CnpcGuiLayout.GridButton.run("§6Global TP boost", () -> open(player, "boost_panel")));
            } else {
                grid.add(CnpcGuiLayout.GridButton.disabled(
                        "§6Global TP boost §8· " + CnpcGuiSupport.flagOnOff(ph, "boost")));
            }
        }
        if ("race".equals(page) && ProgressionConfig.androidConversion()) {
            grid.add(CnpcGuiLayout.GridButton.run("§bAndroid tools", () -> open(player, "android_panel")));
        }
        if (grid.isEmpty()) {
            CnpcGuiSupport.navSubmenu(player, gui, row + 8, () -> open(player, "main"), "§7« Back");
            return;
        }
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(
                player, gui, row + 4, 40, grid.toArray(CnpcGuiLayout.GridButton[]::new),
                () -> open(player, page));
        row += 8;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "main"), "§7« Back");
    }

    private static int sectionHeight(String page) {
        int tools = 0;
        if ("tp".equals(page)) {
            tools = 1;
        }
        if ("race".equals(page) && ProgressionConfig.androidConversion()) {
            tools++;
        }
        return CnpcGuiSupport.suggestHeight(200 + ((tools + 1) / 2) * CnpcGuiSupport.ROW_STEP + 64);
    }

    private static void paintBoostPanel(ServerPlayer player, ICustomGui gui) {
        Map<String, String> ph = ProgressionGuiApi.placeholders(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§6Global TP boost", "§7Timed world TP multiplier");
        List<String> lines = new ArrayList<>();
        lines.add(ph.getOrDefault("boost", "§7Global TP boost: §cOFF"));
        lines.add("§8Presets start a boost · End stops it");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 3));

        int b = CnpcGuiSupport.ID_BOOST_PRESET_BASE;
        row = boostPreset(gui, player, row, b, "§e1.25× · 30m", CnpcGuiSupport.COL_L, "1.25:30");
        boostPreset(gui, player, row, b + 1, "§e1.5× · 30m", CnpcGuiSupport.COL_R, "1.5:30");
        row += CnpcGuiSupport.ROW_STEP;
        boostPreset(gui, player, row, b + 2, "§62× · 30m", CnpcGuiSupport.COL_L, "2:30");
        boostPreset(gui, player, row, b + 3, "§62× · 60m", CnpcGuiSupport.COL_R, "2:60");
        row += CnpcGuiSupport.ROW_STEP;
        boostPreset(gui, player, row, b + 4, "§e3× · 30m", CnpcGuiSupport.COL_L, "3:30");
        CnpcGuiSupport.button(gui, b + 5, "§cEnd boost", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> ProgressionGuiApi.handleDo(player, "boost", "end", "boost_panel"),
                () -> open(player, "boost_panel")));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.buttonSmall(gui, b + 6, "§7Refresh", CnpcGuiSupport.COL_L, row, 95,
                () -> open(player, "boost_panel"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "tp"), "§7« Back");
    }

    private static int boostPreset(
            ICustomGui gui, ServerPlayer player, int row, int id, String label, int col, String arg) {
        CnpcGuiSupport.buttonSmall(gui, id, label, col, row, CnpcGuiSupport.BTN_W, () -> CnpcGuiSupport.act(
                player,
                () -> ProgressionGuiApi.handleDo(player, "boost", arg, "boost_panel"),
                () -> open(player, "boost_panel")));
        return row;
    }

    private static void paintAndroidPanel(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§bAndroid tools",
                "§7Dr. Gero convert · remove upgrade");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                "§7Convert keeps race · unlocks Android forms",
                "§7Eligible races:",
                "§f" + com.dbzlegacy.adaptivedifficulty.progression.race.AndroidConversion.eligibleRaceHint(),
                "§7Remove restores prior forms (confirm within 10s)"
        ), 3));
        CnpcGuiSupport.button(gui, 60, "§aConvert to Android…", CnpcGuiSupport.COL_L, row,
                () -> open(player, "android_convert"));
        CnpcGuiSupport.button(gui, 61, "§cRemove upgrade…", CnpcGuiSupport.COL_R, row,
                () -> open(player, "android_remove"));
        row += CnpcGuiSupport.ROW_STEP + 8;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "race"), "§7« Back");
    }

    private static void paintAndroidConvert(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = CnpcGuiSupport.target(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§d", "Progression", "Android convert"),
                CnpcGuiStyle.HINT_DOUBLE_CLICK_PLAYER);
        int row = CnpcGuiSupport.bodyBelowHeader(infoY);
        CnpcGuiSupport.button(gui, 62, "§aConvert yourself", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> ProgressionGuiApi.handleDo(player, "android", subject.m_7755_().getString(), "android_convert"),
                () -> open(player, "android_convert")));
        row += CnpcGuiSupport.ROW_STEP + 4;
        if (StaffAccess.isStaff(player)) {
            row = paintNameScroll(player, gui, row, "android", "android_convert");
        }
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "android_panel"), "§7« Back");
    }

    private static void paintAndroidRemove(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = CnpcGuiSupport.target(player);
        boolean staff = StaffAccess.isStaff(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§cRemove Android",
                "§7Two-step confirm within 10 seconds");
        int row = CnpcGuiSupport.bodyBelowHeader(infoY);
        CnpcGuiSupport.button(gui, 63, "§cRemove on yourself", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> ProgressionGuiApi.handleDo(player, "android_remove", subject.m_7755_().getString(),
                        "android_remove"),
                () -> open(player, "android_remove")));
        row += CnpcGuiSupport.ROW_STEP + 4;
        if (staff) {
            row = paintNameScroll(player, gui, row, "android_remove", "android_remove");
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "android_panel"), "§7« Back");
        } else {
            CnpcGuiSupport.navSubmenu(player, gui, row + 8, () -> open(player, "main"), "§7« Back");
        }
    }

    /** @return Y row for footer after list (or after empty label). */
    private static int paintNameScroll(
            ServerPlayer player, ICustomGui gui, int y, String action, String returnPage) {
        List<String> names = RivalGuiApi.onlinePlayerNames(CnpcGuiSupport.target(player));
        if (names.isEmpty()) {
            gui.addLabel(70, "§7No other players online.", CnpcGuiSupport.M, y + 8, CnpcGuiSupport.textBandWidth(), 14);
            return y + 28;
        }
        int rowsBelow = 1;
        int bandY = y + 14;
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, y, rowsBelow, names.toArray(String[]::new));
        scroll.setOnDoubleClick((g, sc) -> {
            g.close();
            int[] sel = sc.getSelection();
            if (sel != null && sel.length > 0 && sel[0] >= 0 && sel[0] < names.size()) {
                String name = names.get(sel[0]);
                CnpcGuiSupport.act(player,
                        () -> ProgressionGuiApi.handleDo(player, action, name, returnPage),
                        () -> open(player, returnPage));
            }
        });
        return CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
    }

    private static void paintEconomy(ServerPlayer player, ICustomGui gui) {
        Map<String, String> ph = ProgressionGuiApi.placeholders(player);
        boolean staffFree = "true".equalsIgnoreCase(ph.getOrDefault("staff_free_ancient_coin_costs", "false"));
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§6Ancient coin economy",
                "§7Staff pricing for LM paid features");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, ProgressionGuiApi.linesForPage(player, "economy"), 4));
        CnpcGuiSupport.button(gui, 20,
                staffFree ? CnpcGuiStyle.toggleOn("Staff free costs") : CnpcGuiStyle.toggleOff("Staff free costs"),
                CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                        player,
                        () -> ProgressionGuiApi.handleDo(player, "toggle_staff_free_coins",
                                staffFree ? "off" : "on", "economy"),
                        () -> open(player, "economy")));
        row += CnpcGuiSupport.ROW_STEP + 8;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "main"), "§7« Back");
    }

    private static String sectionTitle(String page) {
        return switch (page) {
            case "skills" -> CnpcGuiStyle.subPage("§d", "Progression", "Skills");
            case "tp" -> CnpcGuiStyle.subPage("§d", "Progression", "TP gains");
            case "race" -> CnpcGuiStyle.subPage("§d", "Progression", "Race and form");
            case "combat" -> CnpcGuiStyle.subPage("§d", "Progression", "Combat");
            case "end" -> CnpcGuiStyle.subPage("§d", "Progression", "End");
            case "fabled" -> CnpcGuiStyle.subPage("§d", "Progression", "Fabled bridges");
            case "utility" -> CnpcGuiStyle.subPage("§d", "Progression", "Utility");
            case "status" -> CnpcGuiStyle.subPage("§d", "Progression", "Status");
            default -> "§dProgression";
        };
    }

}
