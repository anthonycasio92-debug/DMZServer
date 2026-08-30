package com.dbzlegacy.adaptivedifficulty.progression.end;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** Soft-entry facade for End Dimension Strength + End portal guard. */
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
        EndPortalGuard.pulse(server, tick);
    }

    public static void onHurt(LivingHurtEvent event) {
        EndDimensionStrength.onHurt(event);
    }

    public static void onKill(LivingDeathEvent event, ServerPlayer killer) {
        EndDimensionStrength.onKill(event, killer);
    }

    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        EndPortalGuard.onTravelToDimension(event);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        EndPortalGuard.onRightClickBlock(event);
    }
}
