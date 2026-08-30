package com.dbzlegacy.adaptivedifficulty.rival;

/** Constants from Rival System 4.7.10. */
public final class RivalConstants {
    private RivalConstants() {}

    public static final int NEMESIS_DEATH_LOSSES = 3;
    public static final int MAX_MUTUAL_RIVALS = 2;
    public static final long REQUEST_EXPIRE_MS = 7L * 24L * 60L * 60L * 1000L;
    public static final long DECLARE_COOLDOWN_MS = 30_000L;

    public static final double GLOBAL_TP_SCALE = 0.60;
    public static final boolean OFFENSE_ENABLED = false;

    public static final int KILL_TP_BASE = 400;
    public static final int KILL_TP_PER_TIER = 120;
    public static final double KILL_TP_MUTUAL_MULT = 1.45;
    public static final int NEAR_RIVAL_KILL_CAP = 2;

    public static final int UNDERDOG_ENGAGE_TP = 250;
    public static final long UNDERDOG_ENGAGE_COOLDOWN_MS = 12_000L;
    public static final int UNDERDOG_WIN_TP = 6500;
    public static final int UNDERDOG_DEATH_RP = 30;

    public static final double ANTIGANK_RATIO = 0.40;
    public static final int ANTIGANK_WITNESS_KILL_TP = 350;
    public static final int ANTIGANK_HIT_TP = 140;
    public static final long ANTIGANK_HIT_COOLDOWN_MS = 15_000L;

    public static final int PRESENCE_TP_ONE_SIDED = 180;
    public static final int PRESENCE_TP_MUTUAL = 280;
    public static final int PRESENCE_TP_NEMESIS = 360;
    public static final long PRESENCE_INTERVAL_MS = 60_000L;
    public static final int PRESENCE_CAP = 2;
    public static final long ACTIVE_KILL_MS = 30_000L;

    public static final double BASE_RANGE = 48.0;
    public static final double RANGE_PER_TIER = 8.0;
    public static final double MAX_RANGE = 160.0;
    public static final long PROXIMITY_PULSE_MS = 5_000L;

    public static final int SURPASS_TP = 8000;
    public static final long SURPASS_COOLDOWN_MS = 6L * 60L * 60L * 1000L;
    public static final long SURPASS_GLOBAL_COOLDOWN_MS = 60L * 60L * 1000L;

    public static final long INSTINCT_PULSE_MS = 10_000L;
    public static final long INSTINCT_ARRIVE_CD_MS = 90_000L;
    public static final long INSTINCT_STATUS_CD_MS = 120_000L;
    public static final long INSTINCT_EVENT_CD_MS = 20_000L;

    public static final long CH_REQUEST_EXPIRE_MS = 30_000L;
    public static final long CH_REQUEST_COOLDOWN_MS = 15_000L;
    public static final long CH_COUNTDOWN_MS = 5_000L;
    public static final long CH_DURATION_MS = 60_000L;
    public static final int CH_MIN_MINUTES = 1;
    public static final int CH_MAX_MINUTES = 10;
    public static final long CH_BETWEEN_COOLDOWN_MS = 2L * 60L * 1000L;
    public static final double CH_MAX_DISTANCE = 64.0;
    public static final long CH_TICK_MS = 250L;
    public static final long CH_PENDING_RESOLVE_MS = 75L;
    public static final long CH_BROADCAST_SCORE_MS = 15_000L;
    /** Planned fights longer than this use the slower live-score cadence. */
    public static final long CH_LONG_FIGHT_MS = 2L * 60L * 1000L;
    public static final long CH_BROADCAST_SCORE_LONG_MS = 60_000L;

    public static final int CH_WIN_TP = 5500;
    public static final int CH_LOSE_TP = 2000;
    public static final int CH_DRAW_TP = 3000;
    public static final int CH_KO_WIN_TP_BONUS = 4000;
    public static final int CH_NON_RIVAL_WIN_TP = 3200;

    public static final int CH_WIN_RP = 18;
    public static final int CH_LOSE_RP = 40;
    public static final int CH_DRAW_RP = 14;
    public static final int CH_KO_LOSE_RP_BONUS = 20;
    public static final int CH_CLOSE_BATTLE_RP = 12;
    public static final double CH_CLOSE_BATTLE_RATIO = 0.85;
    public static final int CH_LONG_RIVALRY_DAY_RP = 2;
    public static final int CH_LONG_RIVALRY_DAY_CAP = 30;
    public static final int CH_FORFEIT_RP_PENALTY = 10;

    public static final RpTier[] RP_TIERS = {
            new RpTier(0, "Acquaintance", "7", 1.00, "None"),
            new RpTier(100, "Competitor", "a", 1.05, "Sense farther + 5% rival TP"),
            new RpTier(300, "Adversary", "2", 1.10, "Better reports + 10% rival TP"),
            new RpTier(700, "Rival", "e", 1.15, "Notifications + 15% rival TP"),
            new RpTier(1500, "Vendetta", "6", 1.25, "Tracker + 25% rival TP"),
            new RpTier(3000, "Legendary", "c", 1.35, "Aura flag + 35% rival TP"),
            new RpTier(5000, "Arch Rival", "d", 1.45, "Entrance flag + 45% rival TP"),
            new RpTier(7500, "Mortal Enemy", "5", 1.55, "Priority alerts + 55% rival TP"),
            new RpTier(10000, "Eternal Rival", "b", 1.70, "Unique title + 70% rival TP"),
            new RpTier(15000, "Mythic Rival", "4", 2.00, "Mythic title + 100% rival TP")
    };

    /**
     * RP rank title (not Mutual Nemesis status).
     *
     * @param tpMult rival TP award multiplier for this title (1.0 = 100%)
     * @param perk   player-facing bonus text for the Title GUI
     */
    public record RpTier(int min, String name, String color, double tpMult, String perk) {}

    public static RpTier tierFor(double points) {
        RpTier best = RP_TIERS[0];
        for (RpTier tier : RP_TIERS) {
            if (points >= tier.min) {
                best = tier;
            }
        }
        return best;
    }

    public static int tierIndex(double points) {
        int idx = 0;
        for (int i = 0; i < RP_TIERS.length; i++) {
            if (points >= RP_TIERS[i].min) {
                idx = i;
            }
        }
        return idx;
    }

    public static double rangeForPoints(double points) {
        return Math.min(MAX_RANGE, BASE_RANGE + RANGE_PER_TIER * tierIndex(points));
    }

    public static float scaleTp(float amount) {
        return (float) Math.max(0.0, Math.floor(amount * GLOBAL_TP_SCALE));
    }
}
