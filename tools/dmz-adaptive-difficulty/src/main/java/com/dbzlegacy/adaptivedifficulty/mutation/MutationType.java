package com.dbzlegacy.adaptivedifficulty.mutation;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;

public enum MutationType {
    BURNING_ZOMBIE("Burning", "Burning Zombie"),
    ELECTRIC_SKELETON("Electric", "Electric Skeleton"),
    GRAVITY_ENDERMAN("Gravity", "Gravity Enderman"),
    TITAN_CREEPER("Titan", "Titan Creeper"),
    BERSERKER_PIGLIN("Berserker", "Berserker Piglin"),
    SHADOW("Shadow", "Shadow Stalker"),
    VAMPIRIC("Vampiric", "Vampiric Horror");

    public final String shortName;
    public final String displayName;

    MutationType(String shortName, String displayName) {
        this.shortName = shortName;
        this.displayName = displayName;
    }

    public static MutationType randomFor(LivingEntity entity) {
        MutationType[] pool;
        if (entity instanceof Zombie) {
            pool = new MutationType[] {BURNING_ZOMBIE, VAMPIRIC, SHADOW};
        } else if (entity instanceof Skeleton) {
            pool = new MutationType[] {ELECTRIC_SKELETON, SHADOW};
        } else if (entity instanceof EnderMan) {
            pool = new MutationType[] {GRAVITY_ENDERMAN, SHADOW};
        } else if (entity instanceof Creeper) {
            pool = new MutationType[] {TITAN_CREEPER, BURNING_ZOMBIE};
        } else if (entity instanceof AbstractPiglin) {
            pool = new MutationType[] {BERSERKER_PIGLIN, BURNING_ZOMBIE};
        } else {
            pool = values();
        }
        return pool[ThreadLocalRandom.current().nextInt(pool.length)];
    }

    public static MutationType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Apply ongoing mutation effects (throttled by caller). */
    public void tick(LivingEntity entity) {
        switch (this) {
            case BURNING_ZOMBIE -> {
                entity.m_20254_(3); // setSecondsOnFire
                entity.m_7292_(new MobEffectInstance(MobEffects.f_19607_, 40, 0, false, false)); // FIRE_RESISTANCE
            }
            case ELECTRIC_SKELETON -> entity.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 40, 1, false, false)); // SPEED
            case GRAVITY_ENDERMAN -> entity.m_7292_(new MobEffectInstance(MobEffects.f_19603_, 40, 0, false, false)); // JUMP_BOOST
            case TITAN_CREEPER -> entity.m_7292_(new MobEffectInstance(MobEffects.f_19606_, 40, 0, false, false)); // RESISTANCE
            case BERSERKER_PIGLIN -> {
                entity.m_7292_(new MobEffectInstance(MobEffects.f_19607_, 40, 0, false, false));
                entity.m_7292_(new MobEffectInstance(MobEffects.f_19600_, 40, 1, false, false)); // STRENGTH
            }
            case SHADOW -> entity.m_7292_(new MobEffectInstance(MobEffects.f_19609_, 40, 0, false, false)); // INVISIBILITY
            case VAMPIRIC -> {
                if (entity.m_21223_() < entity.m_21233_() * 0.5f) {
                    entity.m_5634_(1.0f);
                }
            }
        }
    }
}
