package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Combat Rating for rewards, display, and area readouts.
 * Nearby mob fight stats use {@link PlayerCombatProfile}, not CR.
 * <p>
 * Transform / form contribution is derived from <b>released combat statistics</b>
 * (live STR/SKP/PWR/ENE/RES/VIT channels × power release), not raw DMZ battle power.
 * Androids and similar races can report Inf / absurd BP; sparring already leans on
 * release-aware reads — CR follows the same idea with CombatSanity clamps.
 */
public final class CombatRating {
    /**
     * Absolute CR ceiling used when {@code hardCapDifficulty} is 0.
     * Prevents {@link Math#round(double)} from returning {@link Long#MAX_VALUE}.
     */
    public static final long DISPLAY_ABS_CAP = 1_000_000_000_000_000L; // 1e15

    /** DMZ BP above this is treated as broken (Android / form overflow) — use stat proxy. */
    public static final double SAFE_DMZ_BATTLE_POWER_MAX = 50_000_000_000.0; // 5e10

    /** Scale released-stat power into the old {@code BP/1000} CR transform units. */
    private static final double STAT_POWER_TO_TRANSFORM = 1_000.0;

    private static final double ENERGY_OFFENSE_FACTOR = 0.08;

    private CombatRating() {}

    public static long compute(
            int dmzLevel,
            int prestige,
            long activeDifficulty,
            double transformationPower,
            DifficultyConfig cfg
    ) {
        if (cfg == null) {
            cfg = DifficultyConfig.get();
        }
        double transform = transformationPower;
        if (!Double.isFinite(transform) || transform < 0.0) {
            transform = 0.0;
        }
        double cr = (Math.max(0, dmzLevel) * saneWeight(cfg.combatRatingDmzWeight))
                + (Math.max(0, prestige) * saneWeight(cfg.combatRatingPrestigeWeight))
                + transform * saneWeight(cfg.combatRatingTransformWeight)
                + (Math.max(0L, activeDifficulty) * saneWeight(cfg.combatRatingDifficultyWeight));
        if (!Double.isFinite(cr) || cr < 0.0) {
            cr = 0.0;
        }
        long absCap = DISPLAY_ABS_CAP;
        long cfgCap = Math.max(0L, cfg.hardCapDifficulty);
        if (cfgCap > 0L) {
            absCap = Math.min(absCap, cfgCap);
        }
        if (cr > (double) absCap) {
            cr = absCap;
        }
        long value = Math.round(cr);
        if (value < 0L) {
            // Math.round overflow → Long.MAX_VALUE; treat as capped.
            return absCap;
        }
        return Math.min(value, absCap);
    }

    private static double saneWeight(double w) {
        if (!Double.isFinite(w) || w < 0.0) {
            return 0.0;
        }
        return w;
    }

    public static long of(ServerPlayer player, PlayerDifficultyData data) {
        DifficultyConfig cfg = DifficultyConfig.get();
        int level = DmzProgression.tierScalingDmzLevel(player);
        int prestige = DmzProgression.prestige(player);
        long active = data == null ? 0L : data.getActiveDifficultyLevel();
        double transform = DmzProgression.transformationPower(player);
        return compute(level, prestige, active, transform, cfg);
    }

    /**
     * CR transform term from released combat stats (sparring-style), not raw DMZ BP.
     * Units match the historical {@code battlePower/1000} scale for normal players.
     */
    public static double transformFromReleasedStats(Player player) {
        double power = releasedStatPower(player);
        if (!(power > 1.0) || !Double.isFinite(power)) {
            return 0.0;
        }
        double scaled = power / STAT_POWER_TO_TRANSFORM;
        if (!Double.isFinite(scaled) || scaled < 0.0) {
            return 0.0;
        }
        return Math.min(scaled, 100_000_000_000_000.0); // 1e14
    }

    /**
     * Own BP-like rating from <b>live</b> combat channels × power release.
     * Safe for Androids whose {@code getBattlePowerExact()} overflows.
     * <p>
     * Channels are DMZ live getters ({@code getMeleeDamage}, etc.) which already
     * include active form/stack multipliers — so CR rises in form and falls when
     * dropping to base. Nearby mob scaling does <b>not</b> use this value; it uses
     * {@link PlayerCombatProfile} (soft + liveOffense + formBoost) instead.
     */
    public static double releasedStatPower(Player player) {
        if (player == null) {
            return 0.0;
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return 0.0;
        }
        try {
            // Live form-included channels (same sources PlayerCombatProfile paints from).
            double melee = LmOverhaulScaledCombat.melee(data);
            double strike = LmOverhaulScaledCombat.strike(data);
            double ki = LmOverhaulScaledCombat.ki(data);
            double energy = LmOverhaulScaledCombat.energy(data);
            double def = LmOverhaulScaledCombat.defense(data);
            double hp = LmOverhaulScaledCombat.health(data);

            double offense = blended(melee, strike, ki, Math.max(1.0, energy * ENERGY_OFFENSE_FACTOR));
            double bulk = blended(def, hp, def, hp);
            double releasePct = 100.0;
            try {
                if (player instanceof ServerPlayer sp) {
                    releasePct = DmzRewards.powerReleasePercent(sp);
                }
            } catch (Throwable ignored) {
            }
            // Sparring clamps release into a 100–200% band for mults; for rating we
            // allow 50–200% so suppressed release still counts, full release boosts.
            double releaseFactor = Math.max(0.5, Math.min(2.0, releasePct / 100.0));
            double raw = (offense * 0.72 + bulk * 0.28) * releaseFactor;
            if (!Double.isFinite(raw) || raw < 0.0) {
                return 0.0;
            }
            return Math.min(raw, CombatSanity.maxLiveChannel() * 2.0);
        } catch (Throwable ignored) {
            return 0.0;
        }
    }

    /**
     * Prefer finite DMZ BP when sane; otherwise released-stat power (Android-safe).
     * Used by sparring / rival helpers that previously trusted raw BP alone.
     */
    public static double safeBattlePower(Player player) {
        if (player == null) {
            return 0.0;
        }
        try {
            StatsData data = DmzProgression.stats(player);
            // Overhaul BP weights max melee/strike/ki (scaled) but raw defense.
            // When prestige scale is on, use live post-scale channels for spar/rival/AD.
            if (data != null && LmOverhaulScaledCombat.scaled(data)) {
                return releasedStatPower(player);
            }
            if (data != null) {
                double exact = data.getBattlePowerExact();
                if (Double.isFinite(exact) && exact > 0.0 && exact <= SAFE_DMZ_BATTLE_POWER_MAX) {
                    return exact;
                }
                float bp = data.getBattlePower();
                if (Double.isFinite(bp) && bp > 0.0f && bp <= SAFE_DMZ_BATTLE_POWER_MAX) {
                    return bp;
                }
            }
        } catch (Throwable ignored) {
        }
        return releasedStatPower(player);
    }

    private static double blended(double a, double b, double c, double d) {
        double w = Math.max(1.0, a);
        double x = Math.max(1.0, b);
        double y = Math.max(1.0, c);
        double z = Math.max(1.0, d);
        double peak = Math.max(w, Math.max(x, Math.max(y, z)));
        double avg = (w + x + y + z) * 0.25;
        return peak * 0.55 + avg * 0.45;
    }

    @FunctionalInterface
    private interface DoubleSupply {
        double get() throws Throwable;
    }

    private static double read(DoubleSupply supply, double fallback) {
        try {
            return supply.get();
        } catch (Throwable t) {
            return fallback;
        }
    }
}
