package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.noea.GodKiWipeHelper;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.dragonminez.server.commands.StatsCommand;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Players cannot run {@code /dmzstats reset}. Staff still can. After a
 * successful reset, absorption is wiped through {@code AbsorptionWipeHelper}.
 * That helper is loaded by name so this always-on mixin does not link Noea
 * while {@code StatsCommand} is transformed. Prestige wipes stats through
 * {@code resetPlayerProgress} and does not use this command.
 * {@code require = 0} skips a hook if the signature changes.
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

    /**
     * {@code resetPlayerProgress} is called from a private lambda inside
     * {@code resetStats}. The wipe sits on {@code resetStats} itself, after a
     * successful return. A rejected percentage returns 0 and is left alone.
     */
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
            lm$wipeAbsorption(player, "StatsCommand.resetStats");
            GodKiWipeHelper.wipe(player, "StatsCommand.resetStats");
        }
    }

    private static void lm$wipeAbsorption(Player player, String via) {
        try {
            Class<?> helper = Class.forName(
                    "com.dbzlegacy.adaptivedifficulty.noea.AbsorptionWipeHelper",
                    false,
                    DmzStatsResetPlayerBlockMixin.class.getClassLoader());
            helper.getMethod("wipe", Player.class, String.class).invoke(null, player, via);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
