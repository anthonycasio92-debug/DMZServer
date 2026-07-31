package com.dbzlegacy.adaptivedifficulty.data;

public enum TeamMode {
    PERSONAL_ONLY,
    THRESHOLD_BONUS_ONLY,
    FULL_TEAM_SCALING;

    public String displayName() {
        return switch (this) {
            case PERSONAL_ONLY -> "Personal Only";
            case THRESHOLD_BONUS_ONLY -> "Threshold Bonus";
            case FULL_TEAM_SCALING -> "Full Team";
        };
    }

    public static TeamMode fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return PERSONAL_ONLY;
        }
        String key = raw.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (key) {
            case "THRESHOLD", "THRESHOLD_BONUS", "THRESHOLD_BONUS_ONLY", "BONUS" -> THRESHOLD_BONUS_ONLY;
            case "FULL", "FULL_TEAM", "FULL_TEAM_SCALING", "TEAM" -> FULL_TEAM_SCALING;
            default -> PERSONAL_ONLY;
        };
    }
}
