package com.dbzlegacy.adaptivedifficulty.evolution;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Fires real DragonMineZ ki projectiles from evolved hostiles.
 * <p>
 * DMZ {@code setupKiSmall} / cast setups spawn the entity at the shooter but do
 * <b>not</b> apply launch velocity for mobs. Player kits call {@code shoot}
 * separately — we must do the same or blasts sit stuck on the shooter.
 */
public final class KiAttackHelper {
    private static final int COLOR_MAIN = 0x55DDFF;
    private static final int COLOR_BORDER = 0x2288FF;
    private static final int COLOR_OUTLINE = 0xFFFFFF;
    private static final int COLOR_CHARGED = 0xFFAA33;
    private static final int COLOR_BURN = 0xFF5522;
    private static final int COLOR_BURN_BORDER = 0xFF2200;
    private static final int COLOR_LARGE = 0xAA66FF;

    private KiAttackHelper() {}

    public static boolean fireKiBlast(Mob shooter, LivingEntity target, DifficultyTier tier) {
        return fireKiBlast(shooter, target, tier, false);
    }

    public static boolean fireKiBlast(Mob shooter, LivingEntity target, DifficultyTier tier, boolean burning) {
        if (!ready(shooter, target)) {
            return false;
        }
        try {
            aimAt(shooter, target);
            float damage = baseDamage(shooter, tier, burning ? 4.0f : 3.5f);
            float speed = 1.35f + Math.min(0.8f, tier.ordinalPower() * 0.08f);
            int main = burning ? COLOR_BURN : COLOR_MAIN;
            int border = burning ? COLOR_BURN_BORDER : COLOR_BORDER;
            KiBlastEntity blast = new KiBlastEntity(shooter.m_9236_(), shooter);
            blast.setupKiSmall(shooter, damage, speed, main, border);
            // setupKiSmall adds the entity with firing=true but zero velocity — launch it.
            launchToward(blast, shooter, target, speed);
            blast.setHomingTarget(target.m_19879_());
            if (burning) {
                target.m_20254_(4);
            }
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] ki blast failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    /** Larger, slower blast (Ghast / Warden large blast). */
    public static boolean fireLargeBlast(Mob shooter, LivingEntity target, DifficultyTier tier) {
        if (!ready(shooter, target)) {
            return false;
        }
        try {
            aimAt(shooter, target);
            float damage = baseDamage(shooter, tier, 7.5f);
            float speed = 1.05f + Math.min(0.5f, tier.ordinalPower() * 0.04f);
            float size = 1.6f + Math.min(1.2f, tier.ordinalPower() * 0.08f);
            KiBlastEntity blast = new KiBlastEntity(shooter.m_9236_(), shooter);
            // Cast-then-fire path; aim the caster so fireHability shoots at the player.
            int cast = Math.max(6, 16 - tier.ordinalPower());
            blast.setupKiLargeBlast(
                    shooter, damage, speed, COLOR_LARGE, COLOR_BORDER, COLOR_OUTLINE, size, cast);
            blast.setHomingTarget(target.m_19879_());
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] large blast failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    public static boolean fireKiLaser(Mob shooter, LivingEntity target, DifficultyTier tier) {
        return fireKiLaser(shooter, target, tier, false);
    }

    public static boolean fireKiLaser(Mob shooter, LivingEntity target, DifficultyTier tier, boolean burning) {
        if (!ready(shooter, target)) {
            return false;
        }
        try {
            aimAt(shooter, target);
            float damage = baseDamage(shooter, tier, burning ? 5.5f : 5.0f);
            float speed = 1.6f + Math.min(0.6f, tier.ordinalPower() * 0.05f);
            int cast = Math.max(4, 14 - tier.ordinalPower());
            int main = burning ? COLOR_BURN : COLOR_MAIN;
            int border = burning ? COLOR_BURN_BORDER : COLOR_BORDER;
            KiLaserEntity laser = new KiLaserEntity(shooter.m_9236_(), shooter);
            laser.setupKiLaser(shooter, damage, speed, main, border, COLOR_OUTLINE, cast);
            laser.setHomingTarget(target.m_19879_());
            if (burning) {
                target.m_20254_(5);
            }
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] ki laser failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    public static boolean fireKiBeam(Mob shooter, LivingEntity target, DifficultyTier tier, boolean charged) {
        return fireKiBeam(shooter, target, tier, charged, false);
    }

    public static boolean fireKiBeam(
            Mob shooter, LivingEntity target, DifficultyTier tier, boolean charged, boolean burning) {
        if (!ready(shooter, target)) {
            return false;
        }
        try {
            aimAt(shooter, target);
            float damage = baseDamage(shooter, tier, charged ? 8.0f : 6.0f);
            float speed = charged ? 0.95f : 1.15f;
            float size = charged ? 1.4f : 0.9f;
            int cast = charged ? 28 : 16;
            int main = burning ? COLOR_BURN : (charged ? COLOR_CHARGED : COLOR_MAIN);
            int border = burning ? COLOR_BURN_BORDER : COLOR_BORDER;
            KiWaveEntity wave = new KiWaveEntity(shooter.m_9236_(), shooter);
            wave.setupKiWave(shooter, damage, speed, main, border, COLOR_OUTLINE, size, cast);
            wave.setHomingTarget(target.m_19879_());
            if (burning) {
                target.m_20254_(charged ? 7 : 5);
            }
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] ki beam failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    public static int fireKiBarrage(Mob shooter, LivingEntity target, DifficultyTier tier, int count) {
        return fireKiBarrage(shooter, target, tier, count, false);
    }

    public static int fireKiBarrage(
            Mob shooter, LivingEntity target, DifficultyTier tier, int count, boolean burning) {
        // Barrages use a soft damage scale so same-tick volleys cannot spike through armor.
        return fireKiBarrage(shooter, target, tier, count, burning, 0.55f);
    }

    public static int fireKiBarrage(
            Mob shooter,
            LivingEntity target,
            DifficultyTier tier,
            int count,
            boolean burning,
            float damageScale) {
        if (!ready(shooter, target)) {
            return 0;
        }
        int fired = 0;
        // Hard cap volley size — high tiers used to dump up to 8 same-tick blasts.
        int n = Math.max(1, Math.min(3, count));
        float scale = Math.max(0.25f, Math.min(1.0f, damageScale));
        for (int i = 0; i < n; i++) {
            if (fireKiBlastScaled(shooter, target, tier, burning, scale)) {
                fired++;
            } else {
                break;
            }
        }
        return fired;
    }

    private static boolean fireKiBlastScaled(
            Mob shooter, LivingEntity target, DifficultyTier tier, boolean burning, float damageScale) {
        if (!ready(shooter, target)) {
            return false;
        }
        try {
            aimAt(shooter, target);
            float damage = baseDamage(shooter, tier, burning ? 4.0f : 3.5f) * damageScale;
            float speed = 1.35f + Math.min(0.8f, tier.ordinalPower() * 0.08f);
            int main = burning ? COLOR_BURN : COLOR_MAIN;
            int border = burning ? COLOR_BURN_BORDER : COLOR_BORDER;
            KiBlastEntity blast = new KiBlastEntity(shooter.m_9236_(), shooter);
            blast.setupKiSmall(shooter, damage, speed, main, border);
            launchToward(blast, shooter, target, speed);
            blast.setHomingTarget(target.m_19879_());
            if (burning) {
                target.m_20254_(3);
            }
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] ki blast failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    /** Explosion blast — ki hit + small world explosion near the target. */
    public static boolean fireExplosionBlast(Mob shooter, LivingEntity target, DifficultyTier tier) {
        if (!fireLargeBlast(shooter, target, tier)) {
            return false;
        }
        if (shooter.m_9236_() instanceof ServerLevel level) {
            float power = 1.6f + Math.min(3.5f, tier.ordinalPower() * 0.35f);
            level.m_254849_(shooter, target.m_20185_(), target.m_20186_(), target.m_20189_(),
                    power, Level.ExplosionInteraction.NONE);
            target.m_20254_(4);
        }
        return true;
    }

    /** Explosive wave around the shooter. */
    public static boolean fireExplosiveWave(Mob shooter, LivingEntity target, DifficultyTier tier) {
        if (!ready(shooter, target) || !(shooter.m_9236_() instanceof ServerLevel level)) {
            return false;
        }
        try {
            fireKiBeam(shooter, target, tier, true, true);
            float power = 2.2f + Math.min(4.0f, tier.ordinalPower() * 0.4f);
            level.m_254849_(shooter, shooter.m_20185_(), shooter.m_20186_(), shooter.m_20189_(),
                    power, Level.ExplosionInteraction.MOB);
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] explosive wave failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    private static boolean ready(Mob shooter, LivingEntity target) {
        return shooter != null
                && target != null
                && shooter.m_6084_()
                && target.m_6084_()
                && shooter.m_9236_() instanceof ServerLevel
                && !shooter.m_9236_().f_46443_;
    }

    /**
     * Point the mob at the target and sync body/head yaw immediately so cast-fire
     * ({@code fireHability}) uses a correct look vector.
     */
    private static void aimAt(Mob shooter, LivingEntity target) {
        try {
            Vec3 from = shooter.m_146892_();
            Vec3 to = target.m_146892_();
            double dx = to.f_82479_ - from.f_82479_;
            double dy = to.f_82480_ - from.f_82480_;
            double dz = to.f_82481_ - from.f_82481_;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.max(1.0E-4, horiz))));
            shooter.m_146922_(yaw); // setYRot
            shooter.m_146926_(pitch); // setXRot
            shooter.m_5618_(yaw); // setYBodyRot
            shooter.m_5616_(yaw); // setYHeadRot
            shooter.m_21391_(target, 360.0f, 360.0f);
            shooter.m_21563_().m_24960_(target, 360.0f, 360.0f);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Apply real launch velocity toward the target.
     * Homing only steers existing motion — zero-speed projectiles stay glued to the shooter.
     */
    private static void launchToward(Projectile projectile, Mob shooter, LivingEntity target, float speed) {
        if (projectile == null || shooter == null || target == null) {
            return;
        }
        aimAt(shooter, target);
        double dx = target.m_20185_() - projectile.m_20185_();
        double dy = (target.m_20186_() + target.m_20206_() * 0.45) - projectile.m_20186_();
        double dz = target.m_20189_() - projectile.m_20189_();
        if (dx * dx + dy * dy + dz * dz < 1.0E-6) {
            Vec3 from = shooter.m_146892_();
            Vec3 to = target.m_146892_();
            dx = to.f_82479_ - from.f_82479_;
            dy = to.f_82480_ - from.f_82480_;
            dz = to.f_82481_ - from.f_82481_;
        }
        float launchSpeed = Math.max(0.75f, speed);
        // shoot(dx, dy, dz, velocity, inaccuracy) — same path DMZ player barrages use via shootFromRotation.
        projectile.m_6686_(dx, dy, dz, launchSpeed, 0.35f);
        if (projectile instanceof AbstractKiProjectile ki) {
            // Keep firing flag / homing active after we override motion.
            try {
                ki.setFiring(true);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Tier-scaled base damage. Difficulty multiplier is applied once by
     * {@link com.dbzlegacy.adaptivedifficulty.scaling.MobScaling#scaleOutgoingHurt}
     * for projectiles / ki (indirect hits) — do not bake difficulty here.
     */
    private static float baseDamage(Mob shooter, DifficultyTier tier, float base) {
        float tierBonus = Math.max(0, tier.ordinalPower()) * 1.35f;
        // Soft cap before offense / projectile mult — keeps high-tier kits threatening, not spike-nuke.
        return Math.min(72.0f, base + tierBonus);
    }
}
