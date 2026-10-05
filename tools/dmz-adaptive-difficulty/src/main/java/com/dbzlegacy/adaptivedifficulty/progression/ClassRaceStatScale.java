package com.dbzlegacy.adaptivedifficulty.progression;

import com.dmzrevamp.revamp.classes.DmzClassConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Fighting-class coefficient used by stamina and the other stats.
 *
 * <p>dmzrevamp stores the race baseline in {@code config/dragonminez/races/<race>/stats.json}
 * under {@code classes.race} and the fighting class in {@code config/dragonminez/classes}.
 * {@code DmzClassConfigManager.prepareRaceStats} adds those two. {@code FusionRevampLogic.addPartnerScale}
 * then multiplies the sum by Overhaul prestige ({@code 1 + count × 0.5} on this server).
 *
 * <p>{@code dmzlegacy-legacysaga} {@code RevampClassScalingFix} is the same sum, but its mixin
 * is rejected at startup ({@code missing an @Mixin annotation}), so the sum never replaces
 * the prestiged value. This class applies that sum from the live files. Prestige stays off it.
 */
public final class ClassRaceStatScale {
    private static final String[] RACE_CLASS_KEYS = {
            "race", "default", "racedefault", "race_default", "base"
    };
    private static final ConcurrentHashMap<String, CachedJson> JSON = new ConcurrentHashMap<>();

    private ClassRaceStatScale() {}

    /**
     * Race baseline plus fighting-class scaling for {@code stat} ({@code STR}, {@code STM}, …).
     * {@link Double#NaN} when the stat or the player race is unknown.
     */
    public static double scaling(StatsData data, String stat) {
        try {
            String key = statKey(stat);
            String race = raceId(data);
            if (key == null || race.isEmpty()) {
                return Double.NaN;
            }
            Part racePart = racePart(race);
            if (isRaceClass(classId(data))) {
                return racePart.known ? racePart.scaling(key) : Double.NaN;
            }
            Part classPart = classPart(classId(data));
            if (!racePart.known && !classPart.known) {
                return Double.NaN;
            }
            return racePart.scaling(key) + classPart.scaling(key);
        } catch (Throwable ignored) {
            return Double.NaN;
        }
    }

    /**
     * Starting attributes: race {@code baseStats} plus the fighting class.
     * Null when neither source is on disk or in Overhaul's class table.
     */
    public static RaceStatsConfig.BaseStats base(StatsData data) {
        try {
            return baseUnchecked(data);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static RaceStatsConfig.BaseStats baseUnchecked(StatsData data) {
        String race = raceId(data);
        if (race.isEmpty()) {
            return null;
        }
        Part racePart = racePart(race);
        String cls = classId(data);
        Part classPart = isRaceClass(cls) ? Part.EMPTY : classPart(cls);
        if (!racePart.known && !classPart.known) {
            return null;
        }
        RaceStatsConfig.BaseStats out = new RaceStatsConfig.BaseStats();
        out.setStrength(racePart.base("STR") + classPart.base("STR"));
        out.setStrikePower(racePart.base("SKP") + classPart.base("SKP"));
        out.setResistance(racePart.base("RES") + classPart.base("RES"));
        out.setVitality(racePart.base("VIT") + classPart.base("VIT"));
        out.setKiPower(racePart.base("PWR") + classPart.base("PWR"));
        out.setEnergy(racePart.base("ENE") + classPart.base("ENE"));
        return out;
    }

    private static Part racePart(String race) {
        JsonObject node = raceClassNode(race);
        if (node != null) {
            return Part.fromJson(node);
        }
        try {
            RaceStatsConfig.ClassStats fallback = DmzClassConfigManager.createRaceDefaultStats(race);
            if (fallback != null) {
                return Part.fromClassStats(fallback);
            }
        } catch (Throwable ignored) {
        }
        return Part.EMPTY;
    }

    private static Part classPart(String classId) {
        if (classId.isEmpty()) {
            return Part.EMPTY;
        }
        try {
            RaceStatsConfig.ClassStats configured = DmzClassConfigManager.getConfiguredClassStats(classId);
            if (configured != null) {
                return Part.fromClassStats(configured);
            }
        } catch (Throwable ignored) {
        }
        JsonObject file = readJson(classesDir().resolve(classId + ".json"));
        if (file != null) {
            return Part.fromJson(file);
        }
        return Part.EMPTY;
    }

    private static JsonObject raceClassNode(String race) {
        JsonObject root = readJson(racesDir().resolve(race).resolve("stats.json"));
        if (root == null) {
            return null;
        }
        JsonObject classes = root.getAsJsonObject("classes");
        if (classes == null) {
            return null;
        }
        for (String key : RACE_CLASS_KEYS) {
            JsonObject node = object(classes, key);
            if (node != null) {
                return node;
            }
        }
        for (String key : classes.keySet()) {
            if (isRaceClass(key)) {
                JsonObject node = object(classes, key);
                if (node != null) {
                    return node;
                }
            }
        }
        return null;
    }

    private static JsonObject readJson(Path path) {
        try {
            if (path == null || !Files.isRegularFile(path)) {
                return null;
            }
            long mtime = Files.getLastModifiedTime(path).toMillis();
            String cacheKey = path.toAbsolutePath().normalize().toString();
            CachedJson cached = JSON.get(cacheKey);
            if (cached != null && cached.mtime == mtime) {
                return cached.json;
            }
            JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonObject json = parsed.getAsJsonObject();
            JSON.put(cacheKey, new CachedJson(mtime, json));
            return json;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null || key == null || !parent.has(key) || parent.get(key).isJsonNull()) {
            return null;
        }
        JsonElement el = parent.get(key);
        return el.isJsonObject() ? el.getAsJsonObject() : null;
    }

    private static Path racesDir() {
        return FMLPaths.GAMEDIR.get().resolve("config").resolve("dragonminez").resolve("races");
    }

    private static Path classesDir() {
        return FMLPaths.GAMEDIR.get().resolve("config").resolve("dragonminez").resolve("classes");
    }

    private static String raceId(StatsData data) {
        try {
            Character ch = data == null ? null : data.getCharacter();
            if (ch == null) {
                return "";
            }
            String race = ch.getRaceName();
            if (race == null || race.isBlank()) {
                race = ch.getRace();
            }
            return norm(race);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String classId(StatsData data) {
        try {
            Character ch = data == null ? null : data.getCharacter();
            if (ch == null) {
                return "";
            }
            return norm(ch.getCharacterClass());
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static boolean isRaceClass(String classId) {
        if (classId == null || classId.isEmpty()) {
            return true;
        }
        for (String key : RACE_CLASS_KEYS) {
            if (key.equals(classId)) {
                return true;
            }
        }
        return false;
    }

    private static String norm(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    /** {@code STR}/{@code RES} → scaling key. Defense is stored as {@code DEF}. */
    static String statKey(String stat) {
        if (stat == null || stat.isBlank()) {
            return null;
        }
        String key = stat.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            case "STR", "SKP", "STM", "DEF", "VIT", "PWR", "ENE" -> key;
            case "RES" -> "DEF";
            default -> null;
        };
    }

    private record CachedJson(long mtime, JsonObject json) {}

    private static final class Part {
        private static final Part EMPTY = new Part(false, new double[7], new int[6]);

        private final boolean known;
        private final double[] scaling;
        private final int[] base;

        private Part(boolean known, double[] scaling, int[] base) {
            this.known = known;
            this.scaling = scaling;
            this.base = base;
        }

        private double scaling(String key) {
            int idx = scaleIndex(key);
            return idx < 0 ? 0.0d : scaling[idx];
        }

        private int base(String key) {
            int idx = baseIndex(key);
            return idx < 0 ? 0 : base[idx];
        }

        private static Part fromJson(JsonObject node) {
            double[] scaling = new double[7];
            int[] base = new int[6];
            JsonObject scale = node.getAsJsonObject("statScaling");
            JsonObject bases = node.getAsJsonObject("baseStats");
            scaling[0] = number(scale, "STR_scaling");
            scaling[1] = number(scale, "SKP_scaling");
            scaling[2] = number(scale, "STM_scaling");
            scaling[3] = number(scale, "DEF_scaling");
            scaling[4] = number(scale, "VIT_scaling");
            scaling[5] = number(scale, "PWR_scaling");
            scaling[6] = number(scale, "ENE_scaling");
            base[0] = integer(bases, "STR");
            base[1] = integer(bases, "SKP");
            base[2] = integer(bases, "RES");
            base[3] = integer(bases, "VIT");
            base[4] = integer(bases, "PWR");
            base[5] = integer(bases, "ENE");
            return new Part(true, scaling, base);
        }

        private static Part fromClassStats(RaceStatsConfig.ClassStats stats) {
            RaceStatsConfig.StatScaling scale = stats.getStatScaling();
            RaceStatsConfig.BaseStats bases = stats.getBaseStats();
            double[] scaling = new double[7];
            int[] base = new int[6];
            if (scale != null) {
                scaling[0] = boxed(scale.getStrengthScaling());
                scaling[1] = boxed(scale.getStrikePowerScaling());
                scaling[2] = boxed(scale.getStaminaScaling());
                scaling[3] = boxed(scale.getDefenseScaling());
                scaling[4] = boxed(scale.getVitalityScaling());
                scaling[5] = boxed(scale.getKiPowerScaling());
                scaling[6] = boxed(scale.getEnergyScaling());
            }
            if (bases != null) {
                base[0] = boxedInt(bases.getStrength());
                base[1] = boxedInt(bases.getStrikePower());
                base[2] = boxedInt(bases.getResistance());
                base[3] = boxedInt(bases.getVitality());
                base[4] = boxedInt(bases.getKiPower());
                base[5] = boxedInt(bases.getEnergy());
            }
            return new Part(true, scaling, base);
        }

        private static int scaleIndex(String key) {
            return switch (key) {
                case "STR" -> 0;
                case "SKP" -> 1;
                case "STM" -> 2;
                case "DEF" -> 3;
                case "VIT" -> 4;
                case "PWR" -> 5;
                case "ENE" -> 6;
                default -> -1;
            };
        }

        private static int baseIndex(String key) {
            return switch (key) {
                case "STR" -> 0;
                case "SKP" -> 1;
                case "RES" -> 2;
                case "VIT" -> 3;
                case "PWR" -> 4;
                case "ENE" -> 5;
                default -> -1;
            };
        }

        private static double number(JsonObject obj, String key) {
            if (obj == null || key == null || !obj.has(key) || obj.get(key).isJsonNull()) {
                return 0.0d;
            }
            try {
                return obj.get(key).getAsDouble();
            } catch (Throwable ignored) {
                return 0.0d;
            }
        }

        private static int integer(JsonObject obj, String key) {
            if (obj == null || key == null || !obj.has(key) || obj.get(key).isJsonNull()) {
                return 0;
            }
            try {
                return obj.get(key).getAsInt();
            } catch (Throwable ignored) {
                return 0;
            }
        }

        private static double boxed(Double value) {
            return value == null || !Double.isFinite(value) ? 0.0d : value;
        }

        private static int boxedInt(Integer value) {
            return value == null ? 0 : value;
        }
    }
}
