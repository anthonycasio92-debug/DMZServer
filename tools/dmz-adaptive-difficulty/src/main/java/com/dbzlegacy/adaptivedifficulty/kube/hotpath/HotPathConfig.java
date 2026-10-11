package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.ConfigPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code config/legacymechanics/hot-path.json}.
 * Staff edit this from the Combat tab in {@code /lm admin config}.
 * Weapon damage overrides and the base-damage table are the maps under Weapon damage.
 */
public final class HotPathConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile HotPathConfig INSTANCE = freshDefaults();

    public WeaponDamage weaponDamage = new WeaponDamage();
    public WeaponBonus weaponBonus = new WeaponBonus();
    public Healing healing = new Healing();
    public Pothala pothala = new Pothala();
    public Spawners spawners = new Spawners();
    public Capsules capsules = new Capsules();
    public Heartstop heartstop = new Heartstop();
    public SilkTouch silkTouch = new SilkTouch();
    public Silky silky = new Silky();
    public RaceLocks raceLocks = new RaceLocks();

    public HotPathConfig() {}

    public static HotPathConfig get() {
        return INSTANCE;
    }

    public static HotPathConfig freshDefaults() {
        HotPathConfig config = new HotPathConfig();
        config.weaponDamage.baseDamage = WeaponBaseDamage.copyDefaults();
        return config;
    }

    public static Path path() {
        return ConfigPaths.dataDir().resolve("hot-path.json");
    }

    public static void load() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            if (!Files.isRegularFile(file)) {
                HotPathConfig fresh = freshDefaults();
                try (Writer writer = Files.newBufferedWriter(file)) {
                    GSON.toJson(fresh, writer);
                }
            }
            HotPathConfig parsed;
            try (Reader reader = Files.newBufferedReader(file)) {
                parsed = GSON.fromJson(reader, HotPathConfig.class);
            }
            INSTANCE = sanitize(parsed == null ? freshDefaults() : parsed);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] hot-path config load failed: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
            INSTANCE = freshDefaults();
        }
    }

    public static boolean save() {
        INSTANCE = sanitize(INSTANCE == null ? freshDefaults() : INSTANCE);
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(INSTANCE, writer);
            }
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] hot-path config save failed: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
            return false;
        }
    }

    public static int ticks(int value) {
        return value < 1 ? 1 : value;
    }

    private static HotPathConfig sanitize(HotPathConfig config) {
        if (config.weaponDamage == null) {
            config.weaponDamage = new WeaponDamage();
        }
        if (config.weaponBonus == null) {
            config.weaponBonus = new WeaponBonus();
        }
        if (config.healing == null) {
            config.healing = new Healing();
        }
        if (config.pothala == null) {
            config.pothala = new Pothala();
        }
        if (config.spawners == null) {
            config.spawners = new Spawners();
        }
        if (config.capsules == null) {
            config.capsules = new Capsules();
        }
        if (config.heartstop == null) {
            config.heartstop = new Heartstop();
        }
        if (config.silkTouch == null) {
            config.silkTouch = new SilkTouch();
        }
        if (config.silky == null) {
            config.silky = new Silky();
        }
        if (config.raceLocks == null) {
            config.raceLocks = new RaceLocks();
        }
        WeaponDamage damage = config.weaponDamage;
        if (!Double.isFinite(damage.defaultMultiplier)) {
            damage.defaultMultiplier = 1.0;
        }
        damage.baseDamage = damage.baseDamage == null
                ? WeaponBaseDamage.copyDefaults()
                : finiteMap(damage.baseDamage);
        damage.overrides = finiteMap(damage.overrides);
        WeaponBonus bonus = config.weaponBonus;
        bonus.updateInterval = ticks(bonus.updateInterval);
        if (!Double.isFinite(bonus.uniqueBase) || bonus.uniqueBase < 0.0) {
            bonus.uniqueBase = 1.80;
        }
        if (!Double.isFinite(bonus.uniqueThird) || bonus.uniqueThird < 0.0) {
            bonus.uniqueThird = 1.35;
        }
        if (!Double.isFinite(bonus.thirdStat) || bonus.thirdStat < 0.0) {
            bonus.thirdStat = 0.12;
        }
        if (!Double.isFinite(bonus.damageFloor)) {
            bonus.damageFloor = 0.75;
        }
        if (!Double.isFinite(bonus.damageDivisor) || bonus.damageDivisor == 0.0) {
            bonus.damageDivisor = 18.0;
        }
        Healing healing = config.healing;
        healing.tickInterval = ticks(healing.tickInterval);
        healing.lifeStealMax = finite(healing.lifeStealMax, 0.0);
        healing.overhealMax = finite(healing.overhealMax, 0.05);
        healing.healingReceivedMax = finite(healing.healingReceivedMax, 1.05);
        config.pothala.checkInterval = ticks(config.pothala.checkInterval);
        Spawners spawners = config.spawners;
        spawners.scanInterval = ticks(spawners.scanInterval);
        if (spawners.chunkRadius < 0) {
            spawners.chunkRadius = 0;
        } else if (spawners.chunkRadius > 16) {
            spawners.chunkRadius = 16;
        }
        spawners.minSpawnDelay = ticks(spawners.minSpawnDelay);
        spawners.maxSpawnDelay = ticks(spawners.maxSpawnDelay);
        spawners.spawnCount = ticks(spawners.spawnCount);
        spawners.maxNearby = ticks(spawners.maxNearby);
        spawners.requiredPlayerRange = ticks(spawners.requiredPlayerRange);
        spawners.spawnRange = ticks(spawners.spawnRange);
        config.capsules.purgeInterval = ticks(config.capsules.purgeInterval);
        config.heartstop.interval = ticks(config.heartstop.interval);
        if (config.heartstop.spellId == null || config.heartstop.spellId.isBlank()) {
            config.heartstop.spellId = "irons_spellbooks:heartstop";
        } else {
            config.heartstop.spellId = config.heartstop.spellId.trim();
        }
        config.silkTouch.scanInterval = ticks(config.silkTouch.scanInterval);
        config.silky.clothInterval = ticks(config.silky.clothInterval);
        config.silky.toolInterval = ticks(config.silky.toolInterval);
        config.raceLocks.syncInterval = ticks(config.raceLocks.syncInterval);
        return config;
    }

    private static double finite(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static Map<String, Double> finiteMap(Map<String, Double> raw) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        for (Map.Entry<String, Double> entry : raw.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            double value = entry.getValue();
            if (Double.isFinite(value)) {
                out.put(entry.getKey().trim(), value);
            }
        }
        return out;
    }

    /** Multipliers for stat scaling. Exact item ids beat {@code family:<name>}. */
    public static final class WeaponDamage {
        public boolean enabled = true;
        /** 1.0 leaves the weapon alone. 0.5 is half, 2.0 is double, 0 zeroes the scaling figure. */
        public double defaultMultiplier = 1.0;
        public Map<String, Double> overrides = new LinkedHashMap<>();
        /** Main-hand attack-damage modifier. Player base is 1, so 89 is 90 damage. */
        public Map<String, Double> baseDamage = new LinkedHashMap<>();

        public double multiplier(String itemId, String family) {
            if (!enabled) {
                return 1.0;
            }
            if (itemId != null && !itemId.isEmpty()) {
                Double exact = overrides.get(itemId);
                if (exact != null && Double.isFinite(exact)) {
                    return exact;
                }
            }
            if (family != null && !family.isEmpty()) {
                Double familyMultiplier = overrides.get("family:" + family);
                if (familyMultiplier != null && Double.isFinite(familyMultiplier)) {
                    return familyMultiplier;
                }
            }
            return Double.isFinite(defaultMultiplier) ? defaultMultiplier : 1.0;
        }

        public Double baseOf(String itemId) {
            if (!enabled || itemId == null) {
                return null;
            }
            Double value = baseDamage.get(itemId);
            return value != null && Double.isFinite(value) ? value : null;
        }
    }

    /** How held weapons turn attack damage into stat bonuses. */
    public static final class WeaponBonus {
        public boolean enabled = true;
        public int updateInterval = 20;
        public double uniqueBase = 1.80;
        public double uniqueThird = 1.35;
        public double thirdStat = 0.12;
        public double damageFloor = 0.75;
        public double damageDivisor = 18.0;
    }

    /** Life steal, overheal, and healing received caps. */
    public static final class Healing {
        public boolean enabled = true;
        public int tickInterval = 20;
        public double lifeStealMax = 0.0;
        public double overhealMax = 0.05;
        public double healingReceivedMax = 1.05;
    }

    public static final class Pothala {
        public boolean enabled = true;
        public int checkInterval = 20;
    }

    /** Puts Apotheosis spawners back on these vanilla numbers. Redstone control stays. */
    public static final class Spawners {
        public boolean enabled = true;
        public int scanInterval = 200;
        public int chunkRadius = 3;
        public int minSpawnDelay = 200;
        public int maxSpawnDelay = 800;
        public int spawnCount = 4;
        public int maxNearby = 6;
        public int requiredPlayerRange = 16;
        public int spawnRange = 4;
    }

    public static final class Capsules {
        public boolean enabled = true;
        public int purgeInterval = 100;
    }

    public static final class Heartstop {
        public boolean enabled = true;
        public int interval = 40;
        public String spellId = "irons_spellbooks:heartstop";
    }

    public static final class SilkTouch {
        public boolean enabled = true;
        public int scanInterval = 100;
    }

    public static final class Silky {
        public boolean enabled = true;
        public int clothInterval = 20;
        public int toolInterval = 100;
    }

    public static final class RaceLocks {
        public boolean enabled = true;
        public int syncInterval = 200;
    }
}
