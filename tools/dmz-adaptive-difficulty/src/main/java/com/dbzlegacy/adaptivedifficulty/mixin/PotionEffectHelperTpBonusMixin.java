package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.server.util.PotionEffectHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * DragonMineZ turns a TP gain effect level into {@code 1 + (level + 1) × 0.25}.
 * {@code getBonusFromAmplifier} returns 0 when the level is negative, so the
 * effect multiplier stays {@code 1} and the character-stats Effect line is hidden.
 * <p>
 * Continue the same 0.25 step below zero, on the public method both the TP grant
 * and that tooltip call. Level {@code -3} is a bonus of {@code -0.5}, so the
 * effect multiplier is {@code 0.5}. Other effects (ki, stamina, mastery) are left
 * alone. This does not cut the granted amount itself.
 */
@Mixin(value = PotionEffectHelper.class, remap = false)
public abstract class PotionEffectHelperTpBonusMixin {

    @Inject(method = "getMultiplierFromEffect", at = @At("RETURN"), cancellable = true, remap = false)
    private static void lm$negativeTpGainMultiplier(
            LivingEntity entity,
            MobEffect effect,
            String id,
            CallbackInfoReturnable<Double> cir) {
        if (entity == null || effect == null || !lm$isTpGain(effect, id)) {
            return;
        }
        MobEffectInstance current = entity.m_21124_(effect);
        if (current == null) {
            return;
        }
        int amplifier = current.m_19564_();
        if (amplifier >= 0) {
            return;
        }
        cir.setReturnValue(1.0d + (amplifier + 1) * 0.25d);
    }

    private static boolean lm$isTpGain(MobEffect effect, String id) {
        if ("tp_gain".equals(id)) {
            return true;
        }
        try {
            MobEffect tp = MainEffects.TP_GAIN.get();
            return tp != null && tp == effect;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
