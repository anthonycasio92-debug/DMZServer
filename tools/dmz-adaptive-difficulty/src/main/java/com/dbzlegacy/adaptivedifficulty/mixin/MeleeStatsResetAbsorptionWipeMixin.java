package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.noea.AbsorptionWipeHelper;
import com.dbzlegacy.mohistmelee.StatsResetCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The melee patch adds {@code /dmzstats reset <percent> [keepSkills]} and
 * that form calls {@code resetPlayerProgress} itself, not
 * {@code StatsCommand.resetStats}. Wipe on that executor too.
 * {@code require = 0} skips the hook when the melee method is absent.
 */
@Mixin(value = StatsResetCommands.class, remap = false)
public abstract class MeleeStatsResetAbsorptionWipeMixin {

    @Inject(method = "resetSelf", at = @At("RETURN"), remap = false, require = 0)
    private static void lm$wipeOnMeleeSelfReset(
            CommandSourceStack source,
            String keepPercentage,
            boolean keepSkills,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (source == null || cir.getReturnValueI() <= 0) {
            return;
        }
        ServerPlayer player;
        try {
            player = source.m_81375_();
        } catch (Throwable ignored) {
            return;
        }
        if (player == null) {
            return;
        }
        AbsorptionWipeHelper.wipe(player, "StatsResetCommands.resetSelf");
    }
}
