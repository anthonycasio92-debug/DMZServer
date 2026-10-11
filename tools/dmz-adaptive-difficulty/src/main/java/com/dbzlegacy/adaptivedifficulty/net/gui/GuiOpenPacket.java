package com.dbzlegacy.adaptivedifficulty.net.gui;

import com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraStyle;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server → client. Opens an Ultra screen on KaiPath.
 *
 * <p>Wire format (protocol {@code 1}, append-compatible):
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
 *       {@code center}, {@code margin})
 * </ol>
 *
 * <p>KaiPath owns the client handler. Older clients that stop after field 7 still open the
 * screen but may render fullscreen until they read the panel fields.
 */
public final class GuiOpenPacket {
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
                UltraStyle.PANEL_W,
                UltraStyle.PANEL_H,
                "");
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
        String id = trim(screenId, 64);
        this.screenId = id.isEmpty() ? "hub" : id;
        this.title = trim(title, 128);
        this.pageId = trim(pageId, 64);
        this.tabsJson = trim(tabsJson, 2048);
        this.activeTab = trim(activeTab, 64);
        this.linesJson = trim(linesJson, 4096);
        this.buttonsJson = trim(buttonsJson, 8192);
        this.panelWidth = panelWidth > 0 ? panelWidth : UltraStyle.PANEL_W;
        this.panelHeight = panelHeight > 0 ? panelHeight : UltraStyle.PANEL_H;
        this.layoutJson = trim(
                layoutJson == null || layoutJson.isBlank()
                        ? defaultLayoutJson(this.panelWidth, this.panelHeight)
                        : layoutJson,
                512);
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

    public static void encode(GuiOpenPacket msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.screenId);
        buf.m_130070_(msg.title);
        buf.m_130070_(msg.pageId);
        buf.m_130070_(msg.tabsJson);
        buf.m_130070_(msg.activeTab);
        buf.m_130070_(msg.linesJson);
        buf.m_130070_(msg.buttonsJson);
        buf.m_130130_(msg.panelWidth);
        buf.m_130130_(msg.panelHeight);
        buf.m_130070_(msg.layoutJson);
    }

    public static GuiOpenPacket decode(FriendlyByteBuf buf) {
        String screenId = buf.m_130277_();
        String title = buf.m_130277_();
        String pageId = buf.m_130277_();
        String tabsJson = buf.m_130277_();
        String activeTab = buf.m_130277_();
        String linesJson = buf.m_130277_();
        String buttonsJson = buf.m_130277_();
        int panelWidth = UltraStyle.PANEL_W;
        int panelHeight = UltraStyle.PANEL_H;
        String layoutJson = "";
        if (buf.isReadable()) {
            panelWidth = buf.m_130242_();
            if (buf.isReadable()) {
                panelHeight = buf.m_130242_();
            }
            if (buf.isReadable()) {
                layoutJson = buf.m_130277_();
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
                layoutJson);
    }

    /** Server never opens screens; KaiPath owns the client handler. */
    public static void handle(GuiOpenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
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
                + UltraStyle.PANEL_MARGIN
                + "}";
    }
}
