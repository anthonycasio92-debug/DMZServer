package com.dbzlegacy.adaptivedifficulty.bukkit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Editable GUI button names/lore from {@code plugins/LegacyMechanicsGUI/gui-tooltips.json}.
 * Keys are dotted paths like {@code hub.main.difficulty}. Missing keys fall back to
 * the hard-coded defaults in Java. Reload with {@code /lm admin reload}.
 */
final class GuiTooltips {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static volatile Path filePath;
    private static volatile Logger log;

    private GuiTooltips() {}

    static void init(JavaPlugin plugin) {
        if (plugin == null) {
            return;
        }
        log = plugin.getLogger();
        Path folder = plugin.getDataFolder().toPath();
        filePath = folder.resolve("gui-tooltips.json");
        try {
            Files.createDirectories(folder);
            if (!Files.isRegularFile(filePath)) {
                try (InputStream in = plugin.getResource("gui-tooltips.json")) {
                    if (in != null) {
                        Files.copy(in, filePath);
                    } else {
                        Files.writeString(filePath, defaultSkeleton(), StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Throwable t) {
            if (log != null) {
                log.warning("Could not create gui-tooltips.json: " + t.getMessage());
            }
        }
        reload();
    }

    static String reload() {
        Map<String, Entry> next = new LinkedHashMap<>();
        // Start with jar defaults, then overlay disk so edits win.
        try (InputStream in = AdaptiveDifficultyGuiPlugin.class.getClassLoader()
                .getResourceAsStream("gui-tooltips.json")) {
            if (in != null) {
                parseInto(new String(in.readAllBytes(), StandardCharsets.UTF_8), next);
            }
        } catch (Throwable ignored) {
        }
        Path path = filePath;
        int fromDisk = 0;
        if (path != null && Files.isRegularFile(path)) {
            try {
                Map<String, Entry> disk = new LinkedHashMap<>();
                fromDisk = parseInto(Files.readString(path, StandardCharsets.UTF_8), disk);
                next.putAll(disk);
            } catch (Throwable t) {
                String err = "gui-tooltips reload failed: " + t.getMessage();
                if (log != null) {
                    log.warning(err);
                }
                return "§c" + err;
            }
        }
        ENTRIES.clear();
        ENTRIES.putAll(next);
        String msg = "§aGUI tooltips loaded §f" + ENTRIES.size() + " §akeys"
                + (fromDisk > 0 ? " §8(" + fromDisk + " from gui-tooltips.json)" : "");
        if (log != null) {
            log.info("GUI tooltips: " + ENTRIES.size() + " keys");
        }
        return msg;
    }

    static int size() {
        return ENTRIES.size();
    }

    /** Display name override, or {@code fallback} when unset. */
    static String name(String key, String fallback) {
        Entry e = ENTRIES.get(normalize(key));
        if (e != null && e.name != null && !e.name.isBlank()) {
            return e.name;
        }
        return fallback;
    }

    /** Lore lines for {@code key}, or a copy of {@code defaults} when unset. */
    static List<String> lore(String key, List<String> defaults) {
        return lore(key, defaults, null);
    }

    static List<String> lore(String key, List<String> defaults, Map<String, String> vars) {
        Entry e = ENTRIES.get(normalize(key));
        List<String> src;
        if (e != null && e.lore != null) {
            src = e.lore;
        } else if (defaults == null) {
            return List.of();
        } else {
            src = defaults;
        }
        List<String> out = new ArrayList<>(src.size());
        for (String line : src) {
            out.add(applyVars(line == null ? "" : line, vars));
        }
        return out;
    }

    /** Build button lore: blank line + configured/default body (+ optional staff tip lines). */
    static List<String> buttonLore(
            String key, List<String> defaults, Map<String, String> vars, List<String> staffExtra
    ) {
        List<String> out = new ArrayList<>();
        out.add("");
        out.addAll(lore(key, defaults, vars));
        if (staffExtra != null && !staffExtra.isEmpty()) {
            out.addAll(staffExtra);
        }
        return out;
    }

    static List<String> buttonLore(String key, List<String> defaults) {
        return buttonLore(key, defaults, null, null);
    }

    private static String applyVars(String line, Map<String, String> vars) {
        if (line == null) {
            return "";
        }
        if (vars == null || vars.isEmpty() || !line.contains("{")) {
            return line;
        }
        String out = line;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    private static String normalize(String key) {
        if (key == null) {
            return "";
        }
        return key.trim().toLowerCase(Locale.ROOT);
    }

    private static int parseInto(String json, Map<String, Entry> into) {
        if (json == null || json.isBlank()) {
            return 0;
        }
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            return 0;
        }
        return walk("", root.getAsJsonObject(), into);
    }

    private static int walk(String prefix, JsonObject obj, Map<String, Entry> into) {
        int count = 0;
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            String key = e.getKey();
            if (key == null || key.startsWith("_")) {
                continue; // _comment etc.
            }
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            JsonElement val = e.getValue();
            if (val == null || val.isJsonNull()) {
                continue;
            }
            if (val.isJsonArray()) {
                into.put(normalize(path), Entry.loreOnly(readArray(val.getAsJsonArray())));
                count++;
            } else if (val.isJsonObject()) {
                JsonObject child = val.getAsJsonObject();
                boolean leaf = child.has("lore") || child.has("name") || child.has("title");
                if (leaf) {
                    String name = null;
                    if (child.has("name") && child.get("name").isJsonPrimitive()) {
                        name = child.get("name").getAsString();
                    } else if (child.has("title") && child.get("title").isJsonPrimitive()) {
                        name = child.get("title").getAsString();
                    }
                    List<String> lore = List.of();
                    if (child.has("lore") && child.get("lore").isJsonArray()) {
                        lore = readArray(child.getAsJsonArray("lore"));
                    }
                    into.put(normalize(path), new Entry(name, lore));
                    count++;
                } else {
                    count += walk(path, child, into);
                }
            } else if (val.isJsonPrimitive()) {
                into.put(normalize(path), Entry.loreOnly(List.of(val.getAsString())));
                count++;
            }
        }
        return count;
    }

    private static List<String> readArray(JsonArray arr) {
        List<String> out = new ArrayList<>();
        if (arr == null) {
            return out;
        }
        for (JsonElement el : arr) {
            if (el == null || el.isJsonNull()) {
                out.add("");
            } else if (el.isJsonPrimitive()) {
                out.add(el.getAsString());
            } else {
                out.add(el.toString());
            }
        }
        return out;
    }

    private static String defaultSkeleton() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("_comment", "Edit lore/name for any LM GUI button. Reload: /lm admin reload");
        return GSON.toJson(root);
    }

    private static final class Entry {
        final String name;
        final List<String> lore;

        Entry(String name, List<String> lore) {
            this.name = name;
            this.lore = lore == null ? List.of() : List.copyOf(lore);
        }

        static Entry loreOnly(List<String> lore) {
            return new Entry(null, lore);
        }
    }
}
