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
 * Mohist/DMZ can leave Forge {@code ENTITY_REACH}/{@code BLOCK_REACH} in a bad state
 * (NaN base, collapsed base, non-finite modifiers). DMZ melee then computes
 * {@code getEffectiveAttackRange} as NaN/0 and silently drops every hit until
 * death recreates the AttributeMap.
 */
public final class ReachAttributeFix {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final AtomicInteger LOGS = new AtomicInteger();

    private ReachAttributeFix() {}

    public static boolean repair(Player player, String reason) {
        if (player == null) {
            return false;
        }
        boolean changed = false;
        try {
            Attribute entityReach = ForgeMod.ENTITY_REACH.get();
            Attribute blockReach = ForgeMod.BLOCK_REACH.get();
            if (entityReach != null) {
                changed |= repairOne(player, entityReach, "ENTITY_REACH", reason);
            }
            if (blockReach != null) {
                changed |= repairOne(player, blockReach, "BLOCK_REACH", reason);
            }
        } catch (Throwable t) {
            if (LOGS.get() < 5) {
                LOGGER.warn("[{}] reach repair failed: {}", DmzMohistMeleeFix.MOD_ID, t.toString());
            }
        }
        return changed;
    }

    private static boolean repairOne(Player player, Attribute attribute, String label, String reason) {
        AttributeInstance inst = player.m_21051_(attribute);
        if (inst == null) {
            int n = LOGS.incrementAndGet();
            if (n <= 20) {
                LOGGER.warn(
                        "[{}] missing {} attribute player={} reason={}",
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
            def = "ENTITY_REACH".equals(label) ? 3.0D : 4.5D;
        }

        double base = inst.m_22115_();
        if (!Double.isFinite(base) || base < 0.25D) {
            inst.m_22100_(def);
            changed = true;
        }

        List<UUID> badMods = new ArrayList<>();
        for (AttributeModifier mod : inst.m_22122_()) {
            double amt = mod.m_22218_();
            if (!Double.isFinite(amt)) {
                badMods.add(mod.m_22209_());
            }
        }
        for (UUID id : badMods) {
            inst.m_22120_(id);
            changed = true;
        }

        double value = inst.m_22135_();
        if (!Double.isFinite(value) || value < 0.25D) {
            // Nuclear: strip all modifiers and restore default base (form reach reapplied by DMZ tick).
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
                        base,
                        inst.m_22115_(),
                        value,
                        reason
                );
            }
        }
        return changed;
    }

    /**
     * Sanitize DMZ range: if reach math produced NaN/non-positive, repair attrs and
     * fall back to a usable melee range so hits are not silently dropped.
     */
    public static double sanitizeEffectiveRange(Player player, double weaponAttackRange, double computed) {
        if (Double.isFinite(computed) && computed > 0.05D) {
            return computed;
        }
        repair(player, "sanitize-range");
        double fallback = Math.max(2.0D, weaponAttackRange > 0.0D ? weaponAttackRange : 2.0D);
        // Prefer live attribute if repair restored it.
        try {
            Attribute entityReach = ForgeMod.ENTITY_REACH.get();
            if (entityReach != null) {
                AttributeInstance inst = player.m_21051_(entityReach);
                if (inst != null) {
                    double cur = inst.m_22135_();
                    double def = entityReach.m_22082_();
                    if (Double.isFinite(cur) && Double.isFinite(def)) {
                        double fixed = weaponAttackRange + (cur - def);
                        if (Double.isFinite(fixed) && fixed > 0.05D) {
                            return fixed;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        int n = LOGS.incrementAndGet();
        if (n <= 30) {
            LOGGER.info(
                    "[{}] sanitized attack range player={} weapon={} computed={} -> {}",
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
