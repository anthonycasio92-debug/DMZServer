package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.mixin.noea.MajinAbsorptionStore;
import com.dragonminez.common.network.C2S.NPCActionC2S;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dende action 2 is the stat reset. It calls {@code resetPlayerProgress}
 * itself and never enters {@code StatsCommand.resetStats}. Heal (1) and
 * tail (3) are left alone. {@code require = 0} skips a signature change.
 */
@Mixin(value = NPCActionC2S.class, remap = false)
public abstract class DendeResetAbsorptionWipeMixin {

    @Inject(method = "handleDende", at = @At("RETURN"), remap = false, require = 0)
    private static void lm$wipeOnDendeReset(
            ServerPlayer player,
            StatsData data,
            int actionId,
            CallbackInfo ci
    ) {
        if (player == null || actionId != 2) {
            return;
        }
        try {
            System.out.println("[LM] wipeAbsorption firing for " + player.m_7755_().getString()
                    + " via NPCActionC2S.handleDende");
        } catch (Throwable ignored) {
        }
        try {
            MajinAbsorptionStore.clear(player);
        } catch (Throwable t) {
            try {
                AbsorptionClearLog.failure(t);
            } catch (Throwable ignored) {
                t.printStackTrace();
            }
        }
    }
}
