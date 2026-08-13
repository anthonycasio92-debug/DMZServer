package com.dbzlegacy.adaptivedifficulty.tier;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * V3 unlock logic: current DMZ level requirement OR prestige ≥ tier id.
 * Prestige bypasses level gates but never activates difficulty.
 * Unlock bits are live-reconciled when a reliable base-form level is known —
 * never revoke from a "level 1" placeholder while transformed with no sample.
 * No free coin bootstrap — coins come from kill drops.
 * <p>
 * Combat Rating / Battle Power does <b>not</b> unlock tiers.
 */
public final class UnlockSystem {
    private UnlockSystem() {}

    public static boolean isEligible(ServerPlayer player, UnlockTier tier) {
        if (player == null || tier == null) {
            return false;
        }
        int prestige = DmzProgression.prestige(player);
        if (prestige >= tier.id) {
            return true;
        }
        long level = gateLevelForEligibility(player);
        return level >= tier.requiredDmzLevel();
    }

    /**
     * DMZ level used for unlock eligibility.
     * Prefer a reliable base-form sample. When the player is transformed (or stats
     * are mid-attach) with no session sample, fall back to the persisted
     * prestige-scoped high-water mark so login-in-form does not lock every tier.
     * Returns {@code 0} when no safe level is known (not eligible via level).
     */
    public static long gateLevelForEligibility(ServerPlayer player) {
        if (player == null) {
            return 0L;
        }
        if (DmzProgression.hasReliableUnlockGateSample(player)) {
            return DmzProgression.dmzLevelForUnlockGate(player);
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        return persistedGateLevel(player, data);
    }

    /**
     * Prestige-scoped high-water level for eligibility when the live base-form
     * sample is unavailable. Never uses form-inflated live {@code getLevel()}.
     */
    static long persistedGateLevel(ServerPlayer player, PlayerDifficultyData data) {
        if (player == null || data == null) {
            return 0L;
        }
        int prestige = DmzProgression.prestige(player);
        int seen = data.getLastSeenPrestige();
        // Only trust high-water from the same prestige cycle (or unset while still P0).
        if (seen >= 0 && seen != prestige) {
            return 0L;
        }
        if (seen < 0 && prestige > 0) {
            // Prestige tracking not initialized yet — do not grant from a pre-prestige mark.
            return 0L;
        }
        long hw = data.getHighestDmzLevel();
        return hw > 0L ? hw : 0L;
    }

    /**
     * Sync permanent unlocks from current DMZ level + prestige.
     * Grants newly eligible tiers. Revokes only when the unlock gate sample is reliable
     * (base form, or transformed with a base-form sample this session).
     *
     * @return newly unlocked tier ids
     */
    public static List<Integer> syncUnlocks(ServerPlayer player, PlayerDifficultyData data) {
        List<Integer> newly = new ArrayList<>();
        if (player == null || data == null) {
            return newly;
        }

        int prestige = DmzProgression.prestige(player);
        boolean reliable = DmzProgression.hasReliableUnlockGateSample(player);
        int gateLevel = reliable ? DmzProgression.dmzLevelForUnlockGate(player) : 0;
        int prevPrestige = data.getLastSeenPrestige();

        if (prevPrestige < 0) {
            // First sync on tracked-prestige builds: clamp leftover pre-prestige high-water
            // when the player already has Prestige and we can see their live base level.
            if (prestige > 0 && reliable) {
                data.resetHighestDmzLevel(gateLevel);
            } else if (prestige > 0) {
                data.resetHighestDmzLevel(0);
            }
        } else if (prestige > prevPrestige) {
            // Prestige-up restarts the DMZ level ladder.
            if (reliable) {
                data.resetHighestDmzLevel(gateLevel);
            } else {
                data.resetHighestDmzLevel(0);
            }
        }
        data.setLastSeenPrestige(prestige);

        if (reliable && !DmzProgression.isTransformed(player)) {
            data.noteDmzLevel(gateLevel);
        }

        for (UnlockTier tier : UnlockTier.values()) {
            if (isEligible(player, tier) && data.unlockTier(tier.id)) {
                newly.add(tier.id);
            }
        }

        // Never revoke while the level sample is unknown — that wiped paid tiers on
        // early login (StatsData null → fake level 1), transformed login, or admin reload.
        if (reliable) {
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
        } else {
            // Prestige-only grants still apply above; prestige-only revokes are safe.
            for (Integer id : new ArrayList<>(data.getUnlockedTiers())) {
                UnlockTier tier = UnlockTier.byId(id);
                if (tier == null) {
                    data.revokeTier(id);
                }
                // Keep level-gated unlocks until we have a real base-form sample.
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
