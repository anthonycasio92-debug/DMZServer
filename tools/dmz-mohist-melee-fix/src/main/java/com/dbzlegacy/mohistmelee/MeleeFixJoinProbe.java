package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Join diagnostics + soft combat repair. Cancelled ki/PvP on Mohist can leave
 * attackers unable to M1 until death; repairing on login reduces that window.
 */
public final class MeleeFixJoinProbe {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);

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
    }
}
