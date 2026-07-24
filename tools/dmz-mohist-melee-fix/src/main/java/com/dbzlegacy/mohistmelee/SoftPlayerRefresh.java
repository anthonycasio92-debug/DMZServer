package com.dbzlegacy.mohistmelee;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Recreate {@link ServerPlayer} in-place the same way death/respawn does (new entity +
 * client LocalPlayer), without killing the player or enabling keepInventory.
 * This rebuilds Mohist's CraftPlayer bridge and flushes DMZ client combat state.
 */
public final class SoftPlayerRefresh {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final Set<UUID> REFRESHED_THIS_SESSION = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> LAST_REFRESH_MS = new ConcurrentHashMap<>();

    private SoftPlayerRefresh() {}

    public static boolean alreadyRefreshed(UUID id) {
        return REFRESHED_THIS_SESSION.contains(id);
    }

    /**
     * @return the possibly-new player instance (same connection), or the original on failure
     */
    public static ServerPlayer recreateAtPlace(ServerPlayer player, String reason) {
        if (player == null || player.m_9236_().f_46443_) {
            return player;
        }
        UUID id = player.m_20148_();
        long now = System.currentTimeMillis();
        Long prev = LAST_REFRESH_MS.get(id);
        if (prev != null && now - prev < 15_000L) {
            return player;
        }

        try {
            double x = player.m_20185_();
            double y = player.m_20186_();
            double z = player.m_20189_();
            float yRot = player.m_146908_();
            float xRot = player.m_146909_();
            float health = player.m_21223_();
            float absorption = player.m_6103_();
            ServerLevel level = player.m_284548_();
            GameType gameType = player.f_8941_.m_9290_();
            int food = player.m_36324_().m_38702_();
            float saturation = player.m_36324_().m_38722_();
            int xp = player.f_36078_;
            float xpProgress = player.f_36080_;
            int xpLevel = player.f_36079_;

            PlayerList list = player.m_20194_().m_6846_();
            // conqueredEnd=true keeps inventory like End return — does NOT toggle keepInventory gamerule
            ServerPlayer neu = list.m_11236_(player, true);
            if (neu == null) {
                LOGGER.warn("[{}] soft refresh returned null player={} reason={}", DmzMohistMeleeFix.MOD_ID, id, reason);
                return player;
            }

            neu.f_8941_.m_143473_(gameType);
            neu.m_8999_(level, x, y, z, yRot, xRot);
            neu.m_21153_(Math.max(1.0F, health));
            neu.m_7911_(absorption);
            neu.m_36324_().m_38705_(food);
            neu.m_36324_().m_38717_(saturation);
            neu.f_36078_ = xp;
            neu.f_36080_ = xpProgress;
            neu.f_36079_ = xpLevel;

            DamageBridge.forceClearCombatLocks(neu, "soft-refresh");
            DamageBridge.repairAttacker(neu, "soft-refresh:" + reason);

            REFRESHED_THIS_SESSION.add(id);
            LAST_REFRESH_MS.put(id, now);
            LOGGER.info(
                    "[{}] soft player refresh player={} reason={}",
                    DmzMohistMeleeFix.MOD_ID,
                    neu.m_36316_().getName(),
                    reason
            );
            return neu;
        } catch (Throwable t) {
            LOGGER.warn("[{}] soft refresh failed reason={}: {}", DmzMohistMeleeFix.MOD_ID, reason, t.toString());
            return player;
        }
    }
}
