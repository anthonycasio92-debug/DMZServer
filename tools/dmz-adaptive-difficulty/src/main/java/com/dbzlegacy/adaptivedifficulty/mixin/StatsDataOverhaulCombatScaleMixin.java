package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Not registered. DragonMineZ and dmzrevamp already scale combat.
 * LegacyMechanics must not multiply {@code getTotalMultiplier} again.
 */
@Mixin(value = StatsData.class, remap = false, priority = 2100)
public abstract class StatsDataOverhaulCombatScaleMixin {
    @Inject(method = "getTotalMultiplier", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$overhaulCombatScale(String stat, CallbackInfoReturnable<Double> cir) {
    }
}
