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
 * used to treat that like a Mohist dim-wipe and put the old stats back within seconds.
 * Suppress restore during the reset, then adopt the post-reset live values.
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataResetMixin {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final AtomicInteger LOGS = new AtomicInteger();

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
        // Long enough to cover the reset body + a couple of tick ensures / read fallbacks.
        PrimaryStatRepair.suppressRestore(player, 100);
        PrimaryStatRepair.clear(player.m_20148_());
        // clear() also clears suppress — re-apply suppress after clear
        PrimaryStatRepair.suppressRestore(player, 100);
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
        PrimaryStatRepair.adoptCurrent(player);
        // Keep suppress a bit longer so any delayed dim-followup cannot undo the reset.
        PrimaryStatRepair.suppressRestore(player, 100);
    }
}
