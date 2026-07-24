package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Unlocks DMZ combat gates that silently drop M1 packets.
 * Does <b>not</b> redirect damage — vanilla {@code ServerPlayer.attack} stays intact
 * so CustomNPCs / mob deaths keep working.
 */
public final class CombatUnlock {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final AtomicInteger LOGS = new AtomicInteger();

    private CombatUnlock() {}

    /**
     * Clear stale strikeLocked/knockedDown when the player is not in an ACTIVE strike.
     * Real STUN potion is left alone (checked separately).
     */
    public static boolean clearStaleStrikeLock(ServerPlayer player, String reason) {
        boolean[] cleared = {false};
        StatsProvider.get(StatsCapability.INSTANCE, (Entity) player).ifPresent(stats -> {
            boolean locked = stats.getStatus().isStrikeLocked() || stats.getStatus().isKnockedDown();
            if (!locked) {
                return;
            }
            if (isInActiveStrike(player)) {
                return;
            }
            stats.getStatus().setStrikeLocked(false);
            stats.getStatus().setKnockedDown(false);
            stats.getStatus().setStunEffect(false);
            cleared[0] = true;
        });
        if (cleared[0]) {
            int n = LOGS.incrementAndGet();
            if (n <= 40) {
                LOGGER.info(
                        "[{}] cleared stale strike lock player={} reason={}",
                        DmzMohistMeleeFix.MOD_ID,
                        player.m_36316_().getName(),
                        reason
                );
            }
            sync(player);
        }
        return cleared[0];
    }

    /** True only for the STUN mob effect — not strikeLocked. */
    public static boolean hasRealStunPotion(ServerPlayer player) {
        try {
            MobEffect stun = MainEffects.STUN.get();
            return stun != null && player.m_21023_(stun);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Join/respawn recovery: unlock gates + clear stuck technique charge flags and sync
     * so the client stops cancelling M1. No entity recreate, no damage changes.
     */
    public static void unlockForLogin(ServerPlayer player, String reason) {
        clearStaleStrikeLock(player, reason);
        ReachAttributeFix.repair(player, reason);
        StatsProvider.get(StatsCapability.INSTANCE, (Entity) player).ifPresent(data -> {
            try {
                if (data.getTechniques().isTechniqueCharging() || data.getTechniques().isTechniqueChargeActive()) {
                    data.getTechniques().clearTechniqueCharge();
                }
                data.getStatus().setChargingKi(false);
                data.getStatus().setActionCharging(false);
            } catch (Throwable ignored) {
            }
        });
        try {
            MobEffect stun = MainEffects.STUN.get();
            if (stun != null) {
                player.m_21195_(stun);
            }
        } catch (Throwable ignored) {
        }
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), (Entity) player);
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean isInActiveStrike(ServerPlayer player) {
        try {
            Class<?> cls = Class.forName("com.dragonminez.server.events.players.combat.StrikeAttackHandler");
            Field active = cls.getDeclaredField("ACTIVE");
            active.setAccessible(true);
            Object map = active.get(null);
            return map instanceof Map<?, ?> m && m.containsKey(player.m_20148_());
        } catch (Throwable t) {
            return false;
        }
    }
}
