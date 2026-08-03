package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.character.Stats;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Combat snapshot used to scale nearby hostiles to a fraction of the player's power.
 * <p>
 * Live DMZ combat getters include full form multipliers. Enemy scaling blends
 * form-stripped stats with only a portion of that transform boost
 * ({@link DifficultyConfig#transformScaleWeight}) so transforming does not
 * instantly spike mob/creeper damage 1:1 with your form.
 * <p>
 * Counters are intentionally light:
 * <ul>
 *   <li>DMZ fighting class overlay</li>
 *   <li>single highest combat stat (STR/SKP/RES/VIT/PWR)</li>
 * </ul>
 * Race overlays, weak-stat dump multipliers, and specialization tax are off.
 * Baseline DEF/VIT tank pierce floors remain so high mitigation cannot zero hits.
 */
public final class PlayerCombatProfile {
    private static final long CACHE_TTL_MS = 250L;
    private static final Map<UUID, Cached> CACHE = new ConcurrentHashMap<>();
    private static final WeakStat[] NO_TOP = new WeakStat[0];

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
    /**
     * Player's single highest invested combat stat (length 0 or 1).
     * Empty when inactive / unavailable.
     */
    public final WeakStat[] topStats;
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
            WeakStat[] topStats,
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
        this.weakest = weakest == null ? WeakStat.NONE : weakest;
        this.imbalance = Math.max(0.0, Math.min(1.0, imbalance));
        this.topStats = topStats == null ? NO_TOP : Arrays.copyOf(topStats, topStats.length);
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
        DifficultyConfig cfg = DifficultyConfig.get();
        double pct = cfg.tierPlayerStatPercent(tier);
        StatsData data = DmzProgression.stats(player);
        double melee = 1.0;
        double strike = 1.0;
        double ki = 1.0;
        double def = 1.0;
        double hp = 20.0;
        double release = 100.0;
        if (data != null) {
            try {
                double liveMelee = Math.max(1.0, data.getMeleeDamage());
                double liveStrike = Math.max(1.0, data.getStrikeDamage());
                double liveKi = Math.max(1.0, data.getKiDamage());
                double liveDef = Math.max(1.0, data.getDefense());
                double liveHp = Math.max(20.0, data.getMaxHealth());
                // Form-stripped offense when DMZ exposes it.
                double baseMelee = Math.max(1.0, data.getMeleeDamageNoMultipliers());
                double baseStrike = Math.max(1.0, data.getStrikeDamageNoForms());
                double baseKi = Math.max(1.0, data.getKiDamageNoForms());
                double formBoost = estimateFormBoost(
                        liveMelee, baseMelee, liveStrike, baseStrike, liveKi, baseKi);
                // DEF/HP have no no-form getters — peel the shared form boost.
                double baseDef = Math.max(1.0, liveDef / formBoost);
                double baseHp = Math.max(20.0, liveHp / formBoost);
                double tw = Math.max(0.0, Math.min(1.0, cfg.transformScaleWeight));
                melee = blendForm(baseMelee, liveMelee, tw);
                strike = blendForm(baseStrike, liveStrike, tw);
                ki = blendForm(baseKi, liveKi, tw);
                def = blendForm(baseDef, liveDef, tw);
                hp = blendForm(baseHp, liveHp, tw);
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
                balance.weakest, balance.imbalance, balance.topStats,
                fightingClass, race, style
        );
        return new PlayerCombatProfile(
                tier, pct, melee, strike, ki, def, hp, offense, release,
                balance.weakest, balance.imbalance, balance.topStats,
                fightingClass, race, style, sig
        );
    }

    private static PlayerCombatProfile inactive() {
        return new PlayerCombatProfile(
                0, 0.0, 1.0, 1.0, 1.0, 1.0, 20.0, 1.0, 100.0,
                WeakStat.NONE, 0.0, NO_TOP, "", "", FightingStyle.HYBRID, 0L
        );
    }

    public boolean active() {
        return activeTier > 0 && tierPercent > 0.0;
    }

    public UnlockTier unlockTier() {
        return UnlockTier.byId(activeTier);
    }

    /** True when {@code stat} is the player's single highest combat stat. */
    public boolean isTopStat(WeakStat stat) {
        if (stat == null || stat == WeakStat.NONE || topStats.length == 0) {
            return false;
        }
        return topStats[0] == stat;
    }

    /** Compact label for GUI/staff: single top stat ({@code STR}) or {@code —}. */
    public String topStatsLabel() {
        if (topStats.length == 0) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < topStats.length; i++) {
            if (i > 0) {
                sb.append('>');
            }
            sb.append(shortStat(topStats[i]));
        }
        return sb.toString();
    }

    private static String shortStat(WeakStat stat) {
        return switch (stat == null ? WeakStat.NONE : stat) {
            case STRENGTH -> "STR";
            case STRIKE -> "SKP";
            case DEFENSE -> "RES";
            case VITALITY -> "VIT";
            case KI_POWER -> "PWR";
            case NONE -> "?";
        };
    }

    /**
     * Target mob HP at this player's current transformed / released power.
     * Scaled by {@link DifficultyConfig#mobHealthScale} (default 50%).
     */
    public double targetMobHealth(DifficultyConfig cfg) {
        double base = maxHealth * tierPercent;
        // Survive the player's strongest damage channel (class + top-stat, capped).
        // Counter intensity ramps with tier% so T2 20% stays near 20%.
        double overlay = 1.0;
        if (cfg.enableStrongStatCounters) {
            overlay *= blendCounter(strongStatHealthBias(cfg));
        }
        if (cfg.enableClassCounters) {
            overlay *= blendCounter(classHealthBias(cfg));
        }
        overlay = clampCounterOverlay(overlay, cfg);
        double scale = cfg == null ? 0.5 : Math.max(0.05, Math.min(4.0, cfg.mobHealthScale));
        return Math.max(10.0, base * overlay * scale);
    }

    /**
     * Target mob attack. Uses blended offense, a light tankiness floor so high
     * DEF cannot cancel hits to 0, plus class + top-stat overlays only.
     */
    public double targetMobDamage(DifficultyConfig cfg) {
        double strength = counterStrength();
        double offenseShare = offense * tierPercent;
        // Early tiers lean on offense share; DEF/HP floors ramp in later.
        double floorScale = 0.40 + 0.60 * strength;
        double defFloor = defense * tierPercent * Math.max(0.0, cfg.tankDamageDefenseRatio) * floorScale;
        double hpFloor = maxHealth * tierPercent * Math.max(0.0, cfg.tankDamageHealthRatio) * floorScale;
        // Class tanks always get the floor treatment even with "even" invested stats.
        if (cfg.enableClassCounters && style == FightingStyle.TANK) {
            double tankBump = 1.0 + 0.08 * strength;
            defFloor *= tankBump;
            hpFloor *= tankBump;
        }
        double base = Math.max(offenseShare, Math.max(defFloor, hpFloor));

        // DEF:offense pierce floor — keeps DMZ mitigation from zeroing hits.
        // Soft at low tier% so "20%" is not secretly ~DEF×0.3 after pierce+overlay.
        double tankiness = defense / Math.max(1.0, offense);
        boolean tankBuild = tankiness > 1.15
                || isTopStat(WeakStat.DEFENSE)
                || isTopStat(WeakStat.VITALITY)
                || (cfg.enableClassCounters && style == FightingStyle.TANK);
        if (tankBuild) {
            double pierce = Math.max(0.0, cfg.tankDamageDefenseRatio) * floorScale;
            double tankExtra = 1.0 + Math.min(0.12, Math.max(0.0, tankiness - 1.0) * 0.12) * strength;
            double throughDefense = defense * tierPercent * pierce * tankExtra;
            base = Math.max(base, throughDefense);
        }

        // Class + top-stat overlays only (no race / weak-stat / specialization stack).
        double overlay = 1.0;
        if (cfg.enableStrongStatCounters) {
            overlay *= blendCounter(strongStatDamageBias(cfg));
        }
        if (cfg.enableClassCounters) {
            overlay *= blendCounter(classDamageBias(cfg));
        }
        overlay = clampCounterOverlay(overlay, cfg);
        return Math.max(1.0, base * overlay);
    }

    /** Vanilla-ish armor contribution derived from player defense share. */
    public double targetMobArmor(DifficultyConfig cfg) {
        double share = defense * tierPercent;
        double armor = Math.log1p(Math.max(0.0, share)) * cfg.defenseToArmorFactor;
        double overlay = 1.0;
        if (cfg.enableStrongStatCounters) {
            overlay *= blendCounter(strongStatArmorBias(cfg));
        }
        if (cfg.enableClassCounters) {
            if (style == FightingStyle.MELEE || style == FightingStyle.STRIKE) {
                overlay *= blendCounter(Math.max(1.0, cfg.classCounterArmorMult));
            } else if (style == FightingStyle.KI) {
                overlay *= blendCounter(Math.max(1.0, 1.0 + (cfg.classCounterArmorMult - 1.0) * 0.45));
            }
        }
        overlay = clampCounterOverlay(overlay, cfg);
        armor *= overlay;
        if (cfg.maxArmorBonus > 0.0) {
            armor = Math.min(cfg.maxArmorBonus, armor);
        }
        return Math.max(0.0, armor);
    }

    /**
     * How hard class/top-stat counters apply (0–1).
     * Full strength from ~50% tier ladder up; T2 20% ≈ 40% counter power.
     */
    private double counterStrength() {
        return Math.max(0.0, Math.min(1.0, tierPercent / 0.50));
    }

    /** Lerp a counter bias toward 1.0 at low unlock tiers. */
    private double blendCounter(double bias) {
        double s = counterStrength();
        return 1.0 + (Math.max(1.0, bias) - 1.0) * s;
    }

    private static double clampCounterOverlay(double overlay, DifficultyConfig cfg) {
        double cap = cfg == null ? 1.25 : Math.max(1.0, Math.min(4.0, cfg.maxCounterOverlayMult));
        return Math.max(1.0, Math.min(cap, overlay));
    }

    /**
     * Kit cooldown scale for evolution abilities (&lt; 1 = more aggressive).
     * Driven only by fighting class + single top combat stat.
     * No kit-cadence acceleration on early tiers (keeps T1/T2 near raw tier%).
     */
    public double kitCooldownScale(DifficultyConfig cfg) {
        if (cfg == null || (!cfg.enableClassCounters && !cfg.enableStrongStatCounters)) {
            return 1.0;
        }
        double strength = counterStrength();
        if (strength < 0.45) {
            // Below ~T3 (22.5% of the 50% full-counter mark) — no faster kits.
            return 1.0;
        }
        double scale = 1.0;
        if (cfg.enableClassCounters) {
            scale *= switch (style) {
                case MELEE, STRIKE -> 0.96;
                case TANK -> 0.95;
                case KI -> 0.97;
                case HYBRID -> 0.99;
            };
        }
        if (cfg.enableStrongStatCounters && topStats.length > 0) {
            scale *= switch (topStats[0]) {
                case STRENGTH, STRIKE -> 0.97;
                case KI_POWER -> 0.96;
                case DEFENSE, VITALITY -> 0.98;
                case NONE -> 1.0;
            };
        }
        // Blend toward 1.0 when still ramping into mid tiers.
        scale = 1.0 + (scale - 1.0) * strength;
        return Math.max(0.90, Math.min(1.0, scale));
    }

    // ── Strong-stat (top-1) biases ─────────────────────────────────────────

    private double strongStatHealthBias(DifficultyConfig cfg) {
        double mult = Math.max(1.0, cfg.strongStatCounterMult);
        WeakStat top = topStat();
        if (top == WeakStat.NONE) {
            return 1.0;
        }
        double bump = mult - 1.0;
        return switch (top) {
            case STRENGTH, STRIKE -> 1.0 + bump * 0.70;
            case KI_POWER -> 1.0 + bump * 0.85;
            case DEFENSE, VITALITY -> 1.0 + bump * 0.30;
            case NONE -> 1.0;
        };
    }

    private double strongStatDamageBias(DifficultyConfig cfg) {
        double mult = Math.max(1.0, cfg.strongStatCounterMult);
        WeakStat top = topStat();
        if (top == WeakStat.NONE) {
            return 1.0;
        }
        double bump = mult - 1.0;
        return switch (top) {
            case STRENGTH, STRIKE -> 1.0 + bump * 0.55;
            case KI_POWER -> 1.0 + bump * 0.65;
            case DEFENSE -> 1.0 + bump * 0.85;
            case VITALITY -> 1.0 + bump * 0.75;
            case NONE -> 1.0;
        };
    }

    private double strongStatArmorBias(DifficultyConfig cfg) {
        double mult = Math.max(1.0, cfg.strongStatCounterMult);
        WeakStat top = topStat();
        if (top == WeakStat.NONE) {
            return 1.0;
        }
        double bump = mult - 1.0;
        return switch (top) {
            case STRENGTH, STRIKE -> 1.0 + bump * 0.80;
            case KI_POWER -> 1.0 + bump * 0.40;
            case DEFENSE, VITALITY -> 1.0 + bump * 0.20;
            case NONE -> 1.0;
        };
    }

    private WeakStat topStat() {
        if (topStats.length == 0 || topStats[0] == null) {
            return WeakStat.NONE;
        }
        return topStats[0];
    }

    // ── Class biases ──────────────────────────────────────────────────────

    private double classDamageBias(DifficultyConfig cfg) {
        double mult = Math.max(1.0, cfg.classCounterDamageMult);
        String cls = fightingClass == null ? "" : fightingClass;
        if (cls.contains("berserk")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 1.10);
        }
        if (cls.contains("tank") || cls.contains("paladin")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 1.12);
        }
        if (cls.contains("warrior")) {
            return mult;
        }
        if (cls.contains("martial")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 1.00);
        }
        if (cls.contains("spirit") || cls.contains("cleric")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 0.90);
        }
        return switch (style) {
            case MELEE, STRIKE -> mult;
            case KI -> Math.max(1.0, 1.0 + (mult - 1.0) * 0.80);
            case TANK -> Math.max(1.0, 1.0 + (mult - 1.0) * 1.10);
            case HYBRID -> Math.max(1.0, 1.0 + (mult - 1.0) * 0.45);
        };
    }

    private double classHealthBias(DifficultyConfig cfg) {
        double mult = Math.max(1.0, cfg.classCounterHealthMult);
        String cls = fightingClass == null ? "" : fightingClass;
        if (cls.contains("spirit") || cls.contains("cleric")) {
            return mult;
        }
        if (cls.contains("martial")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 0.90);
        }
        if (cls.contains("berserk")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 0.75);
        }
        if (cls.contains("warrior")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 0.55);
        }
        if (cls.contains("tank") || cls.contains("paladin")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 0.35);
        }
        return switch (style) {
            case KI, STRIKE -> mult;
            case MELEE -> Math.max(1.0, 1.0 + (mult - 1.0) * 0.45);
            case TANK -> Math.max(1.0, 1.0 + (mult - 1.0) * 0.30);
            case HYBRID -> Math.max(1.0, 1.0 + (mult - 1.0) * 0.40);
        };
    }

    private record StatBalance(WeakStat weakest, double imbalance, WeakStat[] topStats) {}

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
        if (cls.contains("berserk") || cls.contains("warrior")) {
            return FightingStyle.MELEE;
        }
        if (cls.contains("martial")) {
            return FightingStyle.STRIKE;
        }
        if (cls.contains("spirit") || cls.contains("cleric")) {
            return FightingStyle.KI;
        }
        if (cls.contains("tank") || cls.contains("paladin")) {
            return FightingStyle.TANK;
        }
        return styleFromOffense(melee, strike, ki, defense, health);
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
            return new StatBalance(WeakStat.NONE, 0.0, NO_TOP);
        }
        // Normalize to peak so different units stay comparable.
        record Ranked(WeakStat stat, double norm) {}
        Ranked[] ranked = {
                new Ranked(WeakStat.STRENGTH, str / peak),
                new Ranked(WeakStat.STRIKE, skp / peak),
                new Ranked(WeakStat.DEFENSE, res / peak),
                new Ranked(WeakStat.VITALITY, vit / peak),
                new Ranked(WeakStat.KI_POWER, pwr / peak)
        };
        Arrays.sort(ranked, Comparator.comparingDouble((Ranked r) -> r.norm).reversed());

        // Combat counters use only the single highest invested stat.
        WeakStat[] top = { ranked[0].stat };

        WeakStat weakest = ranked[ranked.length - 1].stat;
        double lowest = ranked[ranked.length - 1].norm;
        // Display-only dump readout (no longer taxes mob damage).
        if (lowest > 0.85) {
            return new StatBalance(WeakStat.NONE, 0.0, top);
        }
        double imbalance = Math.max(0.0, Math.min(1.0, (0.85 - lowest) / 0.85));
        return new StatBalance(weakest, imbalance, top);
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
            WeakStat[] topStats,
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
        if (topStats != null) {
            for (int i = 0; i < topStats.length; i++) {
                WeakStat s = topStats[i];
                h = mix(h, (s == null ? 0 : s.ordinal() + 1) * 31L + i);
            }
        }
        h = mix(h, fightingClass == null ? 0 : fightingClass.hashCode());
        h = mix(h, race == null ? 0 : race.hashCode());
        h = mix(h, style == null ? 0 : style.ordinal() + 1);
        // Include live counter formula knobs so admin retunes invalidate paint.
        DifficultyConfig liveCfg = DifficultyConfig.get();
        h = mix(h, Math.round(liveCfg.strongStatCounterMult * 1000.0));
        h = mix(h, Math.round(liveCfg.classCounterDamageMult * 1000.0));
        h = mix(h, Math.round(liveCfg.classCounterHealthMult * 1000.0));
        h = mix(h, Math.round(liveCfg.classCounterArmorMult * 1000.0));
        h = mix(h, Math.round(liveCfg.maxCounterOverlayMult * 1000.0));
        h = mix(h, Math.round(liveCfg.mobHealthScale * 1000.0));
        h = mix(h, Math.round(liveCfg.transformScaleWeight * 1000.0));
        h = mix(h, liveCfg.enableClassCounters ? 1L : 0L);
        h = mix(h, liveCfg.enableStrongStatCounters ? 1L : 0L);
        // Formula revision: transform-boost dampening for enemy scaling.
        h = mix(h, 10L);
        return h;
    }

    /**
     * How hard the live (form) stats outpace form-stripped offense.
     * 1.0 = base form / no detectable boost.
     */
    private static double estimateFormBoost(
            double liveMelee, double baseMelee,
            double liveStrike, double baseStrike,
            double liveKi, double baseKi
    ) {
        double rMelee = liveMelee / Math.max(1.0, baseMelee);
        double rStrike = liveStrike / Math.max(1.0, baseStrike);
        double rKi = liveKi / Math.max(1.0, baseKi);
        // Median-ish: average of the two closest ratios to ignore one weird channel.
        double sum = rMelee + rStrike + rKi;
        double max = Math.max(rMelee, Math.max(rStrike, rKi));
        double min = Math.min(rMelee, Math.min(rStrike, rKi));
        double mid = sum - max - min;
        double boost = mid > 0.0 ? mid : (sum / 3.0);
        if (!(boost > 0.0) || Double.isNaN(boost) || Double.isInfinite(boost)) {
            return 1.0;
        }
        return Math.max(1.0, Math.min(50.0, boost));
    }

    /** {@code base + (live - base) × weight} — enemies only see part of the form spike. */
    private static double blendForm(double base, double live, double weight) {
        double b = Math.max(0.0, base);
        double l = Math.max(b, live);
        double w = Math.max(0.0, Math.min(1.0, weight));
        return b + (l - b) * w;
    }

    private static long mix(long h, long v) {
        h ^= v;
        h *= 1099511628211L;
        return h;
    }
}
