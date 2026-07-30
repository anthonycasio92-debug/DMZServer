package com.dbzlegacy.adaptivedifficulty.network;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.currency.CurrencyBridge;
import com.dbzlegacy.adaptivedifficulty.team.TeamScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class DifficultyNet {
    private static final String PROTOCOL = "1";
    private static SimpleChannel channel;
    private static int packetId;

    private DifficultyNet() {}

    public static void register() {
        channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(AdaptiveDifficultyMod.MOD_ID, "main"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );
        packetId = 0;
        channel.registerMessage(
                packetId++,
                OpenDifficultyScreenPacket.class,
                OpenDifficultyScreenPacket::encode,
                OpenDifficultyScreenPacket::decode,
                OpenDifficultyScreenPacket::handle
        );
        channel.registerMessage(
                packetId++,
                RequestOpenDifficultyPacket.class,
                RequestOpenDifficultyPacket::encode,
                RequestOpenDifficultyPacket::decode,
                RequestOpenDifficultyPacket::handle
        );
        channel.registerMessage(
                packetId++,
                DifficultyActionPacket.class,
                DifficultyActionPacket::encode,
                DifficultyActionPacket::decode,
                DifficultyActionPacket::handle
        );
        AdaptiveDifficultyMod.LOGGER.info("[{}] network channel registered", AdaptiveDifficultyMod.MOD_ID);
    }

    public static void openScreen(ServerPlayer player, String page) {
        if (player == null || channel == null) {
            return;
        }
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        long buy100 = DifficultyCalculator.purchaseCost(snap.purchased, 100);
        long buy1k = DifficultyCalculator.purchaseCost(snap.purchased, 1_000);
        long buy10k = DifficultyCalculator.purchaseCost(snap.purchased, 10_000);
        OpenDifficultyScreenPacket pkt = new OpenDifficultyScreenPacket(
                page == null || page.isBlank() ? "main" : page,
                snap.active,
                snap.calculated,
                snap.purchased,
                snap.personalMax,
                snap.availableMax,
                snap.teamThresholdBonus,
                snap.teamContribution,
                snap.dmzLevel,
                snap.prestige,
                snap.teamMode.name(),
                TeamScaling.teamName(player),
                TeamScaling.teammates(player).size(),
                TeamScaling.teamSourceLabel(),
                DifficultyTier.of(snap.active).display,
                CurrencyBridge.currencyLabel(),
                CurrencyBridge.balanceText(player),
                CurrencyBridge.formatCost(buy100),
                CurrencyBridge.formatCost(buy1k),
                CurrencyBridge.formatCost(buy10k),
                buy100,
                buy1k,
                buy10k,
                snap.stateColorCode()
        );
        sendToPlayer(pkt, player);
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        channel.sendToServer(packet);
    }

    /** Unused helper kept for Predicate typing clarity in some Forge versions. */
    @SuppressWarnings("unused")
    private static Predicate<String> protoCheck(Supplier<String> expected) {
        return expected.get()::equals;
    }
}
