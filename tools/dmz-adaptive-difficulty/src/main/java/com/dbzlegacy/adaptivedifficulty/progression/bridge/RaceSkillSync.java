package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.progression.race.RaceLockConfig;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Race → Fabled skill half of RaceClass sync.
 *
 * <p>Discovers every DMZ race on disk ({@code config/dragonminez/races}) plus
 * Fabled classes in the {@code race} group, ensures a matching Race-type Fabled
 * skill exists, and grants it at level ≥ 1 while the player is that race.
 *
 * <p>Skills listed as {@code fabledSkill} in {@code race-lock.json} are
 * purchase gates — they are never auto-granted (Race Lock still enforces them).
 */
public final class RaceSkillSync {
    private static final String MANAGED_KEY = "dmz_race_skill_managed";
    private static final String NS_KEY = "legacymechanics";
    private static final String NS_PATH = "race-skill";
    private static final long ENSURE_INTERVAL_MS = 60_000L;

    private static volatile long lastEnsureMs;
    private static volatile String lastEnsureSummary = "";

    private RaceSkillSync() {}

    /** Ensure catalog + grant skill for the player's current DMZ race. */
    public static void sync(ServerPlayer player, String dmzRaceRaw) {
        if (player == null || !DifficultyConfig.get().enableRaceClassSync) {
            return;
        }
        ensureRaceSkills();
        String race = stripNamespace(dmzRaceRaw);
        if (race.isBlank()) {
            return;
        }
        String skillName = resolveSkillNameForRace(race);
        if (skillName == null || skillName.isBlank()) {
            return;
        }
        if (isPurchaseGatedSkill(skillName, race)) {
            return;
        }
        grantSkill(player, skillName, race);
    }

    /** Periodic / login ensure of skill stubs for every known race. */
    public static void ensureRaceSkills() {
        long now = System.currentTimeMillis();
        if (now - lastEnsureMs < ENSURE_INTERVAL_MS) {
            return;
        }
        lastEnsureMs = now;
        if (!FabledBridge.available()) {
            return;
        }
        Class<?> fabledClass = FabledBridge.fabledClass();
        if (fabledClass == null) {
            return;
        }

        Set<String> races = new LinkedHashSet<>();
        races.addAll(discoverDmzRaceIds());
        races.addAll(discoverFabledRaceClassKeys(fabledClass));

        int created = 0;
        int existing = 0;
        for (String race : races) {
            if (race == null || race.isBlank()) {
                continue;
            }
            String skillName = preferredSkillName(race, fabledClass);
            if (skillName.isBlank()) {
                continue;
            }
            Object skill = findSkill(fabledClass, skillName);
            if (skill != null) {
                existing++;
                continue;
            }
            if (registerRaceSkill(fabledClass, skillName, race)) {
                created++;
            }
        }
        lastEnsureSummary = "races=" + races.size() + " existing=" + existing + " created=" + created;
        if (created > 0) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] RaceClass skill ensure: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    lastEnsureSummary);
        }
    }

    public static String lastEnsureSummary() {
        return lastEnsureSummary == null ? "" : lastEnsureSummary;
    }

    private static void grantSkill(ServerPlayer player, String skillName, String race) {
        Object fabledData = FabledBridge.fabledData(player);
        if (fabledData == null) {
            return;
        }
        Class<?> fabledClass = FabledBridge.fabledClass();
        if (fabledClass == null) {
            return;
        }
        Object skill = findSkill(fabledClass, skillName);
        if (skill == null) {
            // One more ensure pass in case catalog raced login.
            lastEnsureMs = 0L;
            ensureRaceSkills();
            skill = findSkill(fabledClass, skillName);
            if (skill == null) {
                FabledBridge.logSync(player, "race_skill_miss", "race", race, "skill", skillName);
                return;
            }
        }

        int level = skillLevel(fabledData, skillName);
        if (level >= 1) {
            rememberManaged(player, skillName);
            return;
        }

        Object playerClass = null;
        try {
            playerClass = fabledData.getClass().getMethod("getMainClass").invoke(fabledData);
        } catch (Throwable ignored) {
        }

        boolean granted = false;
        // Prefer externally-added so class swaps do not wipe the race marker.
        granted = addSkillExternally(fabledData, skill, playerClass, 1);
        if (!granted) {
            granted = giveSkill(fabledData, skill, playerClass);
            if (granted) {
                forceUp(fabledData, skillName, 1);
            }
        }
        level = skillLevel(fabledData, skillName);
        if (level < 1) {
            forceUp(fabledData, skillName, 1);
            level = skillLevel(fabledData, skillName);
        }
        if (level >= 1) {
            clearPreviousManaged(player, fabledData, skillName);
            rememberManaged(player, skillName);
            FabledBridge.logSync(player, "race_skill", "race", race, "skill", skillName, "level", level);
        } else {
            FabledBridge.logSync(player, "race_skill_reject", "race", race, "skill", skillName);
        }
    }

    private static boolean addSkillExternally(
            Object fabledData, Object skill, Object playerClass, int level) {
        try {
            ClassLoader loader = fabledData.getClass().getClassLoader();
            Class<?> namespacedKey = Class.forName("org.bukkit.NamespacedKey", true, loader);
            Constructor<?> ctor = namespacedKey.getConstructor(String.class, String.class);
            Object key = ctor.newInstance(NS_KEY, NS_PATH);
            for (Method m : fabledData.getClass().getMethods()) {
                if (!"addSkillExternally".equals(m.getName()) || m.getParameterCount() != 4) {
                    continue;
                }
                m.invoke(fabledData, skill, playerClass, key, level);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean giveSkill(Object fabledData, Object skill, Object playerClass) {
        try {
            for (Method m : fabledData.getClass().getMethods()) {
                if (!"giveSkill".equals(m.getName())) {
                    continue;
                }
                if (m.getParameterCount() == 2 && playerClass != null) {
                    m.invoke(fabledData, skill, playerClass);
                    return true;
                }
                if (m.getParameterCount() == 1) {
                    m.invoke(fabledData, skill);
                    return true;
                }
            }
            if (playerClass != null) {
                for (Method m : fabledData.getClass().getMethods()) {
                    if ("giveSkill".equals(m.getName()) && m.getParameterCount() == 2) {
                        m.invoke(fabledData, skill, playerClass);
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void forceUp(Object fabledData, String skillName, int targetLevel) {
        try {
            Object playerSkill = fabledData.getClass()
                    .getMethod("getSkill", String.class)
                    .invoke(fabledData, skillName);
            if (playerSkill == null) {
                return;
            }
            int current = 0;
            Object lv = playerSkill.getClass().getMethod("getLevel").invoke(playerSkill);
            if (lv instanceof Number n) {
                current = n.intValue();
            }
            if (current >= targetLevel) {
                return;
            }
            int delta = targetLevel - Math.max(0, current);
            try {
                fabledData.getClass()
                        .getMethod("forceUpSkill", playerSkill.getClass(), int.class)
                        .invoke(fabledData, playerSkill, delta);
            } catch (NoSuchMethodException e) {
                fabledData.getClass()
                        .getMethod("forceUpSkill", playerSkill.getClass())
                        .invoke(fabledData, playerSkill);
            }
            // Last resort: PlayerSkill.setLevel
            try {
                playerSkill.getClass().getMethod("setLevel", int.class).invoke(playerSkill, targetLevel);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
    }

    private static int skillLevel(Object fabledData, String skillName) {
        try {
            Object v = fabledData.getClass()
                    .getMethod("getSkillLevel", String.class)
                    .invoke(fabledData, skillName);
            if (v instanceof Number n) {
                return Math.max(0, n.intValue());
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static void rememberManaged(ServerPlayer player, String skillName) {
        ProgressionData.storedPut(player, MANAGED_KEY, skillName);
    }

    private static void clearPreviousManaged(ServerPlayer player, Object fabledData, String keep) {
        String prev = ProgressionData.storedGet(player, MANAGED_KEY, "");
        if (prev == null || prev.isBlank() || equalsIgnoreCase(prev, keep)) {
            return;
        }
        if (isPurchaseGatedSkill(prev, "")) {
            return;
        }
        try {
            Object skill = null;
            Class<?> fabledClass = FabledBridge.fabledClass();
            if (fabledClass != null) {
                skill = findSkill(fabledClass, prev);
            }
            if (skill == null) {
                return;
            }
            ClassLoader loader = fabledData.getClass().getClassLoader();
            Class<?> namespacedKey = Class.forName("org.bukkit.NamespacedKey", true, loader);
            Object key = namespacedKey.getConstructor(String.class, String.class)
                    .newInstance(NS_KEY, NS_PATH);
            for (Method m : fabledData.getClass().getMethods()) {
                if ("removeSkillExternally".equals(m.getName()) && m.getParameterCount() == 2) {
                    m.invoke(fabledData, skill, key);
                    break;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Purchase-gated skills (Race Lock) must never be auto-granted — players buy
     * them from the Prestige tree.
     */
    private static boolean isPurchaseGatedSkill(String skillName, String raceId) {
        try {
            for (RaceLockConfig.RestrictedRace entry : RaceLockConfig.restricted()) {
                if (entry == null) {
                    continue;
                }
                if (entry.fabledSkill != null && equalsIgnoreCase(entry.fabledSkill, skillName)) {
                    return true;
                }
                if (raceId != null
                        && !raceId.isBlank()
                        && entry.id != null
                        && equalsIgnoreCase(entry.id, raceId)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        // Hard safety for prestige race unlocks even if race-lock.json is empty.
        String n = normalize(skillName);
        return n.equals("ancientsaiyan") || n.equals("sentosaiyan");
    }

    private static String resolveSkillNameForRace(String race) {
        Class<?> fabledClass = FabledBridge.fabledClass();
        if (fabledClass == null) {
            return preferredSkillName(race, null);
        }
        return preferredSkillName(race, fabledClass);
    }

    private static String preferredSkillName(String race, Class<?> fabledClass) {
        String title = titleCaseId(race);
        List<String> candidates = new ArrayList<>();
        // Prefer live Fabled class name for this race when present.
        if (fabledClass != null) {
            Object cls = resolveRaceClass(fabledClass, race);
            if (cls != null) {
                try {
                    Object n = cls.getClass().getMethod("getName").invoke(cls);
                    if (n != null && !String.valueOf(n).isBlank()) {
                        candidates.add(String.valueOf(n).trim());
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        candidates.add(title);
        candidates.add(race);
        candidates.add(race.replace('_', ' '));
        candidates.add(formatRaceName(race));
        if (fabledClass != null) {
            for (String c : candidates) {
                if (c == null || c.isBlank()) {
                    continue;
                }
                if (findSkill(fabledClass, c) != null) {
                    return c;
                }
            }
        }
        return title.isBlank() ? race : title;
    }

    private static Object resolveRaceClass(Class<?> fabledClass, String race) {
        Method getClassMethod = FabledBridge.findUnaryStatic(fabledClass, "getClass");
        String[] candidates = new String[] {
                race,
                titleCaseId(race),
                formatRaceName(race),
                race.toLowerCase(Locale.ROOT),
                race.toUpperCase(Locale.ROOT),
                race.replace('_', ' '),
                titleCaseId(race.replace('_', ' '))
        };
        if (getClassMethod != null) {
            for (String c : candidates) {
                if (c == null || c.isBlank()) {
                    continue;
                }
                try {
                    Object found = getClassMethod.invoke(null, c);
                    if (found != null) {
                        return found;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        try {
            Method getClasses = FabledBridge.findNoArg(fabledClass, "getClasses");
            if (getClasses == null) {
                return null;
            }
            Object registered = getClasses.invoke(null);
            if (!(registered instanceof Map<?, ?> map)) {
                return null;
            }
            for (Map.Entry<?, ?> e : map.entrySet()) {
                Object registeredClass = e.getValue();
                if (registeredClass == null) {
                    continue;
                }
                String name = "";
                try {
                    Object n = registeredClass.getClass().getMethod("getName").invoke(registeredClass);
                    name = n == null ? "" : String.valueOf(n);
                } catch (Throwable ignored) {
                }
                if (normalize(name).equals(normalize(race))
                        || (e.getKey() != null && normalize(String.valueOf(e.getKey())).equals(normalize(race)))) {
                    return registeredClass;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object findSkill(Class<?> fabledClass, String skillName) {
        if (fabledClass == null || skillName == null || skillName.isBlank()) {
            return null;
        }
        Method getSkill = FabledBridge.findUnaryStatic(fabledClass, "getSkill");
        if (getSkill != null) {
            String[] candidates = new String[] {
                    skillName,
                    skillName.toLowerCase(Locale.ROOT),
                    skillName.toUpperCase(Locale.ROOT),
                    titleCaseId(skillName)
            };
            for (String c : candidates) {
                try {
                    Object found = getSkill.invoke(null, c);
                    if (found != null) {
                        return found;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        try {
            Method getSkills = FabledBridge.findNoArg(fabledClass, "getSkills");
            if (getSkills == null) {
                return null;
            }
            Object map = getSkills.invoke(null);
            if (!(map instanceof Map<?, ?> m)) {
                return null;
            }
            String wanted = normalize(skillName);
            for (Map.Entry<?, ?> e : m.entrySet()) {
                Object skill = e.getValue();
                if (skill == null) {
                    continue;
                }
                String name = "";
                String key = e.getKey() == null ? "" : String.valueOf(e.getKey());
                try {
                    Object n = skill.getClass().getMethod("getName").invoke(skill);
                    name = n == null ? "" : String.valueOf(n);
                } catch (Throwable ignored) {
                }
                if (normalize(name).equals(wanted) || normalize(key).equals(wanted)) {
                    return skill;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean registerRaceSkill(Class<?> fabledClass, String skillName, String raceId) {
        try {
            writeSkillYaml(skillName, raceId);
            ClassLoader loader = fabledClass.getClassLoader();
            Class<?> dynamicSkill = Class.forName(
                    "studio.magemonkey.fabled.dynamic.DynamicSkill", true, loader);
            Object skill = dynamicSkill.getConstructor(String.class).newInstance(skillName);
            // Prefer type Race when the field is writable (YAML reload also sets it).
            setSkillType(skill, "Race");
            Object plugin = FabledBridge.getPlugin(FabledBridge.PLUGIN_NAME);
            if (plugin == null) {
                return false;
            }
            boolean added = false;
            try {
                plugin.getClass().getMethod("addDynamicSkill", dynamicSkill).invoke(plugin, skill);
                added = true;
            } catch (Throwable ignored) {
            }
            if (!added) {
                try {
                    Class<?> skillCl = Class.forName(
                            "studio.magemonkey.fabled.api.skills.Skill", true, loader);
                    plugin.getClass().getMethod("addSkill", skillCl).invoke(plugin, skill);
                    added = true;
                } catch (Throwable ignored) {
                }
            }
            if (!added) {
                Method addSkills = null;
                for (Method m : plugin.getClass().getMethods()) {
                    if ("addSkills".equals(m.getName()) && m.isVarArgs()) {
                        addSkills = m;
                        break;
                    }
                }
                if (addSkills != null) {
                    Object arr = java.lang.reflect.Array.newInstance(skill.getClass().getSuperclass(), 1);
                    java.lang.reflect.Array.set(arr, 0, skill);
                    addSkills.invoke(plugin, arr);
                    added = true;
                }
            }
            // Attach to matching race class skill list when possible.
            Object raceClass = resolveRaceClass(fabledClass, raceId);
            if (raceClass != null) {
                try {
                    raceClass.getClass().getMethod("addSkill", String.class).invoke(raceClass, skillName);
                } catch (Throwable ignored) {
                }
            }
            return added || findSkill(fabledClass, skillName) != null;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] Race skill register failed for {}: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    skillName,
                    t.toString());
            return false;
        }
    }

    private static void setSkillType(Object skill, String type) {
        Class<?> search = skill.getClass();
        while (search != null) {
            try {
                Field f = search.getDeclaredField("type");
                f.setAccessible(true);
                f.set(skill, type);
                return;
            } catch (NoSuchFieldException e) {
                search = search.getSuperclass();
            } catch (Throwable ignored) {
                return;
            }
        }
    }

    private static void writeSkillYaml(String skillName, String raceId) {
        try {
            Path dir = FMLPaths.GAMEDIR.get()
                    .resolve("plugins")
                    .resolve("Fabled")
                    .resolve("dynamic")
                    .resolve("skill");
            Files.createDirectories(dir);
            Path file = dir.resolve(skillName + ".yml");
            if (Files.isRegularFile(file)) {
                return;
            }
            String display = skillName;
            String body = ""
                    + "'" + escapeYaml(skillName) + "':\n"
                    + "  name: '" + escapeYaml(display) + "'\n"
                    + "  type: 'Race'\n"
                    + "  'max-level': 1\n"
                    + "  'skill-req-lvl': 0\n"
                    + "  'needs-permission': 'false'\n"
                    + "  'cooldown-message': 'true'\n"
                    + "  incompatible: []\n"
                    + "  attributes:\n"
                    + "    'level-base': '1'\n"
                    + "    'level-scale': '0'\n"
                    + "    'cost-base': '0'\n"
                    + "    'cost-scale': '0'\n"
                    + "    'cooldown-base': '0'\n"
                    + "    'cooldown-scale': '0'\n"
                    + "    'mana-base': '0'\n"
                    + "    'mana-scale': '0'\n"
                    + "    'points-spent-req-base': '0'\n"
                    + "    'points-spent-req-scale': '0'\n"
                    + "  msg: '&6{player} &2has cast &6{skill}'\n"
                    + "  icon: 'pumpkin'\n"
                    + "  'icon-data': 0\n"
                    + "  'icon-durability': 0\n"
                    + "  'icon-lore':\n"
                    + "  - '&d{name} &7({level}/{max})'\n"
                    + "  - '&2Type: &6{type}'\n"
                    + "  - ''\n"
                    + "  - 'Auto race skill for " + escapeYaml(raceId) + "'\n"
                    + "  - '&7Granted by LegacyMechanics RaceClass sync.'\n"
                    + "  desc: []\n"
                    + "  components: {}\n";
            Files.writeString(file, body, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] Race skill YAML write failed for {}: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    skillName,
                    t.toString());
        }
    }

    private static String escapeYaml(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("'", "''");
    }

    private static Set<String> discoverDmzRaceIds() {
        Set<String> out = new LinkedHashSet<>();
        Path dir = FMLPaths.GAMEDIR.get().resolve("config").resolve("dragonminez").resolve("races");
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path child : stream) {
                if (Files.isDirectory(child)) {
                    String name = child.getFileName().toString().trim();
                    if (!name.isBlank() && !name.startsWith(".")) {
                        out.add(name.toLowerCase(Locale.ROOT));
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static Set<String> discoverFabledRaceClassKeys(Class<?> fabledClass) {
        Set<String> out = new LinkedHashSet<>();
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
                if (!"race".equalsIgnoreCase(group.trim())) {
                    continue;
                }
                try {
                    Object n = registeredClass.getClass().getMethod("getName").invoke(registeredClass);
                    if (n != null && !String.valueOf(n).isBlank()) {
                        out.add(String.valueOf(n).trim());
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static String stripNamespace(String raw) {
        if (raw == null) {
            return "";
        }
        String r = raw.trim();
        int colon = r.lastIndexOf(':');
        if (colon >= 0 && colon < r.length() - 1) {
            r = r.substring(colon + 1).trim();
        }
        return r;
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

    private static String formatRaceName(String race) {
        String r = race == null ? "" : race.trim().toLowerCase(Locale.ROOT);
        if (r.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(r.charAt(0)) + r.substring(1);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\u00A7.", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }
}
