package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Live DMZ content from {@code config/dragonminez/races/} plus {@link ConfigManager}.
 * Used so new/changed races, classes, and head bones appear without a mod rebuild.
 */
public final class DmzContentDiscovery {
    /** Last logged discovery signature — avoid INFO spam on every menu open. */
    private static volatile String lastDiscoveryLogKey = "";
    private static volatile long lastDiscoveryLogAtMs;

    private DmzContentDiscovery() {}

    public static Path racesRoot() {
        return FMLPaths.GAMEDIR.get().resolve("config").resolve("dragonminez").resolve("races");
    }

    /**
     * All known race ids (lowercase), sorted for stable menus.
     * Union of {@link ConfigManager#getLoadedRaces()} and on-disk race folders that have
     * {@code character.json} and/or {@code stats.json}.
     */
    public static List<String> discoverRaceIds() {
        LinkedHashSet<String> fromConfig = new LinkedHashSet<>();
        LinkedHashSet<String> fromDisk = new LinkedHashSet<>();
        List<String> skippedNoDef = new ArrayList<>();
        try {
            for (String race : ConfigManager.getLoadedRaces()) {
                if (race != null && !race.isBlank()) {
                    String id = race.trim().toLowerCase(Locale.ROOT);
                    fromConfig.add(id);
                    AdaptiveDifficultyMod.LOGGER.debug(
                            "[{}] discoverRaceIds: config → {}", AdaptiveDifficultyMod.MOD_ID, id);
                }
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] discoverRaceIds: ConfigManager.getLoadedRaces failed: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
        }
        Path root = racesRoot();
        if (!Files.isDirectory(root)) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] discoverRaceIds: racesRoot missing or not a directory: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    root.toAbsolutePath());
            List<String> onlyConfig = sortedCopy(fromConfig);
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] discoverRaceIds: {} race(s) (config only, no disk scan) → {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    onlyConfig.size(),
                    onlyConfig);
            return onlyConfig;
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
                String id = name.trim().toLowerCase(Locale.ROOT);
                if (hasRaceDefinition(raceDir)) {
                    fromDisk.add(id);
                    AdaptiveDifficultyMod.LOGGER.debug(
                            "[{}] discoverRaceIds: disk → {} ({})",
                            AdaptiveDifficultyMod.MOD_ID,
                            id,
                            raceDir.toAbsolutePath());
                } else {
                    skippedNoDef.add(id);
                    AdaptiveDifficultyMod.LOGGER.warn(
                            "[{}] discoverRaceIds: skipped {} — no character.json/stats.json under {}",
                            AdaptiveDifficultyMod.MOD_ID,
                            id,
                            raceDir.toAbsolutePath());
                }
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] discoverRaceIds: directory scan failed under {}: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    root.toAbsolutePath(),
                    t.toString());
        }
        TreeSet<String> merged = new TreeSet<>();
        merged.addAll(fromConfig);
        merged.addAll(fromDisk);
        List<String> out = List.copyOf(merged);
        LinkedHashSet<String> diskOnly = new LinkedHashSet<>(fromDisk);
        diskOnly.removeAll(fromConfig);
        LinkedHashSet<String> configOnly = new LinkedHashSet<>(fromConfig);
        configOnly.removeAll(fromDisk);
        int both = fromConfig.size() - configOnly.size();
        String summaryKey = root.toAbsolutePath() + "|" + out + "|skip=" + skippedNoDef;
        long now = System.currentTimeMillis();
        boolean changed = !summaryKey.equals(lastDiscoveryLogKey);
        boolean due = now - lastDiscoveryLogAtMs > 30_000L;
        if (changed || due) {
            lastDiscoveryLogKey = summaryKey;
            lastDiscoveryLogAtMs = now;
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] discoverRaceIds: root={} total={} config={} disk={} both={} diskOnly={} configOnly={} skippedNoDef={} → {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    root.toAbsolutePath(),
                    out.size(),
                    fromConfig.size(),
                    fromDisk.size(),
                    both,
                    diskOnly,
                    configOnly,
                    skippedNoDef,
                    out);
        } else {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] discoverRaceIds: unchanged total={} → {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    out.size(),
                    out);
        }
        return out;
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

    /**
     * Fighting classes defined for a race — {@code stats.json} on disk is authoritative when present
     * (per-race class list and order). Falls back to live {@link ConfigManager} only when the file
     * is missing.
     */
    public static List<String> classIdsForRace(String raceId) {
        if (raceId == null || raceId.isBlank()) {
            return List.of();
        }
        String race = raceId.trim().toLowerCase(Locale.ROOT);
        List<String> fromFile = readClassIdsFromStatsFileOrdered(race);
        if (!fromFile.isEmpty()) {
            return fromFile;
        }
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
        AdaptiveDifficultyMod.LOGGER.debug(
                "[{}] headBonesForRace({}) → {}", AdaptiveDifficultyMod.MOD_ID, race, ordered);
        return ordered;
    }

    private static boolean hasRaceDefinition(Path raceDir) {
        return Files.isRegularFile(raceDir.resolve("stats.json"))
                || Files.isRegularFile(raceDir.resolve("character.json"));
    }

    private static List<String> sortedCopy(Set<String> ids) {
        List<String> out = new ArrayList<>(ids);
        Collections.sort(out);
        return List.copyOf(out);
    }

    private static List<String> readClassIdsFromStatsFileOrdered(String raceId) {
        List<String> out = new ArrayList<>();
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
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] failed reading headBones from {}: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    file.toAbsolutePath(),
                    t.toString());
        }
        return out;
    }

    private static String normalizeId(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
