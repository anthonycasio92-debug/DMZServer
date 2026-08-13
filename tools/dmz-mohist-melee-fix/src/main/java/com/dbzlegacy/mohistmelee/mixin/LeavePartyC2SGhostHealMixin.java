package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.GhostPartyHeal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LeavePartyC2S early-returns with "leave.solo" when SavedData has no party,
 * leaving PlayerQuestData ghost party fields intact. Heal that path.
 */
@Mixin(targets = "com.dragonminez.common.network.C2S.LeavePartyC2S", remap = false)
public abstract class LeavePartyC2SGhostHealMixin {

    @Inject(method = "lambda$handle$0", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dbzlegacy$healGhostLeave(NetworkEvent.Context ctx, CallbackInfo ci) {
        ServerPlayer player = ctx.getSender();
        if (player == null) {
            return;
        }
        if (GhostPartyHeal.healIfGhost(player)) {
            player.m_213846_(
                    Component.m_237113_("Cleared stuck party status. You can continue saga solo.")
                            .m_130940_(ChatFormatting.GREEN)
            );
            ci.cancel();
        }
    }
}
