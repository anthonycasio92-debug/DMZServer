package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.ReachAttributeFix;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * If Forge ENTITY_REACH is NaN/collapsed, DMZ {@code getEffectiveAttackRange} returns
 * NaN/0 and server-side M1 range checks fail until death rebuilds attributes.
 */
@Mixin(value = PlayerAttackHelper.class, remap = false)
public abstract class PlayerAttackHelperReachMixin {

    @Inject(method = "getEffectiveAttackRange", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dbzlegacy$sanitizeReach(Player player, double weaponAttackRange, CallbackInfoReturnable<Double> cir) {
        Double value = cir.getReturnValue();
        double computed = value == null ? Double.NaN : value;
        double fixed = ReachAttributeFix.sanitizeEffectiveRange(player, weaponAttackRange, computed);
        if (!Double.isFinite(computed) || Math.abs(fixed - computed) > 1.0E-9D) {
            cir.setReturnValue(fixed);
        }
    }
}
