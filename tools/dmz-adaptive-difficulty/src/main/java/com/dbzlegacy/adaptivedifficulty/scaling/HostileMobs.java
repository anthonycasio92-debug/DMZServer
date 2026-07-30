package com.dbzlegacy.adaptivedifficulty.scaling;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
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
}
