package com.dbzlegacy.adaptivedifficulty.calc;

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

    public String stateColorCode() {
        if (active <= 0) {
            return "a"; // green-ish / low
        }
        if (active < calculated) {
            return "a"; // green below progression
        }
        if (active <= personalMax) {
            if (active == personalMax) {
                return "6"; // orange/gold personal max
            }
            return "e"; // yellow balanced
        }
        if (active <= availableMax) {
            return "d"; // purple team boosted
        }
        return "c"; // red extreme / clamped
    }
}
