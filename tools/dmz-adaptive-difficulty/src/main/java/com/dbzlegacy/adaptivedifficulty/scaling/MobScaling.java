package com.dbzlegacy.adaptivedifficulty.scaling;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Apply scaling once at spawn and cache difficulty on the mob (concept §9 / §17).
 * Uses SRG attribute/entity accessors to match this repo's Mohist compile classpath.
 */
public final class MobScaling {
    public static final String TAG_DIFFICULTY = "dmz_ad_difficulty";
    public static final String TAG_SCALED = "dmz_ad_scaled";

    /** Vanilla generic.max_health upper bound — never push past this. */
    private static final double VANILLA_MAX_HEALTH_CAP = 1024.0;

    private MobScaling() {}

    public static long difficultyOf(LivingEntity entity) {
        if (entity == null) {
            return 0L;
        }
        CompoundTag tag = PersistentDataAccess.get(entity);
        return tag.m_128441_(TAG_DIFFICULTY) ? tag.m_128454_(TAG_DIFFICULTY) : 0L;
    }

    public static void scaleIfNeeded(LivingEntity entity) {
        try {
            scaleIfNeededInternal(entity);
        } catch (Throwable t) {
            // Never let scaling abort FinalizeSpawn — that kills natural spawns.
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] mob scaling failed for {}: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    entity == null ? "?" : entity.m_6095_().toString(),
                    t.toString()
            );
        }
    }

    private static void scaleIfNeededInternal(LivingEntity entity) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableMobScaling || entity == null || entity.m_9236_().f_46443_) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(entity);
        if (tag.m_128471_(TAG_SCALED)) {
            return;
        }
        if (cfg.scaleHostileOnly && entity instanceof Mob mob) {
            if (mob.m_6095_().m_20674_() != MobCategory.MONSTER) {
                tag.m_128379_(TAG_SCALED, true);
                tag.m_128356_(TAG_DIFFICULTY, 0L);
                return;
            }
        }

        // Detect bosses BEFORE health scaling. Post-scale HP>=threshold was marking
        // every high-difficulty zombie as a boss and re-multiplying stats into oblivion.
        boolean boss = BossScaling.isNaturalBoss(entity);

        long difficulty = resolveNearbyDifficulty(entity);
        tag.m_128356_(TAG_DIFFICULTY, difficulty);
        tag.m_128379_(TAG_SCALED, true);
        if (difficulty <= 0) {
            return;
        }

        double healthMult = 1.0 + (difficulty * (cfg.healthPercentPerDifficulty / 100.0));
        double armorBonus = difficulty * (cfg.defensePercentPerDifficulty / 100.0);
        double moveMult = 1.0 + ((difficulty / 100.0) * (cfg.movementPercentPer100Difficulty / 100.0));

        if (isDragonMineZMob(entity)) {
            healthMult += difficulty * (cfg.dmzExtraHealthPercent / 100.0);
            armorBonus += difficulty * (cfg.dmzExtraDefensePercent / 100.0);
            tag.m_128379_("dmz_ad_dmz_mob", true);
        }

        healthMult = clamp(healthMult, 1.0, Math.max(1.0, cfg.maxHealthMultiplier));
        moveMult = clamp(moveMult, 1.0, Math.max(1.0, cfg.maxMoveMultiplier));
        armorBonus = Math.min(armorBonus, Math.max(0.0, cfg.maxArmorBonus));

        scaleMaxHealth(entity, healthMult, cfg.maxScaledHealth);
        scaleAttribute(entity, Attributes.f_22279_, moveMult); // MOVEMENT_SPEED
        AttributeInstance armor = entity.m_21051_(Attributes.f_22284_); // ARMOR
        if (armor != null && armorBonus > 0) {
            double next = Math.min(30.0, armor.m_22115_() + armorBonus);
            armor.m_22100_(next);
        }

        if (boss) {
            BossScaling.scaleIfBoss(entity, difficulty);
        }
        EliteSystem.maybePromote(entity, difficulty);
        MutationSystem.maybeMutate(entity, difficulty);
    }

    public static boolean isDragonMineZMob(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.m_6095_());
        if (id != null && "dragonminez".equals(id.m_135827_())) {
            return true;
        }
        String cn = entity.getClass().getName().toLowerCase();
        return cn.contains("dragonminez") || cn.contains("shurui");
    }

    private static void scaleMaxHealth(LivingEntity entity, double multiplier, double hardCap) {
        AttributeInstance instance = entity.m_21051_(Attributes.f_22276_);
        if (instance == null || multiplier <= 1.0) {
            return;
        }
        double cap = hardCap > 0 ? Math.min(hardCap, VANILLA_MAX_HEALTH_CAP) : VANILLA_MAX_HEALTH_CAP;
        double base = instance.m_22115_();
        if (!(base > 0.0) || Double.isNaN(base) || Double.isInfinite(base)) {
            return;
        }
        double next = Math.min(cap, base * multiplier);
        if (!(next > 0.0) || Double.isNaN(next) || Double.isInfinite(next)) {
            return;
        }
        instance.m_22100_(next);
        float max = entity.m_21233_();
        if (max > 0.0f && !Float.isNaN(max) && !Float.isInfinite(max)) {
            entity.m_21153_(max);
        }
    }

    private static void scaleAttribute(LivingEntity entity, Attribute attribute, double multiplier) {
        AttributeInstance instance = entity.m_21051_(attribute);
        if (instance == null || multiplier <= 1.0) {
            return;
        }
        double base = instance.m_22115_();
        if (!(base > 0.0) || Double.isNaN(base) || Double.isInfinite(base)) {
            return;
        }
        double next = base * multiplier;
        if (!(next > 0.0) || Double.isNaN(next) || Double.isInfinite(next)) {
            return;
        }
        instance.m_22100_(next);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long resolveNearbyDifficulty(LivingEntity entity) {
        if (!(entity.m_9236_() instanceof ServerLevel level)) {
            return 0L;
        }
        // Scaling Health-style area difficulty at the mob's block position.
        return AreaDifficulty.at(level, entity.m_20183_());
    }

    public static float outgoingDamageMultiplier(LivingEntity attacker) {
        long d = difficultyOf(attacker);
        if (d <= 0) {
            return 1.0f;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        double mult = 1.0 + d * (cfg.damagePercentPerDifficulty / 100.0);
        if (isDragonMineZMob(attacker) || PersistentDataAccess.get(attacker).m_128471_("dmz_ad_dmz_mob")) {
            mult += d * (cfg.dmzExtraDamagePercent / 100.0);
            mult += d * (cfg.dmzExtraKiDamagePercent / 100.0);
        }
        // Keep damage sane so one hit doesn't break combat systems
        mult = Math.min(mult, Math.max(1.0, cfg.maxDamageMultiplier));
        return (float) mult;
    }
}
