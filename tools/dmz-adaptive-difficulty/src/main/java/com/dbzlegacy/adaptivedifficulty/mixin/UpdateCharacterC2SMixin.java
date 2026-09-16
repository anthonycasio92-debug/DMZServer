package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.character.RaceHeadBoneSync;
import com.dbzlegacy.adaptivedifficulty.character.ReskinSessionGuard;
import com.dragonminez.common.network.C2S.UpdateCharacterC2S;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UpdateCharacterC2S.class, remap = false)
public abstract class UpdateCharacterC2SMixin {

    @Inject(method = "handle", at = @At("HEAD"), remap = false)
    private static void lm$reskinLockClassOnPacket(
            UpdateCharacterC2S packet,
            Supplier<NetworkEvent.Context> ctxSupplier,
            CallbackInfo ci
    ) {
        try {
            NetworkEvent.Context ctx = ctxSupplier == null ? null : ctxSupplier.get();
            ServerPlayer player = ctx == null ? null : ctx.getSender();
            if (player == null) {
                return;
            }
            ReskinSessionGuard.applyPacketClassLock(packet, player);
            RaceHeadBoneSync.applyPacketHeadBone(packet, player);
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "handle", at = @At("RETURN"), remap = false)
    private static void lm$reskinEnforceClassAfterPacket(
            UpdateCharacterC2S packet,
            Supplier<NetworkEvent.Context> ctxSupplier,
            CallbackInfo ci
    ) {
        try {
            NetworkEvent.Context ctx = ctxSupplier == null ? null : ctxSupplier.get();
            ServerPlayer player = ctx == null ? null : ctx.getSender();
            if (player == null) {
                return;
            }
            ReskinSessionGuard.enforceOnCharacter(player);
            if (RaceHeadBoneSync.syncCharacter(player)) {
                RaceHeadBoneSync.syncClient(player);
            }
        } catch (Throwable ignored) {
        }
    }
}
