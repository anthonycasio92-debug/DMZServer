package com.dbzlegacy.adaptivedifficulty.mutation;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
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

    /**
     * @param rollSeed claim-owner unlock seed ({@code unlockTier * 10_000}). Unlock-tier
     *                 gating is done by the caller via {@code mutationMinUnlockTier}.
     */
    public static void maybeMutate(LivingEntity entity, long rollSeed) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableMutations || entity == null || rollSeed <= 0L) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(entity);
        if (tag.m_128441_(TAG_MUTATION) && !tag.m_128461_(TAG_MUTATION).isEmpty()) {
            return;
        }
        // Flat config %; elites get a small bump (not double — that felt common in packs).
        double chance = Math.max(0.0, Math.min(1.0, cfg.mutationChancePercent / 100.0));
        if (EliteSystem.isElite(entity)) {
            chance = Math.min(1.0, chance * 1.35);
        }
        if (chance <= 0.0 || ThreadLocalRandom.current().nextDouble() >= chance) {
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
