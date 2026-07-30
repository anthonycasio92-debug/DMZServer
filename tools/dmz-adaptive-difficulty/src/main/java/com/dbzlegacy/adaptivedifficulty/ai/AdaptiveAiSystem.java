package com.dbzlegacy.adaptivedifficulty.ai;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
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

/** Concept §14 — progressive AI unlocked by difficulty tiers. */
public final class AdaptiveAiSystem {
    private AdaptiveAiSystem() {}

    /**
     * Counter ki charging: nearby Advanced+ mobs rush the charging player and apply pressure.
     */
    public static void onPlayerKiCharge(ServerPlayer player) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableAdaptiveAi || player == null || !(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        AABB box = player.m_20191_().m_82400_(16.0);
        List<Mob> mobs = level.m_45976_(Mob.class, box);
        for (Mob mob : mobs) {
            long difficulty = MobScaling.difficultyOf(mob);
            DifficultyTier tier = DifficultyTier.of(difficulty);
            if (EliteSystem.isElite(mob) && tier.ordinalPower() < DifficultyTier.ELITE.ordinalPower()) {
                tier = DifficultyTier.ELITE;
            }
            if (tier.ordinalPower() < DifficultyTier.ADVANCED.ordinalPower()) {
                continue;
            }
            mob.m_6710_(player);
            mob.m_21573_().m_5624_(player, 1.4);
            if (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()) {
                player.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 30, 0, false, true)); // SLOWNESS
            }
            // Interrupt feel: brief blindness / nausea at high tiers
            if (tier.ordinalPower() >= DifficultyTier.LEGENDARY.ordinalPower()
                    && ThreadLocalRandom.current().nextDouble() < 0.15) {
                player.m_7292_(new MobEffectInstance(MobEffects.f_19604_, 40, 0, false, true)); // NAUSEA
            }
        }
    }

    public static void tick(LivingEntity entity) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableAdaptiveAi || entity == null || !(entity instanceof Mob mob)) {
            return;
        }
        if (!(entity.m_9236_() instanceof ServerLevel level) || !entity.m_6084_()) {
            return;
        }
        // Throttle: every 20 ticks
        if (entity.f_19797_ % 20 != 0) {
            return;
        }
        long difficulty = MobScaling.difficultyOf(entity);
        if (difficulty <= 0 && !EliteSystem.isElite(entity)) {
            return;
        }
        DifficultyTier tier = DifficultyTier.of(difficulty);
        if (EliteSystem.isElite(entity) && tier.ordinalPower() < DifficultyTier.ELITE.ordinalPower()) {
            tier = DifficultyTier.ELITE;
        }

        LivingEntity target = mob.m_5448_();
        if (tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()) {
            focusWeakest(mob, level, cfg.mobScaleRadius);
            target = mob.m_5448_();
        }
        if (target instanceof Player player && tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()) {
            antiFlight(mob, player);
        }
        if (target != null && tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()) {
            maybeRetreat(mob, target);
        }
        if (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()) {
            coordinate(mob, level, cfg.mobScaleRadius);
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
        long difficulty = MobScaling.difficultyOf(entity);
        DifficultyTier tier = DifficultyTier.of(difficulty);
        if (EliteSystem.isElite(entity) && tier.ordinalPower() < DifficultyTier.ELITE.ordinalPower()) {
            tier = DifficultyTier.ELITE;
        }
        if (tier.ordinalPower() < DifficultyTier.ADVANCED.ordinalPower()) {
            return;
        }
        // Dodge chance scales with tier
        double dodge = 0.05 * (tier.ordinalPower() - DifficultyTier.ENHANCED.ordinalPower());
        if (EliteSystem.isElite(entity)) {
            dodge += 0.05;
        }
        if (ThreadLocalRandom.current().nextDouble() < Math.min(0.35, dodge)) {
            event.setAmount(0.0f);
            // Sidestep impulse
            double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
            entity.m_5997_(Math.cos(angle) * 0.6, 0.15, Math.sin(angle) * 0.6);
        }
    }

    private static void focusWeakest(Mob mob, ServerLevel level, double radius) {
        AABB box = mob.m_20191_().m_82400_(Math.max(8.0, radius));
        List<ServerPlayer> players = level.m_45976_(ServerPlayer.class, box);
        ServerPlayer weakest = null;
        float lowest = Float.MAX_VALUE;
        for (ServerPlayer p : players) {
            if (!p.m_6084_()) {
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

    private static void antiFlight(Mob mob, Player player) {
        boolean grounded = false;

        // Vanilla creative / ability flying
        if (player.m_150110_().f_35935_) {
            player.m_150110_().f_35935_ = false;
            grounded = true;
        }

        // DragonMineZ Fly skill + MainEffects.FLY (concept §14 Countering flight)
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
            // DMZ APIs unavailable
        }

        // Airborne chase pressure even if skill toggle raced us
        if (!grounded && !player.m_20096_() && player.m_20186_() - mob.m_20186_() > 2.5) {
            grounded = true;
        }

        if (!grounded) {
            return;
        }
        player.m_20256_(new Vec3(0.0, -0.85, 0.0));
        double dx = player.m_20185_() - mob.m_20185_();
        double dz = player.m_20189_() - mob.m_20189_();
        mob.m_5997_(dx * 0.08, 0.45, dz * 0.08);
    }

    private static void maybeRetreat(Mob mob, LivingEntity target) {
        float pct = mob.m_21223_() / Math.max(1.0f, mob.m_21233_());
        if (pct > 0.25f) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(mob);
        long last = tag.m_128454_("dmz_ad_retreat");
        if (mob.f_19797_ - last < 100) {
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

    private static void coordinate(Mob mob, ServerLevel level, double radius) {
        LivingEntity target = mob.m_5448_();
        if (target == null) {
            return;
        }
        AABB box = mob.m_20191_().m_82400_(Math.min(24.0, radius));
        List<Mob> allies = level.m_45976_(Mob.class, box);
        int shared = 0;
        for (Mob ally : allies) {
            if (ally == mob || !ally.m_6084_()) {
                continue;
            }
            if (MobScaling.difficultyOf(ally) <= 0 && !EliteSystem.isElite(ally)) {
                continue;
            }
            if (ally.m_5448_() == null) {
                ally.m_6710_(target);
                shared++;
                if (shared >= 4) {
                    break;
                }
            }
        }
    }
}
