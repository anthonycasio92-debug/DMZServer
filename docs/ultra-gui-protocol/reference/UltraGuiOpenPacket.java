package com.example.ultragui;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Server → client: open an Ultra GUI screen. <b>Mod-agnostic wire format</b> — no channel
 * namespace, mod id, or game-content strings are embedded in encode/decode. The Forge channel
 * ({@code <modid>:gui}) is chosen at registration time; this class only serializes fields.
 *
 * <p>Copy into your mod and rename the package. Depends only on Minecraft {@link FriendlyByteBuf}.
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
 *   <li>panelHeight — varint (default 220)
 *   <li>layoutJson — string (JSON object)
 * </ol>
 *
 * <p>Missing trailing fields use defaults. Bytes after field 10 are ignored so newer servers
 * can append fields without breaking older clients.
 */
public final class UltraGuiOpenPacket {
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
        this.screenId = blankTo(trim(screenId, 64), "main");
        this.title = trim(title, 128);
        this.pageId = trim(pageId, 64);
        this.tabsJson = blankTo(trim(tabsJson, 2048), "[]");
        this.activeTab = trim(activeTab, 64);
        this.linesJson = blankTo(trim(linesJson, 4096), "[]");
        this.buttonsJson = blankTo(trim(buttonsJson, 8192), "[]");
        this.panelWidth = panelWidth > 0 ? panelWidth : UltraGuiChannel.DEFAULT_PANEL_WIDTH;
        this.panelHeight = panelHeight > 0 ? panelHeight : UltraGuiChannel.DEFAULT_PANEL_HEIGHT;
        this.layoutJson = trim(layoutJson, 2048);
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

    public static void encode(UltraGuiOpenPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.screenId);
        buf.writeUtf(msg.title);
        buf.writeUtf(msg.pageId);
        buf.writeUtf(msg.tabsJson);
        buf.writeUtf(msg.activeTab);
        buf.writeUtf(msg.linesJson);
        buf.writeUtf(msg.buttonsJson);
        buf.writeVarInt(msg.panelWidth);
        buf.writeVarInt(msg.panelHeight);
        buf.writeUtf(msg.layoutJson == null ? "" : msg.layoutJson);
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
        if (buf.isReadable()) {
            panelWidth = buf.readVarInt();
            if (panelWidth <= 0) {
                panelWidth = UltraGuiChannel.DEFAULT_PANEL_WIDTH;
            }
        }
        if (buf.isReadable()) {
            panelHeight = buf.readVarInt();
            if (panelHeight <= 0) {
                panelHeight = UltraGuiChannel.DEFAULT_PANEL_HEIGHT;
            }
        }
        if (buf.isReadable()) {
            layoutJson = readUtf(buf, "");
        }
        // Ignore any future fields beyond #10.
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
                layoutJson);
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
            String s = buf.readUtf();
            return s == null ? fallback : s;
        } catch (Throwable t) {
            return fallback;
        }
    }
}
