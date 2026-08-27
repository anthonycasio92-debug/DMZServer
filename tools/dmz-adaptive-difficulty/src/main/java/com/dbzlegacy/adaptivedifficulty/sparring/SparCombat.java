package com.dbzlegacy.adaptivedifficulty.sparring;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.rival.RivalChallengeManager;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.server.util.GravityLogic;
import net.minecraft.server.level.ServerPlayer;

/** Sparring TP formula from Sparring Tp System 3.2.11. */
public final class SparCombat {
    public static final float BASE_TP_PER_HIT = 280.0f;
    public static final float DAMAGE_QUALITY_REF = 800.0f;
    public static final float MIN_DAMAGE_QUALITY = 0.35f;
    public static final float MAX_DAMAGE_QUALITY = 1.80f;
    public static final float MAX_BASE_TP_PER_HIT = 700.0f;
    public static final float MAX_TP_PER_ACTION = 250_000.0f;
    public static final float MAX_TP_PER_ACTION_BP_SCALE = 4.0f;
    public static final float GLOBAL_TP_GAIN_MULT = 1.50f;
    public static final float BLOCK_TP_BASE = 40.0f;
    public static final float BEAM_CLASH_TP_PER_TICK = 35.0f;
    public static final float KNOCKBACK_RECOVERY_TP = 45.0f;
    public static final float MENTOR_SHARE_PCT = 0.15f;
    public static final float MENTOR_SPAR_BONUS_PCT = 0.18f;
    public static final float MAX_RIVAL_MULTIPLIER = 3.0f;
    public static final float MAX_BP_MULTIPLIER = 600.0f;
    public static final float MAX_GRAVITY = 1000.0f;
    public static final float MAX_GRAVITY_MULTIPLIER = 5.0f;
    public static final float MAX_EFFECTIVE_WEIGHT = 1000.0f;
    public static final float MAX_WEIGHT_MULTIPLIER = 2.0f;
    public static final float MIN_RELEASE_PERCENT = 100.0f;
    public static final float MAX_RELEASE_PERCENT = 200.0f;
    public static final float MAX_RELEASE_MULTIPLIER = 2.0f;
    public static final int MAX_PRESTIGE_LEVEL = 10;
    public static final float PRESTIGE_MULTIPLIER_PER_LEVEL = 0.10f;
    public static final float SESSION_BONUS_PER_MINUTE = 0.05f;
    public static final float MAX_SESSION_BONUS = 0.50f;
    public static final float PERFECT_TRAINING_MULTIPLIER = 2.0f;
    public static final float PERFECT_BP_DIFFERENCE = 0.10f;
    public static final float PERFECT_RELEASE_MIN = 180.0f;
    public static final float PERFECT_RELEASE_MAX = 200.0f;
    public static final float MELEE_EFF = 1.00f;

    private static final int[] MOMENTUM_THRESHOLDS = {5, 10, 15, 20, 30, 40};
    private static final float[] MOMENTUM_MULTIPLIERS = {1.05f, 1.10f, 1.20f, 1.35f, 1.50f, 2.00f};

    private static final double[] BP_ANCHORS = {
            1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000,
            100_000_000, 1_000_000_000L, 10_000_000_000L, 100_000_000_000L, 1_000_000_000_000L,
            10_000_000_000_000L, 100_000_000_000_000L
    };
    private static final double[] BP_MULT_ANCHORS = {
            1.0, 2.0, 5.0, 12.0, 25.0, 50.0, 100.0, 150.0, 250.0, 400.0, 600.0, 600.0, 600.0, 600.0, 600.0
    };

    private SparCombat() {}

    public static float damageQuality(double damage) {
        if (!(damage > 0.0) || !(DAMAGE_QUALITY_REF > 0.0f)) {
            return MIN_DAMAGE_QUALITY;
        }
        double quality = Math.sqrt(damage / DAMAGE_QUALITY_REF);
        return (float) DmzRewards.clamp(quality, MIN_DAMAGE_QUALITY, MAX_DAMAGE_QUALITY);
    }

    public static float battlePowerMultiplier(double battlePower) {
        if (!(battlePower > 0.0)) {
            return 1.0f;
        }
        if (battlePower <= BP_ANCHORS[0]) {
            return (float) BP_MULT_ANCHORS[0];
        }
        int n = Math.min(BP_ANCHORS.length, BP_MULT_ANCHORS.length);
        for (int i = 0; i < n - 1; i++) {
            if (battlePower <= BP_ANCHORS[i + 1]) {
                double lowerLog = Math.log10(BP_ANCHORS[i]);
                double upperLog = Math.log10(BP_ANCHORS[i + 1]);
                double currentLog = Math.log10(battlePower);
                double progress = (currentLog - lowerLog) / Math.max(0.0001, upperLog - lowerLog);
                return (float) (BP_MULT_ANCHORS[i] + (BP_MULT_ANCHORS[i + 1] - BP_MULT_ANCHORS[i]) * progress);
            }
        }
        double finalBp = BP_ANCHORS[n - 1];
        double finalMult = BP_MULT_ANCHORS[n - 1];
        double extraDecades = (Math.log(battlePower) - Math.log(finalBp)) / Math.log(10.0);
        return (float) Math.min(MAX_BP_MULTIPLIER, finalMult + extraDecades * 200.0);
    }

    public static float rivalMultiplier(double bpA, double bpB) {
        double highest = Math.max(bpA, bpB);
        if (!(highest > 0.0)) {
            return 1.0f;
        }
        double ratio = Math.min(bpA, bpB) / highest;
        if (ratio >= 0.90) {
            return Math.min(3.0f, MAX_RIVAL_MULTIPLIER);
        }
        if (ratio >= 0.80) {
            return Math.min(2.5f, MAX_RIVAL_MULTIPLIER);
        }
        if (ratio >= 0.50) {
            return Math.min(2.0f, MAX_RIVAL_MULTIPLIER);
        }
        if (ratio >= 0.25) {
            return Math.min(1.5f, MAX_RIVAL_MULTIPLIER);
        }
        return 1.0f;
    }

    public static float releaseMultiplier(double releasePercent) {
        double release = DmzRewards.clamp(releasePercent, MIN_RELEASE_PERCENT, MAX_RELEASE_PERCENT);
        double progress = (release - MIN_RELEASE_PERCENT) / (MAX_RELEASE_PERCENT - MIN_RELEASE_PERCENT);
        return (float) DmzRewards.clamp(1.0 + progress * (MAX_RELEASE_MULTIPLIER - 1.0), 1.0, MAX_RELEASE_MULTIPLIER);
    }

    public static float gravityMultiplier(double gravity) {
        double g = DmzRewards.clamp(gravity, 1.0, MAX_GRAVITY);
        double progress = (g - 1.0) / (MAX_GRAVITY - 1.0);
        return (float) DmzRewards.clamp(1.0 + progress * (MAX_GRAVITY_MULTIPLIER - 1.0), 1.0, MAX_GRAVITY_MULTIPLIER);
    }

    public static float weightMultiplier(double weight) {
        double w = DmzRewards.clamp(weight, 0.0, MAX_EFFECTIVE_WEIGHT);
        double progress = w / MAX_EFFECTIVE_WEIGHT;
        return (float) DmzRewards.clamp(1.0 + progress * (MAX_WEIGHT_MULTIPLIER - 1.0), 1.0, MAX_WEIGHT_MULTIPLIER);
    }

    public static float prestigeMultiplier(int prestige) {
        int p = Math.max(0, Math.min(MAX_PRESTIGE_LEVEL, prestige));
        return 1.0f + p * PRESTIGE_MULTIPLIER_PER_LEVEL;
    }

    public static float momentumMultiplier(SparPlayerRuntime rt) {
        if (rt == null || rt.momentumTier <= 0) {
            return 1.0f;
        }
        int idx = Math.min(rt.momentumTier, MOMENTUM_MULTIPLIERS.length) - 1;
        if (idx < 0) {
            return 1.0f;
        }
        return MOMENTUM_MULTIPLIERS[idx];
    }

    public static float sessionBonusMultiplier(SparPlayerRuntime rt) {
        if (rt == null || rt.startAt <= 0L) {
            return 1.0f;
        }
        long minutes = Math.max(0L, (System.currentTimeMillis() - rt.startAt) / 60_000L);
        float bonus = Math.min(MAX_SESSION_BONUS, minutes * SESSION_BONUS_PER_MINUTE);
        return 1.0f + bonus;
    }

    public static float streakMultiplier(SparStore.MentorBond bond) {
        if (bond == null) {
            return 1.0f;
        }
        int days = Math.max(0, Math.min(14, bond.streakCurrent));
        return Math.min(1.25f, 1.0f + days * 0.02f);
    }

    public static float styleMultiplier(SparPlayerRuntime rt) {
        String id = styleId(rt);
        return switch (id) {
            case "melee" -> 1.10f;
            case "ki" -> 1.10f;
            case "beam" -> 1.15f;
            case "balanced" -> 1.20f;
            case "guardian" -> 1.08f;
            case "speed" -> 1.05f;
            default -> 1.0f;
        };
    }

    public static String styleId(SparPlayerRuntime rt) {
        if (rt == null) {
            return "none";
        }
        double melee = rt.styleMelee;
        double ki = rt.styleKi;
        double beam = rt.styleBeam;
        double blocks = rt.styleBlock;
        double move = rt.styleMove;
        double total = melee + ki;
        if (total < 200.0) {
            return "none";
        }
        double meleeRatio = melee / Math.max(1.0, total);
        double kiRatio = ki / Math.max(1.0, total);
        double beamRatio = beam / Math.max(1.0, Math.max(ki, total));
        double blockRatio = blocks / Math.max(1.0, total + blocks);
        if (beamRatio >= 0.35 && kiRatio >= 0.50) {
            return "beam";
        }
        if (blockRatio >= 0.35) {
            return "guardian";
        }
        if (meleeRatio >= 0.80) {
            return "melee";
        }
        if (kiRatio >= 0.80) {
            return "ki";
        }
        if (meleeRatio >= 0.35 && kiRatio >= 0.35) {
            return "balanced";
        }
        if (move > total) {
            return "speed";
        }
        return "none";
    }

    public static float kiEfficiency(String kiKind) {
        if (kiKind == null) {
            return 1.0f;
        }
        return switch (kiKind.toLowerCase()) {
            case "beam" -> 1.15f;
            case "charge" -> 1.10f;
            case "basic" -> 1.00f;
            case "blast" -> 1.05f;
            default -> 1.00f;
        };
    }

    public static TrainingValues liveValues(ServerPlayer player) {
        try {
            StatsData data = DmzProgression.stats(player);
            if (data == null) {
                return null;
            }
            TrainingValues v = new TrainingValues();
            v.bp = DmzRewards.battlePower(player);
            v.release = DmzRewards.powerReleasePercent(player);
            try {
                v.gravity = GravityLogic.getNetGravity(player);
            } catch (Throwable ignored) {
                v.gravity = 1.0;
            }
            try {
                v.weight = GravityLogic.getEffectiveWeight(player);
            } catch (Throwable ignored) {
                v.weight = 0.0;
            }
            v.prestige = DmzProgression.prestige(player);
            return v;
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isPerfect(TrainingValues a, TrainingValues b) {
        if (a == null || b == null) {
            return false;
        }
        if (percentDiff(a.bp, b.bp) > PERFECT_BP_DIFFERENCE) {
            return false;
        }
        if (a.release < PERFECT_RELEASE_MIN || a.release > PERFECT_RELEASE_MAX) {
            return false;
        }
        if (b.release < PERFECT_RELEASE_MIN || b.release > PERFECT_RELEASE_MAX) {
            return false;
        }
        if (Math.abs(a.gravity - b.gravity) > 0.01) {
            return false;
        }
        return !(Math.abs(a.weight - b.weight) > 1.0);
    }

    private static double percentDiff(double a, double b) {
        double max = Math.max(Math.abs(a), Math.abs(b));
        if (!(max > 0.0)) {
            return 0.0;
        }
        return Math.abs(a - b) / max;
    }

    public static float buildTotalMultiplier(
            ServerPlayer player,
            ServerPlayer partner,
            SparPlayerRuntime rt,
            TrainingValues a,
            TrainingValues b
    ) {
        double trainingBp = Math.min(a.bp, b.bp);
        if (!(trainingBp > 0.0)) {
            trainingBp = Math.max(a.bp, Math.max(b.bp, 1.0));
        }
        double avgRelease = (a.release + b.release) / 2.0;
        double avgGravity = (a.gravity + b.gravity) / 2.0;
        double avgWeight = (a.weight + b.weight) / 2.0;
        boolean perfect = isPerfect(a, b);
        if (perfect && rt != null) {
            rt.sessionPerfect = true;
        }
        float bpMult = battlePowerMultiplier(trainingBp);
        float total = bpMult
                * rivalMultiplier(a.bp, b.bp)
                * releaseMultiplier(avgRelease)
                * gravityMultiplier(avgGravity)
                * weightMultiplier(avgWeight)
                * prestigeMultiplier(a.prestige)
                * momentumMultiplier(rt)
                * sessionBonusMultiplier(rt)
                * streakMultiplier(SparStore.get().bond(player.m_20148_()))
                * styleMultiplier(rt)
                * (perfect ? PERFECT_TRAINING_MULTIPLIER : 1.0f);
        return total;
    }

    public static float maxTpForAction(float bpMult) {
        float scaled = (float) Math.floor(BASE_TP_PER_HIT * Math.max(1.0f, bpMult) * MAX_TP_PER_ACTION_BP_SCALE);
        if (!(scaled >= 1.0f)) {
            scaled = MAX_TP_PER_ACTION;
        }
        return Math.min(MAX_TP_PER_ACTION, Math.max(BASE_TP_PER_HIT, scaled));
    }

    public static int awardCombatTp(
            ServerPlayer player,
            ServerPlayer partner,
            SparPlayerRuntime rt,
            float baseAmount,
            String hitKind
    ) {
        if (player == null || partner == null || rt == null || !rt.active) {
            return 0;
        }
        if (RivalChallengeManager.get().isInChallenge(player.m_20148_())
                || RivalChallengeManager.get().isInChallenge(partner.m_20148_())) {
            return 0;
        }
        float base = Math.min(MAX_BASE_TP_PER_HIT, Math.max(0.0f, baseAmount));
        if (!(base > 0.0f)) {
            return 0;
        }
        TrainingValues a = liveValues(player);
        TrainingValues b = liveValues(partner);
        if (a == null || b == null) {
            return 0;
        }
        float totalMult = buildTotalMultiplier(player, partner, rt, a, b);
        float amount = (float) Math.floor(base * totalMult * GLOBAL_TP_GAIN_MULT);
        boolean withMentor = SparringSystem.isSparringWithOwnMentor(player, partner);
        if (withMentor) {
            amount = (float) Math.floor(amount * (1.0f + MENTOR_SPAR_BONUS_PCT));
        }
        float bpMult = battlePowerMultiplier(Math.min(a.bp, b.bp) > 0 ? Math.min(a.bp, b.bp) : Math.max(a.bp, b.bp));
        float actionCap = maxTpForAction(bpMult) * GLOBAL_TP_GAIN_MULT;
        if (withMentor) {
            actionCap *= (1.0f + MENTOR_SPAR_BONUS_PCT);
        }
        if (amount > actionCap) {
            amount = actionCap;
        }
        int award = Math.max(0, Math.round(amount));
        if (award <= 0) {
            return 0;
        }
        if (!DmzRewards.awardTp(player, award, null, false, null)) {
            return 0;
        }
        rt.sessionTp += award;
        queueTpMessage(player, rt, award, hitKind);
        SparringSystem.shareTpWithMentor(player, award);
        return award;
    }

    public static void awardDamageTp(
            ServerPlayer attacker,
            ServerPlayer victim,
            SparPlayerRuntime atkRt,
            double damage,
            boolean ki,
            String kiKind
    ) {
        if (atkRt == null || !atkRt.active) {
            return;
        }
        if (!(damage > 0.0)) {
            return;
        }
        atkRt.sessionDmg += damage;
        if (ki) {
            atkRt.sessionKi += damage;
            atkRt.styleKi += damage;
            if ("beam".equalsIgnoreCase(kiKind)) {
                atkRt.styleBeam += damage;
            }
        } else {
            atkRt.sessionMelee += damage;
            atkRt.styleMelee += damage;
        }
        if (isBlocking(victim)) {
            SparringSystem.breakCombo(attacker, atkRt, "attack blocked");
            return;
        }
        SparringSystem.registerCombatHit(atkRt);
        long now = System.currentTimeMillis();
        if (now <= atkRt.heavyMotionUntil) {
            atkRt.sessionKb++;
            awardCombatTp(attacker, victim, atkRt, KNOCKBACK_RECOVERY_TP, "melee");
            atkRt.heavyMotionUntil = 0L;
        }
        float eff = ki ? kiEfficiency(kiKind) : MELEE_EFF;
        float base = BASE_TP_PER_HIT * damageQuality(damage) * eff;
        awardCombatTp(attacker, victim, atkRt, base, ki ? "ki" : "melee");
    }

    public static boolean isBlocking(ServerPlayer player) {
        try {
            StatsData data = DmzProgression.stats(player);
            Status status = data == null ? null : data.getStatus();
            return status != null && status.isBlocking();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void queueTpMessage(ServerPlayer player, SparPlayerRuntime rt, int amount, String hitKind) {
        if (rt == null || amount <= 0) {
            return;
        }
        rt.tpPending += amount;
        String kind = hitKind == null ? "" : hitKind.toLowerCase();
        if ("melee".equals(kind)) {
            rt.tpPendingMelee += amount;
        } else if ("beam".equals(kind) || "clash".equals(kind) || "beam-clash".equals(kind)) {
            rt.tpPendingClash += amount;
        } else if (!kind.isEmpty()) {
            rt.tpPendingKi += amount;
        }
        long now = System.currentTimeMillis();
        if (now >= rt.tpMsgNext) {
            rt.tpMsgNext = now + 2000L;
            flushTpMessage(player, rt);
        }
    }

    public static void flushTpMessage(ServerPlayer player, SparPlayerRuntime rt) {
        if (rt == null || rt.tpPending <= 0) {
            return;
        }
        int pending = (int) Math.floor(rt.tpPending);
        rt.tpPending = 0;
        rt.tpPendingMelee = 0;
        rt.tpPendingKi = 0;
        rt.tpPendingClash = 0;
        String style = styleId(rt);
        String label = switch (style) {
            case "melee" -> "Melee Specialist";
            case "ki" -> "Ki Specialist";
            case "beam" -> "Beam Specialist";
            case "balanced" -> "Balanced";
            case "guardian" -> "Guardian";
            default -> "Combat";
        };
        DmzRewards.msg(player, "§6[Sparring] §a+" + DmzRewards.formatWhole(pending) + " TP §8(" + label + ")");
    }

    public static void updateMomentum(SparPlayerRuntime rt) {
        if (rt == null) {
            return;
        }
        int tier = 0;
        for (int i = 0; i < MOMENTUM_THRESHOLDS.length; i++) {
            if (rt.combo >= MOMENTUM_THRESHOLDS[i]) {
                tier = i + 1;
            }
        }
        if (tier > rt.momentumTier) {
            rt.momentumTier = tier;
            rt.momentumUntil = System.currentTimeMillis() + 10_000L;
            rt.sessionMaxMom = Math.max(rt.sessionMaxMom, tier);
        }
    }

    public static final class TrainingValues {
        public double bp;
        public double release;
        public double gravity;
        public double weight;
        public int prestige;
    }
}
