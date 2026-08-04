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
 * Design (player-side counters stay in DMZ):
 * <ul>
 *   <li><b>Mob damage</b> tracks soft-blended <b>STR/SKP only</b> × tier%</li>
 *   <li><b>Mob HP</b> tracks soft <b>VIT</b> only — never drastic, never sponged off offense</li>
 *   <li><b>PWR / ENE are not scaled against</b></li>
 *   <li>Player <b>RES</b> and <b>ki protection</b> counter hits; AD must not inflate
 *       mob ATK to pierce RES or dump a full ki bar in one punch</li>
 * </ul>
 * Form boost uses a diminishing soft curve
 * ({@link DifficultyConfig#transformScaleWeight} / {@link DifficultyConfig#transformScaleExponent},
 * mega compress ×6→×80) on STR/SKP only.
 */
public final class PlayerCombatProfile {
    private static final long CACHE_TTL_MS = 250L;
    private static final Map<UUID, Cached> CACHE = new ConcurrentHashMap<>();
    /** Base-form combat snapshot — used when NoForms getters ignore custom-race forms. */
    private static final Map<UUID, FormBaseline> FORM_BASELINES = new ConcurrentHashMap<>();
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
    /** Full live form offense (pre soft-curve) — used for T1–T3 transform pressure. */
    public final double liveOffense;
    /** Full live form max HP (pre soft-curve). */
    public final double liveMaxHealth;
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
            double liveOffense,
            double liveMaxHealth,
            double formBoost,
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
        this.liveOffense = Math.max(offense, liveOffense);
        this.liveMaxHealth = Math.max(maxHealth, liveMaxHealth);
        this.formBoost = Math.max(1.0, formBoost);
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
        if (data != null) {
            // Read live channels independently — one bad custom-race getter must not
            // wipe the whole profile. PWR/ENE are read for display only, never offense.
            // CombatSanity clamps NaN / absurd values so future race JSON cannot explode AD.
            double liveMelee = CombatSanity.saneLive(readStat(() -> data.getMeleeDamage(), 1.0), 1.0);
            double liveStrike = CombatSanity.saneLive(readStat(() -> data.getStrikeDamage(), 1.0), 1.0);
            double liveKi = CombatSanity.saneLive(readStat(() -> data.getKiDamage(), 1.0), 1.0);
            double liveDef = CombatSanity.saneLive(readStat(() -> data.getDefense(), 1.0), 1.0);
            double liveHp = CombatSanity.saneLive(readStat(() -> data.getMaxHealth(), 20.0), 20.0);
            liveOffense = physicalOffense(liveMelee, liveStrike);
            liveMaxHealth = liveHp;

            String raceId = DmzProgression.race(player);
            double strForm = CombatSanity.saneFormMult(statFormMultiplier(data, "STR"));
            double skpForm = CombatSanity.saneFormMult(statFormMultiplier(data, "SKP"));
            double vitForm = CombatSanity.saneFormMult(statFormMultiplier(data, "VIT"));
            double resForm = CombatSanity.saneFormMult(statFormMultiplier(data, "RES"));
            formBoost = CombatSanity.saneFormMult(Math.max(strForm, Math.max(skpForm, Math.max(vitForm, resForm))));
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
            double baseKi = liveKi;

            if (!dmzFormActive) {
                FORM_BASELINES.put(id, new FormBaseline(
                        liveMelee, liveStrike, liveKi, liveDef, liveHp,
                        System.currentTimeMillis(), raceId == null ? "" : raceId));
                baseMelee = liveMelee;
                baseStrike = liveStrike;
                formBoost = 1.0;
            } else if (baseline != null) {
                // Custom races that bake STR/SKP outside multipliers.
                double fromBaseline = estimatePhysicalFormBoost(
                        liveMelee, baseline.melee, liveStrike, baseline.strike);
                if (fromBaseline > formBoost + 0.05) {
                    formBoost = CombatSanity.saneFormMult(fromBaseline);
                    baseMelee = Math.max(1.0, baseline.melee);
                    baseStrike = Math.max(1.0, baseline.strike);
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
            ki = baseKi; // unused for scaling — leave unboosted
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
        // STR/SKP only — PWR/ENE never feed mob damage or HP.
        double offense = physicalOffense(melee, strike);
        // Counter identity: STR/SKP/RES/VIT only (no PWR).
        StatBalance balance = resolveBalance(data, melee, strike, def, hp, formBoost);
        String fightingClass = DmzProgression.fightingClass(player);
        String race = DmzProgression.race(player);
        FightingStyle style = resolveStyle(fightingClass, melee, strike, ki, def, hp);
        long sig = fingerprint(
                tier, pct, melee, strike, ki, def, hp, release,
                balance.weakest, balance.imbalance, balance.topStats,
                fightingClass, race, style
        );
        sig = mix(sig, Math.round(formBoost * 100.0));
        sig = mix(sig, Math.round(liveOffense));
        return new PlayerCombatProfile(
                tier, pct, melee, strike, ki, def, hp, offense,
                liveOffense, liveMaxHealth, formBoost, release,
                balance.weakest, balance.imbalance, balance.topStats,
                fightingClass, race, style, sig
        );
    }

    private static PlayerCombatProfile inactive() {
        return new PlayerCombatProfile(
                0, 0.0, 1.0, 1.0, 1.0, 1.0, 20.0, 1.0,
                1.0, 20.0, 1.0, 100.0,
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
     * Target mob HP — soft VIT share only.
     * Never sponges off STR/SKP/PWR. Player VIT is their HP pool; RES + ki protection
     * counter hits. Scaled by {@link DifficultyConfig#mobHealthScale}.
     */
    public double targetMobHealth(DifficultyConfig cfg) {
        // Soft VIT × tier% — never an offense sponge / health wall.
        double vitShare = maxHealth * tierPercent;
        double base = vitShare;
        // Tiny early-tier floor so T1 packs aren't wet paper when VIT is still low.
        if (activeTier >= 1 && activeTier <= 2 && formBoost > 1.12) {
            double floor = maxHealth * (activeTier == 1 ? 0.12 : 0.18);
            base = Math.max(base, floor);
        }
        // Mild durability floor from soft STR/SKP so ×50–×80 forms don't vaporize
        // packs in 0.01 hits — still capped ≤ ~2× soft VIT (not a drastic sponge).
        if (formBoost > 1.12 && offense > maxHealth * 0.5) {
            double hits = switch (activeTier) {
                case 1 -> 0.55;
                case 2 -> 0.50;
                case 3 -> 0.45;
                case 4 -> 0.40;
                case 5 -> 0.35;
                case 6 -> 0.32;
                default -> 0.30;
            };
            double durability = offense * tierPercent * hits;
            double vitCap = vitShare * 2.0;
            base = Math.max(base, Math.min(durability, vitCap));
        }
        // Hard cap: transforms must not invent drastic HP.
        double formPad = 1.0;
        if (formBoost > 1.12) {
            formPad = 1.0 + 0.25 * Math.min(1.0, Math.log(formBoost) / Math.log(80.0));
        }
        double hardCap = maxHealth * Math.max(tierPercent, 0.15) * formPad * 1.25;
        if (base > hardCap) {
            base = hardCap;
        }
        double overlay = 1.0;
        if (cfg.enableStrongStatCounters) {
            overlay *= blendCounter(strongStatHealthBias(cfg));
        }
        if (cfg.enableClassCounters) {
            overlay *= blendCounter(classHealthBias(cfg));
        }
        overlay = Math.min(1.12, clampCounterOverlay(overlay, cfg));
        double scale = cfg == null ? 0.65 : Math.max(0.05, Math.min(2.0, cfg.mobHealthScale));
        return Math.max(10.0, base * overlay * scale);
    }

    /**
     * Target mob attack — soft-blended STR/SKP × tier% only, then capped to a
     * VIT-relative hit fraction so higher tiers pressure ki protection without
     * dumping a full ki bar / player bag in one unprotected punch.
     */
    public double targetMobDamage(DifficultyConfig cfg) {
        double offenseShare = offense * tierPercent;
        double base = offenseShare;

        // T1–T3 + transformed: mild floor from soft-blended STR/SKP (not raw live).
        if (activeTier >= 1 && activeTier <= 3 && formBoost > 1.12) {
            double threatPct = switch (activeTier) {
                case 1 -> 0.18;
                case 2 -> 0.30;
                case 3 -> 0.40;
                default -> 0.0;
            };
            double softFloor = offense * threatPct;
            if (formBoost >= 6.0) {
                double megaT = megaFormT(formBoost);
                double shareMul = 1.15 - 0.18 * Math.min(1.25, megaT);
                softFloor = Math.min(softFloor, offenseShare * Math.max(1.0, shareMul));
            }
            base = Math.max(base, softFloor);
        }

        if (activeTier >= 4 && formBoost > 1.12) {
            double nudge = switch (activeTier) {
                case 4 -> 1.04;
                case 5 -> 1.06;
                case 6 -> 1.08;
                default -> 1.10;
            };
            base = Math.max(base, offenseShare * nudge);
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

        // VIT-relative cap — calibrated from server race/form sim so T5–T7 force
        // ki protection without one-punching the player bag before RES.
        double hitCap = maxHealth * kiProtectionHitFrac();
        if (base > hitCap) {
            base = hitCap;
        }
        return Math.max(1.0, base);
    }

    /**
     * Max fraction of player soft VIT a single mob hit may deal (pre-RES / ki protect).
     * <p>
     * Base form only uses ~55% of the tier budget so transforming still raises pressure.
     * Full mastery mega forms reach the tier ceiling (+small pad) — enough to force
     * ki protection, never a full-bag dump. Calibrated from server race/form sim.
     */
    private double kiProtectionHitFrac() {
        double tierFrac = switch (activeTier) {
            case 1 -> 0.10;
            case 2 -> 0.14;
            case 3 -> 0.18;
            case 4 -> 0.24;
            case 5 -> 0.30;
            case 6 -> 0.34;
            default -> 0.38;
        };
        double formFactor;
        if (formBoost <= 1.12) {
            formFactor = 0.55; // base — leave headroom for transforms
        } else {
            // ×1.12→~0.55, ×6→~0.72, ×80→1.0
            double t = Math.min(1.0, Math.log(Math.max(1.12, formBoost)) / Math.log(80.0));
            formFactor = 0.55 + 0.45 * t;
        }
        return Math.max(0.06, Math.min(0.42, tierFrac * formFactor));
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
            }
            // KI style: no armor pad — PWR/ENE are not scaled against.
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
                case DEFENSE, VITALITY -> 0.98;
                case KI_POWER, NONE -> 1.0; // PWR never accelerates kits
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
        if (top == WeakStat.NONE || top == WeakStat.KI_POWER) {
            return 1.0; // PWR is never scaled against
        }
        double bump = mult - 1.0;
        return switch (top) {
            case STRENGTH, STRIKE -> 1.0 + bump * 0.35; // mild bag pad only
            case DEFENSE, VITALITY -> 1.0 + bump * 0.15;
            case KI_POWER, NONE -> 1.0;
        };
    }

    private double strongStatDamageBias(DifficultyConfig cfg) {
        double mult = Math.max(1.0, cfg.strongStatCounterMult);
        WeakStat top = topStat();
        if (top == WeakStat.NONE || top == WeakStat.KI_POWER) {
            return 1.0; // PWR is never scaled against
        }
        double bump = mult - 1.0;
        return switch (top) {
            case STRENGTH, STRIKE -> 1.0 + bump * 0.55;
            // RES/VIT are player counters — do not inflate mob ATK to pierce them.
            case DEFENSE, VITALITY -> 1.0;
            case KI_POWER, NONE -> 1.0;
        };
    }

    private double strongStatArmorBias(DifficultyConfig cfg) {
        double mult = Math.max(1.0, cfg.strongStatCounterMult);
        WeakStat top = topStat();
        if (top == WeakStat.NONE || top == WeakStat.KI_POWER) {
            return 1.0;
        }
        double bump = mult - 1.0;
        return switch (top) {
            case STRENGTH, STRIKE -> 1.0 + bump * 0.50;
            case DEFENSE, VITALITY -> 1.0;
            case KI_POWER, NONE -> 1.0;
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
        // Ki / caster classes (PWR) — no damage inflation; PWR is not scaled against.
        // Match future custom class ids that contain these tokens.
        if (cls.contains("spirit") || cls.contains("cleric") || cls.contains("mage")
                || cls.contains("kiuser") || cls.contains("energy")) {
            return 1.0;
        }
        if (cls.contains("berserk")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 1.10);
        }
        if (cls.contains("tank") || cls.contains("paladin")) {
            // RES + ki protection counter tanks — never inflate mob ATK to pierce them.
            return 1.0;
        }
        if (cls.contains("warrior")) {
            return mult;
        }
        if (cls.contains("martial")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 1.00);
        }
        return switch (style) {
            case MELEE, STRIKE -> mult;
            case KI, TANK -> 1.0;
            case HYBRID -> Math.max(1.0, 1.0 + (mult - 1.0) * 0.45);
        };
    }

    private double classHealthBias(DifficultyConfig cfg) {
        // HP overlays stay mild — VIT owns bag size; never pad vs PWR/ki classes.
        double mult = Math.max(1.0, cfg.classCounterHealthMult);
        String cls = fightingClass == null ? "" : fightingClass;
        if (cls.contains("spirit") || cls.contains("cleric")) {
            return 1.0;
        }
        if (cls.contains("martial") || cls.contains("berserk") || cls.contains("warrior")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 0.35);
        }
        if (cls.contains("tank") || cls.contains("paladin")) {
            return Math.max(1.0, 1.0 + (mult - 1.0) * 0.15);
        }
        return switch (style) {
            case KI -> 1.0;
            case STRIKE, MELEE -> Math.max(1.0, 1.0 + (mult - 1.0) * 0.30);
            case TANK -> Math.max(1.0, 1.0 + (mult - 1.0) * 0.15);
            case HYBRID -> Math.max(1.0, 1.0 + (mult - 1.0) * 0.20);
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
        // Physical peak only — PWR never decides style for scaling counters.
        double peakOff = Math.max(melee, strike);
        double tankiness = Math.max(defense, health / 50.0);
        if (tankiness > peakOff * 1.15) {
            return FightingStyle.TANK;
        }
        if (melee >= strike) {
            return FightingStyle.MELEE;
        }
        if (strike > melee) {
            return FightingStyle.STRIKE;
        }
        return FightingStyle.HYBRID;
    }

    private static StatBalance resolveBalance(
            StatsData data,
            double melee,
            double strike,
            double defense,
            double health,
            double formBoost
    ) {
        // Prefer raw invested stats; strip form mult so transform doesn't reshuffle counters.
        // PWR/ENE are intentionally excluded — never top-stat for scaling.
        double peel = Math.max(1.0, formBoost);
        double str = melee;
        double skp = strike;
        double res = defense;
        double vit = health;
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
                }
            } catch (Throwable ignored) {
            }
        }
        double peak = Math.max(str, Math.max(skp, Math.max(res, vit)));
        if (!(peak > 0.0)) {
            return new StatBalance(WeakStat.NONE, 0.0, NO_TOP);
        }
        // Normalize to peak so different units stay comparable.
        record Ranked(WeakStat stat, double norm) {}
        Ranked[] ranked = {
                new Ranked(WeakStat.STRENGTH, str / peak),
                new Ranked(WeakStat.STRIKE, skp / peak),
                new Ranked(WeakStat.DEFENSE, res / peak),
                new Ranked(WeakStat.VITALITY, vit / peak)
        };
        Arrays.sort(ranked, Comparator.comparingDouble((Ranked r) -> r.norm).reversed());

        // Combat counters use only the single highest invested physical/bulk stat.
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
        // Formula revision: tank class ATK pierce removed; unused tankDamage* out of fingerprint.
        h = mix(h, 23L);
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

    /**
     * Peak form⊕stack across <b>physical/bulk</b> channels only (STR/SKP/RES/VIT).
     * PWR/ENE are never included — they are not scaled against.
     */
    private static double formMultiplierBoost(StatsData data) {
        if (data == null) {
            return 1.0;
        }
        double peak = 1.0;
        for (String key : new String[] {"STR", "SKP", "RES", "VIT"}) {
            double combined = statFormMultiplier(data, key);
            if (combined > peak) {
                peak = combined;
            }
        }
        return peak;
    }

    /** Soft STR/SKP offense blend used for mob damage (never PWR). */
    private static double physicalOffense(double melee, double strike) {
        double m = Math.max(1.0, melee);
        double s = Math.max(1.0, strike);
        double peak = Math.max(m, s);
        double avg = (m + s) * 0.5;
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

    /** Physical-only form ratio from a base-form baseline (melee/strike). */
    private static double estimatePhysicalFormBoost(
            double liveMelee, double baseMelee,
            double liveStrike, double baseStrike
    ) {
        double rMelee = liveMelee / Math.max(1.0, baseMelee);
        double rStrike = liveStrike / Math.max(1.0, baseStrike);
        double boost = (rMelee + rStrike) * 0.5;
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
            double melee, double strike, double ki, double def, double hp, long atMs, String race
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

    /** Soft-curve exponent ceiling as mega-forms grow (×6→0.48, ×80→0.34, ×100≈0.30). */
    private static double megaFormExpCap(double megaT) {
        double t = Math.max(0.0, megaT);
        if (t <= 1.0) {
            return 0.48 - 0.14 * t;
        }
        return Math.max(0.28, 0.34 - 0.08 * (t - 1.0));
    }

    /** Offense weight scale vs twBase (×6→1.0, ×80→0.72, floor 0.62). */
    private static double megaFormTwScale(double megaT) {
        double t = Math.max(0.0, Math.min(1.25, megaT));
        return Math.max(0.62, 1.0 - 0.28 * Math.min(1.0, t));
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
