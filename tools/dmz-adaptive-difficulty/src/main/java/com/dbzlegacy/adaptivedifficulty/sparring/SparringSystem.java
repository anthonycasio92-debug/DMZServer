package com.dbzlegacy.adaptivedifficulty.sparring;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.rival.RivalChallengeManager;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Status;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/** Sparring Tp System 3.2.11 facade. */
public final class SparringSystem {
    private static final Map<UUID, SparPlayerRuntime> RUNTIME = new ConcurrentHashMap<>();

    public static final double MAX_SPAR_DISTANCE = 30.0;
    public static final long MELEE_HIT_ACTIVITY_WINDOW_MS = 4500L;
    public static final long KI_HIT_ACTIVITY_WINDOW_MS = 10_000L;
    public static final long SESSION_START_WINDOW_MS = 15_000L;
    public static final long PAIR_RESTART_COOLDOWN_MS = 3000L;
    public static final long SESSION_GRACE_PERIOD_MS = 4000L;
    public static final long DISTANCE_GRACE_PERIOD_MS = 4000L;
    public static final long MOVEMENT_ACTIVITY_WINDOW_MS = 10_000L;
    public static final double MIN_MOVEMENT_DISTANCE = 0.35;
    public static final double MIN_MOTION_SPEED = 0.08;
    public static final double HEAVY_MOTION_SPEED = 0.55;
    public static final long COMBO_TIMEOUT_MS = 2500L;
    public static final long PENDING_HP_RESOLVE_MS = 75L;
    public static final long MENTOR_CHANGE_COOLDOWN_MS = 7L * 24L * 60L * 60L * 1000L;
    /** Mentor invite TTL — long enough for a Pending board (was 2 minutes). */
    public static final long MENTOR_INVITE_MS = 24L * 60L * 60L * 1000L;
    public static final long TICK_MS = 250L;
    public static final long MIN_COUNTED_SESSION_MS = 30_000L;
    public static final long STREAK_MIN_SESSION_MS = 300_000L;

    private static long lastPulseAt;

    private SparringSystem() {}

    public static SparPlayerRuntime runtime(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return RUNTIME.computeIfAbsent(uuid, u -> new SparPlayerRuntime());
    }

    public static boolean isSparring(UUID uuid) {
        SparPlayerRuntime rt = uuid == null ? null : RUNTIME.get(uuid);
        return rt != null && rt.active;
    }

    public static void onLogin(ServerPlayer player) {
        if (player != null) {
            runtime(player.m_20148_());
        }
    }

    public static void onLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        SparPlayerRuntime rt = RUNTIME.get(player.m_20148_());
        if (rt != null && rt.active && rt.partner != null) {
            ServerPlayer partner = player.m_20194_() == null
                    ? null
                    : player.m_20194_().m_6846_().m_11259_(rt.partner);
            endSession(player, partner, "logout");
        }
        RUNTIME.remove(player.m_20148_());
    }

    public static void pulse(MinecraftServer server, int tick) {
        if (!DifficultyConfig.get().enableSparringSystem || server == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPulseAt < TICK_MS) {
            return;
        }
        lastPulseAt = now;
        expireInvites(now);
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (player == null) {
                continue;
            }
            SparPlayerRuntime rt = runtime(player.m_20148_());
            resolvePending(player, rt, now);
            if (!rt.active || rt.partner == null) {
                continue;
            }
            if (RivalChallengeManager.get().isInChallenge(player.m_20148_())) {
                ServerPlayer partner = server.m_6846_().m_11259_(rt.partner);
                endSession(player, partner, "rival-challenge");
                continue;
            }
            ServerPlayer partner = server.m_6846_().m_11259_(rt.partner);
            if (partner == null) {
                endSession(player, null, "partner-offline");
                continue;
            }
            tickMovement(player, rt, now);
            tickClash(player, partner, rt, now);
            tickReleaseControl(player, partner, rt, now);
            tickPerfectBanner(player, partner, rt, now);
            tickActivity(player, partner, rt, now);
        }
        SparStore.get().saveIfNeeded(now);
    }

    public static void onPlayerHurt(ServerPlayer victim, ServerPlayer attacker, DamageSource source) {
        if (!DifficultyConfig.get().enableSparringSystem) {
            return;
        }
        if (victim == null || attacker == null || victim.m_20148_().equals(attacker.m_20148_())) {
            return;
        }
        if (RivalChallengeManager.get().isInChallenge(victim.m_20148_())
                || RivalChallengeManager.get().isInChallenge(attacker.m_20148_())) {
            return;
        }
        boolean ki = DmzRewards.isKiDamage(source);
        String kiKind = ki ? SparCombat.classifyKiType(source) : "";
        recordCombatExchange(attacker, victim, ki, kiKind);
        SparPlayerRuntime vRt = runtime(victim.m_20148_());
        if (vRt.active && vRt.partner != null && vRt.partner.equals(attacker.m_20148_())) {
            vRt.pendingSampleHp = victim.m_21223_() + victim.m_6103_();
            vRt.pendingAttacker = attacker.m_20148_();
            vRt.pendingKi = ki;
            vRt.pendingKiKind = kiKind;
            vRt.pendingUntil = System.currentTimeMillis() + PENDING_HP_RESOLVE_MS;
            maybeFriendlyFist(victim, attacker, vRt);
        }
    }

    public static void onDeath(ServerPlayer victim) {
        if (victim == null) {
            return;
        }
        SparPlayerRuntime rt = RUNTIME.get(victim.m_20148_());
        if (rt != null && rt.active) {
            ServerPlayer partner = victim.m_20194_() == null || rt.partner == null
                    ? null
                    : victim.m_20194_().m_6846_().m_11259_(rt.partner);
            endSession(victim, partner, "death");
        }
    }

    private static void resolvePending(ServerPlayer player, SparPlayerRuntime rt, long now) {
        if (rt.pendingUntil <= 0L || now < rt.pendingUntil || rt.pendingAttacker == null) {
            return;
        }
        // Health + absorption (script getHealthPool).
        float nowPool = player.m_21223_() + player.m_6103_();
        float lost = Math.max(0.0f, rt.pendingSampleHp - nowPool);
        UUID atkId = rt.pendingAttacker;
        boolean ki = rt.pendingKi;
        String kiKind = rt.pendingKiKind;
        rt.pendingUntil = 0L;
        rt.pendingAttacker = null;
        if (!(lost > 0.01f)) {
            // Fully mitigated kiblast still credits a token floor so ki training registers.
            if (ki) {
                lost = SparCombat.KI_FULL_MIT_FLOOR;
            } else {
                return;
            }
        }
        if (!rt.active || rt.partner == null || !rt.partner.equals(atkId)) {
            return;
        }
        MinecraftServer server = player.m_20194_();
        ServerPlayer attacker = server == null ? null : server.m_6846_().m_11259_(atkId);
        if (attacker == null) {
            return;
        }
        SparPlayerRuntime atkRt = runtime(atkId);
        rt.sessionTaken += lost;
        SparCombat.awardDamageTp(attacker, player, atkRt, lost, ki, kiKind);
        if (SparCombat.isBlocking(player)) {
            rt.sessionBlocks++;
            rt.styleBlock += 1.0; // script: +1 per block, not HP lost
            SparCombat.awardCombatTp(player, attacker, rt, SparCombat.BLOCK_TP_BASE, "melee");
        }
    }

    private static void maybeFriendlyFist(ServerPlayer victim, ServerPlayer attacker, SparPlayerRuntime vRt) {
        if (victim == null || attacker == null || vRt == null) {
            return;
        }
        try {
            if (vRt.ffKdHealed) {
                return;
            }
            StatsData data = DmzProgression.stats(victim);
            Status status = data == null ? null : data.getStatus();
            if (status == null) {
                return;
            }
            // Either fighter having Friendly Fist can save the knockdown victim.
            boolean ffOn = status.isFriendlyFistEnabled()
                    || isFriendlyFistOn(attacker);
            if (!ffOn) {
                return;
            }
            boolean kd = status.isKnockedDown();
            float hp = victim.m_21223_();
            boolean lethal = hp <= 1.5f;
            if (!kd && !lethal) {
                return;
            }
            float max = victim.m_21233_();
            victim.m_21153_(max);
            try {
                status.setKnockedDown(false);
            } catch (Throwable ignored) {
            }
            vRt.ffKdHealed = true;
            long now = System.currentTimeMillis();
            stampHitActivity(vRt, attacker.m_7755_().getString(), now, "ki");
            SparPlayerRuntime aRt = runtime(attacker.m_20148_());
            stampHitActivity(aRt, victim.m_7755_().getString(), now, "ki");
            refreshMovementActivity(victim, vRt, now);
            refreshMovementActivity(attacker, aRt, now);
            vRt.graceUntil = 0L;
            aRt.graceUntil = 0L;
            if (now >= vRt.messageNext) {
                vRt.messageNext = now + 4000L;
                DmzRewards.msg(attacker, "§6[Sparring] §aFriendly Fist §7knockdown — healed §f"
                        + victim.m_7755_().getString() + "§7.");
                DmzRewards.msg(victim, "§6[Sparring] §aFriendly Fist §7heal from §f"
                        + attacker.m_7755_().getString() + "§7.");
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isFriendlyFistOn(ServerPlayer player) {
        try {
            StatsData data = DmzProgression.stats(player);
            Status status = data == null ? null : data.getStatus();
            return status != null && status.isFriendlyFistEnabled();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void recordCombatExchange(
            ServerPlayer attacker,
            ServerPlayer target,
            boolean ki,
            String kiKind
    ) {
        SparPlayerRuntime aRt = runtime(attacker.m_20148_());
        SparPlayerRuntime tRt = runtime(target.m_20148_());
        long now = System.currentTimeMillis();
        String aName = attacker.m_7755_().getString();
        String tName = target.m_7755_().getString();

        boolean stampOut = !aRt.active || (aRt.partner != null && aRt.partner.equals(target.m_20148_()));
        if (stampOut) {
            aRt.lastOutPartner = tName;
            aRt.lastOutAt = now;
            aRt.lastOutKind = ki ? "ki" : "melee";
            tRt.lastInPartner = aName;
            tRt.lastInAt = now;
            if (ki) {
                aRt.lastKiOutAt = now;
                aRt.lastLaserOutAt = now;
            }
        }
        if (aRt.active || tRt.active) {
            return;
        }
        if (!tRt.lastOutPartner.equalsIgnoreCase(aName)) {
            return;
        }
        if (now - tRt.lastOutAt > SESSION_START_WINDOW_MS) {
            return;
        }
        if (attacker.m_9236_() != target.m_9236_()) {
            return;
        }
        if (attacker.m_20270_(target) > MAX_SPAR_DISTANCE) {
            return;
        }
        if (now < aRt.restartCooldownUntil || now < tRt.restartCooldownUntil) {
            return;
        }
        startSession(attacker, target);
    }

    private static void startSession(ServerPlayer a, ServerPlayer b) {
        SparPlayerRuntime aRt = runtime(a.m_20148_());
        SparPlayerRuntime bRt = runtime(b.m_20148_());
        long now = System.currentTimeMillis();
        aRt.resetSession();
        bRt.resetSession();
        aRt.active = true;
        bRt.active = true;
        aRt.partner = b.m_20148_();
        bRt.partner = a.m_20148_();
        aRt.startAt = now;
        bRt.startAt = now;
        // Seed movement window only — hits/blocks must not refresh the AFK gate.
        refreshMovementActivity(a, aRt, now);
        refreshMovementActivity(b, bRt, now);
        DmzRewards.msg(a, "§6[Sparring] §aSession started with §f" + b.m_7755_().getString());
        DmzRewards.msg(b, "§6[Sparring] §aSession started with §f" + a.m_7755_().getString());
        DmzRewards.msg(a, "§8Stay active: trade damage, move, and keep the fight going.");
        DmzRewards.msg(b, "§8Stay active: trade damage, move, and keep the fight going.");
        SystemTelemetry.log("sparring", "spar_start", a, b, null);
    }

    public static void endSession(ServerPlayer player, ServerPlayer partner, String reason) {
        if (player == null) {
            return;
        }
        SparPlayerRuntime aRt = runtime(player.m_20148_());
        if (!aRt.active) {
            return;
        }
        SparPlayerRuntime bRt = partner == null ? null : runtime(partner.m_20148_());
        long now = System.currentTimeMillis();
        long duration = Math.max(0L, now - aRt.startAt);
        SparCombat.flushTpMessage(player, aRt);
        if (partner != null && bRt != null) {
            SparCombat.flushTpMessage(partner, bRt);
        }
        report(player, aRt, partner, duration, reason);
        if (partner != null && bRt != null && bRt.active) {
            report(partner, bRt, player, duration, reason);
            updateLeaderboard(partner, bRt, duration);
            updateStreak(partner, bRt, duration);
            bRt.resetSession();
            bRt.restartCooldownUntil = now + PAIR_RESTART_COOLDOWN_MS;
        }
        updateLeaderboard(player, aRt, duration);
        updateStreak(player, aRt, duration);
        SystemTelemetry.log("sparring", "spar_end", player, partner,
                SystemTelemetry.fields("reason", reason == null ? "" : reason,
                        "tp", (int) aRt.sessionTp, "ms", duration));
        aRt.resetSession();
        aRt.restartCooldownUntil = now + PAIR_RESTART_COOLDOWN_MS;
        SparStore.get().markDirty();
    }

    private static void report(
            ServerPlayer player,
            SparPlayerRuntime rt,
            ServerPlayer partner,
            long durationMs,
            String reason
    ) {
        String who = partner == null ? "?" : partner.m_7755_().getString();
        DmzRewards.msg(player, "§6[Sparring] §7Session ended with §f" + who
                + " §8(" + reason + ", " + (durationMs / 1000L) + "s)");
        DmzRewards.msg(player, "§7TP §a+" + DmzRewards.formatWhole(rt.sessionTp)
                + " §8| melee §f" + (int) rt.sessionMelee
                + " §8| ki §f" + (int) rt.sessionKi
                + " §8| combo §f" + rt.sessionMaxCombo);
    }

    private static void updateLeaderboard(ServerPlayer player, SparPlayerRuntime rt, long durationMs) {
        if (durationMs < MIN_COUNTED_SESSION_MS) {
            return;
        }
        SparStore.LeaderboardEntry e = SparStore.get().leaderboard.computeIfAbsent(
                player.m_20148_().toString(), k -> new SparStore.LeaderboardEntry());
        e.name = player.m_7755_().getString();
        e.totalTp += rt.sessionTp;
        e.longestMs = Math.max(e.longestMs, durationMs);
        e.bestPayout = Math.max(e.bestPayout, rt.sessionTp);
        e.totalTimeMs += durationMs;
        e.sessions++;
        if (rt.sessionPerfect) {
            e.perfectSessions++;
        }
        e.highestCombo = Math.max(e.highestCombo, rt.sessionMaxCombo);
    }

    private static void updateStreak(ServerPlayer player, SparPlayerRuntime rt, long durationMs) {
        if (durationMs < STREAK_MIN_SESSION_MS) {
            return;
        }
        SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
        long today = System.currentTimeMillis() / 86_400_000L;
        if (bond.streakLastDay >= 0 && today - bond.streakLastDay > 1) {
            bond.streakCurrent = 0;
        }
        if (bond.streakLastDay != today) {
            bond.streakCurrent++;
            bond.streakLastDay = today;
            bond.streakBest = Math.max(bond.streakBest, bond.streakCurrent);
        }
    }

    /** Seed / hold the movement AFK window (script refreshMovementActivity). */
    private static void refreshMovementActivity(ServerPlayer player, SparPlayerRuntime rt, long now) {
        if (player == null || rt == null) {
            return;
        }
        rt.moveX = player.m_20185_();
        rt.moveY = player.m_20186_();
        rt.moveZ = player.m_20189_();
        rt.moveValidUntil = now + MOVEMENT_ACTIVITY_WINDOW_MS;
    }

    /**
     * Script {@code updateMovement}: only real displacement / velocity refreshes the AFK gate.
     * Hits and blocks never call this — standing still and punching (box farm) expires the window.
     */
    private static void tickMovement(ServerPlayer player, SparPlayerRuntime rt, long now) {
        double x = player.m_20185_();
        double y = player.m_20186_();
        double z = player.m_20189_();
        double dx = x - rt.moveX;
        double dy = y - rt.moveY;
        double dz = z - rt.moveZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double speed = readMotionSpeed(player);
        rt.moveX = x;
        rt.moveY = y;
        rt.moveZ = z;
        if (dist >= MIN_MOVEMENT_DISTANCE || speed >= MIN_MOTION_SPEED) {
            rt.moveValidUntil = now + MOVEMENT_ACTIVITY_WINDOW_MS;
            rt.styleMove += 1.0;
        }
        if (speed >= HEAVY_MOTION_SPEED) {
            rt.heavyMotionUntil = now + 2500L;
        }
    }

    private static double readMotionSpeed(ServerPlayer player) {
        try {
            var motion = player.m_20184_();
            if (motion == null) {
                return 0.0;
            }
            double mx = motion.f_82479_;
            double my = motion.f_82480_;
            double mz = motion.f_82481_;
            return Math.sqrt(mx * mx + my * my + mz * mz);
        } catch (Throwable ignored) {
            return 0.0;
        }
    }

    private static boolean hasRecentMovement(SparPlayerRuntime rt, long now) {
        return rt != null && rt.moveValidUntil > 0L && now <= rt.moveValidUntil;
    }

    private static void tickClash(ServerPlayer player, ServerPlayer partner, SparPlayerRuntime rt, long now) {
        boolean selfClash = DmzRewards.isClashing(player.m_20148_());
        boolean partnerClash = DmzRewards.isClashing(partner.m_20148_());
        boolean bothClashing = selfClash && partnerClash;
        if (selfClash || partnerClash) {
            // Soft linger keeps activity gates alive even if only one side reports clash.
            rt.clashUntil = now + 4000L;
        }
        // Script: TP drip only while BOTH fighters are actively clashing.
        if (bothClashing && now >= rt.clashNext) {
            rt.clashNext = now + 500L;
            rt.sessionClashMs += 500L;
            rt.styleBeam += 1.0;
            SparCombat.awardCombatTp(player, partner, rt, SparCombat.BEAM_CLASH_TP_PER_TICK, "clash");
        }
        // Charging / clash holds hit + movement gates (script holdSparForKiCharge).
        holdSparForKiOrClash(player, partner, rt, now);
    }

    /**
     * Standing still mid-charge / clash must not trip AFK or hit-activity gates.
     * @return true when the pair is currently held
     */
    private static boolean holdSparForKiOrClash(
            ServerPlayer player, ServerPlayer partner, SparPlayerRuntime rt, long now
    ) {
        if (player == null || partner == null || rt == null) {
            return false;
        }
        boolean clashing = now <= rt.clashUntil
                || DmzRewards.isClashing(player.m_20148_())
                || DmzRewards.isClashing(partner.m_20148_());
        boolean charging = isChargingKi(player) || isChargingKi(partner);
        if (!clashing && !charging) {
            return false;
        }
        SparPlayerRuntime pRt = runtime(partner.m_20148_());
        stampHitActivity(rt, partner.m_7755_().getString(), now, "ki");
        stampHitActivity(pRt, player.m_7755_().getString(), now, "ki");
        refreshMovementActivity(player, rt, now);
        refreshMovementActivity(partner, pRt, now);
        return true;
    }

    private static void stampHitActivity(SparPlayerRuntime rt, String partnerName, long now, String kind) {
        if (rt == null) {
            return;
        }
        rt.lastOutAt = now;
        rt.lastOutKind = kind == null ? "melee" : kind;
        if (partnerName != null) {
            rt.lastOutPartner = partnerName;
        }
    }

    private static boolean isChargingKi(ServerPlayer player) {
        try {
            StatsData data = DmzProgression.stats(player);
            Status status = data == null ? null : data.getStatus();
            return status != null && (status.isChargingKi() || status.isActionCharging());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void tickReleaseControl(
            ServerPlayer player,
            ServerPlayer partner,
            SparPlayerRuntime rt,
            long now
    ) {
        if (rt == null || !rt.active || partner == null) {
            return;
        }
        SparCombat.TrainingValues values = SparCombat.liveValues(player);
        if (values == null || values.release < SparCombat.HIGH_RELEASE_THRESHOLD) {
            return;
        }
        boolean recentHit = hasRecentOutgoingHit(rt, partner.m_7755_().getString(), now);
        boolean clashing = now <= rt.clashUntil;
        if (!recentHit && !clashing) {
            return;
        }
        if (now < rt.releaseCtrlNext) {
            return;
        }
        rt.releaseCtrlNext = now + 1000L;
        SparCombat.awardCombatTp(player, partner, rt, SparCombat.RELEASE_CONTROL_TP_PER_SEC, "release");
    }

    private static void tickPerfectBanner(
            ServerPlayer player,
            ServerPlayer partner,
            SparPlayerRuntime rt,
            long now
    ) {
        if (rt == null || !rt.active || partner == null) {
            return;
        }
        SparCombat.TrainingValues a = SparCombat.liveValues(player);
        SparCombat.TrainingValues b = SparCombat.liveValues(partner);
        if (!SparCombat.isPerfect(a, b)) {
            return;
        }
        rt.sessionPerfect = true;
        if (now < rt.messageNext) {
            return;
        }
        rt.messageNext = now + SparCombat.PERFECT_ACTIONBAR_MS;
        DmzRewards.msg(player, "§6§lPERFECT TRAINING ACTIVE");
    }

    /**
     * Script processSession activity gates:
     * both fighters must exchange damage AND keep moving (unless clash/ki-charge hold).
     * Prevents AFK box-farming: standing still punching still expires the move window.
     */
    private static void tickActivity(ServerPlayer player, ServerPlayer partner, SparPlayerRuntime rt, long now) {
        SparPlayerRuntime pRt = runtime(partner.m_20148_());
        boolean held = holdSparForKiOrClash(player, partner, rt, now);

        String failure = "";
        if (player.m_9236_() != partner.m_9236_() || player.m_20270_(partner) > MAX_SPAR_DISTANCE) {
            failure = "fighters moved too far apart";
        } else if (!held) {
            boolean hitA = hasRecentOutgoingHit(rt, partner.m_7755_().getString(), now);
            boolean hitB = hasRecentOutgoingHit(pRt, player.m_7755_().getString(), now);
            if (!hitA || !hitB) {
                failure = "both fighters must resume exchanging damage";
            } else if (!hasRecentMovement(rt, now) || !hasRecentMovement(pRt, now)) {
                failure = "both fighters must resume moving";
            }
        }

        if (!failure.isEmpty()) {
            handleRecoverableFailure(player, partner, rt, pRt, failure, now);
            return;
        }

        rt.graceUntil = 0L;
        rt.graceWarned = false;
        rt.graceReason = "";
        pRt.graceUntil = 0L;
        pRt.graceWarned = false;
        pRt.graceReason = "";

        if (rt.combo > 0 && now > rt.comboUntil) {
            rt.combo = 0;
        }
        if (rt.momentumTier > 0 && now > rt.momentumUntil) {
            rt.momentumTier = 0;
        }
    }

    private static void handleRecoverableFailure(
            ServerPlayer player,
            ServerPlayer partner,
            SparPlayerRuntime rt,
            SparPlayerRuntime pRt,
            String reason,
            long now
    ) {
        long graceMs = reason != null && reason.contains("far")
                ? DISTANCE_GRACE_PERIOD_MS
                : SESSION_GRACE_PERIOD_MS;
        if (rt.graceUntil <= 0L || !reason.equals(rt.graceReason)) {
            rt.graceUntil = now + graceMs;
            rt.graceReason = reason;
            rt.graceWarned = false;
            if (pRt != null) {
                pRt.graceUntil = rt.graceUntil;
                pRt.graceReason = reason;
                pRt.graceWarned = false;
            }
        }
        if (!rt.graceWarned) {
            rt.graceWarned = true;
            if (pRt != null) {
                pRt.graceWarned = true;
            }
            long left = Math.max(1L, (rt.graceUntil - now + 999L) / 1000L);
            String msg = "§6[Sparring] §eRecover within " + left + "s§8 - " + reason;
            DmzRewards.msg(player, msg);
            if (partner != null) {
                DmzRewards.msg(partner, msg);
            }
        }
        if (now >= rt.graceUntil) {
            endSession(player, partner, reason);
        }
    }

    private static boolean hasRecentOutgoingHit(SparPlayerRuntime rt, String partnerName, long now) {
        if (rt == null || partnerName == null || partnerName.isBlank()) {
            return false;
        }
        if (!rt.lastOutPartner.equalsIgnoreCase(partnerName)) {
            return false;
        }
        long window = "ki".equalsIgnoreCase(rt.lastOutKind) || "beam".equalsIgnoreCase(rt.lastOutKind)
                ? KI_HIT_ACTIVITY_WINDOW_MS
                : MELEE_HIT_ACTIVITY_WINDOW_MS;
        return now - rt.lastOutAt <= window;
    }

    public static void registerCombatHit(SparPlayerRuntime rt) {
        long now = System.currentTimeMillis();
        if (now <= rt.comboUntil) {
            rt.combo++;
        } else {
            rt.combo = 1;
        }
        rt.comboUntil = now + COMBO_TIMEOUT_MS;
        rt.sessionMaxCombo = Math.max(rt.sessionMaxCombo, rt.combo);
        SparCombat.updateMomentum(rt);
    }

    public static void breakCombo(ServerPlayer player, SparPlayerRuntime rt, String reason) {
        if (rt == null) {
            return;
        }
        if (rt.combo > 0 || rt.momentumTier > 0) {
            long now = System.currentTimeMillis();
            if (now >= rt.messageNext) {
                rt.messageNext = now + 1500L;
                DmzRewards.msg(player, "§c[Sparring] Combo broken (" + reason + ").");
            }
        }
        rt.combo = 0;
        rt.comboUntil = 0L;
        rt.momentumTier = 0;
        rt.momentumUntil = 0L;
    }

    public static boolean isSparringWithOwnMentor(ServerPlayer player, ServerPlayer partner) {
        if (player == null || partner == null) {
            return false;
        }
        SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
        return bond.mentorUuid != null
                && bond.mentorUuid.equals(partner.m_20148_().toString());
    }

    public static void shareTpWithMentor(ServerPlayer apprentice, int amount) {
        if (apprentice == null || amount <= 0) {
            return;
        }
        SparStore.MentorBond bond = SparStore.get().bond(apprentice.m_20148_());
        if (bond.mentorUuid == null || bond.mentorUuid.isBlank()) {
            return;
        }
        MinecraftServer server = apprentice.m_20194_();
        if (server == null) {
            return;
        }
        try {
            ServerPlayer mentor = server.m_6846_().m_11259_(UUID.fromString(bond.mentorUuid));
            if (mentor == null) {
                return;
            }
            int share = Math.max(1, Math.round(amount * SparCombat.MENTOR_SHARE_PCT));
            DmzRewards.awardTp(mentor, share, "Mentor share from " + apprentice.m_7755_().getString(),
                    true, "§6[Mentor] ");
        } catch (Throwable ignored) {
        }
    }

    /* ========================= Commands / mentor ========================= */

    public static List<String> statsLines(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        SparPlayerRuntime rt = runtime(player.m_20148_());
        SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
        SparStore.LeaderboardEntry lb = SparStore.get().leaderboard.get(player.m_20148_().toString());
        lines.add("§6§lSparring Stats §8— §f" + player.m_7755_().getString());
        if (rt.active && rt.partner != null) {
            lines.add("§aActive spar §7with partner UUID ending …"
                    + rt.partner.toString().substring(24)
                    + " §8TP this session §a" + (int) rt.sessionTp);
        } else {
            lines.add("§7No active spar session.");
        }
        lines.add("§7Mentor §f" + (bond.mentorName == null || bond.mentorName.isBlank() ? "none" : bond.mentorName));
        lines.add("§7Apprentice §f" + (bond.apprenticeName == null || bond.apprenticeName.isBlank() ? "none" : bond.apprenticeName));
        lines.add("§7Streak §f" + bond.streakCurrent + " §8(best " + bond.streakBest + ")");
        if (lb != null) {
            lines.add("§7Lifetime TP §a" + DmzRewards.formatWhole(lb.totalTp)
                    + " §8| sessions §f" + lb.sessions
                    + " §8| best combo §f" + lb.highestCombo);
        }
        return lines;
    }

    public static List<String> topLines(String category, int limit) {
        List<String> lines = new ArrayList<>();
        String cat = category == null || category.isBlank() ? "tp" : category.trim().toLowerCase();
        lines.add("§6§lSparring Top §8— §f" + cat);
        List<Map.Entry<String, SparStore.LeaderboardEntry>> entries =
                new ArrayList<>(SparStore.get().leaderboard.entrySet());
        entries.sort((a, b) -> {
            SparStore.LeaderboardEntry ea = a.getValue();
            SparStore.LeaderboardEntry eb = b.getValue();
            double va = switch (cat) {
                case "sessions", "session" -> ea == null ? 0 : ea.sessions;
                case "perfect", "perfects" -> ea == null ? 0 : ea.perfectSessions;
                case "combo" -> ea == null ? 0 : ea.highestCombo;
                case "time" -> ea == null ? 0 : ea.totalTimeMs;
                default -> ea == null ? 0 : ea.totalTp;
            };
            double vb = switch (cat) {
                case "sessions", "session" -> eb == null ? 0 : eb.sessions;
                case "perfect", "perfects" -> eb == null ? 0 : eb.perfectSessions;
                case "combo" -> eb == null ? 0 : eb.highestCombo;
                case "time" -> eb == null ? 0 : eb.totalTimeMs;
                default -> eb == null ? 0 : eb.totalTp;
            };
            return Double.compare(vb, va);
        });
        int i = 1;
        for (Map.Entry<String, SparStore.LeaderboardEntry> e : entries) {
            if (i > Math.max(1, limit)) {
                break;
            }
            SparStore.LeaderboardEntry lb = e.getValue();
            if (lb == null) {
                continue;
            }
            String value = switch (cat) {
                case "sessions", "session" -> String.valueOf(lb.sessions);
                case "perfect", "perfects" -> String.valueOf(lb.perfectSessions);
                case "combo" -> String.valueOf(lb.highestCombo);
                case "time" -> (lb.totalTimeMs / 60000L) + "m";
                default -> DmzRewards.formatWhole(lb.totalTp) + " TP";
            };
            lines.add("§e#" + i + " §f" + (lb.name == null || lb.name.isBlank() ? "?" : lb.name)
                    + " §7" + value);
            i++;
        }
        if (i == 1) {
            lines.add("§7No sparring data yet.");
        }
        return lines;
    }

    public static String endCommand(ServerPlayer player) {
        SparPlayerRuntime rt = runtime(player.m_20148_());
        if (!rt.active) {
            return "§cNo active spar session.";
        }
        ServerPlayer partner = player.m_20194_() == null || rt.partner == null
                ? null
                : player.m_20194_().m_6846_().m_11259_(rt.partner);
        endSession(player, partner, "command");
        return "§eSpar session ended.";
    }

    public static String mentorInvite(ServerPlayer player, ServerPlayer target) {
        if (target == null) {
            return "§cPlayer not online.";
        }
        if (player.m_20148_().equals(target.m_20148_())) {
            return "§cYou cannot mentor yourself.";
        }
        SparStore.MentorBond mine = SparStore.get().bond(player.m_20148_());
        long now = System.currentTimeMillis();
        if (now < mine.mentorChangeReadyAt) {
            return "§cMentor change cooldown active.";
        }
        if (mine.apprenticeUuid != null && !mine.apprenticeUuid.isBlank()) {
            return "§cYou already have an apprentice.";
        }
        SparStore.BondInvite invite = new SparStore.BondInvite();
        invite.fromUuid = player.m_20148_().toString();
        invite.fromName = player.m_7755_().getString();
        invite.kind = "apprentice"; // player asks target to be apprentice
        invite.expiresAt = now + MENTOR_INVITE_MS;
        SparStore.get().invites.put(target.m_20148_().toString(), invite);
        SparStore.get().markDirty();
        DmzRewards.msg(target, "§6[Mentor Bond] §f" + invite.fromName
                + " §ewants you as their Apprentice.");
        DmzRewards.msg(target, "§8Open §e/spar §8→ Mentor → Pending to Accept or Decline");
        return "§aInvite sent to §f" + target.m_7755_().getString() + "§a.";
    }

    public static String apprenticeInvite(ServerPlayer player, ServerPlayer target) {
        if (target == null) {
            return "§cPlayer not online.";
        }
        if (player.m_20148_().equals(target.m_20148_())) {
            return "§cInvalid target.";
        }
        SparStore.MentorBond mine = SparStore.get().bond(player.m_20148_());
        long now = System.currentTimeMillis();
        if (now < mine.apprenticeChangeReadyAt) {
            return "§cApprentice change cooldown active.";
        }
        if (mine.mentorUuid != null && !mine.mentorUuid.isBlank()) {
            return "§cYou already have a mentor.";
        }
        SparStore.BondInvite invite = new SparStore.BondInvite();
        invite.fromUuid = player.m_20148_().toString();
        invite.fromName = player.m_7755_().getString();
        invite.kind = "mentor"; // player asks target to be mentor
        invite.expiresAt = now + MENTOR_INVITE_MS;
        SparStore.get().invites.put(target.m_20148_().toString(), invite);
        SparStore.get().markDirty();
        DmzRewards.msg(target, "§6[Mentor Bond] §f" + invite.fromName
                + " §ewants you as their Mentor.");
        DmzRewards.msg(target, "§8Open §e/spar §8→ Mentor → Pending to Accept or Decline");
        return "§aInvite sent to §f" + target.m_7755_().getString() + "§a.";
    }

    /**
     * Encoded pending mentor invites for GUI boards:
     * {@code uuid\tname\tIN|OUT\texpiresAtMs\tonline(0/1)\tkind(mentor|apprentice)}.
     * IN = someone invited you; OUT = you invited them. Kind is the role requested of the recipient.
     */
    public static List<String> pendingMentorInviteCards(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        if (player == null) {
            return out;
        }
        expireInvites(System.currentTimeMillis());
        String me = player.m_20148_().toString();
        MinecraftServer server = player.m_20194_();
        long now = System.currentTimeMillis();

        SparStore.BondInvite incoming = SparStore.get().invites.get(me);
        if (incoming != null && now <= incoming.expiresAt) {
            String uuid = incoming.fromUuid == null ? "" : incoming.fromUuid;
            String name = incoming.fromName == null || incoming.fromName.isBlank() ? uuid : incoming.fromName;
            name = name.replace('\t', ' ').replace('\n', ' ');
            boolean online = isOnline(server, uuid);
            String kind = incoming.kind == null || incoming.kind.isBlank() ? "apprentice" : incoming.kind;
            out.add(uuid + "\t" + name + "\tIN\t" + incoming.expiresAt + "\t" + (online ? "1" : "0")
                    + "\t" + kind);
        }

        for (Map.Entry<String, SparStore.BondInvite> e : SparStore.get().invites.entrySet()) {
            SparStore.BondInvite inv = e.getValue();
            if (inv == null || now > inv.expiresAt) {
                continue;
            }
            if (inv.fromUuid == null || !me.equals(inv.fromUuid)) {
                continue;
            }
            String targetUuid = e.getKey() == null ? "" : e.getKey();
            if (targetUuid.equals(me)) {
                continue;
            }
            String name = resolveName(server, targetUuid, inv);
            name = name.replace('\t', ' ').replace('\n', ' ');
            boolean online = isOnline(server, targetUuid);
            String kind = inv.kind == null || inv.kind.isBlank() ? "apprentice" : inv.kind;
            out.add(targetUuid + "\t" + name + "\tOUT\t" + inv.expiresAt + "\t" + (online ? "1" : "0")
                    + "\t" + kind);
        }
        return out;
    }

    public static int pendingMentorInviteCount(ServerPlayer player) {
        return pendingMentorInviteCards(player).size();
    }

    /** Incoming invite args ({@code uuid:} preferred) for Accept/Decline pickers. */
    public static List<String> pendingIncomingMentorArgs(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        if (player == null) {
            return out;
        }
        expireInvites(System.currentTimeMillis());
        SparStore.BondInvite incoming = SparStore.get().invites.get(player.m_20148_().toString());
        if (incoming == null || System.currentTimeMillis() > incoming.expiresAt) {
            return out;
        }
        if (incoming.fromUuid != null && !incoming.fromUuid.isBlank()) {
            out.add("uuid:" + incoming.fromUuid);
        } else if (incoming.fromName != null && !incoming.fromName.isBlank()) {
            out.add(incoming.fromName);
        }
        return out;
    }

    public static String mentorAccept(ServerPlayer player) {
        return mentorAccept(player, null);
    }

    public static String mentorAccept(ServerPlayer player, String fromArg) {
        SparStore.BondInvite invite = SparStore.get().invites.get(player.m_20148_().toString());
        if (invite == null || System.currentTimeMillis() > invite.expiresAt) {
            SparStore.get().invites.remove(player.m_20148_().toString());
            SparStore.get().markDirty();
            return "§cNo pending invite.";
        }
        if (fromArg != null && !fromArg.isBlank() && !matchesInviteFrom(invite, fromArg)) {
            return "§cThat invite is not pending for you.";
        }
        MinecraftServer server = player.m_20194_();
        ServerPlayer other = resolveInviter(server, invite);
        if (other == null) {
            return "§cInviter is no longer online.";
        }
        ServerPlayer mentor;
        ServerPlayer apprentice;
        if ("mentor".equals(invite.kind)) {
            mentor = player;
            apprentice = other;
        } else {
            mentor = other;
            apprentice = player;
        }
        bindMentor(mentor, apprentice);
        SparStore.get().invites.remove(player.m_20148_().toString());
        SparStore.get().markDirty();
        DmzRewards.msg(mentor, "§6[Mentor Bond] §aYou are now mentoring §f" + apprentice.m_7755_().getString());
        DmzRewards.msg(apprentice, "§6[Mentor Bond] §aYour mentor is now §f" + mentor.m_7755_().getString());
        return "§aMentor bond created.";
    }

    public static String mentorDecline(ServerPlayer player) {
        return mentorDecline(player, null);
    }

    public static String mentorDecline(ServerPlayer player, String fromArg) {
        SparStore.BondInvite invite = SparStore.get().invites.get(player.m_20148_().toString());
        if (invite == null) {
            return "§cNo pending invite.";
        }
        if (fromArg != null && !fromArg.isBlank() && !matchesInviteFrom(invite, fromArg)) {
            return "§cThat invite is not pending for you.";
        }
        SparStore.get().invites.remove(player.m_20148_().toString());
        MinecraftServer server = player.m_20194_();
        if (server != null) {
            try {
                ServerPlayer other = server.m_6846_().m_11259_(UUID.fromString(invite.fromUuid));
                if (other != null) {
                    DmzRewards.msg(other, "§6[Mentor Bond] §f" + player.m_7755_().getString()
                            + " §cdenied your invite.");
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        SparStore.get().markDirty();
        return "§7Invite denied.";
    }

    /** Cancel an outgoing invite you sent to {@code targetArg} (uuid: or name). */
    public static String mentorCancelInvite(ServerPlayer player, String targetArg) {
        if (player == null) {
            return "§cPlayers only.";
        }
        if (targetArg == null || targetArg.isBlank()) {
            return "§cPick whose invite to cancel.";
        }
        expireInvites(System.currentTimeMillis());
        String me = player.m_20148_().toString();
        String targetKey = resolveInviteTargetKey(player, targetArg);
        if (targetKey == null) {
            return "§cNo outgoing invite to that player.";
        }
        SparStore.BondInvite inv = SparStore.get().invites.get(targetKey);
        if (inv == null || inv.fromUuid == null || !me.equals(inv.fromUuid)) {
            return "§cNo outgoing invite to that player.";
        }
        SparStore.get().invites.remove(targetKey);
        SparStore.get().markDirty();
        MinecraftServer server = player.m_20194_();
        if (server != null) {
            try {
                ServerPlayer other = server.m_6846_().m_11259_(UUID.fromString(targetKey));
                if (other != null) {
                    DmzRewards.msg(other, "§6[Mentor Bond] §f" + player.m_7755_().getString()
                            + " §7cancelled their invite.");
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return "§7Outgoing invite cancelled.";
    }

    /** Leave mentor bond or release apprentice, whichever applies. */
    public static String removeBond(ServerPlayer player) {
        SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
        boolean hasMentor = bond.mentorUuid != null && !bond.mentorUuid.isBlank();
        boolean hasApprentice = bond.apprenticeUuid != null && !bond.apprenticeUuid.isBlank();
        if (hasApprentice) {
            return removeApprentice(player);
        }
        if (hasMentor) {
            return removeMentor(player);
        }
        return "§cYou have no mentor bond to remove.";
    }

    public static String removeMentor(ServerPlayer player) {
        SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
        if (bond.mentorUuid == null || bond.mentorUuid.isBlank()) {
            return "§cYou have no mentor.";
        }
        String mentorName = bond.mentorName;
        clearBond(player.m_20148_().toString(), bond.mentorUuid, true);
        return "§7Left mentor §f" + mentorName + "§7. 7-day cooldown started.";
    }

    public static String removeApprentice(ServerPlayer player) {
        SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
        if (bond.apprenticeUuid == null || bond.apprenticeUuid.isBlank()) {
            return "§cYou have no apprentice.";
        }
        String name = bond.apprenticeName;
        clearBond(bond.apprenticeUuid, player.m_20148_().toString(), true);
        return "§7Released apprentice §f" + name + "§7. 7-day cooldown started.";
    }

    private static boolean isOnline(MinecraftServer server, String uuid) {
        if (server == null || uuid == null || uuid.isBlank()) {
            return false;
        }
        try {
            return server.m_6846_().m_11259_(UUID.fromString(uuid)) != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String resolveName(MinecraftServer server, String uuid, SparStore.BondInvite inv) {
        if (server != null && uuid != null && !uuid.isBlank()) {
            try {
                ServerPlayer online = server.m_6846_().m_11259_(UUID.fromString(uuid));
                if (online != null) {
                    return online.m_7755_().getString();
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return uuid == null || uuid.isBlank() ? "?" : uuid;
    }

    private static ServerPlayer resolveInviter(MinecraftServer server, SparStore.BondInvite invite) {
        if (server == null || invite == null || invite.fromUuid == null || invite.fromUuid.isBlank()) {
            return null;
        }
        try {
            return server.m_6846_().m_11259_(UUID.fromString(invite.fromUuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean matchesInviteFrom(SparStore.BondInvite invite, String fromArg) {
        if (invite == null || fromArg == null || fromArg.isBlank()) {
            return false;
        }
        String raw = fromArg.trim();
        if (raw.regionMatches(true, 0, "uuid:", 0, 5)) {
            return invite.fromUuid != null
                    && invite.fromUuid.equalsIgnoreCase(raw.substring(5).trim());
        }
        return invite.fromUuid != null && invite.fromUuid.equalsIgnoreCase(raw)
                || invite.fromName != null && invite.fromName.equalsIgnoreCase(raw);
    }

    private static String resolveInviteTargetKey(ServerPlayer player, String targetArg) {
        String raw = targetArg.trim();
        if (raw.regionMatches(true, 0, "uuid:", 0, 5)) {
            String id = raw.substring(5).trim();
            if (SparStore.get().invites.containsKey(id)) {
                return id;
            }
            return null;
        }
        if (SparStore.get().invites.containsKey(raw)) {
            return raw;
        }
        MinecraftServer server = player.m_20194_();
        if (server != null) {
            ServerPlayer online = server.m_6846_().m_11255_(raw);
            if (online != null) {
                String id = online.m_20148_().toString();
                if (SparStore.get().invites.containsKey(id)) {
                    return id;
                }
            }
        }
        return null;
    }

    public static String resetMentorCd(ServerPlayer admin, ServerPlayer target) {
        ServerPlayer t = target == null ? admin : target;
        SparStore.MentorBond bond = SparStore.get().bond(t.m_20148_());
        bond.mentorChangeReadyAt = 0L;
        bond.apprenticeChangeReadyAt = 0L;
        SparStore.get().markDirty();
        return "§aCleared mentor/apprentice cooldowns for §f" + t.m_7755_().getString();
    }

    public static String bondStatus(ServerPlayer player) {
        SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
        return "§6Mentor: §f" + blank(bond.mentorName, "none")
                + " §8| §6Apprentice: §f" + blank(bond.apprenticeName, "none");
    }

    private static void bindMentor(ServerPlayer mentor, ServerPlayer apprentice) {
        SparStore.MentorBond m = SparStore.get().bond(mentor.m_20148_());
        SparStore.MentorBond a = SparStore.get().bond(apprentice.m_20148_());
        m.apprenticeUuid = apprentice.m_20148_().toString();
        m.apprenticeName = apprentice.m_7755_().getString();
        a.mentorUuid = mentor.m_20148_().toString();
        a.mentorName = mentor.m_7755_().getString();
        SparStore.get().markDirty();
    }

    private static void clearBond(String apprenticeUuid, String mentorUuid, boolean cooldown) {
        long ready = System.currentTimeMillis() + MENTOR_CHANGE_COOLDOWN_MS;
        SparStore.MentorBond a = SparStore.get().bondsByPlayer.get(apprenticeUuid);
        SparStore.MentorBond m = SparStore.get().bondsByPlayer.get(mentorUuid);
        if (a != null) {
            a.mentorUuid = "";
            a.mentorName = "";
            if (cooldown) {
                a.mentorChangeReadyAt = ready;
            }
        }
        if (m != null) {
            m.apprenticeUuid = "";
            m.apprenticeName = "";
            if (cooldown) {
                m.apprenticeChangeReadyAt = ready;
            }
        }
        SparStore.get().markDirty();
    }

    private static void expireInvites(long now) {
        Iterator<Map.Entry<String, SparStore.BondInvite>> it = SparStore.get().invites.entrySet().iterator();
        while (it.hasNext()) {
            SparStore.BondInvite inv = it.next().getValue();
            if (inv == null || now > inv.expiresAt) {
                it.remove();
            }
        }
    }

    private static String blank(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v;
    }
}
