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
 * Overrides dmzrevamp {@code StatsDataLevelingRevampMixin} after it runs (priority 2000).
 * Playable max value / max stat total / per-click stat buys follow LM breakthrough caps.
 * Overhaul {@code PrestigeSystem.levelCap} is left native for hex/prestige scale math.
 */
@Mixin(value = StatsData.class, remap = false, priority = 2000)
public abstract class StatsDataMixin {
    @Shadow(remap = false)
    public abstract Player getPlayer();

    @Inject(method = "getConfiguredMaxValue", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$personalMaxValue(CallbackInfoReturnable<Integer> cir) {
        if (!prestigeCapsActive()) {
            return;
        }
        StatsData self = (StatsData) (Object) this;
        int personal = LmOverhaulCapMath.personalLevelCap(self);
        if (personal > 0) {
            cir.setReturnValue(personal);
        }
    }

    @Inject(method = "getConfiguredMaxTotalStats", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$personalMaxTotal(CallbackInfoReturnable<Integer> cir) {
        if (!prestigeCapsActive()) {
            return;
        }
        StatsData self = (StatsData) (Object) this;
        cir.setReturnValue(LmOverhaulCapMath.maxAssignableTotal(self));
    }

    @Inject(method = "getMaxAllowedIncreaseForStat", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$clampStatBuy(String stat, int amount, CallbackInfoReturnable<Integer> cir) {
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
        int allowed = cir.getReturnValue() != null ? Math.max(0, cir.getReturnValue()) : 0;
        cir.setReturnValue(Math.min(allowed, room));
    }

    private static boolean prestigeCapsActive() {
        try {
            return DifficultyConfig.get().enablePrestigeSystem;
        } catch (Throwable t) {
            return false;
        }
    }
}
