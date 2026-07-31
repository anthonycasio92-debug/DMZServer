package com.dbzlegacy.adaptivedifficulty.currency;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import io.github.lightman314.lightmanscurrency.common.core.ModItems;
import io.github.lightman314.lightmanscurrency.common.items.AncientCoinItem;
import io.github.lightman314.lightmanscurrency.common.items.ancient_coins.AncientCoinType;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

/**
 * Ancient Coin economy — real Lightman's {@code coin_ancient} items only.
 * <p>
 * Charges take an <b>exact</b> copper total from available denominations and
 * never consume a higher coin that would overshoot. If the player cannot make
 * exact payment, the charge fails and callers report how much is missing.
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
        LAPIS(500_000L, "Lapis", AncientCoinType.LAPIS),
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
            String name = type.name();
            if (name != null && name.startsWith("NETHERITE")) {
                return NETHERITE;
            }
            if ("LAPIS".equals(name)) {
                return LAPIS;
            }
            if ("ENDER_PEARL".equals(name)) {
                return DIVINE;
            }
            return null;
        }
    }

    /** Exact drop: one coin kind + count (no copper-value conversion). */
    public record Drop(CoinKind kind, int count) {
        public Drop {
            kind = kind == null ? CoinKind.COPPER : kind;
            count = Math.max(0, count);
        }

        public long copperValue() {
            return Math.max(0L, kind.copperValue * (long) count);
        }

        public String display() {
            if (count <= 0) {
                return "nothing";
            }
            return count + "× " + kind.display;
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
        return inventoryBreakdown(player);
    }

    /**
     * Per-type Ancient Coin counts in inventory.
     * Always lists every Lightman's Ancient type the mod recognizes (incl. 0s).
     */
    public static String inventoryBreakdown(ServerPlayer player) {
        if (player == null || !LIGHTMANS) {
            return format(balance(player));
        }
        long[] counts = countByKind(player);
        StringBuilder sb = new StringBuilder("§f");
        boolean first = true;
        for (CoinKind kind : CoinKind.values()) {
            if (!first) {
                sb.append(" §8· §f");
            }
            first = false;
            sb.append(kind.display).append(' ').append(counts[kind.ordinal()]);
        }
        return sb.toString();
    }

    /** Alias used by some GUI/chat bindings. */
    public static String formatCoins(long copper) {
        return formatExactCost(copper);
    }

    public static long countOf(ServerPlayer player, CoinKind kind) {
        if (player == null || kind == null) {
            return 0L;
        }
        return countByKind(player)[kind.ordinal()];
    }

    public static String format(long copper) {
        if (copper <= 0L) {
            return "0 Copper";
        }
        if (copper >= CoinKind.DIVINE.copperValue && copper % CoinKind.DIVINE.copperValue == 0L) {
            return (copper / CoinKind.DIVINE.copperValue) + "× Divine";
        }
        if (copper >= CoinKind.LAPIS.copperValue && copper % CoinKind.LAPIS.copperValue == 0L) {
            return (copper / CoinKind.LAPIS.copperValue) + "× Lapis";
        }
        if (copper >= CoinKind.NETHERITE.copperValue && copper % CoinKind.NETHERITE.copperValue == 0L) {
            return (copper / CoinKind.NETHERITE.copperValue) + "× Netherite";
        }
        if (copper >= CoinKind.EMERALD.copperValue && copper % CoinKind.EMERALD.copperValue == 0L) {
            return (copper / CoinKind.EMERALD.copperValue) + "× Emerald";
        }
        if (copper >= CoinKind.DIAMOND.copperValue && copper % CoinKind.DIAMOND.copperValue == 0L) {
            return (copper / CoinKind.DIAMOND.copperValue) + "× Diamond";
        }
        if (copper >= CoinKind.GOLD.copperValue && copper % CoinKind.GOLD.copperValue == 0L) {
            return (copper / CoinKind.GOLD.copperValue) + "× Gold";
        }
        if (copper >= CoinKind.IRON.copperValue && copper % CoinKind.IRON.copperValue == 0L) {
            return (copper / CoinKind.IRON.copperValue) + "× Iron";
        }
        return copper + "× Copper";
    }

    /** Format a tier activation cost as an exact coin shopping list. */
    public static String formatExactCost(long copperCost) {
        if (copperCost <= 0L) {
            return "free";
        }
        // Prefer a single clean denomination when the cost divides evenly.
        CoinKind[] order = {
                CoinKind.DIVINE, CoinKind.LAPIS, CoinKind.NETHERITE, CoinKind.EMERALD,
                CoinKind.DIAMOND, CoinKind.GOLD, CoinKind.IRON, CoinKind.COPPER
        };
        for (CoinKind kind : order) {
            if (copperCost >= kind.copperValue && copperCost % kind.copperValue == 0L) {
                long n = copperCost / kind.copperValue;
                return n + "× " + kind.display + " Ancient";
            }
        }
        return copperCost + "× Copper Ancient";
    }

    public static boolean canAfford(ServerPlayer player, long copperCost) {
        if (copperCost <= 0L) {
            return true;
        }
        if (player == null) {
            return false;
        }
        migrateWalletToItems(player);
        if (LIGHTMANS) {
            return planExactPayment(countByKind(player), copperCost) != null;
        }
        return balance(player) >= copperCost;
    }

    /** Human-readable shortfall when {@link #canAfford} is false. */
    public static String missingText(ServerPlayer player, long copperCost) {
        if (copperCost <= 0L) {
            return "free";
        }
        long have = balance(player);
        long missing = Math.max(0L, copperCost - have);
        if (missing <= 0L && !canAfford(player, copperCost)) {
            // Has enough copper value but cannot make exact change without overpaying.
            return "Need exactly " + formatExactCost(copperCost)
                    + " — break coins into smaller Ancient types (have "
                    + inventoryBreakdown(player) + ")";
        }
        return "Need " + formatExactCost(copperCost)
                + " — missing " + formatExactCost(missing)
                + " (have " + inventoryBreakdown(player) + ")";
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
            return chargeExact(player, copperCost);
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

    /** Grant exact copper-value as Copper ancient coins only (bootstrap / migration). */
    public static void grantCopperExact(ServerPlayer player, long copperCount) {
        if (player == null || copperCount <= 0L) {
            return;
        }
        grantExact(player, CoinKind.COPPER, copperCount);
    }

    /** Put exactly {@code count} coins of {@code kind} into inventory. No splitting / change. */
    public static void grantExact(ServerPlayer player, CoinKind kind, long count) {
        if (player == null || kind == null || count <= 0L) {
            return;
        }
        migrateWalletToItems(player);
        if (LIGHTMANS) {
            giveStacks(player, kind, count);
            DifficultyCache.refresh(player);
            return;
        }
        warnMissingLightmans();
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.addAncientCopper(safeMul(kind.copperValue, count));
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
    }

    /**
     * Spawn exact Ancient Coin item stacks in the world at the killed mob.
     * Does <b>not</b> put coins into the player's inventory.
     */
    public static void dropInWorld(LivingEntity at, Drop drop) {
        if (at == null || drop == null || drop.count() <= 0 || drop.kind() == null) {
            return;
        }
        if (!LIGHTMANS) {
            warnMissingLightmans();
            return;
        }
        Level level = at.m_9236_();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        double x = at.m_20185_();
        double y = at.m_20186_() + 0.35;
        double z = at.m_20189_();
        long left = drop.count();
        while (left > 0L) {
            int chunk = (int) Math.min(64L, left);
            ItemStack stack = drop.kind().ancientType.asItem(chunk);
            if (stack == null || stack.m_41619_()) {
                break;
            }
            ItemEntity entity = new ItemEntity(server, x, y, z, stack);
            // Short pickup delay so nearby killer can grab it, but still a world drop.
            entity.m_32061_(); // setDefaultPickUpDelay (10 ticks)
            double spread = 0.12;
            entity.m_20334_(
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * spread,
                    0.12 + ThreadLocalRandom.current().nextDouble() * 0.08,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * spread
            );
            server.m_7967_(entity); // addFreshEntity
            left -= chunk;
        }
    }

    /** @deprecated Kill rewards must use {@link #dropInWorld}; kept for non-kill grants only. */
    @Deprecated
    public static void grantDrop(ServerPlayer player, Drop drop) {
        if (player == null || drop == null || drop.count() <= 0) {
            return;
        }
        grantExact(player, drop.kind(), drop.count());
    }

    /**
     * Roll an exact Ancient Coin drop. Returns the coin type + count to spawn at the mob.
     */
    public static Drop rollKillDrop(ServerPlayer killer, long combatRating, boolean elite, boolean boss) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableAncientCoinDrops) {
            return new Drop(CoinKind.COPPER, 0);
        }
        PlayerDifficultyData data = DifficultyCache.data(killer);
        int tier = data.getActiveTier();
        if (tier <= 0) {
            // Inactive: always 1× Copper (exact).
            return new Drop(CoinKind.COPPER, 1);
        }
        CoinKind kind = rollKind(tier);
        int count = 1;
        if (elite) {
            count += 1;
        }
        if (boss) {
            count += 2;
        }
        // Optional mult: chance for +1 extra of the same kind (never converts denomination).
        double mult = Math.max(0.0, cfg.ancientCoinDropMult);
        if (mult > 1.0 && ThreadLocalRandom.current().nextDouble() < Math.min(0.75, (mult - 1.0) * 0.35)) {
            count += 1;
        }
        if (combatRating > cfg.ancientCoinRatingDivisor
                && ThreadLocalRandom.current().nextDouble() < 0.15) {
            count += 1;
        }
        return new Drop(kind, Math.max(1, count));
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

    public static void notifyGrant(ServerPlayer player, Drop drop) {
        if (player == null || drop == null || drop.count() <= 0) {
            return;
        }
        player.m_213846_(Component.m_237113_("§6Dropped " + drop.display() + " §7Ancient Coin"
                + (drop.count() == 1 ? "" : "s")));
    }

    public static void notifyGrant(ServerPlayer player, long copper) {
        if (player == null || copper <= 0L) {
            return;
        }
        player.m_213846_(Component.m_237113_("§6+ " + formatExactCost(copper)));
    }

    public static long activationCost(UnlockTier tier) {
        return tier == null ? 0L : tier.activationCost();
    }

    public static long activationCost(UnlockTier tier, ServerPlayer player) {
        if (tier == null) {
            return 0L;
        }
        int level = player == null ? 0
                : com.dbzlegacy.adaptivedifficulty.calc.DmzProgression.dmzLevel(player);
        return tier.activationCostForLevel(level);
    }

    /** Convert any leftover NBT wallet into exact Copper ancient coins (once). */
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
        giveStacks(player, CoinKind.COPPER, wallet);
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] migrated {} wallet units → {}× Copper Ancient for {}",
                AdaptiveDifficultyMod.MOD_ID,
                wallet,
                wallet,
                player.m_20148_()
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

    private static long[] countByKind(ServerPlayer player) {
        long[] counts = new long[CoinKind.values().length];
        if (player == null) {
            return counts;
        }
        Inventory inv = player.m_150109_();
        for (int i = 0; i < inv.m_6643_(); i++) {
            CoinKind kind = kindOf(inv.m_8020_(i));
            if (kind == null) {
                continue;
            }
            counts[kind.ordinal()] = safeAdd(counts[kind.ordinal()], inv.m_8020_(i).m_41613_());
        }
        return counts;
    }

    /**
     * Plan an exact payment (no overpay). Returns take-counts per CoinKind ordinal, or null.
     */
    private static long[] planExactPayment(long[] available, long copperCost) {
        if (copperCost <= 0L) {
            return new long[CoinKind.values().length];
        }
        long[] plan = new long[CoinKind.values().length];
        long remaining = copperCost;
        // Largest first that still fits exactly into the remainder.
        CoinKind[] highFirst = {
                CoinKind.DIVINE, CoinKind.LAPIS, CoinKind.NETHERITE, CoinKind.EMERALD,
                CoinKind.DIAMOND, CoinKind.GOLD, CoinKind.IRON, CoinKind.COPPER
        };
        for (CoinKind kind : highFirst) {
            long unit = kind.copperValue;
            if (unit <= 0L || remaining < unit) {
                continue;
            }
            long canTake = Math.min(available[kind.ordinal()], remaining / unit);
            plan[kind.ordinal()] = canTake;
            remaining -= canTake * unit;
        }
        return remaining == 0L ? plan : null;
    }

    /** Remove an exact copper total. Never takes a higher coin that would overshoot. */
    private static boolean chargeExact(ServerPlayer player, long copperCost) {
        long[] available = countByKind(player);
        long[] plan = planExactPayment(available, copperCost);
        if (plan == null) {
            return false;
        }
        Inventory inv = player.m_150109_();
        for (CoinKind kind : CoinKind.values()) {
            long need = plan[kind.ordinal()];
            if (need <= 0L) {
                continue;
            }
            for (int i = 0; i < inv.m_6643_() && need > 0L; i++) {
                ItemStack stack = inv.m_8020_(i);
                if (kindOf(stack) != kind) {
                    continue;
                }
                int count = stack.m_41613_();
                int take = (int) Math.min(count, Math.min(Integer.MAX_VALUE, need));
                if (take <= 0) {
                    continue;
                }
                stack.m_41774_(take);
                if (stack.m_41619_()) {
                    inv.m_6836_(i, ItemStack.f_41583_);
                }
                need -= take;
            }
            if (need > 0L) {
                return false;
            }
        }
        DifficultyCache.refresh(player);
        return true;
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
        ItemStack copy = stack.m_41777_();
        if (player.m_150109_().m_36054_(copy)) {
            return;
        }
        ItemEntity dropped = player.m_36176_(stack, false);
        if (dropped != null) {
            dropped.m_32060_();
        }
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

    private static void warnMissingLightmans() {
        if (warnedMissingLightmans) {
            return;
        }
        warnedMissingLightmans = true;
        AdaptiveDifficultyMod.LOGGER.warn(
                "[{}] Lightman's Currency not loaded — Ancient Coins falling back to NBT wallet. "
                        + "Install lightmanscurrency for real coin_ancient items.",
                AdaptiveDifficultyMod.MOD_ID
        );
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
