package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Status;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fighting-class stats and stamina both read {@code getStatScaling}. dmzrevamp
 * multiplies that result by Overhaul {@code scaleMultiplier}
 * ({@code 1 + prestige × scaleBonusPerPrestige}) on every stat, including
 * {@code STM}. Clients running DragonMineZ without that hook show the class
 * and race config values, so the server pools and class coefficients come out
 * scaled again.
 *
 * <p>Divide that prestige coefficient back out. The value that remains is the
 * config scaling. LegacyMechanics does not apply a scale of its own. Fusion
 * adds a partner term that is not a pure multiply, so fused players are left
 * to dmzrevamp.
 */
@Mixin(value = StatsData.class, remap = false, priority = 6100)
public abstract class StatsDataStatScalingMixin {

    @Inject(method = "getStatScaling", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$keepConfigStatScaling(String stat, CallbackInfoReturnable<Double> cir) {
        if (fused()) {
            return;
        }
        Double boxed = cir.getReturnValue();
        if (boxed == null) {
            return;
        }
        double live = boxed;
        double scale = LmOverhaulPrestigeIntegration.combatScaleMultiplier((StatsData) (Object) this);
        if (!Double.isFinite(live) || !Double.isFinite(scale) || scale <= 1.000_001d) {
            return;
        }
        double configScale = live / scale;
        if (Double.isFinite(configScale)) {
            cir.setReturnValue(configScale);
        }
    }

    private boolean fused() {
        try {
            Status status = ((StatsData) (Object) this).getStatus();
            return status != null && status.isFused();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
