package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Public static API for Bukkit companion reflection — Legacy Mechanics hub ({@code /lm}).
 */
public final class MechanicsGuiApi {
    private MechanicsGuiApi() {}

    public static Map<String, String> placeholders(ServerPlayer player) {
        Map<String, String> out = new HashMap<>();
        out.put("bridge_ok", "false");
        if (player == null) {
            return out;
        }
        DifficultyConfig c = DifficultyConfig.get();
        out.put("bridge_ok", "true");
        out.put("staff", StaffAccess.isStaff(player) ? "true" : "false");
        out.put("progression", c.enableProgression ? "true" : "false");
        out.put("rival", c.enableRivalSystem ? "true" : "false");
        out.put("spar", c.enableSparringSystem ? "true" : "false");
        out.put("prestige", c.enablePrestigeSystem ? "true" : "false");
        out.put("skills", c.enableSkillUnlockService ? "true" : "false");
        out.put("syslog", c.enableSystemTelemetry ? "true" : "false");
        out.put("syslog_status", SystemTelemetry.statusLine());
        return out;
    }

    public static List<String> linesForPage(ServerPlayer player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        return switch (p) {
            case "help" -> List.of(
                    "§6§lLegacy Mechanics",
                    "§e/difficulty §7— Unlock tiers & scaling",
                    "§e/rival §7— Rivalry, challenges, progression",
                    "§e/spar §7— Sparring TP & mentor",
                    "§e/progression §7— Natural skills / TP / race",
                    "§e/prestige §7— Prestige levels",
                    "§e/skills §7— Skill unlock progress",
                    "§e/lm §7— This hub"
            );
            case "logs", "syslog" -> {
                if (player == null || !StaffAccess.isStaff(player)) {
                    yield List.of("§cStaff only.");
                }
                boolean on = DifficultyConfig.get().enableSystemTelemetry;
                yield List.of(
                        "§7System telemetry §f" + (on ? "ON" : "OFF"),
                        "§8" + SystemTelemetry.statusLine()
                );
            }
            default -> {
                Map<String, String> ph = placeholders(player);
                List<String> lore = new ArrayList<>();
                lore.add("§7Difficulty · Rival · Spar · Progression");
                lore.add("§7Prog §f" + onOff(ph.get("progression"))
                        + " §8| §7Rival §f" + onOff(ph.get("rival"))
                        + " §8| §7Spar §f" + onOff(ph.get("spar")));
                lore.add("§7Prestige §f" + onOff(ph.get("prestige"))
                        + " §8| §7Skills §f" + onOff(ph.get("skills")));
                yield lore;
            }
        };
    }

    /**
     * Dispatch {@code /lm do} actions. Does not reopen GUI — caller reopens.
     */
    public static String handleDo(ServerPlayer player, String action, String arg, String page) {
        if (player == null) {
            return "§cPlayers only.";
        }
        String act = action == null ? "" : action.toLowerCase(Locale.ROOT).trim();
        if ("page".equals(act) || "refresh".equals(act)) {
            return "";
        }
        return "§cUnknown hub action: " + act;
    }

    private static String onOff(String v) {
        return "true".equalsIgnoreCase(v) ? "ON" : "OFF";
    }
}
