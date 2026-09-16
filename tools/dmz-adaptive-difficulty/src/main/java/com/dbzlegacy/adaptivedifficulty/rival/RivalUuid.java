package com.dbzlegacy.adaptivedifficulty.rival;

import java.util.Locale;
import java.util.UUID;

/** Canonical rivalry UUID strings for map keys and online lookups. */
public final class RivalUuid {
    private RivalUuid() {}

    public static String canonical(String uuid) {
        if (uuid == null) {
            return null;
        }
        String trimmed = uuid.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        try {
            return UUID.fromString(trimmed).toString();
        } catch (IllegalArgumentException ignored) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
    }

    public static boolean samePlayer(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.equalsIgnoreCase(b)) {
            return true;
        }
        String ca = canonical(a);
        String cb = canonical(b);
        return ca != null && cb != null && ca.equalsIgnoreCase(cb);
    }
}
