package com.dbzlegacy.adaptivedifficulty.data;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.ConfigPaths;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.rival.ProvingGrounds;
import com.dbzlegacy.adaptivedifficulty.rival.RivalLink;
import com.dbzlegacy.adaptivedifficulty.rival.RivalPlayerRecord;
import com.dbzlegacy.adaptivedifficulty.rival.RivalProgression;
import com.dbzlegacy.adaptivedifficulty.rival.RivalStore;
import com.dbzlegacy.adaptivedifficulty.sparring.SparStore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * One-time CustomNPCs storeddata → LegacyMechanics JSON/NBT migration.
 * <p>
 * World (server start): Rival DB, Rival progression, Spar leaderboard →
 * {@code rivalry-v4.json} / {@code progression-v4.json} / {@code sparring.json},
 * then clears the CNPC world keys.
 * <p>
 * Player (login): Spar bonds/streaks + flight/meditation/potential progress keys →
 * Forge {@code lm_progression} NBT / SparStore, then clears those CNPC player keys.
 * <p>
 * Guarded by {@code config/legacymechanics/cnpc-world-migration.done} and per-player
 * {@code lm_cnpc_player_migrated}. Raw CNPC blobs are copied to
 * {@code config/legacymechanics/cnpc-import-backup/} before clear.
 */
public final class CnpcDataMigrator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final AtomicBoolean WORLD_RAN = new AtomicBoolean(false);

    private static final String MARKER_FILE = "cnpc-world-migration.done";
    private static final String PLAYER_MARK = "lm_cnpc_player_migrated";

    private static final String RIVAL_DB = "dlr.rivalry.v4.database";
    private static final String RIVAL_DB_BAK = "dlr.rivalry.v4.database.backup";
    private static final String RIVAL_PROG = "dlr.rivalry.v4.progression";
    private static final String RIVAL_PROG_BAK = "dlr.rivalry.v4.progression.backup";
    private static final String RIVAL_CH = "dlr.rivalry.v4.challenges";
    private static final String RIVAL_CH_BAK = "dlr.rivalry.v4.challenges.backup";
    private static final String SPAR_LB_NAMES = "spar.leaderboard.names";

    private CnpcDataMigrator() {}

    /** ServerStarted — CNPC worlds are available. Safe to call repeatedly. */
    public static void migrateWorldIfNeeded(MinecraftServer server) {
        if (server == null || !DifficultyConfig.get().enableCnpcDataMigration) {
            return;
        }
        if (!WORLD_RAN.compareAndSet(false, true)) {
            return;
        }
        Path marker = ConfigPaths.dataDir().resolve(MARKER_FILE);
        if (Files.isRegularFile(marker)) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] CNPC world migration already done ({})",
                    AdaptiveDifficultyMod.MOD_ID, marker.getFileName());
            return;
        }
        try {
            Object stored = overworldStoreddata();
            if (stored == null) {
                AdaptiveDifficultyMod.LOGGER.info(
                        "[{}] CNPC world migration skipped — CustomNPCs storeddata unavailable",
                        AdaptiveDifficultyMod.MOD_ID);
                // Do not write marker — retry next boot when CNPC is present.
                WORLD_RAN.set(false);
                return;
            }

            String rivalRaw = storedGet(stored, RIVAL_DB);
            String progRaw = storedGet(stored, RIVAL_PROG);
            String chRaw = storedGet(stored, RIVAL_CH);
            String sparNames = storedGet(stored, SPAR_LB_NAMES);

            boolean hasAnything = notBlank(rivalRaw) || notBlank(progRaw) || notBlank(sparNames)
                    || hasSparLeaderboardKeys(stored);
            if (!hasAnything) {
                writeMarker(marker, "empty-no-cnpc-data");
                AdaptiveDifficultyMod.LOGGER.info(
                        "[{}] CNPC world migration: no Rival/Spar blobs found — marked done",
                        AdaptiveDifficultyMod.MOD_ID);
                return;
            }

            Path backupDir = ConfigPaths.dataDir().resolve("cnpc-import-backup");
            Files.createDirectories(backupDir);
            backupRaw(backupDir, "rivalry-database.json", rivalRaw);
            backupRaw(backupDir, "rivalry-database.backup.json", storedGet(stored, RIVAL_DB_BAK));
            backupRaw(backupDir, "rivalry-progression.json", progRaw);
            backupRaw(backupDir, "rivalry-progression.backup.json", storedGet(stored, RIVAL_PROG_BAK));
            backupRaw(backupDir, "rivalry-challenges.json", chRaw);
            backupRaw(backupDir, "spar-leaderboard-names.txt", sparNames);

            int rivalPlayers = 0;
            if (notBlank(rivalRaw) && RivalStore.get().players.isEmpty()) {
                rivalPlayers = importRivalDatabase(rivalRaw);
            } else if (notBlank(rivalRaw) && !RivalStore.get().players.isEmpty()) {
                AdaptiveDifficultyMod.LOGGER.info(
                        "[{}] CNPC Rival DB present but rivalry-v4.json already has {} players — keeping mod data",
                        AdaptiveDifficultyMod.MOD_ID, RivalStore.get().players.size());
            }

            boolean progImported = false;
            if (notBlank(progRaw) && RivalProgression.get().isImportEmpty()) {
                progImported = importRivalProgression(progRaw);
            }

            int sparLb = importSparLeaderboard(stored, sparNames);

            RivalStore.get().markDirty();
            RivalStore.get().save();
            SparStore.get().markDirty();
            SparStore.get().save();
            if (progImported) {
                RivalProgression.get().save();
            }

            clearWorldCnpcKeys(stored);
            writeMarker(marker, "rivalPlayers=" + rivalPlayers
                    + " prog=" + progImported
                    + " sparLb=" + sparLb
                    + " at=" + System.currentTimeMillis());

            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] CNPC→LM world migration complete: rivalPlayers={} prog={} sparLb={} (CNPC keys cleared; backup in {})",
                    AdaptiveDifficultyMod.MOD_ID, rivalPlayers, progImported, sparLb, backupDir);
        } catch (Throwable t) {
            WORLD_RAN.set(false);
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] CNPC world migration failed (will retry next boot): {}",
                    AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    /**
     * Staff force re-import: ignores marker, still prefers empty mod stores
     * (won't overwrite non-empty rivalry unless {@code forceOverwrite}).
     */
    public static String forceMigrateWorld(MinecraftServer server, boolean forceOverwrite) {
        if (server == null) {
            return "§cNo server.";
        }
        try {
            Path marker = ConfigPaths.dataDir().resolve(MARKER_FILE);
            Files.deleteIfExists(marker);
            WORLD_RAN.set(false);
            if (forceOverwrite) {
                RivalStore.get().players.clear();
                RivalStore.get().declareRequests.clear();
                RivalStore.get().markDirty();
                RivalProgression.get().resetForImport();
                SparStore.get().leaderboard.clear();
                SparStore.get().markDirty();
            }
            migrateWorldIfNeeded(server);
            if (Files.isRegularFile(marker)) {
                return "§aCNPC world migration finished. See server log + §fconfig/legacymechanics/cnpc-import-backup/";
            }
            return "§eMigration did not complete — check log (CNPC may be unavailable).";
        } catch (Throwable t) {
            return "§cMigration error: " + t;
        }
    }

    /** Login — copy player CNPC storeddata into LM, then clear those keys. */
    public static void migratePlayerIfNeeded(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableCnpcDataMigration) {
            return;
        }
        if ("1".equals(ProgressionData.storedGet(player, PLAYER_MARK, ""))) {
            return;
        }
        try {
            Object stored = playerStoreddata(player);
            if (stored == null) {
                // CNPC missing entirely — nothing to import; mark so we don't retry forever.
                if (!cnpcAvailable()) {
                    ProgressionData.storedPut(player, PLAYER_MARK, "1");
                }
                return;
            }

            migrateSparBond(player, stored);
            migrateProgressionKeys(player, stored);

            // Clear migrated player keys
            clearPlayerSparKeys(stored);
            clearPlayerProgressionKeys(stored);

            ProgressionData.storedPut(player, PLAYER_MARK, "1");
            SparStore.get().markDirty();
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] CNPC→LM player migration for {}",
                    AdaptiveDifficultyMod.MOD_ID, player.m_6302_());
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] CNPC player migration soft-fail {}: {}",
                    AdaptiveDifficultyMod.MOD_ID, player.m_6302_(), t.toString());
        }
    }

    /* ========================= Rival import ========================= */

    private static int importRivalDatabase(String raw) {
        JsonObject root = parseObject(raw);
        if (root == null || !root.has("players")) {
            return 0;
        }
        JsonObject players = root.getAsJsonObject("players");
        if (players == null) {
            return 0;
        }
        RivalStore store = RivalStore.get();
        int count = 0;
        for (Map.Entry<String, JsonElement> e : players.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || !e.getValue().isJsonObject()) {
                continue;
            }
            RivalPlayerRecord rec = mapPlayer(e.getKey(), e.getValue().getAsJsonObject());
            if (rec != null) {
                store.players.put(rec.uuid, rec);
                count++;
            }
        }
        // Optional requests → declareRequests
        if (root.has("requests") && root.get("requests").isJsonObject()) {
            JsonObject reqs = root.getAsJsonObject("requests");
            for (Map.Entry<String, JsonElement> e : reqs.entrySet()) {
                if (e.getValue() == null || !e.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject o = e.getValue().getAsJsonObject();
                RivalStore.DeclareRequest req = new RivalStore.DeclareRequest();
                req.fromUuid = str(o, "fromUuid", str(o, "from", ""));
                req.fromName = str(o, "fromName", "");
                req.toUuid = str(o, "toUuid", str(o, "to", ""));
                req.toName = str(o, "toName", "");
                req.createdAt = lng(o, "createdAt", 0L);
                req.expiresAt = lng(o, "expiresAt", 0L);
                if (!req.fromUuid.isBlank() && !req.toUuid.isBlank()) {
                    store.declareRequests.put(req.fromUuid + ">" + req.toUuid, req);
                }
            }
        }
        store.markDirty();
        return count;
    }

    private static RivalPlayerRecord mapPlayer(String uuid, JsonObject o) {
        RivalPlayerRecord r = RivalPlayerRecord.create(uuid, str(o, "name", ""), lng(o, "createdAt", System.currentTimeMillis()));
        r.name = str(o, "name", r.name);
        r.uuid = str(o, "uuid", uuid);
        r.nemesisUuid = str(o, "nemesisUuid", "");
        r.createdAt = lng(o, "createdAt", r.createdAt);
        r.lastSeenAt = lng(o, "lastSeenAt", r.lastSeenAt);
        r.lastChallengeEndAt = lng(o, "lastChallengeEndAt", 0L);
        r.lastDeclareAt = lng(o, "lastDeclareAt", 0L);
        r.lastSurpassAt = lng(o, "lastSurpassAt", 0L);
        r.tpMessages = !o.has("tpMessages") || bool(o, "tpMessages", true);
        r.instinctEnabled = !o.has("instinctEnabled") || bool(o, "instinctEnabled", true);

        JsonObject career = o.has("career") && o.get("career").isJsonObject()
                ? o.getAsJsonObject("career") : o;
        r.totalRp = dbl(career, "rivalPointsTotal", dbl(o, "totalRp", 0.0));
        r.officialWins = (int) lng(career, "officialWins", lng(o, "officialWins", 0L));
        r.officialLosses = (int) lng(career, "officialLosses", lng(o, "officialLosses", 0L));
        r.officialDraws = (int) lng(career, "officialDraws", lng(o, "officialDraws", 0L));
        r.knockouts = (int) lng(career, "knockouts", lng(o, "knockouts", 0L));
        r.challengesPlayed = (int) lng(career, "challengesPlayed", lng(o, "challengesPlayed", 0L));
        r.surpassAwards = (int) lng(career, "surpassAwards", lng(o, "surpassAwards", 0L));
        r.presenceMs = lng(career, "presenceMs", lng(o, "presenceMs", 0L));
        r.currentWinStreak = (int) lng(career, "currentStreak", lng(o, "currentWinStreak", 0L));
        r.bestWinStreak = (int) lng(career, "bestStreak", lng(o, "bestWinStreak", 0L));
        r.careerDamageDealt = dbl(career, "damageDealt", dbl(o, "careerDamageDealt", 0.0));
        r.careerHighestCombo = (int) lng(career, "highestCombo", lng(o, "careerHighestCombo", 0L));
        r.careerHits = (int) lng(o, "careerHits", 0L);

        JsonObject totals = o.has("totals") && o.get("totals").isJsonObject()
                ? o.getAsJsonObject("totals") : o;
        r.declarationsSent = (int) lng(totals, "declarationsSent", 0L);
        r.declarationsAccepted = (int) lng(totals, "declarationsAccepted", 0L);
        r.declarationsDeclined = (int) lng(totals, "declarationsDeclined", 0L);
        r.rivalsRemoved = (int) lng(totals, "rivalsRemoved", 0L);

        r.rivals = mapLinks(o.has("rivals") && o.get("rivals").isJsonObject()
                ? o.getAsJsonObject("rivals") : null);
        r.pastRivals = mapLinks(o.has("pastRivals") && o.get("pastRivals").isJsonObject()
                ? o.getAsJsonObject("pastRivals") : null);
        r.recalcTotalRp();
        return r;
    }

    private static Map<String, RivalLink> mapLinks(JsonObject rivals) {
        Map<String, RivalLink> out = new ConcurrentHashMap<>();
        if (rivals == null) {
            return out;
        }
        for (Map.Entry<String, JsonElement> e : rivals.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || !e.getValue().isJsonObject()) {
                continue;
            }
            JsonObject o = e.getValue().getAsJsonObject();
            RivalLink link = new RivalLink();
            link.uuid = str(o, "uuid", e.getKey());
            link.name = str(o, "name", "");
            link.mutual = bool(o, "mutual", false);
            link.declaredByMe = bool(o, "declaredByMe", false);
            link.declaredByThem = bool(o, "declaredByThem", false);
            link.inviteSent = bool(o, "inviteSent", false);
            link.inviteReceived = bool(o, "inviteReceived", false);
            link.points = dbl(o, "points", 0.0);
            link.wins = (int) lng(o, "wins", 0L);
            link.losses = (int) lng(o, "losses", 0L);
            link.draws = (int) lng(o, "draws", 0L);
            link.deathLosses = (int) lng(o, "deathLosses", 0L);
            link.deathWins = (int) lng(o, "deathWins", 0L);
            link.isNemesis = bool(o, "isNemesis", false);
            link.presenceMs = lng(o, "presenceMs", 0L);
            link.createdAt = lng(o, "createdAt", 0L);
            link.firstMetAt = lng(o, "firstMetAt", 0L);
            link.mutualSince = lng(o, "mutualSince", 0L);
            link.lastBattleAt = lng(o, "lastBattleAt", 0L);
            link.lastInteractAt = lng(o, "lastInteractAt", 0L);
            link.lastSeenTogetherAt = lng(o, "lastSeenTogetherAt", 0L);
            link.lastSurpassAt = lng(o, "lastSurpassAt", 0L);
            link.pendingExpireAt = lng(o, "pendingExpireAt", 0L);
            link.surpassWasBelow = bool(o, "surpassWasBelow", false);
            if (o.has("provingGrounds") && o.get("provingGrounds").isJsonObject()) {
                try {
                    link.provingGrounds = GSON.fromJson(o.get("provingGrounds"), ProvingGrounds.Grounds.class);
                    link.provingGrounds = ProvingGrounds.normalize(link.provingGrounds);
                } catch (Throwable ignored) {
                }
            }
            out.put(e.getKey(), link);
        }
        return out;
    }

    private static boolean importRivalProgression(String raw) {
        try {
            Path file = ConfigPaths.progressionPath();
            Files.createDirectories(file.getParent());
            // Write CNPC blob as-is (schema already aligned with RivalProgression.Persist).
            Files.writeString(file, raw, StandardCharsets.UTF_8);
            RivalProgression.get().load();
            RivalProgression.get().markDirty();
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Rival progression import failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    /* ========================= Spar import ========================= */

    private static int importSparLeaderboard(Object stored, String namesCsv) {
        List<String> names = new ArrayList<>();
        if (notBlank(namesCsv)) {
            for (String part : namesCsv.split(",")) {
                String n = part == null ? "" : part.trim();
                if (!n.isBlank()) {
                    names.add(n);
                }
            }
        }
        // Also scan keys if names index missing
        if (names.isEmpty()) {
            for (String key : storedKeys(stored)) {
                if (key != null && key.startsWith("spar.leaderboard.tp.")) {
                    names.add(key.substring("spar.leaderboard.tp.".length()));
                }
            }
        }
        RivalStore rivals = RivalStore.get();
        int imported = 0;
        for (String nameOrSafe : names) {
            String safe = safeName(nameOrSafe);
            double tp = num(storedGet(stored, "spar.leaderboard.tp." + safe), 0.0);
            if (!(tp > 0) && storedGet(stored, "spar.leaderboard.sessions." + safe) == null) {
                continue;
            }
            String uuid = resolveUuidByName(rivals, nameOrSafe);
            if (uuid == null) {
                // Keep a stable synthetic key so board isn't lost — staff can merge later.
                uuid = "name:" + safe;
            }
            SparStore.LeaderboardEntry lb = SparStore.get().leaderboard.computeIfAbsent(
                    uuid, k -> new SparStore.LeaderboardEntry());
            lb.name = displayName(nameOrSafe, safe);
            lb.totalTp = Math.max(lb.totalTp, tp);
            lb.longestMs = Math.max(lb.longestMs, (long) num(storedGet(stored, "spar.leaderboard.longest." + safe), 0));
            lb.bestPayout = Math.max(lb.bestPayout, num(storedGet(stored, "spar.leaderboard.bestPayout." + safe), 0));
            lb.totalTimeMs = Math.max(lb.totalTimeMs, (long) num(storedGet(stored, "spar.leaderboard.totalTime." + safe), 0));
            lb.sessions = Math.max(lb.sessions, (int) num(storedGet(stored, "spar.leaderboard.sessions." + safe), 0));
            lb.perfectSessions = Math.max(lb.perfectSessions,
                    (int) num(storedGet(stored, "spar.leaderboard.perfectPayouts." + safe), 0));
            lb.highestCombo = Math.max(lb.highestCombo,
                    (int) num(storedGet(stored, "spar.leaderboard.highestCombo." + safe), 0));
            lb.currentStreak = Math.max(lb.currentStreak,
                    (int) num(storedGet(stored, "spar.leaderboard.currentStreak." + safe), 0));
            lb.bestStreak = Math.max(lb.bestStreak,
                    (int) num(storedGet(stored, "spar.leaderboard.bestStreak." + safe), 0));
            imported++;
        }
        if (imported > 0) {
            SparStore.get().markDirty();
        }
        return imported;
    }

    private static void migrateSparBond(ServerPlayer player, Object stored) {
        String mentorName = storedGet(stored, "spar.bond.mentorName");
        String apprenticeName = storedGet(stored, "spar.bond.apprenticeName");
        long mentorCd = (long) num(storedGet(stored, "spar.bond.mentorChangeReadyAt"), 0);
        long apprenticeCd = (long) num(storedGet(stored, "spar.bond.apprenticeChangeReadyAt"), 0);
        int streakCur = (int) num(storedGet(stored, "spar.streak.current"), 0);
        int streakBest = (int) num(storedGet(stored, "spar.streak.best"), 0);
        String streakDay = storedGet(stored, "spar.streak.lastDay");

        boolean any = notBlank(mentorName) || notBlank(apprenticeName) || streakCur > 0 || streakBest > 0;
        if (!any) {
            return;
        }
        SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
        if (notBlank(mentorName) && (bond.mentorName == null || bond.mentorName.isBlank())) {
            bond.mentorName = mentorName;
            String mu = resolveUuidByName(RivalStore.get(), mentorName);
            if (mu != null) {
                bond.mentorUuid = mu;
            }
        }
        if (notBlank(apprenticeName) && (bond.apprenticeName == null || bond.apprenticeName.isBlank())) {
            bond.apprenticeName = apprenticeName;
            String au = resolveUuidByName(RivalStore.get(), apprenticeName);
            if (au != null) {
                bond.apprenticeUuid = au;
            }
        }
        bond.mentorChangeReadyAt = Math.max(bond.mentorChangeReadyAt, mentorCd);
        bond.apprenticeChangeReadyAt = Math.max(bond.apprenticeChangeReadyAt, apprenticeCd);
        bond.streakCurrent = Math.max(bond.streakCurrent, streakCur);
        bond.streakBest = Math.max(bond.streakBest, streakBest);
        if (notBlank(streakDay)) {
            try {
                bond.streakLastDay = Long.parseLong(streakDay.trim());
            } catch (Exception ignored) {
            }
        }
    }

    private static void migrateProgressionKeys(ServerPlayer player, Object stored) {
        // Copy known training keys if LM NBT is empty for that key.
        for (String key : storedKeys(stored)) {
            if (key == null) {
                continue;
            }
            if (!(key.startsWith("fly_training_")
                    || key.startsWith("potentialunlock_")
                    || key.startsWith("meditation_restore_")
                    || key.startsWith("med2_")
                    || key.startsWith("potential_"))) {
                continue;
            }
            String existing = ProgressionData.storedGet(player, key, null);
            if (existing != null && !existing.isBlank()) {
                continue;
            }
            String v = storedGet(stored, key);
            if (v != null) {
                ProgressionData.storedPut(player, key, v);
            }
        }
        // Kill-TP chat pref shared with End / Rival messaging
        String killTp = storedGet(stored, "dmz_kill_tp_chat");
        if (killTp != null) {
            ProgressionData.storedPut(player, "dmz_kill_tp_chat", killTp);
            // Keep CNPC copy too — End scripts / other packs may still read it.
        }
    }

    /* ========================= Clear ========================= */

    private static void clearWorldCnpcKeys(Object stored) {
        storedRemove(stored, RIVAL_DB);
        storedRemove(stored, RIVAL_DB_BAK);
        storedRemove(stored, RIVAL_PROG);
        storedRemove(stored, RIVAL_PROG_BAK);
        storedRemove(stored, RIVAL_CH);
        storedRemove(stored, RIVAL_CH_BAK);
        storedRemove(stored, SPAR_LB_NAMES);
        List<String> toRemove = new ArrayList<>();
        for (String key : storedKeys(stored)) {
            if (key == null) {
                continue;
            }
            if (key.startsWith("spar.leaderboard.")
                    || key.startsWith("dlr.rivalry.v4.challenge.announce.")
                    || key.startsWith("dlr.rivalry.v4.challenge.end.")) {
                toRemove.add(key);
            }
        }
        for (String key : toRemove) {
            storedRemove(stored, key);
        }
    }

    private static void clearPlayerSparKeys(Object stored) {
        storedRemove(stored, "spar.bond.mentorName");
        storedRemove(stored, "spar.bond.apprenticeName");
        storedRemove(stored, "spar.bond.mentorChangeReadyAt");
        storedRemove(stored, "spar.bond.apprenticeChangeReadyAt");
        storedRemove(stored, "spar.bond.inviteFrom");
        storedRemove(stored, "spar.bond.inviteKind");
        storedRemove(stored, "spar.bond.inviteUntil");
        storedRemove(stored, "spar.streak.current");
        storedRemove(stored, "spar.streak.best");
        storedRemove(stored, "spar.streak.lastDay");
    }

    private static void clearPlayerProgressionKeys(Object stored) {
        List<String> toRemove = new ArrayList<>();
        for (String key : storedKeys(stored)) {
            if (key == null) {
                continue;
            }
            if (key.startsWith("fly_training_")
                    || key.startsWith("potentialunlock_")
                    || key.startsWith("meditation_restore_")
                    || key.startsWith("med2_")
                    || key.startsWith("potential_")) {
                toRemove.add(key);
            }
        }
        for (String key : toRemove) {
            storedRemove(stored, key);
        }
    }

    private static boolean hasSparLeaderboardKeys(Object stored) {
        for (String key : storedKeys(stored)) {
            if (key != null && key.startsWith("spar.leaderboard.")) {
                return true;
            }
        }
        return false;
    }

    /* ========================= CNPC reflection ========================= */

    private static boolean cnpcAvailable() {
        try {
            Class<?> npcApi = Class.forName("noppes.npcs.api.NpcAPI");
            Object available = npcApi.getMethod("IsAvailable").invoke(null);
            return available instanceof Boolean ok && ok;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object overworldStoreddata() {
        try {
            Class<?> npcApi = Class.forName("noppes.npcs.api.NpcAPI");
            Object available = npcApi.getMethod("IsAvailable").invoke(null);
            if (!(available instanceof Boolean ok) || !ok) {
                return null;
            }
            Object api = npcApi.getMethod("Instance").invoke(null);
            Object world = null;
            try {
                world = api.getClass().getMethod("getIWorld", String.class).invoke(api, "minecraft:overworld");
            } catch (Throwable ignored) {
            }
            if (world == null) {
                try {
                    world = api.getClass().getMethod("getIWorld", String.class).invoke(api, "overworld");
                } catch (Throwable ignored) {
                }
            }
            if (world == null) {
                return null;
            }
            return world.getClass().getMethod("getStoreddata").invoke(world);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object playerStoreddata(ServerPlayer player) {
        try {
            Class<?> npcApi = Class.forName("noppes.npcs.api.NpcAPI");
            Object available = npcApi.getMethod("IsAvailable").invoke(null);
            if (!(available instanceof Boolean ok) || !ok) {
                return null;
            }
            Object api = npcApi.getMethod("Instance").invoke(null);
            Object entity = api.getClass()
                    .getMethod("getIEntity", net.minecraft.world.entity.Entity.class)
                    .invoke(api, player);
            if (entity == null) {
                return null;
            }
            return entity.getClass().getMethod("getStoreddata").invoke(entity);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String storedGet(Object stored, String key) {
        if (stored == null || key == null) {
            return null;
        }
        try {
            Object has = stored.getClass().getMethod("has", String.class).invoke(stored, key);
            if (has instanceof Boolean b && !b) {
                return null;
            }
            Object v = stored.getClass().getMethod("get", String.class).invoke(stored, key);
            return v == null ? null : String.valueOf(v);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void storedRemove(Object stored, String key) {
        if (stored == null || key == null) {
            return;
        }
        try {
            stored.getClass().getMethod("remove", String.class).invoke(stored, key);
        } catch (Throwable t1) {
            try {
                // Some CNPC builds use put("",…) or clear via put null
                stored.getClass().getMethod("put", String.class, String.class).invoke(stored, key, "");
            } catch (Throwable ignored) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> storedKeys(Object stored) {
        List<String> out = new ArrayList<>();
        if (stored == null) {
            return out;
        }
        try {
            Object keys = stored.getClass().getMethod("getKeys").invoke(stored);
            if (keys instanceof Iterable<?> it) {
                for (Object o : it) {
                    if (o != null) {
                        out.add(String.valueOf(o));
                    }
                }
                return out;
            }
            if (keys instanceof String[] arr) {
                for (String s : arr) {
                    if (s != null) {
                        out.add(s);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    /* ========================= helpers ========================= */

    private static void backupRaw(Path dir, String name, String raw) {
        if (!notBlank(raw)) {
            return;
        }
        try {
            Files.writeString(dir.resolve(name), raw, StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
        }
    }

    private static void writeMarker(Path marker, String note) throws Exception {
        Files.writeString(marker, note == null ? "done" : note, StandardCharsets.UTF_8);
    }

    private static JsonObject parseObject(String raw) {
        try {
            JsonElement el = JsonParser.parseString(raw);
            return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String resolveUuidByName(RivalStore store, String name) {
        if (name == null || name.isBlank() || store == null) {
            return null;
        }
        String want = name.trim().toLowerCase(Locale.ROOT);
        for (RivalPlayerRecord rec : store.players.values()) {
            if (rec != null && rec.name != null && rec.name.toLowerCase(Locale.ROOT).equals(want)) {
                return rec.uuid;
            }
        }
        return null;
    }

    private static String safeName(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceAll("[^A-Za-z0-9_\\-]", "_").toLowerCase(Locale.ROOT);
    }

    private static String displayName(String original, String safe) {
        if (original == null || original.isBlank()) {
            return safe;
        }
        return original;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String str(JsonObject o, String key, String fallback) {
        try {
            if (o != null && o.has(key) && !o.get(key).isJsonNull()) {
                return o.get(key).getAsString();
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static long lng(JsonObject o, String key, long fallback) {
        try {
            if (o != null && o.has(key) && !o.get(key).isJsonNull()) {
                return o.get(key).getAsLong();
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static double dbl(JsonObject o, String key, double fallback) {
        try {
            if (o != null && o.has(key) && !o.get(key).isJsonNull()) {
                return o.get(key).getAsDouble();
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static boolean bool(JsonObject o, String key, boolean fallback) {
        try {
            if (o != null && o.has(key) && !o.get(key).isJsonNull()) {
                return o.get(key).getAsBoolean();
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static double num(String raw, double fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
