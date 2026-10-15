package com.dbzlegacy.adaptivedifficulty.net.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server → client. Opens an Ultra screen on KaiPath.
 *
 * <p><b>Mod-agnostic wire codec</b> — encode/decode contain no channel namespace, mod id, or
 * content-specific strings. The owning mod selects {@code <modid>:gui} at channel registration
 * ({@link UltraGuiProtocol#channel(String)}). Reference copy for other mods:
 * {@code tools/ultra-gui-kit/server/UltraGuiOpenPacket.java}.
 *
 * <p>Wire format (protocol {@link UltraGuiProtocol#PROTOCOL_VERSION}, append-compatible):
 *
 * <ol>
 *   <li>screen id
 *   <li>title
 *   <li>page id
 *   <li>tabs JSON (array)
 *   <li>active tab
 *   <li>body lines JSON (array)
 *   <li>buttons JSON (array of objects with {@code id}/{@code label}/{@code style}/{@code x}/
 *       {@code y}/{@code w}/{@code h}/optional {@code args})
 *   <li>panel width (GUI px) — centered dialog; KaiPath must not stretch to fullscreen
 *   <li>panel height (GUI px)
 *   <li>layout JSON ({@code panelWidth}, {@code panelHeight}, {@code width}, {@code height},
 *       {@code center}, {@code margin}, {@code titleAlign}, …)
 *   <li>inputs JSON (array of text fields / future controls) — optional; older clients ignore
 * </ol>
 *
 * <p>KaiPath owns the client handler. Older clients that stop after field 7 still open the
 * screen but may render fullscreen until they read the panel fields. Bytes after the last
 * understood field are ignored.
 */
public final class GuiOpenPacket {
    /**
     * Max layoutJson chars on the wire. Must stay valid JSON — hard truncation makes KaiPath
     * drop the object and default {@code titleAlign} to left.
     */
    public static final int LAYOUT_JSON_MAX = 4096;

    private final String screenId;
    private final String title;
    private final String pageId;
    private final String tabsJson;
    private final String activeTab;
    private final String linesJson;
    private final String buttonsJson;
    private final int panelWidth;
    private final int panelHeight;
    private final String layoutJson;
    private final String inputsJson;

    public GuiOpenPacket(
            String screenId,
            String title,
            String pageId,
            String tabsJson,
            String activeTab,
            String linesJson,
            String buttonsJson) {
        this(
                screenId,
                title,
                pageId,
                tabsJson,
                activeTab,
                linesJson,
                buttonsJson,
                UltraGuiProtocol.DEFAULT_PANEL_WIDTH,
                UltraGuiProtocol.DEFAULT_PANEL_HEIGHT,
                "",
                "[]");
    }

    public GuiOpenPacket(
            String screenId,
            String title,
            String pageId,
            String tabsJson,
            String activeTab,
            String linesJson,
            String buttonsJson,
            int panelWidth,
            int panelHeight,
            String layoutJson) {
        this(
                screenId,
                title,
                pageId,
                tabsJson,
                activeTab,
                linesJson,
                buttonsJson,
                panelWidth,
                panelHeight,
                layoutJson,
                "[]");
    }

    public GuiOpenPacket(
            String screenId,
            String title,
            String pageId,
            String tabsJson,
            String activeTab,
            String linesJson,
            String buttonsJson,
            int panelWidth,
            int panelHeight,
            String layoutJson,
            String inputsJson) {
        String id = trim(screenId, 64);
        // Empty id → "main" (protocol default). LM content often uses "hub" explicitly.
        this.screenId = id.isEmpty() ? "main" : id;
        this.title = trim(title, 128);
        this.pageId = trim(pageId, 64);
        this.tabsJson = trim(tabsJson, 2048);
        this.activeTab = trim(activeTab, 64);
        this.linesJson = trim(linesJson, 4096);
        this.buttonsJson = trim(buttonsJson, 8192);
        this.panelWidth = panelWidth > 0 ? panelWidth : UltraGuiProtocol.DEFAULT_PANEL_WIDTH;
        this.panelHeight = panelHeight > 0 ? panelHeight : UltraGuiProtocol.DEFAULT_PANEL_HEIGHT;
        // Keep valid JSON and always force titleAlign=center (KaiPath left-defaults otherwise).
        this.layoutJson =
                sanitizeLayoutJson(
                        layoutJson == null || layoutJson.isBlank()
                                ? defaultLayoutJson(this.panelWidth, this.panelHeight)
                                : layoutJson,
                        this.panelWidth,
                        this.panelHeight);
        this.inputsJson = blankToArray(trim(inputsJson, 4096));
    }

    public String screenId() {
        return screenId;
    }

    public String title() {
        return title;
    }

    public String pageId() {
        return pageId;
    }

    public String tabsJson() {
        return tabsJson;
    }

    public String activeTab() {
        return activeTab;
    }

    public String linesJson() {
        return linesJson;
    }

    public String buttonsJson() {
        return buttonsJson;
    }

    public int panelWidth() {
        return panelWidth;
    }

    public int panelHeight() {
        return panelHeight;
    }

    public String layoutJson() {
        return layoutJson;
    }

    /** Field 11 — JSON array of input elements ({@code type=text} today). */
    public String inputsJson() {
        return inputsJson;
    }

    /** Serialize 11 fields — no channel / mod-id bytes. JSON strings use explicit UTF-8. */
    public static void encode(GuiOpenPacket msg, FriendlyByteBuf buf) {
        writeUtf8(buf, msg.screenId);
        writeUtf8(buf, msg.title);
        writeUtf8(buf, msg.pageId);
        writeUtf8(buf, msg.tabsJson);
        writeUtf8(buf, msg.activeTab);
        writeUtf8(buf, msg.linesJson);
        writeUtf8(buf, msg.buttonsJson);
        buf.m_130130_(msg.panelWidth);
        buf.m_130130_(msg.panelHeight);
        writeUtf8(buf, msg.layoutJson);
        writeUtf8(buf, msg.inputsJson);
    }

    /**
     * Decode fields 1–11. Missing trailing fields use {@link UltraGuiProtocol} defaults;
     * leftover bytes after the last read field are left unread (ignored by the caller).
     * Strings are decoded with {@link StandardCharsets#UTF_8} (never platform default).
     */
    public static GuiOpenPacket decode(FriendlyByteBuf buf) {
        String screenId = readUtf8(buf);
        String title = readUtf8(buf);
        String pageId = readUtf8(buf);
        String tabsJson = readUtf8(buf);
        String activeTab = readUtf8(buf);
        String linesJson = readUtf8(buf);
        String buttonsJson = readUtf8(buf);
        int panelWidth = UltraGuiProtocol.DEFAULT_PANEL_WIDTH;
        int panelHeight = UltraGuiProtocol.DEFAULT_PANEL_HEIGHT;
        String layoutJson = "";
        String inputsJson = "[]";
        if (buf.isReadable()) {
            panelWidth = buf.m_130242_();
            if (panelWidth <= 0) {
                panelWidth = UltraGuiProtocol.DEFAULT_PANEL_WIDTH;
            }
            if (buf.isReadable()) {
                panelHeight = buf.m_130242_();
                if (panelHeight <= 0) {
                    panelHeight = UltraGuiProtocol.DEFAULT_PANEL_HEIGHT;
                }
            }
            if (buf.isReadable()) {
                layoutJson = readUtf8(buf);
            }
            if (buf.isReadable()) {
                inputsJson = readUtf8(buf);
            }
        }
        return new GuiOpenPacket(
                screenId,
                title,
                pageId,
                tabsJson,
                activeTab,
                linesJson,
                buttonsJson,
                panelWidth,
                panelHeight,
                layoutJson,
                inputsJson);
    }

    /**
     * Length-prefixed UTF-8 string — same wire shape as {@code FriendlyByteBuf.writeUtf},
     * but charset is always {@link StandardCharsets#UTF_8} (never the platform default).
     */
    static void writeUtf8(FriendlyByteBuf buf, String value) {
        String s = value == null ? "" : value;
        byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
        buf.m_130130_(utf8.length);
        buf.writeBytes(utf8);
    }

    /** Inverse of {@link #writeUtf8}; empty when the buffer ends early. */
    static String readUtf8(FriendlyByteBuf buf) {
        if (buf == null || !buf.isReadable()) {
            return "";
        }
        int len = buf.m_130242_();
        if (len < 0 || len > buf.readableBytes()) {
            return "";
        }
        byte[] utf8 = new byte[len];
        buf.readBytes(utf8);
        return new String(utf8, StandardCharsets.UTF_8);
    }

    /** Server never opens screens; KaiPath owns the client handler. */
    public static void handle(GuiOpenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // No player/world access on the network thread — mark handled only.
        context.setPacketHandled(true);
    }

    static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }

    private static String blankToArray(String json) {
        return json == null || json.isBlank() ? "[]" : json;
    }

    /**
     * Ensures layoutJson stays parseable and always carries {@code titleAlign=center}. When the
     * payload is oversized, drop optional {@code playerModel.parts} then {@code playerModel}
     * rather than cutting mid-JSON (which left-aligns titles on stock KaiPath).
     */
    public static String sanitizeLayoutJson(String layoutJson, int w, int h) {
        String fallback = defaultLayoutJson(w, h);
        if (layoutJson == null || layoutJson.isBlank()) {
            return fallback;
        }
        try {
            JsonObject o = JsonParser.parseString(layoutJson).getAsJsonObject();
            o.addProperty("titleAlign", "center");
            String out = o.toString();
            if (out.length() <= LAYOUT_JSON_MAX) {
                return out;
            }
            if (o.has("playerModel") && o.get("playerModel").isJsonObject()) {
                JsonObject pm = o.getAsJsonObject("playerModel");
                if (pm.has("parts") && pm.get("parts").isJsonObject()) {
                    JsonObject parts = pm.getAsJsonObject("parts");
                    // Keep who to draw after cosmetics are dropped. Stock clients that
                    // ignore subject still render; clients that honor it must not fall back
                    // to the local player just because parts was oversized.
                    if (!pm.has("subject") && parts.has("subject") && !parts.get("subject").isJsonNull()) {
                        pm.add("subject", parts.get("subject"));
                    }
                    pm.remove("parts");
                    out = o.toString();
                    if (out.length() <= LAYOUT_JSON_MAX) {
                        return out;
                    }
                }
                o.remove("playerModel");
                out = o.toString();
                if (out.length() <= LAYOUT_JSON_MAX) {
                    return out;
                }
            }
            return fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static String defaultLayoutJson(int w, int h) {
        return "{\"panelWidth\":"
                + w
                + ",\"panelHeight\":"
                + h
                + ",\"width\":"
                + w
                + ",\"height\":"
                + h
                + ",\"center\":true,\"margin\":"
                + UltraGuiProtocol.DEFAULT_MARGIN
                + ",\"titleAlign\":\"center\"}";
    }
}
