package com.dbzlegacy.mohistmelee.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {
    @Invoker("m_6475_")
    void dbzlegacy$invokeActuallyHurt(DamageSource source, float amount);
}
