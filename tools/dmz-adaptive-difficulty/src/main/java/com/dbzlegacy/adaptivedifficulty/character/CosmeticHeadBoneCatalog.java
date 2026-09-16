package com.dbzlegacy.adaptivedifficulty.character;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/** All {@code headBones} ids from every loaded DMZ race (global cosmetic catalog). */
public final class CosmeticHeadBoneCatalog {
    private static volatile List<Entry> CACHE = List.of();
    private static volatile long CACHE_AT;

    private CosmeticHeadBoneCatalog() {}

    public static List<Entry> all() {
        refreshIfStale();
        return CACHE;
    }

    public static boolean isKnown(String boneId) {
        if (boneId == null || boneId.isBlank()) {
            return false;
        }
        String key = boneId.trim().toLowerCase(Locale.ROOT);
        for (Entry e : all()) {
            if (e.id().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static Entry get(String boneId) {
        if (boneId == null || boneId.isBlank()) {
            return null;
        }
        String key = boneId.trim().toLowerCase(Locale.ROOT);
        for (Entry e : all()) {
            if (e.id().equals(key)) {
                return e;
            }
        }
        return null;
    }

    public static void invalidate() {
        CACHE_AT = 0L;
    }

    public static void refreshIfStale() {
        long now = System.currentTimeMillis();
        if (!CACHE.isEmpty() && now - CACHE_AT < 60_000L) {
            return;
        }
        CACHE = build();
        CACHE_AT = now;
    }

    private static List<Entry> build() {
        Map<String, TreeSet<String>> byBone = new LinkedHashMap<>();
        try {
            for (String raceId : ConfigManager.getLoadedRaces()) {
                if (raceId == null || raceId.isBlank()) {
                    continue;
                }
                String race = raceId.trim().toLowerCase(Locale.ROOT);
                RaceCharacterConfig cfg = ConfigManager.getRaceCharacter(race);
                if (cfg == null) {
                    continue;
                }
                String[] bones = cfg.getHeadBones();
                if (bones == null) {
                    continue;
                }
                for (String bone : bones) {
                    if (bone == null || bone.isBlank()) {
                        continue;
                    }
                    String id = bone.trim().toLowerCase(Locale.ROOT);
                    byBone.computeIfAbsent(id, k -> new TreeSet<>()).add(race);
                }
            }
        } catch (Throwable ignored) {
        }
        if (byBone.isEmpty()) {
            byBone.put("hair", new TreeSet<>(List.of("human")));
        }
        CharacterServicesConfig.HeadBoneShop shop = CharacterServicesConfig.get().headBoneShop;
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<String, TreeSet<String>> e : byBone.entrySet()) {
            if (shop.isExcluded(e.getKey())) {
                continue;
            }
            out.add(new Entry(e.getKey(), labelFor(e.getKey(), shop), List.copyOf(e.getValue())));
        }
        out.sort(Comparator.comparing(Entry::id));
        return out;
    }

    private static String labelFor(String boneId, CharacterServicesConfig.HeadBoneShop shop) {
        if (shop.displayNames != null) {
            String custom = shop.displayNames.get(boneId);
            if (custom != null && !custom.isBlank()) {
                return custom.trim();
            }
        }
        return prettyId(boneId);
    }

    static String prettyId(String id) {
        if (id == null || id.isBlank()) {
            return "?";
        }
        String[] parts = id.replace('_', ' ').split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.isEmpty() ? id : sb.toString();
    }

    public record Entry(String id, String displayName, List<String> sourceRaces) {
        public Entry {
            id = id == null ? "" : id.toLowerCase(Locale.ROOT);
            displayName = displayName == null || displayName.isBlank() ? id : displayName;
            sourceRaces = sourceRaces == null ? Collections.emptyList() : sourceRaces;
        }
    }
}
