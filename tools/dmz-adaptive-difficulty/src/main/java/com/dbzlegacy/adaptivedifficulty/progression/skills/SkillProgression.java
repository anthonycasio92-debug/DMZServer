package com.dbzlegacy.adaptivedifficulty.progression.skills;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Soft-entry facade that pulses sibling skill modules when present. */
public final class SkillProgression {
    private SkillProgression() {}

    public static void onLogin(ServerPlayer player) {
        // no-op
    }

    public static void onLogout(ServerPlayer player) {
        try {
            FlightProgression.onLogout(player);
        } catch (Throwable ignored) {
        }
        try {
            MeditationProgression.onLogout(player);
        } catch (Throwable ignored) {
        }
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
                FlightProgression.pulse(player, now);
            } catch (Throwable ignored) {
            }
            try {
                SprintJumpProgression.pulse(player, now);
            } catch (Throwable ignored) {
            }
            try {
                MeditationProgression.pulse(player, now);
            } catch (Throwable ignored) {
            }
        }
    }
}
