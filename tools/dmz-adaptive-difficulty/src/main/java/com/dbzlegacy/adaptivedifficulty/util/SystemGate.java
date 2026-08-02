package com.dbzlegacy.adaptivedifficulty.util;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Master on/off + optional testing whitelist + per-player participation.
 * <p>
 * When whitelist mode is on, only listed players may use Adaptive Difficulty
 * (purchases, scaling driven by them, kill rewards, death reset).
 * <p>
 * {@link #allows} = admin gates (GUI / purchases).<br>
 * {@link #participates} = allows + the player's personal difficulty toggle.
 */
public final class SystemGate {
    private SystemGate() {}

    public static boolean isEnabled() {
        return DifficultyConfig.isEnabled();
    }

    public static boolean isDisabled() {
        return !isEnabled();
    }

    /** True when this player may use the system right now (admin gates). */
    public static boolean allows(ServerPlayer player) {
        if (!isEnabled() || player == null) {
            return false;
        }
        return DifficultyConfig.isPlayerAllowed(player);
    }

    public static boolean allows(Player player) {
        if (player instanceof ServerPlayer sp) {
            return allows(sp);
        }
        return false;
    }

    /**
     * True when this player's personal difficulty is on — scaling, kill coins,
     * death reset, AI pressure, and tier buy/lower driven by them.
     */
    public static boolean participates(ServerPlayer player) {
        if (!allows(player)) {
            return false;
        }
        return DifficultyCache.data(player).isPersonalEnabled();
    }

    public static boolean participates(Player player) {
        if (player instanceof ServerPlayer sp) {
            return participates(sp);
        }
        return false;
    }

    /** True if any involved entity is an allowed player. */
    public static boolean allowsAny(Entity a, Entity b) {
        return allowsEntity(a) || allowsEntity(b);
    }

    private static boolean allowsEntity(Entity entity) {
        return entity instanceof ServerPlayer sp && allows(sp);
    }
}
