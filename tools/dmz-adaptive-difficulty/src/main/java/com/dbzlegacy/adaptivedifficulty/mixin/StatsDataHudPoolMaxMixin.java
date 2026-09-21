package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * XenoverseHUD / AlternativeHUD call {@code getMaxEnergy}/{@code getMaxStamina} locally.
 * {@code ResourceSyncS2C} only sends current. Replace the vanilla return with the same
 * canonical cap {@link DmzResourcePoolClamp#actualMaxEnergy} / {@link #actualMaxStamina}
 * uses (native read, Iron reject, HUD formula, Overhaul scale once).
 *
 * <p>Do not only {@link DmzResourcePoolClamp#applyOverhaulScale} on the vanilla return —
 * live 2.4.115 did that via raw {@code getMax*} in {@code actualMax*} and still allowed
 * overflow when native max and clamp max diverged. Skip while
 * {@link DmzResourcePoolClamp#isReadingNativeMax()} to avoid recursion.
 */
@Mixin(value = StatsData.class, remap = false, priority = 2100)
public abstract class StatsDataHudPoolMaxMixin {

    @Inject(method = "getMaxEnergy", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$prestigeAwareMaxEnergy(CallbackInfoReturnable<Float> cir) {
        applyCanonicalMax(cir, true);
    }

    @Inject(method = "getMaxStamina", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$prestigeAwareMaxStamina(CallbackInfoReturnable<Float> cir) {
        applyCanonicalMax(cir, false);
    }

    private void applyCanonicalMax(CallbackInfoReturnable<Float> cir, boolean energy) {
        if (DmzResourcePoolClamp.isReadingNativeMax()) {
            return;
        }
        StatsData self = (StatsData) (Object) this;
        float canonical = energy
                ? DmzResourcePoolClamp.actualMaxEnergy(self)
                : DmzResourcePoolClamp.actualMaxStamina(self);
        if (Float.isFinite(canonical) && canonical > 1f) {
            cir.setReturnValue(canonical);
        }
    }
}
