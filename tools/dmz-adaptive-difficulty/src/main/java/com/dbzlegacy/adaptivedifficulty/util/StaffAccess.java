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
        // Mohist: Bukkit OP often does not map to Forge permission level 2.
        if (hasBukkitIsOp(player)) {
            return true;
        }
        String node = DifficultyConfig.get().adminPermission;
        if (node == null || node.isBlank()) {
            node = "difficulty.admin";
        }
        return hasBukkitPermission(player, node);
    }

    /**
     * Skill Check (donator) access: configured {@code skillCheckPermission}
     * (default {@code legacymechanics.skillcheck}). Staff are <b>not</b> auto-granted —
     * without the node they use {@code /skills} (admin) and do not see Skill Check in the hub.
     */
    public static boolean hasSkillCheck(ServerPlayer player) {
        if (player == null) {
            return false;
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

    private static boolean hasBukkitIsOp(ServerPlayer player) {
        Object bukkit = bukkitEntity(player);
        if (bukkit == null) {
            return false;
        }
        try {
            Object result = bukkit.getClass().getMethod("isOp").invoke(bukkit);
            return result instanceof Boolean b && b;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean hasBukkitPermission(ServerPlayer player, String node) {
        if (player == null || node == null || node.isBlank()) {
            return false;
        }
        Object bukkit = bukkitEntity(player);
        if (bukkit == null) {
            return false;
        }
        try {
            Object result = bukkit.getClass().getMethod("hasPermission", String.class).invoke(bukkit, node);
            return result instanceof Boolean b && b;
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** Mohist/CraftBukkit bridge — NMS {@code ServerPlayer} often lacks {@code hasPermission} directly. */
    private static Object bukkitEntity(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        try {
            return player.getClass().getMethod("getBukkitEntity").invoke(player);
        } catch (Throwable ignored) {
        }
        return player;
    }
}
