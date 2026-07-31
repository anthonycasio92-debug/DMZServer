package com.dbzlegacy.adaptivedifficulty.ai;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.HostileMobs;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.skills.Skills;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Progressive AI spaced across the full Awakened → Zenith ladder.
 * Early tiers teach pressure; late tiers escalate pack tactics, anti-flight,
 * debuffs, and dodge — so multi-million difficulty keeps unlocking new threat.
 */
public final class AdaptiveAiSystem {
    private AdaptiveAiSystem() {}

    /**
     * Counter ki charging — starts at Master; intensity grows through high tiers.
     */
    public static void onPlayerKiCharge(ServerPlayer player) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableAdaptiveAi || player == null || !(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        AABB box = player.m_20191_().m_82400_(18.0);
        List<Mob> mobs = level.m_45976_(Mob.class, box);
        for (Mob mob : mobs) {
            DifficultyTier tier = resolveTier(mob, MobScaling.difficultyOf(mob));
            if (tier.ordinalPower() < DifficultyTier.MASTER.ordinalPower()) {
                continue;
            }
            mob.m_6710_(player);
            double speed = 1.25 + Math.min(0.75, (tier.ordinalPower() - DifficultyTier.MASTER.ordinalPower()) * 0.06);
            mob.m_21573_().m_5624_(player, speed);

            if (tier.ordinalPower() >= DifficultyTier.GOD.ordinalPower()) {
                player.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 40, 0, false, true)); // SLOWNESS
            }
            if (tier.ordinalPower() >= DifficultyTier.DIVINE.ordinalPower()) {
                player.m_7292_(new MobEffectInstance(MobEffects.f_19613_, 40, 0, false, true)); // WEAKNESS
            }
            if (tier.ordinalPower() >= DifficultyTier.LEGENDARY.ordinalPower()
                    && ThreadLocalRandom.current().nextDouble() < chance(tier, 0.08, 0.28)) {
                player.m_7292_(new MobEffectInstance(MobEffects.f_19604_, 50, 0, false, true)); // NAUSEA
            }
            if (tier.ordinalPower() >= DifficultyTier.IMPOSSIBLE.ordinalPower()
                    && ThreadLocalRandom.current().nextDouble() < 0.12) {
                player.m_7292_(new MobEffectInstance(MobEffects.f_19610_, 30, 0, false, true)); // BLINDNESS
            }
            if (tier.ordinalPower() >= DifficultyTier.OMEGA.ordinalPower()) {
                mob.m_7292_(new MobEffectInstance(MobEffects.f_19600_, 60, 1, false, false)); // STRENGTH
            }
        }
    }

    public static void tick(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        tick(mob, MobScaling.difficultyOf(entity), EliteSystem.isElite(entity));
    }

    public static void tick(Mob mob, long difficulty, boolean elite) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableAdaptiveAi || mob == null || !mob.m_6084_()) {
            return;
        }
        if (!(mob.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        if (difficulty <= 0 && !elite) {
            return;
        }
        DifficultyTier tier = resolveTier(mob, difficulty);
        if (elite && tier.ordinalPower() < DifficultyTier.ELITE.ordinalPower()) {
            tier = DifficultyTier.ELITE;
        }
        if (tier.ordinalPower() < DifficultyTier.AWAKENED.ordinalPower()) {
            return;
        }

        // Never let scaled AI chase other hostiles (pack/ki mistakes → civil war).
        clearHostileTarget(mob);

        LivingEntity target = mob.m_5448_();
        double scan = Math.min(aiRadius(tier), cfg.mobScaleRadius);

        // Awakened+: lock onto a nearby player so kits (gravity/ki/leap) actually fire.
        // Enhanced+ still prefers the weakest target.
        if (tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()) {
            focusWeakest(mob, level, scan);
            target = mob.m_5448_();
        } else if (!(target instanceof Player)) {
            focusNearest(mob, level, Math.min(20.0, scan));
            target = mob.m_5448_();
        }

        // Elite+: tactical retreat when nearly dead
        if (target != null && tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()) {
            maybeRetreat(mob, target, tier);
        }

        // Advanced+: light speed pressure while chasing
        if (target != null && tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()) {
            int amp = tier.ordinalPower() >= DifficultyTier.MYTHIC.ordinalPower() ? 2 : 1;
            mob.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 40, amp, false, false)); // SPEED
        }

        // Master+: call allies onto the same target (pack size scales by tier)
        if (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()) {
            coordinate(mob, level, scan, packSize(tier));
        }

        // Legendary+: anti-flight (moved later so early tiers stay grounded-melee)
        if (target instanceof Player player && tier.ordinalPower() >= DifficultyTier.LEGENDARY.ordinalPower()) {
            antiFlight(mob, player, tier);
        }

        // God+: keep aggression locked on players + jump boost for vertical chase
        if (target instanceof Player && tier.ordinalPower() >= DifficultyTier.GOD.ordinalPower()) {
            mob.m_6710_(target);
            mob.m_7292_(new MobEffectInstance(MobEffects.f_19603_, 40, 1, false, false)); // JUMP_BOOST
        }

        // Impossible+: periodic pressure pulse (slowness)
        if (target instanceof ServerPlayer sp
                && tier.ordinalPower() >= DifficultyTier.IMPOSSIBLE.ordinalPower()
                && mob.f_19797_ % 80 == 0
                && mob.m_20270_(sp) < 10.0f) {
            sp.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 35, 1, false, true));
        }

        // Transcendent+: mining fatigue / hunger pressure in melee range
        if (target instanceof ServerPlayer sp
                && tier.ordinalPower() >= DifficultyTier.TRANSCENDENT.ordinalPower()
                && mob.f_19797_ % 100 == 0
                && mob.m_20270_(sp) < 6.0f) {
            sp.m_7292_(new MobEffectInstance(MobEffects.f_19599_, 60, 0, false, true)); // MINING_FATIGUE
            if (tier.ordinalPower() >= DifficultyTier.ETERNAL.ordinalPower()) {
                sp.m_7292_(new MobEffectInstance(MobEffects.f_19605_, 60, 1, false, true)); // HUNGER
            }
        }

        // Mythic+: strength self-buff
        if (tier.ordinalPower() >= DifficultyTier.MYTHIC.ordinalPower()) {
            int amp = tier.ordinalPower() >= DifficultyTier.APEX.ordinalPower() ? 2 : 1;
            mob.m_7292_(new MobEffectInstance(MobEffects.f_19600_, 50, amp, false, false));
        }

        // Omega+: frenzy — buff nearby scaled allies
        if (tier.ordinalPower() >= DifficultyTier.OMEGA.ordinalPower() && mob.f_19797_ % 120 == 0) {
            frenzyAllies(mob, level, scan);
        }

        // Absolute+: wither touch while in melee
        if (target instanceof ServerPlayer sp
                && tier.ordinalPower() >= DifficultyTier.ABSOLUTE.ordinalPower()
                && mob.m_20270_(sp) < 3.5f
                && mob.f_19797_ % 40 == 0) {
            sp.m_7292_(new MobEffectInstance(MobEffects.f_19615_, 60, 0, false, true)); // WITHER
        }
    }

    public static void onHurt(LivingHurtEvent event) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableAdaptiveAi || event.isCanceled()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob) || entity.m_9236_().f_46443_) {
            return;
        }
        DifficultyTier tier = resolveTier(entity, MobScaling.difficultyOf(entity));
        // Dodge starts at Advanced, stays modest until high tiers
        if (tier.ordinalPower() < DifficultyTier.ADVANCED.ordinalPower()) {
            return;
        }
        double dodge = dodgeChance(tier);
        if (EliteSystem.isElite(entity)) {
            dodge += 0.03;
        }
        if (ThreadLocalRandom.current().nextDouble() < dodge) {
            event.setAmount(0.0f);
            double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
            double force = tier.ordinalPower() >= DifficultyTier.MYTHIC.ordinalPower() ? 0.95 : 0.6;
            entity.m_5997_(Math.cos(angle) * force, 0.15, Math.sin(angle) * force);
        }

        // Divine+: retaliate with brief weakness on the attacker when hit
        if (tier.ordinalPower() >= DifficultyTier.DIVINE.ordinalPower()
                && event.getSource().m_7639_() instanceof ServerPlayer attacker
                && ThreadLocalRandom.current().nextDouble() < chance(tier, 0.15, 0.45)) {
            attacker.m_7292_(new MobEffectInstance(MobEffects.f_19613_, 40, 0, false, true));
        }
    }

    private static DifficultyTier resolveTier(LivingEntity entity, long difficulty) {
        DifficultyTier tier = DifficultyTier.of(difficulty);
        if (EliteSystem.isElite(entity) && tier.ordinalPower() < DifficultyTier.ELITE.ordinalPower()) {
            return DifficultyTier.ELITE;
        }
        return tier;
    }

    private static double aiRadius(DifficultyTier tier) {
        return 16.0 + Math.min(16.0, Math.max(0, tier.ordinalPower() - DifficultyTier.ENHANCED.ordinalPower()) * 1.2);
    }

    private static int packSize(DifficultyTier tier) {
        int base = 2;
        if (tier.ordinalPower() >= DifficultyTier.GOD.ordinalPower()) {
            base = 4;
        }
        if (tier.ordinalPower() >= DifficultyTier.IMPOSSIBLE.ordinalPower()) {
            base = 6;
        }
        if (tier.ordinalPower() >= DifficultyTier.OMEGA.ordinalPower()) {
            base = 8;
        }
        if (tier.ordinalPower() >= DifficultyTier.ZENITH.ordinalPower()) {
            base = 10;
        }
        return base;
    }

    /** Advanced ~5% → Zenith ~32% (was uncapped scaling that maxed too early). */
    private static double dodgeChance(DifficultyTier tier) {
        int steps = Math.max(0, tier.ordinalPower() - DifficultyTier.ADVANCED.ordinalPower());
        return Math.min(0.32, 0.05 + steps * 0.018);
    }

    private static double chance(DifficultyTier tier, double min, double max) {
        double t = Math.max(0, tier.ordinalPower() - DifficultyTier.LEGENDARY.ordinalPower()) / 12.0;
        return min + (max - min) * Math.min(1.0, t);
    }

    private static void focusWeakest(Mob mob, ServerLevel level, double radius) {
        AABB box = mob.m_20191_().m_82400_(Math.max(8.0, radius));
        List<ServerPlayer> players = level.m_45976_(ServerPlayer.class, box);
        ServerPlayer weakest = null;
        float lowest = Float.MAX_VALUE;
        for (ServerPlayer p : players) {
            if (!p.m_6084_() || p.m_5833_()) {
                continue;
            }
            float hp = p.m_21223_();
            if (hp < lowest) {
                lowest = hp;
                weakest = p;
            }
        }
        if (weakest != null) {
            LivingEntity current = mob.m_5448_();
            if (current == null || current.m_21223_() > weakest.m_21223_() + 4.0f) {
                mob.m_6710_(weakest);
            }
        }
    }

    /** Awakened fallback — lock the nearest living player so kits do not idle. */
    private static void focusNearest(Mob mob, ServerLevel level, double radius) {
        AABB box = mob.m_20191_().m_82400_(Math.max(8.0, radius));
        ServerPlayer nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : level.m_45976_(ServerPlayer.class, box)) {
            if (!p.m_6084_() || p.m_5833_()) {
                continue;
            }
            double d = mob.m_20280_(p);
            if (d < best) {
                best = d;
                nearest = p;
            }
        }
        if (nearest != null) {
            mob.m_6710_(nearest);
        }
    }

    private static void antiFlight(Mob mob, Player player, DifficultyTier tier) {
        boolean grounded = false;

        if (player.m_150110_().f_35937_) { // instabuild
            return;
        }

        if (player.m_150110_().f_35935_) {
            player.m_150110_().f_35935_ = false;
            grounded = true;
        }

        try {
            StatsData stats = DmzProgression.stats(player);
            Skills skills = stats == null ? null : stats.getSkills();
            if (skills != null && skills.isSkillActive("fly")) {
                skills.setSkillActive("fly", false);
                grounded = true;
                if (player instanceof ServerPlayer sp) {
                    try {
                        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(sp), sp);
                    } catch (Throwable ignored) {
                    }
                }
            }
            MobEffect flyFx = MainEffects.FLY.get();
            if (flyFx != null && player.m_21023_(flyFx)) {
                player.m_21195_(flyFx);
                grounded = true;
            }
        } catch (Throwable ignored) {
        }

        double airGap = player.m_20186_() - mob.m_20186_();
        double gapNeed = tier.ordinalPower() >= DifficultyTier.APEX.ordinalPower() ? 1.5 : 2.5;
        if (!grounded && !player.m_20096_() && airGap > gapNeed) {
            grounded = true;
        }

        if (!grounded) {
            return;
        }
        double slam = tier.ordinalPower() >= DifficultyTier.ZENITH.ordinalPower() ? -1.35
                : tier.ordinalPower() >= DifficultyTier.OMEGA.ordinalPower() ? -1.15 : -0.85;
        player.m_20256_(new Vec3(0.0, slam, 0.0));
        double dx = player.m_20185_() - mob.m_20185_();
        double dz = player.m_20189_() - mob.m_20189_();
        double leap = tier.ordinalPower() >= DifficultyTier.MYTHIC.ordinalPower() ? 0.65 : 0.45;
        mob.m_5997_(dx * 0.08, leap, dz * 0.08);
    }

    private static void maybeRetreat(Mob mob, LivingEntity target, DifficultyTier tier) {
        float pct = mob.m_21223_() / Math.max(1.0f, mob.m_21233_());
        float trigger = tier.ordinalPower() >= DifficultyTier.GOD.ordinalPower() ? 0.35f : 0.25f;
        if (pct > trigger) {
            return;
        }
        // High tiers prefer fighting through — rarer retreats
        if (tier.ordinalPower() >= DifficultyTier.TRANSCENDENT.ordinalPower()
                && ThreadLocalRandom.current().nextDouble() < 0.65) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(mob);
        long cd = tier.ordinalPower() >= DifficultyTier.DIVINE.ordinalPower() ? 140 : 100;
        long last = tag.m_128454_("dmz_ad_retreat");
        if (mob.f_19797_ - last < cd) {
            return;
        }
        tag.m_128356_("dmz_ad_retreat", mob.f_19797_);
        double dx = mob.m_20185_() - target.m_20185_();
        double dz = mob.m_20189_() - target.m_20189_();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) {
            return;
        }
        mob.m_5997_((dx / len) * 0.9, 0.2, (dz / len) * 0.9);
    }

    private static void coordinate(Mob mob, ServerLevel level, double radius, int maxAllies) {
        LivingEntity target = mob.m_5448_();
        // Pack call is player-only — never point allies at other mobs.
        if (!(target instanceof Player)) {
            return;
        }
        AABB box = mob.m_20191_().m_82400_(Math.min(28.0, radius));
        List<Mob> allies = level.m_45976_(Mob.class, box);
        int shared = 0;
        for (Mob ally : allies) {
            if (ally == mob || !ally.m_6084_()) {
                continue;
            }
            clearHostileTarget(ally);
            if (ally.m_5448_() != null) {
                continue;
            }
            if (!PersistentDataAccess.flag(ally, MobScaling.TAG_SCALED)
                    && !PersistentDataAccess.flag(ally, EliteSystem.TAG_ELITE)) {
                continue;
            }
            ally.m_6710_(target);
            shared++;
            if (shared >= maxAllies) {
                break;
            }
        }
    }

    /** Drop targets that are other hostiles so mobs don't farm each other. */
    private static void clearHostileTarget(Mob mob) {
        if (mob == null) {
            return;
        }
        LivingEntity target = mob.m_5448_();
        if (target != null && !(target instanceof Player) && HostileMobs.isHostile(target)) {
            mob.m_6710_(null);
        }
    }

    private static void frenzyAllies(Mob mob, ServerLevel level, double radius) {
        AABB box = mob.m_20191_().m_82400_(Math.min(20.0, radius));
        for (Mob ally : level.m_45976_(Mob.class, box)) {
            if (!ally.m_6084_()) {
                continue;
            }
            if (!PersistentDataAccess.flag(ally, MobScaling.TAG_SCALED)
                    && ally != mob) {
                continue;
            }
            ally.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 80, 1, false, false));
            ally.m_7292_(new MobEffectInstance(MobEffects.f_19600_, 80, 0, false, false));
        }
    }
}
