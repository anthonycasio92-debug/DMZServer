package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;

/**
 * Opens the companion Bukkit plugin chest GUI ({@code AdaptiveDifficultyGUI}).
 */
public final class BukkitGuiBridge {
    public static final String PLUGIN_NAME = CmiGuiBridge.PLUGIN_NAME;

    private BukkitGuiBridge() {}

    public static boolean available() {
        return CmiGuiBridge.available();
    }

    public static boolean open(ServerPlayer player, String page) {
        if (player == null || !available()) {
            return false;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        try {
            Object plugin = CmiGuiBridge.getCompanionPlugin();
            if (plugin == null) {
                return false;
            }
            if (CmiGuiBridge.invokeUuidOpen(plugin, "openChestMenuForUuid", player.m_20148_(), target)) {
                return true;
            }
            // Older companion jars — fall back to Player open.
            if (CmiGuiBridge.invokeUuidOpen(plugin, "openMenuForUuid", player.m_20148_(), target)) {
                return true;
            }
            Object bukkitPlayer = CmiGuiBridge.bukkitPlayer(player);
            if (bukkitPlayer == null) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] Chest GUI open skipped — bukkit player unresolved for {}",
                        AdaptiveDifficultyMod.MOD_ID, player.m_6302_()
                );
                return false;
            }
            Method open = CmiGuiBridge.findOpenMethod(plugin.getClass(), "openChestMenu");
            if (open == null) {
                open = CmiGuiBridge.findOpenMethod(plugin.getClass(), "openMenu");
            }
            if (open == null) {
                throw new NoSuchMethodException("openChestMenu/openMenu");
            }
            open.invoke(plugin, bukkitPlayer, target);
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
