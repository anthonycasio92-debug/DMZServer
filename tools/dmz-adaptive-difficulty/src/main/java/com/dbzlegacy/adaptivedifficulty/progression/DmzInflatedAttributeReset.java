package com.dbzlegacy.adaptivedifficulty.progression;

import com.dragonminez.common.init.MainAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

/**
 * One login pass for bases that 4.6.3 wrote into player NBT.
 * Does not reconcile on a timer. A normal base is left alone.
 */
public final class DmzInflatedAttributeReset {
    /** Registered default is 20, so this flags bases above 2,000. */
    private static final double INFLATED_BASE_MULTIPLE = 100d;

    private DmzInflatedAttributeReset() {}

    public static void onLogin(ServerPlayer player) {
        if (player == null) {
            return;
        }
        resetIfInflated(player, true);
        resetIfInflated(player, false);
    }

    private static void resetIfInflated(ServerPlayer player, boolean energy) {
        try {
            AttributeInstance inst = player.m_21051_(
                    (energy ? MainAttributes.MAX_ENERGY : MainAttributes.MAX_STAMINA).get());
            if (inst == null) {
                return;
            }
            double registered = inst.m_22099_().m_22082_();
            if (!Double.isFinite(registered) || registered <= 0d) {
                return;
            }
            double base = inst.m_22115_();
            if (!Double.isFinite(base) || base <= registered * INFLATED_BASE_MULTIPLE) {
                return;
            }
            inst.m_22100_(registered);
        } catch (Throwable ignored) {
        }
    }
}
