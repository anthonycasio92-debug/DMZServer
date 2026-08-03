package com.dbzlegacy.adaptivedifficulty.evolution;

import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.HostileMobs;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tick.CombatIndex;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockAbilityCaps;
import com.dbzlegacy.adaptivedifficulty.util.DimensionGates;
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
        DifficultyTier tier = resolveAbilityTier(mob, difficulty, elite);
        if (tier.ordinalPower() < DifficultyTier.AWAKENED.ordinalPower()) {
            return;
        }

        markEvolved(mob, tier);

        // Kits are player-focused — drop other-hostile targets/revenge so packs don't civil-war.
        HostileMobs.clearCivilWarAggro(mob);
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

    /**
     * Records which ability kit this mob was claimed with.
     * Intentionally does NOT set a custom nameplate or glow — those are reserved for
     * true rarity rolls (Elite / Mutation / Boss). Older builds named every T3+ kit mob
     * "Elite …", which made almost every scaled hostile look like a rarity variant.
     */
    private static void markEvolved(Mob mob, DifficultyTier tier) {
        if (tier.ordinalPower() < DifficultyTier.AWAKENED.ordinalPower()) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(mob);
        String want = tier.display;
        String prev = tag.m_128461_("dmz_ad_ability_tier");
        if (!tag.m_128471_(TAG_EVOLVED) || !want.equals(prev)) {
            tag.m_128379_(TAG_EVOLVED, true);
            tag.m_128359_("dmz_ad_ability_tier", want);
        }
        // Clear leftover kit cosmetics from older jars; rarity nameplates stay.
        stripKitCosmeticIfPresent(mob);
    }

    /** True when this mob rolled a real rarity tag (elite / mutation / boss). */
    public static boolean hasRarityVariant(Mob mob) {
        if (mob == null) {
            return false;
        }
        CompoundTag tag = PersistentDataAccess.get(mob);
        if (tag.m_128471_(EliteSystem.TAG_ELITE) || tag.m_128471_(BossScaling.TAG_BOSS)) {
            return true;
        }
        return tag.m_128441_(MutationSystem.TAG_MUTATION)
                && !tag.m_128461_(MutationSystem.TAG_MUTATION).isEmpty();
    }

    /**
     * Strip leftover kit-only cosmetics from older builds (custom name + glow with no rarity tag).
     * Leaves true elite / mutation / boss nameplates alone.
     */
    public static void stripKitCosmeticIfPresent(Mob mob) {
        if (mob == null || hasRarityVariant(mob)) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(mob);
        if (!tag.m_128471_(TAG_EVOLVED)) {
            return;
        }
        try {
            if (mob.m_8077_() && looksLikeKitNameplate(mob)) {
                mob.m_6593_(null);
            }
            mob.m_20340_(false);
        } catch (Throwable ignored) {
            // cosmetic cleanup only
        }
    }

    /** Matches the old kit format {@code §6<Tier> §f<Type>} (and stripped variants). */
    private static boolean looksLikeKitNameplate(Mob mob) {
        Component name = mob.m_7770_();
        if (name == null) {
            return false;
        }
        String s = name.getString();
        if (s == null || s.isEmpty()) {
            return false;
        }
        // Belt-and-suspenders: never strip known rarity nameplate shapes.
        if (s.contains("✦") || s.contains("☠") || s.startsWith("§d")) {
            return false;
        }
        for (DifficultyTier t : DifficultyTier.values()) {
            if (t == DifficultyTier.NONE) {
                continue;
            }
            String d = t.display;
            if (s.startsWith(d + " ") || s.startsWith("§6" + d + " ")) {
                return true;
            }
        }
        return false;
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
        DifficultyTier tier = resolveAbilityTier(creeper, difficulty, EliteSystem.isElite(creeper));
        if (tier.ordinalPower() < DifficultyTier.AWAKENED.ordinalPower()) {
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

        // Short dash — faster at high tiers / vs countered class+top stats
        long dashCd = kitCd(mob, power >= DifficultyTier.MYTHIC.ordinalPower() ? 20
                : power >= DifficultyTier.GOD.ordinalPower() ? 30 : 45);
        if (dist > 2.0 && dist < 7.0 && age - tag.m_128454_("dmz_ad_dash") > dashCd
                && power >= DifficultyTier.AWAKENED.ordinalPower()) {
            tag.m_128356_("dmz_ad_dash", age);
            double force = power >= DifficultyTier.OMEGA.ordinalPower() ? 1.35 : 0.95;
            pushToward(mob, target, force, 0.12);
        }

        // Leap — Enhanced+; chained Master+; rapid Transcendent+
        long leapCd = kitCd(mob, power >= DifficultyTier.TRANSCENDENT.ordinalPower() ? 22
                : power >= DifficultyTier.ADVANCED.ordinalPower() ? 35 : 60);
        if (dist > 3.0 && dist < 14.0 && age - tag.m_128454_("dmz_ad_leap") > leapCd
                && power >= DifficultyTier.ENHANCED.ordinalPower()) {
            tag.m_128356_("dmz_ad_leap", age);
            pushToward(mob, target, 1.25 + Math.min(0.6, power * 0.04), 0.58);
            if (power >= DifficultyTier.MASTER.ordinalPower()
                    && age - tag.m_128454_("dmz_ad_leap2") > kitCd(mob, 20)) {
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

        // Ground slam — Advanced+; more frequent Impossible+ / vs tanks
        long slamEvery = kitCd(mob, power >= DifficultyTier.IMPOSSIBLE.ordinalPower() ? 35
                : power >= DifficultyTier.DIVINE.ordinalPower() ? 50 : 70);
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

        // Global gap between ANY skeleton ki shot (tick kits + arrow-replace share this).
        // High tiers stay threatening without stunlocking players in place.
        if (!skeletonKiReady(mob, tag, age, tier)) {
            return;
        }

        // Per-ability cooldowns — tightened vs ki / high-PWR counter targets.
        long chargedCd = kitCd(mob, power >= DifficultyTier.ZENITH.ordinalPower() ? 100
                : power >= DifficultyTier.OMEGA.ordinalPower() ? 120
                : power >= DifficultyTier.IMPOSSIBLE.ordinalPower() ? 140 : 160);
        long beamCd = kitCd(mob, power >= DifficultyTier.MYTHIC.ordinalPower() ? 80
                : power >= DifficultyTier.DIVINE.ordinalPower() ? 95 : 115);
        long laserCd = kitCd(mob, power >= DifficultyTier.GOD.ordinalPower() ? 55
                : power >= DifficultyTier.LEGENDARY.ordinalPower() ? 70 : 90);
        long blastCd = kitCd(mob, power >= DifficultyTier.TRANSCENDENT.ordinalPower() ? 40
                : power >= DifficultyTier.MASTER.ordinalPower() ? 50 : 65);
        long barrageCd = kitCd(mob, power >= DifficultyTier.ZENITH.ordinalPower() ? 140
                : power >= DifficultyTier.OMEGA.ordinalPower() ? 160 : 180);

        if (power >= DifficultyTier.MASTER.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_charged") >= chargedCd
                && dist < 26.0f) {
            if (KiAttackHelper.fireKiBeam(mob, target, tier, true)) {
                markSkeletonKiShot(tag, age, "dmz_ad_ki_charged");
                return;
            }
        }
        if (power >= DifficultyTier.ADVANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_beam") >= beamCd
                && dist < 24.0f) {
            if (KiAttackHelper.fireKiBeam(mob, target, tier, false)) {
                markSkeletonKiShot(tag, age, "dmz_ad_ki_beam");
                return;
            }
        }
        if (power >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_laser") >= laserCd
                && dist < 22.0f) {
            if (KiAttackHelper.fireKiLaser(mob, target, tier)) {
                markSkeletonKiShot(tag, age, "dmz_ad_ki_laser");
                return;
            }
        }
        if (power >= DifficultyTier.AWAKENED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_blast") >= blastCd
                && dist < 18.0f) {
            boolean burning = power >= DifficultyTier.GOD.ordinalPower();
            if (KiAttackHelper.fireKiBlast(mob, target, tier, burning)) {
                markSkeletonKiShot(tag, age, "dmz_ad_ki_blast");
                return;
            }
        }

        // Impossible+: short barrage volleys (fewer shots, longer gap).
        if (power >= DifficultyTier.IMPOSSIBLE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_barrage") >= barrageCd
                && dist < 20.0f) {
            int shots = 2 + Math.min(3, (power - DifficultyTier.IMPOSSIBLE.ordinalPower()) / 2);
            if (KiAttackHelper.fireKiBarrage(mob, target, tier, shots, power >= DifficultyTier.OMEGA.ordinalPower()) > 0) {
                markSkeletonKiShot(tag, age, "dmz_ad_ki_barrage");
            }
        }
    }

    /** Minimum ticks between any skeleton ki projectile (shared by kits + arrow replace). */
    private static long skeletonShotGap(LivingEntity entity, DifficultyTier tier) {
        int power = tier == null ? 0 : tier.ordinalPower();
        long base;
        if (power >= DifficultyTier.ZENITH.ordinalPower()) {
            base = 28L;
        } else if (power >= DifficultyTier.MYTHIC.ordinalPower()) {
            base = 32L;
        } else if (power >= DifficultyTier.GOD.ordinalPower()) {
            base = 36L;
        } else if (power >= DifficultyTier.MASTER.ordinalPower()) {
            base = 40L;
        } else {
            base = 45L;
        }
        return kitCd(entity, base);
    }

    private static boolean skeletonKiReady(
            LivingEntity entity, CompoundTag tag, long age, DifficultyTier tier
    ) {
        if (tag == null) {
            return false;
        }
        return age - tag.m_128454_("dmz_ad_ki_shot") >= skeletonShotGap(entity, tier);
    }

    /** Apply stamped class/race/top-stat counter cadence (&lt;1 = more aggressive kits). */
    private static long kitCd(LivingEntity entity, long baseTicks) {
        double scale = MobScaling.counterKitCooldownScale(entity);
        return Math.max(8L, Math.round(Math.max(1L, baseTicks) * scale));
    }

    private static void markSkeletonKiShot(CompoundTag tag, long age, String abilityKey) {
        if (tag == null) {
            return;
        }
        tag.m_128356_("dmz_ad_ki_shot", age);
        if (abilityKey != null && !abilityKey.isBlank()) {
            tag.m_128356_(abilityKey, age);
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
                && age - tag.m_128454_("dmz_ad_flare") >= kitCd(ender, 90)
                && dist < 14.0f) {
            tag.m_128356_("dmz_ad_flare", age);
            solarFlare(level, ender, 10.0);
        }

        // Teleport combos — more aggressive at high tiers
        long tpCd = kitCd(ender, tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower() ? 35
                : tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower() ? 55 : 80);
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
        if (dist > 4.0 && dist < 16.0 && age - tag.m_128454_("dmz_ad_leap") > kitCd(warden, 50)
                && tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()) {
            tag.m_128356_("dmz_ad_leap", age);
            pushToward(warden, target, 1.35, 0.65);
        }

        // Rapid blasts / Ki barrage — longer CD, small volley (was up to 8 same-tick).
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_barrage") >= kitCd(warden, 95)
                && dist < 22.0f) {
            int shots = 2 + Math.min(1, tier.ordinalPower() / 4);
            if (KiAttackHelper.fireKiBarrage(warden, target, tier, shots) > 0) {
                tag.m_128356_("dmz_ad_ki_barrage", age);
            }
        }

        // Large blast
        if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_large") >= kitCd(warden, 85)
                && dist < 24.0f) {
            if (KiAttackHelper.fireLargeBlast(warden, target, tier)) {
                tag.m_128356_("dmz_ad_large", age);
            }
        }

        // Explosive wave
        if (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()
                && age - tag.m_128454_("dmz_ad_wave") >= kitCd(warden, 140)
                && dist < 12.0f) {
            if (KiAttackHelper.fireExplosiveWave(warden, target, tier)) {
                tag.m_128356_("dmz_ad_wave", age);
            }
        }

        // Beam
        if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_beam") >= kitCd(warden, 100)
                && dist < 26.0f) {
            if (KiAttackHelper.fireKiBeam(warden, target, tier,
                    tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower())) {
                tag.m_128356_("dmz_ad_ki_beam", age);
            }
        }

        // Teleport
        if (tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()
                && age - tag.m_128454_("dmz_ad_tp") >= kitCd(warden, 100)
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

        // Shared ki gap with vanilla fireball replace — prevents kit + projectile double-dump.
        if (!blazeKiReady(blaze, tag, age, tier)) {
            return;
        }

        if (tier.ordinalPower() >= DifficultyTier.AWAKENED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_barrage") >= kitCd(blaze, 85)
                && dist < 18.0f) {
            int shots = 2 + Math.min(1, tier.ordinalPower() / 5);
            if (KiAttackHelper.fireKiBarrage(blaze, target, tier, shots, true) > 0) {
                markBlazeKiShot(tag, age, "dmz_ad_ki_barrage");
                return;
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_blast") >= kitCd(blaze, 70)
                && dist < 20.0f) {
            if (KiAttackHelper.fireKiBlast(blaze, target, tier, true)) {
                markBlazeKiShot(tag, age, "dmz_ad_ki_blast");
                return;
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_explode") >= kitCd(blaze, 120)
                && dist < 16.0f) {
            if (KiAttackHelper.fireExplosionBlast(blaze, target, tier)) {
                markBlazeKiShot(tag, age, "dmz_ad_explode");
            }
        }
        // Intentionally no per-tick ignite — burning is applied by successful ki hits only.
    }

    private static long blazeKiGap(LivingEntity entity, DifficultyTier tier) {
        int power = tier == null ? 0 : tier.ordinalPower();
        long base;
        if (power >= DifficultyTier.ZENITH.ordinalPower()) {
            base = 30L;
        } else if (power >= DifficultyTier.GOD.ordinalPower()) {
            base = 36L;
        } else if (power >= DifficultyTier.MASTER.ordinalPower()) {
            base = 42L;
        } else {
            base = 48L;
        }
        return kitCd(entity, base);
    }

    private static boolean blazeKiReady(
            LivingEntity entity, CompoundTag tag, long age, DifficultyTier tier
    ) {
        return tag != null && age - tag.m_128454_("dmz_ad_ki_shot") >= blazeKiGap(entity, tier);
    }

    private static void markBlazeKiShot(CompoundTag tag, long age, String abilityKey) {
        if (tag == null) {
            return;
        }
        tag.m_128356_("dmz_ad_ki_shot", age);
        if (abilityKey != null && !abilityKey.isBlank()) {
            tag.m_128356_(abilityKey, age);
        }
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
                && age - tag.m_128454_("dmz_ad_large") >= kitCd(ghast, 50)
                && dist < 40.0f) {
            if (KiAttackHelper.fireLargeBlast(ghast, target, tier)) {
                tag.m_128356_("dmz_ad_large", age);
                target.m_20254_(3);
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_beam") >= kitCd(ghast, 80)
                && dist < 42.0f) {
            if (KiAttackHelper.fireKiBeam(ghast, target, tier, true, true)) {
                tag.m_128356_("dmz_ad_ki_beam", age);
            }
        }
        if (tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_wave") >= kitCd(ghast, 120)
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

        if (dist > 3.0 && dist < 12.0 && age - tag.m_128454_("dmz_ad_leap") > kitCd(piglin, 55)
                && tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()) {
            tag.m_128356_("dmz_ad_leap", age);
            pushToward(piglin, target, 1.2, 0.55);
        }
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_blast") >= kitCd(piglin, 50)
                && dist < 16.0) {
            if (KiAttackHelper.fireKiBlast(piglin, target, tier)) {
                tag.m_128356_("dmz_ad_ki_blast", age);
            }
        }
        if (dist < 16.0 && tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()) {
            piglin.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 40, 1, false, false));
            piglin.m_21573_().m_5624_(target, 1.4);
        }
        if (dist < 3.0 && tier.ordinalPower() >= DifficultyTier.ADVANCED.ordinalPower()
                && age - tag.m_128454_("dmz_ad_rush") >= kitCd(piglin, 60)) {
            tag.m_128356_("dmz_ad_rush", age);
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

        if (dist > 3.0 && dist < 13.0 && age - tag.m_128454_("dmz_ad_leap") > kitCd(zp, 50)
                && tier.ordinalPower() >= DifficultyTier.ENHANCED.ordinalPower()) {
            tag.m_128356_("dmz_ad_leap", age);
            pushToward(zp, target, 1.3, 0.6);
        }
        if (tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                && age - tag.m_128454_("dmz_ad_ki_barrage") >= kitCd(zp, 65)
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
        if (dist > 14.0 || age - tag.m_128454_("dmz_ad_aerial") < kitCd(hoglin, 55)) {
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
        // Prefer participants so kits don't lock onto personal-off / blocked players.
        return NearbyPlayers.nearestParticipating(mob, radius);
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
        DifficultyTier tier = resolveAbilityTier(entity, difficulty, EliteSystem.isElite(entity));
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

    /**
     * Replace skeleton arrows / ghast fireballs / blaze shots with ki.
     * <p>
     * Skeletons share a global ki-shot cooldown with {@link #skeletonTick}: when the gap
     * is not ready, the vanilla arrow is still cancelled so high-AI bow spam cannot
     * stunlock players with homing blasts.
     */
    public static boolean tryReplaceProjectile(Mob shooter, LivingEntity target) {
        if (shooter == null || target == null || !shooter.m_6084_()) {
            return false;
        }
        long difficulty = MobScaling.difficultyOf(shooter);
        DifficultyTier tier = resolveAbilityTier(shooter, difficulty, EliteSystem.isElite(shooter));
        if (tier.ordinalPower() < DifficultyTier.AWAKENED.ordinalPower()) {
            return false;
        }
        if (shooter instanceof AbstractSkeleton) {
            CompoundTag tag = PersistentDataAccess.get(shooter);
            long age = shooter.f_19797_;
            if (!skeletonKiReady(shooter, tag, age, tier)) {
                // Eat the arrow — do not fall back to unlimited vanilla/ki hybrid spam.
                return true;
            }
            boolean burning = tier.ordinalPower() >= DifficultyTier.GOD.ordinalPower();
            boolean laserOk = tier.ordinalPower() >= DifficultyTier.ELITE.ordinalPower()
                    && age - tag.m_128454_("dmz_ad_ki_laser") >= kitCd(shooter, 55L);
            boolean fired = false;
            if (laserOk && ThreadLocalRandom.current().nextDouble() < 0.28) {
                fired = KiAttackHelper.fireKiLaser(shooter, target, tier);
                if (fired) {
                    markSkeletonKiShot(tag, age, "dmz_ad_ki_laser");
                }
            }
            if (!fired) {
                fired = KiAttackHelper.fireKiBlast(shooter, target, tier, burning);
                if (fired) {
                    markSkeletonKiShot(tag, age, "dmz_ad_ki_blast");
                }
            }
            // Always suppress the vanilla arrow once evolved — rate limit via ki_shot gap.
            return true;
        }
        if (shooter instanceof Blaze) {
            CompoundTag tag = PersistentDataAccess.get(shooter);
            long age = shooter.f_19797_;
            if (!blazeKiReady(shooter, tag, age, tier)) {
                // Eat the vanilla fireball — do not stack with kit barrage.
                return true;
            }
            boolean fired = KiAttackHelper.fireKiBlast(shooter, target, tier, true);
            if (fired) {
                markBlazeKiShot(tag, age, "dmz_ad_ki_blast");
            }
            return true;
        }
        if (shooter instanceof Ghast) {
            return KiAttackHelper.fireLargeBlast(shooter, target, tier);
        }
        return false;
    }

    /** Difficulty ladder clamped by the mob's Unlock Tier (more tiers → more abilities). */
    private static DifficultyTier resolveAbilityTier(LivingEntity entity, long difficulty, boolean elite) {
        int unlockTier = 0;
        if (entity != null) {
            CompoundTag tag = PersistentDataAccess.get(entity);
            if (tag.m_128441_("dmz_ad_unlock_tier")) {
                unlockTier = tag.m_128451_("dmz_ad_unlock_tier");
            }
        }
        DifficultyTier tier = UnlockAbilityCaps.resolve(difficulty, unlockTier);
        if (elite && tier.ordinalPower() < DifficultyTier.ELITE.ordinalPower() && unlockTier >= 3) {
            tier = UnlockAbilityCaps.clamp(DifficultyTier.ELITE, unlockTier);
        }
        return tier;
    }
}
