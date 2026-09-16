package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.util.PaidFeatureAccess;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.server.level.ServerPlayer;

public final class CharacterServicesAccess {
    private CharacterServicesAccess() {}

    public static boolean canUseServices(ServerPlayer player) {
        return has(player, CharacterServicesConfig.get().permissions.services, true);
    }

    public static boolean canRaceChange(ServerPlayer player) {
        return canUseServices(player)
                && has(player, CharacterServicesConfig.get().permissions.race, true);
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
        return defaultWhenBlank;
    }
}
