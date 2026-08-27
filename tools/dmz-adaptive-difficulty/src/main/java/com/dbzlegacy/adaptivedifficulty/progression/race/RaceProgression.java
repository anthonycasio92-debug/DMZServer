package com.dbzlegacy.adaptivedifficulty.progression.race;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Soft-entry facade for race progression modules. */
public final class RaceProgression {
    private RaceProgression() {}

    public static void onLogin(ServerPlayer player) {
        // no-op
    }

    public static void onLogout(ServerPlayer player) {
        // no-op
    }

    public static void pulse(MinecraftServer server, int tick) {
        if (server == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (player == null) {
                continue;
            }
            try {
                RaceLock.pulse(player, now);
            } catch (Throwable ignored) {
            }
            try {
                YardratProgression.pulse(player, now);
            } catch (Throwable ignored) {
            }
            try {
                SpiritualistKiControl.pulse(player, now);
            } catch (Throwable ignored) {
            }
        }
    }
}
