package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player-facing slash commands: {@code /lm}, {@code /diff}, {@code /difficulty},
 * {@code /rival}, {@code /spar}, and {@code /skillcheck} (donators). Everything else is staff.
 */
public final class CommandAccess {
    private CommandAccess() {}

    public static boolean isStaff(CommandSourceStack src) {
        return StaffAccess.isStaffSource(src);
    }

    public static boolean hasSkillCheck(CommandSourceStack src) {
        return StaffAccess.hasSkillCheckSource(src);
    }

    /** In-game player commands (hub GUIs). Console must be staff. */
    public static boolean isPlayerSlashUser(CommandSourceStack src) {
        if (src == null) {
            return false;
        }
        if (src.m_230896_() != null) {
            return true;
        }
        return isStaff(src);
    }

    public static int denyUnlessPlayerSlashUser(CommandSourceStack source) {
        if (isPlayerSlashUser(source)) {
            return 1;
        }
        source.m_288197_(() -> Component.m_237113_("§cPlayers only."), false);
        return 0;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> playerRoot(String name) {
        return Commands.m_82127_(name).requires(CommandAccess::isPlayerSlashUser);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> staffRoot(String name) {
        return Commands.m_82127_(name).requires(StaffAccess::isStaffSource);
    }
}
