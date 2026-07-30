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
        // Keep stored active within bounds when progression drops.
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

    public static long purchaseCost(long currentPurchased, long amountToBuy) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (amountToBuy <= 0) {
            return 0L;
        }
        // Concept §6: Cost = Base Cost × (Purchased Difficulty / Cost Scaling)
        // Applied per costScaling-sized chunk being bought (min 1 chunk).
        double purchasedRatio = cfg.costScaling <= 0
                ? 1.0
                : Math.max(1.0, (double) Math.max(1L, currentPurchased) / (double) cfg.costScaling);
        long chunks = Math.max(1L, (amountToBuy + Math.max(1L, cfg.costScaling) - 1L) / Math.max(1L, cfg.costScaling));
        return Math.max(1L, Math.round(cfg.baseCost * purchasedRatio) * chunks);
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
