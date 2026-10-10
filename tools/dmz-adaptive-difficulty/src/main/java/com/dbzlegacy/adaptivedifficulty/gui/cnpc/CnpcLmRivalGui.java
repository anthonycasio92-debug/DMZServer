package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.gui.GuiClickConfirm;
import com.dbzlegacy.adaptivedifficulty.gui.RivalGuiApi;
import com.dbzlegacy.adaptivedifficulty.rival.RivalChallengeManager;
import com.dbzlegacy.adaptivedifficulty.rival.RivalSystem;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IScroll;

public final class CnpcLmRivalGui {
    private static final int H = 360;

    private CnpcLmRivalGui() {}

    public static void open(ServerPlayer player, String page) {
        CnpcUltraStyle.withAccent(CnpcUltraStyle.ACCENT_RIVAL, () -> openRival(player, page));
    }

    private static void openRival(ServerPlayer player, String page) {
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("challenge_time:")) {
            openChallengeTime(player, raw.substring("challenge_time:".length()).trim());
            return;
        }
        if (lower.startsWith("pending_decide:")) {
            openPendingDecide(player, raw.substring("pending_decide:".length()).trim());
            return;
        }
        if (lower.startsWith("challenge_decide:")) {
            openChallengeDecide(player, raw.substring("challenge_decide:".length()).trim());
            return;
        }
        if (lower.startsWith("list_detail:")) {
            openListDetail(player, raw.substring("list_detail:".length()).trim());
            return;
        }
        if (lower.startsWith("pick_confirm:")) {
            openPickConfirm(player, raw.substring("pick_confirm:".length()).trim());
            return;
        }
        final String pageKey = lower.isBlank() || "main".equals(lower) || "actions".equals(lower)
                ? "challenges" : lower;
        int height = heightForPage(player, pageKey);
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, height, (pl, gui) -> {
            String records = recordsTab(pageKey);
            if (records != null) {
                paintRecords(pl, gui, records);
                return;
            }
            switch (pageKey) {
                case "list" -> paintList(pl, gui);
                case "pending", "invites", "requests", "challenge_pending", "challenge_requests" ->
                        paintRequests(pl, gui);
                case "challenge", "challenges" -> paintChallenges(pl, gui);
                case "top", "leaderboard" -> paintScroll(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Leaderboard"), RivalGuiApi.topLines(subject(pl)), "records");
                case "pick_declare" -> paintNamePick(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Declare rival"), RivalGuiApi.onlinePlayerNames(subject(pl)),
                        "declare", "list");
                case "pick_silent" -> paintNamePick(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Silent rival"), RivalGuiApi.onlinePlayerNames(subject(pl)),
                        "silent", "list");
                case "pick_accept", "pick_decline" -> open(pl, "requests");
                case "pick_remove" -> open(pl, "list");
                case "pick_replace_mutual", "replace_mutual" -> paintRivalCardPick(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Replace mutual slot"),
                        RivalGuiApi.currentRivalCards(subject(pl)), "accept_replace", "list");
                case "pick_challenge" -> paintNamePick(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Challenge rival"), RivalGuiApi.onlinePlayerNames(subject(pl)),
                        "challenge_pick", "challenges");
                case "pick_spectate" -> paintNamePick(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Spectate"),
                        RivalChallengeManager.get().activeFighterNames(),
                        "spectate", "challenges");
                case "admin" -> paintAdmin(pl, gui);
                case "settings" -> paintSettings(pl, gui);
                default -> paintChallenges(pl, gui);
            }
        });
    }

    private static int heightForPage(ServerPlayer player, String page) {
        int extra = rivalTabs(page) ? CnpcGuiSupport.TAB_BAR_H : 0;
        if (recordsTab(page) != null) {
            return CnpcGuiSupport.window(520 + extra);
        }
        return CnpcGuiSupport.window(switch (page) {
            case "list" -> 460;
            case "challenges", "challenge", "actions" -> 440;
            case "pending", "invites", "requests", "challenge_pending", "challenge_requests" -> 440;
            case "pick_declare", "pick_silent", "pick_challenge", "pick_spectate",
                    "pick_accept", "pick_decline", "pick_replace_mutual" -> 400;
            case "top", "leaderboard" -> 440;
            default -> H + extra;
        });
    }

    /** Every rival page keeps the three-tab bar, including sub-pages. */
    private static boolean rivalTabs(String page) {
        return page != null && !page.isBlank();
    }

    private static String rivalTabId(String page) {
        if (page == null || page.isBlank()) {
            return "challenges";
        }
        String p = page.toLowerCase(Locale.ROOT);
        if (p.startsWith("pending_decide") || p.startsWith("challenge_decide")) {
            return "requests";
        }
        if (p.startsWith("list_detail") || p.startsWith("pick_declare") || p.startsWith("pick_silent")
                || p.startsWith("pick_replace") || "replace_mutual".equals(p)) {
            return "records";
        }
        if (p.startsWith("challenge_time") || p.startsWith("pick_challenge") || p.startsWith("pick_spectate")) {
            return "challenges";
        }
        if (recordsTab(p) != null) {
            return "records";
        }
        return switch (p) {
            case "pending", "invites", "requests", "challenge_pending", "challenge_requests" -> "requests";
            case "list", "top", "leaderboard", "settings", "admin" -> "records";
            default -> "challenges";
        };
    }

    private static int rivalTabBar(ServerPlayer player, ICustomGui gui, int y, String active) {
        return CnpcGuiSupport.paintTabBar(gui, y, new String[] {
                "challenges|Challenges", "requests|Requests", "records|Records"
        }, active, action -> {
            String id = action.startsWith("tab:") ? action.substring(4) : action;
            switch (id) {
                case "requests" -> open(player, "requests");
                case "records" -> open(player, "records");
                default -> open(player, "challenges");
            }
        });
    }

    private static int rivalChrome(ServerPlayer player, ICustomGui gui, int y, String page) {
        return rivalTabBar(player, gui, y, rivalTabId(page));
    }

    /**
     * History, stats, progress, season, quests, achievements, hall of fame, journal,
     * and title share one Records page. {@code null} means this page is not records.
     */
    private static String recordsTab(String page) {
        if (page == null || page.isBlank()) {
            return null;
        }
        String p = page.toLowerCase(Locale.ROOT);
        if (p.startsWith("records:")) {
            p = p.substring("records:".length()).trim();
        }
        return switch (p) {
            case "history", "past", "previous", "records", "more" -> "history";
            case "progress", "season" -> "season";
            case "stats", "statistics" -> "stats";
            case "quests", "quest" -> "quests";
            case "achievements", "achs", "ach" -> "achievements";
            case "hof", "hall" -> "hof";
            case "journal" -> "journal";
            case "title", "titles" -> "title";
            default -> null;
        };
    }

    private static ServerPlayer subject(ServerPlayer viewer) {
        return CnpcGuiSupport.target(viewer);
    }

    private static void act(ServerPlayer viewer, String action, String arg, String returnPage) {
        CnpcGuiSupport.act(viewer, () -> RivalGuiApi.handleDo(subject(viewer), action, arg, returnPage),
                () -> open(viewer, returnPage));
    }

    /** Unknown pages land on Challenges. */
    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        paintChallenges(player, gui);
    }

    private static void paintSettings(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        Map<String, String> ph = RivalGuiApi.placeholders(who);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Settings"),
                CnpcUltraStyle.SUBTITLE + "Chat and instinct");
        infoY = rivalChrome(player, gui, infoY, "settings");
        int row = CnpcGuiSupport.bodyBelowHeader(infoY);
        boolean tpOn = "true".equalsIgnoreCase(ph.get("tpMsg"));
        CnpcGuiSupport.button(gui, 26, tpOn ? CnpcGuiStyle.toggleOn("TP")
                : CnpcGuiStyle.toggleOff("TP"),
                CnpcGuiSupport.COL_L, row, () -> act(
                player, "tpmsg", "toggle", "settings"));
        if ("true".equalsIgnoreCase(ph.get("instinct_feature"))) {
            boolean instinctOn = "true".equalsIgnoreCase(ph.get("instinct"));
            CnpcGuiSupport.button(gui, 27, instinctOn ? CnpcGuiStyle.toggleOn("Rival instinct")
                    : CnpcGuiStyle.toggleOff("Rival instinct"),
                    CnpcGuiSupport.COL_R, row,
                    () -> act(player, "instinct", "toggle", "settings"));
        }
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, "records");
    }

    private static void paintChallenges(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        Map<String, String> ph = RivalGuiApi.placeholders(who);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Challenges"),
                CnpcUltraStyle.SUBTITLE + "Send a challenge, see the live fight, or spectate");
        infoY = rivalTabBar(player, gui, infoY, "challenges");
        if (!"true".equalsIgnoreCase(ph.get("system_enabled"))) {
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(CnpcGuiStyle.MSG_RIVALS_OFF),
                            CnpcGuiStyle.INFO_INLINE_MAX));
            footer(player, gui, row + 8, null);
            return;
        }
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, RivalGuiApi.challengeLines(who),
                        CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.INFO + "Send challenge…", CnpcGuiSupport.COL_L, row,
                () -> open(player, "pick_challenge"));
        CnpcGuiSupport.button(gui, 24, CnpcUltraStyle.ACCENT + "Spectate…", CnpcGuiSupport.COL_R, row,
                () -> open(player, "pick_spectate"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 25, CnpcUltraStyle.DIM + "Stop spectate", CnpcGuiSupport.COL_L, row,
                () -> act(player, "spectate_stop", "", "challenges"));
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, null);
    }

    /**
     * One Requests tab: incoming and outgoing declare invites and challenge requests.
     * Each row names who, the kind, and whether the row is Accept/Decline or Cancel.
     */
    private static void paintRequests(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Requests"),
                CnpcUltraStyle.SUBTITLE + "Declares and challenges you sent or received");
        infoY = rivalTabBar(player, gui, infoY, "requests");
        List<String> cards = new ArrayList<>();
        List<String> kinds = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (String card : RivalGuiApi.pendingInviteCards(who)) {
            cards.add(card);
            kinds.add("invite");
            labels.add(requestRowLabel(card, true));
        }
        for (String card : RivalGuiApi.pendingChallengeCards(who)) {
            cards.add(card);
            kinds.add("challenge");
            labels.add(requestRowLabel(card, false));
        }
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (cards.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    CnpcUltraStyle.SUBTITLE + "No pending requests. A declare invite or a challenge request will show up here.",
                    CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, null);
            return;
        }
        int rowsBelow = 3;
        String[] rowLabels = labels.toArray(String[]::new);
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, rowLabels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, rowLabels);
        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.button(gui, 31, CnpcUltraStyle.CONFIRM + "Accept", CnpcGuiSupport.COL_L, row,
                () -> respondRequest(player, scroll, cards, kinds, "accept"));
        CnpcGuiSupport.button(gui, 32, CnpcUltraStyle.DANGER + "Decline", CnpcGuiSupport.COL_R, row,
                () -> respondRequest(player, scroll, cards, kinds, "decline"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 33, CnpcUltraStyle.DANGER + "Cancel", CnpcGuiSupport.COL_L, row,
                () -> respondRequest(player, scroll, cards, kinds, "cancel"));
        footer(player, gui, row + CnpcGuiSupport.ROW_STEP, null);
    }

    /** Scroll text: direction, kind, who, and the action that row accepts. */
    private static String requestRowLabel(String card, boolean invite) {
        String[] p = card == null ? new String[0] : card.split("\t", -1);
        String name = p.length > 1 && p[1] != null && !p[1].isBlank() ? p[1].trim() : "?";
        boolean outgoing = invite
                ? RivalGuiApi.pendingInviteCardOutgoing(card)
                : RivalGuiApi.pendingChallengeCardOutgoing(card);
        String kind = invite ? "Declare invite" : "Challenge request";
        String way = outgoing ? "Outgoing" : "Incoming";
        String action = outgoing ? "Cancel" : "Accept / Decline";
        return CnpcUltraStyle.BODY + way + " " + kind + " · " + name + " · " + action;
    }

    private static void respondRequest(
            ServerPlayer player, IScroll scroll, List<String> cards, List<String> kinds, String verb) {
        int[] sel = scroll == null ? null : scroll.getSelection();
        if (sel == null || sel.length == 0 || sel[0] < 0 || sel[0] >= cards.size()) {
            CnpcGuiSupport.feedback(player, CnpcUltraStyle.SUBTITLE + "Select a request first.");
            open(player, "requests");
            return;
        }
        String card = cards.get(sel[0]);
        boolean invite = "invite".equals(kinds.get(sel[0]));
        String arg = CnpcGuiSupport.rivalPickerArgFromCard(card);
        boolean outgoing = invite
                ? RivalGuiApi.pendingInviteCardOutgoing(card)
                : RivalGuiApi.pendingChallengeCardOutgoing(card);
        String who = RivalGuiApi.displayPickerArg(player, arg);
        if ("cancel".equals(verb)) {
            if (!outgoing) {
                CnpcGuiSupport.feedback(player, CnpcUltraStyle.SUBTITLE + "That one is incoming. Use Decline.");
                open(player, "requests");
                return;
            }
            if (invite) {
                confirmAct(player, "rival-withdraw:" + arg,
                        CnpcUltraStyle.SUBTITLE + "Cancel your declare invite to " + CnpcUltraStyle.BODY + who
                                + CnpcUltraStyle.SUBTITLE + "? " + CnpcUltraStyle.INFO + "Click again within 10 seconds.",
                        "requests",
                        () -> act(player, "remove", arg, "requests"));
            } else {
                confirmAct(player, "challenge-cancel:" + arg,
                        CnpcUltraStyle.SUBTITLE + "Cancel your challenge request to " + CnpcUltraStyle.BODY + who
                                + CnpcUltraStyle.SUBTITLE + "? " + CnpcUltraStyle.INFO + "Click again within 10 seconds.",
                        "requests",
                        () -> act(player, "challenge_cancel", arg, "requests"));
            }
            return;
        }
        if (outgoing) {
            CnpcGuiSupport.feedback(player, CnpcUltraStyle.SUBTITLE + "That one is outgoing. Use Cancel.");
            open(player, "requests");
            return;
        }
        if (invite) {
            act(player, verb, arg, "requests");
        } else {
            act(player, "accept".equals(verb) ? "challenge_accept" : "challenge_decline", arg, "requests");
        }
    }

    private static void openPendingDecide(ServerPlayer player, String pickerArg) {
        ServerPlayer who = subject(player);
        List<String> cards = RivalGuiApi.pendingInviteCards(who);
        String card = CnpcGuiSupport.findCardByPickerArg(cards, pickerArg);
        List<String> detail = card != null ? RivalGuiApi.pendingInviteDetailLines(card)
                : List.of(CnpcUltraStyle.SUBTITLE + "Pending declare", RivalGuiApi.displayPickerArg(player, pickerArg));
        boolean outgoing = card != null && RivalGuiApi.pendingInviteCardOutgoing(card);
        boolean mutual = card != null && RivalGuiApi.pendingInviteCardMutualConfirm(card);
        String subTitle = outgoing ? "Outgoing declare" : (mutual ? "Mutual confirm" : "Incoming declare");
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, CnpcGuiSupport.window(300), (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", subTitle),
                    outgoing ? CnpcUltraStyle.SUBTITLE + "Withdraw or wait for their answer" : CnpcUltraStyle.SUBTITLE + "Accept or decline below");
            infoY = rivalChrome(pl, gui, infoY, "requests");
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, detail, CnpcGuiStyle.INFO_INLINE_MAX));
            row += 8;
            if (outgoing) {
                CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.DANGER + "Withdraw declare", CnpcGuiSupport.COL_L, row,
                        () -> confirmAct(pl, "rival-withdraw:" + pickerArg,
                                CnpcUltraStyle.SUBTITLE + "Withdraw your declare invite to " + CnpcUltraStyle.BODY
                                        + RivalGuiApi.displayPickerArg(pl, pickerArg)
                                        + CnpcUltraStyle.SUBTITLE + "? " + CnpcUltraStyle.INFO + "Click again within 10 seconds.",
                                "pending_decide:" + pickerArg,
                                () -> act(pl, "remove", pickerArg, "requests")));
                CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.SUBTITLE + "Keep waiting", CnpcGuiSupport.COL_R, row, () -> open(pl, "requests"));
            } else if (mutual) {
                CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Accept mutual", CnpcGuiSupport.COL_L, row,
                        () -> act(pl, "accept", pickerArg, "requests"));
                CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.DANGER + "Decline mutual", CnpcGuiSupport.COL_R, row,
                        () -> act(pl, "decline", pickerArg, "requests"));
            } else {
                CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Accept", CnpcGuiSupport.COL_L, row,
                        () -> act(pl, "accept", pickerArg, "requests"));
                CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.DANGER + "Decline", CnpcGuiSupport.COL_R, row,
                        () -> act(pl, "decline", pickerArg, "requests"));
            }
            row += CnpcGuiSupport.ROW_STEP;
            CnpcGuiSupport.navBackToParent(pl, gui, row, () -> open(pl, "requests"));
        });
    }

    private static void openListDetail(ServerPlayer player, String pickerArg) {
        ServerPlayer who = subject(player);
        List<String> cards = RivalGuiApi.currentRivalCards(who);
        String card = CnpcGuiSupport.findCardByPickerArg(cards, pickerArg);
        List<String> detail = card != null ? RivalGuiApi.rivalCardDetailLines(card)
                : List.of(RivalGuiApi.displayPickerArg(player, pickerArg), CnpcUltraStyle.SUBTITLE + "Rival record");
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, CnpcGuiSupport.window(300), (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Profile"),
                    CnpcUltraStyle.SUBTITLE + "Stats and actions for this rival");
            infoY = rivalChrome(pl, gui, infoY, "records");
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, detail, CnpcGuiStyle.INFO_INLINE_MAX));
            row += 8;
            CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.DANGER + "Remove rival", CnpcGuiSupport.COL_L, row,
                    () -> confirmAct(pl, "rival-remove:" + pickerArg,
                            CnpcUltraStyle.SUBTITLE + "End your rivalry with " + CnpcUltraStyle.BODY + RivalGuiApi.displayPickerArg(pl, pickerArg)
                                    + CnpcUltraStyle.SUBTITLE + "? They'll move to History. " + CnpcUltraStyle.INFO + "Click again within 10 seconds.",
                            "list_detail:" + pickerArg,
                            () -> act(pl, "remove", pickerArg, "list")));
            row += CnpcGuiSupport.ROW_STEP;
            CnpcGuiSupport.navBackToParent(pl, gui, row, () -> open(pl, "list"));
        });
    }

    /** {@code payload} = {@code action|returnPage|targetArg} (target may contain {@code :}). */
    private static void openPickConfirm(ServerPlayer player, String payload) {
        String[] parts = payload.split("\\|", 3);
        if (parts.length < 3) {
            open(player, "challenges");
            return;
        }
        String action = parts[0];
        String returnPage = parts[1];
        String targetArg = parts[2];
        String display = RivalGuiApi.displayPickerArg(player, targetArg);
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, CnpcGuiSupport.window(280), (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, confirmTitle(action), display);
            infoY = rivalChrome(pl, gui, infoY, returnPage);
            List<String> lines = confirmBody(action, display);
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, CnpcGuiStyle.INFO_INLINE_MAX));
            row += 8;
            CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Confirm", CnpcGuiSupport.COL_L, row, () -> {
                if ("challenge_pick".equals(action)) {
                    CnpcGuiSupport.runDeferred(pl, () -> open(pl, "challenge_time:" + targetArg));
                } else {
                    act(pl, action, targetArg, returnPage);
                }
            });
            CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.SUBTITLE + "Cancel", CnpcGuiSupport.COL_R, row, () -> open(pl, returnPage));
            row += CnpcGuiSupport.ROW_STEP;
            CnpcGuiSupport.navBackToParent(pl, gui, row, () -> open(pl, returnPage));
        });
    }

    private static void confirmAct(
            ServerPlayer player, String key, String prompt, String reopen, Runnable go) {
        if (!GuiClickConfirm.confirmed(player.m_20148_(), key)) {
            CnpcGuiSupport.act(player, () -> prompt, () -> open(player, reopen));
            return;
        }
        go.run();
    }

    private static String confirmTitle(String action) {
        return switch (action) {
            case "declare" -> CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Declare rival");
            case "silent" -> CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Silent rival");
            case "challenge_pick" -> CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Challenge rival");
            case "spectate" -> CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Spectate");
            case "accept_replace" -> CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Replace mutual slot");
            default -> CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Confirm action");
        };
    }

    private static List<String> confirmBody(String action, String display) {
        return switch (action) {
            case "declare" -> List.of(
                    CnpcUltraStyle.SUBTITLE + "Declare " + CnpcUltraStyle.BODY + display + " " + CnpcUltraStyle.SUBTITLE + "as your rival? They'll get an invite to accept.");
            case "silent" -> List.of(
                    CnpcUltraStyle.SUBTITLE + "Mark " + CnpcUltraStyle.BODY + display + " " + CnpcUltraStyle.SUBTITLE + "as a silent rival? They won't be told.");
            case "challenge_pick" -> List.of(
                    CnpcUltraStyle.SUBTITLE + "Challenge " + CnpcUltraStyle.BODY + display + " " + CnpcUltraStyle.SUBTITLE + "to a timed challenge? Pick the length next.");
            case "spectate" -> List.of(
                    CnpcUltraStyle.SUBTITLE + "Watch " + CnpcUltraStyle.BODY + display + CnpcUltraStyle.SUBTITLE + "'s challenge? If they're not fighting you'll join any live challenge.");
            case "accept_replace" -> List.of(
                    CnpcUltraStyle.SUBTITLE + "Drop " + CnpcUltraStyle.BODY + display + " " + CnpcUltraStyle.SUBTITLE + "as a Mutual to make room? They'll stay in your history.");
            default -> List.of(CnpcUltraStyle.SUBTITLE + "Confirm this for " + CnpcUltraStyle.BODY + display + CnpcUltraStyle.SUBTITLE + ".");
        };
    }

    private static void openChallengeDecide(ServerPlayer player, String pickerArg) {
        ServerPlayer who = subject(player);
        List<String> cards = RivalGuiApi.pendingChallengeCards(who);
        String card = CnpcGuiSupport.findCardByPickerArg(cards, pickerArg);
        List<String> detail = card != null ? RivalGuiApi.pendingChallengeDetailLines(card)
                : List.of(CnpcUltraStyle.SUBTITLE + "Pending challenge", RivalGuiApi.displayPickerArg(player, pickerArg));
        boolean outgoing = card != null && RivalGuiApi.pendingChallengeCardOutgoing(card);
        String subTitle = outgoing ? "Outgoing challenge" : "Incoming challenge";
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, CnpcGuiSupport.window(300), (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", subTitle),
                    outgoing ? CnpcUltraStyle.SUBTITLE + "Cancel or keep waiting" : CnpcUltraStyle.SUBTITLE + "Accept or decline below");
            infoY = rivalChrome(pl, gui, infoY, "requests");
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, detail,
                    CnpcGuiStyle.INFO_INLINE_MAX));
            row += 8;
            if (outgoing) {
                CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.DANGER + "Cancel challenge", CnpcGuiSupport.COL_L, row,
                        () -> confirmAct(pl, "challenge-cancel:" + pickerArg,
                                CnpcUltraStyle.SUBTITLE + "Cancel your challenge request to " + CnpcUltraStyle.BODY
                                        + RivalGuiApi.displayPickerArg(pl, pickerArg)
                                        + CnpcUltraStyle.SUBTITLE + "? " + CnpcUltraStyle.INFO + "Click again within 10 seconds.",
                                "challenge_decide:" + pickerArg,
                                () -> act(pl, "challenge_cancel", pickerArg, "challenge_pending")));
                CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.SUBTITLE + "Keep waiting", CnpcGuiSupport.COL_R, row,
                        () -> open(pl, "challenge_pending"));
            } else {
                CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Accept", CnpcGuiSupport.COL_L, row,
                        () -> act(pl, "challenge_accept", pickerArg, "challenge_pending"));
                CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.DANGER + "Decline", CnpcGuiSupport.COL_R, row,
                        () -> act(pl, "challenge_decline", pickerArg, "challenge_pending"));
            }
            row += CnpcGuiSupport.ROW_STEP;
            CnpcGuiSupport.navBackToParent(pl, gui, row, () -> open(pl, "requests"));
        });
    }

    private static void openChallengeTime(ServerPlayer player, String targetArg) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, CnpcGuiSupport.window(360), (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Challenge length"),
                    CnpcUltraStyle.SUBTITLE + "Target " + RivalGuiApi.displayPickerArg(player, targetArg));
            infoY = rivalChrome(pl, gui, infoY, "challenges");
            int row = CnpcGuiSupport.bodyBelowHeader(infoY);
            CnpcGuiLayout.GridButton[] grid = new CnpcGuiLayout.GridButton[10];
            for (int min = 1; min <= 10; min++) {
                int m = min;
                String sendArg = targetArg + "@" + m;
                grid[min - 1] = CnpcGuiLayout.GridButton.action(
                        CnpcUltraStyle.BODY + m + " min",
                        () -> RivalGuiApi.handleDo(subject(pl), "challenge_send", sendArg, "challenge_pending"),
                        () -> open(pl, "challenge_pending"));
            }
            row = CnpcGuiLayout.paintTwoColumnButtonGrid(pl, gui, row, CnpcGuiSupport.ID_GRID_BASE, grid, () -> {});
            row += 4;
            CnpcGuiSupport.navBackToParent(pl, gui, row, () -> open(pl, "challenges"));
        });
    }

    private static void paintRecords(ServerPlayer player, ICustomGui gui, String tab) {
        String key = tab == null || tab.isBlank() ? "history" : tab;
        String label = recordsLabel(key);
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Records · " + label),
                CnpcGuiStyle.HINT_READ_ONLY);
        infoY = rivalTabBar(player, gui, infoY, "records");
        int row = CnpcGuiSupport.bodyBelowHeader(infoY);
        CnpcGuiSupport.button(gui, 70, CnpcUltraStyle.INFO + "Rival list", CnpcGuiSupport.COL_L, row,
                () -> open(player, "list"));
        CnpcGuiSupport.button(gui, 71, CnpcUltraStyle.ACCENT + "Leaderboard", CnpcGuiSupport.COL_R, row,
                () -> open(player, "top"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 72, CnpcUltraStyle.SUBTITLE + "Settings", CnpcGuiSupport.COL_L, row,
                () -> open(player, "settings"));
        if (StaffAccess.isStaff(player)) {
            CnpcGuiSupport.buttonSmall(gui, CnpcGuiSupport.ID_STAFF_EXTRA, CnpcUltraStyle.DANGER + "Staff Admin",
                    CnpcGuiSupport.COL_R, row, CnpcGuiSupport.BTN_W, () -> open(player, "admin"));
        }
        row += CnpcGuiSupport.ROW_STEP;
        String[] ids = {"history", "stats", "season", "quests", "achievements", "hof", "journal", "title"};
        CnpcGuiLayout.GridButton[] tabs = new CnpcGuiLayout.GridButton[ids.length];
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            String name = recordsLabel(id);
            String button = id.equals(key) ? CnpcUltraStyle.ACCENT + name : CnpcUltraStyle.SUBTITLE + name;
            tabs[i] = CnpcGuiLayout.GridButton.run(button, () -> open(player, "records:" + id));
        }
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(player, gui, row, CnpcGuiSupport.ID_GRID_BASE, tabs, () -> {});
        int bodyBottom = CnpcGuiSupport.paintLongReadOnlyBody(gui, row, recordsLines(player, key));
        footer(player, gui, bodyBottom, null);
    }

    private static String recordsLabel(String key) {
        return switch (key) {
            case "stats" -> "Stats";
            case "season" -> "Season";
            case "quests" -> "Quests";
            case "achievements" -> "Achievements";
            case "hof" -> "Hall of fame";
            case "journal" -> "Journal";
            case "title" -> "Title";
            default -> "History";
        };
    }

    private static List<String> recordsLines(ServerPlayer player, String key) {
        ServerPlayer who = subject(player);
        return switch (key) {
            case "stats" -> RivalGuiApi.statsLines(who);
            case "season" -> RivalGuiApi.seasonLines(who);
            case "quests" -> RivalGuiApi.questLines(who);
            case "achievements" -> RivalGuiApi.achievementLines(who);
            case "hof" -> RivalGuiApi.hofLines(who);
            case "journal" -> RivalGuiApi.journalLines(who);
            case "title" -> RivalGuiApi.titleLines(who);
            default -> RivalGuiApi.linesForPage(who, "history");
        };
    }

    private static void paintList(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        List<String> cards = RivalGuiApi.currentRivalCards(who);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Your list"),
                cards.isEmpty() ? CnpcUltraStyle.SUBTITLE + "Your list is empty — declare a rival below"
                        : CnpcUltraStyle.SUBTITLE + cards.size() + " rival" + (cards.size() == 1 ? "" : "s")
                                + " · select a name, then Open");
        infoY = rivalChrome(player, gui, infoY, "list");
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (cards.isEmpty()) {
            listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY,
                    RivalSystem.emptyRivalListGuide(), CnpcGuiStyle.INFO_INLINE_MAX));
            CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.INFO + "Declare…", CnpcGuiSupport.COL_L, listY,
                    () -> open(player, "pick_declare"));
            CnpcGuiSupport.button(gui, 22, CnpcUltraStyle.DIM + "Silent…", CnpcGuiSupport.COL_R, listY,
                    () -> open(player, "pick_silent"));
            footer(player, gui, listY + CnpcGuiSupport.ROW_STEP, "records");
            return;
        }
        int rowsBelow = 4;
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, cards.size());
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, CnpcGuiSupport.cardLabels(cards, 1));
        int actionRow = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.selectionButton(player, gui, 93, CnpcUltraStyle.INFO + "Open", CnpcGuiSupport.COL_L, actionRow,
                CnpcGuiSupport.BTN_W, () -> {
                    int[] sel = scroll.getSelection();
                    if (sel == null || sel.length == 0 || sel[0] < 0 || sel[0] >= cards.size()) {
                        return null;
                    }
                    return CnpcGuiSupport.rivalPickerArgFromCard(cards.get(sel[0]));
                },
                picker -> open(player, "list_detail:" + picker),
                () -> open(player, "list"));
        int tools = actionRow + CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.INFO + "Declare…", CnpcGuiSupport.COL_L, tools,
                () -> open(player, "pick_declare"));
        CnpcGuiSupport.button(gui, 22, CnpcUltraStyle.DIM + "Silent…", CnpcGuiSupport.COL_R, tools,
                () -> open(player, "pick_silent"));
        tools += CnpcGuiSupport.ROW_STEP;
        if (RivalGuiApi.needsMutualReplacePick(who)) {
            CnpcGuiSupport.button(gui, 24, CnpcUltraStyle.INFO + "Replace mutual…", CnpcGuiSupport.COL_L, tools,
                    () -> open(player, "pick_replace_mutual"));
            tools += CnpcGuiSupport.ROW_STEP;
        }
        footer(player, gui, tools, "records");
    }

    private static void paintNamePick(
            ServerPlayer player,
            ICustomGui gui,
            String title,
            List<String> names,
            String action,
            String returnPage
    ) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, CnpcGuiStyle.HINT_CLICK_PLAYER);
        infoY = rivalChrome(player, gui, infoY, returnPage);
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (names == null || names.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    "spectate".equals(action)
                            ? CnpcUltraStyle.SUBTITLE + "No live challenge right now. Start one from Challenge, then come back to watch."
                            : CnpcUltraStyle.SUBTITLE + "Nobody else is online right now — try again when other players are on.",
                    CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, returnPage);
            return;
        }
        String[] items = names.toArray(String[]::new);
        int rowsBelow = 2;
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, items.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, items);
        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.selectionButton(player, gui, 30, CnpcUltraStyle.INFO + "Choose", CnpcGuiSupport.COL_L, row,
                CnpcGuiSupport.BTN_W, () -> CnpcGuiSupport.selectedLine(scroll, items),
                name -> open(player, "pick_confirm:" + action + "|" + returnPage + "|" + name),
                () -> open(player, "challenge_pick".equals(action) ? "pick_challenge" : "pick_" + action));
        footer(player, gui, row + CnpcGuiSupport.ROW_STEP, returnPage);
    }

    private static void paintArgPick(
            ServerPlayer player,
            ICustomGui gui,
            String title,
            List<String> args,
            String action,
            String returnPage
    ) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, CnpcGuiStyle.HINT_CLICK_ENTRY);
        infoY = rivalChrome(player, gui, infoY, returnPage);
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        List<String> cards = args;
        if (cards == null || cards.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    CnpcUltraStyle.SUBTITLE + "Nothing to choose yet. Go back and pick an action that has options.",
                    CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, returnPage);
            return;
        }
        String[] labels = new String[cards.size()];
        for (int i = 0; i < cards.size(); i++) {
            labels[i] = RivalGuiApi.displayPickerArg(player, cards.get(i));
        }
        int rowsBelow = 2;
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.selectionButton(player, gui, 30, CnpcUltraStyle.INFO + "Choose", CnpcGuiSupport.COL_L, row,
                CnpcGuiSupport.BTN_W, () -> CnpcGuiSupport.cardField(cards, scroll, 0),
                arg -> open(player, "pick_confirm:" + action + "|" + returnPage + "|" + arg),
                () -> open(player, returnPage));
        footer(player, gui, row + CnpcGuiSupport.ROW_STEP, returnPage);
    }

    /** Remove / replace mutual — uses encoded rival cards so names show instead of raw {@code uuid:}. */
    private static void paintRivalCardPick(
            ServerPlayer player,
            ICustomGui gui,
            String title,
            List<String> cards,
            String action,
            String returnPage
    ) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, CnpcGuiStyle.HINT_CLICK_ENTRY);
        infoY = rivalChrome(player, gui, infoY, returnPage);
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (cards == null || cards.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    CnpcUltraStyle.SUBTITLE + "No rivals match this action — add rivals from Actions first.",
                    CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, returnPage);
            return;
        }
        String[] labels = CnpcGuiSupport.cardLabels(cards, 1);
        int rowsBelow = 2;
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.selectionButton(player, gui, 30, CnpcUltraStyle.INFO + "Choose", CnpcGuiSupport.COL_L, row,
                CnpcGuiSupport.BTN_W, () -> {
                    int[] sel = scroll.getSelection();
                    if (sel == null || sel.length == 0 || sel[0] < 0 || sel[0] >= cards.size()) {
                        return null;
                    }
                    String arg = RivalGuiApi.pickerArgFromRivalCard(cards.get(sel[0]));
                    return arg == null || arg.isBlank() ? null : arg;
                },
                arg -> open(player, "pick_confirm:" + action + "|" + returnPage + "|" + arg),
                () -> open(player, returnPage));
        footer(player, gui, row + CnpcGuiSupport.ROW_STEP, returnPage);
    }

    private static void paintScroll(ServerPlayer player, ICustomGui gui, String title, List<String> body, String backPage) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, CnpcGuiStyle.HINT_READ_ONLY);
        infoY = rivalChrome(player, gui, infoY, backPage);
        int row = CnpcGuiSupport.paintLongReadOnlyBody(gui, infoY, body);
        footer(player, gui, row + 8, backPage);
    }

    private static void paintAdmin(ServerPlayer player, ICustomGui gui) {
        if (!StaffAccess.isStaff(player)) {
            CnpcGuiSupport.pushMenuMessage(player, CnpcMenuFeedback.NOTICE_BODY + "Staff only.");
            open(player, "records");
            return;
        }
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Rivals", "Staff Admin"),
                CnpcUltraStyle.SUBTITLE + "Save data, reload config, print status");
        infoY = rivalChrome(player, gui, infoY, "admin");
        int row = CnpcGuiSupport.bodyBelowHeader(infoY);
        CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Save data", CnpcGuiSupport.COL_L, row,
                () -> act(player, "admin", "save", "admin"));
        CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.INFO + "Reload", CnpcGuiSupport.COL_R, row,
                () -> act(player, "admin", "refresh", "admin"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 22, CnpcUltraStyle.SUBTITLE + "Status", CnpcGuiSupport.COL_L, row,
                () -> act(player, "admin", "status", "admin"));
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, "records");
    }

    /** {@code parentPage} null on a rival tab; otherwise Back reopens that parent. */
    private static void footer(ServerPlayer player, ICustomGui gui, int row, String parentPage) {
        if (parentPage == null) {
            CnpcGuiSupport.navSystemRoot(player, gui, row);
        } else {
            CnpcGuiSupport.navBackToParent(player, gui, row, () -> open(player, parentPage));
        }
        if (parentPage == null) {
            CnpcGuiSupport.paintSystemMainPreview(subject(player), gui, player);
        }
    }
}
