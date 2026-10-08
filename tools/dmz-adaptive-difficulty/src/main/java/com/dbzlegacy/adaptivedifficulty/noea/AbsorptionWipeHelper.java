package com.dbzlegacy.adaptivedifficulty.noea;

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
            System.out.println("[LM] wipeAbsorption firing for " + name + where);
        } catch (Throwable ignored) {
        }
        try {
            MajinAbsorptionStore.clear(player);
        } catch (Throwable t) {
            try {
                AbsorptionClearLog.failure(t);
            } catch (Throwable ignored) {
                t.printStackTrace();
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
                t.printStackTrace();
            }
        }
    }
}
