package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.UltraUiBridge;
import java.util.function.Supplier;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server → client Ultra GUI open. Protocol {@link LmGuiNetwork#PROTOCOL_VERSION} field layout
 * matches {@code docs/KAIPATH_GUI_PROTOCOL.md} (11 fields, append-tolerant). Channel-agnostic
 * — the same codec serves every {@code <modid>:gui} partner, not only LegacyMechanics.
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
    private final String inputsJson;

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
        this.screenId = blankTo(trim(screenId, 64), "hub");
        this.title = trim(title, 128);
        this.pageId = trim(pageId, 64);
        this.tabsJson = blankTo(trim(tabsJson, 2048), "[]");
        this.activeTab = trim(activeTab, 64);
        this.linesJson = blankTo(trim(linesJson, 4096), "[]");
        this.buttonsJson = blankTo(trim(buttonsJson, 8192), "[]");
        this.panelWidth = panelWidth > 0 ? panelWidth : DEFAULT_PANEL_W;
        this.panelHeight = panelHeight > 0 ? panelHeight : DEFAULT_PANEL_H;
        this.layoutJson = trim(layoutJson, 4096);
        this.inputsJson = blankTo(trim(inputsJson, 4096), "[]");
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
                        + "\"buttonPad\":8,\"buttonGap\":6,\"buttonMinWidth\":80,\"buttonHeight\":18}",
                "[]");
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

    /** Field 11 — text inputs (JLDK renders when {@code textInput} is advertised). */
    public String inputsJson() {
        return inputsJson;
    }

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
        writeUtf8(buf, msg.layoutJson == null ? "" : msg.layoutJson);
        writeUtf8(buf, msg.inputsJson == null ? "[]" : msg.inputsJson);
    }

    /**
     * Decode fields 1–11. Missing trailing fields use defaults; extra bytes after the last
     * read field are ignored.
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
        String inputsJson = "[]";
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
        if (buf.isReadable()) {
            inputsJson = readUtf(buf, "[]");
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

    public static void handle(GuiOpenPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            try {
                LmScreens.open(packet);
            } catch (Throwable t) {
                UltraUiBridge.LOGGER.error("[kaipath] GuiOpenPacket handle failed screenId={}",
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

    /** Length-prefixed UTF-8 — never platform default charset. */
    public static void writeUtf8(FriendlyByteBuf buf, String value) {
        String s = value == null ? "" : value;
        byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
        buf.m_130130_(utf8.length);
        buf.writeBytes(utf8);
    }

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
}
