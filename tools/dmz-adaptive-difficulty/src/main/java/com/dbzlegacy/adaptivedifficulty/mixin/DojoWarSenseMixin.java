package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.sparring.DojoWarSense;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While a dojo war is active, Noea's tracking scan includes the other dojo.
 * Skipped when Noea's sense channel is not installed.
 */
@Mixin(targets = "com.butterjaffa.noeabosses.sense.SenseNetwork", remap = false, priority = 700)
public abstract class DojoWarSenseMixin {

    @Inject(method = "send", at = @At("HEAD"), remap = false, require = 0)
    private static void lm$addDojoWarSignatures(ServerPlayer player, CompoundTag packet, CallbackInfo ci) {
        DojoWarSense.addSignatures(player, packet);
    }
}
