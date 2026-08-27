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
        return openNamed(
                player,
                page,
                "openChestMenuForUuid",
                "openMenuForUuid",
                "openChestMenu",
                "openMenu",
                "Difficulty"
        );
    }

    /** Chest-only Rival open for {@code guiBackend=chest}. */
    public static boolean openRival(ServerPlayer player, String page) {
        return openNamed(
                player,
                page,
                "openRivalChestMenuForUuid",
                "openRivalMenuForUuid",
                "openRivalChestMenu",
                "openRivalMenu",
                "Rival"
        );
    }

    /** Chest-only Spar open for {@code guiBackend=chest}. */
    public static boolean openSpar(ServerPlayer player, String page) {
        return openNamed(
                player,
                page,
                "openSparChestMenuForUuid",
                "openSparMenuForUuid",
                "openSparChestMenu",
                "openSparMenu",
                "Spar"
        );
    }

    private static boolean openNamed(
            ServerPlayer player,
            String page,
            String chestUuidMethod,
            String fallbackUuidMethod,
            String chestPlayerMethod,
            String fallbackPlayerMethod,
            String label
    ) {
        if (player == null || !available()) {
            return false;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        try {
            Object plugin = CmiGuiBridge.getCompanionPlugin();
            if (plugin == null) {
                return false;
            }
            if (CmiGuiBridge.invokeUuidOpen(plugin, chestUuidMethod, player.m_20148_(), target)) {
                return true;
            }
            // Older companion jars — fall back to inventory UUID open.
            if (CmiGuiBridge.invokeUuidOpen(plugin, fallbackUuidMethod, player.m_20148_(), target)) {
                return true;
            }
            Object bukkitPlayer = CmiGuiBridge.bukkitPlayer(player);
            if (bukkitPlayer == null) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] {} chest GUI open skipped — bukkit player unresolved for {}",
                        AdaptiveDifficultyMod.MOD_ID, label, player.m_6302_()
                );
                return false;
            }
            Method open = CmiGuiBridge.findOpenMethod(plugin.getClass(), chestPlayerMethod);
            if (open == null) {
                open = CmiGuiBridge.findOpenMethod(plugin.getClass(), fallbackPlayerMethod);
            }
            if (open == null) {
                throw new NoSuchMethodException(chestPlayerMethod + "/" + fallbackPlayerMethod);
            }
            open.invoke(plugin, bukkitPlayer, target);
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Bukkit {} chest GUI open failed: {}",
                    AdaptiveDifficultyMod.MOD_ID, label, t.toString()
            );
            return false;
        }
    }
}
