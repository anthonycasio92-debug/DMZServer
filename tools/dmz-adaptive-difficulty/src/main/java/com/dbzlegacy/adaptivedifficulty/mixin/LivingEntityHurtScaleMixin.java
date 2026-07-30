package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Optional backup for outgoing damage scaling.
 * Primary path is Forge {@code LivingHurtEvent} + ATTACK_DAMAGE attribute
 * (see {@link MobScaling#scaleOutgoingHurt}) — more reliable on Mohist.
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
        // Disabled: LivingHurtEvent handles this. Kept so older mixin configs don't crash.
        // Returning amount unchanged avoids double-scaling with the Forge event.
        return amount;
    }
}
