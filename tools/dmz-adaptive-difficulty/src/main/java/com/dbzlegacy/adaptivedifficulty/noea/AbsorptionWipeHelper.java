package com.dbzlegacy.adaptivedifficulty.noea;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.entity.player.Player;

/**
 * One wipe path for every reset. The mixins only decide when to call it.
 * Lives outside the mixin package so the server classloader can find it.
 */
public final class AbsorptionWipeHelper {
    private AbsorptionWipeHelper() {}

    public static void wipe(Player player, String via) {
        if (player == null) {
            return;
        }
        try {
            String name = player.m_7755_().getString();
            String where = via == null || via.isEmpty() ? "" : " via " + via;
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] wipeAbsorption firing for {}{}", AdaptiveDifficultyMod.MOD_ID, name, where);
        } catch (Throwable ignored) {
        }
        try {
            MajinAbsorptionStore.clear(player);
        } catch (Throwable t) {
            try {
                AbsorptionClearLog.failure(t);
            } catch (Throwable ignored) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] wipeAbsorption failed", AdaptiveDifficultyMod.MOD_ID, t);
            }
        }
    }

    public static void wipeIfRaceChanges(StatsData stats, String race) {
        try {
            MajinAbsorptionStore.clearIfRaceChanges(stats, race);
        } catch (Throwable t) {
            try {
                AbsorptionClearLog.failure(t);
            } catch (Throwable ignored) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] wipeIfRaceChanges failed", AdaptiveDifficultyMod.MOD_ID, t);
            }
        }
    }
}
