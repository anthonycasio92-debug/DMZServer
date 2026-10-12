package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.UltraScreen;
import com.dmzlegacy.kaipath.ultra.UltraUiAccess;
import com.dmzlegacy.kaipath.ultra.UltraUiBridge;
import com.dmzlegacy.kaipath.ultra.consumers.UltraGuiConsumerRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Renders whatever the server sends in {@link GuiOpenPacket} — title, tabs, lines, buttons.
 * No hardcoded screen/tab/button lists. Clicks go back on the bound {@code <modid>:gui} channel.
 *
 * <p><b>JLDK TODO (beyond 1.3.1 baseline):</b>
 *
 * <ul>
 *   <li>Honor button {@code style}: {@code normal}, {@code confirm} (green), {@code danger}
 *       (red), {@code dim} (dark gray).
 *   <li>Honor {@link GuiOpenPacket#activeTab()} — visually distinguish the active tab.
 *   <li>Advertise {@code ["style","activeTab"]} via {@link GuiFeaturesPacket} /
 *       {@link UltraGuiConsumerRegistry#advertiseFeatures}.
 * </ul>
 */
public class LmDynamicScreen extends UltraScreen {
    private final GuiOpenPacket packet;
    private final String channelId;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private boolean center = true;
    private int margin = GuiOpenPacket.DEFAULT_MARGIN;
    private int lineHeight = 14;
    private int lineGap = 4;
    private int bodyTop = 36;
    private int bodyMaxHeight = 96;
    private boolean scrollBody = true;
    private int buttonPad = 8;
    private int buttonMinWidth = 80;
    private JsonObject playerModel;
    private final List<String> bodyLines = new ArrayList<>();

    public LmDynamicScreen(GuiOpenPacket packet) {
        this(packet, LmGuiNetwork.CHANNEL_ID);
    }

    public LmDynamicScreen(GuiOpenPacket packet, String channelId) {
        super(Component.m_237113_(plainTitle(packet)));
        this.packet = packet == null ? GuiOpenPacket.emptyHub() : packet;
        this.channelId = channelId == null || channelId.isBlank()
                ? LmGuiNetwork.CHANNEL_ID
                : channelId;
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

        // Body lines — honor layoutJson lineHeight/lineGap/bodyMaxHeight; scroll, don't squeeze
        JsonArray lines = parseArray(packet.linesJson());
        for (JsonElement el : lines) {
            if (el != null && el.isJsonPrimitive()) {
                bodyLines.add(el.getAsString());
            }
        }
        int lineStep = Math.max(8, lineHeight + lineGap);
        int naturalH = bodyLines.isEmpty() ? 0 : bodyLines.size() * lineStep;
        int listH = scrollBody
                ? Math.min(Math.max(bodyMaxHeight, lineStep), Math.max(lineStep, naturalH > 0 ? bodyMaxHeight : 0))
                : Math.max(lineStep, naturalH);
        if (!bodyLines.isEmpty()) {
            int listY = panelY + Math.max(cursorY - panelY, bodyTop);
            addThemedList(panelX + 8, listY, Math.max(1, panelW - 16), listH, bodyLines);
            cursorY = listY + listH + 6;
        }

        // Optional playerModel — JLDK draws the entity; unknown clients ignore the field.
        if (playerModel != null) {
            UltraUiBridge.LOGGER.debug(
                    "[kaipath] playerModel present x={} y={} w={} h={} scale={} rotate={}",
                    intVal(playerModel, "x", 0),
                    intVal(playerModel, "y", 0),
                    intVal(playerModel, "width", 0),
                    intVal(playerModel, "height", 0),
                    playerModel.has("scale") ? playerModel.get("scale").getAsFloat() : 1f,
                    playerModel.has("rotate") && playerModel.get("rotate").getAsBoolean());
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
            int w = Math.max(buttonMinWidth, intVal(b, "w", 150));
            int h = intVal(b, "h", 18);
            int pad = intVal(b, "pad", buttonPad);
            // pad reserved for themed button label inset (min width already enforced)
            if (pad < 0) {
                pad = buttonPad;
            }
            String argsJson = "{}";
            if (b.has("args") && b.get("args").isJsonObject()) {
                argsJson = b.get("args").toString();
            }
            String actionId = id;
            String args = argsJson;
            addThemedButton(
                    panelX + x,
                    panelY + y,
                    Math.max(buttonMinWidth, w),
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
        // Verbatim echo on the same channel that opened this screen — never strip actionId
        UltraGuiConsumerRegistry.sendAction(
                channelId,
                packet.screenId(),
                actionId,
                argsJson == null ? "{}" : argsJson);
    }

    private void parseLayout() {
        center = true;
        margin = GuiOpenPacket.DEFAULT_MARGIN;
        lineHeight = 14;
        lineGap = 4;
        bodyTop = 36;
        bodyMaxHeight = 96;
        scrollBody = true;
        buttonPad = 8;
        buttonMinWidth = 80;
        playerModel = null;
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
            if (o.has("lineHeight")) {
                lineHeight = Math.max(8, o.get("lineHeight").getAsInt());
            }
            if (o.has("lineGap")) {
                lineGap = Math.max(0, o.get("lineGap").getAsInt());
            }
            if (o.has("bodyTop")) {
                bodyTop = Math.max(0, o.get("bodyTop").getAsInt());
            }
            if (o.has("bodyMaxHeight")) {
                bodyMaxHeight = Math.max(lineHeight, o.get("bodyMaxHeight").getAsInt());
            }
            if (o.has("scrollBody") && o.get("scrollBody").isJsonPrimitive()) {
                scrollBody = o.get("scrollBody").getAsBoolean();
            }
            if (o.has("buttonPad")) {
                buttonPad = Math.max(0, o.get("buttonPad").getAsInt());
            }
            if (o.has("buttonMinWidth")) {
                buttonMinWidth = Math.max(20, o.get("buttonMinWidth").getAsInt());
            }
            if (o.has("playerModel") && o.get("playerModel").isJsonObject()) {
                playerModel = o.getAsJsonObject("playerModel");
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
