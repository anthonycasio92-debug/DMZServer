package com.dbzlegacy.adaptivedifficulty.progression;

import java.io.BufferedReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Best-effort Fabled skill level reads via Bukkit reflection, then YAML fallback.
 */
public final class FabledSkills {
    private FabledSkills() {}

    public static int skillLevel(ServerPlayer player, String skillName) {
        if (player == null || skillName == null || skillName.isBlank()) {
            return 0;
        }
        int viaApi = viaFabledApi(player, skillName);
        if (viaApi > 0) {
            return viaApi;
        }
        return viaYaml(player.m_20148_(), skillName);
    }

    private static int viaFabledApi(ServerPlayer player, String skillName) {
        try {
            Object bukkitPlayer = toBukkitPlayer(player);
            if (bukkitPlayer == null) {
                return 0;
            }
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object pm = bukkit.getMethod("getPluginManager").invoke(null);
            Object plugin = pm.getClass().getMethod("getPlugin", String.class).invoke(pm, "Fabled");
            if (plugin == null) {
                return 0;
            }
            Boolean enabled = (Boolean) plugin.getClass().getMethod("isEnabled").invoke(plugin);
            if (enabled == null || !enabled) {
                return 0;
            }
            ClassLoader loader = plugin.getClass().getClassLoader();
            Class<?> fabled = loader.loadClass("studio.magemonkey.fabled.Fabled");
            Method getData = null;
            for (Method m : fabled.getMethods()) {
                if ("getData".equals(m.getName()) && m.getParameterCount() == 1) {
                    getData = m;
                    break;
                }
            }
            if (getData == null) {
                return 0;
            }
            Object data = getData.invoke(null, bukkitPlayer);
            if (data == null) {
                return 0;
            }
            Object level = data.getClass().getMethod("getSkillLevel", String.class).invoke(data, skillName);
            if (level instanceof Number n) {
                return Math.max(0, n.intValue());
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static Object toBukkitPlayer(ServerPlayer player) {
        try {
            Method getBukkit = player.getClass().getMethod("getBukkitEntity");
            return getBukkit.invoke(player);
        } catch (Throwable ignored) {
        }
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object byUuid = bukkit.getMethod("getPlayer", UUID.class).invoke(null, player.m_20148_());
            if (byUuid != null) {
                return byUuid;
            }
            return bukkit.getMethod("getPlayerExact", String.class)
                    .invoke(null, player.m_7755_().getString());
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** YAML fallback matching Farming TP Skill.js path. */
    public static int viaYaml(UUID uuid, String skillName) {
        if (uuid == null || skillName == null || skillName.isBlank()) {
            return 0;
        }
        Path file = FMLPaths.GAMEDIR.get()
                .resolve("plugins")
                .resolve("Fabled")
                .resolve("players")
                .resolve(uuid.toString().toLowerCase(Locale.ROOT) + ".yml");
        if (!Files.isRegularFile(file)) {
            return 0;
        }
        String want = skillName.trim();
        boolean found = false;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.equals(want + ":") || trimmed.equalsIgnoreCase(want + ":")) {
                    found = true;
                    continue;
                }
                if (found) {
                    if (trimmed.startsWith("level:")) {
                        String num = trimmed.substring("level:".length()).trim();
                        return Math.max(0, Integer.parseInt(num));
                    }
                    if (!trimmed.isEmpty() && !trimmed.startsWith(" ") && trimmed.endsWith(":")) {
                        // next top-level key
                        found = false;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
