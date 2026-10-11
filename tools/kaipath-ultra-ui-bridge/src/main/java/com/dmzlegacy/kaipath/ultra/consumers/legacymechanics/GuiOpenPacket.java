package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Server → client. LegacyMechanics asks the client to open one Ultra screen. */
public final class GuiOpenPacket {
    private final String screenId;

    public GuiOpenPacket(String screenId) {
        this.screenId = screenId == null || screenId.isBlank() ? "hub" : screenId;
    }

    public static void encode(GuiOpenPacket msg, FriendlyByteBuf buf) {
        buf.m_130070_(trim(msg.screenId, 64));
    }

    public static GuiOpenPacket decode(FriendlyByteBuf buf) {
        return new GuiOpenPacket(buf.m_130277_());
    }

    public static void handle(GuiOpenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        String screenId = msg.screenId;
        context.enqueueWork(() -> LmScreens.open(screenId));
        context.setPacketHandled(true);
    }

    static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
