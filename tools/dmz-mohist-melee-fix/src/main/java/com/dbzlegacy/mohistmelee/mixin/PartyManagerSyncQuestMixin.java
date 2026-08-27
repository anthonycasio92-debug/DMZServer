package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.GhostPartyHeal;
import com.dbzlegacy.mohistmelee.PersonalSagaGuard;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.quest.PlayerQuestData;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Filters party quest merges so already-finished saga quests are not copied onto
 * players who did not earn them. Also clears ghost party UI state when SavedData
 * already has the player as solo. Does not wipe existing progress on leave/login/death.
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
            Set<String> completedBefore = PersonalSagaGuard.snapshotCompleted(toData);
            Set<String> acceptedBefore = PersonalSagaGuard.snapshotAccepted(toData);
            toData.mergeQuestStateFrom(fromData);
            // Only strip completions that this merge newly applied without earn/co-op credit.
            if (toPlayer != null) {
                PersonalSagaGuard.stripNewlyBorrowedCompletions(
                        toPlayer, toData, completedBefore, acceptedBefore
                );
            }
        } finally {
            PersonalSagaGuard.MERGE_TARGET.remove();
        }
    }

    /**
     * leaveParty returns immediately when PartySavedData has no party, so ghost
     * PlayerQuestData.activePartyId is never cleared. Heal after every leave attempt.
     */
    @Inject(method = "leaveParty", at = @At("RETURN"), remap = false)
    private static void dbzlegacy$healGhostAfterLeave(ServerPlayer player, CallbackInfo ci) {
        GhostPartyHeal.healIfGhost(player);
    }

    /**
     * onPlayerLogin also returns early when SavedData has no party, leaving a
     * persisted ghost party in quest NBT for the V-menu.
     */
    @Inject(method = "onPlayerLogin", at = @At("RETURN"), remap = false)
    private static void dbzlegacy$healGhostOnLogin(
            PlayerEvent.PlayerLoggedInEvent event,
            CallbackInfo ci
    ) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            GhostPartyHeal.healIfGhost(sp);
        }
    }
}
