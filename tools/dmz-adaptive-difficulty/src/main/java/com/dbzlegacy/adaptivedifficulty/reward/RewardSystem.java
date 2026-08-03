package com.dbzlegacy.adaptivedifficulty.reward;

import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.ScalingCurves;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.HostileMobs;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tick.ScaledMobTracker;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Kill rewards: Ancient Coins drop in the world at the mob + modest XP.
 * Requires personal difficulty on. Pre-T1 still drops Copper so players can buy T1.
 */
public final class RewardSystem {
    private static final String TAG_REWARDED = "dmz_ad_kill_rewarded";
    /** Fallback when entity persistent data is not writable (never touch shared EMPTY). */
    private static final Set<UUID> SESSION_REWARDED = ConcurrentHashMap.newKeySet();

    private RewardSystem() {}

    public static void onKill(ServerPlayer killer, LivingEntity dead) {
        if (killer == null || dead == null || dead instanceof Player) {
            return;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enabled || !cfg.enableRewardScaling || !SystemGate.participates(killer)) {
            return;
        }
        // Passive mobs / farms must never mint Ancient Coins.
        if (!HostileMobs.isHostile(dead) || MobScaling.isExemptFromConversion(dead)) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(dead);
        UUID deadId = dead.m_20148_();
        // Only AD-scaled, claim-owned hostiles drop the kill ladder — no vanilla farm minting.
        if (!tag.m_128471_(MobScaling.TAG_SCALED)) {
            return;
        }
        UUID claimOwner = ScaledMobTracker.findClaimOwnerId(deadId);
        if (claimOwner == null || !claimOwner.equals(killer.m_20148_())) {
            return;
        }
        if (PersistentDataAccess.isWritable(tag)) {
            if (tag.m_128471_(TAG_REWARDED)) {
                return;
            }
            tag.m_128379_(TAG_REWARDED, true);
        } else if (!SESSION_REWARDED.add(deadId)) {
            return;
        }
        if (SESSION_REWARDED.size() > 4096) {
            // Prune instead of wipe — avoids double-pay once the set fills.
            int remove = SESSION_REWARDED.size() / 2;
            var it = SESSION_REWARDED.iterator();
            while (it.hasNext() && remove-- > 0) {
                it.next();
                it.remove();
            }
        }

        DifficultySnapshot snap = DifficultyCache.get(killer);
        PlayerDifficultyData data = DifficultyCache.data(killer);
        UnlockTier unlock = UnlockTier.byId(snap.activeTier);
        boolean elite = EliteSystem.isElite(dead);
        boolean boss = tag.m_128471_(BossScaling.TAG_BOSS);
        double mult = ScalingCurves.rewardMultiplier(Math.max(1L, snap.active));

        AncientCoinEconomy.KillLoot loot = AncientCoinEconomy.rollKillLoot(
                killer, snap.combatRating, elite, boss);
        if (loot.hasPrimary()) {
            AncientCoinEconomy.dropInWorld(dead, loot.primary());
        }
        if (loot.hasBonus()) {
            AncientCoinEconomy.dropInWorld(dead, loot.bonus());
        }
        if (data.isCoinDropChat() && (loot.hasPrimary() || loot.hasBonus())) {
            AncientCoinEconomy.notifyGrant(killer, loot);
        }
        // XP / titles only once a tier is active.
        if (snap.activeTier > 0) {
            grantExperience(killer, mult, elite, boss, unlock);
            TitleSystem.maybeUnlockCombatTitle(killer, snap, elite, boss);
        }
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
