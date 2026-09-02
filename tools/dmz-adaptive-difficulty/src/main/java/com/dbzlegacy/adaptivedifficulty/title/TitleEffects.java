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
 * Adaptive Difficulty tiers remain the primary power system; equipped titles
 * add a rarity-based presence (softer landings + a touch of AD damage) plus
 * their specific perk — usable after lowering tier.
 */
public final class TitleEffects {
    /** Hard cap on combined absolute coin-drop chance from titles/score. */
    public static final double MAX_COIN_DROP_BONUS = 0.12;
    /** Hard cap on coin quantity multiplier bonus (e.g. 0.08 = +8%). */
    public static final double MAX_COIN_MULT_BONUS = 0.08;
    /** Hard cap on any single outgoing damage family bonus. */
    public static final double MAX_DAMAGE_BONUS = 0.12;
    /** Hard cap on landing-damage relief from equipped title rarity. */
    public static final double MAX_LAND_RELIEF = 0.025;
    /** Hard cap on TP gain bonus from equipped title rarity. */
    public static final double MAX_TP_BONUS = 0.03;

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

    /** Equipped title, or null. */
    public static DifficultyTitle equipped(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        return DifficultyTitle.byId(DifficultyCache.data(player).getActiveTitle());
    }

    /**
     * Rarity presence: reduce Adaptive landing damage on the player (softer hits).
     * COMMON 0.5% · RARE 1% · EPIC 1.5% · LEGENDARY/MYTHIC 2.5%.
     */
    public static double landFracRelief(ServerPlayer player) {
        DifficultyTitle title = equipped(player);
        if (title == null) {
            return 0.0;
        }
        double relief = switch (title.rarity) {
            case COMMON -> 0.005;
            case RARE -> 0.010;
            case EPIC -> 0.015;
            case LEGENDARY, MYTHIC -> 0.025;
        };
        return Math.min(MAX_LAND_RELIEF, relief);
    }

    /**
     * Rarity presence: tiny Adaptive outgoing damage while any title is equipped.
     * Stacks with the title's specific damage perk under {@link #MAX_DAMAGE_BONUS}.
     */
    public static double presenceDamageBonus(ServerPlayer player) {
        DifficultyTitle title = equipped(player);
        if (title == null) {
            return 0.0;
        }
        return switch (title.rarity) {
            case COMMON -> 0.0;
            case RARE -> 0.005;
            case EPIC -> 0.010;
            case LEGENDARY, MYTHIC -> 0.015;
        };
    }

    /** Rarity presence: +TP on awards while an Adaptive tier is active. */
    public static double tpGainBonus(ServerPlayer player) {
        if (player == null) {
            return 0.0;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        if (data.getActiveTier() <= 0) {
            return 0.0;
        }
        DifficultyTitle title = DifficultyTitle.byId(data.getActiveTitle());
        if (title == null) {
            return 0.0;
        }
        double bonus = switch (title.rarity) {
            case COMMON -> 0.01;
            case RARE -> 0.015;
            case EPIC -> 0.02;
            case LEGENDARY, MYTHIC -> 0.03;
        };
        return Math.min(MAX_TP_BONUS, bonus);
    }

    /** One-line summary of rarity presence for GUI tips. */
    public static String presenceTip(DifficultyTitle title) {
        if (title == null) {
            return "";
        }
        return switch (title.rarity) {
            case COMMON -> "Presence: §a-0.5% landing §8· §a+1% TP §8(tier on)";
            case RARE -> "Presence: §a-1% landing §8· §a+0.5% AD dmg §8· §a+1.5% TP";
            case EPIC -> "Presence: §a-1.5% landing §8· §a+1% AD dmg §8· §a+2% TP";
            case LEGENDARY, MYTHIC -> "Presence: §a-2.5% landing §8· §a+1.5% AD dmg §8· §a+3% TP";
        };
    }

    /** Compact presence fragment appended to perk tips. */
    public static String presenceTipShort(DifficultyTitle title) {
        if (title == null) {
            return "";
        }
        return switch (title.rarity) {
            case COMMON -> "§a-0.5% land §8· §a+1% TP";
            case RARE -> "§a-1% land §8· §a+0.5% AD §8· §a+1.5% TP";
            case EPIC -> "§a-1.5% land §8· §a+1% AD §8· §a+2% TP";
            case LEGENDARY, MYTHIC -> "§a-2.5% land §8· §a+1.5% AD §8· §a+3% TP";
        };
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
        if (equipped == DifficultyTitle.T7_GOD) {
            chance += 0.04;
        }
        return Math.min(0.14, chance);
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
        bonus += presenceDamageBonus(attacker);
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
