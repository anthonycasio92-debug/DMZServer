package com.dbzlegacy.adaptivedifficulty.progression.combat;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.util.ApothicAttributes;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.skills.Skills;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Port of {@code KiWeapons.js} — Apothic ranks boost ki projectile / ki-weapon / strike
 * damage and award EXPERIENCE_GAINED-based TP.
 */
public final class KiWeapons {
    private static final double MAX_TP_BONUS = 100.0;
    private static final double MAX_SHRED = 1.0;
    private static final long DAMAGE_DUP_MS = 75L;
    private static final long TP_DUP_MS = 350L;
    private static final Map<String, Long> LAST_DMG = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_TP = new ConcurrentHashMap<>();

    private KiWeapons() {}

    public static void onPlayerDealHurt(LivingHurtEvent event, ServerPlayer attacker, LivingEntity target) {
        if (!DifficultyConfig.get().enableKiWeapons || attacker == null || target == null) {
            return;
        }
        float amount = event.getAmount();
        if (!(amount > 0.0f)) {
            return;
        }
        DamageSource source = event.getSource();
        boolean kiProjectile = DmzRewards.isKiDamage(source);
        boolean melee = isPlayerMelee(source);
        if (!kiProjectile && !melee) {
            return;
        }

        double fireRank = Math.max(0.0, ApothicAttributes.fireDamage(attacker));
        double coldRank = Math.max(0.0, ApothicAttributes.coldDamage(attacker));
        double strikeRank = Math.max(0.0, ApothicAttributes.protPierce(attacker));
        double armorPierce = Math.max(0.0, ApothicAttributes.armorPierce(attacker));
        double armorShred = Math.min(MAX_SHRED, Math.max(0.0, ApothicAttributes.armorShred(attacker)));
        double tpRaw = ApothicAttributes.experienceGained(attacker);
        if (!(tpRaw >= 1.0)) {
            tpRaw = 1.0;
        }
        double tpBonus = Math.min(MAX_TP_BONUS, Math.max(0.0, tpRaw - 1.0));

        StatsData data = DmzProgression.stats(attacker);
        boolean kiWeapon = false;
        boolean strike = false;
        double meleeDamage = 0.0;
        double strikeDamage = 0.0;
        if (data != null) {
            try {
                meleeDamage = Math.max(0.0, data.getMeleeDamage());
            } catch (Throwable ignored) {
            }
            try {
                strikeDamage = Math.max(0.0, data.getStrikeDamage());
            } catch (Throwable ignored) {
            }
            if (melee) {
                kiWeapon = isKiWeaponActive(data);
                if (!kiWeapon) {
                    strike = looksLikeStrike(attacker, amount, meleeDamage, strikeDamage);
                }
            }
        }

        double targetDefense = 0.0;
        StatsData targetData = DmzProgression.stats(target instanceof ServerPlayer sp ? sp : null);
        if (targetData == null && target != null) {
            try {
                targetData = com.dragonminez.common.stats.StatsProvider
                        .get(com.dragonminez.common.stats.StatsCapability.INSTANCE, target)
                        .orElse(null);
            } catch (Throwable ignored) {
            }
        }
        if (targetData != null) {
            try {
                targetDefense = Math.max(0.0, targetData.getDefense());
            } catch (Throwable ignored) {
            }
        }

        double pierceBonus = Math.min(armorPierce, targetDefense);
        double shredBonus = Math.min(targetDefense * armorShred, targetDefense);
        double resistBonus = Math.min(pierceBonus + shredBonus, targetDefense);

        double extra = resistBonus;
        if (kiProjectile) {
            extra += amount * (fireRank * 0.10);
        } else if (kiWeapon) {
            extra += amount * (coldRank * 0.10);
        } else if (strike) {
            extra += amount * (strikeRank * 0.10);
        }

        String srcKey = sourceKey(source);
        String dmgKey = attacker.m_20148_() + "|" + target.m_20148_() + "|" + srcKey;
        long now = System.currentTimeMillis();
        if (extra > 0.0) {
            Long last = LAST_DMG.get(dmgKey);
            if (last == null || now - last > DAMAGE_DUP_MS) {
                LAST_DMG.put(dmgKey, now);
                event.setAmount(amount + (float) extra);
                SystemTelemetry.log("kiweapons", "extra_damage", attacker, null,
                        Map.of("extra", Math.round(extra), "src", srcKey));
            }
        }

        if (tpBonus > 0.0 && data != null) {
            String tpKey = "dealt|" + dmgKey;
            Long lastTp = LAST_TP.get(tpKey);
            if (lastTp == null || now - lastTp > TP_DUP_MS) {
                LAST_TP.put(tpKey, now);
                double maxHealth = Math.max(1.0, target.m_21233_());
                double baseTp = Math.min(amount, Math.max(1.0, maxHealth * 0.2));
                float bonusTp = (float) Math.floor(baseTp * tpBonus);
                if (bonusTp > 0.0f) {
                    DmzRewards.awardTp(attacker, bonusTp, "ki-weapon", false, "§6[Ki] ");
                }
            }
        }
    }

    public static void onPlayerTakeHurt(LivingHurtEvent event, ServerPlayer victim) {
        if (!DifficultyConfig.get().enableKiWeapons || victim == null) {
            return;
        }
        float amount = event.getAmount();
        if (!(amount > 0.0f)) {
            return;
        }
        double tpRaw = ApothicAttributes.experienceGained(victim);
        if (!(tpRaw >= 1.0)) {
            return;
        }
        double tpBonus = Math.min(MAX_TP_BONUS, Math.max(0.0, tpRaw - 1.0));
        if (!(tpBonus > 0.0)) {
            return;
        }
        String srcKey = sourceKey(event.getSource());
        String tpKey = "taken|" + victim.m_20148_() + "|" + srcKey;
        long now = System.currentTimeMillis();
        Long last = LAST_TP.get(tpKey);
        if (last != null && now - last <= TP_DUP_MS) {
            return;
        }
        LAST_TP.put(tpKey, now);
        float bonusTp = (float) Math.floor(amount * tpBonus);
        if (bonusTp > 0.0f) {
            DmzRewards.awardTp(victim, bonusTp, "taken", false, "§6[Ki] ");
        }
    }

    public static boolean isKiWeaponActive(StatsData data) {
        if (data == null) {
            return false;
        }
        try {
            Skills skills = data.getSkills();
            if (skills == null || !skills.isSkillActive("kimanipulation")) {
                return false;
            }
            Status status = data.getStatus();
            if (status == null) {
                return false;
            }
            String type = status.getKiWeaponType();
            if (type == null) {
                return false;
            }
            String t = type.toLowerCase(Locale.ROOT);
            return "blade".equals(t) || "scythe".equals(t) || "clawlance".equals(t);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean looksLikeStrike(
            ServerPlayer attacker, float amount, double meleeDamage, double strikeDamage
    ) {
        if (strikeDamage <= 0.0) {
            return false;
        }
        double strikeFloor = strikeDamage * 0.60;
        double meleeCeiling = meleeDamage * 1.20;
        return amount >= strikeFloor && amount > meleeCeiling;
    }

    private static boolean isPlayerMelee(DamageSource source) {
        if (source == null) {
            return false;
        }
        try {
            String key = source.m_19385_(); // getMsgId
            if (key == null) {
                return false;
            }
            String k = key.toLowerCase(Locale.ROOT);
            return "player".equals(k) || "minecraft:player".equals(k);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String sourceKey(DamageSource source) {
        if (source == null) {
            return "unknown";
        }
        try {
            String key = source.m_19385_();
            return key == null ? "unknown" : key.toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    public static void clearPlayer(UUID uuid) {
        if (uuid == null) {
            return;
        }
        String prefix = uuid.toString();
        LAST_DMG.keySet().removeIf(k -> k.startsWith(prefix));
        LAST_TP.keySet().removeIf(k -> k.contains(prefix));
    }
}
