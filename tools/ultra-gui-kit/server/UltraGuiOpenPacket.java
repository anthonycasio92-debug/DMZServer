package com.example.ultragui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server → client: open an Ultra GUI screen. <b>Mod-agnostic wire format</b> — no channel
 * namespace, mod id, or game-content strings are embedded in encode/decode. The Forge channel
 * ({@code <modid>:gui}) is chosen at registration time; this class only serializes fields.
 *
 * <p>Copy into your mod and rename the package. Depends on Minecraft {@link FriendlyByteBuf}
 * and Gson (already on the Forge compile classpath) for layout sanitizing.
 *
 * <p>Every server codec in this kit writes strings through {@link #writeUtf8} and varints
 * through {@code m_130130_} / {@code m_130242_}. Those SRG names are what this repo's
 * LegacyMechanics sources and {@code tools/ultra-gui-kit/stubs} compile against. On a Mojmap
 * Forge MDK the same two methods are {@code writeVarInt} / {@code readVarInt}.
 *
 * <p>Field order (protocol {@link UltraGuiChannel#PROTOCOL_VERSION}):
 *
 * <ol>
 *   <li>screenId — string
 *   <li>title — string
 *   <li>pageId — string
 *   <li>tabsJson — string (JSON array of {@code {id,label}})
 *   <li>activeTab — string
 *   <li>linesJson — string (JSON array of body lines)
 *   <li>buttonsJson — string (JSON array of button objects)
 *   <li>panelWidth — varint (default 350)
 *   <li>panelHeight — varint (default 300; may grow toward 420 when content requires)
 *   <li>layoutJson — string (JSON object; always includes {@code titleAlign})
 *   <li>inputsJson — string (JSON array of text fields; optional, default {@code []})
 * </ol>
 *
 * <p>Missing trailing fields use defaults. Bytes after the last understood field are ignored.
 */
public final class UltraGuiOpenPacket {
    /**
     * Max layoutJson chars on the wire. Must stay valid JSON — cutting with {@code substring}
     * makes KaiPath drop the object and default {@code titleAlign} to left.
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

    public UltraGuiOpenPacket(
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

    public UltraGuiOpenPacket(
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
        this.screenId = blankTo(trim(screenId, 64), "main");
        this.title = trim(title, 128);
        this.pageId = trim(pageId, 64);
        this.tabsJson = blankTo(trim(tabsJson, 2048), "[]");
        this.activeTab = trim(activeTab, 64);
        this.linesJson = blankTo(trim(linesJson, 4096), "[]");
        this.buttonsJson = blankTo(trim(buttonsJson, 8192), "[]");
        this.panelWidth = panelWidth > 0 ? panelWidth : UltraGuiChannel.DEFAULT_PANEL_WIDTH;
        this.panelHeight = panelHeight > 0 ? panelHeight : UltraGuiChannel.DEFAULT_PANEL_HEIGHT;
        // Keep valid JSON and always force titleAlign=center (KaiPath left-defaults otherwise).
        this.layoutJson =
                sanitizeLayoutJson(
                        layoutJson == null || layoutJson.isBlank()
                                ? defaultLayoutJson(this.panelWidth, this.panelHeight)
                                : layoutJson,
                        this.panelWidth,
                        this.panelHeight);
        this.inputsJson = blankTo(trim(inputsJson, 4096), "[]");
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

    public String inputsJson() {
        return inputsJson;
    }

    public static void encode(UltraGuiOpenPacket msg, FriendlyByteBuf buf) {
        writeUtf8(buf, msg.screenId);
        writeUtf8(buf, msg.title);
        writeUtf8(buf, msg.pageId);
        writeUtf8(buf, msg.tabsJson);
        writeUtf8(buf, msg.activeTab);
        writeUtf8(buf, msg.linesJson);
        writeUtf8(buf, msg.buttonsJson);
        buf.m_130130_(msg.panelWidth);
        buf.m_130130_(msg.panelHeight);
        writeUtf8(buf, msg.layoutJson == null ? "" : msg.layoutJson);
        writeUtf8(buf, msg.inputsJson == null ? "[]" : msg.inputsJson);
    }

    public static UltraGuiOpenPacket decode(FriendlyByteBuf buf) {
        String screenId = readUtf(buf, "main");
        String title = buf.isReadable() ? readUtf(buf, "") : "";
        String pageId = buf.isReadable() ? readUtf(buf, "main") : "main";
        String tabsJson = buf.isReadable() ? readUtf(buf, "[]") : "[]";
        String activeTab = buf.isReadable() ? readUtf(buf, "") : "";
        String linesJson = buf.isReadable() ? readUtf(buf, "[]") : "[]";
        String buttonsJson = buf.isReadable() ? readUtf(buf, "[]") : "[]";
        int panelWidth = UltraGuiChannel.DEFAULT_PANEL_WIDTH;
        int panelHeight = UltraGuiChannel.DEFAULT_PANEL_HEIGHT;
        String layoutJson = "";
        String inputsJson = "[]";
        if (buf.isReadable()) {
            panelWidth = buf.m_130242_();
            if (panelWidth <= 0) {
                panelWidth = UltraGuiChannel.DEFAULT_PANEL_WIDTH;
            }
        }
        if (buf.isReadable()) {
            panelHeight = buf.m_130242_();
            if (panelHeight <= 0) {
                panelHeight = UltraGuiChannel.DEFAULT_PANEL_HEIGHT;
            }
        }
        if (buf.isReadable()) {
            layoutJson = readUtf(buf, "");
        }
        if (buf.isReadable()) {
            inputsJson = readUtf(buf, "[]");
        }
        return new UltraGuiOpenPacket(
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

    private static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        return text.length() <= max ? text : text.substring(0, max);
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String readUtf(FriendlyByteBuf buf, String fallback) {
        try {
            String s = readUtf8(buf);
            return s == null || s.isEmpty() ? fallback : s;
        } catch (Throwable t) {
            return fallback;
        }
    }

    /**
     * Length-prefixed UTF-8 — same wire shape as {@code FriendlyByteBuf.writeUtf}, charset always
     * {@link StandardCharsets#UTF_8}. Shared by the open, action, and features codecs.
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

    /**
     * Ensures layoutJson stays parseable and always carries {@code titleAlign=center}. When the
     * payload is oversized, drop optional {@code playerModel.parts} then {@code playerModel}
     * rather than cutting mid-JSON.
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
                if (pm.has("parts")) {
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
                + UltraGuiChannel.DEFAULT_MARGIN
                + ",\"titleAlign\":\"center\"}";
    }
}
