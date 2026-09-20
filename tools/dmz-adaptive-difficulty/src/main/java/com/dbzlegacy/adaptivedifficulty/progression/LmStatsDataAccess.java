package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Resolve {@link ServerPlayer} for DMZ {@link StatsData} (Mohist sometimes leaves {@link StatsData#getPlayer()} null). */
public final class LmStatsDataAccess {
    private LmStatsDataAccess() {}

    public static ServerPlayer serverPlayer(StatsData data) {
        if (data == null) {
            return null;
        }
        try {
            Player p = data.getPlayer();
            if (p instanceof ServerPlayer sp && sp.m_6084_()) {
                return sp;
            }
        } catch (Throwable ignored) {
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        for (ServerPlayer sp : server.m_6846_().m_11314_()) {
            try {
                if (DmzProgression.stats(sp) == data) {
                    return sp;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
