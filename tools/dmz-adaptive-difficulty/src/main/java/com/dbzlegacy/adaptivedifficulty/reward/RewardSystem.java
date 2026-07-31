package com.dbzlegacy.adaptivedifficulty.reward;

import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.ScalingCurves;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * V3 kill rewards: Ancient Coins drop in the world at the mob + modest XP.
 * No TP, titles, capsules, or inventory coin injection.
 */
public final class RewardSystem {
    private RewardSystem() {}

    public static void onKill(ServerPlayer killer, LivingEntity dead) {
        if (killer == null || dead == null || dead instanceof Player) {
            return;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableRewardScaling) {
            return;
        }
        DifficultySnapshot snap = DifficultyCache.get(killer);
        long killDifficulty = Math.max(snap.combatRating, MobScaling.difficultyOf(dead));
        UnlockTier unlock = UnlockTier.byId(Math.max(snap.activeTier, 1));
        boolean elite = EliteSystem.isElite(dead);
        boolean boss = PersistentDataAccess.get(dead).m_128471_(BossScaling.TAG_BOSS);
        double mult = ScalingCurves.rewardMultiplier(Math.max(1L, snap.active));

        AncientCoinEconomy.Drop drop = AncientCoinEconomy.rollKillDrop(killer, snap.combatRating, elite, boss);
        if (drop.count() > 0) {
            AncientCoinEconomy.dropInWorld(dead, drop);
            AncientCoinEconomy.notifyGrant(killer, drop);
        }
        grantExperience(killer, mult, elite, boss, unlock);
    }

    private static void grantExperience(
            ServerPlayer killer, double mult, boolean elite, boolean boss, UnlockTier unlock
    ) {
        int power = unlock == null ? 1 : Math.max(1, unlock.id);
        int xp = (int) Math.round(3.0 * Math.max(1.0, mult) * power);
        if (elite) {
            xp *= 2;
        }
        if (boss) {
            xp *= 4;
        }
        // Soft-cap so high CR kills don't dump absurd vanilla XP.
        xp = Math.min(xp, 250 * power);
        if (xp > 0) {
            killer.m_6756_(xp); // giveExperiencePoints
        }
    }
}
