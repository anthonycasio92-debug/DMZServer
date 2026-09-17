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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Editable GUI button names/lore from {@code plugins/LegacyMechanicsGUI/gui-tooltips.json}.
 * Keys are dotted paths like {@code hub.main.difficulty}. Missing keys fall back to
 * the hard-coded defaults in Java. When a key defines {@code lore} in JSON, that
 * replaces Java lore entirely — do not add static catalog lore on dynamic buttons
 * (tier costs, unlock gates, {@code {action}} toggles); see
 * {@code tools/dmz-adaptive-difficulty/sim/gui_tooltip_policy.java}.
 * Reload with {@code /lm admin reload}.
 */
final class GuiTooltips {
    /** Bump when the in-jar tooltip catalog is humanized; older on-disk files are replaced on reload. */
    private static final int CATALOG_REVISION = 193;

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

    /**
     * Balance / copy fixes that must win over stale on-disk lore (disk overrides jar).
     * Longer phrases first. Applied to {@code plugins/.../gui-tooltips.json} on reload.
     */
    private static final String[][] STALE_COPY_FIXES = {
            {"&87-day cooldown after leaving", "&812-hour cooldown after leaving"},
            {"&87-day cooldown after releasing", "&812-hour cooldown after releasing"},
            {"&87-day cooldown", "&812-hour cooldown"},
            {"7-day cooldown", "12-hour cooldown"},
            {"(7d cooldown)", "(12h cooldown)"},
            {"7d cooldown", "12h cooldown"},
    };

    /**
     * Mentor tip keys whose jar lore must win over stale on-disk copy
     * (disk otherwise keeps old {@code End bond with \{name\}} / missing vars).
     */
    private static final String[] FORCE_JAR_LORE_KEYS = {
            "spar.mentor.leave",
            "spar.mentor.leave_none",
            "spar.mentor.release",
            "spar.mentor.release_none",
            "spar.mentor.dojo",
            "skills.main.header",
            "progression.economy.staff_free",
            "difficulty.tiers.tier",
    };

    static String reload() {
        Map<String, Entry> jarEntries = new LinkedHashMap<>();
        String jarJson = null;
        try (InputStream in = AdaptiveDifficultyGuiPlugin.class.getClassLoader()
                .getResourceAsStream("gui-tooltips.json")) {
            if (in != null) {
                jarJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                parseInto(jarJson, jarEntries);
            }
        } catch (Throwable ignored) {
        }

        Map<String, Entry> next = new LinkedHashMap<>(jarEntries);
        Path path = filePath;
        int fromDisk = 0;
        int merged = 0;
        int staleFixed = 0;
        int forced = 0;
        int catalogUpgraded = 0;
        if (path != null && Files.isRegularFile(path)) {
            try {
                String diskJson = Files.readString(path, StandardCharsets.UTF_8);
                int jarRev = jarJson != null ? catalogRevision(jarJson) : CATALOG_REVISION;
                int diskRev = catalogRevision(diskJson);
                if (jarJson != null && jarRev > 0 && diskRev < jarRev) {
                    Files.writeString(path, jarJson, StandardCharsets.UTF_8);
                    diskJson = jarJson;
                    catalogUpgraded = jarRev;
                    if (log != null) {
                        log.info("GUI tooltips: upgraded on-disk catalog rev " + diskRev + " → " + jarRev);
                    }
                }
                String sanitized = sanitizeStaleCopy(diskJson);
                if (!sanitized.equals(diskJson)) {
                    Files.writeString(path, sanitized, StandardCharsets.UTF_8);
                    staleFixed = countStaleFixes(diskJson);
                    diskJson = sanitized;
                    if (log != null) {
                        log.info("GUI tooltips: rewrote " + staleFixed
                                + " stale mentor-cooldown phrase(s) to 12h on disk");
                    }
                }
                Map<String, Entry> disk = new LinkedHashMap<>();
                fromDisk = parseInto(diskJson, disk);
                // Preserve edits, but fill any new jar keys into the on-disk file for editing.
                if (jarJson != null) {
                    merged = mergeMissingKeysToDisk(path, jarJson, diskJson, jarEntries.keySet(), disk.keySet());
                    // Re-read after merge so FORCE overwrite sees latest disk tree.
                    if (merged > 0 && Files.isRegularFile(path)) {
                        diskJson = Files.readString(path, StandardCharsets.UTF_8);
                        disk.clear();
                        fromDisk = parseInto(diskJson, disk);
                    }
                    forced = forceJarLoreKeysToDisk(path, jarJson, jarEntries, disk);
                }
                next.putAll(disk);
                // Jar lore for force-keys wins even after disk putAll.
                for (String key : FORCE_JAR_LORE_KEYS) {
                    Entry jar = jarEntries.get(normalize(key));
                    if (jar != null) {
                        next.put(normalize(key), jar);
                    }
                }
            } catch (Throwable t) {
                String err = "gui-tooltips reload failed: " + t.getMessage();
                if (log != null) {
                    log.warning(err);
                }
                return "§c" + err;
            }
        } else if (path != null && jarJson != null) {
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, jarJson, StandardCharsets.UTF_8);
            } catch (Throwable ignored) {
            }
        }
        ENTRIES.clear();
        ENTRIES.putAll(next);
        String msg = "§aGUI tooltips loaded §f" + ENTRIES.size() + " §akeys"
                + (fromDisk > 0 ? " §8(" + fromDisk + " from gui-tooltips.json)" : "")
                + (merged > 0 ? " §a(+ " + merged + " new keys merged into file)" : "")
                + (staleFixed > 0 ? " §a(+ " + staleFixed + " stale 7d→12h fixes)" : "")
                + (forced > 0 ? " §a(+ " + forced + " mentor tips refreshed from jar)" : "")
                + (catalogUpgraded > 0 ? " §a(catalog rev " + catalogUpgraded + " from jar)" : "");
        if (log != null) {
            log.info("GUI tooltips: " + ENTRIES.size() + " keys"
                    + (merged > 0 ? " (merged " + merged + " new)" : "")
                    + (staleFixed > 0 ? " (fixed " + staleFixed + " stale)" : "")
                    + (forced > 0 ? " (forced " + forced + " mentor tips)" : "")
                    + (catalogUpgraded > 0 ? " (catalog → rev " + catalogUpgraded + ")" : ""));
        }
        return msg;
    }

    private static int catalogRevision(String json) {
        if (json == null || json.isBlank()) {
            return 0;
        }
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("_catalogRevision") && root.get("_catalogRevision").isJsonPrimitive()) {
                return root.get("_catalogRevision").getAsInt();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    /**
     * Overwrite known mentor tip keys on disk with jar content so Leave/Release
     * stop showing {@code ?} / literal {@code {name}} from old lore.
     */
    private static int forceJarLoreKeysToDisk(
            Path path, String jarJson, Map<String, Entry> jarEntries, Map<String, Entry> disk
    ) {
        if (path == null || jarJson == null || jarEntries == null || disk == null) {
            return 0;
        }
        int changed = 0;
        try {
            JsonObject jarRoot = JsonParser.parseString(jarJson).getAsJsonObject();
            String diskJson = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject diskRoot = JsonParser.parseString(diskJson).getAsJsonObject();
            boolean dirty = false;
            for (String key : FORCE_JAR_LORE_KEYS) {
                Entry jar = jarEntries.get(normalize(key));
                if (jar == null) {
                    continue;
                }
                Entry cur = disk.get(normalize(key));
                boolean same = cur != null
                        && Objects.equals(cur.name, jar.name)
                        && Objects.equals(cur.lore, jar.lore);
                if (same) {
                    continue;
                }
                if (overwritePath(jarRoot, diskRoot, key.split("\\."))) {
                    disk.put(normalize(key), jar);
                    changed++;
                    dirty = true;
                }
            }
            if (dirty) {
                Files.writeString(path, GSON.toJson(diskRoot) + "\n", StandardCharsets.UTF_8);
            }
        } catch (Throwable t) {
            if (log != null) {
                log.warning("Could not force mentor tip keys from jar: " + t.getMessage());
            }
            return 0;
        }
        return changed;
    }

    /** Like {@link #copyPath} but replaces an existing leaf. */
    private static boolean overwritePath(JsonObject from, JsonObject to, String[] parts) {
        if (parts == null || parts.length == 0) {
            return false;
        }
        JsonObject src = from;
        JsonObject dst = to;
        for (int i = 0; i < parts.length - 1; i++) {
            String p = parts[i];
            if (!src.has(p) || !src.get(p).isJsonObject()) {
                return false;
            }
            JsonObject srcChild = src.getAsJsonObject(p);
            if (!dst.has(p) || !dst.get(p).isJsonObject()) {
                dst.add(p, new JsonObject());
            }
            src = srcChild;
            dst = dst.getAsJsonObject(p);
        }
        String leaf = parts[parts.length - 1];
        if (!src.has(leaf)) {
            return false;
        }
        dst.add(leaf, src.get(leaf).deepCopy());
        return true;
    }

    private static String sanitizeStaleCopy(String json) {
        if (json == null || json.isBlank()) {
            return json == null ? "" : json;
        }
        String out = json;
        for (String[] pair : STALE_COPY_FIXES) {
            out = out.replace(pair[0], pair[1]);
        }
        return out;
    }

    private static int countStaleFixes(String before) {
        if (before == null || before.isBlank()) {
            return 0;
        }
        int n = 0;
        String cursor = before;
        for (String[] pair : STALE_COPY_FIXES) {
            int from = 0;
            while (true) {
                int at = cursor.indexOf(pair[0], from);
                if (at < 0) {
                    break;
                }
                n++;
                from = at + pair[0].length();
            }
            cursor = cursor.replace(pair[0], pair[1]);
        }
        return n;
    }

    /**
     * Deep-merge jar keys missing from the disk JSON so admins always see the full editable catalog
     * after a mod update, without wiping their edits.
     */
    private static int mergeMissingKeysToDisk(
            Path path, String jarJson, String diskJson,
            java.util.Set<String> jarKeys, java.util.Set<String> diskKeys
    ) {
        java.util.Set<String> missing = new java.util.LinkedHashSet<>();
        for (String k : jarKeys) {
            if (!diskKeys.contains(k)) {
                missing.add(k);
            }
        }
        if (missing.isEmpty()) {
            return 0;
        }
        try {
            JsonObject jarRoot = JsonParser.parseString(jarJson).getAsJsonObject();
            JsonObject diskRoot = JsonParser.parseString(diskJson).getAsJsonObject();
            int added = 0;
            for (String key : missing) {
                if (copyPath(jarRoot, diskRoot, key.split("\\."))) {
                    added++;
                }
            }
            if (added > 0) {
                Files.writeString(path, GSON.toJson(diskRoot) + "\n", StandardCharsets.UTF_8);
            }
            return added;
        } catch (Throwable t) {
            if (log != null) {
                log.warning("Could not merge new tooltip keys to disk: " + t.getMessage());
            }
            return 0;
        }
    }

    private static boolean copyPath(JsonObject from, JsonObject to, String[] parts) {
        if (parts == null || parts.length == 0) {
            return false;
        }
        JsonObject src = from;
        JsonObject dst = to;
        for (int i = 0; i < parts.length - 1; i++) {
            String p = parts[i];
            if (!src.has(p) || !src.get(p).isJsonObject()) {
                return false;
            }
            JsonObject srcChild = src.getAsJsonObject(p);
            if (!dst.has(p) || !dst.get(p).isJsonObject()) {
                dst.add(p, new JsonObject());
            }
            src = srcChild;
            dst = dst.getAsJsonObject(p);
        }
        String leaf = parts[parts.length - 1];
        if (!src.has(leaf) || dst.has(leaf)) {
            return false;
        }
        dst.add(leaf, src.get(leaf).deepCopy());
        return true;
    }

    static int size() {
        return ENTRIES.size();
    }

    /** Display name override, or {@code fallback} when unset. */
    static String name(String key, String fallback) {
        return name(key, fallback, null);
    }

    /** Display name with {@code {var}} substitution from {@code vars}. */
    static String name(String key, String fallback, Map<String, String> vars) {
        Entry e = ENTRIES.get(normalize(key));
        String raw;
        if (e != null && e.name != null && !e.name.isBlank()) {
            raw = e.name;
        } else {
            raw = fallback == null ? "" : fallback;
        }
        return applyVars(raw, vars);
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
                // A leaf button has "name"/"title" as a plain string and/or "lore" as an array.
                // Checking mere key presence would misfire when a *container* object happens to
                // have a nested leaf literally named "name", "lore", or "title" (e.g. a
                // "progress" group with a "title" sub-button), swallowing its other children.
                boolean leaf = (child.has("lore") && child.get("lore").isJsonArray())
                        || (child.has("name") && child.get("name").isJsonPrimitive())
                        || (child.has("title") && child.get("title").isJsonPrimitive());
                if (leaf) {
                    String name = null;
                    if (child.has("name") && child.get("name").isJsonPrimitive()) {
                        name = child.get("name").getAsString();
                    } else if (child.has("title") && child.get("title").isJsonPrimitive()) {
                        name = child.get("title").getAsString();
                    }
                    // null lore = keep Java defaults; explicit [] clears lore.
                    List<String> lore = null;
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
            // null = fall back to Java defaults; non-null (even empty) = intentional override
            this.lore = lore == null ? null : List.copyOf(lore);
        }

        static Entry loreOnly(List<String> lore) {
            return new Entry(null, lore);
        }
    }
}
