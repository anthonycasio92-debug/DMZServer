package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.BonusStats;
import com.dragonminez.common.stats.character.Stats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

/**
 * Future-proof vanilla attribute reconciler for the client HUD.
 *
 * <p>The client HUD ({@code XenoverseHUD}) recomputes max ki/stamina via
 * {@code StatsData.getMaxEnergy()}/{@code getMaxStamina()}, which read the
 * secondary value from the vanilla {@code MAX_ENERGY}/{@code MAX_STAMINA}
 * attribute instances. Vanilla syncs those attributes to the client
 * automatically — but no mod updates them when the effective max changes
 * (class change, dmzrevamp prestige, apothicdmz bridges, ...), so the HUD
 * renders a stale max (e.g. 450,020 / 285,020).
 *
 * <p>This reconciler reverse-engineers the secondary attribute value that
 * produces the server's computed max and writes it to the vanilla attribute.
 * It is mod-agnostic: any drift from any source is corrected within one
 * poll interval. Writes only happen on drift beyond epsilon, so vanilla
 * sync traffic is negligible.
 */
public final class DmzVanillaAttributeSync {
    private DmzVanillaAttributeSync() {
    }

    /** Skip writes below this delta to avoid spamming vanilla attribute syncs. */
    private static final double EPSILON = 0.5d;

    /**
     * Reconcile both attributes for one player. Cheap and idempotent —
     * safe to call every tick; only writes on drift.
     */
    public static void reconcile(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            StatsData data = DmzProgression.stats(player);
            if (data == null) {
                return;
            }
            syncOne(player, data, true);
            syncOne(player, data, false);
        } catch (Throwable ignored) {
        }
    }

    private static void syncOne(ServerPlayer player, StatsData data, boolean energy) {
        try {
            float targetMax = energy ? data.getMaxEnergy() : data.getMaxStamina();
            if (!Float.isFinite(targetMax) || targetMax <= 0f) {
                return;
            }
            Stats stats = data.getStats();
            if (stats == null) {
                return;
            }
            String key = energy ? "ENE" : "STM";
            int invested = energy ? stats.getEnergy() : stats.getResistance();
            double scaling = finitePositive(data.getStatScaling(key), 1.0d);
            double totalMult = finitePositive(data.getTotalMultiplier(key), 1.0d);
            double bonusMult = 0.0d;
            double bonusAdd = 0.0d;
            try {
                BonusStats bonus = data.getBonusStats();
                if (bonus != null) {
                    bonusMult = finiteOrZero(bonus.calculateBonus(key, invested, true));
                    bonusAdd = finiteOrZero(bonus.calculateBonus(key, invested, false));
                }
            } catch (Throwable ignored) {
            }
            // Rearranged from DMZ's formula:
            //   max = secondary + (invested + bonusMult) * scaling * totalMult + bonusAdd * scaling
            double targetSecondary = targetMax
                    - (invested + bonusMult) * scaling * totalMult
                    - bonusAdd * scaling;
            if (!Double.isFinite(targetSecondary) || targetSecondary < 0d) {
                return;
            }
            AttributeInstance inst;
            try {
                inst = player.m_21051_(
                        (energy ? MainAttributes.MAX_ENERGY : MainAttributes.MAX_STAMINA).get());
            } catch (Throwable t) {
                return;
            }
            if (inst == null) {
                return;
            }
            // getMaxEnergy()/getMaxStamina() read getValue() via getSecondaryAttributeValue.
            // Comparing against getBaseValue() stacks modifiers into the base every pass.
            double currentValue;
            try {
                currentValue = inst.m_22135_();
            } catch (Throwable t) {
                return;
            }
            if (!Double.isFinite(currentValue) || Math.abs(currentValue - targetSecondary) <= EPSILON) {
                return;
            }
            // Shift the base by the drift so existing modifiers stay in place.
            double newBase;
            try {
                newBase = inst.m_22115_() + (targetSecondary - currentValue);
            } catch (Throwable t) {
                return;
            }
            if (!Double.isFinite(newBase) || newBase < 0d) {
                return;
            }
            inst.m_22100_(newBase);
        } catch (Throwable ignored) {
        }
    }

    private static double finitePositive(double v, double fallback) {
        return Double.isFinite(v) && v > 0d ? v : fallback;
    }

    private static double finiteOrZero(double v) {
        return Double.isFinite(v) ? v : 0d;
    }
}
