package com.dbzlegacy.adaptivedifficulty.net.gui;

import com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraGuiActions;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client → server. KaiPath button / tab press. Server runs the same logic as CNPC handlers.
 * No client-only classes are referenced here.
 */
public final class GuiActionPacket {
    private final String screenId;
    private final String actionId;
    private final String argsJson;

    public GuiActionPacket(String screenId, String actionId, String argsJson) {
        this.screenId = GuiOpenPacket.trim(screenId, 64);
        this.actionId = GuiOpenPacket.trim(actionId, 64);
        this.argsJson = GuiOpenPacket.trim(argsJson == null ? "{}" : argsJson, 512);
    }

    public String screenId() {
        return screenId;
    }

    public String actionId() {
        return actionId;
    }

    public String argsJson() {
        return argsJson;
    }

    public static void encode(GuiActionPacket msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.screenId);
        buf.m_130070_(msg.actionId);
        buf.m_130070_(msg.argsJson);
    }

    public static GuiActionPacket decode(FriendlyByteBuf buf) {
        return new GuiActionPacket(buf.m_130277_(), buf.m_130277_(), buf.m_130277_());
    }

    public static void handle(GuiActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player != null) {
                UltraGuiActions.dispatch(player, msg.screenId, msg.actionId, msg.argsJson);
            }
        });
        context.setPacketHandled(true);
    }
}
