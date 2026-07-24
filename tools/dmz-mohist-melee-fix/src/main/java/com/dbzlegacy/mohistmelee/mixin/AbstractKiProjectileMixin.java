package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.DamageBridge;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Ki blasts call {@code LivingEntity.hurt}. On Mohist, a cancelled ki hit
 * (no-PvP / invulnerable target) can brick subsequent M1. Use {@link DamageBridge}
 * so denied hits repair the owner and allowed hits bypass the broken bridge.
 */
@Mixin(value = AbstractKiProjectile.class, remap = false)
public abstract class AbstractKiProjectileMixin {

    @Redirect(
            method = "applyDamageOrHeal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    remap = true
            ),
            remap = false,
            require = 0
    )
    private boolean dbzlegacy$kiHurtBypass(LivingEntity target, DamageSource source, float amount) {
        AbstractKiProjectile self = (AbstractKiProjectile) (Object) this;
        Entity owner = self.m_19749_();
        if (owner instanceof ServerPlayer player) {
            DamageBridge.Result result = DamageBridge.applyPlayerDamage(player, target, source, amount);
            if (result != DamageBridge.Result.APPLIED) {
                DamageBridge.repairAttacker(player, "ki-" + result.name());
            }
            return result == DamageBridge.Result.APPLIED;
        }
        // Non-player owners: keep vanilla hurt
        return target.m_6469_(source, amount);
    }
}
