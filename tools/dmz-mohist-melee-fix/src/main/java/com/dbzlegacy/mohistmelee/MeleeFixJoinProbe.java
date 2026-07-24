package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Join diagnostics + combat repair + one soft player recreate on Mohist.
 * Soft recreate mirrors suicide/respawn (new ServerPlayer + client LocalPlayer)
 * without dying or enabling keepInventory — the only reliable Mohist recovery.
 */
public final class MeleeFixJoinProbe {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final Map<UUID, Integer> PENDING_SOFT_REFRESH = new ConcurrentHashMap<>();

    private MeleeFixJoinProbe() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new MeleeFixJoinProbe());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        boolean hasStats = StatsProvider.get(StatsCapability.INSTANCE, (Entity) serverPlayer).isPresent();
        DamageBridge.repairAttacker(serverPlayer, "join");
        LOGGER.info(
                "[{}] join probe player={} statsCapability={}",
                DmzMohistMeleeFix.MOD_ID,
                serverPlayer.m_36316_().getName(),
                hasStats
        );

        if (!isMohist()) {
            return;
        }
        // Delay ~2s so login packets settle, then soft-recreate once.
        PENDING_SOFT_REFRESH.put(serverPlayer.m_20148_(), 40);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_SOFT_REFRESH.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, Integer>> it = PENDING_SOFT_REFRESH.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            int left = e.getValue() - 1;
            if (left > 0) {
                e.setValue(left);
                continue;
            }
            it.remove();
            ServerPlayer online = server.m_6846_().m_11259_(e.getKey());
            if (online == null) {
                continue;
            }
            if (SoftPlayerRefresh.alreadyRefreshed(online.m_20148_())) {
                continue;
            }
            SoftPlayerRefresh.recreateAtPlace(online, "join");
        }
    }

    private static boolean isMohist() {
        try {
            Class.forName("com.mohistmc.MohistMC");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("org.bukkit.Bukkit");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }
}
