package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;

/** Maps DMZ fighting class across race changes (per-race {@code stats.json} class defs). */
public final class RaceChangeClassMapper {
    private static final String[] PREFERRED_FALLBACK =
            new String[] {"warrior", "cleric", "martialartist", "berserker", "assassin"};

    private RaceChangeClassMapper() {}

    /**
     * Keep the same class id when the target race defines it; otherwise pick the first preferred
     * class available on that race, or the first listed class.
     */
    public static String resolveClassForRace(String priorClassId, String newRaceId) {
        String prior = normalize(priorClassId);
        List<String> allowed = DmzContentDiscovery.classIdsForRace(newRaceId);
        if (allowed.isEmpty()) {
            return prior.isEmpty() ? "warrior" : prior;
        }
        if (!prior.isEmpty()) {
            String canonical = canonicalId(prior, allowed);
            if (canonical != null) {
                return canonical;
            }
        }
        for (String pref : PREFERRED_FALLBACK) {
            for (String id : allowed) {
                if (pref.equalsIgnoreCase(id)) {
                    return id;
                }
            }
        }
        return allowed.get(0);
    }

    /**
     * After a race change: keep the class already on the character when it exists on the new race;
     * otherwise map from the prior class id (same role on the old race).
     */
    public static String resolveClassForRaceAfterChange(
            String currentClassOnCharacter, String priorClassId, String newRaceId) {
        List<String> allowed = DmzContentDiscovery.classIdsForRace(newRaceId);
        if (allowed.isEmpty()) {
            return resolveClassForRace(priorClassId, newRaceId);
        }
        String canonical = canonicalId(currentClassOnCharacter, allowed);
        if (canonical != null) {
            return canonical;
        }
        return resolveClassForRace(priorClassId, newRaceId);
    }

    /**
     * Target race does not offer the player's current class — they must pick one (free with race
     * change) instead of auto-remapping to a fallback.
     */
    public static boolean requiresClassPicker(String currentClassId, String newRaceId) {
        List<String> allowed = DmzContentDiscovery.classIdsForRace(newRaceId);
        if (allowed.isEmpty()) {
            return true;
        }
        if (currentClassId == null || currentClassId.isBlank()) {
            return true;
        }
        return canonicalId(currentClassId, allowed) == null;
    }

    /** True when {@code classId} is defined on that race's {@code stats.json}. */
    public static boolean isClassValidForRace(String raceId, String classId) {
        if (raceId == null || raceId.isBlank() || classId == null || classId.isBlank()) {
            return false;
        }
        return canonicalId(classId, DmzContentDiscovery.classIdsForRace(raceId)) != null;
    }

    private static String canonicalId(String classId, List<String> allowed) {
        if (classId == null || classId.isBlank() || allowed == null || allowed.isEmpty()) {
            return null;
        }
        String want = normalize(classId);
        for (String id : allowed) {
            if (want.equalsIgnoreCase(id)) {
                return id;
            }
        }
        return null;
    }

    /**
     * Canonical fighting class id for {@code raceId}, preferring {@code preferredClassId} when it
     * exists on that race, otherwise mapping from {@code fallbackPriorClassId}.
     */
    public static String fightingClassForRace(
            String preferredClassId, String fallbackPriorClassId, String raceId) {
        return resolveClassForRaceAfterChange(preferredClassId, fallbackPriorClassId, raceId);
    }

    /**
     * Sets race + fighting class on DMZ character data so class passives/base stats use the
     * <em>new</em> race's {@code stats.json} entry for that class id.
     *
     * @param preferredClassId class to keep when valid on {@code newRaceId} (often current pick)
     * @param fallbackPriorClassId used only when {@code preferredClassId} is invalid on the race
     * @return the class id now on the character
     */
    public static String commitFightingClassForRace(
            StatsData data, String newRaceId, String preferredClassId, String fallbackPriorClassId) {
        if (data == null || newRaceId == null || newRaceId.isBlank()) {
            return preferredClassId == null ? "" : preferredClassId;
        }
        Character ch = data.getCharacter();
        if (ch == null) {
            return preferredClassId == null ? "" : preferredClassId;
        }
        String race = newRaceId.trim().toLowerCase(Locale.ROOT);
        String mapped = fightingClassForRace(preferredClassId, fallbackPriorClassId, race);
        try {
            ch.setRace(race);
            ch.setCharacterClass(mapped);
        } catch (Throwable ignored) {
        }
        return mapped;
    }

    /**
     * Sets race + fighting class on DMZ character data so class passives/base stats use the
     * <em>new</em> race's {@code stats.json} entry for that class id.
     *
     * @return the class id now on the character
     */
    public static String applyRaceAndFightingClass(
            ServerPlayer player, StatsData data, String newRaceId, String priorClassId) {
        if (data == null || newRaceId == null || newRaceId.isBlank()) {
            return priorClassId == null ? "" : priorClassId;
        }
        Character ch = data.getCharacter();
        if (ch == null) {
            return priorClassId == null ? "" : priorClassId;
        }
        String prior = priorClassId;
        if (prior == null || prior.isBlank()) {
            try {
                prior = ch.getCharacterClass();
            } catch (Throwable ignored) {
            }
        }
        if (prior == null || prior.isBlank()) {
            prior = DmzProgression.fightingClass(player);
        }
        String currentOnCharacter;
        try {
            currentOnCharacter = ch.getCharacterClass();
        } catch (Throwable t) {
            currentOnCharacter = prior;
        }
        return commitFightingClassForRace(data, newRaceId, currentOnCharacter, prior);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
