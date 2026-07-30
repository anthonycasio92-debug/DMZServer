package com.dbzlegacy.adaptivedifficulty.evolution;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Fires real DragonMineZ ki projectiles from evolved hostiles (concept §11).
 * Uses DMZ entity APIs ({@code KiBlastEntity}, {@code KiLaserEntity}, {@code KiWaveEntity}).
 */
public final class KiAttackHelper {
    private static final int COLOR_MAIN = 0x55DDFF;
    private static final int COLOR_BORDER = 0x2288FF;
    private static final int COLOR_OUTLINE = 0xFFFFFF;
    private static final int COLOR_CHARGED = 0xFFAA33;

    private KiAttackHelper() {}

    public static boolean fireKiBlast(Mob shooter, LivingEntity target, DifficultyTier tier) {
        if (!ready(shooter, target)) {
            return false;
        }
        try {
            aimAt(shooter, target);
            float damage = baseDamage(shooter, tier, 3.5f);
            float speed = 1.35f + Math.min(0.8f, tier.ordinalPower() * 0.08f);
            KiBlastEntity blast = new KiBlastEntity(shooter.m_9236_(), shooter);
            // Immediate small blast — setup adds the entity to the world.
            blast.setupKiSmall(shooter, damage, speed, COLOR_MAIN, COLOR_BORDER);
            blast.setHomingTarget(target.m_19879_());
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] ki blast failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    public static boolean fireKiLaser(Mob shooter, LivingEntity target, DifficultyTier tier) {
        if (!ready(shooter, target)) {
            return false;
        }
        try {
            aimAt(shooter, target);
            float damage = baseDamage(shooter, tier, 5.0f);
            float speed = 1.6f + Math.min(0.6f, tier.ordinalPower() * 0.05f);
            int cast = Math.max(4, 14 - tier.ordinalPower());
            KiLaserEntity laser = new KiLaserEntity(shooter.m_9236_(), shooter);
            laser.setupKiLaser(shooter, damage, speed, COLOR_MAIN, COLOR_BORDER, COLOR_OUTLINE, cast);
            laser.setHomingTarget(target.m_19879_());
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] ki laser failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    /** Shorter beam / continuous wave. */
    public static boolean fireKiBeam(Mob shooter, LivingEntity target, DifficultyTier tier, boolean charged) {
        if (!ready(shooter, target)) {
            return false;
        }
        try {
            aimAt(shooter, target);
            float damage = baseDamage(shooter, tier, charged ? 8.0f : 6.0f);
            float speed = charged ? 0.95f : 1.15f;
            float size = charged ? 1.4f : 0.9f;
            int cast = charged ? 28 : 16;
            int main = charged ? COLOR_CHARGED : COLOR_MAIN;
            KiWaveEntity wave = new KiWaveEntity(shooter.m_9236_(), shooter);
            wave.setupKiWave(shooter, damage, speed, main, COLOR_BORDER, COLOR_OUTLINE, size, cast);
            wave.setHomingTarget(target.m_19879_());
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] ki beam failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    /** Warden / high-tier barrage: several small blasts aimed at the target. */
    public static int fireKiBarrage(Mob shooter, LivingEntity target, DifficultyTier tier, int count) {
        if (!ready(shooter, target)) {
            return 0;
        }
        int fired = 0;
        int n = Math.max(1, Math.min(6, count));
        for (int i = 0; i < n; i++) {
            try {
                aimAt(shooter, target);
                float damage = baseDamage(shooter, tier, 2.2f);
                float speed = 1.2f + (i * 0.05f);
                KiBlastEntity blast = new KiBlastEntity(shooter.m_9236_(), shooter);
                blast.setupKiSmall(shooter, damage, speed, COLOR_MAIN, COLOR_BORDER);
                blast.setHomingTarget(target.m_19879_());
                fired++;
            } catch (Throwable t) {
                AdaptiveDifficultyMod.LOGGER.debug(
                        "[{}] ki barrage shot failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
                break;
            }
        }
        return fired;
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
            shooter.m_21391_(target, 360.0f, 360.0f); // lookAt
            shooter.m_21563_().m_24960_(target, 360.0f, 360.0f); // getLookControl().setLookAt
        } catch (Throwable ignored) {
        }
    }

    private static float baseDamage(Mob shooter, DifficultyTier tier, float base) {
        long difficulty = Math.max(0L, MobScaling.difficultyOf(shooter));
        float tierBonus = Math.max(0, tier.ordinalPower()) * 1.25f;
        float scale = 1.0f + (float) (difficulty * 0.01);
        // Cap so Impossible tiers don't one-shot absurdly from a single blast.
        return Math.min(80.0f, (base + tierBonus) * Math.min(scale, 8.0f));
    }
}
