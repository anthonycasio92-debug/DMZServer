package com.dbzlegacy.adaptivedifficulty.calc;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.skills.Skills;
import net.minecraft.world.entity.player.Player;

/**
 * Reads DMZ progression the same way current Dragon-Mine-Z scripts do
 * ({@code Fabled Sync.js} on branch {@code cursor/rival-sparring-reaudit-dd5f}):
 * {@code StatsData.getLevel()} and skill {@code prestige}
 * (Fabled Prestige class level - 1).
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
     * DMZ prestige skill level (synced from Fabled Prestige class level - 1 in your scripts).
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
}
