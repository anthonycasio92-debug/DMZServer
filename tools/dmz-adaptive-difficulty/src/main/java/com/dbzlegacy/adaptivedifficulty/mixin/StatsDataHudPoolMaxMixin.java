package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dbzlegacy.adaptivedifficulty.progression.StatsDataLoadContext;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * XenoverseHUD / AlternativeHUD call {@code getMaxEnergy}/{@code getMaxStamina} locally.
 * {@code ResourceSyncS2C} only sends current. Mohist / Iron can disagree with the HUD
 * formula; {@link DmzResourcePoolClamp} picks one canonical max (native Overhaul-scaled
 * {@code getMax*} when valid). LM does not apply {@code scaleMultiplier} again.
 *
 * <p>Replace DMZ return with {@link DmzResourcePoolClamp#actualMaxEnergy} /
 * {@link DmzResourcePoolClamp#actualMaxStamina} so HUD, clamps, and Fabled share one cap.
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
        if (StatsDataLoadContext.inLoad() || DmzResourcePoolClamp.isReadingNativeMax()) {
            return;
        }
        StatsData data = (StatsData) (Object) this;
        float canon = energy ? DmzResourcePoolClamp.actualMaxEnergy(data)
                : DmzResourcePoolClamp.actualMaxStamina(data);
        if (!Float.isFinite(canon) || canon <= 1f) {
            return;
        }
        Float value = cir.getReturnValue();
        if (value == null || !Float.isFinite(value) || canon > value + 0.01f) {
            cir.setReturnValue(canon);
        }
    }
}
