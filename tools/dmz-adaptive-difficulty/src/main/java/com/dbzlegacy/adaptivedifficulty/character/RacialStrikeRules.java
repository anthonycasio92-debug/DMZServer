package com.dbzlegacy.adaptivedifficulty.character;

import java.util.List;
import java.util.Locale;

/**
 * Which strike attacks a race change may delete.
 *
 * <p>A known strike is one DMZ lists as a strike skill or stores as a strike technique.
 * Quest ki, trainer ki, and player-made techniques are not known strikes, so they stay.
 * A strike the destination race lists, or an open-race strike with a non-negative TP
 * cost (the player bought it), also stays. A strike with an open race list and only
 * negative costs is a racial template and does not carry to another race.
 */
public final class RacialStrikeRules {
    private RacialStrikeRules() {}

    public static String normalizeRace(String race) {
        if (race == null) {
            return "";
        }
        String r = race.trim().toLowerCase(Locale.ROOT);
        int colon = r.lastIndexOf(':');
        if (colon >= 0 && colon < r.length() - 1) {
            r = r.substring(colon + 1).trim();
        }
        return r;
    }

    /** Empty or blank-only lists mean DMZ allows every race. */
    public static boolean allowedOpen(List<String> allowedRaces) {
        if (allowedRaces == null || allowedRaces.isEmpty()) {
            return true;
        }
        for (int i = 0; i < allowedRaces.size(); i++) {
            String raw = allowedRaces.get(i);
            if (raw != null && !raw.isBlank()) {
                return false;
            }
        }
        return true;
    }

    public static boolean raceListed(List<String> allowedRaces, String race) {
        if (allowedRaces == null || allowedRaces.isEmpty()) {
            return false;
        }
        String want = normalizeRace(race);
        if (want.isEmpty()) {
            return false;
        }
        for (int i = 0; i < allowedRaces.size(); i++) {
            String raw = allowedRaces.get(i);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String token = normalizeRace(raw);
            if ("all".equals(token) || want.equals(token)) {
                return true;
            }
        }
        return false;
    }

    /** A cost of zero or more is a skill the player can buy. Negative-only ladders are not. */
    public static boolean purchasable(List<Integer> costs) {
        if (costs == null || costs.isEmpty()) {
            return false;
        }
        for (int i = 0; i < costs.size(); i++) {
            Integer cost = costs.get(i);
            if (cost != null && cost >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true when a race change should delete this strike and its skill
     */
    public static boolean dropOnRaceChange(
            boolean knownStrike, List<String> allowedRaces, List<Integer> costs, String newRace
    ) {
        if (!knownStrike) {
            return false;
        }
        if (raceListed(allowedRaces, newRace)) {
            return false;
        }
        if (allowedOpen(allowedRaces) && purchasable(costs)) {
            return false;
        }
        return true;
    }

    /**
     * Read-time absence. An open race list still grants the strike, so a copy the
     * player still has stays visible. A listed race set that excludes the current
     * race is treated as absent.
     */
    public static boolean absentOnRead(boolean knownStrike, List<String> allowedRaces, String currentRace) {
        if (!knownStrike) {
            return false;
        }
        if (normalizeRace(currentRace).isEmpty() || allowedOpen(allowedRaces)) {
            return false;
        }
        return !raceListed(allowedRaces, currentRace);
    }
}
