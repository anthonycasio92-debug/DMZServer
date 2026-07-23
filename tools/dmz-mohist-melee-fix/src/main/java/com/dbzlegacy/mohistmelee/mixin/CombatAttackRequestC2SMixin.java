package com.dbzlegacy.mohistmelee.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * DragonMineZ applies melee inside {@code CombatAttackRequestC2S} via
 * {@code ServerPlayer.attack} ({@code m_5706_}). On Mohist that path often
 * deals no damage until the player entity is recreated by death.
 *
 * Redirect living targets to {@code LivingEntity.hurt} ({@code m_6469_}) with a
 * normal player-attack DamageSource. DMZ's {@code CombatEvent#onLivingHurt}
 * still rewrites the amount to {@code getMeleeDamage()}.
 *
 * The invoke lives in {@code lambda$processAttackRequest$2}.
 */
@Mixin(targets = "com.dragonminez.common.network.C2S.CombatAttackRequestC2S", remap = false)
public abstract class CombatAttackRequestC2SMixin {

    @Redirect(
            method = "lambda$processAttackRequest$2",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;m_5706_(Lnet/minecraft/world/entity/Entity;)V",
                    remap = true
            ),
            remap = false,
            require = 0
    )
    private static void dbzlegacy$useHurtInsteadOfAttack(ServerPlayer player, Entity target) {
        if (target instanceof LivingEntity living) {
            // Entity.invulnerableTime / LivingEntity.hurtTime (SRG)
            living.f_19802_ = 0;
            living.f_20916_ = 0;
            // damageSources().playerAttack(player) → hurt(...)
            living.m_6469_(player.m_269291_().m_269075_(player), 1.0F);
            return;
        }
        player.m_5706_(target);
    }
}
