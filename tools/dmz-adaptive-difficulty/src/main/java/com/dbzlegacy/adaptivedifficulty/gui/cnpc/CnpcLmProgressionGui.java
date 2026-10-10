package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionModuleCatalog;
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
        CnpcUltraStyle.withAccent(CnpcUltraStyle.ACCENT_PROGRESSION, () -> openProgression(player, page));
    }

    private static void openProgression(ServerPlayer player, String page) {
        String p = normalizePage(page);
        if (requiresStaff(p) && !StaffAccess.isStaff(player)) {
            CnpcGuiSupport.denyToHub(player,
                    CnpcMenuFeedback.NOTICE_BODY + "Staff only — that progression page needs staff access.");
            return;
        }
        int h = switch (p) {
            case "android_convert", "android_remove" -> CnpcGuiSupport.window(320);
            case "skills", "tp", "race", "combat", "end", "utility", "status", "shop" ->
                    sectionHeight(player, p);
            case "flags" -> flagsPageHeight();
            default -> CnpcGuiSupport.window("main".equals(p) ? 520 : H_MAIN);
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
            case "admin", "flags", "disable", "flags_fabled", "fabled_flags" -> "flags";
            case "ancient_coins", "coins" -> "economy";
            default -> p;
        };
    }

    private static boolean requiresStaff(String page) {
        // Players may only use Remove Android. Android tools and convert stay staff-only.
        return !"android_remove".equals(page);
    }

    private static void paint(ServerPlayer player, ICustomGui gui, String page) {
        switch (page) {
            case "main" -> paintMainHub(player, gui);
            case "skills", "tp", "race", "combat", "end", "utility", "status", "shop" ->
                    paintSection(player, gui, page);
            case "flags" -> paintAllFlags(player, gui);
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
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcUltraStyle.ACCENT + "Progression",
                staff ? CnpcUltraStyle.SUBTITLE + "Staff: Global TP boost · Android tools · Ancient Coins"
                        : CnpcUltraStyle.SUBTITLE + "Module status and progression tools");
        List<String> info = new ArrayList<>(ProgressionGuiApi.linesForPage(player, "main"));
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, info, CnpcGuiStyle.INFO_INLINE_MAX));
        row = CnpcGuiSupport.paintSectionTag(gui, CnpcGuiSupport.ID_INLINE_NOTE, row, CnpcUltraStyle.ACCENT + "Modules");
        String[] moduleIds = {"skills", "tp", "race", "combat", "end", "utility", "status", "shop"};
        String[] moduleNames = {"Skills", "TP Gains", "Race & Form", "Combat", "End", "Utility", "Status", "Shop"};
        CnpcGuiLayout.GridButton[] modules = new CnpcGuiLayout.GridButton[moduleIds.length];
        for (int i = 0; i < moduleIds.length; i++) {
            String id = moduleIds[i];
            modules[i] = CnpcGuiLayout.GridButton.run(CnpcUltraStyle.BODY + moduleNames[i], () -> open(player, id));
        }
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(
                player, gui, row, CnpcGuiSupport.ID_GRID_BASE, modules, () -> open(player, "main"));
        row += 4;
        if (staff) {
            placeRow(gui, player, row, 21, CnpcUltraStyle.ACCENT + "TP boost", CnpcGuiSupport.COL_L, () -> open(player, "boost_panel"));
            placeRow(gui, player, row, 22, CnpcUltraStyle.ACCENT + "Ancient coins", CnpcGuiSupport.COL_R, () -> open(player, "economy"));
            row += CnpcGuiSupport.ROW_STEP;
            if (ProgressionConfig.androidConversion()) {
                placeRow(gui, player, row, 23, CnpcUltraStyle.ACCENT + "Android tools", CnpcGuiSupport.COL_L,
                        () -> open(player, "android_panel"));
            }
            placeRow(gui, player, row, 29, CnpcUltraStyle.ACCENT + "All flags", CnpcGuiSupport.COL_R, () -> open(player, "flags"));
            row += CnpcGuiSupport.ROW_STEP;
        }
        placeRow(gui, player, row, 24, CnpcUltraStyle.ACCENT + "Prestige", CnpcGuiSupport.COL_L,
                () -> CnpcLmGui.open(player, "prestige", "main"));
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
        String hint = staff ? CnpcGuiStyle.HINT_TOGGLE_STAFF : CnpcGuiStyle.HINT_READ_ONLY;
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, hint);
        int row = CnpcGuiSupport.bodyBelowHeader(infoY);
        List<String> sectionLines = ProgressionGuiApi.linesForPage(player, page);
        row = sectionLines.size() > 2
                ? CnpcGuiSupport.paintLongReadOnlyBody(gui, row, sectionLines)
                : CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, row, sectionLines, CnpcGuiStyle.INFO_INLINE_MAX));

        Map<String, String> ph = ProgressionGuiApi.placeholders(player);
        List<CnpcGuiLayout.GridButton> grid = new ArrayList<>();
        if (staff) {
            grid.addAll(flagToggleButtons(player, ph, page, ProgressionGuiApi.flagKeysForPage(page)));
        }
        if ("tp".equals(page)) {
            if (staff) {
                grid.add(CnpcGuiLayout.GridButton.run(CnpcUltraStyle.ACCENT + "Global TP boost", () -> open(player, "boost_panel")));
            } else {
                grid.add(CnpcGuiLayout.GridButton.disabled(
                        CnpcUltraStyle.ACCENT + "Global TP boost " + CnpcUltraStyle.DIM + "· " + CnpcGuiSupport.flagOnOff(ph, "boost")));
            }
        }
        if (staff && "race".equals(page) && ProgressionConfig.androidConversion()) {
            grid.add(CnpcGuiLayout.GridButton.run(CnpcUltraStyle.ACCENT + "Android tools", () -> open(player, "android_panel")));
        }
        if ("shop".equals(page)) {
            grid.add(CnpcGuiLayout.GridButton.run(CnpcUltraStyle.ACCENT + "Open Prestige",
                    () -> CnpcLmGui.open(player, "prestige", "main")));
            grid.add(CnpcGuiLayout.GridButton.run(CnpcUltraStyle.INFO + "Open Skill Check",
                    () -> CnpcLmGui.open(player, "skillcheck", "main")));
        }
        if (staff && ProgressionGuiApi.flagKeysForPage(page).length > 0) {
            String sectionPage = page;
            grid.add(CnpcGuiLayout.GridButton.action(
                    CnpcUltraStyle.DIM + "Module sources",
                    () -> ProgressionGuiApi.handleDo(player, "module_doc", "", sectionPage),
                    () -> open(player, page)));
        }
        if (grid.isEmpty()) {
            CnpcGuiSupport.navSubmenu(player, gui, row + 8, () -> open(player, "main"), CnpcUltraStyle.BACK);
            return;
        }
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(
                player, gui, row + 4, 40, grid.toArray(CnpcGuiLayout.GridButton[]::new),
                () -> open(player, page));
        row += 8;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "main"), CnpcUltraStyle.BACK);
    }

    private static List<CnpcGuiLayout.GridButton> flagToggleButtons(
            ServerPlayer player, Map<String, String> ph, String returnPage, String... keys) {
        List<CnpcGuiLayout.GridButton> out = new ArrayList<>();
        if (keys == null) {
            return out;
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            if ("android".equals(key) && "race".equals(returnPage)) {
                continue;
            }
            if ("boost".equals(key) && "tp".equals(returnPage)) {
                continue;
            }
            boolean on = "true".equalsIgnoreCase(ph.getOrDefault("flag_" + key, "false"));
            String label = on
                    ? CnpcGuiStyle.toggleOn(ProgressionModuleCatalog.displayTitle(key))
                    : CnpcGuiStyle.toggleOff(ProgressionModuleCatalog.displayTitle(key));
            String flagKey = key;
            out.add(CnpcGuiLayout.GridButton.action(
                    label,
                    () -> ProgressionGuiApi.handleDo(player, "flag", flagKey, returnPage),
                    () -> open(player, returnPage)));
        }
        return out;
    }

    private static void paintAllFlags(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Progression", "All flags"), CnpcGuiStyle.HINT_TOGGLE_STAFF);
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                CnpcUltraStyle.SUBTITLE + "Turn entire progression modules on or off.",
                CnpcUltraStyle.DIM + "Category pages have the same toggles plus tools."
        ), CnpcGuiStyle.INFO_INLINE_MAX));
        Map<String, String> ph = ProgressionGuiApi.placeholders(player);
        String[] keys = ProgressionModuleCatalog.allFlagKeys();
        List<CnpcGuiLayout.GridButton> grid = flagToggleButtons(player, ph, "flags", keys);
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(
                player, gui, row, CnpcGuiSupport.ID_GRID_BASE, grid.toArray(CnpcGuiLayout.GridButton[]::new),
                () -> open(player, "flags"));
        row += 8;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "main"), CnpcUltraStyle.BACK);
    }

    private static int sectionHeight(ServerPlayer player, String page) {
        boolean staff = player != null && StaffAccess.isStaff(player);
        int tools = 0;
        if ("tp".equals(page)) {
            tools = 1;
        }
        if ("shop".equals(page)) {
            tools += 2;
        }
        if ("race".equals(page) && ProgressionConfig.androidConversion()) {
            tools++;
        }
        int flags = staff ? ProgressionGuiApi.flagKeysForPage(page).length : 0;
        int extra = staff && flags > 0 ? 1 : 0;
        int gridItems = flags + tools + extra;
        return CnpcGuiSupport.suggestHeight(280 + ((gridItems + 1) / 2) * CnpcGuiSupport.ROW_STEP + 96);
    }

    private static int flagsPageHeight() {
        int n = ProgressionModuleCatalog.allFlagKeys().length;
        return CnpcGuiSupport.suggestHeight(280 + ((n + 1) / 2) * CnpcGuiSupport.ROW_STEP + 48);
    }

    private static void paintBoostPanel(ServerPlayer player, ICustomGui gui) {
        Map<String, String> ph = ProgressionGuiApi.placeholders(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Progression", "TP boost"), CnpcUltraStyle.SUBTITLE + "Timed world TP multiplier");
        List<String> lines = new ArrayList<>();
        lines.add(ph.getOrDefault("boost", CnpcUltraStyle.SUBTITLE + "Global TP boost: " + CnpcUltraStyle.DANGER + "OFF"));
        lines.add(CnpcUltraStyle.DIM + "Presets start a boost · End stops it");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, CnpcGuiStyle.INFO_INLINE_MAX));

        int b = CnpcGuiSupport.ID_BOOST_PRESET_BASE;
        row = boostPreset(gui, player, row, b, CnpcUltraStyle.INFO + "1.25× · 30m", CnpcGuiSupport.COL_L, "1.25:30");
        boostPreset(gui, player, row, b + 1, CnpcUltraStyle.INFO + "1.5× · 30m", CnpcGuiSupport.COL_R, "1.5:30");
        row += CnpcGuiSupport.ROW_STEP;
        boostPreset(gui, player, row, b + 2, CnpcUltraStyle.INFO + "2× · 30m", CnpcGuiSupport.COL_L, "2:30");
        boostPreset(gui, player, row, b + 3, CnpcUltraStyle.INFO + "2× · 60m", CnpcGuiSupport.COL_R, "2:60");
        row += CnpcGuiSupport.ROW_STEP;
        boostPreset(gui, player, row, b + 4, CnpcUltraStyle.INFO + "3× · 30m", CnpcGuiSupport.COL_L, "3:30");
        CnpcGuiSupport.button(gui, b + 5, CnpcUltraStyle.DANGER + "End boost", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> ProgressionGuiApi.handleDo(player, "boost", "end", "boost_panel"),
                () -> open(player, "boost_panel")));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "main"), CnpcUltraStyle.BACK);
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
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Progression", "Android tools"),
                CnpcUltraStyle.SUBTITLE + "Android convert · Remove Android");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                CnpcUltraStyle.SUBTITLE + "Convert keeps race · unlocks Android forms",
                CnpcUltraStyle.SUBTITLE + "Eligible races:",
                CnpcUltraStyle.BODY + com.dbzlegacy.adaptivedifficulty.progression.race.AndroidConversion.eligibleRaceHint(),
                CnpcUltraStyle.SUBTITLE + "Remove restores prior forms (confirm within 10s)"
        ), CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 60, CnpcUltraStyle.CONFIRM + "Convert to Android…", CnpcGuiSupport.COL_L, row,
                () -> open(player, "android_convert"));
        CnpcGuiSupport.button(gui, 61, CnpcUltraStyle.DANGER + "Remove Android…", CnpcGuiSupport.COL_R, row,
                () -> open(player, "android_remove"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "main"), CnpcUltraStyle.BACK);
    }

    private static void paintAndroidConvert(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = CnpcGuiSupport.target(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Progression", "Android convert"),
                CnpcGuiStyle.HINT_DOUBLE_CLICK_PLAYER);
        boolean self = subject.m_20148_().equals(player.m_20148_());
        String who = subject.m_7755_().getString();
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                self ? CnpcUltraStyle.SUBTITLE + "This converts you." : CnpcUltraStyle.SUBTITLE + "This converts " + CnpcUltraStyle.BODY + who,
                CnpcUltraStyle.DANGER + "Super forms and Legendary forms are deleted."
        ), CnpcGuiStyle.INFO_INLINE_MAX));
        String convertLabel = self
                ? CnpcUltraStyle.CONFIRM + "Convert yourself"
                : CnpcUltraStyle.CONFIRM + "Convert " + who;
        CnpcGuiSupport.button(gui, 62, convertLabel, CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> ProgressionGuiApi.handleDo(player, "android", subject.m_7755_().getString(), "android_convert"),
                () -> open(player, "android_convert")));
        row += CnpcGuiSupport.ROW_STEP;
        if (StaffAccess.isStaff(player)) {
            row = paintNameScroll(player, gui, row, "android", "android_convert");
        }
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "android_panel"), CnpcUltraStyle.BACK);
    }

    private static void paintAndroidRemove(ServerPlayer player, ICustomGui gui) {
        boolean staff = StaffAccess.isStaff(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Progression", "Remove Android"),
                CnpcUltraStyle.SUBTITLE + "Two-step confirm within 10 seconds");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY, List.of(
                CnpcUltraStyle.SUBTITLE + "Removes Android forms and restores what you had.",
                CnpcUltraStyle.INFO + "Click again within 10 seconds to confirm."
        ), CnpcGuiStyle.INFO_LIST_HEADER_MAX));
        CnpcGuiSupport.button(gui, 63, CnpcUltraStyle.DANGER + "Remove on yourself", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> ProgressionGuiApi.handleDo(player, "android_remove", "",
                        "android_remove"),
                () -> open(player, "android_remove")));
        row += CnpcGuiSupport.ROW_STEP;
        if (staff) {
            row = paintNameScroll(player, gui, row, "android_remove", "android_remove");
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "android_panel"), CnpcUltraStyle.BACK);
        } else {
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> CnpcLmHubGui.open(player, "main"), CnpcUltraStyle.BACK);
        }
    }

    /** @return Y row for footer after list (or after empty label). */
    private static int paintNameScroll(
            ServerPlayer player, ICustomGui gui, int y, String action, String returnPage) {
        List<String> names = RivalGuiApi.onlinePlayerNames(CnpcGuiSupport.target(player));
        if (names.isEmpty()) {
            gui.addLabel(70, CnpcUltraStyle.SUBTITLE + "Nobody else is online right now — try again when other players are on.", CnpcGuiSupport.M, y + 8, CnpcGuiSupport.textBandWidth(), 14);
            return y + 28;
        }
        int rowsBelow = 1;
        int bandY = CnpcGuiSupport.pickListBandY(y, rowsBelow, gui, names.size());
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
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Progression", "Ancient coins"),
                CnpcUltraStyle.SUBTITLE + "Staff pricing for LM paid features");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY,
                ProgressionGuiApi.linesForPage(player, "economy"), CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20,
                staffFree ? CnpcGuiStyle.toggleOn("Staff free costs") : CnpcGuiStyle.toggleOff("Staff free costs"),
                CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                        player,
                        () -> ProgressionGuiApi.handleDo(player, "toggle_staff_free_coins",
                                staffFree ? "off" : "on", "economy"),
                        () -> open(player, "economy")));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "main"), CnpcUltraStyle.BACK);
    }

    private static String sectionTitle(String page) {
        return switch (page) {
            case "skills" -> CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Progression", "Skills");
            case "tp" -> CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Progression", "TP Gains");
            case "race" -> CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Progression", "Race & Form");
            case "combat" -> CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Progression", "Combat");
            case "end" -> CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Progression", "End");
            case "utility" -> CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Progression", "Utility");
            case "status" -> CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Progression", "Status");
            case "shop" -> CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Progression", "Shop");
            default -> CnpcUltraStyle.ACCENT + "Progression";
        };
    }

}
