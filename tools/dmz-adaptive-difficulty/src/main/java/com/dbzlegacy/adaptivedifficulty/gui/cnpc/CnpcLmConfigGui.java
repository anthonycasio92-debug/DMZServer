package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.config.editor.ConfigEditor;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IScroll;
import noppes.npcs.api.gui.ITextField;

/** Staff editor for live config files. Rows are public fields, not a hand-written list. */
public final class CnpcLmConfigGui {
    private static final int ID_TEXT = 190;
    private static final int ID_TEXT_VALUE = 191;
    private static final int ID_KEY_LABEL = 186;
    private static final int ID_VALUE_LABEL = 187;

    private CnpcLmConfigGui() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        CnpcUltraPreview.leave(player);
        if (!StaffAccess.isStaff(player)) {
            CnpcGuiSupport.denyToHub(player, CnpcMenuFeedback.NOTICE_BODY + "Staff only.");
            return;
        }
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        if (raw.startsWith("edit:")) {
            paintEdit(player, raw);
            return;
        }
        if (raw.startsWith("add:")) {
            paintAdd(player, raw);
            return;
        }
        if (raw.startsWith("browse:")) {
            paintBrowse(player, raw);
            return;
        }
        paintHome(player);
    }

    private static void paintHome(ServerPlayer player) {
        List<ConfigEditor.Root> roots = ConfigEditor.roots();
        int rows = (roots.size() + 1) / 2;
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CONFIG, CnpcGuiSupport.W,
                CnpcGuiSupport.window(260 + rows * CnpcGuiSupport.ROW_STEP),
                (pl, gui) -> {
                    int infoY = CnpcGuiSupport.paintHeader(pl, gui, "§6Config",
                            "§7Settings from the live config files");
                    int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                            "§7A new field shows up here on its own.",
                            "§7Player records in rivalry, sparring, and rival progression stay in their files."
                    ), CnpcGuiStyle.INFO_INLINE_MAX));
                    row = CnpcGuiSupport.paintSectionTag(gui, CnpcGuiSupport.ID_INLINE_NOTE, row, "Files");
                    for (int i = 0; i < roots.size(); i++) {
                        ConfigEditor.Root root = roots.get(i);
                        int col = i % 2 == 0 ? CnpcGuiSupport.COL_L : CnpcGuiSupport.COL_R;
                        CnpcGuiSupport.button(gui, CnpcGuiSupport.ID_GRID_BASE + i, "§6" + root.title(), col, row,
                                () -> open(player, browsePage(root.id(), "")));
                        if (i % 2 == 1) {
                            row += CnpcGuiSupport.ROW_STEP;
                        }
                    }
                    if (roots.size() % 2 == 1) {
                        row += CnpcGuiSupport.ROW_STEP;
                    }
                    CnpcGuiSupport.navSubmenu(player, gui, row,
                            () -> CnpcLmAdminGui.open(player, "main"), "§7« Back");
                });
    }

    private static void paintBrowse(ServerPlayer player, String page) {
        String rootId = part(page, 1);
        String path = part(page, 2);
        ConfigEditor.Root root = ConfigEditor.root(rootId);
        if (root == null) {
            open(player, "main");
            return;
        }
        List<ConfigEditor.Entry> entries = ConfigEditor.children(rootId, path);
        ConfigEditor.AddMode addMode = ConfigEditor.addMode(rootId, path);
        int actionRows = addMode == ConfigEditor.AddMode.NONE ? 1 : 2;
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CONFIG, CnpcGuiSupport.W,
                CnpcGuiSupport.window(420), (pl, gui) -> {
                    String subtitle = path == null || path.isEmpty()
                            ? root.fileName()
                            : path;
                    int infoY = CnpcGuiSupport.paintHeader(pl, gui, "§6" + root.title(), "§7" + subtitle);
                    List<String> notes = new ArrayList<>();
                    int hidden = ConfigEditor.hiddenCount(rootId, path);
                    if (hidden > 0) {
                        notes.add("§7Showing the first 200. Add an entry to reach a name that is not listed.");
                    }
                    if (entries.isEmpty()) {
                        notes.add("§7Nothing is listed here yet.");
                    }
                    int listY = notes.isEmpty()
                            ? infoY
                            : CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(
                                    gui, infoY, notes, CnpcGuiStyle.INFO_INLINE_MAX));
                    String[] lines = entries.stream().map(ConfigEditor.Entry::line).toArray(String[]::new);
                    IScroll scroll = lines.length == 0
                            ? null
                            : CnpcGuiSupport.scrollPickList(gui, listY, actionRows + 1, lines);
                    int row = scroll == null
                            ? listY + 8
                            : CnpcGuiSupport.navRowAfterScroll(
                                    CnpcGuiSupport.pickListBandY(listY, actionRows + 1, gui, lines.length),
                                    CnpcGuiSupport.listScrollHeight(
                                            gui,
                                            CnpcGuiSupport.pickListBandY(listY, actionRows + 1, gui, lines.length),
                                            actionRows + 1));
                    if (scroll != null) {
                        CnpcGuiSupport.selectionButton(player, gui, 30, "§eChange this",
                                CnpcGuiSupport.COL_L, row, CnpcGuiSupport.BTN_W,
                                () -> selectedLine(scroll),
                                line -> change(player, rootId, path, line),
                                () -> open(player, page));
                    }
                    if (addMode != ConfigEditor.AddMode.NONE) {
                        CnpcGuiSupport.button(gui, 31, "§aAdd entry", CnpcGuiSupport.COL_R, row,
                                () -> open(player, "add:" + rootId + ":" + path));
                        row += CnpcGuiSupport.ROW_STEP;
                        if (scroll != null) {
                            CnpcGuiSupport.selectionButton(player, gui, 32, "§cRemove this",
                                    CnpcGuiSupport.COL_L, row, CnpcGuiSupport.BTN_W,
                                    () -> selectedLine(scroll),
                                    line -> remove(player, rootId, path, line, page),
                                    () -> open(player, page));
                        }
                    }
                    row += CnpcGuiSupport.ROW_STEP;
                    CnpcGuiSupport.navSubmenu(player, gui, row,
                            () -> open(player, backPage(rootId, path)), "§7« Back");
                });
    }

    private static void paintEdit(ServerPlayer player, String page) {
        String rootId = part(page, 1);
        String path = part(page, 2);
        ConfigEditor.Root root = ConfigEditor.root(rootId);
        if (root == null || path.isEmpty()) {
            open(player, "main");
            return;
        }
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CONFIG, CnpcGuiSupport.W,
                CnpcGuiSupport.window(300), (pl, gui) -> {
                    String name = leaf(path);
                    int infoY = CnpcGuiSupport.paintHeader(pl, gui, "§6" + name, "§7" + root.fileName());
                    int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(
                            gui, infoY, ConfigEditor.editLines(rootId, path), CnpcGuiStyle.INFO_INLINE_MAX));
                    ITextField field = gui.addTextField(ID_TEXT, CnpcGuiSupport.M, row,
                            CnpcGuiSupport.textBandWidth(), CnpcGuiSupport.BTN_H);
                    field.setText(ConfigEditor.currentText(rootId, path));
                    row += CnpcGuiSupport.ROW_STEP + 4;
                    gui.addButton(20, "§aSave", CnpcGuiSupport.COL_L, row, CnpcGuiSupport.BTN_W, CnpcGuiSupport.BTN_H)
                            .setOnPress((g, btn) -> {
                                String typed = text(g, ID_TEXT);
                                CnpcGuiSupport.afterGuiClosed(g, () -> finish(
                                        player, ConfigEditor.apply(rootId, path, typed),
                                        "Saved " + name + ".",
                                        backPage(rootId, path)));
                            });
                    row += CnpcGuiSupport.ROW_STEP;
                    CnpcGuiSupport.navSubmenu(player, gui, row,
                            () -> open(player, backPage(rootId, path)), "§7« Back");
                });
    }

    private static void paintAdd(ServerPlayer player, String page) {
        String rootId = part(page, 1);
        String path = part(page, 2);
        ConfigEditor.Root root = ConfigEditor.root(rootId);
        ConfigEditor.AddMode mode = ConfigEditor.addMode(rootId, path);
        if (root == null || mode == ConfigEditor.AddMode.NONE) {
            open(player, browsePage(rootId, path));
            return;
        }
        boolean keyed = mode == ConfigEditor.AddMode.KEY_AND_VALUE;
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CONFIG, CnpcGuiSupport.W,
                CnpcGuiSupport.window(keyed ? 340 : 280), (pl, gui) -> {
                    int infoY = CnpcGuiSupport.paintHeader(pl, gui, "§6Add entry", "§7" + root.fileName());
                    int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                            keyed
                                    ? "§7Type the name, then the value. Saving writes the file."
                                    : "§7Type the new entry. Saving writes the file."
                    ), CnpcGuiStyle.INFO_INLINE_MAX));
                    if (keyed) {
                        gui.addLabel(ID_KEY_LABEL, "§7Name", CnpcGuiSupport.M, row,
                                CnpcGuiSupport.textBandWidth(), 12);
                        row += 14;
                        gui.addTextField(ID_TEXT, CnpcGuiSupport.M, row,
                                CnpcGuiSupport.textBandWidth(), CnpcGuiSupport.BTN_H);
                        row += CnpcGuiSupport.ROW_STEP;
                        gui.addLabel(ID_VALUE_LABEL, "§7Value", CnpcGuiSupport.M, row,
                                CnpcGuiSupport.textBandWidth(), 12);
                        row += 14;
                    }
                    gui.addTextField(keyed ? ID_TEXT_VALUE : ID_TEXT, CnpcGuiSupport.M, row,
                            CnpcGuiSupport.textBandWidth(), CnpcGuiSupport.BTN_H);
                    row += CnpcGuiSupport.ROW_STEP + 4;
                    gui.addButton(20, "§aSave", CnpcGuiSupport.COL_L, row, CnpcGuiSupport.BTN_W, CnpcGuiSupport.BTN_H)
                            .setOnPress((g, btn) -> {
                                String key = keyed ? text(g, ID_TEXT) : "";
                                String value = text(g, keyed ? ID_TEXT_VALUE : ID_TEXT);
                                CnpcGuiSupport.afterGuiClosed(g, () -> finish(
                                        player, ConfigEditor.add(rootId, path, key, value),
                                        "Added.",
                                        browsePage(rootId, path)));
                            });
                    row += CnpcGuiSupport.ROW_STEP;
                    CnpcGuiSupport.navSubmenu(player, gui, row,
                            () -> open(player, browsePage(rootId, path)), "§7« Back");
                });
    }

    private static void change(ServerPlayer player, String rootId, String path, String line) {
        ConfigEditor.Entry entry = match(rootId, path, line);
        if (entry == null) {
            CnpcGuiSupport.pushMenuMessage(player, CnpcMenuFeedback.NOTICE_BODY + "Select a row first.");
            open(player, browsePage(rootId, path));
            return;
        }
        switch (entry.kind()) {
            case BOOLEAN -> finish(player, ConfigEditor.toggle(rootId, entry.path()),
                    leaf(entry.path()) + " updated.", browsePage(rootId, path));
            case NUMBER, TEXT, ENUM -> open(player, "edit:" + rootId + ":" + entry.path());
            case GROUP, LIST, MAP -> open(player, browsePage(rootId, entry.path()));
            default -> finish(player, "That setting can't be edited here.", null, browsePage(rootId, path));
        }
    }

    private static void remove(ServerPlayer player, String rootId, String path, String line, String page) {
        ConfigEditor.Entry entry = match(rootId, path, line);
        if (entry == null) {
            finish(player, "Select a row first.", null, page);
            return;
        }
        finish(player, ConfigEditor.remove(rootId, entry.path()), "Removed.", page);
    }

    private static void finish(ServerPlayer player, String error, String ok, String page) {
        if (error != null) {
            CnpcGuiSupport.pushMenuMessage(player, CnpcMenuFeedback.NOTICE_BODY + error);
        } else if (ok != null) {
            CnpcGuiSupport.pushMenuMessage(player, CnpcMenuFeedback.NOTICE_BODY + ok);
        }
        open(player, page);
    }

    private static ConfigEditor.Entry match(String rootId, String path, String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String plain = line.replaceAll("§.", "");
        for (ConfigEditor.Entry entry : ConfigEditor.children(rootId, path)) {
            if (line.equals(entry.line()) || plain.equals(entry.line().replaceAll("§.", ""))) {
                return entry;
            }
        }
        return null;
    }

    private static String selectedLine(IScroll scroll) {
        if (scroll == null) {
            return null;
        }
        String[] list = scroll.getList();
        int[] selected = scroll.getSelection();
        if (list == null || selected == null || selected.length == 0) {
            return null;
        }
        int index = selected[0];
        if (index < 0 || index >= list.length) {
            return null;
        }
        return list[index];
    }

    private static String text(ICustomGui gui, int id) {
        if (gui == null || !(gui.getComponent(id) instanceof ITextField field)) {
            return "";
        }
        String value = field.getText();
        return value == null ? "" : value;
    }

    private static String browsePage(String rootId, String path) {
        return "browse:" + rootId + ":" + (path == null ? "" : path);
    }

    private static String backPage(String rootId, String path) {
        if (path == null || path.isEmpty()) {
            return "main";
        }
        return browsePage(rootId, ConfigEditor.parentPath(path));
    }

    private static String leaf(String path) {
        if (path == null || path.isEmpty()) {
            return "Setting";
        }
        int bracket = path.lastIndexOf('[');
        if (path.endsWith("]") && bracket >= 0) {
            return path.substring(bracket + 1, path.length() - 1);
        }
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(dot + 1);
    }

    /** {@code kind:root:path}. The path keeps its capitals because it contains a colon. */
    private static String part(String page, int index) {
        String[] bits = page.split(":", 3);
        if (index < 0 || index >= bits.length) {
            return "";
        }
        return bits[index];
    }
}
