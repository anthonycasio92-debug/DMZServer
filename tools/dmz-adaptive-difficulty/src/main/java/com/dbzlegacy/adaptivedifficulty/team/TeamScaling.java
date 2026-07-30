package com.dbzlegacy.adaptivedifficulty.team;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Online teammates = same scoreboard team (vanilla teams).
 * Threshold: +teamBonus% of personal max per teammate.
 * Contribution: share of teammates' spare personal max.
 */
public final class TeamScaling {
    private TeamScaling() {}

    public static List<ServerPlayer> teammates(ServerPlayer player) {
        List<ServerPlayer> out = new ArrayList<>();
        if (player == null) {
            return out;
        }
        MinecraftServer server = player.f_8924_;
        if (server == null) {
            return out;
        }
        Scoreboard board = server.m_129896_(); // getScoreboard
        // getPlayersTeam(String)
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
            long mateCalc = DifficultyCalculator.calculatedDifficulty(
                    DmzProgression.dmzLevel(mate), DmzProgression.prestige(mate));
            long matePersonal = mateCalc + mateData.getPurchasedDifficulty();
            long mateUsed = Math.min(mateData.getActiveDifficulty(), matePersonal);
            long spare = Math.max(0L, matePersonal - mateUsed);
            spareTotal += spare;
        }
        return Math.max(0L, Math.round(spareTotal * (cfg.contributionPercent / 100.0)));
    }
}
