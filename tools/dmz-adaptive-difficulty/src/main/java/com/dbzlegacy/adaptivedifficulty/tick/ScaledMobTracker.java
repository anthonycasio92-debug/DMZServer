package com.dbzlegacy.adaptivedifficulty.tick;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Hard cap: at most N difficulty-adjusted hostiles per player (default 5).
 * Tracks claimed mob UUIDs and prefers the closest hostiles.
 */
public final class ScaledMobTracker {
    private static final Map<UUID, List<Claim>> CLAIMS = new ConcurrentHashMap<>();

    private ScaledMobTracker() {}

    public static int maxSlots() {
        return Math.max(1, DifficultyConfig.get().maxScaledMobsPerPlayer);
    }

    /**
     * Try to claim a slot for {@code mob} under {@code player}.
     * Evicts the farthest claimed mob when full and {@code mob} is closer.
     *
     * @return true if this mob may receive difficulty scaling for the player
     */
    public static boolean tryClaim(ServerPlayer player, Mob mob) {
        if (player == null || mob == null || !mob.m_6084_()) {
            return false;
        }
        UUID playerId = player.m_20148_();
        UUID mobId = mob.m_20148_();
        double distSq = player.m_20275_(mob.m_20185_(), mob.m_20186_(), mob.m_20189_());
        List<Claim> claims = CLAIMS.computeIfAbsent(playerId, id -> new ArrayList<>(maxSlots()));
        synchronized (claims) {
            // Fast path: already claimed — no world scan.
            for (Claim c : claims) {
                if (c.mobId.equals(mobId)) {
                    c.distSq = distSq;
                    return true;
                }
            }
            int max = maxSlots();
            // Only world-scan dead claims when we need a free slot.
            if (claims.size() >= max) {
                pruneDead(claims, player);
            }
            if (claims.size() < max) {
                claims.add(new Claim(mobId, distSq));
                return true;
            }
            // Replace farthest if this mob is closer.
            Claim farthest = null;
            for (Claim c : claims) {
                if (farthest == null || c.distSq > farthest.distSq) {
                    farthest = c;
                }
            }
            if (farthest != null && distSq < farthest.distSq) {
                UUID evicted = farthest.mobId;
                claims.remove(farthest);
                claims.add(new Claim(mobId, distSq));
                PENDING_REVERT.put(evicted, playerId);
                return true;
            }
            return false;
        }
    }

    public static boolean isClaimed(ServerPlayer player, Mob mob) {
        if (player == null || mob == null) {
            return false;
        }
        List<Claim> claims = CLAIMS.get(player.m_20148_());
        if (claims == null) {
            return false;
        }
        UUID mobId = mob.m_20148_();
        synchronized (claims) {
            for (Claim c : claims) {
                if (c.mobId.equals(mobId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int claimedCount(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        List<Claim> claims = CLAIMS.get(player.m_20148_());
        if (claims == null) {
            return 0;
        }
        synchronized (claims) {
            return claims.size();
        }
    }

    /** Periodic cleanup from the nearby scaler pulse (player's dimension first). */
    public static void prunePlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        List<Claim> claims = CLAIMS.get(player.m_20148_());
        if (claims == null || claims.isEmpty()) {
            return;
        }
        synchronized (claims) {
            pruneDead(claims, player);
        }
    }

    public static void release(ServerPlayer player, UUID mobId) {
        if (player == null || mobId == null) {
            return;
        }
        List<Claim> claims = CLAIMS.get(player.m_20148_());
        if (claims == null) {
            return;
        }
        synchronized (claims) {
            claims.removeIf(c -> c.mobId.equals(mobId));
        }
    }

    /** Drop every claim + pending eviction for this mob (used when a mob becomes exempt). */
    public static void releaseAllClaims(Mob mob) {
        if (mob == null) {
            return;
        }
        UUID mobId = mob.m_20148_();
        PENDING_REVERT.remove(mobId);
        for (List<Claim> claims : CLAIMS.values()) {
            synchronized (claims) {
                claims.removeIf(c -> c.mobId.equals(mobId));
            }
        }
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            CLAIMS.remove(playerId);
        }
    }

    /** Mob UUIDs that lost their slot and should revert to base stats. */
    private static final Map<UUID, UUID> PENDING_REVERT = new ConcurrentHashMap<>();

    public static UUID pollEvictedOwner(UUID mobId) {
        return mobId == null ? null : PENDING_REVERT.remove(mobId);
    }

    public static Map<UUID, UUID> drainEvictions() {
        if (PENDING_REVERT.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> out = new ConcurrentHashMap<>(PENDING_REVERT);
        PENDING_REVERT.keySet().removeAll(out.keySet());
        return out;
    }

    private static void pruneDead(List<Claim> claims, ServerPlayer preferPlayer) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        ServerLevel preferLevel = null;
        if (preferPlayer != null && preferPlayer.m_9236_() instanceof ServerLevel sl) {
            preferLevel = sl;
        }
        Iterator<Claim> it = claims.iterator();
        while (it.hasNext()) {
            Claim c = it.next();
            Mob mob = findMob(server, preferLevel, c.mobId);
            if (mob == null || !mob.m_6084_()) {
                it.remove();
            }
        }
    }

    private static Mob findMob(
            net.minecraft.server.MinecraftServer server, ServerLevel preferLevel, UUID id
    ) {
        if (preferLevel != null) {
            var entity = preferLevel.m_8791_(id);
            if (entity instanceof Mob mob) {
                return mob;
            }
        }
        for (ServerLevel level : server.m_129785_()) {
            if (level == preferLevel) {
                continue;
            }
            var entity = level.m_8791_(id); // getEntity
            if (entity instanceof Mob mob) {
                return mob;
            }
        }
        return null;
    }

    /** Sort candidates nearest-first for claiming. */
    public static void sortNearest(ServerPlayer player, List<Mob> mobs) {
        if (player == null || mobs == null || mobs.size() < 2) {
            return;
        }
        double px = player.m_20185_();
        double py = player.m_20186_();
        double pz = player.m_20189_();
        mobs.sort(Comparator.comparingDouble(m -> {
            double dx = m.m_20185_() - px;
            double dy = m.m_20186_() - py;
            double dz = m.m_20189_() - pz;
            return dx * dx + dy * dy + dz * dz;
        }));
    }

    private static final class Claim {
        final UUID mobId;
        double distSq;

        Claim(UUID mobId, double distSq) {
            this.mobId = mobId;
            this.distSq = distSq;
        }
    }
}
