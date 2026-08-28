package com.dbzlegacy.adaptivedifficulty.rival;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Facade for Rival System 4.7.10 ported into LegacyMechanics.
 */
public final class RivalSystem {
    private RivalSystem() {}

    public static void markMobKill(ServerPlayer killer) {
        if (!DifficultyConfig.get().enableRivalSystem || killer == null) {
            return;
        }
        RivalProximity.markMobKill(killer.m_20148_());
        RivalFusion.onMobKill(killer);
    }

    public static void onMobKillNear(ServerPlayer killer, LivingEntity victim) {
        if (!DifficultyConfig.get().enableRivalSystem || killer == null) {
            return;
        }
        RivalProximity.markMobKill(killer.m_20148_());
        RivalProximity.handleKillNearRivals(killer, victim);
        RivalFusion.onMobKill(killer);
    }

    public static void onLogin(ServerPlayer player) {
        if (!DifficultyConfig.get().enableRivalSystem || player == null) {
            return;
        }
        RivalStore.get().ensurePlayer(player);
    }

    public static void onLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID id = player.m_20148_();
        RivalChallengeManager.get().cleanupPlayer(id);
        RivalProximity.clearPlayer(id);
        RivalInstinct.clearPlayer(id);
        RivalSpectator.clear(id);
        RivalFusion.clear(id);
    }

    public static void pulse(MinecraftServer server, int tick) {
        if (!DifficultyConfig.get().enableRivalSystem || server == null) {
            return;
        }
        long now = System.currentTimeMillis();
        RivalStore.get().expireRequests(now);
        RivalChallengeManager.get().pulse(server, now);
        RivalProximity.pulse(server, now);
        RivalInstinct.pulse(server, now);
        RivalSpectator.pulse(server, now);
        RivalFusion.pulse(server, now);
        RivalStore.get().saveIfNeeded(now);
        RivalProgression.get().saveIfNeeded(now);
    }

    public static void onPlayerHurt(ServerPlayer victim, ServerPlayer attacker, DamageSource source) {
        if (!DifficultyConfig.get().enableRivalSystem) {
            return;
        }
        if (victim == null || attacker == null) {
            return;
        }
        if (DifficultyConfig.get().rivalChallenges) {
            RivalChallengeManager.get().queueHurt(victim, attacker, DmzRewards.isKiDamage(source));
        }
        RivalProximity.handleDamagedByRival(victim, attacker);
    }

    public static void onDeath(ServerPlayer victim, LivingEntity killerEntity) {
        if (!DifficultyConfig.get().enableRivalSystem || victim == null) {
            return;
        }
        ServerPlayer killer = killerEntity instanceof ServerPlayer sp ? sp : null;
        RivalChallengeManager.get().onDeath(victim, killer);
        if (killer != null) {
            registerNemesisDeath(victim, killer);
            RivalProximity.handleKillNearRivals(killer, victim);
        }
    }

    private static void registerNemesisDeath(ServerPlayer victim, ServerPlayer killer) {
        RivalStore store = RivalStore.get();
        RivalPlayerRecord vRec = store.ensurePlayer(victim);
        RivalPlayerRecord kRec = store.ensurePlayer(killer);
        if (vRec == null || kRec == null) {
            return;
        }
        RivalLink vLink = vRec.rivals.get(kRec.uuid);
        RivalLink kLink = kRec.rivals.get(vRec.uuid);
        if (vLink == null || !vLink.mutual) {
            return;
        }
        long now = System.currentTimeMillis();
        vLink.deathLosses++;
        vLink.losses++;
        vLink.touch(now);
        if (kLink != null) {
            kLink.deathWins++;
            kLink.wins++;
            kLink.touch(now);
        }
        if (vLink.deathLosses >= RivalConstants.NEMESIS_DEATH_LOSSES) {
            vLink.isNemesis = true;
            vRec.nemesisUuid = kRec.uuid;
            DmzRewards.msg(victim, "§c[Rival] " + killer.m_7755_().getString()
                    + " is now your Nemesis (" + vLink.deathLosses + " death losses).");
            DmzRewards.msg(killer, "§6[Rival] You became Nemesis to "
                    + victim.m_7755_().getString() + ".");
        }
        store.markDirty();
    }

    /* ========================= Command API ========================= */

    public static String silentRival(ServerPlayer player, ServerPlayer target) {
        if (player == null || target == null) {
            return "§cPlayer not found.";
        }
        if (player.m_20148_().equals(target.m_20148_())) {
            return "§cYou cannot rival yourself.";
        }
        RivalStore store = RivalStore.get();
        RivalPlayerRecord me = store.ensurePlayer(player);
        RivalPlayerRecord them = store.ensurePlayer(target);
        long now = System.currentTimeMillis();
        RivalLink myLink = me.getOrCreateLink(them.uuid, them.name, now);
        restorePastIfAny(me, them, myLink);

        if (myLink.mutual) {
            return "§eAlready mutual rivals with " + them.name + ".";
        }
        if (myLink.inviteSent || myLink.inviteReceived) {
            return "§cA visible declare is already pending with that player.";
        }
        // Already Declared (both silently rivaled) — tell them how to Mutual.
        if (isReciprocatedSilent(myLink)) {
            return "§eAlready Declared with §f" + them.name
                    + "§e. §8For Mutual: Actions → Accept…";
        }
        // Already one-sided silent.
        if (myLink.declaredByMe && !myLink.declaredByThem) {
            return "§eAlready silently rivaled §f" + them.name
                    + " §8(Unknown). §7They are not notified. For Mutual: Declare.";
        }

        RivalLink theirLink = them.rivals.get(me.uuid);
        // They already silently rivaled you → crossed silent → Declared for both.
        if (theirLink != null && theirLink.declaredByMe && !theirLink.mutual
                && !theirLink.inviteSent && !theirLink.inviteReceived) {
            restorePastIfAny(them, me, theirLink);
            promoteDeclared(player, target, me, them, myLink, theirLink, now);
            store.markDirty();
            return "§aDeclared with §f" + them.name
                    + "§a — you both silently rivaled each other."
                    + " §8For Mutual: Actions → Accept…";
        }

        // One-sided silent: you get Unknown benefits; target is not notified / sees nothing.
        myLink.declaredByMe = true;
        myLink.declaredByThem = false;
        myLink.inviteSent = false;
        myLink.inviteReceived = false;
        myLink.mutual = false;
        myLink.touch(now);
        me.declarationsSent++;
        store.markDirty();
        return "§aSilent rival on §f" + them.name + " §8[Unknown]"
                + "\n§8They are not notified and do not see you."
                + "\n§8If they Silent you too, it becomes Declared. For Mutual: Declare.";
    }

    public static String declare(ServerPlayer player, ServerPlayer target) {
        if (player == null || target == null) {
            return "§cPlayer not found.";
        }
        if (player.m_20148_().equals(target.m_20148_())) {
            return "§cYou cannot declare yourself.";
        }
        RivalStore store = RivalStore.get();
        RivalPlayerRecord me = store.ensurePlayer(player);
        RivalPlayerRecord them = store.ensurePlayer(target);
        long now = System.currentTimeMillis();
        if (now - me.lastDeclareAt < RivalConstants.DECLARE_COOLDOWN_MS) {
            return "§cDeclare cooldown (" + ((RivalConstants.DECLARE_COOLDOWN_MS - (now - me.lastDeclareAt)) / 1000L) + "s).";
        }
        RivalLink myLink = me.getOrCreateLink(them.uuid, them.name, now);
        RivalLink theirLink = them.getOrCreateLink(me.uuid, me.name, now);
        restorePastIfAny(me, them, myLink);
        restorePastIfAny(them, me, theirLink);
        if (myLink.mutual) {
            return "§eAlready mutual rivals with " + them.name + ".";
        }
        // If they already sent a visible declare, accepting path.
        if (theirLink.inviteSent || myLink.inviteReceived) {
            return accept(player, target.m_7755_().getString());
        }
        if (myLink.inviteSent) {
            return "§eDeclare already pending for " + them.name + ".";
        }

        String declaredNote = "";
        if (isReciprocatedSilent(myLink)) {
            declaredNote = " §8(was Declared — sending visible declare for Mutual)";
        }

        // You keep benefits (declaredByMe). They get invite only — no benefits until accept.
        myLink.declaredByMe = true;
        myLink.inviteSent = true;
        myLink.pendingExpireAt = now + RivalConstants.REQUEST_EXPIRE_MS;
        myLink.mutual = false;
        theirLink.inviteReceived = true;
        theirLink.pendingExpireAt = myLink.pendingExpireAt;
        theirLink.mutual = false;
        // Do NOT set declaredByThem / declaredByMe on them — decline/ignore means no benefits.
        me.lastDeclareAt = now;
        me.declarationsSent++;

        RivalStore.DeclareRequest req = new RivalStore.DeclareRequest();
        req.fromUuid = me.uuid;
        req.fromName = me.name;
        req.toUuid = them.uuid;
        req.toName = them.name;
        req.createdAt = now;
        req.expiresAt = myLink.pendingExpireAt;
        store.declareRequests.put(me.uuid + ">" + them.uuid, req);
        store.markDirty();

        DmzRewards.msg(target, "§6[Rival] §e" + me.name + " §7visibly declared you as a rival!");
        DmzRewards.msg(target, "§8Open §e/rival §8→ Actions → §aAccept…  §7or  §cDecline…");
        return "§aDeclared §f" + them.name + " §a(Pending). They were notified." + declaredNote;
    }

    public static String accept(ServerPlayer player, String otherName) {
        RivalStore store = RivalStore.get();
        RivalPlayerRecord me = store.ensurePlayer(player);
        RivalPlayerRecord them = findByName(store, otherName);
        if (them == null) {
            return "§cNo pending declare from that player.";
        }
        RivalLink myLink = me.rivals.get(them.uuid);
        long now = System.currentTimeMillis();

        // Declared (both Silent) → Accept upgrades straight to Mutual (no pending invite needed).
        if (myLink != null && isReciprocatedSilent(myLink) && !myLink.mutual
                && !myLink.inviteReceived && !myLink.inviteSent) {
            RivalLink theirLink = them.getOrCreateLink(me.uuid, me.name, now);
            myLink.declaredByMe = true;
            myLink.declaredByThem = true;
            theirLink.declaredByMe = true;
            theirLink.declaredByThem = true;
            promoteMutual(me, them, myLink, theirLink, now);
            me.declarationsAccepted++;
            store.markDirty();
            ServerPlayer online = onlineByUuid(player.m_20194_(), them.uuid);
            if (online != null) {
                DmzRewards.msg(online, "§a§l[Rival] MUTUAL");
                DmzRewards.msg(online, "§a" + me.name + " accepted — Declared → Mutual!");
                DmzRewards.msg(online, "§8Both ways. Full rivalry benefits.");
            }
            DmzRewards.msg(player, "§a§l[Rival] MUTUAL");
            return "§aAccepted §f" + them.name + " §a— Declared → Mutual!";
        }

        if (myLink == null || !myLink.inviteReceived) {
            return "§cNo pending declare from " + them.name + ".";
        }
        RivalLink theirLink = them.getOrCreateLink(me.uuid, me.name, now);
        myLink.declaredByMe = true;
        myLink.declaredByThem = true;
        theirLink.declaredByMe = true;
        theirLink.declaredByThem = true;
        store.clearInviteFlags(them.uuid, me.uuid);
        store.declareRequests.remove(them.uuid + ">" + me.uuid);
        promoteMutual(me, them, myLink, theirLink, now);
        me.declarationsAccepted++;
        store.markDirty();

        ServerPlayer online = onlineByUuid(player.m_20194_(), them.uuid);
        if (online != null) {
            DmzRewards.msg(online, "§a§l[Rival] MUTUAL");
            DmzRewards.msg(online, "§a" + me.name + " accepted your rivalry — Mutual!");
            DmzRewards.msg(online, "§8Both declared. Benefits work both ways.");
        }
        DmzRewards.msg(player, "§a§l[Rival] MUTUAL");
        return "§aAccepted rivalry with §f" + them.name + " §a— Mutual!";
    }

    public static String decline(ServerPlayer player, String otherName) {
        RivalStore store = RivalStore.get();
        RivalPlayerRecord me = store.ensurePlayer(player);
        RivalPlayerRecord them = findByName(store, otherName);
        if (them == null) {
            return "§cNo pending declare from that player.";
        }
        RivalLink myLink = me.rivals.get(them.uuid);
        if (myLink == null || !myLink.inviteReceived) {
            return "§cNo pending declare from " + them.name + ".";
        }
        store.clearInviteFlags(them.uuid, me.uuid);
        store.declareRequests.remove(them.uuid + ">" + me.uuid);
        // Decliner keeps no benefits; declarer keeps declaredByMe
        myLink.declaredByMe = false;
        me.declarationsDeclined++;
        store.markDirty();
        ServerPlayer online = onlineByUuid(player.m_20194_(), them.uuid);
        if (online != null) {
            DmzRewards.msg(online, "§c[Rival] " + me.name + " declined your declare.");
        }
        return "§eDeclined rivalry from " + them.name + ".";
    }

    public static String remove(ServerPlayer player, String otherName) {
        RivalStore store = RivalStore.get();
        RivalPlayerRecord me = store.ensurePlayer(player);
        RivalPlayerRecord them = findByName(store, otherName);
        if (them == null) {
            return "§cRival not found: " + otherName;
        }
        RivalLink myLink = me.rivals.remove(them.uuid);
        if (myLink == null) {
            return "§cYou have no rivalry with " + them.name + ".";
        }
        archiveRivalLink(me, them.uuid, myLink);
        RivalLink theirLink = them.rivals.get(me.uuid);
        if (theirLink != null) {
            archiveRivalLink(them, me.uuid, theirLink);
            them.rivals.remove(me.uuid);
            them.recalcTotalRp();
        }
        me.rivalsRemoved++;
        me.recalcTotalRp();
        if (them.uuid.equals(me.nemesisUuid)) {
            me.nemesisUuid = "";
        }
        if (me.uuid.equals(them.nemesisUuid)) {
            them.nemesisUuid = "";
        }
        store.markDirty();
        return "§eRemoved rivalry with §f" + them.name + ".";
    }

    public static List<String> listLines(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        RivalPlayerRecord me = RivalStore.get().ensurePlayer(player);
        if (me == null || me.rivals.isEmpty()) {
            lines.add("§7No rivals yet. §8Open /rival → Actions → Declare…");
            return lines;
        }
        lines.add("§6§lYour Rivals");
        List<Map.Entry<String, RivalLink>> entries = new ArrayList<>(me.rivals.entrySet());
        entries.sort(Comparator.comparingDouble((Map.Entry<String, RivalLink> e) ->
                e.getValue() == null ? 0.0 : e.getValue().points).reversed());
        for (Map.Entry<String, RivalLink> e : entries) {
            RivalLink link = e.getValue();
            if (link == null) {
                continue;
            }
            RivalStatus st = link.status();
            RivalConstants.RpTier tier = RivalConstants.tierFor(link.points);
            lines.add("§f" + link.name + " §8[" + st.label() + "] §"
                    + tier.color() + tier.name() + " §7RP §f" + (int) link.points
                    + (link.deathLosses > 0 ? " §cDL " + link.deathLosses : ""));
            lines.addAll(ProvingGrounds.listLines(link));
        }
        return lines;
    }

    /**
     * Encoded rival cards for inventory GUIs (one string per rival).
     * Fields tab-separated: uuid, name, status, tier, rp, wins, losses, draws,
     * deathLosses, deathWins, online(0/1), past(0/1), presenceMs, lastBattleAt.
     */
    public static List<String> currentRivalCards(ServerPlayer player) {
        return rivalCards(player, false);
    }

    /** Encoded past-rival cards (archived on remove). Same format as {@link #currentRivalCards}. */
    public static List<String> pastRivalCards(ServerPlayer player) {
        return rivalCards(player, true);
    }

    /** Chat/history page lines for previous rivals. */
    public static List<String> historyLines(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        RivalPlayerRecord me = RivalStore.get().ensurePlayer(player);
        if (me == null || me.pastRivals == null || me.pastRivals.isEmpty()) {
            lines.add("§7No previous rivals yet.");
            lines.add("§8Removed rivalries appear here.");
            return lines;
        }
        lines.add("§6§lPrevious Rivals");
        List<Map.Entry<String, RivalLink>> entries = new ArrayList<>(me.pastRivals.entrySet());
        entries.sort(Comparator.comparingDouble((Map.Entry<String, RivalLink> e) ->
                e.getValue() == null ? 0.0 : e.getValue().points).reversed());
        for (Map.Entry<String, RivalLink> e : entries) {
            RivalLink link = e.getValue();
            if (link == null) {
                continue;
            }
            RivalConstants.RpTier tier = RivalConstants.tierFor(link.points);
            String name = link.name == null || link.name.isBlank() ? e.getKey() : link.name;
            lines.add("§f" + name + " §8[PAST] §" + tier.color() + tier.name()
                    + " §7RP §f" + (int) link.points
                    + " §8· §a" + link.wins + "§7/§c" + link.losses + "§7/§e" + link.draws);
        }
        return lines;
    }

    private static List<String> rivalCards(ServerPlayer player, boolean past) {
        List<String> out = new ArrayList<>();
        if (player == null) {
            return out;
        }
        RivalPlayerRecord me = RivalStore.get().ensurePlayer(player);
        if (me == null) {
            return out;
        }
        Map<String, RivalLink> map = past ? me.pastRivals : me.rivals;
        if (map == null || map.isEmpty()) {
            return out;
        }
        MinecraftServer server = player.m_20194_();
        List<Map.Entry<String, RivalLink>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Comparator.comparingDouble((Map.Entry<String, RivalLink> e) ->
                e.getValue() == null ? 0.0 : e.getValue().points).reversed());
        for (Map.Entry<String, RivalLink> e : entries) {
            RivalLink link = e.getValue();
            if (link == null) {
                continue;
            }
            String uuid = link.uuid == null || link.uuid.isBlank() ? e.getKey() : link.uuid;
            String name = link.name == null || link.name.isBlank() ? uuid : link.name;
            name = name.replace('\t', ' ').replace('\n', ' ');
            RivalStatus st = past ? RivalStatus.NONE : link.status();
            String status = past ? "PAST" : (st == null ? "?" : st.label());
            RivalConstants.RpTier tier = RivalConstants.tierFor(link.points);
            boolean online = false;
            if (server != null && uuid != null && !uuid.isBlank()) {
                try {
                    online = server.m_6846_().m_11259_(java.util.UUID.fromString(uuid)) != null;
                } catch (IllegalArgumentException ignored) {
                    online = false;
                }
            }
            out.add(String.join("\t",
                    nullToEmpty(uuid),
                    nullToEmpty(name),
                    nullToEmpty(status),
                    tier == null ? "?" : tier.name(),
                    String.valueOf((int) link.points),
                    String.valueOf(link.wins),
                    String.valueOf(link.losses),
                    String.valueOf(link.draws),
                    String.valueOf(link.deathLosses),
                    String.valueOf(link.deathWins),
                    online ? "1" : "0",
                    past ? "1" : "0",
                    String.valueOf(Math.max(0L, link.presenceMs)),
                    String.valueOf(Math.max(0L, link.lastBattleAt))));
        }
        return out;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static List<String> statsLines(ServerPlayer player) {
        return statsLines(player, null);
    }

    public static List<String> statsLines(ServerPlayer viewer, String targetName) {
        RivalStore store = RivalStore.get();
        RivalPlayerRecord me;
        if (targetName == null || targetName.isBlank()) {
            me = store.ensurePlayer(viewer);
        } else {
            me = findByName(store, targetName);
            if (me == null) {
                ServerPlayer online = findOnline(viewer == null ? null : viewer.m_20194_(), targetName);
                if (online != null) {
                    me = store.ensurePlayer(online);
                }
            }
            if (me == null) {
                List<String> miss = new ArrayList<>();
                miss.add("§cNo rivalry record for §f" + targetName);
                return miss;
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add("§6§lRival Stats §8— §f" + me.name);
        RivalConstants.RpTier tier = RivalConstants.tierFor(me.totalRp);
        lines.add("§7Total RP §f" + (int) me.totalRp + " §8(§" + tier.color() + tier.name() + "§8)");
        lines.add("§7Record §a" + me.officialWins + "§7/§c" + me.officialLosses + "§7/§e" + me.officialDraws
                + " §8(KO " + me.knockouts + ")");
        lines.add("§7Streak §f" + me.currentWinStreak + " §8(best " + me.bestWinStreak + ")");
        lines.add("§7Challenges §f" + me.challengesPlayed + " §7Surpass §f" + me.surpassAwards);
        lines.add("§7Career dmg §f" + (int) me.careerDamageDealt
                + " §8| combo §f" + me.careerHighestCombo
                + " §8| hits §f" + me.careerHits);
        lines.add("§7Mutual slots §f" + me.countMutual() + "§8/§f" + RivalConstants.MAX_MUTUAL_RIVALS);
        lines.add("§7TP messages §f" + (me.tpMessages ? "ON" : "OFF") + " §8(/rival tpmsg)");
        return lines;
    }

    public static String setTpMsg(ServerPlayer player, Boolean on) {
        RivalPlayerRecord me = RivalStore.get().ensurePlayer(player);
        if (on == null) {
            return "§7Rival TP messages: §f" + (me.tpMessages ? "ON" : "OFF");
        }
        me.tpMessages = on;
        RivalStore.get().markDirty();
        return "§aRival TP messages §f" + (on ? "ON" : "OFF");
    }

    public static List<String> topLines(int limit) {
        return topLines("rp", limit);
    }

    public static List<String> topLines(String category, int limit) {
        String cat = category == null || category.isBlank() ? "rp" : category.trim().toLowerCase();
        List<String> lines = new ArrayList<>();
        String label = switch (cat) {
            case "wins", "win" -> "Wins";
            case "streak", "beststreak" -> "Streak";
            case "damage", "dmg" -> "Damage";
            case "combo" -> "Combo";
            case "hit", "hits" -> "Hits";
            case "battles", "battle", "challenges" -> "Battles";
            default -> "RP";
        };
        lines.add("§6§lRival " + label + " Top");
        int i = 1;
        for (RivalPlayerRecord rec : RivalStore.get().topBy(limit, cat)) {
            String value = switch (cat) {
                case "wins", "win" -> String.valueOf(rec.officialWins);
                case "streak", "beststreak" -> String.valueOf(rec.bestWinStreak);
                case "damage", "dmg" -> String.valueOf((int) rec.careerDamageDealt);
                case "combo" -> String.valueOf(rec.careerHighestCombo);
                case "hit", "hits" -> String.valueOf(rec.careerHits);
                case "battles", "battle", "challenges" -> String.valueOf(rec.challengesPlayed);
                default -> {
                    RivalConstants.RpTier tier = RivalConstants.tierFor(rec.totalRp);
                    yield (int) rec.totalRp + " §8(§" + tier.color() + tier.name() + "§8)";
                }
            };
            lines.add("§e#" + i + " §f" + rec.name + " §7" + value);
            i++;
        }
        if (i == 1) {
            lines.add("§7No rivalry data yet.");
        }
        return lines;
    }

    private static void archiveRivalLink(RivalPlayerRecord owner, String rivalUuid, RivalLink link) {
        if (owner == null || rivalUuid == null || link == null) {
            return;
        }
        if (owner.pastRivals == null) {
            owner.pastRivals = new ConcurrentHashMap<>();
        }
        RivalLink snap = new RivalLink();
        snap.uuid = link.uuid;
        snap.name = link.name;
        snap.points = link.points;
        snap.wins = link.wins;
        snap.losses = link.losses;
        snap.draws = link.draws;
        snap.deathLosses = link.deathLosses;
        snap.deathWins = link.deathWins;
        snap.presenceMs = link.presenceMs;
        snap.createdAt = link.createdAt;
        snap.firstMetAt = link.firstMetAt;
        snap.mutualSince = link.mutualSince;
        snap.lastBattleAt = link.lastBattleAt;
        snap.provingGrounds = link.provingGrounds;
        owner.pastRivals.put(rivalUuid, snap);
    }

    private static void restorePastIfAny(RivalPlayerRecord owner, RivalPlayerRecord target, RivalLink link) {
        if (owner == null || target == null || link == null || owner.pastRivals == null) {
            return;
        }
        RivalLink past = owner.pastRivals.remove(target.uuid);
        if (past == null) {
            return;
        }
        link.points = Math.max(link.points, past.points);
        link.wins = Math.max(link.wins, past.wins);
        link.losses = Math.max(link.losses, past.losses);
        link.draws = Math.max(link.draws, past.draws);
        link.deathLosses = Math.max(link.deathLosses, past.deathLosses);
        link.deathWins = Math.max(link.deathWins, past.deathWins);
        link.presenceMs = Math.max(link.presenceMs, past.presenceMs);
        if (past.createdAt > 0 && (link.createdAt <= 0 || past.createdAt < link.createdAt)) {
            link.createdAt = past.createdAt;
        }
        if (past.firstMetAt > 0 && (link.firstMetAt <= 0 || past.firstMetAt < link.firstMetAt)) {
            link.firstMetAt = past.firstMetAt;
        }
        if (past.mutualSince > 0) {
            link.mutualSince = past.mutualSince;
        }
        if (past.lastBattleAt > link.lastBattleAt) {
            link.lastBattleAt = past.lastBattleAt;
        }
        if (link.provingGrounds == null && past.provingGrounds != null) {
            link.provingGrounds = past.provingGrounds;
        }
    }

    /** Both sides silently rivaled each other (Declared, not Mutual, no invites). */
    private static boolean isReciprocatedSilent(RivalLink link) {
        return link != null
                && link.declaredByMe
                && link.declaredByThem
                && !link.mutual
                && !link.inviteSent
                && !link.inviteReceived;
    }

    /**
     * Crossed silent rivals → Declared for both, notify both online players.
     * Mutual still requires a visible Declare → Accept.
     */
    private static void promoteDeclared(
            ServerPlayer actor,
            ServerPlayer target,
            RivalPlayerRecord me,
            RivalPlayerRecord them,
            RivalLink myLink,
            RivalLink theirLink,
            long now
    ) {
        myLink.declaredByMe = true;
        myLink.declaredByThem = true;
        myLink.inviteSent = false;
        myLink.inviteReceived = false;
        myLink.mutual = false;
        myLink.touch(now);

        theirLink.declaredByMe = true;
        theirLink.declaredByThem = true;
        theirLink.inviteSent = false;
        theirLink.inviteReceived = false;
        theirLink.mutual = false;
        theirLink.touch(now);

        me.declarationsSent++;

        DmzRewards.msg(actor, "§6§l[Rival] DECLARED");
        DmzRewards.msg(actor, "§e" + them.name);
        DmzRewards.msg(actor, "§8You both silently rivaled each other.");
        DmzRewards.msg(actor, "§8For Mutual: /rival → Actions → Accept… §7" + them.name);

        DmzRewards.msg(target, "§6§l[Rival] DECLARED");
        DmzRewards.msg(target, "§e" + me.name);
        DmzRewards.msg(target, "§8You both silently rivaled each other.");
        DmzRewards.msg(target, "§8For Mutual: /rival → Actions → Accept… §7" + me.name);
    }

    private static void promoteMutual(
            RivalPlayerRecord me,
            RivalPlayerRecord them,
            RivalLink myLink,
            RivalLink theirLink,
            long now
    ) {
        ensureMutualRoom(me, them.uuid);
        ensureMutualRoom(them, me.uuid);
        myLink.mutual = true;
        theirLink.mutual = true;
        myLink.declaredByMe = true;
        myLink.declaredByThem = true;
        theirLink.declaredByMe = true;
        theirLink.declaredByThem = true;
        myLink.inviteSent = false;
        myLink.inviteReceived = false;
        theirLink.inviteSent = false;
        theirLink.inviteReceived = false;
        if (myLink.mutualSince <= 0L) {
            myLink.mutualSince = now;
        }
        if (theirLink.mutualSince <= 0L) {
            theirLink.mutualSince = now;
        }
    }

    private static void ensureMutualRoom(RivalPlayerRecord record, String excludeUuid) {
        while (record.countMutual() >= RivalConstants.MAX_MUTUAL_RIVALS) {
            String oldest = null;
            long oldestAt = Long.MAX_VALUE;
            for (Map.Entry<String, RivalLink> e : record.rivals.entrySet()) {
                if (e.getKey().equals(excludeUuid)) {
                    continue;
                }
                RivalLink link = e.getValue();
                if (link == null || !link.mutual) {
                    continue;
                }
                long at = link.mutualSince > 0L ? link.mutualSince : link.createdAt;
                if (at < oldestAt) {
                    oldestAt = at;
                    oldest = e.getKey();
                }
            }
            if (oldest == null) {
                break;
            }
            RivalLink old = record.rivals.get(oldest);
            if (old != null) {
                old.mutual = false;
                old.isNemesis = false;
            }
            RivalPlayerRecord other = RivalStore.get().get(oldest);
            if (other != null) {
                RivalLink back = other.rivals.get(record.uuid);
                if (back != null) {
                    back.mutual = false;
                    back.isNemesis = false;
                }
            }
        }
    }

    private static RivalPlayerRecord findByName(RivalStore store, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String want = name.trim().toLowerCase();
        for (RivalPlayerRecord rec : store.players.values()) {
            if (rec.name != null && rec.name.toLowerCase().equals(want)) {
                return rec;
            }
        }
        return null;
    }

    private static ServerPlayer onlineByUuid(MinecraftServer server, String uuid) {
        if (server == null || uuid == null) {
            return null;
        }
        try {
            return server.m_6846_().m_11259_(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static ServerPlayer findOnline(MinecraftServer server, String name) {
        if (server == null || name == null || name.isBlank()) {
            return null;
        }
        ServerPlayer exact = server.m_6846_().m_11255_(name);
        if (exact != null) {
            return exact;
        }
        String want = name.trim().toLowerCase();
        for (ServerPlayer p : server.m_6846_().m_11314_()) {
            if (p.m_7755_().getString().toLowerCase().equals(want)) {
                return p;
            }
        }
        return null;
    }
}
