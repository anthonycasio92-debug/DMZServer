package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Statistics Max Ki/Stamina call {@link StatsData#getMaxEnergy()} / {@code getMaxStamina()},
 * which add Forge secondary attributes the client HUD never sees. Strip that extra so the
 * panel matches the bar (and so 0% Limit Release isn't sitting under a 2.8M "max").
 *
 * <p>Does not write current pools — {@code setCurrentEnergy(≤1)} zeros power release.
 */
@Mixin(value = StatsData.class, remap = false, priority = 2100)
public abstract class StatsDataHudPoolMaxMixin {
    private static final ThreadLocal<Boolean> IN_HUD_CAP = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "getMaxEnergy", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$hudMaxEnergy(CallbackInfoReturnable<Float> cir) {
        applyHudCap(cir, true);
    }

    @Inject(method = "getMaxStamina", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$hudMaxStamina(CallbackInfoReturnable<Float> cir) {
        applyHudCap(cir, false);
    }

    private void applyHudCap(CallbackInfoReturnable<Float> cir, boolean energy) {
        if (Boolean.TRUE.equals(IN_HUD_CAP.get())) {
            return;
        }
        Float live = cir.getReturnValue();
        if (live == null || !Float.isFinite(live) || live <= 0f) {
            return;
        }
        IN_HUD_CAP.set(Boolean.TRUE);
        try {
            float hud = DmzResourcePoolClamp.toHudMax(live, (StatsData) (Object) this, energy);
            if (Float.isFinite(hud) && hud > 0f && hud + 0.5f < live) {
                cir.setReturnValue(hud);
            }
        } finally {
            IN_HUD_CAP.set(Boolean.FALSE);
        }
    }
}
