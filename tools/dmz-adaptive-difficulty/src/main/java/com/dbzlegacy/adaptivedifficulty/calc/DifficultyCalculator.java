package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
import com.dbzlegacy.adaptivedifficulty.team.TeamScaling;
import net.minecraft.server.level.ServerPlayer;

public final class DifficultyCalculator {
    private DifficultyCalculator() {}

    public static long calculatedDifficulty(int dmzLevel, int prestige) {
        DifficultyConfig cfg = DifficultyConfig.get();
        long levelPart = Math.round(Math.max(1, dmzLevel) * cfg.levelMultiplier);
        if (prestige <= 0) {
            return clamp(levelPart);
        }
        long prestigeFactor = Math.round(prestige * cfg.prestigeMultiplier);
        return clamp(levelPart * Math.max(1L, prestigeFactor));
    }

    public static DifficultySnapshot snapshot(ServerPlayer player, PlayerDifficultyData data) {
        int level = DmzProgression.dmzLevel(player);
        int prestige = DmzProgression.prestige(player);
        long calculated = calculatedDifficulty(level, prestige);
        long purchased = data.getPurchasedDifficulty();
        long personalMax = clamp(calculated + purchased);

        TeamMode mode = data.getTeamMode();
        long thresholdBonus = 0L;
        long contribution = 0L;
        if (mode != TeamMode.PERSONAL_ONLY) {
            thresholdBonus = TeamScaling.thresholdBonus(player, personalMax);
        }
        long afterThreshold = clamp(personalMax + thresholdBonus);
        if (mode == TeamMode.FULL_TEAM_SCALING) {
            contribution = TeamScaling.contributionBonus(player, afterThreshold);
        }
        long availableMax = clamp(afterThreshold + contribution);

        long active = Math.min(data.getActiveDifficulty(), availableMax);
        if (active < 0) {
            active = 0;
        }
        if (active != data.getActiveDifficulty()) {
            data.setActiveDifficulty(active);
        }

        return new DifficultySnapshot(
                level,
                prestige,
                calculated,
                purchased,
                personalMax,
                thresholdBonus,
                contribution,
                availableMax,
                active,
                mode
        );
    }

    /**
     * Iron-coin cost to raise difficulty by {@code amount} levels starting from {@code fromLevel}.
     * <p>
     * Default: 1 iron coin per level at 0, scaling by {@code costScalePerDifficulty}
     * so higher difficulty is more expensive per level.
     * <pre>
     * cost(level) = baseIron × (1 + level × scale)
     * total ≈ amount × baseIron × (1 + scale × (from + (amount-1)/2))
     * </pre>
     */
    public static long raiseCostIronCoins(long fromLevel, long amount) {
        if (amount <= 0) {
            return 0L;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        double base = Math.max(0.0, cfg.baseCostIronCoins);
        if (base <= 0.0) {
            return 0L;
        }
        double scale = Math.max(0.0, cfg.costScalePerDifficulty);
        double from = Math.max(0L, fromLevel);
        double total = amount * base * (1.0 + scale * (from + (amount - 1L) / 2.0));
        return Math.max(1L, Math.round(total));
    }

    /** Cost to unlock more purchased max (same iron-coin scaling, keyed off current purchased). */
    public static long purchaseCost(long currentPurchased, long amountToBuy) {
        return raiseCostIronCoins(Math.max(0L, currentPurchased), amountToBuy);
    }

    public static double rewardMultiplier(long activeDifficulty) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableRewardScaling || cfg.rewardScaling <= 0) {
            return 1.0;
        }
        return 1.0 + (activeDifficulty / cfg.rewardScaling);
    }

    private static long clamp(long value) {
        long cap = Math.max(1L, DifficultyConfig.get().hardCapDifficulty);
        if (value < 0) {
            return 0;
        }
        return Math.min(value, cap);
    }
}
