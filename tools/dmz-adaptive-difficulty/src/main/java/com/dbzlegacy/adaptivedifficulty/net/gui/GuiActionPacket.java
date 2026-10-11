package com.dbzlegacy.adaptivedifficulty.net.gui;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Client → server. A native Ultra screen button asked LegacyMechanics to run. */
public final class GuiActionPacket {
    private static final Logger LOGGER = LogManager.getLogger("LegacyMechanicsUltra");
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
            if (player == null) {
                return;
            }
            // Loaded by name so the client companion jar can ship this packet
            // class without NativeGuiActions (server-only menu opens).
            try {
                Class.forName("com.dbzlegacy.adaptivedifficulty.net.gui.NativeGuiActions")
                        .getMethod("dispatch", ServerPlayer.class, String.class, String.class, String.class)
                        .invoke(null, player, msg.screenId, msg.actionId, msg.argsJson);
            } catch (Throwable t) {
                LOGGER.warn("[legacymechanics] ultra GUI action failed: {}", t.toString());
            }
        });
        context.setPacketHandled(true);
    }
}
