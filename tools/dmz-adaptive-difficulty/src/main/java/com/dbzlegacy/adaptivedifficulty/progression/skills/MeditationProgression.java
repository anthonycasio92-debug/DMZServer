package com.dbzlegacy.adaptivedifficulty.progression.skills;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.skills.Skills;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Port of Meditation new.js + ChangeBiomeMED trigger 41.
 */
public final class MeditationProgression {
    private static final String SKILL = "meditation";
    private static final int MAX_LEVEL = 10;
    private static final long TRIAL_DURATION_MS = 15L * 60L * 1000L;
    private static final long FOCUS_WINDOW_MS = 10_000L;
    private static final int[] REQUIRED_SECONDS = {
            0, 0, 30, 60, 120, 240, 360, 480, 600, 720, 900
    };

    private static final Trial[] TRIALS = {
            new Trial("minecraft:plains", "Plains", "plains"),
            new Trial("minecraft:desert", "Desert", "desert"),
            new Trial("minecraft:snowy_plains", "Snowy Plains", "snowy_plains"),
            new Trial("minecraft:nether_wastes", "Nether Wastes", "nether_wastes"),
            new Trial("minecraft:warped_forest", "Warped Forest", "warped_forest"),
            new Trial("minecraft:soul_sand_valley", "Soul Sand Valley", "soul_sand_valley"),
            new Trial("dragonminez:ajissa_plains", "Ajissa Plains", "ajissa_plains"),
            new Trial("dragonminez:namekian_rivers", "Namekian Rivers", "namekian_rivers"),
            new Trial("dragonminez:sacredkai_hills", "Sacred Kai Hills", "sacredkai_hills"),
            new Trial("dragonminez:hyperbolic_time_chamber", "Hyperbolic Time Chamber", "htc")
    };

    private static final AtomicInteger GLOBAL_INDEX = new AtomicInteger(0);
    private static final AtomicLong GLOBAL_END = new AtomicLong(0L);
    private static final AtomicLong CYCLE_LOCK = new AtomicLong(0L);

    private MeditationProgression() {}

    public static void pulse(ServerPlayer player, long nowMs) {
        if (!ProgressionConfig.meditation() || player == null) {
            return;
        }
        if (nowMs < ProgressionData.tempGetLong(player, "med2_next_check", 0L)) {
            return;
        }
        ProgressionData.tempPut(player, "med2_next_check", nowMs + 1000L);
        try {
            Trial trial = currentTrial(nowMs);
            if (trial == null) {
                return;
            }
            StatsData data = DmzProgression.stats(player);
            Skills skills = data == null ? null : data.getSkills();
            if (skills == null) {
                return;
            }
            int level = DmzSkillUtil.level(skills, SKILL);
            handleLevelDrop(player, level);
            if (level < 1) {
                return;
            }
            int max = DmzSkillUtil.maxLevel(skills, SKILL, MAX_LEVEL);
            if (level >= max) {
                return;
            }
            String biome = biomeId(player);
            boolean charging = isCharging(data);
            boolean wrong = !biomeMatches(biome, trial.id);
            if (wrong) {
                resetPosition(player);
                resetFocus(player);
                return;
            }
            float energy = currentEnergy(data);
            float maxEnergy = data.getMaxEnergy();
            if (maxEnergy <= 0) {
                return;
            }
            if (energy >= maxEnergy * 0.995f) {
                resetFocus(player);
                return;
            }
            if (!passesFocus(player, charging, nowMs)) {
                return;
            }
            if (!passesCondition(player, trial, energy, maxEnergy, nowMs)) {
                return;
            }
            addProgress(player, skills, level, max);
        } catch (Throwable ignored) {
        }
    }

    public static void onHurt(ServerPlayer player) {
        if (!ProgressionConfig.meditation() || player == null) {
            return;
        }
        ProgressionData.tempPut(player, "med2_last_damage", System.currentTimeMillis());
    }

    public static void onLogout(ServerPlayer player) {
        // temp cleared by ProgressionSystem
    }

    /** Trigger 41 — advance global meditation trial. */
    public static String advanceTrial(ServerPlayer actor) {
        if (!ProgressionConfig.meditation()) {
            return "§cMeditation progression is disabled.";
        }
        long now = System.currentTimeMillis();
        if (now < CYCLE_LOCK.get()) {
            return "§7Trial cycle is on cooldown.";
        }
        CYCLE_LOCK.set(now + 5000L);
        int next = (GLOBAL_INDEX.get() + 1) % TRIALS.length;
        GLOBAL_INDEX.set(next);
        GLOBAL_END.set(now + TRIAL_DURATION_MS);
        Trial trial = TRIALS[next];
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            String msg = "§d[Meditation] §fTrial biome: §b" + trial.name
                    + " §7(" + trial.id + ")";
            for (ServerPlayer p : server.m_6846_().m_11314_()) {
                DmzRewards.msg(p, msg);
            }
        }
        SystemTelemetry.log("progression", "meditation_trial", actor, null,
                Map.of("biome", trial.id, "name", trial.name));
        return "§aAdvanced meditation trial to §f" + trial.name + "§a.";
    }

    public static String statusLine() {
        Trial t = currentTrial(System.currentTimeMillis());
        if (t == null) {
            return "§7No active meditation trial.";
        }
        long rem = Math.max(0L, GLOBAL_END.get() - System.currentTimeMillis());
        return "§7Trial §f" + t.name + " §8(" + t.id + ") §7" + (rem / 60000L) + "m left";
    }

    /** Seconds of restore meditation required to reach {@code nextLevel} (2–10). */
    public static int requiredSecondsForLevel(int nextLevel) {
        if (nextLevel < 0 || nextLevel >= REQUIRED_SECONDS.length) {
            return 0;
        }
        return REQUIRED_SECONDS[nextLevel];
    }

    public static String currentTrialName() {
        Trial t = currentTrial(System.currentTimeMillis());
        return t == null ? "" : t.name;
    }

    public static long trialRemainingMs() {
        currentTrial(System.currentTimeMillis());
        return Math.max(0L, GLOBAL_END.get() - System.currentTimeMillis());
    }

    private static Trial currentTrial(long now) {
        if (GLOBAL_END.get() <= now) {
            int next = (GLOBAL_INDEX.get() + 1) % TRIALS.length;
            GLOBAL_INDEX.set(next);
            GLOBAL_END.set(now + TRIAL_DURATION_MS);
        }
        int idx = Math.floorMod(GLOBAL_INDEX.get(), TRIALS.length);
        return TRIALS[idx];
    }

    private static void addProgress(ServerPlayer player, Skills skills, int level, int max) {
        int next = level + 1;
        if (next >= REQUIRED_SECONDS.length || REQUIRED_SECONDS[next] <= 0 || next > max) {
            return;
        }
        String key = "meditation_restore_progress_to_level_" + next;
        long progress = ProgressionData.storedGetLong(player, key, 0L) + 1L;
        int need = REQUIRED_SECONDS[next];
        if (progress >= need) {
            DmzSkillUtil.setLevel(skills, SKILL, next);
            ProgressionData.storedPut(player, key, need);
            ProgressionData.storedPut(player, "med2_last_level", next);
            DmzSkillUtil.sync(player);
            DmzRewards.msg(player, "§d[Meditation] Increased to level " + next + ".");
            SystemTelemetry.log("progression", "meditation_level", player, null,
                    Map.of("level", next));
        } else {
            ProgressionData.storedPut(player, key, progress);
        }
    }

    private static void handleLevelDrop(ServerPlayer player, int current) {
        long last = ProgressionData.storedGetLong(player, "med2_last_level", current);
        if (current < last) {
            for (int level = 2; level <= MAX_LEVEL; level++) {
                ProgressionData.storedRemove(player, "meditation_restore_progress_to_level_" + level);
            }
        }
        ProgressionData.storedPut(player, "med2_last_level", current);
    }

    private static boolean passesFocus(ServerPlayer player, boolean charging, long now) {
        boolean was = "1".equals(ProgressionData.tempGet(player, "med2_focus_was_charging", "0"));
        if (charging) {
            if (!was) {
                ProgressionData.tempPut(player, "med2_focus_was_charging", "1");
                ProgressionData.tempPut(player, "med2_focus_started", now);
            }
            long started = ProgressionData.tempGetLong(player, "med2_focus_started", now);
            return now - started >= FOCUS_WINDOW_MS;
        }
        resetFocus(player);
        return false;
    }

    private static void resetFocus(ServerPlayer player) {
        ProgressionData.tempRemove(player, "med2_focus_was_charging");
        ProgressionData.tempRemove(player, "med2_focus_started");
        ProgressionData.tempRemove(player, "med2_focus_wait_release");
    }

    private static boolean passesCondition(
            ServerPlayer player, Trial trial, float energy, float maxEnergy, long now
    ) {
        double percent = maxEnergy > 0 ? energy / maxEnergy : 1.0;
        double y = player.m_20186_();
        return switch (trial.type) {
            case "plains", "ajissa_plains" -> player.m_6144_() && percent <= 0.50;
            case "desert" -> isDay(player) && percent <= 0.40;
            case "snowy_plains" -> player.m_6144_() && y >= 80;
            case "nether_wastes" -> y <= 64 && percent <= 0.40;
            case "warped_forest" -> player.m_6144_() && insideRadius(player, 1.5, trial.type);
            case "soul_sand_valley" ->
                    avoidedDamage(player, 12_000L, now) && remainedStill(player, 1.0, 12_000L, now);
            case "namekian_rivers" -> percent <= 0.50;
            case "sacredkai_hills" -> y >= 90 && avoidedDamage(player, 10_000L, now);
            case "htc" -> percent <= 0.30 && remainedStill(player, 1.5, 10_000L, now);
            default -> false;
        };
    }

    private static boolean isDay(ServerPlayer player) {
        try {
            ServerLevel level = player.m_284548_();
            return level != null && level.m_46461_();
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static boolean avoidedDamage(ServerPlayer player, long requiredMs, long now) {
        long last = ProgressionData.tempGetLong(player, "med2_last_damage", 0L);
        return last <= 0L || now - last >= requiredMs;
    }

    private static boolean remainedStill(ServerPlayer player, double radius, long requiredMs, long now) {
        double x = player.m_20185_();
        double z = player.m_20189_();
        if (!ProgressionData.tempHas(player, "med2_still_x")) {
            ProgressionData.tempPut(player, "med2_still_x", x);
            ProgressionData.tempPut(player, "med2_still_z", z);
            ProgressionData.tempPut(player, "med2_still_started", now);
            return false;
        }
        double ox = ProgressionData.tempGetDouble(player, "med2_still_x", x);
        double oz = ProgressionData.tempGetDouble(player, "med2_still_z", z);
        double dx = x - ox;
        double dz = z - oz;
        if (Math.sqrt(dx * dx + dz * dz) > radius) {
            ProgressionData.tempPut(player, "med2_still_x", x);
            ProgressionData.tempPut(player, "med2_still_z", z);
            ProgressionData.tempPut(player, "med2_still_started", now);
            return false;
        }
        long started = ProgressionData.tempGetLong(player, "med2_still_started", now);
        return now - started >= requiredMs;
    }

    private static boolean insideRadius(ServerPlayer player, double radius, String trialType) {
        String lastTrial = ProgressionData.tempGet(player, "med2_radius_trial", "");
        double x = player.m_20185_();
        double z = player.m_20189_();
        if (!trialType.equals(lastTrial) || !ProgressionData.tempHas(player, "med2_radius_x")) {
            ProgressionData.tempPut(player, "med2_radius_x", x);
            ProgressionData.tempPut(player, "med2_radius_z", z);
            ProgressionData.tempPut(player, "med2_radius_trial", trialType);
            return true;
        }
        double ox = ProgressionData.tempGetDouble(player, "med2_radius_x", x);
        double oz = ProgressionData.tempGetDouble(player, "med2_radius_z", z);
        double dx = x - ox;
        double dz = z - oz;
        return Math.sqrt(dx * dx + dz * dz) <= radius;
    }

    private static void resetPosition(ServerPlayer player) {
        ProgressionData.tempRemove(player, "med2_still_x");
        ProgressionData.tempRemove(player, "med2_still_z");
        ProgressionData.tempRemove(player, "med2_still_started");
        ProgressionData.tempRemove(player, "med2_radius_x");
        ProgressionData.tempRemove(player, "med2_radius_z");
        ProgressionData.tempRemove(player, "med2_radius_trial");
    }

    private static boolean isCharging(StatsData data) {
        try {
            Status status = data.getStatus();
            return status != null && status.isChargingKi();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static float currentEnergy(StatsData data) {
        try {
            Resources resources = data.getResources();
            return resources == null ? -1f : resources.getCurrentEnergy();
        } catch (Throwable ignored) {
            return -1f;
        }
    }

    private static String biomeId(ServerPlayer player) {
        try {
            Holder<Biome> holder = player.m_9236_().m_204166_(player.m_20183_());
            return holder.m_203543_()
                    .map(ResourceKey::m_135782_)
                    .map(ResourceLocation::toString)
                    .orElse("");
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static boolean biomeMatches(String found, String required) {
        if (found == null || required == null) {
            return false;
        }
        String a = found.toLowerCase().replace("minecraft:", "");
        String b = required.toLowerCase().replace("minecraft:", "");
        return a.equals(b) || found.equalsIgnoreCase(required);
    }

    private record Trial(String id, String name, String type) {}
}
