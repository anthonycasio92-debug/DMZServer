package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.RivalGuiApi;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Rival system - CNPC parity via page: navigation and arg-bearing do: buttons.
 * Tabs: Home / Actions / Challenge. Sub-pages for list, records, pickers, requests.
 */
public final class UltraRivalScreens {
    private UltraRivalScreens() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String raw = page == null || page.isBlank() ? "home" : page.trim();
        String p = raw.toLowerCase(Locale.ROOT);
        if (p.startsWith("challenge_time:")) {
            openChallengeTime(player, raw.substring("challenge_time:".length()).trim());
            return;
        }
        if (p.startsWith("list_detail:")) {
            openListDetail(player, raw.substring("list_detail:".length()).trim());
            return;
        }
        if (p.startsWith("records:")) {
            openRecords(player, p.substring("records:".length()).trim());
            return;
        }
        switch (p) {
            case "actions", "action" -> openActions(player);
            case "challenge", "challenges" -> openChallenge(player);
            case "list" -> openList(player);
            case "top", "leaderboard" -> openTop(player);
            case "records" -> openRecords(player, "history");
            case "requests", "pending", "invites" -> openRequests(player);
            case "settings" -> openSettings(player);
            case "pick_declare" -> openNamePick(player, "Declare a rival", "declare", "actions");
            case "pick_silent" -> openNamePick(player, "Silent declare", "silent", "actions");
            case "pick_challenge" -> openNamePick(player, "Challenge rival", "challenge_pick", "challenge");
            case "pick_spectate" -> openNamePick(player, "Spectate", "spectate", "challenge");
            case "pick_replace_mutual", "replace_mutual" -> openReplacePick(player);
            case "admin" -> openAdmin(player);
            default -> openHome(player);
        }
    }

    private static UltraScreenSpec base(String page, String tab) {
        return UltraScreenSpec.of("rival")
                .title("Rivals")
                .page(page)
                .activeTab(tab)
                .tabs("home|Home", "actions|Actions", "challenge|Challenge");
    }

    private static void openHome(ServerPlayer player) {
        Map<String, String> ph = RivalGuiApi.placeholders(player);
        UltraScreenSpec spec = base("home", "home")
                .labelValue("RP", ph.getOrDefault("rp", "0"))
                .labelValue("Tier", ph.getOrDefault("tier", "-"))
                .labelValue("Wins", ph.getOrDefault("wins", "0"))
                .labelValue("Losses", ph.getOrDefault("losses", "0"))
                .labelValue(
                        "Mutual",
                        ph.getOrDefault("mutual", "0") + "/" + ph.getOrDefault("mutual_max", "0"))
                .setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        UltraPickHelper.placePageButtons(
                spec, "list|View rival list", "top|View leaderboard", "records|View records");
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openActions(ServerPlayer player) {
        Map<String, String> ph = RivalGuiApi.placeholders(player);
        UltraScreenSpec spec = base("actions", "actions")
                .line(UltraStyle.LABEL + "Declare rivals and review requests.")
                .labelValue("Pending invites", ph.getOrDefault("pending_invites", "0"))
                .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
        UltraPickHelper.placePageButtons(
                spec,
                "pick_declare|Declare a rival",
                "pick_silent|Silent declare",
                "requests|Review requests",
                "settings|Settings");
        if (RivalGuiApi.needsMutualReplacePick(player)) {
            spec.buttonLeft("page:pick_replace_mutual", UltraStyle.danger("Replace mutual slot"), "danger")
                    .rowGap();
        }
        if (StaffAccess.isStaff(player)) {
            spec.buttonLeft("page:admin", UltraStyle.dim("Staff Admin"), "dim").rowGap();
        }
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openChallenge(ServerPlayer player) {
        UltraScreenSpec spec = base("challenge", "challenge");
        for (String line : RivalGuiApi.challengeLines(player)) {
            if (line != null && !line.isBlank()) {
                spec.line(line);
            }
        }
        for (String line : RivalGuiApi.pendingChallengeLines(player)) {
            if (line != null && !line.isBlank()) {
                spec.line(line);
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        UltraPickHelper.placePageButtons(
                spec,
                "pick_challenge|Send challenge...",
                "pick_spectate|Spectate...",
                "records:history|Challenge history");
        // Stack Accept above Decline - side-by-side is a misclick invite-kill risk.
        spec.button("do:challenge:accept", UltraStyle.confirm("Accept challenge"), "confirm")
                .rowGap();
        spec.button("do:challenge:decline", UltraStyle.danger("Decline challenge"), "danger")
                .rowGap();
        spec.buttonLeft("do:challenge:cancel", UltraStyle.danger("Cancel challenge"), "danger")
                .buttonRight("do:spectate_stop", UltraStyle.dim("Stop spectate"), "dim")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openList(ServerPlayer player) {
        UltraScreenSpec spec = base("list", "home")
                .line(UltraStyle.LABEL + "Your current rivals.");
        for (String line : RivalGuiApi.listLines(player)) {
            if (line != null && !line.isBlank()) {
                spec.line(line);
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        List<String> args = RivalGuiApi.currentRivalArgs(player);
        int placed = 0;
        if (args != null) {
            for (String arg : args) {
                if (placed >= 8 || arg == null || arg.isBlank()) {
                    continue;
                }
                String action = "page:list_detail:" + arg;
                String label = UltraStyle.confirm("Open");
                // Prefer a human name from the arg when it looks like name/uuid.
                String shown = arg.contains(":") ? arg.substring(arg.lastIndexOf(':') + 1) : arg;
                if (shown.length() > 16) {
                    shown = shown.substring(0, 15) + "...";
                }
                label = UltraStyle.confirm(shown);
                if (placed % 2 == 0) {
                    spec.buttonLeft(action, label, "confirm");
                } else {
                    spec.buttonRight(action, label, "confirm");
                    spec.rowGap();
                }
                placed++;
            }
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        if (placed == 0) {
            spec.line(UltraStyle.dim("No rivals on your list yet."));
        }
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openListDetail(ServerPlayer player, String arg) {
        String who = RivalGuiApi.displayPickerArg(player, arg);
        UltraScreenSpec spec = base("list_detail:" + arg, "home")
                .line(UltraStyle.LABEL + "Rival profile")
                .labelValue("Target", who == null || who.isBlank() ? "-" : who)
                .notice("Removing a rival cannot be undone from this menu.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 100);
        if (arg != null && !arg.isBlank()) {
            spec.buttonLeft("do:remove:" + arg, UltraStyle.danger("Remove rival"), "danger")
                    .rowGap();
        }
        // One Back only - rival list (no footerBackHome stack).
        spec.button("page:list", UltraStyle.BACK, "dim").rowGap();
        spec.send(player);
    }

    private static void openTop(ServerPlayer player) {
        UltraScreenSpec spec = base("top", "home").line(UltraStyle.LABEL + "Rival leaderboard");
        for (String line : RivalGuiApi.topLines(player)) {
            if (line != null && !line.isBlank()) {
                spec.line(line);
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openRecords(ServerPlayer player, String sub) {
        String tab = sub == null || sub.isBlank() ? "history" : sub.toLowerCase(Locale.ROOT);
        UltraScreenSpec spec = base("records:" + tab, "home")
                .line(UltraStyle.LABEL + "Records - " + tab);
        List<String> lines = switch (tab) {
            case "stats" -> RivalGuiApi.statsLines(player);
            case "season" -> RivalGuiApi.seasonLines(player);
            case "quests" -> RivalGuiApi.questLines(player);
            case "achievements" -> RivalGuiApi.achievementLines(player);
            case "hof" -> RivalGuiApi.hofLines(player);
            case "journal" -> RivalGuiApi.journalLines(player);
            case "title" -> RivalGuiApi.titleLines(player);
            default -> RivalGuiApi.linesForPage(player, "records:" + tab);
        };
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
                "records:history|History",
                "records:stats|Stats",
                "records:season|Season",
                "records:quests|Quests",
                "records:achievements|Achievements",
                "records:hof|Hall of fame",
                "records:journal|Journal",
                "records:title|Title");
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openRequests(ServerPlayer player) {
        UltraScreenSpec spec = base("requests", "actions")
                .line(UltraStyle.LABEL + "Incoming and outgoing rival requests.");
        List<String> cards = RivalGuiApi.pendingInviteCards(player);
        if (cards != null) {
            for (String card : cards) {
                if (card == null || card.isBlank()) {
                    continue;
                }
                for (String line : RivalGuiApi.pendingInviteDetailLines(card)) {
                    if (line != null && !line.isBlank()) {
                        spec.line(line);
                    }
                }
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        List<String> incoming = RivalGuiApi.pendingIncomingDeclareArgs(player);
        UltraPickHelper.placeNameButtons(spec, "accept", incoming, 4);
        UltraPickHelper.placeNameButtons(spec, "decline", incoming, 4);
        // Stack Accept above Decline - side-by-side is a misclick invite-kill risk.
        spec.button("do:challenge:accept", UltraStyle.confirm("Accept challenge"), "confirm")
                .rowGap();
        spec.button("do:challenge:decline", UltraStyle.danger("Decline challenge"), "danger")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openSettings(ServerPlayer player) {
        Map<String, String> ph = RivalGuiApi.placeholders(player);
        boolean tpOn = "true".equalsIgnoreCase(ph.getOrDefault("tpMsg", ph.getOrDefault("tp_msg", "false")));
        boolean instinctOn = "true".equalsIgnoreCase(ph.getOrDefault("instinct", "false"));
        UltraScreenSpec spec = base("settings", "actions")
                .line(UltraStyle.LABEL + "Rival chat and instinct toggles.")
                .labelValue("TP chat", tpOn ? "ON" : "OFF")
                .labelValue("Instinct", instinctOn ? "ON" : "OFF")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
        spec.buttonLeft(
                        "do:tpmsg:toggle",
                        tpOn ? UltraStyle.confirm("TP chat - ON") : UltraStyle.dim("TP chat - OFF"),
                        tpOn ? UltraStyle.STYLE_CONFIRM : UltraStyle.STYLE_DIM)
                .buttonRight(
                        "do:instinct",
                        instinctOn
                                ? UltraStyle.confirm("Instinct - ON")
                                : UltraStyle.dim("Instinct - OFF"),
                        instinctOn ? UltraStyle.STYLE_CONFIRM : UltraStyle.STYLE_DIM)
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openNamePick(
            ServerPlayer player, String heading, String doAction, String returnTab) {
        String tab = "actions".equals(returnTab) ? "actions" : "challenge";
        UltraScreenSpec spec = base("pick_" + doAction, tab)
                .line(UltraStyle.LABEL + heading)
                .line(UltraStyle.DIM + "Choose an online player.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 70);
        if ("challenge_pick".equals(doAction)) {
            // Choosing a name opens duration page rather than immediate send.
            List<String> names = RivalGuiApi.onlinePlayerNames(player);
            int placed = 0;
            if (names != null) {
                for (String name : names) {
                    if (placed >= 10 || name == null || name.isBlank()) {
                        continue;
                    }
                    String action = "page:challenge_time:" + name;
                    if (placed % 2 == 0) {
                        spec.buttonLeft(action, UltraStyle.confirm(name), "confirm");
                    } else {
                        spec.buttonRight(action, UltraStyle.confirm(name), "confirm");
                        spec.rowGap();
                    }
                    placed++;
                }
            }
            if (placed % 2 == 1) {
                spec.rowGap();
            }
            if (placed == 0) {
                spec.line(UltraStyle.dim("Nobody is available right now."));
            }
        } else {
            UltraPickHelper.placeNameButtons(spec, doAction, RivalGuiApi.onlinePlayerNames(player), 10);
        }
        // Cancel is the only exit (no footerBackHome stack).
        spec.button("page:" + ("challenge".equals(tab) ? "challenge" : "actions"), UltraStyle.dim("Cancel"), "dim")
                .rowGap();
        spec.send(player);
    }

    private static void openChallengeTime(ServerPlayer player, String target) {
        UltraScreenSpec spec = base("challenge_time:" + target, "challenge")
                .line(UltraStyle.LABEL + "Challenge length")
                .labelValue("Rival", target == null || target.isBlank() ? "-" : target)
                .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
        if (target != null && !target.isBlank()) {
            for (int min = 1; min <= 10; min += 2) {
                spec.buttonLeft(
                        "do:challenge_send:" + target + "@" + min,
                        UltraStyle.confirm(min + " min"),
                        "confirm");
                if (min + 1 <= 10) {
                    spec.buttonRight(
                            "do:challenge_send:" + target + "@" + (min + 1),
                            UltraStyle.confirm((min + 1) + " min"),
                            "confirm");
                }
                spec.rowGap();
            }
        }
        // Cancel is the only exit (no footerBackHome stack).
        spec.button("page:pick_challenge", UltraStyle.dim("Cancel"), "dim").rowGap();
        spec.send(player);
    }

    private static void openReplacePick(ServerPlayer player) {
        UltraScreenSpec spec = base("pick_replace_mutual", "actions")
                .line(UltraStyle.LABEL + "Choose which mutual rival to replace.")
                .notice("This frees a mutual slot for the incoming request.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
        UltraPickHelper.placeNameButtons(spec, "accept_replace", RivalGuiApi.currentRivalArgs(player), 6);
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openAdmin(ServerPlayer player) {
        if (!StaffAccess.isStaff(player)) {
            openHome(player);
            return;
        }
        UltraScreenSpec spec = base("admin", "actions")
                .line(UltraStyle.LABEL + "Staff rival tools")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
        spec.buttonLeft("do:admin:save", UltraStyle.confirm("Save data"), "confirm")
                .buttonRight("do:admin:refresh", UltraStyle.confirm("Reload"), "confirm")
                .rowGap();
        spec.button("do:admin:status", UltraStyle.dim("Status"), "dim").rowGap();
        spec.footerBackHome();
        spec.send(player);
    }
}
