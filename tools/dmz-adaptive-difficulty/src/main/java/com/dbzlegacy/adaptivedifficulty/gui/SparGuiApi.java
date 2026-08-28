package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.sparring.SparPlayerRuntime;
import com.dbzlegacy.adaptivedifficulty.sparring.SparStore;
import com.dbzlegacy.adaptivedifficulty.sparring.SparringSystem;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
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

        boolean hasMentor = bond != null
                && bond.mentorUuid != null && !bond.mentorUuid.isBlank();
        boolean hasApprentice = bond != null
                && bond.apprenticeUuid != null && !bond.apprenticeUuid.isBlank();
        boolean bonded = hasMentor || hasApprentice;
        out.put("has_mentor", hasMentor ? "true" : "false");
        out.put("has_apprentice", hasApprentice ? "true" : "false");
        out.put("mentor_bonded", bonded ? "true" : "false");
        out.put("mentor_name", hasMentor && bond.mentorName != null ? bond.mentorName : "");
        out.put("apprentice_name", hasApprentice && bond.apprenticeName != null ? bond.apprenticeName : "");
        if (hasMentor && hasApprentice) {
            out.put("mentor_role", "both");
            out.put("mentor", blank(bond.mentorName, "?") + " / " + blank(bond.apprenticeName, "?"));
            out.put("streak", String.valueOf(bond.streakCurrent));
            out.put("streak_best", String.valueOf(bond.streakBest));
        } else if (hasApprentice) {
            out.put("mentor_role", "mentor");
            out.put("mentor", bond.apprenticeName == null ? "" : bond.apprenticeName);
            out.put("streak", String.valueOf(bond.streakCurrent));
            out.put("streak_best", String.valueOf(bond.streakBest));
        } else if (hasMentor) {
            out.put("mentor_role", "apprentice");
            out.put("mentor", bond.mentorName == null ? "" : bond.mentorName);
            out.put("streak", String.valueOf(bond.streakCurrent));
            out.put("streak_best", String.valueOf(bond.streakBest));
        } else {
            out.put("mentor_role", "");
            out.put("mentor", "");
            out.put("streak", "0");
            out.put("streak_best", "0");
        }
        int pending = SparringSystem.pendingMentorInviteCount(player);
        out.put("pending_invites", String.valueOf(pending));
        out.put("pendingInvites", out.get("pending_invites"));
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
        boolean hasMentor = bond != null
                && bond.mentorUuid != null && !bond.mentorUuid.isBlank();
        boolean hasApprentice = bond != null
                && bond.apprenticeUuid != null && !bond.apprenticeUuid.isBlank();
        if (hasMentor || hasApprentice) {
            if (hasMentor) {
                lines.add("§7Your mentor §f" + blank(bond.mentorName, "?"));
            }
            if (hasApprentice) {
                lines.add("§7Your apprentice §f" + blank(bond.apprenticeName, "?"));
            }
            lines.add("§7Streak §f" + bond.streakCurrent + " §8best §f" + bond.streakBest);
        } else {
            lines.add("§7Invite apprentice or ask a mentor below");
            lines.add(SparringSystem.bondStatus(player));
        }
        int pending = SparringSystem.pendingMentorInviteCount(player);
        if (pending > 0) {
            lines.add("§ePending invites §f" + pending + " §8— open Pending");
        } else {
            lines.add("§8Pending · Accept · Decline · Leave / Release");
        }
        return lines;
    }

    /** Encoded pending mentor invites (IN + OUT) for head boards. */
    public static List<String> pendingMentorInviteCards(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableSparringSystem) {
            return List.of();
        }
        return SparringSystem.pendingMentorInviteCards(player);
    }

    /** Incoming mentor invite args for Accept/Decline pickers. */
    public static List<String> pendingIncomingMentorArgs(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableSparringSystem) {
            return List.of();
        }
        return SparringSystem.pendingIncomingMentorArgs(player);
    }

    public static List<String> pendingMentorLines(ServerPlayer player) {
        List<String> cards = pendingMentorInviteCards(player);
        List<String> lines = new ArrayList<>();
        if (cards.isEmpty()) {
            lines.add("§7No pending mentor invites.");
            lines.add("§8Outgoing: you invited someone.");
            lines.add("§8Incoming: they invited you — Accept or Decline.");
            return lines;
        }
        lines.add("§e§lPending Mentor Invites");
        for (String card : cards) {
            String[] p = card.split("\t", -1);
            if (p.length < 3) {
                continue;
            }
            String name = p[1];
            String dir = p[2];
            boolean online = p.length > 4 && "1".equals(p[4]);
            String kind = p.length > 5 ? p[5] : "";
            String role = "mentor".equalsIgnoreCase(kind) ? "Mentor" : "Apprentice";
            if ("IN".equals(dir)) {
                lines.add("§a◀ Incoming §f" + name + (online ? " §a●" : " §8○")
                        + " §8→ you as " + role);
            } else {
                lines.add("§6▶ Outgoing §f" + name + (online ? " §a●" : " §8○")
                        + " §8→ them as " + role);
            }
        }
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
            case "pending", "invites", "pendinginvites" -> pendingMentorLines(player);
            case "help" -> List.of(
                    "§6§l/spar §8— Sparring TP",
                    "§7Open GUI for mentor invites",
                    "§e/spar stats|end|top [category]",
                    "§e/spar mentor <player>|accept|decline|leave",
                    "§e/spar apprentice <player>|release"
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
                    String role = ph.getOrDefault("mentor_role", "?");
                    if ("both".equalsIgnoreCase(role)) {
                        lore.add("§bMentor §f" + blank(ph.get("mentor_name"), "?")
                                + " §8· §bApprentice §f" + blank(ph.get("apprentice_name"), "?")
                                + "  §7streak §f" + ph.getOrDefault("streak", "0"));
                    } else if ("mentor".equalsIgnoreCase(role)) {
                        lore.add("§bMentoring §f" + blank(ph.get("apprentice_name"), "?")
                                + "  §7streak §f" + ph.getOrDefault("streak", "0"));
                    } else {
                        lore.add("§bApprentice of §f" + blank(ph.get("mentor_name"), "?")
                                + "  §7streak §f" + ph.getOrDefault("streak", "0"));
                    }
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
        String act = action == null ? "" : action.toLowerCase(Locale.ROOT).trim();
        String a = arg == null ? "" : arg.trim();
        if ("admin".equals(act)) {
            return handleAdmin(player, a);
        }
        if (!DifficultyConfig.get().enableSparringSystem) {
            return "§cSparring system is disabled.";
        }
        if ("page".equals(act) || "refresh".equals(act)) {
            return "";
        }
        if ("end".equals(act) || "stop".equals(act)) {
            return SparringSystem.endCommand(player);
        }
        if ("mentor".equals(act)) {
            String sub = a.toLowerCase(Locale.ROOT).trim();
            if (sub.equals("accept") || sub.startsWith("accept ") || sub.startsWith("accept:")) {
                String from = "";
                if (sub.startsWith("accept")) {
                    from = a.substring(6).trim();
                    if (from.startsWith(":")) {
                        from = from.substring(1).trim();
                    }
                }
                return SparringSystem.mentorAccept(player, from.isBlank() ? null : from);
            }
            if (sub.equals("decline") || sub.equals("deny")
                    || sub.startsWith("decline ") || sub.startsWith("decline:")
                    || sub.startsWith("deny ") || sub.startsWith("deny:")) {
                String prefix = sub.startsWith("deny") ? "deny" : "decline";
                String from = a.length() > prefix.length() ? a.substring(prefix.length()).trim() : "";
                if (from.startsWith(":")) {
                    from = from.substring(1).trim();
                }
                return SparringSystem.mentorDecline(player, from.isBlank() ? null : from);
            }
            if (sub.equals("cancel") || sub.startsWith("cancel ") || sub.startsWith("cancel:")) {
                String target = a.length() > 6 ? a.substring(6).trim() : "";
                if (target.startsWith(":")) {
                    target = target.substring(1).trim();
                }
                return SparringSystem.mentorCancelInvite(player, target);
            }
            if (sub.equals("remove") || sub.equals("clear")) {
                // Ambiguous — ask which side. Prefer explicit leave/release from GUI.
                SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
                boolean hasMentor = bond != null
                        && bond.mentorUuid != null && !bond.mentorUuid.isBlank();
                boolean hasApprentice = bond != null
                        && bond.apprenticeUuid != null && !bond.apprenticeUuid.isBlank();
                if (hasMentor && hasApprentice) {
                    return "§eChoose: §fLeave mentor §8or §fRelease apprentice"
                            + "\n§8GUI: Mentor → Leave / Release · Commands: /spar mentor remove · /spar apprentice remove";
                }
                return SparringSystem.removeBond(player);
            }
            if (sub.equals("leave") || sub.equals("leavementor") || sub.equals("leave_mentor")
                    || sub.equals("remove_mentor") || sub.equals("removementor")) {
                return SparringSystem.removeMentor(player);
            }
            if (sub.equals("release") || sub.equals("releaseapprentice") || sub.equals("release_apprentice")
                    || sub.equals("remove_apprentice") || sub.equals("removeapprentice")) {
                return SparringSystem.removeApprentice(player);
            }
            return "§cUsage: spar do mentor accept|decline|cancel|leave|release [player]";
        }
        if ("mentor_leave".equals(act) || "mentorleave".equals(act) || "leave_mentor".equals(act)) {
            return SparringSystem.removeMentor(player);
        }
        if ("mentor_release".equals(act) || "mentorrelease".equals(act)
                || "release_apprentice".equals(act) || "apprentice_remove".equals(act)) {
            return SparringSystem.removeApprentice(player);
        }
        if ("mentor_accept".equals(act) || "mentoraccept".equals(act)) {
            return SparringSystem.mentorAccept(player, a.isBlank() ? null : a);
        }
        if ("mentor_decline".equals(act) || "mentordecline".equals(act) || "mentor_deny".equals(act)) {
            return SparringSystem.mentorDecline(player, a.isBlank() ? null : a);
        }
        if ("mentor_cancel".equals(act) || "mentorcancel".equals(act)) {
            return SparringSystem.mentorCancelInvite(player, a);
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

    /** Staff: save / status / mentor resetcd — used by GUI + {@code /spar admin …}. */
    public static String handleAdmin(ServerPlayer player, String arg) {
        if (!StaffAccess.isStaff(player)) {
            return "§cStaff only.";
        }
        String raw = arg == null ? "" : arg.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("save")) {
            SparStore.get().markDirty();
            SparStore.get().save();
            return "§aSpar store saved.";
        }
        if (lower.startsWith("status")) {
            boolean on = DifficultyConfig.get().enableSparringSystem;
            return "§6Spar admin status\n"
                    + "§7enabled §f" + (on ? "ON" : "OFF") + "\n"
                    + "§7bonds §f" + SparStore.get().bondsByPlayer.size() + "\n"
                    + "§8" + SparStore.path();
        }
        if (lower.startsWith("resetcd") || lower.startsWith("mentor") || lower.contains("resetcd")) {
            String targetArg = raw;
            if (lower.startsWith("mentor")) {
                targetArg = raw.replaceFirst("(?i)^mentor[_\\s]*resetcd\\s*", "").trim();
            } else if (lower.startsWith("resetcd")) {
                targetArg = raw.replaceFirst("(?i)^resetcd\\s*:?\\s*", "").trim();
            }
            ServerPlayer target = player;
            if (targetArg != null && !targetArg.isBlank()
                    && !targetArg.equalsIgnoreCase("0")
                    && !targetArg.equalsIgnoreCase("self")) {
                ServerPlayer found = resolveOnline(player, targetArg);
                if (found == null) {
                    return "§cPlayer not online: " + targetArg;
                }
                target = found;
            }
            return SparringSystem.resetMentorCd(player, target);
        }
        return "§6§l/spar admin\n"
                + "§e/spar admin save §7— write sparring.json\n"
                + "§e/spar admin status §7— enabled + path\n"
                + "§e/spar admin mentor resetcd [player] §7— clear mentor cooldown\n"
                + "§8GUI buttons call these directly (no Forge perm-level gate).";
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
