package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.ClassRaceStatScale;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Status;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fighting-class stats and stamina both read {@code getStatScaling}.
 * dmzrevamp's {@code FusionRevampLogic.addPartnerScale} multiplies that return by
 * Overhaul prestige for every player, fused or not. Dividing earlier
 * ({@code live / scale}) does not stick: this mixin used to run first and fusion
 * multiplied the divided value again.
 *
 * <p>Priority 6100 is applied after Overhaul's default 1000, so the value set here
 * is the one callers see. It is the live race baseline plus the fighting class
 * ({@link ClassRaceStatScale}). Fusion players are left to dmzrevamp, because the
 * partner term is not a pure multiply.
 */
@Mixin(value = StatsData.class, remap = false, priority = 6100)
public abstract class StatsDataStatScalingMixin {

    @Inject(method = "getStatScaling", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$keepConfigStatScaling(String stat, CallbackInfoReturnable<Double> cir) {
        if (fused()) {
            return;
        }
        double configScale = ClassRaceStatScale.scaling((StatsData) (Object) this, stat);
        if (Double.isFinite(configScale)) {
            cir.setReturnValue(configScale);
        }
    }

    @Inject(method = "getInitialBaseStats", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$keepConfigBaseStats(CallbackInfoReturnable<RaceStatsConfig.BaseStats> cir) {
        if (fused()) {
            return;
        }
        RaceStatsConfig.BaseStats base = ClassRaceStatScale.base((StatsData) (Object) this);
        if (base != null) {
            cir.setReturnValue(base);
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
