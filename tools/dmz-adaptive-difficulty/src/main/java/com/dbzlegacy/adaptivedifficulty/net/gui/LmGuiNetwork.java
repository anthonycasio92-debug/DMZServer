package com.dbzlegacy.adaptivedifficulty.net.gui;

import java.lang.reflect.Field;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Native Ultra GUI channel. Namespace stays {@code legacymechanics:gui} so the
 * server LegacyMechanics jar and the client LegacyMechanicsUltra jar match.
 * Client screen classes are never referenced from here.
 */
public final class LmGuiNetwork {
    public static final String CHANNEL_NAMESPACE = "legacymechanics";
    private static final String PROTOCOL = "1";
    private static final Logger LOGGER = LogManager.getLogger("LegacyMechanicsUltra");
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CHANNEL_NAMESPACE, "gui"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);
    private static boolean registered;
    private static Field connectionField;

    private LmGuiNetwork() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.messageBuilder(GuiOpenPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GuiOpenPacket::encode)
                .decoder(GuiOpenPacket::decode)
                .consumerMainThread(GuiOpenPacket::handle)
                .add();
        CHANNEL.messageBuilder(GuiActionPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GuiActionPacket::encode)
                .decoder(GuiActionPacket::decode)
                .consumerMainThread(GuiActionPacket::handle)
                .add();
    }

    public static boolean sendOpen(ServerPlayer player, String screenId) {
        if (player == null || !remoteHasChannel(player)) {
            return false;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new GuiOpenPacket(screenId));
        return true;
    }

    public static void sendAction(String screenId, String actionId, String argsJson) {
        CHANNEL.sendToServer(new GuiActionPacket(screenId, actionId, argsJson));
    }

    private static boolean remoteHasChannel(ServerPlayer player) {
        try {
            if (player.f_8906_ == null) {
                return false;
            }
            Connection connection = connection(player.f_8906_);
            return connection != null && CHANNEL.isRemotePresent(connection);
        } catch (Throwable t) {
            LOGGER.warn("[legacymechanics] ultra GUI channel check failed: {}", t.toString());
            return false;
        }
    }

    private static Connection connection(ServerGamePacketListenerImpl listener) throws Exception {
        if (connectionField == null) {
            connectionField = ServerGamePacketListenerImpl.class.getDeclaredField("f_9742_");
            connectionField.setAccessible(true);
        }
        return (Connection) connectionField.get(listener);
    }
}
