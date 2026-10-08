package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.dragonminez.server.commands.StatsCommand;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Players cannot run {@code /dmzstats reset}. Staff still can. Prestige
 * wipes stats through {@code resetPlayerProgress} and does not use this command.
 * {@code require = 0} skips the hook if the signature changes.
 */
@Mixin(value = StatsCommand.class, remap = false)
public abstract class DmzStatsResetPlayerBlockMixin {

    @Inject(method = "resetStats", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void lm$blockPlayerDmzStatsReset(
            CommandSourceStack source,
            Collection<ServerPlayer> players,
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
