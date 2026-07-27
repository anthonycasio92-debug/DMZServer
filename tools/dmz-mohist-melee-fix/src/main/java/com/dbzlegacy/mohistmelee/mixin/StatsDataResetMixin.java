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
 * <p>
 * Do <b>not</b> touch {@code hasCreatedCharacter}. Vanilla DMZ {@code Status.reset()} clears it
 * so the client reopens race selection ({@code forceCharacterCreation}). Soft resets still work:
 * {@code initializeWithRaceAndClass} only writes base stats when all primaries are 0, and does
 * not wipe kept skills.
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
        if (player != null) {
            PrimaryStatRepair.beginIntentionalReset(player, SUPPRESS_TICKS);
        }
        int n = LOGS.incrementAndGet();
        if (n <= 40) {
            String name = player != null ? player.m_36316_().getName() : "?";
            LOGGER.info(
                    "[{}] stat reset: player={} keepPercent={} keepSkills={} (DMZ clears hasCreatedCharacter → race select)",
                    DmzMohistMeleeFix.MOD_ID,
                    name,
                    keepPercent,
                    keepSkills
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
        if (player != null) {
            PrimaryStatRepair.endIntentionalReset(player, keepPercent, SUPPRESS_TICKS);
        }
    }
}
