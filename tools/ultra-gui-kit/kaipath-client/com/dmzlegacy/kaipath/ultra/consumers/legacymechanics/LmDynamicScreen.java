package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.UltraScreen;
import com.dmzlegacy.kaipath.ultra.UltraUiAccess;
import com.dmzlegacy.kaipath.ultra.UltraUiBridge;
import com.dmzlegacy.kaipath.ultra.consumers.UltraGuiConsumerRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/**
 * Renders whatever the server sends in {@link GuiOpenPacket} — title, tabs, lines, buttons,
 * field 11 {@code inputsJson} text fields, button {@code style} colors, and optional
 * {@code layoutJson.playerModel}.
 *
 * <p>No hardcoded screen/tab/button lists. Clicks go back on the bound {@code <modid>:gui}
 * channel. Advertise via {@link UltraGuiConsumerRegistry#ADVERTISED_FEATURES_JSON}.
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
    private int bodyPad = 0;
    private int bodyMaxHeight = 96;
    private boolean scrollBody = true;
    private int buttonPad = 8;
    private int buttonMinWidth = 80;
    private String titleAlign = "left";
    private JsonObject playerModel;
    private final List<String> bodyLines = new ArrayList<>();
    /** input id → live EditBox (for bindInput / Enter → args.value). */
    private final Map<String, EditBox> textInputs = new LinkedHashMap<>();
    /** input id → args template from inputsJson (for Enter submit). */
    private final Map<String, JsonObject> inputArgsById = new LinkedHashMap<>();
    /** Button chrome underlays drawn from {@code style}. */
    private final List<StyledChrome> buttonChrome = new ArrayList<>();
    /** Tab chrome for activeTab highlight. */
    private final List<StyledChrome> tabChrome = new ArrayList<>();
    private float turntableDegrees;

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
        textInputs.clear();
        inputArgsById.clear();
        buttonChrome.clear();
        tabChrome.clear();
        parseLayout();
        placePanel();

        int cursorY = panelY + 8;

        // Title drawn in render; reserve space
        cursorY += 14;

        // Tabs — dynamic from tabsJson; activeTab gets a highlight chip
        JsonArray tabs = parseArray(packet.tabsJson());
        int tabX = panelX + 8;
        int tabW = Math.max(48, Math.min(90, (panelW - 16) / Math.max(1, tabs.size())));
        String active = packet.activeTab() == null ? "" : packet.activeTab().trim();
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
            String tabSemantic = stripAtPrefix(id);
            if (tabSemantic.startsWith("tab:")) {
                tabSemantic = tabSemantic.substring(4);
            }
            boolean isActive =
                    !active.isBlank() && active.equalsIgnoreCase(tabSemantic);
            if (isActive) {
                tabChrome.add(new StyledChrome(tabX, cursorY, tabW - 2, 16, "confirm"));
            }
            String actionId = id;
            JsonObject tabArgs = new JsonObject();
            addThemedButton(
                    tabX,
                    cursorY,
                    tabW - 2,
                    16,
                    stripSection(label),
                    () -> echoClick(actionId, tabArgs));
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
        int pad = Math.max(0, bodyPad);
        int naturalH = bodyLines.isEmpty() ? 0 : pad * 2 + bodyLines.size() * lineStep;
        int listH = scrollBody
                ? Math.max(
                        lineStep + pad * 2,
                        Math.min(
                                Math.max(bodyMaxHeight, lineStep + pad * 2),
                                Math.max(lineStep + pad * 2, naturalH > 0 ? bodyMaxHeight : 0)))
                : Math.max(lineStep + pad * 2, naturalH);
        if (!bodyLines.isEmpty()) {
            int listY = panelY + Math.max(cursorY - panelY, bodyTop);
            addThemedList(panelX + 8, listY, Math.max(1, panelW - 16), listH, bodyLines);
            cursorY = listY + listH + 6;
        }

        // Text inputs — GuiOpenPacket field 11 inputsJson (type=text)
        JsonArray inputs = parseArray(packet.inputsJson());
        for (JsonElement el : inputs) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject in = el.getAsJsonObject();
            String id = str(in, "id", "");
            if (id.isBlank()) {
                continue;
            }
            String type = str(in, "type", "text");
            if (!"text".equalsIgnoreCase(type)) {
                continue;
            }
            int x = intVal(in, "x", 8);
            int y = intVal(in, "y", cursorY - panelY);
            int w = Math.max(40, intVal(in, "w", panelW - 16));
            int h = Math.max(14, intVal(in, "h", 18));
            int maxLen = Math.max(1, Math.min(256, intVal(in, "maxLength", 64)));
            String value = str(in, "value", "");
            String placeholder = str(in, "placeholder", "");
            JsonObject inputArgs =
                    in.has("args") && in.get("args").isJsonObject()
                            ? in.getAsJsonObject("args")
                            : new JsonObject();
            EditBox box =
                    new EditBox(
                            this.f_96547_,
                            panelX + x,
                            panelY + y,
                            w,
                            h,
                            Component.m_237113_(placeholder.isBlank() ? id : placeholder));
            box.m_94199_(maxLen);
            box.m_94144_(value);
            if (!placeholder.isBlank()) {
                try {
                    box.getClass()
                            .getMethod("m_259207_", Component.class)
                            .invoke(box, Component.m_237113_(placeholder));
                } catch (Throwable ignored) {
                    // hint optional
                }
            }
            m_142416_(box);
            textInputs.put(id, box);
            inputArgsById.put(id, inputArgs);
        }

        // Buttons — dynamic from buttonsJson; honor style chrome
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
            String style = normalizeStyle(str(b, "style", "normal"));
            int x = intVal(b, "x", 8);
            int y = intVal(b, "y", cursorY - panelY);
            int w = Math.max(buttonMinWidth, intVal(b, "w", 150));
            int h = intVal(b, "h", 18);
            int padBtn = intVal(b, "pad", buttonPad);
            if (padBtn < 0) {
                padBtn = buttonPad;
            }
            JsonObject argsObj = new JsonObject();
            if (b.has("args") && b.get("args").isJsonObject()) {
                argsObj = b.getAsJsonObject("args").deepCopy();
            }
            int absX = panelX + x;
            int absY = panelY + y;
            int absW = Math.max(buttonMinWidth, w);
            int absH = Math.max(12, h);
            buttonChrome.add(new StyledChrome(absX, absY, absW, absH, style));
            String actionId = id;
            JsonObject args = argsObj;
            addThemedButton(
                    absX,
                    absY,
                    absW,
                    absH,
                    styleLabel(style, stripSection(label)),
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

    private void echoClick(String actionId, JsonObject argsObj) {
        JsonObject args = argsObj == null ? new JsonObject() : argsObj.deepCopy();
        String bind = str(args, "bindInput", "");
        if (!bind.isBlank()) {
            EditBox box = textInputs.get(bind);
            if (box != null) {
                args.addProperty("value", box.m_94155_() == null ? "" : box.m_94155_());
            }
        }
        UltraGuiConsumerRegistry.sendAction(
                channelId, packet.screenId(), actionId, args.toString());
    }

    @Override
    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        // Enter in a focused text field → submit as input:<id> with args.value
        if (keyCode == 257 || keyCode == 335) { // Enter / Numpad Enter
            for (Map.Entry<String, EditBox> e : textInputs.entrySet()) {
                EditBox box = e.getValue();
                if (box != null && box.m_93696_()) {
                    JsonObject args =
                            inputArgsById
                                    .getOrDefault(e.getKey(), new JsonObject())
                                    .deepCopy();
                    args.addProperty("value", box.m_94155_() == null ? "" : box.m_94155_());
                    UltraGuiConsumerRegistry.sendAction(
                            channelId, packet.screenId(), e.getKey(), args.toString());
                    return true;
                }
            }
        }
        return super.m_7933_(keyCode, scanCode, modifiers);
    }

    private void parseLayout() {
        center = true;
        margin = GuiOpenPacket.DEFAULT_MARGIN;
        lineHeight = 14;
        lineGap = 4;
        bodyTop = 36;
        bodyPad = 0;
        bodyMaxHeight = 96;
        scrollBody = true;
        buttonPad = 8;
        buttonMinWidth = 80;
        titleAlign = "left";
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
            if (o.has("bodyPad")) {
                bodyPad = Math.max(0, o.get("bodyPad").getAsInt());
            }
            if (o.has("titleAlign") && o.get("titleAlign").isJsonPrimitive()) {
                String align = o.get("titleAlign").getAsString();
                titleAlign = align == null ? "left" : align.trim().toLowerCase(Locale.ROOT);
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
        g.m_280509_(0, 0, this.f_96543_, this.f_96544_, 0xC0101010);
        UltraUiAccess.panel(g, theme(), panelX, panelY, panelW, panelH);
    }

    @Override
    public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float tick) {
        m_280273_(g);
        // Style chrome under tabs/buttons (confirm=green, danger=red, dim=gray)
        for (StyledChrome c : tabChrome) {
            paintChrome(g, c);
        }
        for (StyledChrome c : buttonChrome) {
            paintChrome(g, c);
        }
        // Player model entity preview (bounds from layoutJson.playerModel)
        drawPlayerModel(g, mouseX, mouseY, tick);
        UltraUiAccess.themed(theme(), () -> {
            String title = stripSection(packet.title());
            int maxTitleW = Math.max(8, panelW - 16);
            var titleComp = this.f_96547_.m_92834_(title, maxTitleW);
            int titleW = this.f_96547_.m_92852_(titleComp);
            int titleX = "center".equalsIgnoreCase(titleAlign)
                    ? panelX + Math.max(8, (panelW - titleW) / 2)
                    : panelX + 8;
            g.m_280488_(this.f_96547_, titleComp, titleX, panelY + 8, 0xFFE8E8E8);
            super.m_88315_(g, mouseX, mouseY, tick);
        });
    }

    private void paintChrome(GuiGraphics g, StyledChrome c) {
        if (c == null) {
            return;
        }
        int argb = styleArgb(c.style);
        if (argb == 0) {
            return;
        }
        // Soft fill + left accent bar
        UltraUiAccess.fillRect(g, theme(), c.x, c.y, c.w, c.h, argb);
        UltraUiAccess.fillRect(g, theme(), c.x, c.y, 3, c.h, styleAccentArgb(c.style));
    }

    private void drawPlayerModel(GuiGraphics g, int mouseX, int mouseY, float tick) {
        if (playerModel == null) {
            return;
        }
        int pmX = intVal(playerModel, "x", 0);
        int pmY = intVal(playerModel, "y", 0);
        int pmW = Math.max(16, intVal(playerModel, "width", 64));
        int pmH = Math.max(16, intVal(playerModel, "height", 96));
        float pmScale = playerModel.has("scale") ? playerModel.get("scale").getAsFloat() : 1f;
        boolean pmRotate = playerModel.has("rotate") && playerModel.get("rotate").getAsBoolean();
        // parts (headBones / form / extras) — apply when DMZUltra/geo helpers are present
        JsonObject parts =
                playerModel.has("parts") && playerModel.get("parts").isJsonObject()
                        ? playerModel.getAsJsonObject("parts")
                        : null;
        if (parts != null) {
            UltraUiBridge.LOGGER.debug("[kaipath] playerModel.parts schema={}", str(parts, "schema", "?"));
        }

        Minecraft mc = Minecraft.m_91087_();
        if (mc == null || mc.f_91074_ == null) {
            return;
        }
        LivingEntity entity = mc.f_91074_;
        int centerX = panelX + pmX + pmW / 2;
        int centerY = panelY + pmY + pmH;
        int scale = Math.max(8, Math.round(30f * Math.max(0.1f, pmScale)));
        if (pmRotate) {
            turntableDegrees = (turntableDegrees + tick * 1.2f) % 360f;
        }
        float lookX = pmRotate ? (float) Math.sin(Math.toRadians(turntableDegrees)) * 40f : mouseX - centerX;
        float lookY = pmRotate ? -20f : mouseY - (panelY + pmY + pmH / 2f);
        if (!renderEntityPreview(g, centerX, centerY, scale, lookX, lookY, entity)) {
            // Fallback plate so the bounds are visible even if InventoryScreen APIs move
            UltraUiAccess.fillRect(
                    g, theme(), panelX + pmX, panelY + pmY, pmW, pmH, 0x33000000);
        }
    }

    /**
     * Call InventoryScreen entity-preview helpers reflectively — SRG names differ slightly
     * across Forge mappings; try the known 1.20.1 candidates.
     */
    private static boolean renderEntityPreview(
            GuiGraphics g,
            int x,
            int y,
            int scale,
            float lookX,
            float lookY,
            LivingEntity entity) {
        try {
            Class<?> inv = Class.forName("net.minecraft.client.gui.screens.inventory.InventoryScreen");
            // Mojmap: renderEntityInInventoryFollowsMouse(GuiGraphics, int, int, int, float, float, LivingEntity)
            String[] names = {"m_275590_", "m_280432_", "renderEntityInInventoryFollowsMouse"};
            for (String name : names) {
                try {
                    Method m =
                            inv.getMethod(
                                    name,
                                    GuiGraphics.class,
                                    int.class,
                                    int.class,
                                    int.class,
                                    float.class,
                                    float.class,
                                    LivingEntity.class);
                    m.invoke(null, g, x, y, scale, lookX, lookY, entity);
                    return true;
                } catch (NoSuchMethodException ignored) {
                    // try next
                }
            }
        } catch (Throwable t) {
            UltraUiBridge.LOGGER.debug("[kaipath] playerModel draw failed", t);
        }
        return false;
    }

    private static String normalizeStyle(String style) {
        if (style == null || style.isBlank()) {
            return "normal";
        }
        String s = style.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "confirm", "danger", "dim", "normal" -> s;
            default -> "normal";
        };
    }

    private static int styleArgb(String style) {
        return switch (normalizeStyle(style)) {
            case "confirm" -> 0x4022AA44;
            case "danger" -> 0x40CC3333;
            case "dim" -> 0x40333333;
            default -> 0;
        };
    }

    private static int styleAccentArgb(String style) {
        return switch (normalizeStyle(style)) {
            case "confirm" -> 0xFF22AA44;
            case "danger" -> 0xFFCC3333;
            case "dim" -> 0xFF666666;
            default -> 0;
        };
    }

    /** Ensure label carries a readable style tint when the server sent a plain string. */
    private static String styleLabel(String style, String label) {
        if (label == null) {
            return "";
        }
        if (label.indexOf('§') >= 0 || label.indexOf('\u00a7') >= 0) {
            return label;
        }
        return switch (normalizeStyle(style)) {
            case "confirm" -> "§a" + label;
            case "danger" -> "§c" + label;
            case "dim" -> "§8" + label;
            default -> label;
        };
    }

    private static String stripAtPrefix(String actionId) {
        if (actionId == null) {
            return "";
        }
        String a = actionId.trim();
        if (a.startsWith("@")) {
            int colon = a.indexOf(':');
            if (colon > 1 && colon < a.length() - 1) {
                return a.substring(colon + 1);
            }
        }
        return a;
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

    private static String stripSection(String s) {
        return s == null ? "" : s;
    }

    private record StyledChrome(int x, int y, int w, int h, String style) {}
}
