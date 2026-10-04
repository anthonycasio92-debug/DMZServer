package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * dmzrevamp 2.1.x does not apply {@code PrestigeSystem.scaleMultiplier} on
 * {@code getTotalMultiplier} (fusion mixins only add partner scale). DMZ combat getters use
 * totalMult, so LM applies Overhaul prestige scale here once via
 * {@link LmOverhaulPrestigeIntegration#combatScaleMultiplier} — never a local formula.
 * ENE/STM stay unscaled here; pools use {@link DmzResourcePoolClamp} (2.4.85).
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
