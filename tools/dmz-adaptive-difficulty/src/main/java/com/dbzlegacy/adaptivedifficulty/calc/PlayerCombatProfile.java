package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.character.Stats;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Post-transform / limit-release combat snapshot used to scale nearby hostiles
 * to a fraction of the player's real fighting power.
 * <p>
 * Counters cover:
 * <ul>
 *   <li>weak-stat dumps (STR/SKP/RES/VIT/PWR imbalance)</li>
 *   <li>DEF/VIT tank floors + specialization tax</li>
 *   <li>DMZ fighting class / race overlays (warrior, spiritualist, tank, …)</li>
 * </ul>
 */
public final class PlayerCombatProfile {
    private static final long CACHE_TTL_MS = 250L;
    private static final Map<UUID, Cached> CACHE = new ConcurrentHashMap<>();

    public enum WeakStat {
        STRENGTH,
        STRIKE,
        DEFENSE,
        VITALITY,
        KI_POWER,
        NONE
    }

    /** High-level fighting style derived from DMZ class (or offense peak fallback). */
    public enum FightingStyle {
        MELEE,
        STRIKE,
        KI,
        TANK,
        HYBRID
    }

    public final int activeTier;
    public final double tierPercent;
    public final double meleeDamage;
    public final double strikeDamage;
    public final double kiDamage;
    public final double defense;
    public final double maxHealth;
    /** Blended offensive threat (peak + average), not peak-only. */
    public final double offense;
    public final double releasePercent;
    public final WeakStat weakest;
    /**
     * 0 = even build, approaches 1 as the weakest combat stat is dumped
     * relative to the player's peak invested stat.
     */
    public final double imbalance;
    public final String fightingClass;
    public final String race;
    public final FightingStyle style;
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
            double imbalance,
            String fightingClass,
            String race,
            FightingStyle style,
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
        this.imbalance = Math.max(0.0, Math.min(1.0, imbalance));
        this.fightingClass = fightingClass == null ? "" : fightingClass;
        this.race = race == null ? "" : race;
        this.style = style == null ? FightingStyle.HYBRID : style;
        this.signature = signature;
    }

    public static PlayerCombatProfile of(ServerPlayer player) {
        if (player == null) {
            return inactive();
        }
        UUID id = player.m_20148_();
        long now = System.currentTimeMillis();
        Cached cached = CACHE.get(id);
        if (cached != null && cached.expiresAtMs > now) {
            return cached.profile;
        }
        PlayerCombatProfile profile = build(player);
        CACHE.put(id, new Cached(profile, now + CACHE_TTL_MS));
        if (CACHE.size() > 512) {
            pruneCache(now);
        }
        return profile;
    }

    public static void clear(UUID playerId) {
        if (playerId != null) {
            CACHE.remove(playerId);
        }
    }

    public static void clearAll() {
        CACHE.clear();
    }

    private static PlayerCombatProfile build(ServerPlayer player) {
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
        // Peak-only let pure tanks face wet-noodle hits; blend in the average.
        double peakOffense = Math.max(melee, Math.max(strike, ki));
        double avgOffense = (melee + strike + ki) / 3.0;
        double offense = peakOffense * 0.55 + avgOffense * 0.45;
        StatBalance balance = resolveBalance(data, melee, strike, def, hp, ki);
        String fightingClass = DmzProgression.fightingClass(player);
        String race = DmzProgression.race(player);
        FightingStyle style = resolveStyle(fightingClass, melee, strike, ki, def, hp);
        long sig = fingerprint(
                tier, pct, melee, strike, ki, def, hp, release,
                balance.weakest, balance.imbalance, fightingClass, race, style
        );
        return new PlayerCombatProfile(
                tier, pct, melee, strike, ki, def, hp, offense, release,
                balance.weakest, balance.imbalance, fightingClass, race, style, sig
        );
    }

    private static PlayerCombatProfile inactive() {
        return new PlayerCombatProfile(
                0, 0.0, 1.0, 1.0, 1.0, 1.0, 20.0, 1.0, 100.0,
                WeakStat.NONE, 0.0, "", "", FightingStyle.HYBRID, 0L
        );
    }

    public boolean active() {
        return activeTier > 0 && tierPercent > 0.0;
    }

    public UnlockTier unlockTier() {
        return UnlockTier.byId(activeTier);
    }

    /**
     * Target mob HP at this player's current transformed / released power.
     * Scaled by {@link DifficultyConfig#mobHealthScale} (default 50%).
     */
    public double targetMobHealth(DifficultyConfig cfg) {
        double base = maxHealth * tierPercent;
        // Glass cannons (weak DEF/VIT) shouldn't free-melt tiered packs.
        if (weakest == WeakStat.DEFENSE || weakest == WeakStat.VITALITY) {
            base *= Math.max(1.0, Math.sqrt(Math.max(1.0, cfg.weakStatCounterMult)));
        }
        // Mild HP bump when the build is heavily skewed.
        if (imbalance > 0.35) {
            base *= 1.0 + (imbalance - 0.35) * 0.35;
        }
        // Class/race: ki casters + glass races need denser packs.
        if (cfg.enableClassCounters) {
            if (style == FightingStyle.KI || style == FightingStyle.STRIKE) {
                base *= Math.max(1.0, cfg.classCounterHealthMult);
            } else if (style == FightingStyle.MELEE) {
                base *= Math.max(1.0, 1.0 + (cfg.classCounterHealthMult - 1.0) * 0.45);
            }
            base *= raceHealthBias(cfg);
        }
        double scale = cfg == null ? 0.5 : Math.max(0.05, Math.min(4.0, cfg.mobHealthScale));
        return Math.max(10.0, base * scale);
    }

    /**
     * Target mob attack. Uses blended offense, a tankiness floor (so DEF/VIT
     * dumps still get pressured), weak-stat counters, specialization tax,
     * and DMZ class/race overlays.
     * <p>
     * High-DEF builds are pressed even when DEF is their strongest stat: DMZ
     * cancels hits when DEF is much larger than damage, so the pierce floor
     * must stay a real fraction of player defense (not only a weak-stat counter).
     */
    public double targetMobDamage(DifficultyConfig cfg) {
        double offenseShare = offense * tierPercent;
        double defFloor = defense * tierPercent * Math.max(0.0, cfg.tankDamageDefenseRatio);
        double hpFloor = maxHealth * tierPercent * Math.max(0.0, cfg.tankDamageHealthRatio);
        // Class tanks always get the floor treatment even with "even" invested stats.
        if (cfg.enableClassCounters && style == FightingStyle.TANK) {
            defFloor *= 1.20;
            hpFloor *= 1.25;
        }
        double base = Math.max(offenseShare, Math.max(defFloor, hpFloor));

        // DEF:offense ratio — pure tanks used to face wet-noodle hits at low tiers
        // because pierce only ran when DEF was the *weakest* invested stat.
        double tankiness = defense / Math.max(1.0, offense);
        boolean tankBuild = tankiness > 1.15
                || weakest == WeakStat.STRENGTH
                || weakest == WeakStat.STRIKE
                || weakest == WeakStat.KI_POWER
                || (cfg.enableClassCounters && style == FightingStyle.TANK);
        if (tankBuild) {
            double pierce = Math.max(cfg.tankDamageDefenseRatio, cfg.weakDefensePierceMult);
            // Extra bite as DEF outpaces offense (capped so they aren't one-shot).
            double tankExtra = Math.min(1.75, 1.0 + Math.max(0.0, tankiness - 1.0) * 0.45);
            double throughDefense = defense * tierPercent * pierce * tankExtra;
            base = Math.max(base, throughDefense);
            if (weakest == WeakStat.STRENGTH
                    || weakest == WeakStat.STRIKE
                    || weakest == WeakStat.KI_POWER) {
                base *= Math.max(1.0, cfg.weakStatCounterMult);
            }
        }

        if (weakest == WeakStat.DEFENSE || weakest == WeakStat.VITALITY) {
            double counter = Math.max(1.0, cfg.weakStatCounterMult);
            // Glass cannons: aim through their thin defense share.
            double throughDefense = defense * tierPercent * Math.max(1.25, cfg.weakDefensePierceMult);
            base = Math.max(base * counter, throughDefense);
        }

        if (imbalance > 0.0) {
            base *= 1.0 + imbalance * Math.max(0.0, cfg.specializationDamageTax);
        }

        if (cfg.enableClassCounters) {
            base *= classDamageBias(cfg);
            base *= raceDamageBias(cfg);
        }
        return Math.max(1.0, base);
    }

    /** Vanilla-ish armor contribution derived from player defense share. */
    public double targetMobArmor(DifficultyConfig cfg) {
        double share = defense * tierPercent;
        double armor = Math.log1p(Math.max(0.0, share)) * cfg.defenseToArmorFactor;
        if (weakest == WeakStat.STRENGTH || weakest == WeakStat.STRIKE || weakest == WeakStat.KI_POWER) {
            armor *= Math.max(1.0, cfg.weakStatCounterMult);
        }
        if (cfg.enableClassCounters
                && (style == FightingStyle.MELEE || style == FightingStyle.STRIKE)) {
            armor *= Math.max(1.0, cfg.classCounterArmorMult);
        }
        if (cfg.maxArmorBonus > 0.0) {
            armor = Math.min(cfg.maxArmorBonus, armor);
        }
        return Math.max(0.0, armor);
    }

    private double classDamageBias(DifficultyConfig cfg) {
        double mult = Math.max(1.0, cfg.classCounterDamageMult);
        return switch (style) {
            case MELEE, STRIKE -> mult;
            case KI -> Math.max(1.0, 1.0 + (mult - 1.0) * 0.85);
            case TANK -> Math.max(1.0, 1.0 + (mult - 1.0) * 1.15);
            case HYBRID -> Math.max(1.0, 1.0 + (mult - 1.0) * 0.55);
        };
    }

    private double raceDamageBias(DifficultyConfig cfg) {
        double raceMult = Math.max(1.0, cfg.raceCounterMult);
        String r = race == null ? "" : race;
        // Regen / sustain races — keep pressure up between hits.
        if (r.contains("majin") || r.contains("namek") || r.contains("bio")) {
            return raceMult;
        }
        // Transform / glass burst races — denser hits so they can't one-shot free.
        if (r.contains("saiyan") || r.contains("frost") || r.contains("viltrum")) {
            return Math.max(1.0, 1.0 + (raceMult - 1.0) * 0.75);
        }
        if (r.contains("human") || r.contains("monkey")) {
            return Math.max(1.0, 1.0 + (raceMult - 1.0) * 0.5);
        }
        return Math.max(1.0, 1.0 + (raceMult - 1.0) * 0.35);
    }

    private double raceHealthBias(DifficultyConfig cfg) {
        double raceMult = Math.max(1.0, cfg.raceCounterMult);
        String r = race == null ? "" : race;
        if (r.contains("saiyan") || r.contains("frost") || r.contains("viltrum")) {
            return raceMult;
        }
        if (r.contains("human")) {
            return Math.max(1.0, 1.0 + (raceMult - 1.0) * 0.4);
        }
        return 1.0;
    }

    private record StatBalance(WeakStat weakest, double imbalance) {}

    private record Cached(PlayerCombatProfile profile, long expiresAtMs) {}

    private static void pruneCache(long now) {
        CACHE.entrySet().removeIf(e -> e.getValue() == null || e.getValue().expiresAtMs <= now);
        if (CACHE.size() > 512) {
            CACHE.clear();
        }
    }

    static FightingStyle resolveStyle(
            String fightingClass,
            double melee,
            double strike,
            double ki,
            double defense,
            double health
    ) {
        String cls = fightingClass == null ? "" : fightingClass.toLowerCase(Locale.ROOT).trim();
        return switch (cls) {
            case "warrior", "berserker" -> FightingStyle.MELEE;
            case "martialartist", "martial_artist", "martial-artist" -> FightingStyle.STRIKE;
            case "spiritualist", "cleric" -> FightingStyle.KI;
            case "tank", "paladin" -> FightingStyle.TANK;
            default -> styleFromOffense(melee, strike, ki, defense, health);
        };
    }

    private static FightingStyle styleFromOffense(
            double melee, double strike, double ki, double defense, double health
    ) {
        double peakOff = Math.max(melee, Math.max(strike, ki));
        double tankiness = Math.max(defense, health / 50.0);
        if (tankiness > peakOff * 1.15) {
            return FightingStyle.TANK;
        }
        if (melee >= strike && melee >= ki) {
            return FightingStyle.MELEE;
        }
        if (strike >= melee && strike >= ki) {
            return FightingStyle.STRIKE;
        }
        if (ki >= melee && ki >= strike) {
            return FightingStyle.KI;
        }
        return FightingStyle.HYBRID;
    }

    private static StatBalance resolveBalance(
            StatsData data,
            double melee,
            double strike,
            double defense,
            double health,
            double ki
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
            return new StatBalance(WeakStat.NONE, 0.0);
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
            lowest = nPwr;
            best = WeakStat.KI_POWER;
        }
        // Only counter when the gap is meaningful (not a flat build).
        if (lowest > 0.85) {
            return new StatBalance(WeakStat.NONE, 0.0);
        }
        // Soften near the flat threshold so mild spreads aren't taxed like dumps.
        double imbalance = Math.max(0.0, Math.min(1.0, (0.85 - lowest) / 0.85));
        return new StatBalance(best, imbalance);
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
            WeakStat weakest,
            double imbalance,
            String fightingClass,
            String race,
            FightingStyle style
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
        h = mix(h, Math.round(imbalance * 1000.0));
        h = mix(h, fightingClass == null ? 0 : fightingClass.hashCode());
        h = mix(h, race == null ? 0 : race.hashCode());
        h = mix(h, style == null ? 0 : style.ordinal() + 1);
        // Formula revision bump so cached mobs re-paint after class counters.
        h = mix(h, 3L);
        return h;
    }

    private static long mix(long h, long v) {
        h ^= v;
        h *= 1099511628211L;
        return h;
    }
}
