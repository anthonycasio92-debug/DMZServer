package com.dbzlegacy.adaptivedifficulty.scaling;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;

/**
 * Shared hostile detection for scaling + evolution.
 * <p>
 * Vanilla {@link MobCategory#MONSTER} alone misses hostiles like Hoglin
 * ({@code CREATURE} + {@link Enemy}) and many modded enemies.
 */
public final class HostileMobs {
    private HostileMobs() {}

    public static boolean isHostile(LivingEntity entity) {
        return entity instanceof Mob mob && isHostile(mob);
    }

    public static boolean isHostile(Mob mob) {
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
            MobCategory cat = mob.m_6095_().m_20674_();
            return cat == MobCategory.MONSTER;
        } catch (Throwable t) {
            return false;
        }
    }

    /** True when both sides are hostiles — used to block friendly fire / mob civil wars. */
    public static boolean bothHostile(LivingEntity a, LivingEntity b) {
        return isHostile(a) && isHostile(b);
    }

    /**
     * True when {@code a}/{@code b} are vehicle↔passenger (spider jockey, skeleton horse,
     * etc.). Skeleton ki blasts spawn on the rider and otherwise nuke their own mount.
     */
    public static boolean isMountPair(Entity a, Entity b) {
        if (a == null || b == null || a == b) {
            return false;
        }
        try {
            if (a.m_20202_() == b || b.m_20202_() == a) { // getVehicle
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (a.m_20197_().contains(b) || b.m_20197_().contains(a)) { // getPassengers
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Drop other-hostile combat targets and revenge memory so vanilla
     * {@code HurtByTargetGoal} cannot keep packs fighting each other.
     *
     * @return true if a hostile target was cleared
     */
    public static boolean clearCivilWarAggro(Mob mob) {
        if (mob == null) {
            return false;
        }
        boolean cleared = false;
        LivingEntity target = mob.m_5448_(); // getTarget
        if (target != null && !(target instanceof Player) && isHostile(target)) {
            mob.m_6710_(null); // setTarget
            cleared = true;
        }
        LivingEntity revenge = mob.m_21188_(); // getLastHurtByMob
        if (revenge != null && !(revenge instanceof Player) && isHostile(revenge)) {
            mob.m_6703_(null); // setLastHurtByMob
            cleared = true;
        }
        return cleared;
    }
}
