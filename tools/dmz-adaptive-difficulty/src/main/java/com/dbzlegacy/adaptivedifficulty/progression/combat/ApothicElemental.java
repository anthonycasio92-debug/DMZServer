package com.dbzlegacy.adaptivedifficulty.progression.combat;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.util.ApothicAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Port of {@code Apothicfireandcolddamage.js} — multiplies dealt damage by
 * {@code 1 + (attack + fire + cold) * 0.01} from the main-hand Apothic ranks.
 */
public final class ApothicElemental {
    private ApothicElemental() {}

    public static void onPlayerDealHurt(LivingHurtEvent event, ServerPlayer attacker, LivingEntity target) {
        if (!DifficultyConfig.get().enableApothicElemental || attacker == null || target == null) {
            return;
        }
        float amount = event.getAmount();
        if (!(amount > 0.0f)) {
            return;
        }
        ItemStack main = attacker.m_21205_(); // getMainHandItem
        if (main == null || main.m_41619_()) {
            return;
        }

        double attack = 0.0;
        try {
            var attr = attacker.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22281_); // ATTACK_DAMAGE
            if (attr != null) {
                attack = Math.max(0.0, attr.m_22115_());
            }
        } catch (Throwable ignored) {
        }

        double fire = Math.max(0.0, ApothicAttributes.fireDamage(attacker));
        double cold = Math.max(0.0, ApothicAttributes.coldDamage(attacker));
        double total = attack + fire + cold;
        if (!(total > 0.0)) {
            return;
        }
        event.setAmount(amount * (float) (1.0 + total * 0.01));
    }
}
