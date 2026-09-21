package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * XenoverseHUD / AlternativeHUD call {@code getMaxEnergy}/{@code getMaxStamina} locally.
 * Replace the vanilla return with {@link DmzResourcePoolClamp#canonicalPoolMax} using that
 * same return — clamps and Fabled use {@link DmzResourcePoolClamp#actualMaxEnergy} which
 * reads native via {@link DmzResourcePoolClamp#isReadingNativeMax()} and runs the same math.
 *
 * <p>Do not call {@code actualMaxEnergy} here (re-enters {@code getMaxEnergy} and diverges).
 */
@Mixin(value = StatsData.class, remap = false, priority = 2100)
public abstract class StatsDataHudPoolMaxMixin {

    @Inject(method = "getMaxEnergy", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$prestigeAwareMaxEnergy(CallbackInfoReturnable<Float> cir) {
        applyCanonicalPoolMax(cir, true);
    }

    @Inject(method = "getMaxStamina", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$prestigeAwareMaxStamina(CallbackInfoReturnable<Float> cir) {
        applyCanonicalPoolMax(cir, false);
    }

    private void applyCanonicalPoolMax(CallbackInfoReturnable<Float> cir, boolean energy) {
        if (DmzResourcePoolClamp.isReadingNativeMax()) {
            return;
        }
        Float value = cir.getReturnValue();
        if (value == null || !Float.isFinite(value) || value <= 1f) {
            return;
        }
        StatsData self = (StatsData) (Object) this;
        float canonical = DmzResourcePoolClamp.canonicalPoolMax(self, value, energy);
        if (Float.isFinite(canonical) && canonical > 1f) {
            cir.setReturnValue(canonical);
        }
    }
}
