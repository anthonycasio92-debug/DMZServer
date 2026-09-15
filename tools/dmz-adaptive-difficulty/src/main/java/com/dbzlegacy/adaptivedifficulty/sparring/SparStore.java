package com.dbzlegacy.adaptivedifficulty.sparring;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.ConfigPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mentor bonds + sparring leaderboard persistence at {@code config/legacymechanics/sparring.json}.
 * <p>Mod JSON only — no CustomNPCs (CNPC) script writes.
 */
public final class SparStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SparStore INSTANCE = new SparStore();
    /** How many finished spars to keep for the Stats board. */
    public static final int RECENT_SESSION_LIMIT = 3;

    public final Map<String, MentorBond> bondsByPlayer = new ConcurrentHashMap<>();
    public final Map<String, LeaderboardEntry> leaderboard = new ConcurrentHashMap<>();
    public final Map<String, BondInvite> invites = new ConcurrentHashMap<>();
    /** uuid → newest-first finished session summaries (capped at {@link #RECENT_SESSION_LIMIT}). */
    public final Map<String, List<RecentSession>> recentSessions = new ConcurrentHashMap<>();
    /** uuid → spar combat TP chat messages (missing = default ON). */
    public final Map<String, Boolean> tpMessages = new ConcurrentHashMap<>();
    /** uuid → mentor share TP chat messages (missing = default ON). */
    public final Map<String, Boolean> mentorTpMessages = new ConcurrentHashMap<>();
    /** Active dojo season leaderboard keyed by mentor UUID. */
    public DojoSeason dojoSeason;
    /** Pending/active dojo war challenges keyed by target mentor UUID. */
    public final Map<String, DojoChallenge> dojoChallenges = new ConcurrentHashMap<>();
    /** Persistent dojo identity (display name, banner) keyed by mentor UUID. */
    public final Map<String, DojoProfile> dojoProfiles = new ConcurrentHashMap<>();
    /** Past season champions — newest first. */
    public final List<DojoSeasonRecord> dojoHallOfFame = new ArrayList<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private long lastSaveAt;

    public static SparStore get() {
        return INSTANCE;
    }

    private SparStore() {}

    public static Path path() {
        return ConfigPaths.sparringPath();
    }

    public void markDirty() {
        dirty.set(true);
    }

    public synchronized void load() {
        Path file = path();
        try {
            if (!Files.isRegularFile(file)) {
                bondsByPlayer.clear();
                leaderboard.clear();
                invites.clear();
                recentSessions.clear();
                tpMessages.clear();
                mentorTpMessages.clear();
                dojoSeason = null;
                dojoChallenges.clear();
                dojoProfiles.clear();
                dojoHallOfFame.clear();
                dirty.set(false);
                return;
            }
            try (Reader reader = Files.newBufferedReader(file)) {
                Persist blob = GSON.fromJson(reader, Persist.class);
                bondsByPlayer.clear();
                leaderboard.clear();
                invites.clear();
                recentSessions.clear();
                tpMessages.clear();
                mentorTpMessages.clear();
                if (blob != null) {
                    if (blob.bondsByPlayer != null) {
                        bondsByPlayer.putAll(blob.bondsByPlayer);
                        for (MentorBond bond : bondsByPlayer.values()) {
                            if (bond != null) {
                                bond.normalizeApprentices();
                            }
                        }
                    }
                    if (blob.leaderboard != null) {
                        leaderboard.putAll(blob.leaderboard);
                    }
                    if (blob.invites != null) {
                        invites.putAll(blob.invites);
                    }
                    if (blob.recentSessions != null) {
                        for (Map.Entry<String, List<RecentSession>> e : blob.recentSessions.entrySet()) {
                            if (e.getKey() == null || e.getValue() == null) {
                                continue;
                            }
                            recentSessions.put(e.getKey(), normalizeRecent(e.getValue()));
                        }
                    }
                    if (blob.tpMessages != null) {
                        tpMessages.putAll(blob.tpMessages);
                    }
                    if (blob.mentorTpMessages != null) {
                        mentorTpMessages.putAll(blob.mentorTpMessages);
                    }
                    dojoSeason = blob.dojoSeason;
                    if (blob.dojoChallenges != null) {
                        for (Map.Entry<String, DojoChallenge> e : blob.dojoChallenges.entrySet()) {
                            DojoChallenge norm = normalizeDojoChallenge(e.getKey(), e.getValue());
                            if (norm != null) {
                                dojoChallenges.put(norm.toDojoUuid, norm);
                            }
                        }
                    }
                    if (blob.dojoProfiles != null) {
                        dojoProfiles.putAll(blob.dojoProfiles);
                    }
                    if (blob.dojoHallOfFame != null) {
                        dojoHallOfFame.addAll(blob.dojoHallOfFame);
                    }
                }
                dirty.set(false);
                AdaptiveDifficultyMod.LOGGER.info(
                        "[{}] SparStore loaded {} bonds from {}",
                        AdaptiveDifficultyMod.MOD_ID, bondsByPlayer.size(), file);
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] SparStore load failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    public synchronized void save() {
        try {
            Path file = path();
            Files.createDirectories(file.getParent());
            Persist blob = new Persist();
            blob.bondsByPlayer = new ConcurrentHashMap<>(bondsByPlayer);
            blob.leaderboard = new ConcurrentHashMap<>(leaderboard);
            blob.invites = new ConcurrentHashMap<>(invites);
            blob.recentSessions = new ConcurrentHashMap<>();
            for (Map.Entry<String, List<RecentSession>> e : recentSessions.entrySet()) {
                blob.recentSessions.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
            blob.tpMessages = new ConcurrentHashMap<>(tpMessages);
            blob.mentorTpMessages = new ConcurrentHashMap<>(mentorTpMessages);
            blob.dojoSeason = dojoSeason;
            blob.dojoChallenges = new ConcurrentHashMap<>(dojoChallenges);
            blob.dojoProfiles = new ConcurrentHashMap<>(dojoProfiles);
            blob.dojoHallOfFame = new ArrayList<>(dojoHallOfFame);
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(blob, writer);
            }
            dirty.set(false);
            lastSaveAt = System.currentTimeMillis();
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] SparStore save failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    public void saveIfNeeded(long now) {
        if (dirty.get() && now - lastSaveAt >= 60_000L) {
            save();
        }
    }

    public MentorBond bond(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        MentorBond bond = bondsByPlayer.computeIfAbsent(uuid.toString(), k -> new MentorBond());
        bond.normalizeApprentices();
        return bond;
    }

    /** Default ON when unset — players see spar TP chat while fighting. */
    public boolean tpMessagesOn(UUID uuid) {
        if (uuid == null) {
            return true;
        }
        Boolean v = tpMessages.get(uuid.toString());
        return v == null || v;
    }

    public void setTpMessages(UUID uuid, boolean on) {
        if (uuid == null) {
            return;
        }
        tpMessages.put(uuid.toString(), on);
        markDirty();
    }

    /** Default ON when unset — mentors see apprentice share TP in chat. */
    public boolean mentorTpMessagesOn(UUID uuid) {
        if (uuid == null) {
            return true;
        }
        Boolean v = mentorTpMessages.get(uuid.toString());
        return v == null || v;
    }

    public void setMentorTpMessages(UUID uuid, boolean on) {
        if (uuid == null) {
            return;
        }
        mentorTpMessages.put(uuid.toString(), on);
        markDirty();
    }

    static DojoChallenge normalizeDojoChallenge(String mapKey, DojoChallenge c) {
        if (c == null) {
            return null;
        }
        String to = canonicalDojoKey(c.toDojoUuid);
        if (to.isEmpty()) {
            to = canonicalDojoKey(mapKey);
        }
        if (to.isEmpty()) {
            return null;
        }
        String from = canonicalDojoKey(c.fromDojoUuid);
        if (from.isEmpty()) {
            from = canonicalDojoKey(c.fromMentorUuid);
        }
        c.toDojoUuid = to;
        if (!from.isEmpty()) {
            c.fromDojoUuid = from;
            c.fromMentorUuid = from;
        }
        return c;
    }

    static String canonicalDojoKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    /** Newest-first snapshot of finished spars for Stats (never null). */
    public List<RecentSession> recentFor(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return List.of();
        }
        List<RecentSession> list = recentSessions.get(uuid);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    /** Push a finished spar onto the front of this player's recent list (cap {@link #RECENT_SESSION_LIMIT}). */
    public void pushRecent(String uuid, RecentSession session) {
        if (uuid == null || uuid.isBlank() || session == null) {
            return;
        }
        List<RecentSession> next = new ArrayList<>();
        next.add(session);
        List<RecentSession> prev = recentSessions.get(uuid);
        if (prev != null) {
            for (RecentSession r : prev) {
                if (next.size() >= RECENT_SESSION_LIMIT) {
                    break;
                }
                if (r != null) {
                    next.add(r);
                }
            }
        }
        recentSessions.put(uuid, next);
        markDirty();
    }

    private static List<RecentSession> normalizeRecent(List<RecentSession> raw) {
        List<RecentSession> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (RecentSession r : raw) {
            if (r == null) {
                continue;
            }
            out.add(r);
            if (out.size() >= RECENT_SESSION_LIMIT) {
                break;
            }
        }
        return out;
    }

    public static final class ApprenticeRef {
        public String uuid = "";
        public String name = "";
    }

    public static final class MentorBond {
        public String mentorUuid = "";
        public String mentorName = "";
        /** Legacy primary apprentice — kept in sync with {@link #apprentices}[0] for older saves/GUI. */
        public String apprenticeUuid = "";
        public String apprenticeName = "";
        /** Dojo roster: one mentor may train many apprentices; each apprentice still has one master. */
        public List<ApprenticeRef> apprentices = new ArrayList<>();
        public long mentorChangeReadyAt;
        public long apprenticeChangeReadyAt;
        public int streakCurrent;
        public int streakBest;
        public long streakLastDay = -999999L;

        /** Migrate legacy single apprentice fields into the roster and keep primary fields synced. */
        public void normalizeApprentices() {
            if (apprentices == null) {
                apprentices = new ArrayList<>();
            }
            apprentices.removeIf(r -> r == null || r.uuid == null || r.uuid.isBlank());
            if (apprentices.isEmpty()
                    && apprenticeUuid != null && !apprenticeUuid.isBlank()) {
                ApprenticeRef legacy = new ApprenticeRef();
                legacy.uuid = apprenticeUuid;
                legacy.name = apprenticeName == null ? "" : apprenticeName;
                apprentices.add(legacy);
            }
            // Dedupe by uuid (case-insensitive), keep first name.
            Map<String, ApprenticeRef> uniq = new java.util.LinkedHashMap<>();
            for (ApprenticeRef r : apprentices) {
                String key = r.uuid.toLowerCase(java.util.Locale.ROOT);
                uniq.putIfAbsent(key, r);
                if (r.name == null) {
                    r.name = "";
                }
            }
            apprentices = new ArrayList<>(uniq.values());
            syncPrimaryApprentice();
        }

        public void syncPrimaryApprentice() {
            if (apprentices == null || apprentices.isEmpty()) {
                apprenticeUuid = "";
                apprenticeName = "";
                return;
            }
            ApprenticeRef first = apprentices.get(0);
            apprenticeUuid = first.uuid == null ? "" : first.uuid;
            apprenticeName = first.name == null ? "" : first.name;
        }

        public int apprenticeCount() {
            normalizeApprentices();
            return apprentices.size();
        }

        public boolean isMentoringUuid(String uuid) {
            if (uuid == null || uuid.isBlank()) {
                return false;
            }
            normalizeApprentices();
            for (ApprenticeRef r : apprentices) {
                if (uuid.equalsIgnoreCase(r.uuid)) {
                    return true;
                }
            }
            return false;
        }

        public boolean addApprentice(String uuid, String name) {
            if (uuid == null || uuid.isBlank()) {
                return false;
            }
            normalizeApprentices();
            if (isMentoringUuid(uuid)) {
                // Refresh display name.
                for (ApprenticeRef r : apprentices) {
                    if (uuid.equalsIgnoreCase(r.uuid)) {
                        if (name != null && !name.isBlank()) {
                            r.name = name;
                        }
                        syncPrimaryApprentice();
                        return true;
                    }
                }
            }
            ApprenticeRef r = new ApprenticeRef();
            r.uuid = uuid;
            r.name = name == null ? "" : name;
            apprentices.add(r);
            syncPrimaryApprentice();
            return true;
        }

        public boolean removeApprenticeUuid(String uuid) {
            if (uuid == null || uuid.isBlank()) {
                return false;
            }
            normalizeApprentices();
            boolean removed = apprentices.removeIf(r -> uuid.equalsIgnoreCase(r.uuid));
            syncPrimaryApprentice();
            return removed;
        }

        public String apprenticeNamesSummary() {
            normalizeApprentices();
            if (apprentices.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (ApprenticeRef r : apprentices) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(r.name == null || r.name.isBlank() ? r.uuid : r.name);
            }
            return sb.toString();
        }
    }

    public static final class BondInvite {
        public String fromUuid = "";
        public String fromName = "";
        public String kind = ""; // mentor | apprentice
        public long expiresAt;
    }

    public static final class LeaderboardEntry {
        public String name = "";
        public double totalTp;
        public long longestMs;
        public double bestPayout;
        public long totalTimeMs;
        public int sessions;
        public int perfectSessions;
        public int highestCombo;
        public int currentStreak;
        public int bestStreak;
    }

    /** One finished spar shown on the Stats board (newest first). */
    public static final class RecentSession {
        public String partnerName = "";
        public String partnerUuid = "";
        public double tp;
        public long durationMs;
        public int maxCombo;
        public boolean perfect;
        public long endedAt;
        public String reason = "";
        /** Session melee damage scored (optional; 0 on older saves). */
        public double melee;
        /** Session ki damage scored (optional; 0 on older saves). */
        public double ki;
    }

    /** One dojo on the season ladder (key = mentor UUID). */
    public static final class DojoEntry {
        public String mentorUuid = "";
        public String mentorName = "";
        public double seasonRp;
        public int wins;
        public int losses;
        public int draws;
        public double totalTp;
        public long totalTimeMs;
        public int sessions;
        public int rosterSize;
        public long updatedAt;
        /** Per-member inter-dojo contribution this season (uuid → stats). */
        public Map<String, DojoMemberStats> members = new ConcurrentHashMap<>();
    }

    public static final class DojoMemberStats {
        public String uuid = "";
        public String name = "";
        public double rpContributed;
        public int wins;
        public int losses;
        public int draws;
        public double tp;
        public int sessions;
    }

    /** Persistent dojo branding — survives season rollovers. */
    public static final class DojoProfile {
        public String displayName = "";
        /** Bukkit material name for GUI banner icon (e.g. WHITE_BANNER). */
        public String bannerMaterial = "WHITE_BANNER";
        public long updatedAt;
    }

    /** Archived season result for Hall of Fame. */
    public static final class DojoSeasonRecord {
        public int seasonId;
        public long endedAt;
        public String championUuid = "";
        public String championName = "";
        public int championRp;
        public String secondName = "";
        public int secondRp;
        public String thirdName = "";
        public int thirdRp;
    }

    public static final class DojoSeason {
        public int seasonId;
        public long startedAt;
        public Map<String, DojoEntry> leaderboard = new ConcurrentHashMap<>();
    }

    /** Dojo war challenge between two mentor-led dojos. */
    public static final class DojoChallenge {
        public String fromDojoUuid = "";
        public String fromDojoName = "";
        public String fromMentorUuid = "";
        public String toDojoUuid = "";
        public String toDojoName = "";
        public long expiresAt;
        public boolean active;
    }

    private static final class Persist {
        Map<String, MentorBond> bondsByPlayer;
        Map<String, LeaderboardEntry> leaderboard;
        Map<String, BondInvite> invites;
        Map<String, List<RecentSession>> recentSessions;
        Map<String, Boolean> tpMessages;
        Map<String, Boolean> mentorTpMessages;
        DojoSeason dojoSeason;
        Map<String, DojoChallenge> dojoChallenges;
        Map<String, DojoProfile> dojoProfiles;
        List<DojoSeasonRecord> dojoHallOfFame;
    }
}
