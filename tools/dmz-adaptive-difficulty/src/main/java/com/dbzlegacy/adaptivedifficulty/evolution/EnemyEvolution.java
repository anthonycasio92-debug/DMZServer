package com.dbzlegacy.adaptivedifficulty.evolution;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.HostileMobs;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tick.CombatIndex;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.util.DimensionGates;
import com.dbzlegacy.adaptivedifficulty.util.EntityDisplayNames;
import com.dbzlegacy.adaptivedifficulty.util.NearbyPlayers;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import java.lang.reflect.Field;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
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
import net.minecraft.world.phys.Vec3;

/**
 * Per-mob evolution kits unlocked by difficulty tiers.
 * Endermen / Wardens apply stacked DMZ gravity-chamber pressure to aggro'd players.
 */
public final class EnemyEvolution {
    public static final String TAG_EVOLVED = "dmz_ad_evolved";

    private static final Field CREEPER_RADIUS;
    private static final Field CREEPER_SWELL;

    static {
        Field radius = null;
        Field swell = null;
        try {
            radius = Creeper.class.getDeclaredField("f_32272_"); // explosionRadius
            radius.setAccessible(true);
            swell = Creeper.class.getDeclaredField("f_32271_"); // maxSwell
            swell.setAccessible(true);
        } catch (Throwable ignored) {
        }
        CREEPER_RADIUS = radius;
        CREEPER_SWELL = swell;
    }

    private EnemyEvolution() {}

    public static boolean isEvolvable(Mob mob) {
        return HostileMobs.isHostile(mob);
    }

    public static void tick(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        tick(mob, MobScaling.difficultyOf(entity), EliteSystem.isElite(entity));
    }

    public static void tick(Mob mob, long difficulty, boolean elite) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableEnemyEvolution || mob == null || !mob.m_6084_() || DimensionGates.isDisabled(mob)) {
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

        // Kits are player-focused — drop other-hostile targets so packs don't civil-war.
        LivingEntity target = mob.m_5448_();
        if (target != null && !(target instanceof Player) && HostileMobs.isHostile(target)) {
            mob.m_6710_(null);
            target = null;
        }

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

    // ── Creepers: ignite + scaled blast radius / faster fuse ───────────────

    private static void creeperTick(
            Creeper creeper, ServerLevel level, LivingEntity target, DifficultyTier tier, long difficulty) {
        scaleCreeperBlast(creeper, tier, difficulty);
        // Only arm against players — never waste fuse on other mobs.
        if (!(target instanceof Player) || tier.ordinalPower() < DifficultyTier.AWAKENED.ordinalPower()) {
            return;
        }
        float dist = creeper.m_20270_(target);
        double speed = tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower() ? 1.45 : 1.25;
        // Chase earlier than Elite so Awakened+ creepers actually close the gap.
        if (dist < 18.0f) {
            creeper.m_21573_().m_26519_(target.m_20185_(), target.m_20186_(), target.m_20189_(), speed);
        }
        // Fuse when close. m_32312_ = ignite(); m_32283_(1) = setSwellDir(1).
        if (dist < 6.0f) {
            if (!creeper.m_32311_()) { // isIgnited
                creeper.m_32312_(); // ignite
            }
            creeper.m_32283_(1); // setSwellDir toward explosion
        }
        // Concept §11 Tracking Explosion — keep pathing to the player while swelling.
        if (creeper.m_32311_() && dist < 22.0f && target.m_6084_()) {
            double trackSpeed = speed + (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower() ? 0.35 : 0.15);
            creeper.m_21573_().m_26519_(target.m_20185_(), target.m_20186_(), target.m_20189_(), trackSpeed);
            creeper.m_6710_(target);
        }
    }

    /** Called on creeper death — final explosion when killed before fuse detonation. */
    public static void onCreeperDeath(Creeper creeper) {
        onCreeperDeath(creeper, null);
    }

    /**
     * Final boom only when the creeper did <b>not</b> already explode (fuse / other blast).
     * Fuse detonation kills via explosion damage — stacking another blast was near-instant death.
     */
    public static void onCreeperDeath(Creeper creeper, DamageSource source) {
        if (creeper == null || creeper.m_9236_().f_46443_) {
            return;
        }
        // Skip if death was already from an explosion (vanilla fuse boom, TNT, etc.).
        if (source != null && source.m_269533_(DamageTypeTags.f_268415_)) { // IS_EXPLOSION
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
        bonus += (int) Math.min(8, difficulty / 500);
        if (CREEPER_RADIUS == null || CREEPER_SWELL == null) {
            return;
        }
        try {
            int base = Math.max(3, CREEPER_RADIUS.getInt(creeper));
            CREEPER_RADIUS.setInt(creeper, Math.min(12, base + Math.min(6, bonus / 2) + 1));
            int maxSwell = CREEPER_SWELL.getInt(creeper);
            // Higher difficulty → faster fuse (vanilla default 30).
            CREEPER_SWELL.setInt(creeper, Math.max(10, maxSwell - Math.min(18, bonus + 4)));
        } catch (Throwable ignored) {
        }
    }

    // ── Zombies: Leap / Rush / Ground Slam / Short Dash / chained leaps ───

    private static void zombieTick(Mob mob, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            if (mob.m_9236_() instanceof ServerLevel level) {
                target = nearestPlayer(level, mob, 18.0);
                if (target != null) {
                    mob.m_6710_(target);
                }
            }
        }
        if (target == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(mob);
        long age = mob.f_19797_;
        double dist = mob.m_20270_(target);
        int power = tier.ordinalPower();

        // Short dash — faster at high tiers
        long dashCd = power >= DifficultyTier.MYTHIC.ordinalPower() ? 20
                : power >= DifficultyTier.GOD.ordinalPower() ? 30 : 45;
        if (dist > 2.0 && dist < 7.0 && age - tag.m_128454_("dmz_ad_dash") > dashCd
                && power >= DifficultyTier.AWAKENED.ordinalPower()) {
            tag.m_128356_("dmz_ad_dash", age);
            double force = power >= DifficultyTier.OMEGA.ordinalPower() ? 1.35 : 0.95;
            pushToward(mob, target, force, 0.12);
        }

        // Leap — Enhanced+; chained Master+; rapid Transcendent+
        int leapCd = power >= DifficultyTier.TRANSCENDENT.ordinalPower() ? 22
                : power >= DifficultyTier.ADVANCED.ordinalPower() ? 35 : 60;
        if (dist > 3.0 && dist < 14.0 && age - tag.m_128454_("dmz_ad_leap") > leapCd
                && power >= DifficultyTier.ENHANCED.ordinalPower()) {
            tag.m_128356_("dmz_ad_leap", age);
            pushToward(mob, target, 1.25 + Math.min(0.6, power * 0.04), 0.58);
            if (power >= DifficultyTier.MASTER.ordinalPower()
                    && age - tag.m_128454_("dmz_ad_leap2") > 20) {
                tag.m_128356_("dmz_ad_leap2", age);
                mob.m_5997_(0, 0.25, 0);
            }
        }

        // Rush — Elite+
        if (dist < 18.0 && power >= DifficultyTier.ELITE.ordinalPower()) {
            int amp = power >= DifficultyTier.MYTHIC.ordinalPower() ? 2 : 1;
            mob.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 40, amp, false, false));
            mob.m_21573_().m_5624_(target, power >= DifficultyTier.GOD.ordinalPower() ? 1.55 : 1.35);
        }

        // Ground slam — Advanced+; more frequent Impossible+
        int slamEvery = power >= DifficultyTier.IMPOSSIBLE.ordinalPower() ? 35
                : power >= DifficultyTier.DIVINE.ordinalPower() ? 50 : 70;
        if (dist < 3.5 && power >= DifficultyTier.ADVANCED.ordinalPower() && age % slamEvery == 0) {
            int amp = power >= DifficultyTier.OMEGA.ordinalPower() ? 2 : 1;
            groundSlam(mob, target, 3.0 + Math.min(2.0, power * 0.1), amp);
            if (power >= DifficultyTier.ABSOLUTE.ordinalPower() && target instanceof ServerPlayer sp) {
                sp.m_7292_(new MobEffectInstance(MobEffects.f_19615_, 50, 0, false, true)); // WITHER
            }
        }

        // God+: berserk strength while chasing
        if (power >= DifficultyTier.GOD.ordinalPower() && dist < 16.0) {
            mob.m_7292_(new MobEffectInstance(MobEffects.f_19600_, 40,
                    power >= DifficultyTier.APEX.ordinalPower() ? 2 : 1, false, false));
        }
    }

    // ── Skeletons: Ki only (blast / laser / beam / charge) ────────────────

    private static void skeletonTick(Mob mob, LivingEntity target, DifficultyTier tier) {
        if (target == null) {
            if (mob.m_9236_() instanceof ServerLevel level) {
                target = nearestPlayer(level, mob, 28.0);
                if (target != null) {
                    mob.m_6710_(target);
                }
            }
        }
        if (target == null) {
            return;
        }
        float dist = mob.m_20270_(target);
        if (dist > 32.0f) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(mob);
        long age = mob.f_19797_;
        int power = tier.ordinalPower();

        // Cooldowns shrink across the high ladder so Zenith actually fires more than Master.
        long chargedCd = power >= DifficultyTier.ZENITH.ordinalPower() ? 55
                : power >= DifficultyTier.OMEGA.ordinalPower() ? 70
                : power >= DifficultyTier.IMPOSSIBLE.ordinalPower() ? 90 : 120;
        long beamCd = power >= DifficultyTier.MYTHIC.ordinalPower() ? 45
                : power >= DifficultyTier.DIVINE.ordinalPower() ? 65 : 90;
        long laserCd = power >= DifficultyTier.GOD.ordinalPower() ? 30
                : power >= DifficultyTier.LEGENDARY.ordinalPower() ? 45 : 60;
        long blastCd = power >= DifficultyTier.TRANSCENDENT.ordinalPower() ? 14
                : power >= DifficultyTier.MASTER.ordinalPower() ? 22 : 35;

        if (power >= DifficultyTier.MASTER.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_charged") >= chargedCd
                && dist < 26.0f) {
            if (KiAttackHelper.fireKiBeam(mob, target, tier, true)) {
                tag.m_128356_("dmz_ad_ki_charged", age);
                return;
            }
        }
        if (power >= DifficultyTier.ADVANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_beam") >= beamCd
                && dist < 24.0f) {
            if (KiAttackHelper.fireKiBeam(mob, target, tier, false)) {
                tag.m_128356_("dmz_ad_ki_beam", age);
                return;
            }
        }
        if (power >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_laser") >= laserCd
                && dist < 22.0f) {
            if (KiAttackHelper.fireKiLaser(mob, target, tier)) {
                tag.m_128356_("dmz_ad_ki_laser", age);
                return;
            }
        }
        if (power >= DifficultyTier.AWAKENED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_blast") >= blastCd
                && dist < 18.0f) {
            boolean burning = power >= DifficultyTier.GOD.ordinalPower();
            if (KiAttackHelper.fireKiBlast(mob, target, tier, burning)) {
                tag.m_128356_("dmz_ad_ki_blast", age);
            }
        }

        // Impossible+: barrage volleys
        if (power >= DifficultyTier.IMPOSSIBLE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_barrage") >= 80
                && dist < 20.0f) {
            int shots = 3 + Math.min(6, power - DifficultyTier.IMPOSSIBLE.ordinalPower());
            if (KiAttackHelper.fireKiBarrage(mob, target, tier, shots, power >= DifficultyTier.OMEGA.ordinalPower()) > 0) {
                tag.m_128356_("dmz_ad_ki_barrage", age);
            }
        }
    }

    // ── Endermen: Gravity (stacked) / Solar Flare / Teleport combos ───────

    private static void endermanTick(
            EnderMan ender, ServerLevel level, LivingEntity target, DifficultyTier tier, long difficulty) {
        // Gravity is applied every player tick via CombatGravity.scanNearbySources.
        // Still acquire a player target so flare / TP / chase kits keep working after blinks.
        ServerPlayer player = target instanceof ServerPlayer sp
                ? sp
                : nearestPlayer(level, ender, 28.0);
        if (player == null) {
            return;
        }
        if (target != player) {
            ender.m_6710_(player);
        }
        CompoundTag tag = PersistentDataAccess.get(ender);
        long age = ender.f_19797_;
        float dist = ender.m_20270_(player);

        // Backup contribute (player-tick scan is primary).
        if (tier.ordinalPower() >= DifficultyTier.AWAKENED.ordinalPower() && dist < 28.0f) {
            double g = CombatGravity.gravityFor(ender, tier, difficulty, 1.0);
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
        ServerPlayer focused = target instanceof ServerPlayer sp
                ? sp
                : nearestPlayer(level, warden, 32.0);
        if (focused != null && target != focused) {
            warden.m_6710_(focused);
            target = focused;
        }
        if (target == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(warden);
        long age = warden.f_19797_;
        float dist = warden.m_20270_(target);

        // Backup contribute (player-tick scan is primary).
        if (focused != null
                && tier.ordinalPower() >= DifficultyTier.AWAKENED.ordinalPower()
                && dist < 32.0f) {
            double g = CombatGravity.gravityFor(warden, tier, difficulty, 1.6);
            CombatGravity.contribute(focused, warden.m_20148_(), g, 45);
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
        if (target == null && blaze.m_9236_() instanceof ServerLevel level) {
            target = nearestPlayer(level, blaze, 26.0);
            if (target != null) {
                blaze.m_6710_(target);
            }
        }
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
        // Intentionally no per-tick ignite — burning is applied by successful ki hits only.
    }

    // ── Ghasts: Large Blast / Burn Beam / Explosion Wave ──────────────────

    private static void ghastTick(Ghast ghast, LivingEntity target, DifficultyTier tier) {
        if (target == null && ghast.m_9236_() instanceof ServerLevel level) {
            target = nearestPlayer(level, ghast, 48.0);
            if (target != null) {
                ghast.m_6710_(target);
            }
        }
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
        if (target == null && piglin.m_9236_() instanceof ServerLevel level) {
            target = nearestPlayer(level, piglin, 16.0);
            if (target != null) {
                piglin.m_6710_(target);
            }
        }
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
        if (target == null && zp.m_9236_() instanceof ServerLevel level) {
            target = nearestPlayer(level, zp, 18.0);
            if (target != null) {
                zp.m_6710_(target);
            }
        }
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
        // Aggressive swarm — combat-index peers only (no world AABB)
        if (target instanceof Player packTarget
                && tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower() && age % 40 == 0) {
            CombatIndex.forEachNear(zp, 16.0, 12, ally -> {
                if (!(ally instanceof ZombifiedPiglin) || !ally.m_6084_()) {
                    return;
                }
                LivingEntity allyTarget = ally.m_5448_();
                if (allyTarget != null && !(allyTarget instanceof Player) && HostileMobs.isHostile(allyTarget)) {
                    ally.m_6710_(null);
                }
                ally.m_6710_(packTarget);
                ally.m_21573_().m_5624_(packTarget, 1.3);
                CombatIndex.mark(ally);
            });
            zp.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 50, 1, false, false));
        }
    }

    // ── Hoglins: aerial — launch grounded / smash airborne ────────────────

    private static void hoglinTick(Hoglin hoglin, LivingEntity target, DifficultyTier tier) {
        if (target == null && hoglin.m_9236_() instanceof ServerLevel level) {
            target = nearestPlayer(level, hoglin, 14.0);
            if (target != null) {
                hoglin.m_6710_(target);
            }
        }
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

    private static ServerPlayer nearestPlayer(ServerLevel level, Mob mob, double radius) {
        // Player-list distance — avoids AABB entity queries on busy servers.
        return NearbyPlayers.nearest(mob, radius);
    }

    private static void solarFlare(ServerLevel level, Mob source, double radius) {
        NearbyPlayers.forEachWithin(source, radius, p -> {
            p.m_7292_(new MobEffectInstance(MobEffects.f_19610_, 70, 0, false, true)); // BLINDNESS
            p.m_7292_(new MobEffectInstance(MobEffects.f_19604_, 50, 0, false, true)); // NAUSEA
            p.m_7292_(new MobEffectInstance(MobEffects.f_19619_, 40, 0, false, true)); // GLOWING flash
        });
        source.m_7292_(new MobEffectInstance(MobEffects.f_19619_, 40, 0, false, true));
    }

    private static void groundSlam(Mob mob, LivingEntity target, double radius, int slowAmp) {
        target.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 60, slowAmp, false, true));
        target.m_5997_(0, 0.35, 0);
        NearbyPlayers.forEachWithin(mob, radius, p -> {
            if (p != target) {
                p.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 40, 0, false, true));
            }
        });
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
        // Only kits that counter on hurt — bail before any NBT for other entities.
        if (!(entity instanceof EnderMan || entity instanceof ZombifiedPiglin || entity instanceof Warden)) {
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
