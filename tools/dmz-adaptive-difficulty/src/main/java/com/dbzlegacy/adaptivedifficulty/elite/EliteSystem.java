package com.dbzlegacy.adaptivedifficulty.elite;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.util.EntityDisplayNames;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/**
 * Concept §12 — rare elite promotions on claim conversion.
 * Stat bonuses are applied by {@link com.dbzlegacy.adaptivedifficulty.scaling.MobScaling}
 * from base attrs so retargets stay consistent.
 */
public final class EliteSystem {
    public static final String TAG_ELITE = "dmz_ad_elite";

    private EliteSystem() {}

    public static boolean isElite(LivingEntity entity) {
        return entity != null && PersistentDataAccess.get(entity).m_128471_(TAG_ELITE);
    }

    /**
     * @param rollSeed claim-owner unlock seed ({@code unlockTier * 10_000}). Unlock-tier
     *                 gating is done by the caller via {@code eliteMinUnlockTier}.
     */
    public static void maybePromote(LivingEntity entity, long rollSeed) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableElites || entity == null || rollSeed <= 0L) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(entity);
        if (!PersistentDataAccess.isWritable(tag)
                || tag.m_128471_(TAG_ELITE)
                || tag.m_128471_("dmz_ad_boss")) {
            return;
        }
        // Flat config % only. Old builds added rollSeed/100000 (capped +15%), which with
        // unlockTier*10000 seeds always maxed out and made elites ~18% of claims.
        double chance = Math.max(0.0, Math.min(1.0, cfg.eliteChancePercent / 100.0));
        if (chance <= 0.0 || ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        promote(entity, rollSeed);
    }

    public static void promote(LivingEntity entity, long difficulty) {
        CompoundTag tag = PersistentDataAccess.get(entity);
        tag.m_128379_(TAG_ELITE, true);
        tag.m_128350_("dmz_ad_elite_scale", 1.35f); // hint for client/Pehkui packs

        DifficultyTier tier = DifficultyTier.of(difficulty);
        String typeName = EntityDisplayNames.of(entity);
        entity.m_6593_(Component.m_237113_("§6✦ Elite §e" + typeName + " §7(" + tier.display + ")"));
        entity.m_20340_(true); // glowing aura stand-in
    }
}
