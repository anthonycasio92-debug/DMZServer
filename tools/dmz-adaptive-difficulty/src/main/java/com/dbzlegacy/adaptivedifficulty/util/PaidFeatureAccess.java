package com.dbzlegacy.adaptivedifficulty.util;

import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesConfig;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ancient Coin (paid) features: staff/OP bypass is gated by
 * {@link DifficultyConfig#staffFreeAncientCoinCosts} (default off).
 * Explicit permission nodes always bypass cost.
 */
public final class PaidFeatureAccess {
    private PaidFeatureAccess() {}

    public static boolean bypassAncientCoinCost(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (hasCharacterBypassPermission(player)) {
            return true;
        }
        if (!DifficultyConfig.get().staffFreeAncientCoinCosts) {
            return false;
        }
        return StaffAccess.isStaff(player);
    }

    private static boolean hasCharacterBypassPermission(ServerPlayer player) {
        return hasBukkitPermission(player, CharacterServicesConfig.get().permissions.bypassCost)
                || hasBukkitPermission(player, CharacterServicesConfig.get().permissions.admin);
    }

    private static boolean hasBukkitPermission(ServerPlayer player, String node) {
        if (node == null || node.isBlank()) {
            return false;
        }
        try {
            Object bukkit = player.getClass().getMethod("getBukkitEntity").invoke(player);
            if (bukkit != null) {
                Object ok = bukkit.getClass().getMethod("hasPermission", String.class).invoke(bukkit, node);
                if (ok instanceof Boolean b) {
                    return b;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Object ok = player.getClass().getMethod("hasPermission", String.class).invoke(player, node);
            return ok instanceof Boolean b && b;
        } catch (Throwable ignored) {
        }
        return false;
    }
}
