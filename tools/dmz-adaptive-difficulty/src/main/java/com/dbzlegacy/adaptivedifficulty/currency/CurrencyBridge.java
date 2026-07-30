package com.dbzlegacy.adaptivedifficulty.currency;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.server.level.ServerPlayer;

/**
 * Payment for purchased difficulty.
 * Default uses DMZ Training Points (same authoritative currency your TP↔Fabled scripts use).
 * {@code lightmans} reserved for reflection when Lightman's Currency is installed later.
 */
public final class CurrencyBridge {
    private CurrencyBridge() {}

    public static boolean canAfford(ServerPlayer player, long cost) {
        if (cost <= 0) {
            return true;
        }
        String mode = DifficultyConfig.get().purchaseCurrency;
        if ("free".equalsIgnoreCase(mode)) {
            return true;
        }
        if ("lightmans".equalsIgnoreCase(mode)) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] lightmans currency mode selected but not wired yet; denying purchase",
                    AdaptiveDifficultyMod.MOD_ID
            );
            return false;
        }
        // training_points
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return false;
        }
        Resources resources = data.getResources();
        return resources != null && resources.getTrainingPoints() >= cost;
    }

    public static boolean charge(ServerPlayer player, long cost) {
        if (cost <= 0) {
            return true;
        }
        String mode = DifficultyConfig.get().purchaseCurrency;
        if ("free".equalsIgnoreCase(mode)) {
            return true;
        }
        if ("lightmans".equalsIgnoreCase(mode)) {
            return false;
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return false;
        }
        Resources resources = data.getResources();
        if (resources == null || resources.getTrainingPoints() < cost) {
            return false;
        }
        resources.removeTrainingPoints((float) cost);
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        } catch (Throwable ignored) {
            // Sync best-effort.
        }
        return true;
    }

    public static String currencyLabel() {
        String mode = DifficultyConfig.get().purchaseCurrency;
        if ("free".equalsIgnoreCase(mode)) {
            return "free";
        }
        if ("lightmans".equalsIgnoreCase(mode)) {
            return "Lightman's Currency";
        }
        return "Training Points";
    }
}
