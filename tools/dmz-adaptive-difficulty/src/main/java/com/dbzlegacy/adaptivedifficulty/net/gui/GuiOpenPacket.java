package com.dbzlegacy.adaptivedifficulty.net.gui;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server → client. Opens an Ultra screen on KaiPath.
 *
 * <p>Wire format (protocol {@code 1}): screen id, title, page id, tabs JSON, active tab,
 * body lines JSON, buttons JSON. KaiPath renders; this class has no client imports.
 */
public final class GuiOpenPacket {
    private final String screenId;
    private final String title;
    private final String pageId;
    private final String tabsJson;
    private final String activeTab;
    private final String linesJson;
    private final String buttonsJson;

    public GuiOpenPacket(
            String screenId,
            String title,
            String pageId,
            String tabsJson,
            String activeTab,
            String linesJson,
            String buttonsJson) {
        String id = trim(screenId, 64);
        this.screenId = id.isEmpty() ? "hub" : id;
        this.title = trim(title, 128);
        this.pageId = trim(pageId, 64);
        this.tabsJson = trim(tabsJson, 1024);
        this.activeTab = trim(activeTab, 32);
        this.linesJson = trim(linesJson, 4096);
        this.buttonsJson = trim(buttonsJson, 8192);
    }

    public String screenId() {
        return screenId;
    }

    public static void encode(GuiOpenPacket msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.screenId);
        buf.m_130070_(msg.title);
        buf.m_130070_(msg.pageId);
        buf.m_130070_(msg.tabsJson);
        buf.m_130070_(msg.activeTab);
        buf.m_130070_(msg.linesJson);
        buf.m_130070_(msg.buttonsJson);
    }

    public static GuiOpenPacket decode(FriendlyByteBuf buf) {
        return new GuiOpenPacket(
                buf.m_130277_(),
                buf.m_130277_(),
                buf.m_130277_(),
                buf.m_130277_(),
                buf.m_130277_(),
                buf.m_130277_(),
                buf.m_130277_());
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
}
