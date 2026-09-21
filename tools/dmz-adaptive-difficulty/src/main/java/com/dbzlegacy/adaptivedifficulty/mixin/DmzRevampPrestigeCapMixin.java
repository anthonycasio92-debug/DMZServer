package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulCapMath;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * LM owns personal playable max stat total (100k + 10k×breakthroughs, max 150k).
 * Overhaul {@code levelCap} follows that personal cap so prestige 0 is 100k
 * (not stock 50k) and held 1 + 0 breakthroughs stays 100k (not 150k).
 * {@code hexStatReference} follows the same cap. Playable totals stay
 * {@link com.dbzlegacy.adaptivedifficulty.mixin.StatsDataMixin} + this
 * {@code maxAssignableTotal} + TP/stat soft-locks.
 */
@Mixin(targets = "com.dmzrevamp.revamp.prestige.PrestigeSystem", remap = false)
public abstract class DmzRevampPrestigeCapMixin {

    @Inject(method = "levelCap", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lm$personalOverhaulLevelCap(StatsData data, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(LmOverhaulCapMath.overhaulLevelCap(data));
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
            return;
        }
        // Native Overhaul gates on stock 50k / pinned 150k. 0-breakthrough players
        // max at 100k and could never prestige. Use the personal cap instead.
        if (data == null) {
            return;
        }
        try {
            int cap = LmOverhaulCapMath.overhaulLevelCap(data);
            cir.setReturnValue(data.getLevel() >= cap);
        } catch (Throwable ignored) {
        }
    }

    @ModifyVariable(method = "setCount", at = @At("HEAD"), argsOnly = true, remap = false)
    private static int lm$capOverhaulPrestigeCount(int count) {
        return Math.max(0, Math.min(LmOverhaulPrestigeIntegration.OVERHAUL_MAX_PRESTIGE, count));
    }

    private static boolean lmOwnsPrestige() {
        try {
            return DifficultyConfig.get().enablePrestigeSystem;
        } catch (Throwable t) {
            return false;
        }
    }
}
