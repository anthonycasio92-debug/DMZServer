package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.PersonalSagaGuard;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.quest.PlayerQuestData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Tags the merge target player so {@link QuestProgressMergeMixin} can refuse
 * copying already-completed saga quests onto players who did not earn them.
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
        } finally {
            PersonalSagaGuard.MERGE_TARGET.remove();
        }
    }
}
