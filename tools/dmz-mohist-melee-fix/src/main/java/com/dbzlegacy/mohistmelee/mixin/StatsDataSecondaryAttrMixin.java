package com.dbzlegacy.mohistmelee.mixin;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Read-side NaN guard for DMZ secondary attributes.
 * Does not mutate AttributeInstance (writing ki_damage/melee_damage bases wiped player bonuses).
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataSecondaryAttrMixin {

    @Inject(method = "getSecondaryAttributeValue", at = @At("RETURN"), cancellable = true, remap = false)
    private void dbzlegacy$finiteSecondaryValue(
            Attribute attribute,
            double fallback,
            CallbackInfoReturnable<Double> cir
    ) {
        Double value = cir.getReturnValue();
        if (value == null || !Double.isFinite(value)) {
            cir.setReturnValue(fallback);
        }
    }

    @Inject(method = "getSecondaryAttributeBaseValue", at = @At("RETURN"), cancellable = true, remap = false)
    private void dbzlegacy$finiteSecondaryBase(
            Attribute attribute,
            double fallback,
            CallbackInfoReturnable<Double> cir
    ) {
        Double value = cir.getReturnValue();
        if (value == null || !Double.isFinite(value)) {
            cir.setReturnValue(fallback);
        }
    }
}
