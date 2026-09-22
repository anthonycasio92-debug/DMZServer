package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.calc.LmOverhaulScaledCombat;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.rival.RivalChallengeManager;
import com.dbzlegacy.adaptivedifficulty.rival.RivalConstants;
import com.dbzlegacy.adaptivedifficulty.rival.RivalInstinct;
import com.dbzlegacy.adaptivedifficulty.rival.RivalLink;
import com.dbzlegacy.adaptivedifficulty.rival.RivalPlayerRecord;
import com.dbzlegacy.adaptivedifficulty.rival.RivalProgression;
import com.dbzlegacy.adaptivedifficulty.rival.RivalSpectator;
import com.dbzlegacy.adaptivedifficulty.rival.RivalStore;
import com.dbzlegacy.adaptivedifficulty.rival.RivalSystem;
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
 * Status maps, lore lines, and {@code /rival do} action dispatch without reopening menus —
 * the companion plugin owns inventory reopen.
 */
public final class RivalGuiApi {
    private RivalGuiApi() {}

    public static Map<String, String> placeholders(ServerPlayer player) {
        Map<String, String> out = new HashMap<>();
        out.put("bridge_ok", "false");
        out.put("system_enabled", "false");
        if (player == null) {
            return out;
        }
        boolean enabled = DifficultyConfig.get().enableRivalSystem;
        out.put("bridge_ok", "true");
        out.put("system_enabled", enabled ? "true" : "false");
        if (!enabled) {
            return out;
        }
        RivalPlayerRecord me = RivalStore.get().ensurePlayer(player);
        RivalConstants.RpTier tier = RivalConstants.tierFor(me.totalRp);
        out.put("rp", String.valueOf((int) me.totalRp));
        out.put("tier", tier.name());
        out.put("tier_color", String.valueOf(tier.color()));
        out.put("mutual", String.valueOf(me.countMutual()));
        out.put("mutual_max", String.valueOf(RivalConstants.MAX_MUTUAL_RIVALS));
        out.put("wins", String.valueOf(me.officialWins));
        out.put("losses", String.valueOf(me.officialLosses));
        out.put("draws", String.valueOf(me.officialDraws));
        out.put("tpMsg", me.tpMessages ? "true" : "false");
        out.put("tp_msg", me.tpMessages ? "true" : "false");
        out.put("instinct", RivalInstinct.isEnabled(player) ? "true" : "false");
        out.put("instinct_feature", DifficultyConfig.get().rivalInstinct ? "true" : "false");
        out.put("challengeActive",
                RivalChallengeManager.get().isInChallenge(player.m_20148_()) ? "true" : "false");
        out.put("challenge_active", out.get("challengeActive"));
        out.put("name", me.name == null ? "" : me.name);
        int pendingCount = 0;
        long now = System.currentTimeMillis();
        if (me.rivals != null) {
            for (RivalLink link : me.rivals.values()) {
                if (link == null || link.mutual) {
                    continue;
                }
                boolean mutualConfirm = link.needsMutualConfirm();
                if (!link.inviteSent && !link.inviteReceived && !mutualConfirm) {
                    continue;
                }
                if (link.pendingExpireAt > 0L && now > link.pendingExpireAt) {
                    continue;
                }
                pendingCount++;
            }
        }
        out.put("pending_invites", String.valueOf(pendingCount));
        out.put("pendingInvites", String.valueOf(pendingCount));
        int pendingChallenges = pendingChallengeCards(player).size();
        out.put("pending_challenges", String.valueOf(pendingChallenges));
        out.put("pendingChallenges", String.valueOf(pendingChallenges));
        out.put("pending_mutual_accept",
                me.pendingMutualAcceptUuid == null ? "" : me.pendingMutualAcceptUuid.trim());
        out.put("pending_mutual_accept_name", RivalSystem.pendingMutualAcceptName(player));
        out.put("needs_mutual_replace", RivalSystem.needsMutualReplacePick(player) ? "true" : "false");
        LmOverhaulScaledCombat.putPlaceholders(out, player);
        return out;
    }

    public static List<String> listLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalSystem.listLines(player);
    }

    /** Encoded current-rival cards for inventory GUIs (see RivalSystem.currentRivalCards). */
    public static List<String> currentRivalCards(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of();
        }
        return RivalSystem.currentRivalCards(player);
    }

    /** Encoded past-rival cards for history board. */
    public static List<String> pastRivalCards(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of();
        }
        return RivalSystem.pastRivalCards(player);
    }

    /** True when Accept is waiting for the player to pick which Mutual to replace. */
    public static boolean needsMutualReplacePick(ServerPlayer player) {
        return RivalSystem.needsMutualReplacePick(player);
    }

    /** Encoded pending declare invites (incoming + outgoing). */
    public static List<String> pendingInviteCards(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of();
        }
        return RivalSystem.pendingInviteCards(player);
    }

    /**
     * Current-rival picker args ({@code uuid:&lt;uuid&gt;} preferred) for remove —
     * only existing rivals, not all online players.
     */
    public static List<String> currentRivalArgs(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        for (String card : currentRivalCards(player)) {
            if (card == null || card.isBlank()) {
                continue;
            }
            String[] p = card.split("\t", -1);
            if (p.length < 2) {
                continue;
            }
            String uuid = p[0] == null ? "" : p[0].trim();
            String name = p[1] == null ? "" : p[1].trim();
            if (!uuid.isBlank()) {
                out.add("uuid:" + uuid);
            } else if (!name.isBlank()) {
                out.add(name);
            }
        }
        return out;
    }

    public static List<String> statsLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalSystem.statsLines(player);
    }

    public static List<String> topLines(ServerPlayer player) {
        if (!DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalSystem.topLines(10);
    }

    public static List<String> challengeLines(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        lines.add("§8── §cChallenge §8──");
        lines.add("§7Send a duel · respond from §6Pending");
        if (player != null && RivalChallengeManager.get().isInChallenge(player.m_20148_())) {
            lines.add("§eChallenge active");
        } else {
            lines.add("§7No active challenge.");
        }
        int pending = player == null ? 0 : pendingChallengeCards(player).size();
        if (pending > 0) {
            lines.add("§6" + pending + " pending request" + (pending == 1 ? "" : "s"));
        }
        return lines;
    }

    /**
     * Pending duel requests (incoming + outgoing). Tab fields:
     * uuid, name, IN|OUT, expiresAtMs, online(0/1), kind=challenge, minutes.
     */
    public static List<String> pendingChallengeCards(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        if (player == null || !DifficultyConfig.get().enableRivalSystem
                || !DifficultyConfig.get().rivalChallenges) {
            return out;
        }
        RivalChallengeManager.ChallengeRequest req =
                RivalChallengeManager.get().getRequestInvolving(player.m_20148_());
        if (req == null) {
            return out;
        }
        java.util.UUID self = player.m_20148_();
        boolean incoming = self.equals(req.to);
        java.util.UUID other = incoming ? req.from : req.to;
        String otherName = incoming ? req.fromName : req.toName;
        if (otherName == null || otherName.isBlank()) {
            otherName = other == null ? "?" : other.toString();
        }
        otherName = otherName.replace('\t', ' ').replace('\n', ' ');
        String uuid = other == null ? "" : other.toString();
        boolean online = false;
        MinecraftServer server = player.m_20194_();
        if (server != null && other != null) {
            online = server.m_6846_().m_11259_(other) != null;
        }
        int minutes = (int) Math.max(1L, req.durationMs / 60_000L);
        out.add(String.join("\t",
                uuid,
                otherName,
                incoming ? "IN" : "OUT",
                String.valueOf(Math.max(0L, req.expiresAt)),
                online ? "1" : "0",
                "challenge",
                String.valueOf(minutes)));
        return out;
    }

    public static List<String> pendingChallengeLines(ServerPlayer player) {
        List<String> cards = pendingChallengeCards(player);
        List<String> lines = new ArrayList<>();
        if (cards.isEmpty()) {
            lines.add("§7No pending challenge requests.");
            lines.add("§8Incoming: Accept or Decline.");
            lines.add("§8Outgoing: Cancel your send.");
            return lines;
        }
        lines.add("§e§lPending Challenges");
        for (String card : cards) {
            String[] p = card.split("\t", -1);
            if (p.length < 3) {
                continue;
            }
            String name = p[1];
            String dir = p[2];
            boolean online = p.length > 4 && "1".equals(p[4]);
            String mins = p.length > 6 ? p[6] : "?";
            if ("IN".equals(dir)) {
                lines.add("§a◀ Incoming §f" + name + (online ? " §a●" : " §8○")
                        + " §8· §f" + mins + " min §8— Accept or Decline");
            } else {
                lines.add("§6▶ Outgoing §f" + name + (online ? " §a●" : " §8○")
                        + " §8· §f" + mins + " min §8— Cancel send");
            }
        }
        return lines;
    }

    public static List<String> pendingChallengeDetailLines(String card) {
        if (card == null || card.isBlank()) {
            return List.of("§7Unknown challenge request.");
        }
        String[] p = card.split("\t", -1);
        String name = p.length > 1 && p[1] != null && !p[1].isBlank() ? p[1] : "?";
        String dir = p.length > 2 ? p[2] : "?";
        boolean online = p.length > 4 && "1".equals(p[4]);
        String mins = p.length > 6 ? p[6] : "?";
        List<String> lines = new ArrayList<>();
        lines.add("§f" + name + (online ? " §a●" : " §8○"));
        lines.add("§7Length §f" + mins + " min");
        if ("IN".equalsIgnoreCase(dir)) {
            lines.add("§aIncoming duel challenge");
            lines.add("§7Accept to start countdown · Decline to refuse.");
        } else {
            lines.add("§6Outgoing duel challenge");
            lines.add("§7Waiting for them · Cancel to withdraw.");
        }
        return lines;
    }

    public static boolean pendingChallengeCardIncoming(String card) {
        if (card == null) {
            return false;
        }
        String[] p = card.split("\t", -1);
        return p.length >= 3 && "IN".equalsIgnoreCase(p[2]);
    }

    /** Online player names for GUI head pickers (excludes {@code exclude} when non-null). */
    public static List<String> onlinePlayerNames(ServerPlayer exclude) {
        List<String> names = new ArrayList<>();
        MinecraftServer server = exclude == null ? null : exclude.m_20194_();
        if (server == null) {
            return names;
        }
        for (ServerPlayer p : server.m_6846_().m_11314_()) {
            if (p == null) {
                continue;
            }
            if (exclude != null && p.m_20148_().equals(exclude.m_20148_())) {
                continue;
            }
            String name = p.m_7755_().getString();
            if (name != null && !name.isBlank()) {
                names.add(name);
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /**
     * Names of players with a pending incoming declare ({@code inviteReceived}) on this player.
     * Online declarers are listed first; offline still included (accept/decline use name lookup).
     */
    public static List<String> pendingIncomingDeclareNames(ServerPlayer player) {
        return collectIncomingNames(player, false);
    }

    /**
     * Accept candidates: pending incoming declare or Declared (both Silent).
     * Online first; offline still included.
     */
    public static List<String> acceptCandidateNames(ServerPlayer player) {
        return collectIncomingNames(player, true);
    }

    private static List<String> collectIncomingNames(ServerPlayer player, boolean includeDeclared) {
        List<String> online = new ArrayList<>();
        List<String> offline = new ArrayList<>();
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return online;
        }
        RivalPlayerRecord me = RivalStore.get().ensurePlayer(player);
        if (me == null || me.rivals.isEmpty()) {
            return online;
        }
        MinecraftServer server = player.m_20194_();
        long now = System.currentTimeMillis();
        for (RivalLink link : me.rivals.values()) {
            if (link == null || !matchesIncoming(link, now, includeDeclared)) {
                continue;
            }
            String name = link.name == null || link.name.isBlank() ? link.uuid : link.name;
            if (name == null || name.isBlank()) {
                continue;
            }
            boolean isOnline = false;
            if (server != null && link.uuid != null && !link.uuid.isBlank()) {
                try {
                    isOnline = server.m_6846_().m_11259_(java.util.UUID.fromString(link.uuid)) != null;
                } catch (IllegalArgumentException ignored) {
                    isOnline = false;
                }
            }
            if (isOnline) {
                online.add(name);
            } else {
                offline.add(name);
            }
        }
        online.sort(String.CASE_INSENSITIVE_ORDER);
        offline.sort(String.CASE_INSENSITIVE_ORDER);
        List<String> out = new ArrayList<>(online.size() + offline.size());
        out.addAll(online);
        out.addAll(offline);
        return out;
    }

    /** Invite-only pending, or also Declared Mutual-confirm when {@code includeDeclared}. */
    private static boolean matchesIncoming(RivalLink link, long now, boolean includeDeclared) {
        if (link.mutual) {
            return false;
        }
        if (link.inviteReceived) {
            return link.pendingExpireAt <= 0L || now <= link.pendingExpireAt;
        }
        if (includeDeclared && link.needsMutualConfirm()) {
            return link.pendingExpireAt <= 0L || now <= link.pendingExpireAt;
        }
        if (!includeDeclared) {
            return false;
        }
        return link.declaredByMe && link.declaredByThem
                && !link.inviteSent && !link.inviteReceived;
    }

    /**
     * Pending incoming declare picker args: {@code uuid:&lt;uuid&gt;} when online, else stored name
     * (decline uses this — invite only). Online first.
     */
    public static List<String> pendingIncomingDeclareArgs(ServerPlayer player) {
        return collectIncomingArgs(player, false);
    }

    /**
     * Accept picker args: pending invite or Declared (both Silent).
     * {@code uuid:&lt;uuid&gt;} when online, else stored name. Online first.
     */
    public static List<String> acceptCandidateArgs(ServerPlayer player) {
        return collectIncomingArgs(player, true);
    }

    private static List<String> collectIncomingArgs(ServerPlayer player, boolean includeDeclared) {
        List<String> online = new ArrayList<>();
        List<String> offline = new ArrayList<>();
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return online;
        }
        RivalPlayerRecord me = RivalStore.get().ensurePlayer(player);
        if (me == null || me.rivals.isEmpty()) {
            return online;
        }
        MinecraftServer server = player.m_20194_();
        long now = System.currentTimeMillis();
        for (RivalLink link : me.rivals.values()) {
            if (link == null || !matchesIncoming(link, now, includeDeclared)) {
                continue;
            }
            String name = link.name == null || link.name.isBlank() ? "" : link.name;
            String uuid = link.uuid == null ? "" : link.uuid.trim();
            boolean isOnline = false;
            if (server != null && !uuid.isBlank()) {
                try {
                    isOnline = server.m_6846_().m_11259_(java.util.UUID.fromString(uuid)) != null;
                } catch (IllegalArgumentException ignored) {
                    isOnline = false;
                }
            }
            if (isOnline) {
                online.add("uuid:" + uuid);
            } else if (!name.isBlank()) {
                offline.add(name);
            } else if (!uuid.isBlank()) {
                offline.add(uuid);
            }
        }
        online.sort(String.CASE_INSENSITIVE_ORDER);
        offline.sort(String.CASE_INSENSITIVE_ORDER);
        List<String> out = new ArrayList<>(online.size() + offline.size());
        out.addAll(online);
        out.addAll(offline);
        return out;
    }

    /**
     * Resolve an online {@link ServerPlayer} by {@code uuid:&lt;uuid&gt;} or by exact /
     * case-insensitive name.
     */
    public static ServerPlayer resolveOnline(MinecraftServer server, String arg) {
        if (server == null || arg == null || arg.isBlank()) {
            return null;
        }
        String raw = arg.trim();
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
        return RivalSystem.findOnline(server, raw);
    }

    public static ServerPlayer resolveOnline(ServerPlayer from, String name) {
        if (from == null) {
            return null;
        }
        return resolveOnline(from.m_20194_(), name);
    }

    /** Resolve GUI arg to a stored/display name (supports {@code uuid:} for accept/decline/remove). */
    public static String resolveNameArg(ServerPlayer from, String arg) {
        if (arg == null || arg.isBlank()) {
            return "";
        }
        String raw = arg.trim();
        if (raw.regionMatches(true, 0, "uuid:", 0, 5)) {
            ServerPlayer online = resolveOnline(from, raw);
            if (online != null) {
                return online.m_7755_().getString();
            }
            String id = raw.substring(5).trim();
            RivalPlayerRecord rec = RivalStore.get().get(id);
            if (rec != null && rec.name != null && !rec.name.isBlank()) {
                return rec.name;
            }
            return "";
        }
        return raw;
    }

    public static List<String> seasonLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalProgression.get().seasonLines(player);
    }

    public static List<String> questLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalProgression.get().questLines(player);
    }

    public static List<String> achievementLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalProgression.get().achievementLines(player);
    }

    public static List<String> hofLines(ServerPlayer player) {
        if (!DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalProgression.get().hofLines();
    }

    public static List<String> journalLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalProgression.get().journalLines(player);
    }

    public static List<String> titleLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalProgression.get().titleLines(player);
    }

    /** Lore lines for a GUI page (main status is placeholders — use page-specific lists). */
    public static List<String> linesForPage(ServerPlayer player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        return switch (p) {
            case "list" -> listLines(player);
            case "history", "past", "previous" -> RivalSystem.historyLines(player);
            case "pending", "invites", "pendinginvites" -> RivalSystem.pendingInviteLines(player);
            case "actions" -> List.of(
                    "§6§lRival Actions",
                    "§7Silent → only you see them",
                    "§7Declare → on your list as Declared; they get Pending",
                    "§7They Accept → Mutual (Decline/ignore: you keep Declared)",
                    "§7Both Silent → Declared → both Accept (Pending) → Mutual"
            );
            case "stats", "statistics" -> statsLines(player);
            case "challenge", "challenges" -> challengeLines(player);
            case "challenge_pending", "challengepending", "challenge_invites" -> pendingChallengeLines(player);
            case "top", "leaderboard" -> topLines(player);
            case "progress" -> List.of(
                    "§b§lRival Progress",
                    "§7Season · Quests · Achievements",
                    "§7Hall of Fame · Journal · Title",
                    "§8Open a page from the Progress menu"
            );
            case "season" -> seasonLines(player);
            case "quests", "quest" -> questLines(player);
            case "achievements", "achs", "ach" -> achievementLines(player);
            case "hof", "hall" -> hofLines(player);
            case "journal" -> journalLines(player);
            case "title", "titles" -> titleLines(player);
            case "help" -> List.of(
                    "§6§l/rival §8— Rival System",
                    "§7Prefer §e/lm §7→ Rival from the hub",
                    "§7GUI: List · Challenge · Top · Progress",
                    "§e/rival <player> §7silent rival",
                    "§e/rival declare|accept|decline|remove <player>",
                    "§e/rival challenge send <player> [minutes]",
                    "§7GUI: pick rival → choose 1–10 minutes",
                    "§e/rival spectate [player]|stop",
                    "§e/rival admin save|refresh|status (staff)"
            );
            default -> {
                Map<String, String> ph = placeholders(player);
                List<String> lore = new ArrayList<>();
                lore.add("§7RP §f" + ph.getOrDefault("rp", "0")
                        + " §8(§" + ph.getOrDefault("tier_color", "7")
                        + ph.getOrDefault("tier", "?") + "§8)");
                lore.add("§7Mutual §f" + ph.getOrDefault("mutual", "0")
                        + "§8/§f" + ph.getOrDefault("mutual_max",
                        String.valueOf(RivalConstants.MAX_MUTUAL_RIVALS)));
                lore.add("§7Record §a" + ph.getOrDefault("wins", "0")
                        + "§7/§c" + ph.getOrDefault("losses", "0")
                        + "§7/§e" + ph.getOrDefault("draws", "0"));
                lore.add("§7TP msg §f"
                        + ("true".equalsIgnoreCase(ph.get("tpMsg")) ? "ON" : "OFF"));
                if ("true".equalsIgnoreCase(ph.get("challengeActive"))) {
                    lore.add("§eChallenge active");
                }
                yield lore;
            }
        };
    }

    /**
     * Dispatch {@code /rival do} actions. Does not reopen GUI — caller reopens.
     *
     * @param action e.g. {@code page}, {@code tpmsg}, {@code instinct}, {@code challenge}
     * @param arg    e.g. page name, {@code toggle}, {@code accept}/{@code decline}/{@code cancel}
     * @param page   return page (unused for page action; used by caller to reopen)
     */
    public static String handleDo(ServerPlayer player, String action, String arg, String page) {
        if (player == null) {
            return "§cPlayers only.";
        }
        String act = action == null ? "" : action.toLowerCase(Locale.ROOT).trim();
        String a = arg == null ? "" : arg.trim();
        // Staff admin works even when the rival system flag is off (save/status/reload).
        if ("admin".equals(act)) {
            return handleAdmin(player, a);
        }
        if (!DifficultyConfig.get().enableRivalSystem) {
            return "§cRival system is disabled.";
        }
        if ("page".equals(act) || "refresh".equals(act)) {
            return "";
        }
        if ("tpmsg".equals(act) || "tp_msg".equals(act)) {
            if ("toggle".equalsIgnoreCase(a) || a.isBlank()) {
                RivalPlayerRecord me = RivalStore.get().ensurePlayer(player);
                boolean next = me == null || !me.tpMessages;
                return RivalSystem.setTpMsg(player, next);
            }
            if ("on".equalsIgnoreCase(a) || "true".equalsIgnoreCase(a)) {
                return RivalSystem.setTpMsg(player, true);
            }
            if ("off".equalsIgnoreCase(a) || "false".equalsIgnoreCase(a)) {
                return RivalSystem.setTpMsg(player, false);
            }
            return "§cUsage: rival do tpmsg toggle";
        }
        if ("instinct".equals(act)) {
            if (!DifficultyConfig.get().rivalInstinct) {
                return "§cRival Instinct is disabled.";
            }
            boolean on = RivalInstinct.toggle(player);
            return "§aRival Instinct §f" + (on ? "ON" : "OFF");
        }
        if ("challenge".equals(act)) {
            String sub = a.toLowerCase(Locale.ROOT);
            return switch (sub) {
                case "accept" -> RivalChallengeManager.get().acceptChallenge(player);
                case "decline", "deny" -> RivalChallengeManager.get().declineChallenge(player);
                case "cancel" -> RivalChallengeManager.get().cancelChallenge(player);
                default -> "§cUsage: rival do challenge accept|decline|cancel";
            };
        }
        if ("challenge_accept".equals(act) || "challengeaccept".equals(act)) {
            return RivalChallengeManager.get().acceptChallenge(player, a);
        }
        if ("challenge_decline".equals(act) || "challengedecline".equals(act)) {
            return RivalChallengeManager.get().declineChallenge(player, a);
        }
        if ("challenge_cancel".equals(act) || "challengecancel".equals(act)) {
            return RivalChallengeManager.get().cancelChallengeRequest(player, a);
        }
        if ("declare".equals(act)) {
            if (a.isBlank()) {
                return "§cPick a player to declare.";
            }
            ServerPlayer target = resolveOnline(player, a);
            if (target == null) {
                return "§cPlayer not online: " + a;
            }
            return RivalSystem.declare(player, target);
        }
        if ("accept".equals(act)) {
            if (a.isBlank()) {
                return "§cPick a player to accept.";
            }
            // Pass uuid: args through — name-only lookup misses offline declarers.
            return RivalSystem.accept(player, a);
        }
        if ("accept_replace".equals(act) || "acceptreplace".equals(act) || "replacemutual".equals(act)) {
            if (a.isBlank()) {
                return "§cPick which Mutual to replace.";
            }
            return RivalSystem.acceptReplace(player, a);
        }
        if ("decline".equals(act) || "deny".equals(act)) {
            if (a.isBlank()) {
                return "§cPick a player to decline.";
            }
            return RivalSystem.decline(player, a);
        }
        if ("remove".equals(act)) {
            if (a.isBlank()) {
                return "§cPick a player to remove.";
            }
            String name = resolveNameArg(player, a);
            if (name.isBlank()) {
                return "§cPick a player to remove.";
            }
            return RivalSystem.remove(player, name);
        }
        if ("silent".equals(act)) {
            if (a.isBlank()) {
                return "§cPick a player for silent rival.";
            }
            ServerPlayer target = resolveOnline(player, a);
            if (target == null) {
                return "§cPlayer not online: " + a;
            }
            return RivalSystem.silentRival(player, target);
        }
        if ("challenge_send".equals(act) || "challengesend".equals(act)) {
            if (a.isBlank()) {
                return "§cPick a player to challenge.";
            }
            ChallengeSendArg parsed = parseChallengeSendArg(a);
            if (parsed.targetRaw().isBlank()) {
                return "§cPick a player to challenge.";
            }
            ServerPlayer target = resolveOnline(player, parsed.targetRaw());
            if (target == null) {
                return "§cPlayer not online: " + parsed.targetRaw();
            }
            return RivalChallengeManager.get().sendChallenge(player, target, parsed.minutes());
        }
        if ("spectate".equals(act)) {
            if (a.isBlank() || "any".equalsIgnoreCase(a) || "0".equals(a)) {
                return RivalSpectator.start(player, null);
            }
            ServerPlayer target = resolveOnline(player, a);
            if (target == null) {
                return "§cPlayer not online: " + a;
            }
            return RivalSpectator.start(player, target);
        }
        if ("spectate_stop".equals(act) || "spectatestop".equals(act)) {
            return RivalSpectator.stop(player);
        }
        return "§cUnknown rival action: " + act;
    }

    /** Staff: save / refresh / status / help — used by GUI + {@code /rival admin …}. */
    public static String handleAdmin(ServerPlayer player, String arg) {
        if (!StaffAccess.isStaff(player)) {
            return "§cStaff only.";
        }
        String sub = arg == null || arg.isBlank() ? "help" : arg.trim().toLowerCase(Locale.ROOT);
        // Allow "admin save" style where arg may be "save" or compound.
        if (sub.startsWith("save")) {
            RivalStore.get().markDirty();
            RivalStore.get().save();
            RivalProgression.get().save();
            return "§aRival store + progression saved.";
        }
        if (sub.startsWith("refresh") || sub.startsWith("reload")) {
            RivalStore.get().load();
            RivalProgression.get().load();
            return "§aRival store + progression reloaded.";
        }
        if (sub.startsWith("status")) {
            boolean on = DifficultyConfig.get().enableRivalSystem;
            int players = RivalStore.get().players.size();
            return "§6Rival admin status\n"
                    + "§7enabled §f" + (on ? "ON" : "OFF") + "\n"
                    + "§7players §f" + players + "\n"
                    + "§8" + RivalStore.path() + "\n"
                    + "§8" + RivalProgression.path();
        }
        return "§6§l/rival admin\n"
                + "§e/rival admin save §7— save rivalry and progress data to disk\n"
                + "§e/rival admin refresh|reload §7— reload stores from disk\n"
                + "§e/rival admin status §7— enabled + path summary\n"
                + "§e/rival admin open [page] §7— open rival GUI\n"
                + "§8GUI buttons call these directly (no Forge perm-level gate).";
    }

    /** Detail lore for a tab-encoded {@link com.dbzlegacy.adaptivedifficulty.rival.RivalSystem#currentRivalCards} row. */
    public static List<String> rivalCardDetailLines(String card) {
        if (card == null || card.isBlank()) {
            return List.of("§7Unknown rival.");
        }
        String[] p = card.split("\t", -1);
        String name = p.length > 1 && p[1] != null && !p[1].isBlank() ? p[1] : "?";
        String status = p.length > 2 ? p[2] : "?";
        String tier = p.length > 3 ? p[3] : "?";
        String rp = p.length > 4 ? p[4] : "0";
        String wins = p.length > 5 ? p[5] : "0";
        String losses = p.length > 6 ? p[6] : "0";
        String draws = p.length > 7 ? p[7] : "0";
        boolean online = p.length > 10 && "1".equals(p[10]);
        List<String> lines = new ArrayList<>();
        lines.add("§f" + name + (online ? " §a● online" : " §8○ offline"));
        lines.add("§7Status §f" + status + "  §8·  §7Tier §6" + tier);
        lines.add("§7RP §f" + rp + "  §8·  §7W/L/D §f" + wins + "/" + losses + "/" + draws);
        if (p.length > 8) {
            lines.add("§7Deaths lost/won §f" + p[8] + "/" + (p.length > 9 ? p[9] : "0"));
        }
        lines.add("§8Tap Remove to end this rivalry.");
        return lines;
    }

    /** Detail for a tab-encoded {@link com.dbzlegacy.adaptivedifficulty.rival.RivalSystem#pendingInviteCards} row. */
    public static List<String> pendingInviteDetailLines(String card) {
        if (card == null || card.isBlank()) {
            return List.of("§7Unknown invite.");
        }
        String[] p = card.split("\t", -1);
        String name = p.length > 1 && p[1] != null && !p[1].isBlank() ? p[1] : "?";
        String dir = p.length > 2 ? p[2] : "?";
        boolean online = p.length > 4 && "1".equals(p[4]);
        boolean mutual = p.length > 5 && "mutual".equalsIgnoreCase(p[5]);
        List<String> lines = new ArrayList<>();
        lines.add("§f" + name + (online ? " §a●" : " §8○"));
        if ("IN".equalsIgnoreCase(dir)) {
            if (mutual) {
                lines.add("§eIncoming mutual confirm");
                lines.add("§7Both players Silent'd — Accept to confirm.");
            } else {
                lines.add("§aIncoming declare");
                lines.add("§7Accept to become Mutual · Decline to ignore.");
            }
        } else {
            lines.add("§6Outgoing declare");
            lines.add("§7Waiting for them to Accept or Decline.");
        }
        return lines;
    }

    public static boolean pendingInviteCardIncoming(String card) {
        if (card == null) {
            return false;
        }
        String[] p = card.split("\t", -1);
        return p.length >= 3 && "IN".equalsIgnoreCase(p[2]);
    }

    /**
     * GUI arg formats: {@code uuid:&lt;id&gt;@&lt;minutes&gt;} or plain target (defaults to
     * {@link RivalConstants#CH_MIN_MINUTES}).
     */
    private static ChallengeSendArg parseChallengeSendArg(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            return new ChallengeSendArg("", RivalConstants.CH_MIN_MINUTES);
        }
        int at = s.lastIndexOf('@');
        if (at <= 0 || at >= s.length() - 1) {
            return new ChallengeSendArg(s, RivalConstants.CH_MIN_MINUTES);
        }
        String target = s.substring(0, at).trim();
        String minsRaw = s.substring(at + 1).trim();
        int minutes = RivalConstants.CH_MIN_MINUTES;
        try {
            minutes = Integer.parseInt(minsRaw);
        } catch (NumberFormatException ignored) {
            minutes = RivalConstants.CH_MIN_MINUTES;
        }
        minutes = Math.max(RivalConstants.CH_MIN_MINUTES,
                Math.min(RivalConstants.CH_MAX_MINUTES, minutes));
        return new ChallengeSendArg(target, minutes);
    }

    private record ChallengeSendArg(String targetRaw, int minutes) {}
}
