package com.dbzlegacy.adaptivedifficulty.progression.combat;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Port of {@code damageovertime.js} — adds a % of max HP when DoT sources tick.
 */
public final class DotExtraDamage {
    private static final Set<UUID> REENTRY = ConcurrentHashMap.newKeySet();

    private DotExtraDamage() {}

    public static void onPlayerHurt(LivingHurtEvent event, ServerPlayer victim) {
        if (!DifficultyConfig.get().enableDotExtraDamage || victim == null) {
            return;
        }
        UUID id = victim.m_20148_();
        if (!REENTRY.add(id)) {
            return;
        }
        try {
            DamageSource source = event.getSource();
            if (source == null) {
                return;
            }
            String type = "";
            try {
                type = source.m_19385_();
            } catch (Throwable ignored) {
            }
            if (type == null) {
                type = "";
            }
            type = type.toLowerCase(Locale.ROOT);

            double percent = percentFor(type, victim);
            if (!(percent > 0.0)) {
                return;
            }
            double maxHealth = victim.m_21233_();
            if (!(maxHealth > 0.0)) {
                return;
            }
            float bonus = (float) (maxHealth * percent);
            if (!(bonus > 0.0f)) {
                return;
            }
            float hp = victim.m_21223_();
            float next = Math.max(0.0f, hp - bonus);
            victim.m_21153_(next);
        } finally {
            REENTRY.remove(id);
        }
    }

    private static double percentFor(String type, ServerPlayer victim) {
        return switch (type) {
            case "wither", "minecraft:wither" -> 0.02;
            case "starve", "minecraft:starve" -> 0.025;
            case "drown", "minecraft:drown" -> 0.02;
            case "inwall", "minecraft:in_wall", "in_wall" -> 0.02;
            case "infire", "minecraft:in_fire", "in_fire",
                    "onfire", "minecraft:on_fire", "on_fire" -> 0.025;
            case "magic", "minecraft:magic" -> victim.m_21023_(MobEffects.f_19614_) ? 0.01 : 0.0; // POISON
            default -> 0.0;
        };
    }
}
