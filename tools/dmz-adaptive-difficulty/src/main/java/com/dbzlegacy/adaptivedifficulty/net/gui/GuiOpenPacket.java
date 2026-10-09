package com.dbzlegacy.adaptivedifficulty.net.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Server → client. Asks the staff client to open one native screen. */
public final class GuiOpenPacket {
    private final String screenId;

    public GuiOpenPacket(String screenId) {
        this.screenId = screenId == null || screenId.isBlank() ? "hub" : screenId;
    }

    public String screenId() {
        return screenId;
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
        context.enqueueWork(() -> deliver(screenId));
        context.setPacketHandled(true);
    }

    /**
     * The client screen classes are loaded by name. The dedicated server never
     * resolves them, and this class has no compile-time reference to them.
     */
    private static void deliver(String screenId) {
        if (AdaptiveDifficultyMod.dedicatedServerForGui()) {
            return;
        }
        try {
            Class.forName("com.dbzlegacy.adaptivedifficulty.client.gui.LmScreens")
                    .getMethod("open", String.class)
                    .invoke(null, screenId);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] native GUI open failed for {}: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    screenId,
                    t.toString());
        }
    }

    static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
