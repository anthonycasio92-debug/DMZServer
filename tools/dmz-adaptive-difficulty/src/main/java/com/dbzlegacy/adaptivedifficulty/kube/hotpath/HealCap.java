package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Hard-cap AttributesLib healing values. Same UUIDs and MULTIPLY_TOTAL math as lifesteal_cap.js.
 */
public final class HealCap {
    public static final int TICK_INTERVAL = 20;

    private static final Cap[] CAPS = {
        new Cap("attributeslib:life_steal", 0.0, "dbz_lifesteal_cap",
                UUID.fromString("c4a1e8d2-5b73-4f0a-9d6e-2f8b1a0c7e55")),
        new Cap("attributeslib:overheal", 0.05, "dbz_overheal_cap",
                UUID.fromString("d5b2f9e3-6c84-501b-ae7f-309c2b1d8f66")),
        new Cap("attributeslib:healing_received", 1.05, "dbz_healing_received_cap",
                UUID.fromString("e6c3a0f4-7d95-612c-bf80-41ad3c2e9077"))
    };

    private HealCap() {}

    public static void clamp(ServerPlayer player) {
        if (player == null) {
            return;
        }
        for (Cap cap : CAPS) {
            try {
                clampOne(player, cap);
            } catch (Throwable t) {
                HotLog.debug("[Heal Cap] " + cap.attrId + " failed: " + t);
            }
        }
    }

    private static void clampOne(ServerPlayer player, Cap cap) {
        Attribute attribute = cap.attribute();
        if (attribute == null) {
            return;
        }
        AttributeInstance inst = player.m_21051_(attribute);
        if (inst == null) {
            return;
        }
        try {
            inst.m_22120_(cap.uuid);
        } catch (Throwable ignored) {
        }
        double value = inst.m_22135_();
        double amount = multiplyCapAmount(value, cap.max);
        if (amount == 0.0) {
            return;
        }
        AttributeModifier mod = new AttributeModifier(
                cap.uuid, cap.name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL);
        try {
            inst.m_22118_(mod);
        } catch (Throwable transientFailed) {
            inst.m_22125_(mod);
        }
    }

    static double multiplyCapAmount(double value, double max) {
        if (!(value > max) || !(value > 0.0) || !Double.isFinite(value)) {
            return 0.0;
        }
        return max / value - 1.0;
    }

    private static final class Cap {
        final String attrId;
        final double max;
        final String name;
        final UUID uuid;
        private Attribute resolved;
        private boolean looked;

        Cap(String attrId, double max, String name, UUID uuid) {
            this.attrId = attrId;
            this.max = max;
            this.name = name;
            this.uuid = uuid;
        }

        Attribute attribute() {
            if (looked) {
                return resolved;
            }
            looked = true;
            int colon = attrId.indexOf(':');
            resolved = ForgeRegistries.ATTRIBUTES.getValue(
                    new ResourceLocation(attrId.substring(0, colon), attrId.substring(colon + 1)));
            return resolved;
        }
    }
}
