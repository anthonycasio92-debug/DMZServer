package com.dbzlegacy.adaptivedifficulty.title;

import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationType;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Resolves small capped title perks + permanent Title Score rewards.
 * Adaptive Difficulty tiers remain the primary power system.
 */
public final class TitleEffects {
    /** Hard cap on combined absolute coin-drop chance from titles/score. */
    public static final double MAX_COIN_DROP_BONUS = 0.10;
    /** Hard cap on coin quantity multiplier bonus (e.g. 0.05 = +5%). */
    public static final double MAX_COIN_MULT_BONUS = 0.05;
    /** Hard cap on any single outgoing damage family bonus. */
    public static final double MAX_DAMAGE_BONUS = 0.10;

    private TitleEffects() {}

    public static double equippedPerkValue(DifficultyTitle title, int masteryLevel) {
        if (title == null || title.perk == DifficultyTitle.Perk.NONE) {
            return 0.0;
        }
        double base = title.perkValue;
        if (!title.supportsMastery() || masteryLevel <= 0) {
            return base;
        }
        // +20% of base perk per mastery rank (I..V), still under global caps later.
        return base * (1.0 + 0.20 * Math.min(TitleProgress.MASTERY_MAX, masteryLevel));
    }

    public static double coinDropChanceBonus(ServerPlayer player) {
        if (player == null) {
            return 0.0;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        TitleProgress progress = data.titleProgress();
        double bonus = 0.0;
        DifficultyTitle equipped = DifficultyTitle.byId(data.getActiveTitle());
        if (equipped != null && equipped.perk == DifficultyTitle.Perk.COIN_DROP) {
            bonus += equippedPerkValue(equipped, progress.masteryLevel(equipped.id));
        }
        if (progress.hasClaimedMilestone(TitleScoreRewards.COIN_SEEKER.scoreRequired)) {
            bonus += 0.05;
        }
        return Math.min(MAX_COIN_DROP_BONUS, Math.max(0.0, bonus));
    }

    public static double coinQuantityBonus(ServerPlayer player) {
        if (player == null) {
            return 0.0;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        TitleProgress progress = data.titleProgress();
        double bonus = 0.0;
        DifficultyTitle equipped = DifficultyTitle.byId(data.getActiveTitle());
        if (equipped != null && equipped.perk == DifficultyTitle.Perk.COIN_MULT) {
            bonus += equippedPerkValue(equipped, progress.masteryLevel(equipped.id));
        }
        if (progress.hasClaimedMilestone(TitleScoreRewards.CHAMPION.scoreRequired)) {
            bonus += 0.02;
        }
        return Math.min(MAX_COIN_MULT_BONUS, Math.max(0.0, bonus));
    }

    public static double bossCoinChanceBonus(ServerPlayer player) {
        if (player == null) {
            return 0.0;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        DifficultyTitle equipped = DifficultyTitle.byId(data.getActiveTitle());
        if (equipped == null || equipped.perk != DifficultyTitle.Perk.BOSS_COIN) {
            return 0.0;
        }
        return Math.min(0.08, equippedPerkValue(equipped, data.titleProgress().masteryLevel(equipped.id)));
    }

    public static double eliteRewardBonus(ServerPlayer player) {
        if (player == null) {
            return 0.0;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        DifficultyTitle equipped = DifficultyTitle.byId(data.getActiveTitle());
        if (equipped == null || equipped.perk != DifficultyTitle.Perk.ELITE_REWARD) {
            return 0.0;
        }
        return Math.min(0.08, equippedPerkValue(equipped, data.titleProgress().masteryLevel(equipped.id)));
    }

    /**
     * Extra chance to promote the rolled coin denomination by one step
     * (quality bump — hard-capped, never skips two steps).
     */
    public static double coinQualityBumpChance(ServerPlayer player, boolean elite, boolean boss, int activeTier) {
        if (player == null) {
            return 0.0;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        DifficultyTitle equipped = DifficultyTitle.byId(data.getActiveTitle());
        if (equipped == null) {
            return 0.0;
        }
        double chance = 0.0;
        if (equipped == DifficultyTitle.ELITE_HUNTER && elite) {
            chance += 0.04 + 0.01 * data.titleProgress().masteryLevel(equipped.id);
        }
        if (equipped == DifficultyTitle.BOSS_SLAYER && boss) {
            chance += 0.06 + 0.01 * data.titleProgress().masteryLevel(equipped.id);
        }
        if (equipped == DifficultyTitle.T7_GOD && activeTier >= 7) {
            chance += 0.03;
        }
        return Math.min(0.12, chance);
    }

    public static float applyOutgoingDamageBonus(ServerPlayer attacker, LivingEntity victim, float amount) {
        if (attacker == null || victim == null || amount <= 0.0f) {
            return amount;
        }
        // Only affect Adaptive Difficulty / rarity targets.
        if (!MobScaling.isAdPainted(victim)
                && !EliteSystem.isElite(victim)
                && MutationSystem.get(victim) == null
                && !isBoss(victim)) {
            return amount;
        }
        double mult = 1.0 + outgoingDamageBonus(attacker, victim);
        if (mult <= 1.0001) {
            return amount;
        }
        return (float) (amount * mult);
    }

    public static double outgoingDamageBonus(ServerPlayer attacker, LivingEntity victim) {
        if (attacker == null || victim == null) {
            return 0.0;
        }
        PlayerDifficultyData data = DifficultyCache.data(attacker);
        TitleProgress progress = data.titleProgress();
        DifficultyTitle equipped = DifficultyTitle.byId(data.getActiveTitle());
        boolean elite = EliteSystem.isElite(victim);
        boolean boss = isBoss(victim);
        boolean mutated = MutationSystem.get(victim) != null;

        double bonus = 0.0;
        if (equipped != null) {
            double perk = equippedPerkValue(equipped, progress.masteryLevel(equipped.id));
            bonus += switch (equipped.perk) {
                case ELITE_DAMAGE -> elite ? perk : 0.0;
                case MUTATION_DAMAGE -> mutated ? perk : 0.0;
                case BOSS_DAMAGE -> boss ? perk : 0.0;
                case AD_DAMAGE -> perk;
                default -> 0.0;
            };
        }
        if (progress.hasClaimedMilestone(TitleScoreRewards.VETERAN.scoreRequired)) {
            bonus += 0.01;
        }
        if (progress.hasClaimedMilestone(TitleScoreRewards.CONQUEROR.scoreRequired)
                && (elite || mutated)) {
            bonus += 0.03;
        }
        if (progress.hasClaimedMilestone(TitleScoreRewards.DREADED.scoreRequired) && boss) {
            bonus += 0.05;
        }
        return Math.min(MAX_DAMAGE_BONUS, Math.max(0.0, bonus));
    }

    private static boolean isBoss(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return PersistentDataAccess.get(entity).m_128471_(BossScaling.TAG_BOSS);
    }
}
