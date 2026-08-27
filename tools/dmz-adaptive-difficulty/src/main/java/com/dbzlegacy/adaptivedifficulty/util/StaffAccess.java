package com.dbzlegacy.adaptivedifficulty.util;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared access checks: staff (op level 2 / {@code difficulty.admin}) and SkillCheck donators.
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
        return hasBukkitPermission(player, node);
    }

    /**
     * Skill Check (donator) access: staff always, or configured {@code skillCheckPermission}
     * (default {@code legacymechanics.skillcheck}).
     */
    public static boolean hasSkillCheck(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (isStaff(player)) {
            return true;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableSkillCheck || !cfg.enableSkillUnlockService) {
            return false;
        }
        String node = cfg.skillCheckPermission;
        if (node == null || node.isBlank()) {
            node = "legacymechanics.skillcheck";
        }
        return hasBukkitPermission(player, node);
    }

    private static boolean hasBukkitPermission(ServerPlayer player, String node) {
        if (player == null || node == null || node.isBlank()) {
            return false;
        }
        try {
            var method = player.getClass().getMethod("hasPermission", String.class);
            Object result = method.invoke(player, node);
            return result instanceof Boolean b && b;
        } catch (Throwable ignored) {
        }
        return false;
    }
}
