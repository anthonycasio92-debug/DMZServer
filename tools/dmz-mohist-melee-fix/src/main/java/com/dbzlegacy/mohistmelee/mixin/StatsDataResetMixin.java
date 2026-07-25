package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.DmzMohistMeleeFix;
import com.dbzlegacy.mohistmelee.PrimaryStatRepair;
import com.dragonminez.common.stats.StatsData;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code StatsData.resetPlayerProgress} sets primaries to 0 (or a %). Our snapshot/restore
 * must not treat that like a Mohist dim-wipe and put the old stats back.
 * Cancel delayed teleport follow-ups, suppress restore, and adopt/force-zero the snapshot.
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataResetMixin {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final AtomicInteger LOGS = new AtomicInteger();
    private static final int SUPPRESS_TICKS = 200;

    @Inject(method = "resetPlayerProgress", at = @At("HEAD"), remap = false)
    private void dbzlegacy$suppressBeforeReset(
            ServerPlayer player,
            Integer keepPercent,
            boolean keepSkills,
            boolean keepTail,
            CallbackInfo ci
    ) {
        if (player == null) {
            return;
        }
        PrimaryStatRepair.beginIntentionalReset(player, SUPPRESS_TICKS);
        int n = LOGS.incrementAndGet();
        if (n <= 40) {
            LOGGER.info(
                    "[{}] stat reset: cleared primary snapshot player={} keepPercent={}",
                    DmzMohistMeleeFix.MOD_ID,
                    player.m_36316_().getName(),
                    keepPercent
            );
        }
    }

    @Inject(method = "resetPlayerProgress", at = @At("RETURN"), remap = false)
    private void dbzlegacy$adoptAfterReset(
            ServerPlayer player,
            Integer keepPercent,
            boolean keepSkills,
            boolean keepTail,
            CallbackInfo ci
    ) {
        if (player == null) {
            return;
        }
        PrimaryStatRepair.endIntentionalReset(player, keepPercent, SUPPRESS_TICKS);
    }
}
