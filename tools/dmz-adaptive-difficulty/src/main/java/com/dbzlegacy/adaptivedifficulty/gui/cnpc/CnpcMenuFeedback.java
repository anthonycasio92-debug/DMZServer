package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** One-shot messages shown inside CNPC menus (instead of chat). */
public final class CnpcMenuFeedback {
    private static final ConcurrentHashMap<UUID, List<String>> PENDING = new ConcurrentHashMap<>();

    private CnpcMenuFeedback() {}

    public static void set(ServerPlayer player, String message) {
        if (player == null || message == null || message.isBlank()) {
            return;
        }
        List<String> lines = new ArrayList<>();
        for (String line : message.split("\n")) {
            if (line != null && !line.isBlank()) {
                lines.add(CnpcGuiSupport.safeChat(CnpcGuiStyle.normalizeLine(line)));
            }
        }
        if (!lines.isEmpty()) {
            PENDING.put(player.m_20148_(), lines);
        }
    }

    /** True when a notice is waiting. Does not consume it. */
    public static boolean hasPending(ServerPlayer player) {
        return pendingLineCount(player) > 0;
    }

    /** Lines waiting for the next menu, capped at the inline notice band. Does not consume them. */
    public static int pendingLineCount(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        List<String> lines = PENDING.get(player.m_20148_());
        if (lines == null || lines.isEmpty()) {
            return 0;
        }
        return Math.min(CnpcGuiStyle.INFO_INLINE_MAX, lines.size());
    }

    /** Removes and returns pending lines for this player (shown once per menu open). */
    public static List<String> take(ServerPlayer player) {
        if (player == null) {
            return List.of();
        }
        List<String> lines = PENDING.remove(player.m_20148_());
        return lines == null ? List.of() : lines;
    }
}
