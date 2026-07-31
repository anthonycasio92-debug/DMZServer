package com.dbzlegacy.adaptivedifficulty.currency;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Thin facade over {@link AncientCoinEconomy}.
 * Legacy iron-coin / main-chain payment helpers were removed in V3.1.
 */
public final class CurrencyBridge {
    private static final boolean LIGHTMANS_LOADED = ModList.get().isLoaded("lightmanscurrency");

    private CurrencyBridge() {}

    public static boolean lightmansAvailable() {
        return LIGHTMANS_LOADED;
    }

    public static String activeMode() {
        return AncientCoinEconomy.realCoinsAvailable() ? "ancient_coins_items" : "unavailable";
    }

    public static boolean canAfford(ServerPlayer player, long copperCost) {
        return AncientCoinEconomy.canAfford(player, copperCost);
    }

    public static boolean charge(ServerPlayer player, long copperCost) {
        return AncientCoinEconomy.charge(player, copperCost);
    }

    public static void grant(ServerPlayer player, long copper) {
        AncientCoinEconomy.grantCopperExact(player, copper);
    }

    public static String balanceText(ServerPlayer player) {
        return AncientCoinEconomy.balanceText(player);
    }

    public static String formatCost(long copperCost) {
        return AncientCoinEconomy.formatExactCost(copperCost);
    }

    public static String currencyLabel() {
        return AncientCoinEconomy.realCoinsAvailable()
                ? "Ancient Coins (inventory)"
                : "Ancient Coins";
    }
}
