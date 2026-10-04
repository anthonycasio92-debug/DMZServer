package com.dbzlegacy.mohistmelee.mixin;

import com.butterjaffa.noeabosses.GrabAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Belt-and-suspenders: block direct {@link com.butterjaffa.noeabosses.GrabService#request} calls. */
@Mixin(targets = "com.butterjaffa.noeabosses.GrabService", remap = false)
public abstract class NoeaGrabServiceRequestMixin {

    @Inject(
            method = "request",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void dbzlegacy$blockGrabRequest(
            ServerPlayer player,
            GrabAction action,
            int targetEntityId,
            Vec3 origin,
            Vec3 direction,
            int sequence,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}
