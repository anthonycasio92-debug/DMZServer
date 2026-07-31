package com.dbzlegacy.adaptivedifficulty.currency;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import io.github.lightman314.lightmanscurrency.api.capability.money.IMoneyHandler;
import io.github.lightman314.lightmanscurrency.api.money.MoneyAPI;
import io.github.lightman314.lightmanscurrency.common.core.ModItems;
import io.github.lightman314.lightmanscurrency.common.items.AncientCoinItem;
import io.github.lightman314.lightmanscurrency.common.items.ancient_coins.AncientCoinType;
import io.github.lightman314.lightmanscurrency.common.money.ancient_money.AncientMoneyValue;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/**
 * V3 Ancient Coin economy backed by <b>real Lightman's {@code coin_ancient} items</b>.
 * <p>
 * Kill drops and bootstrap grants put physical coins into the player's inventory.
 * Activation / upgrades consume those inventory coins (our copper-value exchange rates).
 * The old NBT wallet is migrated once into real items, then cleared.
 */
public final class AncientCoinEconomy {
    private static final boolean LIGHTMANS = ModList.get().isLoaded("lightmanscurrency");
    private static volatile boolean warnedMissingLightmans;

    public enum CoinKind {
        COPPER(1L, "Copper", AncientCoinType.COPPER),
        IRON(10L, "Iron", AncientCoinType.IRON),
        GOLD(100L, "Gold", AncientCoinType.GOLD),
        DIAMOND(1_000L, "Diamond", AncientCoinType.DIAMOND),
        EMERALD(10_000L, "Emerald", AncientCoinType.EMERALD),
        NETHERITE(100_000L, "Netherite", AncientCoinType.NETHERITE_H),
        DIVINE(1_000_000L, "Divine", AncientCoinType.ENDER_PEARL);

        public final long copperValue;
        public final String display;
        public final AncientCoinType ancientType;

        CoinKind(long copperValue, String display, AncientCoinType ancientType) {
            this.copperValue = copperValue;
            this.display = display;
            this.ancientType = ancientType;
        }

        static CoinKind of(AncientCoinType type) {
            if (type == null) {
                return null;
            }
            for (CoinKind kind : values()) {
                if (kind.ancientType == type) {
                    return kind;
                }
            }
            // Other netherite letter coins count as Netherite value.
            String name = type.name();
            if (name != null && name.startsWith("NETHERITE")) {
                return NETHERITE;
            }
            return null;
        }
    }

    private AncientCoinEconomy() {}

    public static boolean realCoinsAvailable() {
        return LIGHTMANS;
    }

    public static long balance(ServerPlayer player) {
        if (player == null) {
            return 0L;
        }
        migrateWalletToItems(player);
        if (LIGHTMANS) {
            return inventoryCopper(player);
        }
        return DifficultyCache.data(player).getAncientCopper();
    }

    public static String balanceText(ServerPlayer player) {
        return format(balance(player));
    }

    public static String format(long copper) {
        if (copper >= CoinKind.DIVINE.copperValue) {
            return String.format("%.2f Divine", copper / (double) CoinKind.DIVINE.copperValue);
        }
        if (copper >= CoinKind.NETHERITE.copperValue) {
            return String.format("%.2f Netherite", copper / (double) CoinKind.NETHERITE.copperValue);
        }
        if (copper >= CoinKind.EMERALD.copperValue) {
            return String.format("%.2f Emerald", copper / (double) CoinKind.EMERALD.copperValue);
        }
        if (copper >= CoinKind.DIAMOND.copperValue) {
            return String.format("%.1f Diamond", copper / (double) CoinKind.DIAMOND.copperValue);
        }
        if (copper >= CoinKind.GOLD.copperValue) {
            return String.format("%.1f Gold", copper / (double) CoinKind.GOLD.copperValue);
        }
        if (copper >= CoinKind.IRON.copperValue) {
            return String.format("%.1f Iron", copper / (double) CoinKind.IRON.copperValue);
        }
        return copper + " Copper";
    }

    public static boolean canAfford(ServerPlayer player, long copperCost) {
        return copperCost <= 0L || balance(player) >= copperCost;
    }

    public static boolean charge(ServerPlayer player, long copperCost) {
        if (copperCost <= 0L) {
            return true;
        }
        if (player == null) {
            return false;
        }
        migrateWalletToItems(player);
        if (LIGHTMANS) {
            return chargeInventory(player, copperCost);
        }
        warnMissingLightmans();
        PlayerDifficultyData data = DifficultyCache.data(player);
        if (!data.spendAncientCopper(copperCost)) {
            return false;
        }
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        return true;
    }

    public static void grant(ServerPlayer player, long copper) {
        if (player == null || copper <= 0L) {
            return;
        }
        migrateWalletToItems(player);
        if (LIGHTMANS) {
            giveInventoryCoins(player, copper);
            DifficultyCache.refresh(player);
            return;
        }
        warnMissingLightmans();
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.addAncientCopper(copper);
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
    }

    /**
     * Roll an Ancient Coin drop for a kill. Higher active tiers unlock better coin kinds.
     */
    public static long rollKillDrop(ServerPlayer killer, long combatRating, boolean elite, boolean boss) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableAncientCoinDrops) {
            return 0L;
        }
        PlayerDifficultyData data = DifficultyCache.data(killer);
        int tier = data.getActiveTier();
        // Inactive players still earn a copper trickle so the economy can bootstrap.
        if (tier <= 0) {
            long trickle = Math.max(1L, Math.min(25L, snaplessLevel(killer) / 20L));
            return Math.round(trickle * Math.max(0.25, cfg.ancientCoinDropMult));
        }
        CoinKind kind = rollKind(tier);
        double mult = 1.0 + Math.min(4.0, combatRating / Math.max(1.0, cfg.ancientCoinRatingDivisor));
        if (elite) {
            mult *= Math.max(1.0, cfg.eliteRewardBonus);
        }
        if (boss) {
            mult *= 2.5;
        }
        long base = Math.max(1L, Math.round(kind.copperValue * cfg.ancientCoinDropMult * mult));
        double jitter = 0.85 + ThreadLocalRandom.current().nextDouble() * 0.30;
        return Math.max(1L, Math.round(base * jitter));
    }

    public static CoinKind rollKind(int activeTier) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        return switch (Math.max(1, activeTier)) {
            case 1 -> CoinKind.COPPER;
            case 2 -> rng.nextDouble() < 0.25 ? CoinKind.IRON : CoinKind.COPPER;
            case 3 -> rng.nextDouble() < 0.30 ? CoinKind.GOLD : (rng.nextDouble() < 0.55 ? CoinKind.IRON : CoinKind.COPPER);
            case 4 -> rng.nextDouble() < 0.25 ? CoinKind.DIAMOND : (rng.nextDouble() < 0.55 ? CoinKind.GOLD : CoinKind.IRON);
            case 5 -> rng.nextDouble() < 0.22 ? CoinKind.EMERALD : (rng.nextDouble() < 0.55 ? CoinKind.DIAMOND : CoinKind.GOLD);
            case 6 -> rng.nextDouble() < 0.18 ? CoinKind.NETHERITE : (rng.nextDouble() < 0.50 ? CoinKind.EMERALD : CoinKind.DIAMOND);
            default -> rng.nextDouble() < 0.12 ? CoinKind.DIVINE : (rng.nextDouble() < 0.45 ? CoinKind.NETHERITE : CoinKind.EMERALD);
        };
    }

    public static void notifyGrant(ServerPlayer player, long copper) {
        if (player == null || copper <= 0L) {
            return;
        }
        player.m_213846_(Component.m_237113_("§6+ " + format(copper) + " §7Ancient Coins"));
    }

    public static long activationCost(UnlockTier tier) {
        return tier == null ? 0L : tier.activationCost();
    }

    /** Convert any leftover NBT wallet into real inventory coins (once). */
    public static void migrateWalletToItems(ServerPlayer player) {
        if (player == null || !LIGHTMANS) {
            return;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        long wallet = data.getAncientCopper();
        if (wallet <= 0L) {
            return;
        }
        data.setAncientCopper(0L);
        DifficultyCache.save(player);
        giveInventoryCoins(player, wallet);
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] migrated {} Ancient Coin wallet units → real Lightman's coins for {}",
                AdaptiveDifficultyMod.MOD_ID,
                wallet,
                player.m_20148_() // UUID
        );
    }

    // ── Inventory helpers ──────────────────────────────────────────────────

    private static long inventoryCopper(ServerPlayer player) {
        long total = 0L;
        Inventory inv = player.m_150109_();
        for (int i = 0; i < inv.m_6643_(); i++) {
            ItemStack stack = inv.m_8020_(i);
            CoinKind kind = kindOf(stack);
            if (kind == null) {
                continue;
            }
            total = safeAdd(total, safeMul(kind.copperValue, stack.m_41613_()));
        }
        return total;
    }

    private static boolean chargeInventory(ServerPlayer player, long copperCost) {
        long have = inventoryCopper(player);
        if (have < copperCost) {
            return false;
        }
        // Prefer Lightman's money handler when it accepts ancient values.
        if (tryChargeViaHandler(player, copperCost)) {
            DifficultyCache.refresh(player);
            return true;
        }
        // Manual remove + change (supports our cross-type copper exchange).
        long removed = removeCoinsForValue(player, copperCost);
        if (removed < copperCost) {
            // Shouldn't happen after balance check — refund what we took.
            if (removed > 0L) {
                giveInventoryCoins(player, removed);
            }
            return false;
        }
        long change = removed - copperCost;
        if (change > 0L) {
            giveInventoryCoins(player, change);
        }
        DifficultyCache.refresh(player);
        return true;
    }

    private static boolean tryChargeViaHandler(ServerPlayer player, long copperCost) {
        // Only works when the entire cost is paid as Copper-type ancient coins.
        // Used as a fast path when the player already holds enough Copper ancients.
        try {
            MoneyValueOrEmpty price = ancientPrice(CoinKind.COPPER, copperCost);
            if (price.value == null) {
                return false;
            }
            IMoneyHandler handler = MoneyAPI.getApi().GetContainersMoneyHandler(player.m_150109_(), player);
            if (handler == null) {
                return false;
            }
            if (!handler.getStoredMoney().containsValue(price.value)) {
                return false;
            }
            var leftover = handler.extractMoney(price.value, false);
            return leftover == null || leftover.isEmpty() || leftover.isFree();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static long removeCoinsForValue(ServerPlayer player, long need) {
        Inventory inv = player.m_150109_();
        // Highest first so change math stays simple.
        CoinKind[] order = {
                CoinKind.DIVINE, CoinKind.NETHERITE, CoinKind.EMERALD,
                CoinKind.DIAMOND, CoinKind.GOLD, CoinKind.IRON, CoinKind.COPPER
        };
        long removed = 0L;
        for (CoinKind kind : order) {
            if (removed >= need) {
                break;
            }
            for (int i = 0; i < inv.m_6643_() && removed < need; i++) {
                ItemStack stack = inv.m_8020_(i);
                if (kindOf(stack) != kind) {
                    continue;
                }
                int count = stack.m_41613_();
                if (count <= 0) {
                    continue;
                }
                long unit = kind.copperValue;
                long stillNeed = need - removed;
                long takeUnits = (stillNeed + unit - 1L) / unit; // ceil
                int take = (int) Math.min(count, Math.min(Integer.MAX_VALUE, takeUnits));
                if (take <= 0) {
                    continue;
                }
                stack.m_41774_(take); // shrink
                if (stack.m_41619_()) {
                    inv.m_6836_(i, ItemStack.f_41583_); // EMPTY
                }
                removed = safeAdd(removed, safeMul(unit, take));
            }
        }
        return removed;
    }

    private static void giveInventoryCoins(ServerPlayer player, long copper) {
        if (copper <= 0L) {
            return;
        }
        Map<CoinKind, Long> bags = splitToKinds(copper);
        // Prefer money-handler insert (places tidy stacks); fall back to add/drop.
        boolean usedHandler = tryInsertViaHandler(player, bags);
        if (usedHandler) {
            return;
        }
        for (Map.Entry<CoinKind, Long> e : bags.entrySet()) {
            giveStacks(player, e.getKey(), e.getValue());
        }
    }

    private static boolean tryInsertViaHandler(ServerPlayer player, Map<CoinKind, Long> bags) {
        try {
            IMoneyHandler handler = MoneyAPI.getApi().GetContainersMoneyHandler(player.m_150109_(), player);
            if (handler == null) {
                return false;
            }
            List<ItemStack> overflow = new ArrayList<>();
            for (Map.Entry<CoinKind, Long> e : bags.entrySet()) {
                long remaining = e.getValue();
                while (remaining > 0L) {
                    long chunk = Math.min(remaining, 64L);
                    MoneyValueOrEmpty mv = ancientPrice(e.getKey(), chunk);
                    if (mv.value == null) {
                        return false;
                    }
                    var leftover = handler.insertMoney(mv.value, false);
                    if (leftover != null && !leftover.isEmpty() && !leftover.isFree()) {
                        // Handler couldn't take it — drop physical stacks for the remainder.
                        overflow.add(e.getKey().ancientType.asItem((int) chunk));
                    }
                    remaining -= chunk;
                }
            }
            for (ItemStack stack : overflow) {
                dropOrAdd(player, stack);
            }
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] ancient insertMoney failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    private static void giveStacks(ServerPlayer player, CoinKind kind, long count) {
        long left = count;
        while (left > 0L) {
            int chunk = (int) Math.min(64L, left);
            dropOrAdd(player, kind.ancientType.asItem(chunk));
            left -= chunk;
        }
    }

    private static void dropOrAdd(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return;
        }
        ItemStack copy = stack.m_41777_(); // copy
        if (player.m_150109_().m_36054_(copy)) { // add
            return;
        }
        ItemEntity dropped = player.m_36176_(stack, false); // drop
        if (dropped != null) {
            dropped.m_32060_(); // setNoPickUpDelay
        }
    }

    private static Map<CoinKind, Long> splitToKinds(long copper) {
        Map<CoinKind, Long> out = new EnumMap<>(CoinKind.class);
        long left = Math.max(0L, copper);
        CoinKind[] order = {
                CoinKind.DIVINE, CoinKind.NETHERITE, CoinKind.EMERALD,
                CoinKind.DIAMOND, CoinKind.GOLD, CoinKind.IRON, CoinKind.COPPER
        };
        for (CoinKind kind : order) {
            if (left < kind.copperValue) {
                continue;
            }
            long n = left / kind.copperValue;
            if (n > 0L) {
                out.put(kind, n);
                left -= n * kind.copperValue;
            }
        }
        if (left > 0L) {
            out.merge(CoinKind.COPPER, left, Long::sum);
        }
        return out;
    }

    private static CoinKind kindOf(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return null;
        }
        try {
            Item ancient = ModItems.COIN_ANCIENT.get();
            if (!stack.m_150930_(ancient)) {
                return null;
            }
            return CoinKind.of(AncientCoinItem.getAncientCoinType(stack));
        } catch (Throwable t) {
            return null;
        }
    }

    private static MoneyValueOrEmpty ancientPrice(CoinKind kind, long count) {
        try {
            if (kind == null || count <= 0L) {
                return MoneyValueOrEmpty.EMPTY;
            }
            return new MoneyValueOrEmpty(AncientMoneyValue.of(kind.ancientType, count));
        } catch (Throwable t) {
            return MoneyValueOrEmpty.EMPTY;
        }
    }

    private static final class MoneyValueOrEmpty {
        static final MoneyValueOrEmpty EMPTY = new MoneyValueOrEmpty(null);
        final io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue value;

        MoneyValueOrEmpty(io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue value) {
            this.value = value;
        }
    }

    private static void warnMissingLightmans() {
        if (warnedMissingLightmans) {
            return;
        }
        warnedMissingLightmans = true;
        AdaptiveDifficultyMod.LOGGER.warn(
                "[{}] Lightman's Currency not loaded — Ancient Coins falling back to NBT wallet (fake). "
                        + "Install lightmanscurrency for real coin_ancient items.",
                AdaptiveDifficultyMod.MOD_ID
        );
    }

    private static long snaplessLevel(ServerPlayer player) {
        try {
            return Math.max(1, com.dbzlegacy.adaptivedifficulty.calc.DmzProgression.dmzLevel(player));
        } catch (Throwable t) {
            return 1L;
        }
    }

    private static long safeAdd(long a, long b) {
        long r = a + b;
        if (((a ^ r) & (b ^ r)) < 0L) {
            return Long.MAX_VALUE / 4L;
        }
        return Math.max(0L, r);
    }

    private static long safeMul(long a, long b) {
        if (a == 0L || b == 0L) {
            return 0L;
        }
        try {
            return Math.multiplyExact(a, b);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE / 4L;
        }
    }
}
