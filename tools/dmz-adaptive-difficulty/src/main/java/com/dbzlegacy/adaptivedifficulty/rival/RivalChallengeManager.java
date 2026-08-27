package com.dbzlegacy.adaptivedifficulty.rival;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Official rival challenges: requests, countdown, HP-lost scoring, win/lose/draw payouts.
 */
public final class RivalChallengeManager {
    private static final RivalChallengeManager INSTANCE = new RivalChallengeManager();
    private static final AtomicLong NEXT_ID = new AtomicLong(1L);

    private final Map<String, ChallengeRequest> requests = new ConcurrentHashMap<>();
    private final Map<String, RivalChallenge> activeById = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, PendingHp> pendingHp = new ConcurrentHashMap<>();
    private long lastTickAt;

    public static RivalChallengeManager get() {
        return INSTANCE;
    }

    private RivalChallengeManager() {}

    public boolean isInChallenge(UUID uuid) {
        return uuid != null && activeByPlayer.containsKey(uuid);
    }

    public RivalChallenge getChallenge(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        String id = activeByPlayer.get(uuid);
        return id == null ? null : activeById.get(id);
    }

    public RivalChallenge byId(String id) {
        return id == null ? null : activeById.get(id);
    }

    public RivalChallenge anyActive() {
        for (RivalChallenge ch : activeById.values()) {
            if (ch != null && ch.status != RivalChallenge.Phase.ENDED
                    && (ch.status == RivalChallenge.Phase.ACTIVE || ch.status == RivalChallenge.Phase.COUNTDOWN)) {
                return ch;
            }
        }
        return null;
    }

    public ChallengeRequest getRequestInvolving(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        for (ChallengeRequest req : requests.values()) {
            if (req != null && (uuid.equals(req.from) || uuid.equals(req.to))) {
                return req;
            }
        }
        return null;
    }

    public String sendChallenge(ServerPlayer from, ServerPlayer to, int minutes) {
        if (from == null || to == null) {
            return "§cInvalid players.";
        }
        if (from.m_20148_().equals(to.m_20148_())) {
            return "§cYou cannot challenge yourself.";
        }
        if (!DifficultyConfig.get().rivalChallenges) {
            return "§cRival challenges are disabled.";
        }
        long now = System.currentTimeMillis();
        RivalPlayerRecord fromRec = RivalStore.get().ensurePlayer(from);
        RivalPlayerRecord toRec = RivalStore.get().ensurePlayer(to);
        if (fromRec == null || toRec == null) {
            return "§cCould not load rivalry data.";
        }
        if (now - fromRec.lastChallengeEndAt < RivalConstants.CH_BETWEEN_COOLDOWN_MS) {
            long left = RivalConstants.CH_BETWEEN_COOLDOWN_MS - (now - fromRec.lastChallengeEndAt);
            return "§cYou are on challenge cooldown (" + formatMs(left) + ").";
        }
        if (now - toRec.lastChallengeEndAt < RivalConstants.CH_BETWEEN_COOLDOWN_MS) {
            return "§c" + to.m_7755_().getString() + " is on challenge cooldown.";
        }
        if (distance(from, to) > RivalConstants.CH_MAX_DISTANCE) {
            return "§cGet within " + (int) RivalConstants.CH_MAX_DISTANCE + " blocks to challenge.";
        }
        if (isInChallenge(from.m_20148_()) || getRequestInvolving(from.m_20148_()) != null) {
            return "§cYou already have a pending or active challenge.";
        }
        if (isInChallenge(to.m_20148_()) || getRequestInvolving(to.m_20148_()) != null) {
            return "§cThat player is already in a challenge.";
        }
        RivalLink link = fromRec.rivals.get(to.m_20148_().toString());
        RivalStatus status = link == null ? RivalStatus.NONE : link.status();
        boolean related = status == RivalStatus.DECLARED
                || status == RivalStatus.MUTUAL
                || status == RivalStatus.NEMESIS
                || status == RivalStatus.PENDING
                || status == RivalStatus.UNKNOWN;
        if (!related && !true /* CH_ALLOW_NON_RIVAL */) {
            return "§cYou can only challenge declared rivals.";
        }
        int mins = Math.max(RivalConstants.CH_MIN_MINUTES, Math.min(RivalConstants.CH_MAX_MINUTES, minutes));
        ChallengeRequest req = new ChallengeRequest();
        req.id = "req-" + NEXT_ID.getAndIncrement();
        req.from = from.m_20148_();
        req.to = to.m_20148_();
        req.fromName = from.m_7755_().getString();
        req.toName = to.m_7755_().getString();
        req.createdAt = now;
        req.expiresAt = now + RivalConstants.CH_REQUEST_EXPIRE_MS;
        req.durationMs = mins * 60_000L;
        requests.put(req.id, req);
        DmzRewards.msg(to, "§6[Rival Challenge] §e" + req.fromName
                + " §7challenged you for §f" + mins + "§7 min!");
        DmzRewards.msg(to, "§8  /rival challenge accept  §7or  §8/rival challenge decline");
        return "§aChallenge sent to §f" + req.toName + "§a (" + mins + " min).";
    }

    public String acceptChallenge(ServerPlayer player) {
        ChallengeRequest req = findIncoming(player.m_20148_());
        if (req == null) {
            return "§cNo pending challenge to accept.";
        }
        MinecraftServer server = player.m_20194_();
        ServerPlayer challenger = server == null ? null : server.m_6846_().m_11259_(req.from);
        if (challenger == null) {
            requests.remove(req.id);
            return "§cThe challenger went offline.";
        }
        if (distance(player, challenger) > RivalConstants.CH_MAX_DISTANCE) {
            return "§cGet within " + (int) RivalConstants.CH_MAX_DISTANCE + " blocks to accept.";
        }
        long now = System.currentTimeMillis();
        RivalPlayerRecord aRec = RivalStore.get().ensurePlayer(challenger);
        RivalPlayerRecord bRec = RivalStore.get().ensurePlayer(player);
        if (aRec != null && now - aRec.lastChallengeEndAt < RivalConstants.CH_BETWEEN_COOLDOWN_MS) {
            return "§cChallenger is on cooldown.";
        }
        if (bRec != null && now - bRec.lastChallengeEndAt < RivalConstants.CH_BETWEEN_COOLDOWN_MS) {
            return "§cYou are on challenge cooldown.";
        }
        requests.remove(req.id);
        RivalChallenge ch = new RivalChallenge();
        ch.id = "ch-" + NEXT_ID.getAndIncrement();
        ch.a = req.from;
        ch.b = req.to;
        ch.nameA = req.fromName;
        ch.nameB = req.toName;
        ch.durationMs = Math.max(RivalConstants.CH_DURATION_MS, req.durationMs);
        ch.status = RivalChallenge.Phase.COUNTDOWN;
        ch.countdownUntil = now + RivalConstants.CH_COUNTDOWN_MS;
        ch.startAt = ch.countdownUntil;
        ch.endsAt = ch.startAt + ch.durationMs;
        activeById.put(ch.id, ch);
        activeByPlayer.put(ch.a, ch.id);
        activeByPlayer.put(ch.b, ch.id);
        broadcast(server, "§e" + ch.nameA + " §7vs §e" + ch.nameB
                + " §8— challenge starts in 5s!");
        DmzRewards.msg(challenger, "§aChallenge accepted! Countdown started.");
        return "§aChallenge accepted! Countdown started.";
    }

    public String declineChallenge(ServerPlayer player) {
        ChallengeRequest req = findIncoming(player.m_20148_());
        if (req == null) {
            return "§cNo pending challenge to decline.";
        }
        requests.remove(req.id);
        MinecraftServer server = player.m_20194_();
        ServerPlayer from = server == null ? null : server.m_6846_().m_11259_(req.from);
        if (from != null) {
            DmzRewards.msg(from, "§c" + player.m_7755_().getString() + " declined your challenge.");
        }
        return "§eDeclined challenge from " + req.fromName + ".";
    }

    public String cancelChallenge(ServerPlayer player) {
        ChallengeRequest outgoing = findOutgoing(player.m_20148_());
        if (outgoing != null) {
            requests.remove(outgoing.id);
            return "§eChallenge request cancelled.";
        }
        RivalChallenge ch = getChallenge(player.m_20148_());
        if (ch == null || ch.status == RivalChallenge.Phase.ENDED) {
            return "§cYou have no active challenge.";
        }
        UUID other = ch.other(player.m_20148_());
        endChallenge(player.m_20194_(), ch, other, player.m_20148_(), false, "forfeit");
        return "§eYou forfeited the challenge.";
    }

    public void queueHurt(ServerPlayer victim, ServerPlayer attacker, boolean ki) {
        if (victim == null || attacker == null) {
            return;
        }
        RivalChallenge ch = getChallenge(victim.m_20148_());
        if (ch == null || ch.status != RivalChallenge.Phase.ACTIVE) {
            return;
        }
        if (!ch.involves(attacker.m_20148_())) {
            return;
        }
        PendingHp pending = new PendingHp();
        pending.attacker = attacker.m_20148_();
        pending.sampleHp = victim.m_21223_();
        pending.ki = ki;
        pending.resolveAt = System.currentTimeMillis() + RivalConstants.CH_PENDING_RESOLVE_MS;
        pendingHp.put(victim.m_20148_(), pending);
    }

    public void onDeath(ServerPlayer victim, ServerPlayer killer) {
        if (victim == null) {
            return;
        }
        RivalChallenge ch = getChallenge(victim.m_20148_());
        if (ch == null || ch.status != RivalChallenge.Phase.ACTIVE) {
            return;
        }
        UUID winner = killer != null && ch.involves(killer.m_20148_())
                ? killer.m_20148_()
                : ch.other(victim.m_20148_());
        endChallenge(victim.m_20194_(), ch, winner, victim.m_20148_(), true, "knockout");
    }

    public void pulse(MinecraftServer server, long now) {
        if (server == null) {
            return;
        }
        expireRequests(now);
        resolvePendingHp(server, now);
        if (now - lastTickAt < RivalConstants.CH_TICK_MS && !activeById.isEmpty()) {
            // still allow end checks at least every tick window
        }
        if (now - lastTickAt < RivalConstants.CH_TICK_MS) {
            return;
        }
        lastTickAt = now;

        for (RivalChallenge ch : activeById.values()) {
            if (ch == null || ch.status == RivalChallenge.Phase.ENDED) {
                continue;
            }
            ServerPlayer a = server.m_6846_().m_11259_(ch.a);
            ServerPlayer b = server.m_6846_().m_11259_(ch.b);
            if (a == null || b == null) {
                UUID winner = a != null ? ch.a : (b != null ? ch.b : null);
                UUID loser = a == null ? ch.a : ch.b;
                endChallenge(server, ch, winner, loser, false, "disconnect");
                continue;
            }
            if (ch.status == RivalChallenge.Phase.COUNTDOWN) {
                if (now >= ch.countdownUntil) {
                    ch.status = RivalChallenge.Phase.ACTIVE;
                    ch.startAt = now;
                    ch.endsAt = now + ch.durationMs;
                    broadcast(server, "§6FIGHT! §e" + ch.nameA + " §7vs §e" + ch.nameB
                            + " §8(" + (ch.durationMs / 1000L) + "s)");
                }
                continue;
            }
            if (ch.status == RivalChallenge.Phase.ACTIVE) {
                if (distance(a, b) > RivalConstants.CH_MAX_DISTANCE * 2.0) {
                    // whoever is farther from start loses — treat as draw-ish forfeit for distant
                    endChallenge(server, ch, null, null, false, "distance");
                    continue;
                }
                if (now >= ch.endsAt) {
                    finishByDamage(server, ch);
                    continue;
                }
                if (now - ch.lastBroadcastAt >= RivalConstants.CH_BROADCAST_SCORE_MS) {
                    ch.lastBroadcastAt = now;
                    DmzRewards.msg(a, scoreLine(ch));
                    DmzRewards.msg(b, scoreLine(ch));
                }
            }
        }
        // purge ended
        Iterator<Map.Entry<String, RivalChallenge>> it = activeById.entrySet().iterator();
        while (it.hasNext()) {
            RivalChallenge ch = it.next().getValue();
            if (ch != null && ch.status == RivalChallenge.Phase.ENDED) {
                it.remove();
            }
        }
    }

    public void cleanupPlayer(UUID uuid) {
        if (uuid == null) {
            return;
        }
        pendingHp.remove(uuid);
        ChallengeRequest req = getRequestInvolving(uuid);
        if (req != null) {
            requests.remove(req.id);
        }
        RivalChallenge ch = getChallenge(uuid);
        if (ch != null && ch.status != RivalChallenge.Phase.ENDED) {
            endChallenge(null, ch, ch.other(uuid), uuid, false, "logout");
        }
    }

    private void finishByDamage(MinecraftServer server, RivalChallenge ch) {
        double da = ch.damageA;
        double db = ch.damageB;
        if (Math.abs(da - db) < 1.0) {
            endChallenge(server, ch, null, null, false, "draw");
        } else if (da > db) {
            endChallenge(server, ch, ch.a, ch.b, false, "damage");
        } else {
            endChallenge(server, ch, ch.b, ch.a, false, "damage");
        }
    }

    private void endChallenge(
            MinecraftServer server,
            RivalChallenge ch,
            UUID winner,
            UUID loser,
            boolean knockout,
            String reason
    ) {
        if (ch == null || ch.status == RivalChallenge.Phase.ENDED) {
            return;
        }
        ch.status = RivalChallenge.Phase.ENDED;
        ch.knockout = knockout;
        ch.endReason = reason == null ? "" : reason;
        ch.winner = winner;
        ch.loser = loser;
        activeByPlayer.remove(ch.a);
        activeByPlayer.remove(ch.b);
        long now = System.currentTimeMillis();

        RivalStore store = RivalStore.get();
        RivalPlayerRecord recA = store.get(ch.a == null ? null : ch.a.toString());
        RivalPlayerRecord recB = store.get(ch.b == null ? null : ch.b.toString());
        if (recA != null) {
            recA.lastChallengeEndAt = now;
            recA.challengesPlayed++;
        }
        if (recB != null) {
            recB.lastChallengeEndAt = now;
            recB.challengesPlayed++;
        }

        ServerPlayer pA = server == null || ch.a == null ? null : server.m_6846_().m_11259_(ch.a);
        ServerPlayer pB = server == null || ch.b == null ? null : server.m_6846_().m_11259_(ch.b);

        boolean related = areRelated(recA, recB, ch);
        RivalChallenge.Combat cA = ch.combatOf(ch.a);
        RivalChallenge.Combat cB = ch.combatOf(ch.b);
        long duration = Math.max(0L, now - ch.startAt);

        if ("draw".equals(reason) || "distance".equals(reason) || winner == null) {
            if (pA != null) {
                float tp = RivalTpCurve.scale(pA, RivalConstants.CH_DRAW_TP, "burst");
                DmzRewards.awardTp(pA, tp, "Rival Draw", true, "§6[Rival Challenge] ");
            }
            if (pB != null) {
                float tp = RivalTpCurve.scale(pB, RivalConstants.CH_DRAW_TP, "burst");
                DmzRewards.awardTp(pB, tp, "Rival Draw", true, "§6[Rival Challenge] ");
            }
            if (related && recA != null && recB != null) {
                awardDrawRp(recA, recB, ch);
                ProvingGrounds.touchDraw(pA, pB, recA, recB,
                        cA.damage, cB.damage, cA.biggestHit, cB.biggestHit, duration);
            }
            deliverReport(server, ch, null, null, true);
            RivalProgression.get().onChallengeEnd(pA, pB, ch, true, false);
            SystemTelemetry.log("rival", "challenge_end", pA, pB,
                    SystemTelemetry.fields("result", "draw", "reason", reason));
            broadcast(server, "§eDRAW §7— §f" + ch.nameA + " §7vs §f" + ch.nameB
                    + " §8(" + reason + ")");
        } else if ("forfeit".equals(reason) || "logout".equals(reason) || "disconnect".equals(reason)) {
            ServerPlayer winP = winner.equals(ch.a) ? pA : pB;
            ServerPlayer loseP = loser != null && loser.equals(ch.a) ? pA : pB;
            float baseWin = related ? RivalConstants.CH_WIN_TP : RivalConstants.CH_NON_RIVAL_WIN_TP;
            float winTp = RivalTpCurve.scale(winP, baseWin, "burst");
            float loseTp = RivalTpCurve.scale(loseP, RivalConstants.CH_LOSE_TP, "burst");
            if (winP != null) {
                DmzRewards.awardTp(winP, winTp, "Rival Forfeit Win", true, "§6[Rival Challenge] ");
            }
            if (loseP != null) {
                DmzRewards.awardTp(loseP, loseTp, "Rival Forfeit Loss", true, "§6[Rival Challenge] ");
            }
            if (related) {
                applyWinLoss(recA, recB, ch, winner, loser, false, true);
            }
            deliverReport(server, ch, winner, loser, false);
            RivalProgression.get().onChallengeEnd(winP, loseP, ch, false, false);
            SystemTelemetry.log("rival", "challenge_end", winP, loseP,
                    SystemTelemetry.fields("result", "forfeit", "reason", reason));
            broadcast(server, "§a" + nameOf(ch, winner) + " §7wins by forfeit vs §c" + nameOf(ch, loser));
        } else {
            float baseWin = (related ? RivalConstants.CH_WIN_TP : RivalConstants.CH_NON_RIVAL_WIN_TP)
                    + (knockout ? RivalConstants.CH_KO_WIN_TP_BONUS : 0);
            ServerPlayer winP = winner.equals(ch.a) ? pA : pB;
            ServerPlayer loseP = loser != null && loser.equals(ch.a) ? pA : pB;
            RivalLink winLink = null;
            if (related && recA != null && recB != null) {
                RivalPlayerRecord winRec = winner.toString().equals(recA.uuid) ? recA : recB;
                RivalPlayerRecord loseRec = loser.toString().equals(recA.uuid) ? recA : recB;
                winLink = winRec.rivals.get(loseRec.uuid);
            }
            float pgMult = ProvingGrounds.challengeTpMultiplier(winP, winLink);
            float winTp = RivalTpCurve.scale(winP, baseWin * pgMult, "burst");
            float loseTp = RivalTpCurve.scale(loseP, RivalConstants.CH_LOSE_TP, "burst");
            if (winP != null) {
                DmzRewards.awardTp(winP, winTp, knockout ? "Rival KO Win" : "Rival Win", true,
                        "§6[Rival Challenge] ");
            }
            if (loseP != null) {
                DmzRewards.awardTp(loseP, loseTp, "Rival Loss", true, "§6[Rival Challenge] ");
            }
            if (related && recA != null && recB != null) {
                applyWinLoss(recA, recB, ch, winner, loser, knockout, false);
                RivalPlayerRecord winRec = winner.toString().equals(recA.uuid) ? recA : recB;
                RivalPlayerRecord loseRec = loser.toString().equals(recA.uuid) ? recA : recB;
                RivalChallenge.Combat wC = ch.combatOf(winner);
                RivalChallenge.Combat lC = ch.combatOf(loser);
                ProvingGrounds.processBattle(winP, loseP, winRec, loseRec,
                        wC.damage, lC.damage, wC.biggestHit, lC.biggestHit, duration);
            }
            deliverReport(server, ch, winner, loser, false);
            RivalProgression.get().onChallengeEnd(winP, loseP, ch, false, knockout);
            SystemTelemetry.log("rival", "challenge_end", winP, loseP,
                    SystemTelemetry.fields("result", knockout ? "ko" : "win", "reason", reason,
                            "dmgA", (int) ch.damageA, "dmgB", (int) ch.damageB));
            String tag = knockout ? "KO" : "WIN";
            broadcast(server, "§a" + nameOf(ch, winner) + " §7" + tag + " vs §c" + nameOf(ch, loser)
                    + " §8(" + (int) ch.damageOf(winner) + " / " + (int) ch.damageOf(loser) + ")");
        }
        store.markDirty();
    }

    private void deliverReport(
            MinecraftServer server,
            RivalChallenge ch,
            UUID winner,
            UUID loser,
            boolean draw
    ) {
        java.util.List<String> report = buildReport(ch, winner, loser, draw);
        ServerPlayer pA = server == null || ch.a == null ? null : server.m_6846_().m_11259_(ch.a);
        ServerPlayer pB = server == null || ch.b == null ? null : server.m_6846_().m_11259_(ch.b);
        for (String line : report) {
            if (pA != null) {
                DmzRewards.msg(pA, line);
            }
            if (pB != null) {
                DmzRewards.msg(pB, line);
            }
        }
        if (!draw) {
            if (pA != null) {
                DmzRewards.msg(pA, pA.m_20148_().equals(winner) ? "§a[Rival] Victory!" : "§c[Rival] Defeat!");
            }
            if (pB != null) {
                DmzRewards.msg(pB, pB.m_20148_().equals(winner) ? "§a[Rival] Victory!" : "§c[Rival] Defeat!");
            }
        }
    }

    private static java.util.List<String> buildReport(
            RivalChallenge ch,
            UUID winner,
            UUID loser,
            boolean draw
    ) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("§8--------------------------------");
        lines.add("§6§l RIVAL BATTLE REPORT");
        lines.add("§8--------------------------------");
        if (draw) {
            lines.add("§8Result  §eDraw");
        } else {
            lines.add("§8Winner  §a" + nameOf(ch, winner));
            lines.add("§8Runner  §c" + nameOf(ch, loser));
        }
        long elapsed = Math.max(0L, System.currentTimeMillis() - ch.startAt);
        lines.add("§8Time    §f" + formatMs(elapsed) + " §8  via  §7" + ch.endReason);
        for (UUID id : new UUID[]{ch.a, ch.b}) {
            if (id == null) {
                continue;
            }
            RivalChallenge.Combat combat = ch.combatOf(id);
            String name = nameOf(ch, id);
            lines.add(" ");
            lines.add("§e" + name);
            lines.add("§8  Damage  §f" + (int) combat.damage
                    + " §8(Phy " + (int) combat.physical + " / Ki " + (int) combat.ki + ")");
            lines.add("§8  Hits  §f" + combat.hits
                    + " §8  Best  §f" + (int) combat.biggestHit
                    + " §8  Combo  §f" + combat.longestCombo);
        }
        lines.add("§8--------------------------------");
        return lines;
    }

    private void applyWinLoss(
            RivalPlayerRecord recA,
            RivalPlayerRecord recB,
            RivalChallenge ch,
            UUID winner,
            UUID loser,
            boolean knockout,
            boolean forfeit
    ) {
        if (recA == null || recB == null || winner == null || loser == null) {
            return;
        }
        RivalPlayerRecord winRec = winner.toString().equals(recA.uuid) ? recA : recB;
        RivalPlayerRecord loseRec = loser.toString().equals(recA.uuid) ? recA : recB;
        winRec.officialWins++;
        loseRec.officialLosses++;
        if (knockout) {
            winRec.knockouts++;
        }
        RivalLink wLink = winRec.getOrCreateLink(loseRec.uuid, loseRec.name, System.currentTimeMillis());
        RivalLink lLink = loseRec.getOrCreateLink(winRec.uuid, winRec.name, System.currentTimeMillis());
        wLink.wins++;
        lLink.losses++;
        wLink.lastBattleAt = System.currentTimeMillis();
        lLink.lastBattleAt = wLink.lastBattleAt;
        int winRp = RivalConstants.CH_WIN_RP;
        int loseRp = RivalConstants.CH_LOSE_RP + (knockout ? RivalConstants.CH_KO_LOSE_RP_BONUS : 0);
        if (forfeit) {
            loseRp = Math.max(0, loseRp - RivalConstants.CH_FORFEIT_RP_PENALTY);
        }
        if (wLink.mutual || lLink.mutual) {
            double minD = Math.min(ch.damageOf(winner), ch.damageOf(loser));
            double maxD = Math.max(1.0, Math.max(ch.damageOf(winner), ch.damageOf(loser)));
            if (minD / maxD >= RivalConstants.CH_CLOSE_BATTLE_RATIO) {
                winRp += RivalConstants.CH_CLOSE_BATTLE_RP;
                loseRp += RivalConstants.CH_CLOSE_BATTLE_RP;
            }
            winRp += longRivalryBonus(wLink);
            loseRp += longRivalryBonus(lLink);
        }
        RivalStore.get().addRp(winRec, loseRec.uuid, winRp, "challenge_win");
        RivalStore.get().addRp(loseRec, winRec.uuid, loseRp, "challenge_lose");
    }

    private void awardDrawRp(RivalPlayerRecord a, RivalPlayerRecord b, RivalChallenge ch) {
        RivalLink aLink = a.rivals.get(b.uuid);
        RivalLink bLink = b.rivals.get(a.uuid);
        if (aLink != null) {
            aLink.draws++;
            aLink.lastBattleAt = System.currentTimeMillis();
        }
        if (bLink != null) {
            bLink.draws++;
            bLink.lastBattleAt = System.currentTimeMillis();
        }
        a.officialDraws++;
        b.officialDraws++;
        int rp = RivalConstants.CH_DRAW_RP;
        if (aLink != null && aLink.mutual) {
            double minD = Math.min(ch.damageA, ch.damageB);
            double maxD = Math.max(1.0, Math.max(ch.damageA, ch.damageB));
            if (minD / maxD >= RivalConstants.CH_CLOSE_BATTLE_RATIO) {
                rp += RivalConstants.CH_CLOSE_BATTLE_RP;
            }
            rp += longRivalryBonus(aLink);
            RivalStore.get().addRp(a, b.uuid, rp, "challenge_draw");
            RivalStore.get().addRp(b, a.uuid, rp, "challenge_draw");
        }
    }

    private static int longRivalryBonus(RivalLink link) {
        if (link == null || !link.mutual) {
            return 0;
        }
        long since = link.mutualSince > 0L ? link.mutualSince : link.firstMetAt;
        long days = Math.max(0L, (System.currentTimeMillis() - since) / 86_400_000L);
        days = Math.min(RivalConstants.CH_LONG_RIVALRY_DAY_CAP, days);
        return (int) (days * RivalConstants.CH_LONG_RIVALRY_DAY_RP);
    }

    private static boolean areRelated(RivalPlayerRecord a, RivalPlayerRecord b, RivalChallenge ch) {
        if (a == null || b == null) {
            return false;
        }
        RivalLink link = a.rivals.get(b.uuid);
        if (link == null) {
            return false;
        }
        RivalStatus st = link.status();
        return st == RivalStatus.UNKNOWN || st == RivalStatus.DECLARED
                || st == RivalStatus.PENDING || st == RivalStatus.MUTUAL || st == RivalStatus.NEMESIS;
    }

    private void resolvePendingHp(MinecraftServer server, long now) {
        Iterator<Map.Entry<UUID, PendingHp>> it = pendingHp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PendingHp> e = it.next();
            PendingHp p = e.getValue();
            if (p == null || now < p.resolveAt) {
                continue;
            }
            it.remove();
            ServerPlayer victim = server.m_6846_().m_11259_(e.getKey());
            if (victim == null) {
                continue;
            }
            RivalChallenge ch = getChallenge(e.getKey());
            if (ch == null || ch.status != RivalChallenge.Phase.ACTIVE) {
                continue;
            }
            float lost = Math.max(0.0f, p.sampleHp - victim.m_21223_());
            if (lost > 0.0f) {
                ch.addDamage(p.attacker, lost, p.ki);
            }
        }
    }

    private void expireRequests(long now) {
        requests.entrySet().removeIf(e -> e.getValue() == null || now > e.getValue().expiresAt);
    }

    private ChallengeRequest findIncoming(UUID to) {
        for (ChallengeRequest req : requests.values()) {
            if (req != null && to.equals(req.to)) {
                return req;
            }
        }
        return null;
    }

    private ChallengeRequest findOutgoing(UUID from) {
        for (ChallengeRequest req : requests.values()) {
            if (req != null && from.equals(req.from)) {
                return req;
            }
        }
        return null;
    }

    private static String scoreLine(RivalChallenge ch) {
        return "§8[Challenge] §f" + ch.nameA + " §e" + (int) ch.damageA
                + " §7- §e" + (int) ch.damageB + " §f" + ch.nameB;
    }

    private static String nameOf(RivalChallenge ch, UUID id) {
        if (id == null) {
            return "?";
        }
        if (id.equals(ch.a)) {
            return ch.nameA;
        }
        if (id.equals(ch.b)) {
            return ch.nameB;
        }
        return id.toString();
    }

    private static void broadcast(MinecraftServer server, String text) {
        if (server == null || text == null) {
            return;
        }
        for (ServerPlayer p : server.m_6846_().m_11314_()) {
            DmzRewards.msg(p, "§6[Rival] " + text);
        }
    }

    private static double distance(LivingEntity a, LivingEntity b) {
        if (a == null || b == null || a.m_9236_() != b.m_9236_()) {
            return Double.MAX_VALUE;
        }
        return a.m_20270_(b);
    }

    private static String formatMs(long ms) {
        long s = Math.max(0L, ms / 1000L);
        return s + "s";
    }

    public static final class ChallengeRequest {
        public String id = "";
        public UUID from;
        public UUID to;
        public String fromName = "";
        public String toName = "";
        public long createdAt;
        public long expiresAt;
        public long durationMs = RivalConstants.CH_DURATION_MS;
    }

    private static final class PendingHp {
        UUID attacker;
        float sampleHp;
        boolean ki;
        long resolveAt;
    }
}
