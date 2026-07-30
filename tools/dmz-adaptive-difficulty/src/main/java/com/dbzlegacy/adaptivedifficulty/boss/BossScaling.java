package com.dbzlegacy.adaptivedifficulty.boss;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraftforge.registries.ForgeRegistries;

/** Concept §16 — bosses inherit nearby difficulty + phased combat. */
public final class BossScaling {
    public static final String TAG_BOSS = "dmz_ad_boss";
    public static final String TAG_PHASE = "dmz_ad_boss_phase";

    private BossScaling() {}

    public static boolean isBoss(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (PersistentDataAccess.get(entity).m_128471_(TAG_BOSS)) {
            return true;
        }
        if (entity instanceof Warden) {
            return true;
        }
        AttributeInstance health = entity.m_21051_(Attributes.f_22276_);
        if (health != null && health.m_22115_() >= DifficultyConfig.get().bossHealthThreshold) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.m_6095_());
        if (id != null) {
            String s = id.toString().toLowerCase();
            for (String needle : DifficultyConfig.get().bossIdContains) {
                if (needle != null && !needle.isBlank() && s.contains(needle.toLowerCase())) {
                    return true;
                }
            }
        }
        // Shurui / DMZ raid boss classname hint
        String cn = entity.getClass().getName().toLowerCase();
        return cn.contains("raidboss") || cn.contains("bossentity") || cn.contains("boss_");
    }

    public static void scaleIfBoss(LivingEntity entity, long difficulty) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableBossScaling || entity == null || difficulty <= 0 || !isBoss(entity)) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(entity);
        if (tag.m_128471_(TAG_BOSS) && tag.m_128451_(TAG_PHASE) > 0) {
            return; // already scaled
        }
        tag.m_128379_(TAG_BOSS, true);
        tag.m_128405_(TAG_PHASE, 1); // putInt
        tag.m_128356_(MobScaling.TAG_DIFFICULTY, Math.max(MobScaling.difficultyOf(entity), difficulty));

        double mult = cfg.bossStatMultiplier * (1.0 + difficulty * (cfg.healthPercentPerDifficulty / 100.0) * 0.5);
        AttributeInstance health = entity.m_21051_(Attributes.f_22276_);
        if (health != null) {
            health.m_22100_(health.m_22115_() * mult);
            entity.m_21153_(entity.m_21233_());
        }
        AttributeInstance armor = entity.m_21051_(Attributes.f_22284_);
        if (armor != null) {
            armor.m_22100_(armor.m_22115_() + difficulty * (cfg.defensePercentPerDifficulty / 100.0));
        }

        DifficultyTier tier = DifficultyTier.of(difficulty);
        String typeName = entity.m_6095_().m_20676_().getString();
        entity.m_6593_(Component.m_237113_("§c☠ Boss §4" + typeName + " §7[" + tier.display + "]"));
        entity.m_20340_(true);
    }

    /** Advance combat phases at HP thresholds (concept warden-style). */
    public static void tickPhases(LivingEntity entity) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableBossScaling || entity == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(entity);
        if (!tag.m_128471_(TAG_BOSS)) {
            if (!isBoss(entity)) {
                return;
            }
            tag.m_128379_(TAG_BOSS, true);
            tag.m_128405_(TAG_PHASE, 1);
        }
        if (entity.f_19797_ % 10 != 0) {
            return;
        }
        float pct = entity.m_21223_() / Math.max(1.0f, entity.m_21233_());
        int phase = tag.m_128451_(TAG_PHASE);
        if (pct <= 0.75f && phase < 2) {
            enterPhase(entity, tag, 2);
        } else if (pct <= 0.50f && phase < 3) {
            enterPhase(entity, tag, 3);
        } else if (pct <= 0.25f && phase < 4) {
            enterPhase(entity, tag, 4);
        }
    }

    private static void enterPhase(LivingEntity entity, CompoundTag tag, int phase) {
        tag.m_128405_(TAG_PHASE, phase);
        entity.m_7292_(new MobEffectInstance(MobEffects.f_19600_, 200, Math.min(3, phase - 1), false, true)); // STRENGTH
        entity.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 200, 1, false, true)); // SPEED
        if (phase >= 3) {
            entity.m_5634_(entity.m_21233_() * 0.1f);
            entity.m_7292_(new MobEffectInstance(MobEffects.f_19606_, 200, 1, false, true)); // RESISTANCE
        }
        if (phase >= 4 && entity instanceof Mob mob) {
            mob.m_7292_(new MobEffectInstance(MobEffects.f_19605_, 160, 1, false, true)); // REGENERATION
        }
        String name = entity.m_7770_() != null ? entity.m_7770_().getString() : "Boss";
        // Strip old phase suffix
        int idx = name.indexOf(" §8P");
        if (idx > 0) {
            name = name.substring(0, idx);
        }
        entity.m_6593_(Component.m_237113_(name + " §8P" + phase));
    }
}
