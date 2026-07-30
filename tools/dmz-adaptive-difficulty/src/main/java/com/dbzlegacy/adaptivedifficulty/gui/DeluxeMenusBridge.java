package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;

/**
 * Opens HelpChat DeluxeMenus GUIs via console {@code dm open <menu> <player>}.
 * Works without granting players {@code deluxemenus.open}.
 */
public final class DeluxeMenusBridge {
    private DeluxeMenusBridge() {}

    public static boolean available() {
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object pm = bukkit.getMethod("getPluginManager").invoke(null);
            Object plugin = pm.getClass().getMethod("getPlugin", String.class).invoke(pm, "DeluxeMenus");
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
        String menu = menuName(page);
        String name = player.m_6302_();
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object console = bukkit.getMethod("getConsoleSender").invoke(null);
            Method dispatch = bukkit.getMethod("dispatchCommand",
                    Class.forName("org.bukkit.command.CommandSender"), String.class);
            boolean ok = Boolean.TRUE.equals(dispatch.invoke(null, console, "dm open " + menu + " " + name));
            if (ok) {
                AdaptiveDifficultyMod.LOGGER.debug(
                        "[{}] opened DeluxeMenus {} for {}",
                        AdaptiveDifficultyMod.MOD_ID, menu, name
                );
            }
            return ok;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] DeluxeMenus open failed: {}",
                    AdaptiveDifficultyMod.MOD_ID, t.toString()
            );
            return false;
        }
    }

    public static String menuName(String page) {
        if (page == null || page.isBlank()) {
            return "dmz_difficulty";
        }
        return switch (page.trim().toLowerCase()) {
            case "rewards" -> "dmz_difficulty_rewards";
            case "tiers", "enemies" -> "dmz_difficulty_tiers";
            case "stats", "statistics" -> "dmz_difficulty_stats";
            case "settings" -> "dmz_difficulty"; // admin settings stay chat-only
            default -> "dmz_difficulty";
        };
    }
}
