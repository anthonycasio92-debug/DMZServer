package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.config.editor.ConfigEditor;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IScroll;
import noppes.npcs.api.gui.ITextField;

/**
 * Staff field editor. A change is shown as old → new, then the file is saved and reloaded.
 */
public final class CnpcConfigEditor {
    private static final int ID_TEXT = 190;
    private static final int ID_TEXT_VALUE = 191;
    private static final int ID_KEY_LABEL = 186;
    private static final int ID_VALUE_LABEL = 187;

    private CnpcConfigEditor() {}

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
        if (raw.startsWith("confirm:")) {
            paintConfirm(player, raw);
            return;
        }
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
                    int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcUltraStyle.ACCENT + "Config",
                            CnpcUltraStyle.SUBTITLE + "Settings from the live config files");
                    int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                            CnpcUltraStyle.SUBTITLE + "A new field shows up here on its own.",
                            CnpcUltraStyle.SUBTITLE + "Saving asks you to confirm, writes the file, and reloads it.",
                            CnpcUltraStyle.SUBTITLE + "Rival and sparring records stay in their own files."
                    ), CnpcGuiStyle.INFO_INLINE_MAX));
                    row = CnpcGuiSupport.paintSectionTag(gui, CnpcGuiSupport.ID_INLINE_NOTE, row, "Files");
                    for (int i = 0; i < roots.size(); i++) {
                        ConfigEditor.Root root = roots.get(i);
                        int col = i % 2 == 0 ? CnpcGuiSupport.COL_L : CnpcGuiSupport.COL_R;
                        CnpcGuiSupport.button(gui, CnpcGuiSupport.ID_GRID_BASE + i, CnpcUltraStyle.ACCENT + root.title(), col, row,
                                () -> open(player, browsePage(root.id(), "")));
                        if (i % 2 == 1) {
                            row += CnpcGuiSupport.ROW_STEP;
                        }
                    }
                    if (roots.size() % 2 == 1) {
                        row += CnpcGuiSupport.ROW_STEP;
                    }
                    CnpcGuiSupport.navSubmenu(player, gui, row,
                            () -> CnpcLmAdminGui.open(player, "main"), CnpcUltraStyle.BACK);
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
                            : ConfigEditor.displayName(leaf(path));
                    int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcUltraStyle.ACCENT + root.title(),
                            CnpcUltraStyle.SUBTITLE + subtitle);
                    List<String> notes = new ArrayList<>();
                    int hidden = ConfigEditor.hiddenCount(rootId, path);
                    if (hidden > 0) {
                        notes.add(CnpcUltraStyle.SUBTITLE + "Showing the first 200. Add an entry to reach a name that is not listed.");
                    }
                    if (entries.isEmpty()) {
                        notes.add(CnpcUltraStyle.SUBTITLE + "Nothing is listed here yet.");
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
                        CnpcGuiSupport.selectionButton(player, gui, 30, CnpcUltraStyle.INFO + "Change this",
                                CnpcGuiSupport.COL_L, row, CnpcGuiSupport.BTN_W,
                                () -> selectedLine(scroll),
                                line -> change(player, rootId, path, line),
                                () -> open(player, page));
                    }
                    if (addMode != ConfigEditor.AddMode.NONE) {
                        CnpcGuiSupport.button(gui, 31, CnpcUltraStyle.CONFIRM + "Add entry", CnpcGuiSupport.COL_R, row,
                                () -> open(player, "add:" + rootId + ":" + path));
                        row += CnpcGuiSupport.ROW_STEP;
                        if (scroll != null) {
                            CnpcGuiSupport.selectionButton(player, gui, 32, CnpcUltraStyle.DANGER + "Remove this",
                                    CnpcGuiSupport.COL_L, row, CnpcGuiSupport.BTN_W,
                                    () -> selectedLine(scroll),
                                    line -> remove(player, rootId, path, line, page),
                                    () -> open(player, page));
                        }
                    }
                    row += CnpcGuiSupport.ROW_STEP;
                    CnpcGuiSupport.navSubmenu(player, gui, row,
                            () -> open(player, backPage(rootId, path)), CnpcUltraStyle.BACK);
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
        ConfigEditor.Kind kind = ConfigEditor.kindOf(rootId, path);
        boolean number = kind == ConfigEditor.Kind.NUMBER;
        boolean enumeration = kind == ConfigEditor.Kind.ENUM;
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CONFIG, CnpcGuiSupport.W,
                CnpcGuiSupport.window(number || enumeration ? 360 : 300), (pl, gui) -> {
                    String name = ConfigEditor.displayName(leaf(path));
                    int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcUltraStyle.ACCENT + name,
                            CnpcUltraStyle.SUBTITLE + root.fileName());
                    int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(
                            gui, infoY, colored(ConfigEditor.editLines(rootId, path)), CnpcGuiStyle.INFO_INLINE_MAX));
                    ITextField field = gui.addTextField(ID_TEXT, CnpcGuiSupport.M, row,
                            CnpcGuiSupport.textBandWidth(), CnpcGuiSupport.BTN_H);
                    field.setText(ConfigEditor.currentText(rootId, path));
                    row += CnpcGuiSupport.ROW_STEP + 4;
                    if (number) {
                        CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.SUBTITLE + "−", CnpcGuiSupport.COL_L, row,
                                () -> stageNumber(player, rootId, path, -1));
                        CnpcGuiSupport.button(gui, 22, CnpcUltraStyle.SUBTITLE + "+", CnpcGuiSupport.COL_R, row,
                                () -> stageNumber(player, rootId, path, 1));
                        row += CnpcGuiSupport.ROW_STEP;
                    } else if (enumeration) {
                        CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.INFO + "Next value", CnpcGuiSupport.COL_L, row,
                                () -> stageEnum(player, rootId, path));
                        row += CnpcGuiSupport.ROW_STEP;
                    }
                    gui.addButton(20, CnpcUltraStyle.CONFIRM + "Review change", CnpcGuiSupport.COL_L, row,
                                    CnpcGuiSupport.BTN_W, CnpcGuiSupport.BTN_H)
                            .setOnPress((g, btn) -> {
                                String typed = text(g, ID_TEXT);
                                CnpcGuiSupport.afterGuiClosed(g, () -> stageSet(player, rootId, path, typed));
                            });
                    row += CnpcGuiSupport.ROW_STEP;
                    CnpcGuiSupport.navSubmenu(player, gui, row,
                            () -> open(player, backPage(rootId, path)), CnpcUltraStyle.BACK);
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
                    int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcUltraStyle.ACCENT + "Add entry",
                            CnpcUltraStyle.SUBTITLE + root.fileName());
                    int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                            keyed
                                    ? CnpcUltraStyle.SUBTITLE + "Type the name, then the value. You'll confirm before it is saved."
                                    : CnpcUltraStyle.SUBTITLE + "Type the new entry. You'll confirm before it is saved."
                    ), CnpcGuiStyle.INFO_INLINE_MAX));
                    if (keyed) {
                        gui.addLabel(ID_KEY_LABEL, CnpcUltraStyle.SUBTITLE + "Name", CnpcGuiSupport.M, row,
                                CnpcGuiSupport.textBandWidth(), 12);
                        row += 14;
                        gui.addTextField(ID_TEXT, CnpcGuiSupport.M, row,
                                CnpcGuiSupport.textBandWidth(), CnpcGuiSupport.BTN_H);
                        row += CnpcGuiSupport.ROW_STEP;
                        gui.addLabel(ID_VALUE_LABEL, CnpcUltraStyle.SUBTITLE + "Value", CnpcGuiSupport.M, row,
                                CnpcGuiSupport.textBandWidth(), 12);
                        row += 14;
                    }
                    gui.addTextField(keyed ? ID_TEXT_VALUE : ID_TEXT, CnpcGuiSupport.M, row,
                            CnpcGuiSupport.textBandWidth(), CnpcGuiSupport.BTN_H);
                    row += CnpcGuiSupport.ROW_STEP + 4;
                    gui.addButton(20, CnpcUltraStyle.CONFIRM + "Review change", CnpcGuiSupport.COL_L, row,
                                    CnpcGuiSupport.BTN_W, CnpcGuiSupport.BTN_H)
                            .setOnPress((g, btn) -> {
                                String key = keyed ? text(g, ID_TEXT) : "";
                                String value = text(g, keyed ? ID_TEXT_VALUE : ID_TEXT);
                                CnpcGuiSupport.afterGuiClosed(g, () -> {
                                    ConfigEditor.stage(player.m_20148_(), new ConfigEditor.Proposal(
                                            rootId, path, value, "add", key));
                                    open(player, confirmPage(rootId, browsePage(rootId, path)));
                                });
                            });
                    row += CnpcGuiSupport.ROW_STEP;
                    CnpcGuiSupport.navSubmenu(player, gui, row,
                            () -> open(player, browsePage(rootId, path)), CnpcUltraStyle.BACK);
                });
    }

    private static void paintConfirm(ServerPlayer player, String page) {
        String rootId = part(page, 1);
        String returnPage = part(page, 2);
        ConfigEditor.ChangePreview preview = ConfigEditor.preview(player.m_20148_());
        if (preview.error() != null) {
            finish(player, preview.error(), null, returnPage.isEmpty() ? "main" : returnPage);
            return;
        }
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CONFIG, CnpcGuiSupport.W,
                CnpcGuiSupport.window(280), (pl, gui) -> {
                    int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcUltraStyle.HEADER + "Confirm",
                            CnpcUltraStyle.SUBTITLE + "This writes the file and reloads it");
                    int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                            CnpcUltraStyle.INFO + preview.sentence()
                    ), CnpcGuiStyle.INFO_INLINE_MAX));
                    CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Confirm", CnpcGuiSupport.COL_L, row,
                            () -> finish(player, ConfigEditor.commit(player.m_20148_(), actor(player)),
                                    "Saved.", returnPage.isEmpty() ? "main" : returnPage));
                    row += CnpcGuiSupport.ROW_STEP;
                    CnpcGuiSupport.navSubmenu(player, gui, row,
                            () -> open(player, returnPage.isEmpty() ? "main" : returnPage), CnpcUltraStyle.BACK);
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
            case BOOLEAN -> {
                boolean on = "true".equalsIgnoreCase(ConfigEditor.currentText(rootId, entry.path()));
                stageSet(player, rootId, entry.path(), Boolean.toString(!on));
            }
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
        ConfigEditor.stage(player.m_20148_(), new ConfigEditor.Proposal(
                rootId, entry.path(), "", "remove", ""));
        open(player, confirmPage(rootId, page));
    }

    private static void stageSet(ServerPlayer player, String rootId, String path, String raw) {
        ConfigEditor.stage(player.m_20148_(), new ConfigEditor.Proposal(rootId, path, raw, "set", ""));
        open(player, confirmPage(rootId, backPage(rootId, path)));
    }

    private static void stageNumber(ServerPlayer player, String rootId, String path, int direction) {
        String next = ConfigEditor.stepNumber(rootId, path, direction);
        if (next == null) {
            finish(player, "That setting is not a number.", null, "edit:" + rootId + ":" + path);
            return;
        }
        stageSet(player, rootId, path, next);
    }

    private static void stageEnum(ServerPlayer player, String rootId, String path) {
        String next = ConfigEditor.nextEnumValue(rootId, path);
        if (next == null) {
            finish(player, "That setting is not a list of names.", null, "edit:" + rootId + ":" + path);
            return;
        }
        stageSet(player, rootId, path, next);
    }

    private static void finish(ServerPlayer player, String error, String ok, String page) {
        if (error != null) {
            CnpcGuiSupport.pushMenuMessage(player, CnpcMenuFeedback.NOTICE_BODY + error);
        } else if (ok != null) {
            CnpcGuiSupport.pushMenuMessage(player, CnpcMenuFeedback.NOTICE_BODY + ok);
        }
        open(player, page);
    }

    private static List<String> colored(List<String> lines) {
        List<String> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            out.add(line.startsWith(CnpcUltraStyle.MARK) ? line : CnpcUltraStyle.SUBTITLE + line);
        }
        return out;
    }

    private static ConfigEditor.Entry match(String rootId, String path, String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String plain = line.replaceAll(CnpcUltraStyle.MARK + ".", "");
        for (ConfigEditor.Entry entry : ConfigEditor.children(rootId, path)) {
            if (line.equals(entry.line()) || plain.equals(entry.line().replaceAll(CnpcUltraStyle.MARK + ".", ""))) {
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

    private static String actor(ServerPlayer player) {
        return player.m_7755_().getString();
    }

    private static String browsePage(String rootId, String path) {
        return "browse:" + rootId + ":" + (path == null ? "" : path);
    }

    private static String confirmPage(String rootId, String returnPage) {
        return "confirm:" + rootId + ":" + returnPage;
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

    /** {@code kind:root:rest}. The rest keeps its colons. */
    private static String part(String page, int index) {
        String[] bits = page.split(":", 3);
        if (index < 0 || index >= bits.length) {
            return "";
        }
        return bits[index];
    }
}
