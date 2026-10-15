package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.editor.ConfigEditor;
import com.dbzlegacy.adaptivedifficulty.net.gui.UltraClientFeatures;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Staff config editor.
 *
 * <p>Left category rail, search, and sections such as {@code Difficulty > Mob Spawning}.
 * Each field shows a plain label, a one-line description, and the current value next to
 * the default. Edits stay in memory until Review changes, then Confirm save.
 * Rival/spar record files stay closed.
 */
public final class UltraConfigScreens {
    /** Settings shown per page (inline controls are dense). */
    private static final int PAGE_SIZE = 2;

    /**
     * Fit the default Ultra dialog width so small GUI scales do not clip a 480px panel.
     * Left category rail is chrome (does not clamp body packing); main column is flow layout.
     */
    private static final int PANEL_W = UltraStyle.PANEL_W;
    private static final int SIDE_X = 8;
    /** Matches {@link UltraStyle#BTN_MIN_W} — client never shrinks buttons below that. */
    private static final int SIDE_W = UltraStyle.BTN_MIN_W;
    private static final int MAIN_X = SIDE_X + SIDE_W + 8;
    private static final int MAIN_W = PANEL_W - MAIN_X - UltraStyle.PANEL_MARGIN;
    /** Extra gap under a setting label before its control (avoids text/button overlap). */
    private static final int LABEL_CTRL_GAP = 4;
    /** Min clear space between two controls on one row before we stack instead. */
    private static final int CTRL_GAP = 8;
    /**
     * Glyphs that fit a main-column flow button ({@link #MAIN_W} minus pad).
     * Longer setting names and descriptions are cut on a word boundary.
     */
    private static final int FLOW_CHARS = 38;

    /** Categories always shown in the left rail (user-facing order). */
    private static final String[][] CATEGORIES = {
        {"difficulty", "Difficulty"},
        {"progression", "Progress"},
        {"character-services", "Character"},
        {"saga", "Saga"},
        {"rival", "Rival"},
        {"sparring", "Sparring"},
        {"currency", "Currency"},
    };

    private UltraConfigScreens() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        if (!StaffAccess.isStaff(player)) {
            UltraHubScreens.open(player, "main");
            return;
        }
        String raw = page == null || page.isBlank() ? "edit:difficulty:0:" : page.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        if ("review".equals(lower) || lower.startsWith("review:")) {
            openReview(player);
            return;
        }
        // Legacy drill-down pages → search-first editor.
        if ("modules".equals(lower) || "main".equals(lower) || "list".equals(lower)) {
            openEditor(player, "difficulty", 0, "");
            return;
        }
        if (lower.startsWith("module:")) {
            String id = raw.substring("module:".length()).trim();
            openEditor(player, id.isBlank() ? "difficulty" : id, 0, "");
            return;
        }
        if (lower.startsWith("list:")) {
            openEditor(player, part(raw, 1), Math.max(0, number(part(raw, 2))), part(raw, 3));
            return;
        }
        if (lower.startsWith("detail:")) {
            String module = part(raw, 1);
            String fieldPath = part(raw, 3);
            String parent = ConfigEditor.parentPath(fieldPath);
            openEditor(player, module.isBlank() ? "difficulty" : module, 0, parent);
            return;
        }
        if (lower.startsWith("edit:")) {
            openEditor(player, part(raw, 1), Math.max(0, number(part(raw, 2))), part(raw, 3));
            return;
        }
        openEditor(player, "difficulty", 0, "");
    }

    private static void openEditor(ServerPlayer player, String moduleId, int pageNumber, String path) {
        String module = moduleId == null || moduleId.isBlank() ? "difficulty" : moduleId.trim();
        ConfigEditor.EditorTab tab = ConfigEditor.tab(module);
        if (tab == null) {
            module = "difficulty";
            tab = ConfigEditor.tab(module);
        }
        UUID uuid = player.m_20148_();
        String query = ConfigEditor.search(uuid);
        boolean searching = query != null && !query.isBlank();
        String groupPath = path == null ? "" : path;
        String where = categoryLabel(module, tab);

        boolean sectionMode = !searching && ConfigEditor.isSectionPath(groupPath);
        boolean nestedGroup = !searching && !sectionMode && !groupPath.isBlank();
        boolean showIndex = !searching && !sectionMode && !nestedGroup;
        List<ConfigEditor.SectionLink> sections =
                showIndex ? ConfigEditor.sections(module) : List.of();
        if (showIndex && sections.size() == 1) {
            groupPath = sections.get(0).openPath();
            sectionMode = ConfigEditor.isSectionPath(groupPath);
            nestedGroup = !sectionMode && !groupPath.isBlank();
            showIndex = false;
            sections = List.of();
        }

        List<Row> rows = new ArrayList<>();
        if (searching) {
            for (ConfigEditor.SearchHit hit : ConfigEditor.searchAll(query)) {
                if (hit == null || hit.entry() == null) {
                    continue;
                }
                rows.add(new Row(hit.moduleId(), hit.moduleLabel(), hit.entry(), true));
            }
        } else if (sectionMode && tab != null) {
            String section = groupPath.substring(ConfigEditor.SECTION_PREFIX.length());
            for (ConfigEditor.Entry entry : ConfigEditor.fieldsInSection(module, section)) {
                if (entry != null) {
                    rows.add(new Row(module, where, entry, false));
                }
            }
        } else if (nestedGroup && tab != null) {
            for (ConfigEditor.Entry entry : ConfigEditor.visibleFields(module, groupPath, "")) {
                if (entry != null) {
                    rows.add(new Row(module, where, entry, false));
                }
            }
        }

        int pages = rows.isEmpty() ? 1 : (rows.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int current = showIndex ? 0 : Math.min(Math.max(0, pageNumber), Math.max(0, pages - 1));
        int from = current * PAGE_SIZE;
        int to = Math.min(rows.size(), from + PAGE_SIZE);
        int dirty = ConfigEditor.dirtyCount(uuid);
        String crumb = breadcrumb(where, groupPath, searching);
        ConfigEditor.rememberEditorPage(uuid, editPage(module, current, groupPath));

        UltraScreenSpec spec = UltraScreenSpec.of("config")
                .title("Config Editor")
                .page(editPage(module, current, groupPath))
                .panel(PANEL_W, UltraStyle.PANEL_MAX_H)
                .lineGap(2)
                .bodyPad(4)
                .labelValue("Unsaved", String.valueOf(dirty));
        // Header copy lives in the body band. Category rail is chrome (left) so it does not
        // clamp body packing. Search + settings use main-column flow (label above control).
        int railTop = UltraStyle.CONTENT_ROW;
        placeCategoryRail(spec, module, searching, railTop);
        int mainTop = UltraStyle.CONTENT_ROW;
        placeSearchRow(spec, player, query, mainTop);
        flowLabel(spec, crumb);
        if (tab != null && !tab.editable() && !searching) {
            flowLabel(spec, "This category is closed - player records are not edited here.");
        }
        if (!searching && !groupPath.isBlank()) {
            String parent = ConfigEditor.isSectionPath(groupPath)
                    ? ""
                    : ConfigEditor.parentPath(groupPath);
            String upLabel = ConfigEditor.isSectionPath(groupPath) ? "All sections" : "Up one group";
            spec.button(
                            "page:" + editPage(module, 0, parent),
                            UltraStyle.dim(upLabel),
                            UltraStyle.STYLE_DIM,
                            MAIN_X,
                            spec.row(),
                            Math.min(160, MAIN_W))
                    .rowGap();
        }

        if (showIndex) {
            if (sections.isEmpty()) {
                flowLabel(spec, "No settings in this category.");
            }
            for (ConfigEditor.SectionLink section : sections) {
                if (section == null) {
                    continue;
                }
                String count = section.count() == 1 ? "1 setting" : section.count() + " settings";
                spec.button(
                                "page:" + editPage(module, 0, section.openPath()),
                                UltraStyle.LABEL + clipWords(section.title() + " (" + count + ")", FLOW_CHARS),
                                UltraStyle.STYLE_NORMAL,
                                MAIN_X,
                                spec.row(),
                                MAIN_W)
                        .rowGap();
            }
        } else if (from >= to) {
            flowLabel(
                    spec,
                    searching ? "No settings match that search." : "No settings in this section.");
        }
        if (!showIndex) {
            for (int i = from; i < to; i++) {
                appendSetting(spec, player, uuid, rows.get(i));
            }
        }

        if (current > 0) {
            spec.button(
                    "page:" + editPage(module, current - 1, groupPath),
                    UltraStyle.dim("Prev"),
                    UltraStyle.STYLE_DIM,
                    MAIN_X,
                    spec.row(),
                    80);
        }
        if (current + 1 < pages) {
            spec.button(
                    "page:" + editPage(module, current + 1, groupPath),
                    UltraStyle.LABEL + "Next",
                    UltraStyle.STYLE_NORMAL,
                    MAIN_X + 90,
                    spec.row(),
                    80);
        }
        if (current > 0 || current + 1 < pages) {
            spec.rowGap();
        }

        placeActionFooter(spec);
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void placeCategoryRail(
            UltraScreenSpec spec, String activeModule, boolean searching, int startY) {
        int y = startY;
        for (String[] cat : CATEGORIES) {
            String id = cat[0];
            String label = cat[1];
            boolean active = !searching && id.equalsIgnoreCase(activeModule);
            String style = active ? UltraStyle.STYLE_CONFIRM : UltraStyle.STYLE_DIM;
            String text = active ? UltraStyle.confirm(label) : UltraStyle.dim(label);
            // Chrome: left rail must not set firstButtonY or body text collapses onto controls.
            spec.chromeButton("do:select_module:" + id, text, style, SIDE_X, y, SIDE_W);
            y += UltraStyle.BTN_H + 2;
        }
    }

    private static void placeSearchRow(
            UltraScreenSpec spec, ServerPlayer player, String query, int y) {
        noteIfTextInputMissing(spec, player);
        spec.setRow(y);
        String inputId = "input:search";
        // Full main-column width; Search then Clear stacked so small widths never overlap.
        spec.textFieldRow(inputId, query == null ? "" : query, "Search settings...", MAIN_X, MAIN_W);
        spec.rowGap(LABEL_CTRL_GAP);
        spec.buttonBound(
                        "do:search",
                        UltraStyle.confirm("Search"),
                        UltraStyle.STYLE_CONFIRM,
                        MAIN_X,
                        spec.row(),
                        MAIN_W,
                        inputId)
                .rowGap();
        spec.button(
                        "do:search_clear",
                        UltraStyle.dim("Clear"),
                        UltraStyle.STYLE_DIM,
                        MAIN_X,
                        spec.row(),
                        MAIN_W)
                .rowGap();
    }

    private static void placeActionFooter(UltraScreenSpec spec) {
        // Pinned action band above nav footer — stacked for small UI (no side-by-side squeeze).
        spec.button(
                        "page:review",
                        UltraStyle.confirm("Review changes"),
                        UltraStyle.STYLE_CONFIRM,
                        MAIN_X,
                        spec.row(),
                        MAIN_W)
                .rowGap();
        spec.button(
                        "do:reload_module",
                        UltraStyle.LABEL + "Reload disk",
                        UltraStyle.STYLE_NORMAL,
                        MAIN_X,
                        spec.row(),
                        MAIN_W)
                .rowGap();
        spec.button(
                        "do:discard_all",
                        UltraStyle.danger("Discard changes"),
                        UltraStyle.STYLE_DANGER,
                        MAIN_X,
                        spec.row(),
                        MAIN_W)
                .rowGap();
    }

    /**
     * Setting title/description as a main-column flow row (not body {@code line}), so the label
     * sits above its control instead of scrolling in a band that overlaps buttons.
     */
    private static void flowLabel(UltraScreenSpec spec, String text) {
        if (spec == null || text == null || text.isBlank()) {
            return;
        }
        spec.button(
                        "do:refresh",
                        UltraStyle.LABEL + clipWords(UltraStyle.plain(text), FLOW_CHARS),
                        UltraStyle.STYLE_DIM,
                        MAIN_X,
                        spec.row(),
                        MAIN_W)
                .rowGap(LABEL_CTRL_GAP);
    }

    /** Up to three flow rows so a one-line description is not cut off mid-sentence. */
    private static void flowText(UltraScreenSpec spec, String text) {
        if (spec == null || text == null || text.isBlank()) {
            return;
        }
        String rest = UltraStyle.plain(text).trim();
        for (int i = 0; i < 3 && !rest.isEmpty(); i++) {
            if (rest.length() <= FLOW_CHARS) {
                flowLabel(spec, rest);
                return;
            }
            String cut = rest.substring(0, FLOW_CHARS);
            int sp = cut.lastIndexOf(' ');
            int at = sp >= FLOW_CHARS / 2 ? sp : FLOW_CHARS;
            flowLabel(spec, rest.substring(0, at).trim());
            rest = rest.substring(at).trim();
        }
    }

    private static void appendSetting(
            UltraScreenSpec spec, ServerPlayer player, UUID uuid, Row row) {
        ConfigEditor.Entry entry = row.entry();
        String module = row.moduleId();
        String path = entry.path();
        boolean dirty = ConfigEditor.dirty(uuid, module, path);
        String name = ConfigEditor.staffLabel(path);
        String raw = ConfigEditor.effectiveRaw(uuid, module, path);
        String value = ConfigEditor.shownValue(uuid, module, path);
        String desc = ConfigEditor.friendlyDescription(module, path, raw == null ? "" : raw);
        String section = ConfigEditor.sectionTitle(module, path);
        String crumb = row.searchHit()
                ? row.moduleLabel() + " > " + (section.isBlank() || section.equalsIgnoreCase(name)
                        ? name
                        : section + " > " + name)
                : name;
        String mark = dirty ? "* " : "";
        // Label on top, control below — flow rows, never body lines mixed with absolute buttons.
        flowText(spec, mark + crumb);
        flowText(spec, desc);
        String now = value == null ? "" : value;
        String def = ConfigEditor.defaultShown(module, path);
        flowLabel(spec, "Now: " + clipWords(now, 28));
        if (def != null && !def.isBlank() && !"group".equalsIgnoreCase(def)) {
            flowLabel(spec, "Default: " + clipWords(def, 24));
        }
        String invalid = ConfigEditor.invalidMessage(uuid, module, path);
        if (invalid != null && !invalid.isBlank()) {
            flowLabel(spec, UltraStyle.plain(invalid));
        }
        ConfigEditor.EditorTab moduleTab = ConfigEditor.tab(module);
        if (moduleTab == null || !moduleTab.editable()) {
            flowLabel(spec, "Closed - " + shortVal(value));
            return;
        }
        appendInlineControl(spec, player, module, path, entry.kind(), value, raw);
        if (dirty) {
            spec.button(
                            "do:undo:" + module + "|" + encodePath(path),
                            UltraStyle.dim("Undo"),
                            UltraStyle.STYLE_DIM,
                            MAIN_X,
                            spec.row(),
                            Math.min(80, MAIN_W))
                    .rowGap();
        }
    }

    /**
     * Inline control for one setting. Case labels stay for audits that require typed
     * {@code textFieldRow} + Apply on NUMBER / ENUM / LIST. Controls stack under the label;
     * side-by-side only when the main column has room (see {@link #fitsRow}).
     */
    private static void appendInlineControl(
            UltraScreenSpec spec,
            ServerPlayer player,
            String module,
            String path,
            ConfigEditor.Kind kind,
            String value,
            String raw) {
        String encoded = encodePath(path);
        switch (kind) {
            case BOOLEAN -> {
                boolean on = "ON".equalsIgnoreCase(value) || "true".equalsIgnoreCase(raw);
                spec.button(
                                "do:toggle:" + module + "|" + encoded,
                                on ? UltraStyle.confirm("On") : UltraStyle.dim("Off"),
                                on ? UltraStyle.STYLE_CONFIRM : UltraStyle.STYLE_DIM,
                                MAIN_X,
                                spec.row(),
                                Math.min(100, MAIN_W))
                        .rowGap();
            }
            case NUMBER -> {
                noteIfTextInputMissing(spec, player);
                String inputId = "input:" + module + "|" + encoded;
                spec.textFieldRow(inputId, value == null ? "" : value, "Type a number...", MAIN_X, MAIN_W);
                spec.rowGap(LABEL_CTRL_GAP);
                // Apply on its own row; +/- share a row only when width allows.
                spec.buttonBound(
                                "do:set:" + module + "|" + encoded,
                                UltraStyle.confirm("Apply"),
                                UltraStyle.STYLE_CONFIRM,
                                MAIN_X,
                                spec.row(),
                                MAIN_W,
                                inputId)
                        .rowGap();
                placeStepPair(spec, module, encoded);
            }
            case ENUM -> {
                noteIfTextInputMissing(spec, player);
                String inputId = "input:" + module + "|" + encoded;
                spec.textFieldRow(inputId, value == null ? "" : value, "Type an option...", MAIN_X, MAIN_W);
                spec.rowGap(LABEL_CTRL_GAP);
                if (fitsRow(90, 90)) {
                    spec.buttonBound(
                                    "do:set:" + module + "|" + encoded,
                                    UltraStyle.confirm("Apply"),
                                    UltraStyle.STYLE_CONFIRM,
                                    MAIN_X,
                                    spec.row(),
                                    90,
                                    inputId)
                            .button(
                                    "do:cycle:" + module + "|" + encoded,
                                    UltraStyle.confirm("Next"),
                                    UltraStyle.STYLE_CONFIRM,
                                    MAIN_X + 90 + CTRL_GAP,
                                    spec.row(),
                                    90)
                            .rowGap();
                } else {
                    spec.buttonBound(
                                    "do:set:" + module + "|" + encoded,
                                    UltraStyle.confirm("Apply"),
                                    UltraStyle.STYLE_CONFIRM,
                                    MAIN_X,
                                    spec.row(),
                                    MAIN_W,
                                    inputId)
                            .rowGap();
                    spec.button(
                                    "do:cycle:" + module + "|" + encoded,
                                    UltraStyle.confirm("Next"),
                                    UltraStyle.STYLE_CONFIRM,
                                    MAIN_X,
                                    spec.row(),
                                    MAIN_W)
                            .rowGap();
                }
            }
            case LIST, MAP, GROUP -> {
                ConfigEditor.AddMode addMode = ConfigEditor.addMode(module, path);
                if (addMode != ConfigEditor.AddMode.NONE) {
                    noteIfTextInputMissing(spec, player);
                    String inputId = "input:" + module + "|" + encoded;
                    String placeholder = addMode == ConfigEditor.AddMode.KEY_AND_VALUE
                            ? "Type key=value..."
                            : "Type a new entry...";
                    spec.textFieldRow(inputId, "", placeholder, MAIN_X, MAIN_W);
                    spec.rowGap(LABEL_CTRL_GAP);
                    spec.buttonBound(
                                    "do:add:" + module + "|" + encoded,
                                    UltraStyle.confirm("Apply"),
                                    UltraStyle.STYLE_CONFIRM,
                                    MAIN_X,
                                    spec.row(),
                                    MAIN_W,
                                    inputId)
                            .rowGap();
                }
                // Open nested entries in the same editor (no detail sub-page).
                spec.button(
                                "page:" + editPage(module, 0, path),
                                UltraStyle.LABEL + "Open " + shortVal(value),
                                UltraStyle.STYLE_NORMAL,
                                MAIN_X,
                                spec.row(),
                                MAIN_W)
                        .rowGap();
            }
            case TEXT -> {
                noteIfTextInputMissing(spec, player);
                String inputId = "input:" + module + "|" + encoded;
                spec.textFieldRow(inputId, value == null ? "" : value, "Type a value...", MAIN_X, MAIN_W);
                spec.rowGap(LABEL_CTRL_GAP);
                spec.buttonBound(
                                "do:set:" + module + "|" + encoded,
                                UltraStyle.confirm("Apply"),
                                UltraStyle.STYLE_CONFIRM,
                                MAIN_X,
                                spec.row(),
                                MAIN_W,
                                inputId)
                        .rowGap();
            }
            default -> flowLabel(spec, "This field type has no Ultra editor yet.");
        }
    }

    private static void placeStepPair(UltraScreenSpec spec, String module, String encoded) {
        int stepW = UltraStyle.BTN_MIN_W;
        if (fitsRow(stepW, stepW)) {
            spec.button(
                            "do:step:" + module + "|" + encoded + "|-1",
                            UltraStyle.dim("-"),
                            UltraStyle.STYLE_DIM,
                            MAIN_X,
                            spec.row(),
                            stepW)
                    .button(
                            "do:step:" + module + "|" + encoded + "|1",
                            UltraStyle.dim("+"),
                            UltraStyle.STYLE_DIM,
                            MAIN_X + stepW + CTRL_GAP,
                            spec.row(),
                            stepW)
                    .rowGap();
            return;
        }
        spec.button(
                        "do:step:" + module + "|" + encoded + "|-1",
                        UltraStyle.dim("-"),
                        UltraStyle.STYLE_DIM,
                        MAIN_X,
                        spec.row(),
                        MAIN_W)
                .rowGap();
        spec.button(
                        "do:step:" + module + "|" + encoded + "|1",
                        UltraStyle.dim("+"),
                        UltraStyle.STYLE_DIM,
                        MAIN_X,
                        spec.row(),
                        MAIN_W)
                .rowGap();
    }

    /** True when {@code leftW + gap + rightW} fits in the main column. */
    private static boolean fitsRow(int leftW, int rightW) {
        return leftW + CTRL_GAP + rightW <= MAIN_W;
    }

    private static void openReview(ServerPlayer player) {
        List<ConfigEditor.PendingChange> pending = ConfigEditor.pendingChanges(player.m_20148_());
        int count = pending == null ? 0 : pending.size();
        UltraScreenSpec spec = UltraScreenSpec.of("config")
                .title("Review changes")
                .page("review")
                .panel(PANEL_W, UltraStyle.PANEL_MAX_H)
                .labelValue("Pending", String.valueOf(count));
        if (count == 0) {
            spec.line(UltraStyle.dim("No unsaved changes."));
            spec.line(UltraStyle.DIM + "Nothing is written until you confirm a change.");
        } else {
            spec.line(UltraStyle.LABEL + "These edits are not saved yet.");
            spec.line(UltraStyle.DIM + "Confirm save writes them. Discard throws them away.");
            int shown = 0;
            for (ConfigEditor.PendingChange change : pending) {
                if (change == null) {
                    continue;
                }
                if (shown >= 12) {
                    break;
                }
                spec.line(UltraStyle.LABEL + change.name());
                spec.line(UltraStyle.DIM + "Now " + prettyRaw(change.before())
                        + " -> " + UltraStyle.VALUE + prettyRaw(change.after()));
                shown++;
            }
            if (count > shown) {
                spec.line(UltraStyle.DIM + "And " + (count - shown) + " more.");
            }
        }
        spec.buttonsAfterLines();
        // Stack Confirm above Discard - side-by-side is a misclick wipe risk.
        spec.button("do:save_all", UltraStyle.confirm("Confirm save"), "confirm").rowGap();
        spec.button("do:discard_all", UltraStyle.danger("Discard all"), "danger").rowGap();
        spec.footerBack("page:" + ConfigEditor.editorPage(player.m_20148_()), "Config Editor");
        spec.send(player);
    }

    private static String prettyRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return "empty";
        }
        if ("true".equalsIgnoreCase(raw)) {
            return "ON";
        }
        if ("false".equalsIgnoreCase(raw)) {
            return "OFF";
        }
        return raw;
    }

    private static String categoryLabel(String module, ConfigEditor.EditorTab tab) {
        for (String[] cat : CATEGORIES) {
            if (cat[0].equalsIgnoreCase(module)) {
                return cat[1];
            }
        }
        return tab == null ? module : tab.label();
    }

    private static String breadcrumb(String category, String groupPath, boolean searching) {
        if (searching) {
            return "Search";
        }
        if (groupPath == null || groupPath.isBlank()) {
            return category;
        }
        if (ConfigEditor.isSectionPath(groupPath)) {
            return category + " > " + groupPath.substring(ConfigEditor.SECTION_PREFIX.length());
        }
        return category + " > " + ConfigEditor.staffLabel(groupPath);
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
                        ? UltraStyle.noticeLine("Reloaded " + module + " from disk.")
                        : UltraStyle.dangerLine(error);
            }
            case "select_module", "open_module" -> {
                ConfigEditor.rememberSearch(uuid, "");
                yield "";
            }
            case "search" -> {
                String typed = typedValueFromArgs(args, "search");
                if (typed == null) {
                    typed = a;
                }
                ConfigEditor.rememberSearch(uuid, typed == null ? "" : typed);
                yield typed == null || typed.isBlank()
                        ? UltraStyle.noticeLine("Search cleared.")
                        : UltraStyle.noticeLine("Showing matches for \"" + shortVal(typed) + "\".");
            }
            case "search_clear" -> {
                ConfigEditor.rememberSearch(uuid, "");
                yield UltraStyle.noticeLine("Search cleared.");
            }
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
                String typed = typedValueFromArgs(args, "set");
                if (typed == null) {
                    yield UltraStyle.dangerLine(
                            "Type a value in the field, then press Apply.");
                }
                String error = ConfigEditor.stageSet(uuid, parts[0], parts[1], typed);
                yield error == null
                        ? UltraStyle.noticeLine("Staged new value.")
                        : UltraStyle.dangerLine(error);
            }
            case "add" -> {
                String[] parts = splitModulePath(a);
                if (parts == null) {
                    yield UltraStyle.dangerLine("Missing field.");
                }
                String typed = typedValueFromArgs(args, "add");
                if (typed == null || typed.isBlank()) {
                    yield UltraStyle.dangerLine(
                            "Type a value in the field, then press Apply.");
                }
                String key = "";
                String raw = typed.trim();
                if (ConfigEditor.addMode(parts[0], parts[1]) == ConfigEditor.AddMode.KEY_AND_VALUE) {
                    int sep = raw.indexOf('=');
                    if (sep < 0) {
                        sep = raw.indexOf(':');
                    }
                    if (sep > 0) {
                        key = raw.substring(0, sep).trim();
                        raw = raw.substring(sep + 1).trim();
                    } else {
                        key = raw;
                        raw = "";
                    }
                }
                String error = ConfigEditor.stageAdd(uuid, parts[0], parts[1], key, raw);
                yield error == null
                        ? UltraStyle.noticeLine("Staged new entry.")
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

    private static String editPage(String module, int page, String path) {
        return "edit:" + module + ":" + page + ":" + (path == null ? "" : path);
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

    private static String shortVal(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String plain = UltraStyle.plain(value);
        return clipWords(plain, 16);
    }

    /** Keep the longest leading words that fit {@code max} glyphs. No ellipsis. */
    private static String clipWords(String plain, int max) {
        if (plain == null || plain.isBlank() || max <= 0) {
            return "";
        }
        String text = plain.trim();
        if (text.length() <= max) {
            return text;
        }
        String cut = text.substring(0, max);
        int sp = cut.lastIndexOf(' ');
        if (sp >= max / 2) {
            return cut.substring(0, sp).trim();
        }
        return cut.trim();
    }

    private static String moduleFromPage(String page) {
        if (page == null || page.isBlank()) {
            return "difficulty";
        }
        String p = page.trim();
        if (p.regionMatches(true, 0, "module:", 0, 7)
                || p.regionMatches(true, 0, "list:", 0, 5)
                || p.regionMatches(true, 0, "detail:", 0, 7)
                || p.regionMatches(true, 0, "edit:", 0, 5)) {
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

    /**
     * KaiPath 1.3.9 copies the bound EditBox into {@code args.value} when a buttonBound
     * fires. Log every key so a client/key mismatch is obvious in server logs, and fall
     * back to {@code bindInput} / {@code input:*} keys if {@code value} is absent.
     */
    private static String typedValueFromArgs(com.google.gson.JsonObject args, String op) {
        if (args == null) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] config {} args=null (no typed value)",
                    AdaptiveDifficultyMod.MOD_ID,
                    op);
            return null;
        }
        StringBuilder keys = new StringBuilder();
        for (Map.Entry<String, JsonElement> entry : args.entrySet()) {
            if (keys.length() > 0) {
                keys.append(',');
            }
            keys.append(entry.getKey());
        }
        String direct = argsString(args, "value");
        AdaptiveDifficultyMod.LOGGER.debug(
                "[{}] config {} args.keys=[{}] valuePresent={} valueLen={}",
                AdaptiveDifficultyMod.MOD_ID,
                op,
                keys,
                direct != null,
                direct == null ? -1 : direct.length());
        if (direct != null) {
            return direct;
        }
        String bind = argsString(args, "bindInput");
        if (bind != null && !bind.isBlank()) {
            String viaBind = argsString(args, bind);
            if (viaBind == null) {
                String bare = bind.contains(":") ? bind.substring(bind.indexOf(':') + 1) : bind;
                viaBind = argsString(args, bare);
            }
            if (viaBind != null) {
                AdaptiveDifficultyMod.LOGGER.debug(
                        "[{}] config {} recovered typed value from bindInput key={}",
                        AdaptiveDifficultyMod.MOD_ID,
                        op,
                        bind);
                return viaBind;
            }
        }
        for (Map.Entry<String, JsonElement> entry : args.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            String lower = key.toLowerCase(Locale.ROOT);
            if (lower.startsWith("input:") || "text".equals(lower) || "typed".equals(lower)) {
                String alt = argsString(args, key);
                if (alt != null) {
                    AdaptiveDifficultyMod.LOGGER.debug(
                            "[{}] config {} recovered typed value from alt key={}",
                            AdaptiveDifficultyMod.MOD_ID,
                            op,
                            key);
                    return alt;
                }
            }
        }
        return null;
    }

    /** Older KaiPath without field 11 - surface that before a dead Apply button. */
    private static void noteIfTextInputMissing(UltraScreenSpec spec, ServerPlayer player) {
        if (spec == null || UltraClientFeatures.hasTextInput(player)) {
            return;
        }
        spec.line(UltraStyle.danger("Typed fields need KaiPath 1.3.9 or newer."))
                .line(UltraStyle.DIM + "Until then use Toggle / Next value / +/- step buttons.");
    }

    private record Row(String moduleId, String moduleLabel, ConfigEditor.Entry entry, boolean searchHit) {}
}
