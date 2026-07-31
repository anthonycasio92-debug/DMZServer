package com.dbzlegacy.adaptivedifficulty.currency;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * V3 Ancient Coin wallet (stored as copper-value units on the player).
 * Kill drops grant coins; activation / upgrades spend them permanently.
 */
public final class AncientCoinEconomy {
    public enum CoinKind {
        COPPER(1, "Copper"),
        IRON(10, "Iron"),
        GOLD(100, "Gold"),
        DIAMOND(1_000, "Diamond"),
        EMERALD(10_000, "Emerald"),
        NETHERITE(100_000, "Netherite"),
        DIVINE(1_000_000, "Divine");

        public final long copperValue;
        public final String display;

        CoinKind(long copperValue, String display) {
            this.copperValue = copperValue;
            this.display = display;
        }
    }

    private AncientCoinEconomy() {}

    public static long balance(ServerPlayer player) {
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
        // Small variance
        double jitter = 0.85 + ThreadLocalRandom.current().nextDouble() * 0.30;
        return Math.max(1L, Math.round(base * jitter));
    }

    public static CoinKind rollKind(int activeTier) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        // Spec progression: higher tiers unlock better coin opportunities.
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

    private static long snaplessLevel(ServerPlayer player) {
        try {
            return Math.max(1, com.dbzlegacy.adaptivedifficulty.calc.DmzProgression.dmzLevel(player));
        } catch (Throwable t) {
            return 1L;
        }
    }
}
