package com.dbzlegacy.adaptivedifficulty.tier;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * V3 unlock logic: DMZ level requirement OR prestige ≥ tier id.
 * Prestige bypasses level gates but never activates difficulty.
 */
public final class UnlockSystem {
    private UnlockSystem() {}

    public static boolean isEligible(ServerPlayer player, UnlockTier tier) {
        if (player == null || tier == null) {
            return false;
        }
        int level = DmzProgression.dmzLevel(player);
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
        int level = DmzProgression.dmzLevel(player);
        data.noteDmzLevel(level);
        boolean firstUnlockEver = data.getUnlockedTiers().isEmpty();
        for (UnlockTier tier : UnlockTier.values()) {
            if (isEligible(player, tier) && data.unlockTier(tier.id)) {
                newly.add(tier.id);
            }
        }
        // Bootstrap exact Copper Ancient Coins so the first tier purchase is reachable.
        if (firstUnlockEver && !newly.isEmpty() && AncientCoinEconomy.balance(player) <= 0L) {
            AncientCoinEconomy.grantCopperExact(player, 250L);
            AncientCoinEconomy.notifyGrant(player, 250L);
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
        int active = data.getActiveTier();
        UnlockTier tier = UnlockTier.byId(active);
        if (tier == null) {
            return 0L;
        }
        return tier.maxDifficulty();
    }
}
