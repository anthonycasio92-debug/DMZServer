package com.dbzlegacy.adaptivedifficulty.calc;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.skills.Skills;
import net.minecraft.world.entity.player.Player;

/**
 * Reads DMZ progression for V3 unlocks + combat rating.
 */
public final class DmzProgression {
    private DmzProgression() {}

    public static StatsData stats(Player player) {
        if (player == null) {
            return null;
        }
        try {
            return StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static int dmzLevel(Player player) {
        StatsData data = stats(player);
        if (data == null) {
            return 1;
        }
        try {
            return Math.max(1, data.getLevel());
        } catch (Throwable ignored) {
            return 1;
        }
    }

    /**
     * DMZ prestige skill level (synced from Fabled Prestige class level - 1).
     */
    public static int prestige(Player player) {
        StatsData data = stats(player);
        if (data == null) {
            return 0;
        }
        try {
            Skills skills = data.getSkills();
            if (skills == null) {
                return 0;
            }
            return Math.max(0, skills.getSkillLevel("prestige"));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /**
     * Transformation / form power contribution for Combat Rating.
     * Uses battle power (includes form multipliers) scaled into CR units.
     */
    public static double transformationPower(Player player) {
        StatsData data = stats(player);
        if (data == null) {
            return 0.0;
        }
        try {
            double bp = data.getBattlePowerExact();
            if (!(bp > 0.0) || Double.isNaN(bp) || Double.isInfinite(bp)) {
                bp = data.getBattlePower();
            }
            // Keep CR readable — raw BP can be multi-million.
            return Math.max(0.0, bp / 1000.0);
        } catch (Throwable ignored) {
            return 0.0;
        }
    }
}
