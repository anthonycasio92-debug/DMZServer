package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Opens the companion Bukkit plugin chest GUI ({@code DMZAdaptiveDifficultyGUI}).
 */
public final class BukkitGuiBridge {
    public static final String PLUGIN_NAME = "DMZAdaptiveDifficultyGUI";

    private BukkitGuiBridge() {}

    public static boolean available() {
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object pm = bukkit.getMethod("getPluginManager").invoke(null);
            Object plugin = pm.getClass().getMethod("getPlugin", String.class).invoke(pm, PLUGIN_NAME);
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
        if (player == null || !available()) {
            return false;
        }
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object pm = bukkit.getMethod("getPluginManager").invoke(null);
            Object plugin = pm.getClass().getMethod("getPlugin", String.class).invoke(pm, PLUGIN_NAME);
            Class<?> playerClass = Class.forName("org.bukkit.entity.Player");
            Object bukkitPlayer = bukkit.getMethod("getPlayer", UUID.class).invoke(null, player.m_20148_());
            if (bukkitPlayer == null) {
                return false;
            }
            Method open = plugin.getClass().getMethod("openMenu", playerClass, String.class);
            open.invoke(plugin, bukkitPlayer, page == null || page.isBlank() ? "main" : page);
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Bukkit chest GUI open failed: {}",
                    AdaptiveDifficultyMod.MOD_ID, t.toString()
            );
            return false;
        }
    }
}
