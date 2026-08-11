package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.DmzMohistMeleeFix;
import com.dbzlegacy.mohistmelee.PersonalSagaGuard;
import com.dragonminez.common.quest.PlayerQuestData.QuestProgress;
import com.dragonminez.common.quest.PlayerQuestData.QuestStatus;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents party sync from granting {@code SUCCESS} (and its objective state)
 * to players who did not personally earn the quest and were not actively on it.
 */
@Mixin(value = QuestProgress.class, remap = false)
public abstract class QuestProgressMergeMixin {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final AtomicInteger LOGS = new AtomicInteger();

    @Shadow
    public abstract String getQuestId();

    @Shadow
    public abstract QuestStatus getStatus();

    @Inject(method = "mergeForwardFrom", at = @At("HEAD"), cancellable = true, remap = false)
    private void dbzlegacy$blockUnearnedSuccess(QuestProgress other, CallbackInfo ci) {
        ServerPlayer target = PersonalSagaGuard.MERGE_TARGET.get();
        if (target == null || other == null) {
            return;
        }
        if (other.getStatus() != QuestStatus.SUCCESS) {
            return;
        }
        String questId = getQuestId();
        QuestStatus own = getStatus();
        if (PersonalSagaGuard.canInheritCompletion(target, questId, own)) {
            // Co-op member who was actively on the quest — credit them, then allow SUCCESS.
            PersonalSagaGuard.markEarned(target, questId);
            return;
        }
        // Refuse borrowed completions entirely (purge after merge is the safety net).
        ci.cancel();
        int n = LOGS.incrementAndGet();
        if (n <= 40) {
            LOGGER.info(
                    "[{}] blocked party SUCCESS merge: player={} quest={} ownStatus={}",
                    DmzMohistMeleeFix.MOD_ID,
                    target.m_36316_().getName(),
                    questId,
                    own
            );
        }
    }
}
