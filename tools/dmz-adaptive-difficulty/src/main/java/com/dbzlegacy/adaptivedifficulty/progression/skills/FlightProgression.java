package com.dbzlegacy.adaptivedifficulty.progression.skills;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.skills.Skills;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;

/**
 * Port of Flight.js 1.0.0 — organic fly leveling + Viltrumite max grant + flight suppression.
 */
public final class FlightProgression {
    private static final String FLY = "fly";
    private static final int[] LEVEL_SECONDS = {
            0, 0, 60, 300, 600, 1800, 3600, 5400, 7200, 9000, 10800
    };

    /** Seconds of flight training required to reach {@code nextLevel} (2–10). */
    public static int requiredSecondsForLevel(int nextLevel) {
        if (nextLevel < 0 || nextLevel >= LEVEL_SECONDS.length) {
            return 0;
        }
        return LEVEL_SECONDS[nextLevel];
    }
    private static final double SEARCH_H = 0.08;
    private static final double SEARCH_UP = 0.07;
    private static final double SEARCH_DOWN = 0.08;
    private static final double SEARCH_TOTAL = 0.10;
    private static final float SEARCH_ABILITY = 0.015f;
    private static final double COMBAT_H = 0.28;
    private static final double COMBAT_UP = 0.22;
    private static final double COMBAT_DOWN = 0.24;
    private static final double COMBAT_TOTAL = 0.32;
    private static final double LEVEL_BONUS = 0.35;
    private static final float DEFAULT_ABILITY = 0.05f;

    private static final Map<UUID, Float> SAVED_ABILITY = new ConcurrentHashMap<>();

    private FlightProgression() {}

    public static void pulse(ServerPlayer player, long nowMs) {
        if (!ProgressionConfig.flight() || player == null) {
            return;
        }
        try {
            suppressTick(player);
        } catch (Throwable ignored) {
        }
        if (nowMs < ProgressionData.tempGetLong(player, "flight_training_next_check", 0L)) {
            return;
        }
        ProgressionData.tempPut(player, "flight_training_next_check", nowMs + 1000L);
        try {
            flyTraining(player);
        } catch (Throwable ignored) {
        }
        try {
            viltrumiteTick(player);
        } catch (Throwable ignored) {
        }
    }

    public static void onLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        restoreAbility(player);
        SAVED_ABILITY.remove(player.m_20148_());
    }

    private static void flyTraining(ServerPlayer player) {
        StatsData data = DmzProgression.stats(player);
        Skills skills = data == null ? null : data.getSkills();
        if (skills == null) {
            return;
        }
        DmzSkillUtil.ensureRegistered(skills, FLY, 10);
        int current = DmzSkillUtil.level(skills, FLY);
        int max = DmzSkillUtil.maxLevel(skills, FLY, 10);
        if (current < 1 || current >= max) {
            return;
        }
        int next = current + 1;
        if (next >= LEVEL_SECONDS.length || LEVEL_SECONDS[next] <= 0) {
            return;
        }
        boolean flying = DmzSkillUtil.isActive(skills, FLY) && !player.m_20096_();
        if (!flying) {
            return;
        }
        String progressKey = "fly_training_progress_to_level_" + next;
        String activeKey = "fly_training_active_next_level";
        long active = ProgressionData.storedGetLong(player, activeKey, 0L);
        if (active != next) {
            ProgressionData.storedPut(player, activeKey, next);
            if (!ProgressionData.storedHas(player, progressKey)) {
                ProgressionData.storedPut(player, progressKey, 0L);
            }
        }
        long progress = ProgressionData.storedGetLong(player, progressKey, 0L);
        int required = LEVEL_SECONDS[next];
        if (progress > required * 100L) {
            progress = progress / 1000L;
        }
        progress += 1L;
        if (progress >= required) {
            DmzSkillUtil.setLevel(skills, FLY, next);
            ProgressionData.storedPut(player, progressKey, required);
            DmzSkillUtil.sync(player);
            DmzRewards.msg(player, "§b[Flight] Flight increased to level " + next + ".");
            SystemTelemetry.log("progression", "flight_level", player, null,
                    Map.of("level", next));
            if (next >= max) {
                DmzRewards.msg(player, "§6[Flight] Flight is now maxed.");
            } else {
                ProgressionData.storedPut(player, activeKey, next + 1);
                String nk = "fly_training_progress_to_level_" + (next + 1);
                if (!ProgressionData.storedHas(player, nk)) {
                    ProgressionData.storedPut(player, nk, 0L);
                }
            }
        } else {
            ProgressionData.storedPut(player, progressKey, progress);
        }
    }

    private static void viltrumiteTick(ServerPlayer player) {
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return;
        }
        Character ch = data.getCharacter();
        Skills skills = data.getSkills();
        if (ch == null || skills == null) {
            return;
        }
        String race = String.valueOf(ch.getRace()).toLowerCase();
        int current = DmzSkillUtil.level(skills, FLY);
        if ("viltrumite".equals(race)) {
            int max = DmzSkillUtil.maxLevel(skills, FLY, 10);
            if (current < max) {
                DmzSkillUtil.setLevel(skills, FLY, max);
                ProgressionData.tempPut(player, "viltrumite_flight_granted", "1");
                DmzSkillUtil.sync(player);
            }
        } else if (ProgressionData.tempHas(player, "viltrumite_flight_granted")) {
            if (current > 0) {
                DmzSkillUtil.setLevel(skills, FLY, 0);
                DmzSkillUtil.sync(player);
            }
            ProgressionData.tempRemove(player, "viltrumite_flight_granted");
        }
    }

    private static void suppressTick(ServerPlayer player) {
        if (player.m_150110_().f_35937_) { // instabuild / creative
            restoreAbility(player);
            return;
        }
        StatsData data = DmzProgression.stats(player);
        Skills skills = data == null ? null : data.getSkills();
        Status status = null;
        try {
            status = data == null ? null : data.getStatus();
        } catch (Throwable ignored) {
        }
        if (!DmzSkillUtil.isActive(skills, FLY)) {
            restoreAbility(player);
            return;
        }
        if (player.m_20096_()) {
            restoreAbility(player);
            return;
        }
        int mode = -1;
        try {
            if (status != null) {
                mode = status.getFlightMode();
            }
        } catch (Throwable ignored) {
        }
        int flyLevel = DmzSkillUtil.level(skills, FLY);
        double scale = levelScale(flyLevel);
        if (mode == 1) {
            restoreAbility(player);
            clampMotion(player, COMBAT_H * scale, COMBAT_UP * scale, COMBAT_DOWN * scale, COMBAT_TOTAL * scale);
            return;
        }
        setAbilityTracked(player, SEARCH_ABILITY);
        clampMotion(player, SEARCH_H * scale, SEARCH_UP * scale, SEARCH_DOWN * scale, SEARCH_TOTAL * scale);
    }

    private static double levelScale(int flyLevel) {
        if (flyLevel <= 1) {
            return 1.0;
        }
        int level = Math.min(10, flyLevel);
        double progress = (level - 1) / 9.0;
        return 1.0 + progress * LEVEL_BONUS;
    }

    private static void setAbilityTracked(ServerPlayer player, float speed) {
        Abilities ab = player.m_150110_();
        float current = ab.m_35947_(); // flyingSpeed
        if (Math.abs(current - speed) <= 0.0001f) {
            return;
        }
        SAVED_ABILITY.putIfAbsent(player.m_20148_(), current);
        ab.m_35948_(speed);
        player.m_6885_(); // onUpdateAbilities
    }

    private static void restoreAbility(ServerPlayer player) {
        Float saved = SAVED_ABILITY.remove(player.m_20148_());
        if (saved == null) {
            return;
        }
        try {
            player.m_150110_().m_35948_(saved);
            player.m_6885_();
        } catch (Throwable ignored) {
            try {
                player.m_150110_().m_35948_(DEFAULT_ABILITY);
                player.m_6885_();
            } catch (Throwable ignored2) {
            }
        }
    }

    private static void clampMotion(ServerPlayer player, double hMax, double upMax, double downMax, double totalMax) {
        if (player.m_20096_()) {
            return;
        }
        Vec3 motion = player.m_20184_();
        double x = motion.f_82479_;
        double y = motion.f_82480_;
        double z = motion.f_82481_;
        double speedNow = Math.sqrt(x * x + y * y + z * z);
        if (speedNow < 0.0015) {
            return;
        }
        boolean changed = false;
        double horizontal = Math.sqrt(x * x + z * z);
        if (horizontal > hMax && horizontal > 0) {
            double hScale = hMax / horizontal;
            x *= hScale;
            z *= hScale;
            changed = true;
        }
        if (y > upMax) {
            y = upMax;
            changed = true;
        } else if (y < -downMax) {
            y = -downMax;
            changed = true;
        }
        double total = Math.sqrt(x * x + y * y + z * z);
        if (total > totalMax && total > 0) {
            double tScale = totalMax / total;
            x *= tScale;
            y *= tScale;
            z *= tScale;
            changed = true;
        }
        if (changed) {
            player.m_20334_(x, y, z);
            player.f_19812_ = true; // hurtMarked
        }
    }
}
