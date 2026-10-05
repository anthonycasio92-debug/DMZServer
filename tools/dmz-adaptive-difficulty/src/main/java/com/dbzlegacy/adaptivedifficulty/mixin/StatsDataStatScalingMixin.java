package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * DragonMineZ and dmzrevamp own fighting-class stat scaling for every stat,
 * fused or unfused. This mixin leaves {@code getStatScaling} and
 * {@code getInitialBaseStats} on that return. It does not write
 * {@code live / scale}.
 */
@Mixin(value = StatsData.class, remap = false, priority = 6100)
public abstract class StatsDataStatScalingMixin {

    @Inject(method = "getStatScaling", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$keepConfigStatScaling(String stat, CallbackInfoReturnable<Double> cir) {
    }

    @Inject(method = "getInitialBaseStats", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$keepLiveBaseStats(CallbackInfoReturnable<RaceStatsConfig.BaseStats> cir) {
    }
}
