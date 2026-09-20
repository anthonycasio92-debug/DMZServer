package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import com.dbzlegacy.adaptivedifficulty.progression.LmPrestigeResourceScale;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies LM prestige pool scaling to ki/stamina caps only. Does not touch level cap,
 * max stat total, or Overhaul rebirth ({@link DmzRevampPrestigeCapMixin}).
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataPrestigePoolMixin {

    @Inject(method = "getMaxEnergy", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$prestigeScaleMaxEnergy(CallbackInfoReturnable<Float> cir) {
        if (LmOverhaulPrestigeIntegration.overhaulPrestigeEnabled()) {
            return;
        }
        Float base = cir.getReturnValue();
        if (base == null || base <= 0f) {
            return;
        }
        StatsData self = (StatsData) (Object) this;
        float scaled = LmPrestigeResourceScale.scaleMax(base, self);
        if (scaled > base + 0.01f) {
            cir.setReturnValue(scaled);
        }
    }

    @Inject(method = "getMaxStamina", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$prestigeScaleMaxStamina(CallbackInfoReturnable<Float> cir) {
        if (LmOverhaulPrestigeIntegration.overhaulPrestigeEnabled()) {
            return;
        }
        Float base = cir.getReturnValue();
        if (base == null || base <= 0f) {
            return;
        }
        StatsData self = (StatsData) (Object) this;
        float scaled = LmPrestigeResourceScale.scaleMax(base, self);
        if (scaled > base + 0.01f) {
            cir.setReturnValue(scaled);
        }
    }
}
