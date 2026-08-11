package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.PersonalSagaGuard;
import com.dragonminez.common.quest.PlayerQuestData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clears personal earn marks on story/quest resets, and credits {@code completeQuest}
 * to the owning player (covers admin {@code /dmzquest finish} as well as normal turn-in).
 */
@Mixin(value = PlayerQuestData.class, remap = false)
public abstract class PlayerQuestDataResetMixin {

    @Inject(method = "resetAll", at = @At("RETURN"), remap = false)
    private void dbzlegacy$clearAllPersonalEarns(CallbackInfo ci) {
        ServerPlayer player = PersonalSagaGuard.findOwner((PlayerQuestData) (Object) this);
        if (player != null) {
            PersonalSagaGuard.clearAll(player);
        }
    }

    @Inject(method = "resetSaga", at = @At("RETURN"), remap = false)
    private void dbzlegacy$clearSagaPersonalEarns(String sagaId, CallbackInfo ci) {
        ServerPlayer player = PersonalSagaGuard.findOwner((PlayerQuestData) (Object) this);
        if (player != null) {
            PersonalSagaGuard.clearSaga(player, sagaId);
        }
    }

    @Inject(method = "resetQuest", at = @At("RETURN"), remap = false)
    private void dbzlegacy$clearQuestPersonalEarn(String questId, CallbackInfo ci) {
        ServerPlayer player = PersonalSagaGuard.findOwner((PlayerQuestData) (Object) this);
        if (player != null) {
            PersonalSagaGuard.clearQuest(player, questId);
        }
    }

    @Inject(method = "completeQuest", at = @At("RETURN"), remap = false)
    private void dbzlegacy$markPersonalEarnOnComplete(String questId, CallbackInfo ci) {
        ServerPlayer player = PersonalSagaGuard.findOwner((PlayerQuestData) (Object) this);
        if (player != null) {
            PersonalSagaGuard.markEarned(player, questId);
        }
    }
}
