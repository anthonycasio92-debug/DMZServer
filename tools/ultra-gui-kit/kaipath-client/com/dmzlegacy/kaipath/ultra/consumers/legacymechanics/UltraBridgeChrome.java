package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;

/**
 * Shared helpers for mod-agnostic Ultra chrome fields ({@code palette}, {@code frame},
 * {@code sound}, {@code buttonSkin}, letter {@code accent}).
 */
final class UltraBridgeChrome {
    private UltraBridgeChrome() {}

    /** Generic palette name → opaque ARGB. Unknown names fall back to gold. */
    static int paletteArgb(String name) {
        String n = normalizePalette(name);
        if (n.isBlank()) {
            return 0xFFFFAA00;
        }
        return switch (n) {
            case "red" -> 0xFFFF5555;
            case "dark_red" -> 0xFFAA0000;
            case "purple" -> 0xFFAA00AA;
            case "dark_purple" -> 0xFF5500AA;
            case "gold" -> 0xFFFFAA00;
            case "yellow" -> 0xFFFFFF55;
            case "green" -> 0xFF55FF55;
            case "aqua" -> 0xFF55FFFF;
            case "blue" -> 0xFF5555FF;
            case "white" -> 0xFFFFFFFF;
            case "gray", "grey" -> 0xFFAAAAAA;
            default -> 0xFFFFAA00;
        };
    }

    static String normalizePalette(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    /** Letter accent → ARGB (Minecraft formatting colors). */
    static int accentArgb(String code) {
        String letter = normalizeAccent(code);
        if (letter.isBlank()) {
            return 0xFFFFAA00;
        }
        return switch (letter.charAt(0)) {
            case '0' -> 0xFF000000;
            case '1' -> 0xFF0000AA;
            case '2' -> 0xFF00AA00;
            case '3' -> 0xFF00AAAA;
            case '4' -> 0xFFAA0000;
            case '5' -> 0xFFAA00AA;
            case '6' -> 0xFFFFAA00;
            case '7' -> 0xFFAAAAAA;
            case '8' -> 0xFF555555;
            case '9' -> 0xFF5555FF;
            case 'a' -> 0xFF55FF55;
            case 'b' -> 0xFF55FFFF;
            case 'c' -> 0xFFFF5555;
            case 'd' -> 0xFFFF55FF;
            case 'e' -> 0xFFFFFF55;
            case 'f' -> 0xFFFFFFFF;
            default -> 0xFFFFAA00;
        };
    }

    static String normalizeAccent(String accent) {
        if (accent == null || accent.isBlank()) {
            return "";
        }
        String s = accent.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("§") || s.startsWith("\u00a7")) {
            s = s.substring(1);
        }
        if (s.isEmpty()) {
            return "";
        }
        char c = s.charAt(0);
        if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')) {
            return String.valueOf(c);
        }
        return "";
    }

    /** Prefer palette name over letter accent for title / chrome tint. */
    static int resolveTintArgb(String palette, String accent) {
        String p = normalizePalette(palette);
        if (!p.isBlank()) {
            return paletteArgb(p);
        }
        String a = normalizeAccent(accent);
        if (!a.isBlank()) {
            return accentArgb(a);
        }
        return 0xFFFFAA00;
    }

    /** {@code frame} may be a string id or an object with {@code id}. */
    static String frameId(JsonObject layout) {
        if (layout == null || !layout.has("frame") || layout.get("frame").isJsonNull()) {
            return "";
        }
        JsonElement el = layout.get("frame");
        if (el.isJsonPrimitive()) {
            return el.getAsString() == null ? "" : el.getAsString().trim();
        }
        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            if (o.has("id") && o.get("id").isJsonPrimitive()) {
                String id = o.get("id").getAsString();
                return id == null ? "" : id.trim();
            }
        }
        return "";
    }

    /** {@code buttonSkin} may be a string id or an object with {@code id}. */
    static String buttonSkinId(JsonObject button) {
        if (button == null || !button.has("buttonSkin") || button.get("buttonSkin").isJsonNull()) {
            return "";
        }
        JsonElement el = button.get("buttonSkin");
        if (el.isJsonPrimitive()) {
            return el.getAsString() == null ? "" : el.getAsString().trim();
        }
        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            if (o.has("id") && o.get("id").isJsonPrimitive()) {
                String id = o.get("id").getAsString();
                return id == null ? "" : id.trim();
            }
        }
        return "";
    }

    static String str(JsonObject o, String key, String fallback) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return o.get(key).getAsString();
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
