package com.dbzlegacy.adaptivedifficulty.evolution;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Concept §11 — enemy evolution unlocked by difficulty tiers.
 * <p>
 * Specific kits for creepers / zombies / skeletons / endermen / wardens,
 * plus shared melee / ranged packages so <b>all hostiles</b> evolve.
 */
public final class EnemyEvolution {
    public static final String TAG_EVOLVED = "dmz_ad_evolved";

    private EnemyEvolution() {}

    /** Any hostile the system should evolve (vanilla + modded MONSTER / Enemy). */
    public static boolean isEvolvable(Mob mob) {
        if (mob == null) {
            return false;
        }
        if (mob instanceof Monster
                || mob instanceof Enemy
                || mob instanceof Warden
                || mob instanceof AbstractPiglin
                || mob instanceof Raider) {
            return true;
        }
        try {
            return mob.m_6095_().m_20674_() == MobCategory.MONSTER;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void tick(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        tick(mob, MobScaling.difficultyOf(entity), EliteSystem.isElite(entity));
    }

    /** Prefer this when the caller already resolved difficulty / elite. */
    public static void tick(Mob mob, long difficulty, boolean elite) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableEnemyEvolution || mob == null || !mob.m_6084_()) {
            return;
        }
        if (!(mob.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        if (difficulty <= 0 && !elite) {
            return;
        }
        DifficultyTier tier = DifficultyTier.of(difficulty);
        if (elite && tier.ordinalPower() < DifficultyTier.ELITE.ordinalPower()) {
            tier = DifficultyTier.ELITE;
        }
        if (tier.ordinalPower() < DifficultyTier.AWAKENED.ordinalPower()) {
            return;
        }

        markEvolved(mob, tier);

        LivingEntity target = mob.m_5448_();
        if (mob instanceof Creeper creeper) {
            creeperTick(creeper, level, target, tier);
        } else if (mob instanceof Zombie zombie) {
            // Husk / Drowned / Zombie Villager / Zombified Piglin
            meleeTick(zombie, target, tier);
        } else if (mob instanceof AbstractSkeleton skeleton) {
            // Skeleton / Stray / Wither Skeleton
            rangedKiTick(skeleton, target, tier);
        } else if (mob instanceof EnderMan enderMan) {
            endermanTick(enderMan, level, target, tier);
        } else if (mob instanceof Warden warden) {
            wardenTick(warden, level, target, tier);
        } else if (isRangedStyle(mob)) {
            rangedKiTick(mob, target, tier);
        } else {
            meleeTick(mob, target, tier);
        }
    }

    private static boolean isRangedStyle(Mob mob) {
        return mob instanceof RangedAttackMob
                || mob instanceof Witch
                || mob instanceof Blaze
                || mob instanceof Ghast
                || mob instanceof Guardian
                || mob instanceof Shulker
                || mob instanceof Raider && !(mob instanceof net.minecraft.world.entity.monster.Vindicator)
                        && !(mob instanceof net.minecraft.world.entity.monster.Ravager);
    }

    /** One-time mark so players can tell the mob is evolved (glow pulse + name hint). */
    private static void markEvolved(Mob mob, DifficultyTier tier) {
        if (tier.ordinalPower() < DifficultyTier.ENHANCED.ordinalPower()) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(mob);
        if (tag.m_128471_(TAG_EVOLVED)) {
            return;
        }
        tag.m_128379_(TAG_EVOLVED, true);
        // Brief glow so evolution is visible in the field
        mob.m_7292_(new MobEffectInstance(MobEffects.f_19619_, 100, 0, false, false)); // GLOWING
        if (!mob.m_8077_()) { // hasCustomName
            String typeName = String.valueOf(mob.m_6095_().m_20675_());
            mob.m_6593_(Component.m_237113_("§6" + tier.display + " §f" + typeName));
            mob.m_20340_(true); // setCustomNameVisible
        }
    }

    private static void creeperTick(Creeper creeper, ServerLevel level, LivingEntity target, DifficultyTier tier) {
        scaleCreeperBlast(creeper, tier);
        if (target != null && creeper.m_20270_(target) < 4.0f
                && tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()) {
            creeper.m_32314_(); // ignite
        }
        if (target != null && tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()) {
            creeper.m_21573_().m_26519_(target.m_20185_(), target.m_20186_(), target.m_20189_(), 1.35);
        }
        CompoundTag tag = PersistentDataAccess.get(creeper);
        if (!tag.m_128471_("dmz_ad_final_boom")
                && creeper.m_21223_() <= creeper.m_21233_() * 0.15f
                && tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()) {
            tag.m_128379_("dmz_ad_final_boom", true);
            float power = tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower() ? 7.0f : 5.0f;
            if (tier.ordinalPower() >= DifficultyTier.GOD.ordinalPower()) {
                power = 9.0f;
            }
            level.m_254849_(creeper, creeper.m_20185_(), creeper.m_20186_(), creeper.m_20189_(),
                    power, Level.ExplosionInteraction.MOB);
        }
    }

    private static void scaleCreeperBlast(Creeper creeper, DifficultyTier tier) {
        if (tier.ordinalPower() < DifficultyTier.ENHANCED.ordinalPower()) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(creeper);
        if (tag.m_128471_("dmz_ad_blast_scaled")) {
            return;
        }
        tag.m_128379_("dmz_ad_blast_scaled", true);
        int bonus = Math.max(0, tier.ordinalPower() - DifficultyTier.AWAKENED.ordinalPower());
        try {
            Field radius = Creeper.class.getDeclaredField("bV");
            radius.setAccessible(true);
            int base = radius.getInt(creeper);
            radius.setInt(creeper, Math.min(16, base + bonus));

            Field swell = Creeper.class.getDeclaredField("bU");
            swell.setAccessible(true);
            int maxSwell = swell.getInt(creeper);
            swell.setInt(creeper, Math.max(10, maxSwell - (bonus * 4)));
        } catch (Throwable ignored) {
        }
    }

    /** Shared melee kit: leap / rush / ground slam / berserker (concept zombie examples). */
    private static void meleeTick(Mob mob, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(mob);
        long age = mob.f_19797_;
        double dist = mob.m_20270_(target);

        if (dist > 3.0 && dist < 12.0 && age - tag.m_128454_("dmz_ad_leap") > 60
                && tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()) {
            tag.m_128356_("dmz_ad_leap", age);
            double dx = target.m_20185_() - mob.m_20185_();
            double dz = target.m_20189_() - mob.m_20189_();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0.001) {
                mob.m_5997_((dx / len) * 1.15, 0.55, (dz / len) * 1.15);
            }
        }
        if (dist < 16.0 && tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()) {
            mob.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 40, 1, false, false)); // SPEED
        }
        if (dist < 3.5 && tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower() && age % 80 == 0) {
            target.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 60, 1, false, true)); // SLOWNESS
            target.m_5997_(0, 0.35, 0);
            AABB box = mob.m_20191_().m_82400_(3.0);
            if (mob.m_9236_() instanceof ServerLevel level) {
                for (ServerPlayer p : level.m_45976_(ServerPlayer.class, box)) {
                    if (p != target) {
                        p.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 40, 0, false, true));
                    }
                }
            }
        }
        if (mob.m_21223_() < mob.m_21233_() * 0.35f
                && tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()) {
            mob.m_7292_(new MobEffectInstance(MobEffects.f_19600_, 80, 1, false, true)); // STRENGTH
            mob.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 80, 2, false, false));
        }
    }

    /** Shared ranged kit: real DMZ ki blast / laser / beam / charged beam. */
    private static void rangedKiTick(Mob mob, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            return;
        }
        float dist = mob.m_20270_(target);
        if (dist > 28.0f) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(mob);
        long age = mob.f_19797_;

        if (tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_blast") >= 45
                && dist < 18.0f) {
            if (KiAttackHelper.fireKiBlast(mob, target, tier)) {
                tag.m_128356_("dmz_ad_ki_blast", age);
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_laser") >= 70
                && dist < 22.0f) {
            if (KiAttackHelper.fireKiLaser(mob, target, tier)) {
                tag.m_128356_("dmz_ad_ki_laser", age);
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()) {
            mob.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 40, 0, false, false));
            if (age - tag.m_128454_("dmz_ad_ki_beam") >= 100 && dist < 24.0f) {
                if (KiAttackHelper.fireKiBeam(mob, target, tier, false)) {
                    tag.m_128356_("dmz_ad_ki_beam", age);
                }
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_charged") >= 140
                && dist < 26.0f) {
            if (KiAttackHelper.fireKiBeam(mob, target, tier, true)) {
                tag.m_128356_("dmz_ad_ki_charged", age);
            }
        }
    }

    private static void endermanTick(EnderMan ender, ServerLevel level, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(ender);
        if (tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower() && ender.f_19797_ % 50 == 0
                && ender.m_20270_(target) < 12.0f) {
            target.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 40, 2, false, true));
            target.m_7292_(new MobEffectInstance(MobEffects.f_19610_, 40, 0, false, true));
        }
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && ender.f_19797_ - tag.m_128454_("dmz_ad_tp") > 80) {
            tag.m_128356_("dmz_ad_tp", ender.f_19797_);
            double ox = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.5;
            double oz = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.5;
            ender.m_6021_(target.m_20185_() + ox, target.m_20186_(), target.m_20189_() + oz);
        }
    }

    private static void wardenTick(Warden warden, ServerLevel level, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(warden);
        long age = warden.f_19797_;
        float dist = warden.m_20270_(target);

        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_barrage") >= 70
                && dist < 20.0f) {
            int shots = 2 + Math.min(4, tier.ordinalPower() / 2);
            if (KiAttackHelper.fireKiBarrage(warden, target, tier, shots) > 0) {
                tag.m_128356_("dmz_ad_ki_barrage", age);
            }
            if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()) {
                target.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 40, 1, false, true));
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_beam") >= 110
                && dist < 24.0f) {
            if (KiAttackHelper.fireKiBeam(warden, target, tier,
                    tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower())) {
                tag.m_128356_("dmz_ad_ki_beam", age);
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower() && age % 80 == 0) {
            AABB box = warden.m_20191_().m_82400_(8.0);
            List<ServerPlayer> players = level.m_45976_(ServerPlayer.class, box);
            for (ServerPlayer p : players) {
                p.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 40, 1, false, true));
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()
                && age - tag.m_128454_("dmz_ad_tp") >= 120
                && dist > 6.0f) {
            tag.m_128356_("dmz_ad_tp", age);
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
