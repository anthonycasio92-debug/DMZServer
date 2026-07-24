package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mohist damage helpers:
 * <ul>
 *   <li>Ask Bukkit/WorldGuard if damage is denied without using the broken attack/hurt bridge</li>
 *   <li>Apply allowed damage via Forge LivingHurt + setHealth</li>
 *   <li>Repair attacker combat state after denied/cancelled hits (ki blast / no-PvP triggers)</li>
 * </ul>
 */
public final class DamageBridge {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final AtomicInteger GLOBAL_LOGS = new AtomicInteger();
    private static final Map<UUID, Integer> PLAYER_LOGS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_REPAIR_MS = new ConcurrentHashMap<>();

    private DamageBridge() {}

    public enum Result {
        APPLIED,
        DENIED_BUKKIT,
        CANCELLED_FORGE,
        ZERO_AMOUNT,
        ATTACK_CANCELLED
    }

    /**
     * Apply player-caused damage without calling attack/hurt/actuallyHurt.
     * Respects Bukkit cancels (WorldGuard no-PvP, etc.).
     */
    public static Result applyPlayerDamage(ServerPlayer attacker, LivingEntity target, DamageSource source, float seedAmount) {
        target.f_19802_ = 0;
        target.f_20916_ = 0;

        if (isBukkitDamageDenied(attacker, target, seedAmount)) {
            repairAttacker(attacker, "bukkitDenied");
            logHit(attacker, target, 0.0F, "bukkitDenied");
            return Result.DENIED_BUKKIT;
        }

        if (!ForgeHooks.onLivingAttack(target, source, seedAmount)) {
            repairAttacker(attacker, "livingAttackCancelled");
            logHit(attacker, target, 0.0F, "livingAttackCancelled");
            return Result.ATTACK_CANCELLED;
        }

        LivingHurtEvent hurtEvent = new LivingHurtEvent(target, source, seedAmount);
        if (MinecraftForge.EVENT_BUS.post(hurtEvent)) {
            repairAttacker(attacker, "livingHurtCancelled");
            logHit(attacker, target, 0.0F, "livingHurtCancelled");
            return Result.CANCELLED_FORGE;
        }
        float amount = hurtEvent.getAmount();
        if (amount <= 0.0F) {
            repairAttacker(attacker, "livingHurtZero");
            logHit(attacker, target, 0.0F, "livingHurtZero");
            return Result.ZERO_AMOUNT;
        }

        LivingDamageEvent damageEvent = new LivingDamageEvent(target, source, amount);
        if (MinecraftForge.EVENT_BUS.post(damageEvent)) {
            repairAttacker(attacker, "livingDamageCancelled");
            logHit(attacker, target, 0.0F, "livingDamageCancelled");
            return Result.CANCELLED_FORGE;
        }
        amount = damageEvent.getAmount();
        if (amount <= 0.0F) {
            repairAttacker(attacker, "livingDamageZero");
            logHit(attacker, target, 0.0F, "livingDamageZero");
            return Result.ZERO_AMOUNT;
        }

        float before = target.m_21223_();
        float next = Math.max(0.0F, before - amount);
        target.m_21153_(next);
        target.f_20917_ = 10;
        target.f_20916_ = 10;
        target.f_19802_ = 20;
        if (next <= 0.0F && target.m_6084_()) {
            target.m_6667_(source);
        }

        float delta = before - target.m_21223_();
        logHit(attacker, target, delta, "applied amount=" + amount);
        return Result.APPLIED;
    }

    /**
     * Clear stuck combat locks that cancelled ki/PvP can leave behind on Mohist.
     */
    public static void repairAttacker(ServerPlayer player, String reason) {
        long now = System.currentTimeMillis();
        Long prev = LAST_REPAIR_MS.put(player.m_20148_(), now);
        if (prev != null && now - prev < 250L) {
            return;
        }

        player.f_19802_ = 0;
        player.f_20916_ = 0;
        player.f_20917_ = 0;

        try {
            MobEffect stun = MainEffects.STUN.get();
            if (stun != null) {
                player.m_21195_(stun);
            }
        } catch (Throwable ignored) {
            // MainEffects may be unavailable during very early boot
        }

        StatsProvider.get(StatsCapability.INSTANCE, (Entity) player).ifPresent(data -> {
            data.getStatus().setStunEffect(false);
            data.getStatus().setStrikeLocked(false);
            data.getStatus().setKnockedDown(false);
            // Do not force-clear blocking — player may still hold block.
            try {
                if (data.getTechniques().isTechniqueCharging() || data.getTechniques().isTechniqueChargeActive()) {
                    // Only clear charge when repairing after a denied projectile/hit, not every melee.
                    if (reason != null && (reason.contains("bukkit") || reason.contains("ki") || reason.contains("Cancelled"))) {
                        data.getTechniques().clearTechniqueCharge();
                    }
                }
            } catch (Throwable ignored) {
            }
        });

        rebindBukkitHandle(player);

        int n = GLOBAL_LOGS.incrementAndGet();
        if (n <= 30) {
            LOGGER.info(
                    "[{}] repaired attacker={} reason={}",
                    DmzMohistMeleeFix.MOD_ID,
                    player.m_36316_().getName(),
                    reason
            );
        }
    }

    /**
     * Probe Bukkit EntityDamageByEntityEvent so WorldGuard no-PvP is respected
     * without going through Mohist's broken LivingEntity.hurt bridge.
     */
    public static boolean isBukkitDamageDenied(ServerPlayer attacker, LivingEntity target, float amount) {
        try {
            Method getBukkitEntity = findMethod(attacker.getClass(), "getBukkitEntity");
            if (getBukkitEntity == null) {
                getBukkitEntity = findMethod(Entity.class, "getBukkitEntity");
            }
            if (getBukkitEntity == null) {
                return false; // pure Forge — no Bukkit bridge
            }
            Object bukkitAttacker = getBukkitEntity.invoke(attacker);
            Object bukkitTarget = getBukkitEntity.invoke(target);
            if (bukkitAttacker == null || bukkitTarget == null) {
                return false;
            }

            Class<?> entityClass = Class.forName("org.bukkit.entity.Entity");
            Class<?> causeClass = Class.forName("org.bukkit.event.entity.EntityDamageEvent$DamageCause");
            Class<?> eventClass = Class.forName("org.bukkit.event.entity.EntityDamageByEntityEvent");
            Object cause = Enum.valueOf(causeClass.asSubclass(Enum.class), "ENTITY_ATTACK");

            Constructor<?> ctor;
            Object event;
            try {
                ctor = eventClass.getConstructor(entityClass, entityClass, causeClass, double.class);
                event = ctor.newInstance(bukkitAttacker, bukkitTarget, cause, (double) amount);
            } catch (NoSuchMethodException ex) {
                // Newer Paper API uses DamageSource — fall back to allowing Forge path.
                return false;
            }

            Object pluginManager = Class.forName("org.bukkit.Bukkit").getMethod("getPluginManager").invoke(null);
            pluginManager.getClass()
                    .getMethod("callEvent", Class.forName("org.bukkit.event.Event"))
                    .invoke(pluginManager, event);
            return (boolean) eventClass.getMethod("isCancelled").invoke(event);
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable t) {
            if (GLOBAL_LOGS.get() < 5) {
                LOGGER.warn("[{}] bukkit deny probe failed: {}", DmzMohistMeleeFix.MOD_ID, t.toString());
            }
            return false;
        }
    }

    private static void rebindBukkitHandle(ServerPlayer player) {
        try {
            Method getBukkitEntity = findMethod(player.getClass(), "getBukkitEntity");
            if (getBukkitEntity == null) {
                return;
            }
            Object craft = getBukkitEntity.invoke(player);
            if (craft == null) {
                return;
            }
            Method setHandle = findMethod(craft.getClass(), "setHandle", Entity.class);
            if (setHandle == null) {
                setHandle = findMethod(craft.getClass(), "setHandle", Object.class);
            }
            if (setHandle != null) {
                setHandle.invoke(craft, player);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        Class<?> c = type;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static void logHit(ServerPlayer player, LivingEntity living, float delta, String detail) {
        int global = GLOBAL_LOGS.incrementAndGet();
        int perPlayer = PLAYER_LOGS.merge(player.m_20148_(), 1, Integer::sum);
        if (global > 40 && perPlayer > 5) {
            return;
        }
        LOGGER.info(
                "[{}] hit player={} target={} delta={} {}",
                DmzMohistMeleeFix.MOD_ID,
                player.m_36316_().getName(),
                living.m_6095_().m_20675_(),
                delta,
                detail
        );
    }
}
