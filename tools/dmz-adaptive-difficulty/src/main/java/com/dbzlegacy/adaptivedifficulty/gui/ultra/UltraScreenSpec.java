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
 * <p>Panel defaults match CNPC / DMZUltra dialog size ({@link UltraStyle#PANEL_W}×{@link
 * UltraStyle#PANEL_H}), centered with a screen margin. Button action ids are qualified as
 * {@code @&lt;screenId&gt;:&lt;action&gt;} so KaiPath can omit or mishandle {@code screenId} and
 * the server still routes correctly.
 */
public final class UltraScreenSpec {
    private static final Gson GSON = new Gson();

    private final String screenId;
    private String title = "";
    private String pageId = "main";
    private String activeTab = "home";
    private final List<Tab> tabs = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();
    private int nextRow = 36;
    private int panelWidth = UltraStyle.PANEL_W;
    private int panelHeight = UltraStyle.PANEL_H;
    private boolean centerPanel = true;
    private int panelMargin = UltraStyle.PANEL_MARGIN;

    private UltraScreenSpec(String screenId) {
        this.screenId = screenId == null || screenId.isBlank() ? "hub" : screenId.trim();
    }

    public static UltraScreenSpec of(String screenId) {
        return new UltraScreenSpec(screenId);
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

    public UltraScreenSpec tabs(String... idLabelPairs) {
        tabs.clear();
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
        int width = Math.max(UltraStyle.BTN_MIN_W, w);
        buttons.add(new Button(
                qualifyAction(actionId),
                UltraStyle.buttonLabel(label),
                style == null ? "normal" : style,
                x,
                y,
                width,
                UltraStyle.BTN_H));
        return this;
    }

    public UltraScreenSpec rowGap() {
        nextRow += 24;
        return this;
    }

    public UltraScreenSpec setRow(int y) {
        nextRow = y;
        return this;
    }

    public int row() {
        return nextRow;
    }

    /** Home tab footer: back to main menu. */
    public UltraScreenSpec footerBackToMain() {
        return button("nav:hub", UltraStyle.BACK_TO_MAIN, "dim", UltraStyle.COL_L, nextRow, 120)
                .button("nav:refresh", UltraStyle.LABEL + "Refresh", "dim", UltraStyle.COL_R, nextRow, 95);
    }

    /** Sub-page footer: back to system home. */
    public UltraScreenSpec footerBackHome() {
        return button("nav:home", UltraStyle.BACK, "dim", UltraStyle.COL_L, nextRow, 95)
                .button("nav:refresh", UltraStyle.LABEL + "Refresh", "dim", UltraStyle.COL_R, nextRow, 95);
    }

    public void send(ServerPlayer player) {
        if (player == null) {
            return;
        }
        // Grow panel if buttons sit below the default dialog height.
        int contentBottom = nextRow + UltraStyle.BTN_H + UltraStyle.PANEL_MARGIN;
        if (contentBottom > panelHeight) {
            panelHeight = Math.min(UltraStyle.PANEL_MAX_H, contentBottom);
        }

        JsonArray tabArr = new JsonArray();
        for (Tab tab : tabs) {
            JsonObject o = new JsonObject();
            // Tab bar clicks: qualify so dispatch can recover screen if KaiPath omits it.
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
            lineArr.add(line);
        }
        JsonArray btnArr = new JsonArray();
        for (Button b : buttons) {
            JsonObject o = new JsonObject();
            o.addProperty("id", b.actionId);
            o.addProperty("label", b.label);
            o.addProperty("style", b.style);
            o.addProperty("x", b.x);
            o.addProperty("y", b.y);
            o.addProperty("w", b.w);
            o.addProperty("h", b.h);
            // Echoed by KaiPath into GuiActionPacket.argsJson when present.
            JsonObject args = new JsonObject();
            args.addProperty("screen", screenId);
            args.addProperty("page", pageId);
            o.add("args", args);
            btnArr.add(o);
        }
        JsonObject layout = new JsonObject();
        layout.addProperty("panelWidth", panelWidth);
        layout.addProperty("panelHeight", panelHeight);
        layout.addProperty("width", panelWidth);
        layout.addProperty("height", panelHeight);
        layout.addProperty("center", centerPanel);
        layout.addProperty("margin", panelMargin);
        LmGuiNetwork.sendOpen(
                player,
                new GuiOpenPacket(
                        screenId,
                        title,
                        pageId,
                        GSON.toJson(tabArr),
                        activeTab,
                        GSON.toJson(lineArr),
                        GSON.toJson(btnArr),
                        panelWidth,
                        panelHeight,
                        GSON.toJson(layout)));
    }

    /** Prefix {@code @screenId:} unless the action is already qualified. */
    String qualifyAction(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            return "@" + screenId + ":nav:refresh";
        }
        String raw = actionId.trim();
        if (raw.startsWith("@")) {
            return raw;
        }
        return "@" + screenId + ":" + raw;
    }

    private record Tab(String id, String label) {}

    private record Button(String actionId, String label, String style, int x, int y, int w, int h) {}
}
