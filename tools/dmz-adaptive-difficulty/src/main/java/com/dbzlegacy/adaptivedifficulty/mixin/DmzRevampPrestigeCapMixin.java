package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulCapMath;
import com.dbzlegacy.adaptivedifficulty.progression.LmPrestigeResourceScale;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Legacy Mechanics owns level caps and rebirth — not dmzrevamp Overhaul prestige ladder.
 * Overhaul still shows synced prestige count ({@link com.dbzlegacy.adaptivedifficulty.progression.bridge.DmzRevampPrestigeBridge})
 * but {@code levelCap} must follow LM breakthrough caps (100k–150k), not 50k / +40k per overhaul prestige.
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
        if (lmOwnsPrestige()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Overhaul {@code scaleMultiplier} is gated on {@code Prestige.enabled} in JSON (off on live).
     * LM applies the same math from lifetime completed prestiges for fusion/revamp hooks without
     * turning on the Overhaul prestige ladder or level-cap rules.
     */
    @Inject(method = "scaleMultiplier", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lm$prestigeScaleMultiplier(StatsData data, CallbackInfoReturnable<Double> cir) {
        if (!lmOwnsPrestige() || data == null) {
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
