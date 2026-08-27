package com.dbzlegacy.adaptivedifficulty.progression.shop;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Soft-entry facade for skill unlock GUI + prestige purchase. */
public final class ShopProgression {
    private ShopProgression() {}

    public static void onLogin(ServerPlayer player) {
        // no-op
    }

    public static void onLogout(ServerPlayer player) {
        if (player != null) {
            PrestigeSystem.clearPlayer(player.m_20148_());
        }
    }

    public static void pulse(MinecraftServer server, int tick) {
        // shop is command/GUI driven
    }
}
