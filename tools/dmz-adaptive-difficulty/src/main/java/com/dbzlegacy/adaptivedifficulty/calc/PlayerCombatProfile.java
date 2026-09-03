package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
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
 * Design:
 * <ul>
 *   <li><b>Mob damage</b> tracks soft-blended <b>STR/SKP/PWR</b> (+ mild ENE pool) × tier%</li>
 *   <li><b>Mob HP</b> tracks soft <b>VIT</b> with a mild offense durability floor</li>
 *   <li><b>Counters:</b> fighting class + top <b>2</b> invested combat stats
 *       (STR/SKP/RES/VIT/PWR/ENE), intensity ramps with tier%</li>
 *   <li>VIT-relative hit cap keeps unprotected punches from dumping a full bag</li>
 * </ul>
 * Form boost uses a diminishing soft curve
 * ({@link DifficultyConfig#transformScaleWeight} / {@link DifficultyConfig#transformScaleExponent},
 * mega compress ×6→×80) on STR/SKP/PWR/ENE.
 */
public final class PlayerCombatProfile {
    private static final long CACHE_TTL_MS = 250L;
    private static final Map<UUID, Cached> CACHE = new ConcurrentHashMap<>();
    /** Base-form combat snapshot — used when NoForms getters ignore custom-race forms. */
    private static final Map<UUID, FormBaseline> FORM_BASELINES = new ConcurrentHashMap<>();
    private static final WeakStat[] NO_TOP = new WeakStat[0];
    /** How much of max energy pool feeds the soft offense blend (ENE is a pool, not ATK). */
    private static final double ENERGY_OFFENSE_FACTOR = 0.08;

    public enum WeakStat {
        STRENGTH,
        STRIKE,
        DEFENSE,
        VITALITY,
        KI_POWER,
        ENERGY,
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
    /** Full live form offense (pre soft-curve) — used for T1–T3 transform pressure. */
    public final double liveOffense;
    /** Full live form max HP (pre soft-curve). */
    public final double liveMaxHealth;
    /**
     * Live DMZ flat mitigation ({@code getDefense() × DEF form mult}).
     * Used to clear {@code cancelDamageMitigationThreshold} (stock 2.5×) so
     * SSJB-style DEF≫VIT forms cannot hard-cancel painted hits to 0.
     */
    public final double liveFlatMitigation;
    /** Detected form boost (≥1). */
    public final double formBoost;
    public final double releasePercent;
    public final WeakStat weakest;
    /**
     * 0 = even build, approaches 1 as the weakest combat stat is dumped
     * relative to the player's peak invested stat.
     */
    public final double imbalance;
    /**
     * Player's highest invested combat stats (up to 2), highest first.
     * Empty when inactive / unavailable.
     */
    public final WeakStat[] topStats;
    public final String fightingClass;
    public final String race;
    public final FightingStyle style;
    /** DMZ {@code kiprotection} level (0–10). Used for hit-cap headroom so KP stays load-bearing. */
    public final int kiProtectionLevel;
    /** DMZ {@code ki_infusion} level (0–10). Raises pack sponge when trained / active. */
    public final int kiInfusionLevel;
    /** True when {@code ki_infusion} is toggled on. */
    public final boolean kiInfusionActive;
    /** DMZ {@code potentialunlock} level (0–30). Mild form-sponge when transformed. */
    public final int potentialUnlockLevel;
    /** Stable fingerprint for mob re-scale cache invalidation. */
    public final long signature;
    /**
     * Paint attenuation (0.40–1.0): eases fresh tier buys within the DMZ level band and
     * falls off when level exceeds the next tier gate (e.g. T3 @ 100k DMZ).
     */
    public final double paintEase;
    /** DMZ level used for {@link #paintEase} (live progression read). */
    public final int progressionDmzLevel;
    /**
     * Paint relief from live DMZ mitigation (DEF + enchantments via
     * {@code calculatePostMitigationDamage}). High-defense builds get slightly
     * lower painted mob damage so enchants/DEF counter scaling.
     */
    public final double defenseMitigationRelief;

    private PlayerCombatProfile(
            int activeTier,
            double tierPercent,
            double meleeDamage,
            double strikeDamage,
            double kiDamage,
            double defense,
            double maxHealth,
            double offense,
            double liveOffense,
            double liveMaxHealth,
            double liveFlatMitigation,
            double formBoost,
            double releasePercent,
            WeakStat weakest,
            double imbalance,
            WeakStat[] topStats,
            String fightingClass,
            String race,
            FightingStyle style,
            int kiProtectionLevel,
            int kiInfusionLevel,
            boolean kiInfusionActive,
            int potentialUnlockLevel,
            long signature,
            double paintEase,
            int progressionDmzLevel,
            double defenseMitigationRelief
    ) {
        this.activeTier = activeTier;
        this.tierPercent = tierPercent;
        this.meleeDamage = meleeDamage;
        this.strikeDamage = strikeDamage;
        this.kiDamage = kiDamage;
        this.defense = defense;
        this.maxHealth = maxHealth;
        this.offense = offense;
        this.liveOffense = Math.max(offense, liveOffense);
        this.liveMaxHealth = Math.max(maxHealth, liveMaxHealth);
        this.liveFlatMitigation = Math.max(0.0, liveFlatMitigation);
        this.formBoost = Math.max(1.0, formBoost);
        this.releasePercent = releasePercent;
        this.weakest = weakest == null ? WeakStat.NONE : weakest;
        this.imbalance = Math.max(0.0, Math.min(1.0, imbalance));
        this.topStats = topStats == null ? NO_TOP : Arrays.copyOf(topStats, topStats.length);
        this.fightingClass = fightingClass == null ? "" : fightingClass;
        this.race = race == null ? "" : race;
        this.style = style == null ? FightingStyle.HYBRID : style;
        this.kiProtectionLevel = Math.max(0, Math.min(10, kiProtectionLevel));
        this.kiInfusionLevel = Math.max(0, Math.min(10, kiInfusionLevel));
        this.kiInfusionActive = kiInfusionActive;
        this.potentialUnlockLevel = Math.max(0, Math.min(30, potentialUnlockLevel));
        this.signature = signature;
        this.paintEase = Math.max(0.35, Math.min(1.0, paintEase));
        this.progressionDmzLevel = Math.max(0, progressionDmzLevel);
        this.defenseMitigationRelief = Math.max(0.0, Math.min(0.12, defenseMitigationRelief));
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
        PlayerCombatProfile profile;
        try {
            profile = build(player);
        } catch (Throwable t) {
            // Future race/getter bugs must never take down nearby scaling.
            profile = inactive();
        }
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

    /** Drop form baseline (logout / race change). */
    public static void clearFormBaseline(UUID playerId) {
        if (playerId != null) {
            FORM_BASELINES.remove(playerId);
        }
    }

    /** Live DMZ form×stack multiplier peak (1.0 = base form). */
    public static double liveFormMultiplier(ServerPlayer player) {
        StatsData data = DmzProgression.stats(player);
        return liveFormMultiplierPeak(data);
    }

    /** Package-visible for {@link DmzProgression} unlock-gate transform checks. */
    public static double liveFormMultiplierPeak(StatsData data) {
        return data == null ? 1.0 : formMultiplierBoost(data);
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
        double formBoost = 1.0;
        double liveOffense = 1.0;
        double liveMaxHealth = 20.0;
        double liveFlatMitigation = 1.0;
        double energy = 1.0;
        if (data != null) {
            // Read live channels independently — one bad custom-race getter must not
            // wipe the whole profile. CombatSanity clamps NaN / absurd values.
            double liveMelee = CombatSanity.saneLive(readStat(() -> data.getMeleeDamage(), 1.0), 1.0);
            double liveStrike = CombatSanity.saneLive(readStat(() -> data.getStrikeDamage(), 1.0), 1.0);
            double liveKi = CombatSanity.saneLive(readStat(() -> data.getKiDamage(), 1.0), 1.0);
            double liveEnergy = CombatSanity.saneLive(readStat(() -> data.getMaxEnergy(), 1.0), 1.0);
            double liveDef = CombatSanity.saneLive(readStat(() -> data.getDefense(), 1.0), 1.0);
            double liveHp = CombatSanity.saneLive(readStat(() -> data.getMaxHealth(), 20.0), 20.0);
            // Flat mit includes DEF form mult — getDefense() alone understates SSJB cancel bar.
            double liveFlat = CombatSanity.saneLive(
                    readStat(() -> data.getFlatMitigation(), liveDef), liveDef);
            liveOffense = blendedOffense(liveMelee, liveStrike, liveKi, liveEnergy);
            liveMaxHealth = liveHp;
            liveFlatMitigation = Math.max(liveDef, liveFlat);

            String raceId = DmzProgression.race(player);
            double strForm = CombatSanity.saneFormMult(statFormMultiplier(data, "STR"));
            double skpForm = CombatSanity.saneFormMult(statFormMultiplier(data, "SKP"));
            double pwrForm = CombatSanity.saneFormMult(statFormMultiplier(data, "PWR"));
            double eneForm = CombatSanity.saneFormMult(statFormMultiplier(data, "ENE"));
            double vitForm = CombatSanity.saneFormMult(statFormMultiplier(data, "VIT"));
            double resForm = CombatSanity.saneFormMult(statFormMultiplier(data, "RES"));
            formBoost = CombatSanity.saneFormMult(Math.max(
                    strForm, Math.max(skpForm, Math.max(pwrForm, Math.max(eneForm, Math.max(vitForm, resForm))))));
            boolean dmzFormActive = isDmzFormActive(data) || formBoost > 1.12;

            UUID id = player.m_20148_();
            FormBaseline rawBaseline = FORM_BASELINES.get(id);
            FormBaseline baseline = rawBaseline;
            if (baseline != null && !CombatSanity.usableBaseline(baseline.atMs, baseline.race, raceId)) {
                FORM_BASELINES.remove(id);
                baseline = null;
            }

            double baseMelee = peelChannel(liveMelee, strForm, baseline == null ? 0.0 : baseline.melee, dmzFormActive);
            double baseStrike = peelChannel(liveStrike, skpForm, baseline == null ? 0.0 : baseline.strike, dmzFormActive);
            double baseKi = peelChannel(liveKi, pwrForm, baseline == null ? 0.0 : baseline.ki, dmzFormActive);
            double baseEnergy = peelChannel(liveEnergy, eneForm, baseline == null ? 0.0 : baseline.energy, dmzFormActive);

            if (!dmzFormActive) {
                FORM_BASELINES.put(id, new FormBaseline(
                        liveMelee, liveStrike, liveKi, liveEnergy, liveDef, liveHp,
                        System.currentTimeMillis(), raceId == null ? "" : raceId));
                baseMelee = liveMelee;
                baseStrike = liveStrike;
                baseKi = liveKi;
                baseEnergy = liveEnergy;
                formBoost = 1.0;
            } else if (baseline != null) {
                // Custom races that bake offense outside multipliers.
                double fromBaseline = estimateOffenseFormBoost(
                        liveMelee, baseline.melee,
                        liveStrike, baseline.strike,
                        liveKi, baseline.ki,
                        liveEnergy, baseline.energy);
                if (fromBaseline > formBoost + 0.05) {
                    formBoost = CombatSanity.saneFormMult(fromBaseline);
                    baseMelee = Math.max(1.0, baseline.melee);
                    baseStrike = Math.max(1.0, baseline.strike);
                    baseKi = Math.max(1.0, baseline.ki);
                    baseEnergy = Math.max(1.0, baseline.energy);
                }
            }

            double baseDef = resForm > 1.08
                    ? Math.max(1.0, liveDef / resForm)
                    : (baseline != null && dmzFormActive ? Math.max(1.0, baseline.def) : liveDef);
            double baseHp = vitForm > 1.08
                    ? Math.max(20.0, liveHp / vitForm)
                    : (baseline != null && dmzFormActive ? Math.max(20.0, baseline.hp) : liveHp);
            if (baseline != null && dmzFormActive) {
                baseDef = Math.max(1.0, Math.min(baseDef, baseline.def));
                baseHp = Math.max(20.0, Math.min(baseHp, baseline.hp));
            }
            // Guard peel underflow from typo'd 0.001 form mults.
            baseMelee = CombatSanity.saneLive(baseMelee, 1.0);
            baseStrike = CombatSanity.saneLive(baseStrike, 1.0);
            baseKi = CombatSanity.saneLive(baseKi, 1.0);
            baseEnergy = CombatSanity.saneLive(baseEnergy, 1.0);
            baseDef = CombatSanity.saneLive(baseDef, 1.0);
            baseHp = CombatSanity.saneLive(baseHp, 20.0);

            double twBase = Math.max(0.0, Math.min(1.0, cfg.transformScaleWeight));
            double exp = Math.max(0.20, Math.min(1.0, cfg.transformScaleExponent));
            // Mild tier damp — floor 0.55 so admin ladders above 100% still inherit forms.
            double tierDamp = Math.max(0.55, 1.0 - 0.40 * Math.max(0.0, Math.min(1.0, pct)));
            double twOffense = twBase * tierDamp;
            // Mild T2–T3 form lift only — T1 stays on the soft curve.
            if (tier >= 2 && tier <= 3 && formBoost > 1.12 && formBoost < 6.0) {
                double bump = tier == 2 ? 0.14 : 0.10;
                twOffense = Math.max(twOffense, Math.min(1.0, twBase + bump));
            }
            double megaT = formBoost >= 6.0 ? megaFormT(formBoost) : 0.0;
            if (formBoost >= 6.0) {
                exp = Math.min(exp, megaFormExpCap(megaT));
                twOffense = Math.min(twOffense, twBase * megaFormTwScale(megaT));
            }
            // VIT/RES inherit gently — never near-linear on mega forms (HP must not jump).
            double twBulk = Math.min(0.85, Math.max(twBase, twBase * 1.15) * (0.80 + 0.20 * tierDamp));
            double bulkExp = 0.85;
            if (formBoost >= 6.0) {
                twBulk = Math.min(twBulk, twBase * megaFormBulkTwScale(megaT));
                bulkExp = megaFormBulkExp(megaT);
            }

            melee = blendForm(baseMelee, liveMelee, twOffense, exp);
            strike = blendForm(baseStrike, liveStrike, twOffense, exp);
            ki = blendForm(baseKi, liveKi, twOffense, exp);
            energy = blendForm(baseEnergy, liveEnergy, twOffense, exp);
            def = blendForm(baseDef, liveDef, twBulk, bulkExp);
            hp = blendForm(baseHp, liveHp, twBulk, bulkExp);

            try {
                Resources resources = data.getResources();
                if (resources != null) {
                    double powerRelease = resources.getPowerRelease();
                    double plainRelease = resources.getRelease();
                    release = Math.max(powerRelease, plainRelease);
                    if (!(release > 0.0) || Double.isNaN(release) || Double.isInfinite(release)) {
                        release = 100.0;
                    }
                }
            } catch (Throwable ignored) {
                release = 100.0;
            }
        }
        // Soft STR/SKP/PWR (+ mild ENE pool) — peak+avg so pure tanks aren't wet noodles.
        double offense = blendedOffense(melee, strike, ki, energy);
        // Counter identity: top-2 of STR/SKP/RES/VIT/PWR/ENE (form-stripped).
        StatBalance balance = resolveBalance(data, melee, strike, def, hp, ki, energy, formBoost);
        String fightingClass = DmzProgression.fightingClass(player);
        String race = DmzProgression.race(player);
        FightingStyle style = resolveStyle(fightingClass, melee, strike, ki, def, hp);
        int kiProtect = DmzProgression.skillLevel(player, "kiprotection");
        int kiInfusion = DmzProgression.skillLevel(player, "ki_infusion");
        boolean infusionOn = DmzProgression.skillActive(player, "ki_infusion");
        int potential = DmzProgression.skillLevel(player, "potentialunlock");
        int dmzLevel = DmzProgression.guiDisplayDmzLevel(player);
        double levelEase = paintEase(cfg, tier, dmzLevel);
        double mitRelief = estimateMitigationRelief(data, kiProtect);
        long sig = fingerprint(
                tier, pct, melee, strike, ki, def, hp, release,
                balance.weakest, balance.imbalance, balance.topStats,
                fightingClass, race, style
        );
        sig = mix(sig, Math.round(formBoost * 100.0));
        sig = mix(sig, Math.round(liveOffense));
        sig = mix(sig, Math.round(liveFlatMitigation));
        sig = mix(sig, kiProtect);
        sig = mix(sig, kiInfusion);
        sig = mix(sig, infusionOn ? 1L : 0L);
        sig = mix(sig, potential);
        sig = mix(sig, Math.round(levelEase * 1000.0));
        sig = mix(sig, Math.round(mitRelief * 10000.0));
        return new PlayerCombatProfile(
                tier, pct, melee, strike, ki, def, hp, offense,
                liveOffense, liveMaxHealth, liveFlatMitigation, formBoost, release,
                balance.weakest, balance.imbalance, balance.topStats,
                fightingClass, race, style,
                kiProtect, kiInfusion, infusionOn, potential, sig, levelEase, dmzLevel,
                mitRelief
        );
    }

    private static long tierBandTop(DifficultyConfig cfg, int tier) {
        if (cfg == null || tier <= 0) {
            return Long.MAX_VALUE / 4L;
        }
        long min = cfg.tierRequiredLevel(tier);
        if (tier < 7) {
            return cfg.tierRequiredLevel(tier + 1);
        }
        return Math.max(min + 1L, (long) cfg.tierCostLevelAnchor);
    }

    private static PlayerCombatProfile inactive() {
        return new PlayerCombatProfile(
                0, 0.0, 1.0, 1.0, 1.0, 1.0, 20.0, 1.0,
                1.0, 20.0, 1.0, 1.0, 100.0,
                WeakStat.NONE, 0.0, NO_TOP, "", "", FightingStyle.HYBRID,
                0, 0, false, 0, 0L, 1.0, 0, 0.0
        );
    }

    /**
     * DMZ-level paint ease: ramp within tier band (78%→100% to next tier gate), then
     * sqrt falloff when level exceeds that gate so veterans are not shredded.
     */
    private static double paintEase(DifficultyConfig cfg, int tier, int dmzLevel) {
        if (tier <= 0 || cfg == null) {
            return 1.0;
        }
        long min = cfg.tierRequiredLevel(tier);
        long nextGate = tier < 7 ? cfg.tierRequiredLevel(tier + 1)
                : Math.max(min + 1L, (long) cfg.tierCostLevelAnchor);
        if (nextGate <= min) {
            nextGate = min + 1L;
        }
        long level = Math.max(1L, dmzLevel);
        if (level < min) {
            return 1.0;
        }
        long clamped = Math.max(min, Math.min(nextGate, level));
        double bandT = (clamped - min) / (double) (nextGate - min);
        double bandEase = 0.78 + 0.22 * Math.max(0.0, Math.min(1.0, bandT));
        double overEase = 1.0;
        if (level > nextGate) {
            overEase = Math.sqrt(nextGate / (double) level);
            overEase = Math.max(0.40, Math.min(1.0, overEase));
        }
        double ease = bandEase * overEase;
        // T4+ veterans (50k+ DMZ): extra relief — telemetry retunes overshot mid/high tiers.
        if (tier >= 4 && level >= 50_000L) {
            double vet = Math.pow(50_000.0 / level, 0.25);
            vet = Math.max(0.70, Math.min(1.0, vet));
            ease *= vet;
        }
        return Math.max(0.35, Math.min(1.0, ease));
    }

    /** Blend a floor toward {@code base} when {@link #paintEase} &lt; 1. */
    private double easedFloor(double base, double floor) {
        if (floor <= base + 1e-6 || paintEase >= 0.999) {
            return Math.max(base, floor);
        }
        return base + (floor - base) * paintEase;
    }

    public boolean active() {
        return activeTier > 0 && tierPercent > 0.0;
    }

    public UnlockTier unlockTier() {
        return UnlockTier.byId(activeTier);
    }

    /** True when {@code stat} is among the player's top invested combat stats. */
    public boolean isTopStat(WeakStat stat) {
        if (stat == null || stat == WeakStat.NONE || topStats.length == 0) {
            return false;
        }
        for (WeakStat top : topStats) {
            if (top == stat) {
                return true;
            }
        }
        return false;
    }

    /** Compact label for GUI/staff: top-2 ({@code PWR>ENE}) or {@code —}. */
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
            case ENERGY -> "ENE";
            case NONE -> "?";
        };
    }

    /**
     * Target mob HP — soft VIT share with an offense durability floor so STR/PWR
     * dumps cannot one-punch packs. Ki Infusion / Potential Unlock raise sponge
     * when those skills are trained. Scaled by {@link DifficultyConfig#mobHealthScale}.
     */
    public double targetMobHealth(DifficultyConfig cfg) {
        // Soft VIT × tier%.
        double vitShare = maxHealth * tierPercent;
        double base = vitShare;
        // Early-tier floor so T1 packs aren't wet paper when VIT is still low.
        if (activeTier >= 1 && activeTier <= 2 && formBoost > 1.12) {
            double floor = maxHealth * (activeTier == 1 ? 0.20 : 0.28);
            base = Math.max(base, floor);
        }
        // Durability floor from soft offense — STR/PWR dumps must still trade hits.
        // Glass cannons (offense >> VIT) get a higher VIT-share pad; pure tanks stay bounded.
        if (offense > maxHealth * 0.30) {
            double hits = switch (activeTier) {
                case 1 -> 1.00;
                case 2 -> 0.90;
                case 3 -> 0.78;
                case 4 -> 0.68;
                case 5 -> 0.58;
                case 6 -> 0.52;
                default -> 0.48;
            };
            double durability = offense * tierPercent * hits;
            double offenseVitRatio = offense / Math.max(1.0, maxHealth);
            double vitCapMul = 3.2;
            if (offenseVitRatio > 1.15) {
                vitCapMul = Math.min(8.0, 3.2 + (offenseVitRatio - 1.15) * 1.05);
            }
            double vitCap = vitShare * vitCapMul;
            base = Math.max(base, Math.min(durability, vitCap));
        }
        // Hard cap: transforms must not invent drastic HP — but glass offense
        // still keeps a sponge floor near soft durability.
        double formPad = 1.0;
        if (formBoost > 1.12) {
            formPad = 1.0 + 0.40 * Math.min(1.0, Math.log(formBoost) / Math.log(80.0));
        }
        double hardCap = maxHealth * Math.max(tierPercent, 0.20) * formPad * 1.55;
        if (offense > maxHealth * 1.15) {
            double glassHits = switch (activeTier) {
                case 1 -> 0.82;
                case 2 -> 0.72;
                case 3 -> 0.62;
                case 4 -> 0.54;
                case 5 -> 0.48;
                case 6 -> 0.44;
                default -> 0.40;
            };
            double glassHard = offense * tierPercent * glassHits;
            hardCap = Math.max(hardCap, Math.min(glassHard, vitShare * 8.0));
        }
        if (base > hardCap) {
            base = hardCap;
        }
        // Skill sponge: Ki Infusion (+2.5%/lvl outgoing in DMZ) → packs need more HP.
        // Potential Unlock raises form access — mild extra sponge while transformed.
        double skillHp = 1.0
                + Math.min(0.22, kiInfusionLevel * 0.022)
                + (kiInfusionActive ? 0.06 : 0.0)
                + (formBoost > 1.12 ? Math.min(0.12, potentialUnlockLevel / 30.0 * 0.12) : 0.0);
        base *= skillHp;

        double overlay = 1.0;
        if (cfg.enableStrongStatCounters) {
            overlay *= blendCounter(strongStatHealthBias(cfg));
        }
        if (cfg.enableClassCounters) {
            overlay *= blendCounter(classHealthBias(cfg));
        }
        overlay = Math.min(1.20, clampCounterOverlay(overlay, cfg));
        double scale = cfg == null ? 1.05 : Math.max(0.05, Math.min(2.0, cfg.mobHealthScale));
        return Math.max(10.0, base * overlay * scale);
    }

    /**
     * Target mob attack — soft offense × tier%, with VIT/RES floors so tank dumps
     * feel the ladder, then hit-capped against a soft↔live HP blend so god forms
     * cannot out-tank packs after DMZ DEF mitigation.
     */
    public double targetMobDamage(DifficultyConfig cfg) {
        double offenseShare = offense * tierPercent;
        double base = offenseShare;

        // Live tank floors — VIT/RES dumps must still feel tier pressure.
        // Early tiers damp floors so 21–42% ladders stay near raw offense share.
        double floorStrength = Math.max(0.35, Math.min(1.0, counterStrength()));
        double defRatio = cfg == null ? 0.45 : Math.max(0.0, Math.min(10.0, cfg.tankDamageDefenseRatio));
        // Stock 0.28 — VIT dumps / tanks keep climbing T3→T6 (telemetry 1.0.19).
        double hpRatio = cfg == null ? 0.28 : Math.max(0.0, Math.min(1.0, cfg.tankDamageHealthRatio));
        double defFloor = defense * tierPercent * defRatio * floorStrength;
        // Floor against the live-aware bag so high-VIT / tank class still get pressed.
        // HP floor uses a higher early-tier floorStrength floor so T1–T2 dumps aren't free.
        // 1.0.19: 0.65→0.80 was too harsh — veterans at 100k+ were 2-shot at T3+.
        double hpFloorStrength = Math.max(0.60, floorStrength);
        double hpFloor = hitCapHealth() * tierPercent * hpRatio * hpFloorStrength;
        base = easedFloor(base, Math.max(defFloor, hpFloor));

        // T1–T3 + transformed: pre-telemetry god floors (1.0.12) — Aug retunes overshot.
        if (activeTier >= 1 && activeTier <= 3 && formBoost > 1.12) {
            double threatPct = switch (activeTier) {
                case 1 -> 0.35;
                case 2 -> 0.42;
                case 3 -> 0.48;
                default -> 0.0;
            };
            double softFloor = offense * threatPct;
            if (formBoost >= 6.0) {
                double megaT = megaFormT(formBoost);
                // Less mega compression — high forms must still raise bag pressure.
                double shareMul = 1.55 - 0.08 * Math.min(1.25, megaT);
                softFloor = Math.min(softFloor, offenseShare * Math.max(1.25, shareMul));
            }
            base = easedFloor(base, softFloor);
        }

        // T4–T7 form nudges — restore 1.0.12 ladder (telemetry 2.3.129 overshot).
        if (activeTier >= 4 && formBoost > 1.12) {
            double nudge = switch (activeTier) {
                case 4 -> 1.06;
                case 5 -> 1.10;
                case 6 -> 1.14;
                default -> 1.18;
            };
            base = Math.max(base, offenseShare * nudge);
        }

        // Live-offense pressure: soft-curve alone under-represents god forms. Pull a
        // bounded slice of live offense×tier% so transforms actually raise threat.
        if (formBoost > 1.12 && liveOffense > offense * 1.05) {
            // 2.3.129: T6 liveShare above T5 so god packs climb after T6 buy.
            double liveShare = switch (activeTier) {
                case 1 -> 0.22;
                case 2 -> 0.28;
                case 3 -> 0.34;
                case 4 -> 0.42;
                case 5 -> 0.50;
                case 6 -> 0.55;
                default -> 0.58;
            };
            // Mega forms: more of the live slice (still hit-capped after).
            double megaBoost = formBoost >= 6.0
                    ? 1.0 + 0.35 * Math.min(1.0, megaFormT(formBoost))
                    : 1.0;
            // T7: damp mega live-slice so ×50 forms aren't free one-shots before hit-cap.
            if (activeTier >= 7 && formBoost >= 6.0) {
                megaBoost = 1.0 + 0.18 * Math.min(1.0, megaFormT(formBoost));
            }
            double liveFloor = liveOffense * tierPercent * liveShare * megaBoost;
            base = easedFloor(base, liveFloor);
        }

        double overlay = 1.0;
        if (cfg.enableStrongStatCounters) {
            overlay *= blendCounter(strongStatDamageBias(cfg));
        }
        if (cfg.enableClassCounters) {
            overlay *= blendCounter(classDamageBias(cfg));
        }
        overlay = clampCounterOverlay(overlay, cfg);
        base = Math.max(1.0, base * overlay);

        // Hit-cap against soft↔live HP blend — sized so post-DEF (~65% mit cap) still bites.
        double hitCap = hitCapHealth() * kiProtectionHitFrac();
        if (base > hitCap) {
            base = hitCap;
        }

        // DMZ hard-cancels when flatMitigation >= damage × threshold (stock 2.5).
        // Pierce T4+ always; T3 god-forms too (telemetry: tanks lived only on safety-net
        // at ~7–13% bag because T1–T3 never cleared the cancel bar).
        double thr = dmzCancelMitigationThreshold();
        if (liveFlatMitigation > 1.0 && thr > 1.0 && base * thr <= liveFlatMitigation) {
            double pierce = liveFlatMitigation / thr * 1.08;
            boolean allowPierce = activeTier >= 4
                    || (activeTier >= 3 && formBoost >= 6.0);
            if (allowPierce) {
                base = Math.max(base, pierce);
            }
        }
        // 1.0.25: never paint ATK above the live incoming soft-cap.
        // Unclamped pierce on high-DEF god forms produced hitFrac ≫ 0.75 (race/form
        // audit) while LivingDamageEvent would soft-cap anyway — kiblasts/explosions
        // still read the inflated ATTACK_DAMAGE. Match the event ceiling here.
        double bagCap = Math.max(20.0, liveMaxHealth) * incomingSoftCapFrac();
        if (base > bagCap) {
            base = bagCap;
        }
        // DMZ level ease — veterans above tier gate should not be 2-shot.
        if (paintEase < 0.999) {
            double rawShare = offense * tierPercent;
            if (base > rawShare + 1e-6) {
                // Floor-bound: blend tank/god floors down toward offense share.
                base = rawShare + (base - rawShare) * paintEase;
            } else if (progressionDmzLevel > tierBandTop(cfg, activeTier)) {
                // Cap-bound veteran (past tier band top): scale capped damage down.
                base = Math.max(1.0, base * paintEase);
            }
        }
        return Math.max(1.0, base);
    }

    /** Same progressive ceilings as {@code DifficultyEvents.onDamageDone}. */
    public double incomingSoftCapFrac() {
        // 2.3.161: roll back Aug telemetry inflation — T5/T7 were 2-hit deaths.
        double base = switch (activeTier) {
            case 7 -> 0.52;
            case 6 -> 0.48;
            case 5 -> 0.44;
            case 4 -> 0.40;
            case 3 -> 0.36;
            case 2 -> 0.32;
            default -> 0.30; // T1
        };
        double relief = defensivePaintRelief();
        if (relief > 1e-6) {
            base *= Math.max(0.78, 1.0 - relief * 0.90);
        }
        return base;
    }

    /**
     * Post-mitigation HP restored when DMZ hard-cancels a hit.
     * <p>
     * 2.3.148 (uploads/live-telemetry-2026-09-02-retune, Aug31–Sep2): after fp40,
     * KP8+ gods still flat — JarebearT T3→T4 +0.027 (0.284→0.311); Rogerio T5→T6
     * +0.027 (0.473→0.500). Lift T4 landFrac toward soft-cap; raise T6 soft/landCap
     * so the T5→T6 buy can move. Soft-caps stay monotonic T1→T7.
     */
    public double targetLandingDamage(DifficultyConfig cfg) {
        double liveBag = Math.max(20.0, liveMaxHealth);
        double blendBag = hitCapHealth();
        // Prefer live HP so god-form restores read as real bag % in telemetry.
        double bag = Math.max(blendBag, liveBag * 0.90);
        // Tier landing fractions — progressive ladder from live claimed hits.
        double landFrac = switch (activeTier) {
            case 1 -> 0.13;
            case 2 -> 0.16;
            case 3 -> 0.20;
            case 4 -> 0.28;
            case 5 -> 0.33;
            case 6 -> 0.38;
            default -> 0.42;
        };
        if (formBoost > 1.12) {
            double t = Math.min(1.0, Math.log(Math.max(1.12, formBoost)) / Math.log(80.0));
            // Early tiers: lighter form bump (T2 gods overshot soft-cap via kits).
            double bump = activeTier <= 2 ? 0.06 : 0.12;
            landFrac *= 1.0 + bump * t;
        }
        double land = bag * landFrac;
        // KP (when trained) still matters on the safety-net path.
        // 1.0.24: 1.5%/lvl (KP10 ≈ 15% save) — live aggregate showed KP barely moving pressure.
        if (kiProtectionLevel > 0) {
            land *= Math.max(0.65, 1.0 - kiProtectionLevel * 0.015);
        }
        // Floor: never a free tap; cap: never a free one-shot.
        double minLand = liveBag * Math.max(0.05, tierPercent * 0.08);
        land = easedFloor(minLand, land);
        // Early-tier caps tight; mid/high climb with buys.
        double landCap = switch (activeTier) {
            case 1 -> 0.18;
            case 2 -> 0.22;
            case 3 -> 0.26;
            case 4 -> 0.32;
            case 5 -> 0.36;
            case 6 -> 0.40;
            default -> 0.44;
        };
        land = Math.min(land, liveBag * landCap);
        if (paintEase < 0.999) {
            land *= paintEase;
        }
        return Math.max(1.0, land);
    }

    /** Stock DMZ {@code cancelDamageMitigationThreshold} (2.5). */
    public static double dmzCancelMitigationThreshold() {
        try {
            var combat = com.dragonminez.common.config.ConfigManager.getCombatConfig();
            if (combat != null) {
                double t = combat.getCancelDamageMitigationThreshold();
                if (t > 1.0 && t < 20.0 && !Double.isNaN(t) && !Double.isInfinite(t)) {
                    return t;
                }
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return 2.5;
    }

    /**
     * HP used for the VIT hit-cap. Blends soft VIT toward live max health as forms
     * climb — god forms inflate the real bag far above soft peel, and capping only
     * on soft let transformed players out-tank packs.
     */
    private double hitCapHealth() {
        double soft = Math.max(20.0, maxHealth);
        double live = Math.max(soft, liveMaxHealth);
        double blend;
        if (formBoost <= 1.12) {
            blend = 0.20; // equipment / small buffs
        } else {
            // ×1.12→~0.30, ×6→~0.55, ×80→0.75
            double t = Math.min(1.0, Math.log(Math.max(1.12, formBoost)) / Math.log(80.0));
            blend = 0.30 + 0.45 * t;
        }
        return soft + (live - soft) * blend;
    }

    /**
     * Max fraction of {@link #hitCapHealth()} a single mob hit may deal
     * (pre-RES / ki protect).
     * <p>
     * Sized for DMZ adaptive DEF (up to ~65% mitigation): a T5 transformed hit at
     * ~50% of the blend bag lands ~17% after worst-case DEF — KP then shaves more.
     * Base form uses a lower formFactor so transforming still raises pressure.
     */
    private double kiProtectionHitFrac() {
        // 1.0.28 telemetry: T1 bite up; T6 budget clears T5 soft-cap glue.
        double tierFrac = switch (activeTier) {
            case 1 -> 0.22;
            case 2 -> 0.28;
            case 3 -> 0.34;
            case 4 -> 0.40;
            case 5 -> 0.44;
            case 6 -> 0.48;
            default -> 0.52;
        };
        double formFactor;
        if (formBoost <= 1.12) {
            formFactor = 0.78; // base — leave headroom for transforms
        } else {
            // ×1.12→~0.78, ×6→~0.90, ×80→1.0
            double t = Math.min(1.0, Math.log(Math.max(1.12, formBoost)) / Math.log(80.0));
            formFactor = 0.78 + 0.22 * t;
            // Extreme forms at T7: don't grow the hit budget with form (soft-cap).
            if (activeTier >= 7 && formBoost >= 25.0) {
                formFactor = Math.min(formFactor, 0.88);
            }
        }
        double frac = tierFrac * formFactor;
        // KP trained: lower pre-DEF hit budget (stacks with landing + post-mit KP).
        if (kiProtectionLevel > 0) {
            frac *= Math.max(0.85, 1.0 - kiProtectionLevel * 0.010);
        }
        if (defenseMitigationRelief > 1e-6) {
            frac *= Math.max(0.85, 1.0 - defenseMitigationRelief * 0.75);
        }
        return Math.max(0.12, Math.min(0.75, frac));
    }

    /**
     * Combined paint relief from Ki Protection + live DMZ mitigation (DEF/enchants).
     * Capped so unprotected builds still feel tier pressure.
     */
    private double defensivePaintRelief() {
        double relief = defenseMitigationRelief;
        if (kiProtectionLevel > 0) {
            relief += kiProtectionLevel * 0.010;
        }
        return Math.min(0.22, relief);
    }

    /**
     * Probe DMZ post-mitigation to reward defense/enchant investments at paint time.
     * Uses {@code calculatePostMitigationDamage} so enchant DEF is included.
     */
    private static double estimateMitigationRelief(StatsData data, int kpLevel) {
        if (data == null) {
            return 0.0;
        }
        try {
            double probe = 10_000.0;
            double kpMit = Math.max(0.0, Math.min(0.10, kpLevel * kiProtectionMitigationPerLevel()));
            double post = data.calculatePostMitigationDamage(probe, false, kpMit);
            if (!(post >= 0.0) || Double.isNaN(post) || Double.isInfinite(post)) {
                return 0.0;
            }
            double mitigated = 1.0 - Math.min(1.0, post / probe);
            // Even builds ~45–55% mit; reward excess above that band.
            double excess = Math.max(0.0, mitigated - 0.68);
            return Math.min(0.12, excess * 0.28);
        } catch (Throwable ignored) {
            return 0.0;
        }
    }

    /** Stock DMZ {@code kiProtectionMitigationPerLevel} (0.01). */
    private static double kiProtectionMitigationPerLevel() {
        try {
            var combat = com.dragonminez.common.config.ConfigManager.getCombatConfig();
            if (combat != null) {
                double v = combat.getKiProtectionMitigationPerLevel();
                if (v > 0.0 && v < 0.05 && !Double.isNaN(v) && !Double.isInfinite(v)) {
                    return v;
                }
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return 0.01;
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
                overlay *= blendCounter(Math.max(1.0, 1.0 + (cfg.classCounterArmorMult - 1.0) * 0.55));
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
     * Full strength from T4 (90% tier%) — T3 was full at 65% and wiped fresh buys.
     */
    private double counterStrength() {
        return Math.max(0.0, Math.min(1.0, tierPercent / 0.90));
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
     * Driven by fighting class + top-2 combat stats.
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
            scale *= kitScaleForStat(topStats[0]);
            if (topStats.length > 1) {
                // Second top-stat: half the kit-cadence pull.
                double second = kitScaleForStat(topStats[1]);
                scale *= 1.0 + (second - 1.0) * 0.50;
            }
        }
        // Blend toward 1.0 when still ramping into mid tiers.
        scale = 1.0 + (scale - 1.0) * strength;
        return Math.max(0.90, Math.min(1.0, scale));
    }

    private static double kitScaleForStat(WeakStat stat) {
        return switch (stat == null ? WeakStat.NONE : stat) {
            case STRENGTH, STRIKE -> 0.97;
            case KI_POWER, ENERGY -> 0.96;
            case DEFENSE, VITALITY -> 0.98;
            case NONE -> 1.0;
        };
    }

    // ── Strong-stat (top-2) biases ─────────────────────────────────────────

    private double strongStatHealthBias(DifficultyConfig cfg) {
        return combineTopStatBiases(cfg, this::healthBiasForStat);
    }

    private double strongStatDamageBias(DifficultyConfig cfg) {
        return combineTopStatBiases(cfg, this::damageBiasForStat);
    }

    private double strongStatArmorBias(DifficultyConfig cfg) {
        return combineTopStatBiases(cfg, this::armorBiasForStat);
    }

    private double combineTopStatBiases(DifficultyConfig cfg, java.util.function.Function<WeakStat, Double> biasFn) {
        if (topStats.length == 0) {
            return 1.0;
        }
        double primary = Math.max(1.0, biasFn.apply(topStats[0]));
        if (topStats.length < 2 || topStats[1] == null || topStats[1] == WeakStat.NONE) {
            return primary;
        }
        double secondary = Math.max(1.0, biasFn.apply(topStats[1]));
        // Top-1 full bump + 60% of top-2 bump (capped by maxCounterOverlay later).
        return 1.0 + (primary - 1.0) + (secondary - 1.0) * 0.60;
    }

    private double healthBiasForStat(WeakStat top) {
        double mult = Math.max(1.0, DifficultyConfig.get().strongStatCounterMult);
        if (top == null || top == WeakStat.NONE) {
            return 1.0;
        }
        double bump = mult - 1.0;
        return switch (top) {
            case STRENGTH, STRIKE -> 1.0 + bump * 0.70;
            case KI_POWER, ENERGY -> 1.0 + bump * 0.85;
            case DEFENSE, VITALITY -> 1.0 + bump * 0.30;
            case NONE -> 1.0;
        };
    }

    private double damageBiasForStat(WeakStat top) {
        double mult = Math.max(1.0, DifficultyConfig.get().strongStatCounterMult);
        if (top == null || top == WeakStat.NONE) {
            return 1.0;
        }
        double bump = mult - 1.0;
        return switch (top) {
            case STRENGTH, STRIKE -> 1.0 + bump * 0.55;
            case KI_POWER, ENERGY -> 1.0 + bump * 0.65;
            case DEFENSE -> 1.0 + bump * 0.85;
            case VITALITY -> 1.0 + bump * 0.75;
            case NONE -> 1.0;
        };
    }

    private double armorBiasForStat(WeakStat top) {
        double mult = Math.max(1.0, DifficultyConfig.get().strongStatCounterMult);
        if (top == null || top == WeakStat.NONE) {
            return 1.0;
        }
        double bump = mult - 1.0;
        return switch (top) {
            case STRENGTH, STRIKE -> 1.0 + bump * 0.80;
            case KI_POWER, ENERGY -> 1.0 + bump * 0.40;
            case DEFENSE, VITALITY -> 1.0 + bump * 0.20;
            case NONE -> 1.0;
        };
    }

    // ── Class biases ──────────────────────────────────────────────────────

    private double classDamageBias(DifficultyConfig cfg) {
        double mult = Math.max(1.0, cfg.classCounterDamageMult);
        String cls = fightingClass == null ? "" : fightingClass.toLowerCase(Locale.ROOT);
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
        if (cls.contains("spirit") || cls.contains("cleric") || cls.contains("mage")
                || cls.contains("kiuser") || cls.contains("energy")) {
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
        String cls = fightingClass == null ? "" : fightingClass.toLowerCase(Locale.ROOT);
        if (cls.contains("spirit") || cls.contains("cleric") || cls.contains("mage")
                || cls.contains("kiuser") || cls.contains("energy")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 0.90);
        }
        if (cls.contains("martial") || cls.contains("berserk") || cls.contains("warrior")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 0.55);
        }
        if (cls.contains("tank") || cls.contains("paladin")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 0.35);
        }
        return switch (style) {
            case KI -> Math.max(1.0, 1.0 + (mult - 1.0) * 0.85);
            case STRIKE, MELEE -> Math.max(1.0, 1.0 + (mult - 1.0) * 0.45);
            case TANK -> Math.max(1.0, 1.0 + (mult - 1.0) * 0.35);
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
            double ki,
            double energy,
            double formBoost
    ) {
        // Prefer raw invested stats; strip form mult so transform doesn't reshuffle counters.
        double peel = Math.max(1.0, formBoost);
        double str = melee;
        double skp = strike;
        double res = defense;
        double vit = health;
        double pwr = ki;
        double ene = Math.max(1.0, energy * ENERGY_OFFENSE_FACTOR);
        if (data != null) {
            try {
                Stats stats = data.getStats();
                if (stats != null) {
                    str = Math.max(1.0, stats.getStrength())
                            * Math.max(0.01, data.getTotalMultiplier("STR")) / peel;
                    skp = Math.max(1.0, stats.getStrikePower())
                            * Math.max(0.01, data.getTotalMultiplier("SKP")) / peel;
                    res = Math.max(1.0, stats.getResistance())
                            * Math.max(0.01, data.getTotalMultiplier("RES")) / peel;
                    vit = Math.max(1.0, stats.getVitality())
                            * Math.max(0.01, data.getTotalMultiplier("VIT")) / peel;
                    pwr = Math.max(1.0, stats.getKiPower())
                            * Math.max(0.01, data.getTotalMultiplier("PWR")) / peel;
                    ene = Math.max(1.0, stats.getEnergy())
                            * Math.max(0.01, data.getTotalMultiplier("ENE")) / peel;
                }
            } catch (Throwable ignored) {
            }
        }
        double peak = Math.max(str, Math.max(skp, Math.max(res, Math.max(vit, Math.max(pwr, ene)))));
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
                new Ranked(WeakStat.KI_POWER, pwr / peak),
                new Ranked(WeakStat.ENERGY, ene / peak)
        };
        Arrays.sort(ranked, Comparator.comparingDouble((Ranked r) -> r.norm).reversed());

        // Combat counters use the top 2 invested combat stats.
        int topCount = Math.min(2, ranked.length);
        WeakStat[] top = new WeakStat[topCount];
        for (int i = 0; i < topCount; i++) {
            top[i] = ranked[i].stat;
        }

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
        // Fixed-point so soft form deltas under 0.5 still invalidate paint.
        h = mix(h, Math.round(melee * 100.0));
        h = mix(h, Math.round(strike * 100.0));
        h = mix(h, Math.round(ki * 100.0));
        h = mix(h, Math.round(def * 100.0));
        h = mix(h, Math.round(hp * 100.0));
        h = mix(h, Math.round(release * 100.0));
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
        // Include live combat knobs so admin retunes invalidate paint.
        DifficultyConfig liveCfg = DifficultyConfig.get();
        h = mix(h, Math.round(liveCfg.strongStatCounterMult * 1000.0));
        h = mix(h, Math.round(liveCfg.classCounterDamageMult * 1000.0));
        h = mix(h, Math.round(liveCfg.classCounterHealthMult * 1000.0));
        h = mix(h, Math.round(liveCfg.classCounterArmorMult * 1000.0));
        h = mix(h, Math.round(liveCfg.maxCounterOverlayMult * 1000.0));
        h = mix(h, Math.round(liveCfg.mobHealthScale * 1000.0));
        h = mix(h, Math.round(liveCfg.tankDamageDefenseRatio * 1000.0));
        h = mix(h, Math.round(liveCfg.tankDamageHealthRatio * 1000.0));
        h = mix(h, Math.round(liveCfg.transformScaleWeight * 1000.0));
        h = mix(h, Math.round(liveCfg.transformScaleExponent * 1000.0));
        h = mix(h, Math.round(liveCfg.defenseToArmorFactor * 1000.0));
        h = mix(h, Math.round(liveCfg.eliteStatMultiplier * 1000.0));
        h = mix(h, Math.round(liveCfg.bossStatMultiplier * 1000.0));
        h = mix(h, Math.round(liveCfg.maxScaledHealth * 10.0));
        h = mix(h, Math.round(liveCfg.maxDamageMultiplier * 1000.0));
        h = mix(h, liveCfg.enableClassCounters ? 1L : 0L);
        h = mix(h, liveCfg.enableStrongStatCounters ? 1L : 0L);
        h = mix(h, liveCfg.paintEpoch());
        // Formula revision: Aug 29–30 early soft-cap + landing ease (2.3.57).
        h = mix(h, 45L); // 2.3.163 KP hit-cap + DEF/enchant paint relief
        h = mix(h, Math.round(CombatSanity.maxFormBoost() * 10.0));
        return h;
    }

    @FunctionalInterface
    private interface StatRead {
        double get() throws Throwable;
    }

    private static double readStat(StatRead read, double fallback) {
        try {
            double v = read.get();
            if (!(v > 0.0) || Double.isNaN(v) || Double.isInfinite(v)) {
                return fallback;
            }
            return Math.max(fallback > 1.0 ? 1.0 : fallback, v);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    /** True when DMZ reports an active form / stack form (works before multipliers resolve). */
    private static boolean isDmzFormActive(StatsData data) {
        if (data == null) {
            return false;
        }
        try {
            Character ch = data.getCharacter();
            if (ch == null) {
                return false;
            }
            return ch.hasActiveForm() || ch.hasActiveStackForm();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Peak form⊕stack across combat channels (STR/SKP/PWR/ENE/RES/VIT). */
    private static double formMultiplierBoost(StatsData data) {
        if (data == null) {
            return 1.0;
        }
        double peak = 1.0;
        for (String key : new String[] {"STR", "SKP", "PWR", "ENE", "RES", "VIT"}) {
            double combined = statFormMultiplier(data, key);
            if (combined > peak) {
                peak = combined;
            }
        }
        return peak;
    }

    /** Soft offense blend: STR/SKP/PWR + mild ENE pool share. */
    private static double blendedOffense(double melee, double strike, double ki, double energy) {
        double m = Math.max(1.0, melee);
        double s = Math.max(1.0, strike);
        double k = Math.max(1.0, ki);
        double e = Math.max(1.0, energy * ENERGY_OFFENSE_FACTOR);
        double peak = Math.max(m, Math.max(s, Math.max(k, e)));
        double avg = (m + s + k + e) * 0.25;
        return peak * 0.55 + avg * 0.45;
    }

    /** Peel one live channel by its form⊕stack factor, with baseline fallback. */
    private static double peelChannel(double live, double formMult, double baseline, boolean inForm) {
        if (formMult > 1.08) {
            return Math.max(1.0, live / formMult);
        }
        if (inForm && baseline > 0.0) {
            return Math.max(1.0, baseline);
        }
        return Math.max(1.0, live);
    }

    /** Offense form ratio from a base-form baseline (melee/strike/ki/energy). */
    private static double estimateOffenseFormBoost(
            double liveMelee, double baseMelee,
            double liveStrike, double baseStrike,
            double liveKi, double baseKi,
            double liveEnergy, double baseEnergy
    ) {
        double rMelee = liveMelee / Math.max(1.0, baseMelee);
        double rStrike = liveStrike / Math.max(1.0, baseStrike);
        double rKi = liveKi / Math.max(1.0, baseKi);
        double rEne = liveEnergy / Math.max(1.0, baseEnergy);
        double boost = (rMelee + rStrike + rKi + rEne) * 0.25;
        if (!(boost > 0.0) || Double.isNaN(boost) || Double.isInfinite(boost)) {
            return 1.0;
        }
        return CombatSanity.saneFormMult(boost);
    }

    /** Form⊕stack for one combat channel (1.0 when unavailable / base). */
    private static double statFormMultiplier(StatsData data, String key) {
        if (data == null || key == null) {
            return 1.0;
        }
        try {
            boolean multiply = dmzMultiplicationMode();
            double form = Math.max(0.0, data.getFormMultiplier(key));
            double stack = Math.max(0.0, data.getStackFormMultiplier(key));
            return CombatSanity.saneFormMult(combineDmzMults(form, stack, multiply));
        } catch (Throwable ignored) {
            return 1.0;
        }
    }

    /**
     * Match {@link com.dragonminez.common.stats.StatsData#getTotalMultiplier} form+stack fold.
     * Addition (default here): {@code 1 + (a-1) + (b-1)}. Multiply: {@code a * b}.
     */
    static double combineDmzMults(double a, double b, boolean multiply) {
        double fa = a > 0.0 ? a : 1.0;
        double fb = b > 0.0 ? b : 1.0;
        if (multiply) {
            return Math.max(1.0, Math.max(fa, 1.0) * Math.max(fb, 1.0));
        }
        // Tiny sub-1 bonuses (rare) — treat as +fraction when the other side is idle.
        if (fa > 0.0 && fa < 1.0 && fb <= 1.0) {
            return 1.0 + fa;
        }
        if (fb > 0.0 && fb < 1.0 && fa <= 1.0) {
            return 1.0 + fb;
        }
        return Math.max(0.01, 1.0 + (Math.max(fa, 1.0) - 1.0) + (Math.max(fb, 1.0) - 1.0));
    }

    private static boolean dmzMultiplicationMode() {
        try {
            var server = com.dragonminez.common.config.ConfigManager.getServerConfig();
            if (server == null) {
                return false;
            }
            var gameplay = server.getGameplay();
            if (gameplay == null) {
                return false;
            }
            Boolean flag = gameplay.getMultiplicationInsteadOfAdditionForMultipliers();
            return Boolean.TRUE.equals(flag);
        } catch (Throwable ignored) {
            // This pack ships addition mode (false) — safer default than product inflation.
            return false;
        }
    }

    private record FormBaseline(
            double melee, double strike, double ki, double energy, double def, double hp, long atMs, String race
    ) {}

    /** Mega-form compress anchor: log-lerp from ×6 → ×80 (works past ×80 via megaT&gt;1). */
    private static final double MEGA_FORM_START = 6.0;
    private static final double MEGA_FORM_TARGET = 80.0;

    /**
     * 0 at {@link #MEGA_FORM_START}, 1 at {@link #MEGA_FORM_TARGET}, can exceed 1 past ×80.
     */
    private static double megaFormT(double formBoost) {
        if (!(formBoost >= MEGA_FORM_START)) {
            return 0.0;
        }
        double denom = Math.log(MEGA_FORM_TARGET / MEGA_FORM_START);
        if (!(denom > 0.0)) {
            return 0.0;
        }
        return Math.log(formBoost / MEGA_FORM_START) / denom;
    }

    /** Soft-curve exponent ceiling as mega-forms grow (×6→0.58, ×80→0.42, ×100≈0.38). */
    private static double megaFormExpCap(double megaT) {
        double t = Math.max(0.0, megaT);
        if (t <= 1.0) {
            return 0.58 - 0.16 * t;
        }
        return Math.max(0.36, 0.42 - 0.08 * (t - 1.0));
    }

    /** Offense weight scale vs twBase (×6→1.0, ×80→0.82, floor 0.72). */
    private static double megaFormTwScale(double megaT) {
        double t = Math.max(0.0, Math.min(1.25, megaT));
        return Math.max(0.72, 1.0 - 0.18 * Math.min(1.0, t));
    }

    /** Bulk (HP/DEF) weight scale vs twBase — higher than offense, still compresses mega. */
    private static double megaFormBulkTwScale(double megaT) {
        double t = Math.max(0.0, Math.min(1.25, megaT));
        return Math.max(0.72, 1.15 - 0.35 * Math.min(1.0, t)); // ×6→1.15, ×80→0.80
    }

    /** Bulk soft-curve exponent (×6→0.85, ×80→0.55). */
    private static double megaFormBulkExp(double megaT) {
        double t = Math.max(0.0, Math.min(1.25, megaT));
        return Math.max(0.50, 0.85 - 0.30 * Math.min(1.0, t));
    }


    /**
     * Soft form blend: {@code base × (1 + (live/base - 1)^exp × weight)}.
     * Linear weight alone still exploded on 10–20× forms; the exponent compresses surplus.
     * Extreme surpluses (×50–×80) also hit a weight-scaled inherit cap.
     */
    private static double blendForm(double base, double live, double weight, double exponent) {
        double b = Math.max(1e-9, base);
        double l = Math.max(b, live);
        double w = Math.max(0.0, Math.min(1.0, weight));
        if (w <= 0.0) {
            return b;
        }
        double exp = Math.max(0.20, Math.min(1.0, exponent));
        double surplus = Math.max(0.0, l / b - 1.0);
        double seenRatio = 1.0 + Math.pow(surplus, exp) * w;
        // Cap form multiple enemies can inherit (scales with weight so admin=1 stays strong).
        double cap = 1.0 + 10.0 * w;
        if (seenRatio > cap) {
            seenRatio = cap;
        }
        return b * seenRatio;
    }

    private static long mix(long h, long v) {
        h ^= v;
        h *= 1099511628211L;
        return h;
    }
}
