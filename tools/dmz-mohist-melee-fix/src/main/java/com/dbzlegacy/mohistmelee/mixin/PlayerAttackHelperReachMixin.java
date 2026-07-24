package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.CombatRepair;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={PlayerAttackHelper.class}, remap=false)
public abstract class PlayerAttackHelperReachMixin {
    @Inject(method={"getEffectiveAttackRange"}, at={@At(value="RETURN")}, cancellable=true, remap=false)
    private static void dmzmmf$sanitizeReach(Player player, double weaponRange, CallbackInfoReturnable<Double> cir) {
        Double raw = (Double)cir.getReturnValue();
        double computed = raw == null ? Double.NaN : raw;
        double fixed = CombatRepair.sanitizeEffectiveRange(player, weaponRange, computed);
        if (!Double.isFinite(computed) || Math.abs(fixed - computed) > 1.0E-9) {
            cir.setReturnValue(fixed);
        }
    }
}

