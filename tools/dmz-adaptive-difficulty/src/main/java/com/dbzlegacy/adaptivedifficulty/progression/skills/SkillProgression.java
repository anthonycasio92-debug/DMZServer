package com.dbzlegacy.adaptivedifficulty.progression.skills;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Potential Unlock is wired from {@code ProgressionSystem}. Flight, sprint, and
 * the old meditation trainer are not in this mod.
 */
public final class SkillProgression {
    private SkillProgression() {}

    public static void onLogin(ServerPlayer player) {
    }

    public static void onLogout(ServerPlayer player) {
    }

    public static void pulse(MinecraftServer server, int tick) {
    }
}
