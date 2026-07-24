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
 * Strike techniques call {@code LivingEntity.hurt}. Cancelled strike damage on Mohist
 * can leave the attacker {@code strikeLocked} while the ACTIVE strike ends uncleanly.
 * Route through {@link DamageBridge}; denied hits repair the attacker.
 */
@Mixin(targets = "com.dragonminez.server.events.players.combat.StrikeAttackHandler", remap = false)
public abstract class StrikeAttackHandlerMixin {

    @Redirect(
            method = "applyStrikeDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    remap = true
            ),
            remap = false,
            require = 0
    )
    private static boolean dbzlegacy$strikeHurtBypass(LivingEntity target, DamageSource source, float amount) {
        Entity direct = source.m_7639_();
        if (!(direct instanceof ServerPlayer player)) {
            return target.m_6469_(source, amount);
        }
        DamageBridge.Result result = DamageBridge.applyPlayerDamage(player, target, source, amount);
        if (result != DamageBridge.Result.APPLIED) {
            DamageBridge.repairAttacker(player, "strike-" + result.name());
            // Force-clear strike lock on deny so M1 cannot stick locked.
            DamageBridge.forceClearCombatLocks(player, "strike-deny");
        }
        return result == DamageBridge.Result.APPLIED;
    }
}
