package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Opens the companion plugin's CMILib/CMI inventory GUI. */
public final class CmiGuiBridge {
    public static final String PLUGIN_NAME = "DMZAdaptiveDifficultyGUI";

    private CmiGuiBridge() {}

    public static boolean available() {
        // Only require the companion plugin. It chooses CMI vs chest itself —
        // requiring CMILib here caused Forge to skip the plugin and dump to chat
        // when the Mohist plugin lookup for CMILib was flaky.
        return pluginEnabled(PLUGIN_NAME);
    }

    public static boolean open(ServerPlayer player, String page) {
        if (player == null || !available()) {
            return false;
        }
        try {
            Object plugin = getPlugin(PLUGIN_NAME);
            Object bukkitPlayer = bukkitPlayer(player);
            if (bukkitPlayer == null || plugin == null) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] CMI GUI open skipped — bukkit player unresolved for {}",
                        AdaptiveDifficultyMod.MOD_ID, player.m_7755_().getString()
                );
                return false;
            }
            // Resolve by name — avoid Class.forName(Player) CL mismatch on Mohist.
            Method open = findOpenMethod(plugin.getClass(), "openMenu", bukkitPlayer.getClass());
            if (open == null) {
                throw new NoSuchMethodException("openMenu(Player,String)");
            }
            open.invoke(plugin, bukkitPlayer, page == null || page.isBlank() ? "main" : page);
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] CMI GUI open failed: {}",
                    AdaptiveDifficultyMod.MOD_ID, t.toString()
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

    /** Find {@code name(PlayerLike, String)} without loading Player via Forge CL. */
    static Method findOpenMethod(Class<?> pluginClass, String name, Class<?> playerRuntimeClass) {
        if (pluginClass == null || name == null) {
            return null;
        }
        for (Method m : pluginClass.getMethods()) {
            if (!name.equals(m.getName()) || m.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params[1] != String.class) {
                continue;
            }
            if (playerRuntimeClass != null && params[0].isAssignableFrom(playerRuntimeClass)) {
                return m;
            }
            // Fallback: first arg looks like a Bukkit Player interface/class.
            String pn = params[0].getName();
            if ("org.bukkit.entity.Player".equals(pn) || pn.endsWith(".Player")) {
                return m;
            }
        }
        return null;
    }

    /**
     * Mohist-safe Bukkit player resolve: prefer NMS {@code getBukkitEntity()},
     * then UUID / name lookups.
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
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            UUID id = player.m_20148_();
            Object byId = bukkit.getMethod("getPlayer", UUID.class).invoke(null, id);
            if (byId != null) {
                return byId;
            }
            // Prefer scoreboard/login name over display Component (nicknames break lookup).
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
