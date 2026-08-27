package com.dbzlegacy.adaptivedifficulty.progression.shop;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.skills.Skills;
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
 * as {@code /skills} chat GUI pages.
 */
public final class SkillUnlockService {
    public static final int TRIGGER_ID = 21;

    private SkillUnlockService() {}

    public static void open(ServerPlayer player, String page) {
        if (!DifficultyConfig.get().enableSkillUnlockService || player == null) {
            return;
        }
        if (page == null || page.isBlank() || "main".equalsIgnoreCase(page) || "core".equalsIgnoreCase(page)) {
            showPage(player, "core");
        } else if ("advanced".equalsIgnoreCase(page) || "dmz".equalsIgnoreCase(page)) {
            showPage(player, "advanced");
        } else if ("saga".equalsIgnoreCase(page)) {
            showPage(player, "saga");
        } else {
            showPage(player, "core");
        }
        SystemTelemetry.log("skills", "open", player, null, Map.of("page", page == null ? "core" : page));
    }

    /** Trigger-21 equivalent. */
    public static void trigger21(ServerPlayer player) {
        open(player, "core");
    }

    private static void showPage(ServerPlayer player, String page) {
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            DmzRewards.msg(player, "§c[Skill Progress] ERROR: No DMZ data found.");
            return;
        }
        Skills skills;
        try {
            skills = data.getSkills();
        } catch (Throwable t) {
            skills = null;
        }
        if (skills == null) {
            DmzRewards.msg(player, "§c[Skill Progress] ERROR: No DMZ skill data found.");
            return;
        }

        int level = safeLevel(data);
        double kiDamage = safeKi(data);
        double maxEnergy = safeEnergy(data);
        int strength = safeStrength(data);

        send(player, "");
        send(player, "§6§l------ Skill Progress ------§r");
        send(player, "§7DMZ Level: §f" + level + " §8| §7Ki Damage: §f" + format(kiDamage));
        send(player, "§7Max Energy: §f" + format(maxEnergy) + " §8| §7Strength: §f" + strength);
        send(player, "§8----------------------------");

        MutableComponent nav = Component.m_237113_("§7")
                .m_7220_(btn(pageEquals(page, "core") ? "§e[Core]" : "§7[Core]", "/skills do page core", "Core skills"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn(pageEquals(page, "advanced") ? "§e[Advanced]" : "§7[Advanced]",
                        "/skills do page advanced", "DMZ 2.1 skills"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn(pageEquals(page, "saga") ? "§e[Saga]" : "§7[Saga]",
                        "/skills do page saga", "Saga unlocks"));
        send(player, nav);
        send(player, "§8----------------------------");

        switch (page.toLowerCase(Locale.ROOT)) {
            case "advanced" -> showAdvanced(player, skills, strength);
            case "saga" -> showSaga(player, skills);
            default -> showCore(player, skills);
        }
        send(player, "§8────────────────");
    }

    private static void showCore(ServerPlayer player, Skills skills) {
        send(player, "§6§lCore Skills§r");
        line(player, skills, "potentialunlock", "Potential Unlock", "§d", 30);
        line(player, skills, "fly", "Flight", "§b", 10);
        line(player, skills, "meditation", "Meditation", "§a", 10);
        line(player, skills, "kicontrol", "Ki Control", "§3", 10);
        line(player, skills, "kimanipulation", "Ki Manipulation", "§9", 10);
        line(player, skills, "kisense", "Ki Sense", "§5", 10);
        line(player, skills, "jump", "Jump", "§e", 10);
        line(player, skills, "sprint", "Sprint", "§6", 10);
    }

    private static void showAdvanced(ServerPlayer player, Skills skills, int strength) {
        send(player, "§6§lDragonMineZ 2.1 Skills§r");
        strengthLine(player, skills, "defense_penetration", "Defense Penetration", "§c", strength);
        strengthLine(player, skills, "healing_reduction", "Healing Reduction", "§4", strength);
        line(player, skills, "instant_transmission", "Instant Transmission", "§d", 10);
        line(player, skills, "ki_infusion", "Ki Infusion", "§b", 10);
        line(player, skills, "kiboost", "Ki Boost", "§3", 10);
        line(player, skills, "kiprotection", "Ki Protection", "§9", 10);
    }

    private static void showSaga(ServerPlayer player, Skills skills) {
        send(player, "§6§lSaga Skills§r");
        sagaLine(player, skills, "kaioken", "Kaioken", "§c", 10, "Unlock via Saiyan saga progress.");
        sagaLine(player, skills, "fusion", "Fusion", "§d", 5, "Unlock via fusion saga progress.");
        sagaLine(player, skills, "potential", "Potential", "§5", 10, "Complete Potential Unlock trials.");
    }

    private static void line(
            ServerPlayer player, Skills skills, String id, String name, String color, int fallbackMax
    ) {
        int level = skillLevel(skills, id);
        int max = skillMax(skills, id, fallbackMax);
        if (level >= max && max > 0) {
            send(player, color + name + "§7: §6§lMAX§r §7(" + level + "/" + max + ")");
        } else {
            send(player, color + name + "§7: §f" + level + "/" + max);
        }
    }

    private static void strengthLine(
            ServerPlayer player, Skills skills, String id, String name, String color, int strength
    ) {
        int level = skillLevel(skills, id);
        int max = Math.min(10, skillMax(skills, id, 10));
        if (level >= max) {
            send(player, color + name + "§7: §6§lMAX§r §7(" + level + "/" + max + ")");
            return;
        }
        int next = level + 1;
        int required = strengthRequirement(next);
        send(player, color + name + "§7: §f" + level + "/" + max);
        send(player, "§8  - §7Next requires Strength §f" + required
                + (strength >= required ? " §a✓" : " §c(have " + strength + ")"));
    }

    private static void sagaLine(
            ServerPlayer player, Skills skills, String id, String name, String color, int max, String hint
    ) {
        int level = skillLevel(skills, id);
        if (level < 1) {
            send(player, color + name + "§7: §f0/" + max);
            send(player, "§8  - §7" + hint);
        } else if (level >= max) {
            send(player, color + name + "§7: §6§lMAX§r §7(" + level + "/" + max + ")");
        } else {
            send(player, color + name + "§7: §f" + level + "/" + max);
        }
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
            return Math.max(0, (int) Math.floor(data.getStats().getStrength()));
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
