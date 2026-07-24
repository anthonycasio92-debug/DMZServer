package com.dbzlegacy.mohistmelee;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Repairs collapsed Forge {@code ENTITY_REACH} / {@code BLOCK_REACH}.
 *
 * <p>DMZ melee range is {@code weaponRange + (entityReach - default)}. When reach
 * base/modifiers become NaN or collapse on Mohist, server range checks fail and
 * M1 does nothing until death rebuilds the AttributeMap.
 *
 * <p>This class only touches reach attributes — no damage redirects, no combat locks.
 */
public final class ReachAttributeFix {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final AtomicInteger LOGS = new AtomicInteger();

    private ReachAttributeFix() {}

    /** @return true if any attribute was changed */
    public static boolean repair(Player player, String reason) {
        if (player == null) {
            return false;
        }
        boolean changed = false;
        try {
            Attribute entityReach = ForgeMod.ENTITY_REACH.get();
            Attribute blockReach = ForgeMod.BLOCK_REACH.get();
            if (entityReach != null) {
                changed |= repairOne(player, entityReach, "ENTITY_REACH", 3.0D, reason);
            }
            if (blockReach != null) {
                changed |= repairOne(player, blockReach, "BLOCK_REACH", 4.5D, reason);
            }
        } catch (Throwable t) {
            if (LOGS.get() < 5) {
                LOGGER.warn("[{}] reach repair failed: {}", DmzMohistMeleeFix.MOD_ID, t.toString());
            }
        }
        return changed;
    }

    private static boolean repairOne(
            Player player,
            Attribute attribute,
            String label,
            double fallbackDefault,
            String reason
    ) {
        AttributeInstance inst = player.m_21051_(attribute);
        if (inst == null) {
            int n = LOGS.incrementAndGet();
            if (n <= 20) {
                LOGGER.warn(
                        "[{}] missing {} player={} reason={}",
                        DmzMohistMeleeFix.MOD_ID,
                        label,
                        playerName(player),
                        reason
                );
            }
            return false;
        }

        boolean changed = false;
        double def = attribute.m_22082_();
        if (!Double.isFinite(def) || def <= 0.0D) {
            def = fallbackDefault;
        }

        double oldBase = inst.m_22115_();
        if (!Double.isFinite(oldBase) || oldBase < 0.25D) {
            inst.m_22100_(def);
            changed = true;
        }

        List<UUID> badMods = new ArrayList<>();
        for (AttributeModifier mod : inst.m_22122_()) {
            if (!Double.isFinite(mod.m_22218_())) {
                badMods.add(mod.m_22209_());
            }
        }
        for (UUID id : badMods) {
            inst.m_22120_(id);
            changed = true;
        }

        double value = inst.m_22135_();
        if (!Double.isFinite(value) || value < 0.25D) {
            // Last resort: clear modifiers and restore default base.
            // DMZ form-reach tick will re-apply its bonus afterward.
            inst.m_22132_();
            inst.m_22100_(def);
            changed = true;
            value = inst.m_22135_();
        }

        if (changed) {
            int n = LOGS.incrementAndGet();
            if (n <= 40) {
                LOGGER.info(
                        "[{}] repaired {} player={} base={}->{} value={} reason={}",
                        DmzMohistMeleeFix.MOD_ID,
                        label,
                        playerName(player),
                        oldBase,
                        inst.m_22115_(),
                        value,
                        reason
                );
            }
        }
        return changed;
    }

    /**
     * If DMZ range math produced NaN / non-positive, repair attrs and return a
     * usable range. Does not alter damage application.
     */
    public static double sanitizeEffectiveRange(Player player, double weaponAttackRange, double computed) {
        if (Double.isFinite(computed) && computed > 0.05D) {
            return computed;
        }
        repair(player, "sanitize-range");

        try {
            Attribute entityReach = ForgeMod.ENTITY_REACH.get();
            if (entityReach != null) {
                AttributeInstance inst = player.m_21051_(entityReach);
                if (inst != null) {
                    double cur = inst.m_22135_();
                    double def = entityReach.m_22082_();
                    if (!Double.isFinite(def) || def <= 0.0D) {
                        def = 3.0D;
                    }
                    if (Double.isFinite(cur)) {
                        double fixed = weaponAttackRange + (cur - def);
                        if (Double.isFinite(fixed) && fixed > 0.05D) {
                            return fixed;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        double fallback = Math.max(2.0D, weaponAttackRange > 0.0D ? weaponAttackRange : 2.0D);
        int n = LOGS.incrementAndGet();
        if (n <= 30) {
            LOGGER.info(
                    "[{}] sanitized range player={} weapon={} computed={} -> {}",
                    DmzMohistMeleeFix.MOD_ID,
                    playerName(player),
                    weaponAttackRange,
                    computed,
                    fallback
            );
        }
        return fallback;
    }

    public static double readEntityReach(Player player) {
        try {
            Attribute attr = ForgeMod.ENTITY_REACH.get();
            if (attr == null) {
                return Double.NaN;
            }
            AttributeInstance inst = player.m_21051_(attr);
            return inst == null ? Double.NaN : inst.m_22135_();
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static String playerName(Player player) {
        if (player instanceof ServerPlayer sp) {
            return sp.m_36316_().getName();
        }
        return String.valueOf(player.m_20148_());
    }
}
