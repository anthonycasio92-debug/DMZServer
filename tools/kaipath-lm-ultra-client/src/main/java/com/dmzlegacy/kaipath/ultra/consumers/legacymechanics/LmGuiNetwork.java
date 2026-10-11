package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.UltraUiBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Client half of {@code legacymechanics:gui}. Protocol string must match LM
 * {@code LmGuiNetwork.PROTOCOL_VERSION}.
 */
public final class LmGuiNetwork {
    public static final String CHANNEL_NAMESPACE = "legacymechanics";
    public static final String CHANNEL_PATH = "gui";
    /**
     * Forge SimpleChannel protocol. Bumping requires coordinated LM release.
     * Mismatched remotes log a warning but still accept (never refuse to open).
     */
    public static final String PROTOCOL_VERSION = "1";

    /** @deprecated use {@link #PROTOCOL_VERSION} */
    @Deprecated
    public static final String PROTOCOL = PROTOCOL_VERSION;

    private static SimpleChannel channel;

    private LmGuiNetwork() {}

    public static boolean accepts(String remote) {
        if (remote == null || remote.isBlank()) {
            return true;
        }
        if (!PROTOCOL_VERSION.equals(remote)) {
            UltraUiBridge.LOGGER.warn(
                    "[kaipath] LM gui channel protocol remote={} local={} — accepting anyway",
                    remote,
                    PROTOCOL_VERSION);
        }
        return true;
    }

    public static void registerClient() {
        if (channel != null) {
            return;
        }
        channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(CHANNEL_NAMESPACE, CHANNEL_PATH),
                () -> PROTOCOL_VERSION,
                LmGuiNetwork::accepts,
                LmGuiNetwork::accepts);
        channel.messageBuilder(GuiOpenPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GuiOpenPacket::encode)
                .decoder(GuiOpenPacket::decode)
                .consumerMainThread(GuiOpenPacket::handle)
                .add();
        channel.messageBuilder(GuiActionPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GuiActionPacket::encode)
                .decoder(GuiActionPacket::decode)
                .consumerMainThread(GuiActionPacket::handleClientNoop)
                .add();
        UltraUiBridge.LOGGER.info(
                "[kaipath] LM Ultra GUI channel {}:{} protocol={}",
                CHANNEL_NAMESPACE,
                CHANNEL_PATH,
                PROTOCOL_VERSION);
    }

    /**
     * Send a click. Echoes {@code screenId} / {@code actionId} / {@code argsJson} verbatim —
     * no client-side filtering.
     */
    public static void sendAction(String screenId, String actionId, String argsJson) {
        if (channel == null) {
            UltraUiBridge.LOGGER.warn(
                    "[kaipath] sendAction dropped: channel null screenId={} actionId={}",
                    screenId,
                    actionId);
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc == null || mc.m_91403_() == null) {
            UltraUiBridge.LOGGER.warn(
                    "[kaipath] sendAction dropped: no connection screenId={} actionId={}",
                    screenId,
                    actionId);
            return;
        }
        if (!channel.isRemotePresent(mc.m_91403_().m_104910_())) {
            UltraUiBridge.LOGGER.warn(
                    "[kaipath] sendAction dropped: channel not present on server screenId={} actionId={}",
                    screenId,
                    actionId);
            return;
        }
        UltraUiBridge.LOGGER.info(
                "[kaipath] GuiActionPacket → screenId={} actionId={}",
                screenId,
                actionId);
        channel.sendToServer(new GuiActionPacket(screenId, actionId, argsJson));
    }
}
