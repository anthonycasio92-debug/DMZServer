package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import com.dmzrevamp.revamp.classes.DmzClassConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stamina and ki both read {@code getStatScaling}. dmzrevamp adds the fighting-class
 * file onto the race baseline, then multiplies that sum by Overhaul prestige.
 * This hook removes the class share, so a class change leaves those pools on the
 * race baseline. It does not write {@code live / scale}.
 * Other stats keep the dmzrevamp return. Priority 6100 runs after fusion.
 */
@Mixin(value = StatsData.class, remap = false, priority = 6100)
public abstract class StatsDataStatScalingMixin {

    @Inject(method = "getStatScaling", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$keepConfigStatScaling(String stat, CallbackInfoReturnable<Double> cir) {
        if (!pool(stat)) {
            return;
        }
        Double classScale = lm$classPoolScale(stat);
        if (classScale == null || !Double.isFinite(classScale)) {
            return;
        }
        Double boxed = cir.getReturnValue();
        if (boxed == null) {
            return;
        }
        double live = boxed;
        double prestige = LmOverhaulPrestigeIntegration.combatScaleMultiplier((StatsData) (Object) this);
        if (!Double.isFinite(live) || !Double.isFinite(prestige) || prestige <= 0.0d) {
            return;
        }
        double withoutClass = live - classScale * prestige;
        if (Double.isFinite(withoutClass) && withoutClass > 0.0d) {
            cir.setReturnValue(withoutClass);
        }
    }

    @Inject(method = "getInitialBaseStats", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$keepLiveBaseStats(CallbackInfoReturnable<RaceStatsConfig.BaseStats> cir) {
    }

    /** Fighting-class file coefficient for stamina or ki. */
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
}
