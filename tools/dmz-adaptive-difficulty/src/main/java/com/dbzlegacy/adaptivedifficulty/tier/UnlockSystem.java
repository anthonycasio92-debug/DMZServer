package com.dbzlegacy.adaptivedifficulty.tier;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * V3 unlock logic: current DMZ level requirement OR prestige ≥ tier id.
 * Prestige bypasses level gates but never activates difficulty.
 * Unlock bits are live-reconciled — prestige/level reset cannot keep high tiers.
 * No free coin bootstrap — coins come from kill drops.
 */
public final class UnlockSystem {
    private UnlockSystem() {}

    public static boolean isEligible(ServerPlayer player, UnlockTier tier) {
        if (player == null || tier == null) {
            return false;
        }
        int level = DmzProgression.dmzLevelForUnlockGate(player);
        int prestige = DmzProgression.prestige(player);
        return level >= tier.requiredDmzLevel() || prestige >= tier.id;
    }

    /**
     * Sync permanent unlocks from current DMZ level + prestige.
     * Grants newly eligible tiers and revokes tiers the player no longer qualifies for
     * (e.g. prestiged and lost the DMZ level gate without enough Prestige).
     *
     * @return newly unlocked tier ids
     */
    public static List<Integer> syncUnlocks(ServerPlayer player, PlayerDifficultyData data) {
        List<Integer> newly = new ArrayList<>();
        if (player == null || data == null) {
            return newly;
        }

        int prestige = DmzProgression.prestige(player);
        int gateLevel = DmzProgression.dmzLevelForUnlockGate(player);
        int prevPrestige = data.getLastSeenPrestige();
        if (prevPrestige >= 0 && prestige > prevPrestige) {
            // Prestige-up restarts the DMZ level ladder — drop the old high-water mark.
            data.resetHighestDmzLevel(gateLevel);
        }
        data.setLastSeenPrestige(prestige);

        if (!DmzProgression.isTransformed(player)) {
            data.noteDmzLevel(gateLevel);
        }

        for (UnlockTier tier : UnlockTier.values()) {
            if (isEligible(player, tier) && data.unlockTier(tier.id)) {
                newly.add(tier.id);
            }
        }

        // Revoke stale unlock bits so coins alone cannot rebuy gated tiers.
        for (Integer id : new ArrayList<>(data.getUnlockedTiers())) {
            UnlockTier tier = UnlockTier.byId(id);
            if (tier == null || !isEligible(player, tier)) {
                data.revokeTier(id);
            }
        }

        int activeId = data.getActiveTier();
        if (activeId > 0 && !data.hasUnlockedTier(activeId)) {
            data.resetTemporary();
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
