package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Stats;
import net.minecraft.server.level.ServerPlayer;

/**
 * Invested STR = {@code getStrength() - race/class baseStats.STR}.
 * Matches SprintJump.js 1.1.0.
 */
public final class InvestedStrength {
    private InvestedStrength() {}

    public static int points(ServerPlayer player) {
        try {
            StatsData data = DmzProgression.stats(player);
            if (data == null) {
                return 0;
            }
            Stats stats = data.getStats();
            if (stats == null) {
                return 0;
            }
            int total = Math.max(0, stats.getStrength());
            int base = raceClassBaseStrength(data);
            return Math.max(0, total - base);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static int raceClassBaseStrength(StatsData data) {
        try {
            Character character = data.getCharacter();
            if (character == null) {
                return 0;
            }
            String race = character.getRaceName();
            if (race == null || race.isBlank()) {
                race = character.getRace();
            }
            String cls = character.getCharacterClass();
            if (race == null || race.isBlank() || cls == null || cls.isBlank()) {
                return 0;
            }
            RaceStatsConfig raceStats = ConfigManager.getRaceStats(String.valueOf(race));
            if (raceStats == null) {
                return 0;
            }
            RaceStatsConfig.ClassStats classStats = raceStats.getClassStats(String.valueOf(cls));
            if (classStats == null) {
                return 0;
            }
            RaceStatsConfig.BaseStats base = classStats.getBaseStats();
            if (base == null || base.getStrength() == null) {
                return 0;
            }
            return Math.max(0, base.getStrength());
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
