package com.dbzlegacy.adaptivedifficulty.team;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.fml.ModList;

/**
 * Online teammates via FTB Teams (party / server team) when present,
 * otherwise vanilla scoreboard teams.
 */
public final class TeamScaling {
    private static final boolean FTB_LOADED = ModList.get().isLoaded("ftbteams");

    private TeamScaling() {}

    public static boolean ftbAvailable() {
        return FTB_LOADED;
    }

    public static String teamSourceLabel() {
        return FTB_LOADED ? "FTB Teams" : "Scoreboard";
    }

    public static String teamName(ServerPlayer player) {
        if (player == null) {
            return "none";
        }
        if (FTB_LOADED) {
            try {
                Optional<Team> team = FTBTeamsAPI.api().getManager().getTeamForPlayer(player);
                if (team.isPresent()) {
                    Team t = team.get();
                    // Personal teams are solo — treat as no party for display.
                    if (t.isPlayerTeam() && t.getMembers().size() <= 1) {
                        return "none (solo)";
                    }
                    return t.getName().getString();
                }
            } catch (Throwable t) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] FTB teamName failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            }
        }
        MinecraftServer server = player.f_8924_;
        if (server == null) {
            return "none";
        }
        PlayerTeam team = server.m_129896_().m_83500_(player.m_6302_());
        return team == null ? "none" : team.m_5758_();
    }

    public static List<ServerPlayer> teammates(ServerPlayer player) {
        List<ServerPlayer> out = new ArrayList<>();
        if (player == null) {
            return out;
        }
        if (FTB_LOADED) {
            try {
                List<ServerPlayer> ftb = ftbTeammates(player);
                if (ftb != null) {
                    return ftb;
                }
            } catch (Throwable t) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] FTB teammates failed, falling back to scoreboard: {}",
                        AdaptiveDifficultyMod.MOD_ID,
                        t.toString());
            }
        }
        return scoreboardTeammates(player);
    }

    private static List<ServerPlayer> ftbTeammates(ServerPlayer player) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return null;
        }
        Optional<Team> opt = FTBTeamsAPI.api().getManager().getTeamForPlayer(player);
        if (opt.isEmpty()) {
            return List.of();
        }
        Team team = opt.get();
        // Solo personal teams do not contribute team scaling.
        if (team.isPlayerTeam() && team.getMembers().size() <= 1) {
            return List.of();
        }
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer other : team.getOnlineMembers()) {
            if (other != null && !other.m_20148_().equals(player.m_20148_())) {
                out.add(other);
            }
        }
        return out;
    }

    private static List<ServerPlayer> scoreboardTeammates(ServerPlayer player) {
        List<ServerPlayer> out = new ArrayList<>();
        MinecraftServer server = player.f_8924_;
        if (server == null) {
            return out;
        }
        Scoreboard board = server.m_129896_();
        PlayerTeam team = board.m_83500_(player.m_6302_());
        if (team == null) {
            return out;
        }
        for (ServerPlayer other : server.m_6846_().m_11314_()) {
            if (other == null || other.m_20148_().equals(player.m_20148_())) {
                continue;
            }
            PlayerTeam otherTeam = board.m_83500_(other.m_6302_());
            if (team.equals(otherTeam)) {
                out.add(other);
            }
        }
        return out;
    }

    public static long thresholdBonus(ServerPlayer player, long personalMax) {
        DifficultyConfig cfg = DifficultyConfig.get();
        int count = teammates(player).size();
        if (count <= 0 || personalMax <= 0) {
            return 0L;
        }
        double factor = (cfg.teamBonusPercent / 100.0) * count;
        return Math.max(0L, Math.round(personalMax * factor));
    }

    public static long contributionBonus(ServerPlayer self, long selfAfterThreshold) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (cfg.contributionPercent <= 0) {
            return 0L;
        }
        long spareTotal = 0L;
        for (ServerPlayer mate : teammates(self)) {
            PlayerDifficultyData mateData = DifficultyCache.data(mate);
            if (mateData.getTeamMode() == TeamMode.PERSONAL_ONLY) {
                continue;
            }
            // V3: contribute unused room inside the mate's activated tier ceiling.
            long matePersonal = com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem.maxDifficultyFor(mateData);
            if (matePersonal <= 0L) {
                continue;
            }
            if (cfg.hardCapDifficulty > 0L) {
                matePersonal = Math.min(matePersonal, cfg.hardCapDifficulty);
            }
            long mateUsed = Math.min(mateData.getActiveDifficultyLevel(), matePersonal);
            long spare = Math.max(0L, matePersonal - mateUsed);
            spareTotal += spare;
        }
        return Math.max(0L, Math.round(spareTotal * (cfg.contributionPercent / 100.0)));
    }
}
