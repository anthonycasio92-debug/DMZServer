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
 * Also: vanilla DMZ {@code Status.reset()} clears {@code hasCreatedCharacter}. After sync the
 * client reopens race selection; recreating the character wipes skills/forms/level. Preserve the
 * flag across reset so {@code keepSkills}/percentage resets stay usable.
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataResetMixin {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final AtomicInteger LOGS = new AtomicInteger();
    private static final int SUPPRESS_TICKS = 200;
    private static final ThreadLocal<Boolean> HAD_CREATED_CHARACTER = new ThreadLocal<>();

    @Inject(method = "resetPlayerProgress", at = @At("HEAD"), remap = false)
    private void dbzlegacy$suppressBeforeReset(
            ServerPlayer player,
            Integer keepPercent,
            boolean keepSkills,
            boolean keepTail,
            CallbackInfo ci
    ) {
        StatsData self = (StatsData) (Object) this;
        boolean hadCreated = false;
        try {
            hadCreated = self.getStatus() != null && self.getStatus().isHasCreatedCharacter();
        } catch (Throwable ignored) {
        }
        HAD_CREATED_CHARACTER.set(hadCreated);

        if (player != null) {
            PrimaryStatRepair.beginIntentionalReset(player, SUPPRESS_TICKS);
        }
        int n = LOGS.incrementAndGet();
        if (n <= 40) {
            String name = player != null ? player.m_36316_().getName() : "?";
            LOGGER.info(
                    "[{}] stat reset: player={} keepPercent={} keepSkills={} hadCreatedChar={}",
                    DmzMohistMeleeFix.MOD_ID,
                    name,
                    keepPercent,
                    keepSkills,
                    hadCreated
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
        StatsData self = (StatsData) (Object) this;
        Boolean hadCreated = HAD_CREATED_CHARACTER.get();
        HAD_CREATED_CHARACTER.remove();
        if (Boolean.TRUE.equals(hadCreated)) {
            try {
                // DMZ Status.reset() always clears this; restore so keepSkills/forms survive.
                self.getStatus().setHasCreatedCharacter(true);
            } catch (Throwable t) {
                if (LOGS.get() < 5) {
                    LOGGER.warn(
                            "[{}] failed to restore hasCreatedCharacter: {}",
                            DmzMohistMeleeFix.MOD_ID,
                            t.toString()
                    );
                }
            }
        }
        if (player != null) {
            PrimaryStatRepair.endIntentionalReset(player, keepPercent, SUPPRESS_TICKS);
        }
    }
}
