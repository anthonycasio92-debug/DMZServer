package com.dbzlegacy.adaptivedifficulty.title;

import java.util.List;

/**
 * Permanent Title Score milestones. Claimed once; bonuses stack with equipped title perks
 * under hard caps in {@link TitleEffects}.
 */
public enum TitleScoreRewards {
    COIN_SEEKER(100, "Coin Seeker", "Permanent +5% Ancient Coin drop chance"),
    VETERAN(250, "Veteran", "Permanent +1% damage vs Adaptive enemies"),
    CHAMPION(500, "Champion", "Permanent +2% Ancient Coin quantity"),
    CONQUEROR(1_000, "Conqueror", "Permanent +3% damage vs Elites and Mutations"),
    DREADED(2_500, "Dreaded", "Permanent +5% Boss damage"),
    ASCENDANT_MARK(5_000, "Ascendant Mark", "Exclusive Title Score nameplate mark");

    public final int scoreRequired;
    public final String display;
    public final String tip;

    TitleScoreRewards(int scoreRequired, String display, String tip) {
        this.scoreRequired = scoreRequired;
        this.display = display;
        this.tip = tip;
    }

    public static List<TitleScoreRewards> ordered() {
        return List.of(values());
    }
}
