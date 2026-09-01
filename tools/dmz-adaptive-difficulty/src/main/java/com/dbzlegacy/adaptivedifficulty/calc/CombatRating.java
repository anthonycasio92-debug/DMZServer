package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Combat Rating for rewards, display, and area readouts.
 * Nearby mob fight stats use {@link PlayerCombatProfile}, not CR.
 */
public final class CombatRating {
    /**
     * Absolute CR ceiling used when {@code hardCapDifficulty} is 0.
     * Prevents {@link Math#round(double)} from returning {@link Long#MAX_VALUE}
     * when form battle power overflows into the transform term.
     */
    public static final long DISPLAY_ABS_CAP = 1_000_000_000_000_000L; // 1e15

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
        int level = DmzProgression.dmzLevelForProgression(player);
        int prestige = DmzProgression.prestige(player);
        long active = data == null ? 0L : data.getActiveDifficultyLevel();
        double transform = DmzProgression.transformationPower(player);
        return compute(level, prestige, active, transform, cfg);
    }
}
