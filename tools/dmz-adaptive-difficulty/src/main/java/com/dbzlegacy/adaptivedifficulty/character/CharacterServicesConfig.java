package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.ConfigPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** {@code config/legacymechanics/character-services.json} — rebirth / race / class / reskin costs. */
public final class CharacterServicesConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile CharacterServicesConfig INSTANCE = defaults();

    public boolean enabled = true;
    public Permissions permissions = new Permissions();
    public Restrictions restrictions = new Restrictions();
    public RaceChange raceChange = new RaceChange();
    public ClassChange classChange = new ClassChange();
    public Reskin reskin = new Reskin();
    public HeadBoneShop headBoneShop = new HeadBoneShop();

    public static CharacterServicesConfig get() {
        return INSTANCE;
    }

    /** Field defaults for the editor. Does not replace the live config. */
    public static CharacterServicesConfig freshDefaults() {
        return defaults();
    }

    public static Path path() {
        return ConfigPaths.dataDir().resolve("character-services.json");
    }

    public static void load() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            if (!Files.isRegularFile(file)) {
                seedDefault(file);
            }
            try (Reader reader = Files.newBufferedReader(file)) {
                CharacterServicesConfig parsed = GSON.fromJson(reader, CharacterServicesConfig.class);
                INSTANCE = sanitize(parsed == null ? defaults() : parsed);
                CosmeticHeadBoneCatalog.invalidate();
                CosmeticHeadBoneCatalog.refreshIfStale();
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] character-services config load failed: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
            INSTANCE = defaults();
        }
    }

    /** Writes the live config. Staff edits use this so the file matches the menu. */
    public static boolean save() {
        INSTANCE = sanitize(INSTANCE == null ? defaults() : INSTANCE);
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(INSTANCE, writer);
            }
            CosmeticHeadBoneCatalog.invalidate();
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] character-services config save failed: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
            return false;
        }
    }

    private static void seedDefault(Path file) throws Exception {
        try (var in = CharacterServicesConfig.class.getClassLoader()
                .getResourceAsStream("character-services.default.json")) {
            if (in != null) {
                Files.copy(in, file);
                return;
            }
        }
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(defaults(), w);
        }
    }

    private static CharacterServicesConfig defaults() {
        CharacterServicesConfig c = new CharacterServicesConfig();
        c.raceChange.preservationCostCopper = defaultPreservationCosts();
        return c;
    }

    private static Map<String, Long> defaultPreservationCosts() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (int pct : defaultPreservationSteps()) {
            m.put(String.valueOf(pct), (long) pct * 10_000L);
        }
        return m;
    }

    /** Seed list used only when the live map is empty. */
    public static int[] defaultPreservationSteps() {
        return new int[] {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
    }

    /**
     * Percents the race-change GUI may offer and {@code executeRaceChange} may charge.
     * Read from {@code raceChange.preservationCostCopper} so the label and the charge share one map.
     */
    public static int[] preservationSteps() {
        Map<String, Long> map = null;
        CharacterServicesConfig live = INSTANCE;
        if (live != null && live.raceChange != null) {
            map = live.raceChange.preservationCostCopper;
        }
        if (map == null || map.isEmpty()) {
            return defaultPreservationSteps();
        }
        java.util.TreeSet<Integer> steps = new java.util.TreeSet<>();
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            int pct;
            try {
                pct = Integer.parseInt(entry.getKey().trim());
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (pct < 0 || pct > 100) {
                continue;
            }
            long cost = entry.getValue() == null ? -1L : entry.getValue();
            if (cost < 0L) {
                continue;
            }
            steps.add(pct);
        }
        steps.add(0);
        int[] out = new int[steps.size()];
        int i = 0;
        for (int pct : steps) {
            out[i++] = pct;
        }
        return out;
    }

    private static CharacterServicesConfig sanitize(CharacterServicesConfig c) {
        if (c.permissions == null) {
            c.permissions = new Permissions();
        }
        if (c.restrictions == null) {
            c.restrictions = new Restrictions();
        }
        if (c.raceChange == null) {
            c.raceChange = new RaceChange();
        }
        if (c.classChange == null) {
            c.classChange = new ClassChange();
        }
        if (c.reskin == null) {
            c.reskin = new Reskin();
        }
        if (c.headBoneShop == null) {
            c.headBoneShop = new HeadBoneShop();
        }
        if (c.headBoneShop.boneCosts == null) {
            c.headBoneShop.boneCosts = new LinkedHashMap<>();
        }
        if (c.headBoneShop.displayNames == null) {
            c.headBoneShop.displayNames = new LinkedHashMap<>();
        }
        if (c.headBoneShop.excludeBones == null) {
            c.headBoneShop.excludeBones = Collections.emptyList();
        }
        if (c.raceChange.preservationCostCopper == null || c.raceChange.preservationCostCopper.isEmpty()) {
            c.raceChange.preservationCostCopper = defaultPreservationCosts();
        } else {
            c.raceChange.preservationCostCopper.putIfAbsent("0", 0L);
        }
        if (c.raceChange.blockedRaces == null) {
            c.raceChange.blockedRaces = Collections.emptyList();
        }
        return c;
    }

    public long raceCostCopper(int preservationPercent) {
        if (preservationPercent <= 0) {
            return 0L;
        }
        if (raceChange.preservationCostCopper == null) {
            return 0L;
        }
        String key = String.valueOf(preservationPercent);
        Long v = raceChange.preservationCostCopper.get(key);
        if (v == null || v < 0L) {
            return 0L;
        }
        return v;
    }

    public Set<String> blockedRaceSet() {
        Set<String> out = new TreeSet<>();
        for (String r : raceChange.blockedRaces) {
            if (r != null && !r.isBlank()) {
                out.add(r.trim().toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    public static final class Permissions {
        public String services = "legacymechanics.character.services";
        public String race = "legacymechanics.character.racechange";
        public String classChange = "legacymechanics.character.classchange";
        public String reskin = "legacymechanics.character.reskin";
        public String bypassCost = "legacymechanics.character.bypass.cost";
        public String bypassCooldown = "legacymechanics.character.bypass.cooldown";
        public String admin = "legacymechanics.character.admin";
        public String headBones = "legacymechanics.character.headbones";
    }

    public static final class Restrictions {
        public boolean blockWhileDead = true;
        /** When true, legacy strict mode (unused — services auto-detransform instead). */
        public boolean blockWhileTransformed = false;
        public boolean blockWhileInCombat = false;
        public boolean blockWhileSparring = true;
    }

    public static final class RaceChange {
        public boolean enabled = true;
        public long cooldownMs = 12L * 60L * 60L * 1000L;
        public boolean keepSkillsOnRaceChange = true;
        public Map<String, Long> preservationCostCopper = defaultPreservationCosts();
        public boolean levelCostMultiplier = true;
        public java.util.List<String> blockedRaces = Collections.emptyList();
    }

    public static final class ClassChange {
        public boolean enabled = true;
        public long cooldownMs = 12L * 60L * 60L * 1000L;
        public long baseCostCopper = 250_000L;
        public boolean levelCostMultiplier = true;
        public boolean preserveBaseStats = true;
        public boolean resetClassProgression = true;
    }

    public static final class Reskin {
        public boolean enabled = true;
        public long cooldownMs = 12L * 60L * 60L * 1000L;
        public long baseCostCopper = 50_000L;
        public boolean levelCostMultiplier = true;
        public boolean openDmzRecustomizeOnly = true;
        /** DMZ recustomize UI includes a class tab — server locks class while reskin session is active. */
        public boolean lockClassDuringEditor = true;
    }

    /** Global catalog shop — unlock head parts from any race for cross-race cosmetics. */
    public static final class HeadBoneShop {
        public boolean enabled = true;
        public long defaultUnlockCostCopper = 100_000L;
        public boolean levelCostMultiplier = true;
        /** Parts listed on the player's current race config do not require a shop unlock. */
        public boolean nativeRaceBonesFree = true;
        public Map<String, Long> boneCosts = new LinkedHashMap<>();
        public Map<String, String> displayNames = new LinkedHashMap<>();
        public java.util.List<String> excludeBones = Collections.emptyList();
        /** Re-apply {@link CharacterServicesStore.PlayerRecord#equippedHeadBone} after transformations. */
        public boolean persistThroughForms = true;

        public boolean isExcluded(String boneId) {
            if (boneId == null || excludeBones == null) {
                return false;
            }
            String want = boneId.trim().toLowerCase(Locale.ROOT);
            for (String e : excludeBones) {
                if (e != null && want.equals(e.trim().toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            return false;
        }
    }
}
