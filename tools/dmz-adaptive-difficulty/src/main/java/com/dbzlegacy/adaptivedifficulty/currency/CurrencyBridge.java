package com.dbzlegacy.adaptivedifficulty.currency;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import io.github.lightman314.lightmanscurrency.api.capability.money.IMoneyHandler;
import io.github.lightman314.lightmanscurrency.api.money.MoneyAPI;
import io.github.lightman314.lightmanscurrency.api.money.coins.CoinAPI;
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue;
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyView;
import io.github.lightman314.lightmanscurrency.api.money.value.builtin.CoinValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Difficulty increases are always paid in Lightman's Currency.
 * Costs are priced in iron coins (configurable item id).
 * <p>
 * Payments use <b>inventory coins only</b> (not wallet or bank).
 */
public final class CurrencyBridge {
    private static final boolean LIGHTMANS_LOADED = ModList.get().isLoaded("lightmanscurrency");

    private CurrencyBridge() {}

    public static boolean lightmansAvailable() {
        return LIGHTMANS_LOADED;
    }

    /** Always Lightman's for difficulty payments. */
    public static String activeMode() {
        return "lightmans";
    }

    public static boolean canAfford(ServerPlayer player, long ironCoins) {
        if (ironCoins <= 0) {
            return true;
        }
        if (!LIGHTMANS_LOADED) {
            return false;
        }
        return canAffordLightmans(player, ironCoins);
    }

    public static boolean charge(ServerPlayer player, long ironCoins) {
        if (ironCoins <= 0) {
            return true;
        }
        if (!LIGHTMANS_LOADED) {
            return false;
        }
        return chargeLightmans(player, ironCoins);
    }

    /** Build a Lightman's price of {@code ironCoins} iron coins (or configured coin item). */
    private static MoneyValue coinPrice(long ironCoins) {
        long count = Math.max(1L, ironCoins);
        Item coin = resolveCostCoin();
        if (coin != null) {
            try {
                // fromItemOrValue(Item,long) is always count=1 — use (Item,int,long) for N coins.
                int coinCount = (int) Math.min(Integer.MAX_VALUE, count);
                MoneyValue priced = CoinValue.fromItemOrValue(coin, coinCount, count);
                if (priced != null && !priced.isEmpty() && !priced.isInvalid()) {
                    return priced;
                }
            } catch (Throwable ignored) {
            }
        }
        // Fallback: treat number as main-chain core value
        return CoinValue.fromNumber(CoinAPI.MAIN_CHAIN, count);
    }

    private static Item resolveCostCoin() {
        String id = DifficultyConfig.get().costCoinItem;
        if (id == null || id.isBlank()) {
            id = "lightmanscurrency:coin_iron";
        }
        try {
            ResourceLocation rl = new ResourceLocation(id.trim());
            return ForgeRegistries.ITEMS.getValue(rl);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Inventory coin stacks only — wallet/bank are ignored for difficulty payments. */
    private static IMoneyHandler inventoryHandler(ServerPlayer player) {
        try {
            return MoneyAPI.getApi().GetContainersMoneyHandler(player.m_150109_(), player);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Lightman's inventory handler failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return null;
        }
    }

    private static boolean canAffordLightmans(ServerPlayer player, long ironCoins) {
        try {
            MoneyValue price = coinPrice(ironCoins);
            if (price == null || price.isEmpty() || price.isInvalid()) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] Lightman's price invalid for {} iron coins", AdaptiveDifficultyMod.MOD_ID, ironCoins);
                return false;
            }
            IMoneyHandler handler = inventoryHandler(player);
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

    private static boolean chargeLightmans(ServerPlayer player, long ironCoins) {
        try {
            MoneyValue price = coinPrice(ironCoins);
            if (price == null || price.isEmpty() || price.isInvalid()) {
                return false;
            }
            IMoneyHandler handler = inventoryHandler(player);
            if (handler == null) {
                return false;
            }
            MoneyView stored = handler.getStoredMoney();
            if (stored == null || !stored.containsValue(price)) {
                return false;
            }
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

    public static String balanceText(ServerPlayer player) {
        if (!LIGHTMANS_LOADED) {
            return "Lightman's missing";
        }
        try {
            IMoneyHandler handler = inventoryHandler(player);
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

    /** Format an iron-coin cost for GUI / chat. */
    public static String formatCost(long ironCoins) {
        if (ironCoins <= 0) {
            return "free";
        }
        if (!LIGHTMANS_LOADED) {
            return ironCoins + " iron coins";
        }
        try {
            MoneyValue price = coinPrice(ironCoins);
            if (price != null && !price.isEmpty()) {
                return price.getString();
            }
        } catch (Throwable ignored) {
        }
        return ironCoins + " iron";
    }

    public static String currencyLabel() {
        return LIGHTMANS_LOADED ? "Inventory Coins" : "Lightman's Currency (missing)";
    }
}
