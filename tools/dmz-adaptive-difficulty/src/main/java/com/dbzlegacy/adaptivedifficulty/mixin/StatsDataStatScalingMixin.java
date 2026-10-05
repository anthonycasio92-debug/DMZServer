package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import com.dmzrevamp.revamp.classes.DmzClassConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Status;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stamina and ki both read {@code getStatScaling} ({@code STM} and {@code ENE}).
 * dmzrevamp sums the race baseline with the class file, then its fusion hook
 * multiplies that sum by Overhaul prestige. Either step changes the pool with
 * the fighting class.
 *
 * <p>For those two stats this mixin writes the class file's own coefficient
 * from {@code DmzClassConfigManager}. It does not add the race baseline and it
 * does not multiply. Other stats still write {@code live / scale}, which is
 * the summed config coefficient. Priority 6100 runs after fusion.
 * A fused player keeps the fusion return on those other stats, because the
 * partner term is not a pure multiply.
 */
@Mixin(value = StatsData.class, remap = false, priority = 6100)
public abstract class StatsDataStatScalingMixin {

    @Inject(method = "getStatScaling", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$keepConfigStatScaling(String stat, CallbackInfoReturnable<Double> cir) {
        if (pool(stat)) {
            Double classScale = lm$classPoolScale(stat);
            if (classScale != null && Double.isFinite(classScale) && classScale > 0.0d) {
                cir.setReturnValue(classScale);
            }
            return;
        }
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

    @Inject(method = "getInitialBaseStats", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$keepLiveBaseStats(CallbackInfoReturnable<RaceStatsConfig.BaseStats> cir) {
    }

    /** Class-file stamina or ki coefficient. No race add and no prestige multiply. */
    private Double lm$classPoolScale(String stat) {
        try {
            Character character = ((StatsData) (Object) this).getCharacter();
            if (character == null) {
                return null;
            }
            String classId = character.getCharacterClass();
            if (classId == null || classId.isBlank()) {
                return null;
            }
            RaceStatsConfig.ClassStats classStats = DmzClassConfigManager.getConfiguredClassStats(classId);
            if (classStats == null || classStats.getStatScaling() == null) {
                return null;
            }
            RaceStatsConfig.StatScaling scaling = classStats.getStatScaling();
            if (stat.equalsIgnoreCase("STM") || stat.equalsIgnoreCase("STAMINA")) {
                return scaling.getStaminaScaling();
            }
            if (stat.equalsIgnoreCase("ENE") || stat.equalsIgnoreCase("ENERGY")) {
                return scaling.getEnergyScaling();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean pool(String stat) {
        return stat != null
                && (stat.equalsIgnoreCase("STM")
                        || stat.equalsIgnoreCase("STAMINA")
                        || stat.equalsIgnoreCase("ENE")
                        || stat.equalsIgnoreCase("ENERGY"));
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
