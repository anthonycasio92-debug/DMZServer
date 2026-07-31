package com.dbzlegacy.adaptivedifficulty.scaling;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.ScalingCurves;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationType;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
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
 * Scale hostiles after spawn from their captured base stats, then re-scale to the
 * engaged player's difficulty on target switch / attack (before damage resolves).
 * <p>
 * Damage is applied two ways for Mohist reliability:
 * <ul>
 *   <li>{@link Attributes#ATTACK_DAMAGE} rewritten on each retarget (melee)</li>
 *   <li>{@link #scaleOutgoingHurt} via Forge {@code LivingHurtEvent} (projectiles / custom hits)</li>
 * </ul>
 */
public final class MobScaling {
    public static final String TAG_DIFFICULTY = "dmz_ad_difficulty";
    public static final String TAG_SCALED = "dmz_ad_scaled";
    public static final String TAG_DMG_MULT = "dmz_ad_dmg_mult";
    public static final String TAG_DMZ_STYLE = "dmz_ad_dmz_mob";
    /** True when ATTACK_DAMAGE was written from our scaler — melee must not be event-multiplied again. */
    public static final String TAG_ATTR_DMG_SCALED = "dmz_ad_attr_dmg";

    public static final String TAG_BASE_HEALTH = "dmz_ad_base_max_health";
    public static final String TAG_BASE_ATTACK = "dmz_ad_base_attack";
    public static final String TAG_BASE_ARMOR = "dmz_ad_base_armor";
    public static final String TAG_BASE_SPEED = "dmz_ad_base_speed";
    public static final String TAG_BASE_KNOCKBACK = "dmz_ad_base_knockback";

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

    /**
     * Re-scale a hostile to {@code player}'s active difficulty, preserving HP %.
     * No-op when already matched. Safe to call every hit / target change.
     */
    public static void retargetToPlayer(LivingEntity entity, ServerPlayer player) {
        if (entity == null || player == null || entity.m_9236_().f_46443_) {
            return;
        }
        try {
            DifficultyConfig cfg = DifficultyConfig.get();
            if (!cfg.enableMobScaling) {
                return;
            }
            // Cheap hostility gate before any NBT / kill checks.
            if (cfg.scaleHostileOnly && !HostileMobs.isHostile(entity)) {
                return;
            }
            CompoundTag tag = PersistentDataAccess.get(entity);
            if (!PersistentDataAccess.isWritable(tag)) {
                return;
            }
            long difficulty = Math.max(0L, DifficultyCache.get(player).active);
            // Hot path: already matched — exit before terminate / apply work.
            if (tag.m_128471_(TAG_SCALED)
                    && tag.m_128441_(TAG_BASE_HEALTH)
                    && tag.m_128441_(TAG_DIFFICULTY)
                    && tag.m_128454_(TAG_DIFFICULTY) == difficulty) {
                return;
            }
            // Never retarget / revive a mob that is already at 0 HP.
            if (terminateIfZeroHealth(entity)) {
                return;
            }
            // Ensure spawn init ran (bases + elite/boss/mut rolls).
            if (!tag.m_128471_(TAG_SCALED) || !tag.m_128441_(TAG_BASE_HEALTH)) {
                scaleIfNeededInternal(entity);
                tag = PersistentDataAccess.get(entity);
            }
            if (!tag.m_128441_(TAG_BASE_HEALTH)) {
                return;
            }
            if (tag.m_128441_(TAG_DIFFICULTY) && tag.m_128454_(TAG_DIFFICULTY) == difficulty) {
                return;
            }
            applyForDifficulty(entity, tag, difficulty, cfg);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] retarget scaling failed for {}: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    entity.m_6095_().toString(),
                    t.toString()
            );
        }
    }

    /**
     * If a difficulty-scaled mob is at ≤0 HP but still alive, force it to die.
     * Retarget/HP-ratio math and Mohist edge cases can otherwise leave 0-HP zombies.
     *
     * @return true if the entity was terminated (or already dead/removed)
     */
    public static boolean terminateIfZeroHealth(LivingEntity entity) {
        if (entity == null || entity.m_9236_().f_46443_) {
            return false;
        }
        // Fast reject: healthy mobs never need NBT.
        float hp = entity.m_21223_();
        if (hp > 0.0f && !Float.isNaN(hp) && !Float.isInfinite(hp)) {
            return false;
        }
        if (entity.m_213877_()) { // isRemoved
            return true;
        }
        if (entity.m_21224_()) { // isDeadOrDying
            return true;
        }
        CompoundTag tag = PersistentDataAccess.get(entity);
        if (!tag.m_128471_(TAG_SCALED)) {
            return false;
        }
        try {
            // Entity.kill() — applies a lethal generic hit and runs normal death.
            entity.m_6074_();
        } catch (Throwable ignored) {
        }
        if (!entity.m_21224_() && !entity.m_213877_()) {
            try {
                DamageSource src = entity.m_269291_().m_269425_(); // generic
                entity.m_21153_(0.0f);
                entity.m_6667_(src); // die
            } catch (Throwable ignored) {
            }
        }
        if (!entity.m_21224_() && !entity.m_213877_()) {
            try {
                entity.m_146870_(); // discard — last resort
            } catch (Throwable ignored) {
            }
        }
        return true;
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
        // Already initialized — combat retarget uses {@link #retargetToPlayer}.
        if (tag.m_128471_(TAG_SCALED) && tag.m_128441_(TAG_BASE_HEALTH)) {
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

        // Capture natural / current stats before any of our multipliers.
        // Legacy mobs already scaled without bases: reverse the old difficulty mult.
        if (!tag.m_128441_(TAG_BASE_HEALTH)) {
            captureBases(entity, tag, cfg);
        }

        // Detect bosses from captured base HP (not post-scale).
        boolean naturalBoss = BossScaling.isNaturalBoss(entity)
                || (tag.m_128441_(TAG_BASE_HEALTH)
                && tag.m_128459_(TAG_BASE_HEALTH) >= cfg.bossHealthThreshold);

        boolean dmzStyle = shouldApplyDmzStyleExtras(entity, cfg);
        if (dmzStyle) {
            tag.m_128379_(TAG_DMZ_STYLE, true);
        }
        tag.m_128379_(TAG_SCALED, true);

        long areaDifficulty = resolveNearbyDifficulty(entity);

        // Roll elite / boss / mutation once (flags + cosmetics only — stats via applyForDifficulty).
        if (naturalBoss) {
            BossScaling.markBoss(entity, areaDifficulty);
        }
        EliteSystem.maybePromote(entity, areaDifficulty);
        MutationSystem.maybeMutate(entity, areaDifficulty);

        applyForDifficulty(entity, tag, areaDifficulty, cfg);
    }

    /**
     * Rewrite health / damage / armor / speed from stored bases for {@code difficulty}.
     * Preserves current HP as a fraction of max so mid-fight retargets stay fair.
     */
    public static void applyForDifficulty(
            LivingEntity entity, CompoundTag tag, long difficulty, DifficultyConfig cfg
    ) {
        if (entity == null || tag == null || !PersistentDataAccess.isWritable(tag)) {
            return;
        }
        if (!tag.m_128441_(TAG_BASE_HEALTH)) {
            return;
        }

        boolean dmzStyle = tag.m_128471_(TAG_DMZ_STYLE) || shouldApplyDmzStyleExtras(entity, cfg);
        boolean elite = tag.m_128471_(EliteSystem.TAG_ELITE);
        boolean boss = tag.m_128471_(BossScaling.TAG_BOSS);
        MutationType mutation = MutationType.fromString(tag.m_128461_(MutationSystem.TAG_MUTATION));

        double healthMult = 1.0;
        double armorBonus = 0.0;
        double moveMult = 1.0;
        double dmgMult = 1.0;

        if (difficulty > 0L) {
            healthMult = 1.0 + ScalingCurves.healthBonus(difficulty, cfg.healthPercentPerDifficulty);
            armorBonus = ScalingCurves.offenseBonus(difficulty, cfg.defensePercentPerDifficulty);
            moveMult = 1.0 + ((difficulty / 100.0) * (cfg.movementPercentPer100Difficulty / 100.0));
            dmgMult = 1.0 + ScalingCurves.offenseBonus(difficulty, cfg.damagePercentPerDifficulty);
            if (dmzStyle) {
                healthMult += ScalingCurves.healthBonus(difficulty, cfg.dmzExtraHealthPercent);
                armorBonus += ScalingCurves.offenseBonus(difficulty, cfg.dmzExtraDefensePercent);
                dmgMult += ScalingCurves.offenseBonus(difficulty, cfg.dmzExtraDamagePercent);
                dmgMult += ScalingCurves.offenseBonus(difficulty, cfg.dmzExtraKiDamagePercent);
            }
        }

        // Optional ceilings (0 / 1 = uncapped mult).
        if (cfg.maxHealthMultiplier > 1.0) {
            healthMult = Math.min(healthMult, cfg.maxHealthMultiplier);
        }
        if (cfg.maxMoveMultiplier > 1.0) {
            moveMult = clamp(moveMult, 1.0, cfg.maxMoveMultiplier);
        } else {
            moveMult = Math.max(1.0, moveMult);
        }
        if (cfg.maxArmorBonus > 0.0) {
            armorBonus = Math.min(armorBonus, cfg.maxArmorBonus);
        }
        if (cfg.maxDamageMultiplier > 1.0) {
            dmgMult = Math.min(dmgMult, cfg.maxDamageMultiplier);
        }

        // Permanent rarity multipliers — baked into HP/damage (not separate reward bonuses).
        double rarityHealth = 1.0;
        double rarityDamage = 1.0;
        if (elite) {
            rarityHealth *= Math.max(1.0, cfg.eliteStatMultiplier);
            rarityDamage *= Math.max(1.0, cfg.eliteStatMultiplier);
            armorBonus += 4.0;
            moveMult *= 0.92;
        }
        if (boss) {
            rarityHealth *= Math.max(1.0, cfg.bossStatMultiplier);
            rarityDamage *= Math.max(1.0, cfg.bossStatMultiplier);
            if (difficulty > 0L) {
                double bossArmor = ScalingCurves.offenseBonus(difficulty, cfg.defensePercentPerDifficulty) * 0.25;
                if (cfg.maxArmorBonus > 0.0) {
                    bossArmor = Math.min(cfg.maxArmorBonus, bossArmor);
                }
                armorBonus += bossArmor;
            }
        }
        if (mutation == MutationType.TITAN_CREEPER) {
            rarityHealth *= 1.75;
        }
        if (mutation == MutationType.BERSERKER_PIGLIN) {
            moveMult *= 1.25;
        }

        double baseHealth = Math.max(1.0e-3, tag.m_128459_(TAG_BASE_HEALTH));
        double baseAttack = tag.m_128441_(TAG_BASE_ATTACK) ? Math.max(0.0, tag.m_128459_(TAG_BASE_ATTACK)) : 0.0;
        double baseArmor = tag.m_128441_(TAG_BASE_ARMOR) ? Math.max(0.0, tag.m_128459_(TAG_BASE_ARMOR)) : 0.0;
        double baseSpeed = tag.m_128441_(TAG_BASE_SPEED) ? Math.max(0.0, tag.m_128459_(TAG_BASE_SPEED)) : 0.0;
        double baseKnock = tag.m_128441_(TAG_BASE_KNOCKBACK) ? Math.max(0.0, tag.m_128459_(TAG_BASE_KNOCKBACK)) : 0.0;

        double absHealthCap = cfg.maxScaledHealth > 0.0
                ? Math.min(cfg.maxScaledHealth, VANILLA_MAX_HEALTH_CAP)
                : VANILLA_MAX_HEALTH_CAP;
        double newMaxHealth = Math.min(absHealthCap, baseHealth * healthMult * rarityHealth);
        if (!(newMaxHealth > 0.0) || Double.isNaN(newMaxHealth) || Double.isInfinite(newMaxHealth)) {
            newMaxHealth = Math.min(absHealthCap, baseHealth);
        }

        // Preserve fight progress across retargets. Never revive a 0-HP mob.
        float oldMax = entity.m_21233_();
        float oldHp = entity.m_21223_();
        if (entity.m_21224_() || !(oldHp > 0.0f) || Float.isNaN(oldHp) || Float.isInfinite(oldHp)) {
            terminateIfZeroHealth(entity);
            return;
        }
        double hpRatio = (oldMax > 0.0f && !Float.isNaN(oldMax))
                ? Math.max(0.0, Math.min(1.0, oldHp / oldMax))
                : 1.0;
        if (hpRatio <= 0.0) {
            terminateIfZeroHealth(entity);
            return;
        }

        setAttributeValue(entity, Attributes.f_22276_, newMaxHealth); // MAX_HEALTH
        float appliedMax = entity.m_21233_();
        if (appliedMax > 0.0f && !Float.isNaN(appliedMax) && !Float.isInfinite(appliedMax)) {
            float nextHp = (float) (appliedMax * hpRatio);
            // Keep at full when first applying from a full-health spawn.
            if (!tag.m_128441_(TAG_DIFFICULTY) && oldHp >= oldMax - 0.5f) {
                nextHp = appliedMax;
            }
            // Floor only when still meaningfully alive — never keep a corpse at 0.001 HP.
            if (nextHp <= 0.0f) {
                terminateIfZeroHealth(entity);
                return;
            }
            entity.m_21153_(Math.min(appliedMax, nextHp));
        }

        if (baseAttack > 0.0) {
            double nextAtk = baseAttack * Math.max(1.0, dmgMult) * rarityDamage;
            if (nextAtk > 0.0 && !Double.isNaN(nextAtk) && !Double.isInfinite(nextAtk)) {
                if (setAttributeValue(entity, Attributes.f_22281_, nextAtk)) { // ATTACK_DAMAGE
                    tag.m_128379_(TAG_ATTR_DMG_SCALED, true);
                }
            }
        }

        double nextArmor = baseArmor + Math.max(0.0, armorBonus);
        if (cfg.maxArmorBonus > 0.0) {
            nextArmor = Math.min(cfg.maxArmorBonus, nextArmor);
        }
        if (nextArmor >= 0.0 && !Double.isNaN(nextArmor) && !Double.isInfinite(nextArmor)) {
            setAttributeValue(entity, Attributes.f_22284_, nextArmor); // ARMOR
        }

        if (baseSpeed > 0.0) {
            double nextSpeed = baseSpeed * Math.max(0.05, moveMult);
            if (nextSpeed > 0.0 && !Double.isNaN(nextSpeed) && !Double.isInfinite(nextSpeed)) {
                setAttributeValue(entity, Attributes.f_22279_, nextSpeed); // MOVEMENT_SPEED
            }
        }

        if (elite) {
            double nextKnock = Math.min(1.0, baseKnock + 0.6);
            setAttributeValue(entity, Attributes.f_22278_, nextKnock); // KNOCKBACK_RESISTANCE
        } else if (tag.m_128441_(TAG_BASE_KNOCKBACK)) {
            setAttributeValue(entity, Attributes.f_22278_, baseKnock);
        }

        tag.m_128356_(TAG_DIFFICULTY, Math.max(0L, difficulty));
        tag.m_128350_(TAG_DMG_MULT, (float) Math.max(1.0, dmgMult * rarityDamage));
    }

    private static void captureBases(LivingEntity entity, CompoundTag tag, DifficultyConfig cfg) {
        double health = attrBase(entity, Attributes.f_22276_, 20.0);
        double attack = attrBase(entity, Attributes.f_22281_, 0.0);
        double armor = attrBase(entity, Attributes.f_22284_, 0.0);
        double speed = attrBase(entity, Attributes.f_22279_, 0.0);
        double knock = attrBase(entity, Attributes.f_22278_, 0.0);

        // Best-effort reverse of a prior one-shot scale (pre-retarget builds).
        if (tag.m_128471_(TAG_SCALED) && tag.m_128441_(TAG_DIFFICULTY)) {
            long oldD = tag.m_128454_(TAG_DIFFICULTY);
            if (oldD > 0L) {
                boolean dmzStyle = tag.m_128471_(TAG_DMZ_STYLE) || shouldApplyDmzStyleExtras(entity, cfg);
                double healthMult = 1.0 + ScalingCurves.healthBonus(oldD, cfg.healthPercentPerDifficulty);
                double dmgMult = 1.0 + ScalingCurves.offenseBonus(oldD, cfg.damagePercentPerDifficulty);
                double moveMult = 1.0 + ((oldD / 100.0) * (cfg.movementPercentPer100Difficulty / 100.0));
                if (dmzStyle) {
                    healthMult += ScalingCurves.healthBonus(oldD, cfg.dmzExtraHealthPercent);
                    dmgMult += ScalingCurves.offenseBonus(oldD, cfg.dmzExtraDamagePercent);
                    dmgMult += ScalingCurves.offenseBonus(oldD, cfg.dmzExtraKiDamagePercent);
                }
                if (cfg.maxHealthMultiplier > 1.0) {
                    healthMult = Math.min(healthMult, cfg.maxHealthMultiplier);
                }
                if (tag.m_128471_(EliteSystem.TAG_ELITE)) {
                    healthMult *= Math.max(1.0, cfg.eliteStatMultiplier);
                    dmgMult *= Math.max(1.0, cfg.eliteStatMultiplier);
                    moveMult *= 0.92;
                }
                if (tag.m_128471_(BossScaling.TAG_BOSS)) {
                    healthMult *= Math.max(1.0, cfg.bossStatMultiplier);
                    dmgMult *= Math.max(1.0, cfg.bossStatMultiplier);
                }
                if (MutationType.TITAN_CREEPER == MutationType.fromString(tag.m_128461_(MutationSystem.TAG_MUTATION))) {
                    healthMult *= 1.75;
                }
                if (MutationType.BERSERKER_PIGLIN == MutationType.fromString(tag.m_128461_(MutationSystem.TAG_MUTATION))) {
                    moveMult *= 1.25;
                }
                if (healthMult > 1.0e-6) {
                    health /= healthMult;
                }
                if (dmgMult > 1.0e-6 && attack > 0.0) {
                    attack /= dmgMult;
                }
                if (moveMult > 1.0e-6 && speed > 0.0) {
                    speed /= moveMult;
                }
            }
        }

        tag.m_128347_(TAG_BASE_HEALTH, Math.max(1.0, health));
        if (attack > 0.0) {
            tag.m_128347_(TAG_BASE_ATTACK, attack);
        }
        tag.m_128347_(TAG_BASE_ARMOR, Math.max(0.0, armor));
        if (speed > 0.0) {
            tag.m_128347_(TAG_BASE_SPEED, speed);
        }
        tag.m_128347_(TAG_BASE_KNOCKBACK, Math.max(0.0, knock));
    }

    private static double attrBase(LivingEntity entity, Attribute attribute, double fallback) {
        AttributeInstance instance = entity.m_21051_(attribute);
        if (instance == null) {
            return fallback;
        }
        double v = instance.m_22115_();
        if (!(v >= 0.0) || Double.isNaN(v) || Double.isInfinite(v)) {
            return fallback;
        }
        return v;
    }

    private static boolean setAttributeValue(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.m_21051_(attribute);
        if (instance == null) {
            return false;
        }
        if (!(value >= 0.0) || Double.isNaN(value) || Double.isInfinite(value)) {
            return false;
        }
        instance.m_22100_(value);
        return true;
    }

    /** DMZ-style extras apply to every hostile by default. */
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
        if (tag.m_128471_(EliteSystem.TAG_ELITE)) {
            mult *= Math.max(1.0, cfg.eliteStatMultiplier);
        }
        if (tag.m_128471_(BossScaling.TAG_BOSS)) {
            mult *= Math.max(1.0, cfg.bossStatMultiplier);
        }
        if (cfg.maxDamageMultiplier > 1.0) {
            mult = Math.min(mult, cfg.maxDamageMultiplier);
        }
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128350_(TAG_DMG_MULT, (float) mult);
        }
        return (float) mult;
    }

    /**
     * Scale hurt amount from a hostile attacker.
     * Skips melee when {@link #TAG_ATTR_DMG_SCALED} is set (already on ATTACK_DAMAGE).
     * Still scales projectiles / indirect damage.
     */
    public static float scaleOutgoingHurt(float amount, DamageSource source) {
        if (amount <= 0.0f || source == null) {
            return amount;
        }
        Entity causing = source.m_7639_(); // getEntity
        if (!(causing instanceof LivingEntity attacker) || attacker instanceof Player) {
            return amount;
        }
        // Creeper / mob explosions: soft scale (full offense mult skips or melts these).
        if (source.m_269533_(DamageTypeTags.f_268415_)) { // IS_EXPLOSION
            long d = difficultyOf(attacker);
            if (d <= 0L) {
                return amount;
            }
            double boom = 1.0 + ScalingCurves.offenseEffective(d) * 0.003;
            boom = Math.min(500.0, Math.max(1.0, boom));
            return amount * (float) boom;
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
