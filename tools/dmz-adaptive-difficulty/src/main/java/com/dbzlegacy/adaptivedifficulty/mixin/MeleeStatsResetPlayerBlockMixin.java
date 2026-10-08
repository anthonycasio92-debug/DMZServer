package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.dbzlegacy.mohistmelee.StatsResetCommands;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The melee patch's {@code /dmzstats reset <percent>} form does not enter
 * {@code StatsCommand.resetStats}. Players cannot use that form either.
 * {@code require = 0} skips the hook when the melee method is absent.
 */
@Mixin(value = StatsResetCommands.class, remap = false)
public abstract class MeleeStatsResetPlayerBlockMixin {

    @Inject(method = "resetSelf", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void lm$blockPlayerMeleeSelfReset(
            CommandSourceStack source,
            String keepPercentage,
            boolean keepSkills,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (StaffAccess.allowDmzStatsReset(source)) {
            return;
        }
        StaffAccess.denyDmzStatsReset(source);
        cir.setReturnValue(0);
    }
}
