package com.dbzlegacy.adaptivedifficulty.evolution;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationType;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.util.EntityDisplayNames;
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
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Per-mob evolution kits unlocked by difficulty tiers.
 * Endermen / Wardens apply stacked DMZ gravity-chamber pressure to aggro'd players.
 */
public final class EnemyEvolution {
    public static final String TAG_EVOLVED = "dmz_ad_evolved";

    private EnemyEvolution() {}

    public static boolean isEvolvable(Mob mob) {
        if (mob == null) {
            return false;
        }
        if (mob instanceof Monster
                || mob instanceof Enemy
                || mob instanceof Warden
                || mob instanceof AbstractPiglin
                || mob instanceof Hoglin
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
            creeperTick(creeper, level, target, tier, difficulty);
        } else if (mob instanceof ZombifiedPiglin zp) {
            zombiePiglinTick(zp, target, tier);
        } else if (mob instanceof Zombie zombie) {
            zombieTick(zombie, target, tier);
        } else if (mob instanceof AbstractSkeleton skeleton) {
            skeletonTick(skeleton, target, tier);
        } else if (mob instanceof EnderMan enderMan) {
            endermanTick(enderMan, level, target, tier, difficulty);
        } else if (mob instanceof Warden warden) {
            wardenTick(warden, level, target, tier, difficulty);
        } else if (mob instanceof Blaze blaze) {
            blazeTick(blaze, target, tier);
        } else if (mob instanceof Ghast ghast) {
            ghastTick(ghast, target, tier);
        } else if (mob instanceof Hoglin hoglin) {
            hoglinTick(hoglin, target, tier);
        } else if (mob instanceof AbstractPiglin piglin) {
            piglinTick(piglin, target, tier);
        } else if (isRangedStyle(mob)) {
            skeletonTick(mob, target, tier);
        } else {
            zombieTick(mob, target, tier);
        }
    }

    private static boolean isRangedStyle(Mob mob) {
        return mob instanceof RangedAttackMob
                || mob instanceof Witch
                || mob instanceof Guardian
                || mob instanceof Shulker
                || mob instanceof Raider && !(mob instanceof net.minecraft.world.entity.monster.Vindicator)
                        && !(mob instanceof net.minecraft.world.entity.monster.Ravager);
    }

    private static void markEvolved(Mob mob, DifficultyTier tier) {
        if (tier.ordinalPower() < DifficultyTier.ENHANCED.ordinalPower()) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(mob);
        if (tag.m_128471_(TAG_EVOLVED)) {
            return;
        }
        tag.m_128379_(TAG_EVOLVED, true);
        mob.m_7292_(new MobEffectInstance(MobEffects.f_19619_, 100, 0, false, false));
        if (!mob.m_8077_()) {
            String typeName = EntityDisplayNames.of(mob);
            mob.m_6593_(Component.m_237113_("§6" + tier.display + " §f" + typeName));
            mob.m_20340_(true);
        }
    }

    // ── Creepers: always Final Explosion, radius/fuse/damage scale ──────────

    private static void creeperTick(
            Creeper creeper, ServerLevel level, LivingEntity target, DifficultyTier tier, long difficulty) {
        scaleCreeperBlast(creeper, tier, difficulty);
        if (target != null && creeper.m_20270_(target) < 5.0f
                && tier.ordinalPower() >= DifficultyTier.AWAKENED.ordinalPower()) {
            creeper.m_32314_(); // always push toward detonation
        }
        if (target != null && tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()) {
            creeper.m_21573_().m_26519_(target.m_20185_(), target.m_20186_(), target.m_20189_(), 1.4);
        }
    }

    /** Called on creeper death — guaranteed final explosion. */
    public static void onCreeperDeath(Creeper creeper) {
        if (creeper == null || creeper.m_9236_().f_46443_) {
            return;
        }
        long difficulty = MobScaling.difficultyOf(creeper);
        DifficultyTier tier = DifficultyTier.of(difficulty);
        if (tier.ordinalPower() < DifficultyTier.AWAKENED.ordinalPower() && !EliteSystem.isElite(creeper)) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(creeper);
        if (tag.m_128471_("dmz_ad_final_boom")) {
            return;
        }
        tag.m_128379_("dmz_ad_final_boom", true);
        float power = creeperExplosionPower(tier, difficulty);
        if (creeper.m_9236_() instanceof ServerLevel level) {
            level.m_254849_(creeper, creeper.m_20185_(), creeper.m_20186_(), creeper.m_20189_(),
                    power, Level.ExplosionInteraction.MOB);
        }
    }

    private static float creeperExplosionPower(DifficultyTier tier, long difficulty) {
        float power = 3.5f + tier.ordinalPower() * 0.65f + (float) Math.min(6.0, difficulty / 200.0);
        return Math.min(12.0f, power);
    }

    private static void scaleCreeperBlast(Creeper creeper, DifficultyTier tier, long difficulty) {
        CompoundTag tag = PersistentDataAccess.get(creeper);
        if (tag.m_128471_("dmz_ad_blast_scaled")) {
            return;
        }
        tag.m_128379_("dmz_ad_blast_scaled", true);
        int bonus = Math.max(0, tier.ordinalPower() - DifficultyTier.AWAKENED.ordinalPower());
        bonus += (int) Math.min(6, difficulty / 250);
        try {
            Field radius = Creeper.class.getDeclaredField("bV");
            radius.setAccessible(true);
            int base = radius.getInt(creeper);
            radius.setInt(creeper, Math.min(16, base + bonus + 1));

            Field swell = Creeper.class.getDeclaredField("bU");
            swell.setAccessible(true);
            int maxSwell = swell.getInt(creeper);
            // Higher difficulty → faster fuse
            swell.setInt(creeper, Math.max(8, maxSwell - (bonus * 5) - 6));
        } catch (Throwable ignored) {
        }
    }

    // ── Zombies: Leap / Rush / Ground Slam / Short Dash / chained leaps ───

    private static void zombieTick(Mob mob, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(mob);
        long age = mob.f_19797_;
        double dist = mob.m_20270_(target);

        // Short dash
        if (dist > 2.0 && dist < 7.0 && age - tag.m_128454_("dmz_ad_dash") > 45
                && tier.ordinalPower() >= DifficultyTier.AWAKENED.ordinalPower()) {
            tag.m_128356_("dmz_ad_dash", age);
            pushToward(mob, target, 0.95, 0.12);
        }

        // Leap (chained at Advanced+)
        int leapCd = tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower() ? 35 : 60;
        if (dist > 3.0 && dist < 14.0 && age - tag.m_128454_("dmz_ad_leap") > leapCd
                && tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()) {
            tag.m_128356_("dmz_ad_leap", age);
            pushToward(mob, target, 1.25, 0.58);
            if (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()
                    && age - tag.m_128454_("dmz_ad_leap2") > 20) {
                tag.m_128356_("dmz_ad_leap2", age);
                // Second hop mid-air feel
                mob.m_5997_(0, 0.25, 0);
            }
        }

        // Rush
        if (dist < 18.0 && tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()) {
            mob.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 40, 1, false, false));
            mob.m_21573_().m_5624_(target, 1.35);
        }

        // Ground slam
        if (dist < 3.5 && tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower() && age % 70 == 0) {
            groundSlam(mob, target, 3.0, 1);
        }
    }

    // ── Skeletons: Ki only (blast / laser / beam / charge) ────────────────

    private static void skeletonTick(Mob mob, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            return;
        }
        float dist = mob.m_20270_(target);
        if (dist > 28.0f) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(mob);
        long age = mob.f_19797_;

        // Beam choice scales with difficulty
        if (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_charged") >= 120
                && dist < 26.0f) {
            if (KiAttackHelper.fireKiBeam(mob, target, tier, true)) {
                tag.m_128356_("dmz_ad_ki_charged", age);
                return;
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_beam") >= 90
                && dist < 24.0f) {
            if (KiAttackHelper.fireKiBeam(mob, target, tier, false)) {
                tag.m_128356_("dmz_ad_ki_beam", age);
                return;
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_laser") >= 60
                && dist < 22.0f) {
            if (KiAttackHelper.fireKiLaser(mob, target, tier)) {
                tag.m_128356_("dmz_ad_ki_laser", age);
                return;
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.AWAKENED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_blast") >= 35
                && dist < 18.0f) {
            if (KiAttackHelper.fireKiBlast(mob, target, tier)) {
                tag.m_128356_("dmz_ad_ki_blast", age);
            }
        }
    }

    // ── Endermen: Gravity (stacked) / Solar Flare / Teleport combos ───────

    private static void endermanTick(
            EnderMan ender, ServerLevel level, LivingEntity target, DifficultyTier tier, long difficulty) {
        if (!(target instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(ender);
        long age = ender.f_19797_;
        float dist = ender.m_20270_(player);

        // Stacked gravity chamber pressure — more aggro'd endermen = heavier
        if (tier.ordinalPower() >= DifficultyTier.AWAKENED.ordinalPower() && dist < 28.0f) {
            double g = gravityFor(ender, tier, difficulty, 1.0);
            CombatGravity.contribute(player, ender.m_20148_(), g, 45);
        }

        // Solar Flare
        if (tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_flare") >= 90
                && dist < 14.0f) {
            tag.m_128356_("dmz_ad_flare", age);
            solarFlare(level, ender, 10.0);
        }

        // Teleport combos — more aggressive at high tiers
        int tpCd = tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower() ? 35
                : tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower() ? 55 : 80;
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_tp") > tpCd) {
            tag.m_128356_("dmz_ad_tp", age);
            teleportNear(ender, player, 2.5);
            if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()) {
                // Combo: second blink after brief delay feel via immediate second hop
                teleportNear(ender, player, 1.5);
            }
        }

        if (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()) {
            ender.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 40, 1, false, false));
            ender.m_21573_().m_5624_(player, 1.45);
        }
    }

    // ── Wardens: mini raid boss kit + gravity ─────────────────────────────

    private static void wardenTick(
            Warden warden, ServerLevel level, LivingEntity target, DifficultyTier tier, long difficulty) {
        if (target == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(warden);
        long age = warden.f_19797_;
        float dist = warden.m_20270_(target);

        if (target instanceof ServerPlayer player
                && tier.ordinalPower() >= DifficultyTier.AWAKENED.ordinalPower()
                && dist < 32.0f) {
            double g = gravityFor(warden, tier, difficulty, 1.6);
            CombatGravity.contribute(player, warden.m_20148_(), g, 45);
        }

        // Leap
        if (dist > 4.0 && dist < 16.0 && age - tag.m_128454_("dmz_ad_leap") > 50
                && tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()) {
            tag.m_128356_("dmz_ad_leap", age);
            pushToward(warden, target, 1.35, 0.65);
        }

        // Rapid blasts / Ki barrage
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_barrage") >= 55
                && dist < 22.0f) {
            int shots = 3 + Math.min(5, tier.ordinalPower());
            if (KiAttackHelper.fireKiBarrage(warden, target, tier, shots) > 0) {
                tag.m_128356_("dmz_ad_ki_barrage", age);
            }
        }

        // Large blast
        if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_large") >= 85
                && dist < 24.0f) {
            if (KiAttackHelper.fireLargeBlast(warden, target, tier)) {
                tag.m_128356_("dmz_ad_large", age);
            }
        }

        // Explosive wave
        if (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()
                && age - tag.m_128454_("dmz_ad_wave") >= 140
                && dist < 12.0f) {
            if (KiAttackHelper.fireExplosiveWave(warden, target, tier)) {
                tag.m_128356_("dmz_ad_wave", age);
            }
        }

        // Beam
        if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_beam") >= 100
                && dist < 26.0f) {
            if (KiAttackHelper.fireKiBeam(warden, target, tier,
                    tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower())) {
                tag.m_128356_("dmz_ad_ki_beam", age);
            }
        }

        // Teleport
        if (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()
                && age - tag.m_128454_("dmz_ad_tp") >= 100
                && dist > 5.0f) {
            tag.m_128356_("dmz_ad_tp", age);
            teleportNear(warden, target, 2.0);
        }
    }

    // ── Blazes: Rapid barrage / Burning blast / Explosion blast ───────────

    private static void blazeTick(Blaze blaze, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(blaze);
        long age = blaze.f_19797_;
        float dist = blaze.m_20270_(target);
        if (dist > 26.0f) {
            return;
        }

        if (tier.ordinalPower() >= DifficultyTier.AWAKENED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_barrage") >= 40
                && dist < 18.0f) {
            int shots = 2 + Math.min(4, tier.ordinalPower());
            if (KiAttackHelper.fireKiBarrage(blaze, target, tier, shots, true) > 0) {
                tag.m_128356_("dmz_ad_ki_barrage", age);
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_blast") >= 55
                && dist < 20.0f) {
            if (KiAttackHelper.fireKiBlast(blaze, target, tier, true)) {
                tag.m_128356_("dmz_ad_ki_blast", age);
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_explode") >= 100
                && dist < 16.0f) {
            if (KiAttackHelper.fireExplosionBlast(blaze, target, tier)) {
                tag.m_128356_("dmz_ad_explode", age);
            }
        }
        target.m_20254_(2);
    }

    // ── Ghasts: Large Blast / Burn Beam / Explosion Wave ──────────────────

    private static void ghastTick(Ghast ghast, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(ghast);
        long age = ghast.f_19797_;
        float dist = ghast.m_20270_(target);
        if (dist > 48.0f) {
            return;
        }

        if (tier.ordinalPower() >= DifficultyTier.AWAKENED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_large") >= 50
                && dist < 40.0f) {
            if (KiAttackHelper.fireLargeBlast(ghast, target, tier)) {
                tag.m_128356_("dmz_ad_large", age);
                target.m_20254_(3);
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_beam") >= 80
                && dist < 42.0f) {
            if (KiAttackHelper.fireKiBeam(ghast, target, tier, true, true)) {
                tag.m_128356_("dmz_ad_ki_beam", age);
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_wave") >= 120
                && dist < 28.0f) {
            if (KiAttackHelper.fireExplosiveWave(ghast, target, tier)) {
                tag.m_128356_("dmz_ad_wave", age);
            }
        }
    }

    // ── Piglins: Leap / Small Blast / Rush Combo ──────────────────────────

    private static void piglinTick(AbstractPiglin piglin, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(piglin);
        long age = piglin.f_19797_;
        double dist = piglin.m_20270_(target);

        if (dist > 3.0 && dist < 12.0 && age - tag.m_128454_("dmz_ad_leap") > 55
                && tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()) {
            tag.m_128356_("dmz_ad_leap", age);
            pushToward(piglin, target, 1.2, 0.55);
        }
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_blast") >= 50
                && dist < 16.0) {
            if (KiAttackHelper.fireKiBlast(piglin, target, tier)) {
                tag.m_128356_("dmz_ad_ki_blast", age);
            }
        }
        if (dist < 16.0 && tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()) {
            piglin.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 40, 1, false, false));
            piglin.m_21573_().m_5624_(target, 1.4);
        }
        if (dist < 3.0 && tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower() && age % 60 == 0) {
            // Rush combo — slam + brief strength
            groundSlam(piglin, target, 2.5, 0);
            piglin.m_7292_(new MobEffectInstance(MobEffects.f_19600_, 40, 1, false, true));
        }
    }

    // ── Zombie Pigmen: Leap / Ki Barrage / Counter TP / swarm ─────────────

    private static void zombiePiglinTick(ZombifiedPiglin zp, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(zp);
        long age = zp.f_19797_;
        double dist = zp.m_20270_(target);

        if (dist > 3.0 && dist < 13.0 && age - tag.m_128454_("dmz_ad_leap") > 50
                && tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()) {
            tag.m_128356_("dmz_ad_leap", age);
            pushToward(zp, target, 1.3, 0.6);
        }
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_barrage") >= 65
                && dist < 18.0) {
            int shots = 2 + Math.min(4, tier.ordinalPower() / 2);
            if (KiAttackHelper.fireKiBarrage(zp, target, tier, shots) > 0) {
                tag.m_128356_("dmz_ad_ki_barrage", age);
            }
        }
        // Aggressive swarm — pull nearby zombified piglins onto the same target
        if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower() && age % 40 == 0
                && zp.m_9236_() instanceof ServerLevel level) {
            AABB box = zp.m_20191_().m_82400_(16.0);
            for (ZombifiedPiglin ally : level.m_45976_(ZombifiedPiglin.class, box)) {
                if (ally != zp && ally.m_6084_()) {
                    ally.m_6710_(target);
                    ally.m_21573_().m_5624_(target, 1.3);
                }
            }
            zp.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 50, 1, false, false));
        }
    }

    // ── Hoglins: aerial — launch grounded / smash airborne ────────────────

    private static void hoglinTick(Hoglin hoglin, LivingEntity target, DifficultyTier tier) {
        if (target == null || tier.ordinalPower() < DifficultyTier.ENHANCED.ordinalPower()) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(hoglin);
        long age = hoglin.f_19797_;
        double dist = hoglin.m_20270_(target);
        if (dist > 14.0 || age - tag.m_128454_("dmz_ad_aerial") < 55) {
            return;
        }
        tag.m_128356_("dmz_ad_aerial", age);

        boolean grounded = target.m_20096_(); // onGround
        if (grounded) {
            // Launch upward
            target.m_5997_(0, 1.05 + Math.min(0.6, tier.ordinalPower() * 0.08), 0);
            pushToward(hoglin, target, 0.8, 0.45);
        } else {
            // Smash downward while airborne
            Vec3 v = target.m_20184_();
            target.m_20334_(v.f_82479_ * 0.4, -1.35 - tier.ordinalPower() * 0.08, v.f_82481_ * 0.4);
            hoglin.m_6021_(target.m_20185_(), target.m_20186_(), target.m_20189_());
            target.m_6469_(hoglin.m_269291_().m_269333_(hoglin), 4.0f + tier.ordinalPower());
        }
    }

    // ── Shared helpers ────────────────────────────────────────────────────

    private static double gravityFor(Mob mob, DifficultyTier tier, long difficulty, double mult) {
        double base = 6.0 + tier.ordinalPower() * 5.0;
        base += Math.min(30.0, difficulty / 80.0);
        if (EliteSystem.isElite(mob)) {
            base *= 1.35;
        }
        if (MutationSystem.get(mob) == MutationType.GRAVITY_ENDERMAN) {
            base *= 1.5;
        }
        return Math.max(4.0, base * mult);
    }

    private static void solarFlare(ServerLevel level, Mob source, double radius) {
        AABB box = source.m_20191_().m_82400_(radius);
        List<ServerPlayer> players = level.m_45976_(ServerPlayer.class, box);
        for (ServerPlayer p : players) {
            p.m_7292_(new MobEffectInstance(MobEffects.f_19610_, 70, 0, false, true)); // BLINDNESS
            p.m_7292_(new MobEffectInstance(MobEffects.f_19604_, 50, 0, false, true)); // NAUSEA
            p.m_7292_(new MobEffectInstance(MobEffects.f_19619_, 40, 0, false, true)); // GLOWING flash
        }
        source.m_7292_(new MobEffectInstance(MobEffects.f_19619_, 40, 0, false, true));
    }

    private static void groundSlam(Mob mob, LivingEntity target, double radius, int slowAmp) {
        target.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 60, slowAmp, false, true));
        target.m_5997_(0, 0.35, 0);
        if (!(mob.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        AABB box = mob.m_20191_().m_82400_(radius);
        for (ServerPlayer p : level.m_45976_(ServerPlayer.class, box)) {
            if (p != target) {
                p.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 40, 0, false, true));
            }
        }
    }

    private static void pushToward(Mob mob, LivingEntity target, double horizontal, double upward) {
        double dx = target.m_20185_() - mob.m_20185_();
        double dz = target.m_20189_() - mob.m_20189_();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len > 0.001) {
            mob.m_5997_((dx / len) * horizontal, upward, (dz / len) * horizontal);
        }
    }

    private static void teleportNear(Mob mob, LivingEntity target, double spread) {
        double ox = (ThreadLocalRandom.current().nextDouble() - 0.5) * spread * 2.0;
        double oz = (ThreadLocalRandom.current().nextDouble() - 0.5) * spread * 2.0;
        mob.m_6021_(target.m_20185_() + ox, target.m_20186_(), target.m_20189_() + oz);
    }

    /** Counter-teleport / counter-movement when damaged. */
    public static void onHurt(LivingEntity entity) {
        if (entity == null || entity.m_9236_().f_46443_) {
            return;
        }
        long difficulty = MobScaling.difficultyOf(entity);
        DifficultyTier tier = DifficultyTier.of(difficulty);
        if (EliteSystem.isElite(entity) && tier.ordinalPower() < DifficultyTier.ELITE.ordinalPower()) {
            tier = DifficultyTier.ELITE;
        }
        LivingEntity target = entity instanceof Mob mob ? mob.m_5448_() : null;

        if (entity instanceof EnderMan ender
                && tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()
                && ThreadLocalRandom.current().nextDouble() < 0.4
                && target != null) {
            teleportNear(ender, target, 2.0);
            return;
        }
        if (entity instanceof ZombifiedPiglin zp
                && tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()
                && ThreadLocalRandom.current().nextDouble() < 0.3
                && target != null) {
            teleportNear(zp, target, 2.5);
            return;
        }
        if (entity instanceof Warden warden
                && tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && ThreadLocalRandom.current().nextDouble() < 0.35
                && target != null) {
            // Counter movement — blink or leap at attacker
            if (warden.m_20270_(target) > 6.0f) {
                teleportNear(warden, target, 1.5);
            } else {
                pushToward(warden, target, -1.1, 0.35); // hop back then re-engage next tick
            }
        }
    }

    /** Replace skeleton arrows / ghast fireballs / blaze shots with ki. */
    public static boolean tryReplaceProjectile(Mob shooter, LivingEntity target) {
        if (shooter == null || target == null || !shooter.m_6084_()) {
            return false;
        }
        long difficulty = MobScaling.difficultyOf(shooter);
        DifficultyTier tier = DifficultyTier.of(difficulty);
        if (EliteSystem.isElite(shooter) && tier.ordinalPower() < DifficultyTier.ELITE.ordinalPower()) {
            tier = DifficultyTier.ELITE;
        }
        if (tier.ordinalPower() < DifficultyTier.AWAKENED.ordinalPower()) {
            return false;
        }
        if (shooter instanceof AbstractSkeleton) {
            return KiAttackHelper.fireKiBlast(shooter, target, tier)
                    || KiAttackHelper.fireKiLaser(shooter, target, tier);
        }
        if (shooter instanceof Blaze) {
            return KiAttackHelper.fireKiBlast(shooter, target, tier, true);
        }
        if (shooter instanceof Ghast) {
            return KiAttackHelper.fireLargeBlast(shooter, target, tier);
        }
        return false;
    }
}
