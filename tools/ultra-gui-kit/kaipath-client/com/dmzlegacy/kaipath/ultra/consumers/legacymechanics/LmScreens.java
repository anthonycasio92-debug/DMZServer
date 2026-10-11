package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.UltraScreen;
import com.dmzlegacy.kaipath.ultra.UltraUiBridge;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Opens LM Ultra screens. <b>No screenId whitelist</b> — any id renders via
 * {@link LmDynamicScreen}. Optional {@link #register} factories are only used when a caller
 * opens by bare id without a packet (legacy); packet opens always use the dynamic renderer.
 */
public final class LmScreens {
    private static final Map<String, Function<Minecraft, UltraScreen>> SCREENS = new HashMap<>();

    private LmScreens() {}

    public static void registerDefaults() {
        // Intentionally empty whitelist. Dynamic packet path is the default.
        // Keep hub factory only for legacy callers that construct by id without a packet.
        SCREENS.put("hub", mc -> new LmDynamicScreen(GuiOpenPacket.emptyHub()));
    }

    public static void register(String id, Function<Minecraft, UltraScreen> factory) {
        if (id != null && !id.isBlank() && factory != null) {
            SCREENS.put(id.trim(), factory);
        }
    }

    /** Preferred entry: full packet → dynamic screen (any screenId) on the LM channel. */
    public static void open(GuiOpenPacket packet) {
        open(packet, LmGuiNetwork.CHANNEL_ID);
    }

    /**
     * Open a dynamic screen bound to {@code channelId} so clicks echo on the same
     * {@code <modid>:gui} that delivered the open.
     */
    public static void open(GuiOpenPacket packet, String channelId) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc == null) {
            return;
        }
        String channel = channelId == null || channelId.isBlank() ? LmGuiNetwork.CHANNEL_ID : channelId;
        if (packet == null) {
            UltraUiBridge.LOGGER.warn(
                    "[kaipath] GuiOpenPacket null on channel={} — opening empty shell", channel);
            packet = GuiOpenPacket.emptyHub();
        }
        try {
            UltraUiBridge.LOGGER.info(
                    "[kaipath] GuiOpenPacket ← channel={} screenId={} title={} buttons≈{} panel={}x{}",
                    channel,
                    packet.screenId(),
                    packet.title(),
                    approxButtonCount(packet.buttonsJson()),
                    packet.panelWidth(),
                    packet.panelHeight());
            mc.m_91152_(new LmDynamicScreen(packet, channel));
        } catch (RuntimeException e) {
            UltraUiBridge.LOGGER.warn(
                    "[kaipath] dynamic screen failed channel={} screenId={}",
                    channel,
                    packet.screenId(),
                    e);
            mc.m_91152_(new MissingScreen(packet.screenId(), e.getMessage()));
        }
    }

    /**
     * Legacy id-only open (no packet body). Prefer {@link #open(GuiOpenPacket)}.
     * Unregistered ids still open a dynamic empty shell — never MissingScreen for unknown id alone.
     */
    public static void open(String screenId) {
        String id = screenId == null || screenId.isBlank() ? "hub" : screenId.trim();
        Function<Minecraft, UltraScreen> factory = SCREENS.get(id);
        Minecraft mc = Minecraft.m_91087_();
        if (mc == null) {
            return;
        }
        try {
            if (factory != null) {
                mc.m_91152_(factory.apply(mc));
            } else {
                // Future-proof: unknown id → empty dynamic screen, not MissingScreen.
                mc.m_91152_(new LmDynamicScreen(new GuiOpenPacket(
                        id,
                        id,
                        "main",
                        "[]",
                        "home",
                        "[\"No packet body for this open.\"]",
                        "[]",
                        GuiOpenPacket.DEFAULT_PANEL_W,
                        GuiOpenPacket.DEFAULT_PANEL_H,
                        "{\"center\":true,\"margin\":6}")));
            }
        } catch (RuntimeException e) {
            UltraUiBridge.LOGGER.warn("[kaipath] LM screen {} failed", id, e);
            mc.m_91152_(new MissingScreen(id, e.getMessage()));
        }
    }

    private static int approxButtonCount(String buttonsJson) {
        if (buttonsJson == null || buttonsJson.isBlank() || "[]".equals(buttonsJson.trim())) {
            return 0;
        }
        int n = 0;
        int i = 0;
        while ((i = buttonsJson.indexOf("\"id\"", i)) >= 0) {
            n++;
            i += 4;
        }
        return n;
    }

    /** Only for truly malformed opens (exception while constructing the dynamic screen). */
    static final class MissingScreen extends UltraScreen {
        private final String detail;

        MissingScreen(String id, String detail) {
            super(Component.m_237113_("LM GUI error"));
            this.detail = "Failed: " + (id == null ? "?" : id)
                    + (detail == null || detail.isBlank() ? "" : " — " + detail);
        }

        @Override
        protected void m_7856_() {
            addThemedList(20, 40, Math.max(1, this.f_96543_ - 40), 48, List.of(detail));
            addThemedButton(20, 100, 120, 20, "Close", this::m_7379_);
        }
    }
}
