package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    /** uuid -> ticks remaining until next follow-up force unlock */
    private static final Map<UUID, Integer> FOLLOWUP_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> FOLLOWUP_REASON = new ConcurrentHashMap<>();
    /** when true, after the +5t follow-up schedule another at +15t (total +20 from teleport) */
    private static final Map<UUID, Boolean> FOLLOWUP_NEED_SECOND = new ConcurrentHashMap<>();

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
            logClear(player, reason);
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
        clearChargeFlags(player);
        clearStunPotion(player);
        sync(player);
    }

    /**
     * Raid / mod teleports: abort leftover strike maps, force-clear strikeLocked even if
     * ACTIVE still references this player, repair attributes, and schedule short follow-ups
     * (Shurui {@code fullHeal} sync can race the first unlock).
     */
    public static void unlockAfterTeleport(ServerPlayer player, String reason) {
        if (player == null || player.m_9236_().f_46443_) {
            return;
        }
        forceUnlockCombat(player, reason);
        FOLLOWUP_TICKS.put(player.m_20148_(), 5);
        FOLLOWUP_REASON.put(player.m_20148_(), reason);
        FOLLOWUP_NEED_SECOND.put(player.m_20148_(), Boolean.TRUE);
    }

    /** Called from player tick for delayed post-teleport unlocks. */
    public static void tickFollowups(ServerPlayer player) {
        UUID id = player.m_20148_();
        Integer left = FOLLOWUP_TICKS.get(id);
        if (left == null) {
            return;
        }
        if (left <= 1) {
            String reason = FOLLOWUP_REASON.getOrDefault(id, "teleport-followup");
            forceUnlockCombat(player, reason + "-followup");
            if (Boolean.TRUE.equals(FOLLOWUP_NEED_SECOND.remove(id))) {
                FOLLOWUP_TICKS.put(id, 15);
                FOLLOWUP_REASON.put(id, reason);
            } else {
                FOLLOWUP_TICKS.remove(id);
                FOLLOWUP_REASON.remove(id);
            }
            return;
        }
        FOLLOWUP_TICKS.put(id, left - 1);
    }

    public static void forceUnlockCombat(ServerPlayer player, String reason) {
        abortStrikeMaps(player);
        boolean[] cleared = {false};
        StatsProvider.get(StatsCapability.INSTANCE, (Entity) player).ifPresent(stats -> {
            boolean locked = stats.getStatus().isStrikeLocked()
                    || stats.getStatus().isKnockedDown()
                    || stats.getStatus().isStunEffect();
            stats.getStatus().setStrikeLocked(false);
            stats.getStatus().setKnockedDown(false);
            stats.getStatus().setStunEffect(false);
            cleared[0] = locked;
        });
        if (cleared[0]) {
            logClear(player, reason);
        }
        ReachAttributeFix.repair(player, reason);
        clearChargeFlags(player);
        clearStunPotion(player);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), (Entity) player);
        } catch (Throwable ignored) {
        }
    }

    private static void clearChargeFlags(ServerPlayer player) {
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
    }

    private static void clearStunPotion(ServerPlayer player) {
        try {
            MobEffect stun = MainEffects.STUN.get();
            if (stun != null) {
                player.m_21195_(stun);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void logClear(ServerPlayer player, String reason) {
        int n = LOGS.incrementAndGet();
        if (n <= 80) {
            LOGGER.info(
                    "[{}] cleared stale strike lock player={} reason={}",
                    DmzMohistMeleeFix.MOD_ID,
                    player.m_36316_().getName(),
                    reason
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static void abortStrikeMaps(ServerPlayer player) {
        try {
            Class<?> cls = Class.forName("com.dragonminez.server.events.players.combat.StrikeAttackHandler");
            UUID id = player.m_20148_();
            for (String fieldName : new String[] {"ACTIVE", "PENDING", "STRIKE_ANCHOR_PART"}) {
                Field field = cls.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object map = field.get(null);
                if (map instanceof Map<?, ?> m) {
                    ((Map<Object, Object>) m).remove(id);
                }
            }
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
