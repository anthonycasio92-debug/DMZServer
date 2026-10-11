package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.net.gui.GuiOpenPacket;
import com.dbzlegacy.adaptivedifficulty.net.gui.LmGuiNetwork;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/** Builds a GuiOpenPacket payload for KaiPath. */
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
                actionId == null ? "" : actionId,
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
        JsonArray tabArr = new JsonArray();
        for (Tab tab : tabs) {
            JsonObject o = new JsonObject();
            o.addProperty("id", tab.id);
            boolean active = tab.id.equalsIgnoreCase(activeTab);
            o.addProperty("label", active ? "§6§l" + UltraStyle.plain(tab.label) : "§7" + UltraStyle.plain(tab.label));
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
            btnArr.add(o);
        }
        LmGuiNetwork.sendOpen(
                player,
                new GuiOpenPacket(
                        screenId,
                        title,
                        pageId,
                        GSON.toJson(tabArr),
                        activeTab,
                        GSON.toJson(lineArr),
                        GSON.toJson(btnArr)));
    }

    private record Tab(String id, String label) {}

    private record Button(String actionId, String label, String style, int x, int y, int w, int h) {}
}
