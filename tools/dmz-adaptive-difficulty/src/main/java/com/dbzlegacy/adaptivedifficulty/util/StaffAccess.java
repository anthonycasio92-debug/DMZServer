package com.dbzlegacy.adaptivedifficulty.util;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared staff check: op level 2 or {@code difficulty.admin} (Mohist/Bukkit hasPermission).
 */
public final class StaffAccess {
    private StaffAccess() {}

    public static boolean isStaff(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        try {
            if (player.m_20310_(2)) { // permission level / op
                return true;
            }
        } catch (Throwable ignored) {
        }
        String node = DifficultyConfig.get().adminPermission;
        if (node == null || node.isBlank()) {
            node = "difficulty.admin";
        }
        try {
            var method = player.getClass().getMethod("hasPermission", String.class);
            Object result = method.invoke(player, node);
            if (result instanceof Boolean b && b) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
