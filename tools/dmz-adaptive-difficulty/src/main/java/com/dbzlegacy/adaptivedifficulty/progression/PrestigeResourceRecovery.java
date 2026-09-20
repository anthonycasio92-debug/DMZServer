package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.EnergyManaSync;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.TickTask;

/**
 * After LM prestige / {@code dmzstats reset}, clear stale Fabled energy trackers and
 * stamina-pause cooldowns that block regen until relog.
 */
public final class PrestigeResourceRecovery {
    private PrestigeResourceRecovery() {}

    public static void afterDmzStatsReset(ServerPlayer player) {
        if (player == null) {
            return;
        }
        pulse(player);
        MinecraftServer server = player.m_20194_();
        if (server == null) {
            return;
        }
        final java.util.UUID id = player.m_20148_();
        server.execute(() -> runDelayed(server, id, 1));
        for (int delay : new int[] {5, 20, 40, 80, 160}) {
            try {
                server.m_6937_(new TickTask(server.m_129921_() + delay, () -> runDelayed(server, id, delay)));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void runDelayed(MinecraftServer server, java.util.UUID id, int delay) {
        ServerPlayer player = server.m_6846_().m_11259_(id);
        if (player == null || !player.m_6084_()) {
            return;
        }
        pulse(player);
    }

    public static void pulse(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            EnergyManaSync.clear(player.m_20148_());
        } catch (Throwable ignored) {
        }
        try {
            ProgressionData.tempRemove(player, "dmz_fabled_last_mana");
        } catch (Throwable ignored) {
        }
        try {
            StatsData data = DmzProgression.stats(player);
            if (data != null && data.getCooldowns() != null) {
                Cooldowns cds = data.getCooldowns();
                cds.removeCooldown(Cooldowns.STAMINA_PAUSE);
                cds.removeCooldown(Cooldowns.DASH_ACTIVE);
                cds.removeCooldown(Cooldowns.DRAIN);
                cds.removeCooldown(Cooldowns.DRAIN_ACTIVE);
                try {
                    cds.tick();
                } catch (Throwable ignored) {
                }
            }
            Resources res = data != null ? data.getResources() : null;
            if (data != null && res != null) {
                float maxE = data.getMaxEnergy();
                float maxS = data.getMaxStamina();
                if (maxE > 0 && res.getCurrentEnergy() <= 0) {
                    res.setCurrentEnergy(Math.min(maxE, Math.max(res.getCurrentEnergy(), maxE * 0.01f)));
                }
                if (maxS > 0 && res.getCurrentStamina() <= 0) {
                    res.setCurrentStamina(Math.min(maxS, Math.max(res.getCurrentStamina(), maxS * 0.01f)));
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            StaminaRegenGuard.pulse(player);
        } catch (Throwable ignored) {
        }
        try {
            PersonalLevelCapMirror.publish(player);
        } catch (Throwable ignored) {
        }
        try {
            com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil.sync(player);
        } catch (Throwable ignored) {
        }
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
        try {
            EnergyManaSync.sync(player, true);
        } catch (Throwable ignored) {
        }
    }
}
