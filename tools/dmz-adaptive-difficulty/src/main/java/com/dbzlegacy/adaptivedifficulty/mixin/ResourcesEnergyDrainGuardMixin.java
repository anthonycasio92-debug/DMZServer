package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dragonminez.common.stats.character.Resources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ignores non-positive {@code removeEnergy} calls so a zero or negative drain
 * cannot wipe the ki pool.
 */
@Mixin(value = Resources.class, remap = false)
public abstract class ResourcesEnergyDrainGuardMixin {

    @Inject(method = "removeEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private void lm$ignoreNonPositiveEnergyDrain(float amount, CallbackInfo ci) {
        if (amount <= 0f) {
            ci.cancel();
        }
    }
}
