package com.dbzlegacy.adaptivedifficulty.network;

import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** C2S: GUI button action. */
public final class DifficultyActionPacket {
    private final String action;
    private final long amount;
    private final String page;

    public DifficultyActionPacket(String action, long amount, String page) {
        this.action = action == null ? "" : action;
        this.amount = amount;
        this.page = page == null || page.isBlank() ? "main" : page;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.m_130070_(action);
        buf.writeLong(amount);
        buf.m_130070_(page);
    }

    public static DifficultyActionPacket decode(FriendlyByteBuf buf) {
        return new DifficultyActionPacket(buf.m_130277_(), buf.readLong(), buf.m_130277_());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            DifficultyActions.Result result = DifficultyActions.handle(player, action, amount, page);
            result.tell(player);
        });
        context.setPacketHandled(true);
    }
}
