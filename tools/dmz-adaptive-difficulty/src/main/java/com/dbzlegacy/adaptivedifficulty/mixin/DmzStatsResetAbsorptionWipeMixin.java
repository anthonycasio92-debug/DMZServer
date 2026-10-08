package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.noea.AbsorptionClearLog;
import com.dbzlegacy.adaptivedifficulty.noea.MajinAbsorptionStore;
import com.dragonminez.server.commands.StatsCommand;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code /dmzstats reset} runs {@code StatsCommand.resetStats}. That method
 * calls {@code resetPlayerProgress} only from a private lambda. The wipe
 * sits on {@code resetStats} itself, after a successful return, so it does
 * not depend on the stats-data mixin. A rejected percentage returns 0 and
 * is left alone. {@code require = 0} skips the hook if the signature changes.
 */
@Mixin(value = StatsCommand.class, remap = false)
public abstract class DmzStatsResetAbsorptionWipeMixin {

    @Inject(method = "resetStats", at = @At("RETURN"), remap = false, require = 0)
    private static void lm$wipeOnDmzStatsReset(
            CommandSourceStack source,
            Collection<ServerPlayer> players,
            String keepPercentage,
            boolean keepSkills,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (players == null || cir.getReturnValueI() <= 0) {
            return;
        }
        for (ServerPlayer player : players) {
            if (player == null) {
                continue;
            }
            try {
                System.out.println("[LM] wipeAbsorption firing for " + player.m_7755_().getString()
                        + " via StatsCommand.resetStats");
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
}
