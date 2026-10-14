package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.RivalGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.SparGuiApi;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sparring - CNPC parity (Home / Dojo / Wars) with mentor/apprentice picks, war challenges,
 * and rankings. Home is overview only; Dojo and Wars hold the controls.
 */
public final class UltraSparScreens {
    private UltraSparScreens() {}

    /**
     * Spar card rows from {@link SparGuiApi} / SparringSystem are tab-encoded
     * ({@code uuid\tname\tIN|OUT\t...}). Splitting on {@code |} left the whole row as the
     * picker arg, so mentor Accept failed {@code matchesInviteFrom}.
     */
    static String[] splitCard(String card) {
        if (card == null || card.isBlank()) {
            return new String[0];
        }
        if (card.indexOf('\t') >= 0) {
            return card.split("\t", -1);
        }
        return card.split("\\|", -1);
    }

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("pending_decide:")) {
            openPendingDecide(player, raw.substring("pending_decide:".length()).trim());
            return;
        }
        if (lower.startsWith("dojo_war_pending_decide:")) {
            openDojoWarDecide(player, raw.substring("dojo_war_pending_decide:".length()).trim());
            return;
        }
        if (lower.startsWith("pick_confirm:")) {
            openPickConfirm(player, raw.substring("pick_confirm:".length()).trim());
            return;
        }
        if (lower.startsWith("top_")) {
            openTop(player, lower.substring(4).trim());
            return;
        }
        if (lower.startsWith("dojo_top_")) {
            openTop(player, lower.substring("dojo_top_".length()).trim());
            return;
        }
        switch (lower) {
            case "stats" -> openStats(player);
            case "top", "leaderboard", "dojo_rank", "dojo_rankings" -> openTop(player, "tp");
            case "dojo", "roster" -> openDojo(player, "home");
            case "dojo_members" -> openDojo(player, "members");
            case "dojo_hof" -> openDojo(player, "hof");
            case "mentor", "actions" -> openMentor(player);
            case "pending", "invites" -> openPending(player);
            case "settings" -> openSettings(player);
            case "admin" -> openAdmin(player);
            case "wars", "dojo_war" -> openWars(player);
            case "dojo_war_pending" -> openDojoWarPending(player);
            case "pick_apprentice" ->
                    openOnlinePick(player, "Invite apprentice", "mentor_invite", "pick_apprentice", "mentor");
            case "pick_mentor" ->
                    openOnlinePick(player, "Ask as apprentice", "apprentice_invite", "pick_mentor", "mentor");
            case "pick_release" -> openReleasePick(player);
            case "pick_dojo_challenge" -> openDojoChallengePick(player);
            default -> openHome(player);
        }
    }

    private static UltraScreenSpec base(String page, String tab) {
        return UltraScreenSpec.of("spar")
                .title("Sparring")
                .page(page)
                .activeTab(tab)
                .tabs("home|Home", "dojo|Dojo", "wars|Wars");
    }

    private static String tabFor(String page) {
        if (page == null) {
            return "home";
        }
        String p = page.toLowerCase(Locale.ROOT);
        if (p.startsWith("dojo_war") || "pick_dojo_challenge".equals(p) || p.contains("dojo_challenge")) {
            return "wars";
        }
        if (p.startsWith("dojo_top") || "dojo_rank".equals(p) || "dojo_rankings".equals(p) || p.startsWith("top")) {
            return "home";
        }
        if (p.startsWith("dojo")
                || "mentor".equals(p)
                || "actions".equals(p)
                || "pending".equals(p)
                || "invites".equals(p)
                || "settings".equals(p)
                || "admin".equals(p)
                || "roster".equals(p)
                || p.startsWith("pick_apprentice")
                || p.startsWith("pick_mentor")
                || p.startsWith("pick_accept")
                || p.startsWith("pick_decline")
                || p.startsWith("pick_release")
                || p.startsWith("pick_confirm")) {
            return "dojo";
        }
        return "home";
    }

    /** Rankings and personal stats only - Dojo / Wars stay on their tabs. */
    private static void openHome(ServerPlayer player) {
        Map<String, String> ph = SparGuiApi.placeholders(player);
        boolean active = "true".equalsIgnoreCase(ph.get("session_active"));
        String partner = ph.getOrDefault("partner", "");
        UltraScreenSpec spec = base("main", "home")
                .labelValue("Session", active ? "in progress" : "not running")
                .labelValue("Partner", partner == null || partner.isBlank() ? "none" : partner)
                .line(UltraStyle.LABEL + "Rankings show who has trained the most. Your stats are this character only.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        UltraPickHelper.placePageButtons(spec, "top|Player rankings", "stats|Your stats");
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openStats(ServerPlayer player) {
        UltraScreenSpec spec = base("stats", "home").line(UltraStyle.LABEL + "Spar stats");
        for (String line : SparGuiApi.linesForPage(player, "stats")) {
            if (line != null && !line.isBlank()) {
                spec.line(line);
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openTop(ServerPlayer player, String category) {
        String cat = category == null || category.isBlank() ? "tp" : category;
        if ("wins".equalsIgnoreCase(cat)
                || "win".equalsIgnoreCase(cat)
                || "streak".equalsIgnoreCase(cat)) {
            cat = "tp";
        }
        if ("reputation".equalsIgnoreCase(cat)) {
            cat = "rp";
        } else if ("war".equalsIgnoreCase(cat)) {
            cat = "wars";
        }
        boolean dojoBoard = "rp".equalsIgnoreCase(cat) || "wars".equalsIgnoreCase(cat);
        List<String> lines = dojoBoard
                ? SparGuiApi.dojoTopLines(player, cat)
                : SparGuiApi.topLines(player, cat);
        UltraScreenSpec spec = base(dojoBoard ? "dojo_top_" + cat : "top_" + cat, "home")
                .line(UltraStyle.LABEL + "Leaderboard - " + boardLabel(cat));
        if (lines != null) {
            for (String line : lines) {
                if (line != null && !line.isBlank()) {
                    spec.line(line);
                }
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        UltraPickHelper.placePageButtons(
                spec,
                "top_tp|Training points",
                "top_sessions|Sessions",
                "top_perfect|Perfect spars",
                "top_combo|Combo",
                "top_time|Time",
                "dojo_top_rp|Reputation",
                "dojo_top_wars|War wins");
        spec.footerBackHome();
        spec.send(player);
    }

    private static String boardLabel(String cat) {
        return switch (cat == null ? "" : cat.toLowerCase(Locale.ROOT)) {
            case "sessions" -> "Sessions";
            case "perfect" -> "Perfect spars";
            case "combo" -> "Combo";
            case "time" -> "Time";
            case "rp" -> "Reputation";
            case "wars" -> "War wins";
            default -> "Training points";
        };
    }

    private static void openDojo(ServerPlayer player, String sub) {
        String key = sub == null || sub.isBlank() ? "home" : sub;
        String page = switch (key) {
            case "members" -> "dojo_members";
            case "hof" -> "dojo_hof";
            default -> "dojo";
        };
        Map<String, String> ph = SparGuiApi.placeholders(player);
        UltraScreenSpec spec = base(page, "dojo")
                .labelValue(
                        "Session",
                        "true".equalsIgnoreCase(ph.get("session_active")) ? "in progress" : "none active")
                .labelValue("Partner", blank(ph.get("partner")));
        if (!"true".equalsIgnoreCase(ph.get("system_enabled"))) {
            spec.line(UltraStyle.danger("Sparring is turned off. Ask staff to enable it."))
                    .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
            spec.footerBackHome();
            spec.send(player);
            return;
        }
        switch (key) {
            case "members" -> {
                for (String line : SparGuiApi.dojoMemberLines(player)) {
                    if (line != null && !line.isBlank()) {
                        spec.line(line);
                    }
                }
            }
            case "hof" -> {
                for (String line : SparGuiApi.linesForPage(player, "dojo_hof")) {
                    if (line != null && !line.isBlank()) {
                        spec.line(line);
                    }
                }
            }
            default -> {
                for (String line : SparGuiApi.linesForPage(player, "dojo")) {
                    if (line != null && !line.isBlank()) {
                        spec.line(line);
                    }
                }
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        UltraPickHelper.placePageButtons(
                spec, "dojo|Home", "dojo_members|Members", "dojo_hof|Hall of fame");
        if ("home".equals(key)) {
            UltraPickHelper.placePageButtons(
                    spec, "stats|Stats", "mentor|Training bonds", "settings|Chat settings");
            if (StaffAccess.isStaff(player)) {
                spec.buttonLeft(
                                "page:admin",
                                "Staff Admin",
                                UltraStyle.STYLE_NORMAL,
                                UltraStyle.hubTileAccentCode("admin"))
                        .rowGap();
            }
        }
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openMentor(ServerPlayer player) {
        UltraScreenSpec spec = base("mentor", "dojo")
                .line(UltraStyle.LABEL + "Masters recruit - students request a master");
        for (String line : SparGuiApi.mentorLines(player)) {
            if (line != null && !line.isBlank()) {
                spec.line(line);
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        UltraPickHelper.placePageButtons(
                spec,
                "pick_apprentice|Recruit apprentice...",
                "pick_mentor|Request a master...",
                "pending|Bond invites",
                "pick_release|Release apprentice...");
        spec.buttonLeft(
                        "page:pick_confirm:mentor|mentor|leave",
                        UltraStyle.danger("Leave mentor"),
                        "danger")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openPending(ServerPlayer player) {
        UltraScreenSpec spec = base("pending", "dojo")
                .line(UltraStyle.LABEL + "Mentor / apprentice requests only");
        for (String line : SparGuiApi.pendingMentorLines(player)) {
            if (line != null && !line.isBlank()) {
                spec.line(line);
            }
        }
        List<String> cards = SparGuiApi.pendingMentorInviteCards(player);
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        int placed = 0;
        if (cards != null) {
            for (String card : cards) {
                if (placed >= 8 || card == null || card.isBlank()) {
                    continue;
                }
                String[] parts = splitCard(card);
                String arg = parts.length > 0 ? parts[0].trim() : "";
                String label = parts.length > 1 && !parts[1].isBlank() ? parts[1].trim() : arg;
                String dir = parts.length > 2 ? parts[2].trim() : "IN";
                if (arg.isBlank()) {
                    continue;
                }
                String picker = arg.contains(":") ? arg : "uuid:" + arg;
                String packed = ("OUT".equalsIgnoreCase(dir) ? "OUT|" : "IN|") + picker;
                String action = "page:pending_decide:" + packed;
                String shown = label.isBlank()
                        ? RivalGuiApi.displayPickerArg(player, picker)
                        : label;
                if (shown == null || shown.isBlank()) {
                    shown = "-";
                }
                if (placed % 2 == 0) {
                    spec.buttonLeft(action, UltraStyle.confirm(shown), "confirm");
                } else {
                    spec.buttonRight(action, UltraStyle.confirm(shown), "confirm");
                    spec.rowGap();
                }
                placed++;
            }
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        if (placed == 0) {
            spec.line(UltraStyle.dim(
                    "No bond invites waiting. Send one from Training bonds, or wait for someone to invite you."));
        }
        // Named destination - training bonds (no footerBackHome stack).
        spec.button("page:mentor", UltraStyle.backTo("Training bonds"), "dim").rowGap();
        spec.send(player);
    }

    private static void openPendingDecide(ServerPlayer player, String pickerArg) {
        String dir = "IN";
        String picker = pickerArg == null ? "" : pickerArg;
        if (picker.regionMatches(true, 0, "OUT|", 0, 4) || picker.regionMatches(true, 0, "IN|", 0, 3)) {
            int bar = picker.indexOf('|');
            dir = picker.substring(0, bar);
            picker = picker.substring(bar + 1);
        }
        boolean outgoing = "OUT".equalsIgnoreCase(dir);
        String who = RivalGuiApi.displayPickerArg(player, picker);
        UltraScreenSpec spec = base("pending_decide:" + (outgoing ? "OUT|" : "IN|") + picker, "dojo")
                .line(UltraStyle.LABEL + "Training bond invite")
                .labelValue("Player", who == null || who.isBlank() ? "-" : who);
        if (outgoing) {
            spec.line(UltraStyle.LABEL + "You invited this player.");
        } else {
            spec.line(UltraStyle.LABEL + "Accept to form this training bond.")
                    .line(UltraStyle.LABEL + "Decline to dismiss the invite.");
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + 100);
        if (outgoing) {
            spec.buttonLeft("do:mentor:cancel:" + picker, UltraStyle.danger("Withdraw invite"), "danger")
                    .buttonRight("page:pending", UltraStyle.dim("Cancel"), "dim")
                    .rowGap();
        } else {
            // Stack Accept above Decline - side-by-side is a misclick invite-kill risk.
            spec.button("do:mentor_accept:" + picker, UltraStyle.confirm("Accept"), "confirm")
                    .rowGap();
            spec.button("do:mentor_decline:" + picker, UltraStyle.danger("Decline"), "danger")
                    .rowGap();
            // Named destination - pending list (no footerBackHome stack).
            spec.button("page:pending", UltraStyle.backTo("Pending invites"), "dim").rowGap();
        }
        spec.send(player);
    }

    private static void openPickConfirm(ServerPlayer player, String payload) {
        String[] parts = payload.split("\\|", 3);
        if (parts.length < 3) {
            openMentor(player);
            return;
        }
        String action = parts[0];
        String returnPage = parts[1];
        String targetArg = parts[2];
        String[] copy = confirmCopy(action);
        String who = "mentor".equals(action) && "leave".equalsIgnoreCase(targetArg)
                ? "Your master"
                : RivalGuiApi.displayPickerArg(player, targetArg);
        UltraScreenSpec spec = base("pick_confirm:" + payload, tabFor(returnPage))
                .line(UltraStyle.LABEL + copy[0])
                .labelValue("Target", who == null || who.isBlank() ? "-" : who)
                .line(UltraStyle.LABEL + copy[1])
                .setRow(UltraStyle.CONTENT_ROW_TABS + 100);
        boolean destructive = isDestructiveConfirm(action);
        spec.buttonLeft(
                        "do:" + action + ":" + targetArg,
                        destructive ? UltraStyle.danger("Confirm") : UltraStyle.confirm("Confirm"),
                        destructive ? UltraStyle.STYLE_DANGER : UltraStyle.STYLE_CONFIRM)
                .buttonRight("page:" + returnPage, UltraStyle.dim("Cancel"), UltraStyle.STYLE_DIM)
                .rowGap();
        // Cancel is the only exit (no footerBackHome stack).
        spec.send(player);
    }

    /** Leave / release / decline are irreversible or dismissive - wire danger, not confirm. */
    private static boolean isDestructiveConfirm(String action) {
        return switch (action == null ? "" : action) {
            case "mentor", "mentor_release", "mentor_decline" -> true;
            default -> false;
        };
    }

    private static String[] confirmCopy(String action) {
        return switch (action == null ? "" : action) {
            case "mentor_invite" -> new String[] {"Invite apprentice", "Invite this player to train under you."};
            case "apprentice_invite" -> new String[] {"Ask as apprentice", "Ask this player to be your master."};
            case "mentor_release" -> new String[] {
                "Release apprentice", "Release this apprentice. A 12-hour cooldown starts."
            };
            case "mentor" -> new String[] {"Leave mentor", "Leave your master. A 12-hour cooldown starts."};
            case "mentor_accept" -> new String[] {"Accept mentor invite", "Accept to form this training bond."};
            case "mentor_decline" -> new String[] {"Decline mentor invite", "Decline to dismiss the invite."};
            case "dojo_challenge" -> new String[] {"Challenge rival dojo", "Challenge this dojo to a war."};
            default -> new String[] {"Confirm action", "Confirm to continue."};
        };
    }

    private static void openSettings(ServerPlayer player) {
        Map<String, String> ph = SparGuiApi.placeholders(player);
        boolean tpOn = "true".equalsIgnoreCase(ph.get("tpMsg"));
        boolean mentorTpOn = "true".equalsIgnoreCase(ph.get("mentorTpMsg"));
        UltraScreenSpec spec = base("settings", "dojo")
                .line(UltraStyle.LABEL + "Training point messages")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
        spec.buttonLeft(
                        "do:tpmsg:toggle",
                        tpOn ? UltraStyle.confirm("TP - ON") : UltraStyle.dim("TP - OFF"),
                        tpOn ? "confirm" : "dim")
                .buttonRight(
                        "do:mentor_tpmsg:toggle",
                        mentorTpOn
                                ? UltraStyle.confirm("Mentor TP - ON")
                                : UltraStyle.dim("Mentor TP - OFF"),
                        mentorTpOn ? "confirm" : "dim")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openWars(ServerPlayer player) {
        UltraScreenSpec spec = base("dojo_war", "wars");
        for (String line : SparGuiApi.dojoWarLines(player)) {
            if (line != null && !line.isBlank()) {
                spec.line(line);
            }
        }
        int warPending = SparGuiApi.pendingDojoWarCards(player).size();
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        UltraPickHelper.placePageButtons(spec, "pick_dojo_challenge|Challenge rival dojo...");
        String pendingLabel = warPending > 0 ? "War pending (" + warPending + ")" : "War pending";
        spec.buttonLeft("page:dojo_war_pending", UltraStyle.confirm(pendingLabel), "confirm").rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openDojoWarPending(ServerPlayer player) {
        UltraScreenSpec spec = base("dojo_war_pending", "wars")
                .line(UltraStyle.LABEL + "Dojo war pending");
        List<String> cards = SparGuiApi.pendingDojoWarCards(player);
        if (cards == null || cards.isEmpty()) {
            spec.line(UltraStyle.dim(
                    "No dojo war challenges waiting - declare war when your roster is ready."));
        } else {
            for (String card : cards) {
                if (card == null || card.isBlank()) {
                    continue;
                }
                String[] parts = splitCard(card);
                String label = parts.length > 1 && !parts[1].isBlank() ? parts[1].trim() : parts[0];
                if (label != null && !label.isBlank()) {
                    spec.line(UltraStyle.LABEL + label);
                }
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        int placed = 0;
        if (cards != null) {
            for (String card : cards) {
                if (placed >= 8 || card == null || card.isBlank()) {
                    continue;
                }
                String[] parts = splitCard(card);
                String arg = parts.length > 0 ? parts[0].trim() : "";
                String label = parts.length > 1 && !parts[1].isBlank() ? parts[1].trim() : arg;
                if (arg.isBlank()) {
                    continue;
                }
                String action = "page:dojo_war_pending_decide:" + arg;
                if (placed % 2 == 0) {
                    spec.buttonLeft(action, UltraStyle.confirm(label), "confirm");
                } else {
                    spec.buttonRight(action, UltraStyle.confirm(label), "confirm");
                    spec.rowGap();
                }
                placed++;
            }
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        // Named destination - dojo wars (no footerBackHome stack).
        spec.button("page:dojo_war", UltraStyle.backTo("Dojo wars"), "dim").rowGap();
        spec.send(player);
    }

    private static void openDojoWarDecide(ServerPlayer player, String pickerArg) {
        String who = RivalGuiApi.displayPickerArg(player, pickerArg);
        UltraScreenSpec spec = base("dojo_war_pending_decide:" + pickerArg, "wars")
                .line(UltraStyle.LABEL + "Dojo war request")
                .labelValue("Dojo", who == null || who.isBlank() ? "-" : who)
                .line(UltraStyle.LABEL + "Accept to start the war (2x RP vs that dojo).")
                .line(UltraStyle.LABEL + "Cancel to drop this war challenge.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 100);
        String arg = pickerArg == null ? "" : pickerArg;
        // Stack Accept above Cancel - side-by-side is a misclick cancel risk.
        spec.button("do:dojo_accept:" + arg, UltraStyle.confirm("Accept"), "confirm").rowGap();
        spec.button("do:dojo_war_cancel:" + arg, UltraStyle.danger("Cancel challenge"), "danger")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openOnlinePick(
            ServerPlayer player, String heading, String action, String page, String back) {
        UltraScreenSpec spec = base(page, tabFor(back))
                .line(UltraStyle.LABEL + heading)
                .line(UltraStyle.DIM + "Choose an online player.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 70);
        List<String> names = RivalGuiApi.onlinePlayerNames(player);
        int placed = 0;
        if (names != null) {
            for (String name : names) {
                if (placed >= 10 || name == null || name.isBlank()) {
                    continue;
                }
                String nav = "page:pick_confirm:" + action + "|" + back + "|" + name;
                if (placed % 2 == 0) {
                    spec.buttonLeft(nav, UltraStyle.confirm(name), "confirm");
                } else {
                    spec.buttonRight(nav, UltraStyle.confirm(name), "confirm");
                    spec.rowGap();
                }
                placed++;
            }
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        if (placed == 0) {
            spec.line(UltraStyle.dim("Nobody else is online right now - try again when other players are on."));
        }
        // Cancel is the only exit (no footerBackHome stack).
        spec.button("page:" + back, UltraStyle.dim("Cancel"), "dim").rowGap();
        spec.send(player);
    }

    private static void openReleasePick(ServerPlayer player) {
        UltraScreenSpec spec = base("pick_release", "dojo")
                .line(UltraStyle.LABEL + "Release apprentice")
                .line(UltraStyle.DIM + "Choose an apprentice.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 70);
        List<String> cards = SparGuiApi.apprenticeCards(player);
        int placed = 0;
        if (cards != null) {
            for (String card : cards) {
                if (placed >= 8 || card == null || card.isBlank()) {
                    continue;
                }
                String[] parts = splitCard(card);
                String arg = parts.length > 0 ? parts[0].trim() : "";
                String label = parts.length > 1 && !parts[1].isBlank() ? parts[1].trim() : arg;
                if (arg.isBlank()) {
                    continue;
                }
                String nav = "page:pick_confirm:mentor_release|mentor|" + arg;
                if (placed % 2 == 0) {
                    spec.buttonLeft(nav, UltraStyle.confirm(label), "confirm");
                } else {
                    spec.buttonRight(nav, UltraStyle.confirm(label), "confirm");
                    spec.rowGap();
                }
                placed++;
            }
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        if (placed == 0) {
            spec.line(UltraStyle.dim("No apprentices to release. Take an apprentice from Training bonds first."));
        }
        // Cancel is the only exit (no footerBackHome stack).
        spec.button("page:mentor", UltraStyle.dim("Cancel"), "dim").rowGap();
        spec.send(player);
    }

    private static void openDojoChallengePick(ServerPlayer player) {
        UltraScreenSpec spec = base("pick_dojo_challenge", "wars")
                .line(UltraStyle.LABEL + "Challenge rival dojo")
                .line(UltraStyle.DIM + "Choose a rival dojo master.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 70);
        List<String> cards = SparGuiApi.rivalDojoCards(player);
        int placed = 0;
        if (cards != null) {
            for (String card : cards) {
                if (placed >= 8 || card == null || card.isBlank()) {
                    continue;
                }
                String[] parts = splitCard(card);
                String arg = parts.length > 0 ? parts[0].trim() : "";
                String label = parts.length > 1 && !parts[1].isBlank() ? parts[1].trim() : arg;
                if (arg.isBlank()) {
                    continue;
                }
                String nav = "page:pick_confirm:dojo_challenge|dojo_war|" + arg;
                if (placed % 2 == 0) {
                    spec.buttonLeft(nav, UltraStyle.confirm(label), "confirm");
                } else {
                    spec.buttonRight(nav, UltraStyle.confirm(label), "confirm");
                    spec.rowGap();
                }
                placed++;
            }
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        if (placed == 0) {
            spec.line(UltraStyle.dim("No rival dojo masters are online - challenge when one is on."));
        }
        // Cancel is the only exit (no footerBackHome stack).
        spec.button("page:dojo_war", UltraStyle.dim("Cancel"), "dim").rowGap();
        spec.send(player);
    }

    private static void openAdmin(ServerPlayer player) {
        if (!StaffAccess.isStaff(player)) {
            openDojo(player, "home");
            return;
        }
        UltraScreenSpec spec = base("admin", "dojo")
                .line(UltraStyle.LABEL + "Save spar data and show status")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
        spec.buttonLeft("do:admin:save", UltraStyle.confirm("Save data"), "confirm")
                .buttonRight("do:admin:status", UltraStyle.dim("Show status"), "dim")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
