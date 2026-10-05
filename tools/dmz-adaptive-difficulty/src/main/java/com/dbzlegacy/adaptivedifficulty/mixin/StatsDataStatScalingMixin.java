package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fighting-class stats and stamina both read {@code getStatScaling}.
 * dmzrevamp's {@code FusionRevampLogic.addPartnerScale} already multiplies that
 * return. Dividing it back ({@code live / scale}) does not stick: an earlier
 * build ran first and fusion multiplied the divided value again.
 *
 * <p>Ki was the same mistake. LegacyMechanics was multiplying {@code getMaxEnergy}
 * on top of the value DragonMineZ and the other mods had already computed, so
 * the ki mixin is empty and unregistered. Class coefficients and stamina stay
 * on that live return too. This mixin stays registered at priority 6100 and
 * does not replace {@code getStatScaling} or {@code getInitialBaseStats}.
 */
@Mixin(value = StatsData.class, remap = false, priority = 6100)
public abstract class StatsDataStatScalingMixin {

    @Inject(method = "getStatScaling", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$keepLiveStatScaling(String stat, CallbackInfoReturnable<Double> cir) {
    }

    @Inject(method = "getInitialBaseStats", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$keepLiveBaseStats(CallbackInfoReturnable<RaceStatsConfig.BaseStats> cir) {
    }
}
