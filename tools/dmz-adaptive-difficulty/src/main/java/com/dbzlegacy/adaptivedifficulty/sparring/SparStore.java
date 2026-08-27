package com.dbzlegacy.adaptivedifficulty.sparring;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.ConfigPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Mentor bonds + sparring leaderboard persistence. */
public final class SparStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SparStore INSTANCE = new SparStore();

    public final Map<String, MentorBond> bondsByPlayer = new ConcurrentHashMap<>();
    public final Map<String, LeaderboardEntry> leaderboard = new ConcurrentHashMap<>();
    public final Map<String, BondInvite> invites = new ConcurrentHashMap<>();
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
                dirty.set(false);
                return;
            }
            try (Reader reader = Files.newBufferedReader(file)) {
                Persist blob = GSON.fromJson(reader, Persist.class);
                bondsByPlayer.clear();
                leaderboard.clear();
                invites.clear();
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

    private static final class Persist {
        Map<String, MentorBond> bondsByPlayer;
        Map<String, LeaderboardEntry> leaderboard;
        Map<String, BondInvite> invites;
    }
}
