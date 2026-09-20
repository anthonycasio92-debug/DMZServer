package com.dbzlegacy.mohistmelee;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Mohist: Bukkit OP / LuckPerms often do not map to Forge permission level 2.
 * Same bridge LM {@code StaffAccess} uses for {@code /lm} and admin GUIs.
 */
public final class MohistStaffAccess {
    private MohistStaffAccess() {}

    public static boolean canEditSdu(Player player) {
        if (player == null) {
            return false;
        }
        try {
            if (player.m_20310_(2)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return bukkitIsOp(player)
                || hasBukkitPermission(player, "difficulty.admin")
                || hasBukkitPermission(player, "sdu.edit");
    }

    public static boolean canEditSdu(CommandSourceStack source) {
        if (source == null) {
            return false;
        }
        try {
            if (source.m_6761_(2)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            Entity entity = source.m_81373_();
            if (entity instanceof Player player) {
                return canEditSdu(player);
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    static boolean bukkitIsOp(Player player) {
        try {
            Object result = player.getClass().getMethod("isOp").invoke(player);
            return result instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean hasBukkitPermission(Player player, String node) {
        try {
            Object result = player.getClass()
                    .getMethod("hasPermission", String.class)
                    .invoke(player, node);
            return result instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
