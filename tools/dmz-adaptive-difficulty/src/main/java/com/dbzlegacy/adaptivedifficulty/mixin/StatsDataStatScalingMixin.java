package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Status;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fighting-class coefficients and stamina both read {@code getStatScaling}.
 * dmzrevamp's fusion hook multiplies that return by Overhaul prestige
 * ({@code 1 + count × scaleBonusPerPrestige}) even when the player is not fused.
 * The class file and the race baseline are already summed before that multiply,
 * so strength, stamina, and the other class stats come out past the config.
 *
 * <p>Ki is the other case. {@code getMaxEnergy} reads {@code ENE}, and that
 * prestige multiply is the one scale the ki pool is supposed to keep. This
 * mixin leaves {@code ENE} on that live return. For every other stat it writes
 * {@code live / scale} back, which is the config coefficient. Priority 6100
 * runs after fusion's default priority, so that write is the one that sticks.
 * LegacyMechanics does not apply a scale of its own.
 * A fused player keeps the fusion return, because the partner term is not a
 * pure multiply.
 */
@Mixin(value = StatsData.class, remap = false, priority = 6100)
public abstract class StatsDataStatScalingMixin {

    @Inject(method = "getStatScaling", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$keepConfigStatScaling(String stat, CallbackInfoReturnable<Double> cir) {
        if (energy(stat) || fused()) {
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

    @Inject(method = "getInitialBaseStats", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$keepLiveBaseStats(CallbackInfoReturnable<RaceStatsConfig.BaseStats> cir) {
    }

    /** Ki pool. Prestige on {@code ENE} stays, the same way {@code getMaxEnergy} does. */
    private static boolean energy(String stat) {
        return stat != null && (stat.equalsIgnoreCase("ENE") || stat.equalsIgnoreCase("ENERGY"));
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
