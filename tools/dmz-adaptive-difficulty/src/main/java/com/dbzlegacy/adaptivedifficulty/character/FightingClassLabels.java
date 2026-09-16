package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.progression.classdef.FightingClassCatalog;
import java.util.Locale;

/** Human-readable DMZ fighting class names for GUIs (not raw translation keys). */
public final class FightingClassLabels {
    private FightingClassLabels() {}

    public static String display(String raw) {
        String id = normalizeClassId(raw);
        if (id.isBlank()) {
            return "Not set";
        }
        String name = FightingClassCatalog.skillNameFor(id);
        if (name != null && !name.isBlank()) {
            return stripLegacyFormatting(name);
        }
        return titleCase(id);
    }

    static String normalizeClassId(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return "";
        }
        if (s.startsWith("class.")) {
            s = s.substring("class.".length());
        }
        if (s.startsWith("dragonminez.")) {
            s = s.substring("dragonminez.".length());
        }
        return s.toLowerCase(Locale.ROOT).trim();
    }

    private static String stripLegacyFormatting(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replaceAll("\u00A7.", "").trim();
    }

    private static String titleCase(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        String[] parts = id.replace('-', '_').split("[_\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
