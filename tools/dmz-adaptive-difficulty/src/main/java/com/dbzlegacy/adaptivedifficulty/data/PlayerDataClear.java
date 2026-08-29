package com.dbzlegacy.adaptivedifficulty.data;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.rival.RivalPlayerRecord;
import com.dbzlegacy.adaptivedifficulty.rival.RivalProgression;
import com.dbzlegacy.adaptivedifficulty.rival.RivalStore;
import com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty;
import com.dbzlegacy.adaptivedifficulty.sparring.SparStore;
import com.dbzlegacy.adaptivedifficulty.tick.NearbyMobScaler;
import com.dbzlegacy.adaptivedifficulty.tick.ScaledMobTracker;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Staff wipe of one player's LegacyMechanics data
 * ({@code /lm admin clear <player> [all|rival|spar|difficulty|progression]}).
 */
public final class PlayerDataClear {
    private PlayerDataClear() {}

    /**
     * @param playerArg online name, offline name known to RivalStore, or UUID string
     * @param scope     {@code all} (default), {@code rival}, {@code spar}, {@code difficulty},
     *                  {@code progression}
     */
    public static String clear(MinecraftServer server, String playerArg, String scope) {
        if (playerArg == null || playerArg.isBlank()) {
            return "§cUsage: /lm admin clear <player> [all|rival|spar|difficulty|progression]";
        }
        String sc = scope == null || scope.isBlank() ? "all" : scope.toLowerCase(Locale.ROOT).trim();
        Resolved target = resolve(server, playerArg.trim());
        if (target == null) {
            return "§cPlayer not found: §f" + playerArg.trim()
                    + "\n§8Online name, RivalStore name, or UUID.";
        }

        List<String> done = new ArrayList<>();
        boolean all = "all".equals(sc) || "lm".equals(sc) || "*".equals(sc);
        try {
            if (all || "rival".equals(sc) || "rivals".equals(sc) || "rivalry".equals(sc)) {
                done.add(clearRival(target.uuid, target.name));
            }
            if (all || "spar".equals(sc) || "sparring".equals(sc)) {
                done.add(clearSpar(target.uuid, target.name));
            }
            if (all || "difficulty".equals(sc) || "diff".equals(sc) || "ad".equals(sc)) {
                done.add(clearDifficulty(target.online));
            }
            if (all || "progression".equals(sc) || "prog".equals(sc)
                    || "skills".equals(sc) || "meditation".equals(sc)) {
                done.add(clearProgression(target.online, target.uuid));
            }
            if (done.isEmpty()) {
                return "§cUnknown scope: §f" + sc
                        + "\n§8Use all · rival · spar · difficulty · progression";
            }
            String who = target.name == null || target.name.isBlank() ? target.uuid : target.name;
            StringBuilder sb = new StringBuilder("§aCleared LM data for §f").append(who);
            if (target.online == null) {
                sb.append("\n§7(offline — difficulty/progression NBT skipped if not loaded)");
            }
            for (String line : done) {
                if (line != null && !line.isBlank()) {
                    sb.append('\n').append(line);
                }
            }
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Staff cleared player data uuid={} name={} scope={}",
                    AdaptiveDifficultyMod.MOD_ID, target.uuid, target.name, sc);
            return sb.toString();
        } catch (Throwable t) {
            return "§cClear failed: " + t;
        }
    }

    private static String clearRival(String uuid, String name) {
        RivalStore store = RivalStore.get();
        RivalPlayerRecord removed = store.players.remove(uuid);
        int linkScrubs = 0;
        for (RivalPlayerRecord other : store.players.values()) {
            if (other == null) {
                continue;
            }
            if (other.rivals != null && other.rivals.remove(uuid) != null) {
                linkScrubs++;
                other.recalcTotalRp();
            }
            if (other.pastRivals != null && other.pastRivals.remove(uuid) != null) {
                linkScrubs++;
            }
            if (uuid.equalsIgnoreCase(other.nemesisUuid)) {
                other.nemesisUuid = "";
                linkScrubs++;
            }
        }
        int reqs = 0;
        Iterator<Map.Entry<String, RivalStore.DeclareRequest>> it =
                store.declareRequests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, RivalStore.DeclareRequest> e = it.next();
            RivalStore.DeclareRequest r = e.getValue();
            if (r != null && (uuid.equalsIgnoreCase(r.fromUuid) || uuid.equalsIgnoreCase(r.toUuid))) {
                it.remove();
                reqs++;
            }
        }
        // Season / achievements / journal for this uuid
        try {
            RivalProgression prog = RivalProgression.get();
            prog.clearPlayer(uuid);
        } catch (Throwable ignored) {
        }
        store.markDirty();
        store.save();
        boolean had = removed != null;
        return "§7Rival: " + (had ? "§arecord removed" : "§eno record")
                + " §8· links scrubbed §f" + linkScrubs
                + " §8· requests §f" + reqs
                + (name != null && !name.isBlank() ? " §8(" + name + ")" : "");
    }

    private static String clearSpar(String uuid, String name) {
        SparStore store = SparStore.get();
        SparStore.MentorBond bond = store.bondsByPlayer.remove(uuid);
        // Break reverse mentor/apprentice pointers
        int reverse = 0;
        if (bond != null) {
            if (bond.mentorUuid != null && !bond.mentorUuid.isBlank()) {
                SparStore.MentorBond mentor = store.bondsByPlayer.get(bond.mentorUuid);
                if (mentor != null && uuid.equalsIgnoreCase(mentor.apprenticeUuid)) {
                    mentor.apprenticeUuid = "";
                    mentor.apprenticeName = "";
                    reverse++;
                }
            }
            if (bond.apprenticeUuid != null && !bond.apprenticeUuid.isBlank()) {
                SparStore.MentorBond app = store.bondsByPlayer.get(bond.apprenticeUuid);
                if (app != null && uuid.equalsIgnoreCase(app.mentorUuid)) {
                    app.mentorUuid = "";
                    app.mentorName = "";
                    reverse++;
                }
            }
        }
        int invites = 0;
        if (store.invites.remove(uuid) != null) {
            invites++;
        }
        Iterator<Map.Entry<String, SparStore.BondInvite>> invIt =
                store.invites.entrySet().iterator();
        while (invIt.hasNext()) {
            SparStore.BondInvite inv = invIt.next().getValue();
            if (inv != null && uuid.equalsIgnoreCase(inv.fromUuid)) {
                invIt.remove();
                invites++;
            }
        }
        SparStore.LeaderboardEntry lb = store.leaderboard.remove(uuid);
        int nameKeys = 0;
        if (name != null && !name.isBlank()) {
            String safe = name.replaceAll("[^A-Za-z0-9_\\-]", "_").toLowerCase(Locale.ROOT);
            if (store.leaderboard.remove("name:" + safe) != null) {
                nameKeys++;
            }
            // Also drop entries keyed only by matching display name
            Iterator<Map.Entry<String, SparStore.LeaderboardEntry>> it =
                    store.leaderboard.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, SparStore.LeaderboardEntry> e = it.next();
                SparStore.LeaderboardEntry ent = e.getValue();
                if (ent != null && ent.name != null && ent.name.equalsIgnoreCase(name)) {
                    it.remove();
                    nameKeys++;
                }
            }
        }
        store.markDirty();
        store.save();
        return "§7Spar: " + (bond != null ? "§abond cleared" : "§eno bond")
                + " §8· LB §f" + (lb != null ? "removed" : "none")
                + " §8· name keys §f" + nameKeys
                + " §8· invites §f" + invites
                + " §8· reverse §f" + reverse;
    }

    private static String clearDifficulty(ServerPlayer online) {
        if (online == null) {
            return "§7Difficulty: §eskipped (player offline)";
        }
        PlayerDifficultyData data = DifficultyCache.data(online);
        data.resetTemporary();
        // Full staff wipe of unlock ladder + titles for this character.
        for (Integer id : new ArrayList<>(data.getUnlockedTiers())) {
            data.revokeTier(id);
        }
        try {
            TitleSystem.clear(online);
        } catch (Throwable ignored) {
        }
        DmzProgression.clearBaseFormLevel(online.m_20148_());
        DifficultyCache.save(online);
        DifficultyCache.refresh(online);
        DifficultyCache.invalidate(online.m_20148_());
        try {
            ScaledMobTracker.releaseAndRevertPlayer(online);
            NearbyMobScaler.processEvictions();
            AreaDifficulty.clearCache();
        } catch (Throwable ignored) {
        }
        return "§7Difficulty: §aactive + unlocks + titles cleared";
    }

    private static String clearProgression(ServerPlayer online, String uuid) {
        try {
            UUID id = UUID.fromString(uuid);
            ProgressionData.clearPlayer(id);
            try {
                com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem.clearPlayer(id);
            } catch (Throwable ignored) {
            }
            try {
                com.dbzlegacy.adaptivedifficulty.progression.combat.KiWeapons.clearPlayer(id);
                com.dbzlegacy.adaptivedifficulty.progression.combat.PiercingBonus.clearPlayer(id);
            } catch (Throwable ignored) {
            }
            try {
                com.dbzlegacy.adaptivedifficulty.progression.dummy.ShadowDummyLimiter.clearPlayer(id);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        if (online == null) {
            return "§7Progression: §etemp cleared; NBT skipped (offline)";
        }
        try {
            com.dbzlegacy.adaptivedifficulty.progression.ProgressionSystem.onLogout(online);
        } catch (Throwable ignored) {
        }
        // Wipe lm_progression NBT bag (flight / meditation / potential / migrate mark).
        try {
            var tag = com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess.get(online);
            if (tag != null && com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess.isWritable(tag)) {
                tag.m_128473_("lm_progression");
            }
        } catch (Throwable ignored) {
        }
        // Allow CNPC→LM player migrate to run again if CNPC keys remain.
        ProgressionData.storedRemove(online, "lm_cnpc_player_migrated");
        ProgressionData.clearPlayer(online.m_20148_());
        return "§7Progression: §aNBT + temp cleared (re-migrate allowed)";
    }

    private static Resolved resolve(MinecraftServer server, String raw) {
        // UUID?
        try {
            UUID id = UUID.fromString(raw);
            ServerPlayer online = server == null ? null : server.m_6846_().m_11259_(id);
            String name = online != null ? online.m_6302_() : null;
            if (name == null) {
                RivalPlayerRecord rec = RivalStore.get().get(id.toString());
                if (rec != null) {
                    name = rec.name;
                }
            }
            return new Resolved(id.toString(), name == null ? raw : name, online);
        } catch (IllegalArgumentException ignored) {
        }

        // Online by name
        if (server != null) {
            ServerPlayer exact = server.m_6846_().m_11255_(raw);
            if (exact != null) {
                return new Resolved(exact.m_20148_().toString(), exact.m_6302_(), exact);
            }
            String want = raw.toLowerCase(Locale.ROOT);
            for (ServerPlayer p : server.m_6846_().m_11314_()) {
                if (p.m_6302_().toLowerCase(Locale.ROOT).equals(want)) {
                    return new Resolved(p.m_20148_().toString(), p.m_6302_(), p);
                }
            }
        }

        // Offline via RivalStore name
        String want = raw.toLowerCase(Locale.ROOT);
        for (RivalPlayerRecord rec : RivalStore.get().players.values()) {
            if (rec != null && rec.name != null && rec.name.toLowerCase(Locale.ROOT).equals(want)) {
                return new Resolved(rec.uuid, rec.name, null);
            }
        }
        return null;
    }

    private record Resolved(String uuid, String name, ServerPlayer online) {}
}
