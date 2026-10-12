package com.dbzlegacy.adaptivedifficulty.net.gui;

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
        // 2048: full layout metrics + optional playerModel must not truncate mid-JSON.
        this.layoutJson = trim(
                layoutJson == null || layoutJson.isBlank()
                        ? defaultLayoutJson(this.panelWidth, this.panelHeight)
                        : layoutJson,
                2048);
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

    /** Serialize 11 fields — no channel / mod-id bytes. */
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
        buf.m_130070_(msg.inputsJson);
    }

    /**
     * Decode fields 1–11. Missing trailing fields use {@link UltraGuiProtocol} defaults;
     * leftover bytes after the last read field are left unread (ignored by the caller).
     */
    public static GuiOpenPacket decode(FriendlyByteBuf buf) {
        String screenId = buf.m_130277_();
        String title = buf.m_130277_();
        String pageId = buf.m_130277_();
        String tabsJson = buf.m_130277_();
        String activeTab = buf.m_130277_();
        String linesJson = buf.m_130277_();
        String buttonsJson = buf.m_130277_();
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
                layoutJson = buf.m_130277_();
            }
            if (buf.isReadable()) {
                inputsJson = buf.m_130277_();
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
