package com.dbzlegacy.adaptivedifficulty.reward;

import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.ScalingCurves;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.HostileMobs;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Kill rewards: Ancient Coins drop in the world at the mob + modest XP.
 * Only hostiles, only with an active Unlock Tier. No farm-animal / inactive spam.
 */
public final class RewardSystem {
    private static final String TAG_REWARDED = "dmz_ad_kill_rewarded";

    private RewardSystem() {}

    public static void onKill(ServerPlayer killer, LivingEntity dead) {
        if (killer == null || dead == null || dead instanceof Player) {
            return;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enabled || !cfg.enableRewardScaling || !SystemGate.allows(killer)) {
            return;
        }
        // Passive mobs / farms must never mint Ancient Coins.
        if (!HostileMobs.isHostile(dead) || MobScaling.isExemptFromConversion(dead)) {
            return;
        }
        DifficultySnapshot snap = DifficultyCache.get(killer);
        // No active tier → no coin economy participation.
        if (snap.activeTier <= 0) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(dead);
        if (tag.m_128471_(TAG_REWARDED)) {
            return;
        }
        tag.m_128379_(TAG_REWARDED, true);

        UnlockTier unlock = UnlockTier.byId(snap.activeTier);
        boolean elite = EliteSystem.isElite(dead);
        boolean boss = tag.m_128471_(BossScaling.TAG_BOSS);
        double mult = ScalingCurves.rewardMultiplier(Math.max(1L, snap.active));

        AncientCoinEconomy.Drop drop = AncientCoinEconomy.rollKillDrop(killer, snap.combatRating, elite, boss);
        if (drop.count() > 0) {
            AncientCoinEconomy.dropInWorld(dead, drop);
            AncientCoinEconomy.notifyGrant(killer, drop);
        }
        grantExperience(killer, mult, elite, boss, unlock);
        TitleSystem.maybeUnlockCombatTitle(killer, snap, elite, boss);
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
