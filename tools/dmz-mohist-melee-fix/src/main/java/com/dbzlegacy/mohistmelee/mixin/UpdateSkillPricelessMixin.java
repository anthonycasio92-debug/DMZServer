package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.SkillTpCostHelper;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * DMZ {@code UpdateSkillC2S.computeTpCost} does {@code Math.max(0, configuredCost)}.
 * Configured {@code -1} ("Priceless") therefore becomes {@code 0} TP.
 * <p>
 * Form skills with {@code buyFromMaster: false} are pre-registered at level 0, so the menu
 * "buy" is actually an {@code UPGRADE} that hits this clamp and grants the skill for free.
 * Restore negative costs so purchase/upgrade handlers reject via {@code if (cost < 0) return}.
 */
@Mixin(value = UpdateSkillC2S.class, priority = 2000, remap = false)
public abstract class UpdateSkillPricelessMixin {

    @Inject(
            method = "computeTpCost(Lcom/dragonminez/common/stats/StatsData;Ljava/lang/String;I)I",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void dbzlegacy$preservePricelessCost(
            StatsData data,
            String skillName,
            int level,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (skillName == null || skillName.isEmpty() || level < 0) {
            return;
        }
        try {
            int raw = SkillTpCostHelper.rawConfiguredCost(data, skillName, level);
            if (raw < 0) {
                cir.setReturnValue(-1);
            }
        } catch (Throwable ignored) {
            // Never break skill purchase flow on unexpected config shapes.
        }
    }
}
