package com.dbzlegacy.mohistmelee.mixin;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={StatsData.class}, remap=false)
public abstract class StatsDataSecondaryAttrMixin {
    @Inject(method={"getSecondaryAttributeValue"}, at={@At(value="RETURN")}, cancellable=true, remap=false)
    private void dmzmmf$finiteSecondaryValue(Attribute attribute, double dflt, CallbackInfoReturnable<Double> cir) {
        Double ret = (Double)cir.getReturnValue();
        if (ret == null || !Double.isFinite(ret)) {
            cir.setReturnValue(dflt);
        }
    }

    @Inject(method={"getSecondaryAttributeBaseValue"}, at={@At(value="RETURN")}, cancellable=true, remap=false)
    private void dmzmmf$finiteSecondaryBase(Attribute attribute, double dflt, CallbackInfoReturnable<Double> cir) {
        Double ret = (Double)cir.getReturnValue();
        if (ret == null || !Double.isFinite(ret)) {
            cir.setReturnValue(dflt);
        }
    }
}

