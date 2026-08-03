package com.dbzlegacy.adaptivedifficulty.tick;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Hard cap: at most N difficulty-adjusted hostiles per player (default 5).
 * Global ownership: a mob claimed by one player cannot be stolen by another.
 */
public final class ScaledMobTracker {
    private static final Map<UUID, List<Claim>> CLAIMS = new ConcurrentHashMap<>();
    /** mob UUID → claiming player UUID (single global owner). */
    private static final Map<UUID, UUID> MOB_OWNER = new ConcurrentHashMap<>();
    /** Mob UUIDs that lost their slot and should revert to base stats. */
    private static final Map<UUID, UUID> PENDING_REVERT = new ConcurrentHashMap<>();

    private ScaledMobTracker() {}

    public static int maxSlots() {
        return Math.max(1, Math.min(5, DifficultyConfig.get().maxScaledMobsPerPlayer));
    }

    /**
     * Try to claim a slot for {@code mob} under {@code player}.
     * Refuses if another online player already owns the mob.
     */
    public static boolean tryClaim(ServerPlayer player, Mob mob) {
        if (player == null || mob == null || !mob.m_6084_()) {
            return false;
        }
        UUID playerId = player.m_20148_();
        UUID mobId = mob.m_20148_();
        double distSq = player.m_20275_(mob.m_20185_(), mob.m_20186_(), mob.m_20189_());

        UUID owner = MOB_OWNER.get(mobId);
        if (owner != null && !owner.equals(playerId)) {
            if (isOwnerOnline(owner)) {
                return false; // another player holds this mob
            }
            // Stale owner (offline) — drop ownership so we can claim.
            MOB_OWNER.remove(mobId, owner);
            List<Claim> stale = CLAIMS.get(owner);
            if (stale != null) {
                synchronized (stale) {
                    stale.removeIf(c -> c.mobId.equals(mobId));
                }
            }
        }

        List<Claim> claims = CLAIMS.computeIfAbsent(playerId, id -> new ArrayList<>(maxSlots()));
        synchronized (claims) {
            for (Claim c : claims) {
                if (c.mobId.equals(mobId)) {
                    c.distSq = distSq;
                    MOB_OWNER.put(mobId, playerId);
                    return true;
                }
            }
            int max = maxSlots();
            if (claims.size() >= max) {
                pruneDead(claims, player);
            }
            if (claims.size() < max) {
                claims.add(new Claim(mobId, distSq));
                MOB_OWNER.put(mobId, playerId);
                return true;
            }
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
                MOB_OWNER.remove(evicted, playerId);
                PENDING_REVERT.put(evicted, playerId);
                MOB_OWNER.put(mobId, playerId);
                return true;
            }
            return false;
        }
    }

    public static boolean isClaimed(ServerPlayer player, Mob mob) {
        if (player == null || mob == null) {
            return false;
        }
        return player.m_20148_().equals(MOB_OWNER.get(mob.m_20148_()));
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

    /**
     * Keep only {@code keep} mobs for this player; queue the rest for base-stat revert.
     * Used when the player leaves range of previously claimed hostiles.
     */
    public static void retainOnly(ServerPlayer player, Set<UUID> keep) {
        if (player == null) {
            return;
        }
        UUID playerId = player.m_20148_();
        List<Claim> claims = CLAIMS.get(playerId);
        if (claims == null || claims.isEmpty()) {
            return;
        }
        Set<UUID> retain = keep == null ? Set.of() : keep;
        synchronized (claims) {
            Iterator<Claim> it = claims.iterator();
            while (it.hasNext()) {
                Claim c = it.next();
                if (!retain.contains(c.mobId)) {
                    MOB_OWNER.remove(c.mobId, playerId);
                    PENDING_REVERT.put(c.mobId, playerId);
                    it.remove();
                }
            }
        }
    }

    /** Drop all claims for a player and queue every mob for revert (death / personal off / logout). */
    public static void releaseAndRevertPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        clearPlayer(player.m_20148_());
    }

    /**
     * Drop every player's claims and queue all claimed mobs for revert.
     * Used when master Adaptive Difficulty / mob scaling is turned off.
     */
    public static void releaseAllPlayers() {
        if (CLAIMS.isEmpty()) {
            return;
        }
        for (UUID playerId : List.copyOf(CLAIMS.keySet())) {
            clearPlayer(playerId);
        }
    }

    /** True when this mob UUID is waiting for a base-stat revert (may be unloaded). */
    public static boolean isPendingRevert(UUID mobId) {
        return mobId != null && PENDING_REVERT.containsKey(mobId);
    }

    /** Remove a pending eviction after a successful revert (or death cleanup). */
    public static void clearPendingRevert(UUID mobId) {
        if (mobId != null) {
            PENDING_REVERT.remove(mobId);
        }
    }

    public static void release(ServerPlayer player, UUID mobId) {
        if (player == null || mobId == null) {
            return;
        }
        UUID playerId = player.m_20148_();
        List<Claim> claims = CLAIMS.get(playerId);
        if (claims == null) {
            return;
        }
        synchronized (claims) {
            if (claims.removeIf(c -> c.mobId.equals(mobId))) {
                MOB_OWNER.remove(mobId, playerId);
                PENDING_REVERT.put(mobId, playerId);
            }
        }
    }

    /** Drop every claim + pending eviction for this mob (used when a mob becomes exempt). */
    public static void releaseAllClaims(Mob mob) {
        if (mob == null) {
            return;
        }
        UUID mobId = mob.m_20148_();
        PENDING_REVERT.remove(mobId);
        MOB_OWNER.remove(mobId);
        for (List<Claim> claims : CLAIMS.values()) {
            synchronized (claims) {
                claims.removeIf(c -> c.mobId.equals(mobId));
            }
        }
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        List<Claim> claims = CLAIMS.remove(playerId);
        if (claims == null) {
            return;
        }
        synchronized (claims) {
            for (Claim c : claims) {
                MOB_OWNER.remove(c.mobId, playerId);
                PENDING_REVERT.put(c.mobId, playerId);
            }
            claims.clear();
        }
    }

    public static UUID findClaimOwnerId(UUID mobId) {
        return mobId == null ? null : MOB_OWNER.get(mobId);
    }

    public static void forEachClaimed(MinecraftServer server, BiConsumer<ServerPlayer, Mob> consumer) {
        if (server == null || consumer == null || CLAIMS.isEmpty()) {
            return;
        }
        var players = server.m_6846_();
        for (Map.Entry<UUID, List<Claim>> e : CLAIMS.entrySet()) {
            ServerPlayer owner = players.m_11259_(e.getKey());
            if (owner == null || !owner.m_6084_()) {
                continue;
            }
            forEachClaimedMob(owner, mob -> consumer.accept(owner, mob));
        }
    }

    /** Visit loaded hostiles currently claimed by {@code player}. */
    public static void forEachClaimedMob(ServerPlayer player, Consumer<Mob> consumer) {
        if (player == null || consumer == null) {
            return;
        }
        List<Claim> claims = CLAIMS.get(player.m_20148_());
        if (claims == null || claims.isEmpty()) {
            return;
        }
        List<UUID> mobIds;
        synchronized (claims) {
            mobIds = new ArrayList<>(claims.size());
            for (Claim c : claims) {
                mobIds.add(c.mobId);
            }
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        ServerLevel prefer = player.m_9236_() instanceof ServerLevel sl ? sl : null;
        for (UUID mobId : mobIds) {
            Mob mob = findMob(server, prefer, mobId);
            if (mob != null && mob.m_6084_()) {
                consumer.accept(mob);
            }
        }
    }

    public static UUID pollEvictedOwner(UUID mobId) {
        return mobId == null ? null : PENDING_REVERT.remove(mobId);
    }

    /**
     * Snapshot of pending reverts. Entries stay queued until
     * {@link #clearPendingRevert} after a successful revert — unloaded mobs
     * are retried on later pulses / chunk load instead of becoming scaled orphans.
     */
    public static Map<UUID, UUID> peekEvictions() {
        if (PENDING_REVERT.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(PENDING_REVERT);
    }

    /** @deprecated use {@link #peekEvictions()} + {@link #clearPendingRevert(UUID)} */
    @Deprecated
    public static Map<UUID, UUID> drainEvictions() {
        return peekEvictions();
    }

    private static boolean isOwnerOnline(UUID ownerId) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return false;
        }
        ServerPlayer p = server.m_6846_().m_11259_(ownerId);
        return p != null && p.m_6084_();
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
        UUID playerId = preferPlayer == null ? null : preferPlayer.m_20148_();
        Iterator<Claim> it = claims.iterator();
        while (it.hasNext()) {
            Claim c = it.next();
            Mob mob = findMob(server, preferLevel, c.mobId);
            if (mob == null || !mob.m_6084_()) {
                if (playerId != null) {
                    MOB_OWNER.remove(c.mobId, playerId);
                } else {
                    MOB_OWNER.remove(c.mobId);
                }
                it.remove();
            }
        }
    }

    private static Mob findMob(MinecraftServer server, ServerLevel preferLevel, UUID id) {
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
            var entity = level.m_8791_(id);
            if (entity instanceof Mob mob) {
                return mob;
            }
        }
        return null;
    }

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
