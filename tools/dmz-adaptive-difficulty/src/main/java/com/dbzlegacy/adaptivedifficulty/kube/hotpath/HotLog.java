package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Debug-only console notes. Player chat lines stay, matching the old scripts. */
public final class HotLog {
    private HotLog() {}

    static void debug(String message) {
        AdaptiveDifficultyMod.LOGGER.debug(message);
    }

    public static void tell(ServerPlayer player, String text) {
        tell(player, ChatFormatting.GRAY, text);
    }

    public static void tell(ServerPlayer player, ChatFormatting color, String text) {
        if (player == null || text == null) {
            return;
        }
        ChatFormatting style = color == null ? ChatFormatting.GRAY : color;
        try {
            player.m_5661_(Component.m_237113_(text).m_130940_(style), false);
        } catch (Throwable ignored) {
        }
    }
}
