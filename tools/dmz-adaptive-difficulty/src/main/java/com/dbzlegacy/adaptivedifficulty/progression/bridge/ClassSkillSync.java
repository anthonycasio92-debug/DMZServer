package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.classdef.FightingClassCatalog;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Fighting class → Fabled skill stubs (port of race auto-provision for DMZ classes).
 *
 * <p>Discovers every class from {@link FightingClassCatalog}, ensures matching
 * Fabled base + Prestige skill YAML exists ({@code needs-permission: true} on base),
 * and registers at runtime when missing. Does <em>not</em> auto-grant levels — LP
 * via {@link ClassPermissionSync} unlocks the base skill when the player picks the class.
 */
public final class ClassSkillSync {
    private static final String PRESTIGE_SUFFIX = " Prestige";
    private static final long ENSURE_INTERVAL_MS = 60_000L;

    private static volatile long lastEnsureMs;
    private static volatile String lastEnsureSummary = "";

    private ClassSkillSync() {}

    /** Periodic / login ensure of skill stubs for every known fighting class. */
    public static void ensureClassSkills() {
        long now = System.currentTimeMillis();
        if (now - lastEnsureMs < ENSURE_INTERVAL_MS) {
            return;
        }
        lastEnsureMs = now;
        if (!DifficultyConfig.get().enableClassPermissionSync || !FabledBridge.available()) {
            return;
        }
        Class<?> fabledClass = FabledBridge.fabledClass();
        if (fabledClass == null) {
            return;
        }

        int created = 0;
        int existing = 0;
        int classes = 0;
        for (FightingClassCatalog.ClassEntry entry : FightingClassCatalog.entries()) {
            if (entry == null || entry.id == null || entry.id.isBlank()) {
                continue;
            }
            classes++;
            String base = entry.fabledSkill == null || entry.fabledSkill.isBlank()
                    ? FightingClassCatalog.skillNameFor(entry.id)
                    : entry.fabledSkill.trim();
            if (base.isBlank()) {
                continue;
            }
            if (findSkill(fabledClass, base) != null) {
                existing++;
            } else if (registerClassSkill(fabledClass, base, entry.id, false)) {
                created++;
            }
            String prestige = entry.prestigeSkill == null || entry.prestigeSkill.isBlank()
                    ? base + PRESTIGE_SUFFIX
                    : entry.prestigeSkill.trim();
            if (prestige.isBlank()) {
                continue;
            }
            if (findSkill(fabledClass, prestige) != null) {
                existing++;
            } else if (registerClassSkill(fabledClass, prestige, entry.id, true)) {
                created++;
            }
        }
        lastEnsureSummary = "classes=" + classes + " existing=" + existing + " created=" + created;
        if (created > 0) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Class Fabled skill ensure: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    lastEnsureSummary);
        }
    }

    public static String lastEnsureSummary() {
        return lastEnsureSummary == null ? "" : lastEnsureSummary;
    }

    private static boolean registerClassSkill(
            Class<?> fabledClass, String skillName, String classId, boolean prestige) {
        try {
            writeSkillYaml(skillName, classId, prestige);
            ClassLoader loader = fabledClass.getClassLoader();
            Class<?> dynamicSkill = Class.forName(
                    "studio.magemonkey.fabled.dynamic.DynamicSkill", true, loader);
            Object skill = dynamicSkill.getConstructor(String.class).newInstance(skillName);
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
            attachToFabledClass(fabledClass, skillName, classId);
            return added || findSkill(fabledClass, skillName) != null;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] Class skill register failed for {}: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    skillName,
                    t.toString());
            return false;
        }
    }

    private static void attachToFabledClass(Class<?> fabledClass, String skillName, String classId) {
        Object fightingClass = resolveFightingClass(fabledClass, classId, skillName);
        if (fightingClass == null) {
            return;
        }
        try {
            fightingClass.getClass().getMethod("addSkill", String.class).invoke(fightingClass, skillName);
        } catch (Throwable ignored) {
        }
    }

    private static Object resolveFightingClass(Class<?> fabledClass, String classId, String skillName) {
        Method getClassMethod = FabledBridge.findUnaryStatic(fabledClass, "getClass");
        String[] candidates = new String[] {
                skillName,
                FightingClassCatalog.skillNameFor(classId),
                classId,
                titleCaseId(classId),
                classId.replace('_', ' ')
        };
        if (getClassMethod != null) {
            for (String c : candidates) {
                if (c == null || c.isBlank()) {
                    continue;
                }
                try {
                    Object found = getClassMethod.invoke(null, c);
                    if (found != null && !isRaceOrPrestigeGroup(found)) {
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
            String wanted = normalize(classId);
            String wantedSkill = normalize(skillName);
            for (Map.Entry<?, ?> e : map.entrySet()) {
                Object registeredClass = e.getValue();
                if (registeredClass == null || isRaceOrPrestigeGroup(registeredClass)) {
                    continue;
                }
                String name = "";
                try {
                    Object n = registeredClass.getClass().getMethod("getName").invoke(registeredClass);
                    name = n == null ? "" : String.valueOf(n);
                } catch (Throwable ignored) {
                }
                if (normalize(name).equals(wanted)
                        || normalize(name).equals(wantedSkill)
                        || (e.getKey() != null && normalize(String.valueOf(e.getKey())).equals(wanted))) {
                    return registeredClass;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isRaceOrPrestigeGroup(Object registeredClass) {
        try {
            Object g = registeredClass.getClass().getMethod("getGroup").invoke(registeredClass);
            String group = g == null ? "" : String.valueOf(g).trim();
            return "race".equalsIgnoreCase(group) || "prestige".equalsIgnoreCase(group);
        } catch (Throwable ignored) {
            return false;
        }
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

    private static void writeSkillYaml(String skillName, String classId, boolean prestige) {
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
            String perm = FightingClassCatalog.permissionForSkillName(skillName);
            String body;
            if (prestige) {
                body = ""
                        + "'" + escapeYaml(skillName) + "':\n"
                        + "  name: '" + escapeYaml(skillName) + "'\n"
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
                        + "  msg: '&6{player} &2unlocked &6{skill}'\n"
                        + "  icon: 'nether_star'\n"
                        + "  'icon-data': 0\n"
                        + "  'icon-durability': 0\n"
                        + "  'icon-lore':\n"
                        + "  - '&d{name} &7({level}/{max})'\n"
                        + "  - '&2Type: &6Prestige'\n"
                        + "  - ''\n"
                        + "  - 'Prestige marker for " + escapeYaml(classId) + "'\n"
                        + "  - '&7Locks base class LP via LegacyMechanics.'\n"
                        + "  desc: []\n"
                        + "  components: {}\n";
            } else {
                body = ""
                        + "'" + escapeYaml(skillName) + "':\n"
                        + "  name: '" + escapeYaml(skillName) + "'\n"
                        + "  'max-level': 1\n"
                        + "  'skill-req-lvl': 0\n"
                        + "  'needs-permission': 'true'\n"
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
                        + "  icon: 'iron_sword'\n"
                        + "  'icon-data': 0\n"
                        + "  'icon-durability': 0\n"
                        + "  'icon-lore':\n"
                        + "  - '&d{name} &7({level}/{max})'\n"
                        + "  - '&2Type: &6Class'\n"
                        + "  - ''\n"
                        + "  - 'DMZ class " + escapeYaml(classId) + "'\n"
                        + "  - '&7LP: &f" + escapeYaml(perm) + "'\n"
                        + "  - '&7Granted by LegacyMechanics ClassPermission sync.'\n"
                        + "  desc: []\n"
                        + "  components: {}\n";
            }
            Files.writeString(file, body, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] Class skill YAML write failed for {}: {}",
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

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\u00A7.", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
