package com.dbzlegacy.adaptivedifficulty.rival;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Resolve online players by rivalry UUID (case-insensitive). */
public final class RivalOnlinePlayers {
    private RivalOnlinePlayers() {}

    public static ServerPlayer find(MinecraftServer server, String uuid) {
        if (server == null || uuid == null || uuid.isBlank()) {
            return null;
        }
        String canon = RivalUuid.canonical(uuid);
        for (ServerPlayer p : server.m_6846_().m_11314_()) {
            if (p == null) {
                continue;
            }
            String online = p.m_20148_().toString();
            if (RivalUuid.samePlayer(online, uuid) || (canon != null && RivalUuid.samePlayer(online, canon))) {
                return p;
            }
        }
        return null;
    }
}
