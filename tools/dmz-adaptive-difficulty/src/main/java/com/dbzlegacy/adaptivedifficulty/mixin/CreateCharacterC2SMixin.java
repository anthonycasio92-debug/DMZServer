package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.character.RaceChangeCreationFlow;
import com.dragonminez.common.network.C2S.CreateCharacterC2S;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CreateCharacterC2S.class, remap = false)
public abstract class CreateCharacterC2SMixin {

    @Inject(method = "handle", at = @At("RETURN"), remap = false)
    private static void lm$afterRaceChangeCreation(
            CreateCharacterC2S packet,
            Supplier<NetworkEvent.Context> ctxSupplier,
            CallbackInfo ci
    ) {
        try {
            NetworkEvent.Context ctx = ctxSupplier == null ? null : ctxSupplier.get();
            ServerPlayer player = ctx == null ? null : ctx.getSender();
            if (player == null || !RaceChangeCreationFlow.isActive(player)) {
                return;
            }
            var server = player.m_20194_();
            Runnable work = () -> RaceChangeCreationFlow.onCharacterCreated(player);
            if (server != null) {
                server.execute(work);
            } else {
                work.run();
            }
        } catch (Throwable ignored) {
        }
    }
}
