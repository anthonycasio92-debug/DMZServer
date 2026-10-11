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

/**
 * Server half of {@code legacymechanics:gui} / protocol {@code "1"}.
 * KaiPath owns the client consumer. No client screen classes are referenced here.
 */
public final class LmGuiNetwork {
    public static final String CHANNEL_NAMESPACE = "legacymechanics";
    public static final String CHANNEL_PATH = "gui";
    public static final String PROTOCOL = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CHANNEL_NAMESPACE, CHANNEL_PATH),
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

    public static void sendOpen(ServerPlayer player, GuiOpenPacket packet) {
        if (player == null || packet == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** True when the player's connection lists the KaiPath mod id. */
    public static boolean clientHasKaiPath(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        try {
            Connection connection = connectionOf(player);
            if (connection == null) {
                return false;
            }
            Class<?> hooks = Class.forName("net.minecraftforge.network.NetworkHooks");
            Object data = hooks.getMethod("getConnectionData", Connection.class).invoke(null, connection);
            if (data == null) {
                return false;
            }
            Object modList = data.getClass().getMethod("getModList").invoke(data);
            if (!(modList instanceof Iterable<?> mods)) {
                return false;
            }
            for (Object mod : mods) {
                if (mod != null && "kaipath".equalsIgnoreCase(String.valueOf(mod))) {
                    return true;
                }
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Connection connectionOf(ServerPlayer player) {
        try {
            ServerGamePacketListenerImpl listener = player.f_8906_;
            if (listener == null) {
                return null;
            }
            if (connectionField == null) {
                for (Field field : ServerGamePacketListenerImpl.class.getDeclaredFields()) {
                    if (Connection.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        connectionField = field;
                        break;
                    }
                }
            }
            if (connectionField == null) {
                return null;
            }
            return (Connection) connectionField.get(listener);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
