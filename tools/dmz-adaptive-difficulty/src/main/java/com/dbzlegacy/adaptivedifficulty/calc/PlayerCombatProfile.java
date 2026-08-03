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
 * Live DMZ combat getters include full form multipliers. Enemy scaling blends
 * form-stripped stats with a <em>diminishing</em> slice of that transform boost
 * ({@link DifficultyConfig#transformScaleWeight} + {@link DifficultyConfig#transformScaleExponent},
 * further dampened for mega forms up to ×80+) so transforming does not linearly
 * explode mob/creeper damage — especially at T1 and T6–T7.
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
            // Read live / no-form channels independently — one bad custom-race getter
            // must not wipe the whole profile to wet-noodle defaults.
            double liveMelee = readStat(() -> data.getMeleeDamage(), 1.0);
            double liveStrike = readStat(() -> data.getStrikeDamage(), 1.0);
            double liveKi = readStat(() -> data.getKiDamage(), 1.0);
            double liveDef = readStat(() -> data.getDefense(), 1.0);
            double liveHp = readStat(() -> data.getMaxHealth(), 20.0);
            double livePeak = Math.max(liveMelee, Math.max(liveStrike, liveKi));
            double liveAvg = (liveMelee + liveStrike + liveKi) / 3.0;
            liveOffense = livePeak * 0.55 + liveAvg * 0.45;
            liveMaxHealth = liveHp;

            // NoForms / NoMultipliers strip ALL totalMult (form+stack+effects+secondary).
            // Soft-curving that ratio also compresses racial passives — wrong for custom races.
            // Peel ONLY form⊕stack so effects stay at full strength in the base channel.
            double formOnly = formMultiplierBoost(data);
            boolean dmzFormActive = isDmzFormActive(data) || formOnly > 1.12;

            UUID id = player.m_20148_();
            FormBaseline baseline = FORM_BASELINES.get(id);

            double baseMelee;
            double baseStrike;
            double baseKi;
            if (formOnly > 1.08) {
                // Live / formOnly keeps effects+secondary in the base (full tier% scale).
                baseMelee = Math.max(1.0, liveMelee / formOnly);
                baseStrike = Math.max(1.0, liveStrike / formOnly);
                baseKi = Math.max(1.0, liveKi / formOnly);
                formBoost = formOnly;
            } else if (dmzFormActive && baseline != null) {
                // Active form string but multipliers stayed ~1 (misconfigured custom form).
                double fromBaseline = estimateFormBoost(
                        liveMelee, baseline.melee, liveStrike, baseline.strike, liveKi, baseline.ki);
                if (fromBaseline > 1.12) {
                    formBoost = fromBaseline;
                    baseMelee = Math.max(1.0, baseline.melee);
                    baseStrike = Math.max(1.0, baseline.strike);
                    baseKi = Math.max(1.0, baseline.ki);
                } else {
                    baseMelee = liveMelee;
                    baseStrike = liveStrike;
                    baseKi = liveKi;
                    formBoost = 1.0;
                }
            } else if (!dmzFormActive) {
                // True base form — snapshot for later custom-race transforms.
                FORM_BASELINES.put(id, new FormBaseline(
                        liveMelee, liveStrike, liveKi, liveDef, liveHp, System.currentTimeMillis()));
                baseMelee = liveMelee;
                baseStrike = liveStrike;
                baseKi = liveKi;
                formBoost = 1.0;
            } else {
                // Transformed on login with no baseline / no mults — don't soft-curve phantoms.
                baseMelee = liveMelee;
                baseStrike = liveStrike;
                baseKi = liveKi;
                formBoost = 1.0;
            }

            // Baseline can still beat formOnly when custom races bake power outside multipliers.
            if (dmzFormActive && baseline != null && formBoost > 1.12) {
                double fromBaseline = estimateFormBoost(
                        liveMelee, baseline.melee, liveStrike, baseline.strike, liveKi, baseline.ki);
                if (fromBaseline > formBoost + 0.05) {
                    formBoost = fromBaseline;
                    baseMelee = Math.max(1.0, baseline.melee);
                    baseStrike = Math.max(1.0, baseline.strike);
                    baseKi = Math.max(1.0, baseline.ki);
                }
            }

            // Peel bulk by its own channel — many high-STR forms leave VIT at ×1.
            // Dividing HP by peak offense formBoost invented a fake ×N HP surplus and
            // made near-linear bulk inherit explode mob health on transform.
            double vitForm = statFormMultiplier(data, "VIT");
            double resForm = statFormMultiplier(data, "RES");
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
            // Continuous mega-form compress (design target ×80, headroom past that).
            // Custom races with maxStatsMultiplier 20 turn ×4 forms into ×80 — keep a
            // visible inherit so packs still react to higher forms.
            double megaT = formBoost >= 6.0 ? megaFormT(formBoost) : 0.0;
            if (formBoost >= 6.0) {
                exp = Math.min(exp, megaFormExpCap(megaT));
                twOffense = Math.min(twOffense, twBase * megaFormTwScale(megaT));
            }
            // Bulk inherits more than offense, but mega forms must soft-curve too —
            // near-linear HP on high mults made bags jump through the roof.
            double twBulk = Math.min(1.0, Math.max(twBase + 0.20, twBase * 1.35) * (0.85 + 0.15 * tierDamp));
            double bulkExp = 1.0;
            if (formBoost >= 6.0) {
                twBulk = Math.min(twBulk, Math.max(twOffense + 0.12, twBase * megaFormBulkTwScale(megaT)));
                bulkExp = megaFormBulkExp(megaT);
            }

            melee = blendForm(baseMelee, liveMelee, twOffense, exp);
            strike = blendForm(baseStrike, liveStrike, twOffense, exp);
            ki = blendForm(baseKi, liveKi, twOffense, exp);
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
        // Peak-only let pure tanks face wet-noodle hits; blend in the average.
        double peakOffense = Math.max(melee, Math.max(strike, ki));
        double avgOffense = (melee + strike + ki) / 3.0;
        double offense = peakOffense * 0.55 + avgOffense * 0.45;
        // Counter identity from form-stripped investments — don't reshuffle on transform.
        StatBalance balance = resolveBalance(data, melee, strike, def, hp, ki, formBoost);
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
     * Target mob HP at this player's current transformed / released power.
     * Scaled by {@link DifficultyConfig#mobHealthScale} (default 65%).
     */
    public double targetMobHealth(DifficultyConfig cfg) {
        double softHp = maxHealth * tierPercent;
        double base = softHp;
        // Soft-blended HP floor for early tiers (not raw live — VIT often doesn't rise with STR forms).
        if (activeTier >= 1 && activeTier <= 3 && formBoost > 1.12) {
            double hpThreat = switch (activeTier) {
                case 1 -> 0.22;
                case 2 -> 0.36;
                case 3 -> 0.48;
                default -> 0.0;
            };
            base = Math.max(base, maxHealth * hpThreat);
        }
        // STR/SKP mega-forms delete packs when HP didn't transform — sponge off live offense.
        // Soft mob damage ≠ player live punches; size the bag for a multi-hit fight (incl. ×80).
        // Cap the sponge so high-mult transforms don't make mob HP jump through the roof.
        if (formBoost > 1.12 && liveOffense > offense * 1.35) {
            double hits = switch (activeTier) {
                case 1 -> 3.6;
                case 2 -> 3.2;
                case 3 -> 2.9;
                case 4 -> 2.6;
                case 5 -> 2.35;
                case 6 -> 2.15;
                default -> 2.0;
            };
            // Keep more tier% damp on mega forms — ×80 used to drop tierMix to ~0.07.
            double tierMix = 0.70;
            if (formBoost >= 6.0) {
                double megaT = megaFormT(formBoost);
                tierMix = Math.max(0.22, 0.55 - 0.28 * Math.min(1.25, megaT)); // ×6→0.55, ×80→0.27
                if (activeTier <= 3) {
                    hits += 0.25 + 0.75 * Math.min(1.25, megaT);
                } else if (activeTier <= 5) {
                    hits += 0.15 + 0.40 * Math.min(1.0, megaT);
                }
            }
            double spongeTier = Math.max(0.15, tierPercent) * tierMix + (1.0 - tierMix);
            double offenseSponge = liveOffense * hits * spongeTier;
            // Never let sponge outrun soft HP by more than a form-aware cap.
            double spongeCap = softHp * megaHealthSpongeCap(formBoost);
            if (offenseSponge > spongeCap) {
                offenseSponge = spongeCap;
            }
            base = Math.max(base, offenseSponge);
        }
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
        double scale = cfg == null ? 0.65 : Math.max(0.05, Math.min(4.0, cfg.mobHealthScale));
        // Admin ladders above 100%: restore sponge so T5–T7 packs aren't deleted.
        if (tierPercent > 1.0) {
            scale = Math.min(4.0, scale * (1.0 + 0.40 * Math.min(2.0, tierPercent - 1.0)));
        }
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

        // T1–T3 + transformed: floor from soft-blended offense only.
        // Raw liveOffense×pct made ×49 STR forms one-shot players at T1.
        if (activeTier >= 1 && activeTier <= 3 && formBoost > 1.12) {
            double threatPct = switch (activeTier) {
                case 1 -> 0.24;
                case 2 -> 0.42;
                case 3 -> 0.55;
                default -> 0.0;
            };
            double softFloor = offense * threatPct;
            if (formBoost >= 6.0) {
                // Tighter as forms climb to ×80 — never pull raw-live threat through this floor.
                double megaT = megaFormT(formBoost);
                double shareMul = 1.35 - 0.28 * Math.min(1.25, megaT); // ×6→1.35, ×80→1.07
                softFloor = Math.min(softFloor, offenseShare * Math.max(1.05, shareMul));
            }
            base = Math.max(base, softFloor);
        }

        // Mega / mastery forms (custom races often ×40–×80): soft curve alone asymptotes so
        // hard that higher forms barely move mob damage. Add a capped live-threat floor.
        if (formBoost >= 6.0 && liveOffense > offense * 1.5) {
            double megaT = megaFormT(formBoost);
            double liveShare = switch (activeTier) {
                case 1 -> 0.05 + 0.04 * Math.min(1.25, megaT);
                case 2 -> 0.08 + 0.06 * Math.min(1.25, megaT);
                case 3 -> 0.11 + 0.08 * Math.min(1.25, megaT);
                case 4 -> 0.15 + 0.10 * Math.min(1.25, megaT);
                case 5 -> 0.19 + 0.12 * Math.min(1.25, megaT);
                case 6 -> 0.23 + 0.14 * Math.min(1.25, megaT);
                default -> 0.27 + 0.16 * Math.min(1.25, megaT);
            };
            base = Math.max(base, liveOffense * Math.max(0.15, tierPercent) * liveShare);
        }

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
            double ki,
            double formBoost
    ) {
        // Prefer raw invested stats; strip form mult so transform doesn't reshuffle counters.
        double peel = Math.max(1.0, formBoost);
        double str = melee;
        double skp = strike;
        double res = defense;
        double vit = health;
        double pwr = ki;
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
        h = mix(h, Math.round(liveCfg.tankDamageDefenseRatio * 1000.0));
        h = mix(h, Math.round(liveCfg.tankDamageHealthRatio * 1000.0));
        h = mix(h, Math.round(liveCfg.defenseToArmorFactor * 1000.0));
        h = mix(h, Math.round(liveCfg.eliteStatMultiplier * 1000.0));
        h = mix(h, Math.round(liveCfg.bossStatMultiplier * 1000.0));
        h = mix(h, Math.round(liveCfg.maxScaledHealth * 10.0));
        h = mix(h, Math.round(liveCfg.maxDamageMultiplier * 1000.0));
        h = mix(h, liveCfg.enableClassCounters ? 1L : 0L);
        h = mix(h, liveCfg.enableStrongStatCounters ? 1L : 0L);
        h = mix(h, liveCfg.paintEpoch());
        // Formula revision: VIT/RES peel + compressed mega HP sponge.
        h = mix(h, 19L);
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
     * Peak form⊕stack multiplier across combat stats.
     * Honors DMZ {@code multiplicationInsteadOfAdditionForMultipliers} (this server: addition).
     * Includes mastery / maxStatsMultiplier via {@code getFormMultiplier}.
     */
    private static double formMultiplierBoost(StatsData data) {
        if (data == null) {
            return 1.0;
        }
        double peak = 1.0;
        for (String key : new String[] {"STR", "SKP", "PWR", "RES", "VIT"}) {
            double combined = statFormMultiplier(data, key);
            if (combined > peak) {
                peak = combined;
            }
        }
        return peak;
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
            double combined = combineDmzMults(form, stack, multiply);
            if (!(combined > 0.0) || Double.isNaN(combined) || Double.isInfinite(combined)) {
                return 1.0;
            }
            return Math.max(1.0, Math.min(MAX_FORM_BOOST, combined));
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
            double melee, double strike, double ki, double def, double hp, long atMs
    ) {}

    /** Detected form multiple ceiling (planned ×80 forms + headroom). */
    private static final double MAX_FORM_BOOST = 100.0;
    /** Mega-form compress anchor: log-lerp from ×6 → ×80. */
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
     * Max offense-sponge / softHp ratio. Mild forms can still pad bags; mega forms
     * must not turn a transform into a sudden HP wall.
     */
    private static double megaHealthSpongeCap(double formBoost) {
        if (!(formBoost > 1.12)) {
            return 1.0;
        }
        if (formBoost < 6.0) {
            // ×1.12→~2.2× softHp, ×6→~3.5×
            return Math.min(3.5, 1.8 + 0.35 * (formBoost - 1.0));
        }
        double megaT = megaFormT(formBoost);
        // ×6→3.6×, ×80→5.2× softHp (was unbounded via liveOffense×hits).
        return 3.6 + 1.6 * Math.min(1.25, megaT);
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
        return Math.max(1.0, Math.min(MAX_FORM_BOOST, boost));
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
