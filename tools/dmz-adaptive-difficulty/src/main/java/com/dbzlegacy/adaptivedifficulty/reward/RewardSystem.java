package com.dbzlegacy.adaptivedifficulty.reward;

import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.calc.ScalingCurves;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Concept §15 — reward scaling: TP, XP, rare drops, capsules, titles.
 * <p>
 * Potential unlock ({@code potentialunlock}) is owned by the CustomNPCs
 * {@code Potential.js} script — this mod must not write that skill.
 * <p>
 * Kill TP comes from the mob's max health only (no difficulty / elite / boss TP multipliers).
 * Harder difficulties pay more TP because the mob has more HP after retarget scaling.
 * Training {@code TPGainEvent} is never multiplied by this mod.
 */
public final class RewardSystem {
    /**
     * Legacy flag — TP multipliers are removed; kept so older call sites compile.
     */
    public static final ThreadLocal<Boolean> SKIP_TP_EVENT_SCALE = ThreadLocal.withInitial(() -> false);

    private RewardSystem() {}

    public static void onKill(ServerPlayer killer, LivingEntity dead) {
        if (killer == null || dead == null || dead instanceof Player) {
            return;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableRewardScaling) {
            return;
        }
        DifficultySnapshot snap = DifficultyCache.refresh(killer);
        long killDifficulty = Math.max(snap.active, MobScaling.difficultyOf(dead));
        DifficultyTier tier = DifficultyTier.of(killDifficulty);
        boolean elite = EliteSystem.isElite(dead);
        boolean boss = PersistentDataAccess.get(dead).m_128471_(BossScaling.TAG_BOSS);
        boolean mutated = MutationSystem.get(dead) != null;

        grantTrainingPoints(killer, dead, cfg);
        // XP/drops use tier only — no difficulty TP-style multiplier.
        grantExperience(killer, 1.0, elite, boss, tier);
        grantDrops(killer, 1.0, elite, boss, mutated, tier);
        maybeUnlockTitle(killer, snap, elite, boss, tier);
    }

    private static void grantTrainingPoints(
            ServerPlayer killer,
            LivingEntity dead,
            DifficultyConfig cfg
    ) {
        // Extra TP comes from the mob having more health after difficulty scaling —
        // not from a separate difficulty / elite / boss TP multiplier.
        double tp = ScalingCurves.killTrainingPointsFromHealth(dead.m_21233_());
        if (tp <= 0.0) {
            return;
        }
        float grant = (float) Math.min(Float.MAX_VALUE, tp);
        if (grant <= 0.0f) {
            return;
        }
        StatsData stats = DmzProgression.stats(killer);
        if (stats == null) {
            return;
        }
        Resources resources = stats.getResources();
        if (resources == null) {
            return;
        }
        SKIP_TP_EVENT_SCALE.set(true);
        try {
            resources.addTrainingPoints(grant);
        } finally {
            SKIP_TP_EVENT_SCALE.set(false);
        }
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(killer), killer);
        } catch (Throwable ignored) {
        }
    }

    private static void grantExperience(
            ServerPlayer killer, double mult, boolean elite, boolean boss, DifficultyTier tier
    ) {
        int xp = (int) Math.round(3.0 * mult * Math.max(1, tier.ordinalPower()));
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
        double rareChance = 0.02 * mult * Math.max(1, tier.ordinalPower());
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
        // Capsules (cosmetics / loot)
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

    private static void maybeUnlockTitle(
            ServerPlayer killer, DifficultySnapshot snap, boolean elite, boolean boss, DifficultyTier tier
    ) {
        PlayerDifficultyData data = DifficultyCache.data(killer);
        String title = null;
        if (boss && tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()) {
            title = "Boss Slayer";
        } else if (elite && snap.active >= DifficultyTier.LEGENDARY.threshold()) {
            title = "Legendary Hunter";
        } else if (snap.active >= DifficultyTier.IMPOSSIBLE.threshold()) {
            title = "Impossible";
        } else if (snap.active >= DifficultyTier.DIVINE.threshold()) {
            // Divine (50k) must be checked before God (10k) or it is unreachable.
            title = "Divine";
        } else if (snap.active >= DifficultyTier.GOD.threshold()) {
            title = "God Challenger";
        }
        if (title != null && data.unlockTitle(title)) {
            DifficultyCache.save(killer);
            killer.m_213846_(Component.m_237113_("§6✦ Title unlocked: §e" + title));
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
            player.m_36176_(stack, false); // drop if full — check method
        }
    }
}
