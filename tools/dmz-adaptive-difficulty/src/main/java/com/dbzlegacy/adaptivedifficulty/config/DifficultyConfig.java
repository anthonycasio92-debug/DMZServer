package com.dbzlegacy.adaptivedifficulty.config;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

/** Mirrors the concept doc admin settings. Saved at {@code config/adaptivedifficulty.json}. */
public final class DifficultyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DifficultyConfig INSTANCE = new DifficultyConfig();

    /**
     * Master switch. When {@code false}, scaling / rewards / AI / purchases are inert.
     * Toggle in-game: {@code /difficulty admin off|on|toggle}.
     */
    public boolean enabled = true;

    /**
     * Testing whitelist. When {@code true}, only players in {@link #whitelist} may use AD.
     * Toggle: {@code /difficulty admin whitelist on|off}.
     */
    public boolean whitelistEnabled = false;
    /**
     * Entries are player names (case-insensitive) and/or UUID strings.
     * Managed via {@code /difficulty admin whitelist add|remove}.
     */
    public List<String> whitelist = new ArrayList<>();

    public double prestigeMultiplier = 10.0;
    public double levelMultiplier = 1.0;
    public double teamBonusPercent = 10.0;
    public double contributionPercent = 25.0;
    /** Soft divisor for kill XP multiplier. */
    public double rewardScaling = 2_500.0;
    /**
     * Offense (damage/defense) curve exponent. Higher = steeper growth with difficulty.
     * Effective = {@code pow(d, exp) * pow(pivot, 1-exp)}.
     */
    public double combatCurveExponent = 0.96;
    /** Difficulty where the offense curve matches old linear rates. */
    public long combatCurvePivot = 450L;
    /**
     * Health curve exponent — kept low so mob HP does not become unkillable sponges.
     */
    public double healthCurveExponent = 0.40;
    /** Pivot for the health curve (usually same ballpark as combat pivot). */
    public long healthCurvePivot = 250L;
    /** Flat health % rate (applied through the flat health curve). */
    public double healthPercentPerDifficulty = 0.45;
    /**
     * Per-difficulty damage % (offense curve).
     * Tuned so at ~8M (level-100k band) a base-3 melee hit stays under ~800k raw —
     * what most players can tank from HP alone — while still biting through ~200k DEF.
     */
    public double damagePercentPerDifficulty = 0.62;
    /**
     * Per-difficulty armor points (offense curve).
     * Tuned so mob armor ≈ ~200k at 8M, matching typical endgame player DEF.
     */
    public double defensePercentPerDifficulty = 1.85;
    public double movementPercentPer100Difficulty = 0.15;
    /** DMZ-style extra health % (kept low; health uses its own flat curve). */
    public double dmzExtraHealthPercent = 0.45;
    /** DMZ-style extra damage % (offense curve). */
    public double dmzExtraDamagePercent = 0.62;
    /** DMZ-style extra armor points (offense curve). */
    public double dmzExtraDefensePercent = 1.85;
    /** DMZ-style extra ki damage % (offense curve). */
    public double dmzExtraKiDamagePercent = 0.62;
    public double mobScaleRadius = 64.0;
    /**
     * Area difficulty mode (Scaling Health-inspired):
     * {@code weighted} (default), {@code average}, or {@code max}.
     */
    public String areaDifficultyMode = "weighted";
    /** Extra area difficulty percent per additional nearby player (Scaling Health group bonus). */
    public double areaGroupBonusPercent = 5.0;
    /** Random variance applied to area difficulty when scaling a mob (percent, e.g. 5 => 0.95–1.05). */
    public double areaDifficultyVariancePercent = 5.0;
    /**
     * Absolute ceiling on difficulty values. {@code 0} = no hardcap
     * (max is only calculated from DMZ stats + purchased + team bonuses).
     */
    public long hardCapDifficulty = 0L;

    /**
     * Reference caps used to document the top tier (Zenith).
     * Theoretical max ≈ referenceMaxLevel × referenceMaxPrestige × prestigeMultiplier
     * = 100000 × 10 × 10 = 10,000,000 with defaults.
     */
    public long referenceMaxLevel = 100_000L;
    public int referenceMaxPrestige = 10;

    // Ability unlock tier thresholds (admin-editable)
    public long tierAwakened = 10L;
    public long tierEnhanced = 50L;
    public long tierElite = 100L;
    public long tierAdvanced = 500L;
    public long tierMaster = 1_000L;
    public long tierLegendary = 5_000L;
    public long tierGod = 10_000L;
    public long tierDivine = 50_000L;
    public long tierImpossible = 100_000L;
    public long tierTranscendent = 250_000L;
    public long tierEternal = 500_000L;
    public long tierMythic = 1_000_000L;
    public long tierOmega = 2_500_000L;
    public long tierAbsolute = 5_000_000L;
    public long tierApex = 7_500_000L;
    /** Top internal ability ladder threshold (not a player Unlock Tier). */
    public long tierZenith = 10_000_000L;
    public boolean scaleHostileOnly = true;
    /**
     * Apply DMZ extra health/defense/damage/ki percents to <b>all</b> hostiles
     * (not only {@code dragonminez:} mobs). Default true so vanilla/modded
     * hostiles scale the same way as DMZ enemies.
     */
    public boolean applyDmzExtrasToAllHostiles = true;
    public boolean enableRewardScaling = true;
    public boolean enableMobScaling = true;

    // Phase 2 systems
    public boolean enableElites = true;
    /** Percent chance a claimed T4+ hostile becomes elite (rare). Nameplate only on true rarity. */
    public double eliteChancePercent = 2.25;
    public double eliteStatMultiplier = 1.50;
    public boolean enableMutations = true;
    /** Percent chance a claimed T5+ hostile mutates. Elites get ×1.35. Nameplate only on true rarity. */
    public double mutationChancePercent = 3.75;
    /**
     * One-time: stock 3%/5% chances felt common after the claim-seed +15% elite bug.
     * Migrates untouched defaults down to 1.25%/2%.
     */
    public Boolean rarityChanceMigrated = Boolean.FALSE;
    /**
     * One-time: after kit nameplates stopped faking "Elite" variants, lower stock rarity
     * defaults again (1.25%/2% → 0.75%/1.25%) when still on the prior stock values.
     */
    public Boolean rarityChanceMigratedV2 = Boolean.FALSE;
    /**
     * One-time: stock rarity ×3 (0.75%/1.25% → 2.25%/3.75%).
     * Only rewrites untouched stock values; custom admin-set chances are kept.
     */
    public Boolean rarityChanceMigratedV3 = Boolean.FALSE;
    public boolean enableAdaptiveAi = true;
    public boolean enableEnemyEvolution = true;
    public boolean enableBossScaling = true;
    public double bossStatMultiplier = 1.5;
    /** Natural (pre-scale) max-health at/above this marks a boss. Default keeps wardens/etc. */
    public double bossHealthThreshold = 300.0;
    public List<String> bossIdContains = new ArrayList<>(Arrays.asList(
            "boss", "warden", "wither", "ender_dragon", "raid_boss", "raidboss"
    ));
    /**
     * Optional ceiling on the health multiplier from the health curve.
     * {@code 0} or {@code 1} = uncapped (default). Absolute HP still limited by {@link #maxScaledHealth}.
     */
    public double maxHealthMultiplier = 0.0;
    /**
     * Absolute max HP after scaling.
     * {@code 0} = uncapped (default). Vanilla's 1024 max_health attribute limit is raised at mod boot.
     */
    public double maxScaledHealth = 0.0;
    public double maxMoveMultiplier = 2.0;
    /**
     * Optional armor-point ceiling from defense scaling.
     * {@code 0} = uncapped (default). Only values {@code > 0} apply a ceiling.
     */
    public double maxArmorBonus = 0.0;
    /**
     * Optional damage multiplier ceiling.
     * {@code 0} or {@code 1} = uncapped (default). Only values {@code > 1} apply a ceiling.
     */
    public double maxDamageMultiplier = 0.0;
    /** Admin permission node (Forge PermissionAPI / LuckPerms). Ops always allowed. */
    public String adminPermission = "difficulty.admin";
    /**
     * Player UI backend for {@code /difficulty}:
     * {@code cmi} (default), {@code auto} (CMI → chest → chat), {@code chest}, or {@code chat}.
     */
    public String guiBackend = "cmi";
    /**
     * If the Minecraft world is on Peaceful (no hostile spawns), restore it on server start.
     * Peaceful prevents adaptive mob scaling from doing anything.
     * Null in JSON means enabled (default true).
     */
    public Boolean restoreVanillaDifficultyFromPeaceful = Boolean.TRUE;
    /** Target vanilla difficulty when restoring from Peaceful: easy / normal / hard. */
    public String vanillaDifficulty = "hard";
    /**
     * Dimensions where adaptive difficulty skips scaling / AI / evolution / gravity.
     * Empty by default — The End scales like Overworld/Nether; Ender Dragon is
     * hard-exempt (End Strength script). Re-add {@code minecraft:the_end} to deny.
     */
    public List<String> disabledDimensions = new ArrayList<>();
    /**
     * One-time: strip legacy default {@code minecraft:the_end} from
     * {@link #disabledDimensions} so existing configs pick up End scaling.
     */
    public Boolean endScalingEnabledMigrated = Boolean.FALSE;

    // ── V3 Combat Rating weights ───────────────────────────────────────────
    public double combatRatingDmzWeight = 1.0;
    public double combatRatingPrestigeWeight = 1000.0;
    public double combatRatingTransformWeight = 1.0;
    public double combatRatingDifficultyWeight = 1.0;

    // ── V3 Unlock tier requirements / ceilings / activation costs ───────────
    public long unlockTier1Level = 1L;
    public long unlockTier2Level = 500L;
    public long unlockTier3Level = 1_000L;
    public long unlockTier4Level = 5_000L;
    public long unlockTier5Level = 10_000L;
    public long unlockTier6Level = 50_000L;
    public long unlockTier7Level = 100_000L;
    public long unlockTier1Max = 1_000L;
    public long unlockTier2Max = 5_000L;
    public long unlockTier3Max = 10_000L;
    public long unlockTier4Max = 25_000L;
    public long unlockTier5Max = 50_000L;
    public long unlockTier6Max = 100_000L;
    public long unlockTier7Max = 250_000L;
    /** Copper-value at the tier unlock level. T1 default = 1× Copper Ancient. */
    public long unlockTier1Cost = 1L;
    public long unlockTier2Cost = 5L;
    public long unlockTier3Cost = 15L;
    public long unlockTier4Cost = 50L;
    public long unlockTier5Cost = 150L;
    public long unlockTier6Cost = 500L;
    public long unlockTier7Cost = 1_500L;
    /**
     * Tier purchase cost scales with how far above the tier's unlock level the
     * player is: {@code base × (1 + max(0, dmzLevel - requiredLevel) / divisor)}.
     * Buying at the unlock threshold ≈ base cost (T7 is no longer 100×+ at lvl 100k).
     * Default 1000 → 1000 levels above unlock ≈ 2× base.
     */
    public double tierCostLevelDivisor = 1_000.0;
    /**
     * Nearby-mob scale vs the player's post-transform / limit-release stats.
     * Defaults: T1 10% · T2 20% · T3 30% · T4 40% · T5 50% · T6 65% · T7 90%.
     */
    public double unlockTier1EnemyMult = 0.10;
    public double unlockTier2EnemyMult = 0.20;
    public double unlockTier3EnemyMult = 0.30;
    public double unlockTier4EnemyMult = 0.40;
    public double unlockTier5EnemyMult = 0.50;
    public double unlockTier6EnemyMult = 0.65;
    public double unlockTier7EnemyMult = 0.90;
    /**
     * Multiplier on final scaled mob max HP ({@code playerMaxHp × tier% × …}).
     * Default {@code 0.65} — early tiers need sponge so fights last more than one punch.
     */
    public double mobHealthScale = 0.65;
    /**
     * How much of a form/transform boost enemies scale to (0–1), after the soft curve.
     * {@code 0} = form-stripped base stats only · {@code 1} = strongest allowed form slice.
     * Default {@code 0.25}. Combined with {@link #transformScaleExponent} + tier damp so
     * big forms (10–20×) do not linearly explode mob damage at T6–T7.
     */
    public double transformScaleWeight = 0.25;
    /**
     * Diminishing-returns exponent on form surplus ({@code (live/base - 1)^exp × weight}).
     * {@code 1.0} = linear · {@code 0.50} = sqrt-ish (default) · lower = softer high forms.
     */
    public double transformScaleExponent = 0.50;
    /**
     * Legacy weak-stat dump multiplier (unused in combat as of 3.3.35).
     * Kept for config/admin compat; default 1.0 = no effect.
     */
    public double weakStatCounterMult = 1.0;
    /**
     * Legacy pierce knob (unused in combat as of 3.3.35).
     * Live floors use {@link #tankDamageDefenseRatio} / {@link #tankDamageHealthRatio}.
     */
    public double weakDefensePierceMult = 1.75;
    /**
     * Damage floor from player defense × tier % — stops DEF dumps from facing soft hits.
     * Example: 0.55 → mob attack at least 55% of (defense × tierPercent) at full counter strength.
     * Early tiers further damp this floor so 10–20% ladders stay near raw offense share.
     */
    public double tankDamageDefenseRatio = 0.55;
    /**
     * Damage floor from player max HP × tier % — presses high-VIT tanks.
     * Example: 0.020 → mob attack at least 2.0% of (maxHealth × tierPercent).
     */
    public double tankDamageHealthRatio = 0.020;
    /**
     * One-time: raise soft T1–T3 percents + tank pierce so early difficulty
     * contests high-DEF / low-level players (old 15%/30%/55% + 0.40 floor).
     */
    public Boolean lowTierPressureMigrated = Boolean.FALSE;
    /**
     * One-time: raise mid/high tier stock percents
     * T4 80→90 · T5 110→125 · T6 130→165 (T1–T3/T7 already match the ladder).
     */
    public Boolean tierPercentLadderMigrated = Boolean.FALSE;
    /**
     * One-time: stock ladder 28/42/65/90/125/165/200 → 13/28/42/58/76/90/116.
     * Only rewrites untouched stock values; custom admin-set ladders are kept.
     */
    public Boolean tierPercentLadderMigratedV2 = Boolean.FALSE;
    /**
     * One-time: stock ladder 13/28/42/58/76/90/116 → 10/20/30/40/50/65/90.
     * Only rewrites untouched stock values; custom admin-set ladders are kept.
     */
    public Boolean tierPercentLadderMigratedV3 = Boolean.FALSE;
    /**
     * One-time: drop race / weak-stat / specialization stack and soften class + top-1
     * counter stock values so existing configs match the lighter 3.3.35 model.
     */
    public Boolean counterSimplifyMigratedV1 = Boolean.FALSE;
    /**
     * One-time: soften stock pierce / counter overlays so early 10–20% tiers
     * are not secretly ~DEF pierce + full class/top-stat stacks.
     */
    public Boolean lowTierCounterSoftMigratedV1 = Boolean.FALSE;
    /**
     * One-time: balance pass — raise mob HP sponge, soften elite spike, nudge tank HP floor.
     */
    public Boolean balanceFeelMigratedV1 = Boolean.FALSE;
    /**
     * One-time: soften transform→enemy scaling (weight 0.40→0.25 + soft curve).
     */
    public Boolean transformSoftMigratedV1 = Boolean.FALSE;
    /**
     * One-time: starter economy — T1 at level 1 costs 1× Copper (was 100 = 1× Gold).
     * Only rewrites the stock cost ladder / T1 level gate; custom admin values kept.
     */
    public Boolean starterCopperCostMigratedV1 = Boolean.FALSE;
    /**
     * Bumped on every live sanitize/reload so claimed-mob NBT signatures cannot
     * early-return with stale paint after admin retunes.
     */
    private transient long paintEpoch = 0L;
    /**
     * Legacy specialization tax (unused in combat as of 3.3.35).
     * Kept for config/admin compat; default 0.0 = no effect.
     */
    public double specializationDamageTax = 0.0;
    /**
     * When true, mobs also counter the player's DMZ fighting class
     * (warrior, spiritualist, tank, …).
     */
    public boolean enableClassCounters = true;
    /**
     * When true, mobs also counter the player's single highest invested combat stat
     * (STR / SKP / RES / VIT / PWR).
     */
    public boolean enableStrongStatCounters = true;
    /** Strength of top-stat counter overlays (1.0 = off effect, higher = harder). */
    public double strongStatCounterMult = 1.08;
    /** Extra mob damage vs the player's fighting style (class overlay). */
    public double classCounterDamageMult = 1.06;
    /** Extra mob HP vs glass / caster / ki classes. */
    public double classCounterHealthMult = 1.05;
    /** Extra mob armor vs melee / strike classes. */
    public double classCounterArmorMult = 1.06;
    /**
     * Legacy race overlay (unused in combat as of 3.3.35).
     * Kept for config/admin compat; default 1.0 = no effect.
     */
    public double raceCounterMult = 1.0;
    /**
     * Cap on multiplicative counter overlays after pierce floors
     * (class × top-stat only). Prevents stacking blow-ups.
     */
    public double maxCounterOverlayMult = 1.25;
    /** Converts DMZ defense share into vanilla armor points: log1p(def) × factor. */
    public double defenseToArmorFactor = 2.5;
    /** Server ticks between nearby-player mob rescale pulses (per-player stagger). */
    public int nearbyScaleIntervalTicks = 40;
    /**
     * Max difficulty-adjusted hostiles near one player at a time (closest win).
     * Also used as the nearby rescale budget.
     */
    public int maxScaledMobsPerPlayer = 5;
    /** @deprecated use {@link #maxScaledMobsPerPlayer}; kept for config compat. */
    @Deprecated
    public int nearbyScaleBudgetPerPlayer = 5;

    // ── V3 Ancient Coin economy ────────────────────────────────────────────
    public boolean enableAncientCoinDrops = true;
    public double ancientCoinDropMult = 1.0;
    /**
     * Chance (0–1) on each kill to also drop one coin of the next higher denomination
     * alongside the normal drop (e.g. T0/T1 Copper + rare Iron). Default 2%.
     */
    public double ancientCoinUpgradeChance = 0.02;
    public double ancientCoinRatingDivisor = 25_000.0;
    /** V3: reset active tier/level on player death (unlocks stay). */
    public boolean deathResetsActiveDifficulty = true;
    /** Minimum active unlock-tier for elites / mutations / full AI. */
    public int eliteMinUnlockTier = 4;
    public int mutationMinUnlockTier = 5;
    /** Soft AI starts at Unlock Tier 1; kit depth is capped by {@link com.dbzlegacy.adaptivedifficulty.tier.UnlockAbilityCaps}. */
    public int adaptiveAiMinUnlockTier = 1;
    public int enemyEvolutionMinUnlockTier = 1;
    public int bossMechanicsMinUnlockTier = 6;

    public boolean shouldRestoreVanillaFromPeaceful() {
        return restoreVanillaDifficultyFromPeaceful == null || restoreVanillaDifficultyFromPeaceful;
    }

    public long tierRequiredLevel(int tierId) {
        return switch (tierId) {
            case 1 -> Math.max(0L, unlockTier1Level);
            case 2 -> Math.max(0L, unlockTier2Level);
            case 3 -> Math.max(0L, unlockTier3Level);
            case 4 -> Math.max(0L, unlockTier4Level);
            case 5 -> Math.max(0L, unlockTier5Level);
            case 6 -> Math.max(0L, unlockTier6Level);
            case 7 -> Math.max(0L, unlockTier7Level);
            default -> Long.MAX_VALUE / 4L;
        };
    }

    public long tierMaxDifficulty(int tierId) {
        return switch (tierId) {
            case 1 -> Math.max(0L, unlockTier1Max);
            case 2 -> Math.max(0L, unlockTier2Max);
            case 3 -> Math.max(0L, unlockTier3Max);
            case 4 -> Math.max(0L, unlockTier4Max);
            case 5 -> Math.max(0L, unlockTier5Max);
            case 6 -> Math.max(0L, unlockTier6Max);
            case 7 -> Math.max(0L, unlockTier7Max);
            default -> 0L;
        };
    }

    public long tierActivationCost(int tierId) {
        return switch (tierId) {
            case 1 -> Math.max(0L, unlockTier1Cost);
            case 2 -> Math.max(0L, unlockTier2Cost);
            case 3 -> Math.max(0L, unlockTier3Cost);
            case 4 -> Math.max(0L, unlockTier4Cost);
            case 5 -> Math.max(0L, unlockTier5Cost);
            case 6 -> Math.max(0L, unlockTier6Cost);
            case 7 -> Math.max(0L, unlockTier7Cost);
            default -> Long.MAX_VALUE / 4L;
        };
    }

    /** Level-scaled tier purchase cost for a player at {@code dmzLevel}. */
    public long tierActivationCostScaled(int tierId, int dmzLevel) {
        long base = tierActivationCost(tierId);
        if (base <= 0L) {
            return 0L;
        }
        double divisor = Math.max(1.0, tierCostLevelDivisor);
        // Scale only by levels above this tier's unlock requirement.
        // Absolute-level scaling made T7 (req 100k) cost 152× Netherite at unlock.
        long required = tierRequiredLevel(tierId);
        long excess = Math.max(0L, (long) Math.max(0, dmzLevel) - required);
        double mult = 1.0 + excess / divisor;
        long scaled = Math.round(base * mult);
        long raw = Math.max(base, scaled);
        // Cap at 128 of one coin type, then promote (top rung = 128× Netherite).
        long cost = com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy.normalizeCost(raw);

        // Excess-above-unlock scaling can invert the ladder (T7 << T6 near T7 unlock).
        // Always keep higher tiers at least ~25% above the previous tier's cost.
        if (tierId > 1) {
            long prev = tierActivationCostScaled(tierId - 1, dmzLevel);
            if (cost <= prev) {
                long floor = prev + Math.max(1L, prev / 4L);
                cost = com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy
                        .normalizeCost(Math.max(raw, floor));
                if (cost <= prev) {
                    cost = com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy
                            .costStrictlyAbove(prev);
                }
            }
        }
        return cost;
    }

    public double tierEnemyMult(int tierId) {
        return tierPlayerStatPercent(tierId);
    }

    /** Fraction of the player's transformed/released stats used for nearby mobs. */
    public double tierPlayerStatPercent(int tierId) {
        return switch (tierId) {
            case 1 -> clampPercent(unlockTier1EnemyMult, 0.10);
            case 2 -> clampPercent(unlockTier2EnemyMult, 0.20);
            case 3 -> clampPercent(unlockTier3EnemyMult, 0.30);
            case 4 -> clampPercent(unlockTier4EnemyMult, 0.40);
            case 5 -> clampPercent(unlockTier5EnemyMult, 0.50);
            case 6 -> clampPercent(unlockTier6EnemyMult, 0.65);
            case 7 -> clampPercent(unlockTier7EnemyMult, 0.90);
            default -> 0.0;
        };
    }

    /** Range-only clamp for live tier percents — never remaps admin ladders. */
    private static double clampPercent(double value, double fallback) {
        if (!(value > 0.0) || Double.isNaN(value) || Double.isInfinite(value)) {
            return fallback;
        }
        return Math.max(0.01, Math.min(10.0, value));
    }

    /** Tier thresholds — editable via {@code /difficulty admin set tierX <n>}. */
    public long tierThreshold(com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier tier) {
        if (tier == null) {
            return 0L;
        }
        return switch (tier) {
            case NONE -> 0L;
            case AWAKENED -> Math.max(0L, tierAwakened);
            case ENHANCED -> Math.max(0L, tierEnhanced);
            case ELITE -> Math.max(0L, tierElite);
            case ADVANCED -> Math.max(0L, tierAdvanced);
            case MASTER -> Math.max(0L, tierMaster);
            case LEGENDARY -> Math.max(0L, tierLegendary);
            case GOD -> Math.max(0L, tierGod);
            case DIVINE -> Math.max(0L, tierDivine);
            case IMPOSSIBLE -> Math.max(0L, tierImpossible);
            case TRANSCENDENT -> Math.max(0L, tierTranscendent);
            case ETERNAL -> Math.max(0L, tierEternal);
            case MYTHIC -> Math.max(0L, tierMythic);
            case OMEGA -> Math.max(0L, tierOmega);
            case ABSOLUTE -> Math.max(0L, tierAbsolute);
            case APEX -> Math.max(0L, tierApex);
            case ZENITH -> Math.max(0L, tierZenith);
        };
    }

    private DifficultyConfig() {}

    public static DifficultyConfig get() {
        return INSTANCE;
    }

    /** Master gate used by events / scaling / rewards / player purchases. */
    public static boolean isEnabled() {
        return INSTANCE != null && INSTANCE.enabled;
    }

    public static void setEnabled(boolean on) {
        if (INSTANCE == null) {
            INSTANCE = new DifficultyConfig();
        }
        INSTANCE.enabled = on;
        save();
    }

    public static boolean isWhitelistEnabled() {
        return INSTANCE != null && INSTANCE.whitelistEnabled;
    }

    public static void setWhitelistEnabled(boolean on) {
        if (INSTANCE == null) {
            INSTANCE = new DifficultyConfig();
        }
        INSTANCE.whitelistEnabled = on;
        save();
    }

    /** When whitelist is off, everyone is allowed (if the master switch is on). */
    public static boolean isPlayerAllowed(ServerPlayer player) {
        if (INSTANCE == null || player == null) {
            return false;
        }
        if (!INSTANCE.whitelistEnabled) {
            return true;
        }
        return isWhitelisted(player);
    }

    public static boolean isWhitelisted(ServerPlayer player) {
        if (INSTANCE == null || player == null) {
            return false;
        }
        return matchesWhitelist(player.m_6302_(), player.m_20148_());
    }

    public static boolean matchesWhitelist(String name, UUID uuid) {
        if (INSTANCE == null || INSTANCE.whitelist == null || INSTANCE.whitelist.isEmpty()) {
            return false;
        }
        String nameKey = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        String uuidKey = uuid == null ? "" : uuid.toString().toLowerCase(Locale.ROOT);
        for (String raw : INSTANCE.whitelist) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String entry = raw.trim().toLowerCase(Locale.ROOT);
            if ((!nameKey.isEmpty() && entry.equals(nameKey))
                    || (!uuidKey.isEmpty() && entry.equals(uuidKey))) {
                return true;
            }
        }
        return false;
    }

    /** @return true if newly added */
    public static boolean addWhitelistEntry(String raw) {
        if (INSTANCE == null) {
            INSTANCE = new DifficultyConfig();
        }
        if (INSTANCE.whitelist == null) {
            INSTANCE.whitelist = new ArrayList<>();
        }
        String entry = normalizeWhitelistEntry(raw);
        if (entry.isEmpty()) {
            return false;
        }
        for (String existing : INSTANCE.whitelist) {
            if (existing != null && existing.equalsIgnoreCase(entry)) {
                return false;
            }
        }
        INSTANCE.whitelist.add(entry);
        save();
        return true;
    }

    /** @return true if removed */
    public static boolean removeWhitelistEntry(String raw) {
        if (INSTANCE == null || INSTANCE.whitelist == null) {
            return false;
        }
        String entry = normalizeWhitelistEntry(raw);
        if (entry.isEmpty()) {
            return false;
        }
        boolean removed = INSTANCE.whitelist.removeIf(
                e -> e != null && e.equalsIgnoreCase(entry)
        );
        if (removed) {
            save();
        }
        return removed;
    }

    public static List<String> whitelistEntries() {
        if (INSTANCE == null || INSTANCE.whitelist == null) {
            return List.of();
        }
        return new ArrayList<>(INSTANCE.whitelist);
    }

    private static String normalizeWhitelistEntry(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return "";
        }
        // Keep UUID casing canonical lowercase; names lowercase for matching.
        try {
            return UUID.fromString(t).toString().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ignored) {
            return t.toLowerCase(Locale.ROOT);
        }
    }

    /** Current config filename (AdaptiveDifficulty 1.0+). */
    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("adaptivedifficulty.json");
    }

    /** Pre-1.0 filename — auto-migrated on first load. */
    public static Path legacyPath() {
        return FMLPaths.CONFIGDIR.get().resolve("dmz_adaptive_difficulty.json");
    }

    /** @return false when the JSON exists but could not be parsed. */
    public static boolean load() {
        Path file = path();
        Path legacy = legacyPath();
        try {
            if (!Files.exists(file) && Files.exists(legacy)) {
                try {
                    Files.move(legacy, file);
                    AdaptiveDifficultyMod.LOGGER.info(
                            "[{}] migrated config {} → {}",
                            AdaptiveDifficultyMod.MOD_ID,
                            legacy.getFileName(),
                            file.getFileName());
                } catch (Exception moveFail) {
                    AdaptiveDifficultyMod.LOGGER.warn(
                            "[{}] could not rename legacy config; loading {} then re-saving as {}",
                            AdaptiveDifficultyMod.MOD_ID,
                            legacy.getFileName(),
                            file.getFileName(),
                            moveFail);
                    file = legacy;
                }
            }
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    DifficultyConfig loaded = GSON.fromJson(reader, DifficultyConfig.class);
                    if (loaded != null) {
                        normalize(loaded);
                        INSTANCE = loaded;
                    }
                }
            }
            save();
            return true;
        } catch (Exception e) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] failed to load config: {}", AdaptiveDifficultyMod.MOD_ID, e.toString());
            if (INSTANCE == null) {
                INSTANCE = new DifficultyConfig();
            }
            return false;
        }
    }

    private static void normalize(DifficultyConfig cfg) {
        if (cfg.bossIdContains == null) {
            cfg.bossIdContains = new ArrayList<>();
        }
        if (cfg.whitelist == null) {
            cfg.whitelist = new ArrayList<>();
        } else {
            LinkedHashSet<String> cleaned = new LinkedHashSet<>();
            for (String raw : cfg.whitelist) {
                String entry = normalizeWhitelistEntry(raw);
                if (!entry.isEmpty()) {
                    cleaned.add(entry);
                }
            }
            cfg.whitelist = new ArrayList<>(cleaned);
        }
        if (cfg.disabledDimensions == null) {
            cfg.disabledDimensions = new ArrayList<>();
        }
        // Legacy installs defaulted The End off; enable End hostiles once (dragon still exempt).
        if (!Boolean.TRUE.equals(cfg.endScalingEnabledMigrated)) {
            cfg.disabledDimensions.removeIf(raw -> {
                if (raw == null || raw.isBlank()) {
                    return true;
                }
                String want = raw.trim().toLowerCase();
                return want.equals("minecraft:the_end") || want.equals("the_end");
            });
            cfg.endScalingEnabledMigrated = Boolean.TRUE;
        }
        // Drop inflated stock rarity chances once (3%/5% → 1.25%/2%).
        if (!Boolean.TRUE.equals(cfg.rarityChanceMigrated)) {
            if (nearly(cfg.eliteChancePercent, 3.0)) {
                cfg.eliteChancePercent = 1.25;
            }
            if (nearly(cfg.mutationChancePercent, 5.0)) {
                cfg.mutationChancePercent = 2.0;
            }
            cfg.rarityChanceMigrated = Boolean.TRUE;
        }
        // Further drop stock rarity after kit cosmetics stopped looking like variants.
        if (!Boolean.TRUE.equals(cfg.rarityChanceMigratedV2)) {
            if (nearly(cfg.eliteChancePercent, 1.25)) {
                cfg.eliteChancePercent = 0.75;
            }
            if (nearly(cfg.mutationChancePercent, 2.0)) {
                cfg.mutationChancePercent = 1.25;
            }
            cfg.rarityChanceMigratedV2 = Boolean.TRUE;
        }
        // Triple stock rarity chances (0.75%/1.25% → 2.25%/3.75%).
        if (!Boolean.TRUE.equals(cfg.rarityChanceMigratedV3)) {
            if (nearly(cfg.eliteChancePercent, 0.75)) {
                cfg.eliteChancePercent = 2.25;
            }
            if (nearly(cfg.mutationChancePercent, 1.25)) {
                cfg.mutationChancePercent = 3.75;
            }
            cfg.rarityChanceMigratedV3 = Boolean.TRUE;
        }
        // Raise soft early-tier pressure + tank pierce (stock values only).
        if (!Boolean.TRUE.equals(cfg.lowTierPressureMigrated)) {
            if (nearly(cfg.unlockTier1EnemyMult, 0.15)) {
                cfg.unlockTier1EnemyMult = 0.28;
            }
            if (nearly(cfg.unlockTier2EnemyMult, 0.30)) {
                cfg.unlockTier2EnemyMult = 0.42;
            }
            if (nearly(cfg.unlockTier3EnemyMult, 0.55)) {
                cfg.unlockTier3EnemyMult = 0.65;
            }
            if (nearly(cfg.tankDamageDefenseRatio, 0.40)) {
                cfg.tankDamageDefenseRatio = 0.90;
            }
            if (nearly(cfg.tankDamageHealthRatio, 0.012)) {
                cfg.tankDamageHealthRatio = 0.025;
            }
            if (nearly(cfg.weakDefensePierceMult, 1.35)) {
                cfg.weakDefensePierceMult = 1.75;
            }
            cfg.lowTierPressureMigrated = Boolean.TRUE;
        }
        // Raise mid/high tier stock percents to the 28/42/65/90/125/165/200 ladder.
        if (!Boolean.TRUE.equals(cfg.tierPercentLadderMigrated)) {
            if (nearly(cfg.unlockTier4EnemyMult, 0.80)) {
                cfg.unlockTier4EnemyMult = 0.90;
            }
            if (nearly(cfg.unlockTier5EnemyMult, 1.10)) {
                cfg.unlockTier5EnemyMult = 1.25;
            }
            if (nearly(cfg.unlockTier6EnemyMult, 1.30)) {
                cfg.unlockTier6EnemyMult = 1.65;
            }
            cfg.tierPercentLadderMigrated = Boolean.TRUE;
        }
        // Soften stock ladder: 28/42/65/90/125/165/200 → 13/28/42/58/76/90/116.
        if (!Boolean.TRUE.equals(cfg.tierPercentLadderMigratedV2)) {
            boolean stockPrior =
                    nearly(cfg.unlockTier1EnemyMult, 0.28)
                            && nearly(cfg.unlockTier2EnemyMult, 0.42)
                            && nearly(cfg.unlockTier3EnemyMult, 0.65)
                            && nearly(cfg.unlockTier4EnemyMult, 0.90)
                            && nearly(cfg.unlockTier5EnemyMult, 1.25)
                            && nearly(cfg.unlockTier6EnemyMult, 1.65)
                            && nearly(cfg.unlockTier7EnemyMult, 2.00);
            if (stockPrior) {
                cfg.unlockTier1EnemyMult = 0.13;
                cfg.unlockTier2EnemyMult = 0.28;
                cfg.unlockTier3EnemyMult = 0.42;
                cfg.unlockTier4EnemyMult = 0.58;
                cfg.unlockTier5EnemyMult = 0.76;
                cfg.unlockTier6EnemyMult = 0.90;
                cfg.unlockTier7EnemyMult = 1.16;
            }
            cfg.tierPercentLadderMigratedV2 = Boolean.TRUE;
        }
        // Soften stock ladder: 13/28/42/58/76/90/116 → 10/20/30/40/50/65/90.
        if (!Boolean.TRUE.equals(cfg.tierPercentLadderMigratedV3)) {
            boolean stockPrior =
                    nearly(cfg.unlockTier1EnemyMult, 0.13)
                            && nearly(cfg.unlockTier2EnemyMult, 0.28)
                            && nearly(cfg.unlockTier3EnemyMult, 0.42)
                            && nearly(cfg.unlockTier4EnemyMult, 0.58)
                            && nearly(cfg.unlockTier5EnemyMult, 0.76)
                            && nearly(cfg.unlockTier6EnemyMult, 0.90)
                            && nearly(cfg.unlockTier7EnemyMult, 1.16);
            if (stockPrior) {
                cfg.unlockTier1EnemyMult = 0.10;
                cfg.unlockTier2EnemyMult = 0.20;
                cfg.unlockTier3EnemyMult = 0.30;
                cfg.unlockTier4EnemyMult = 0.40;
                cfg.unlockTier5EnemyMult = 0.50;
                cfg.unlockTier6EnemyMult = 0.65;
                cfg.unlockTier7EnemyMult = 0.90;
            }
            cfg.tierPercentLadderMigratedV3 = Boolean.TRUE;
        }
        // Soften counters: class + top-1 only (drop race / weak / specialization stock stack).
        if (!Boolean.TRUE.equals(cfg.counterSimplifyMigratedV1)) {
            if (nearly(cfg.weakStatCounterMult, 1.45)) {
                cfg.weakStatCounterMult = 1.0;
            }
            if (nearly(cfg.specializationDamageTax, 0.60)) {
                cfg.specializationDamageTax = 0.0;
            }
            if (nearly(cfg.raceCounterMult, 1.12)) {
                cfg.raceCounterMult = 1.0;
            }
            if (nearly(cfg.strongStatCounterMult, 1.28)) {
                cfg.strongStatCounterMult = 1.15;
            }
            if (nearly(cfg.classCounterDamageMult, 1.22)) {
                cfg.classCounterDamageMult = 1.12;
            }
            if (nearly(cfg.classCounterHealthMult, 1.18)) {
                cfg.classCounterHealthMult = 1.10;
            }
            if (nearly(cfg.classCounterArmorMult, 1.20)) {
                cfg.classCounterArmorMult = 1.12;
            }
            if (nearly(cfg.maxCounterOverlayMult, 2.25)) {
                cfg.maxCounterOverlayMult = 1.55;
            }
            cfg.counterSimplifyMigratedV1 = Boolean.TRUE;
        }
        // Soften early-tier stock pierce / counter overlays (post 3.3.35 light model).
        if (!Boolean.TRUE.equals(cfg.lowTierCounterSoftMigratedV1)) {
            if (nearly(cfg.tankDamageDefenseRatio, 0.90)) {
                cfg.tankDamageDefenseRatio = 0.55;
            }
            if (nearly(cfg.tankDamageHealthRatio, 0.025)) {
                cfg.tankDamageHealthRatio = 0.015;
            }
            if (nearly(cfg.strongStatCounterMult, 1.15) || nearly(cfg.strongStatCounterMult, 1.28)) {
                cfg.strongStatCounterMult = 1.08;
            }
            if (nearly(cfg.classCounterDamageMult, 1.12) || nearly(cfg.classCounterDamageMult, 1.22)) {
                cfg.classCounterDamageMult = 1.06;
            }
            if (nearly(cfg.classCounterHealthMult, 1.10) || nearly(cfg.classCounterHealthMult, 1.18)) {
                cfg.classCounterHealthMult = 1.05;
            }
            if (nearly(cfg.classCounterArmorMult, 1.12) || nearly(cfg.classCounterArmorMult, 1.20)) {
                cfg.classCounterArmorMult = 1.06;
            }
            if (nearly(cfg.maxCounterOverlayMult, 1.55) || nearly(cfg.maxCounterOverlayMult, 2.25)) {
                cfg.maxCounterOverlayMult = 1.25;
            }
            cfg.lowTierCounterSoftMigratedV1 = Boolean.TRUE;
        }
        // Balance feel: sponge + elite spike + tank HP floor (3.3.38).
        if (!Boolean.TRUE.equals(cfg.balanceFeelMigratedV1)) {
            if (nearly(cfg.mobHealthScale, 0.5)) {
                cfg.mobHealthScale = 0.65;
            }
            if (nearly(cfg.eliteStatMultiplier, 1.75)) {
                cfg.eliteStatMultiplier = 1.50;
            }
            if (nearly(cfg.tankDamageHealthRatio, 0.015) || nearly(cfg.tankDamageHealthRatio, 0.025)) {
                cfg.tankDamageHealthRatio = 0.020;
            }
            cfg.balanceFeelMigratedV1 = Boolean.TRUE;
        }
        // Transform soft curve (3.3.44): linear 0.40 still one-tapped high-tier form swaps.
        if (!Boolean.TRUE.equals(cfg.transformSoftMigratedV1)) {
            if (nearly(cfg.transformScaleWeight, 0.40)) {
                cfg.transformScaleWeight = 0.25;
            }
            // Only fill missing/invalid exponent — do not overwrite intentional linear (1.0).
            if (cfg.transformScaleExponent <= 0.0
                    || Double.isNaN(cfg.transformScaleExponent)
                    || Double.isInfinite(cfg.transformScaleExponent)) {
                cfg.transformScaleExponent = 0.50;
            }
            cfg.transformSoftMigratedV1 = Boolean.TRUE;
        }
        // Starter economy (3.3.50): stock T1 cost was 100 copper-value → shown as 1× Gold.
        if (!Boolean.TRUE.equals(cfg.starterCopperCostMigratedV1)) {
            boolean stockCosts = cfg.unlockTier1Cost == 100L
                    && cfg.unlockTier2Cost == 500L
                    && cfg.unlockTier3Cost == 1_500L
                    && cfg.unlockTier4Cost == 5_000L
                    && cfg.unlockTier5Cost == 15_000L
                    && cfg.unlockTier6Cost == 50_000L
                    && cfg.unlockTier7Cost == 150_000L;
            if (stockCosts) {
                cfg.unlockTier1Cost = 1L;
                cfg.unlockTier2Cost = 5L;
                cfg.unlockTier3Cost = 15L;
                cfg.unlockTier4Cost = 50L;
                cfg.unlockTier5Cost = 150L;
                cfg.unlockTier6Cost = 500L;
                cfg.unlockTier7Cost = 1_500L;
            }
            // Stock T1 gate was level 100 — low-level players could never buy the 1× Copper tier.
            if (cfg.unlockTier1Level == 100L) {
                cfg.unlockTier1Level = 1L;
            }
            cfg.starterCopperCostMigratedV1 = Boolean.TRUE;
        }
        if (cfg.guiBackend == null || cfg.guiBackend.isBlank()) {
            cfg.guiBackend = "cmi";
        } else {
            // Canonicalize aliases so Forge + Bukkit agree.
            cfg.guiBackend = switch (cfg.guiBackend.trim().toLowerCase()) {
                case "cmi", "cmilib", "cmigui", "deluxemenus", "deluxe", "dm" -> "cmi";
                case "chest", "bukkit", "inventory", "gui" -> "chest";
                case "chat" -> "chat";
                case "auto" -> "auto";
                default -> "auto";
            };
        }
        if (cfg.vanillaDifficulty == null || cfg.vanillaDifficulty.isBlank()) {
            cfg.vanillaDifficulty = "hard";
        }
        if (cfg.adminPermission == null || cfg.adminPermission.isBlank()) {
            cfg.adminPermission = "difficulty.admin";
        }
        if (cfg.combatCurveExponent <= 0.0) {
            cfg.combatCurveExponent = 0.96;
        }
        if (cfg.combatCurvePivot < 1L) {
            cfg.combatCurvePivot = 450L;
        }
        if (cfg.healthCurveExponent <= 0.0) {
            cfg.healthCurveExponent = 0.40;
        }
        if (cfg.healthCurvePivot < 1L) {
            cfg.healthCurvePivot = 250L;
        }
        if (cfg.rewardScaling < 1.0) {
            cfg.rewardScaling = 2_500.0;
        }
        if (nearly(cfg.maxScaledHealth, 400.0) || nearly(cfg.maxScaledHealth, 1024.0)) {
            cfg.maxScaledHealth = 0.0;
        }
        if (cfg.maxDamageMultiplier < 0.0) {
            cfg.maxDamageMultiplier = 0.0;
        }
        if (cfg.maxArmorBonus < 0.0) {
            cfg.maxArmorBonus = 0.0;
        }
        if (cfg.areaDifficultyMode == null || cfg.areaDifficultyMode.isBlank()) {
            cfg.areaDifficultyMode = "weighted";
        }
        if (cfg.hardCapDifficulty < 0L || cfg.hardCapDifficulty == 1_000_000L) {
            cfg.hardCapDifficulty = 0L;
        }
        if (cfg.tierCostLevelDivisor < 1.0) {
            cfg.tierCostLevelDivisor = 1_000.0;
        }
        if (cfg.ancientCoinUpgradeChance < 0.0 || cfg.ancientCoinUpgradeChance > 1.0) {
            cfg.ancientCoinUpgradeChance = 0.02;
        }
        if (cfg.mobHealthScale <= 0.0 || cfg.mobHealthScale > 4.0) {
            cfg.mobHealthScale = 0.65;
        }
        if (cfg.transformScaleWeight < 0.0 || cfg.transformScaleWeight > 1.0
                || Double.isNaN(cfg.transformScaleWeight)) {
            cfg.transformScaleWeight = 0.25;
        }
        if (cfg.transformScaleExponent < 0.20 || cfg.transformScaleExponent > 1.0
                || Double.isNaN(cfg.transformScaleExponent)) {
            cfg.transformScaleExponent = 0.50;
        }
        if (cfg.weakStatCounterMult < 1.0) {
            cfg.weakStatCounterMult = 1.0;
        }
        if (cfg.weakDefensePierceMult < 1.0) {
            cfg.weakDefensePierceMult = 1.75;
        }
        if (cfg.tankDamageDefenseRatio < 0.0) {
            cfg.tankDamageDefenseRatio = 0.55;
        }
        if (cfg.tankDamageHealthRatio < 0.0) {
            cfg.tankDamageHealthRatio = 0.020;
        }
        if (cfg.specializationDamageTax < 0.0) {
            cfg.specializationDamageTax = 0.0;
        }
        if (cfg.classCounterDamageMult < 1.0) {
            cfg.classCounterDamageMult = 1.0;
        }
        if (cfg.classCounterDamageMult > 3.0) {
            cfg.classCounterDamageMult = 3.0;
        }
        if (cfg.classCounterHealthMult < 1.0) {
            cfg.classCounterHealthMult = 1.0;
        }
        if (cfg.classCounterHealthMult > 3.0) {
            cfg.classCounterHealthMult = 3.0;
        }
        if (cfg.classCounterArmorMult < 1.0) {
            cfg.classCounterArmorMult = 1.0;
        }
        if (cfg.classCounterArmorMult > 3.0) {
            cfg.classCounterArmorMult = 3.0;
        }
        if (cfg.strongStatCounterMult < 1.0) {
            cfg.strongStatCounterMult = 1.08;
        }
        if (cfg.strongStatCounterMult > 3.0) {
            cfg.strongStatCounterMult = 3.0;
        }
        if (cfg.raceCounterMult < 1.0) {
            cfg.raceCounterMult = 1.0;
        }
        if (cfg.raceCounterMult > 2.0) {
            cfg.raceCounterMult = 2.0;
        }
        if (cfg.maxCounterOverlayMult < 1.0) {
            cfg.maxCounterOverlayMult = 1.25;
        }
        if (cfg.maxCounterOverlayMult > 4.0) {
            cfg.maxCounterOverlayMult = 4.0;
        }
        if (cfg.defenseToArmorFactor <= 0.0) {
            cfg.defenseToArmorFactor = 2.5;
        }
        if (cfg.nearbyScaleIntervalTicks < 10) {
            cfg.nearbyScaleIntervalTicks = 40;
        }
        if (cfg.mobScaleRadius < 8.0) {
            cfg.mobScaleRadius = 8.0;
        }
        if (cfg.mobScaleRadius > 128.0) {
            cfg.mobScaleRadius = 128.0;
        }
        cfg.eliteChancePercent = Math.max(0.0, Math.min(100.0, cfg.eliteChancePercent));
        cfg.mutationChancePercent = Math.max(0.0, Math.min(100.0, cfg.mutationChancePercent));
        if (cfg.eliteStatMultiplier < 1.0) {
            cfg.eliteStatMultiplier = 1.0;
        }
        if (cfg.eliteStatMultiplier > 10.0) {
            cfg.eliteStatMultiplier = 10.0;
        }
        if (cfg.bossStatMultiplier < 1.0) {
            cfg.bossStatMultiplier = 1.0;
        }
        if (cfg.bossStatMultiplier > 10.0) {
            cfg.bossStatMultiplier = 10.0;
        }
        if (cfg.dmzExtraKiDamagePercent < 0.0) {
            cfg.dmzExtraKiDamagePercent = 0.0;
        }
        if (cfg.dmzExtraKiDamagePercent > 5.0) {
            cfg.dmzExtraKiDamagePercent = 5.0;
        }
        cfg.eliteMinUnlockTier = Math.max(0, Math.min(7, cfg.eliteMinUnlockTier));
        cfg.mutationMinUnlockTier = Math.max(0, Math.min(7, cfg.mutationMinUnlockTier));
        cfg.adaptiveAiMinUnlockTier = Math.max(0, Math.min(7, cfg.adaptiveAiMinUnlockTier));
        cfg.enemyEvolutionMinUnlockTier = Math.max(0, Math.min(7, cfg.enemyEvolutionMinUnlockTier));
        cfg.bossMechanicsMinUnlockTier = Math.max(0, Math.min(7, cfg.bossMechanicsMinUnlockTier));
        if (cfg.adminPermission == null || cfg.adminPermission.isBlank()
                || "*".equals(cfg.adminPermission.trim())) {
            cfg.adminPermission = "difficulty.admin";
        }
        // Hard product rule: at most 5 difficulty-adjusted mobs near a player.
        if (cfg.maxScaledMobsPerPlayer < 1 || cfg.maxScaledMobsPerPlayer > 5) {
            if (cfg.nearbyScaleBudgetPerPlayer >= 1 && cfg.nearbyScaleBudgetPerPlayer <= 5) {
                cfg.maxScaledMobsPerPlayer = cfg.nearbyScaleBudgetPerPlayer;
            } else {
                cfg.maxScaledMobsPerPlayer = 5;
            }
        }
        cfg.maxScaledMobsPerPlayer = Math.max(1, Math.min(5, cfg.maxScaledMobsPerPlayer));
        cfg.nearbyScaleBudgetPerPlayer = cfg.maxScaledMobsPerPlayer;
        // Never allow free tiers via live admin set / bad JSON.
        cfg.unlockTier1Cost = Math.max(1L, cfg.unlockTier1Cost);
        cfg.unlockTier2Cost = Math.max(1L, cfg.unlockTier2Cost);
        cfg.unlockTier3Cost = Math.max(1L, cfg.unlockTier3Cost);
        cfg.unlockTier4Cost = Math.max(1L, cfg.unlockTier4Cost);
        cfg.unlockTier5Cost = Math.max(1L, cfg.unlockTier5Cost);
        cfg.unlockTier6Cost = Math.max(1L, cfg.unlockTier6Cost);
        cfg.unlockTier7Cost = Math.max(1L, cfg.unlockTier7Cost);
        if (cfg.adaptiveAiMinUnlockTier < 1) {
            cfg.adaptiveAiMinUnlockTier = 1;
        }
        if (cfg.enemyEvolutionMinUnlockTier < 1) {
            cfg.enemyEvolutionMinUnlockTier = 1;
        }
        // One-time migrate old CR enemy-mult ladder (1.0/1.15/…/3.0) → player-stat percents.
        // Individual legacy remaps are NOT applied on every sanitize — that collapsed admin ladders.
        if (nearly(cfg.unlockTier1EnemyMult, 1.0)
                && nearly(cfg.unlockTier2EnemyMult, 1.15)
                && nearly(cfg.unlockTier7EnemyMult, 3.0)) {
            cfg.unlockTier1EnemyMult = 0.10;
            cfg.unlockTier2EnemyMult = 0.20;
            cfg.unlockTier3EnemyMult = 0.30;
            cfg.unlockTier4EnemyMult = 0.40;
            cfg.unlockTier5EnemyMult = 0.50;
            cfg.unlockTier6EnemyMult = 0.65;
            cfg.unlockTier7EnemyMult = 0.90;
        }
        cfg.unlockTier1EnemyMult = clampPercent(cfg.unlockTier1EnemyMult, 0.10);
        cfg.unlockTier2EnemyMult = clampPercent(cfg.unlockTier2EnemyMult, 0.20);
        cfg.unlockTier3EnemyMult = clampPercent(cfg.unlockTier3EnemyMult, 0.30);
        cfg.unlockTier4EnemyMult = clampPercent(cfg.unlockTier4EnemyMult, 0.40);
        cfg.unlockTier5EnemyMult = clampPercent(cfg.unlockTier5EnemyMult, 0.50);
        cfg.unlockTier6EnemyMult = clampPercent(cfg.unlockTier6EnemyMult, 0.65);
        cfg.unlockTier7EnemyMult = clampPercent(cfg.unlockTier7EnemyMult, 0.90);
    }

    public long paintEpoch() {
        return paintEpoch;
    }

    private static boolean nearly(double value, double expected) {
        return Math.abs(value - expected) < 1.0e-9;
    }

    public static void save() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(INSTANCE, writer);
            }
            com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier.invalidateThresholdCache();
            com.dbzlegacy.adaptivedifficulty.calc.ScalingCurves.invalidateLut();
            com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty.clearCache();
        } catch (IOException e) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] failed to save config: {}", AdaptiveDifficultyMod.MOD_ID, e.toString());
        }
    }

    /** @return false when config JSON failed to parse (previous live values kept). */
    public static boolean reload() {
        boolean ok = load();
        com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier.invalidateThresholdCache();
        com.dbzlegacy.adaptivedifficulty.calc.ScalingCurves.invalidateLut();
        com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty.clearCache();
        invalidateCombatPaintCaches();
        return ok;
    }

    /** Re-run clamps on the live instance (Bukkit admin set / hot edits). */
    public static void sanitizeLive() {
        if (INSTANCE != null) {
            normalize(INSTANCE);
        }
        invalidateCombatPaintCaches();
    }

    /** Drop profile / applied-paint caches so config retunes re-scale claimed mobs. */
    public static void invalidateCombatPaintCaches() {
        if (INSTANCE != null) {
            INSTANCE.paintEpoch++;
        }
        com.dbzlegacy.adaptivedifficulty.calc.PlayerCombatProfile.clearAll();
        com.dbzlegacy.adaptivedifficulty.calc.DmzProgression.clearAllBaseFormLevels();
        com.dbzlegacy.adaptivedifficulty.scaling.MobScaling.clearAppliedProfiles();
    }
}
