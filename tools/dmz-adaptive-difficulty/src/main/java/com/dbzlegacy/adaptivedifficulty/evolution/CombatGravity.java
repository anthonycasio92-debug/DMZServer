package com.dbzlegacy.adaptivedifficulty.evolution;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dragonminez.server.util.GravityDeviceManager;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/**
 * Stacks DMZ gravity-chamber pressure on players from aggro'd hostiles.
 * <p>
 * {@link GravityDeviceManager#getGravityFor} takes the <b>max</b> of overlapping
 * zones, so we register <b>one zone per player</b> whose gravity equals the
 * sum of all active contributions (more aggro'd Endermen → heavier gravity).
 */
public final class CombatGravity {
    private static final Map<UUID, Map<UUID, Contribution>> BY_PLAYER = new ConcurrentHashMap<>();

    private CombatGravity() {}

    /**
     * Refresh a source's gravity contribution (e.g. one Enderman's UUID).
     * {@code ttlTicks} keeps the pressure alive between staggered mob ticks.
     */
    public static void contribute(ServerPlayer player, UUID sourceId, double gravity, int ttlTicks) {
        if (player == null || sourceId == null || gravity <= 0.0 || player.m_9236_().f_46443_) {
            return;
        }
        long expire = player.f_19797_ + Math.max(10, ttlTicks);
        BY_PLAYER
                .computeIfAbsent(player.m_20148_(), id -> new ConcurrentHashMap<>())
                .put(sourceId, new Contribution(gravity, expire));
    }

    /** Apply / clear the player's combat gravity zone. Call every player tick. */
    public static void tickPlayer(ServerPlayer player) {
        if (player == null || player.m_9236_().f_46443_) {
            return;
        }
        Map<UUID, Contribution> sources = BY_PLAYER.get(player.m_20148_());
        double total = 0.0;
        if (sources != null && !sources.isEmpty()) {
            long now = player.f_19797_;
            Iterator<Map.Entry<UUID, Contribution>> it = sources.entrySet().iterator();
            while (it.hasNext()) {
                Contribution c = it.next().getValue();
                if (c.expireTick < now) {
                    it.remove();
                } else {
                    total += c.gravity;
                }
            }
            if (sources.isEmpty()) {
                BY_PLAYER.remove(player.m_20148_(), sources);
            }
        }

        BlockPos key = keyFor(player);
        try {
            if (total <= 0.05) {
                GravityDeviceManager.unregister(player.m_9236_(), key);
                return;
            }
            // Cap below device max (1000) so combat pressure stays playable.
            double gravity = Math.min(400.0, total);
            AABB box = player.m_20191_().m_82400_(1.5);
            GravityDeviceManager.register(player.m_9236_(), key, box, gravity);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] combat gravity failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        BY_PLAYER.remove(player.m_20148_());
        try {
            GravityDeviceManager.unregister(player.m_9236_(), keyFor(player));
        } catch (Throwable ignored) {
        }
    }

    /** Stable fake BlockPos so combat zones never collide with real gravity devices. */
    private static BlockPos keyFor(ServerPlayer player) {
        UUID id = player.m_20148_();
        int x = id.hashCode();
        int z = (int) (id.getMostSignificantBits() ^ id.getLeastSignificantBits());
        return new BlockPos(x, 512, z);
    }

    private record Contribution(double gravity, long expireTick) {}
}
