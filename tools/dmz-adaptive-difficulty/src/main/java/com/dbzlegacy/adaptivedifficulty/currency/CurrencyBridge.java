package com.dbzlegacy.adaptivedifficulty.currency;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import io.github.lightman314.lightmanscurrency.api.money.MoneyAPI;
import io.github.lightman314.lightmanscurrency.api.money.coins.CoinAPI;
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue;
import io.github.lightman314.lightmanscurrency.api.money.value.builtin.CoinValue;
import io.github.lightman314.lightmanscurrency.api.money.value.holder.IMoneyHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Payment for purchased difficulty.
 * Modes: {@code lightmans} (default when mod present), {@code training_points}, {@code free}.
 */
public final class CurrencyBridge {
    private static final boolean LIGHTMANS_LOADED = ModList.get().isLoaded("lightmanscurrency");

    private CurrencyBridge() {}

    public static boolean lightmansAvailable() {
        return LIGHTMANS_LOADED;
    }

    public static String activeMode() {
        String mode = DifficultyConfig.get().purchaseCurrency;
        if ("lightmans".equalsIgnoreCase(mode) && !LIGHTMANS_LOADED) {
            return "training_points";
        }
        return mode == null ? "training_points" : mode;
    }

    public static boolean canAfford(ServerPlayer player, long cost) {
        if (cost <= 0) {
            return true;
        }
        String mode = activeMode();
        if ("free".equalsIgnoreCase(mode)) {
            return true;
        }
        if ("lightmans".equalsIgnoreCase(mode)) {
            return canAffordLightmans(player, cost);
        }
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
        String mode = activeMode();
        if ("free".equalsIgnoreCase(mode)) {
            return true;
        }
        if ("lightmans".equalsIgnoreCase(mode)) {
            return chargeLightmans(player, cost);
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
        }
        return true;
    }

    private static MoneyValue coinPrice(long coreValue) {
        long value = Math.max(1L, coreValue);
        // CoinAPI.MAIN_CHAIN == "main"
        return CoinValue.fromNumber(CoinAPI.MAIN_CHAIN, value);
    }

    private static boolean canAffordLightmans(ServerPlayer player, long cost) {
        try {
            MoneyValue price = coinPrice(cost);
            IMoneyHolder handler = MoneyAPI.getApi().GetPlayersMoneyHandler(player);
            if (handler == null) {
                return false;
            }
            return handler.getStoredMoney().containsValue(price);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Lightman's canAfford failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    private static boolean chargeLightmans(ServerPlayer player, long cost) {
        try {
            MoneyValue price = coinPrice(cost);
            IMoneyHolder handler = MoneyAPI.getApi().GetPlayersMoneyHandler(player);
            if (handler == null || !handler.getStoredMoney().containsValue(price)) {
                return false;
            }
            // extractMoney returns unextracted remainder; empty = full success
            MoneyValue leftover = handler.extractMoney(price, false);
            return leftover == null || leftover.isEmpty() || leftover.isFree();
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Lightman's charge failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    public static String currencyLabel() {
        String mode = activeMode();
        if ("free".equalsIgnoreCase(mode)) {
            return "free";
        }
        if ("lightmans".equalsIgnoreCase(mode)) {
            return LIGHTMANS_LOADED ? "Lightman's Coins" : "Training Points (Lightman's missing)";
        }
        return "Training Points";
    }
}
