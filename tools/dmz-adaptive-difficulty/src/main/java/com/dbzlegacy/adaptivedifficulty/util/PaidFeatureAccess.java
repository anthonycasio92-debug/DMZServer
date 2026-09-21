package com.dbzlegacy.adaptivedifficulty.util;

import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesConfig;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ancient Coin (paid) features: staff/OP bypass is gated by
 * {@link DifficultyConfig#staffFreeAncientCoinCosts} (default off).
 * Non-op players with explicit bypass permission nodes always skip cost.
 */
public final class PaidFeatureAccess {
    private PaidFeatureAccess() {}

    public static boolean bypassAncientCoinCost(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (hasExplicitBypassPermission(player, CharacterServicesConfig.get().permissions.bypassCost)
                || hasExplicitBypassPermission(player, CharacterServicesConfig.get().permissions.admin)) {
            return true;
        }
        if (!DifficultyConfig.get().staffFreeAncientCoinCosts) {
            return false;
        }
        return StaffAccess.isStaff(player);
    }

    /**
     * Bukkit {@code isOp()} implies every custom permission — do not treat that as
     * {@code legacymechanics.character.bypass.*} unless the node is explicitly granted to a non-op.
     */
    private static boolean hasExplicitBypassPermission(ServerPlayer player, String node) {
        if (node == null || node.isBlank()) {
            return false;
        }
        try {
            Object bukkit = player.getClass().getMethod("getBukkitEntity").invoke(player);
            if (bukkit != null) {
                try {
                    Object isOp = bukkit.getClass().getMethod("isOp").invoke(bukkit);
                    if (isOp instanceof Boolean b && b) {
                        return false;
                    }
                } catch (Throwable ignored) {
                }
                Object ok = bukkit.getClass().getMethod("hasPermission", String.class).invoke(bukkit, node);
                return ok instanceof Boolean b && b;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
