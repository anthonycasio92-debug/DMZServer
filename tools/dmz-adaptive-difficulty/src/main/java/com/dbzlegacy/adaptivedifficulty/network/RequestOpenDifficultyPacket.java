package com.dbzlegacy.adaptivedifficulty.network;

import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** C2S: client asks server to open the Adaptive Difficulty GUI. */
public final class RequestOpenDifficultyPacket {
    private final String page;

    public RequestOpenDifficultyPacket() {
        this("main");
    }

    public RequestOpenDifficultyPacket(String page) {
        this.page = page == null || page.isBlank() ? "main" : page;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.m_130070_(page);
    }

    public static RequestOpenDifficultyPacket decode(FriendlyByteBuf buf) {
        return new RequestOpenDifficultyPacket(buf.m_130277_());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                DifficultyActions.openGui(player, page);
            }
        });
        context.setPacketHandled(true);
    }
}
