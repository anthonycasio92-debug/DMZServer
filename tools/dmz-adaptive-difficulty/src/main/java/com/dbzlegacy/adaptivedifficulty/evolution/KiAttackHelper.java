package com.dbzlegacy.adaptivedifficulty.evolution;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
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
            float damage = baseDamage(shooter, tier, burning ? 0.95f : 0.85f);
            float speed = 1.35f + Math.min(0.8f, tier.ordinalPower() * 0.08f);
            int main = burning ? COLOR_BURN : COLOR_MAIN;
            int border = burning ? COLOR_BURN_BORDER : COLOR_BORDER;
            KiBlastEntity blast = new KiBlastEntity(shooter.m_9236_(), shooter);
            blast.setupKiSmall(shooter, damage, speed, main, border);
            hardenAgainstWalls(blast);
            // setupKiSmall adds the entity with firing=true but zero velocity — launch it.
            launchToward(blast, shooter, target, speed);
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
            float damage = baseDamage(shooter, tier, 1.25f);
            float speed = 1.05f + Math.min(0.5f, tier.ordinalPower() * 0.04f);
            float size = 1.6f + Math.min(1.2f, tier.ordinalPower() * 0.08f);
            KiBlastEntity blast = new KiBlastEntity(shooter.m_9236_(), shooter);
            // Cast must be 0 for mobs: fireHability() uses owner look angle, and flying AI
            // (especially Ghasts) overwrites aim during a multi-tick cast.
            blast.setupKiLargeBlast(
                    shooter, damage, speed, COLOR_LARGE, COLOR_BORDER, COLOR_OUTLINE, size, 0);
            hardenAgainstWalls(blast);
            // Direct velocity — same path as small blasts; do not rely on look alone.
            launchToward(blast, shooter, target, speed);
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
            float damage = baseDamage(shooter, tier, burning ? 1.15f : 1.05f);
            float speed = 1.6f + Math.min(0.6f, tier.ordinalPower() * 0.05f);
            int main = burning ? COLOR_BURN : COLOR_MAIN;
            int border = burning ? COLOR_BURN_BORDER : COLOR_BORDER;
            KiLaserEntity laser = new KiLaserEntity(shooter.m_9236_(), shooter);
            // Instant cast + launchToward — mob look drifts during cast (Ghast flight AI).
            laser.setupKiLaser(shooter, damage, speed, main, border, COLOR_OUTLINE, 0);
            hardenAgainstWalls(laser);
            launchToward(laser, shooter, target, speed);
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
            float damage = baseDamage(shooter, tier, charged ? 1.35f : 1.15f);
            float speed = charged ? 0.95f : 1.15f;
            float size = charged ? 1.4f : 0.9f;
            int main = burning ? COLOR_BURN : (charged ? COLOR_CHARGED : COLOR_MAIN);
            int border = burning ? COLOR_BURN_BORDER : COLOR_BORDER;
            KiWaveEntity wave = new KiWaveEntity(shooter.m_9236_(), shooter);
            // Instant cast + launchToward — same look-drift fix as large blasts.
            wave.setupKiWave(shooter, damage, speed, main, border, COLOR_OUTLINE, size, 0);
            hardenAgainstWalls(wave);
            launchToward(wave, shooter, target, speed);
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
            float damage = baseDamage(shooter, tier, (burning ? 0.95f : 0.85f) * damageScale);
            float speed = 1.35f + Math.min(0.8f, tier.ordinalPower() * 0.08f);
            int main = burning ? COLOR_BURN : COLOR_MAIN;
            int border = burning ? COLOR_BURN_BORDER : COLOR_BORDER;
            KiBlastEntity blast = new KiBlastEntity(shooter.m_9236_(), shooter);
            blast.setupKiSmall(shooter, damage, speed, main, border);
            hardenAgainstWalls(blast);
            launchToward(blast, shooter, target, speed);
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

    /**
     * Explosion-flavored blast — ki packet only.
     * World explosions were a second damage channel on top of painted kiblasts
     * (AD creeper-style boom × painted attack), which one-shot at high kits.
     */
    public static boolean fireExplosionBlast(Mob shooter, LivingEntity target, DifficultyTier tier) {
        if (!fireLargeBlast(shooter, target, tier)) {
            return false;
        }
        // Cosmetic pressure only — no second entity-damage channel.
        if (target != null && target.m_6084_()) {
            target.m_20254_(4);
        }
        return true;
    }

    /**
     * Explosive wave — charged ki beam only (no world MOB explosion double-dip).
     */
    public static boolean fireExplosiveWave(Mob shooter, LivingEntity target, DifficultyTier tier) {
        if (!ready(shooter, target) || !(shooter.m_9236_() instanceof ServerLevel)) {
            return false;
        }
        try {
            return fireKiBeam(shooter, target, tier, true, true);
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
                && !shooter.m_9236_().f_46443_
                && hasClearShot(shooter, target);
    }

    /**
     * Refuse to cast when a solid collider sits between eyes — stops wall-hacks /
     * corner-peek spam. Uses vanilla LOS plus an explicit collider clip.
     */
    private static boolean hasClearShot(LivingEntity shooter, LivingEntity target) {
        if (shooter == null || target == null || shooter.m_9236_() != target.m_9236_()) {
            return false;
        }
        try {
            // LivingEntity#hasLineOfSight — eye-to-eye collider ray.
            if (!shooter.m_142582_(target)) {
                return false;
            }
        } catch (Throwable ignored) {
            // Fall through to explicit clip if the mapped helper is unavailable.
        }
        try {
            Vec3 from = shooter.m_146892_(); // getEyePosition
            Vec3 to = target.m_146892_();
            var hit = shooter.m_9236_().m_45547_(new ClipContext(
                    from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
            return hit.m_6662_() == HitResult.Type.MISS;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * AD mob shots must not chew through terrain. Also clear any accidental homing
     * so projectiles fly straight and die on walls instead of steering through them.
     */
    private static void hardenAgainstWalls(AbstractKiProjectile projectile) {
        if (projectile == null) {
            return;
        }
        try {
            projectile.setBlockDestructionEnabled(false);
        } catch (Throwable ignored) {
        }
        try {
            // -1 clears / disables homing target id in DMZ AbstractKiProjectile.
            projectile.setHomingTarget(-1);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Keep a flying / ranged mob facing its combat target (Ghast flight AI otherwise
     * wanders look away from the player between shots).
     */
    public static void faceTarget(Mob shooter, LivingEntity target) {
        if (shooter == null || target == null || !shooter.m_6084_() || !target.m_6084_()) {
            return;
        }
        aimAt(shooter, target);
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
     * Ki damage must be on the same scale as AD melee {@code ATTACK_DAMAGE}.
     * <p>
     * DMZ {@code calculatePostMitigationDamage} returns <b>0</b> when victim DEF
     * ≫ incoming amount ({@code cancelDamageEventIfMitigationTooHigh}). Vanilla-scale
     * 3.5–72 kiblasts were always cancelled for real characters — shots looked fine
     * but never hurt. Bake the mob's scaled attack × skill ratio instead.
     * {@link MobScaling#scaleOutgoingHurt} skips a second multiply for kiblast once
     * attack attrs are already AD-painted.
     *
     * @param skillRatio fraction of the mob's scaled attack (blast ≈ 0.85, laser ≈ 1.05, …)
     */
    private static float baseDamage(Mob shooter, DifficultyTier tier, float skillRatio) {
        float atk = readScaledAttack(shooter);
        float ratio = Math.max(0.25f, skillRatio) * earlyKitRatioScale(shooter, tier);
        float tierSpice = 1.0f + Math.max(0, tier.ordinalPower()) * 0.018f;
        float dmg = atk * ratio * tierSpice;
        // Soft ceiling vs the mob's own melee so barrages cannot outpace a punch train.
        float ceiling = Math.max(atk * 1.65f, atk + 25.0f);
        // Floor scales with painted attack — avoid a hard 8 that overshoots tiny early kits.
        float floor = Math.max(2.0f, Math.min(8.0f, atk * 0.20f));
        return Math.max(floor, Math.min(ceiling, dmg));
    }

    /** Soften kiblast ratios on early unlock / kit bands so T1–T2 ≠ melee-parity barrages. */
    private static float earlyKitRatioScale(Mob shooter, DifficultyTier tier) {
        int unlock = MobScaling.unlockTierOf(shooter);
        if (unlock > 0 && unlock <= 2) {
            return 0.60f;
        }
        if (unlock == 3) {
            return 0.78f;
        }
        if (unlock <= 0 && tier != null
                && tier.ordinalPower() <= DifficultyTier.ENHANCED.ordinalPower()) {
            return 0.70f;
        }
        return 1.0f;
    }

    /** Live AD-painted attack, with offense-mult fallback when attrs aren't ready yet. */
    private static float readScaledAttack(Mob shooter) {
        if (shooter == null) {
            return 8.0f;
        }
        try {
            var inst = shooter.m_21051_(Attributes.f_22281_); // ATTACK_DAMAGE
            if (inst != null) {
                double v = inst.m_22135_(); // getValue
                if (v > 1.0 && !Double.isNaN(v) && !Double.isInfinite(v)) {
                    return (float) v;
                }
            }
        } catch (Throwable ignored) {
        }
        // Fallback: raw-ish floor × AD outgoing mult (claim may not have painted attrs yet).
        float mult = MobScaling.outgoingDamageMultiplier(shooter);
        return Math.max(8.0f, 12.0f * Math.max(1.0f, mult));
    }
}
