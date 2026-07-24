package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.init.MainAttributes;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Empty-hand / fist melee uses {@code Stats.getMeleeDamage()} → {@code Stats.getStrength()},
 * which reads the {@code dragonminez:strength} AttributeInstance base on the player.
 * Held non-DMZ items skip that path in {@code CombatEvent.onLivingHurt} and keep vanilla damage.
 * <p>
 * On Mohist, cross-dimension teleports (raid arenas) can reset custom attribute bases to their
 * registry defaults (0 for primary stats). Snapshot last-known-good primaries and restore them
 * after dim/teleport. Does <b>not</b> write {@code ki_damage} / melee / strike damage attrs.
 */
public final class PrimaryStatRepair {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final AtomicInteger LOGS = new AtomicInteger();
    /** uuid -> [STR, SKP, RES, VIT, PWR, ENE] last-known-good bases */
    private static final Map<UUID, int[]> SNAPSHOTS = new ConcurrentHashMap<>();

    private static final String[] KEYS = {"STR", "SKP", "RES", "VIT", "PWR", "ENE"};

    @SuppressWarnings("unchecked")
    private static final RegistryObject<Attribute>[] ATTRS = new RegistryObject[] {
            MainAttributes.STRENGTH,
            MainAttributes.STRIKE_POWER,
            MainAttributes.RESISTANCE,
            MainAttributes.VITALITY,
            MainAttributes.KI_POWER,
            MainAttributes.ENERGY
    };

    private PrimaryStatRepair() {}

    /** Cache current primary bases when they look valid (> 0). */
    public static void snapshot(Player player) {
        if (player == null) {
            return;
        }
        try {
            int[] saved = SNAPSHOTS.getOrDefault(player.m_20148_(), new int[ATTRS.length]);
            int[] next = Arrays.copyOf(saved, ATTRS.length);
            boolean any = false;
            for (int i = 0; i < ATTRS.length; i++) {
                Attribute attr = ATTRS[i].get();
                if (attr == null) {
                    continue;
                }
                AttributeInstance inst = player.m_21051_(attr);
                if (inst == null) {
                    continue;
                }
                double base = inst.m_22115_();
                if (Double.isFinite(base) && base > 0.0D) {
                    next[i] = (int) Math.round(base);
                    any = true;
                }
            }
            if (any) {
                SNAPSHOTS.put(player.m_20148_(), next);
            }
        } catch (Throwable t) {
            if (LOGS.get() < 5) {
                LOGGER.warn("[{}] primary snapshot failed: {}", DmzMohistMeleeFix.MOD_ID, t.toString());
            }
        }
    }

    /**
     * Re-inject missing primary AttributeInstances and restore bases wiped to 0/NaN after
     * cross-dimension teleports. Never touches secondary damage attributes.
     *
     * @return true if any attribute was changed
     */
    public static boolean restore(Player player, String reason) {
        if (player == null) {
            return false;
        }
        boolean changed = false;
        try {
            int[] saved = SNAPSHOTS.get(player.m_20148_());
            if (saved == null) {
                return false;
            }
            for (int i = 0; i < ATTRS.length; i++) {
                Attribute attr = ATTRS[i].get();
                if (attr == null || saved[i] <= 0) {
                    continue;
                }
                AttributeInstance inst = player.m_21051_(attr);
                if (inst == null) {
                    inst = injectAttribute(player, attr, saved[i]);
                    if (inst != null) {
                        changed = true;
                        logRestore(player, KEYS[i], Double.NaN, saved[i], reason + "-inject");
                    }
                    continue;
                }
                double base = inst.m_22115_();
                if (!Double.isFinite(base) || base <= 0.0D) {
                    inst.m_22100_((double) saved[i]);
                    changed = true;
                    logRestore(player, KEYS[i], base, saved[i], reason);
                }
            }
        } catch (Throwable t) {
            if (LOGS.get() < 5) {
                LOGGER.warn("[{}] primary restore failed: {}", DmzMohistMeleeFix.MOD_ID, t.toString());
            }
        }
        return changed;
    }

    /** Snapshot then restore — used on dim change / teleport / melee packet. */
    public static boolean ensure(Player player, String reason) {
        snapshot(player);
        return restore(player, reason);
    }

    public static void clear(UUID id) {
        if (id != null) {
            SNAPSHOTS.remove(id);
        }
    }

    @SuppressWarnings("unchecked")
    static AttributeInstance injectAttribute(Player player, Attribute attribute, double base) {
        try {
            Object map = player.m_21204_();
            java.lang.reflect.Field field = null;
            Class<?> c = map.getClass();
            while (c != null && field == null) {
                try {
                    field = c.getDeclaredField("f_22139_");
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            Object raw = field.get(map);
            if (!(raw instanceof java.util.Map<?, ?>)) {
                return null;
            }
            java.util.Map<Attribute, AttributeInstance> instances =
                    (java.util.Map<Attribute, AttributeInstance>) raw;
            AttributeInstance created = new AttributeInstance(attribute, ignored -> {});
            created.m_22100_(base);
            instances.put(attribute, created);
            return created;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void logRestore(Player player, String key, double from, int to, String reason) {
        int n = LOGS.incrementAndGet();
        if (n <= 40) {
            String name = player instanceof ServerPlayer sp
                    ? sp.m_36316_().getName()
                    : String.valueOf(player.m_20148_());
            LOGGER.info(
                    "[{}] restored primary {} player={} base={}->{} reason={}",
                    DmzMohistMeleeFix.MOD_ID,
                    key,
                    name,
                    from,
                    to,
                    reason
            );
        }
    }
}
