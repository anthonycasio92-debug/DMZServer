package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Backup caps after AttributesLib. Same limits as healing_received_cap_hook.js:
 * healing received 1.05, life steal 0, overheal 0.05.
 */
public final class HealEventCap {
    private static final double MAX_HEALING_RECEIVED = 1.05;
    private static final double MAX_OVERHEAL = 0.05;
    private static final double MAX_LIFE_STEAL = 0.0;

    private HealEventCap() {}

    public static void onHeal(LivingHealEvent event) {
        try {
            LivingEntity entity = event.getEntity();
            if (!(entity instanceof Player)) {
                return;
            }
            double factor = value(entity, "attributeslib:healing_received", 1.0);
            if (!(factor > MAX_HEALING_RECEIVED)) {
                return;
            }
            float amount = event.getAmount();
            if (!(amount > 0.0f)) {
                return;
            }
            event.setAmount((float) (amount * (MAX_HEALING_RECEIVED / factor)));
        } catch (Throwable ignored) {
        }
    }

    public static void onHurt(LivingHurtEvent event) {
        try {
            if (event.getSource() == null || !(event.getSource().m_7639_() instanceof Player attacker)) {
                return;
            }
            float damage = event.getAmount();
            if (!(damage > 0.0f)) {
                return;
            }
            double lifeSteal = value(attacker, "attributeslib:life_steal", 0.0);
            if (lifeSteal > MAX_LIFE_STEAL) {
                float undo = (float) (damage * (lifeSteal - MAX_LIFE_STEAL));
                if (undo > 0.0f) {
                    float next = attacker.m_21223_() - undo;
                    if (next < 1.0f) {
                        next = 1.0f;
                    }
                    attacker.m_21153_(next);
                }
            }
            double overheal = value(attacker, "attributeslib:overheal", 0.0);
            if (overheal > MAX_OVERHEAL) {
                float excess = (float) (damage * (overheal - MAX_OVERHEAL));
                if (excess > 0.0f) {
                    float nextAbs = attacker.m_6103_() - excess;
                    if (nextAbs < 0.0f) {
                        nextAbs = 0.0f;
                    }
                    attacker.m_7911_(nextAbs);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static double value(LivingEntity entity, String id, double fallback) {
        try {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(id));
            if (attribute == null) {
                return fallback;
            }
            return entity.m_21133_(attribute);
        } catch (Throwable t) {
            return fallback;
        }
    }
}
