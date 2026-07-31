package com.dbzlegacy.adaptivedifficulty.mutation;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.util.EntityDisplayNames;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

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
        if (!cfg.enableMutations || entity == null || difficulty < DifficultyTier.ENHANCED.threshold()) {
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

    /**
     * Flag + cosmetics only. Titan / berserker stat bonuses are applied by
     * {@link com.dbzlegacy.adaptivedifficulty.scaling.MobScaling} on each retarget.
     */
    public static void apply(LivingEntity entity, MutationType type) {
        if (entity == null || type == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(entity);
        tag.m_128359_(TAG_MUTATION, type.name());

        String prefix = EliteSystem.isElite(entity) ? "§6Elite " : "";
        // Adjective + live type so names always match the mob (e.g. "Burning Husk").
        entity.m_6593_(Component.m_237113_("§d" + prefix + type.shortName + " " + EntityDisplayNames.of(entity)));
        entity.m_20340_(true);
    }

    public static void tick(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        tick(entity, PersistentDataAccess.get(entity));
    }

    /** Prefer this when the caller already loaded the entity tag. */
    public static void tick(LivingEntity entity, CompoundTag tag) {
        if (entity == null || tag == null || !PersistentDataAccess.isWritable(tag)) {
            return;
        }
        if (!tag.m_128441_(TAG_MUTATION)) {
            return;
        }
        MutationType type = MutationType.fromString(tag.m_128461_(TAG_MUTATION));
        if (type != null) {
            type.tick(entity);
        }
    }
}
