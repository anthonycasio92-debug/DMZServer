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
            for (String id : allowed) {
                if (prior.equalsIgnoreCase(id)) {
                    return id;
                }
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
        String race = newRaceId.trim().toLowerCase(Locale.ROOT);
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
        String mapped = resolveClassForRace(prior, race);
        try {
            ch.setRace(race);
            ch.setCharacterClass(mapped);
        } catch (Throwable ignored) {
        }
        return mapped;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
