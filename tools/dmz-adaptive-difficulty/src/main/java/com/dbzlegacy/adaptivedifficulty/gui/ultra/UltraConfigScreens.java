package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.config.editor.ConfigEditor;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Staff config editor — Ultra dialog parity with {@code CnpcConfigEditor}:
 * module list → field pages → detail toggles / steps / enum cycle → Save all.
 * Edits stay in memory until Save all; rival/spar record files stay closed.
 */
public final class UltraConfigScreens {
    private static final int PAGE_SIZE = 6;

    private UltraConfigScreens() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        if (!StaffAccess.isStaff(player)) {
            UltraHubScreens.open(player, "main");
            return;
        }
        String raw = page == null || page.isBlank() ? "modules" : page.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("list:")) {
            openList(player, raw);
            return;
        }
        if (lower.startsWith("detail:")) {
            openDetail(player, raw);
            return;
        }
        if (lower.startsWith("module:")) {
            String id = raw.substring("module:".length()).trim();
            openList(player, listPage(id.isBlank() ? "difficulty" : id, 0, ""));
            return;
        }
        if ("review".equals(lower) || lower.startsWith("review:")) {
            openReview(player);
            return;
        }
        openModules(player);
    }

    private static void openModules(ServerPlayer player) {
        int dirty = ConfigEditor.dirtyCount(player.m_20148_());
        UltraScreenSpec spec = UltraScreenSpec.of("config")
                .title("Config Editor")
                .page("modules")
                .activeTab("home")
                .line(UltraStyle.LABEL + "Edits stay in memory until Save all.")
                .labelValue("Unsaved changes", String.valueOf(dirty))
                .setRow(UltraStyle.CONTENT_ROW + 70);
        int placed = 0;
        for (ConfigEditor.ModuleLink link : ConfigEditor.moduleLinks()) {
            if (placed >= 12 || link == null) {
                continue;
            }
            String label = link.open()
                    ? link.label() + " (" + link.fields() + ")"
                    : UltraStyle.dim(link.label() + " · closed");
            String action = link.open()
                    ? "page:" + listPage(link.id(), 0, "")
                    : "do:open_module:" + link.id();
            if (placed % 2 == 0) {
                spec.buttonLeft(action, label, link.open() ? "normal" : "dim");
            } else {
                spec.buttonRight(action, label, link.open() ? "normal" : "dim");
                spec.rowGap();
            }
            placed++;
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        spec.buttonLeft("page:review", UltraStyle.confirm("Review changes"), "confirm")
                .buttonRight("do:save_all", UltraStyle.confirm("Save all"), "confirm")
                .rowGap();
        spec.buttonLeft("do:discard_all", UltraStyle.danger("Discard all"), "danger")
                .rowGap();
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openList(ServerPlayer player, String page) {
        String module = part(page, 1);
        if (module.isBlank()) {
            module = "difficulty";
        }
        int pageNumber = Math.max(0, number(part(page, 2)));
        String path = part(page, 3);
        ConfigEditor.EditorTab tab = ConfigEditor.tab(module);
        if (tab == null) {
            openModules(player);
            return;
        }
        UUID uuid = player.m_20148_();
        List<ConfigEditor.Entry> fields = ConfigEditor.visibleFields(module, path, ConfigEditor.search(uuid));
        int pages = fields.isEmpty() ? 1 : (fields.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int current = Math.min(pageNumber, Math.max(0, pages - 1));
        int from = current * PAGE_SIZE;
        int to = Math.min(fields.size(), from + PAGE_SIZE);
        UltraScreenSpec spec = UltraScreenSpec.of("config")
                .title("Config · " + tab.label())
                .page(listPage(module, current, path))
                .activeTab("home")
                .labelValue("Module", tab.label())
                .labelValue("File", tab.fileName() == null ? "—" : tab.fileName())
                .labelValue("Unsaved (all)", String.valueOf(ConfigEditor.dirtyCount(uuid)))
                .labelValue("Page", (current + 1) + "/" + pages);
        if (!tab.editable()) {
            spec.line(UltraStyle.dim("This module is closed — player records are not edited here."));
        }
        for (int i = from; i < to; i++) {
            ConfigEditor.Entry entry = fields.get(i);
            if (entry == null) {
                continue;
            }
            boolean dirty = ConfigEditor.dirty(uuid, module, entry.path());
            String value = ConfigEditor.shownValue(uuid, module, entry.path());
            String name = ConfigEditor.displayName(leaf(entry.path()));
            String mark = dirty ? "● " : "";
            spec.line(UltraStyle.LABEL + mark + name + " " + UltraStyle.VALUE + shortVal(value));
        }
        if (from >= to) {
            spec.line(UltraStyle.dim("No fields on this page."));
        }
        spec.setRow(UltraStyle.CONTENT_ROW + UltraStyle.BODY_MIN_H);
        for (int i = from; i < to; i++) {
            ConfigEditor.Entry entry = fields.get(i);
            if (entry == null) {
                continue;
            }
            String name = ConfigEditor.displayName(leaf(entry.path()));
            String action = "page:" + detailPage(module, current, entry.path());
            if ((i - from) % 2 == 0) {
                spec.buttonLeft(action, name, "normal");
            } else {
                spec.buttonRight(action, name, "normal");
                spec.rowGap();
            }
        }
        if ((to - from) % 2 == 1) {
            spec.rowGap();
        }
        if (current > 0) {
            spec.buttonLeft("page:" + listPage(module, current - 1, path), UltraStyle.dim("Prev"), "dim");
        }
        if (current + 1 < pages) {
            spec.buttonRight("page:" + listPage(module, current + 1, path), UltraStyle.dim("Next"), "dim");
        }
        if (current > 0 || current + 1 < pages) {
            spec.rowGap();
        }
        spec.buttonLeft("do:reload_module:" + module, UltraStyle.confirm("Reload module"), "confirm")
                .buttonRight("do:save_all", UltraStyle.confirm("Save all"), "confirm")
                .rowGap();
        spec.buttonLeft("nav:home", UltraStyle.dim("Module list"), "dim").rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openDetail(ServerPlayer player, String page) {
        String module = part(page, 1);
        int listPageNum = Math.max(0, number(part(page, 2)));
        String path = part(page, 3);
        ConfigEditor.EditorTab tab = ConfigEditor.tab(module);
        if (tab == null || !tab.editable() || path.isEmpty()) {
            openList(player, listPage(module.isBlank() ? "difficulty" : module, 0, ""));
            return;
        }
        UUID uuid = player.m_20148_();
        ConfigEditor.Kind kind = ConfigEditor.kindOf(module, path);
        String name = ConfigEditor.displayName(leaf(path));
        String value = ConfigEditor.shownValue(uuid, module, path);
        String raw = ConfigEditor.effectiveRaw(uuid, module, path);
        UltraScreenSpec spec = UltraScreenSpec.of("config")
                .title("Config · " + name)
                .page(detailPage(module, listPageNum, path))
                .activeTab("home")
                .labelValue("Field", name)
                .labelValue("Type", ConfigEditor.typeName(module, path))
                .labelValue("Value", shortVal(value))
                .labelValue("Default", shortVal(ConfigEditor.defaultText(module, path)))
                .line(UltraStyle.LABEL
                        + ConfigEditor.friendlyDescription(module, path, raw == null ? "" : raw));
        String invalid = ConfigEditor.invalidMessage(uuid, module, path);
        if (invalid != null && !invalid.isBlank()) {
            spec.line(UltraStyle.danger(invalid));
        }
        if (kind == ConfigEditor.Kind.ENUM) {
            spec.line(UltraStyle.LABEL + "Options: " + UltraStyle.VALUE
                    + String.join(", ", ConfigEditor.enumNames(module, path)));
        }
        spec.setRow(UltraStyle.CONTENT_ROW + UltraStyle.BODY_MIN_H);
        String encoded = encodePath(path);
        switch (kind) {
            case BOOLEAN -> {
                boolean on = "ON".equalsIgnoreCase(value) || "true".equalsIgnoreCase(raw);
                spec.buttonLeft(
                                "do:toggle:" + module + "|" + encoded,
                                on ? UltraStyle.confirm("Toggle · ON") : UltraStyle.dim("Toggle · OFF"),
                                on ? "confirm" : "dim")
                        .rowGap();
            }
            case NUMBER -> {
                spec.buttonLeft("do:step:" + module + "|" + encoded + "|-1", UltraStyle.dim("−"), "dim")
                        .buttonRight("do:step:" + module + "|" + encoded + "|1", UltraStyle.dim("+"), "dim")
                        .rowGap();
            }
            case ENUM -> {
                spec.buttonLeft(
                                "do:cycle:" + module + "|" + encoded,
                                UltraStyle.confirm("Next value"),
                                "confirm")
                        .rowGap();
            }
            case LIST, MAP, GROUP -> {
                spec.line(UltraStyle.dim("Lists and maps: edit one entry at a time in the field below."))
                        .button(
                                "page:" + listPage(module, listPageNum, path),
                                UltraStyle.dim("Back"),
                                UltraStyle.STYLE_DIM)
                        .rowGap();
            }
            case TEXT -> {
                String inputId = "input:" + module + "|" + encoded;
                spec.textFieldRow(
                                inputId,
                                value == null ? "" : value,
                                "Type a value…")
                        .rowGap();
                spec.buttonBound(
                                "do:set:" + module + "|" + encoded,
                                UltraStyle.confirm("Apply"),
                                UltraStyle.STYLE_CONFIRM,
                                inputId)
                        .rowGap();
            }
            default -> spec.line(UltraStyle.dim("This field type has no Ultra editor yet."));
        }
        if (ConfigEditor.dirty(uuid, module, path)) {
            spec.buttonLeft("do:undo:" + module + "|" + encoded, UltraStyle.dim("Undo field"), "dim")
                    .rowGap();
        }
        spec.buttonLeft("page:" + listPage(module, listPageNum, ConfigEditor.parentPath(path)), UltraStyle.dim("Back"), "dim")
                .buttonRight("do:save_all", UltraStyle.confirm("Save all"), "confirm")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openReview(ServerPlayer player) {
        List<ConfigEditor.PendingChange> pending = ConfigEditor.pendingChanges(player.m_20148_());
        UltraScreenSpec spec = UltraScreenSpec.of("config")
                .title("Config · Review")
                .page("review")
                .activeTab("home")
                .labelValue("Pending", String.valueOf(pending == null ? 0 : pending.size()));
        if (pending == null || pending.isEmpty()) {
            spec.line(UltraStyle.dim("No unsaved changes."));
        } else {
            int shown = 0;
            for (ConfigEditor.PendingChange change : pending) {
                if (shown >= 12 || change == null) {
                    continue;
                }
                spec.line(UltraStyle.LABEL + change.name() + " " + UltraStyle.DIM + change.before()
                        + " → " + UltraStyle.VALUE + change.after());
                shown++;
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW + UltraStyle.BODY_MIN_H);
        spec.buttonLeft("do:save_all", UltraStyle.confirm("Confirm save"), "confirm")
                .buttonRight("do:discard_all", UltraStyle.danger("Discard all"), "danger")
                .rowGap();
        spec.button("nav:home", UltraStyle.dim("Module list"), "dim").rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    /**
     * Config do-actions. Returns a status message for chat/toast.
     */
    public static String handleAction(ServerPlayer player, String action, String arg, String page) {
        return handleAction(player, action, arg, page, null);
    }

    public static String handleAction(
            ServerPlayer player,
            String action,
            String arg,
            String page,
            com.google.gson.JsonObject args) {
        if (player == null) {
            return UltraStyle.dangerLine("This only works in-game.");
        }
        if (!StaffAccess.isStaff(player)) {
            return UltraStyle.dangerLine("Staff only.");
        }
        String act = action == null ? "" : action.toLowerCase(Locale.ROOT).trim();
        String a = arg == null ? "" : arg.trim();
        UUID uuid = player.m_20148_();
        return switch (act) {
            case "save_all", "save" -> {
                ConfigEditor.SaveResult result =
                        ConfigEditor.saveAll(uuid, player.m_7755_().getString());
                if (result.error() != null && !result.error().isBlank()) {
                    yield UltraStyle.dangerLine(result.error());
                }
                yield result.message() == null || result.message().isBlank()
                        ? UltraStyle.noticeLine("Saved.")
                        : UltraStyle.noticeLine(result.message());
            }
            case "discard_all", "discard" -> {
                int count = ConfigEditor.discardAll(uuid);
                yield count <= 0
                        ? UltraStyle.noticeLine("Nothing to discard.")
                        : UltraStyle.noticeLine("Discarded " + count + " change(s).");
            }
            case "reload_module", "reload" -> {
                String module = !a.isBlank() ? a : moduleFromPage(page);
                String error = ConfigEditor.reloadModule(uuid, module);
                yield error == null
                        ? UltraStyle.noticeLine("Reloaded " + module + ".")
                        : UltraStyle.dangerLine(error);
            }
            case "open_module" -> "";
            case "toggle" -> {
                String[] parts = splitModulePath(a);
                if (parts == null) {
                    yield UltraStyle.dangerLine("Missing field.");
                }
                String raw = ConfigEditor.effectiveRaw(uuid, parts[0], parts[1]);
                boolean on = "true".equalsIgnoreCase(raw) || "ON".equalsIgnoreCase(raw);
                String error = ConfigEditor.stageSet(uuid, parts[0], parts[1], on ? "false" : "true");
                yield error == null ? "" : UltraStyle.dangerLine(error);
            }
            case "step" -> {
                String[] parts = splitModulePathDir(a);
                if (parts == null) {
                    yield UltraStyle.dangerLine("Missing field.");
                }
                int dir = "1".equals(parts[2]) ? 1 : -1;
                String next = ConfigEditor.stepNumber(parts[0], parts[1], dir);
                if (next == null) {
                    yield UltraStyle.dangerLine("Cannot step that field.");
                }
                String error = ConfigEditor.stageSet(uuid, parts[0], parts[1], next);
                yield error == null ? "" : UltraStyle.dangerLine(error);
            }
            case "cycle" -> {
                String[] parts = splitModulePath(a);
                if (parts == null) {
                    yield UltraStyle.dangerLine("Missing field.");
                }
                String next = ConfigEditor.nextEnumValue(parts[0], parts[1]);
                if (next == null) {
                    yield UltraStyle.dangerLine("Cannot cycle that field.");
                }
                String error = ConfigEditor.stageSet(uuid, parts[0], parts[1], next);
                yield error == null ? "" : UltraStyle.dangerLine(error);
            }
            case "set" -> {
                String[] parts = splitModulePath(a);
                if (parts == null) {
                    yield UltraStyle.dangerLine("Missing field.");
                }
                String typed = argsString(args, "value");
                if (typed == null) {
                    yield UltraStyle.dangerLine(
                            "Type a value in the field, then press Save.");
                }
                String error = ConfigEditor.stageSet(uuid, parts[0], parts[1], typed);
                yield error == null
                        ? UltraStyle.noticeLine("Staged new value.")
                        : UltraStyle.dangerLine(error);
            }
            case "undo" -> {
                String[] parts = splitModulePath(a);
                if (parts == null) {
                    yield UltraStyle.dangerLine("Missing field.");
                }
                ConfigEditor.undoField(uuid, parts[0], parts[1]);
                yield UltraStyle.noticeLine("Undid field change.");
            }
            case "page", "refresh" -> "";
            default -> UltraStyle.dangerLine("That config action is not available.");
        };
    }

    /** {@code module|path} */
    private static String[] splitModulePath(String arg) {
        if (arg == null || arg.isBlank()) {
            return null;
        }
        int bar = arg.indexOf('|');
        if (bar <= 0 || bar >= arg.length() - 1) {
            return null;
        }
        String module = arg.substring(0, bar).trim();
        String path = decodePath(arg.substring(bar + 1).trim());
        if (module.isBlank() || path.isBlank()) {
            return null;
        }
        return new String[] {module, path};
    }

    /** {@code module|path|dir} */
    private static String[] splitModulePathDir(String arg) {
        if (arg == null || arg.isBlank()) {
            return null;
        }
        int bar = arg.indexOf('|');
        if (bar <= 0) {
            return null;
        }
        String module = arg.substring(0, bar).trim();
        String rest = arg.substring(bar + 1);
        int bar2 = rest.lastIndexOf('|');
        if (bar2 <= 0 || bar2 >= rest.length() - 1) {
            return null;
        }
        String path = decodePath(rest.substring(0, bar2).trim());
        String dir = rest.substring(bar2 + 1).trim();
        if (module.isBlank() || path.isBlank()) {
            return null;
        }
        return new String[] {module, path, dir};
    }

    private static String encodePath(String path) {
        if (path == null) {
            return "";
        }
        return path.replace("|", "%7C");
    }

    private static String decodePath(String path) {
        if (path == null) {
            return "";
        }
        return path.replace("%7C", "|");
    }

    private static String listPage(String module, int page, String path) {
        return "list:" + module + ":" + page + ":" + (path == null ? "" : path);
    }

    private static String detailPage(String module, int listPage, String path) {
        return "detail:" + module + ":" + listPage + ":" + (path == null ? "" : path);
    }

    private static String part(String page, int index) {
        if (page == null) {
            return "";
        }
        String[] parts = page.split(":", -1);
        return index >= 0 && index < parts.length ? parts[index] : "";
    }

    private static int number(String raw) {
        try {
            return Integer.parseInt(raw == null ? "0" : raw.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String leaf(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(dot + 1);
    }

    private static String shortVal(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        String plain = UltraStyle.plain(value);
        return plain.length() <= 24 ? plain : plain.substring(0, 23) + "\u2026";
    }

    private static String moduleFromPage(String page) {
        if (page == null || page.isBlank()) {
            return "difficulty";
        }
        String p = page.trim();
        if (p.regionMatches(true, 0, "module:", 0, 7)
                || p.regionMatches(true, 0, "list:", 0, 5)
                || p.regionMatches(true, 0, "detail:", 0, 7)) {
            String id = part(p, 1);
            return id.isBlank() ? "difficulty" : id;
        }
        return "difficulty";
    }

    private static String argsString(com.google.gson.JsonObject args, String key) {
        if (args == null || key == null || !args.has(key) || args.get(key).isJsonNull()) {
            return null;
        }
        try {
            return args.get(key).getAsString();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
