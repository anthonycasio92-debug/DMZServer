package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.UUID;

/**
 * Who {@code layoutJson.playerModel} names. No Minecraft types — safe to unit-test.
 *
 * <p>Any mod may set {@code playerModel.subject}. {@code local} (or blank) is the player who
 * has the screen open. Another player is a UUID, or the same id prefixed with {@code uuid:}.
 * {@code playerModel.parts.subject} is that same token when the sender nests it. The top-level
 * field wins.
 */
public final class InspectPreviewSubjects {
    private InspectPreviewSubjects() {}

    public static String tokenFromLayout(String layoutJson) {
        if (layoutJson == null || layoutJson.isBlank()) {
            return "local";
        }
        try {
            JsonElement root = JsonParser.parseString(layoutJson);
            if (root == null || !root.isJsonObject()) {
                return "local";
            }
            JsonObject layout = root.getAsJsonObject();
            if (!layout.has("playerModel") || !layout.get("playerModel").isJsonObject()) {
                return "local";
            }
            return token(layout.getAsJsonObject("playerModel"), null);
        } catch (RuntimeException e) {
            return "local";
        }
    }

    /**
     * Top-level {@code subject} wins. {@code parts} may be the nested object or the one the caller
     * already pulled out of {@code playerModel}.
     */
    public static String token(JsonObject model, JsonObject parts) {
        String top = text(model, "subject");
        if (!top.isEmpty()) {
            return top;
        }
        String nested = text(parts, "subject");
        if (!nested.isEmpty()) {
            return nested;
        }
        if (model != null && model.has("parts") && model.get("parts").isJsonObject()) {
            nested = text(model.getAsJsonObject("parts"), "subject");
            if (!nested.isEmpty()) {
                return nested;
            }
        }
        return "local";
    }

    /**
     * Player id named by a subject token. {@code null} means the viewing player ({@code local},
     * blank, or a token that is not a UUID).
     */
    public static UUID playerId(String token) {
        if (token == null || token.isBlank() || "local".equalsIgnoreCase(token)) {
            return null;
        }
        String id = token.trim();
        if (id.regionMatches(true, 0, "uuid:", 0, 5)) {
            id = id.substring(5).trim();
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String text(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        JsonElement el = object.get(key);
        if (!el.isJsonPrimitive()) {
            return "";
        }
        String value = el.getAsString();
        return value == null ? "" : value.trim();
    }
}
