package com.dbzlegacy.adaptivedifficulty.util;

import java.util.function.Consumer;
import java.util.function.Predicate;
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

    /** Visit online players within radius of {@code from} (player-list distance only). */
    public static void forEachWithin(LivingEntity from, double radius, Consumer<ServerPlayer> consumer) {
        if (from == null || consumer == null || !(from.m_9236_() instanceof ServerLevel level) || radius <= 0.0) {
            return;
        }
        double rSq = radius * radius;
        double x = from.m_20185_();
        double y = from.m_20186_();
        double z = from.m_20189_();
        for (ServerPlayer player : level.m_7654_().m_6846_().m_11314_()) {
            if (player == null || player.m_9236_() != level || !player.m_6084_() || player.m_5833_()) {
                continue;
            }
            double dx = player.m_20185_() - x;
            double dy = player.m_20186_() - y;
            double dz = player.m_20189_() - z;
            if (dx * dx + dy * dy + dz * dz <= rSq) {
                consumer.accept(player);
            }
        }
    }

    public static ServerPlayer nearest(LivingEntity from, double radius) {
        if (from == null || !(from.m_9236_() instanceof ServerLevel level)) {
            return null;
        }
        return nearest(level, from.m_20185_(), from.m_20186_(), from.m_20189_(), radius);
    }

    /**
     * Nearest player who currently participates in Adaptive Difficulty
     * (system on, whitelist ok, personal difficulty on). Falls through
     * personal-off / blocked players instead of stopping on them.
     */
    public static ServerPlayer nearestParticipating(LivingEntity from, double radius) {
        if (from == null || !(from.m_9236_() instanceof ServerLevel level)) {
            return null;
        }
        return nearestMatching(
                level, from.m_20185_(), from.m_20186_(), from.m_20189_(), radius,
                SystemGate::participates
        );
    }

    public static ServerPlayer nearest(ServerLevel level, double x, double y, double z, double radius) {
        return nearestMatching(level, x, y, z, radius, null);
    }

    private static ServerPlayer nearestMatching(
            ServerLevel level,
            double x,
            double y,
            double z,
            double radius,
            Predicate<ServerPlayer> filter
    ) {
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
            if (filter != null && !filter.test(player)) {
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
        return weakestMatching(from, radius, null);
    }

    /** Weakest nearby player who currently participates in Adaptive Difficulty. */
    public static ServerPlayer weakestParticipating(LivingEntity from, double radius) {
        return weakestMatching(from, radius, SystemGate::participates);
    }

    private static ServerPlayer weakestMatching(
            LivingEntity from, double radius, Predicate<ServerPlayer> filter
    ) {
        if (from == null || !(from.m_9236_() instanceof ServerLevel level) || radius <= 0.0) {
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
            if (filter != null && !filter.test(player)) {
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
