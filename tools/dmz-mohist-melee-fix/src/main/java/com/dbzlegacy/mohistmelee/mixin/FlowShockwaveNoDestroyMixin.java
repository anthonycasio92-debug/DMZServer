package com.dbzlegacy.mohistmelee.mixin;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No-ops Noea flow-state shockwave block destruction. The shockwave
 * visuals and knockback still play; terrain is left intact.
 *
 * <p>{@code destroyRing}'s second argument is the private {@code Wave}
 * type. {@link Coerce} is required so the injector matches that descriptor.
 */
@Mixin(targets = "com.butterjaffa.noeabosses.FlowShockwaveService", remap = false)
public abstract class FlowShockwaveNoDestroyMixin {

    @Inject(method = "destroyRing",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private static void dbzlegacy$noBlockDestruction(ServerLevel level, @Coerce Object wave,
                                                     double radius, int depth,
                                                     CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }
}
