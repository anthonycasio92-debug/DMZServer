package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulCapMath;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides dmzrevamp {@code StatsDataLevelingRevampMixin} (priority 1000 HEAD).
 * Lower priority so our HEAD inject runs <b>after</b> Overhaul and last
 * {@code setReturnValue} wins: personal 100k + 10k×breakthroughs, never the
 * native prestige ladder (100k / 115k / 130k / 145k / 150k).
 */
@Mixin(value = StatsData.class, remap = false, priority = 400)
public abstract class StatsDataMixin {
    @Shadow(remap = false)
    public abstract Player getPlayer();

    @Inject(method = "getConfiguredMaxValue", at = @At("HEAD"), cancellable = true, remap = false)
    private void lm$personalMaxValueHead(CallbackInfoReturnable<Integer> cir) {
        applyPersonalMaxValue(cir);
    }

    @Inject(method = "getConfiguredMaxValue", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$personalMaxValue(CallbackInfoReturnable<Integer> cir) {
        applyPersonalMaxValue(cir);
    }

    @Inject(method = "getConfiguredMaxTotalStats", at = @At("HEAD"), cancellable = true, remap = false)
    private void lm$personalMaxTotalHead(CallbackInfoReturnable<Integer> cir) {
        applyPersonalMaxTotal(cir);
    }

    @Inject(method = "getConfiguredMaxTotalStats", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$personalMaxTotal(CallbackInfoReturnable<Integer> cir) {
        applyPersonalMaxTotal(cir);
    }

    @Inject(method = "getMaxAllowedIncreaseForStat", at = @At("HEAD"), cancellable = true, remap = false)
    private void lm$clampStatBuyHead(String stat, int amount, CallbackInfoReturnable<Integer> cir) {
        applyStatBuyClamp(stat, amount, cir);
    }

    @Inject(method = "getMaxAllowedIncreaseForStat", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$clampStatBuy(String stat, int amount, CallbackInfoReturnable<Integer> cir) {
        applyStatBuyClamp(stat, amount, cir);
    }

    private void applyPersonalMaxValue(CallbackInfoReturnable<Integer> cir) {
        if (!prestigeCapsActive()) {
            return;
        }
        StatsData self = (StatsData) (Object) this;
        int personal = LmOverhaulCapMath.personalLevelCap(self);
        if (personal > 0) {
            cir.setReturnValue(personal);
        }
    }

    private void applyPersonalMaxTotal(CallbackInfoReturnable<Integer> cir) {
        if (!prestigeCapsActive()) {
            return;
        }
        StatsData self = (StatsData) (Object) this;
        cir.setReturnValue(LmOverhaulCapMath.maxAssignableTotal(self));
    }

    private void applyStatBuyClamp(String stat, int amount, CallbackInfoReturnable<Integer> cir) {
        if (!prestigeCapsActive()) {
            return;
        }
        StatsData self = (StatsData) (Object) this;
        int personal = LmOverhaulCapMath.personalLevelCap(self);
        int maxTotal = LmOverhaulCapMath.maxAssignableTotal(self, personal);
        int total = 0;
        try {
            if (self.getStats() != null) {
                total = Math.max(0, self.getStats().getTotalStats());
            }
        } catch (Throwable ignored) {
        }
        int room = Math.max(0, maxTotal - total);
        int requested = Math.max(0, amount);
        int previous = cir.getReturnValue() != null ? Math.max(0, cir.getReturnValue()) : requested;
        cir.setReturnValue(Math.min(previous, Math.min(requested, room)));
    }

    private static boolean prestigeCapsActive() {
        try {
            return DifficultyConfig.get().enablePrestigeSystem;
        } catch (Throwable t) {
            return false;
        }
    }
}
