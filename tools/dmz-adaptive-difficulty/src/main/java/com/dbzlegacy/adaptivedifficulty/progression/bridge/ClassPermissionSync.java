package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.progression.classdef.FightingClassCatalog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of {@code DMZ Class Permission.js} (v3.3.0) —
 * DMZ fighting class → LuckPerms {@code fabled.skill.*} grants (reflection only).
 */
public final class ClassPermissionSync {
    private static final String PRESTIGE_SUFFIX = " Prestige";
    private static final String PERM_ROOT = "fabled.skill.";
    private static final long CLASS_CONFIRM_MS = 5000L;
    private static final String SYNC_VERSION = "3.4.0";

    private static final String MANAGED_KEY = "dmz_fabled_class_permissions_v3_managed";
    private static final String LOCKED_KEY = "dmz_fabled_class_permissions_v3_prestige_locked";
    private static final String LAST_SYNCED_KEY = "dmz_fabled_class_permissions_v3_last_synced_class";
    private static final String SYNC_VERSION_KEY = "dmz_fabled_class_permissions_v3_sync_version";
    private static final String LAST_SEEN_KEY = "dmz_fabled_class_permissions_v3_last_seen_class";
    private static final String STABLE_SINCE_KEY = "dmz_fabled_class_permissions_v3_class_stable_since";

    private ClassPermissionSync() {}

    public static void clearTemp(UUID id) {
        // Temp keys cleared with ProgressionData.clearPlayer
    }

    /**
     * Paid Character Services / admin class changes — skip the 5s UI debounce so Fabled class
     * permissions apply immediately (recustomize {@code UpdateCharacterC2S} has no debounce).
     */
    public static void syncAuthoritativeClassChange(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableClassPermissionSync) {
            return;
        }
        String dmzClass = DmzProgression.fightingClass(player);
        if (dmzClass == null || dmzClass.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        ProgressionData.tempPut(player, LAST_SEEN_KEY, dmzClass);
        ProgressionData.tempPut(player, STABLE_SINCE_KEY, now - CLASS_CONFIRM_MS);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableClassPermissionSync) {
            return;
        }
        ClassSkillSync.ensureClassSkills();
        ensureSyncVersion(player);

        String dmzClass = DmzProgression.fightingClass(player);
        if (dmzClass == null || dmzClass.isBlank()) {
            return;
        }
        // Prefer original casing from character when available.
        try {
            var ch = DmzProgression.character(player);
            if (ch != null) {
                String raw = ch.getCharacterClass();
                if (raw != null && !raw.isBlank()) {
                    dmzClass = raw.trim();
                }
            }
        } catch (Throwable ignored) {
        }

        if (needsClassChangeConfirm(player, dmzClass) && !isDmzClassConfirmed(player, dmzClass)) {
            return;
        }

        Object bukkitPlayer = FabledBridge.bukkitPlayer(player);
        if (bukkitPlayer == null) {
            return;
        }

        Object fabledData = FabledBridge.fabledData(player);
        List<Object> skills = loadSkills();

        String currentSkillName = resolveSkillName(dmzClass, skills);
        String currentPermission = skillPermission(currentSkillName);

        List<String> previousManaged = readList(player, MANAGED_KEY);
        List<String> lockedPrestige = readList(player, LOCKED_KEY);
        List<String> updatedManaged = new ArrayList<>();
        boolean lockedChanged = false;

        for (String oldSkillName : previousManaged) {
            if (isLocked(lockedPrestige, oldSkillName)) {
                continue;
            }
            if (!currentSkillName.isBlank()
                    && normalize(oldSkillName).equals(normalize(currentSkillName))) {
                continue;
            }
            if (fabledData != null && prestigeLevel(fabledData, oldSkillName, skills) >= 1) {
                if (lockPrestige(player, bukkitPlayer, oldSkillName, lockedPrestige)) {
                    lockedChanged = true;
                }
                continue;
            }
            luckPerms(player, bukkitPlayer, "unset", skillPermission(oldSkillName));
        }

        if (!currentSkillName.isBlank() && !currentPermission.isBlank()) {
            if (!isLocked(lockedPrestige, currentSkillName)) {
                int prestige = fabledData == null ? 0 : prestigeLevel(fabledData, currentSkillName, skills);
                if (prestige >= 1) {
                    if (lockPrestige(player, bukkitPlayer, currentSkillName, lockedPrestige)) {
                        lockedChanged = true;
                    }
                } else {
                    boolean wasManaged = contains(previousManaged, currentSkillName);
                    String lastSynced = ProgressionData.storedGet(player, LAST_SYNCED_KEY, "");
                    boolean classAlreadySynced = normalize(lastSynced).equals(normalize(dmzClass));
                    boolean hasPerm = hasPermission(bukkitPlayer, currentPermission);
                    if (!wasManaged || !hasPerm || !classAlreadySynced) {
                        if (luckPerms(player, bukkitPlayer, "set", currentPermission)) {
                            FabledBridge.logSync(player, "class_perm_grant", "perm", currentPermission, "class", dmzClass);
                        }
                    }
                    addUnique(updatedManaged, currentSkillName);
                }
            }
        }

        writeList(player, MANAGED_KEY, updatedManaged);
        if (lockedChanged) {
            writeList(player, LOCKED_KEY, lockedPrestige);
        }
        if (!currentSkillName.isBlank()) {
            ProgressionData.storedPut(player, LAST_SYNCED_KEY, dmzClass);
        }
    }

    private static void ensureSyncVersion(ServerPlayer player) {
        String current = ProgressionData.storedGet(player, SYNC_VERSION_KEY, "");
        if (SYNC_VERSION.equals(current)) {
            return;
        }
        ProgressionData.storedPut(player, LAST_SYNCED_KEY, "");
        writeList(player, MANAGED_KEY, List.of());
        ProgressionData.storedPut(player, SYNC_VERSION_KEY, SYNC_VERSION);
    }

    private static boolean needsClassChangeConfirm(ServerPlayer player, String dmzClass) {
        String lastSynced = ProgressionData.storedGet(player, LAST_SYNCED_KEY, "");
        if (lastSynced.isBlank()) {
            return false;
        }
        return !normalize(lastSynced).equals(normalize(dmzClass));
    }

    private static boolean isDmzClassConfirmed(ServerPlayer player, String dmzClass) {
        long now = System.currentTimeMillis();
        String lastSeen = ProgressionData.tempGet(player, LAST_SEEN_KEY, "");
        if (!normalize(lastSeen).equals(normalize(dmzClass))) {
            ProgressionData.tempPut(player, LAST_SEEN_KEY, dmzClass);
            ProgressionData.tempPut(player, STABLE_SINCE_KEY, now);
            return false;
        }
        long since = ProgressionData.tempGetLong(player, STABLE_SINCE_KEY, 0L);
        if (since <= 0L) {
            ProgressionData.tempPut(player, STABLE_SINCE_KEY, now);
            return false;
        }
        return now - since >= CLASS_CONFIRM_MS;
    }

    private static String resolveSkillName(String dmzClass, List<Object> skills) {
        Object found = findBaseSkill(skills, dmzClass);
        if (found != null) {
            try {
                Object n = found.getClass().getMethod("getName").invoke(found);
                if (n != null && !String.valueOf(n).isBlank()) {
                    return String.valueOf(n);
                }
            } catch (Throwable ignored) {
            }
        }
        return FightingClassCatalog.skillNameFor(dmzClass);
    }

    private static String skillPermission(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            return "";
        }
        return PERM_ROOT + skillName.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    @SuppressWarnings("unchecked")
    private static List<Object> loadSkills() {
        List<Object> out = new ArrayList<>();
        try {
            Class<?> fabled = FabledBridge.fabledClass();
            if (fabled == null) {
                return out;
            }
            Object map = fabled.getMethod("getSkills").invoke(null);
            if (!(map instanceof Map<?, ?> m)) {
                return out;
            }
            Collection<?> values = m.values();
            out.addAll((Collection<Object>) (Collection<?>) values);
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static Object findBaseSkill(List<Object> skills, String dmzClass) {
        String wanted = normalize(dmzClass);
        if (wanted.isBlank() || skills == null) {
            return null;
        }
        for (Object skill : skills) {
            if (skill == null) {
                continue;
            }
            String name = "";
            String key = "";
            try {
                Object n = skill.getClass().getMethod("getName").invoke(skill);
                name = n == null ? "" : String.valueOf(n);
            } catch (Throwable ignored) {
            }
            if (endsWithIgnoreCase(name, PRESTIGE_SUFFIX)) {
                continue;
            }
            try {
                Object k = skill.getClass().getMethod("getKey").invoke(skill);
                key = k == null ? "" : String.valueOf(k);
            } catch (Throwable ignored) {
            }
            if (normalize(name).equals(wanted) || normalize(key).equals(wanted)) {
                return skill;
            }
        }
        return null;
    }

    private static int prestigeLevel(Object fabledData, String baseSkillName, List<Object> skills) {
        if (fabledData == null || baseSkillName == null || baseSkillName.isBlank()) {
            return 0;
        }
        int level = trySkillLevel(fabledData, baseSkillName + PRESTIGE_SUFFIX);
        if (level >= 1) {
            return level;
        }
        level = trySkillLevel(fabledData, (baseSkillName + PRESTIGE_SUFFIX).toLowerCase(Locale.ROOT));
        if (level >= 1) {
            return level;
        }
        String wanted = normalize(baseSkillName + PRESTIGE_SUFFIX);
        for (Object skill : skills) {
            if (skill == null) {
                continue;
            }
            String name = "";
            try {
                Object n = skill.getClass().getMethod("getName").invoke(skill);
                name = n == null ? "" : String.valueOf(n);
            } catch (Throwable ignored) {
            }
            if (!endsWithIgnoreCase(name, PRESTIGE_SUFFIX)) {
                continue;
            }
            if (normalize(name).equals(wanted)) {
                return trySkillLevel(fabledData, name);
            }
        }
        return 0;
    }

    private static int trySkillLevel(Object fabledData, String label) {
        try {
            Object v = fabledData.getClass().getMethod("getSkillLevel", String.class).invoke(fabledData, label);
            if (v instanceof Number n) {
                return Math.max(0, n.intValue());
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static boolean lockPrestige(
            ServerPlayer player,
            Object bukkitPlayer,
            String skillName,
            List<String> locked
    ) {
        if (skillName == null || skillName.isBlank() || isLocked(locked, skillName)) {
            return false;
        }
        String perm = skillPermission(skillName);
        if (!hasPermission(bukkitPlayer, perm)) {
            luckPerms(player, bukkitPlayer, "set", perm);
        }
        addUnique(locked, skillName);
        return true;
    }

    private static boolean luckPerms(ServerPlayer player, Object bukkitPlayer, String op, String permission) {
        if (permission == null || permission.isBlank()) {
            return false;
        }
        if (tryLuckPermsApi(player, op, permission)) {
            return true;
        }
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object console = bukkit.getMethod("getConsoleSender").invoke(null);
            String uuid = player.m_20148_().toString();
            String name = player.m_6302_();
            String[] targets = {uuid, name};
            String[] prefixes = {"lp", "luckperms"};
            for (String prefix : prefixes) {
                for (String target : targets) {
                    if (target == null || target.isBlank()) {
                        continue;
                    }
                    String cmd = prefix + " user " + target + " permission " + op + " " + permission;
                    if ("set".equals(op)) {
                        cmd = cmd + " true";
                    }
                    Object ok = bukkit.getMethod("dispatchCommand",
                                    Class.forName("org.bukkit.command.CommandSender"),
                                    String.class)
                            .invoke(null, console, cmd);
                    if (ok instanceof Boolean b && b) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean tryLuckPermsApi(ServerPlayer player, String op, String permission) {
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object lp = provider.getMethod("get").invoke(null);
            if (lp == null) {
                return false;
            }
            Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
            Object user = userManager.getClass()
                    .getMethod("getUser", UUID.class)
                    .invoke(userManager, player.m_20148_());
            if (user == null) {
                return false;
            }
            Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
            Object builder = nodeClass.getMethod("builder", String.class).invoke(null, permission);
            Object withValue = builder.getClass().getMethod("value", boolean.class).invoke(builder, true);
            Object node = withValue.getClass().getMethod("build").invoke(withValue);
            Object dataContainer = user.getClass().getMethod("data").invoke(user);
            boolean mutated = false;
            for (var m : dataContainer.getClass().getMethods()) {
                if (m.getParameterCount() != 1) {
                    continue;
                }
                if ("set".equals(op) && "add".equals(m.getName())) {
                    m.invoke(dataContainer, node);
                    mutated = true;
                    break;
                }
                if ("unset".equals(op) && "remove".equals(m.getName())) {
                    m.invoke(dataContainer, node);
                    mutated = true;
                    break;
                }
            }
            if (!mutated) {
                return false;
            }
            for (var m : userManager.getClass().getMethods()) {
                if ("saveUser".equals(m.getName()) && m.getParameterCount() == 1) {
                    m.invoke(userManager, user);
                    return true;
                }
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasPermission(Object bukkitPlayer, String permission) {
        try {
            Object v = bukkitPlayer.getClass().getMethod("hasPermission", String.class)
                    .invoke(bukkitPlayer, permission);
            return v instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<String> readList(ServerPlayer player, String key) {
        String raw = ProgressionData.storedGet(player, key, "");
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split("\\|")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                addUnique(out, t);
            }
        }
        return out;
    }

    private static void writeList(ServerPlayer player, String key, List<String> values) {
        if (values == null || values.isEmpty()) {
            ProgressionData.storedRemove(player, key);
            return;
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                unique.add(v.trim());
            }
        }
        ProgressionData.storedPut(player, key, String.join("|", unique));
    }

    private static boolean isLocked(List<String> locked, String skillName) {
        return contains(locked, skillName);
    }

    private static boolean contains(List<String> list, String value) {
        if (list == null) {
            return false;
        }
        String wanted = normalize(value);
        for (String e : list) {
            if (normalize(e).equals(wanted)) {
                return true;
            }
        }
        return false;
    }

    private static void addUnique(List<String> list, String value) {
        if (value == null || value.isBlank() || contains(list, value)) {
            return;
        }
        list.add(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String text = value.replaceAll("\u00A7.", "").toLowerCase(Locale.ROOT);
        return text.replaceAll("[^a-z0-9]", "");
    }

    private static boolean endsWithIgnoreCase(String text, String suffix) {
        if (text == null || suffix == null) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT));
    }
}
