package com.dbzlegacy.adaptivedifficulty.tier;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * V3 unlock logic: DMZ level requirement OR prestige ≥ tier id.
 * Prestige bypasses level gates but never activates difficulty.
 * No free coin bootstrap — coins come from kill drops.
 */
public final class UnlockSystem {
    private UnlockSystem() {}

    public static boolean isEligible(ServerPlayer player, UnlockTier tier) {
        if (player == null || tier == null) {
            return false;
        }
        long fallback = 0L;
        try {
            fallback = com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache
                    .data(player).getHighestDmzLevel();
        } catch (Throwable ignored) {
        }
        int level = DmzProgression.dmzLevelForProgression(player, fallback);
        int prestige = DmzProgression.prestige(player);
        return level >= tier.requiredDmzLevel() || prestige >= tier.id;
    }

    /**
     * Sync permanent unlocks from current DMZ level + prestige.
     * @return newly unlocked tier ids
     */
    public static List<Integer> syncUnlocks(ServerPlayer player, PlayerDifficultyData data) {
        List<Integer> newly = new ArrayList<>();
        if (player == null || data == null) {
            return newly;
        }
        int level = DmzProgression.dmzLevelForProgression(player, data.getHighestDmzLevel());
        if (!DmzProgression.isTransformed(player)) {
            data.noteDmzLevel(level);
        }
        for (UnlockTier tier : UnlockTier.values()) {
            if (isEligible(player, tier) && data.unlockTier(tier.id)) {
                newly.add(tier.id);
            }
        }
        return newly;
    }

    public static int highestUnlocked(PlayerDifficultyData data) {
        if (data == null) {
            return 0;
        }
        int best = 0;
        for (int id : data.getUnlockedTiers()) {
            best = Math.max(best, id);
        }
        return best;
    }

    public static long maxDifficultyFor(PlayerDifficultyData data) {
        if (data == null) {
            return 0L;
        }
        UnlockTier tier = UnlockTier.byId(data.getActiveTier());
        return tier == null ? 0L : tier.maxDifficulty();
    }
}
