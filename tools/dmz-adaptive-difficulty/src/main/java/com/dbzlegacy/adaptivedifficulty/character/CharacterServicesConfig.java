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

    public static CharacterServicesConfig get() {
        return INSTANCE;
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
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] character-services config load failed: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
            INSTANCE = defaults();
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
        for (int pct : preservationSteps()) {
            m.put(String.valueOf(pct), (long) pct * 10_000L);
        }
        return m;
    }

    public static int[] preservationSteps() {
        return new int[] {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
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
        if (c.raceChange.preservationCostCopper == null || c.raceChange.preservationCostCopper.isEmpty()) {
            c.raceChange.preservationCostCopper = defaultPreservationCosts();
        }
        if (c.raceChange.blockedRaces == null) {
            c.raceChange.blockedRaces = Collections.emptyList();
        }
        return c;
    }

    public long raceCostCopper(int preservationPercent) {
        String key = String.valueOf(preservationPercent);
        Long v = raceChange.preservationCostCopper.get(key);
        if (v == null || v < 0L) {
            v = raceChange.preservationCostCopper.get(String.valueOf(100));
        }
        return v == null ? 0L : Math.max(0L, v);
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
    }

    public static final class Restrictions {
        public boolean blockWhileDead = true;
        public boolean blockWhileTransformed = true;
        public boolean blockWhileInCombat = false;
        public boolean blockWhileSparring = true;
    }

    public static final class RaceChange {
        public boolean enabled = true;
        public long cooldownMs = 7L * 24L * 60L * 60L * 1000L;
        public boolean keepSkillsOnRaceChange = true;
        public Map<String, Long> preservationCostCopper = defaultPreservationCosts();
        public boolean levelCostMultiplier = true;
        public java.util.List<String> blockedRaces = Collections.emptyList();
    }

    public static final class ClassChange {
        public boolean enabled = true;
        public long cooldownMs = 3L * 24L * 60L * 60L * 1000L;
        public long baseCostCopper = 250_000L;
        public boolean levelCostMultiplier = true;
        public boolean preserveBaseStats = true;
        public boolean resetClassProgression = true;
    }

    public static final class Reskin {
        public boolean enabled = true;
        public long cooldownMs = 24L * 60L * 60L * 1000L;
        public long baseCostCopper = 50_000L;
        public boolean levelCostMultiplier = true;
        public boolean openDmzRecustomizeOnly = true;
        /** DMZ recustomize UI includes a class tab — server locks class while reskin session is active. */
        public boolean lockClassDuringEditor = true;
    }
}
