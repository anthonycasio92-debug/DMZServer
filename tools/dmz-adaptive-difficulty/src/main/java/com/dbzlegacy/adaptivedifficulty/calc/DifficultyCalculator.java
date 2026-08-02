package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import net.minecraft.server.level.ServerPlayer;

/** Unlock tiers, activation ceilings, combat rating (rewards/display). Teams are WIP. */
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

        // Teams remain a WIP stub — personal difficulty only.
        TeamMode mode = TeamMode.PERSONAL_ONLY;
        long personalMax = tierMax;
        long thresholdBonus = 0L;
        long contribution = 0L;
        long availableMax = clampNonNegative(personalMax);

        long active = Math.min(data.getActiveDifficultyLevel(), availableMax);
        if (tier == null) {
            active = 0L;
        }
        if (active != data.getActiveDifficultyLevel()) {
            data.setActiveDifficultyLevel(active);
        }
        // Repair unlock-list desync — never wipe a valid purchased activation on
        // login/refresh/logout. Older builds cleared activeTier here when the
        // unlock set failed to load, which looked like "disconnect resets to 0".
        if (activeTier > 0 && !data.hasUnlockedTier(activeTier)) {
            if (tier != null) {
                data.unlockTier(activeTier);
            } else {
                data.resetTemporary();
                activeTier = 0;
                active = 0L;
                availableMax = 0L;
                personalMax = 0L;
                thresholdBonus = 0L;
                contribution = 0L;
            }
        }

        long combatRating = CombatRating.compute(level, prestige, active, transform, DifficultyConfig.get());
        long ancientCopper = AncientCoinEconomy.balance(player);

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
                ancientCopper,
                mode,
                data.isPersonalEnabled()
        );
    }

    public static double rewardMultiplier(long activeDifficulty) {
        return ScalingCurves.rewardMultiplier(activeDifficulty);
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
}
