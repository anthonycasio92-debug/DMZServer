package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.shop.GodKiEnhancementShop;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * God Ki gate for stack-form transforms — the stack twin of
 * {@link FormModeHandlerGodKiGateMixin}. The enhancement group is cleared
 * from both stack and regular masteries on wipe, so both activation paths
 * carry the same gate.
 */
@Mixin(
        targets = "com.dragonminez.server.events.players.actionmode.StackFormModeHandler",
        remap = false)
public abstract class StackFormModeHandlerGodKiGateMixin {

    @Inject(
            method = "attemptTransform"
                    + "(Lnet/minecraft/server/level/ServerPlayer;"
                    + "Lcom/dragonminez/common/stats/StatsData;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private static void lm$gateGodKiStackForms(
            ServerPlayer player, StatsData data, CallbackInfo ci) {
        try {
            if (player == null || data == null) {
                return;
            }
            String group = TransformationsHelper.getTransformTargetGroup(data);
            String block = GodKiEnhancementShop.blockReason(player, group);
            if (block == null) {
                return;
            }
            GodKiEnhancementShop.notifyBlocked(player, block);
            ci.cancel();
        } catch (Throwable ignored) {
        }
    }
}
