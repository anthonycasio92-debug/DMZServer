package com.dbzlegacy.mohistmelee;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Light touch: repair reach on login / respawn / dimension change, and every
 * few seconds while online. No soft-respawn, no damage changes.
 */
public final class ReachRepairEvents {
    private ReachRepairEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ReachRepairEvents());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        repairIfServer(event.getEntity(), "join");
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        repairIfServer(event.getEntity(), "respawn");
    }

    @SubscribeEvent
    public void onDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        repairIfServer(event.getEntity(), "dimension");
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.m_9236_().f_46443_) {
            return;
        }
        // ~every 5 seconds
        if (player.f_19797_ % 100 != 0) {
            return;
        }
        ReachAttributeFix.repair(player, "tick");
    }

    private static void repairIfServer(Player player, String reason) {
        if (player instanceof ServerPlayer) {
            ReachAttributeFix.repair(player, reason);
        }
    }
}
