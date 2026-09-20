package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulCapMath;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import com.dbzlegacy.adaptivedifficulty.progression.LmPrestigeResourceScale;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * LM owns personal level cap + max stat total (breakthrough 100k–150k). When
 * {@link com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration} is active,
 * Overhaul prestige UI/scaling/rebirth stay enabled; only caps are redirected here.
 */
@Mixin(targets = "com.dmzrevamp.revamp.prestige.PrestigeSystem", remap = false)
public abstract class DmzRevampPrestigeCapMixin {

    @Inject(method = "levelCap", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lm$levelCap(StatsData data, CallbackInfoReturnable<Integer> cir) {
        if (!lmOwnsPrestige() || data == null) {
            return;
        }
        int cap = LmOverhaulCapMath.personalLevelCap(data);
        if (cap > 0) {
            cir.setReturnValue(cap);
        }
    }

    @Inject(method = "maxAssignableTotal", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lm$maxAssignableTotal(StatsData data, CallbackInfoReturnable<Integer> cir) {
        if (!lmOwnsPrestige() || data == null) {
            return;
        }
        cir.setReturnValue(LmOverhaulCapMath.maxAssignableTotal(data));
    }

    @Inject(method = "canPrestige", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lm$blockOverhaulRebirth(StatsData data, CallbackInfoReturnable<Boolean> cir) {
        int count = 0;
        try {
            count = com.dbzlegacy.adaptivedifficulty.progression.bridge.DmzRevampPrestigeBridge
                    .overhaulCount(data);
        } catch (Throwable ignored) {
        }
        if (count >= LmOverhaulPrestigeIntegration.OVERHAUL_MAX_PRESTIGE) {
            cir.setReturnValue(false);
            return;
        }
        if (lmOwnsPrestige() && !LmOverhaulPrestigeIntegration.integrationActive()) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "setCount", at = @At("HEAD"), argsOnly = true, remap = false)
    private static int lm$capOverhaulPrestigeCount(int count) {
        return Math.max(0, Math.min(LmOverhaulPrestigeIntegration.OVERHAUL_MAX_PRESTIGE, count));
    }

    /** Fallback pool scale only when Overhaul prestige is disabled in JSON. */
    @Inject(method = "scaleMultiplier", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lm$prestigeScaleMultiplier(StatsData data, CallbackInfoReturnable<Double> cir) {
        if (!lmOwnsPrestige() || data == null || LmOverhaulPrestigeIntegration.overhaulPrestigeEnabled()) {
            return;
        }
        double mult = LmPrestigeResourceScale.multiplier(data);
        if (mult > 1.000_001) {
            cir.setReturnValue(mult);
        }
    }

    private static boolean lmOwnsPrestige() {
        try {
            return DifficultyConfig.get().enablePrestigeSystem;
        } catch (Throwable t) {
            return false;
        }
    }
}
