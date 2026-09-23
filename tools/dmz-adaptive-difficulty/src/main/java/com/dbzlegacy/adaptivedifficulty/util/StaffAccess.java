package com.dbzlegacy.adaptivedifficulty.util;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Shared access checks: staff (op level 2 / {@code difficulty.admin}) and SkillCheck donators.
 */
public final class StaffAccess {
    private StaffAccess() {}

    /**
     * Brigadier {@code .requires()} / admin handlers — use {@link CommandSourceStack#m_230896_()}
     * not {@code getPlayer()} (throws on Mohist and hides whole subtrees).
     */
    public static boolean isStaffSource(CommandSourceStack src) {
        if (src == null) {
            return false;
        }
        try {
            if (src.m_6761_(2)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        ServerPlayer player = src.m_230896_();
        return player != null && isStaff(player);
    }

    /** {@code 1} if staff; else sends denial and returns {@code 0}. */
    public static int denyUnlessStaff(CommandSourceStack source) {
        if (isStaffSource(source)) {
            return 1;
        }
        source.m_288197_(() -> Component.m_237113_(
                "§cNo permission (need op or difficulty.admin)."
        ), false);
        return 0;
    }

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
            Object bp = player.getClass().getMethod("getBukkitEntity").invoke(player);
            if (bp != null) {
                return bp;
            }
        } catch (Throwable ignored) {
        }
        try {
            UUID id = player.m_20148_();
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object byId = bukkit.getMethod("getPlayer", UUID.class).invoke(null, id);
            if (byId != null) {
                return byId;
            }
            String name = player.m_6302_();
            if (name != null && !name.isBlank()) {
                return bukkit.getMethod("getPlayerExact", String.class).invoke(null, name);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
