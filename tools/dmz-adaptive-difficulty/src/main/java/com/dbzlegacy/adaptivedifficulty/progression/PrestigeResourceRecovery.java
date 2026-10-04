package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.progression.bridge.OverhaulPrestigeResourceScale;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.TickTask;

/**
 * After LM prestige / {@code dmzstats reset}, clear stamina-pause cooldowns
 * that block regen until relog.
 */
public final class PrestigeResourceRecovery {
    private PrestigeResourceRecovery() {}

    public static void afterDmzStatsReset(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            OverhaulPrestigeResourceScale.clear(player.m_20148_());
        } catch (Throwable ignored) {
        }
        pulse(player);
        MinecraftServer server = player.m_20194_();
        if (server == null) {
            return;
        }
        final java.util.UUID id = player.m_20148_();
        server.execute(() -> runDelayed(server, id));
        for (int delay : new int[] {5, 20, 40, 80, 160}) {
            try {
                server.m_6937_(new TickTask(server.m_129921_() + delay, () -> runDelayed(server, id)));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void runDelayed(MinecraftServer server, java.util.UUID id) {
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
            }
        } catch (Throwable ignored) {
        }
        try {
            OverhaulPrestigeResourceScale.pulse(player);
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
    }
}
