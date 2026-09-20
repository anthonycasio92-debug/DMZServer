package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.PrestigeResourceRecovery;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.DmzRevampPrestigeBridge;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@link StatsData#resetPlayerProgress} clears dmzrevamp Overhaul prestige count on stats data.
 * Re-apply Legacy Mechanics held prestige after every intentional wipe.
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataResetPrestigeSyncMixin {
    @Inject(method = "resetPlayerProgress", at = @At("RETURN"), remap = false)
    private void lm$syncOverhaulPrestigeAfterReset(
            ServerPlayer player,
            Integer keepPercent,
            boolean keepSkills,
            boolean keepTail,
            CallbackInfo ci
    ) {
        if (player == null) {
            return;
        }
        try {
            PrestigeResourceRecovery.afterDmzStatsReset(player);
        } catch (Throwable ignored) {
        }
        try {
            DmzRevampPrestigeBridge.scheduleSyncAfterStatsReset(player);
        } catch (Throwable ignored) {
        }
    }
}
