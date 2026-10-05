package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dragonminez.common.stats.character.Resources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Registered so the death-penalty hook stays on {@code addTrainingPoints}.
 * The penalty is the TP gain effect. This method does not change the amount.
 */
@Mixin(value = Resources.class, remap = false, priority = 6200)
public abstract class ResourcesDeathTpPenaltyMixin {

    @ModifyVariable(
            method = "addTrainingPoints(FZ)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private float lm$leaveTrainingPoints(float amount) {
        return amount;
    }
}
