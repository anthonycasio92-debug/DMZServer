package com.dbzlegacy.adaptivedifficulty.mixin.noea;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Noea assigns the Destroyer path from story progress.
 * {@code DestroyerRoleEvents.tick} calls
 * {@code DestroyerRoleService.initiateFromStoryProgress} every 100 server
 * ticks for each online player. When God Ki, the story-saga bit, and Beerus's
 * planet are already true, and the saved rank is {@code NONE}, that method
 * writes an assignment with reason {@code story-progress}.
 *
 * <p>Resign removes the assignment ({@code voluntary-resignation}) and
 * reconciles tags back to {@code NONE}. The same tick then sees rank
 * {@code NONE} with eligibility still true and assigns the path again.
 * Cancelling this method at HEAD returns false before that write, so the
 * first automatic assignment and the post-resign reassignment both stop.
 *
 * <p>Choosing the path stays on the explicit Beerus and Whis methods
 * ({@code beginCandidacy}, {@code train}, {@code attemptTrial},
 * {@code acceptApprenticeship}, {@code seekAppointment},
 * {@code completeWhisTraining}, {@code completePlanetaryJudgment},
 * {@code completePlanetaryErasure}, {@code completeZenoAudienceAndAppoint}).
 * {@code HakaiDestroyerGateMixin} is unchanged.
 *
 * <p>The target is a string because this mixin does not compile against Noea.
 * Do not import {@code DestroyerRoleService}.
 */
@Mixin(targets = "com.butterjaffa.noeabosses.DestroyerRoleService", remap = false)
public abstract class DestroyerStoryAssignMixin {

    @Inject(
            method = "initiateFromStoryProgress",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private static void lm$blockStoryAutoAssign(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (cir != null) {
            cir.setReturnValue(false);
        }
    }
}
