package com.dmzlegacy.kaipath.lmbridge.net;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client → server. A hub button press. Server LegacyMechanics owns the handler
 * ({@code NativeGuiActions}); the client only encodes/sends.
 */
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

    /** Client never handles C2S; required by SimpleChannel registration. */
    public static void handleClientNoop(GuiActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
    }
}
