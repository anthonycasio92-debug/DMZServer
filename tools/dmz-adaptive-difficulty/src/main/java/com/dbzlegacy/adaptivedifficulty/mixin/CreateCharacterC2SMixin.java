package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.character.RaceChangeCreationFlow;
import com.dbzlegacy.adaptivedifficulty.character.RaceChangeCreationPacketGuard;
import com.dragonminez.common.network.C2S.CreateCharacterC2S;
import com.dragonminez.common.stats.StatsData;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CreateCharacterC2S.class, remap = false)
public abstract class CreateCharacterC2SMixin {

    @Inject(method = "handle", at = @At("HEAD"), remap = false)
    private static void lm$guardRaceChangeCreatePacket(
            CreateCharacterC2S packet,
            Supplier<NetworkEvent.Context> ctxSupplier,
            CallbackInfo ci
    ) {
        try {
            NetworkEvent.Context ctx = ctxSupplier == null ? null : ctxSupplier.get();
            ServerPlayer player = ctx == null ? null : ctx.getSender();
            if (player == null) {
                return;
            }
            RaceChangeCreationPacketGuard.applyCreateCharacterPacket(packet, player);
        } catch (Throwable ignored) {
        }
    }

    /** After DMZ applies {@code CreateCharacterC2S} (enqueueWork), not at {@code handle} return. */
    @Inject(method = "lambda$handle$0", at = @At("RETURN"), remap = false)
    private static void lm$afterRaceChangeCreationApplied(
            CreateCharacterC2S packet,
            ServerPlayer player,
            StatsData data,
            CallbackInfo ci
    ) {
        try {
            if (player == null || !RaceChangeCreationFlow.isActive(player)) {
                return;
            }
            RaceChangeCreationFlow.onCharacterCreated(player);
        } catch (Throwable ignored) {
        }
    }
}
