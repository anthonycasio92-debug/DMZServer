package com.dbzlegacy.adaptivedifficulty.evolution;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * Fires real DragonMineZ ki projectiles from evolved hostiles.
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
            KiBlastEntity blast = new KiBlastEntity(shooter.m_9236_(), shooter);
            blast.setupKiSmall(shooter, damage, speed, COLOR_LARGE, COLOR_BORDER);
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
        if (!ready(shooter, target)) {
            return 0;
        }
        int fired = 0;
        int n = Math.max(1, Math.min(8, count));
        for (int i = 0; i < n; i++) {
            if (fireKiBlast(shooter, target, tier, burning)) {
                fired++;
            } else {
                break;
            }
        }
        return fired;
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

    private static void aimAt(Mob shooter, LivingEntity target) {
        try {
            shooter.m_21391_(target, 360.0f, 360.0f);
            shooter.m_21563_().m_24960_(target, 360.0f, 360.0f);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Tier-scaled base damage. Difficulty multiplier is applied once by
     * {@link com.dbzlegacy.adaptivedifficulty.scaling.MobScaling#scaleOutgoingHurt}
     * for projectiles / ki (indirect hits) — do not bake difficulty here.
     */
    private static float baseDamage(Mob shooter, DifficultyTier tier, float base) {
        float tierBonus = Math.max(0, tier.ordinalPower()) * 2.0f;
        // Higher soft cap so Zenith kits start from a real base before the offense mult.
        return Math.min(120.0f, base + tierBonus);
    }
}
