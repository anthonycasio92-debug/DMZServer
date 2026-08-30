package com.dbzlegacy.adaptivedifficulty.progression.race;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.ConfigPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Editable Race Lock list — {@code config/legacymechanics/race-lock.json}.
 * Add races here without recompiling the mod. Reload with {@code /difficulty reload}
 * (or any LM config reload).
 */
public final class RaceLockConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile FileBlob BLOB = FileBlob.defaults();

    private RaceLockConfig() {}

    public static Path path() {
        return ConfigPaths.dataDir().resolve("race-lock.json");
    }

    /** Snapshot of restricted races (never null; may be empty). */
    public static List<RestrictedRace> restricted() {
        FileBlob blob = BLOB;
        if (blob == null || blob.restricted == null || blob.restricted.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(blob.restricted));
    }

    /** Match by DMZ race id (case-insensitive). */
    public static RestrictedRace findByRaceId(String raceId) {
        if (raceId == null || raceId.isBlank()) {
            return null;
        }
        String lower = raceId.toLowerCase(Locale.ROOT).trim();
        for (RestrictedRace entry : restricted()) {
            if (entry == null || entry.id == null) {
                continue;
            }
            if (entry.id.toLowerCase(Locale.ROOT).trim().equals(lower)) {
                return entry;
            }
        }
        return null;
    }

    public static boolean load() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                BLOB = FileBlob.defaults();
                save();
                AdaptiveDifficultyMod.LOGGER.info(
                        "[{}] wrote default Race Lock config {}",
                        AdaptiveDifficultyMod.MOD_ID, file);
                return true;
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                FileBlob loaded = GSON.fromJson(reader, FileBlob.class);
                BLOB = sanitize(loaded);
            }
            // Re-save so new fields / cleaned ids appear for admins.
            save();
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] loaded Race Lock config ({} restricted race(s)) from {}",
                    AdaptiveDifficultyMod.MOD_ID, restricted().size(), file);
            return true;
        } catch (Exception e) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] failed to load Race Lock config {}; using defaults: {}",
                    AdaptiveDifficultyMod.MOD_ID, file, e.toString());
            BLOB = FileBlob.defaults();
            try {
                save();
            } catch (Throwable ignored) {
            }
            return false;
        }
    }

    public static boolean reload() {
        return load();
    }

    public static void save() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(sanitize(BLOB == null ? FileBlob.defaults() : BLOB), writer);
            }
        } catch (Exception e) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] failed to save Race Lock config {}: {}",
                    AdaptiveDifficultyMod.MOD_ID, file, e.toString());
        }
    }

    private static FileBlob sanitize(FileBlob raw) {
        FileBlob out = raw == null ? new FileBlob() : raw;
        if (out.restricted == null) {
            out.restricted = new ArrayList<>();
        }
        List<RestrictedRace> cleaned = new ArrayList<>();
        for (RestrictedRace entry : out.restricted) {
            if (entry == null) {
                continue;
            }
            String id = entry.id == null ? "" : entry.id.trim().toLowerCase(Locale.ROOT);
            if (id.isBlank()) {
                continue;
            }
            RestrictedRace keep = new RestrictedRace();
            keep.id = id;
            keep.fabledSkill = entry.fabledSkill == null || entry.fabledSkill.isBlank()
                    ? titleCaseId(id)
                    : entry.fabledSkill.trim();
            keep.displayName = entry.displayName == null || entry.displayName.isBlank()
                    ? keep.fabledSkill
                    : entry.displayName.trim();
            keep.prestigeTooltip = Math.max(0, entry.prestigeTooltip);
            cleaned.add(keep);
        }
        // First boot / empty file → ship the live defaults.
        if (cleaned.isEmpty()) {
            cleaned.addAll(FileBlob.defaults().restricted);
        }
        out.restricted = cleaned;
        return out;
    }

    private static String titleCaseId(String id) {
        String[] parts = id.replace('-', '_').split("_");
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

    /** One restricted race row in {@code race-lock.json}. */
    public static final class RestrictedRace {
        /** DMZ race id, e.g. {@code ancient_saiyan}. */
        public String id = "";
        /** Fabled skill name that permanently unlocks the race. */
        public String fabledSkill = "";
        /** Player-facing name in lock messages. */
        public String displayName = "";
        /**
         * Tooltip-only prestige hint for the race-select padlock UI
         * (KubeJS {@code dmz_race_locks}). Not enforced by the Java lock.
         */
        public int prestigeTooltip = 1;
    }

    private static final class FileBlob {
        List<RestrictedRace> restricted = new ArrayList<>();

        static FileBlob defaults() {
            FileBlob blob = new FileBlob();
            blob.restricted = new ArrayList<>();
            blob.restricted.add(entry("ancient_saiyan", "Ancient Saiyan", "Ancient Saiyan", 10));
            blob.restricted.add(entry("sento_saiyan", "Sento Saiyan", "Sento Saiyan", 1));
            return blob;
        }

        private static RestrictedRace entry(String id, String skill, String display, int tip) {
            RestrictedRace r = new RestrictedRace();
            r.id = id;
            r.fabledSkill = skill;
            r.displayName = display;
            r.prestigeTooltip = tip;
            return r;
        }
    }
}
