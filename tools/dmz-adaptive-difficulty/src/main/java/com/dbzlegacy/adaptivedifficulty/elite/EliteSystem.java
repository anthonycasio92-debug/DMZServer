package com.dbzlegacy.adaptivedifficulty.elite;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Concept §12 — rare elite promotions at spawn. */
public final class EliteSystem {
    public static final String TAG_ELITE = "dmz_ad_elite";

    private EliteSystem() {}

    public static boolean isElite(LivingEntity entity) {
        return entity != null && PersistentDataAccess.get(entity).m_128471_(TAG_ELITE);
    }

    public static void maybePromote(LivingEntity entity, long difficulty) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableElites || entity == null || difficulty < DifficultyTier.ELITE.threshold) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(entity);
        if (tag.m_128471_(TAG_ELITE) || tag.m_128471_("dmz_ad_boss")) {
            return;
        }
        double chance = cfg.eliteChancePercent / 100.0;
        chance += Math.min(0.15, difficulty / 100_000.0);
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }
        promote(entity, difficulty);
    }

    public static void promote(LivingEntity entity, long difficulty) {
        CompoundTag tag = PersistentDataAccess.get(entity);
        tag.m_128379_(TAG_ELITE, true);

        DifficultyConfig cfg = DifficultyConfig.get();
        double mult = Math.min(cfg.eliteStatMultiplier, cfg.maxHealthMultiplier);
        AttributeInstance health = entity.m_21051_(Attributes.f_22276_);
        if (health != null && mult > 1.0) {
            double cap = cfg.maxScaledHealth > 0 ? Math.min(cfg.maxScaledHealth, 1024.0) : 1024.0;
            double next = Math.min(cap, health.m_22115_() * mult);
            if (next > 0 && !Double.isNaN(next) && !Double.isInfinite(next)) {
                health.m_22100_(next);
                entity.m_21153_(entity.m_21233_());
            }
        }
        AttributeInstance armor = entity.m_21051_(Attributes.f_22284_);
        if (armor != null) {
            armor.m_22100_(armor.m_22115_() + 4.0);
        }
        // "Larger size" stand-in without Pehkui: knockback resist + slower but tankier presence
        AttributeInstance knock = entity.m_21051_(Attributes.f_22278_); // KNOCKBACK_RESISTANCE
        if (knock != null) {
            knock.m_22100_(Math.min(1.0, knock.m_22115_() + 0.6));
        }
        AttributeInstance speed = entity.m_21051_(Attributes.f_22279_);
        if (speed != null) {
            speed.m_22100_(speed.m_22115_() * 0.92);
        }
        tag.m_128350_("dmz_ad_elite_scale", 1.35f); // hint for client/Pehkui packs

        long current = MobScaling.difficultyOf(entity);
        long boosted = Math.round(Math.max(current, 1L) * mult);
        tag.m_128356_(MobScaling.TAG_DIFFICULTY, Math.max(current, boosted));

        DifficultyTier tier = DifficultyTier.of(difficulty);
        String typeName = entity.m_6095_().m_20676_().getString();
        entity.m_6593_(Component.m_237113_("§6✦ Elite §e" + typeName + " §7(" + tier.display + ")"));
        entity.m_20340_(true); // glowing aura stand-in
    }
}
