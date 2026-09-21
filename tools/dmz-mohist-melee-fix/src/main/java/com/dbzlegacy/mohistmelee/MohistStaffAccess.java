package com.dbzlegacy.mohistmelee;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
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
        ServerPlayer player = resolvePlayer(source);
        return player != null && canEditSdu(player);
    }

    /**
     * SDU {@code /sdu edit} uses {@code source.getEntity() instanceof ServerPlayer}.
     * Mohist command sources often have a Bukkit sender / getPlayer() but a null entity.
     */
    public static ServerPlayer resolvePlayer(CommandSourceStack source) {
        if (source == null) {
            return null;
        }
        try {
            Entity entity = source.m_81373_();
            if (entity instanceof ServerPlayer player) {
                return player;
            }
        } catch (Throwable ignored) {
        }
        try {
            return source.m_81375_();
        } catch (Throwable ignored) {
        }
        try {
            Object bukkit = source.getClass().getMethod("getBukkitSender").invoke(source);
            if (bukkit != null) {
                Object handle = bukkit.getClass().getMethod("getHandle").invoke(bukkit);
                if (handle instanceof ServerPlayer player) {
                    return player;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
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
