package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.gui.AdminInspectSessions;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.gui.IButton;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IComponentsScrollableWrapper;
import noppes.npcs.api.gui.ILabel;
import noppes.npcs.api.gui.IScroll;

/** Shared CustomNPCs layout + show helpers for Legacy Mechanics UI. */
public final class CnpcGuiSupport {
    public static final int W = 430;
    public static final int H = 280;
    public static final int M = 14;
    public static final int BTN_H = 20;
    public static final int COL_L = M;
    /** Two-column button width — keeps {@link CnpcPlayerPreview#contentRightEdge()} clear of overlap. */
    public static final int BTN_W;
    public static final int COL_R;

    static {
        int right = CnpcPlayerPreview.contentRightEdge();
        int gap = 8;
        BTN_W = Math.max(130, (right - COL_L - gap) / 2);
        COL_R = COL_L + BTN_W + gap;
    }
    public static final int ROW_STEP = 24;
    public static final int LINE_H = 13;
    /** Default height for rival/spar/character player lists. */
    public static final int SCROLL_LIST_H = 160;
    /** Approximate CNPC scroll row height for overflow detection. */
    private static final int SCROLL_ROW_H = 14;
    /** Space reserved at bottom for nav / close row (pixels). */
    public static final int FOOTER_RESERVE = 48;

    /** Reserved widget ids — one role per screen; never reuse on the same gui instance. */
    public static final int ID_TITLE = 1;
    public static final int ID_SUBTITLE = 2;
    public static final int ID_DIVIDER = 3;
    public static final int ID_INSPECT = 4;
    public static final int ID_STATUS_TAG = 5;
    public static final int ID_INFO_SCROLL = 100;
    public static final int ID_LIST_SCROLL = 101;
    public static final int ID_INFO_LABEL_BASE = 10;
    public static final int ID_BTN_BASE = 20;
    public static final int ID_NAV_HUB = 96;
    public static final int ID_NAV_BACK = 97;
    public static final int ID_CLOSE = 98;
    public static final int ID_REFRESH = 99;
    public static final int ID_ENTITY_PREVIEW = 102;
    /** Static note label on the same row as an action button (never reuse the button id). */
    public static final int ID_INLINE_NOTE = 115;
    /** Extra staff-only control (never {@link #ID_CLOSE}). */
    public static final int ID_STAFF_EXTRA = 116;

    /** Shorter divider so labels do not wrap oddly in CNPC. */
    private static String dividerText() {
        int chars = Math.max(14, textBandWidth() / 7);
        return "§8" + "─".repeat(chars);
    }

    private static final Pattern PACKAGE_LIKE = Pattern.compile("(?:\\b[a-z]{2,}\\.){2,}[A-Za-z0-9_$]+");
    private static final Pattern UUID_LINE = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE);

    private CnpcGuiSupport() {}

    public static boolean cnpcReady(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (!NpcAPI.IsAvailable()) {
            feedbackChat(player, "§cThis menu needs CustomNPCs on your client and the server.");
            return false;
        }
        return true;
    }

    public static IPlayer<?> wrap(ServerPlayer player) {
        try {
            IEntity entity = NpcAPI.Instance().getIEntity(player);
            if (entity instanceof IPlayer<?> ip) {
                return ip;
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] CNPC wrap failed: {}", AdaptiveDifficultyMod.MOD_ID, t);
        }
        return null;
    }

    public static void show(ServerPlayer player, int guiId, Painter painter) {
        showSized(player, guiId, W, H, painter);
    }

    public static void showSized(ServerPlayer player, int guiId, int width, int height, Painter painter) {
        IPlayer<?> ip = wrap(player);
        if (ip == null) {
            feedbackChat(player, "§cCould not open the menu. Try relogging, then open it again.");
            return;
        }
        try {
            ICustomGui gui = NpcAPI.Instance().createCustomGui(guiId, width, height, false, ip);
            gui.setClosesOnEsc(true);
            painter.paint(player, gui);
            ip.showCustomGui(gui);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] CNPC GUI {} failed: {}", AdaptiveDifficultyMod.MOD_ID, guiId, t);
            feedbackChat(player, "§cSomething went wrong opening the menu. Ask staff if this keeps happening.");
        }
    }

    /** Title + subtitle + optional inspect line + divider. Returns Y for the info block. */
    public static int paintHeader(ServerPlayer viewer, ICustomGui gui, String title, String subtitle) {
        title(gui, ID_TITLE, title);
        subtitle(gui, ID_SUBTITLE, subtitle == null ? "" : subtitle);
        inspectBanner(viewer, gui);
        boolean inspecting = AdminInspectSessions.isInspecting(viewer.m_20148_());
        int dividerY = inspecting ? 52 : 38;
        divider(gui, ID_DIVIDER, dividerY);
        return paintFlashNotice(viewer, gui, dividerY + 10);
    }

    /** Recent action result / error band (consumed once). Returns Y for the main info block. */
    public static int paintFlashNotice(ServerPlayer viewer, ICustomGui gui, int y) {
        List<String> raw = CnpcMenuFeedback.take(viewer);
        if (raw.isEmpty()) {
            return y;
        }
        List<String> box = new ArrayList<>();
        box.add("§eNotice");
        box.addAll(raw);
        return paintInfoBlock(gui, y, box, CnpcGuiStyle.INFO_INLINE_MAX);
    }

    public static void pushMenuMessage(ServerPlayer player, String message) {
        CnpcMenuFeedback.set(player, message);
    }

    /** Set message and reopen the Legacy Mechanics hub (access denied, etc.). */
    public static void denyToHub(ServerPlayer player, String message) {
        pushMenuMessage(player, message);
        CnpcLmHubGui.open(player, "main");
    }

    /**
     * Max list scroll height that fits above footer rows. {@code rowsBelowList} = button rows under
     * the list (actions + nav, or nav only).
     */
    public static int listScrollHeight(ICustomGui gui, int listY, int rowsBelowList) {
        if (gui == null) {
            return SCROLL_LIST_H;
        }
        int rows = Math.max(1, rowsBelowList);
        int maxBottom = gui.getHeight() - FOOTER_RESERVE - rows * ROW_STEP - 4;
        return Math.max(48, Math.min(SCROLL_LIST_H, maxBottom - listY));
    }

    /** Nav row Y after a scroll list. {@code extraActionRows} = full button rows above nav. */
    public static int navRowAfterScroll(int listY, int scrollH, int extraActionRows) {
        return listY + scrollH + 8 + Math.max(0, extraActionRows) * ROW_STEP;
    }

    public static int navRowAfterScroll(int listY, int scrollH) {
        return navRowAfterScroll(listY, scrollH, 0);
    }

    public static IScroll scrollList(
            ICustomGui gui, int listY, String[] items, int actionRowsAboveFooter, boolean searchable) {
        int h = listScrollHeight(gui, listY, actionRowsAboveFooter);
        if (searchable) {
            return scrollSearchable(gui, ID_LIST_SCROLL, M, listY, textBandWidth(), h, items);
        }
        return scroll(gui, ID_LIST_SCROLL, M, listY, textBandWidth(), h, items);
    }

    /**
     * Chest-style status block: a few inline lines, or a scroll when there is more text.
     * Returns the Y coordinate where action buttons should start (with padding).
     */
    /** Small section caption above a button group. Returns Y for the first button row. */
    public static int paintSectionTag(ICustomGui gui, int labelId, int y, String caption) {
        gui.addLabel(labelId, safeChat(caption), M, y, textBandWidth(), 10);
        return y + 16;
    }

    public static int paintInfoBlock(ICustomGui gui, int startY, List<String> lines, int inlineMax) {
        return paintInfoBlock(gui, startY, lines, inlineMax, false);
    }

    /**
     * Short status above a pick list. Does not use {@link #getScrollingPanel()} — CNPC only supports
     * one scroll region; {@link #scrollSearchable} must own it or the list will not scroll.
     */
    public static int paintInfoBeforePickList(ICustomGui gui, int startY, List<String> lines, int maxInlineLines) {
        return paintInfoBlock(gui, startY, lines, maxInlineLines, true);
    }

    private static int paintInfoBlock(
            ICustomGui gui, int startY, List<String> lines, int inlineMax, boolean reservePickListScroll) {
        List<String> clean = normalizeInfoLines(lines);
        if (clean.isEmpty()) {
            return startY + 4;
        }
        int maxInline = inlineMax <= 0 ? 0 : Math.max(1, inlineMax);
        int textW = textBandWidth();
        if (reservePickListScroll && maxInline > 0) {
            if (clean.size() > maxInline) {
                List<String> trimmed = new ArrayList<>(clean.subList(0, maxInline));
                trimmed.add("§8More summary text is hidden above the list.");
                clean = trimmed;
            }
            bodyLines(gui, ID_INFO_LABEL_BASE, startY, clean, clean.size(), textW);
            return startY + clean.size() * LINE_H + 10;
        }
        if (maxInline > 0 && clean.size() <= maxInline) {
            bodyLines(gui, ID_INFO_LABEL_BASE, startY, clean, maxInline, textW);
            return startY + clean.size() * LINE_H + 10;
        }
        int visibleStatusRows = Math.max(1, (112 / LINE_H));
        if (clean.size() <= visibleStatusRows) {
            bodyLines(gui, ID_INFO_LABEL_BASE, startY, clean, clean.size(), textW);
            return startY + clean.size() * LINE_H + 10;
        }
        gui.addLabel(ID_STATUS_TAG, CnpcGuiStyle.HINT_SCROLL_STATUS, M, startY - 2, textW, 10);
        int preferred = Math.min(112, Math.max(56, clean.size() * LINE_H));
        int scrollH = preferred;
        if (gui != null) {
            int maxBottom = gui.getHeight() - FOOTER_RESERVE - ROW_STEP - 4;
            scrollH = Math.max(48, Math.min(preferred, maxBottom - startY - 8));
        }
        int bandY = startY + 8;
        int bandW = textW;
        IComponentsScrollableWrapper panel = gui.getScrollingPanel();
        panel.init(M, bandY, bandW, scrollH);
        for (int i = 0; i < clean.size(); i++) {
            panel.addLabel(ID_INFO_LABEL_BASE + i, safeScrollLine(clean.get(i)), 0, i * LINE_H, bandW, LINE_H);
        }
        return bandY + scrollH + 10;
    }

    public static int suggestHeight(int actionBottomY) {
        return Math.max(H, Math.min(460, actionBottomY + FOOTER_RESERVE + 8));
    }

    public static void footerCloseRefresh(ServerPlayer player, ICustomGui gui, int row, Runnable refresh) {
        buttonSmall(gui, ID_CLOSE, "§cClose", COL_L, row, 95, () -> {});
        buttonSmall(gui, ID_REFRESH, "§7Refresh", COL_R, row, 95, refresh);
    }

    public static ILabel title(ICustomGui gui, int id, String text) {
        ILabel l = gui.addLabel(id, safeChat(text), M, 8, textBandWidth(), 18);
        try {
            l.setScale(1.15f);
        } catch (Throwable ignored) {
        }
        return l;
    }

    public static void subtitle(ICustomGui gui, int id, String text) {
        gui.addLabel(id, safeChat(text), M, 28, textBandWidth(), 14);
    }

    /** Data/actions target (inspect subject when staff is inspecting). */
    public static net.minecraft.server.level.ServerPlayer target(net.minecraft.server.level.ServerPlayer viewer) {
        return AdminInspectSessions.resolveSubject(viewer);
    }

    public static void inspectBanner(net.minecraft.server.level.ServerPlayer viewer, ICustomGui gui) {
        String line = AdminInspectSessions.inspectBannerLine(viewer);
        if (line != null) {
            gui.addLabel(ID_INSPECT, safeChat(line), M, 40, textBandWidth(), 12);
        }
    }

    public static void divider(ICustomGui gui, int id, int y) {
        int w = textBandWidth();
        gui.addLabel(id, dividerText(), M, y, w, 10);
    }

    public static int textBandWidth() {
        return CnpcPlayerPreview.textBandWidth();
    }

    /** Scroll lists and full-width labels — never {@code W - 2*M} (that overlaps the preview column). */
    public static int listWidth() {
        return textBandWidth();
    }

    /**
     * Stable Y for the right-column character preview on every LM menu (does not move when flash
     * messages or long info blocks change {@link #paintHeader} return value).
     */
    public static int previewAnchorY(net.minecraft.server.level.ServerPlayer viewer) {
        boolean inspecting = AdminInspectSessions.isInspecting(viewer.m_20148_());
        int dividerY = inspecting ? 52 : 38;
        return dividerY + 10;
    }

    /** First Y for lists, buttons, or grids directly under an info block (left column). */
    public static int bodyBelowInfo(int rowAfterBlock) {
        return rowAfterBlock + 4;
    }

    /**
     * First Y for lists/buttons after {@link #paintHeader} — uses the header's returned {@code infoY}
     * so flash notices and status blocks do not overlap pickers.
     */
    public static int bodyBelowHeader(int infoY) {
        return bodyBelowInfo(infoY);
    }

    /** @see CnpcPlayerPreview#paint(ServerPlayer, ICustomGui, int) */
    public static void paintPlayerPreview(ServerPlayer player, ICustomGui gui, int x, int y) {
        CnpcPlayerPreview.paint(player, gui, ID_ENTITY_PREVIEW, x, y);
    }

    public static void paintPlayerPreviewSlot(ServerPlayer player, ICustomGui gui, int anchorY) {
        CnpcPlayerPreview.paint(player, gui, anchorY);
    }

    /** Paint preview last at {@link #previewAnchorY(ServerPlayer)} (ProfTools-style). */
    public static void paintSubjectPreview(ServerPlayer subject, ICustomGui gui, ServerPlayer layoutViewer) {
        if (subject != null && layoutViewer != null) {
            paintPlayerPreviewSlot(subject, gui, previewAnchorY(layoutViewer));
        }
    }

    /** Character preview on a system's top-level menu only (not subpages). */
    public static void paintSystemMainPreview(ServerPlayer subject, ICustomGui gui, ServerPlayer layoutViewer) {
        paintSubjectPreview(subject, gui, layoutViewer);
    }

    public static void bodyLines(ICustomGui gui, int startId, int y, List<String> lines, int maxLines) {
        bodyLines(gui, startId, y, lines, maxLines, textBandWidth());
    }

    public static void bodyLines(ICustomGui gui, int startId, int y, List<String> lines, int maxLines, int width) {
        int row = y;
        int n = 0;
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (n >= maxLines) {
                break;
            }
            gui.addLabel(startId + n, safeChat(line), M, row, width, 12);
            row += LINE_H;
            n++;
        }
    }

    /**
     * Interactive pick list inside the scrolling panel (clicks work). Mouse wheel on the list uses
     * the on-screen scroll bar; {@link #paintInfoBlock} status bands use wheel via label stacks.
     */
    public static IScroll scroll(ICustomGui gui, int id, int x, int y, int w, int h, String[] items) {
        String[] safe = items == null ? new String[0] : items;
        String[] copy = new String[safe.length];
        for (int i = 0; i < safe.length; i++) {
            copy[i] = safeScrollLine(safe[i]);
        }
        int bandW = textBandWidth();
        int useW = Math.min(w, bandW);
        IComponentsScrollableWrapper panel = gui.getScrollingPanel();
        panel.init(x, y, useW, h);
        return panel.addScroll(id, 0, 0, useW, h, copy);
    }

    /** Long read-only copy — inline when short, scroll band only when needed. */
    public static int paintReadOnlyScroll(ICustomGui gui, int startY, List<String> lines) {
        return paintInfoBlock(gui, startY, lines, CnpcGuiStyle.INFO_INLINE_MAX);
    }

    /** Scrollable read-only body under {@link #paintHeader}; returns Y for footer nav. */
    public static int paintLongReadOnlyBody(ICustomGui gui, int infoY, List<String> lines) {
        List<String> clean = normalizeInfoLines(lines);
        if (clean.size() <= CnpcGuiStyle.INFO_INLINE_MAX) {
            return bodyBelowInfo(paintInfoBlock(gui, infoY, clean, CnpcGuiStyle.INFO_INLINE_MAX));
        }
        return bodyBelowInfo(paintReadOnlyScroll(gui, infoY, clean));
    }

    /** Suggested window height for a scroll body ending at {@code rowAfterBody}. */
    public static int heightForScrollPage(int rowAfterBody) {
        return suggestHeight(rowAfterBody + ROW_STEP);
    }

    public static IScroll scrollSearchable(ICustomGui gui, int id, int x, int y, int w, int h, String[] items) {
        IScroll scroll = scroll(gui, id, x, y, w, h, items);
        try {
            scroll.setHasSearch(true);
        } catch (Throwable ignored) {
        }
        return scroll;
    }

    /**
     * Player pick list with hint label and height clamped above footer rows.
     * {@code rowsBelowList} = full button rows under the list (nav, actions, paging).
     */
    public static IScroll scrollPickList(
            ICustomGui gui, int listY, int rowsBelowList, String[] items) {
        String[] safe = items == null ? new String[0] : items;
        int bandY = listY;
        int scrollH = listScrollHeight(gui, bandY, rowsBelowList);
        int visibleRows = Math.max(1, scrollH / SCROLL_ROW_H);
        if (safe.length > visibleRows) {
            gui.addLabel(ID_STATUS_TAG, CnpcGuiStyle.HINT_PICK_LIST, M, listY, textBandWidth(), 10);
            bandY = listY + 14;
            scrollH = listScrollHeight(gui, bandY, rowsBelowList);
        }
        return scrollSearchable(gui, ID_LIST_SCROLL, M, bandY, textBandWidth(), scrollH, safe);
    }

    /** Y of the scroll list band (below optional pick-list hint). */
    public static int pickListBandY(int listY, int rowsBelowList, ICustomGui gui, int itemCount) {
        int bandY = listY;
        int scrollH = listScrollHeight(gui, bandY, rowsBelowList);
        int visibleRows = Math.max(1, scrollH / SCROLL_ROW_H);
        if (itemCount > visibleRows) {
            bandY = listY + 14;
        }
        return bandY;
    }

    public static int pickListScrollBottom(int listY, int rowsBelowList, ICustomGui gui, int itemCount) {
        int bandY = pickListBandY(listY, rowsBelowList, gui, itemCount);
        return bandY + listScrollHeight(gui, bandY, rowsBelowList);
    }

    /** Y coordinate just below a {@link #scrollPickList} widget. */
    public static int belowPickList(int listY, int rowsBelowList, ICustomGui gui, int itemCount) {
        return pickListScrollBottom(listY, rowsBelowList, gui, itemCount) + 8;
    }

    public static IButton button(ICustomGui gui, int id, String label, int x, int y, Runnable onPress) {
        IButton b = gui.addButton(id, safeChat(compactButton(label)), x, y, BTN_W, BTN_H);
        b.setOnPress((g, btn) -> {
            g.close();
            onPress.run();
        });
        return b;
    }

    public static IButton buttonSmall(ICustomGui gui, int id, String label, int x, int y, int w, Runnable onPress) {
        IButton b = gui.addButton(id, safeChat(compactButton(label)), x, y, w, BTN_H);
        b.setOnPress((g, btn) -> {
            g.close();
            onPress.run();
        });
        return b;
    }

    /** Small button without truncating the label (tier costs, etc.). */
    public static IButton buttonSmallFull(ICustomGui gui, int id, String label, int x, int y, int w,
            Runnable onPress) {
        IButton b = gui.addButton(id, safeChat(label), x, y, w, BTN_H);
        b.setOnPress((g, btn) -> {
            g.close();
            onPress.run();
        });
        return b;
    }

    /** Primary button caption without long inline hints (hints belong in the info block). */
    public static String compactButton(String label) {
        if (label == null) {
            return "";
        }
        String s = label;
        int hint = s.indexOf(" §8· ");
        if (hint > 0) {
            s = s.substring(0, hint);
        }
        hint = s.indexOf(" §7· ");
        if (hint > 0) {
            s = s.substring(0, hint);
        }
        if (s.length() > 26) {
            s = s.substring(0, 23) + "…";
        }
        return s;
    }

    /** Short shop row: skill name + progress + cost (fits CNPC button width). */
    public static String compactShopLabel(String name, int bought, int max, String cost) {
        String n = name == null ? "" : name.replaceAll("§.", "");
        if (n.length() > 14) {
            n = n.substring(0, 12) + "…";
        }
        String c = cost == null || cost.isBlank() ? "?" : cost.replaceAll("§.", "");
        return "§f" + n + " §7" + bought + "/" + max + " · §6" + c;
    }

    public static String humanizePickerArg(String arg) {
        if (arg == null || arg.isBlank()) {
            return "§7(entry)";
        }
        String s = arg.trim();
        if (s.startsWith("uuid:")) {
            return humanizeUuid(s.substring(5).trim());
        }
        if (UUID_LINE.matcher(s).matches()) {
            return humanizeUuid(s);
        }
        if (s.contains("\t")) {
            String[] p = s.split("\t", -1);
            if (p.length > 1 && p[1] != null && !p[1].isBlank()) {
                return safeChat(p[1]);
            }
            s = p[0];
        }
        if (looksLikeInternalId(s)) {
            return humanizeToken(s);
        }
        return safeChat(s);
    }

    public static String humanizeSkillLabel(String skillId, String label) {
        if (label != null && !label.isBlank() && !looksLikeInternalId(label)) {
            return safeChat(label);
        }
        return humanizeToken(skillId == null ? label : skillId);
    }

    public static String humanizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return "§7?";
        }
        String s = raw.trim();
        if (s.startsWith("uuid:")) {
            return humanizeUuid(s.substring(5).trim());
        }
        int slash = s.lastIndexOf('/');
        if (slash >= 0 && slash < s.length() - 1) {
            s = s.substring(slash + 1);
        }
        int dot = s.lastIndexOf('.');
        if (dot >= 0 && dot < s.length() - 1 && s.contains(".")) {
            s = s.substring(dot + 1);
        }
        s = s.replace('_', ' ').replace('-', ' ');
        if (s.length() > 1 && s.chars().noneMatch(Character::isWhitespace)) {
            s = splitCamel(s);
        }
        if (s.isBlank()) {
            return "§7?";
        }
        return "§f" + capitalizeWords(s.toLowerCase(Locale.ROOT));
    }

    private static String humanizeUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return "§7(player)";
        }
        String u = uuid.trim();
        if (u.length() > 8) {
            return "§7Player " + u.substring(0, 8) + "…";
        }
        return "§7Player " + u;
    }

    private static String splitCamel(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(s.charAt(i - 1))) {
                out.append(' ');
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String capitalizeWords(String s) {
        String[] parts = s.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                out.append(p.substring(1));
            }
        }
        return out.toString();
    }

    private static boolean looksLikeInternalId(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        if (s.contains(":") && !s.startsWith("uuid:")) {
            return true;
        }
        if (PACKAGE_LIKE.matcher(s).find()) {
            return true;
        }
        if (s.length() > 24 && !s.contains(" ")) {
            return true;
        }
        return false;
    }

    private static List<String> normalizeInfoLines(List<String> lines) {
        List<String> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            out.add(safeChat(CnpcGuiStyle.normalizeLine(line)));
        }
        return out;
    }

    /** Strip characters that break CNPC label/scroll parsing; keep color codes. */
    public static String safeChat(String text) {
        if (text == null) {
            return "";
        }
        String s = text.replace('\n', ' ').replace('\r', ' ');
        s = s.replace("{", "").replace("}", "");
        return s;
    }

    private static String safeScrollLine(String line) {
        String s = safeChat(line);
        s = s.replace(" §8| ", " · ").replace(" | ", " · ");
        if (s.length() > 64) {
            return s.substring(0, 61) + "…";
        }
        return s;
    }

    /** Show text in the next CNPC menu refresh (preferred for LM GUI actions). */
    public static void feedback(ServerPlayer player, String msg) {
        pushMenuMessage(player, msg);
    }

    /** Chat fallback when no menu can be opened (CNPC missing, etc.). */
    public static void feedbackChat(ServerPlayer player, String msg) {
        if (player == null || msg == null || msg.isBlank()) {
            return;
        }
        for (String line : msg.split("\n")) {
            if (line != null && !line.isBlank()) {
                player.m_213846_(Component.m_237113_(line));
            }
        }
    }

    public static void act(ServerPlayer player, Supplier<String> action, Runnable reopen) {
        String msg = action.get();
        if (msg != null && !msg.isBlank()) {
            pushMenuMessage(player, msg);
        }
        reopen.run();
    }

    /** Tab-separated GUI cards: field 0 = id/arg, field 1 = display label. */
    public static String[] cardLabels(List<String> cards, int labelField) {
        if (cards == null || cards.isEmpty()) {
            return new String[0];
        }
        List<String> labels = new ArrayList<>(cards.size());
        for (String card : cards) {
            String[] p = card.split("\t", -1);
            String label = null;
            if (labelField >= 0 && labelField < p.length && p[labelField] != null && !p[labelField].isBlank()) {
                label = p[labelField];
            } else if (p.length > 0) {
                label = p[0];
            }
            if (labelField == 0 || looksLikeInternalId(label)) {
                labels.add(humanizePickerArg(label));
            } else {
                labels.add(safeChat(label));
            }
        }
        return labels.toArray(String[]::new);
    }

    public static String cardField(List<String> cards, IScroll scroll, int field) {
        if (cards == null || cards.isEmpty() || scroll == null) {
            return null;
        }
        int[] sel = scroll.getSelection();
        if (sel == null || sel.length == 0 || sel[0] < 0 || sel[0] >= cards.size()) {
            return null;
        }
        String[] p = cards.get(sel[0]).split("\t", -1);
        if (field < 0 || field >= p.length) {
            return null;
        }
        String v = p[field];
        return v == null || v.isBlank() ? null : v.trim();
    }

    public static void wireScrollDoublePick(
            IScroll scroll,
            List<String> cards,
            int argField,
            Consumer<String> onPick
    ) {
        if (scroll == null || onPick == null) {
            return;
        }
        scroll.setOnDoubleClick((g, sc) -> {
            g.close();
            String arg = cardField(cards, sc, argField);
            if (arg != null) {
                onPick.accept(arg);
            }
        });
    }

    /** Single-click opens a detail / confirm screen (does not close GUI). */
    public static void wireScrollOpenDetail(
            IScroll scroll,
            List<String> cards,
            int argField,
            Consumer<String> onOpen
    ) {
        if (scroll == null || onOpen == null) {
            return;
        }
        scroll.setOnClick((g, sc) -> {
            String arg = cardField(cards, sc, argField);
            if (arg != null) {
                onOpen.accept(arg);
            }
        });
    }

    public static String rivalPickerArgFromCard(String card) {
        if (card == null || card.isBlank()) {
            return null;
        }
        String[] p = card.split("\t", -1);
        if (p.length < 1) {
            return null;
        }
        String uuid = p[0] == null ? "" : p[0].trim();
        String name = p.length > 1 && p[1] != null ? p[1].trim() : "";
        if (!uuid.isBlank()) {
            return "uuid:" + uuid;
        }
        return name.isBlank() ? null : name;
    }

    public static String findCardByPickerArg(List<String> cards, String pickerArg) {
        if (cards == null || pickerArg == null || pickerArg.isBlank()) {
            return null;
        }
        String want = pickerArg.trim();
        for (String card : cards) {
            if (want.equalsIgnoreCase(rivalPickerArgFromCard(card))) {
                return card;
            }
            String[] p = card == null ? new String[0] : card.split("\t", -1);
            if (p.length > 0) {
                String uuid = p[0] == null ? "" : p[0].trim();
                if (!uuid.isBlank()) {
                    if (want.equalsIgnoreCase(uuid) || want.equalsIgnoreCase("uuid:" + uuid)) {
                        return card;
                    }
                }
            }
            if (p.length > 1 && want.equalsIgnoreCase(p[1].trim())) {
                return card;
            }
        }
        return null;
    }

    /** System top-level screen (e.g. Prestige main): Main → Legacy Mechanics hub. */
    public static void navSystemRoot(ServerPlayer player, ICustomGui gui, int row) {
        buttonSmall(gui, ID_NAV_HUB, "§7Hub", COL_L, row, 95, () -> CnpcLmHubGui.open(player, "main"));
    }

    /** Submenu: Back → parent page in this system; Main → Legacy Mechanics hub. */
    public static void navSubmenu(ServerPlayer player, ICustomGui gui, int row, Runnable back, String backLabel) {
        if (back != null) {
            buttonSmall(gui, ID_NAV_BACK, backLabel == null ? "§7« Back" : backLabel, COL_L, row, 95, back);
        }
        buttonSmall(gui, ID_NAV_HUB, "§7Hub", COL_R, row, 95, () -> CnpcLmHubGui.open(player, "main"));
    }

    /** @deprecated use {@link #navSystemRoot} or {@link #navSubmenu} */
    @Deprecated
    public static void navHubMain(ServerPlayer player, ICustomGui gui, int row, Runnable openMain) {
        if (openMain == null) {
            navSystemRoot(player, gui, row);
        } else {
            navSubmenu(player, gui, row, openMain, "§7« Back");
        }
    }

    public static boolean staff(ServerPlayer player) {
        return StaffAccess.isStaff(player);
    }

    /** @deprecated use {@link #navSubmenu} */
    @Deprecated
    public static void navBackHub(ServerPlayer player, ICustomGui gui, int row, Runnable back, String backLabel) {
        navSubmenu(player, gui, row, back, backLabel);
    }

    public static String flagOnOff(java.util.Map<String, String> ph, String key) {
        boolean on = "true".equalsIgnoreCase(ph.getOrDefault("flag_" + key, "false"));
        return on ? "§aOn" : "§cOff";
    }

    @FunctionalInterface
    public interface Painter {
        void paint(ServerPlayer player, ICustomGui gui);
    }
}
