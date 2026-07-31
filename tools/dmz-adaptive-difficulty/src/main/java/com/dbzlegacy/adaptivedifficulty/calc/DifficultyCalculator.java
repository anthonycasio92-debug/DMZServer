package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
import com.dbzlegacy.adaptivedifficulty.team.TeamScaling;
import net.minecraft.server.level.ServerPlayer;

public final class DifficultyCalculator {
    private DifficultyCalculator() {}

    /**
     * Theoretical max from DMZ progression (level × prestige formula).
     * Prestige 0: level × levelMultiplier
     * Prestige 1+: level × levelMultiplier × (prestige × prestigeMultiplier)
     */
    public static long calculatedDifficulty(int dmzLevel, int prestige) {
        DifficultyConfig cfg = DifficultyConfig.get();
        long levelPart = Math.round(Math.max(1, dmzLevel) * Math.max(0.0, cfg.levelMultiplier));
        if (prestige <= 0) {
            return clampNonNegative(levelPart);
        }
        long prestigeFactor = Math.round(prestige * Math.max(0.0, cfg.prestigeMultiplier));
        return clampNonNegative(safeMul(levelPart, Math.max(1L, prestigeFactor)));
    }

    public static DifficultySnapshot snapshot(ServerPlayer player, PlayerDifficultyData data) {
        int level = DmzProgression.dmzLevel(player);
        int prestige = DmzProgression.prestige(player);
        long calculated = calculatedDifficulty(level, prestige);
        long purchased = data.getPurchasedDifficulty();
        // Personal max = theoretical (stats) + purchased unlocks — no artificial hardcap by default.
        long personalMax = clampNonNegative(safeAdd(calculated, purchased));

        TeamMode mode = data.getTeamMode();
        long thresholdBonus = 0L;
        long contribution = 0L;
        if (mode != TeamMode.PERSONAL_ONLY) {
            thresholdBonus = TeamScaling.thresholdBonus(player, personalMax);
        }
        long afterThreshold = clampNonNegative(safeAdd(personalMax, thresholdBonus));
        if (mode == TeamMode.FULL_TEAM_SCALING) {
            contribution = TeamScaling.contributionBonus(player, afterThreshold);
        }
        long availableMax = clampNonNegative(safeAdd(afterThreshold, contribution));

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

    /** Always {@code 1.0} — this mod never multiplies TP / reward events. */
    public static double rewardMultiplier(long activeDifficulty) {
        return ScalingCurves.rewardMultiplier(activeDifficulty);
    }

    /** Always {@code 0} — this mod never grants kill TP. */
    public static double killTrainingPoints(long difficulty) {
        return 0.0;
    }

    /** Always {@code 0} — this mod never grants kill TP. */
    public static double killTrainingPointsFromHealth(double maxHealth) {
        return 0.0;
    }

    /**
     * Floor at 0. Optional admin hardcap only when {@code hardCapDifficulty > 0}.
     * Default 0 = unlimited (ceiling is stats / purchased / team only).
     */
    private static long clampNonNegative(long value) {
        if (value < 0L) {
            return 0L;
        }
        long cap = DifficultyConfig.get().hardCapDifficulty;
        if (cap > 0L) {
            return Math.min(value, cap);
        }
        return value;
    }

    private static long safeAdd(long a, long b) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE / 4L;
        }
    }

    private static long safeMul(long a, long b) {
        try {
            return Math.multiplyExact(a, b);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE / 4L;
        }
    }
}
