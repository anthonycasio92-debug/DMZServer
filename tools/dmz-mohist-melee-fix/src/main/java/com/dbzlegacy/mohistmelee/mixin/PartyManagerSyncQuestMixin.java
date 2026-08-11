package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.PersonalSagaGuard;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.quest.PlayerQuestData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Filters party quest merges and strips borrowed completions on leave so players
 * are not stuck showing finished sagas they never earned.
 */
@Mixin(value = PartyManager.class, remap = false)
public abstract class PartyManagerSyncQuestMixin {

    @Redirect(
            method = "syncQuestProgress",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/quest/PlayerQuestData;mergeQuestStateFrom(Lcom/dragonminez/common/quest/PlayerQuestData;)V"
            ),
            remap = false
    )
    private static void dbzlegacy$filteredPartyMerge(
            PlayerQuestData toData,
            PlayerQuestData fromData,
            ServerPlayer fromPlayer,
            ServerPlayer toPlayer
    ) {
        PersonalSagaGuard.MERGE_TARGET.set(toPlayer);
        try {
            if (toPlayer != null) {
                PersonalSagaGuard.ensureBootstrapped(toPlayer);
            }
            toData.mergeQuestStateFrom(fromData);
            // Belt-and-suspenders: even if SUCCESS leaked past mergeForwardFrom, strip it.
            if (toPlayer != null) {
                PersonalSagaGuard.purgeUnearnedCompletions(toPlayer);
            }
        } finally {
            PersonalSagaGuard.MERGE_TARGET.remove();
        }
    }

    @Inject(method = "leaveParty", at = @At("RETURN"), remap = false)
    private static void dbzlegacy$purgeOnLeave(ServerPlayer player, CallbackInfo ci) {
        if (player == null) {
            return;
        }
        if (PersonalSagaGuard.purgeUnearnedCompletions(player)) {
            NetworkHandler.sendToPlayer(new ProgressionSyncS2C(player), player);
        }
    }
}
