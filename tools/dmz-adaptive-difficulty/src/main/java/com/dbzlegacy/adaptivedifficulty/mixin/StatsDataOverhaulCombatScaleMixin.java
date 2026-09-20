package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overhaul {@code scaleMultiplier} is native for fusion + the Statistics "Scale Increase"
 * tooltip, but {@code getBaseStatFormula} / {@code getMeleeDamage} never multiply by it.
 * Apply that same prestige scale to combat {@code getTotalMultiplier} only — ENE/STM stay
 * unscaled so ki/stamina pools are not copied (2.4.85).
 */
@Mixin(value = StatsData.class, remap = false, priority = 2100)
public abstract class StatsDataOverhaulCombatScaleMixin {
    @Inject(method = "getTotalMultiplier", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$overhaulCombatScale(String stat, CallbackInfoReturnable<Double> cir) {
        if (!LmOverhaulPrestigeIntegration.integrationActive()) {
            return;
        }
        if (LmOverhaulPrestigeIntegration.isResourcePoolStat(stat)) {
            return;
        }
        double scale = LmOverhaulPrestigeIntegration.combatScaleMultiplier((StatsData) (Object) this);
        if (scale <= 1.000_001d) {
            return;
        }
        Double original = cir.getReturnValue();
        double base = original != null && Double.isFinite(original) ? original : 1.0d;
        if (base <= 0.0d) {
            base = 1.0d;
        }
        cir.setReturnValue(base * scale);
    }
}
