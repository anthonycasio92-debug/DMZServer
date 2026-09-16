package com.dbzlegacy.adaptivedifficulty.team;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
import com.dbzlegacy.adaptivedifficulty.rival.RivalConstants;
import com.dbzlegacy.adaptivedifficulty.rival.RivalLink;
import com.dbzlegacy.adaptivedifficulty.rival.RivalPlayerRecord;
import com.dbzlegacy.adaptivedifficulty.rival.RivalStatus;
import com.dbzlegacy.adaptivedifficulty.rival.RivalOnlinePlayers;
import com.dbzlegacy.adaptivedifficulty.rival.RivalStore;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Difficulty team scaling via online mutual rivals (consent-based, max 2).
 * Threshold bonus applies per opted-in rival online; contribution shares spare
 * tier headroom only when rivals are nearby.
 */
public final class TeamScaling {
    private static final boolean FTB_LOADED = ModList.get().isLoaded("ftbteams");

    private TeamScaling() {}

    public static boolean ftbAvailable() {
        return FTB_LOADED;
    }

    public static String teamSourceLabel() {
        if (!DifficultyConfig.get().enableRivalSystem) {
            return "Disabled";
        }
        return "Rival Mutual";
    }

    public static double contributionProximityBlocks() {
        return RivalConstants.BASE_RANGE;
    }

    public static String teamName(ServerPlayer player) {
        int mutual = mutualRivalCount(player);
        if (mutual <= 0) {
            return "none";
        }
        int online = onlineMutualRivalCount(player);
        if (online <= 0) {
            return mutual + " mutual (offline)";
        }
        return online + " online rival" + (online == 1 ? "" : "s");
    }

    /** All mutual / nemesis rivals (online or not). */
    public static int mutualRivalCount(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return 0;
        }
        RivalPlayerRecord me = RivalStore.get().get(player.m_20148_().toString());
        if (me == null || me.rivals == null) {
            return 0;
        }
        int n = 0;
        for (RivalLink link : me.rivals.values()) {
            if (link == null || !link.mutual) {
                continue;
            }
            RivalStatus st = link.status();
            if (st == RivalStatus.MUTUAL || st == RivalStatus.NEMESIS) {
                n++;
            }
        }
        return n;
    }

    public static boolean isOptedIntoTeams(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        return data.isPersonalEnabled() && data.getTeamMode() != TeamMode.PERSONAL_ONLY;
    }

    /** Online mutual / nemesis rivals (any team mode). */
    public static int onlineMutualRivalCount(ServerPlayer player) {
        return onlineMutualRivals(player).size();
    }

    public static List<ServerPlayer> onlineMutualRivals(ServerPlayer player) {
        List<ServerPlayer> out = new ArrayList<>();
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return out;
        }
        MinecraftServer server = player.m_20194_();
        if (server == null) {
            return out;
        }
        RivalPlayerRecord me = RivalStore.get().get(player.m_20148_().toString());
        if (me == null || me.rivals == null) {
            return out;
        }
        for (Map.Entry<String, RivalLink> e : me.rivals.entrySet()) {
            RivalLink link = e.getValue();
            if (!isMutualTeamLink(link)) {
                continue;
            }
            ServerPlayer other = RivalOnlinePlayers.find(server, e.getKey());
            if (other == null || other.m_20148_().equals(player.m_20148_())) {
                continue;
            }
            out.add(other);
        }
        return out;
    }

    /** Online mutual rivals who opted into team scaling. */
    public static List<ServerPlayer> teammates(ServerPlayer player) {
        List<ServerPlayer> out = new ArrayList<>();
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return out;
        }
        MinecraftServer server = player.m_20194_();
        if (server == null) {
            return out;
        }
        RivalPlayerRecord me = RivalStore.get().get(player.m_20148_().toString());
        if (me == null || me.rivals == null) {
            return out;
        }
        for (Map.Entry<String, RivalLink> e : me.rivals.entrySet()) {
            RivalLink link = e.getValue();
            if (!isMutualTeamLink(link)) {
                continue;
            }
            ServerPlayer other = RivalOnlinePlayers.find(server, e.getKey());
            if (other == null || other.m_20148_().equals(player.m_20148_())) {
                continue;
            }
            if (!isOptedIntoTeams(other)) {
                continue;
            }
            out.add(other);
        }
        return out;
    }

    public static boolean withinContributionRange(ServerPlayer self, ServerPlayer mate) {
        if (self == null || mate == null) {
            return false;
        }
        double max = contributionProximityBlocks();
        return self.m_20275_(mate.m_20185_(), mate.m_20186_(), mate.m_20189_()) <= max * max;
    }

    public static long spareHeadroom(ServerPlayer mate) {
        if (mate == null) {
            return 0L;
        }
        PlayerDifficultyData mateData = DifficultyCache.data(mate);
        long matePersonal = UnlockSystem.maxDifficultyFor(mateData);
        if (matePersonal <= 0L) {
            return 0L;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        if (cfg.hardCapDifficulty > 0L) {
            matePersonal = Math.min(matePersonal, cfg.hardCapDifficulty);
        }
        long mateUsed = Math.min(mateData.getActiveDifficultyLevel(), matePersonal);
        return Math.max(0L, matePersonal - mateUsed);
    }

    public static long thresholdBonus(ServerPlayer player, long personalMax) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (personalMax <= 0 || cfg.teamBonusPercent <= 0) {
            return 0L;
        }
        double weighted = 0.0;
        for (ServerPlayer mate : teammates(player)) {
            RivalLink link = linkBetween(player, mate);
            double weight = nemesisWeight(link);
            weighted += weight;
        }
        if (weighted <= 0) {
            return 0L;
        }
        double factor = (cfg.teamBonusPercent / 100.0) * weighted;
        return Math.max(0L, Math.round(personalMax * factor));
    }

    public static long contributionBonus(ServerPlayer self, long selfAfterThreshold) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (cfg.contributionPercent <= 0) {
            return 0L;
        }
        long spareTotal = 0L;
        for (ServerPlayer mate : teammates(self)) {
            if (!withinContributionRange(self, mate)) {
                continue;
            }
            PlayerDifficultyData mateData = DifficultyCache.data(mate);
            if (mateData.getTeamMode() != TeamMode.FULL_TEAM_SCALING) {
                continue;
            }
            long spare = spareHeadroom(mate);
            RivalLink link = linkBetween(self, mate);
            if (link != null && link.status() == RivalStatus.NEMESIS) {
                spare = Math.round(spare * 0.75);
            }
            spareTotal += spare;
        }
        return Math.max(0L, Math.round(spareTotal * (cfg.contributionPercent / 100.0)));
    }

    public enum RarityBonus {
        ELITE,
        MUTATION,
        BOSS_THRESHOLD,
        BOSS_PROMOTION
    }

    /**
     * Weighted online rival count for team rarity bonuses.
     * Threshold counts all online opted-in rivals; Full only counts nearby rivals.
     */
    public static double weightedTeammateFactor(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem || !isOptedIntoTeams(player)) {
            return 0.0;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        TeamMode mode = data.getTeamMode();
        if (mode == TeamMode.PERSONAL_ONLY) {
            return 0.0;
        }
        double weighted = 0.0;
        for (ServerPlayer mate : teammates(player)) {
            if (mode == TeamMode.FULL_TEAM_SCALING && !withinContributionRange(player, mate)) {
                continue;
            }
            weighted += nemesisWeight(linkBetween(player, mate));
        }
        if (weighted <= 0.0) {
            return 0.0;
        }
        if (mode == TeamMode.THRESHOLD_BONUS_ONLY) {
            weighted *= DifficultyConfig.get().teamThresholdRarityMult;
        }
        return weighted;
    }

    /** Bonus percentage points added to elite/mutation rolls or boss threshold/promotion. */
    public static double rarityBonusPercent(ServerPlayer player, RarityBonus kind) {
        double factor = weightedTeammateFactor(player);
        if (factor <= 0.0 || kind == null) {
            return 0.0;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        double perRival = switch (kind) {
            case ELITE -> cfg.teamEliteChanceBonusPercent;
            case MUTATION -> cfg.teamMutationChanceBonusPercent;
            case BOSS_THRESHOLD -> cfg.teamBossThresholdBonusPercent;
            case BOSS_PROMOTION -> cfg.teamBossPromotionChancePercent;
        };
        if (perRival <= 0.0) {
            return 0.0;
        }
        return perRival * factor;
    }

    public static double effectiveBossHealthThreshold(DifficultyConfig cfg, double thresholdReductionPercent) {
        if (cfg == null) {
            return 300.0;
        }
        double reduction = Math.max(0.0, Math.min(50.0, thresholdReductionPercent));
        return cfg.bossHealthThreshold * (1.0 - reduction / 100.0);
    }

    private static boolean isMutualTeamLink(RivalLink link) {
        if (link == null || !link.mutual) {
            return false;
        }
        RivalStatus st = link.status();
        return st == RivalStatus.MUTUAL || st == RivalStatus.NEMESIS;
    }

    private static double nemesisWeight(RivalLink link) {
        if (link != null && link.status() == RivalStatus.NEMESIS) {
            return 0.75;
        }
        return 1.0;
    }

    private static RivalLink linkBetween(ServerPlayer self, ServerPlayer other) {
        if (self == null || other == null) {
            return null;
        }
        return RivalStore.get().getLink(self.m_20148_().toString(), other.m_20148_().toString());
    }

}
