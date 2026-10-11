package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Client half of LegacyMechanics' Ultra GUI channel.
 * Wire format must match server LegacyMechanics: {@code legacymechanics:gui} / {@code "1"}.
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

    private LmGuiNetwork() {}

    public static void registerClient() {
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
                .consumerMainThread(GuiActionPacket::handleClientNoop)
                .add();
    }

    public static void sendAction(String screenId, String actionId, String argsJson) {
        CHANNEL.sendToServer(new GuiActionPacket(screenId, actionId, argsJson));
    }
}
