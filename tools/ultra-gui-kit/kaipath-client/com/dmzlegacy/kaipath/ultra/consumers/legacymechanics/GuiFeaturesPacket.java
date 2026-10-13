package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client → server feature advertisement (message 2). Advertise the full set via
 * {@link com.dmzlegacy.kaipath.ultra.consumers.UltraGuiConsumerRegistry#ADVERTISED_FEATURES_JSON}
 * (includes {@code textInput} when field 11 widgets are present).
 */
public final class GuiFeaturesPacket {
    private final String featuresJson;

    public GuiFeaturesPacket(String featuresJson) {
        this.featuresJson = GuiOpenPacket.trim(featuresJson == null ? "[]" : featuresJson, 512);
    }

    public String featuresJson() {
        return featuresJson;
    }

    public static void encode(GuiFeaturesPacket msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.featuresJson == null || msg.featuresJson.isBlank() ? "[]" : msg.featuresJson);
    }

    public static GuiFeaturesPacket decode(FriendlyByteBuf buf) {
        String json = "[]";
        if (buf.isReadable()) {
            try {
                json = buf.m_130277_();
            } catch (Throwable ignored) {
                json = "[]";
            }
        }
        return new GuiFeaturesPacket(json);
    }

    public static void handleClientNoop(GuiFeaturesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
    }
}
