package com.dbzlegacy.adaptivedifficulty.command;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** Session toggle: staff must enable admin mode before config commands work. */
public final class AdminCommandAccess {
    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();

    private AdminCommandAccess() {}

    public static boolean isEnabled(ServerPlayer player) {
        return player != null && ENABLED.contains(player.m_20148_());
    }

    public static boolean toggle(ServerPlayer player) {
        UUID id = player.m_20148_();
        if (ENABLED.contains(id)) {
            ENABLED.remove(id);
            return false;
        }
        ENABLED.add(id);
        return true;
    }

    public static void disable(ServerPlayer player) {
        if (player != null) {
            ENABLED.remove(player.m_20148_());
        }
    }

    public static void clear() {
        ENABLED.clear();
    }
}
