package com.dbzlegacy.adaptivedifficulty.reward;

import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.ScalingCurves;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dragonminez.common.init.MainItems;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * V3 kill rewards: Ancient Coins (primary), XP, rare drops, capsules, titles.
 * Training Points / Potential are never granted here.
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
        // Cached snapshot — avoid full refresh on every kill.
        DifficultySnapshot snap = DifficultyCache.get(killer);
        long killDifficulty = Math.max(snap.combatRating, MobScaling.difficultyOf(dead));
        DifficultyTier tier = DifficultyTier.of(Math.max(snap.active, killDifficulty / 10));
        boolean elite = EliteSystem.isElite(dead);
        boolean boss = PersistentDataAccess.get(dead).m_128471_(BossScaling.TAG_BOSS);
        boolean mutated = MutationSystem.get(dead) != null;
        double mult = ScalingCurves.rewardMultiplier(Math.max(1L, snap.active));

        AncientCoinEconomy.Drop drop = AncientCoinEconomy.rollKillDrop(killer, snap.combatRating, elite, boss);
        if (drop.count() > 0) {
            // World drop at the mob — never inject into the killer's inventory.
            AncientCoinEconomy.dropInWorld(dead, drop);
            AncientCoinEconomy.notifyGrant(killer, drop);
        }
        grantExperience(killer, mult, elite, boss, tier);
        grantDrops(killer, mult, elite, boss, mutated, tier);
        TitleSystem.maybeUnlockCombatTitle(killer, snap, elite, boss, tier);
    }

    private static void grantExperience(
            ServerPlayer killer, double mult, boolean elite, boolean boss, DifficultyTier tier
    ) {
        int xp = (int) Math.round(3.0 * Math.max(1.0, mult) * Math.max(1, tier.ordinalPower()));
        if (elite) {
            xp *= 2;
        }
        if (boss) {
            xp *= 4;
        }
        if (xp > 0) {
            killer.m_6756_(xp); // giveExperiencePoints
        }
    }

    private static void grantDrops(
            ServerPlayer killer, double mult, boolean elite, boolean boss, boolean mutated, DifficultyTier tier
    ) {
        double rareChance = 0.02 * Math.max(1.0, mult) * Math.max(1, tier.ordinalPower());
        if (elite) {
            rareChance += 0.08;
        }
        if (boss) {
            rareChance += 0.25;
        }
        if (mutated) {
            rareChance += 0.05;
        }
        rareChance = Math.min(0.85, rareChance);

        if (ThreadLocalRandom.current().nextDouble() < rareChance) {
            give(killer, item("minecraft", "emerald"), 1 + (elite ? 1 : 0));
        }
        if (elite && ThreadLocalRandom.current().nextDouble() < 0.35) {
            give(killer, item("minecraft", "diamond"), 1);
        }
        if (boss && ThreadLocalRandom.current().nextDouble() < 0.5) {
            give(killer, item("minecraft", "netherite_scrap"), 1);
            give(killer, item("minecraft", "nether_star"), 1);
        }
        if ((elite || boss || mutated) && ThreadLocalRandom.current().nextDouble() < Math.min(0.6, 0.1 * mult)) {
            giveCapsule(killer, boss);
        }
    }

    private static void giveCapsule(ServerPlayer killer, boolean boss) {
        try {
            Item capsule;
            if (boss) {
                capsule = MainItems.ORANGE_CAPSULE.get();
            } else {
                Item[] pool = new Item[] {
                        MainItems.RED_CAPSULE.get(),
                        MainItems.GREEN_CAPSULE.get(),
                        MainItems.BLUE_CAPSULE.get(),
                        MainItems.YELLOW_CAPSULE.get(),
                        MainItems.PURPLE_CAPSULE.get()
                };
                capsule = pool[ThreadLocalRandom.current().nextInt(pool.length)];
            }
            if (capsule != null) {
                give(killer, capsule, 1);
            }
        } catch (Throwable ignored) {
            // Capsules unavailable if DMZ items not ready
        }
    }

    private static Item item(String ns, String path) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(ns, path));
    }

    private static void give(ServerPlayer player, Item item, int count) {
        if (item == null || count <= 0) {
            return;
        }
        ItemStack stack = new ItemStack(item, count);
        if (!player.m_150109_().m_36054_(stack)) {
            player.m_36176_(stack, false);
        }
    }
}
