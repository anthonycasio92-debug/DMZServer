package com.dbzlegacy.adaptivedifficulty.title;

import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import java.util.Locale;

/**
 * Cosmetic titles unlocked by reaching difficulty tiers or rare combat feats.
 * Stored / equipped by {@link #id}; {@link #display} is what players see.
 */
public enum DifficultyTitle {
    AWAKENED("awakened", "Awakened", Kind.TIER, DifficultyTier.AWAKENED),
    ENHANCED("enhanced", "Enhanced", Kind.TIER, DifficultyTier.ENHANCED),
    ELITE("elite", "Elite", Kind.TIER, DifficultyTier.ELITE),
    ADVANCED("advanced", "Advanced", Kind.TIER, DifficultyTier.ADVANCED),
    MASTER("master", "Master", Kind.TIER, DifficultyTier.MASTER),
    LEGENDARY("legendary", "Legendary", Kind.TIER, DifficultyTier.LEGENDARY),
    GOD("god", "God", Kind.TIER, DifficultyTier.GOD),
    DIVINE("divine", "Divine", Kind.TIER, DifficultyTier.DIVINE),
    IMPOSSIBLE("impossible", "Impossible", Kind.TIER, DifficultyTier.IMPOSSIBLE),
    TRANSCENDENT("transcendent", "Transcendent", Kind.TIER, DifficultyTier.TRANSCENDENT),
    ETERNAL("eternal", "Eternal", Kind.TIER, DifficultyTier.ETERNAL),
    MYTHIC("mythic", "Mythic", Kind.TIER, DifficultyTier.MYTHIC),
    OMEGA("omega", "Omega", Kind.TIER, DifficultyTier.OMEGA),
    ABSOLUTE("absolute", "Absolute", Kind.TIER, DifficultyTier.ABSOLUTE),
    APEX("apex", "Apex", Kind.TIER, DifficultyTier.APEX),
    ZENITH("zenith", "Zenith", Kind.TIER, DifficultyTier.ZENITH),

    BOSS_SLAYER("boss_slayer", "Boss Slayer", Kind.COMBAT, null),
    LEGENDARY_HUNTER("legendary_hunter", "Legendary Hunter", Kind.COMBAT, null),
    GOD_CHALLENGER("god_challenger", "God Challenger", Kind.COMBAT, null);

    public enum Kind {
        TIER,
        COMBAT
    }

    public final String id;
    public final String display;
    public final Kind kind;
    /** For tier titles: the difficulty tier that unlocks this title. */
    public final DifficultyTier unlockTier;

    DifficultyTitle(String id, String display, Kind kind, DifficultyTier unlockTier) {
        this.id = id;
        this.display = display;
        this.kind = kind;
        this.unlockTier = unlockTier;
    }

    public static DifficultyTitle byId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        for (DifficultyTitle title : values()) {
            if (title.id.equals(key)) {
                return title;
            }
        }
        // Legacy unlock strings used display names.
        for (DifficultyTitle title : values()) {
            if (title.display.equalsIgnoreCase(raw.trim())) {
                return title;
            }
        }
        return null;
    }

    public static DifficultyTitle forTier(DifficultyTier tier) {
        if (tier == null || tier == DifficultyTier.NONE) {
            return null;
        }
        for (DifficultyTitle title : values()) {
            if (title.kind == Kind.TIER && title.unlockTier == tier) {
                return title;
            }
        }
        return null;
    }
}
