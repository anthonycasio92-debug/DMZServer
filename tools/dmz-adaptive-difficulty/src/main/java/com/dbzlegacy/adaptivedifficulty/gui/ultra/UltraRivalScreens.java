package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.RivalGuiApi;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Rival system - CNPC parity via page: navigation and arg-bearing do: buttons.
 * Tabs: Home / Actions / Challenge. Sub-pages for list, records, pickers, requests.
 */
public final class UltraRivalScreens {
    /** Online player pickers: two-up rows; page so large servers never truncate. */
    private static final int PICK_PER_PAGE = 8;

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
        if (p.startsWith("replace_confirm:")) {
            openReplaceConfirm(player, raw.substring("replace_confirm:".length()).trim());
            return;
        }
        if (p.startsWith("accept_confirm:")) {
            openAcceptConfirm(player, raw.substring("accept_confirm:".length()).trim());
            return;
        }
        int pickPage = parsePickPage(p);
        if (pickPage >= 0) {
            String kind = pickKind(p);
            if ("declare".equals(kind)) {
                openNamePick(player, "Declare a rival", "declare", "actions", pickPage);
                return;
            }
            if ("silent".equals(kind)) {
                openNamePick(player, "Silent declare", "silent", "actions", pickPage);
                return;
            }
            if ("challenge".equals(kind) || "challenge_pick".equals(kind)) {
                openNamePick(player, "Challenge rival", "challenge_pick", "challenge", pickPage);
                return;
            }
            if ("spectate".equals(kind)) {
                openNamePick(player, "Spectate", "spectate", "challenge", pickPage);
                return;
            }
        }
        switch (p) {
            case "actions", "action" -> openActions(player);
            case "challenge", "challenges" -> openChallenge(player);
            case "list" -> openList(player);
            case "top", "leaderboard" -> openTop(player);
            case "records" -> openRecords(player, "history");
            case "requests", "pending", "invites" -> openRequests(player);
            case "settings" -> openSettings(player);
            case "pick_declare" -> openNamePick(player, "Declare a rival", "declare", "actions", 0);
            case "pick_silent" -> openNamePick(player, "Silent declare", "silent", "actions", 0);
            case "pick_challenge" -> openNamePick(player, "Challenge rival", "challenge_pick", "challenge", 0);
            case "pick_spectate" -> openNamePick(player, "Spectate", "spectate", "challenge", 0);
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
                .line(UltraStyle.LABEL + "Tiers: Silent (private) - Declared (visible) - Mutual (both) - Nemesis")
                .line(UltraStyle.DIM + "Nemesis rises from challenge deaths. Mutual slots are limited.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        UltraPickHelper.placePageButtons(
                spec, "list|View rival list", "top|View leaderboard", "records|View records");
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openActions(ServerPlayer player) {
        Map<String, String> ph = RivalGuiApi.placeholders(player);
        UltraScreenSpec spec = base("actions", "actions")
                .line(UltraStyle.LABEL + "Silent: only you track them. Declare: they get an invite.")
                .line(UltraStyle.DIM + "Accept a declare to become Mutual. Mutual slots are limited.")
                .labelValue("Pending invites", ph.getOrDefault("pending_invites", "0"))
                .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
        UltraPickHelper.placePageButtons(
                spec,
                "pick_declare|Declare a rival",
                "pick_silent|Silent declare",
                "requests|Review requests",
                "settings|Settings");
        if (RivalGuiApi.needsMutualReplacePick(player)) {
            String pending = RivalGuiApi.pendingMutualAcceptName(player);
            String pendingPlain =
                    pending == null || pending.isBlank() ? "them" : UltraStyle.plain(pending);
            // Same flow as after Accept — keep Mutual-cap work on Actions, not a mystery red tile.
            spec.button(
                            "page:pick_replace_mutual",
                            UltraStyle.danger("Drop a Mutual to accept " + pendingPlain),
                            "danger")
                    .rowGap();
        }
        if (StaffAccess.isStaff(player)) {
            spec.buttonLeft("page:admin", UltraStyle.dim("Staff Admin"), "dim").rowGap();
        }
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openChallenge(ServerPlayer player) {
        UltraScreenSpec spec = base("challenge", "challenge")
                .line(UltraStyle.LABEL + "Challenges are timed fights. Challenge deaths climb Nemesis.");
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
        placePerChallengeButtons(spec, player);
        spec.button("do:spectate_stop", UltraStyle.dim("Stop spectate"), "dim").rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    /** One Accept/Decline (or Cancel) pair per pending challenge — never a generic button. */
    private static void placePerChallengeButtons(UltraScreenSpec spec, ServerPlayer player) {
        List<String> cards = RivalGuiApi.pendingChallengeCards(player);
        if (cards == null || cards.isEmpty()) {
            return;
        }
        int placed = 0;
        for (String card : cards) {
            if (placed >= 6 || card == null || card.isBlank()) {
                continue;
            }
            String[] p = card.split("\t", -1);
            if (p.length < 3) {
                continue;
            }
            String uuid = p[0] == null ? "" : p[0].trim();
            String name = p.length > 1 && p[1] != null && !p[1].isBlank() ? p[1].trim() : uuid;
            String dir = p[2] == null ? "" : p[2].trim();
            String picker = uuid.isBlank() ? name : "uuid:" + uuid;
            if (picker.isBlank()) {
                continue;
            }
            String shown = RivalGuiApi.displayPickerArg(player, picker);
            if (shown == null || shown.isBlank()) {
                shown = name.isBlank() ? "-" : name;
            }
            String plain = UltraStyle.plain(shown);
            if ("IN".equalsIgnoreCase(dir)) {
                spec.button(
                                "do:challenge_accept:" + picker,
                                UltraStyle.confirm("Accept " + plain),
                                "confirm")
                        .rowGap();
                spec.button(
                                "do:challenge_decline:" + picker,
                                UltraStyle.danger("Decline " + plain),
                                "danger")
                        .rowGap();
            } else {
                spec.button(
                                "do:challenge_cancel:" + picker,
                                UltraStyle.danger("Cancel vs " + plain),
                                "danger")
                        .rowGap();
            }
            placed++;
        }
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
                String shown = RivalGuiApi.displayPickerArg(player, arg);
                if (shown == null || shown.isBlank()) {
                    shown = "-";
                }
                String label = UltraStyle.confirm(shown);
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
        // Named destination - rival list (no footerBackHome stack).
        spec.button("page:list", UltraStyle.backTo("Rival list"), "dim").rowGap();
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
                .line(UltraStyle.LABEL + "Incoming and outgoing rival requests.")
                .line(UltraStyle.DIM + "Accept a declare to become Mutual. Decline leaves them Declared only.");
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
        int placed = 0;
        if (incoming != null) {
            for (String arg : incoming) {
                if (placed >= 4 || arg == null || arg.isBlank()) {
                    continue;
                }
                String shown = RivalGuiApi.displayPickerArg(player, arg);
                if (shown == null || shown.isBlank()) {
                    shown = "-";
                }
                String plain = UltraStyle.plain(shown);
                // Stack Accept above Decline per player — side-by-side is a misclick risk.
                spec.button("do:accept:" + arg, UltraStyle.confirm("Accept " + plain), "confirm")
                        .rowGap();
                spec.button("do:decline:" + arg, UltraStyle.danger("Decline " + plain), "danger")
                        .rowGap();
                placed++;
            }
        }
        if (placed == 0) {
            spec.line(UltraStyle.dim("No incoming declare requests right now."));
        }
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
            ServerPlayer player, String heading, String doAction, String returnTab, int pageIndex) {
        String tab = "actions".equals(returnTab) ? "actions" : "challenge";
        String pageBase = "pick_" + ("challenge_pick".equals(doAction) ? "challenge" : doAction);
        List<String> args = RivalGuiApi.onlinePlayerArgs(player);
        int total = args == null ? 0 : args.size();
        int pages = Math.max(1, (total + PICK_PER_PAGE - 1) / PICK_PER_PAGE);
        int pg = Math.min(pages - 1, Math.max(0, pageIndex));
        int from = pg * PICK_PER_PAGE;
        int to = Math.min(total, from + PICK_PER_PAGE);
        UltraScreenSpec spec = base(pg > 0 ? pageBase + ":" + pg : pageBase, tab)
                .line(UltraStyle.LABEL + heading)
                .line(UltraStyle.DIM + pickExplainer(doAction))
                .labelValue("Players", total + (pages > 1 ? (" | page " + (pg + 1) + "/" + pages) : ""))
                .setRow(UltraStyle.CONTENT_ROW_TABS + 70);
        int placed = 0;
        for (int i = from; i < to; i++) {
            String arg = args.get(i);
            if (arg == null || arg.isBlank()) {
                continue;
            }
            String shown = RivalGuiApi.displayPickerArg(player, arg);
            if (shown == null || shown.isBlank()) {
                shown = "-";
            }
            String plain = UltraStyle.plain(shown);
            String action;
            String label;
            String style;
            if ("challenge_pick".equals(doAction)) {
                action = "page:challenge_time:" + arg;
                label = UltraStyle.confirm(plain);
                style = UltraStyle.STYLE_CONFIRM;
            } else if ("declare".equals(doAction) && RivalGuiApi.hasIncomingDeclareFrom(player, arg)) {
                // They already declared you — never label this Declare (that would lie).
                action = "page:accept_confirm:" + arg;
                label = UltraStyle.confirm("Accept their declare");
                style = UltraStyle.STYLE_CONFIRM;
            } else if ("declare".equals(doAction)) {
                action = "do:declare:" + arg;
                label = UltraStyle.confirm("Declare " + plain);
                style = UltraStyle.STYLE_CONFIRM;
            } else if ("silent".equals(doAction)) {
                action = "do:silent:" + arg;
                label = UltraStyle.confirm("Silent " + plain);
                style = UltraStyle.STYLE_CONFIRM;
            } else {
                action = "do:" + doAction + ":" + arg;
                label = UltraStyle.confirm(plain);
                style = UltraStyle.STYLE_CONFIRM;
            }
            if (placed % 2 == 0) {
                spec.buttonLeft(action, label, style);
            } else {
                spec.buttonRight(action, label, style);
                spec.rowGap();
            }
            placed++;
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        if (placed == 0) {
            spec.line(UltraStyle.dim("Nobody is available right now."));
        }
        if (pg > 0) {
            spec.buttonLeft("page:" + pageBase + ":" + (pg - 1), UltraStyle.dim("Prev"), UltraStyle.STYLE_DIM);
        }
        if (pg + 1 < pages) {
            if (pg > 0) {
                spec.buttonRight(
                        "page:" + pageBase + ":" + (pg + 1),
                        UltraStyle.LABEL + "Next",
                        UltraStyle.STYLE_NORMAL);
            } else {
                spec.buttonLeft(
                        "page:" + pageBase + ":" + (pg + 1),
                        UltraStyle.LABEL + "Next",
                        UltraStyle.STYLE_NORMAL);
            }
            spec.rowGap();
        } else if (pg > 0) {
            spec.rowGap();
        }
        // Cancel is the only exit (no footerBackHome stack).
        spec.button("page:" + ("challenge".equals(tab) ? "challenge" : "actions"), UltraStyle.dim("Cancel"), "dim")
                .rowGap();
        spec.send(player);
    }

    private static void openChallengeTime(ServerPlayer player, String target) {
        String who = RivalGuiApi.displayPickerArg(player, target);
        UltraScreenSpec spec = base("challenge_time:" + target, "challenge")
                .line(UltraStyle.LABEL + "Challenge length")
                .labelValue("Rival", who == null || who.isBlank() ? "-" : who)
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

    private static void openAcceptConfirm(ServerPlayer player, String arg) {
        String who = RivalGuiApi.displayPickerArg(player, arg);
        String plain = who == null || who.isBlank() ? "-" : UltraStyle.plain(who);
        UltraScreenSpec spec = base("accept_confirm:" + arg, "actions")
                .line(UltraStyle.LABEL + "They already declared you")
                .labelValue("Player", plain)
                .notice("Accept mutual rivalry with " + plain + " instead of sending another declare?")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 100);
        if (arg != null && !arg.isBlank()) {
            spec.button("do:accept:" + arg, UltraStyle.confirm("Accept mutual rivalry"), "confirm")
                    .rowGap();
        }
        spec.button("page:pick_declare", UltraStyle.dim("Cancel"), "dim").rowGap();
        spec.send(player);
    }

    private static void openReplacePick(ServerPlayer player) {
        String pending = RivalGuiApi.pendingMutualAcceptName(player);
        String pendingPlain = pending == null || pending.isBlank() ? "them" : UltraStyle.plain(pending);
        UltraScreenSpec spec = base("pick_replace_mutual", "actions")
                .line(UltraStyle.LABEL + "Drop a Mutual to accept " + pendingPlain)
                .notice("Mutual slots are full. Pick who to demote to Declared so you can accept "
                        + pendingPlain + ".")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
        List<String> choices = labeledArgs(player, RivalGuiApi.mutualRivalArgs(player));
        // Navigate to confirm — do not accept_replace immediately.
        int placed = 0;
        for (String raw : choices) {
            if (placed >= 6 || raw == null || raw.isBlank()) {
                continue;
            }
            int bar = raw.indexOf('|');
            String arg = bar > 0 ? raw.substring(0, bar).trim() : raw.trim();
            String label = bar > 0 ? raw.substring(bar + 1).trim() : arg;
            if (arg.isBlank()) {
                continue;
            }
            if (label.isBlank()) {
                label = arg;
            }
            String action = "page:replace_confirm:" + arg;
            String styled = UltraStyle.danger("Drop " + UltraStyle.plain(label));
            if (placed % 2 == 0) {
                spec.buttonLeft(action, styled, "danger");
            } else {
                spec.buttonRight(action, styled, "danger");
                spec.rowGap();
            }
            placed++;
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        if (placed == 0) {
            spec.line(UltraStyle.dim("No Mutual rivals to drop right now."));
        }
        // Escape hatch — do not trap the player on this page.
        spec.button("do:cancel_mutual_replace", UltraStyle.dim("Decline instead"), "dim").rowGap();
        // Named destination - requests (no footerBackHome stack).
        spec.button("page:requests", UltraStyle.backTo("Requests"), "dim").rowGap();
        spec.send(player);
    }

    private static void openReplaceConfirm(ServerPlayer player, String dropArg) {
        String pending = RivalGuiApi.pendingMutualAcceptName(player);
        String pendingPlain = pending == null || pending.isBlank() ? "them" : UltraStyle.plain(pending);
        String dropShown = RivalGuiApi.displayPickerArg(player, dropArg);
        String dropPlain = dropShown == null || dropShown.isBlank() ? "-" : UltraStyle.plain(dropShown);
        UltraScreenSpec spec = base("replace_confirm:" + dropArg, "actions")
                .line(UltraStyle.LABEL + "Confirm Mutual demotion")
                .labelValue("Accept", pendingPlain)
                .labelValue("Demote", dropPlain)
                .notice("This will demote " + dropPlain + " from Mutual to Declared. Continue?")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 100);
        // Stack Continue above Cancel — side-by-side is a misclick demotion risk.
        if (dropArg != null && !dropArg.isBlank()) {
            spec.button(
                            "do:accept_replace:" + dropArg,
                            UltraStyle.danger("Continue"),
                            "danger")
                    .rowGap();
        }
        spec.button("page:pick_replace_mutual", UltraStyle.dim("Cancel"), "dim").rowGap();
        spec.send(player);
    }

    /** Short action copy so pickers never imply a different tier outcome. */
    private static String pickExplainer(String doAction) {
        return switch (doAction == null ? "" : doAction) {
            case "declare" ->
                    "Declare sends them an invite. You stay Declared. If they already declared you, use Accept their declare.";
            case "silent" ->
                    "Silent is private - they are not notified. If they Silent you too, it becomes Declared.";
            case "challenge_pick" -> "Pick who to challenge. Next step chooses length.";
            case "spectate" -> "Spectate an online rival fight.";
            default -> "Choose an online player.";
        };
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
                .buttonRight("do:admin:refresh", UltraStyle.confirm("Reload data"), "confirm")
                .rowGap();
        spec.button("do:admin:status", UltraStyle.dim("Status"), "dim").rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static List<String> labeledArgs(ServerPlayer player, List<String> args) {
        List<String> out = new ArrayList<>();
        if (args == null) {
            return out;
        }
        for (String arg : args) {
            if (arg == null || arg.isBlank()) {
                continue;
            }
            String shown = RivalGuiApi.displayPickerArg(player, arg);
            if (shown == null || shown.isBlank()) {
                shown = "-";
            }
            out.add(arg + "|" + shown);
        }
        return out;
    }

    /** {@code pick_declare:2} → 2; {@code pick_declare} → 0; unrelated → -1. */
    private static int parsePickPage(String page) {
        if (page == null || page.isBlank()) {
            return -1;
        }
        String p = page.toLowerCase(Locale.ROOT);
        for (String kind : new String[] {"declare", "silent", "challenge", "spectate"}) {
            String prefix = "pick_" + kind;
            if (p.equals(prefix)) {
                return 0;
            }
            if (p.startsWith(prefix + ":")) {
                try {
                    return Integer.parseInt(p.substring(prefix.length() + 1).trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return -1;
    }

    private static String pickKind(String page) {
        if (page == null || page.isBlank()) {
            return "";
        }
        String p = page.toLowerCase(Locale.ROOT);
        if (p.startsWith("pick_")) {
            String rest = p.substring("pick_".length());
            int colon = rest.indexOf(':');
            return colon < 0 ? rest : rest.substring(0, colon);
        }
        return "";
    }
}
