package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.PersonalSagaGuard;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.quest.PlayerQuestData;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Filters party quest merges so already-finished saga quests are not copied onto
 * players who did not earn them. Does not wipe existing progress on leave/login/death.
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
}
