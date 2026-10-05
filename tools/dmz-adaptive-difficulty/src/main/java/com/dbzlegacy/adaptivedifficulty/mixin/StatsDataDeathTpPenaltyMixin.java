package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dragonminez.common.config.TpSource;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Registered so the death-penalty hook stays on {@code calculateTPGain}.
 * DragonMineZ already applies the TP gain effect inside that method.
 * This hook does not replace the result.
 */
@Mixin(value = StatsData.class, remap = false, priority = 6200)
public abstract class StatsDataDeathTpPenaltyMixin {

    @Inject(
            method = "calculateTPGain(ILcom/dragonminez/common/config/TpSource;)I",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void lm$leaveCalculatedTp(int amount, TpSource source, CallbackInfoReturnable<Integer> cir) {
    }
}
