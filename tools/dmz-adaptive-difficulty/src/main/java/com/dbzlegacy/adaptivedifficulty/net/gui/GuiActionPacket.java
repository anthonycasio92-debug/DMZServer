package com.dbzlegacy.adaptivedifficulty.net.gui;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client → server. A native screen button asked the existing LM services to run. */
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

    public static void handle(GuiActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            NativeGuiActions.dispatch(player, msg.screenId, msg.actionId, msg.argsJson);
        });
        context.setPacketHandled(true);
    }
}
