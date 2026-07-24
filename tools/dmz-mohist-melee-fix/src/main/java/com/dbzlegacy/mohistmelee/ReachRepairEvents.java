package com.dbzlegacy.mohistmelee;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Join/respawn/tick maintenance for reach + stale strike locks.
 * No soft-respawn, no damage redirects.
 */
public final class ReachRepairEvents {
    private ReachRepairEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ReachRepairEvents());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            // Primaries are usually intact on join; snapshot after a moment via tick.
            CombatUnlock.unlockForLogin(sp, "join");
            PrimaryStatRepair.snapshot(sp);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PrimaryStatRepair.clear(sp.m_20148_());
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            CombatUnlock.unlockForLogin(sp, "respawn");
        }
    }

    @SubscribeEvent
    public void onDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            // Same path as raids: delayed follow-ups — Mohist often wipes attrs after this event.
            CombatUnlock.unlockAfterTeleport(sp, "dimension");
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.m_9236_().f_46443_ || !(player instanceof ServerPlayer sp)) {
            return;
        }
        CombatUnlock.tickFollowups(sp);
        // ~every 1 second: clear stale strike lock if ACTIVE map is empty
        if (player.f_19797_ % 20 == 0) {
            CombatUnlock.clearStaleStrikeLock(sp, "tick");
            PrimaryStatRepair.snapshot(sp);
        }
        // ~every 5 seconds: reach check + primary restore if wiped
        if (player.f_19797_ % 100 == 0) {
            ReachAttributeFix.repair(sp, "tick");
            PrimaryStatRepair.ensure(sp, "tick");
        }
    }
}
