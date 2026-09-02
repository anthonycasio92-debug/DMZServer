package com.dbzlegacy.adaptivedifficulty.progression.shop;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dbzlegacy.adaptivedifficulty.progression.InvestedStrength;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.progression.skills.FlightProgression;
import com.dbzlegacy.adaptivedifficulty.progression.skills.MeditationProgression;
import com.dbzlegacy.adaptivedifficulty.progression.skills.PotentialProgression;
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
        if (page == null || page.isBlank() || "main".equalsIgnoreCase(page) || "core".equalsIgnoreCase(page)
                || "natural".equalsIgnoreCase(page)) {
            showPage(player, "core", skillCheck);
        } else if ("saga".equalsIgnoreCase(page)
                || "advanced".equalsIgnoreCase(page)
                || "dmz".equalsIgnoreCase(page)) {
            // Advanced was folded into Saga — keep aliases so old links still work.
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

    /** @deprecated Advanced folded into Saga — returns saga lines. */
    public static List<String> advancedLines(ServerPlayer player) {
        return buildPageLines(player, "saga");
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
        // Keep live max levels aligned with skills.json before we read them.
        DmzSkillUtil.refreshMaxes(skills);
        int level = safeLevel(data);
        double kiDamage = safeKi(data);
        double maxEnergy = safeEnergy(data);
        int totalStr = safeStrength(data);
        int investedStr = InvestedStrength.points(player);
        // Slim header — one live line for inventory GUIs (chat path still gets separator).
        out.add("§7DMZ §f" + level
                + " §8· §7Ki §f" + format(kiDamage)
                + " §8· §7Energy §f" + format(maxEnergy)
                + " §8· §7STR §f" + totalStr + " §8(§f" + investedStr + "§8 invested)");
        out.add("§8----------------------------");
        switch (page.toLowerCase(Locale.ROOT)) {
            case "saga", "advanced", "dmz" -> appendSaga(out, skills);
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
                        cmdRoot + " do page saga", "Train with masters in the skills saga."));
        send(player, nav);
        send(player, "§8────────────────");
    }

    /**
     * Natural: Potential Unlock, Flight, Meditation, Jump, Sprint.
     * Strength-gated Jump/Sprint stay here; saga unlocks live on Saga.
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
        boolean piccolo = PotentialProgression.hasPiccoloUnlock(player);
        if (level >= HARD_MAX_POTENTIAL) {
            out.add("§dPotential Unlock§7: §6§lMAX§r §7(" + level + "/" + HARD_MAX_POTENTIAL + ")");
            tip(out, "Potential is fully unlocked.");
            return;
        }
        if (level >= 10 && !piccolo) {
            out.add("§dPotential Unlock§7: §f" + level + "/" + HARD_MAX_POTENTIAL
                    + " §8· §eSOFT CAP");
            tip(out, "Beat Piccolo in the skill saga to open the path to 30.");
            return;
        }
        int next = level + 1;
        int required = next * 100;
        long progress = ProgressionData.storedGetLong(player, "potentialunlock_points_to_level_" + next, 0L);
        if (progress > required) {
            progress = required;
        }
        String status = piccolo && level >= 10 ? " §8· §aPiccolo" : "";
        out.add("§dPotential Unlock§7: §f" + level + "/" + HARD_MAX_POTENTIAL
                + status + " §8· §f" + progress + "/" + required);
        if (level < 10) {
            tip(out, "Spar with players — soft caps at 10 until Piccolo.");
        } else {
            tip(out, "Keep sparring to climb toward 30.");
        }
    }

    private static void appendFlight(List<String> out, ServerPlayer player, Skills skills) {
        int level = skillLevel(skills, "fly");
        int max = skillMax(skills, "fly", 10);
        if (level >= max && max > 0) {
            out.add("§bFlight§7: §6§lMAX§r §7(" + level + "/" + max + ")");
            tip(out, "Flight is fully trained.");
            return;
        }
        int next = level + 1;
        int needSec = FlightProgression.requiredSecondsForLevel(next);
        String progressNote = "";
        if (needSec > 0) {
            long progress = ProgressionData.storedGetLong(player, "fly_training_progress_to_level_" + next, 0L);
            if (progress > needSec * 20L) {
                progress = progress / 1000L;
            }
            if (progress > needSec) {
                progress = needSec;
            }
            progressNote = " §8· §f" + formatTime(progress) + "/" + formatTime(needSec);
        }
        out.add("§bFlight§7: §f" + level + "/" + max + progressNote);
        tip(out, "Stay airborne while flying to raise Flight.");
    }

    private static void appendMeditation(List<String> out, ServerPlayer player, Skills skills) {
        int level = skillLevel(skills, "meditation");
        int max = skillMax(skills, "meditation", 10);
        if (level >= max && max > 0) {
            out.add("§aMeditation§7: §6§lMAX§r §7(" + level + "/" + max + ")");
            tip(out, "Meditation is fully refined.");
            return;
        }
        int next = level + 1;
        int needSec = MeditationProgression.requiredSecondsForLevel(next);
        String progressNote = "";
        if (needSec > 0) {
            long progress = ProgressionData.storedGetLong(
                    player, "meditation_restore_progress_to_level_" + next, 0L);
            if (progress > needSec) {
                progress = needSec;
            }
            progressNote = " §8· §f" + formatTime(progress) + "/" + formatTime(needSec);
        }
        out.add("§aMeditation§7: §f" + level + "/" + max + progressNote);
        tip(out, "Sit and charge Ki in the trial biome (/progression meditation).");
    }

    private static void appendSaga(List<String> out, Skills skills) {
        out.add("§6§lSaga Skills§r");
        out.add("§8Train with masters — unlock and raise these in the skills saga.");
        appendSagaSkill(out, skills, "kicontrol", "Ki Control", "§3", 1);
        appendSagaSkill(out, skills, "kimanipulation", "Ki Manipulation", "§9", 10);
        appendSagaSkill(out, skills, "kisense", "Ki Sense", "§5", 10);
        appendSagaSkill(out, skills, "defense_penetration", "Defense Penetration", "§c", 10);
        appendSagaSkill(out, skills, "healing_reduction", "Healing Reduction", "§4", 10);
        appendSagaSkill(out, skills, "instant_transmission", "Instant Transmission", "§d", 10);
        appendSagaSkill(out, skills, "ki_infusion", "Ki Infusion", "§b", 10);
        appendSagaSkill(out, skills, "kiboost", "Ki Boost", "§3", 4);
        appendSagaSkill(out, skills, "kiprotection", "Ki Protection", "§9", 10);
        appendSagaSkill(out, skills, "kaioken", "Kaioken", "§c", 5);
        appendSagaSkill(out, skills, "fusion", "Fusion", "§d", 5);
    }

    private static void appendSagaSkill(
            List<String> out, Skills skills, String id, String name, String color, int fallbackMax
    ) {
        int level = skillLevel(skills, id);
        int max = skillMax(skills, id, fallbackMax);
        String human = sagaHow(id);
        if (level < 1) {
            out.add(color + name + "§7: §f0/" + max);
            tip(out, "Locked — " + human);
            return;
        }
        if (level >= max) {
            out.add(color + name + "§7: §6§lMAX§r §7(" + level + "/" + max + ")");
            tip(out, human);
        } else {
            out.add(color + name + "§7: §f" + level + "/" + max);
            tip(out, human);
        }
    }

    private static void appendStrengthLine(
            List<String> out, Skills skills, String id, String name, String color, int investedStr
    ) {
        int level = skillLevel(skills, id);
        int max = skillMax(skills, id, 10);
        if (level >= max) {
            out.add(color + name + "§7: §6§lMAX§r §7(" + level + "/" + max + ")");
            tip(out, strengthHow(id, true, investedStr, 0));
            return;
        }
        int next = level + 1;
        int required = strengthRequirement(next);
        int need = Math.max(0, required - investedStr);
        String needNote = need <= 0
                ? " §8· §aready"
                : " §8· need §f" + need + " STR";
        out.add(color + name + "§7: §f" + level + "/" + max + needNote);
        tip(out, strengthHow(id, false, investedStr, required));
    }

    /** One short human tip line per skill (progress lives on the level line). */
    private static void tip(List<String> out, String tip) {
        if (tip == null || tip.isBlank()) {
            return;
        }
        out.add("§8  - §7" + tip);
    }

    private static String strengthHow(String id, boolean maxed, int investedStr, int required) {
        String skill = "jump".equals(id) ? "Jump" : "Sprint";
        if (maxed) {
            return skill + " is fully unlocked.";
        }
        if (investedStr >= required) {
            return "Enough Strength invested — keep training for the next " + skill + " rank.";
        }
        return "Invest Strength to raise " + skill + ".";
    }

    private static String sagaHow(String id) {
        return switch (id == null ? "" : id.toLowerCase(Locale.ROOT)) {
            case "kicontrol" -> "Train Ki Control with skills-saga masters.";
            case "kimanipulation" -> "Shape Ki through skills-saga master training.";
            case "kisense" -> "Sharpen Ki Sense in the skills saga.";
            case "defense_penetration" -> "Earn this through skills-saga challenges.";
            case "healing_reduction" -> "Unlock and train this in the skills saga.";
            case "instant_transmission" -> "Learn Instant Transmission from saga masters.";
            case "ki_infusion" -> "Unlock Ki Infusion in the skills saga.";
            case "kiboost" -> "Raise Ki Boost with skills-saga masters.";
            case "kiprotection" -> "Build Ki Protection through saga training.";
            case "kaioken" -> "Unlock Kaioken in the skills saga, then train ranks.";
            case "fusion" -> "Unlock Fusion via the skills-saga master path.";
            default -> "Train with skills-saga masters to unlock and raise this.";
        };
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
        // skills.json cost-ladder length is authoritative (kiboost=4, kicontrol=1, …).
        int cfg = DmzSkillUtil.configuredMaxLevel(id);
        if (cfg > 0) {
            return cfg;
        }
        try {
            if (skills != null) {
                int max = skills.getMaxSkillLevel(id);
                if (max > 0) {
                    return max;
                }
            }
        } catch (Throwable ignored) {
        }
        return fallback;
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
