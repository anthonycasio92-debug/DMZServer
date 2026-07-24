package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.CombatRepair;
import com.dbzlegacy.mohistmelee.RateLog;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code StatsData.resetPlayerProgress} sets primaries to 0 (or a %). Snapshot restore must not
 * treat that like a Mohist dim-wipe and put old STR/etc back.
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataResetMixin {
    private static final int SUPPRESS_TICKS = 200;

    @Inject(method = "resetPlayerProgress", at = @At("HEAD"), remap = false)
    private void dmzmmf$suppressBeforeReset(
            ServerPlayer player,
            Integer keepPercentage,
            boolean keepSkills,
            boolean forceSaiyanTail,
            CallbackInfo ci
    ) {
        if (player == null) {
            return;
        }
        CombatRepair.beginIntentionalReset((Player) player, SUPPRESS_TICKS);
        RateLog.info(
                "reset",
                40,
                "stat reset: cleared primary snapshot player={} keepPercent={}",
                player.m_36316_().getName(),
                keepPercentage
        );
    }

    @Inject(method = "resetPlayerProgress", at = @At("RETURN"), remap = false)
    private void dmzmmf$adoptAfterReset(
            ServerPlayer player,
            Integer keepPercentage,
            boolean keepSkills,
            boolean forceSaiyanTail,
            CallbackInfo ci
    ) {
        if (player == null) {
            return;
        }
        CombatRepair.endIntentionalReset((Player) player, keepPercentage, SUPPRESS_TICKS);
    }
}
