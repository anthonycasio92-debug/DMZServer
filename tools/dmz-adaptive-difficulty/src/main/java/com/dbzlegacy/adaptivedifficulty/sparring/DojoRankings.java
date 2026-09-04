package com.dbzlegacy.adaptivedifficulty.sparring;

import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.util.LmChat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Dojo-vs-dojo rankings keyed by mentor UUID (the dojo master).
 * <p>Apprentices compete for their master's dojo. Inter-dojo spars award season RP;
 * active dojo challenges double RP for matching rival dojos.
 */
public final class DojoRankings {
    private DojoRankings() {}

    /** Align with rival seasons (~75 days). */
    public static final long SEASON_MS = 75L * 24L * 60L * 60L * 1000L;
    public static final long CHALLENGE_TTL_MS = 24L * 60L * 60L * 1000L;
    public static final double BASE_RP_PER_WIN = 12.0;
    public static final double CHALLENGE_RP_MULT = 2.0;
    public static final double DRAW_RP = 4.0;

    /** Home dojo for rankings: mentor's uuid if apprenticed, else own uuid if mentoring. */
    public static String homeDojoKey(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
        if (bond == null) {
            return null;
        }
        if (bond.mentorUuid != null && !bond.mentorUuid.isBlank()) {
            return bond.mentorUuid.trim().toLowerCase(Locale.ROOT);
        }
        if (bond.apprenticeCount() > 0) {
            return player.m_20148_().toString().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    /** True when this player is the dojo master (has apprentices). */
    public static boolean isDojoMaster(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return SparStore.get().bond(player.m_20148_()).apprenticeCount() > 0;
    }

    public static String dojoDisplayName(String dojoKey) {
        if (dojoKey == null || dojoKey.isBlank()) {
            return "?";
        }
        SparStore.DojoEntry e = entry(dojoKey);
        if (e != null && e.mentorName != null && !e.mentorName.isBlank()) {
            return e.mentorName;
        }
        SparStore.MentorBond bond = SparStore.get().bondsByPlayer.get(dojoKey);
        if (bond != null && bond.apprenticeCount() > 0) {
            // Master bond — name may be in leaderboard from spar data.
            SparStore.LeaderboardEntry lb = SparStore.get().leaderboard.get(dojoKey);
            if (lb != null && lb.name != null && !lb.name.isBlank()) {
                return lb.name;
            }
        }
        return "Dojo";
    }

    public static void ensureSeason() {
        SparStore store = SparStore.get();
        long now = System.currentTimeMillis();
        SparStore.DojoSeason season = store.dojoSeason;
        if (season == null) {
            season = freshSeason(now);
            store.dojoSeason = season;
            store.markDirty();
            return;
        }
        if (season.startedAt <= 0L) {
            season.startedAt = now;
            season.seasonId = 1;
            store.markDirty();
            return;
        }
        if (now - season.startedAt >= SEASON_MS) {
            archiveSeason(season);
            store.dojoSeason = freshSeason(now);
            store.markDirty();
        }
    }

    private static SparStore.DojoSeason freshSeason(long now) {
        SparStore.DojoSeason s = new SparStore.DojoSeason();
        s.seasonId = 1;
        s.startedAt = now;
        s.leaderboard = new ConcurrentHashMap<>();
        SparStore.DojoSeason prev = SparStore.get().dojoSeason;
        if (prev != null && prev.seasonId > 0) {
            s.seasonId = prev.seasonId + 1;
        }
        return s;
    }

    private static void archiveSeason(SparStore.DojoSeason season) {
        if (season == null || season.leaderboard == null || season.leaderboard.isEmpty()) {
            return;
        }
        List<Map.Entry<String, SparStore.DojoEntry>> top = sortedEntries(season.leaderboard, "rp", 3);
        if (top.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("§6§lDojo season §f#").append(season.seasonId).append(" §6ended!");
        int rank = 1;
        for (Map.Entry<String, SparStore.DojoEntry> e : top) {
            SparStore.DojoEntry d = e.getValue();
            sb.append("\n§e#").append(rank).append(" §f")
                    .append(d == null ? "?" : blank(d.mentorName, "?"))
                    .append(" §7").append(d == null ? 0 : (int) d.seasonRp).append(" RP");
            rank++;
        }
        broadcast(sb.toString());
    }

    private static void broadcast(String msg) {
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer p : server.m_6846_().m_11314_()) {
            DmzRewards.msg(p, msg);
        }
    }

    /**
     * Called after both fighters' leaderboards are updated.
     */
    public static void onInterDojoSession(
            ServerPlayer a,
            SparPlayerRuntime aRt,
            ServerPlayer b,
            SparPlayerRuntime bRt,
            long durationMs
    ) {
        if (durationMs < SparringSystem.MIN_COUNTED_SESSION_MS || a == null || b == null || aRt == null || bRt == null) {
            return;
        }
        if (SparringSystem.isSparringWithOwnMentor(a, b) || SparringSystem.isSparringWithDojoPeer(a, b)) {
            return;
        }
        String keyA = homeDojoKey(a);
        String keyB = homeDojoKey(b);
        if (keyA == null || keyB == null || keyA.equalsIgnoreCase(keyB)) {
            return;
        }
        ensureSeason();
        bumpActivity(keyA, a, aRt.sessionTp, durationMs);
        bumpActivity(keyB, b, bRt.sessionTp, durationMs);

        double tpA = aRt.sessionTp;
        double tpB = bRt.sessionTp;
        boolean challenge = isActiveChallenge(keyA, keyB);
        double mult = challenge ? CHALLENGE_RP_MULT : 1.0;

        if (Math.abs(tpA - tpB) < 0.5) {
            awardRp(keyA, DRAW_RP * mult, true, false, false);
            awardRp(keyB, DRAW_RP * mult, true, false, false);
            notifyDojo(a, b, keyA, keyB, "§7Draw", (int) (DRAW_RP * mult), challenge);
            return;
        }
        boolean aWins = tpA > tpB;
        ServerPlayer winner = aWins ? a : b;
        ServerPlayer loser = aWins ? b : a;
        String winKey = aWins ? keyA : keyB;
        String loseKey = aWins ? keyB : keyA;
        double rp = BASE_RP_PER_WIN * mult;
        awardRp(winKey, rp, false, true, false);
        awardRp(loseKey, 0, false, false, true);
        notifyDojo(winner, loser, winKey, loseKey,
                "§a" + winner.m_7755_().getString() + " §7won", (int) rp, challenge);
    }

    private static void bumpActivity(String dojoKey, ServerPlayer fighter, double tp, long durationMs) {
        SparStore.DojoEntry e = entryOrCreate(dojoKey, fighter);
        e.totalTp += tp;
        e.totalTimeMs += durationMs;
        e.sessions++;
        e.rosterSize = rosterSize(dojoKey);
        e.updatedAt = System.currentTimeMillis();
        SparStore.get().markDirty();
    }

    private static void awardRp(String dojoKey, double rp, boolean draw, boolean win, boolean loss) {
        SparStore.DojoEntry e = entryOrCreate(dojoKey, null);
        e.seasonRp += rp;
        if (draw) {
            e.draws++;
        } else if (win) {
            e.wins++;
        } else if (loss) {
            e.losses++;
        }
        e.updatedAt = System.currentTimeMillis();
        SparStore.get().markDirty();
    }

    private static SparStore.DojoEntry entryOrCreate(String dojoKey, ServerPlayer nameSource) {
        ensureSeason();
        SparStore.DojoSeason season = SparStore.get().dojoSeason;
        return season.leaderboard.computeIfAbsent(dojoKey, k -> {
            SparStore.DojoEntry d = new SparStore.DojoEntry();
            d.mentorUuid = dojoKey;
            d.mentorName = resolveMentorName(dojoKey, nameSource);
            d.rosterSize = rosterSize(dojoKey);
            return d;
        });
    }

    private static SparStore.DojoEntry entry(String dojoKey) {
        SparStore.DojoSeason season = SparStore.get().dojoSeason;
        if (season == null || season.leaderboard == null) {
            return null;
        }
        return season.leaderboard.get(dojoKey);
    }

    private static String resolveMentorName(String dojoKey, ServerPlayer fallback) {
        if (fallback != null && fallback.m_20148_().toString().equalsIgnoreCase(dojoKey)) {
            return fallback.m_7755_().getString();
        }
        SparStore.LeaderboardEntry lb = SparStore.get().leaderboard.get(dojoKey);
        if (lb != null && lb.name != null && !lb.name.isBlank()) {
            return lb.name;
        }
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            try {
                ServerPlayer online = server.m_6846_().m_11259_(UUID.fromString(dojoKey));
                if (online != null) {
                    return online.m_7755_().getString();
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return "Dojo";
    }

    private static int rosterSize(String dojoKey) {
        SparStore.MentorBond bond = SparStore.get().bondsByPlayer.get(dojoKey);
        if (bond == null) {
            return 1;
        }
        bond.normalizeApprentices();
        return 1 + bond.apprenticeCount();
    }

    private static boolean isActiveChallenge(String keyA, String keyB) {
        SparStore.DojoChallenge c = findChallenge(keyA, keyB);
        return c != null && c.active && c.expiresAt > System.currentTimeMillis();
    }

    private static SparStore.DojoChallenge findChallenge(String keyA, String keyB) {
        for (SparStore.DojoChallenge c : SparStore.get().dojoChallenges.values()) {
            if (c == null) {
                continue;
            }
            if ((keyA.equalsIgnoreCase(c.fromDojoUuid) && keyB.equalsIgnoreCase(c.toDojoUuid))
                    || (keyB.equalsIgnoreCase(c.fromDojoUuid) && keyA.equalsIgnoreCase(c.toDojoUuid))) {
                return c;
            }
        }
        return null;
    }

    private static void notifyDojo(
            ServerPlayer winner,
            ServerPlayer loser,
            String winKey,
            String loseKey,
            String result,
            int rp,
            boolean challenge
    ) {
        String tag = challenge ? " §6§lDOJO WAR" : "";
        String msg = LmChat.note("Dojo", result + tag
                + " §8· §f" + dojoDisplayName(winKey)
                + " §7+" + rp + " RP vs §f" + dojoDisplayName(loseKey));
        DmzRewards.msg(winner, msg);
        DmzRewards.msg(loser, msg);
        pingDojoMaster(winKey, msg);
        pingDojoMaster(loseKey, msg);
    }

    private static void pingDojoMaster(String dojoKey, String msg) {
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        try {
            ServerPlayer master = server.m_6846_().m_11259_(UUID.fromString(dojoKey));
            if (master != null && master.m_6084_()) {
                DmzRewards.msg(master, msg);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    public static List<String> topLines(String category, int limit) {
        ensureSeason();
        List<String> lines = new ArrayList<>();
        String cat = category == null || category.isBlank() ? "rp" : category.trim().toLowerCase(Locale.ROOT);
        SparStore.DojoSeason season = SparStore.get().dojoSeason;
        Map<String, SparStore.DojoEntry> board = season == null || season.leaderboard == null
                ? Map.of()
                : season.leaderboard;
        lines.add("§6§lDojo Top §8— §f" + cat
                + (season != null ? " §8S#" + season.seasonId : ""));
        List<Map.Entry<String, SparStore.DojoEntry>> sorted = sortedEntries(board, cat, limit);
        int i = 1;
        for (Map.Entry<String, SparStore.DojoEntry> e : sorted) {
            SparStore.DojoEntry d = e.getValue();
            if (d == null) {
                continue;
            }
            String value = formatValue(d, cat);
            lines.add("§e#" + i + " §f" + blank(d.mentorName, "?")
                    + " §7" + value
                    + " §8(" + d.rosterSize + " fighters)");
            i++;
        }
        if (i == 1) {
            lines.add("§7No dojo rankings yet — spar across dojos!");
        }
        return lines;
    }

    public static List<String> infoLines(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        lines.add("§8── §6Dojo Rankings §8──");
        ensureSeason();
        SparStore.DojoSeason season = SparStore.get().dojoSeason;
        if (season != null) {
            long left = Math.max(0L, SEASON_MS - (System.currentTimeMillis() - season.startedAt));
            lines.add("§7Season §f#" + season.seasonId + " §8· §7" + (left / 86_400_000L) + "d left");
        }
        String key = homeDojoKey(player);
        if (key == null) {
            lines.add("§7Join a dojo to compete on the ladder.");
            lines.add("§8Invite apprentices or ask a mentor.");
            return lines;
        }
        SparStore.DojoEntry e = entry(key);
        lines.add("§7Your dojo §f" + dojoDisplayName(key));
        if (e != null) {
            lines.add("§7Season RP §f" + (int) e.seasonRp
                    + " §8· §a" + e.wins + "W §c" + e.losses + "L §7" + e.draws + "D");
            lines.add("§7Spar TP §f" + DmzRewards.formatWhole(e.totalTp)
                    + " §8· §7" + e.sessions + " inter-dojo spars");
        }
        SparStore.DojoChallenge pending = SparStore.get().dojoChallenges.get(player.m_20148_().toString());
        if (pending != null && pending.expiresAt > System.currentTimeMillis() && !pending.active) {
            lines.add("§eChallenge from §f" + blank(pending.fromDojoName, "?")
                    + " §8— accept in Dojo War");
        }
        SparStore.DojoChallenge active = activeChallengeFor(key);
        if (active != null) {
            String rival = key.equalsIgnoreCase(active.fromDojoUuid)
                    ? active.toDojoName
                    : active.fromDojoName;
            lines.add("§6§lActive war §fvs " + blank(rival, "?")
                    + " §8(2× RP)");
        }
        return lines;
    }

    private static SparStore.DojoChallenge activeChallengeFor(String dojoKey) {
        long now = System.currentTimeMillis();
        for (SparStore.DojoChallenge c : SparStore.get().dojoChallenges.values()) {
            if (c == null || !c.active || c.expiresAt <= now) {
                continue;
            }
            if (dojoKey.equalsIgnoreCase(c.fromDojoUuid) || dojoKey.equalsIgnoreCase(c.toDojoUuid)) {
                return c;
            }
        }
        return null;
    }

    /** Encoded rival dojo cards for challenge picker: {@code uuid\tname\trp}. */
    public static List<String> rivalDojoCards(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        if (player == null || !isDojoMaster(player)) {
            return out;
        }
        ensureSeason();
        String self = player.m_20148_().toString().toLowerCase(Locale.ROOT);
        SparStore.DojoSeason season = SparStore.get().dojoSeason;
        if (season == null || season.leaderboard == null) {
            return out;
        }
        List<Map.Entry<String, SparStore.DojoEntry>> sorted =
                sortedEntries(season.leaderboard, "rp", 50);
        for (Map.Entry<String, SparStore.DojoEntry> e : sorted) {
            if (e.getKey().equalsIgnoreCase(self)) {
                continue;
            }
            SparStore.DojoEntry d = e.getValue();
            if (d == null) {
                continue;
            }
            String name = blank(d.mentorName, "?").replace('\t', ' ').replace('\n', ' ');
            out.add(e.getKey() + "\t" + name + "\t" + (int) d.seasonRp);
        }
        return out;
    }

    public static String challengeDojo(ServerPlayer challenger, ServerPlayer target) {
        if (challenger == null) {
            return "§cPlayers only.";
        }
        if (!isDojoMaster(challenger)) {
            return "§cOnly dojo masters (with apprentices) can declare war.";
        }
        if (target == null) {
            return "§cPlayer not online.";
        }
        if (challenger.m_20148_().equals(target.m_20148_())) {
            return "§cYou cannot challenge your own dojo.";
        }
        if (!isDojoMaster(target)) {
            return "§cThey are not a dojo master.";
        }
        String fromKey = challenger.m_20148_().toString().toLowerCase(Locale.ROOT);
        String toKey = target.m_20148_().toString().toLowerCase(Locale.ROOT);
        if (findChallenge(fromKey, toKey) != null) {
            return "§cA challenge already exists between these dojos.";
        }
        long now = System.currentTimeMillis();
        SparStore.DojoChallenge c = new SparStore.DojoChallenge();
        c.fromDojoUuid = fromKey;
        c.fromDojoName = challenger.m_7755_().getString();
        c.toDojoUuid = toKey;
        c.toDojoName = target.m_7755_().getString();
        c.fromMentorUuid = fromKey;
        c.expiresAt = now + CHALLENGE_TTL_MS;
        c.active = false;
        SparStore.get().dojoChallenges.put(toKey, c);
        SparStore.get().markDirty();
        DmzRewards.msg(target, LmChat.note("Dojo", "§f" + c.fromDojoName
                + " §e challenged your dojo to war!"));
        DmzRewards.msg(target, LmChat.tip("/spar", "→ Dojo Rankings → Accept War"));
        return "§aWar challenge sent to §f" + target.m_7755_().getString() + "§a.";
    }

    public static String acceptChallenge(ServerPlayer master) {
        if (master == null) {
            return "§cPlayers only.";
        }
        if (!isDojoMaster(master)) {
            return "§cOnly dojo masters can accept war.";
        }
        String key = master.m_20148_().toString().toLowerCase(Locale.ROOT);
        SparStore.DojoChallenge c = SparStore.get().dojoChallenges.get(key);
        if (c == null || c.expiresAt <= System.currentTimeMillis()) {
            return "§cNo pending dojo war challenge.";
        }
        c.active = true;
        c.expiresAt = System.currentTimeMillis() + CHALLENGE_TTL_MS;
        SparStore.get().markDirty();
        broadcast(LmChat.ok("Dojo", "§6§lDOJO WAR §f" + c.fromDojoName
                + " §7vs §f" + c.toDojoName + " §8— inter-dojo spars earn 2× RP!"));
        return "§aDojo war accepted! §7Inter-dojo spars vs §f" + c.fromDojoName + " §aearn 2× RP.";
    }

    public static String declineChallenge(ServerPlayer master) {
        if (master == null) {
            return "§cPlayers only.";
        }
        String key = master.m_20148_().toString().toLowerCase(Locale.ROOT);
        SparStore.DojoChallenge c = SparStore.get().dojoChallenges.remove(key);
        if (c == null) {
            return "§cNo pending challenge.";
        }
        SparStore.get().markDirty();
        return "§7Declined war challenge from §f" + blank(c.fromDojoName, "?") + "§7.";
    }

    public static int dojoRank(String dojoKey, String category) {
        if (dojoKey == null || dojoKey.isBlank()) {
            return 0;
        }
        ensureSeason();
        SparStore.DojoSeason season = SparStore.get().dojoSeason;
        if (season == null || season.leaderboard == null) {
            return 0;
        }
        List<Map.Entry<String, SparStore.DojoEntry>> sorted =
                sortedEntries(season.leaderboard, category == null ? "rp" : category, 9999);
        int rank = 1;
        for (Map.Entry<String, SparStore.DojoEntry> e : sorted) {
            if (dojoKey.equalsIgnoreCase(e.getKey())) {
                return rank;
            }
            rank++;
        }
        return 0;
    }

    private static List<Map.Entry<String, SparStore.DojoEntry>> sortedEntries(
            Map<String, SparStore.DojoEntry> board,
            String cat,
            int limit
    ) {
        List<Map.Entry<String, SparStore.DojoEntry>> list = new ArrayList<>(board.entrySet());
        list.sort(Comparator.comparingDouble((Map.Entry<String, SparStore.DojoEntry> e) ->
                score(e.getValue(), cat)).reversed());
        if (limit > 0 && list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    private static double score(SparStore.DojoEntry d, String cat) {
        if (d == null) {
            return 0;
        }
        return switch (cat) {
            case "wins", "win" -> d.wins;
            case "tp" -> d.totalTp;
            case "sessions", "session" -> d.sessions;
            default -> d.seasonRp;
        };
    }

    private static String formatValue(SparStore.DojoEntry d, String cat) {
        return switch (cat) {
            case "wins", "win" -> d.wins + " wins";
            case "tp" -> DmzRewards.formatWhole(d.totalTp) + " TP";
            case "sessions", "session" -> d.sessions + " spars";
            default -> (int) d.seasonRp + " RP";
        };
    }

    private static String blank(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v;
    }
}
