package com.dbzlegacy.adaptivedifficulty.progression.skills;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.util.LmChat;
import com.dbzlegacy.adaptivedifficulty.util.ScreenNotify;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.skills.Skills;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Port of {@code Meditation new.js} — global rotating trial biomes with
 * per-biome charge conditions, focus window, and wrong-biome warnings.
 */
public final class MeditationProgression {
    private static final String SKILL = "meditation";
    private static final int MAX_LEVEL = 10;
    /** Auto-rotate window — matches Meditation new.js CONFIG.TRIAL_DURATION_MS. */
    private static final long TRIAL_DURATION_MS = 15L * 60L * 1000L;
    /** Staff/manual cycle window — matches ChangeBiomeMED.js TRIAL_DURATION_MS (30 min). */
    private static final long MANUAL_TRIAL_DURATION_MS = 30L * 60L * 1000L;
    private static final long TRIAL_WARNING_MS = 5L * 60L * 1000L;
    private static final long ROTATION_LOCK_MS = 3_000L;
    private static final long FOCUS_WINDOW_MS = 10_000L;
    private static final long CONDITION_MESSAGE_COOLDOWN_MS = 8_000L;
    private static final long PROGRESS_SUBTITLE_COOLDOWN_MS = 5_000L;
    private static final long WRONG_BIOME_DELAY_MS = 10_000L;
    private static final long WRONG_BIOME_RELEASE_GRACE_MS = 2_500L;
    private static final long WRONG_BIOME_HARD_COOLDOWN_MS = 60_000L;
    private static final int[] REQUIRED_SECONDS = {
            0, 0, 30, 60, 120, 240, 360, 480, 600, 720, 900
    };

    /** Same trials + condition text as Meditation new.js. */
    private static final Trial[] TRIALS = {
            new Trial("minecraft:plains", "Plains", "plains",
                    "Sneak while charging Ki below 50%."),
            new Trial("minecraft:desert", "Desert", "desert",
                    "Charge Ki during daytime while below 40%."),
            new Trial("minecraft:snowy_plains", "Snowy Plains", "snowy_plains",
                    "Sneak above Y 80 while charging Ki."),
            new Trial("minecraft:nether_wastes", "Nether Wastes", "nether_wastes",
                    "Charge Ki below Y 64 while below 40%."),
            new Trial("minecraft:warped_forest", "Warped Forest", "warped_forest",
                    "Sneak and remain within 1.5 blocks while charging."),
            new Trial("minecraft:soul_sand_valley", "Soul Sand Valley", "soul_sand_valley",
                    "Remain within 1 block and avoid damage for 12 seconds while charging."),
            new Trial("dragonminez:ajissa_plains", "Ajissa Plains", "ajissa_plains",
                    "Sneak while charging Ki below 50%."),
            new Trial("dragonminez:namekian_rivers", "Namekian Rivers", "namekian_rivers",
                    "Charge Ki below 50% while inside the Namekian Rivers biome."),
            new Trial("dragonminez:sacredkai_hills", "Sacred Kai Hills", "sacredkai_hills",
                    "Charge above Y 90 without taking damage for 10 seconds."),
            new Trial("dragonminez:hyperbolic_time_chamber", "Hyperbolic Time Chamber", "htc",
                    "Remain within 1.5 blocks for 10 seconds while charging below 30%.")
    };

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final AtomicInteger GLOBAL_INDEX = new AtomicInteger(-1);
    private static final AtomicLong GLOBAL_END = new AtomicLong(0L);
    private static final AtomicBoolean GLOBAL_WARNED = new AtomicBoolean(false);
    private static final AtomicLong ROTATION_LOCK = new AtomicLong(0L);
    private static final AtomicBoolean LOADED = new AtomicBoolean(false);
    private static final AtomicLong LAST_SAVE_AT = new AtomicLong(0L);

    private MeditationProgression() {}

    /**
     * Server tick — rotate the global trial on schedule even when nobody is meditating,
     * and broadcast the new biome to everyone online.
     */
    public static void worldPulse(MinecraftServer server, int tick) {
        if (!ProgressionConfig.meditation() || server == null) {
            return;
        }
        if ((tick % 20) != 0) {
            return;
        }
        try {
            ensureLoaded();
            currentTrial(System.currentTimeMillis());
            saveIfNeeded(false);
        } catch (Throwable ignored) {
        }
    }

    public static void pulse(ServerPlayer player, long nowMs) {
        if (!ProgressionConfig.meditation() || player == null) {
            return;
        }
        if (nowMs < ProgressionData.tempGetLong(player, "med2_next_check", 0L)) {
            return;
        }
        ProgressionData.tempPut(player, "med2_next_check", nowMs + 1000L);
        try {
            ensureLoaded();
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

            // Wrong-biome observer is independent — never blocks via side effects alone.
            updateWrongBiomeWarning(player, trial, wrong, charging, nowMs);

            if (wrong) {
                resetPosition(player);
                resetFocus(player);
                return;
            }

            float energy = currentEnergy(data);
            float maxEnergy = data.getMaxEnergy();
            if (energy < 0f || maxEnergy <= 0f) {
                return;
            }
            if (energy >= maxEnergy * 0.995f) {
                resetFocus(player);
                // Only while actively charging — never spam chat when idle in the biome.
                if (charging) {
                    tellCondition(
                            player,
                            "Ki full",
                            "Spend some Ki, then charge again to train.",
                            nowMs);
                }
                return;
            }
            if (!passesFocus(player, charging, nowMs)) {
                return;
            }
            if (!passesCondition(player, trial, energy, maxEnergy, nowMs)) {
                // Hint the trial goal only while charging (subtitle, anti-spam).
                if (charging) {
                    tellCondition(player, "Meditation", plainCondition(trial), nowMs);
                }
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

    /** Staff / trigger — force a new random trial and announce it. */
    public static String advanceTrial(ServerPlayer actor) {
        if (!ProgressionConfig.meditation()) {
            return "§cMeditation progression is disabled.";
        }
        ensureLoaded();
        long now = System.currentTimeMillis();
        if (now < ROTATION_LOCK.get()) {
            return "§7Trial cycle is on cooldown.";
        }
        ROTATION_LOCK.set(now + ROTATION_LOCK_MS);
        Trial trial = chooseNewTrial(now, true);
        SystemTelemetry.log("progression", "meditation_trial", actor, null,
                Map.of("biome", trial.id, "name", trial.name));
        return "§aAdvanced meditation trial to §f" + trial.name + "§a.";
    }

    /**
     * Player-facing trial status + how-to.
     * Staff rotate hint is omitted — use {@link #explainTrials(boolean)}.
     */
    public static String explainTrials() {
        return explainTrials(false);
    }

    /**
     * Trial status + how-to. When {@code includeStaffHints} is false, never mentions
     * staff-only commands (safe for non-ops).
     */
    public static String explainTrials(boolean includeStaffHints) {
        if (!ProgressionConfig.meditation()) {
            return "§cMeditation progression is disabled.";
        }
        ensureLoaded();
        Trial t = currentTrial(System.currentTimeMillis());
        long rem = Math.max(0L, GLOBAL_END.get() - System.currentTimeMillis());
        StringBuilder sb = new StringBuilder();
        sb.append(LmChat.title("Meditation Trial")).append('\n');
        sb.append(LmChat.DIVIDER).append('\n');
        if (t == null) {
            sb.append("§7No trial is active right now.\n");
            sb.append("§8A new biome will be chosen shortly.\n");
        } else {
            sb.append("§7Biome   §f").append(t.name).append('\n');
            sb.append("§7Goal    §f").append(t.condition).append('\n');
            sb.append("§7Ends in §e").append(formatDuration(rem)).append('\n');
        }
        sb.append('\n');
        sb.append("§aHow to train\n");
        sb.append("§7• Go to the §f").append(t == null ? "trial biome" : t.name).append('\n');
        sb.append("§7• Charge Ki and complete the goal\n");
        sb.append("§7• Stay focused — wrong biome warns after §f10s\n");
        sb.append('\n');
        sb.append(LmChat.tip("/progression meditation", "refreshes this card"));
        if (includeStaffHints) {
            sb.append('\n');
            sb.append("§8Staff · §f/progression meditation next §7— rotate + broadcast");
        }
        return sb.toString();
    }

    public static String statusLine() {
        ensureLoaded();
        Trial t = currentTrial(System.currentTimeMillis());
        if (t == null) {
            return "§7No active meditation trial.";
        }
        long rem = Math.max(0L, GLOBAL_END.get() - System.currentTimeMillis());
        return "§7Trial §e" + t.name + " §8· §f" + formatDuration(rem) + " left"
                + "\n§8" + t.condition;
    }

    public static int requiredSecondsForLevel(int nextLevel) {
        if (nextLevel < 0 || nextLevel >= REQUIRED_SECONDS.length) {
            return 0;
        }
        return REQUIRED_SECONDS[nextLevel];
    }

    public static String currentTrialName() {
        ensureLoaded();
        Trial t = currentTrial(System.currentTimeMillis());
        return t == null ? "" : t.name;
    }

    public static long trialRemainingMs() {
        ensureLoaded();
        currentTrial(System.currentTimeMillis());
        return Math.max(0L, GLOBAL_END.get() - System.currentTimeMillis());
    }

    private static Trial currentTrial(long now) {
        int index = GLOBAL_INDEX.get();
        long end = GLOBAL_END.get();
        if (index < 0 || index >= TRIALS.length || end <= now) {
            // Script parity: during the short rotation lock, do not rotate or
            // keep serving an expired trial — skip until the lock clears.
            if (now < ROTATION_LOCK.get()) {
                return null;
            }
            ROTATION_LOCK.set(now + ROTATION_LOCK_MS);
            // Re-check after lock (another thread may have rotated).
            index = GLOBAL_INDEX.get();
            end = GLOBAL_END.get();
            if (index < 0 || index >= TRIALS.length || end <= now) {
                return chooseNewTrial(now, false);
            }
        }

        long remaining = end - now;
        if (remaining > 0L && remaining <= TRIAL_WARNING_MS && !GLOBAL_WARNED.get()) {
            GLOBAL_WARNED.set(true);
            broadcastChat(LmChat.tagged("Meditation Trial", "§f" + TRIALS[index].name
                    + "§7 remains active for §e5 more minutes§7."));
            saveIfNeeded(true);
        }
        return TRIALS[index];
    }

    /** Script chooseNewTrial — random index, avoid immediate repeat. */
    private static Trial chooseNewTrial(long now, boolean manual) {
        int previous = GLOBAL_INDEX.get();
        int index = ThreadLocalRandom.current().nextInt(TRIALS.length);
        if (TRIALS.length > 1 && index == previous) {
            index = (index + 1) % TRIALS.length;
        }
        long duration = manual ? MANUAL_TRIAL_DURATION_MS : TRIAL_DURATION_MS;
        GLOBAL_INDEX.set(index);
        GLOBAL_END.set(now + duration);
        GLOBAL_WARNED.set(false);
        Trial trial = TRIALS[index];
        announceTrial(trial, duration, manual);
        saveIfNeeded(true);
        return trial;
    }

    private static void announceTrial(Trial trial, long remaining, boolean manual) {
        if (trial == null) {
            return;
        }
        // Public broadcast — never mention staff/ops or staff commands.
        broadcastChat(LmChat.title("Meditation Trial"));
        broadcastChat(LmChat.DIVIDER);
        broadcastChat("§7Biome   §f" + trial.name);
        broadcastChat("§7Goal    §f" + trial.condition);
        broadcastChat("§7Ends in §e" + formatDuration(remaining));
        broadcastChat(LmChat.tip("/progression meditation", "for how to train"));
        if (manual) {
            broadcastStaffChat("§8Staff · trial was rotated manually"
                    + " · §f/progression meditation next");
        }
        // Screen title for visibility (script was chat-only; titles help notice mid-fight).
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer p : server.m_6846_().m_11314_()) {
            if (p == null) {
                continue;
            }
            sendTitle(p, "Meditation Trial", trial.name);
        }
    }

    private static void broadcastChat(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer p : server.m_6846_().m_11314_()) {
            if (p != null) {
                DmzRewards.msg(p, message);
            }
        }
    }

    /** Staff/op only — never shown to non-ops. */
    private static void broadcastStaffChat(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer p : server.m_6846_().m_11314_()) {
            if (p != null && StaffAccess.isStaff(p)) {
                DmzRewards.msg(p, message);
            }
        }
    }

    private static void sendTitle(ServerPlayer player, String title, String subtitle) {
        try {
            if (player.f_8906_ == null) {
                return;
            }
            player.f_8906_.m_9829_(new ClientboundSetTitlesAnimationPacket(5, 50, 10));
            player.f_8906_.m_9829_(new ClientboundSetTitleTextPacket(
                    Component.m_237113_(title == null ? "" : title.replaceAll("§.", ""))));
            if (subtitle != null && !subtitle.isBlank()) {
                player.f_8906_.m_9829_(new ClientboundSetSubtitleTextPacket(
                        Component.m_237113_(subtitle.replaceAll("§.", ""))));
            }
        } catch (Throwable ignored) {
        }
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
            DmzRewards.msg(player, LmChat.tagged("Meditation", "§dIncreased to level " + next + "."));
            SystemTelemetry.log("progression", "meditation_level", player, null,
                    Map.of("level", next));
            ScreenNotify.hint(
                    player,
                    "Meditation",
                    "Level " + next,
                    "med2_progress_subtitle",
                    0L);
        } else {
            ProgressionData.storedPut(player, key, progress);
            // Subtitle only while training — no chat spam (1st tick + every 5s).
            if (progress == 1L || progress % 5L == 0L) {
                ScreenNotify.hint(
                        player,
                        "Meditation",
                        progress + " / " + need + " → Lv " + next,
                        "med2_progress_subtitle",
                        PROGRESS_SUBTITLE_COOLDOWN_MS);
            }
        }
    }

    private static void handleLevelDrop(ServerPlayer player, int current) {
        long last = ProgressionData.storedGetLong(player, "med2_last_level", current);
        if (current < last) {
            for (int level = 2; level <= MAX_LEVEL; level++) {
                ProgressionData.storedRemove(player, "meditation_restore_progress_to_level_" + level);
                ProgressionData.storedRemove(player, "meditation_progress_to_level_" + level);
                ProgressionData.storedRemove(player, "meditation_training_progress_to_level_" + level);
            }
        }
        ProgressionData.storedPut(player, "med2_last_level", current);
    }

    /**
     * Script focus: progress is allowed during the first {@link #FOCUS_WINDOW_MS} of a charge,
     * then the player must release and begin charging again.
     */
    private static boolean passesFocus(ServerPlayer player, boolean charging, long now) {
        boolean wasCharging = "1".equals(ProgressionData.tempGet(player, "med2_focus_was_charging", "0"));
        boolean waiting = "1".equals(ProgressionData.tempGet(player, "med2_focus_wait_release", "0"));
        long started = ProgressionData.tempGetLong(player, "med2_focus_started", 0L);

        if (!charging) {
            ProgressionData.tempPut(player, "med2_focus_was_charging", "0");
            if (waiting) {
                ProgressionData.tempPut(player, "med2_focus_wait_release", "0");
                ProgressionData.tempPut(player, "med2_focus_started", 0L);
            }
            return false;
        }

        if (!wasCharging) {
            ProgressionData.tempPut(player, "med2_focus_was_charging", "1");
            ProgressionData.tempPut(player, "med2_focus_wait_release", "0");
            ProgressionData.tempPut(player, "med2_focus_started", now);
            return true;
        }

        if (waiting) {
            tellCondition(player, "Focus", "Release Ki, then charge again.", now);
            return false;
        }

        if (started <= 0L) {
            ProgressionData.tempPut(player, "med2_focus_started", now);
            return true;
        }

        if (now - started >= FOCUS_WINDOW_MS) {
            ProgressionData.tempPut(player, "med2_focus_wait_release", "1");
            tellCondition(player, "Focus", "Release Ki, then charge again.", now);
            return false;
        }
        return true;
    }

    private static void resetFocus(ServerPlayer player) {
        ProgressionData.tempPut(player, "med2_focus_was_charging", "0");
        ProgressionData.tempPut(player, "med2_focus_wait_release", "0");
        ProgressionData.tempPut(player, "med2_focus_started", 0L);
    }

    private static void updateWrongBiomeWarning(
            ServerPlayer player, Trial trial, boolean wrongBiome, boolean charging, long now
    ) {
        if (!wrongBiome) {
            ProgressionData.tempRemove(player, "med2_wrong_started");
            ProgressionData.tempRemove(player, "med2_wrong_last_true");
            ProgressionData.tempPut(player, "med2_wrong_warned", "0");
            return;
        }
        if (charging) {
            ProgressionData.tempPut(player, "med2_wrong_last_true", now);
            long started = ProgressionData.tempGetLong(player, "med2_wrong_started", 0L);
            boolean warned = "1".equals(ProgressionData.tempGet(player, "med2_wrong_warned", "0"));
            long lastMessage = ProgressionData.tempGetLong(player, "med2_wrong_last_message", 0L);
            if (started <= 0L) {
                started = now;
                ProgressionData.tempPut(player, "med2_wrong_started", started);
                ProgressionData.tempPut(player, "med2_wrong_warned", "0");
                warned = false;
            }
            if (!warned
                    && now - started >= WRONG_BIOME_DELAY_MS
                    && now - lastMessage >= WRONG_BIOME_HARD_COOLDOWN_MS) {
                ScreenNotify.hint(
                        player,
                        "Wrong biome",
                        "Trial: " + trial.name,
                        "med2_wrong_subtitle",
                        WRONG_BIOME_HARD_COOLDOWN_MS);
                ProgressionData.tempPut(player, "med2_wrong_warned", "1");
                ProgressionData.tempPut(player, "med2_wrong_last_message", now);
            }
            return;
        }
        long lastTrue = ProgressionData.tempGetLong(player, "med2_wrong_last_true", 0L);
        if (lastTrue > 0L && now - lastTrue <= WRONG_BIOME_RELEASE_GRACE_MS) {
            return;
        }
        ProgressionData.tempRemove(player, "med2_wrong_started");
        ProgressionData.tempRemove(player, "med2_wrong_last_true");
        ProgressionData.tempPut(player, "med2_wrong_warned", "0");
    }

    /** Subtitle-only condition hint. Never chat — avoids idle biome spam. */
    private static void tellCondition(ServerPlayer player, String title, String subtitle, long now) {
        long next = ProgressionData.tempGetLong(player, "med2_message_next", 0L);
        if (now < next) {
            return;
        }
        ProgressionData.tempPut(player, "med2_message_next", now + CONDITION_MESSAGE_COOLDOWN_MS);
        ScreenNotify.hint(
                player,
                title == null || title.isBlank() ? "Meditation" : title,
                subtitle,
                "med2_condition_subtitle",
                CONDITION_MESSAGE_COOLDOWN_MS);
    }

    private static String plainCondition(Trial trial) {
        if (trial == null || trial.condition == null) {
            return "Charge Ki to train.";
        }
        return trial.condition.replaceAll("§.", "").trim();
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
            if (level == null) {
                return false;
            }
            if (level.m_46461_()) {
                return true;
            }
            long dayTime = ((level.m_46468_() % 24000L) + 24000L) % 24000L;
            return dayTime < 12000L;
        } catch (Throwable ignored) {
            return false;
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
        String a = normalizeBiomeId(found);
        String b = normalizeBiomeId(required);
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        String aPath = a.contains(":") ? a.substring(a.indexOf(':') + 1) : a;
        String bPath = b.contains(":") ? b.substring(b.indexOf(':') + 1) : b;
        return aPath.equals(bPath);
    }

    private static String normalizeBiomeId(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return "";
        }
        return value.toLowerCase().trim().replace(' ', '_');
    }

    private static String formatDuration(long milliseconds) {
        long total = Math.max(0L, milliseconds / 1000L);
        long minutes = total / 60L;
        long seconds = total % 60L;
        if (minutes > 0L) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("legacymechanics").resolve("meditation-trial.json");
    }

    private static void ensureLoaded() {
        if (LOADED.get()) {
            return;
        }
        synchronized (LOADED) {
            if (LOADED.get()) {
                return;
            }
            try {
                Path file = path();
                if (Files.isRegularFile(file)) {
                    String json = Files.readString(file);
                    Persisted p = GSON.fromJson(json, Persisted.class);
                    if (p != null) {
                        GLOBAL_INDEX.set(p.index);
                        GLOBAL_END.set(p.endAt);
                        GLOBAL_WARNED.set(p.warned);
                    }
                }
            } catch (Throwable ignored) {
            }
            LOADED.set(true);
        }
    }

    private static void saveIfNeeded(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - LAST_SAVE_AT.get() < 5_000L) {
            return;
        }
        LAST_SAVE_AT.set(now);
        try {
            Path file = path();
            Files.createDirectories(file.getParent());
            Persisted p = new Persisted();
            p.index = GLOBAL_INDEX.get();
            p.endAt = GLOBAL_END.get();
            p.warned = GLOBAL_WARNED.get();
            Files.writeString(file, GSON.toJson(p));
        } catch (Throwable ignored) {
        }
    }

    private static final class Persisted {
        int index = -1;
        long endAt;
        boolean warned;
    }

    private record Trial(String id, String name, String type, String condition) {}
}
