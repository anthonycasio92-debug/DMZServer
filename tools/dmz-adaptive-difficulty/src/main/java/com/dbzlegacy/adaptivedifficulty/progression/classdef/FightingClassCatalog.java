package com.dbzlegacy.adaptivedifficulty.progression.classdef;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.ConfigPaths;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.FabledBridge;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Discovers every DMZ fighting class from {@code config/dragonminez/races/<race>/stats.json}
 * plus optional overrides in {@code config/legacymechanics/class-fabled.json}.
 *
 * <p>Used by {@link com.dbzlegacy.adaptivedifficulty.progression.bridge.ClassSkillSync}
 * and {@link com.dbzlegacy.adaptivedifficulty.progression.bridge.ClassPermissionSync}.
 */
public final class FightingClassCatalog {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String PRESTIGE_SUFFIX = " Prestige";

    private static volatile Catalog BLOB = Catalog.empty();

    private FightingClassCatalog() {}

    public static Path path() {
        return ConfigPaths.dataDir().resolve("class-fabled.json");
    }

    public static Set<String> allClassIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(BLOB.classIds));
    }

    public static List<ClassEntry> entries() {
        if (BLOB.entries.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(BLOB.entries));
    }

    /** Resolve the Fabled base-skill display name for a DMZ class id. */
    public static String skillNameFor(String classId) {
        if (classId == null || classId.isBlank()) {
            return "";
        }
        String id = classId.trim().toLowerCase(Locale.ROOT);
        ClassEntry entry = BLOB.byId.get(id);
        if (entry != null && entry.fabledSkill != null && !entry.fabledSkill.isBlank()) {
            return entry.fabledSkill.trim();
        }
        String fromFabled = matchFabledSkillName(id);
        if (!fromFabled.isBlank()) {
            return fromFabled;
        }
        String legacy = legacySkillName(id);
        if (!legacy.isBlank()) {
            return legacy;
        }
        return titleCaseId(id);
    }

    public static String prestigeSkillNameFor(String classId) {
        String base = skillNameFor(classId);
        if (base.isBlank()) {
            return "";
        }
        ClassEntry entry = BLOB.byId.get(normalizeId(classId));
        if (entry != null && entry.prestigeSkill != null && !entry.prestigeSkill.isBlank()) {
            return entry.prestigeSkill.trim();
        }
        return base + PRESTIGE_SUFFIX;
    }

    public static String permissionForSkillName(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            return "";
        }
        return "fabled.skill." + skillName.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    public static boolean load() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            FileBlob config = FileBlob.empty();
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    FileBlob loaded = GSON.fromJson(reader, FileBlob.class);
                    if (loaded != null) {
                        config = loaded;
                    }
                }
            }
            BLOB = buildCatalog(config);
            saveMerged(config);
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Fighting class catalog: {} class(es) from DMZ + Fabled",
                    AdaptiveDifficultyMod.MOD_ID,
                    BLOB.classIds.size());
            return true;
        } catch (Exception e) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] failed to load class catalog {}; using DMZ scan only: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    file,
                    e.toString());
            BLOB = buildCatalog(FileBlob.empty());
            return false;
        }
    }

    public static boolean reload() {
        return load();
    }

    private static void saveMerged(FileBlob config) {
        Path file = path();
        try {
            FileBlob out = config == null ? FileBlob.empty() : config;
            if (out.classes == null) {
                out.classes = new ArrayList<>();
            }
            Map<String, ClassEntry> known = new LinkedHashMap<>();
            for (ClassEntry row : out.classes) {
                if (row == null || row.id == null || row.id.isBlank()) {
                    continue;
                }
                known.put(normalizeId(row.id), sanitizeRow(row));
            }
            for (String id : BLOB.classIds) {
                if (!known.containsKey(id)) {
                    ClassEntry auto = new ClassEntry();
                    auto.id = id;
                    auto.fabledSkill = skillNameFor(id);
                    auto.displayName = auto.fabledSkill;
                    auto.prestigeSkill = prestigeSkillNameFor(id);
                    known.put(id, auto);
                }
            }
            out.classes = new ArrayList<>(known.values());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(out, writer);
            }
        } catch (Exception e) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] class-fabled.json save skip: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    e.toString());
        }
    }

    private static Catalog buildCatalog(FileBlob config) {
        Set<String> ids = new LinkedHashSet<>();
        ids.addAll(discoverDmzClassIds());
        ids.addAll(discoverFabledFightingClassIds());

        Map<String, ClassEntry> byId = new LinkedHashMap<>();
        if (config != null && config.classes != null) {
            for (ClassEntry row : config.classes) {
                if (row == null || row.id == null || row.id.isBlank()) {
                    continue;
                }
                String id = normalizeId(row.id);
                ids.add(id);
                byId.put(id, sanitizeRow(row));
            }
        }

        List<ClassEntry> entries = new ArrayList<>();
        for (String id : ids) {
            ClassEntry row = byId.get(id);
            if (row == null) {
                row = new ClassEntry();
                row.id = id;
            }
            if (row.fabledSkill == null || row.fabledSkill.isBlank()) {
                row.fabledSkill = legacySkillName(id);
                if (row.fabledSkill.isBlank()) {
                    row.fabledSkill = titleCaseId(id);
                }
            }
            if (row.displayName == null || row.displayName.isBlank()) {
                row.displayName = row.fabledSkill;
            }
            if (row.prestigeSkill == null || row.prestigeSkill.isBlank()) {
                row.prestigeSkill = row.fabledSkill + PRESTIGE_SUFFIX;
            }
            entries.add(row);
            byId.put(id, row);
        }
        Catalog catalog = new Catalog();
        catalog.classIds = ids;
        catalog.entries = entries;
        catalog.byId = byId;
        return catalog;
    }

    private static ClassEntry sanitizeRow(ClassEntry row) {
        ClassEntry out = new ClassEntry();
        out.id = normalizeId(row.id);
        out.fabledSkill = row.fabledSkill == null ? "" : row.fabledSkill.trim();
        out.displayName = row.displayName == null || row.displayName.isBlank()
                ? out.fabledSkill
                : row.displayName.trim();
        out.prestigeSkill = row.prestigeSkill == null || row.prestigeSkill.isBlank()
                ? (out.fabledSkill.isBlank() ? "" : out.fabledSkill + PRESTIGE_SUFFIX)
                : row.prestigeSkill.trim();
        return out;
    }

    private static Set<String> discoverDmzClassIds() {
        Set<String> out = new LinkedHashSet<>();
        Path races = FMLPaths.GAMEDIR.get().resolve("config").resolve("dragonminez").resolve("races");
        if (!Files.isDirectory(races)) {
            return out;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(races)) {
            for (Path raceDir : stream) {
                if (!Files.isDirectory(raceDir)) {
                    continue;
                }
                Path stats = raceDir.resolve("stats.json");
                if (!Files.isRegularFile(stats)) {
                    continue;
                }
                try {
                    String raw = Files.readString(stats, StandardCharsets.UTF_8);
                    JsonElement root = JsonParser.parseString(raw);
                    if (!root.isJsonObject()) {
                        continue;
                    }
                    JsonObject classes = root.getAsJsonObject().getAsJsonObject("classes");
                    if (classes == null) {
                        continue;
                    }
                    for (String key : classes.keySet()) {
                        if (key != null && !key.isBlank()) {
                            out.add(normalizeId(key));
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static Set<String> discoverFabledFightingClassIds() {
        Set<String> out = new LinkedHashSet<>();
        Class<?> fabledClass = FabledBridge.fabledClass();
        if (fabledClass == null) {
            return out;
        }
        try {
            Method getClasses = FabledBridge.findNoArg(fabledClass, "getClasses");
            if (getClasses == null) {
                return out;
            }
            Object registered = getClasses.invoke(null);
            if (!(registered instanceof Map<?, ?> map)) {
                return out;
            }
            for (Object registeredClass : map.values()) {
                if (registeredClass == null) {
                    continue;
                }
                String group = "";
                try {
                    Object g = registeredClass.getClass().getMethod("getGroup").invoke(registeredClass);
                    group = g == null ? "" : String.valueOf(g);
                } catch (Throwable ignored) {
                }
                if ("race".equalsIgnoreCase(group.trim()) || "prestige".equalsIgnoreCase(group.trim())) {
                    continue;
                }
                String name = "";
                try {
                    Object n = registeredClass.getClass().getMethod("getName").invoke(registeredClass);
                    name = n == null ? "" : String.valueOf(n).trim();
                } catch (Throwable ignored) {
                }
                if (!name.isBlank()) {
                    out.add(normalizeId(name));
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static String matchFabledSkillName(String classId) {
        Class<?> fabledClass = FabledBridge.fabledClass();
        if (fabledClass == null) {
            return "";
        }
        String wanted = normalize(classId);
        try {
            Method getSkills = FabledBridge.findNoArg(fabledClass, "getSkills");
            if (getSkills == null) {
                return "";
            }
            Object map = getSkills.invoke(null);
            if (!(map instanceof Map<?, ?> skills)) {
                return "";
            }
            for (Object skill : skills.values()) {
                if (skill == null) {
                    continue;
                }
                String name = "";
                String key = "";
                try {
                    Object n = skill.getClass().getMethod("getName").invoke(skill);
                    name = n == null ? "" : String.valueOf(n);
                } catch (Throwable ignored) {
                }
                if (name.endsWith(PRESTIGE_SUFFIX)) {
                    continue;
                }
                try {
                    Object k = skill.getClass().getMethod("getKey").invoke(skill);
                    key = k == null ? "" : String.valueOf(k);
                } catch (Throwable ignored) {
                }
                if (normalize(name).equals(wanted) || normalize(key).equals(wanted)) {
                    return name.trim();
                }
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private static String legacySkillName(String id) {
        return switch (normalizeId(id)) {
            case "warrior" -> "Warrior";
            case "martialartist" -> "Martial Artist";
            case "spiritualist" -> "Spiritualist";
            case "berserker" -> "Berserker";
            case "cleric" -> "Cleric";
            case "paladin" -> "Paladin";
            case "tank" -> "Tank";
            default -> "";
        };
    }

    private static String normalizeId(String id) {
        if (id == null) {
            return "";
        }
        return id.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\u00A7.", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String titleCaseId(String id) {
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
                sb.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    /** One row in {@code class-fabled.json}. */
    public static final class ClassEntry {
        /** DMZ class id, e.g. {@code martialartist} or {@code shadow_knight}. */
        public String id = "";
        /** Fabled base skill display name (LP {@code fabled.skill.*}). */
        public String fabledSkill = "";
        /** Player-facing label (GUI / logs). */
        public String displayName = "";
        /** Fabled prestige skill name — defaults to {@code fabledSkill + " Prestige"}. */
        public String prestigeSkill = "";
    }

    private static final class FileBlob {
        List<ClassEntry> classes = new ArrayList<>();

        static FileBlob empty() {
            return new FileBlob();
        }
    }

    private static final class Catalog {
        Set<String> classIds = Set.of();
        List<ClassEntry> entries = List.of();
        Map<String, ClassEntry> byId = Map.of();

        static Catalog empty() {
            Catalog c = new Catalog();
            c.classIds = new LinkedHashSet<>();
            c.entries = new ArrayList<>();
            c.byId = new LinkedHashMap<>();
            return c;
        }
    }
}
