package com.dbzlegacy.adaptivedifficulty.rival;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.ConfigPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerPlayer;

/**
 * In-memory rivalry database persisted to {@code config/legacymechanics/rivalry-v4.json}.
 * <p>Mod JSON only — no CustomNPCs (CNPC) script writes. Rival progression (seasons/quests)
 * lives in {@code config/legacymechanics/progression-v4.json} via {@link RivalProgression}.
 */
public final class RivalStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final RivalStore INSTANCE = new RivalStore();

    public final Map<String, RivalPlayerRecord> players = new ConcurrentHashMap<>();
    /** Pending visible declare requests: key = fromUuid + ">" + toUuid */
    public final Map<String, DeclareRequest> declareRequests = new ConcurrentHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private long lastSaveAt;

    public static RivalStore get() {
        return INSTANCE;
    }

    private RivalStore() {}

    public static Path path() {
        return ConfigPaths.rivalryPath();
    }

    public void markDirty() {
        dirty.set(true);
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public synchronized void load() {
        Path file = path();
        try {
            if (!Files.isRegularFile(file)) {
                players.clear();
                declareRequests.clear();
                dirty.set(false);
                return;
            }
            try (Reader reader = Files.newBufferedReader(file)) {
                Persist blob = GSON.fromJson(reader, Persist.class);
                players.clear();
                declareRequests.clear();
                if (blob != null) {
                    if (blob.players != null) {
                        for (Map.Entry<String, RivalPlayerRecord> e : blob.players.entrySet()) {
                            if (e.getKey() != null && e.getValue() != null) {
                                RivalPlayerRecord rec = e.getValue();
                                if (rec.rivals == null) {
                                    // gson may leave null if type erased oddly — recreate
                                }
                                String key = RivalUuid.canonical(e.getKey());
                                players.put(key, normalize(key, rec));
                            }
                        }
                    }
                    if (blob.declareRequests != null) {
                        declareRequests.putAll(blob.declareRequests);
                    }
                }
                dirty.set(false);
                AdaptiveDifficultyMod.LOGGER.info(
                        "[{}] RivalStore loaded {} players from {}",
                        AdaptiveDifficultyMod.MOD_ID, players.size(), file);
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] RivalStore load failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    public synchronized void save() {
        if (!dirty.get() && Files.isRegularFile(path())) {
            // still allow forced saves via callers that mark dirty
        }
        try {
            Path file = path();
            Files.createDirectories(file.getParent());
            Persist blob = new Persist();
            blob.players = new ConcurrentHashMap<>(players);
            blob.declareRequests = new ConcurrentHashMap<>(declareRequests);
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(blob, writer);
            }
            dirty.set(false);
            lastSaveAt = System.currentTimeMillis();
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] RivalStore save failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    public void saveIfNeeded(long now) {
        if (dirty.get() && now - lastSaveAt >= 60_000L) {
            save();
        }
    }

    public RivalPlayerRecord ensurePlayer(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        return ensurePlayer(player.m_20148_().toString(), player.m_7755_().getString());
    }

    public RivalPlayerRecord ensurePlayer(String uuid, String name) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }
        long now = System.currentTimeMillis();
        String canon = RivalUuid.canonical(uuid);
        if (canon == null || canon.isBlank()) {
            return null;
        }
        RivalPlayerRecord rec = get(canon);
        if (rec == null) {
            rec = RivalPlayerRecord.create(canon, name, now);
            players.put(canon, rec);
            markDirty();
        } else {
            if (name != null && !name.isBlank() && !name.equals(rec.name)) {
                rec.name = name;
                markDirty();
            }
            rec.lastSeenAt = now;
        }
        return rec;
    }

    public RivalPlayerRecord get(UUID uuid) {
        return uuid == null ? null : get(uuid.toString());
    }

    public RivalPlayerRecord get(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }
        String canon = RivalUuid.canonical(uuid);
        RivalPlayerRecord rec = canon == null ? null : players.get(canon);
        if (rec != null) {
            return rec;
        }
        rec = players.get(uuid);
        if (rec != null) {
            return rec;
        }
        for (Map.Entry<String, RivalPlayerRecord> e : players.entrySet()) {
            if (e.getKey() != null && RivalUuid.samePlayer(e.getKey(), uuid)) {
                return e.getValue();
            }
        }
        return null;
    }

    public RivalLink getLink(String ownerUuid, String otherUuid) {
        RivalPlayerRecord rec = get(ownerUuid);
        if (rec == null || otherUuid == null || otherUuid.isBlank()) {
            return null;
        }
        RivalLink link = rec.rivals.get(otherUuid);
        if (link != null) {
            return link;
        }
        String canon = RivalUuid.canonical(otherUuid);
        if (canon != null) {
            link = rec.rivals.get(canon);
            if (link != null) {
                return link;
            }
        }
        for (Map.Entry<String, RivalLink> e : rec.rivals.entrySet()) {
            if (e.getKey() != null && RivalUuid.samePlayer(e.getKey(), otherUuid)) {
                return e.getValue();
            }
        }
        return null;
    }

    public RivalLink setLink(String ownerUuid, String otherUuid, RivalLink link) {
        RivalPlayerRecord rec = get(ownerUuid);
        if (rec == null || otherUuid == null || link == null) {
            return null;
        }
        rec.rivals.put(otherUuid, link);
        rec.recalcTotalRp();
        markDirty();
        return link;
    }

    public int addRp(RivalPlayerRecord owner, String rivalUuid, int amount, String reason) {
        if (owner == null || rivalUuid == null || amount == 0) {
            return 0;
        }
        RivalLink link = owner.rivals.get(rivalUuid);
        if (link == null) {
            return 0;
        }
        link.points = Math.max(0.0, link.points + amount);
        link.touch(System.currentTimeMillis());
        owner.recalcTotalRp();
        markDirty();
        return amount;
    }

    public int mutualCount(RivalPlayerRecord record) {
        return record == null ? 0 : record.countMutual();
    }

    public void expireRequests(long now) {
        Iterator<Map.Entry<String, DeclareRequest>> it = declareRequests.entrySet().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Map.Entry<String, DeclareRequest> e = it.next();
            DeclareRequest req = e.getValue();
            if (req == null || now > req.expiresAt) {
                if (req != null) {
                    clearInviteFlags(req.fromUuid, req.toUuid);
                }
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            markDirty();
        }
    }

    public void clearInviteFlags(String fromUuid, String toUuid) {
        RivalLink a = getLink(fromUuid, toUuid);
        RivalLink b = getLink(toUuid, fromUuid);
        if (a != null) {
            a.inviteSent = false;
            a.pendingExpireAt = 0L;
        }
        if (b != null) {
            b.inviteReceived = false;
            b.pendingExpireAt = 0L;
        }
        // Dual Silent Mutual confirm used inviteReceived on both sides.
        if (a != null && b != null
                && a.declaredByMe && a.declaredByThem
                && b.declaredByMe && b.declaredByThem
                && !a.mutual && !b.mutual) {
            a.inviteReceived = false;
            a.inviteSent = false;
            a.acceptedMutualOffer = false;
            a.pendingExpireAt = 0L;
            b.inviteReceived = false;
            b.inviteSent = false;
            b.acceptedMutualOffer = false;
            b.pendingExpireAt = 0L;
        }
    }

    public List<RivalPlayerRecord> topByRp(int limit) {
        return topBy(limit, "rp");
    }

    public List<RivalPlayerRecord> topBy(int limit, String category) {
        String cat = category == null || category.isBlank() ? "rp" : category.trim().toLowerCase();
        List<RivalPlayerRecord> list = new ArrayList<>(players.values());
        list.sort((a, b) -> Double.compare(metric(b, cat), metric(a, cat)));
        if (list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    private static double metric(RivalPlayerRecord r, String cat) {
        if (r == null) {
            return 0.0;
        }
        return switch (cat) {
            case "wins", "win" -> r.officialWins;
            case "streak", "beststreak" -> r.bestWinStreak;
            case "damage", "dmg" -> r.careerDamageDealt;
            case "combo", "hit", "hits" -> cat.startsWith("hit") ? r.careerHits : r.careerHighestCombo;
            case "battles", "battle", "challenges" -> r.challengesPlayed;
            default -> r.totalRp;
        };
    }

    private static RivalPlayerRecord normalize(String ownerKey, RivalPlayerRecord rec) {
        if (ownerKey != null && !ownerKey.isBlank()) {
            rec.uuid = ownerKey;
        } else if (rec.uuid != null && !rec.uuid.isBlank()) {
            rec.uuid = Objects.requireNonNullElse(RivalUuid.canonical(rec.uuid), rec.uuid);
        }
        if (rec.uuid == null) {
            rec.uuid = "";
        }
        if (rec.name == null) {
            rec.name = "";
        }
        if (rec.nemesisUuid == null) {
            rec.nemesisUuid = "";
        }
        // Ensure concurrent maps after Gson
        rec.rivals = canonicalizeLinkMap(rec.rivals);
        Map<String, RivalLink> past = new ConcurrentHashMap<>();
        if (rec.pastRivals != null) {
            for (Map.Entry<String, RivalLink> e : rec.pastRivals.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    String key = RivalUuid.canonical(e.getKey());
                    if (key != null && !key.isBlank()) {
                        RivalLink link = e.getValue();
                        if (link.uuid == null || link.uuid.isBlank()) {
                            link.uuid = key;
                        } else {
                            link.uuid = Objects.requireNonNullElse(RivalUuid.canonical(link.uuid), link.uuid);
                        }
                        past.putIfAbsent(key, link);
                    }
                }
            }
        }
        rec.pastRivals = past;
        Map<String, Long> cds = new ConcurrentHashMap<>();
        if (rec.surpassCooldown != null) {
            cds.putAll(rec.surpassCooldown);
        }
        rec.surpassCooldown = cds;
        rec.recalcTotalRp();
        return rec;
    }

    private static Map<String, RivalLink> canonicalizeLinkMap(Map<String, RivalLink> in) {
        Map<String, RivalLink> links = new ConcurrentHashMap<>();
        if (in == null) {
            return links;
        }
        for (Map.Entry<String, RivalLink> e : in.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                continue;
            }
            String key = RivalUuid.canonical(e.getKey());
            if (key == null || key.isBlank()) {
                continue;
            }
            RivalLink link = e.getValue();
            if (link.uuid == null || link.uuid.isBlank()) {
                link.uuid = key;
            } else {
                link.uuid = Objects.requireNonNullElse(RivalUuid.canonical(link.uuid), link.uuid);
            }
            links.putIfAbsent(key, link);
        }
        return links;
    }

    public static final class DeclareRequest {
        public String fromUuid = "";
        public String fromName = "";
        public String toUuid = "";
        public String toName = "";
        public long createdAt;
        public long expiresAt;
    }

    private static final class Persist {
        Map<String, RivalPlayerRecord> players;
        Map<String, DeclareRequest> declareRequests;
    }
}
