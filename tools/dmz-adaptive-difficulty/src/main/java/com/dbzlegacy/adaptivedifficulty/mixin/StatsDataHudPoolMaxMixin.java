package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * XenoverseHUD / AlternativeHUD call {@code getMaxEnergy}/{@code getMaxStamina} locally.
 * {@code ResourceSyncS2C} only sends current. Native getters omit Overhaul
 * {@code scaleMultiplier} (ENE/STM stay out of {@code getTotalMultiplier}), so a
 * prestige-scaled current (18k) paints as 300% of an unscaled 6k bar.
 *
 * <p>Apply the same prestige-aware cap {@link DmzResourcePoolClamp#actualMaxEnergy}
 * uses. Skip while the clamp is reading the native getter so scale is applied once.
 */
@Mixin(value = StatsData.class, remap = false, priority = 2100)
public abstract class StatsDataHudPoolMaxMixin {

    @Inject(method = "getMaxEnergy", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$prestigeAwareMaxEnergy(CallbackInfoReturnable<Float> cir) {
        applyPrestigeAwareMax(cir);
    }

    @Inject(method = "getMaxStamina", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$prestigeAwareMaxStamina(CallbackInfoReturnable<Float> cir) {
        applyPrestigeAwareMax(cir);
    }

    private void applyPrestigeAwareMax(CallbackInfoReturnable<Float> cir) {
        if (DmzResourcePoolClamp.isReadingNativeMax()) {
            return;
        }
        Float value = cir.getReturnValue();
        if (value == null || !Float.isFinite(value) || value <= 1f) {
            return;
        }
        float scaled = DmzResourcePoolClamp.applyOverhaulScale((StatsData) (Object) this, value);
        if (scaled > value + 0.01f) {
            cir.setReturnValue(scaled);
        }
    }
}
