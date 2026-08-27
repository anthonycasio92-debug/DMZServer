package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.sparring.SparPlayerRuntime;
import com.dbzlegacy.adaptivedifficulty.sparring.SparStore;
import com.dbzlegacy.adaptivedifficulty.sparring.SparringSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Public static API for Bukkit companion reflection ({@code LegacyMechanicsGUI}).
 * Status maps, lore lines, and {@code /spar do} action dispatch without reopening menus —
 * the companion plugin owns inventory reopen.
 */
public final class SparGuiApi {
    private SparGuiApi() {}

    public static Map<String, String> placeholders(ServerPlayer player) {
        Map<String, String> out = new HashMap<>();
        out.put("bridge_ok", "false");
        out.put("system_enabled", "false");
        if (player == null) {
            return out;
        }
        boolean enabled = DifficultyConfig.get().enableSparringSystem;
        out.put("bridge_ok", "true");
        out.put("system_enabled", enabled ? "true" : "false");
        if (!enabled) {
            return out;
        }
        SparPlayerRuntime rt = SparringSystem.runtime(player.m_20148_());
        SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
        boolean sessionActive = rt != null && rt.active;
        out.put("sessionActive", sessionActive ? "true" : "false");
        out.put("session_active", out.get("sessionActive"));
        String partner = "";
        if (sessionActive && rt.partner != null && player.m_20194_() != null) {
            ServerPlayer p = player.m_20194_().m_6846_().m_11259_(rt.partner);
            if (p != null) {
                partner = p.m_7755_().getString();
            }
        }
        out.put("partner", partner);
        out.put("session_tp", sessionActive ? String.valueOf((int) rt.sessionTp) : "0");
        out.put("perfect", sessionActive && rt.sessionPerfect ? "true" : "false");

        boolean bonded = bond != null
                && bond.mentorUuid != null && !bond.mentorUuid.isBlank()
                && bond.apprenticeUuid != null && !bond.apprenticeUuid.isBlank();
        out.put("mentor_bonded", bonded ? "true" : "false");
        if (bonded) {
            boolean isMentor = player.m_20148_().toString().equals(bond.mentorUuid);
            out.put("mentor_role", isMentor ? "mentor" : "apprentice");
            out.put("mentor", isMentor
                    ? (bond.apprenticeName == null ? "" : bond.apprenticeName)
                    : (bond.mentorName == null ? "" : bond.mentorName));
            out.put("mentor_name", bond.mentorName == null ? "" : bond.mentorName);
            out.put("apprentice_name", bond.apprenticeName == null ? "" : bond.apprenticeName);
            out.put("streak", String.valueOf(bond.streakCurrent));
            out.put("streak_best", String.valueOf(bond.streakBest));
        } else {
            out.put("mentor_role", "");
            out.put("mentor", "");
            out.put("mentor_name", "");
            out.put("apprentice_name", "");
            out.put("streak", "0");
            out.put("streak_best", "0");
        }
        return out;
    }

    public static List<String> statsLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableSparringSystem) {
            return List.of("§cSparring system is disabled.");
        }
        return SparringSystem.statsLines(player);
    }

    public static List<String> topLines(ServerPlayer player, String category) {
        if (!DifficultyConfig.get().enableSparringSystem) {
            return List.of("§cSparring system is disabled.");
        }
        return SparringSystem.topLines(category, 10);
    }

    public static List<String> mentorLines(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        lines.add("§8── §bMentor §8──");
        if (player == null || !DifficultyConfig.get().enableSparringSystem) {
            lines.add("§cSparring system is disabled.");
            return lines;
        }
        SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
        if (bond != null && bond.mentorUuid != null && !bond.mentorUuid.isBlank()
                && bond.apprenticeUuid != null && !bond.apprenticeUuid.isBlank()) {
            lines.add("§7Bonded §f" + bond.mentorName + " §8↔ §f" + bond.apprenticeName);
            lines.add("§7Streak §f" + bond.streakCurrent + " §8best §f" + bond.streakBest);
        } else {
            lines.add("§7Click Invite / Ask below to pick a player");
            lines.add(SparringSystem.bondStatus(player));
        }
        lines.add("§8Accept · Decline · Remove below");
        return lines;
    }

    /** Lore lines for a GUI page. */
    public static List<String> linesForPage(ServerPlayer player, String page) {
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("top_") || lower.startsWith("top ")) {
            String cat = lower.startsWith("top_")
                    ? lower.substring(4).trim()
                    : lower.substring(4).trim();
            if (cat.isBlank()) {
                cat = "tp";
            }
            return topLines(player, cat);
        }
        return switch (lower) {
            case "stats", "statistics" -> statsLines(player);
            case "top", "leaderboard" -> topLines(player, "tp");
            case "mentor" -> mentorLines(player);
            case "help" -> List.of(
                    "§6§l/spar §8— Sparring TP",
                    "§7Open GUI for mentor invites",
                    "§e/spar stats|end|top [category]",
                    "§e/spar mentor <player>|accept|decline|remove",
                    "§e/spar apprentice <player>"
            );
            default -> {
                Map<String, String> ph = placeholders(player);
                List<String> lore = new ArrayList<>();
                if ("true".equalsIgnoreCase(ph.get("sessionActive"))) {
                    lore.add("§aSession ACTIVE §8with §f" + blank(ph.get("partner"), "?")
                            + "  §7TP §f" + ph.getOrDefault("session_tp", "0"));
                    if ("true".equalsIgnoreCase(ph.get("perfect"))) {
                        lore.add("§6§lPERFECT TRAINING");
                    }
                } else {
                    lore.add("§7No active spar — trade hits within 30 blocks to start.");
                }
                if ("true".equalsIgnoreCase(ph.get("mentor_bonded"))) {
                    lore.add("§bMentor bond §7as §f" + ph.getOrDefault("mentor_role", "?")
                            + " §8with §f" + blank(ph.get("mentor"), "?")
                            + "  §7streak §f" + ph.getOrDefault("streak", "0"));
                } else {
                    lore.add("§7No mentor bond. §8Use Mentor page to invite");
                }
                yield lore;
            }
        };
    }

    /**
     * Dispatch {@code /spar do} actions. Does not reopen GUI — caller reopens.
     *
     * @param action e.g. {@code page}, {@code end}, {@code mentor}
     * @param arg    e.g. page name, {@code accept}/{@code decline}/{@code remove}
     * @param page   return page (unused for page action; used by caller to reopen)
     */
    public static String handleDo(ServerPlayer player, String action, String arg, String page) {
        if (player == null) {
            return "§cPlayers only.";
        }
        if (!DifficultyConfig.get().enableSparringSystem) {
            return "§cSparring system is disabled.";
        }
        String act = action == null ? "" : action.toLowerCase(Locale.ROOT).trim();
        String a = arg == null ? "" : arg.trim();
        if ("page".equals(act) || "refresh".equals(act)) {
            return "";
        }
        if ("end".equals(act) || "stop".equals(act)) {
            return SparringSystem.endCommand(player);
        }
        if ("mentor".equals(act)) {
            String sub = a.toLowerCase(Locale.ROOT);
            return switch (sub) {
                case "accept" -> SparringSystem.mentorAccept(player);
                case "decline", "deny" -> SparringSystem.mentorDecline(player);
                case "remove", "clear" -> SparringSystem.removeMentor(player);
                default -> "§cUsage: spar do mentor accept|decline|remove";
            };
        }
        if ("mentor_invite".equals(act) || "mentorinvite".equals(act)) {
            if (a.isBlank()) {
                return "§cPick a player to invite as apprentice.";
            }
            ServerPlayer target = resolveOnline(player, a);
            if (target == null) {
                return "§cPlayer not online: " + a;
            }
            return SparringSystem.mentorInvite(player, target);
        }
        if ("apprentice_invite".equals(act) || "apprenticeinvite".equals(act)) {
            if (a.isBlank()) {
                return "§cPick a player to ask as mentor.";
            }
            ServerPlayer target = resolveOnline(player, a);
            if (target == null) {
                return "§cPlayer not online: " + a;
            }
            return SparringSystem.apprenticeInvite(player, target);
        }
        return "§cUnknown spar action: " + act;
    }

    /**
     * Resolve an online player by {@code uuid:&lt;uuid&gt;} or by exact / case-insensitive name
     * from the caller's server.
     */
    public static ServerPlayer resolveOnline(ServerPlayer from, String name) {
        if (from == null || name == null || name.isBlank()) {
            return null;
        }
        MinecraftServer server = from.m_20194_();
        if (server == null) {
            return null;
        }
        String raw = name.trim();
        if (raw.regionMatches(true, 0, "uuid:", 0, 5)) {
            String id = raw.substring(5).trim();
            if (id.isBlank()) {
                return null;
            }
            try {
                return server.m_6846_().m_11259_(java.util.UUID.fromString(id));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        ServerPlayer exact = server.m_6846_().m_11255_(raw);
        if (exact != null) {
            return exact;
        }
        String want = raw.toLowerCase(Locale.ROOT);
        for (ServerPlayer p : server.m_6846_().m_11314_()) {
            if (p.m_7755_().getString().toLowerCase(Locale.ROOT).equals(want)) {
                return p;
            }
        }
        return null;
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
