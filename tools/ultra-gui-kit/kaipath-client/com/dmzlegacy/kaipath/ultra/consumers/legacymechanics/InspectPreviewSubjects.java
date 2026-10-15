package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Who {@code layoutJson.playerModel} names. No Minecraft types — safe to unit-test.
 *
 * <p>{@code local} (or blank) is the viewing player. Staff inspect sends {@code uuid:<id>} on
 * {@code playerModel.subject}, falling back to {@code playerModel.parts.subject}.
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
