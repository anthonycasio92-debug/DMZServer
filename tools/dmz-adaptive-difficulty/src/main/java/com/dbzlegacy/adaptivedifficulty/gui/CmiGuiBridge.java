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
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object pm = bukkit.getMethod("getPluginManager").invoke(null);
            Object plugin = pm.getClass().getMethod("getPlugin", String.class).invoke(pm, PLUGIN_NAME);
            if (plugin == null) {
                return false;
            }
            Object enabled = plugin.getClass().getMethod("isEnabled").invoke(plugin);
            if (!(enabled instanceof Boolean b) || !b) {
                return false;
            }
            // Require real CMILib / CMI — companion plugin alone is not enough.
            Object cmiLib = pm.getClass().getMethod("getPlugin", String.class).invoke(pm, "CMILib");
            Object cmi = pm.getClass().getMethod("getPlugin", String.class).invoke(pm, "CMI");
            return cmiLib != null || cmi != null;
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
            if (bukkitPlayer == null || plugin == null) {
                return false;
            }
            // Honor Forge guiBackend (chest/chat/cmi) instead of always preferring CMI.
            Method open;
            try {
                open = plugin.getClass().getMethod("openMenuRespectingConfig", playerClass, String.class);
            } catch (NoSuchMethodException missing) {
                open = plugin.getClass().getMethod("openMenu", playerClass, String.class);
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
}
