package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.DamageBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Redirect DMZ melee {@code ServerPlayer.attack} to {@link DamageBridge}:
 * Bukkit/WorldGuard probe + Forge LivingHurt + setHealth, with attacker repair
 * after denied hits (no-PvP / cancelled damage that otherwise bricks M1 on Mohist).
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
            require = 1
    )
    private static void dbzlegacy$useHurtInsteadOfAttack(ServerPlayer player, Entity target) {
        if (!(target instanceof LivingEntity living)) {
            player.m_5706_(target);
            return;
        }
        DamageSource source = player.m_269291_().m_269075_(player);
        DamageBridge.applyPlayerDamage(player, living, source, 1.0F);
    }
}
