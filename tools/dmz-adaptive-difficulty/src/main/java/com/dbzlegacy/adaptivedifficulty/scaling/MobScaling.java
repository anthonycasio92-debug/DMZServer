package com.dbzlegacy.adaptivedifficulty.scaling;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

/**
 * Apply scaling once at spawn and cache difficulty on the mob (concept §9 / §17).
 * Uses SRG attribute/entity accessors to match this repo's Mohist compile classpath.
 */
public final class MobScaling {
    public static final String TAG_DIFFICULTY = "dmz_ad_difficulty";
    public static final String TAG_SCALED = "dmz_ad_scaled";

    private MobScaling() {}

    public static long difficultyOf(LivingEntity entity) {
        if (entity == null) {
            return 0L;
        }
        CompoundTag tag = PersistentDataAccess.get(entity);
        return tag.m_128441_(TAG_DIFFICULTY) ? tag.m_128454_(TAG_DIFFICULTY) : 0L;
    }

    public static void scaleIfNeeded(LivingEntity entity) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableMobScaling || entity == null || entity.m_9236_().f_46443_) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(entity);
        if (tag.m_128471_(TAG_SCALED)) { // getBoolean
            return;
        }
        if (cfg.scaleHostileOnly && entity instanceof Mob mob) {
            if (mob.m_6095_().m_20674_() != MobCategory.MONSTER) {
                tag.m_128379_(TAG_SCALED, true); // putBoolean
                tag.m_128356_(TAG_DIFFICULTY, 0L); // putLong
                return;
            }
        }

        long difficulty = resolveNearbyDifficulty(entity);
        tag.m_128356_(TAG_DIFFICULTY, difficulty);
        tag.m_128379_(TAG_SCALED, true);
        if (difficulty <= 0) {
            return;
        }

        double healthMult = 1.0 + (difficulty * (cfg.healthPercentPerDifficulty / 100.0));
        double armorBonus = difficulty * (cfg.defensePercentPerDifficulty / 100.0);
        double moveMult = 1.0 + ((difficulty / 100.0) * (cfg.movementPercentPer100Difficulty / 100.0));

        healthMult += difficulty * (cfg.dmzExtraHealthPercent / 100.0);
        armorBonus += difficulty * (cfg.dmzExtraDefensePercent / 100.0);

        // Health / move / armor via attributes once at spawn.
        // Attack damage is applied via LivingEntityHurtScaleMixin so projectiles
        // and non-attribute hits also scale (avoids double-applying melee).
        scaleAttribute(entity, Attributes.f_22276_, healthMult, true); // MAX_HEALTH
        scaleAttribute(entity, Attributes.f_22279_, moveMult, false); // MOVEMENT_SPEED
        AttributeInstance armor = entity.m_21051_(Attributes.f_22284_); // ARMOR
        if (armor != null && armorBonus > 0) {
            armor.m_22100_(armor.m_22115_() + Math.min(30.0, armorBonus));
        }
    }

    private static void scaleAttribute(
            LivingEntity entity,
            Attribute attribute,
            double multiplier,
            boolean healToFull
    ) {
        AttributeInstance instance = entity.m_21051_(attribute);
        if (instance == null || multiplier <= 1.0) {
            return;
        }
        double next = instance.m_22115_() * multiplier;
        instance.m_22100_(next);
        if (healToFull && attribute == Attributes.f_22276_) {
            entity.m_21153_(entity.m_21233_());
        }
    }

    private static long resolveNearbyDifficulty(LivingEntity entity) {
        if (!(entity.m_9236_() instanceof ServerLevel level)) {
            return 0L;
        }
        double r = Math.max(8.0, DifficultyConfig.get().mobScaleRadius);
        AABB box = entity.m_20191_().m_82400_(r);
        List<ServerPlayer> players = level.m_45976_(ServerPlayer.class, box);
        long best = 0L;
        for (ServerPlayer player : players) {
            DifficultySnapshot snap = DifficultyCache.refresh(player);
            best = Math.max(best, snap.active);
        }
        return best;
    }

    public static float outgoingDamageMultiplier(LivingEntity attacker) {
        long d = difficultyOf(attacker);
        if (d <= 0) {
            return 1.0f;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        return (float) (1.0 + d * (cfg.damagePercentPerDifficulty / 100.0)
                + d * (cfg.dmzExtraDamagePercent / 100.0));
    }
}
