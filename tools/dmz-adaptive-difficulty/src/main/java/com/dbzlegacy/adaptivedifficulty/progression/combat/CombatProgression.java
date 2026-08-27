package com.dbzlegacy.adaptivedifficulty.progression.combat;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Soft-entry facade for combat progression ports (KiWeapons / Piercing / DoT / Apothic). */
public final class CombatProgression {
    private CombatProgression() {}

    public static void onLogin(ServerPlayer player) {
        // no-op
    }

    public static void onLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID id = player.m_20148_();
        KiWeapons.clearPlayer(id);
        PiercingBonus.clearPlayer(id);
    }

    public static void pulse(MinecraftServer server, int tick) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (player != null) {
                PiercingBonus.pulse(player);
            }
        }
    }

    public static void onHurt(LivingHurtEvent event, ServerPlayer attacker, LivingEntity target) {
        if (attacker != null && target != null) {
            KiWeapons.onPlayerDealHurt(event, attacker, target);
            ApothicElemental.onPlayerDealHurt(event, attacker, target);
        }
    }

    public static void onPlayerHurt(LivingHurtEvent event, ServerPlayer victim) {
        if (victim == null) {
            return;
        }
        KiWeapons.onPlayerTakeHurt(event, victim);
        DotExtraDamage.onPlayerHurt(event, victim);
    }
}
