package com.dbzlegacy.adaptivedifficulty.progression.dummy;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Soft-entry facade for Shadow Dummy limiter + forge protect. */
public final class DummyProgression {
    private DummyProgression() {}

    public static void onLogin(ServerPlayer player) {
        // no-op
    }

    public static void onLogout(ServerPlayer player) {
        if (player != null) {
            ShadowDummyLimiter.clearPlayer(player.m_20148_());
        }
    }

    public static void pulse(MinecraftServer server, int tick) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (player != null) {
                ShadowDummyLimiter.pulse(player);
            }
        }
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        ShadowDummyLimiter.onJoin(event);
    }

    public static void onHurt(LivingHurtEvent event) {
        ShadowDummyLimiter.onHurt(event);
    }
}
