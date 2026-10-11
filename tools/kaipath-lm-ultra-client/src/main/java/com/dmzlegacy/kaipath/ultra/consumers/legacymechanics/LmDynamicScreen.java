package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.UltraScreen;
import com.dmzlegacy.kaipath.ultra.UltraUiAccess;
import com.dmzlegacy.kaipath.ultra.UltraUiBridge;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Renders whatever LM sends in {@link GuiOpenPacket} — title, tabs, lines, buttons.
 * No hardcoded screen/tab/button lists.
 */
public class LmDynamicScreen extends UltraScreen {
    private final GuiOpenPacket packet;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private boolean center = true;
    private int margin = GuiOpenPacket.DEFAULT_MARGIN;
    private final List<String> bodyLines = new ArrayList<>();

    public LmDynamicScreen(GuiOpenPacket packet) {
        super(Component.m_237113_(plainTitle(packet)));
        this.packet = packet == null ? GuiOpenPacket.emptyHub() : packet;
    }

    private static String plainTitle(GuiOpenPacket packet) {
        if (packet == null || packet.title() == null || packet.title().isBlank()) {
            return "Legacy Mechanics";
        }
        return packet.title();
    }

    @Override
    protected void m_7856_() {
        m_169413_();
        bodyLines.clear();
        parseLayout();
        placePanel();

        int cursorY = panelY + 8;

        // Title
        // Drawn in render; reserve space
        cursorY += 14;

        // Tabs — dynamic from tabsJson
        JsonArray tabs = parseArray(packet.tabsJson());
        int tabX = panelX + 8;
        int tabW = Math.max(48, Math.min(90, (panelW - 16) / Math.max(1, tabs.size())));
        for (JsonElement el : tabs) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject tab = el.getAsJsonObject();
            String id = str(tab, "id", "");
            String label = str(tab, "label", id);
            if (id.isBlank()) {
                continue;
            }
            String actionId = id;
            String args = "{}";
            addThemedButton(
                    tabX,
                    cursorY,
                    tabW - 2,
                    16,
                    stripSection(label),
                    () -> echoClick(actionId, args));
            tabX += tabW;
        }
        if (tabs.size() > 0) {
            cursorY += 20;
        }

        // Body lines
        JsonArray lines = parseArray(packet.linesJson());
        for (JsonElement el : lines) {
            if (el != null && el.isJsonPrimitive()) {
                bodyLines.add(el.getAsString());
            }
        }
        int listH = Math.min(72, Math.max(12, bodyLines.size() * 12));
        if (!bodyLines.isEmpty()) {
            addThemedList(panelX + 8, cursorY, Math.max(1, panelW - 16), listH, bodyLines);
            cursorY += listH + 6;
        }

        // Buttons — dynamic from buttonsJson; x/y relative to panel origin
        JsonArray buttons = parseArray(packet.buttonsJson());
        for (JsonElement el : buttons) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject b = el.getAsJsonObject();
            String id = str(b, "id", "");
            if (id.isBlank()) {
                continue;
            }
            String label = str(b, "label", id);
            int x = intVal(b, "x", 8);
            int y = intVal(b, "y", cursorY - panelY);
            int w = intVal(b, "w", 150);
            int h = intVal(b, "h", 18);
            String argsJson = "{}";
            if (b.has("args") && b.get("args").isJsonObject()) {
                argsJson = b.get("args").toString();
            }
            String actionId = id;
            String args = argsJson;
            addThemedButton(
                    panelX + x,
                    panelY + y,
                    Math.max(20, w),
                    Math.max(12, h),
                    stripSection(label),
                    () -> echoClick(actionId, args));
        }

        // Always offer Close so a sparse packet cannot trap the player
        addThemedButton(
                panelX + panelW - 68,
                panelY + panelH - 22,
                60,
                16,
                "Close",
                this::m_7379_);
    }

    private void echoClick(String actionId, String argsJson) {
        // Verbatim echo — never strip/transform actionId
        LmGuiNetwork.sendAction(packet.screenId(), actionId, argsJson == null ? "{}" : argsJson);
    }

    private void parseLayout() {
        center = true;
        margin = GuiOpenPacket.DEFAULT_MARGIN;
        panelW = packet.panelWidth() > 0 ? packet.panelWidth() : GuiOpenPacket.DEFAULT_PANEL_W;
        panelH = packet.panelHeight() > 0 ? packet.panelHeight() : GuiOpenPacket.DEFAULT_PANEL_H;
        String layout = packet.layoutJson();
        if (layout == null || layout.isBlank()) {
            return;
        }
        try {
            JsonObject o = JsonParser.parseString(layout).getAsJsonObject();
            if (o.has("center") && o.get("center").isJsonPrimitive()) {
                center = o.get("center").getAsBoolean();
            }
            if (o.has("margin")) {
                margin = Math.max(0, o.get("margin").getAsInt());
            }
            if (o.has("panelWidth")) {
                int w = o.get("panelWidth").getAsInt();
                if (w > 0) {
                    panelW = w;
                }
            } else if (o.has("width")) {
                int w = o.get("width").getAsInt();
                if (w > 0) {
                    panelW = w;
                }
            }
            if (o.has("panelHeight")) {
                int h = o.get("panelHeight").getAsInt();
                if (h > 0) {
                    panelH = h;
                }
            } else if (o.has("height")) {
                int h = o.get("height").getAsInt();
                if (h > 0) {
                    panelH = h;
                }
            }
        } catch (Throwable t) {
            UltraUiBridge.LOGGER.warn("[kaipath] layoutJson parse failed — using defaults", t);
        }
    }

    private void placePanel() {
        panelW = Math.min(panelW, Math.max(64, this.f_96543_ - margin * 2));
        panelH = Math.min(panelH, Math.max(64, this.f_96544_ - margin * 2));
        if (center) {
            panelX = Math.max(margin, (this.f_96543_ - panelW) / 2);
            panelY = Math.max(margin, (this.f_96544_ - panelH) / 2);
        } else {
            panelX = margin;
            panelY = margin;
        }
    }

    @Override
    public void m_280273_(GuiGraphics g) {
        // Dim the world behind the dialog
        g.m_280509_(0, 0, this.f_96543_, this.f_96544_, 0xC0101010);
        UltraUiAccess.panel(g, theme(), panelX, panelY, panelW, panelH);
    }

    @Override
    public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float tick) {
        m_280273_(g);
        UltraUiAccess.themed(theme(), () -> {
            String title = stripSection(packet.title());
            g.m_280488_(
                    this.f_96547_,
                    this.f_96547_.m_92834_(title, Math.max(8, panelW - 16)),
                    panelX + 8,
                    panelY + 8,
                    0xFFE8E8E8);
            super.m_88315_(g, mouseX, mouseY, tick);
        });
    }

    private static JsonArray parseArray(String json) {
        if (json == null || json.isBlank()) {
            return new JsonArray();
        }
        try {
            JsonElement el = JsonParser.parseString(json);
            return el.isJsonArray() ? el.getAsJsonArray() : new JsonArray();
        } catch (Throwable ignored) {
            return new JsonArray();
        }
    }

    private static String str(JsonObject o, String key, String fallback) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return o.get(key).getAsString();
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static int intVal(JsonObject o, String key, int fallback) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return o.get(key).getAsInt();
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    /** Strip Minecraft § formatting for widget labels that don't need raw codes. */
    private static String stripSection(String s) {
        if (s == null) {
            return "";
        }
        // Keep § codes — Ultra themed buttons often accept them; only null-safe.
        return s;
    }
}
