package com.dbzlegacy.mohistmelee.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks {@code GrabService.request}. The target is named as a string, and this
 * handler does not mention Noea types, so loading the mixin does not load
 * {@code GrabService} before Mixin transforms it.
 */
@Mixin(targets = "com.butterjaffa.noeabosses.GrabService", remap = false)
public abstract class NoeaGrabServiceRequestMixin {

    @Inject(method = "request", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dbzlegacy$blockGrabRequest(CallbackInfo ci) {
        ci.cancel();
    }
}
