package com.dbzlegacy.adaptivedifficulty.progression.end;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Soft-entry facade for End Dimension Strength. */
public final class EndProgression {
    private EndProgression() {}

    public static void onLogin(ServerPlayer player) {
        // no-op
    }

    public static void onLogout(ServerPlayer player) {
        // no-op
    }

    public static void pulse(MinecraftServer server, int tick) {
        EndDimensionStrength.pulse(server);
    }

    public static void onHurt(LivingHurtEvent event) {
        EndDimensionStrength.onHurt(event);
    }

    public static void onKill(LivingDeathEvent event, ServerPlayer killer) {
        EndDimensionStrength.onKill(event, killer);
    }
}
