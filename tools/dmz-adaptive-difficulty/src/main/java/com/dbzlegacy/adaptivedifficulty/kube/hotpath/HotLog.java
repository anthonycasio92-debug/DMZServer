package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Debug-only console notes. Player chat lines stay, matching the old scripts. */
final class HotLog {
    private HotLog() {}

    static void debug(String message) {
        AdaptiveDifficultyMod.LOGGER.debug(message);
    }

    static void tell(ServerPlayer player, String text) {
        if (player == null || text == null) {
            return;
        }
        try {
            player.m_5661_(Component.m_237113_(text), false);
        } catch (Throwable ignored) {
        }
    }
}
