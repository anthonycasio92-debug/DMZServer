package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Client → server. Hub button press; server LegacyMechanics owns the handler. */
public final class GuiActionPacket {
    private final String screenId;
    private final String actionId;
    private final String argsJson;

    public GuiActionPacket(String screenId, String actionId, String argsJson) {
        this.screenId = screenId == null ? "" : screenId;
        this.actionId = actionId == null ? "" : actionId;
        this.argsJson = argsJson == null ? "{}" : argsJson;
    }

    public static void encode(GuiActionPacket msg, FriendlyByteBuf buf) {
        buf.m_130070_(GuiOpenPacket.trim(msg.screenId, 64));
        buf.m_130070_(GuiOpenPacket.trim(msg.actionId, 64));
        buf.m_130070_(GuiOpenPacket.trim(msg.argsJson, 512));
    }

    public static GuiActionPacket decode(FriendlyByteBuf buf) {
        return new GuiActionPacket(buf.m_130277_(), buf.m_130277_(), buf.m_130277_());
    }

    public static void handleClientNoop(GuiActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
    }
}
