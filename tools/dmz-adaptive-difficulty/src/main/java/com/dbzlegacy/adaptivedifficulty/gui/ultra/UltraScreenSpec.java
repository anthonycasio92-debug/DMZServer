package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.net.gui.GuiOpenPacket;
import com.dbzlegacy.adaptivedifficulty.net.gui.LmGuiNetwork;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * Builds a GuiOpenPacket payload for KaiPath.
 *
 * <p>Panel defaults are the compact Ultra dialog ({@link UltraStyle#PANEL_W}×{@link
 * UltraStyle#PANEL_H}), centered with a screen margin. Button action ids are qualified as
 * {@code @&lt;screenId&gt;:&lt;action&gt;} so KaiPath can omit or mishandle {@code screenId} and
 * the server still routes correctly.
 *
 * <p>Layout JSON includes shared body/button metrics ({@code lineHeight}, {@code lineGap},
 * {@code bodyMaxHeight}, {@code scrollBody}, {@code buttonPad}, …) so every screen benefits.
 * Optional {@link #playerModel} declares a client-rendered entity preview (JLDK implements
 * rendering; unknown clients ignore the field).
 */
public final class UltraScreenSpec {
    private static final Gson GSON = new Gson();

    /** When set, {@link #send} captures packets instead of transmitting (startup self-test). */
    private static final ThreadLocal<List<GuiOpenPacket>> PROBE = new ThreadLocal<>();

    private final String screenId;
    private String title = "";
    private String pageId = "main";
    private String activeTab = "home";
    private final List<Tab> tabs = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();
    /** Compact title + tab row; content starts immediately below. */
    private int nextRow = UltraStyle.CONTENT_ROW;
    private int panelWidth = UltraStyle.PANEL_W;
    private int panelHeight = UltraStyle.PANEL_H;
    private boolean centerPanel = true;
    private int panelMargin = UltraStyle.PANEL_MARGIN;
    private PlayerModel playerModel;
    /** Lowest button top Y seen so far; used to size the body scroll region. */
    private int firstButtonY = -1;

    private UltraScreenSpec(String screenId) {
        this.screenId = screenId == null || screenId.isBlank() ? "hub" : screenId.trim();
    }

    public static UltraScreenSpec of(String screenId) {
        return new UltraScreenSpec(screenId);
    }

    /**
     * Run {@code work} while capturing every {@link #send} into a list (no network).
     * Used by {@link UltraScreenSelfTest}.
     */
    public static List<GuiOpenPacket> captureSends(Runnable work) {
        List<GuiOpenPacket> captured = new ArrayList<>();
        PROBE.set(captured);
        try {
            if (work != null) {
                work.run();
            }
        } finally {
            PROBE.remove();
        }
        return captured;
    }

    public UltraScreenSpec title(String title) {
        this.title = UltraStyle.header(title);
        return this;
    }

    public UltraScreenSpec page(String pageId) {
        this.pageId = pageId == null ? "main" : pageId;
        return this;
    }

    public UltraScreenSpec activeTab(String tabId) {
        this.activeTab = tabId == null ? "home" : tabId;
        return this;
    }

    /** Explicit panel size in GUI pixels (DMZUltra-style centered dialog). */
    public UltraScreenSpec panel(int width, int height) {
        this.panelWidth = Math.max(UltraStyle.PANEL_MIN_W, width);
        this.panelHeight = Math.max(UltraStyle.PANEL_MIN_H, height);
        return this;
    }

    public UltraScreenSpec center(boolean center) {
        this.centerPanel = center;
        return this;
    }

    public UltraScreenSpec margin(int margin) {
        this.panelMargin = Math.max(0, margin);
        return this;
    }

    /**
     * Optional client-rendered player model preview. Unknown clients ignore this field;
     * layout and buttons still work. JLDK draws the entity; LM only declares bounds.
     */
    public UltraScreenSpec playerModel(
            int x, int y, int width, int height, float scale, boolean rotate) {
        this.playerModel = new PlayerModel(
                x,
                y,
                Math.max(16, width),
                Math.max(16, height),
                scale <= 0f ? 1f : scale,
                rotate);
        return this;
    }

    /** Clear any previously set player model (default: off). */
    public UltraScreenSpec clearPlayerModel() {
        this.playerModel = null;
        return this;
    }

    public UltraScreenSpec tabs(String... idLabelPairs) {
        tabs.clear();
        if (idLabelPairs == null) {
            return this;
        }
        for (String pair : idLabelPairs) {
            if (pair == null || pair.isBlank()) {
                continue;
            }
            int bar = pair.indexOf('|');
            if (bar <= 0) {
                tabs.add(new Tab(pair, pair));
            } else {
                tabs.add(new Tab(pair.substring(0, bar), pair.substring(bar + 1)));
            }
        }
        // Content starts below title + tab strip when tabs are present.
        if (!tabs.isEmpty() && nextRow < UltraStyle.CONTENT_ROW_TABS) {
            nextRow = UltraStyle.CONTENT_ROW_TABS;
        }
        return this;
    }

    public UltraScreenSpec line(String text) {
        if (text != null && !text.isBlank()) {
            lines.add(text);
        }
        return this;
    }

    public UltraScreenSpec labelValue(String label, String value) {
        return line(UltraStyle.labelValue(label, value));
    }

    public UltraScreenSpec notice(String body) {
        return line(UltraStyle.notice(body));
    }

    public UltraScreenSpec button(String actionId, String label, String style) {
        return button(actionId, label, style, UltraStyle.COL_L, nextRow, UltraStyle.BTN_W);
    }

    public UltraScreenSpec buttonLeft(String actionId, String label, String style) {
        return button(actionId, label, style, UltraStyle.COL_L, nextRow, UltraStyle.BTN_W);
    }

    public UltraScreenSpec buttonRight(String actionId, String label, String style) {
        return button(actionId, label, style, UltraStyle.COL_R, nextRow, UltraStyle.BTN_W);
    }

    public UltraScreenSpec button(
            String actionId, String label, String style, int x, int y, int w) {
        UltraScreenIds.requireValidActionId(screenId, actionId, label);
        int width = Math.max(UltraStyle.BTN_MIN_W, w);
        String resolvedStyle = normalizeStyle(style);
        if (firstButtonY < 0 || y < firstButtonY) {
            firstButtonY = y;
        }
        buttons.add(new Button(
                qualifyAction(actionId),
                UltraStyle.buttonLabel(label),
                resolvedStyle,
                x,
                y,
                width,
                UltraStyle.BTN_H,
                UltraStyle.BTN_PAD));
        return this;
    }

    public UltraScreenSpec rowGap() {
        nextRow += UltraStyle.BTN_H + UltraStyle.BTN_GAP;
        return this;
    }

    public UltraScreenSpec setRow(int y) {
        nextRow = y;
        return this;
    }

    public int row() {
        return nextRow;
    }

    /** Home tab footer: back to main menu (no Refresh — pages reopen via actions/tabs). */
    public UltraScreenSpec footerBackToMain() {
        return button("nav:hub", UltraStyle.BACK_TO_MAIN, "dim", UltraStyle.COL_L, nextRow, UltraStyle.BTN_W);
    }

    /** Sub-page footer: back to system home (no Refresh). */
    public UltraScreenSpec footerBackHome() {
        return button("nav:home", UltraStyle.BACK, "dim", UltraStyle.COL_L, nextRow, UltraStyle.BTN_W);
    }

    /** Build the wire packet without sending. */
    public GuiOpenPacket toPacket() {
        int height = panelHeight;
        int contentBottom = nextRow + UltraStyle.BTN_H + UltraStyle.PANEL_MARGIN;
        if (contentBottom > height) {
            height = Math.min(UltraStyle.PANEL_MAX_H, contentBottom);
        }

        int bodyTop = tabs.isEmpty() ? UltraStyle.BODY_TOP : UltraStyle.BODY_TOP_TABS;
        int lineStep = UltraStyle.LINE_H + UltraStyle.LINE_GAP;
        int naturalBodyH =
                lines.isEmpty()
                        ? 0
                        : UltraStyle.BODY_PAD * 2 + lines.size() * lineStep;
        // Body region sits under title/tabs and above the first button row (or panel bottom).
        int bodyBottom = firstButtonY < 0
                ? height - UltraStyle.PANEL_MARGIN
                : Math.min(firstButtonY - UltraStyle.BTN_GAP, height - UltraStyle.PANEL_MARGIN);
        int bodyMaxH = Math.max(
                UltraStyle.BODY_MIN_H, Math.max(lineStep + UltraStyle.BODY_PAD * 2, bodyBottom - bodyTop));
        boolean scrollBody = naturalBodyH > bodyMaxH || contentBottom > UltraStyle.PANEL_MAX_H;

        JsonArray tabArr = new JsonArray();
        for (Tab tab : tabs) {
            if (tab == null || tab.id == null) {
                continue;
            }
            UltraScreenIds.requireValidActionId(screenId, "tab:" + tab.id, tab.label);
            JsonObject o = new JsonObject();
            o.addProperty("id", qualifyAction("tab:" + tab.id));
            boolean active = tab.id.equalsIgnoreCase(activeTab);
            o.addProperty(
                    "label",
                    active
                            ? UltraStyle.HEADER + UltraStyle.plain(tab.label)
                            : UltraStyle.LABEL + UltraStyle.plain(tab.label));
            tabArr.add(o);
        }
        JsonArray lineArr = new JsonArray();
        for (String line : lines) {
            if (line != null) {
                lineArr.add(line);
            }
        }
        JsonArray btnArr = new JsonArray();
        for (Button b : buttons) {
            if (b == null) {
                continue;
            }
            JsonObject o = new JsonObject();
            o.addProperty("id", b.actionId);
            o.addProperty("label", b.label);
            o.addProperty("style", b.style);
            o.addProperty("x", b.x);
            o.addProperty("y", b.y);
            o.addProperty("w", b.w);
            o.addProperty("h", b.h);
            o.addProperty("pad", b.pad);
            JsonObject args = new JsonObject();
            // Logical system — echoed by KaiPath into GuiActionPacket even when wire screenId is hub.
            args.addProperty("screen", screenId);
            args.addProperty(UltraScreenIds.META_SYSTEM, screenId);
            args.addProperty("page", pageId);
            o.add("args", args);
            btnArr.add(o);
        }
        JsonObject layout = new JsonObject();
        layout.addProperty("panelWidth", panelWidth);
        layout.addProperty("panelHeight", height);
        layout.addProperty("width", panelWidth);
        layout.addProperty("height", height);
        layout.addProperty("center", centerPanel);
        layout.addProperty("margin", panelMargin);
        layout.addProperty("titleAlign", UltraStyle.TITLE_ALIGN);
        // Shared body / button metrics — every screen benefits.
        layout.addProperty("lineHeight", UltraStyle.LINE_H);
        layout.addProperty("lineGap", UltraStyle.LINE_GAP);
        layout.addProperty("bodyTop", bodyTop);
        layout.addProperty("bodyPad", UltraStyle.BODY_PAD);
        layout.addProperty("bodyMaxHeight", bodyMaxH);
        layout.addProperty("scrollBody", scrollBody);
        layout.addProperty("buttonPad", UltraStyle.BTN_PAD);
        layout.addProperty("buttonGap", UltraStyle.BTN_GAP);
        layout.addProperty("buttonMinWidth", UltraStyle.BTN_MIN_W);
        layout.addProperty("buttonHeight", UltraStyle.BTN_H);
        // Logical system metadata (survives hub wire shim in layoutJson).
        layout.addProperty(UltraScreenIds.META_SYSTEM, screenId);
        layout.addProperty("screen", screenId);
        if (playerModel != null) {
            JsonObject pm = new JsonObject();
            pm.addProperty("x", playerModel.x);
            pm.addProperty("y", playerModel.y);
            pm.addProperty("width", playerModel.width);
            pm.addProperty("height", playerModel.height);
            pm.addProperty("scale", playerModel.scale);
            pm.addProperty("rotate", playerModel.rotate);
            layout.add("playerModel", pm);
        }
        return new GuiOpenPacket(
                screenId,
                title,
                pageId,
                GSON.toJson(tabArr),
                activeTab,
                GSON.toJson(lineArr),
                GSON.toJson(btnArr),
                panelWidth,
                height,
                GSON.toJson(layout));
    }

    public void send(ServerPlayer player) {
        GuiOpenPacket packet = toPacket();
        List<GuiOpenPacket> probe = PROBE.get();
        if (probe != null) {
            probe.add(packet);
            return;
        }
        if (player == null) {
            return;
        }
        LmGuiNetwork.sendOpen(player, packet);
    }

    /** Prefix {@code @screenId:} unless the action is already qualified. */
    String qualifyAction(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            return "@" + screenId + ":nav:home";
        }
        String raw = actionId.trim();
        if (raw.startsWith("@")) {
            return raw;
        }
        return "@" + screenId + ":" + raw;
    }

    private static String normalizeStyle(String style) {
        if (style == null || style.isBlank()) {
            return "normal";
        }
        String s = style.trim().toLowerCase();
        return switch (s) {
            case "confirm", "danger", "dim", "normal" -> s;
            default -> "normal";
        };
    }

    private record Tab(String id, String label) {}

    private record Button(
            String actionId, String label, String style, int x, int y, int w, int h, int pad) {}

    private record PlayerModel(int x, int y, int width, int height, float scale, boolean rotate) {}
}
