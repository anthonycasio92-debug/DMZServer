package com.dbzlegacy.adaptivedifficulty.currency;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import io.github.lightman314.lightmanscurrency.api.capability.money.IMoneyHandler;
import io.github.lightman314.lightmanscurrency.api.capability.money.MoneyHandler;
import io.github.lightman314.lightmanscurrency.api.money.MoneyAPI;
import io.github.lightman314.lightmanscurrency.api.money.bank.IBankAccount;
import io.github.lightman314.lightmanscurrency.api.money.bank.reference.builtin.PlayerBankReference;
import io.github.lightman314.lightmanscurrency.api.money.coins.CoinAPI;
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue;
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyView;
import io.github.lightman314.lightmanscurrency.api.money.value.builtin.CoinValue;
import io.github.lightman314.lightmanscurrency.api.money.value.holder.IMoneyHolder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Payment for purchased difficulty.
 * Modes: {@code lightmans} (default when mod present), {@code training_points}, {@code free}.
 *
 * <p>Lightman's path combines equipped wallet + bank account + inventory coins.
 * {@link MoneyAPI#GetPlayersMoneyHandler} alone only sees the equipped wallet.
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
        return CoinValue.fromNumber(CoinAPI.MAIN_CHAIN, value);
    }

    /**
     * Combined money source: wallet + bank + loose inventory coins.
     * Returns null when nothing usable is available.
     */
    private static IMoneyHandler combinedHandler(ServerPlayer player) {
        List<IMoneyHandler> handlers = new ArrayList<>(3);

        try {
            IMoneyHolder wallet = MoneyAPI.getApi().GetPlayersMoneyHandler(player);
            if (wallet != null) {
                handlers.add(wallet);
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Lightman's wallet handler failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }

        try {
            IBankAccount bank = PlayerBankReference.of(player).get();
            if (bank != null) {
                handlers.add(bank);
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Lightman's bank handler failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }

        try {
            // Inventory coins (not just equipped wallet)
            IMoneyHandler inventory = MoneyAPI.getApi().GetContainersMoneyHandler(player.m_150109_(), player);
            if (inventory != null) {
                handlers.add(inventory);
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Lightman's inventory handler failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }

        if (handlers.isEmpty()) {
            return null;
        }
        if (handlers.size() == 1) {
            return handlers.get(0);
        }
        return MoneyHandler.combine(handlers);
    }

    private static boolean canAffordLightmans(ServerPlayer player, long cost) {
        try {
            MoneyValue price = coinPrice(cost);
            if (price == null || price.isEmpty() || price.isInvalid()) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] Lightman's price invalid for cost {}", AdaptiveDifficultyMod.MOD_ID, cost);
                return false;
            }
            IMoneyHandler handler = combinedHandler(player);
            if (handler == null) {
                return false;
            }
            MoneyView stored = handler.getStoredMoney();
            return stored != null && stored.containsValue(price);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Lightman's canAfford failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    private static boolean chargeLightmans(ServerPlayer player, long cost) {
        try {
            MoneyValue price = coinPrice(cost);
            if (price == null || price.isEmpty() || price.isInvalid()) {
                return false;
            }
            IMoneyHandler handler = combinedHandler(player);
            if (handler == null) {
                return false;
            }
            MoneyView stored = handler.getStoredMoney();
            if (stored == null || !stored.containsValue(price)) {
                return false;
            }
            // Simulate first so we never partially drain across sources on failure.
            MoneyValue simulated = handler.extractMoney(price, true);
            if (simulated != null && !simulated.isEmpty() && !simulated.isFree()) {
                return false;
            }
            MoneyValue leftover = handler.extractMoney(price, false);
            return leftover == null || leftover.isEmpty() || leftover.isFree();
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Lightman's charge failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    /** Human-readable balance for GUI / chat. */
    public static String balanceText(ServerPlayer player) {
        String mode = activeMode();
        if ("free".equalsIgnoreCase(mode)) {
            return "free";
        }
        if ("lightmans".equalsIgnoreCase(mode)) {
            try {
                IMoneyHandler handler = combinedHandler(player);
                if (handler == null) {
                    return "0";
                }
                MoneyView view = handler.getStoredMoney();
                if (view == null || view.isEmpty()) {
                    return "0";
                }
                MoneyValue main = view.valueOf(CoinAPI.MAIN_CHAIN);
                if (main != null && !main.isEmpty()) {
                    return main.getString();
                }
                return view.getString();
            } catch (Throwable t) {
                return "?";
            }
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null || data.getResources() == null) {
            return "0 TP";
        }
        return Math.round(data.getResources().getTrainingPoints()) + " TP";
    }

    /** Format a purchase cost in the active currency. */
    public static String formatCost(long cost) {
        if ("free".equalsIgnoreCase(activeMode())) {
            return "free";
        }
        if ("lightmans".equalsIgnoreCase(activeMode()) && LIGHTMANS_LOADED) {
            try {
                MoneyValue price = coinPrice(cost);
                if (price != null && !price.isEmpty()) {
                    return price.getString();
                }
            } catch (Throwable ignored) {
            }
            return cost + " coins";
        }
        return cost + " TP";
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
