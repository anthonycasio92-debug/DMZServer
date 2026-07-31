package com.dbzlegacy.adaptivedifficulty.scaling;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.calc.ScalingCurves;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Apply scaling once at spawn and cache difficulty on the mob (concept §9 / §17).
 * All hostiles receive the same DMZ-style extras (health / defense / damage / ki).
 * <p>
 * Damage is applied two ways for Mohist reliability:
 * <ul>
 *   <li>{@link Attributes#ATTACK_DAMAGE} multiplied at spawn (melee)</li>
 *   <li>{@link #scaleOutgoingHurt} via Forge {@code LivingHurtEvent} (projectiles / custom hits)</li>
 * </ul>
 */
public final class MobScaling {
    public static final String TAG_DIFFICULTY = "dmz_ad_difficulty";
    public static final String TAG_SCALED = "dmz_ad_scaled";
    public static final String TAG_DMG_MULT = "dmz_ad_dmg_mult";
    public static final String TAG_DMZ_STYLE = "dmz_ad_dmz_mob";
    /** True when ATTACK_DAMAGE was multiplied at spawn — melee must not be event-multiplied again. */
    public static final String TAG_ATTR_DMG_SCALED = "dmz_ad_attr_dmg";

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
        if (!PersistentDataAccess.isWritable(tag)) {
            return;
        }
        if (tag.m_128471_(TAG_SCALED)) {
            return;
        }

        // Skip non-hostiles when scaleHostileOnly (animals, villagers, etc.).
        if (cfg.scaleHostileOnly) {
            if (!(entity instanceof Mob mob) || !HostileMobs.isHostile(mob)) {
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

        boolean dmzStyle = shouldApplyDmzStyleExtras(entity, cfg);
        if (dmzStyle) {
            tag.m_128379_(TAG_DMZ_STYLE, true);
        }

        // Health uses a flat curve + hard caps; damage/defense use a steeper offense curve.
        double healthMult = 1.0 + ScalingCurves.healthBonus(difficulty, cfg.healthPercentPerDifficulty);
        double armorBonus = ScalingCurves.offenseBonus(difficulty, cfg.defensePercentPerDifficulty);
        double moveMult = 1.0 + ((difficulty / 100.0) * (cfg.movementPercentPer100Difficulty / 100.0));

        if (dmzStyle) {
            healthMult += ScalingCurves.healthBonus(difficulty, cfg.dmzExtraHealthPercent);
            armorBonus += ScalingCurves.offenseBonus(difficulty, cfg.dmzExtraDefensePercent);
        }

        healthMult = clamp(healthMult, 1.0, Math.max(1.0, cfg.maxHealthMultiplier));
        moveMult = clamp(moveMult, 1.0, Math.max(1.0, cfg.maxMoveMultiplier));
        armorBonus = Math.min(armorBonus, Math.max(0.0, cfg.maxArmorBonus));

        double dmgMult = 1.0 + ScalingCurves.offenseBonus(difficulty, cfg.damagePercentPerDifficulty);
        if (dmzStyle) {
            dmgMult += ScalingCurves.offenseBonus(difficulty, cfg.dmzExtraDamagePercent);
            dmgMult += ScalingCurves.offenseBonus(difficulty, cfg.dmzExtraKiDamagePercent);
        }
        dmgMult = Math.min(dmgMult, Math.max(1.0, cfg.maxDamageMultiplier));
        tag.m_128350_(TAG_DMG_MULT, (float) dmgMult); // putFloat

        scaleMaxHealth(entity, healthMult, cfg.maxScaledHealth);
        scaleAttribute(entity, Attributes.f_22279_, moveMult); // MOVEMENT_SPEED
        // Melee damage attribute — primary path (mixin alone was unreliable on Mohist).
        if (scaleAttribute(entity, Attributes.f_22281_, dmgMult)) { // ATTACK_DAMAGE
            tag.m_128379_(TAG_ATTR_DMG_SCALED, true);
        }
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

    /**
     * DMZ-style extras (extra HP/DEF/DMG/ki) apply to every hostile by default,
     * matching how DragonMineZ mobs were scaled.
     */
    private static boolean shouldApplyDmzStyleExtras(LivingEntity entity, DifficultyConfig cfg) {
        if (isDragonMineZMob(entity)) {
            return true;
        }
        if (!cfg.applyDmzExtrasToAllHostiles) {
            return false;
        }
        return HostileMobs.isHostile(entity);
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

    /** @return true if the attribute existed and was multiplied */
    private static boolean scaleAttribute(LivingEntity entity, Attribute attribute, double multiplier) {
        AttributeInstance instance = entity.m_21051_(attribute);
        if (instance == null || multiplier <= 1.0) {
            return false;
        }
        double base = instance.m_22115_();
        if (!(base > 0.0) || Double.isNaN(base) || Double.isInfinite(base)) {
            return false;
        }
        double next = base * multiplier;
        if (!(next > 0.0) || Double.isNaN(next) || Double.isInfinite(next)) {
            return false;
        }
        instance.m_22100_(next);
        return true;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long resolveNearbyDifficulty(LivingEntity entity) {
        if (!(entity.m_9236_() instanceof ServerLevel level)) {
            return 0L;
        }
        return AreaDifficulty.at(level, entity.m_20183_());
    }

    public static float outgoingDamageMultiplier(LivingEntity attacker) {
        if (attacker == null) {
            return 1.0f;
        }
        CompoundTag tag = PersistentDataAccess.get(attacker);
        if (tag.m_128441_(TAG_DMG_MULT)) {
            float cached = tag.m_128457_(TAG_DMG_MULT); // getFloat
            return cached > 0.0f ? cached : 1.0f;
        }
        long d = tag.m_128441_(TAG_DIFFICULTY) ? tag.m_128454_(TAG_DIFFICULTY) : 0L;
        if (d <= 0) {
            return 1.0f;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        double mult = 1.0 + ScalingCurves.offenseBonus(d, cfg.damagePercentPerDifficulty);
        boolean dmzStyle = tag.m_128471_(TAG_DMZ_STYLE)
                || isDragonMineZMob(attacker)
                || (cfg.applyDmzExtrasToAllHostiles && HostileMobs.isHostile(attacker));
        if (dmzStyle) {
            mult += ScalingCurves.offenseBonus(d, cfg.dmzExtraDamagePercent);
            mult += ScalingCurves.offenseBonus(d, cfg.dmzExtraKiDamagePercent);
        }
        mult = Math.min(mult, Math.max(1.0, cfg.maxDamageMultiplier));
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128350_(TAG_DMG_MULT, (float) mult);
        }
        return (float) mult;
    }

    /**
     * Scale hurt amount from a hostile attacker.
     * Skips melee when {@link #TAG_ATTR_DMG_SCALED} is set (already on ATTACK_DAMAGE).
     * Still scales projectiles / indirect damage.
     *
     * @return scaled amount (unchanged if no boost applies)
     */
    public static float scaleOutgoingHurt(float amount, DamageSource source) {
        if (amount <= 0.0f || source == null) {
            return amount;
        }
        Entity causing = source.m_7639_(); // getEntity
        if (!(causing instanceof LivingEntity attacker) || attacker instanceof Player) {
            return amount;
        }
        float mult = outgoingDamageMultiplier(attacker);
        if (mult <= 1.0f) {
            return amount;
        }
        CompoundTag tag = PersistentDataAccess.get(attacker);
        boolean attrScaled = tag.m_128471_(TAG_ATTR_DMG_SCALED);
        Entity direct = source.m_7640_(); // getDirectEntity
        boolean indirect = direct != null && direct != attacker;
        // Melee already boosted via ATTACK_DAMAGE — don't multiply again.
        if (attrScaled && !indirect) {
            return amount;
        }
        return amount * mult;
    }
}
