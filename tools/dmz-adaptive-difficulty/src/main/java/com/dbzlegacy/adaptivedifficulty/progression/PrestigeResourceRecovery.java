package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.progression.bridge.EnergyManaSync;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

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
        MinecraftServer server = player.m_20194_();
        if (server != null) {
            server.execute(() -> {
                try {
                    com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil.sync(player);
                } catch (Throwable ignored) {
                }
                try {
                    EnergyManaSync.sync(player, true);
                } catch (Throwable ignored) {
                }
            });
        }
    }
}
