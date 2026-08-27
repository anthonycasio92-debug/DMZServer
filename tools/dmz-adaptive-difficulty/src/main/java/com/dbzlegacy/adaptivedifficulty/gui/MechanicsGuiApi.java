package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
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
 * Players are guided to {@code /lm} only; hub buttons use {@code lm do open &lt;system&gt;}.
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
        out.put("skillcheck", SkillCheckService.canUse(player) ? "true" : "false");
        out.put("skillcheck_session", SkillCheckService.inSession(player) ? "true" : "false");
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
        boolean staff = player != null && StaffAccess.isStaff(player);
        boolean skillCheck = player != null && SkillCheckService.canUse(player);
        return switch (p) {
            case "help" -> {
                List<String> help = new ArrayList<>();
                help.add("§6§lLegacy Mechanics");
                help.add("§e/lm §7— Open this hub (use this)");
                help.add("§7From the hub, click a system:");
                help.add("§aDifficulty §7— Unlock tiers & scaling");
                help.add("§6Rival §7— Rivalry, challenges, RP");
                help.add("§bSpar §7— Sparring TP & mentor");
                if (skillCheck) {
                    help.add("§eSkill Check §7— Donator skill progress");
                }
                help.add("");
                help.add("§8Direct /difficulty · /rival · /spar still open GUIs");
                help.add("§8for compatibility, but prefer §f/lm§8.");
                if (staff) {
                    help.add("");
                    help.add("§8Staff: /lm admin · /progression · /prestige · /skills");
                    help.add("§8Staff: /difficulty admin · /rival admin · /spar admin");
                }
                yield help;
            }
            case "logs", "syslog" -> {
                if (player == null || !StaffAccess.isStaff(player)) {
                    yield List.of("§cStaff only.");
                }
                boolean on = DifficultyConfig.get().enableSystemTelemetry;
                List<String> lines = new ArrayList<>();
                lines.add("§7System telemetry §f" + (on ? "ON" : "OFF"));
                lines.add("§8" + SystemTelemetry.statusLine());
                lines.add("§7Dir §f" + SystemTelemetry.telemetryDir());
                lines.add("§8Buttons: on · off · flush");
                lines.add("§8Or: /lm admin syslog on|off|status|flush");
                yield lines;
            }
            default -> {
                Map<String, String> ph = placeholders(player);
                List<String> lore = new ArrayList<>();
                lore.add("§7Pick a system below");
                lore.add("§7Rival §f" + onOff(ph.get("rival"))
                        + " §8| §7Spar §f" + onOff(ph.get("spar")));
                if (staff) {
                    lore.add("§7Prog §f" + onOff(ph.get("progression"))
                            + " §8| §7Prestige §f" + onOff(ph.get("prestige"))
                            + " §8| §7Skills §f" + onOff(ph.get("skills")));
                } else if (skillCheck) {
                    lore.add("§7Skill Check §aavailable");
                }
                yield lore;
            }
        };
    }

    /**
     * Dispatch {@code /lm do} actions. Does not reopen GUI — caller reopens.
     * {@code open} is handled by the Bukkit companion (inventory open); Forge returns a hint.
     */
    public static String handleDo(ServerPlayer player, String action, String arg, String page) {
        if (player == null) {
            return "§cPlayers only.";
        }
        String act = action == null ? "" : action.toLowerCase(Locale.ROOT).trim();
        String a = arg == null ? "" : arg.trim();
        if ("page".equals(act) || "refresh".equals(act)) {
            return "";
        }
        if ("open".equals(act)) {
            // Bukkit plugin opens inventories; Forge chat fallback hints the click path.
            return "";
        }
        if ("syslog".equals(act)) {
            if (!StaffAccess.isStaff(player)) {
                return "§cStaff only.";
            }
            String sub = a.toLowerCase(Locale.ROOT);
            return switch (sub) {
                case "on", "true", "enable" -> {
                    SystemTelemetry.setEnabled(true);
                    yield "§aSystem telemetry ON";
                }
                case "off", "false", "disable" -> {
                    SystemTelemetry.setEnabled(false);
                    yield "§eSystem telemetry OFF";
                }
                case "flush" -> {
                    SystemTelemetry.flushAndClose();
                    yield "§aSyslog flushed.";
                }
                case "status", "0", "" -> "§7" + SystemTelemetry.statusLine();
                default -> "§cUsage: lm do syslog on|off|status|flush";
            };
        }
        return "§cUnknown hub action: " + act;
    }

    private static String onOff(String v) {
        return "true".equalsIgnoreCase(v) ? "ON" : "OFF";
    }
}
