package com.dbzlegacy.adaptivedifficulty.title;

import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import java.util.Locale;

/**
 * Titles are a secondary progression layer around Adaptive Difficulty.
 * Equipped titles grant small capped perks; mastery / score unlock permanent rewards.
 */
public enum DifficultyTitle {
    T1_AWAKENED("t1_awakened", "Awakened", Kind.TIER, TitleRarity.COMMON, UnlockTier.T1, 150, 2, 10,
            Perk.COIN_DROP, 0.03),
    T2_ENHANCED("t2_enhanced", "Enhanced", Kind.TIER, TitleRarity.COMMON, UnlockTier.T2, 750, 3, 10,
            Perk.COIN_DROP, 0.04),
    T3_ELITE("t3_elite", "Elite", Kind.TIER, TitleRarity.COMMON, UnlockTier.T3, 1_500, 4, 10,
            Perk.ELITE_DAMAGE, 0.04),
    T4_ADVANCED("t4_advanced", "Advanced", Kind.TIER, TitleRarity.RARE, UnlockTier.T4, 7_500, 5, 10,
            Perk.ELITE_DAMAGE, 0.06),
    T5_MASTER("t5_master", "Master", Kind.TIER, TitleRarity.RARE, UnlockTier.T5, 15_000, 6, 10,
            Perk.MUTATION_DAMAGE, 0.07),
    T6_LEGENDARY("t6_legendary", "Legendary", Kind.TIER, TitleRarity.EPIC, UnlockTier.T6, 75_000, 7, 10,
            Perk.BOSS_DAMAGE, 0.08),
    T7_GOD("t7_god", "Godslayer", Kind.TIER, TitleRarity.LEGENDARY, UnlockTier.T7, 150_000, 7, 10,
            Perk.BOSS_DAMAGE, 0.10),

    BOSS_SLAYER("boss_slayer", "Boss Slayer", Kind.COMBAT, TitleRarity.EPIC, null, 0, 0, 50,
            Perk.BOSS_COIN, 0.07),
    ELITE_HUNTER("elite_hunter", "Elite Hunter", Kind.COMBAT, TitleRarity.RARE, null, 0, 0, 25,
            Perk.ELITE_REWARD, 0.07),
    ASCENDANT("ascendant", "Ascendant", Kind.COMBAT, TitleRarity.LEGENDARY, null, 0, 0, 100,
            Perk.COIN_MULT, 0.04),

    MUTATION_HUNTER("mutation_hunter", "Mutation Hunter", Kind.CHALLENGE, TitleRarity.EPIC, null, 0, 0, 50,
            Perk.MUTATION_DAMAGE, 0.05),
    UNTOUCHABLE("untouchable", "Untouchable", Kind.CHALLENGE, TitleRarity.MYTHIC, null, 0, 0, 75,
            Perk.AD_DAMAGE, 0.02),
    IMMORTAL("immortal", "Immortal", Kind.CHALLENGE, TitleRarity.MYTHIC, null, 0, 0, 75,
            Perk.AD_DAMAGE, 0.025),
    COIN_LORD("coin_lord", "Coin Lord", Kind.CHALLENGE, TitleRarity.MYTHIC, null, 0, 0, 75,
            Perk.COIN_MULT, 0.04),
    WORLD_BREAKER("worldbreaker", "Worldbreaker", Kind.CHALLENGE, TitleRarity.MYTHIC, null, 0, 0, 100,
            Perk.AD_DAMAGE, 0.03),
    SURVIVOR("survivor", "Survivor", Kind.CHALLENGE, TitleRarity.MYTHIC, null, 0, 0, 50,
            Perk.COIN_DROP, 0.025);

    public enum Kind {
        TIER,
        COMBAT,
        CHALLENGE
    }

    /** Equipped-title perk family. Values are fractional (0.05 = +5%). */
    public enum Perk {
        NONE,
        COIN_DROP,
        COIN_MULT,
        ELITE_DAMAGE,
        MUTATION_DAMAGE,
        BOSS_DAMAGE,
        AD_DAMAGE,
        BOSS_COIN,
        ELITE_REWARD
    }

    public final String id;
    public final String display;
    public final Kind kind;
    public final TitleRarity rarity;
    public final UnlockTier unlockTier;
    public final long requiredDmzLevel;
    public final int requiredPrestige;
    /** Base Title Score when unlocked (mastery adds more). */
    public final int scorePoints;
    public final Perk perk;
    public final double perkValue;

    DifficultyTitle(
            String id, String display, Kind kind, TitleRarity rarity, UnlockTier unlockTier,
            long requiredDmzLevel, int requiredPrestige, int scorePoints,
            Perk perk, double perkValue
    ) {
        this.id = id;
        this.display = display;
        this.kind = kind;
        this.rarity = rarity;
        this.unlockTier = unlockTier;
        this.requiredDmzLevel = requiredDmzLevel;
        this.requiredPrestige = requiredPrestige;
        this.scorePoints = scorePoints;
        this.perk = perk;
        this.perkValue = perkValue;
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
        return switch (key) {
            case "awakened" -> T1_AWAKENED;
            case "enhanced" -> T2_ENHANCED;
            case "elite" -> T3_ELITE;
            case "advanced" -> T4_ADVANCED;
            case "master" -> T5_MASTER;
            case "legendary" -> T6_LEGENDARY;
            case "god", "godslayer" -> T7_GOD;
            case "legendary_hunter" -> ELITE_HUNTER;
            case "god_challenger" -> ASCENDANT;
            case "world_breaker" -> WORLD_BREAKER;
            default -> null;
        };
    }

    public boolean supportsMastery() {
        return this == ELITE_HUNTER || this == BOSS_SLAYER || this == ASCENDANT;
    }

    public String masteryDisplay(int level) {
        if (!supportsMastery() || level <= 0) {
            return display;
        }
        return display + " " + toRoman(Math.min(TitleProgress.MASTERY_MAX, level));
    }

    public String perkTip(int masteryLevel) {
        double value = TitleEffects.equippedPerkValue(this, masteryLevel);
        String pct = String.format(Locale.ROOT, "%.1f", value * 100.0);
        String specific = switch (perk) {
            case COIN_DROP -> "+" + pct + "% Ancient Coin drop chance";
            case COIN_MULT -> "+" + pct + "% Ancient Coin quantity";
            case ELITE_DAMAGE -> "+" + pct + "% damage vs Elites";
            case MUTATION_DAMAGE -> "+" + pct + "% damage vs Mutations";
            case BOSS_DAMAGE -> "+" + pct + "% damage vs Bosses";
            case AD_DAMAGE -> "+" + pct + "% damage vs Adaptive enemies";
            case BOSS_COIN -> "+" + pct + "% boss coin / loot chance";
            case ELITE_REWARD -> "+" + pct + "% Elite Ancient Coin drop chance";
            default -> "No passive";
        };
        String presence = TitleEffects.presenceTipShort(this);
        if (presence == null || presence.isBlank()) {
            return specific;
        }
        return specific + " §8| " + presence;
    }

    public String requirementTip() {
        return switch (this) {
            case BOSS_SLAYER -> "Kill a boss while on Tier 5+";
            case ELITE_HUNTER -> "Kill an elite while on Tier 6+";
            case ASCENDANT -> "Ever unlocked T7 + 100 T7 elites + 25 T7 bosses + 100 no-death T7 kills + all mutations";
            case MUTATION_HUNTER -> "Kill every mutation type";
            case UNTOUCHABLE -> "Kill 100 T6+ enemies without dying";
            case IMMORTAL -> "Kill 1,000 Adaptive enemies without dying";
            case COIN_LORD -> "Earn 1,000,000 Ancient Coin value from kills";
            case WORLD_BREAKER -> "Kill 10,000 T7 enemies";
            case SURVIVOR -> "Keep an active tier for 24h of playtime";
            default -> {
                if (unlockTier == null) {
                    yield "Locked";
                }
                yield "DMZ " + requiredDmzLevel + " or Prestige " + requiredPrestige
                        + " §8(keeps after lowering tier)";
            }
        };
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}
