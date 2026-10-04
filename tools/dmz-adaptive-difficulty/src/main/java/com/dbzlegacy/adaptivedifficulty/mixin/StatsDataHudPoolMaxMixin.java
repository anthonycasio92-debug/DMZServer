package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Not registered. Ki and stamina maxima come from DragonMineZ and the other
 * installed mods. LegacyMechanics must not multiply {@code getMaxEnergy} or
 * {@code getMaxStamina}.
 */
@Mixin(value = StatsData.class, remap = false, priority = 2100)
public abstract class StatsDataHudPoolMaxMixin {

    @Inject(method = "getMaxEnergy", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$prestigeAwareMaxEnergy(CallbackInfoReturnable<Float> cir) {
    }

    @Inject(method = "getMaxStamina", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$prestigeAwareMaxStamina(CallbackInfoReturnable<Float> cir) {
    }
}
