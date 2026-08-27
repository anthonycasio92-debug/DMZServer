package com.dbzlegacy.adaptivedifficulty.rival;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

/** In-memory rivalry database persisted to {@code config/adaptivedifficulty/rivalry-v4.json}. */
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
        return FMLPaths.CONFIGDIR.get().resolve("adaptivedifficulty").resolve("rivalry-v4.json");
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
                                players.put(e.getKey(), normalize(rec));
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
        RivalPlayerRecord rec = players.get(uuid);
        if (rec == null) {
            rec = RivalPlayerRecord.create(uuid, name, now);
            players.put(uuid, rec);
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
        return uuid == null ? null : players.get(uuid.toString());
    }

    public RivalPlayerRecord get(String uuid) {
        return uuid == null ? null : players.get(uuid);
    }

    public RivalLink getLink(String ownerUuid, String otherUuid) {
        RivalPlayerRecord rec = get(ownerUuid);
        if (rec == null || otherUuid == null) {
            return null;
        }
        return rec.rivals.get(otherUuid);
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
    }

    public List<RivalPlayerRecord> topByRp(int limit) {
        List<RivalPlayerRecord> list = new ArrayList<>(players.values());
        list.sort(Comparator.comparingDouble((RivalPlayerRecord r) -> r.totalRp).reversed());
        if (list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    private static RivalPlayerRecord normalize(RivalPlayerRecord rec) {
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
        Map<String, RivalLink> links = new ConcurrentHashMap<>();
        if (rec.rivals != null) {
            for (Map.Entry<String, RivalLink> e : rec.rivals.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    links.put(e.getKey(), e.getValue());
                }
            }
        }
        rec.rivals = links;
        Map<String, Long> cds = new ConcurrentHashMap<>();
        if (rec.surpassCooldown != null) {
            cds.putAll(rec.surpassCooldown);
        }
        rec.surpassCooldown = cds;
        rec.recalcTotalRp();
        return rec;
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
