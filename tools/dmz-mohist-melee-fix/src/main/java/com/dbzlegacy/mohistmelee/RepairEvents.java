package com.dbzlegacy.mohistmelee;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class RepairEvents {
    private static final Map<UUID, Integer> NEXT_IN = new ConcurrentHashMap<UUID, Integer>();
    private static final Map<UUID, String> REASON = new ConcurrentHashMap<UUID, String>();
    private static final Map<UUID, Deque<Integer>> QUEUE = new ConcurrentHashMap<UUID, Deque<Integer>>();

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            CombatRepair.snapshotPrimaries((Player)player2);
            CombatRepair.respawnLikeRecovery(player2, "join");
            CombatRepair.repairAll(player2, "join");
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            CombatRepair.snapshotPrimaries((Player)player2);
            CombatRepair.respawnLikeRecovery(player2, "respawn");
            CombatRepair.repairAll(player2, "respawn");
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            UUID id = player2.m_20148_();
            CombatRepair.dropSnapshot(id);
            NEXT_IN.remove(id);
            REASON.remove(id);
            QUEUE.remove(id);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.m_9236_().f_46443_ || !(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer serverPlayer = (ServerPlayer)player;
        CombatRepair.tickSuppress((Player)serverPlayer);
        RepairEvents.tickFollowups(serverPlayer);
        int age = player.f_19797_;
        if (age % 20 == 0) {
            CombatRepair.snapshotPrimaries((Player)serverPlayer);
            CombatRepair.clearCombatLock(serverPlayer, "tick", true);
        }
        if (age % 100 == 0) {
            CombatRepair.sanitizeReach(serverPlayer, "tick");
            CombatRepair.restorePrimaries((Player)serverPlayer, "tick");
        }
    }

    public static void onTeleport(ServerPlayer player, String reason) {
        if (player == null || player.m_9236_().f_46443_) {
            return;
        }
        CombatRepair.snapshotPrimaries((Player)player);
        CombatRepair.respawnLikeRecovery(player, reason);
        CombatRepair.repairAll(player, reason);
        RepairEvents.schedule(player, reason, 5, 15, 40);
    }

    private static void schedule(ServerPlayer player, String reason, int first, int ... rest) {
        UUID id = player.m_20148_();
        NEXT_IN.put(id, Math.max(1, first));
        REASON.put(id, reason);
        ArrayDeque<Integer> q = new ArrayDeque<Integer>();
        for (int r : rest) {
            if (r <= 0) continue;
            q.addLast(r);
        }
        QUEUE.put(id, q);
    }

    private static void tickFollowups(ServerPlayer player) {
        UUID id = player.m_20148_();
        Integer countdown = NEXT_IN.get(id);
        if (countdown == null) {
            return;
        }
        if (countdown > 1) {
            NEXT_IN.put(id, countdown - 1);
            return;
        }
        String reason = REASON.getOrDefault(id, "teleport") + "-followup";
        CombatRepair.repairAll(player, reason);
        Deque<Integer> q = QUEUE.get(id);
        if (q != null && !q.isEmpty()) {
            NEXT_IN.put(id, q.removeFirst());
        } else {
            NEXT_IN.remove(id);
            REASON.remove(id);
            QUEUE.remove(id);
        }
    }
}
