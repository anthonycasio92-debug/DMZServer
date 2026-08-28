package com.dbzlegacy.adaptivedifficulty.progression.shop;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.InvestedStrength;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.progression.skills.FlightProgression;
import com.dbzlegacy.adaptivedifficulty.progression.skills.MeditationProgression;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.skills.Skills;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of {@code SkillUnlockNPC.js} + {@code SkillCheckCommand.js} (trigger 21)
 * as {@code /skills} chat GUI pages. Also exposes list builders for inventory GUIs.
 */
public final class SkillUnlockService {
    public static final int TRIGGER_ID = 21;
    private static final int HARD_MAX_POTENTIAL = 30;

    private SkillUnlockService() {}

    public static void open(ServerPlayer player, String page) {
        open(player, page, false);
    }

    /**
     * @param skillCheck when true, titles/nav use Skill Check branding ({@code /skillcheck}).
     */
    public static void open(ServerPlayer player, String page, boolean skillCheck) {
        if (!DifficultyConfig.get().enableSkillUnlockService || player == null) {
            return;
        }
        if (page == null || page.isBlank() || "main".equalsIgnoreCase(page) || "core".equalsIgnoreCase(page)) {
            showPage(player, "core", skillCheck);
        } else if ("advanced".equalsIgnoreCase(page) || "dmz".equalsIgnoreCase(page)) {
            showPage(player, "advanced", skillCheck);
        } else if ("saga".equalsIgnoreCase(page)) {
            showPage(player, "saga", skillCheck);
        } else {
            showPage(player, "core", skillCheck);
        }
        SystemTelemetry.log(
                skillCheck ? "skillcheck" : "skills",
                "open",
                player,
                null,
                Map.of("page", page == null ? "core" : page));
    }

    /** Trigger-21 equivalent — donator Skill Check only. */
    public static void trigger21(ServerPlayer player) {
        com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService.trigger21(player);
    }

    /** Header + core skill lines for inventory GUI lore. */
    public static List<String> coreLines(ServerPlayer player) {
        return buildPageLines(player, "core");
    }

    /** Header + advanced skill lines for inventory GUI lore. */
    public static List<String> advancedLines(ServerPlayer player) {
        return buildPageLines(player, "advanced");
    }

    /** Header + saga skill lines for inventory GUI lore. */
    public static List<String> sagaLines(ServerPlayer player) {
        return buildPageLines(player, "saga");
    }

    private static List<String> buildPageLines(ServerPlayer player, String page) {
        List<String> out = new ArrayList<>();
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            out.add("§c[Skill Progress] ERROR: No DMZ data found.");
            return out;
        }
        Skills skills;
        try {
            skills = data.getSkills();
        } catch (Throwable t) {
            skills = null;
        }
        if (skills == null) {
            out.add("§c[Skill Progress] ERROR: No DMZ skill data found.");
            return out;
        }
        int level = safeLevel(data);
        double kiDamage = safeKi(data);
        double maxEnergy = safeEnergy(data);
        int totalStr = safeStrength(data);
        int investedStr = InvestedStrength.points(player);
        out.add("§7DMZ Level: §f" + level + " §8| §7Ki Damage: §f" + format(kiDamage));
        out.add("§7Max Energy: §f" + format(maxEnergy)
                + " §8| §7STR §f" + totalStr + " §8(invested §f" + investedStr + "§8)");
        out.add("§8----------------------------");
        switch (page.toLowerCase(Locale.ROOT)) {
            case "advanced", "dmz" -> appendAdvanced(out, skills, investedStr);
            case "saga" -> appendSaga(out, skills);
            default -> appendNatural(out, player, skills, investedStr);
        }
        return out;
    }

    private static void showPage(ServerPlayer player, String page, boolean skillCheck) {
        List<String> lines = buildPageLines(player, page);
        if (lines.size() == 1 && lines.get(0).startsWith("§c")) {
            DmzRewards.msg(player, lines.get(0).replace("Skill Progress", skillCheck ? "Skill Check" : "Skill Progress"));
            return;
        }

        String cmdRoot = skillCheck ? "/skillcheck" : "/skills";
        send(player, "");
        send(player, skillCheck
                ? "§6§l------ Skill Check ------§r"
                : "§6§l------ Skill Progress ------§r");
        for (String line : lines) {
            send(player, line);
        }

        MutableComponent nav = Component.m_237113_("§7")
                .m_7220_(btn(pageEquals(page, "core") ? "§e[Natural]" : "§7[Natural]",
                        cmdRoot + " do page core", "Natural progression"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn(pageEquals(page, "saga") ? "§e[Saga]" : "§7[Saga]",
                        cmdRoot + " do page saga", "Saga unlocks"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn(pageEquals(page, "advanced") ? "§e[Advanced]" : "§7[Advanced]",
                        cmdRoot + " do page advanced", "DMZ 2.1 skills"));
        send(player, nav);
        send(player, "§8────────────────");
    }

    /**
     * Natural progression first: Potential Unlock (not a train skill), Flight,
     * Meditation, Jump, Sprint. Saga / Advanced pages hold the rest.
     */
    private static void appendNatural(
            List<String> out, ServerPlayer player, Skills skills, int investedStr
    ) {
        out.add("§6§lNatural Progression§r");
        out.add("§8Levels from play — not saga skill purchases.");
        appendPotential(out, player, skills);
        appendFlight(out, player, skills);
        appendMeditation(out, player, skills);
        appendStrengthLine(out, skills, "jump", "Jump", "§a", investedStr);
        appendStrengthLine(out, skills, "sprint", "Sprint", "§e", investedStr);
    }

    private static void appendPotential(List<String> out, ServerPlayer player, Skills skills) {
        int level = skillLevel(skills, "potentialunlock");
        // Always show hard ladder /30 — DMZ max may still read 10 before Guru unlock.
        if (level >= HARD_MAX_POTENTIAL) {
            out.add("§dPotential Unlock§7: §6§lMAX§r §7(" + level + "/" + HARD_MAX_POTENTIAL + ")");
            how(out, "PvP hits & taking damage. Soft-cap 10 → Guru → 11–30.");
            return;
        }
        if (level == 10) {
            out.add("§dPotential Unlock§7: §f" + level + "/" + HARD_MAX_POTENTIAL
                    + " §8· §eSOFT CAP");
            out.add("§8  - §eSpeak to Guru to unlock levels 11–30.");
            how(out, "Natural training stops at 10 until Guru unlocks you.");
            return;
        }
        out.add("§dPotential Unlock§7: §f" + level + "/" + HARD_MAX_POTENTIAL);
        if (level < 10) {
            how(out, "PvP hits & taking damage. Soft-caps at 10 (then Guru for 11–30).");
        } else {
            how(out, "PvP hits & taking damage toward the next Potential Unlock level.");
        }
        int next = level + 1;
        int required = next * 100;
        long progress = ProgressionData.storedGetLong(player, "potentialunlock_points_to_level_" + next, 0L);
        if (progress > required) {
            progress = required;
        }
        out.add("§8  - §7Progress §f" + progress + "§7/§f" + required
                + " §8· §7to level §f" + next);
        String method = ProgressionData.storedGet(player, "potentialunlock_last_method", "");
        long streak = ProgressionData.storedGetLong(player, "potentialunlock_same_method_streak", 0L);
        if (method != null && !method.isBlank()) {
            out.add("§8  - §7Last method §f" + formatPotentialMethod(method)
                    + (streak > 0 ? " §8(streak " + streak + "/5)" : ""));
        }
    }

    private static void appendFlight(List<String> out, ServerPlayer player, Skills skills) {
        int level = skillLevel(skills, "fly");
        int max = skillMax(skills, "fly", 10);
        if (level >= max && max > 0) {
            out.add("§bFlight§7: §6§lMAX§r §7(" + level + "/" + max + ")");
            how(out, "Stay airborne while flying to train flight time.");
            return;
        }
        out.add("§bFlight§7: §f" + level + "/" + max);
        how(out, "Stay airborne while flying to train flight time.");
        int next = level + 1;
        int needSec = FlightProgression.requiredSecondsForLevel(next);
        if (needSec <= 0) {
            return;
        }
        long progress = ProgressionData.storedGetLong(player, "fly_training_progress_to_level_" + next, 0L);
        // Live script stored ms historically; normalize to seconds when oversized.
        if (progress > needSec * 20L) {
            progress = progress / 1000L;
        }
        if (progress > needSec) {
            progress = needSec;
        }
        out.add("§8  - §7Training §f" + formatTime(progress) + "§7/§f" + formatTime(needSec));
    }

    private static void appendMeditation(List<String> out, ServerPlayer player, Skills skills) {
        int level = skillLevel(skills, "meditation");
        int max = skillMax(skills, "meditation", 10);
        if (level >= max && max > 0) {
            out.add("§aMeditation§7: §6§lMAX§r §7(" + level + "/" + max + ")");
            how(out, "Charge Ki in the active global trial biome (focus window).");
        } else {
            out.add("§aMeditation§7: §f" + level + "/" + max);
            how(out, "Charge Ki in the active global trial biome (focus window).");
            int next = level + 1;
            int needSec = MeditationProgression.requiredSecondsForLevel(next);
            if (needSec > 0) {
                long progress = ProgressionData.storedGetLong(
                        player, "meditation_restore_progress_to_level_" + next, 0L);
                if (progress > needSec) {
                    progress = needSec;
                }
                out.add("§8  - §7Restore §f" + formatTime(progress) + "§7/§f" + formatTime(needSec));
            }
        }
        String trial = MeditationProgression.currentTrialName();
        if (trial != null && !trial.isBlank()) {
            long rem = MeditationProgression.trialRemainingMs();
            out.add("§8  - §7Trial §f" + trial + " §8(" + formatTime(rem / 1000L) + " left)");
        }
    }

    private static void appendAdvanced(List<String> out, Skills skills, int investedStr) {
        out.add("§6§lDragonMineZ 2.1 Skills§r");
        out.add("§8Unlocked / trained via saga skill progress.");
        appendStrengthLine(out, skills, "defense_penetration", "Defense Penetration", "§c", investedStr);
        appendStrengthLine(out, skills, "healing_reduction", "Healing Reduction", "§4", investedStr);
        appendSagaSkill(out, skills, "instant_transmission", "Instant Transmission", "§d", 10,
                "Complete the Saga Story to unlock Instant Transmission.");
        appendSagaSkill(out, skills, "ki_infusion", "Ki Infusion", "§b", 10,
                "Complete the Saga Story to unlock Ki Infusion.");
        appendSagaSkill(out, skills, "kiboost", "Ki Boost", "§3", 10,
                "Complete the Saga Story to unlock Ki Boost.");
        appendSagaSkill(out, skills, "kiprotection", "Ki Protection", "§9", 10,
                "Complete the Saga Story to unlock Ki Protection.");
    }

    private static void appendSaga(List<String> out, Skills skills) {
        out.add("§6§lSaga Skills§r");
        out.add("§8Unlocked by completing skill sagas / story progress.");
        appendSagaSkill(out, skills, "kicontrol", "Ki Control", "§3", 10,
                "Gained by skill saga.");
        appendSagaSkill(out, skills, "kimanipulation", "Ki Manipulation", "§9", 10,
                "Gained by skill saga.");
        appendSagaSkill(out, skills, "kisense", "Ki Sense", "§5", 10,
                "Obtained through skill saga.");
        appendSagaLine(out, skills, "kaioken", "Kaioken", "§c", 10, "Unlock via Saiyan saga progress.");
        appendSagaLine(out, skills, "fusion", "Fusion", "§d", 5, "Unlock via fusion saga progress.");
    }

    private static void appendLine(
            List<String> out, Skills skills, String id, String name, String color, int fallbackMax
    ) {
        int level = skillLevel(skills, id);
        int max = skillMax(skills, id, fallbackMax);
        if (level >= max && max > 0) {
            out.add(color + name + "§7: §6§lMAX§r §7(" + level + "/" + max + ")");
        } else {
            out.add(color + name + "§7: §f" + level + "/" + max);
        }
        String tip = howToLevel(id);
        if (tip != null) {
            how(out, tip);
        }
    }

    private static void appendSagaSkill(
            List<String> out, Skills skills, String id, String name, String color, int max, String lockedHint
    ) {
        int level = skillLevel(skills, id);
        if (level < 1) {
            out.add(color + name + "§7: §f0/" + max);
            out.add("§8  - §7" + lockedHint);
            return;
        }
        if (level >= max) {
            out.add(color + name + "§7: §6§lMAX§r §7(" + level + "/" + max + ")");
        } else {
            out.add(color + name + "§7: §f" + level + "/" + max);
        }
        String tip = howToLevel(id);
        if (tip != null) {
            how(out, tip);
        }
    }

    private static void appendStrengthLine(
            List<String> out, Skills skills, String id, String name, String color, int investedStr
    ) {
        int level = skillLevel(skills, id);
        int max = Math.min(10, skillMax(skills, id, 10));
        if (level >= max) {
            out.add(color + name + "§7: §6§lMAX§r §7(" + level + "/" + max + ")");
            how(out, strengthHow(id));
            return;
        }
        int next = level + 1;
        int required = strengthRequirement(next);
        out.add(color + name + "§7: §f" + level + "/" + max
                + " §8· §7next §f" + next);
        how(out, strengthHow(id));
        out.add("§8  - §7Strength unlock §f" + required
                + " §8· §7have §f" + investedStr
                + (investedStr >= required ? " §a✓" : " §c✗"));
    }

    private static void appendSagaLine(
            List<String> out, Skills skills, String id, String name, String color, int max, String hint
    ) {
        int level = skillLevel(skills, id);
        if (level < 1) {
            out.add(color + name + "§7: §f0/" + max);
            out.add("§8  - §7" + hint);
        } else if (level >= max) {
            out.add(color + name + "§7: §6§lMAX§r §7(" + level + "/" + max + ")");
            how(out, "Unlocked via saga progress; raise with saga milestones.");
        } else {
            out.add(color + name + "§7: §f" + level + "/" + max);
            how(out, "Continue saga milestones to raise this skill.");
        }
    }

    private static void how(List<String> out, String tip) {
        if (tip == null || tip.isBlank()) {
            return;
        }
        out.add("§8  - §7How: " + tip);
    }

    private static String strengthHow(String id) {
        if ("jump".equals(id) || "sprint".equals(id)) {
            return "Strength unlocked (invested STR = total − race/class base).";
        }
        if ("defense_penetration".equals(id) || "healing_reduction".equals(id)) {
            return "Strength unlocked at invested Strength thresholds.";
        }
        return "Unlocked by invested Strength at the next threshold.";
    }

    private static String howToLevel(String id) {
        return switch (id == null ? "" : id.toLowerCase(Locale.ROOT)) {
            case "kicontrol", "kimanipulation" -> "Gained by skill saga.";
            case "kisense" -> "Obtained through skill saga.";
            case "instant_transmission" -> "Train after unlocking via the Saga Story.";
            case "ki_infusion" -> "Train after unlocking via the Saga Story.";
            case "kiboost" -> "Train after unlocking via the Saga Story.";
            case "kiprotection" -> "Train after unlocking via the Saga Story.";
            default -> "Unlock via saga / story progress, then train in play.";
        };
    }

    private static String formatPotentialMethod(String method) {
        if (method == null || method.isBlank()) {
            return "";
        }
        if ("blocking".equalsIgnoreCase(method) || "taking_damage".equalsIgnoreCase(method)) {
            return "taking damage";
        }
        return method.replace('_', ' ');
    }

    private static int strengthRequirement(int next) {
        return switch (next) {
            case 1 -> 20;
            case 2 -> 100;
            case 3 -> 250;
            case 4 -> 500;
            case 5 -> 1000;
            case 6 -> 1500;
            case 7 -> 2000;
            case 8 -> 2500;
            case 9 -> 3000;
            case 10 -> 3500;
            default -> 3500;
        };
    }

    private static String formatTime(long seconds) {
        long s = Math.max(0L, seconds);
        long h = s / 3600L;
        long m = (s % 3600L) / 60L;
        long r = s % 60L;
        if (h > 0) {
            return h + "h " + m + "m";
        }
        if (m > 0) {
            return m + "m " + r + "s";
        }
        return r + "s";
    }

    private static int skillLevel(Skills skills, String id) {
        try {
            return Math.max(0, skills.getSkillLevel(id));
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int skillMax(Skills skills, String id, int fallback) {
        try {
            int max = skills.getMaxSkillLevel(id);
            return max > 0 ? max : fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }

    private static int safeLevel(StatsData data) {
        try {
            return Math.max(1, data.getLevel());
        } catch (Throwable t) {
            return 1;
        }
    }

    private static double safeKi(StatsData data) {
        try {
            return Math.max(0.0, data.getKiDamage());
        } catch (Throwable t) {
            return 0.0;
        }
    }

    private static double safeEnergy(StatsData data) {
        try {
            return Math.max(0.0, data.getMaxEnergy());
        } catch (Throwable t) {
            return 0.0;
        }
    }

    private static int safeStrength(StatsData data) {
        try {
            return Math.max(0, data.getStats().getStrength());
        } catch (Throwable t) {
            return 0;
        }
    }

    private static boolean pageEquals(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }

    private static String format(double v) {
        if (v >= 1000) {
            return DmzRewards.formatWhole(v);
        }
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static MutableComponent btn(String label, String command, String hover) {
        return Component.m_237113_(label).m_6270_(Style.f_131099_
                .m_131142_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .m_131144_(new HoverEvent(HoverEvent.Action.f_130831_, Component.m_237113_(hover))));
    }

    private static void send(ServerPlayer player, String text) {
        player.m_213846_(Component.m_237113_(text));
    }

    private static void send(ServerPlayer player, Component text) {
        player.m_213846_(text);
    }
}
