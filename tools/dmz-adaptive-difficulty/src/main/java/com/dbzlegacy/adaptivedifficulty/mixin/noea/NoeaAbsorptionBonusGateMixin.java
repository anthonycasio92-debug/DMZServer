package com.dbzlegacy.adaptivedifficulty.mixin.noea;

import com.butterjaffa.noeabosses.MajinAbsorptionService;
import com.butterjaffa.noeabosses.V090Data;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Noea adds {@code absorptionMelee} / {@code absorptionKi} on every Majin
 * damage read. That add is the full stored number. This scales it by the
 * same power-release factor {@code getMeleeDamage} applies to the stat
 * block ({@code getPowerRelease() / 100}), or removes it when absorption
 * power is not above zero. The adjustment is {@code stored * (factor - 1)}
 * so it lands on the scaled bonus whether this injector runs before or
 * after Noea's. A missing Noea method is skipped ({@code require = 0}).
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class NoeaAbsorptionBonusGateMixin {

    @Inject(method = {"getMeleeDamage", "getMaxMeleeDamage"},
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0)
    private void lm$gateAbsorptionMelee(CallbackInfoReturnable<Double> cir) {
        lm$scaleAbsorptionBonus(cir, true);
    }

    @Inject(method = "getKiDamage",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0)
    private void lm$gateAbsorptionKi(CallbackInfoReturnable<Double> cir) {
        lm$scaleAbsorptionBonus(cir, false);
    }

    private void lm$scaleAbsorptionBonus(CallbackInfoReturnable<Double> cir, boolean melee) {
        try {
            StatsData stats = (StatsData) (Object) this;
            Player player = stats.getPlayer();
            if (player == null || !MajinAbsorptionService.isMajin(stats)) {
                return;
            }
            V090Data data = MajinAbsorptionService.data(player);
            if (data == null) {
                return;
            }
            double absorbed = melee ? data.absorptionMelee : data.absorptionKi;
            if (!Double.isFinite(absorbed) || absorbed <= 0d) {
                return;
            }
            double current = cir.getReturnValueD();
            if (!Double.isFinite(current)) {
                return;
            }
            double power = data.absorptionPower;
            double multiplier = Double.isFinite(power) && power > 0d
                    ? limitReleaseMultiplier(stats)
                    : 0d;
            double scaled = current + absorbed * (multiplier - 1.0d);
            cir.setReturnValue(Math.max(0d, scaled));
        } catch (Throwable ignored) {
        }
    }

    /** {@code 1.0} when release is unset. Otherwise the same {@code release / 100} factor as melee. */
    private static double limitReleaseMultiplier(StatsData stats) {
        try {
            Resources resources = stats.getResources();
            if (resources == null) {
                return 1.0d;
            }
            int release = resources.getPowerRelease();
            if (release <= 0) {
                release = resources.getRelease();
            }
            if (release <= 0) {
                return 1.0d;
            }
            double multiplier = release / 100.0d;
            return Double.isFinite(multiplier) && multiplier >= 0d ? multiplier : 1.0d;
        } catch (Throwable ignored) {
            return 1.0d;
        }
    }
}
