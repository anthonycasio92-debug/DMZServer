package com.dbzlegacy.adaptivedifficulty.evolution;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Concept §11 — per-mob-type evolution abilities unlocked by difficulty tiers.
 */
public final class EnemyEvolution {
    private EnemyEvolution() {}

    public static void tick(LivingEntity entity) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableEnemyEvolution || entity == null || !(entity instanceof Mob mob)) {
            return;
        }
        if (!(entity.m_9236_() instanceof ServerLevel level) || !entity.m_6084_()) {
            return;
        }
        if (entity.f_19797_ % 10 != 0) {
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
        if (tier.ordinalPower() < DifficultyTier.AWAKENED.ordinalPower()) {
            return;
        }

        LivingEntity target = mob.m_5448_();
        if (entity instanceof Creeper creeper) {
            creeperTick(creeper, level, target, tier);
        } else if (entity instanceof Zombie zombie) {
            zombieTick(zombie, target, tier);
        } else if (entity instanceof Skeleton skeleton) {
            skeletonTick(skeleton, target, tier);
        } else if (entity instanceof EnderMan enderMan) {
            endermanTick(enderMan, level, target, tier);
        } else if (entity instanceof Warden warden) {
            wardenTick(warden, level, target, tier);
        }
    }

    private static void creeperTick(Creeper creeper, ServerLevel level, LivingEntity target, DifficultyTier tier) {
        // Faster fuse / primed when close
        if (target != null && creeper.m_20270_(target) < 4.0f && tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()) {
            creeper.m_32314_(); // ignite
        }
        // Tracking: keep chasing aggressively
        if (target != null && tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()) {
            creeper.m_21573_().m_26519_(target.m_20185_(), target.m_20186_(), target.m_20189_(), 1.35);
        }
        CompoundTag tag = PersistentDataAccess.get(creeper);
        if (!tag.m_128471_("dmz_ad_final_boom")
                && creeper.m_21223_() <= creeper.m_21233_() * 0.15f
                && tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()) {
            tag.m_128379_("dmz_ad_final_boom", true);
            float power = tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower() ? 6.0f : 4.0f;
            level.m_254849_(creeper, creeper.m_20185_(), creeper.m_20186_(), creeper.m_20189_(),
                    power, Level.ExplosionInteraction.MOB);
        }
    }

    private static void zombieTick(Zombie zombie, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(zombie);
        long last = tag.m_128454_("dmz_ad_leap");
        double dist = zombie.m_20270_(target);
        // Leap
        if (dist > 3.0 && dist < 10.0 && zombie.f_19797_ - last > 60
                && tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()) {
            tag.m_128356_("dmz_ad_leap", zombie.f_19797_);
            double dx = target.m_20185_() - zombie.m_20185_();
            double dz = target.m_20189_() - zombie.m_20189_();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0.001) {
                zombie.m_5997_((dx / len) * 1.1, 0.55, (dz / len) * 1.1);
            }
        }
        // Rush
        if (dist < 16.0 && tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()) {
            zombie.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 40, 1, false, false)); // SPEED
        }
        // Ground slam AOE slow
        if (dist < 3.0 && tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()
                && zombie.f_19797_ % 80 == 0) {
            target.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 60, 1, false, true)); // SLOWNESS
            target.m_5997_(0, 0.35, 0);
        }
        // Berserker
        if (zombie.m_21223_() < zombie.m_21233_() * 0.35f
                && tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()) {
            zombie.m_7292_(new MobEffectInstance(MobEffects.f_19600_, 80, 1, false, true)); // STRENGTH
            zombie.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 80, 2, false, false));
        }
    }

    private static void skeletonTick(Skeleton skeleton, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            return;
        }
        // "Ki blast" / laser stand-in: ranged pressure via glowing + wither/poison bolts feel
        if (tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower() && skeleton.f_19797_ % 40 == 0
                && skeleton.m_20270_(target) < 18.0f) {
            target.m_7292_(new MobEffectInstance(MobEffects.f_19615_, 40, 0, false, true)); // WITHER brief
        }
        if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()) {
            skeleton.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 40, 0, false, false));
        }
        // Charged beam: heavy burst
        if (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()
                && skeleton.f_19797_ % 100 == 0 && skeleton.m_20270_(target) < 20.0f) {
            target.m_6469_(skeleton.m_269291_().m_269333_(skeleton), 4.0f + tier.ordinalPower());
        }
    }

    private static void endermanTick(EnderMan ender, ServerLevel level, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(ender);
        // Gravity
        if (tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower() && ender.f_19797_ % 50 == 0
                && ender.m_20270_(target) < 12.0f) {
            target.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 40, 2, false, true)); // SLOWNESS heavy
            target.m_7292_(new MobEffectInstance(MobEffects.f_19610_, 40, 0, false, true)); // BLINDNESS "solar flare"
        }
        // Teleport combo toward player
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && ender.f_19797_ - tag.m_128454_("dmz_ad_tp") > 80) {
            tag.m_128356_("dmz_ad_tp", ender.f_19797_);
            double ox = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.5;
            double oz = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.5;
            ender.m_6021_(target.m_20185_() + ox, target.m_20186_(), target.m_20189_() + oz);
        }
        // Counter teleport when hurt recently handled in onHurt
    }

    private static void wardenTick(Warden warden, ServerLevel level, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            return;
        }
        // Ki barrage stand-in: pulse damage to nearby players
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower() && warden.f_19797_ % 60 == 0) {
            AABB box = warden.m_20191_().m_82400_(8.0);
            List<ServerPlayer> players = level.m_45976_(ServerPlayer.class, box);
            for (ServerPlayer p : players) {
                p.m_6469_(warden.m_269291_().m_269333_(warden), 3.0f + tier.ordinalPower());
                if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()) {
                    p.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 40, 1, false, true));
                }
            }
        }
        // Teleport hop
        if (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()
                && warden.f_19797_ % 120 == 0 && warden.m_20270_(target) > 6.0f) {
            warden.m_6021_(target.m_20185_(), target.m_20186_(), target.m_20189_());
        }
    }

    /** Counter-teleport for endermen when damaged. */
    public static void onHurt(LivingEntity entity) {
        if (!(entity instanceof EnderMan ender) || entity.m_9236_().f_46443_) {
            return;
        }
        long difficulty = MobScaling.difficultyOf(entity);
        DifficultyTier tier = DifficultyTier.of(difficulty);
        if (tier.ordinalPower() < DifficultyTier.ADVANCED.ordinalPower()) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < 0.35) {
            LivingEntity target = ender.m_5448_();
            if (target != null) {
                double ox = (ThreadLocalRandom.current().nextDouble() - 0.5) * 4.0;
                double oz = (ThreadLocalRandom.current().nextDouble() - 0.5) * 4.0;
                ender.m_6021_(target.m_20185_() + ox, target.m_20186_(), target.m_20189_() + oz);
            }
        }
    }
}
