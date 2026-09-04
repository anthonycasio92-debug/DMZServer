package com.dbzlegacy.adaptivedifficulty.data;

public enum TeamMode {
    PERSONAL_ONLY,
    THRESHOLD_BONUS_ONLY,
    FULL_TEAM_SCALING;

    public static TeamMode fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return PERSONAL_ONLY;
        }
        String key = raw.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (key) {
            case "THRESHOLD", "THRESHOLD_BONUS", "THRESHOLD_BONUS_ONLY", "BONUS" -> THRESHOLD_BONUS_ONLY;
            case "FULL", "FULL_TEAM", "FULL_TEAM_SCALING", "TEAM" -> FULL_TEAM_SCALING;
            case "PERSONAL", "PERSONAL_ONLY", "OFF", "NONE" -> PERSONAL_ONLY;
            default -> PERSONAL_ONLY;
        };
    }
}
