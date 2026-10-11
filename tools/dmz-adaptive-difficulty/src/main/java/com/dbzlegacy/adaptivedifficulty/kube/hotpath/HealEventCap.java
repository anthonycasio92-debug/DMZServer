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
    private HealEventCap() {}

    public static void onHeal(LivingHealEvent event) {
        try {
            if (!HotPathConfig.get().healing.enabled) {
                return;
            }
            LivingEntity entity = event.getEntity();
            if (!(entity instanceof Player)) {
                return;
            }
            double cap = HotPathConfig.get().healing.healingReceivedMax;
            double factor = value(entity, "attributeslib:healing_received", 1.0);
            if (!(factor > cap) || !(cap > 0.0)) {
                return;
            }
            float amount = event.getAmount();
            if (!(amount > 0.0f)) {
                return;
            }
            event.setAmount((float) (amount * (cap / factor)));
        } catch (Throwable ignored) {
        }
    }

    public static void onHurt(LivingHurtEvent event) {
        try {
            if (!HotPathConfig.get().healing.enabled) {
                return;
            }
            if (event.getSource() == null || !(event.getSource().m_7639_() instanceof Player attacker)) {
                return;
            }
            float damage = event.getAmount();
            if (!(damage > 0.0f)) {
                return;
            }
            HotPathConfig.Healing healing = HotPathConfig.get().healing;
            double lifeSteal = value(attacker, "attributeslib:life_steal", 0.0);
            if (lifeSteal > healing.lifeStealMax) {
                float undo = (float) (damage * (lifeSteal - healing.lifeStealMax));
                if (undo > 0.0f) {
                    float next = attacker.m_21223_() - undo;
                    if (next < 1.0f) {
                        next = 1.0f;
                    }
                    attacker.m_21153_(next);
                }
            }
            double overheal = value(attacker, "attributeslib:overheal", 0.0);
            if (overheal > healing.overhealMax) {
                float excess = (float) (damage * (overheal - healing.overhealMax));
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
