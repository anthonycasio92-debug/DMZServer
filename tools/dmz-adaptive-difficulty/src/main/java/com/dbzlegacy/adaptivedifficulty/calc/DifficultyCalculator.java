package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
import com.dbzlegacy.adaptivedifficulty.team.TeamScaling;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import net.minecraft.server.level.ServerPlayer;

/** V3 difficulty math: unlocks, activation ceilings, combat rating, Ancient Coin costs. */
public final class DifficultyCalculator {
    private DifficultyCalculator() {}

    public static DifficultySnapshot snapshot(ServerPlayer player, PlayerDifficultyData data) {
        UnlockSystem.syncUnlocks(player, data);

        int level = DmzProgression.dmzLevel(player);
        int prestige = DmzProgression.prestige(player);
        double transform = DmzProgression.transformationPower(player);
        data.noteDmzLevel(level);

        int highest = UnlockSystem.highestUnlocked(data);
        int activeTier = data.getActiveTier();
        UnlockTier tier = UnlockTier.byId(activeTier);
        long tierMax = tier == null ? 0L : tier.maxDifficulty();

        TeamMode mode = data.getTeamMode();
        long personalMax = tierMax;
        long thresholdBonus = 0L;
        long contribution = 0L;
        if (tier != null && mode != TeamMode.PERSONAL_ONLY) {
            thresholdBonus = TeamScaling.thresholdBonus(player, personalMax);
        }
        long afterThreshold = clampNonNegative(safeAdd(personalMax, thresholdBonus));
        if (tier != null && mode == TeamMode.FULL_TEAM_SCALING) {
            contribution = TeamScaling.contributionBonus(player, afterThreshold);
        }
        long availableMax = clampNonNegative(safeAdd(afterThreshold, contribution));

        long active = Math.min(data.getActiveDifficultyLevel(), availableMax);
        if (tier == null) {
            active = 0L;
        }
        if (active != data.getActiveDifficultyLevel()) {
            data.setActiveDifficultyLevel(active);
        }
        if (activeTier > 0 && !data.hasUnlockedTier(activeTier)) {
            data.resetTemporary();
            activeTier = 0;
            active = 0L;
            availableMax = 0L;
            personalMax = 0L;
            thresholdBonus = 0L;
            contribution = 0L;
        }

        long combatRating = CombatRating.compute(level, prestige, active, transform, DifficultyConfig.get());

        return new DifficultySnapshot(
                level,
                prestige,
                transform,
                highest,
                activeTier,
                active,
                tierMax,
                availableMax,
                personalMax,
                thresholdBonus,
                contribution,
                combatRating,
                data.getAncientCopper(),
                mode
        );
    }

    /** Ancient-copper cost to raise active difficulty by {@code amount}. */
    public static long upgradeCost(long fromLevel, long amount) {
        if (amount <= 0) {
            return 0L;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        double base = Math.max(0.0, cfg.upgradeCostBaseAncient);
        if (base <= 0.0) {
            return 0L;
        }
        double scale = Math.max(0.0, cfg.upgradeCostScalePerLevel);
        double from = Math.max(0L, fromLevel);
        double total = amount * base * (1.0 + scale * (from + (amount - 1L) / 2.0));
        return Math.max(1L, Math.round(total));
    }

    /** Compatibility alias — V3 upgrade costs in Ancient Copper. */
    @Deprecated
    public static long raiseCostIronCoins(long fromLevel, long amount) {
        return upgradeCost(fromLevel, amount);
    }

    /** Compatibility alias — V3 upgrade costs in Ancient Copper. */
    @Deprecated
    public static long purchaseCost(long currentPurchased, long amountToBuy) {
        return upgradeCost(Math.max(0L, currentPurchased), amountToBuy);
    }

    public static double rewardMultiplier(long activeDifficulty) {
        return ScalingCurves.rewardMultiplier(activeDifficulty);
    }

    public static double killTrainingPoints(long difficulty) {
        return 0.0;
    }

    public static double killTrainingPointsFromHealth(double maxHealth) {
        return 0.0;
    }

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
}
