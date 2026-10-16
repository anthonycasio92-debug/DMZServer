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
 * God Ki gate for regular-form transforms. {@code FormModeHandler} is the
 * single server-side activation path for regular forms (the form menu only
 * selects — {@code SelectFormC2S} never calls {@code setActiveForm}).
 *
 * <p>At transform time the target group is resolved with the same
 * {@code TransformationsHelper.getTransformTargetGroup} call the handler
 * makes. Custom God forms ({@code godforms}) need the God Ki Enhancement
 * present; the true God Ki form ({@code godkienhancement}) also needs the
 * mastery threshold. {@code require = 0} matches the other cross-mod gates:
 * a DMZ rename disables the gate instead of crashing the server.
 */
@Mixin(targets = "com.dragonminez.server.events.players.actionmode.FormModeHandler", remap = false)
public abstract class FormModeHandlerGodKiGateMixin {

    @Inject(
            method = "attemptTransform"
                    + "(Lnet/minecraft/server/level/ServerPlayer;"
                    + "Lcom/dragonminez/common/stats/StatsData;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private static void lm$gateGodKiForms(
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
