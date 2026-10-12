package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.UltraUiBridge;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server → client Ultra GUI open. Protocol {@link LmGuiNetwork#PROTOCOL_VERSION} field layout
 * matches LM {@code docs/KAIPATH_GUI_PROTOCOL.md} (10 fields, append-tolerant).
 */
public final class GuiOpenPacket {
    public static final int DEFAULT_PANEL_W = 350;
    public static final int DEFAULT_PANEL_H = 300;
    public static final int DEFAULT_MARGIN = 10;

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
            String buttonsJson,
            int panelWidth,
            int panelHeight,
            String layoutJson) {
        this.screenId = blankTo(trim(screenId, 64), "hub");
        this.title = trim(title, 128);
        this.pageId = trim(pageId, 64);
        this.tabsJson = blankTo(trim(tabsJson, 2048), "[]");
        this.activeTab = trim(activeTab, 64);
        this.linesJson = blankTo(trim(linesJson, 4096), "[]");
        this.buttonsJson = blankTo(trim(buttonsJson, 8192), "[]");
        this.panelWidth = panelWidth > 0 ? panelWidth : DEFAULT_PANEL_W;
        this.panelHeight = panelHeight > 0 ? panelHeight : DEFAULT_PANEL_H;
        this.layoutJson = trim(layoutJson, 1024);
    }

    public static GuiOpenPacket emptyHub() {
        return new GuiOpenPacket(
                "hub",
                "Legacy Mechanics",
                "main",
                "[]",
                "home",
                "[]",
                "[]",
                DEFAULT_PANEL_W,
                DEFAULT_PANEL_H,
                "{\"center\":true,\"margin\":10,\"titleAlign\":\"center\",\"lineHeight\":16,\"lineGap\":8,"
                        + "\"bodyTop\":42,\"bodyPad\":10,\"bodyMaxHeight\":140,\"scrollBody\":true,"
                        + "\"buttonPad\":8,\"buttonGap\":6,\"buttonMinWidth\":80,\"buttonHeight\":18}");
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
        buf.m_130070_(msg.layoutJson == null ? "" : msg.layoutJson);
    }

    /**
     * Decode all 10 fields. Missing trailing fields use defaults; extra bytes after field 10 are
     * ignored so LM can append fields later without breaking this client.
     */
    public static GuiOpenPacket decode(FriendlyByteBuf buf) {
        String screenId = readUtf(buf, "hub");
        String title = buf.isReadable() ? readUtf(buf, "") : "";
        String pageId = buf.isReadable() ? readUtf(buf, "main") : "main";
        String tabsJson = buf.isReadable() ? readUtf(buf, "[]") : "[]";
        String activeTab = buf.isReadable() ? readUtf(buf, "") : "";
        String linesJson = buf.isReadable() ? readUtf(buf, "[]") : "[]";
        String buttonsJson = buf.isReadable() ? readUtf(buf, "[]") : "[]";
        int panelWidth = DEFAULT_PANEL_W;
        int panelHeight = DEFAULT_PANEL_H;
        String layoutJson = "";
        if (buf.isReadable()) {
            panelWidth = buf.m_130242_();
            if (panelWidth <= 0) {
                panelWidth = DEFAULT_PANEL_W;
            }
        }
        if (buf.isReadable()) {
            panelHeight = buf.m_130242_();
            if (panelHeight <= 0) {
                panelHeight = DEFAULT_PANEL_H;
            }
        }
        if (buf.isReadable()) {
            layoutJson = readUtf(buf, "");
        }
        // Ignore any future fields beyond #10.
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

    public static void handle(GuiOpenPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            try {
                LmScreens.open(packet);
            } catch (Throwable t) {
                UltraUiBridge.LOGGER.error("[kaipath] LM GuiOpenPacket handle failed screenId={}",
                        packet == null ? "<null>" : packet.screenId(), t);
            }
        });
        context.setPacketHandled(true);
    }

    static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String readUtf(FriendlyByteBuf buf, String fallback) {
        try {
            String s = buf.m_130277_();
            return s == null ? fallback : s;
        } catch (Throwable t) {
            return fallback;
        }
    }
}
