package com.dbzlegacy.adaptivedifficulty.progression.skills;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.skills.Skills;
import com.dragonminez.server.util.GravityLogic;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * Port of Potential.js — PvP hit/block progress into {@code potentialunlock}.
 */
public final class PotentialProgression {
    private static final String SKILL = "potentialunlock";
    private static final int HARD_MAX = 30;
    private static final int NATURAL_CAP = 10;
    private static final int MAX_SAME_STREAK = 5;
    private static final long DUP_WINDOW_MS = 500L;
    private static final int MAX_POINTS_PER_HIT = 30;
    private static final double MAX_G = 1000.0;
    private static final double MAX_W = 1000.0;
    private static final double MAX_G_MULT = 5.0;
    private static final double MAX_W_MULT = 2.0;
    private static final double PRESTIGE_PER = 0.10;
    private static final int MENTOR_TP = 50;
    private static final int TP_PER_LEVEL_DIFF = 500;
    private static final double MIN_MOVE = 2.0;
    private static final long MOVE_VALID_MS = 5000L;

    private PotentialProgression() {}

    public static void onPlayerHurt(
            ServerPlayer victim,
            ServerPlayer attacker,
            DamageSource source
    ) {
        if (!ProgressionConfig.potential() || victim == null || attacker == null) {
            return;
        }
        try {
            // Attacker gains from hitting
            String atkMethod = "physical_hit";
            int atkPoints = 3;
            Entity immediate = source == null ? null : source.m_7640_(); // getDirectEntity
            if (immediate instanceof AbstractKiProjectile
                    || (immediate instanceof Projectile p && isKiLike(p))) {
                atkMethod = "ki_attack";
                atkPoints = 3;
            }
            apply(attacker, victim, atkMethod, atkPoints);

            // Victim gains from getting hit / blocking
            String defMethod = "getting_hit";
            int defPoints = 1;
            try {
                StatsData data = DmzProgression.stats(victim);
                Status status = data == null ? null : data.getStatus();
                if (status != null && status.isBlocking()) {
                    defMethod = "blocking";
                    defPoints = 2;
                }
            } catch (Throwable ignored) {
            }
            apply(victim, attacker, defMethod, defPoints);
        } catch (Throwable ignored) {
        }
    }

    private static boolean isKiLike(Projectile projectile) {
        try {
            return projectile.getClass().getName().toLowerCase().contains("ki");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void apply(
            ServerPlayer player,
            ServerPlayer other,
            String method,
            int basePoints
    ) {
        StatsData playerData = DmzProgression.stats(player);
        StatsData otherData = DmzProgression.stats(other);
        if (playerData == null || otherData == null) {
            return;
        }
        Skills skills = playerData.getSkills();
        if (skills == null) {
            return;
        }
        // Soft-cap at 10 until Guru raises them past it; hard max 30 afterward.
        DmzSkillUtil.ensureRegistered(skills, SKILL, HARD_MAX);
        int current = DmzSkillUtil.level(skills, SKILL);
        resetIfNeeded(player, current);
        if (current >= HARD_MAX) {
            return;
        }
        // Natural soft-stop: at exactly 10, no more points until unlocked to 11+.
        if (current == NATURAL_CAP) {
            tellGuru(player);
            return;
        }
        if (!hasMoved(player)) {
            return;
        }
        // Script: duplicate window is checked before method-streak so rapid
        // same-target hits do not burn the 5-streak rotation.
        if (isDuplicate(player, method, other.m_19879_())) {
            return;
        }
        if (!allowMethod(player, method)) {
            return;
        }
        int points = calculatePoints(player, basePoints);
        int next = current + 1;
        int required = next * 100;
        String progressKey = "potentialunlock_points_to_level_" + next;
        int progress = (int) ProgressionData.storedGetLong(player, progressKey, 0L) + points;
        if (progress > required) {
            progress = required;
        }
        giveMentorTrainingTp(player, playerData, other, otherData);
        if (progress < required) {
            ProgressionData.storedPut(player, progressKey, progress);
            return;
        }
        ProgressionData.storedPut(player, progressKey, required);
        DmzSkillUtil.setLevel(skills, SKILL, next);
        int confirmed = DmzSkillUtil.level(skills, SKILL);
        if (confirmed < next) {
            DmzRewards.msg(player, "§c[Potential Unlock] Level-up failed.");
            return;
        }
        ProgressionData.storedPut(player, "potentialunlock_last_known_level", confirmed);
        DmzSkillUtil.sync(player);
        DmzRewards.msg(player, "§5[Potential Unlock] Increased to level " + confirmed + ".");
        giveMentorLevelUpTp(player, playerData, other, otherData);
        if (confirmed == NATURAL_CAP) {
            tellGuru(player);
        }
        SystemTelemetry.log("progression", "potential_level", player, other,
                Map.of("level", confirmed, "method", method));
    }

    private static void resetIfNeeded(ServerPlayer player, int current) {
        long last = ProgressionData.storedGetLong(player, "potentialunlock_last_known_level", current);
        if (current < last) {
            for (int i = 1; i <= HARD_MAX; i++) {
                ProgressionData.storedRemove(player, "potentialunlock_points_to_level_" + i);
            }
            ProgressionData.storedRemove(player, "potentialunlock_last_method");
            ProgressionData.storedRemove(player, "potentialunlock_same_method_streak");
        }
        ProgressionData.storedPut(player, "potentialunlock_last_known_level", current);
    }

    private static boolean hasMoved(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long validUntil = ProgressionData.storedGetLong(player, "potential_movement_valid_until", 0L);
        if (now < validUntil) {
            return true;
        }
        double x = player.m_20185_();
        double z = player.m_20189_();
        if (!ProgressionData.storedHas(player, "potential_last_move_x")) {
            ProgressionData.storedPut(player, "potential_last_move_x", x);
            ProgressionData.storedPut(player, "potential_last_move_z", z);
            return false;
        }
        double ox = ProgressionData.storedGetDouble(player, "potential_last_move_x", x);
        double oz = ProgressionData.storedGetDouble(player, "potential_last_move_z", z);
        double dx = x - ox;
        double dz = z - oz;
        if (Math.sqrt(dx * dx + dz * dz) < MIN_MOVE) {
            return false;
        }
        ProgressionData.storedPut(player, "potential_last_move_x", x);
        ProgressionData.storedPut(player, "potential_last_move_z", z);
        ProgressionData.storedPut(player, "potential_movement_valid_until", now + MOVE_VALID_MS);
        return true;
    }

    private static boolean allowMethod(ServerPlayer player, String method) {
        String last = ProgressionData.storedGet(player, "potentialunlock_last_method", "");
        int streak = (int) ProgressionData.storedGetLong(player, "potentialunlock_same_method_streak", 0L);
        if (method.equals(last)) {
            streak++;
        } else {
            streak = 1;
        }
        ProgressionData.storedPut(player, "potentialunlock_last_method", method);
        ProgressionData.storedPut(player, "potentialunlock_same_method_streak", streak);
        return streak <= MAX_SAME_STREAK;
    }

    private static boolean isDuplicate(ServerPlayer player, String method, int entityId) {
        long now = System.currentTimeMillis();
        String key = "potentialunlock_duplicate_" + method + "_" + entityId;
        long last = ProgressionData.tempGetLong(player, key, 0L);
        if (last > 0L && now - last < DUP_WINDOW_MS) {
            return true;
        }
        ProgressionData.tempPut(player, key, now);
        return false;
    }

    private static int calculatePoints(ServerPlayer player, int basePoints) {
        double gMult = gravityMult(player);
        double wMult = weightMult(player);
        int prestige = Math.min(10, Math.max(0, DmzProgression.prestige(player)));
        double pMult = 1.0 + prestige * PRESTIGE_PER;
        int calculated = (int) Math.floor(basePoints * gMult * wMult * pMult);
        if (calculated < 1) {
            calculated = 1;
        }
        return Math.min(calculated, MAX_POINTS_PER_HIT);
    }

    private static double gravityMult(ServerPlayer player) {
        try {
            double gravity = GravityLogic.getNetGravity(player);
            if (!(gravity >= 1.0)) {
                gravity = 1.0;
            }
            double capped = Math.min(gravity, MAX_G);
            double progress = (capped - 1.0) / (MAX_G - 1.0);
            progress = Math.max(0.0, Math.min(1.0, progress));
            return 1.0 + progress * (MAX_G_MULT - 1.0);
        } catch (Throwable ignored) {
            return 1.0;
        }
    }

    private static double weightMult(ServerPlayer player) {
        try {
            double weight = GravityLogic.getEffectiveWeight(player);
            if (!(weight > 0.0)) {
                weight = GravityLogic.getTotalWeight(player);
            }
            double capped = Math.min(Math.max(0.0, weight), MAX_W);
            double progress = capped / MAX_W;
            return 1.0 + progress * (MAX_W_MULT - 1.0);
        } catch (Throwable ignored) {
            return 1.0;
        }
    }

    private static void giveMentorTrainingTp(
            ServerPlayer player, StatsData playerData, ServerPlayer other, StatsData otherData
    ) {
        try {
            int playerLevel = playerData.getLevel();
            int otherLevel = otherData.getLevel();
            if (otherLevel <= playerLevel) {
                return;
            }
            DmzRewards.awardTp(other, MENTOR_TP, "potential mentor", false, "§6[Potential Mentor] ");
            long now = System.currentTimeMillis();
            long next = ProgressionData.tempGetLong(other, "potential_mentor_tp_message_cooldown", 0L);
            if (now >= next) {
                ProgressionData.tempPut(other, "potential_mentor_tp_message_cooldown", now + 10_000L);
                DmzRewards.msg(other,
                        "§6[Potential Mentor] §eYou are gaining TP for helping train a lower-level player.");
            }
        } catch (Throwable ignored) {
        }
    }

    private static void giveMentorLevelUpTp(
            ServerPlayer player, StatsData playerData, ServerPlayer other, StatsData otherData
    ) {
        try {
            int playerLevel = playerData.getLevel();
            int otherLevel = otherData.getLevel();
            if (otherLevel <= playerLevel) {
                return;
            }
            int tp = (otherLevel - playerLevel) * TP_PER_LEVEL_DIFF;
            if (tp > 0) {
                DmzRewards.awardTp(other, tp, "potential mentor level-up", true, "§6[Potential Mentor] ");
            }
        } catch (Throwable ignored) {
        }
    }

    private static void tellGuru(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long next = ProgressionData.tempGetLong(player, "potential_guru_message_cooldown", 0L);
        if (now < next) {
            return;
        }
        ProgressionData.tempPut(player, "potential_guru_message_cooldown", now + 10_000L);
        DmzRewards.msg(player, "§6[Potential Unlock] §eYou have reached level 10.");
        DmzRewards.msg(player, "§eSpeak to Guru to unlock your hidden potential further.");
    }
}
