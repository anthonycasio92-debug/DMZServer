package com.dbzlegacy.adaptivedifficulty.character;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Fighting class ids as DMZ {@link ConfigManager} exposes them (matches recustomize UI). */
public final class DmzRuntimeClassIds {
    private DmzRuntimeClassIds() {}

    public static List<String> classIdsForRace(String raceId) {
        if (raceId == null || raceId.isBlank()) {
            return List.of();
        }
        String race = raceId.trim().toLowerCase(Locale.ROOT);
        List<String> fromRuntime = fromConfigManager(race);
        if (!fromRuntime.isEmpty()) {
            return fromRuntime;
        }
        return DmzContentDiscovery.classIdsForRace(race);
    }

    public static String canonicalId(String raceId, String classId) {
        if (classId == null || classId.isBlank()) {
            return null;
        }
        String want = classId.trim().toLowerCase(Locale.ROOT);
        for (String id : classIdsForRace(raceId)) {
            if (want.equalsIgnoreCase(id)) {
                return id;
            }
        }
        return null;
    }

    private static List<String> fromConfigManager(String race) {
        List<String> out = new ArrayList<>();
        try {
            RaceStatsConfig cfg = ConfigManager.getRaceStats(race);
            if (cfg == null) {
                return out;
            }
            Collection<String> classes = cfg.getAllClasses();
            if (classes == null) {
                return out;
            }
            for (String id : classes) {
                if (id != null && !id.isBlank()) {
                    // Exact strings — DMZ UpdateCharacterC2S uses Collection.contains(className).
                    out.add(id.trim());
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }
}
