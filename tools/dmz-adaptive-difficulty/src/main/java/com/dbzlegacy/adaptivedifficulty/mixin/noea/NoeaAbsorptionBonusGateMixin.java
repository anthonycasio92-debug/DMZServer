package com.dbzlegacy.adaptivedifficulty.mixin.noea;

import com.butterjaffa.noeabosses.MajinAbsorptionService;
import com.butterjaffa.noeabosses.V090Data;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Noea adds {@code absorptionMelee} / {@code absorptionKi} on every Majin
 * damage read. Subtract that stored bonus unless absorption is selected.
 * Subtraction lands on the unbuffed value whether this injector runs before
 * or after Noea's. A missing Noea method is skipped ({@code require = 0}).
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class NoeaAbsorptionBonusGateMixin {

    @Inject(method = {"getMeleeDamage", "getMaxMeleeDamage"},
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0)
    private void lm$gateAbsorptionMelee(CallbackInfoReturnable<Double> cir) {
        lm$stripInactiveBonus(cir, true);
    }

    @Inject(method = "getKiDamage",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0)
    private void lm$gateAbsorptionKi(CallbackInfoReturnable<Double> cir) {
        lm$stripInactiveBonus(cir, false);
    }

    private void lm$stripInactiveBonus(CallbackInfoReturnable<Double> cir, boolean melee) {
        try {
            StatsData stats = (StatsData) (Object) this;
            Player player = stats.getPlayer();
            if (player == null || MajinAbsorptionService.isActive(player)) {
                return;
            }
            // Noea only adds the bonus for a Majin. Leave everyone else alone.
            if (!MajinAbsorptionService.isMajin(stats)) {
                return;
            }
            V090Data data = MajinAbsorptionService.data(player);
            if (data == null) {
                return;
            }
            double bonus = melee ? data.absorptionMelee : data.absorptionKi;
            if (!Double.isFinite(bonus) || bonus <= 0d) {
                return;
            }
            double current = cir.getReturnValueD();
            if (!Double.isFinite(current)) {
                return;
            }
            cir.setReturnValue(Math.max(0d, current - bonus));
        } catch (Throwable ignored) {
        }
    }
}
