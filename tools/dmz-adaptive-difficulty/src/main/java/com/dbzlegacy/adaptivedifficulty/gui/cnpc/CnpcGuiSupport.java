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
import noppes.npcs.api.gui.ILabel;
import noppes.npcs.api.gui.IScroll;

/** Shared CustomNPCs layout + show helpers for Legacy Mechanics UI. */
public final class CnpcGuiSupport {
    public static final int W = 430;
    public static final int H = 280;
    public static final int M = 14;
    public static final int BTN_W = 195;
    public static final int BTN_H = 20;
    public static final int COL_L = M;
    public static final int COL_R = 220;
    public static final int ROW_STEP = 24;
    public static final int LINE_H = 13;

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
            player.m_213846_(Component.m_237113_("§cCustomNPCs is required for the LM menu (install on client + server)."));
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
            player.m_213846_(Component.m_237113_("§cCould not open LM menu (CNPC player wrap failed)."));
            return;
        }
        try {
            ICustomGui gui = NpcAPI.Instance().createCustomGui(guiId, width, height, false, ip);
            gui.setClosesOnEsc(true);
            painter.paint(player, gui);
            ip.showCustomGui(gui);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] CNPC GUI {} failed: {}", AdaptiveDifficultyMod.MOD_ID, guiId, t);
            player.m_213846_(Component.m_237113_("§cMenu error: " + safeChat(t.getMessage())));
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
        return dividerY + 10;
    }

    /**
     * Chest-style status block: a few inline lines, or a scroll when there is more text.
     * Returns the Y coordinate where action buttons should start (with padding).
     */
    /** Small section caption above a button group. Returns Y for the first button row. */
    public static int paintSectionTag(ICustomGui gui, int labelId, int y, String caption) {
        gui.addLabel(labelId, safeChat(caption), M, y, W - M * 2, 10);
        return y + 16;
    }

    public static int paintInfoBlock(ICustomGui gui, int startY, List<String> lines, int inlineMax) {
        List<String> clean = normalizeInfoLines(lines);
        if (clean.isEmpty()) {
            return startY + 4;
        }
        int maxInline = Math.max(1, inlineMax);
        if (clean.size() <= maxInline) {
            bodyLines(gui, ID_INFO_LABEL_BASE, startY, clean, maxInline);
            return startY + clean.size() * LINE_H + 10;
        }
        gui.addLabel(ID_STATUS_TAG, "§8Status", M, startY - 2, W - M * 2, 10);
        int scrollH = Math.min(112, Math.max(56, clean.size() * 14));
        scroll(gui, ID_INFO_SCROLL, M, startY + 8, W - M * 2, scrollH,
                clean.stream().map(CnpcGuiSupport::safeScrollLine).toArray(String[]::new));
        return startY + 8 + scrollH + 10;
    }

    public static int suggestHeight(int actionBottomY) {
        return Math.max(H, Math.min(420, actionBottomY + 36));
    }

    public static void footerCloseRefresh(ServerPlayer player, ICustomGui gui, int row, Runnable refresh) {
        buttonSmall(gui, ID_CLOSE, "§cClose", COL_L, row, 95, () -> {});
        buttonSmall(gui, ID_REFRESH, "§7Refresh", COL_R, row, 95, refresh);
    }

    public static ILabel title(ICustomGui gui, int id, String text) {
        ILabel l = gui.addLabel(id, safeChat(text), M, 8, W - M * 2, 18);
        try {
            l.setScale(1.15f);
        } catch (Throwable ignored) {
        }
        return l;
    }

    public static void subtitle(ICustomGui gui, int id, String text) {
        gui.addLabel(id, safeChat(text), M, 28, W - M * 2, 14);
    }

    /** Data/actions target (inspect subject when staff is inspecting). */
    public static net.minecraft.server.level.ServerPlayer target(net.minecraft.server.level.ServerPlayer viewer) {
        return AdminInspectSessions.resolveSubject(viewer);
    }

    public static void inspectBanner(net.minecraft.server.level.ServerPlayer viewer, ICustomGui gui) {
        String line = AdminInspectSessions.inspectBannerLine(viewer);
        if (line != null) {
            gui.addLabel(ID_INSPECT, safeChat(line), M, 40, W - M * 2, 12);
        }
    }

    public static void divider(ICustomGui gui, int id, int y) {
        gui.addLabel(id, "§8────────────────────────────────────────", M, y, W - M * 2, 10);
    }

    public static void bodyLines(ICustomGui gui, int startId, int y, List<String> lines, int maxLines) {
        int row = y;
        int n = 0;
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (n >= maxLines) {
                break;
            }
            gui.addLabel(startId + n, safeChat(line), M, row, W - M * 2, 12);
            row += LINE_H;
            n++;
        }
    }

    public static IScroll scroll(ICustomGui gui, int id, int x, int y, int w, int h, String[] items) {
        String[] safe = items == null ? new String[0] : items;
        String[] copy = new String[safe.length];
        for (int i = 0; i < safe.length; i++) {
            copy[i] = safeScrollLine(safe[i]);
        }
        return gui.addScroll(id, x, y, w, h, copy);
    }

    public static IScroll scrollSearchable(ICustomGui gui, int id, int x, int y, int w, int h, String[] items) {
        IScroll scroll = scroll(gui, id, x, y, w, h, items);
        try {
            scroll.setHasSearch(true);
        } catch (Throwable ignored) {
        }
        return scroll;
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
        if (s.length() > 28) {
            s = s.substring(0, 25) + "…";
        }
        return s;
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
            out.add(safeChat(line));
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
        if (s.length() > 64) {
            return s.substring(0, 61) + "…";
        }
        return s;
    }

    public static void feedback(ServerPlayer player, String msg) {
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
        feedback(player, action.get());
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

    /** System top-level screen (e.g. Prestige main): Main → Legacy Mechanics hub. */
    public static void navSystemRoot(ServerPlayer player, ICustomGui gui, int row) {
        buttonSmall(gui, ID_NAV_HUB, "§7Main", COL_L, row, 95, () -> CnpcLmHubGui.open(player, "main"));
    }

    /** Submenu: Back → parent page in this system; Main → Legacy Mechanics hub. */
    public static void navSubmenu(ServerPlayer player, ICustomGui gui, int row, Runnable back, String backLabel) {
        if (back != null) {
            buttonSmall(gui, ID_NAV_BACK, backLabel == null ? "§7« Back" : backLabel, COL_L, row, 95, back);
        }
        buttonSmall(gui, ID_NAV_HUB, "§7Main", COL_R, row, 95, () -> CnpcLmHubGui.open(player, "main"));
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
        return on ? "§aON" : "§cOFF";
    }

    @FunctionalInterface
    public interface Painter {
        void paint(ServerPlayer player, ICustomGui gui);
    }
}
