package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Opens the companion plugin's CMILib/CMI inventory GUI. */
public final class CmiGuiBridge {
    /** Bukkit plugin.yml {@code name} for LegacyMechanicsGUI 2.0+. */
    public static final String PLUGIN_NAME = "LegacyMechanicsGUI";
    /** Pre-2.0 companion plugin name — kept as a lookup fallback. */
    public static final String PREVIOUS_PLUGIN_NAME = "AdaptiveDifficultyGUI";
    /** Pre-1.0 companion plugin name — kept as a lookup fallback. */
    public static final String LEGACY_PLUGIN_NAME = "DMZAdaptiveDifficultyGUI";

    private CmiGuiBridge() {}

    public static boolean available() {
        // Only require the companion plugin. It chooses CMI vs chest itself —
        // requiring CMILib here caused Forge to skip the plugin and dump to chat
        // when the Mohist plugin lookup for CMILib was flaky.
        try {
            Object plugin = getCompanionPlugin();
            if (plugin == null) {
                return false;
            }
            Object enabled = plugin.getClass().getMethod("isEnabled").invoke(plugin);
            return enabled instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean open(ServerPlayer player, String page) {
        return openNamed(player, page, "openMenuForUuid", "openMenu", "CMI Difficulty");
    }

    /** Opens Rival CMI/inventory GUI via companion plugin. */
    public static boolean openRival(ServerPlayer player, String page) {
        return openNamed(player, page, "openRivalMenuForUuid", "openRivalMenu", "CMI Rival");
    }

    /** Opens Sparring CMI/inventory GUI via companion plugin. */
    public static boolean openSpar(ServerPlayer player, String page) {
        return openNamed(player, page, "openSparMenuForUuid", "openSparMenu", "CMI Spar");
    }

    /** Opens Legacy Mechanics hub CMI/inventory GUI via companion plugin. */
    public static boolean openHub(ServerPlayer player, String page) {
        return openNamed(player, page, "openHubMenuForUuid", "openHubMenu", "CMI Hub");
    }

    /** Opens Progression CMI/inventory GUI via companion plugin. */
    public static boolean openProgression(ServerPlayer player, String page) {
        return openNamed(player, page, "openProgressionMenuForUuid", "openProgressionMenu", "CMI Progression");
    }

    /** Opens Prestige CMI/inventory GUI via companion plugin. */
    public static boolean openPrestige(ServerPlayer player, String page) {
        return openNamed(player, page, "openPrestigeMenuForUuid", "openPrestigeMenu", "CMI Prestige");
    }

    /** Opens Skills CMI/inventory GUI via companion plugin. */
    public static boolean openSkills(ServerPlayer player, String page) {
        return openNamed(player, page, "openSkillsMenuForUuid", "openSkillsMenu", "CMI Skills");
    }

    /** Staff inspect: open companion chest GUI as admin while editing subject (Difficulty). */
    public static boolean openInspect(ServerPlayer admin, ServerPlayer subject, String page) {
        return openLmInspect(admin, subject, "difficulty", page);
    }

    /**
     * Staff inspect any LM system (hub/difficulty/rival/spar/…).
     * Reflects {@code openLmInspectForUuid} on the companion plugin.
     */
    public static boolean openLmInspect(
            ServerPlayer admin, ServerPlayer subject, String system, String page) {
        if (admin == null || subject == null || !available()) {
            return false;
        }
        String sys = system == null || system.isBlank() ? "hub" : system;
        String target = page == null || page.isBlank() ? "main" : page;
        try {
            Object plugin = getCompanionPlugin();
            if (plugin == null) {
                return false;
            }
            try {
                Method m = plugin.getClass().getMethod(
                        "openLmInspectForUuid", UUID.class, UUID.class, String.class, String.class);
                m.invoke(plugin, admin.m_20148_(), subject.m_20148_(), sys, target);
                return true;
            } catch (NoSuchMethodException missingNew) {
                // Older GUI jar — Difficulty-only inspect.
                Method m = plugin.getClass().getMethod(
                        "openInspectForUuid", UUID.class, UUID.class, String.class);
                m.invoke(plugin, admin.m_20148_(), subject.m_20148_(), target);
                return true;
            }
        } catch (NoSuchMethodException missing) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] openLmInspectForUuid missing — update LegacyMechanicsGUI jar",
                    AdaptiveDifficultyMod.MOD_ID
            );
            return false;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] openLmInspect failed: {}",
                    AdaptiveDifficultyMod.MOD_ID, t.toString()
            );
            return false;
        }
    }

    /** Clears staff inspect session via companion plugin (no-op if missing). */
    public static boolean clearLmInspect(UUID adminId) {
        if (adminId == null || !available()) {
            return false;
        }
        try {
            Object plugin = getCompanionPlugin();
            if (plugin == null) {
                return false;
            }
            plugin.getClass().getMethod("clearInspectForUuid", UUID.class).invoke(plugin, adminId);
            return true;
        } catch (NoSuchMethodException missing) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] clearInspectForUuid missing — update LegacyMechanicsGUI jar",
                    AdaptiveDifficultyMod.MOD_ID
            );
            return false;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] clearLmInspect failed: {}",
                    AdaptiveDifficultyMod.MOD_ID, t.toString()
            );
            return false;
        }
    }

    private static boolean openNamed(
            ServerPlayer player,
            String page,
            String uuidMethod,
            String playerMethod,
            String label
    ) {
        if (player == null || !available()) {
            return false;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        try {
            Object plugin = getCompanionPlugin();
            if (plugin == null) {
                return false;
            }
            // Prefer UUID entry — avoids Mohist Player classloader mismatch and
            // schedules onto the Bukkit primary thread inside the plugin.
            if (invokeUuidOpen(plugin, uuidMethod, player.m_20148_(), target)) {
                return true;
            }
            Object bukkitPlayer = bukkitPlayer(player);
            if (bukkitPlayer == null) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] {} GUI open skipped — bukkit player unresolved for {}",
                        AdaptiveDifficultyMod.MOD_ID, label, player.m_6302_()
                );
                return false;
            }
            Method open = findOpenMethod(plugin.getClass(), playerMethod);
            if (open == null) {
                throw new NoSuchMethodException(playerMethod + "(Player,String) / " + uuidMethod);
            }
            open.invoke(plugin, bukkitPlayer, target);
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] {} GUI open failed: {}",
                    AdaptiveDifficultyMod.MOD_ID, label, t.toString()
            );
            return false;
        }
    }

    static boolean invokeUuidOpen(Object plugin, String methodName, UUID id, String page) {
        if (plugin == null || methodName == null || id == null) {
            return false;
        }
        try {
            Method m = plugin.getClass().getMethod(methodName, UUID.class, String.class);
            m.invoke(plugin, id, page == null || page.isBlank() ? "main" : page);
            return true;
        } catch (NoSuchMethodException missing) {
            return false;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] {} failed: {}",
                    AdaptiveDifficultyMod.MOD_ID, methodName, t.toString()
            );
            return false;
        }
    }

    static boolean pluginEnabled(String name) {
        try {
            Object plugin = getPlugin(name);
            if (plugin == null) {
                return false;
            }
            Object enabled = plugin.getClass().getMethod("isEnabled").invoke(plugin);
            return enabled instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static Object getPlugin(String name) throws Exception {
        Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
        Object pm = bukkit.getMethod("getPluginManager").invoke(null);
        return pm.getClass().getMethod("getPlugin", String.class).invoke(pm, name);
    }

    /** Resolve LegacyMechanicsGUI, then older companion plugin names. */
    static Object getCompanionPlugin() throws Exception {
        Object plugin = getPlugin(PLUGIN_NAME);
        if (plugin != null) {
            return plugin;
        }
        plugin = getPlugin(PREVIOUS_PLUGIN_NAME);
        if (plugin != null) {
            return plugin;
        }
        return getPlugin(LEGACY_PLUGIN_NAME);
    }

    /**
     * Find {@code name(*, String)} by arity/name only — do not check Player assignability
     * across Mohist classloaders (that always fails and previously broke opens).
     */
    static Method findOpenMethod(Class<?> pluginClass, String name) {
        if (pluginClass == null || name == null) {
            return null;
        }
        Method named = null;
        for (Method m : pluginClass.getMethods()) {
            if (!name.equals(m.getName()) || m.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params[1] != String.class) {
                continue;
            }
            // Prefer exact Bukkit Player parameter when present.
            if ("org.bukkit.entity.Player".equals(params[0].getName())) {
                return m;
            }
            if (named == null) {
                named = m;
            }
        }
        return named;
    }

    /**
     * Mohist-safe Bukkit player resolve: prefer NMS {@code getBukkitEntity()},
     * then UUID / name lookups via Bukkit from the companion plugin's classloader.
     */
    static Object bukkitPlayer(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        try {
            Method getBukkit = player.getClass().getMethod("getBukkitEntity");
            Object bp = getBukkit.invoke(player);
            if (bp != null) {
                return bp;
            }
        } catch (Throwable ignored) {
        }
        UUID id = player.m_20148_();
        // Prefer plugin CL so the Player type matches openMenu's parameter.
        try {
            Object plugin = getCompanionPlugin();
            if (plugin != null) {
                ClassLoader pcl = plugin.getClass().getClassLoader();
                Class<?> bukkit = Class.forName("org.bukkit.Bukkit", true, pcl);
                Object byId = bukkit.getMethod("getPlayer", UUID.class).invoke(null, id);
                if (byId != null) {
                    return byId;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object byId = bukkit.getMethod("getPlayer", UUID.class).invoke(null, id);
            if (byId != null) {
                return byId;
            }
            String name = null;
            try {
                name = player.m_6302_();
            } catch (Throwable ignored) {
            }
            if (name == null || name.isBlank()) {
                name = player.m_7755_().getString();
            }
            if (name != null && !name.isBlank()) {
                Object byName = bukkit.getMethod("getPlayerExact", String.class).invoke(null, name);
                if (byName != null) {
                    return byName;
                }
                return bukkit.getMethod("getPlayer", String.class).invoke(null, name);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
