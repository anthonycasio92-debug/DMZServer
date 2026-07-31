package com.dbzlegacy.adaptivedifficulty.title;

import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import java.util.Locale;

/**
 * Cosmetic titles. Tier titles require holding the matching Unlock Tier
 * plus elevated DMZ / Prestige. Combat titles need harder kill feats.
 */
public enum DifficultyTitle {
    T1_AWAKENED("t1_awakened", "Awakened", Kind.TIER, UnlockTier.T1, 150, 2),
    T2_ENHANCED("t2_enhanced", "Enhanced", Kind.TIER, UnlockTier.T2, 750, 3),
    T3_ELITE("t3_elite", "Elite", Kind.TIER, UnlockTier.T3, 1_500, 4),
    T4_ADVANCED("t4_advanced", "Advanced", Kind.TIER, UnlockTier.T4, 7_500, 5),
    T5_MASTER("t5_master", "Master", Kind.TIER, UnlockTier.T5, 15_000, 6),
    T6_LEGENDARY("t6_legendary", "Legendary", Kind.TIER, UnlockTier.T6, 75_000, 7),
    T7_GOD("t7_god", "Godslayer", Kind.TIER, UnlockTier.T7, 150_000, 7),

    BOSS_SLAYER("boss_slayer", "Boss Slayer", Kind.COMBAT, null, 0, 0),
    ELITE_HUNTER("elite_hunter", "Elite Hunter", Kind.COMBAT, null, 0, 0),
    ASCENDANT("ascendant", "Ascendant", Kind.COMBAT, null, 0, 0);

    public enum Kind {
        TIER,
        COMBAT
    }

    public final String id;
    public final String display;
    public final Kind kind;
    public final UnlockTier unlockTier;
    /** Extra DMZ level required beyond the tier unlock (tier titles). */
    public final long requiredDmzLevel;
    /** Prestige required as an alternate path (tier titles). */
    public final int requiredPrestige;

    DifficultyTitle(
            String id, String display, Kind kind, UnlockTier unlockTier,
            long requiredDmzLevel, int requiredPrestige
    ) {
        this.id = id;
        this.display = display;
        this.kind = kind;
        this.unlockTier = unlockTier;
        this.requiredDmzLevel = requiredDmzLevel;
        this.requiredPrestige = requiredPrestige;
    }

    public static DifficultyTitle byId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        for (DifficultyTitle title : values()) {
            if (title.id.equals(key) || title.display.equalsIgnoreCase(raw.trim())) {
                return title;
            }
        }
        // Legacy ids from the old Zenith ladder.
        return switch (key) {
            case "awakened" -> T1_AWAKENED;
            case "enhanced" -> T2_ENHANCED;
            case "elite" -> T3_ELITE;
            case "advanced" -> T4_ADVANCED;
            case "master" -> T5_MASTER;
            case "legendary" -> T6_LEGENDARY;
            case "god" -> T7_GOD;
            case "legendary_hunter" -> ELITE_HUNTER;
            case "god_challenger" -> ASCENDANT;
            default -> null;
        };
    }

    public String requirementTip() {
        if (kind == Kind.COMBAT) {
            return switch (this) {
                case BOSS_SLAYER -> "Kill a boss while on Tier 5+";
                case ELITE_HUNTER -> "Kill an elite while on Tier 6+";
                case ASCENDANT -> "Get a kill while on Tier 7";
                default -> "Combat feat";
            };
        }
        if (unlockTier == null) {
            return "Locked";
        }
        return "Hold T" + unlockTier.id + " · DMZ " + requiredDmzLevel
                + " or Prestige " + requiredPrestige;
    }
}
