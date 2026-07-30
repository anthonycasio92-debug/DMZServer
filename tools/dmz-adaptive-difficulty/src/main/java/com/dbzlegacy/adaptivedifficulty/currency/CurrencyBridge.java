package com.dbzlegacy.adaptivedifficulty.currency;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import io.github.lightman314.lightmanscurrency.api.capability.money.IMoneyHandler;
import io.github.lightman314.lightmanscurrency.api.money.MoneyAPI;
import io.github.lightman314.lightmanscurrency.api.money.coins.CoinAPI;
import io.github.lightman314.lightmanscurrency.api.money.coins.data.ChainData;
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue;
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyView;
import io.github.lightman314.lightmanscurrency.api.money.value.builtin.CoinValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Difficulty increases are always paid in Lightman's Currency.
 * Costs are priced in iron-coin <b>value</b> (configurable item id).
 * <p>
 * Payments use <b>inventory coins only</b> (not wallet or bank).
 * Higher denominations (gold, emerald, …) are accepted: inventory coins are
 * exchanged down, the value is taken, then leftover is exchanged back up as change.
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

    /**
     * Price as main-chain <b>core value</b> equal to {@code ironCoins} of the cost coin.
     * Gold/emerald/etc. count toward the same value and can make change.
     */
    private static MoneyValue coinPrice(long ironCoins) {
        long count = Math.max(1L, ironCoins);
        Item coin = resolveCostCoin();
        try {
            if (coin != null) {
                ChainData chain = CoinAPI.getApi().ChainDataOfCoin(coin);
                if (chain != null) {
                    long unit = Math.max(1L, chain.getCoreValue(coin));
                    long total = multiplyExactOrCap(unit, count);
                    MoneyValue priced = CoinValue.fromNumber(chain, total);
                    if (priced != null && !priced.isEmpty() && !priced.isInvalid()) {
                        return priced;
                    }
                }
                // Fallback: N of the coin item (still value-aware via getCoreValue)
                int coinCount = (int) Math.min(Integer.MAX_VALUE, count);
                MoneyValue itemPriced = CoinValue.fromItemOrValue(coin, coinCount, count);
                if (itemPriced != null && !itemPriced.isEmpty() && !itemPriced.isInvalid()) {
                    return itemPriced;
                }
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] coinPrice failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
        return CoinValue.fromNumber(CoinAPI.MAIN_CHAIN, count);
    }

    private static long multiplyExactOrCap(long unit, long count) {
        try {
            return Math.multiplyExact(unit, count);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE / 4L;
        }
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
            // Total inventory coin value (any denomination on the chain).
            return stored != null && stored.containsValue(price);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Lightman's canAfford failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    /**
     * Charge iron-coin value from inventory.
     * <ol>
     *   <li>Exchange higher coins down (gold/emerald → iron/copper)</li>
     *   <li>Extract the price (Lightman's also returns change if a larger coin is consumed)</li>
     *   <li>Exchange leftover back up so the player keeps tidy change</li>
     * </ol>
     */
    private static boolean chargeLightmans(ServerPlayer player, long ironCoins) {
        Inventory inv = player.m_150109_();
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

            // Break gold/emerald/etc. into lower coins so payment + change is reliable.
            exchangeAllDown(inv);

            // Re-resolve handler after inventory mutation.
            handler = inventoryHandler(player);
            if (handler == null) {
                exchangeAllUp(inv);
                return false;
            }
            stored = handler.getStoredMoney();
            if (stored == null || !stored.containsValue(price)) {
                exchangeAllUp(inv);
                return false;
            }

            MoneyValue simulated = handler.extractMoney(price, true);
            if (simulated != null && !simulated.isEmpty() && !simulated.isFree()) {
                exchangeAllUp(inv);
                return false;
            }
            MoneyValue leftover = handler.extractMoney(price, false);
            boolean ok = leftover == null || leftover.isEmpty() || leftover.isFree();
            // Recombine leftover copper/iron into higher coins (player's change).
            exchangeAllUp(inv);
            return ok;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Lightman's charge failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            try {
                exchangeAllUp(inv);
            } catch (Throwable ignored) {
            }
            return false;
        }
    }

    private static void exchangeAllDown(Inventory inv) {
        if (inv == null) {
            return;
        }
        try {
            CoinAPI.getApi().CoinExchangeAllDown(inv);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] CoinExchangeAllDown: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    private static void exchangeAllUp(Inventory inv) {
        if (inv == null) {
            return;
        }
        try {
            CoinAPI.getApi().CoinExchangeAllUp(inv);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] CoinExchangeAllUp: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
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
