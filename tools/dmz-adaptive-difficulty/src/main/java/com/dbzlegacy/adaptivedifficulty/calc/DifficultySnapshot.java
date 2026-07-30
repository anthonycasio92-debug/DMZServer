package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;

/** Cached view of a player's difficulty numbers. */
public final class DifficultySnapshot {
    public final int dmzLevel;
    public final int prestige;
    public final long calculated;
    public final long purchased;
    public final long personalMax;
    public final long teamThresholdBonus;
    public final long teamContribution;
    public final long availableMax;
    public final long active;
    public final TeamMode teamMode;

    public DifficultySnapshot(
            int dmzLevel,
            int prestige,
            long calculated,
            long purchased,
            long personalMax,
            long teamThresholdBonus,
            long teamContribution,
            long availableMax,
            long active,
            TeamMode teamMode
    ) {
        this.dmzLevel = dmzLevel;
        this.prestige = prestige;
        this.calculated = calculated;
        this.purchased = purchased;
        this.personalMax = personalMax;
        this.teamThresholdBonus = teamThresholdBonus;
        this.teamContribution = teamContribution;
        this.availableMax = availableMax;
        this.active = active;
        this.teamMode = teamMode;
    }

    /**
     * Concept §8 state colors:
     * Green below progression, Yellow balanced, Orange personal max,
     * Purple team-boosted, Red extreme (full team ceiling or optional admin hardcap).
     */
    public String stateColorCode() {
        return switch (state()) {
            case "Below" -> "a";
            case "Balanced" -> "e";
            case "Personal Max" -> "6";
            case "Team Boosted" -> "d";
            case "Extreme" -> "c";
            default -> "f";
        };
    }

    public String state() {
        if (active <= 0 || active < calculated) {
            return "Below";
        }
        long hardCap = Math.max(0L, DifficultyConfig.get().hardCapDifficulty);
        boolean atHardCap = hardCap > 0 && active >= hardCap;
        boolean fullTeamCeiling = availableMax > personalMax && active >= availableMax;
        if (atHardCap || fullTeamCeiling) {
            return "Extreme";
        }
        if (active > personalMax) {
            return "Team Boosted";
        }
        // At theoretical (stats) + purchased ceiling
        if (active >= personalMax && personalMax > 0) {
            return "Personal Max";
        }
        return "Balanced";
    }
}
