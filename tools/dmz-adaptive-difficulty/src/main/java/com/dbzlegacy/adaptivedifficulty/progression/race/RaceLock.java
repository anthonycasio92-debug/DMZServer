package com.dbzlegacy.adaptivedifficulty.progression.race;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.FabledSkills;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of DMZ RACE LOCK.js — restricted races require a Fabled unlock skill.
 */
public final class RaceLock {
    private static final String[] RESTRICTED_RACE_IDS = {"ancient_saiyan"};
    private static final String[] REQUIRED_FABLED_SKILLS = {"Ancient Saiyan"};
    private static final String[] DISPLAY_NAMES = {"Ancient Saiyan"};

    private RaceLock() {}

    public static void pulse(ServerPlayer player, long nowMs) {
        if (!ProgressionConfig.raceLock() || player == null) {
            return;
        }
        if (nowMs < ProgressionData.tempGetLong(player, "restricted_race_command_tick", 0L)) {
            return;
        }
        ProgressionData.tempPut(player, "restricted_race_command_tick", nowMs + 1000L);
        try {
            StatsData data = DmzProgression.stats(player);
            Character ch = data == null ? null : data.getCharacter();
            if (ch == null) {
                return;
            }
            String raceId = ch.getRace();
            if (raceId == null || raceId.isBlank() || "null".equalsIgnoreCase(raceId)) {
                raceId = ch.getRaceName();
            }
            if (raceId == null || raceId.isBlank()) {
                return;
            }
            String lower = raceId.toLowerCase(Locale.ROOT).trim();
            int idx = -1;
            for (int i = 0; i < RESTRICTED_RACE_IDS.length; i++) {
                if (RESTRICTED_RACE_IDS[i].equalsIgnoreCase(lower)) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) {
                ProgressionData.tempRemove(player, "restricted_race_command_last_state");
                return;
            }
            String required = REQUIRED_FABLED_SKILLS[idx];
            String display = DISPLAY_NAMES[idx];
            int skillLevel = FabledSkills.skillLevel(player, required);
            if (skillLevel >= 1) {
                return;
            }

            long retryUntil = ProgressionData.tempGetLong(player, "restricted_race_command_retry", 0L);
            if (nowMs < retryUntil) {
                return;
            }
            ProgressionData.tempPut(player, "restricted_race_command_retry", nowMs + 5000L);

            String state = "restricted|" + lower + "|" + required.toLowerCase(Locale.ROOT) + "|" + skillLevel;
            String old = ProgressionData.tempGet(player, "restricted_race_command_last_state", "");
            if (!state.equals(old)) {
                ProgressionData.tempPut(player, "restricted_race_command_last_state", state);
                DmzRewards.msg(player, "§c§lRACE LOCKED");
                DmzRewards.msg(player, "§7You have not unlocked the race §f" + display + "§7.");
                DmzRewards.msg(player, "§7Required Fabled skill: §f" + required);
            }

            String cmd = "dmzstats reset " + player.m_7755_().getString() + " 0 false";
            dispatchConsole(player.m_20194_(), cmd);
            SystemTelemetry.log("progression", "race_lock_reset", player, null,
                    Map.of("race", lower, "required", required));
        } catch (Throwable ignored) {
        }
    }

    private static void dispatchConsole(MinecraftServer server, String cmd) {
        if (server == null || cmd == null || cmd.isBlank()) {
            return;
        }
        try {
            Commands commands = server.m_129892_();
            CommandSourceStack source = server.m_129893_();
            commands.m_230957_(source, cmd);
        } catch (Throwable t1) {
            try {
                Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
                Object console = bukkit.getMethod("getConsoleSender").invoke(null);
                Object serverB = bukkit.getMethod("getServer").invoke(null);
                serverB.getClass().getMethod("dispatchCommand",
                                Class.forName("org.bukkit.command.CommandSender"), String.class)
                        .invoke(serverB, console, cmd);
            } catch (Throwable ignored) {
            }
        }
    }
}
