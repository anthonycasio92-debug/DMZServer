package com.dbzlegacy.adaptivedifficulty.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Cheap nearby-player checks via the online player list (no AABB entity queries).
 */
public final class NearbyPlayers {
    private NearbyPlayers() {}

    public static boolean anyWithin(Entity entity, double radius) {
        if (!(entity != null && entity.m_9236_() instanceof ServerLevel level)) {
            return false;
        }
        return nearest(level, entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), radius) != null;
    }

    public static ServerPlayer nearest(LivingEntity from, double radius) {
        if (from == null || !(from.m_9236_() instanceof ServerLevel level)) {
            return null;
        }
        return nearest(level, from.m_20185_(), from.m_20186_(), from.m_20189_(), radius);
    }

    public static ServerPlayer nearest(ServerLevel level, double x, double y, double z, double radius) {
        if (level == null || radius <= 0.0) {
            return null;
        }
        double rSq = radius * radius;
        ServerPlayer best = null;
        double bestDist = Double.MAX_VALUE;
        for (ServerPlayer player : level.m_7654_().m_6846_().m_11314_()) {
            if (player == null || player.m_9236_() != level || !player.m_6084_() || player.m_5833_()) {
                continue;
            }
            double dx = player.m_20185_() - x;
            double dy = player.m_20186_() - y;
            double dz = player.m_20189_() - z;
            double d = dx * dx + dy * dy + dz * dz;
            if (d <= rSq && d < bestDist) {
                bestDist = d;
                best = player;
            }
        }
        return best;
    }

    public static ServerPlayer weakest(LivingEntity from, double radius) {
        if (from == null || !(from.m_9236_() instanceof ServerLevel level)) {
            return null;
        }
        double rSq = radius * radius;
        double x = from.m_20185_();
        double y = from.m_20186_();
        double z = from.m_20189_();
        ServerPlayer weakest = null;
        float lowest = Float.MAX_VALUE;
        for (ServerPlayer player : level.m_7654_().m_6846_().m_11314_()) {
            if (player == null || player.m_9236_() != level || !player.m_6084_() || player.m_5833_()) {
                continue;
            }
            double dx = player.m_20185_() - x;
            double dy = player.m_20186_() - y;
            double dz = player.m_20189_() - z;
            if (dx * dx + dy * dy + dz * dz > rSq) {
                continue;
            }
            float hp = player.m_21223_();
            if (hp < lowest) {
                lowest = hp;
                weakest = player;
            }
        }
        return weakest;
    }
}
