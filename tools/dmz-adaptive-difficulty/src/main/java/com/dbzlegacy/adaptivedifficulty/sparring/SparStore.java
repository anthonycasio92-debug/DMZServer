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
                if (blob != null) {
                    if (blob.bondsByPlayer != null) {
                        bondsByPlayer.putAll(blob.bondsByPlayer);
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
        return bondsByPlayer.computeIfAbsent(uuid.toString(), k -> new MentorBond());
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

    public static final class MentorBond {
        public String mentorUuid = "";
        public String mentorName = "";
        public String apprenticeUuid = "";
        public String apprenticeName = "";
        public long mentorChangeReadyAt;
        public long apprenticeChangeReadyAt;
        public int streakCurrent;
        public int streakBest;
        public long streakLastDay = -999999L;
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

    private static final class Persist {
        Map<String, MentorBond> bondsByPlayer;
        Map<String, LeaderboardEntry> leaderboard;
        Map<String, BondInvite> invites;
        Map<String, List<RecentSession>> recentSessions;
        Map<String, Boolean> tpMessages;
    }
}
