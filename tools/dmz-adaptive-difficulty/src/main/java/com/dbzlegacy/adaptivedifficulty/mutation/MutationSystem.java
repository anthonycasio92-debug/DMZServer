package com.dbzlegacy.adaptivedifficulty.mutation;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Concept section 13 — random mutations on scaled hostiles. */
public final class MutationSystem {
    public static final String TAG_MUTATION = "dmz_ad_mutation";

    private MutationSystem() {}

    public static MutationType get(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        return MutationType.fromString(PersistentDataAccess.get(entity).m_128461_(TAG_MUTATION));
    }

    public static void maybeMutate(LivingEntity entity, long difficulty) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableMutations || entity == null || difficulty < DifficultyTier.ENHANCED.threshold) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(entity);
        if (tag.m_128441_(TAG_MUTATION) && !tag.m_128461_(TAG_MUTATION).isEmpty()) {
            return;
        }
        double chance = cfg.mutationChancePercent / 100.0;
        if (EliteSystem.isElite(entity)) {
            chance *= 2.0;
        }
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }
        apply(entity, MutationType.randomFor(entity));
    }

    public static void apply(LivingEntity entity, MutationType type) {
        if (entity == null || type == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(entity);
        tag.m_128359_(TAG_MUTATION, type.name());

        if (type == MutationType.TITAN_CREEPER) {
            AttributeInstance health = entity.m_21051_(Attributes.f_22276_);
            if (health != null) {
                health.m_22100_(health.m_22115_() * 1.75);
                entity.m_21153_(entity.m_21233_());
            }
        }
        if (type == MutationType.BERSERKER_PIGLIN) {
            AttributeInstance speed = entity.m_21051_(Attributes.f_22279_);
            if (speed != null) {
                speed.m_22100_(speed.m_22115_() * 1.25);
            }
        }

        String prefix = EliteSystem.isElite(entity) ? "§6Elite " : "";
        entity.m_6593_(Component.m_237113_("§d" + prefix + type.displayName));
        entity.m_20340_(true);
    }

    public static void tick(LivingEntity entity) {
        MutationType type = get(entity);
        if (type != null) {
            type.tick(entity);
        }
    }
}
