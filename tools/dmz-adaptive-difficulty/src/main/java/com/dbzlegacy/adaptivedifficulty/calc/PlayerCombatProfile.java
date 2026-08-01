package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.character.Stats;
import net.minecraft.server.level.ServerPlayer;

/**
 * Post-transform / limit-release combat snapshot used to scale nearby hostiles
 * to a fraction of the player's real fighting power.
 */
public final class PlayerCombatProfile {
    public enum WeakStat {
        STRENGTH,
        STRIKE,
        DEFENSE,
        VITALITY,
        KI_POWER,
        NONE
    }

    public final int activeTier;
    public final double tierPercent;
    public final double meleeDamage;
    public final double strikeDamage;
    public final double kiDamage;
    public final double defense;
    public final double maxHealth;
    public final double offense;
    public final double releasePercent;
    public final WeakStat weakest;
    /** Stable fingerprint for mob re-scale cache invalidation. */
    public final long signature;

    private PlayerCombatProfile(
            int activeTier,
            double tierPercent,
            double meleeDamage,
            double strikeDamage,
            double kiDamage,
            double defense,
            double maxHealth,
            double offense,
            double releasePercent,
            WeakStat weakest,
            long signature
    ) {
        this.activeTier = activeTier;
        this.tierPercent = tierPercent;
        this.meleeDamage = meleeDamage;
        this.strikeDamage = strikeDamage;
        this.kiDamage = kiDamage;
        this.defense = defense;
        this.maxHealth = maxHealth;
        this.offense = offense;
        this.releasePercent = releasePercent;
        this.weakest = weakest;
        this.signature = signature;
    }

    public static PlayerCombatProfile of(ServerPlayer player) {
        if (player == null) {
            return inactive();
        }
        DifficultySnapshot snap = DifficultyCache.get(player);
        int tier = Math.max(0, snap.activeTier);
        if (tier <= 0) {
            return inactive();
        }
        double pct = DifficultyConfig.get().tierPlayerStatPercent(tier);
        StatsData data = DmzProgression.stats(player);
        double melee = 1.0;
        double strike = 1.0;
        double ki = 1.0;
        double def = 1.0;
        double hp = 20.0;
        double release = 100.0;
        if (data != null) {
            try {
                melee = Math.max(1.0, data.getMeleeDamage());
                strike = Math.max(1.0, data.getStrikeDamage());
                ki = Math.max(1.0, data.getKiDamage());
                def = Math.max(1.0, data.getDefense());
                hp = Math.max(20.0, data.getMaxHealth());
                Resources resources = data.getResources();
                if (resources != null) {
                    // Prefer power-release (limit release) when available.
                    double powerRelease = resources.getPowerRelease();
                    double plainRelease = resources.getRelease();
                    release = Math.max(powerRelease, plainRelease);
                    if (!(release > 0.0) || Double.isNaN(release) || Double.isInfinite(release)) {
                        release = 100.0;
                    }
                }
            } catch (Throwable ignored) {
                // Fall through with defaults.
            }
        }
        double offense = Math.max(melee, Math.max(strike, ki));
        WeakStat weakest = resolveWeakest(data, melee, strike, def, hp, ki, offense);
        long sig = fingerprint(tier, pct, melee, strike, ki, def, hp, release, weakest);
        return new PlayerCombatProfile(tier, pct, melee, strike, ki, def, hp, offense, release, weakest, sig);
    }

    private static PlayerCombatProfile inactive() {
        return new PlayerCombatProfile(0, 0.0, 1.0, 1.0, 1.0, 1.0, 20.0, 1.0, 100.0, WeakStat.NONE, 0L);
    }

    public boolean active() {
        return activeTier > 0 && tierPercent > 0.0;
    }

    public UnlockTier unlockTier() {
        return UnlockTier.byId(activeTier);
    }

    /**
     * Target mob HP at this player's current transformed / released power.
     */
    public double targetMobHealth(DifficultyConfig cfg) {
        double base = maxHealth * tierPercent;
        double counter = weakest == WeakStat.STRENGTH || weakest == WeakStat.STRIKE || weakest == WeakStat.KI_POWER
                ? Math.max(1.0, cfg.weakStatCounterMult)
                : 1.0;
        return Math.max(20.0, base * counter);
    }

    /**
     * Target mob attack — biased up hard when the player's defense is their weak point
     * so hits feel like they punch through defense.
     */
    public double targetMobDamage(DifficultyConfig cfg) {
        double base = offense * tierPercent;
        if (weakest == WeakStat.DEFENSE || weakest == WeakStat.VITALITY) {
            double counter = Math.max(1.0, cfg.weakStatCounterMult);
            // Aim above the player's defense share so tanking feels contested.
            double throughDefense = defense * tierPercent * Math.max(1.15, cfg.weakDefensePierceMult);
            base = Math.max(base * counter, throughDefense);
        }
        return Math.max(1.0, base);
    }

    /** Vanilla-ish armor contribution derived from player defense share. */
    public double targetMobArmor(DifficultyConfig cfg) {
        double share = defense * tierPercent;
        double armor = Math.log1p(Math.max(0.0, share)) * cfg.defenseToArmorFactor;
        if (weakest == WeakStat.STRENGTH || weakest == WeakStat.STRIKE) {
            armor *= Math.max(1.0, cfg.weakStatCounterMult);
        }
        if (cfg.maxArmorBonus > 0.0) {
            armor = Math.min(cfg.maxArmorBonus, armor);
        }
        return Math.max(0.0, armor);
    }

    private static WeakStat resolveWeakest(
            StatsData data,
            double melee,
            double strike,
            double defense,
            double health,
            double ki,
            double offense
    ) {
        // Prefer raw invested stats (with form multipliers via combat getters as fallback).
        double str = melee;
        double skp = strike;
        double res = defense;
        double vit = health;
        double pwr = ki;
        if (data != null) {
            try {
                Stats stats = data.getStats();
                if (stats != null) {
                    str = Math.max(1.0, stats.getStrength()) * Math.max(0.01, data.getTotalMultiplier("STR"));
                    skp = Math.max(1.0, stats.getStrikePower()) * Math.max(0.01, data.getTotalMultiplier("SKP"));
                    res = Math.max(1.0, stats.getResistance()) * Math.max(0.01, data.getTotalMultiplier("RES"));
                    vit = Math.max(1.0, stats.getVitality()) * Math.max(0.01, data.getTotalMultiplier("VIT"));
                    pwr = Math.max(1.0, stats.getKiPower()) * Math.max(0.01, data.getTotalMultiplier("PWR"));
                }
            } catch (Throwable ignored) {
            }
        }
        double peak = Math.max(str, Math.max(skp, Math.max(res, Math.max(vit, pwr))));
        if (!(peak > 0.0)) {
            return WeakStat.NONE;
        }
        // Normalize to peak so different units stay comparable.
        double nStr = str / peak;
        double nSkp = skp / peak;
        double nRes = res / peak;
        double nVit = vit / peak;
        double nPwr = pwr / peak;
        WeakStat best = WeakStat.STRENGTH;
        double lowest = nStr;
        if (nSkp < lowest) {
            lowest = nSkp;
            best = WeakStat.STRIKE;
        }
        if (nRes < lowest) {
            lowest = nRes;
            best = WeakStat.DEFENSE;
        }
        if (nVit < lowest) {
            lowest = nVit;
            best = WeakStat.VITALITY;
        }
        if (nPwr < lowest) {
            best = WeakStat.KI_POWER;
        }
        // Only counter when the gap is meaningful (not a flat build).
        if (lowest > 0.85) {
            return WeakStat.NONE;
        }
        return best;
    }

    private static long fingerprint(
            int tier,
            double pct,
            double melee,
            double strike,
            double ki,
            double def,
            double hp,
            double release,
            WeakStat weakest
    ) {
        long h = 1469598103934665603L;
        h = mix(h, tier);
        h = mix(h, Math.round(pct * 1000.0));
        h = mix(h, Math.round(melee));
        h = mix(h, Math.round(strike));
        h = mix(h, Math.round(ki));
        h = mix(h, Math.round(def));
        h = mix(h, Math.round(hp));
        h = mix(h, Math.round(release));
        h = mix(h, weakest == null ? 0 : weakest.ordinal() + 1);
        return h;
    }

    private static long mix(long h, long v) {
        h ^= v;
        h *= 1099511628211L;
        return h;
    }
}
