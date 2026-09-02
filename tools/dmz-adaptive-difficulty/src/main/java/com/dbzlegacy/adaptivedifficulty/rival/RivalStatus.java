package com.dbzlegacy.adaptivedifficulty.rival;

/** Rivalry relationship status (script 4.7.10). */
public enum RivalStatus {
    NONE,
    UNKNOWN,
    DECLARED,
    PENDING,
    MUTUAL,
    NEMESIS;

    public String id() {
        return name().toLowerCase();
    }

    public String label() {
        return switch (this) {
            case NONE -> "None";
            case UNKNOWN -> "Silent";
            case DECLARED -> "Declared";
            case PENDING -> "Pending";
            case MUTUAL -> "Mutual";
            case NEMESIS -> "Nemesis";
        };
    }

    public static RivalStatus fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        try {
            return RivalStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
