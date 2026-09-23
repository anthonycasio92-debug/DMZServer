package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.gui.RivalGuiApi;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
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
        int height = heightForPage(lower);
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, height, (pl, gui) -> {
            switch (lower) {
                case "list" -> paintList(pl, gui);
                case "actions" -> paintActions(pl, gui);
                case "pending", "invites" -> paintPending(pl, gui);
                case "history", "past" -> paintScroll(pl, gui, "§6Rival · History", RivalGuiApi.linesForPage(subject(pl), "history"), "main");
                case "challenge", "challenges" -> paintChallenge(pl, gui);
                case "challenge_pending", "challenge_requests" -> paintChallengePending(pl, gui);
                case "stats" -> paintScroll(pl, gui, "§6Rival · Stats", RivalGuiApi.statsLines(subject(pl)), "progress");
                case "top", "leaderboard" -> paintScroll(pl, gui, "§6Rival · Leaderboard", RivalGuiApi.topLines(subject(pl)), "main");
                case "progress" -> paintProgress(pl, gui);
                case "records", "more" -> paintRecords(pl, gui);
                case "season" -> paintScroll(pl, gui, "§aSeason", RivalGuiApi.seasonLines(subject(pl)), "progress");
                case "quests" -> paintScroll(pl, gui, "§bQuests", RivalGuiApi.questLines(subject(pl)), "progress");
                case "achievements", "achs" -> paintScroll(pl, gui, "§dAchievements", RivalGuiApi.achievementLines(subject(pl)), "records");
                case "hof", "hall" -> paintScroll(pl, gui, "§6Hall of Fame", RivalGuiApi.hofLines(subject(pl)), "records");
                case "journal" -> paintScroll(pl, gui, "§fJournal", RivalGuiApi.journalLines(subject(pl)), "records");
                case "title", "titles" -> paintScroll(pl, gui, "§eTitle", RivalGuiApi.titleLines(subject(pl)), "records");
                case "pick_declare" -> paintNamePick(pl, gui, "§eDeclare rival", RivalGuiApi.onlinePlayerNames(subject(pl)),
                        "declare", "actions");
                case "pick_silent" -> paintNamePick(pl, gui, "§8Silent rival", RivalGuiApi.onlinePlayerNames(subject(pl)),
                        "silent", "actions");
                case "pick_accept", "pick_decline" -> open(pl, "pending");
                case "pick_remove" -> open(pl, "list");
                case "pick_replace_mutual", "replace_mutual" -> paintRivalCardPick(pl, gui, "§eReplace mutual slot",
                        RivalGuiApi.currentRivalCards(subject(pl)), "accept_replace", "actions");
                case "pick_challenge" -> paintNamePick(pl, gui, "§cChallenge rival", RivalGuiApi.onlinePlayerNames(subject(pl)),
                        "challenge_pick", "challenge");
                case "pick_spectate" -> paintNamePick(pl, gui, "§bSpectate", RivalGuiApi.onlinePlayerNames(subject(pl)),
                        "spectate", "challenge");
                case "admin" -> paintAdmin(pl, gui);
                default -> paintMain(pl, gui);
            }
        });
    }

    private static int heightForPage(String page) {
        return switch (page) {
            case "list" -> 400;
            case "pending", "invites", "challenge_pending", "challenge_requests" -> 400;
            case "pick_declare", "pick_silent", "pick_challenge", "pick_spectate",
                    "pick_accept", "pick_decline", "pick_replace_mutual" -> 380;
            case "progress", "records" -> 300;
            case "history", "past", "stats", "top", "leaderboard", "season", "quests",
                    "achievements", "achs", "hof", "hall", "journal", "title", "titles" -> 420;
            default -> H;
        };
    }

    private static ServerPlayer subject(ServerPlayer viewer) {
        return CnpcGuiSupport.target(viewer);
    }

    private static void act(ServerPlayer viewer, String action, String arg, String returnPage) {
        CnpcGuiSupport.act(viewer, () -> RivalGuiApi.handleDo(subject(viewer), action, arg, returnPage),
                () -> open(viewer, returnPage));
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        Map<String, String> ph = RivalGuiApi.placeholders(who);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§6Rivals",
                "§7RP §f" + ph.getOrDefault("rp", "?") + CnpcGuiStyle.SEP + "§7Tier §f"
                        + ph.getOrDefault("tier", "?"));

        if (!"true".equalsIgnoreCase(ph.get("system_enabled"))) {
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(CnpcGuiStyle.MSG_RIVALS_OFF),
                            CnpcGuiStyle.INFO_INLINE_MAX));
            footer(player, gui, row + 8, null);
            return;
        }

        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, RivalGuiApi.linesForPage(who, "main"),
                        CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, "§eRival list", CnpcGuiSupport.COL_L, row, () -> open(player, "list"));
        CnpcGuiSupport.button(gui, 21, "§aActions", CnpcGuiSupport.COL_R, row, () -> open(player, "actions"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§cChallenge", CnpcGuiSupport.COL_L, row, () -> open(player, "challenge"));
        CnpcGuiSupport.button(gui, 23, "§dLeaderboard", CnpcGuiSupport.COL_R, row, () -> open(player, "top"));
        row += 24;
        CnpcGuiSupport.button(gui, 24, "§8History", CnpcGuiSupport.COL_L, row, () -> open(player, "history"));
        CnpcGuiSupport.button(gui, 25, "§bProgress", CnpcGuiSupport.COL_R, row, () -> open(player, "progress"));
        row += 24;
        boolean tpOn = "true".equalsIgnoreCase(ph.get("tpMsg"));
        CnpcGuiSupport.button(gui, 26, tpOn ? CnpcGuiStyle.toggleOn("TP")
                : CnpcGuiStyle.toggleOff("TP"),
                CnpcGuiSupport.COL_L, row, () -> act(
                player, "tpmsg", "toggle", "main"));
        if ("true".equalsIgnoreCase(ph.get("instinct_feature"))) {
            boolean instinctOn = "true".equalsIgnoreCase(ph.get("instinct"));
            CnpcGuiSupport.button(gui, 27, instinctOn ? CnpcGuiStyle.toggleOn("Rival instinct")
                    : CnpcGuiStyle.toggleOff("Rival instinct"),
                    CnpcGuiSupport.COL_R, row,
                    () -> act(player, "instinct", "toggle", "main"));
        }
        row += 24;
        if (StaffAccess.isStaff(player)) {
            CnpcGuiSupport.buttonSmall(gui, CnpcGuiSupport.ID_STAFF_EXTRA, "§cStaff Admin", CnpcGuiSupport.COL_L, row,
                    CnpcGuiSupport.BTN_W, () -> open(player, "admin"));
            row += 24;
        }
        footer(player, gui, row, null);
    }

    private static void paintActions(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§6", "Rivals", "Actions"),
                "§7Declare, pending board, and silent rivals");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, RivalGuiApi.linesForPage(subject(player), "actions"),
                        CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, "§eDeclare…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_declare"));
        CnpcGuiSupport.button(gui, 21, "§6Declare invites", CnpcGuiSupport.COL_R, row, () -> open(player, "pending"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§8Silent…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_silent"));
        row += 24;
        if (RivalGuiApi.needsMutualReplacePick(subject(player))) {
            CnpcGuiSupport.button(gui, 24, "§eReplace mutual…", CnpcGuiSupport.COL_L, row,
                    () -> open(player, "pick_replace_mutual"));
            row += 24;
        }
        footer(player, gui, row, "main");
    }

    private static void paintPending(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§6", "Rivals", "Declare invites"),
                "§eRival declare requests — not duel challenges");
        int listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY,
                RivalGuiApi.linesForPage(who, "pending"), 2));
        List<String> cards = RivalGuiApi.pendingInviteCards(who);
        if (cards.isEmpty()) {
            gui.addLabel(50, "§7No pending invites.", CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, "actions");
        } else {
            int rowsBelow = 1;
            int bandY = listY + 14;
            int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
            IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow,
                    RivalGuiApi.pendingInviteScrollLabels(cards));
            scroll.setOnClick((g, sc) -> {
                int[] sel = sc.getSelection();
                if (sel == null || sel.length == 0 || sel[0] < 0 || sel[0] >= cards.size()) {
                    return;
                }
                String card = cards.get(sel[0]);
                String picker = CnpcGuiSupport.rivalPickerArgFromCard(card);
                if (picker == null) {
                    return;
                }
                open(player, "pending_decide:" + picker);
            });
            footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), "actions");
        }
    }

    private static void openPendingDecide(ServerPlayer player, String pickerArg) {
        ServerPlayer who = subject(player);
        List<String> cards = RivalGuiApi.pendingInviteCards(who);
        String card = CnpcGuiSupport.findCardByPickerArg(cards, pickerArg);
        List<String> detail = card != null ? RivalGuiApi.pendingInviteDetailLines(card)
                : List.of("§7Pending declare", RivalGuiApi.displayPickerArg(player, pickerArg));
        boolean outgoing = card != null && RivalGuiApi.pendingInviteCardOutgoing(card);
        boolean mutual = card != null && RivalGuiApi.pendingInviteCardMutualConfirm(card);
        String subTitle = outgoing ? "Outgoing declare" : (mutual ? "Mutual confirm" : "Incoming declare");
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, 300, (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage("§6", "Rivals", subTitle),
                    outgoing ? "§7Withdraw or wait for their answer" : "§7Accept or decline below");
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, detail, CnpcGuiStyle.INFO_INLINE_MAX));
            row += 8;
            if (outgoing) {
                CnpcGuiSupport.button(gui, 20, "§cWithdraw declare", CnpcGuiSupport.COL_L, row,
                        () -> act(pl, "remove", pickerArg, "pending"));
                CnpcGuiSupport.button(gui, 21, "§7Keep waiting", CnpcGuiSupport.COL_R, row, () -> open(pl, "pending"));
            } else if (mutual) {
                CnpcGuiSupport.button(gui, 20, "§aAccept mutual", CnpcGuiSupport.COL_L, row,
                        () -> act(pl, "accept", pickerArg, "pending"));
                CnpcGuiSupport.button(gui, 21, "§cDecline mutual", CnpcGuiSupport.COL_R, row,
                        () -> act(pl, "decline", pickerArg, "pending"));
            } else {
                CnpcGuiSupport.button(gui, 20, "§aAccept", CnpcGuiSupport.COL_L, row,
                        () -> act(pl, "accept", pickerArg, "pending"));
                CnpcGuiSupport.button(gui, 21, "§cDecline", CnpcGuiSupport.COL_R, row,
                        () -> act(pl, "decline", pickerArg, "pending"));
            }
            row += CnpcGuiSupport.ROW_STEP + 4;
            CnpcGuiSupport.navSubmenu(pl, gui, row, () -> open(pl, "pending"), "§7« Back");
        });
    }

    private static void openListDetail(ServerPlayer player, String pickerArg) {
        ServerPlayer who = subject(player);
        List<String> cards = RivalGuiApi.currentRivalCards(who);
        String card = CnpcGuiSupport.findCardByPickerArg(cards, pickerArg);
        List<String> detail = card != null ? RivalGuiApi.rivalCardDetailLines(card)
                : List.of(RivalGuiApi.displayPickerArg(player, pickerArg), "§7Rival record");
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, 300, (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage("§6", "Rivals", "Profile"),
                    "§7Stats and actions for this rival");
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, detail, CnpcGuiStyle.INFO_INLINE_MAX));
            row += 8;
            CnpcGuiSupport.button(gui, 20, "§cRemove rival", CnpcGuiSupport.COL_L, row,
                    () -> act(pl, "remove", pickerArg, "list"));
            row += CnpcGuiSupport.ROW_STEP + 4;
            CnpcGuiSupport.navSubmenu(pl, gui, row, () -> open(pl, "list"), "§7« Back");
        });
    }

    /** {@code payload} = {@code action|returnPage|targetArg} (target may contain {@code :}). */
    private static void openPickConfirm(ServerPlayer player, String payload) {
        String[] parts = payload.split("\\|", 3);
        if (parts.length < 3) {
            open(player, "actions");
            return;
        }
        String action = parts[0];
        String returnPage = parts[1];
        String targetArg = parts[2];
        String display = RivalGuiApi.displayPickerArg(player, targetArg);
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, 280, (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, "§eConfirm action", display);
            List<String> lines = List.of(
                    "§7Player " + display,
                    "§8Action §7" + action.replace('_', ' '),
                    "§7Confirm to continue.");
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 4));
            row += 8;
            CnpcGuiSupport.button(gui, 20, "§aConfirm", CnpcGuiSupport.COL_L, row, () -> {
                if ("challenge_pick".equals(action)) {
                    open(pl, "challenge_time:" + targetArg);
                } else {
                    act(pl, action, targetArg, returnPage);
                }
            });
            CnpcGuiSupport.button(gui, 21, "§7Cancel", CnpcGuiSupport.COL_R, row, () -> open(pl, returnPage));
            row += CnpcGuiSupport.ROW_STEP + 4;
            CnpcGuiSupport.navSubmenu(pl, gui, row, () -> open(pl, returnPage), "§7« Back");
        });
    }

    private static void paintChallenge(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§6", "Rivals", "Challenge"),
                "§7Send duels · pending board · spectate");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, RivalGuiApi.challengeLines(who),
                        CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, "§eSend challenge…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_challenge"));
        int pending = RivalGuiApi.pendingChallengeCards(who).size();
        String pendingLabel = pending > 0 ? "§ePending requests §f(" + pending + ")" : "§6Pending requests";
        CnpcGuiSupport.button(gui, 21, pendingLabel, CnpcGuiSupport.COL_R, row, () -> open(player, "challenge_pending"));
        row += 24;
        CnpcGuiSupport.button(gui, 24, "§bSpectate…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_spectate"));
        CnpcGuiSupport.button(gui, 25, "§8Stop spectate", CnpcGuiSupport.COL_R, row, () -> act(player, "spectate_stop", "", "challenge"));
        row += 24;
        footer(player, gui, row, "main");
    }

    private static void paintChallengePending(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§6", "Rivals", "Duel requests"),
                "§eOfficial timed duels — accept, decline, or cancel");
        int listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY,
                RivalGuiApi.linesForPage(who, "challenge_pending"), 2));
        List<String> cards = RivalGuiApi.pendingChallengeCards(who);
        if (cards.isEmpty()) {
            gui.addLabel(50, "§7No pending challenge requests.", CnpcGuiSupport.M, listY + 4,
                    CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, "challenge");
        } else {
            int rowsBelow = 1;
            int bandY = listY + 14;
            int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
            IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow,
                    RivalGuiApi.pendingChallengeScrollLabels(cards));
            scroll.setOnClick((g, sc) -> {
                int[] sel = sc.getSelection();
                if (sel == null || sel.length == 0 || sel[0] < 0 || sel[0] >= cards.size()) {
                    return;
                }
                String card = cards.get(sel[0]);
                String picker = CnpcGuiSupport.rivalPickerArgFromCard(card);
                if (picker == null) {
                    return;
                }
                open(player, "challenge_decide:" + picker);
            });
            footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), "challenge");
        }
    }

    private static void openChallengeDecide(ServerPlayer player, String pickerArg) {
        ServerPlayer who = subject(player);
        List<String> cards = RivalGuiApi.pendingChallengeCards(who);
        String card = CnpcGuiSupport.findCardByPickerArg(cards, pickerArg);
        List<String> detail = card != null ? RivalGuiApi.pendingChallengeDetailLines(card)
                : List.of("§7Pending challenge", RivalGuiApi.displayPickerArg(player, pickerArg));
        boolean outgoing = card != null && RivalGuiApi.pendingChallengeCardOutgoing(card);
        String subTitle = outgoing ? "Outgoing challenge" : "Incoming challenge";
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, 300, (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage("§6", "Rivals", subTitle),
                    outgoing ? "§7Cancel or keep waiting" : "§7Accept or decline below");
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, detail,
                    CnpcGuiStyle.INFO_INLINE_MAX));
            row += 8;
            if (outgoing) {
                CnpcGuiSupport.button(gui, 20, "§cCancel challenge", CnpcGuiSupport.COL_L, row,
                        () -> act(pl, "challenge_cancel", pickerArg, "challenge_pending"));
                CnpcGuiSupport.button(gui, 21, "§7Keep waiting", CnpcGuiSupport.COL_R, row,
                        () -> open(pl, "challenge_pending"));
            } else {
                CnpcGuiSupport.button(gui, 20, "§aAccept", CnpcGuiSupport.COL_L, row,
                        () -> act(pl, "challenge_accept", pickerArg, "challenge_pending"));
                CnpcGuiSupport.button(gui, 21, "§cDecline", CnpcGuiSupport.COL_R, row,
                        () -> act(pl, "challenge_decline", pickerArg, "challenge_pending"));
            }
            row += CnpcGuiSupport.ROW_STEP + 4;
            CnpcGuiSupport.navSubmenu(pl, gui, row, () -> open(pl, "challenge_pending"), "§7« Back");
        });
    }

    private static void openChallengeTime(ServerPlayer player, String targetArg) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, 280, (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage("§6", "Rivals", "Challenge length"),
                    "§7Target " + RivalGuiApi.displayPickerArg(player, targetArg));
            int row = CnpcGuiSupport.bodyBelowHeader(infoY);
            CnpcGuiLayout.GridButton[] grid = new CnpcGuiLayout.GridButton[10];
            for (int min = 1; min <= 10; min++) {
                int m = min;
                String sendArg = targetArg + "@" + m;
                grid[min - 1] = CnpcGuiLayout.GridButton.run(
                        "§f" + m + " min", () -> act(pl, "challenge_send", sendArg, "challenge_pending"));
            }
            row = CnpcGuiLayout.paintTwoColumnButtonGrid(pl, gui, row, 30, grid, () -> {});
            row += 4;
            CnpcGuiSupport.navSubmenu(pl, gui, row, () -> open(pl, "pick_challenge"), "§7« Back");
        });
    }

    private static void paintProgress(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§6", "Rivals", "Progress"),
                "§7Season, quests, and records");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, RivalGuiApi.linesForPage(subject(player), "progress"),
                        CnpcGuiStyle.INFO_INLINE_MAX));
        row += 8;
        CnpcGuiSupport.button(gui, 20, "§eStats", CnpcGuiSupport.COL_L, row, () -> open(player, "stats"));
        CnpcGuiSupport.button(gui, 21, "§aSeason", CnpcGuiSupport.COL_R, row, () -> open(player, "season"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§bQuests", CnpcGuiSupport.COL_L, row, () -> open(player, "quests"));
        CnpcGuiSupport.button(gui, 23, "§6More records", CnpcGuiSupport.COL_R, row, () -> open(player, "records"));
        row += 24;
        footer(player, gui, row, "main");
    }

    private static void paintRecords(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§6", "Rivals", "Records"),
                "§7Titles, achievements, hall of fame, journal");
        int row = CnpcGuiSupport.bodyBelowHeader(infoY);
        CnpcGuiSupport.button(gui, 20, "§eTitle", CnpcGuiSupport.COL_L, row, () -> open(player, "title"));
        CnpcGuiSupport.button(gui, 21, "§dAchievements", CnpcGuiSupport.COL_R, row, () -> open(player, "achievements"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§6Hall of fame", CnpcGuiSupport.COL_L, row, () -> open(player, "hof"));
        CnpcGuiSupport.button(gui, 23, "§fJournal", CnpcGuiSupport.COL_R, row, () -> open(player, "journal"));
        row += 24;
        footer(player, gui, row, "progress");
    }

    private static void paintList(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        List<String> cards = RivalGuiApi.currentRivalCards(who);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§6", "Rivals", "Your list"),
                cards.isEmpty() ? "§7No rivals yet"
                        : "§7" + cards.size() + " rivals · tap a name for profile and remove");
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (cards.isEmpty()) {
            listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, RivalGuiApi.listLines(who),
                            CnpcGuiStyle.INFO_INLINE_MAX));
            footer(player, gui, listY + 8, "main");
            return;
        }
        int rowsBelow = 2;
        int bandY = listY + 14;
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, CnpcGuiSupport.cardLabels(cards, 1));
        scroll.setOnClick((g, sc) -> {
            int[] sel = sc.getSelection();
            if (sel == null || sel.length == 0 || sel[0] < 0 || sel[0] >= cards.size()) {
                return;
            }
            String picker = CnpcGuiSupport.rivalPickerArgFromCard(cards.get(sel[0]));
            if (picker != null) {
                open(player, "list_detail:" + picker);
            }
        });
        int actionRow = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.buttonSmallFull(gui, 94, "§aActions", CnpcGuiSupport.COL_L, actionRow, CnpcGuiSupport.BTN_W,
                () -> open(player, "actions"));
        CnpcGuiSupport.buttonSmallFull(gui, 95, "§8History", CnpcGuiSupport.COL_R, actionRow, CnpcGuiSupport.BTN_W,
                () -> open(player, "history"));
        int navRow = actionRow + CnpcGuiSupport.ROW_STEP;
        footer(player, gui, navRow, "main");
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
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (names == null || names.isEmpty()) {
            gui.addLabel(50, "§cNo players available.", CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, returnPage);
            return;
        }
        String[] items = names.toArray(String[]::new);
        int rowsBelow = 1;
        int bandY = listY + 14;
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, items);
        scroll.setOnClick((g, sc) -> {
            int[] sel = sc.getSelection();
            if (sel != null && sel.length > 0 && sel[0] >= 0 && sel[0] < items.length) {
                String name = items[sel[0]];
                open(player, "pick_confirm:" + action + "|" + returnPage + "|" + name);
            }
        });
        footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), returnPage);
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
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        List<String> cards = args;
        if (cards == null || cards.isEmpty()) {
            gui.addLabel(50, "§7Nothing to pick.", CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, returnPage);
            return;
        }
        String[] labels = new String[cards.size()];
        for (int i = 0; i < cards.size(); i++) {
            labels[i] = RivalGuiApi.displayPickerArg(player, cards.get(i));
        }
        int rowsBelow = 1;
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
        CnpcGuiSupport.wireScrollOpenDetail(scroll, cards, 0,
                arg -> open(player, "pick_confirm:" + action + "|" + returnPage + "|" + arg));
        footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), returnPage);
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
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (cards == null || cards.isEmpty()) {
            gui.addLabel(50, "§7Nothing to pick.", CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, returnPage);
            return;
        }
        String[] labels = CnpcGuiSupport.cardLabels(cards, 1);
        int rowsBelow = 1;
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
        scroll.setOnClick((g, sc) -> {
            int[] sel = sc.getSelection();
            if (sel == null || sel.length == 0 || sel[0] < 0 || sel[0] >= cards.size()) {
                return;
            }
            String arg = RivalGuiApi.pickerArgFromRivalCard(cards.get(sel[0]));
            if (!arg.isBlank()) {
                open(player, "pick_confirm:" + action + "|" + returnPage + "|" + arg);
            }
        });
        footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), returnPage);
    }

    private static void paintScroll(ServerPlayer player, ICustomGui gui, String title, List<String> body, String backPage) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, CnpcGuiStyle.HINT_READ_ONLY);
        int row = CnpcGuiSupport.paintLongReadOnlyBody(gui, infoY, body);
        footer(player, gui, row + 8, backPage);
    }

    private static void paintAdmin(ServerPlayer player, ICustomGui gui) {
        if (!StaffAccess.isStaff(player)) {
            CnpcGuiSupport.pushMenuMessage(player, "§cStaff only — rival admin is for staff.");
            open(player, "main");
            return;
        }
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§6", "Rivals", "Staff Admin"),
                "§7Save data, reload config, print status");
        int row = CnpcGuiSupport.bodyBelowHeader(infoY);
        CnpcGuiSupport.button(gui, 20, "§aSave stores", CnpcGuiSupport.COL_L, row,
                () -> act(player, "admin", "save", "admin"));
        CnpcGuiSupport.button(gui, 21, "§eReload", CnpcGuiSupport.COL_R, row,
                () -> act(player, "admin", "refresh", "admin"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§7Status", CnpcGuiSupport.COL_L, row,
                () -> act(player, "admin", "status", "admin"));
        row += 24;
        footer(player, gui, row, "main");
    }

    /** {@code parentPage} null on rival main; otherwise Back reopens that page. Main always → LM hub. */
    private static void footer(ServerPlayer player, ICustomGui gui, int row, String parentPage) {
        if (parentPage == null) {
            CnpcGuiSupport.navSystemRoot(player, gui, row);
        } else {
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, parentPage), "§7« Back");
        }
        if (parentPage == null) {
            CnpcGuiSupport.paintSystemMainPreview(subject(player), gui, player);
        }
    }
}
