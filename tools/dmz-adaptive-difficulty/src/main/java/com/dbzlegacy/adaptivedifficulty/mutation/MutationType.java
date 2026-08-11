package com.dbzlegacy.adaptivedifficulty.mutation;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;

/**
 * Concept §13 mutations. Display names are adjectives only — the live entity type
 * is appended at apply-time so a spider never shows as "Burning Zombie".
 */
public enum MutationType {
    BURNING_ZOMBIE("Burning"),
    ELECTRIC_SKELETON("Electric"),
    GRAVITY_ENDERMAN("Gravity"),
    TITAN_CREEPER("Titan"),
    BERSERKER_PIGLIN("Berserker"),
    SHADOW("Shadow"),
    VAMPIRIC("Vampiric");

    public final String shortName;

    MutationType(String shortName) {
        this.shortName = shortName;
    }

    public static MutationType randomFor(LivingEntity entity) {
        MutationType[] pool;
        if (entity instanceof Zombie) {
            pool = new MutationType[] {BURNING_ZOMBIE, VAMPIRIC, SHADOW};
        } else if (entity instanceof AbstractSkeleton) {
            pool = new MutationType[] {ELECTRIC_SKELETON, SHADOW, VAMPIRIC};
        } else if (entity instanceof EnderMan) {
            pool = new MutationType[] {GRAVITY_ENDERMAN, SHADOW};
        } else if (entity instanceof Creeper) {
            pool = new MutationType[] {TITAN_CREEPER, SHADOW, BURNING_ZOMBIE};
        } else if (entity instanceof AbstractPiglin) {
            pool = new MutationType[] {BERSERKER_PIGLIN, SHADOW, BURNING_ZOMBIE};
        } else {
            // Generic hostiles: only names that work for any mob type.
            pool = new MutationType[] {SHADOW, VAMPIRIC};
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
