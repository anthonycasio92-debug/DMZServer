package com.dbzlegacy.adaptivedifficulty.progression.tp;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Soft-entry facade for TP progression modules. */
public final class TpProgression {
    private TpProgression() {}

    public static void onLogin(ServerPlayer player) {
        try {
            GlobalTpBoost.onLogin(player);
        } catch (Throwable ignored) {
        }
    }

    public static void onLogout(ServerPlayer player) {
        // session state cleared via ProgressionData
    }

    public static void pulse(MinecraftServer server, int tick) {
        if (server == null) {
            return;
        }
        try {
            GlobalTpBoost.pulse(server, tick);
        } catch (Throwable ignored) {
        }
        long now = System.currentTimeMillis();
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (player == null) {
                continue;
            }
            try {
                BioAndroidAbsorb.pulse(player, now);
            } catch (Throwable ignored) {
            }
        }
    }
}
