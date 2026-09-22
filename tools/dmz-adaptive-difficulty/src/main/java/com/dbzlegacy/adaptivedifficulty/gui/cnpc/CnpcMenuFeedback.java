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

    /** Removes and returns pending lines for this player (shown once per menu open). */
    public static List<String> take(ServerPlayer player) {
        if (player == null) {
            return List.of();
        }
        List<String> lines = PENDING.remove(player.m_20148_());
        return lines == null ? List.of() : lines;
    }
}
