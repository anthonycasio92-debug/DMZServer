package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.util.PaidFeatureAccess;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.server.level.ServerPlayer;

public final class CharacterServicesAccess {
    private CharacterServicesAccess() {}

    public static boolean canUseServices(ServerPlayer player) {
        return has(player, CharacterServicesConfig.get().permissions.services, true);
    }

    /** Players cannot change race. Staff still can. */
    public static boolean canRaceChange(ServerPlayer player) {
        return StaffAccess.isStaff(player);
    }

    public static boolean canClassChange(ServerPlayer player) {
        return canUseServices(player)
                && has(player, CharacterServicesConfig.get().permissions.classChange, true);
    }

    public static boolean canReskin(ServerPlayer player) {
        return canUseServices(player)
                && has(player, CharacterServicesConfig.get().permissions.reskin, true);
    }

    public static boolean canHeadBoneShop(ServerPlayer player) {
        return canUseServices(player)
                && has(player, CharacterServicesConfig.get().permissions.headBones, true);
    }

    public static boolean bypassCost(ServerPlayer player) {
        return PaidFeatureAccess.bypassAncientCoinCost(player);
    }

    public static boolean bypassCooldown(ServerPlayer player) {
        return StaffAccess.isStaff(player)
                || has(player, CharacterServicesConfig.get().permissions.bypassCooldown, false)
                || has(player, CharacterServicesConfig.get().permissions.admin, false);
    }

    /** Staff / character admin — ignore prestige race-lock gates in Character Services. */
    public static boolean bypassRaceLock(ServerPlayer player) {
        return StaffAccess.isStaff(player)
                || has(player, CharacterServicesConfig.get().permissions.admin, false);
    }

    private static boolean has(ServerPlayer player, String node, boolean defaultWhenBlank) {
        if (player == null) {
            return false;
        }
        if (node == null || node.isBlank()) {
            return defaultWhenBlank;
        }
        Object bukkit = bukkitEntity(player);
        if (bukkit == null) {
            return defaultWhenBlank;
        }
        try {
            Object ok = bukkit.getClass().getMethod("hasPermission", String.class).invoke(bukkit, node);
            if (ok instanceof Boolean b) {
                if (b) {
                    return true;
                }
                // Forge-only: without LegacyMechanicsGUI, nodes may be missing from PluginManager
                // and Bukkit returns false — treat like plugin.yml default: true for player services.
                if (defaultWhenBlank && !permissionRegistered(node)) {
                    return true;
                }
                return false;
            }
        } catch (Throwable ignored) {
        }
        return defaultWhenBlank;
    }

    private static Object bukkitEntity(ServerPlayer player) {
        try {
            return player.getClass().getMethod("getBukkitEntity").invoke(player);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean permissionRegistered(String node) {
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object pm = bukkit.getMethod("getPluginManager").invoke(null);
            Object perm = pm.getClass().getMethod("getPermission", String.class).invoke(pm, node);
            return perm != null;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
