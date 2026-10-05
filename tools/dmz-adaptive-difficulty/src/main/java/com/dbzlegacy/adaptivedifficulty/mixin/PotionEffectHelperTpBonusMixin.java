package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dragonminez.server.util.PotionEffectHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * DragonMineZ turns a TP gain effect level into {@code 1 + (level + 1) × 0.25},
 * then adds that into the TP multiplier. A negative level is treated as no bonus,
 * so a death penalty on the same effect would do nothing.
 * <p>
 * Continue the same 0.25 step below zero. Level {@code -3} is a bonus of
 * {@code -0.5}, and the mod's own TP math applies it.
 */
@Mixin(value = PotionEffectHelper.class, remap = false)
public abstract class PotionEffectHelperTpBonusMixin {

    @Inject(method = "getBonusFromAmplifier", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void lm$negativeAmplifierBonus(int amplifier, CallbackInfoReturnable<Double> cir) {
        if (amplifier >= 0) {
            return;
        }
        cir.setReturnValue((amplifier + 1) * 0.25d);
    }
}
