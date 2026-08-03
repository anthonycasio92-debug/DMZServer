package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;

/**
 * Opens the companion Bukkit plugin chest GUI ({@code DMZAdaptiveDifficultyGUI}).
 */
public final class BukkitGuiBridge {
    public static final String PLUGIN_NAME = CmiGuiBridge.PLUGIN_NAME;

    private BukkitGuiBridge() {}

    public static boolean available() {
        return CmiGuiBridge.pluginEnabled(PLUGIN_NAME);
    }

    public static boolean open(ServerPlayer player, String page) {
        if (player == null || !available()) {
            return false;
        }
        try {
            Object plugin = CmiGuiBridge.getPlugin(PLUGIN_NAME);
            Object bukkitPlayer = CmiGuiBridge.bukkitPlayer(player);
            if (bukkitPlayer == null || plugin == null) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] Chest GUI open skipped — bukkit player unresolved for {}",
                        AdaptiveDifficultyMod.MOD_ID, player.m_7755_().getString()
                );
                return false;
            }
            Class<?> playerClass = Class.forName("org.bukkit.entity.Player");
            Method open;
            try {
                // Prefer chest-only when Forge selected guiBackend=chest.
                open = plugin.getClass().getMethod("openChestMenu", playerClass, String.class);
            } catch (NoSuchMethodException missing) {
                open = plugin.getClass().getMethod("openMenu", playerClass, String.class);
            }
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
