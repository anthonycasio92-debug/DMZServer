package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client → server button/tab press. Echo button {@code id} / {@code args} verbatim —
 * never validate or rewrite action ids on the client.
 */
public final class GuiActionPacket {
    /** Match LM {@code GuiActionPacket.ACTION_ID_MAX}. */
    private static final int ACTION_ID_MAX = 192;

    private final String screenId;
    private final String actionId;
    private final String argsJson;

    public GuiActionPacket(String screenId, String actionId, String argsJson) {
        this.screenId = GuiOpenPacket.trim(screenId, 64);
        this.actionId = GuiOpenPacket.trim(actionId, ACTION_ID_MAX);
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

    public static void handleClientNoop(GuiActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
    }
}
