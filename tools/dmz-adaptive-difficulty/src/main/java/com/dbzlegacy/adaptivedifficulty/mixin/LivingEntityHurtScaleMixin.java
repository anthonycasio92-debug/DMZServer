package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Apply cached mob difficulty to outgoing damage when attributes alone are insufficient.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHurtScaleMixin {

    @ModifyVariable(
            method = "m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private float dmzad$scaleOutgoing(float amount, DamageSource source) {
        if (amount <= 0.0f || source == null) {
            return amount;
        }
        if (!(source.m_7639_() instanceof LivingEntity attacker)) {
            return amount;
        }
        if (attacker instanceof Player) {
            return amount;
        }
        float mult = MobScaling.outgoingDamageMultiplier(attacker);
        if (mult <= 1.0f) {
            return amount;
        }
        return amount * mult;
    }
}
