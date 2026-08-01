package com.dbzlegacy.adaptivedifficulty.currency;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import io.github.lightman314.lightmanscurrency.common.core.ModItems;
import io.github.lightman314.lightmanscurrency.common.items.AncientCoinItem;
import io.github.lightman314.lightmanscurrency.common.items.ancient_coins.AncientCoinType;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
 * Ladder (cheap → expensive): Copper → Iron → Gold → Emerald → Diamond →
 * Netherite (all 9 letter variants share one value). Lapis and Ender Pearl
 * ancient coins are ignored (never spent, counted, granted, or dropped).
 * <p>
 * Charges take an <b>exact</b> copper total from available denominations and
 * never consume a higher coin that would overshoot. If the player cannot make
 * exact payment, the charge fails and callers report how much is missing.
 */
public final class AncientCoinEconomy {
    private static final boolean LIGHTMANS = ModList.get().isLoaded("lightmanscurrency");
    private static volatile boolean warnedMissingLightmans;
    /** One-shot NBT wallet → inventory migrate per login session. */
    private static final Set<UUID> MIGRATED = ConcurrentHashMap.newKeySet();

    /**
     * Max coins of one denomination for a tier price. Beyond this the cost
     * promotes to the next Ancient Coin type (Copper→…→Netherite), rounding up.
     */
    public static final int MAX_COINS_PER_TYPE = 128;

    /** All Lightman's Netherite letter ancients (HEROBRINE) — equal value. */
    private static final AncientCoinType[] NETHERITE_VARIANTS = {
            AncientCoinType.NETHERITE_H,
            AncientCoinType.NETHERITE_E1,
            AncientCoinType.NETHERITE_R1,
            AncientCoinType.NETHERITE_O,
            AncientCoinType.NETHERITE_B,
            AncientCoinType.NETHERITE_R2,
            AncientCoinType.NETHERITE_I,
            AncientCoinType.NETHERITE_N,
            AncientCoinType.NETHERITE_E2
    };

    public enum CoinKind {
        COPPER(1L, "Copper", AncientCoinType.COPPER),
        IRON(10L, "Iron", AncientCoinType.IRON),
        GOLD(100L, "Gold", AncientCoinType.GOLD),
        EMERALD(1_000L, "Emerald", AncientCoinType.EMERALD),
        DIAMOND(10_000L, "Diamond", AncientCoinType.DIAMOND),
        NETHERITE(100_000L, "Netherite", AncientCoinType.NETHERITE_H);

        public final long copperValue;
        public final String display;
        public final AncientCoinType ancientType;

        CoinKind(long copperValue, String display, AncientCoinType ancientType) {
            this.copperValue = copperValue;
            this.display = display;
            this.ancientType = ancientType;
        }

        /** Payment / balance order, highest value first. */
        static CoinKind[] highToLow() {
            return new CoinKind[] {NETHERITE, DIAMOND, EMERALD, GOLD, IRON, COPPER};
        }

        /** Ladder order, cheapest first. */
        static CoinKind[] lowToHigh() {
            return new CoinKind[] {COPPER, IRON, GOLD, EMERALD, DIAMOND, NETHERITE};
        }

        static CoinKind of(AncientCoinType type) {
            if (type == null) {
                return null;
            }
            // Lapis / Ender Pearl ancients are intentionally unused by this mod.
            String name = type.name();
            if ("LAPIS".equals(name) || "ENDER_PEARL".equals(name)) {
                return null;
            }
            if (name != null && name.startsWith("NETHERITE")) {
                return NETHERITE;
            }
            for (CoinKind kind : values()) {
                if (kind.ancientType == type) {
                    return kind;
                }
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
        CoinKind kind = preferredKind(copper);
        long n = copper / kind.copperValue;
        if (copper % kind.copperValue == 0L) {
            return n + "× " + kind.display;
        }
        return copper + "× Copper";
    }

    /** Format a tier activation cost as an exact coin shopping list. */
    public static String formatExactCost(long copperCost) {
        if (copperCost <= 0L) {
            return "free";
        }
        long normalized = normalizeCost(copperCost);
        CoinKind kind = preferredKind(normalized);
        long n = normalized / kind.copperValue;
        return n + "× " + kind.display + " Ancient";
    }

    /**
     * Snap a copper-value cost so it is payable as ≤{@link #MAX_COINS_PER_TYPE}
     * of a single Ancient Coin type. When more would be required, promote to the
     * next ladder step (rounding up).
     */
    public static long normalizeCost(long copperCost) {
        if (copperCost <= 0L) {
            return 0L;
        }
        long value = copperCost;
        CoinKind[] ladder = CoinKind.lowToHigh();
        for (int i = 0; i < ladder.length; i++) {
            long unit = ladder[i].copperValue;
            long count = (value + unit - 1L) / unit; // ceil
            if (count <= MAX_COINS_PER_TYPE || i == ladder.length - 1) {
                return safeMul(count, unit);
            }
            // Keep the ceiled copper value while promoting.
            value = safeMul(count, unit);
        }
        return value;
    }

    /** Best single denomination for a (preferably normalized) copper total. */
    private static CoinKind preferredKind(long copperCost) {
        if (copperCost <= 0L) {
            return CoinKind.COPPER;
        }
        CoinKind fallback = CoinKind.COPPER;
        for (CoinKind kind : CoinKind.highToLow()) {
            if (copperCost % kind.copperValue != 0L) {
                continue;
            }
            long n = copperCost / kind.copperValue;
            if (n <= 0L) {
                continue;
            }
            fallback = kind;
            if (n <= MAX_COINS_PER_TYPE) {
                return kind;
            }
        }
        return fallback;
    }

    public static boolean canAfford(ServerPlayer player, long copperCost) {
        if (copperCost <= 0L) {
            return true;
        }
        if (player == null) {
            return false;
        }
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

    /** Hard safety cap — kill rolls never exceed this many coins per death. */
    private static final int MAX_KILL_DROP_COUNT = 8;

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
        // Clamp runaway counts — kill rewards are tiny (1–5); never spawn thousands.
        long left = Math.min(MAX_KILL_DROP_COUNT, drop.count());
        // Netherite kill drops: one random HEROBRINE letter per coin.
        if (drop.kind() == CoinKind.NETHERITE) {
            while (left > 0L) {
                AncientCoinType variant = NETHERITE_VARIANTS[
                        ThreadLocalRandom.current().nextInt(NETHERITE_VARIANTS.length)];
                ItemStack stack = variant.asItem(1);
                if (stack == null || stack.m_41619_()) {
                    break;
                }
                spawnDropEntity(server, x, y, z, stack);
                left -= 1L;
            }
            return;
        }
        while (left > 0L) {
            int chunk = (int) Math.min(64L, left);
            ItemStack stack = drop.kind().ancientType.asItem(chunk);
            if (stack == null || stack.m_41619_()) {
                break;
            }
            spawnDropEntity(server, x, y, z, stack);
            left -= chunk;
        }
    }

    private static void spawnDropEntity(ServerLevel server, double x, double y, double z, ItemStack stack) {
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
     * Requires an active Unlock Tier — inactive players mint nothing.
     */
    public static Drop rollKillDrop(ServerPlayer killer, long combatRating, boolean elite, boolean boss) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableAncientCoinDrops || killer == null) {
            return new Drop(CoinKind.COPPER, 0);
        }
        PlayerDifficultyData data = DifficultyCache.data(killer);
        int tier = data.getActiveTier();
        if (tier <= 0) {
            return new Drop(CoinKind.COPPER, 0);
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
        return new Drop(kind, Math.min(MAX_KILL_DROP_COUNT, Math.max(1, count)));
    }

    public static CoinKind rollKind(int activeTier) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        // Ladder: Copper < Iron < Gold < Emerald < Diamond < Netherite
        return switch (Math.max(1, activeTier)) {
            case 1 -> CoinKind.COPPER;
            case 2 -> rng.nextDouble() < 0.25 ? CoinKind.IRON : CoinKind.COPPER;
            case 3 -> rng.nextDouble() < 0.30 ? CoinKind.GOLD : (rng.nextDouble() < 0.55 ? CoinKind.IRON : CoinKind.COPPER);
            case 4 -> rng.nextDouble() < 0.25 ? CoinKind.EMERALD : (rng.nextDouble() < 0.55 ? CoinKind.GOLD : CoinKind.IRON);
            case 5 -> rng.nextDouble() < 0.22 ? CoinKind.DIAMOND : (rng.nextDouble() < 0.55 ? CoinKind.EMERALD : CoinKind.GOLD);
            case 6 -> rng.nextDouble() < 0.18 ? CoinKind.NETHERITE : (rng.nextDouble() < 0.50 ? CoinKind.DIAMOND : CoinKind.EMERALD);
            default -> rng.nextDouble() < 0.35 ? CoinKind.NETHERITE : (rng.nextDouble() < 0.55 ? CoinKind.DIAMOND : CoinKind.EMERALD);
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
        return tier == null ? 0L : normalizeCost(tier.activationCost());
    }

    public static long activationCost(UnlockTier tier, ServerPlayer player) {
        if (tier == null) {
            return 0L;
        }
        int level = player == null ? 0
                : com.dbzlegacy.adaptivedifficulty.calc.DmzProgression.dmzLevel(player);
        return normalizeCost(tier.activationCostForLevel(level));
    }

    /**
     * Max copper-value converted from legacy NBT wallet per login.
     * Old {@code purchased} difficulty points were never coin counts — uncapped
     * migration previously dumped tens of thousands of Copper items into the world.
     */
    private static final long MAX_MIGRATE_COPPER = 500_000L; // 5× Netherite value

    /**
     * Convert leftover NBT wallet into mixed Ancient Coin denominations in inventory.
     * Runs at most once per login session. Never world-dumps overflow.
     */
    public static void migrateWalletToItems(ServerPlayer player) {
        if (player == null || !LIGHTMANS) {
            return;
        }
        UUID id = player.m_20148_();
        if (!MIGRATED.add(id)) {
            return;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        long wallet = data.getAncientCopper();
        if (wallet <= 0L) {
            return;
        }
        long toGrant = Math.min(wallet, MAX_MIGRATE_COPPER);
        long leftover = wallet - toGrant;
        data.setAncientCopper(leftover);
        DifficultyCache.save(player);
        long placed = grantMixedCopperValueInventoryOnly(player, toGrant);
        long unplaced = Math.max(0L, toGrant - placed);
        if (unplaced > 0L) {
            // Inventory full — keep remainder in NBT instead of littering the world.
            data.setAncientCopper(data.getAncientCopper() + unplaced);
            DifficultyCache.save(player);
        }
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] migrated wallet {} → {} copper-value Ancient coins for {} (leftover NBT={})",
                AdaptiveDifficultyMod.MOD_ID,
                wallet,
                placed,
                id,
                data.getAncientCopper()
        );
    }

    /**
     * Greedy high→low denomination grant into inventory only.
     * @return copper-value successfully placed
     */
    private static long grantMixedCopperValueInventoryOnly(ServerPlayer player, long copperValue) {
        if (player == null || copperValue <= 0L) {
            return 0L;
        }
        long remaining = copperValue;
        long placed = 0L;
        for (CoinKind kind : CoinKind.highToLow()) {
            if (remaining < kind.copperValue) {
                continue;
            }
            long count = remaining / kind.copperValue;
            long given = giveStacksInventoryOnly(player, kind, count);
            long value = safeMul(kind.copperValue, given);
            remaining -= value;
            placed += value;
            if (remaining <= 0L) {
                break;
            }
        }
        return placed;
    }

    /** @return number of coins actually placed in inventory */
    private static long giveStacksInventoryOnly(ServerPlayer player, CoinKind kind, long count) {
        if (player == null || kind == null || count <= 0L) {
            return 0L;
        }
        long left = count;
        long given = 0L;
        while (left > 0L) {
            int chunk = (int) Math.min(64L, left);
            AncientCoinType type = kind == CoinKind.NETHERITE
                    ? NETHERITE_VARIANTS[ThreadLocalRandom.current().nextInt(NETHERITE_VARIANTS.length)]
                    : kind.ancientType;
            ItemStack stack = type.asItem(chunk);
            if (stack == null || stack.m_41619_()) {
                break;
            }
            ItemStack copy = stack.m_41777_();
            if (!player.m_150109_().m_36054_(copy)) {
                // No room — stop without world-dropping.
                break;
            }
            given += chunk;
            left -= chunk;
        }
        if (given > 0L) {
            DifficultyCache.refresh(player);
        }
        return given;
    }

    public static void clearMigrateFlag(UUID playerId) {
        if (playerId != null) {
            MIGRATED.remove(playerId);
        }
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
        for (CoinKind kind : CoinKind.highToLow()) {
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
            // Netherite has 9 letter variants — pick one at random per stack.
            AncientCoinType type = kind == CoinKind.NETHERITE
                    ? NETHERITE_VARIANTS[ThreadLocalRandom.current().nextInt(NETHERITE_VARIANTS.length)]
                    : kind.ancientType;
            dropOrAdd(player, type.asItem(chunk));
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
