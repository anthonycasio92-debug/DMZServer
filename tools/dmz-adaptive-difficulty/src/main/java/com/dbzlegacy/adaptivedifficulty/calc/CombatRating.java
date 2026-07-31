package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import net.minecraft.server.level.ServerPlayer;

/**
 * V3 Combat Rating — primary enemy-scaling input.
 * Cached via {@link DifficultySnapshot}; recalculate only on progression/difficulty/team changes.
 */
public final class CombatRating {
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
        double cr = (Math.max(0, dmzLevel) * cfg.combatRatingDmzWeight)
                + (Math.max(0, prestige) * cfg.combatRatingPrestigeWeight)
                + Math.max(0.0, transformationPower) * cfg.combatRatingTransformWeight
                + (Math.max(0L, activeDifficulty) * cfg.combatRatingDifficultyWeight);
        long value = Math.round(Math.max(0.0, cr));
        long cap = Math.max(0L, cfg.hardCapDifficulty);
        return cap > 0 ? Math.min(value, cap) : value;
    }

    public static long of(ServerPlayer player, PlayerDifficultyData data) {
        DifficultyConfig cfg = DifficultyConfig.get();
        int level = DmzProgression.dmzLevel(player);
        int prestige = DmzProgression.prestige(player);
        long active = data == null ? 0L : data.getActiveDifficultyLevel();
        double transform = DmzProgression.transformationPower(player);
        return compute(level, prestige, active, transform, cfg);
    }
}
