package com.dbzlegacy.adaptivedifficulty.character;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Live DMZ content from {@code config/dragonminez/races/} plus {@link ConfigManager}.
 * Used so new/changed races, classes, and head bones appear without a mod rebuild.
 */
public final class DmzContentDiscovery {
    private DmzContentDiscovery() {}

    public static Path racesRoot() {
        return FMLPaths.GAMEDIR.get().resolve("config").resolve("dragonminez").resolve("races");
    }

    public static List<String> discoverRaceIds() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        try {
            for (String race : ConfigManager.getLoadedRaces()) {
                if (race != null && !race.isBlank()) {
                    out.add(race.trim().toLowerCase(Locale.ROOT));
                }
            }
        } catch (Throwable ignored) {
        }
        Path root = racesRoot();
        if (!Files.isDirectory(root)) {
            return List.copyOf(out);
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path raceDir : stream) {
                if (!Files.isDirectory(raceDir)) {
                    continue;
                }
                String name = raceDir.getFileName().toString();
                if (name == null || name.isBlank() || name.startsWith(".")) {
                    continue;
                }
                if (hasRaceDefinition(raceDir)) {
                    out.add(name.trim().toLowerCase(Locale.ROOT));
                }
            }
        } catch (Throwable ignored) {
        }
        return List.copyOf(out);
    }

    public static boolean isKnownRace(String raceId) {
        if (raceId == null || raceId.isBlank()) {
            return false;
        }
        String want = raceId.trim().toLowerCase(Locale.ROOT);
        for (String id : discoverRaceIds()) {
            if (want.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** Union of DMZ runtime config and on-disk {@code stats.json} class keys. */
    public static List<String> classIdsForRace(String raceId) {
        if (raceId == null || raceId.isBlank()) {
            return List.of();
        }
        String race = raceId.trim().toLowerCase(Locale.ROOT);
        LinkedHashSet<String> out = new LinkedHashSet<>();
        try {
            RaceStatsConfig stats = ConfigManager.getRaceStats(race);
            if (stats != null) {
                java.util.Collection<String> classes = stats.getAllClasses();
                if (classes != null) {
                    for (String id : classes) {
                        if (id != null && !id.isBlank()) {
                            out.add(normalizeId(id));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        out.addAll(readClassIdsFromStatsFile(race));
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    /** Union of DMZ {@link RaceCharacterConfig} and on-disk {@code character.json}. */
    public static List<String> headBonesForRace(String raceId) {
        return List.copyOf(new LinkedHashSet<>(headBonesForRaceOrdered(raceId)));
    }

    /** Preserves {@code character.json} headBones order, then appends any from runtime config. */
    public static List<String> headBonesForRaceOrdered(String raceId) {
        if (raceId == null || raceId.isBlank()) {
            return List.of();
        }
        String race = raceId.trim().toLowerCase(Locale.ROOT);
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<String> ordered = new ArrayList<>();
        for (String bone : readHeadBonesFromCharacterFileOrdered(race)) {
            if (seen.add(bone)) {
                ordered.add(bone);
            }
        }
        try {
            RaceCharacterConfig cfg = ConfigManager.getRaceCharacter(race);
            if (cfg != null) {
                String[] bones = cfg.getHeadBones();
                if (bones != null) {
                    for (String bone : bones) {
                        if (bone == null || bone.isBlank()) {
                            continue;
                        }
                        String id = bone.trim().toLowerCase(Locale.ROOT);
                        if (seen.add(id)) {
                            ordered.add(id);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return ordered;
    }

    private static boolean hasRaceDefinition(Path raceDir) {
        return Files.isRegularFile(raceDir.resolve("stats.json"))
                || Files.isRegularFile(raceDir.resolve("character.json"));
    }

    private static Set<String> readClassIdsFromStatsFile(String raceId) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        Path stats = racesRoot().resolve(raceId).resolve("stats.json");
        if (!Files.isRegularFile(stats)) {
            return out;
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(stats, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) {
                return out;
            }
            JsonObject classes = root.getAsJsonObject().getAsJsonObject("classes");
            if (classes == null) {
                return out;
            }
            for (String key : classes.keySet()) {
                if (key != null && !key.isBlank()) {
                    out.add(normalizeId(key));
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static List<String> readHeadBonesFromCharacterFileOrdered(String raceId) {
        List<String> out = new ArrayList<>();
        Path file = racesRoot().resolve(raceId).resolve("character.json");
        if (!Files.isRegularFile(file)) {
            return out;
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) {
                return out;
            }
            JsonArray bones = root.getAsJsonObject().getAsJsonArray("headBones");
            if (bones == null) {
                return out;
            }
            for (JsonElement el : bones) {
                if (el != null && el.isJsonPrimitive()) {
                    String bone = el.getAsString();
                    if (bone != null && !bone.isBlank()) {
                        out.add(bone.trim().toLowerCase(Locale.ROOT));
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static String normalizeId(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
