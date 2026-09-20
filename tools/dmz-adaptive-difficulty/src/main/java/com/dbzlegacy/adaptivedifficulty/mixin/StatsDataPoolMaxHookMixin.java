package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps stored pools ≤ live max when {@link StatsData#getMaxEnergy()} / {@link StatsData#getMaxStamina()}
 * shrink (prestige / form / hex curve). LM recovery used to push current ~4/3× max.
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataPoolMaxHookMixin {
    private static final float EPS = 0.25f;

    @Inject(method = "getMaxEnergy", at = @At("RETURN"), remap = false)
    private void lm$pullEnergyDownToMax(CallbackInfoReturnable<Float> cir) {
        lm$pullDown(cir, true);
    }

    @Inject(method = "getMaxStamina", at = @At("RETURN"), remap = false)
    private void lm$pullStaminaDownToMax(CallbackInfoReturnable<Float> cir) {
        lm$pullDown(cir, false);
    }

    private void lm$pullDown(CallbackInfoReturnable<Float> cir, boolean energy) {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return;
        }
        Float max = cir.getReturnValue();
        if (max == null || !Float.isFinite(max) || max <= 0f) {
            return;
        }
        StatsData self = (StatsData) (Object) this;
        Resources res = self.getResources();
        if (res == null) {
            return;
        }
        float cur = energy ? res.getCurrentEnergy() : res.getCurrentStamina();
        if (!Float.isFinite(cur) || cur <= max + EPS) {
            return;
        }
        if (energy) {
            res.setCurrentEnergy(max);
        } else {
            res.setCurrentStamina(max);
        }
    }
}
