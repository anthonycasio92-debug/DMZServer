package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.config.editor.ConfigEditor;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.ITextField;

/**
 * Staff config editor. Tabs switch modules, edits stay in memory until Save all,
 * and rival or sparring record files are never written from here.
 */
public final class CnpcConfigEditor {
    private static final int PAGE_SIZE = 2;
    private static final int ID_SEARCH_LABEL = 180;
    private static final int ID_SEARCH = 181;
    private static final int ID_MATCH = 182;
    private static final int ID_BANNER = 183;
    private static final int ID_PAGE = 184;
    private static final int ID_FILTER = 35;
    private static final int ID_PREV = 30;
    private static final int ID_NEXT = 31;
    private static final int ID_SAVE = 32;
    private static final int ID_RELOAD = 33;
    private static final int ID_DISCARD = 34;
    private static final int ID_NAME = 400;
    private static final int ID_CTRL = 430;
    private static final int ID_VALUE = 470;
    private static final int ID_TEXT = 190;
    private static final int ID_TEXT_2 = 191;
    private static final int ID_DESC = 500;
    private static final int ID_UNDO = 520;
    private static final int ID_GROUP = 540;
    private static final int ID_RECENT = 560;
    private static final int ID_CONFIRM = 37;
    private static final int ID_INFO = 210;
    private static final int INDENT = 12;
    private static boolean refreshing;

    private CnpcConfigEditor() {}

    public static void open(ServerPlayer player, String page) {
        CnpcUltraStyle.withAccent(CnpcUltraStyle.ACCENT_CONFIG, () -> openEditor(player, page));
    }

    private static void openEditor(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        CnpcUltraPreview.leave(player);
        if (!StaffAccess.isStaff(player)) {
            CnpcGuiSupport.denyToHub(player, CnpcMenuFeedback.NOTICE_BODY + "Staff only.");
            return;
        }
        String raw = page == null || page.isBlank() ? "list:difficulty:0:" : page.trim();
        if (raw.startsWith("detail:")) {
            paintDetail(player, raw);
            return;
        }
        if (raw.startsWith("manage:")) {
            paintManage(player, raw);
            return;
        }
        if (raw.startsWith("add:")) {
            paintAdd(player, raw);
            return;
        }
        if (raw.startsWith("review:")) {
            paintReview(player, raw);
            return;
        }
        if (!raw.startsWith("list:")) {
            raw = "list:difficulty:0:";
        }
        paintList(player, raw);
    }

    private static void paintList(ServerPlayer player, String page) {
        String module = moduleOrDefault(part(page, 1));
        int requested = number(part(page, 2));
        String path = part(page, 3);
        ConfigEditor.EditorTab tab = ConfigEditor.tab(module);
        if (tab == null) {
            open(player, "list:difficulty:0:");
            return;
        }
        final int pageNumber = requested;
        final String groupPath = path;
        final boolean editable = tab.editable();
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CONFIG, CnpcGuiSupport.W, 460, (pl, gui) -> {
            int y = paintChrome(pl, gui, tab.label(), editable ? tab.fileName() : "Player records", module);
            y = paintSearch(pl, gui, y, listPage(module, 0, groupPath));
            if (editable && gui.getComponent(ID_SEARCH) instanceof ITextField field) {
                field.setOnChange((g, tf) -> applyFilter(pl, g, module, groupPath, tf));
            }
            paintListBody(pl, gui, y, module, groupPath, pageNumber, editable, tab.fileName());
        });
    }

    /** Rebuild the rows under the search box without closing the screen, so typing keeps focus. */
    private static void applyFilter(
            ServerPlayer player, ICustomGui gui, String module, String path, ITextField field) {
        if (refreshing || gui == null || field == null || player == null) {
            return;
        }
        ConfigEditor.rememberSearch(player.m_20148_(), field.getText());
        refreshing = true;
        try {
            clearFilterBody(gui);
            int y = field.getPosY() + CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
            ConfigEditor.EditorTab tab = ConfigEditor.tab(module);
            String file = tab == null ? "" : tab.fileName();
            paintListBody(player, gui, y, module, path, 0, true, file);
            field.setFocused(true);
            gui.update();
        } finally {
            refreshing = false;
        }
    }

    private static void clearFilterBody(ICustomGui gui) {
        int[] ids = {
                ID_MATCH, ID_BANNER, ID_PAGE, ID_PREV, ID_NEXT, ID_SAVE, ID_RELOAD, ID_DISCARD,
                CnpcGuiSupport.ID_NAV_BACK, CnpcGuiSupport.ID_NAV_HUB
        };
        for (int id : ids) {
            gui.removeComponent(id);
        }
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            gui.removeComponent(ID_NAME + slot);
            gui.removeComponent(ID_UNDO + slot);
            gui.removeComponent(ID_DESC + slot);
            gui.removeComponent(ID_VALUE + slot);
            gui.removeComponent(ID_GROUP + slot);
            gui.removeComponent(ID_CTRL + slot * 3);
            gui.removeComponent(ID_CTRL + slot * 3 + 1);
            gui.removeComponent(ID_CTRL + slot * 3 + 2);
        }
        for (int i = 0; i < 4; i++) {
            gui.removeComponent(ID_RECENT + i);
        }
        for (int id = ID_INFO; id < ID_INFO + 24; id++) {
            gui.removeComponent(id);
        }
    }

    private static void paintListBody(
            ServerPlayer player, ICustomGui gui, int y, String module, String path,
            int requested, boolean editable, String fileName) {
        if (!editable) {
            y = note(gui, y, CnpcUltraStyle.SUBTITLE + "These are player records, not settings.");
            y = note(gui, y, CnpcUltraStyle.SUBTITLE + fileName);
            y = note(gui, y, CnpcUltraStyle.BODY + "Change a player from the Rival or Sparring menu.");
            y += CnpcRowList.ROW_GAP;
        } else {
            String query = ConfigEditor.search(player.m_20148_());
            if (query == null) {
                query = "";
            }
            List<ListRow> rows = listRows(player, module, path, query);
            int listed = query.isBlank()
                    ? ConfigEditor.visibleFields(module, path, "").size()
                    : countFields(rows);
            y = paintMatch(gui, y, query, listed);
            y = paintBanner(player, gui, y);
            if (query.isBlank() && ConfigEditor.fieldCount(module) > 30) {
                y = note(gui, y, CnpcUltraStyle.SUBTITLE + "30+ fields — type to filter.");
            }
            if (query.isBlank() && path.isEmpty()) {
                y = paintRecent(player, gui, y, module);
            }
            if (!query.isBlank() && countFields(rows) == 0) {
                y += CnpcRowList.ROW_GAP;
            } else if (rows.isEmpty()) {
                y = note(gui, y, CnpcUltraStyle.SUBTITLE + "Nothing is listed here yet.");
            } else {
                int pages = pages(rows.size());
                int current = Math.min(requested, pages - 1);
                int from = current * PAGE_SIZE;
                int to = Math.min(rows.size(), from + PAGE_SIZE);
                String reopen = listPage(module, current, path);
                int slot = 0;
                for (int i = from; i < to; i++) {
                    ListRow row = rows.get(i);
                    if (row.header()) {
                        y = paintGroup(player, gui, y, slot, module, row.group(), current, path);
                    } else {
                        y = paintField(player, gui, y, slot, module, row.entry(), reopen);
                    }
                    slot++;
                }
                y = paintPager(player, gui, y, module, path, current, pages);
            }
        }
        y = paintActions(player, gui, y, listPage(module, requested, path));
        paintNav(player, gui, y, path.isEmpty() ? null : listPage(module, 0, ConfigEditor.parentPath(path)));
    }

    private static void paintDetail(ServerPlayer player, String page) {
        String module = moduleOrDefault(part(page, 1));
        int listPage = number(part(page, 2));
        String path = part(page, 3);
        ConfigEditor.EditorTab tab = ConfigEditor.tab(module);
        if (tab == null || !tab.editable() || path.isEmpty()) {
            open(player, listPage(module, 0, ""));
            return;
        }
        ConfigEditor.Kind kind = ConfigEditor.kindOf(module, path);
        String name = ConfigEditor.displayName(leaf(path));
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CONFIG, CnpcGuiSupport.W, 460, (pl, gui) -> {
            int y = paintChrome(pl, gui, name, tab.fileName(), module);
            y = note(gui, ID_INFO, y, CnpcUltraStyle.SUBTITLE + "Type: " + CnpcUltraStyle.BODY
                    + ConfigEditor.typeName(module, path) + " " + CnpcUltraStyle.DIM + "· "
                    + CnpcUltraStyle.SUBTITLE + "Default: " + CnpcUltraStyle.BODY
                    + ConfigEditor.defaultText(module, path));
            y = note(gui, ID_INFO + 1, y, CnpcUltraStyle.SUBTITLE
                    + ConfigEditor.friendlyDescription(module, path,
                    ConfigEditor.effectiveRaw(player.m_20148_(), module, path)));
            String invalid = ConfigEditor.invalidMessage(player.m_20148_(), module, path);
            if (invalid != null) {
                y = note(gui, ID_INFO + 4, y, CnpcUltraStyle.DANGER + invalid);
            }
            ConfigEditor.LastChange change = ConfigEditor.lastChange(module, path);
            if (change != null) {
                y = note(gui, ID_INFO + 2, y, CnpcUltraStyle.DIM + "Last changed by " + change.actor()
                        + " at " + change.when());
            }
            if (kind == ConfigEditor.Kind.ENUM) {
                y = note(gui, ID_INFO + 3, y, CnpcUltraStyle.SUBTITLE + "Use one of " + CnpcUltraStyle.BODY
                        + String.join(", ", ConfigEditor.enumNames(module, path)));
            }
            y = paintBanner(player, gui, y);
            y = paintDetailControl(player, gui, y, module, path, kind, listPage);
            y = paintActions(player, gui, y, page);
            paintDetailNav(player, gui, y, module, path, kind, listPage);
        });
    }

    private static void paintManage(ServerPlayer player, String page) {
        String module = moduleOrDefault(part(page, 1));
        int requested = number(part(page, 2));
        String path = part(page, 3);
        ConfigEditor.EditorTab tab = ConfigEditor.tab(module);
        ConfigEditor.Kind kind = ConfigEditor.kindOf(module, path);
        if (tab == null || !tab.editable() || (kind != ConfigEditor.Kind.LIST && kind != ConfigEditor.Kind.MAP)) {
            open(player, listPage(module, 0, ""));
            return;
        }
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CONFIG, CnpcGuiSupport.W, 460, (pl, gui) -> {
            int y = paintChrome(pl, gui, ConfigEditor.displayName(leaf(path)), tab.fileName(), module);
            List<ConfigEditor.ManageRow> rows = ConfigEditor.manageRows(player.m_20148_(), module, path);
            int hidden = ConfigEditor.hiddenCount(module, path);
            if (hidden > 0) {
                y = note(gui, y, CnpcUltraStyle.SUBTITLE + "Showing the first 200.");
            }
            y = note(gui, y, CnpcUltraStyle.SUBTITLE + rows.size() + " entries");
            y = paintBanner(player, gui, y);
            int pages = pages(rows.size());
            int current = rows.isEmpty() ? 0 : Math.min(requested, pages - 1);
            if (rows.isEmpty()) {
                y = note(gui, y, CnpcUltraStyle.SUBTITLE + "Nothing is listed here yet.");
            } else {
                int from = current * PAGE_SIZE;
                int to = Math.min(rows.size(), from + PAGE_SIZE);
                for (int i = from; i < to; i++) {
                    y = paintManageRow(player, gui, y, i - from, module, path, rows.get(i), page);
                }
                y = paintPager(player, gui, y, module, path, current, pages, true);
            }
            press(gui, 36, CnpcUltraStyle.CONFIRM + "[Add entry]", CnpcGuiSupport.M, y,
                    CnpcGuiSupport.textBandWidth(), CnpcGuiSupport.BTN_H, player,
                    () -> open(player, view("add", module, 0, path)));
            y += CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
            y = paintActions(player, gui, y, page);
            paintNav(player, gui, y, listPage(module, 0, ConfigEditor.parentPath(path)));
        });
    }

    private static void paintAdd(ServerPlayer player, String page) {
        String module = moduleOrDefault(part(page, 1));
        String path = part(page, 3);
        ConfigEditor.EditorTab tab = ConfigEditor.tab(module);
        ConfigEditor.AddMode mode = ConfigEditor.addMode(module, path);
        if (tab == null || !tab.editable() || mode == ConfigEditor.AddMode.NONE) {
            open(player, view("manage", module, 0, path));
            return;
        }
        boolean keyed = mode == ConfigEditor.AddMode.KEY_AND_VALUE;
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CONFIG, CnpcGuiSupport.W, 460, (pl, gui) -> {
            int y = paintChrome(pl, gui, "Add entry", tab.fileName(), module);
            y = paintBanner(player, gui, y);
            if (keyed) {
                y = note(gui, ID_INFO, y, CnpcUltraStyle.SUBTITLE + "Name");
                gui.addTextField(ID_TEXT, CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(), CnpcGuiSupport.BTN_H);
                y += CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
                y = note(gui, ID_INFO + 1, y, CnpcUltraStyle.SUBTITLE + "Value");
            } else {
                y = note(gui, ID_INFO, y, CnpcUltraStyle.SUBTITLE + "Value");
            }
            gui.addTextField(keyed ? ID_TEXT_2 : ID_TEXT, CnpcGuiSupport.M, y,
                    CnpcGuiSupport.textBandWidth(), CnpcGuiSupport.BTN_H);
            y += CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
            boolean keyedPress = keyed;
            gui.addButton(20, CnpcGuiSupport.safeChat(CnpcUltraStyle.CONFIRM + "[Stage entry]"),
                            CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(), 24)
                    .setOnPress((g, btn) -> {
                        String key = keyedPress ? text(g, ID_TEXT) : "";
                        String value = text(g, keyedPress ? ID_TEXT_2 : ID_TEXT);
                        CnpcGuiSupport.afterGuiClosed(g, () -> {
                            String error = ConfigEditor.stageAdd(player.m_20148_(), module, path, key, value);
                            if (error != null) {
                                fail(player, error);
                                open(player, page);
                            } else {
                                open(player, view("manage", module, 0, path));
                            }
                        });
                    });
            y += 24 + CnpcRowList.ROW_GAP;
            y = paintActions(player, gui, y, page);
            paintNav(player, gui, y, view("manage", module, 0, path));
        });
    }

    private static int paintField(
            ServerPlayer player, ICustomGui gui, int y, int index, String module,
            ConfigEditor.Entry entry, String listReturn) {
        boolean dirty = ConfigEditor.dirty(player.m_20148_(), module, entry.path());
        String value = ConfigEditor.shownValue(player.m_20148_(), module, entry.path());
        String prefix = dirty ? CnpcUltraStyle.INFO + "● " : "";
        int nameWidth = dirty ? CnpcGuiSupport.textBandWidth() - 78 : CnpcGuiSupport.textBandWidth();
        press(gui, ID_NAME + index, prefix + CnpcUltraStyle.BODY + trim(ConfigEditor.displayName(leaf(entry.path())), 18)
                        + " " + CnpcUltraStyle.DIM + "· " + CnpcUltraStyle.SUBTITLE + trim(value, 12),
                CnpcGuiSupport.M, y, nameWidth, CnpcGuiSupport.BTN_H, player,
                () -> open(player, detailPage(module, number(part(listReturn, 2)), entry.path())));
        if (dirty) {
            press(gui, ID_UNDO + index, CnpcUltraStyle.DIM + "[undo]",
                    CnpcGuiSupport.M + CnpcGuiSupport.textBandWidth() - 70, y, 70, CnpcGuiSupport.BTN_H, player,
                    () -> {
                        ConfigEditor.undoField(player.m_20148_(), module, entry.path());
                        open(player, listReturn);
                    });
        }
        y += CnpcGuiSupport.BTN_H + 2;
        String invalid = ConfigEditor.invalidMessage(player.m_20148_(), module, entry.path());
        String explain = invalid != null
                ? CnpcUltraStyle.DANGER + invalid
                : CnpcUltraStyle.SUBTITLE + ConfigEditor.friendlyDescription(module, entry.path(),
                        ConfigEditor.effectiveRaw(player.m_20148_(), module, entry.path()));
        gui.addLabel(ID_DESC + index, CnpcGuiSupport.safeChat(explain),
                CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(), 12);
        y += 12 + 2;
        int x = CnpcGuiSupport.M + INDENT;
        int width = CnpcGuiSupport.textBandWidth() - INDENT;
        switch (entry.kind()) {
            case BOOLEAN -> {
                boolean on = "ON".equals(value);
                press(gui, ID_CTRL + index * 3, boolControl(), x, y, width, CnpcGuiSupport.BTN_H, player, () -> {
                    String error = ConfigEditor.stageSet(player.m_20148_(), module, entry.path(), on ? "false" : "true");
                    if (error != null) {
                        fail(player, error);
                    }
                    open(player, listReturn);
                });
            }
            case NUMBER -> {
                gui.addLabel(ID_VALUE + index, CnpcGuiSupport.safeChat(CnpcUltraStyle.SUBTITLE + "Value: "
                        + CnpcUltraStyle.BODY + trim(ConfigEditor.effectiveRaw(player.m_20148_(), module, entry.path()), 12)),
                        x, y + 4, 140, 12);
                press(gui, ID_CTRL + index * 3, CnpcUltraStyle.DIM + "[−]", x + 148, y, 44, CnpcGuiSupport.BTN_H, player,
                        () -> step(player, module, entry.path(), -1, listReturn));
                press(gui, ID_CTRL + index * 3 + 1, CnpcUltraStyle.DIM + "[+]", x + 196, y, 44, CnpcGuiSupport.BTN_H, player,
                        () -> step(player, module, entry.path(), 1, listReturn));
            }
            case ENUM -> {
                gui.addLabel(ID_VALUE + index, CnpcGuiSupport.safeChat(CnpcUltraStyle.SUBTITLE + "Value: "
                        + CnpcUltraStyle.BODY + trim(value, 12)), x, y + 4, 150, 12);
                press(gui, ID_CTRL + index * 3, CnpcUltraStyle.DIM + "[next]", x + 156, y, 70, CnpcGuiSupport.BTN_H, player,
                        () -> cycle(player, module, entry.path(), listReturn));
            }
            case TEXT -> {
                gui.addLabel(ID_VALUE + index, CnpcGuiSupport.safeChat(CnpcUltraStyle.SUBTITLE + "Value: "
                        + CnpcUltraStyle.BODY + trim(value, 14)), x, y + 4, 170, 12);
                press(gui, ID_CTRL + index * 3, CnpcUltraStyle.DIM + "[edit]", x + width - 70, y, 64,
                        CnpcGuiSupport.BTN_H, player,
                        () -> open(player, detailPage(module, number(part(listReturn, 2)), entry.path())));
            }
            case LIST, MAP -> {
                gui.addLabel(ID_VALUE + index, CnpcGuiSupport.safeChat(CnpcUltraStyle.SUBTITLE + value),
                        x, y + 4, 150, 12);
                press(gui, ID_CTRL + index * 3, CnpcUltraStyle.DIM + "[manage]", x + width - 84, y, 78,
                        CnpcGuiSupport.BTN_H, player,
                        () -> open(player, view("manage", module, 0, entry.path())));
            }
            case GROUP -> {
                gui.addLabel(ID_VALUE + index, CnpcGuiSupport.safeChat(CnpcUltraStyle.SUBTITLE + value),
                        x, y + 4, 150, 12);
                press(gui, ID_CTRL + index * 3, CnpcUltraStyle.DIM + "[open]", x + width - 70, y, 64,
                        CnpcGuiSupport.BTN_H, player, () -> {
                            ConfigEditor.rememberSearch(player.m_20148_(), "");
                            open(player, listPage(module, 0, entry.path()));
                        });
            }
            default -> gui.addLabel(ID_VALUE + index, CnpcGuiSupport.safeChat(CnpcUltraStyle.SUBTITLE
                    + "This setting can't be edited here."), x, y + 4, width, 12);
        }
        return y + CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
    }

    private static int paintDetailControl(
            ServerPlayer player, ICustomGui gui, int y, String module, String path,
            ConfigEditor.Kind kind, int listPage) {
        int width = CnpcGuiSupport.textBandWidth();
        String back = listPage(module, listPage, ConfigEditor.parentPath(path));
        switch (kind) {
            case BOOLEAN -> {
                press(gui, 40, CnpcUltraStyle.CONFIRM + "[ON]", CnpcGuiSupport.M, y, width / 2 - 4, 24, player,
                        () -> stageAndReturn(player, module, path, "true", back));
                press(gui, 41, CnpcUltraStyle.SUBTITLE + "[OFF]", CnpcGuiSupport.M + width / 2 + 4, y,
                        width / 2 - 4, 24, player, () -> stageAndReturn(player, module, path, "false", back));
                return y + 24 + CnpcRowList.ROW_GAP;
            }
            case NUMBER, TEXT, ENUM -> {
                ITextField field = gui.addTextField(ID_TEXT, CnpcGuiSupport.M, y, width, 24);
                field.setText(ConfigEditor.effectiveRaw(player.m_20148_(), module, path));
                y += 24 + CnpcRowList.ROW_GAP;
                if (kind == ConfigEditor.Kind.NUMBER) {
                    press(gui, 40, CnpcUltraStyle.DIM + "[−]", CnpcGuiSupport.M, y, 70, 24, player,
                            () -> step(player, module, path, -1, detailPage(module, listPage, path)));
                    press(gui, 41, CnpcUltraStyle.DIM + "[+]", CnpcGuiSupport.M + 78, y, 70, 24, player,
                            () -> step(player, module, path, 1, detailPage(module, listPage, path)));
                    y += 24 + CnpcRowList.ROW_GAP;
                } else if (kind == ConfigEditor.Kind.ENUM) {
                    press(gui, 40, CnpcUltraStyle.DIM + "[next]", CnpcGuiSupport.M, y, 90, 24, player,
                            () -> cycle(player, module, path, detailPage(module, listPage, path)));
                    y += 24 + CnpcRowList.ROW_GAP;
                }
                gui.addButton(42, CnpcGuiSupport.safeChat(CnpcUltraStyle.CONFIRM + "[Set]"),
                                CnpcGuiSupport.M, y, width, 24)
                        .setOnPress((g, btn) -> {
                            String typed = text(g, ID_TEXT);
                            CnpcGuiSupport.afterGuiClosed(g, () -> {
                                String error = ConfigEditor.stageSet(player.m_20148_(), module, path, typed);
                                if (error != null) {
                                    fail(player, error);
                                    open(player, detailPage(module, listPage, path));
                                } else {
                                    open(player, back);
                                }
                            });
                        });
                return y + 24 + CnpcRowList.ROW_GAP;
            }
            case LIST, MAP -> {
                press(gui, 40, CnpcUltraStyle.DIM + "[manage]", CnpcGuiSupport.M, y, width, 24, player,
                        () -> open(player, view("manage", module, 0, path)));
                return y + 24 + CnpcRowList.ROW_GAP;
            }
            case GROUP -> {
                press(gui, 40, CnpcUltraStyle.DIM + "[open]", CnpcGuiSupport.M, y, width, 24, player, () -> {
                    ConfigEditor.rememberSearch(player.m_20148_(), "");
                    open(player, listPage(module, 0, path));
                });
                return y + 24 + CnpcRowList.ROW_GAP;
            }
            default -> {
                return note(gui, y, CnpcUltraStyle.SUBTITLE + "This setting can't be edited here.");
            }
        }
    }

    private static int paintManageRow(
            ServerPlayer player, ICustomGui gui, int y, int index, String module, String collection,
            ConfigEditor.ManageRow row, String reopen) {
        String prefix = row.added() || row.removing() ? CnpcUltraStyle.INFO + "● " : "";
        gui.addLabel(ID_NAME + index, CnpcGuiSupport.safeChat(prefix + CnpcUltraStyle.BODY + trim(row.title(), 22)
                        + " " + CnpcUltraStyle.DIM + "· " + CnpcUltraStyle.SUBTITLE + trim(row.value(), 16)),
                CnpcGuiSupport.M, y + 4, CnpcGuiSupport.textBandWidth() - 80, 12);
        String label = row.added() || row.removing() ? CnpcUltraStyle.SUBTITLE + "[undo]" : CnpcUltraStyle.DANGER + "[remove]";
        press(gui, ID_CTRL + index, label, CnpcGuiSupport.M + CnpcGuiSupport.textBandWidth() - 74, y, 74,
                CnpcGuiSupport.BTN_H, player, () -> {
                    if (row.added()) {
                        ConfigEditor.undo(player.m_20148_(), module, collection, ConfigEditor.DraftOp.ADD, row.key());
                    } else if (row.removing()) {
                        ConfigEditor.undo(player.m_20148_(), module, row.path(), ConfigEditor.DraftOp.REMOVE, row.key());
                    } else {
                        ConfigEditor.stageRemove(player.m_20148_(), module, row.path());
                    }
                    open(player, reopen);
                });
        return y + CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
    }

    private static int paintChrome(ServerPlayer player, ICustomGui gui, String title, String subtitle, String module) {
        int y = CnpcGuiSupport.paintHeader(player, gui, title, subtitle);
        return paintTabs(player, gui, y, module);
    }

    private static int paintTabs(ServerPlayer player, ICustomGui gui, int y, String active) {
        List<ConfigEditor.EditorTab> tabs = ConfigEditor.editorTabs();
        int index = 0;
        int rowCount = 0;
        while (index < tabs.size()) {
            int count = Math.min(4, tabs.size() - index);
            int gap = 4;
            int width = Math.max(60, (CnpcGuiSupport.textBandWidth() - gap * (count - 1)) / count);
            int x = CnpcGuiSupport.M;
            for (int i = 0; i < count; i++) {
                ConfigEditor.EditorTab tab = tabs.get(index);
                String text = tab.id().equals(active)
                        ? CnpcUltraStyle.tabActive(tab.label())
                        : CnpcUltraStyle.tabInactive(tab.label());
                String module = tab.id();
                press(gui, CnpcGuiSupport.ID_TAB_BASE + index, text, x, y, width, CnpcGuiSupport.BTN_H, player, () -> {
                    ConfigEditor.clearBanner(player.m_20148_());
                    open(player, listPage(module, 0, ""));
                });
                x += width + gap;
                index++;
            }
            y += CnpcGuiSupport.TAB_BAR_H;
            rowCount++;
            if (rowCount > 3) {
                break;
            }
        }
        return y;
    }

    private static int paintSearch(ServerPlayer player, ICustomGui gui, int y, String reopen) {
        gui.addLabel(ID_SEARCH_LABEL, CnpcGuiSupport.safeChat(CnpcUltraStyle.SUBTITLE + "Search:"),
                CnpcGuiSupport.M, y + 4, 52, 12);
        ITextField field = gui.addTextField(ID_SEARCH, CnpcGuiSupport.M + 54, y,
                CnpcGuiSupport.textBandWidth() - 54 - 68, CnpcGuiSupport.BTN_H);
        field.setText(ConfigEditor.search(player.m_20148_()));
        press(gui, ID_FILTER, CnpcUltraStyle.SUBTITLE + "[Filter]",
                CnpcGuiSupport.M + CnpcGuiSupport.textBandWidth() - 64, y, 64, CnpcGuiSupport.BTN_H, player,
                () -> open(player, firstPage(reopen)));
        return y + CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
    }

    private static int paintMatch(ICustomGui gui, int y, String query, int count) {
        String line;
        if (query != null && !query.isBlank() && count == 0) {
            line = "No fields match '" + trim(query, 24) + "'.";
        } else if (query != null && !query.isBlank()) {
            line = count + " fields match";
        } else {
            line = count + " fields";
        }
        return note(gui, ID_MATCH, y, CnpcUltraStyle.SUBTITLE + line);
    }

    private static int paintBanner(ServerPlayer player, ICustomGui gui, int y) {
        String banner = ConfigEditor.banner(player.m_20148_());
        if (banner == null || banner.isBlank()) {
            return y;
        }
        String color = switch (ConfigEditor.bannerTone(player.m_20148_())) {
            case OK -> CnpcUltraStyle.CONFIRM;
            case ERROR -> CnpcUltraStyle.DANGER;
            default -> CnpcUltraStyle.SUBTITLE;
        };
        return note(gui, ID_BANNER, y, color + banner);
    }

    private static int paintPager(
            ServerPlayer player, ICustomGui gui, int y, String module, String path, int page, int pages) {
        return paintPager(player, gui, y, module, path, page, pages, false);
    }

    private static int paintPager(
            ServerPlayer player, ICustomGui gui, int y, String module, String path,
            int page, int pages, boolean manage) {
        if (pages <= 1) {
            return y;
        }
        String reopenKind = manage ? "manage" : "list";
        gui.addLabel(ID_PAGE, CnpcGuiSupport.safeChat(CnpcUltraStyle.SUBTITLE + "Page " + (page + 1) + " of " + pages),
                CnpcGuiSupport.M, y + 4, 110, 12);
        if (page > 0) {
            press(gui, ID_PREV, CnpcUltraStyle.SUBTITLE + "[Prev]", CnpcGuiSupport.M + 116, y, 64,
                    CnpcGuiSupport.BTN_H, player,
                    () -> open(player, view(reopenKind, module, page - 1, path)));
        }
        if (page + 1 < pages) {
            press(gui, ID_NEXT, CnpcUltraStyle.SUBTITLE + "[Next]", CnpcGuiSupport.M + 186, y, 64,
                    CnpcGuiSupport.BTN_H, player,
                    () -> open(player, view(reopenKind, module, page + 1, path)));
        }
        return y + CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
    }

    private static int paintActions(ServerPlayer player, ICustomGui gui, int y, String reopen) {
        int gap = 4;
        int width = (CnpcGuiSupport.textBandWidth() - gap * 2) / 3;
        int x = CnpcGuiSupport.M;
        press(gui, ID_SAVE, CnpcUltraStyle.CONFIRM + "[Save all]", x, y, width, CnpcGuiSupport.BTN_H, player,
                () -> save(player, reopen));
        x += width + gap;
        press(gui, ID_RELOAD, CnpcUltraStyle.SUBTITLE + "[Reload from disk]", x, y, width, CnpcGuiSupport.BTN_H, player,
                () -> reload(player, reopen));
        x += width + gap;
        press(gui, ID_DISCARD, CnpcUltraStyle.DANGER + "[Discard changes]", x, y, width, CnpcGuiSupport.BTN_H, player,
                () -> discard(player, reopen));
        return y + CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
    }

    private static void paintNav(ServerPlayer player, ICustomGui gui, int y, String backPage) {
        Runnable back;
        if (backPage == null) {
            back = () -> CnpcLmAdminGui.open(player, "main");
        } else {
            back = () -> open(player, backPage);
        }
        press(gui, CnpcGuiSupport.ID_NAV_BACK, CnpcUltraStyle.BACK, CnpcGuiSupport.COL_L, y, 95,
                CnpcGuiSupport.BTN_H, player, back);
        press(gui, CnpcGuiSupport.ID_NAV_HUB, CnpcUltraStyle.HUB, CnpcGuiSupport.COL_R, y, 95,
                CnpcGuiSupport.BTN_H, player, () -> CnpcLmHubGui.open(player, "main"));
    }

    private static void save(ServerPlayer player, String reopen) {
        int bad = ConfigEditor.invalidCount(player.m_20148_());
        if (bad > 0) {
            fail(player, bad + " fields need fixing");
            open(player, reopen);
            return;
        }
        if (ConfigEditor.dirtyCount(player.m_20148_()) == 0) {
            info(player, "Nothing to save.");
            open(player, reopen);
            return;
        }
        open(player, view("review", moduleOrDefault(part(reopen, 1)), number(part(reopen, 2)), part(reopen, 3)));
    }

    private static void confirmSave(ServerPlayer player, String reopen) {
        ConfigEditor.SaveResult result = ConfigEditor.saveAll(player.m_20148_(), actor(player));
        if (result.error() != null) {
            fail(player, result.error());
        } else if (result.count() == 0) {
            info(player, "Nothing to save.");
        } else {
            ok(player, "Saved " + result.count() + " changes.");
        }
        open(player, reopen);
    }

    private static void reload(ServerPlayer player, String reopen) {
        String error = ConfigEditor.reloadModule(player.m_20148_(), moduleOrDefault(part(reopen, 1)));
        if (error != null) {
            fail(player, error);
        } else {
            info(player, "Reloaded from disk.");
        }
        open(player, reopen);
    }

    private static void discard(ServerPlayer player, String reopen) {
        int count = ConfigEditor.discardAll(player.m_20148_());
        if (count == 0) {
            info(player, "Nothing to discard.");
        } else {
            info(player, "Discarded " + count + (count == 1 ? " change." : " changes."));
        }
        open(player, reopen);
    }

    private static void step(ServerPlayer player, String module, String path, int direction, String reopen) {
        String next = ConfigEditor.stepNumber(module, path,
                ConfigEditor.effectiveRaw(player.m_20148_(), module, path), direction);
        if (next == null) {
            fail(player, "That setting is not a number.");
            open(player, reopen);
            return;
        }
        stageAndReturn(player, module, path, next, reopen);
    }

    private static void cycle(ServerPlayer player, String module, String path, String reopen) {
        String next = ConfigEditor.nextEnumValue(module, path,
                ConfigEditor.effectiveRaw(player.m_20148_(), module, path));
        if (next == null) {
            fail(player, "That setting is not a list of names.");
            open(player, reopen);
            return;
        }
        stageAndReturn(player, module, path, next, reopen);
    }

    private static void stageAndReturn(ServerPlayer player, String module, String path, String raw, String reopen) {
        String error = ConfigEditor.stageSet(player.m_20148_(), module, path, raw);
        if (error != null) {
            fail(player, error);
        }
        open(player, reopen);
    }

    private static void ok(ServerPlayer player, String message) {
        ConfigEditor.setBanner(player.m_20148_(), message, ConfigEditor.BannerTone.OK);
        CnpcGuiSupport.pushMenuMessage(player, CnpcMenuFeedback.NOTICE_BODY + message);
    }

    private static void info(ServerPlayer player, String message) {
        ConfigEditor.setBanner(player.m_20148_(), message, ConfigEditor.BannerTone.INFO);
        CnpcGuiSupport.pushMenuMessage(player, CnpcMenuFeedback.NOTICE_BODY + message);
    }

    private static void fail(ServerPlayer player, String message) {
        ConfigEditor.setBanner(player.m_20148_(), message, ConfigEditor.BannerTone.ERROR);
        CnpcGuiSupport.pushMenuMessage(player, CnpcMenuFeedback.NOTICE_BODY + message);
    }

    private static void paintReview(ServerPlayer player, String page) {
        String module = moduleOrDefault(part(page, 1));
        String back = listPage(module, number(part(page, 2)), part(page, 3));
        List<ConfigEditor.PendingChange> changes = ConfigEditor.pendingChanges(player.m_20148_());
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CONFIG, CnpcGuiSupport.W, 460, (pl, gui) -> {
            int y = CnpcGuiSupport.paintHeader(pl, gui,
                    "Review changes (" + changes.size() + ")",
                    "Nothing is written until you confirm");
            y = paintBanner(player, gui, y);
            if (changes.isEmpty()) {
                y = note(gui, y, CnpcUltraStyle.SUBTITLE + "Nothing to save.");
            } else {
                int shown = Math.min(changes.size(), 8);
                for (int i = 0; i < shown; i++) {
                    ConfigEditor.PendingChange change = changes.get(i);
                    y = note(gui, ID_INFO + i, y, CnpcUltraStyle.SUBTITLE + change.name() + ": "
                            + CnpcUltraStyle.BODY + trim(change.before(), 16) + " "
                            + CnpcUltraStyle.DIM + "→ " + CnpcUltraStyle.CONFIRM + trim(change.after(), 16));
                }
                if (changes.size() > shown) {
                    y = note(gui, y, CnpcUltraStyle.SUBTITLE + (changes.size() - shown) + " more changes");
                }
            }
            press(gui, ID_CONFIRM, CnpcUltraStyle.CONFIRM + "Confirm", CnpcGuiSupport.M, y,
                    CnpcGuiSupport.textBandWidth(), 24, player, () -> confirmSave(player, back));
            y += 24 + CnpcRowList.ROW_GAP;
            paintNav(player, gui, y, back);
        });
    }

    private static int paintRecent(ServerPlayer player, ICustomGui gui, int y, String module) {
        List<String> paths = ConfigEditor.recentPaths(player.m_20148_(), module);
        if (paths.isEmpty()) {
            return y;
        }
        y = note(gui, ID_RECENT, y, CnpcUltraStyle.INFO + "● Recent");
        int shown = Math.min(3, paths.size());
        for (int i = 0; i < shown; i++) {
            String path = paths.get(i);
            int slot = i;
            press(gui, ID_RECENT + 1 + slot, CnpcUltraStyle.BODY + trim(ConfigEditor.displayName(leaf(path)), 28),
                    CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(), CnpcGuiSupport.BTN_H, player,
                    () -> open(player, detailPage(module, 0, path)));
            y += CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
        }
        return y;
    }

    private static int paintGroup(
            ServerPlayer player, ICustomGui gui, int y, int index, String module,
            String group, int page, String path) {
        int count = 0;
        for (ConfigEditor.FieldGroup candidate : ConfigEditor.groups(module, path)) {
            if (group.equals(candidate.name())) {
                count = candidate.fields().size();
                break;
            }
        }
        boolean collapsed = ConfigEditor.groupCollapsed(player.m_20148_(), module, group);
        String mark = collapsed ? CnpcUltraStyle.SUBTITLE + "▸ " : "";
        press(gui, ID_GROUP + index, mark + CnpcUltraStyle.section(group) + " "
                        + CnpcUltraStyle.SUBTITLE + count + " fields",
                CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(), CnpcGuiSupport.BTN_H, player, () -> {
                    ConfigEditor.toggleGroup(player.m_20148_(), module, group);
                    open(player, listPage(module, page, path));
                });
        return y + CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
    }

    private static void paintDetailNav(
            ServerPlayer player, ICustomGui gui, int y, String module, String path,
            ConfigEditor.Kind kind, int listPageNumber) {
        String back = listPage(module, listPageNumber, ConfigEditor.parentPath(path));
        boolean typed = kind == ConfigEditor.Kind.NUMBER
                || kind == ConfigEditor.Kind.TEXT
                || kind == ConfigEditor.Kind.ENUM;
        gui.addButton(CnpcGuiSupport.ID_NAV_BACK, CnpcGuiSupport.safeChat(CnpcUltraStyle.BACK),
                        CnpcGuiSupport.COL_L, y, 95, CnpcGuiSupport.BTN_H)
                .setOnPress((g, btn) -> {
                    capture(player, g);
                    String typedValue = typed ? text(g, ID_TEXT) : null;
                    CnpcGuiSupport.afterGuiClosed(g, () -> leaveDetail(player, module, path, typed, typedValue, back));
                });
        gui.addButton(CnpcGuiSupport.ID_NAV_HUB, CnpcGuiSupport.safeChat(CnpcUltraStyle.HUB),
                        CnpcGuiSupport.COL_R, y, 95, CnpcGuiSupport.BTN_H)
                .setOnPress((g, btn) -> {
                    capture(player, g);
                    String typedValue = typed ? text(g, ID_TEXT) : null;
                    CnpcGuiSupport.afterGuiClosed(g, () -> {
                        if (!leaveDetail(player, module, path, typed, typedValue, null)) {
                            return;
                        }
                        CnpcLmHubGui.open(player, "main");
                    });
                });
    }

    /** @return false when the typed value is invalid and the detail page stays open */
    private static boolean leaveDetail(
            ServerPlayer player, String module, String path, boolean typed, String typedValue, String back) {
        if (typed) {
            String error = ConfigEditor.stageSet(player.m_20148_(), module, path, typedValue);
            if (error != null) {
                fail(player, error);
                open(player, detailPage(module, 0, path));
                return false;
            }
        }
        if (back != null) {
            open(player, back);
        }
        return true;
    }

    private static List<ListRow> listRows(ServerPlayer player, String module, String path, String query) {
        List<ListRow> rows = new ArrayList<>();
        boolean grouped = (query == null || query.isBlank()) && (path == null || path.isEmpty());
        if (!grouped) {
            for (ConfigEditor.Entry entry : ConfigEditor.visibleFields(module, path, query)) {
                rows.add(ListRow.field(entry));
            }
            return rows;
        }
        for (ConfigEditor.FieldGroup group : ConfigEditor.groups(module, "")) {
            if (group.name() == null || group.name().isEmpty()) {
                for (ConfigEditor.Entry entry : group.fields()) {
                    rows.add(ListRow.field(entry));
                }
                continue;
            }
            rows.add(ListRow.header(group.name()));
            if (ConfigEditor.groupCollapsed(player.m_20148_(), module, group.name())) {
                continue;
            }
            for (ConfigEditor.Entry entry : group.fields()) {
                rows.add(ListRow.field(entry));
            }
        }
        return rows;
    }

    private static int countFields(List<ListRow> rows) {
        int count = 0;
        for (ListRow row : rows) {
            if (!row.header()) {
                count++;
            }
        }
        return count;
    }

    private record ListRow(boolean header, String group, ConfigEditor.Entry entry) {
        static ListRow header(String group) {
            return new ListRow(true, group, null);
        }

        static ListRow field(ConfigEditor.Entry entry) {
            return new ListRow(false, "", entry);
        }
    }

    private static String boolControl() {
        return CnpcUltraStyle.CONFIRM + "[ON] " + CnpcUltraStyle.DIM + "/ " + CnpcUltraStyle.SUBTITLE + "[OFF]";
    }

    private static int note(ICustomGui gui, int y, String text) {
        return note(gui, ID_INFO + (y % 7), y, text);
    }

    private static int note(ICustomGui gui, int id, int y, String text) {
        gui.addLabel(id, CnpcGuiSupport.safeChat(text), CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(), 12);
        return y + CnpcGuiSupport.LINE_H + 2;
    }

    private static void press(
            ICustomGui gui, int id, String label, int x, int y, int w, int h,
            ServerPlayer player, Runnable action) {
        gui.addButton(id, CnpcGuiSupport.safeChat(label), x, y, w, h).setOnPress((g, btn) -> {
            capture(player, g);
            CnpcGuiSupport.afterGuiClosed(g, action);
        });
    }

    private static void capture(ServerPlayer player, ICustomGui gui) {
        if (gui != null && gui.getComponent(ID_SEARCH) instanceof ITextField field) {
            ConfigEditor.rememberSearch(player.m_20148_(), field.getText());
        }
    }

    private static String text(ICustomGui gui, int id) {
        if (gui == null || !(gui.getComponent(id) instanceof ITextField field) || field.getText() == null) {
            return "";
        }
        return field.getText();
    }

    private static String actor(ServerPlayer player) {
        return player.m_7755_().getString();
    }

    private static String listPage(String module, int page, String path) {
        return view("list", module, page, path);
    }

    private static String detailPage(String module, int page, String path) {
        return view("detail", module, page, path);
    }

    private static String view(String kind, String module, int page, String path) {
        return kind + ":" + module + ":" + Math.max(0, page) + ":" + (path == null ? "" : path);
    }

    private static String firstPage(String page) {
        return view(part(page, 0).isEmpty() ? "list" : part(page, 0), moduleOrDefault(part(page, 1)), 0, part(page, 3));
    }

    /** {@code kind:module:page:path}. The path keeps its dots. */
    private static String part(String page, int index) {
        String[] bits = (page == null ? "" : page).split(":", 4);
        if (index < 0 || index >= bits.length) {
            return "";
        }
        return bits[index];
    }

    private static String moduleOrDefault(String module) {
        return ConfigEditor.tab(module) == null ? "difficulty" : module;
    }

    private static int number(String raw) {
        try {
            return Math.max(0, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int pages(int count) {
        return Math.max(1, (count + PAGE_SIZE - 1) / PAGE_SIZE);
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

    private static String trim(String text, int max) {
        if (text == null) {
            return "";
        }
        String plain = text.replace('\n', ' ').trim();
        if (plain.length() <= max) {
            return plain;
        }
        return plain.substring(0, Math.max(1, max - 1)) + "…";
    }
}
