package com.dbzlegacy.adaptivedifficulty.net.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraGuiActions;
import com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraStyle;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client → server. KaiPath button / tab press. Server runs the same logic as CNPC handlers.
 * No client-only classes are referenced here.
 */
public final class GuiActionPacket {
    /** Action ids are {@code @screen:do:…} — allow long saga / module ids. */
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

    public static void handle(GuiActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player == null || msg == null) {
                return;
            }
            try {
                UltraGuiActions.dispatch(player, msg.screenId, msg.actionId, msg.argsJson);
            } catch (Throwable t) {
                AdaptiveDifficultyMod.LOGGER.error(
                        "[{}] Ultra GuiActionPacket failed screenId={} rawActionId={} args={} player={}",
                        AdaptiveDifficultyMod.MOD_ID,
                        msg.screenId,
                        msg.actionId,
                        msg.argsJson,
                        player.m_7755_().getString(),
                        t);
                DmzRewards.msg(
                        player,
                        UltraStyle.dangerLine(
                                "Something went wrong. Try again, or ask staff if it keeps happening."));
            }
        });
        context.setPacketHandled(true);
    }
}
