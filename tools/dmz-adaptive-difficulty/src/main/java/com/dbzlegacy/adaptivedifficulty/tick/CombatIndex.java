package com.dbzlegacy.adaptivedifficulty.tick;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Tracks hostiles currently engaged with players — no world AABB scans.
 * <p>
 * Mobs are marked from target-change / hurt events and expire after a short TTL
 * so the behavior scheduler never has to query {@code getEntitiesOfClass}.
 */
public final class CombatIndex {
    /** mob UUID → expire game-time */
    private static final Map<UUID, Long> ACTIVE = new ConcurrentHashMap<>();
    /** Keep pressure alive between scheduler pulses while fighting. */
    private static final long TTL_TICKS = 100L; // 5 seconds

    private CombatIndex() {}

    public static void mark(Mob mob, long gameTime) {
        if (mob == null) {
            return;
        }
        ACTIVE.put(mob.m_20148_(), gameTime + TTL_TICKS);
        if (ACTIVE.size() > 2048) {
            prune(gameTime);
        }
    }

    public static void mark(Mob mob) {
        if (mob == null || !(mob.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        mark(mob, level.m_46467_()); // getGameTime
    }

    public static void unmark(UUID id) {
        if (id != null) {
            ACTIVE.remove(id);
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static int size() {
        return ACTIVE.size();
    }

    /**
     * Snapshot of still-valid engaged mobs loaded on the server.
     * Expired entries are dropped.
     */
    public static List<Mob> snapshot(MinecraftServer server, long gameTime) {
        List<Mob> out = new ArrayList<>(Math.min(64, ACTIVE.size()));
        if (server == null || ACTIVE.isEmpty()) {
            return out;
        }
        Iterator<Map.Entry<UUID, Long>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (e.getValue() < gameTime) {
                it.remove();
                continue;
            }
            Mob mob = findMob(server, e.getKey());
            if (mob == null || !mob.m_6084_()) {
                it.remove();
                continue;
            }
            out.add(mob);
        }
        return out;
    }

    /**
     * Visit engaged mobs near {@code origin} (same level, distance check).
     * Uses the combat index only — never scans the world AABB.
     */
    public static void forEachNear(Mob origin, double radius, int max, Consumer<Mob> consumer) {
        if (origin == null || consumer == null || max <= 0 || radius <= 0.0) {
            return;
        }
        if (!(origin.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer server = level.m_7654_();
        if (server == null || ACTIVE.isEmpty()) {
            return;
        }
        long gameTime = level.m_46467_();
        double rSq = radius * radius;
        double ox = origin.m_20185_();
        double oy = origin.m_20186_();
        double oz = origin.m_20189_();
        int count = 0;
        for (Map.Entry<UUID, Long> e : ACTIVE.entrySet()) {
            if (count >= max) {
                break;
            }
            if (e.getValue() < gameTime) {
                continue;
            }
            UUID id = e.getKey();
            if (id.equals(origin.m_20148_())) {
                continue;
            }
            Entity entity = level.m_8791_(id); // same-level UUID lookup — no AABB
            if (!(entity instanceof Mob ally) || !ally.m_6084_()) {
                continue;
            }
            double dx = ally.m_20185_() - ox;
            double dy = ally.m_20186_() - oy;
            double dz = ally.m_20189_() - oz;
            if (dx * dx + dy * dy + dz * dz > rSq) {
                continue;
            }
            consumer.accept(ally);
            count++;
        }
    }

    /**
     * Visit engaged mobs near a player (same level). Cap visits for TPS safety.
     */
    public static void forEachNearPlayer(
            net.minecraft.server.level.ServerPlayer player,
            double radius,
            int max,
            Consumer<Mob> consumer
    ) {
        if (player == null || consumer == null || max <= 0 || radius <= 0.0) {
            return;
        }
        if (!(player.m_9236_() instanceof ServerLevel level) || ACTIVE.isEmpty()) {
            return;
        }
        long gameTime = level.m_46467_();
        double rSq = radius * radius;
        double px = player.m_20185_();
        double py = player.m_20186_();
        double pz = player.m_20189_();
        int count = 0;
        for (Map.Entry<UUID, Long> e : ACTIVE.entrySet()) {
            if (count >= max) {
                break;
            }
            if (e.getValue() < gameTime) {
                continue;
            }
            Entity entity = level.m_8791_(e.getKey());
            if (!(entity instanceof Mob mob) || !mob.m_6084_()) {
                continue;
            }
            double dx = mob.m_20185_() - px;
            double dy = mob.m_20186_() - py;
            double dz = mob.m_20189_() - pz;
            if (dx * dx + dy * dy + dz * dz > rSq) {
                continue;
            }
            consumer.accept(mob);
            count++;
        }
    }

    private static Mob findMob(MinecraftServer server, UUID id) {
        // Prefer entity lookup by UUID across loaded levels — O(levels), not O(entities in AABB).
        for (ServerLevel level : server.m_129785_()) { // getAllLevels
            if (level == null) {
                continue;
            }
            Entity entity = level.m_8791_(id); // getEntity(UUID)
            if (entity instanceof Mob mob) {
                return mob;
            }
        }
        return null;
    }

    private static void prune(long gameTime) {
        ACTIVE.entrySet().removeIf(e -> e.getValue() < gameTime);
        if (ACTIVE.size() > 2048) {
            int remove = ACTIVE.size() / 2;
            Iterator<Map.Entry<UUID, Long>> it = ACTIVE.entrySet().iterator();
            while (it.hasNext() && remove-- > 0) {
                it.next();
                it.remove();
            }
        }
    }
}
