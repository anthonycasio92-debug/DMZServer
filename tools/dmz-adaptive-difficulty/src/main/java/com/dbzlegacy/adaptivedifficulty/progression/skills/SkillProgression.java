package com.dbzlegacy.adaptivedifficulty.progression.skills;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Flight, sprint/jump, and meditation are not run here. Potential Unlock is
 * wired from {@code ProgressionSystem}.
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
