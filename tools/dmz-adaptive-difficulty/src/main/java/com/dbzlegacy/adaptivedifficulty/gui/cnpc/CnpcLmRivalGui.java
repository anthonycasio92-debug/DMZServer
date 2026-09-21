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
    private static final int H = 320;

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
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, H, (pl, gui) -> {
            switch (lower) {
                case "list" -> paintList(pl, gui);
                case "actions" -> paintActions(pl, gui);
                case "pending", "invites" -> paintPending(pl, gui);
                case "history", "past" -> paintScroll(pl, gui, "§6Rival · History", RivalGuiApi.linesForPage(subject(pl), "history"), "main");
                case "challenge", "challenges" -> paintChallenge(pl, gui);
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
                        arg -> act(pl, "declare", arg, "actions"));
                case "pick_silent" -> paintNamePick(pl, gui, "§8Silent rival", RivalGuiApi.onlinePlayerNames(subject(pl)),
                        arg -> act(pl, "silent", arg, "actions"));
                case "pick_accept" -> paintArgPick(pl, gui, "§aAccept rivalry", RivalGuiApi.acceptCandidateArgs(subject(pl)),
                        arg -> act(pl, "accept", arg, "actions"));
                case "pick_decline" -> paintArgPick(pl, gui, "§cDecline declare", RivalGuiApi.pendingIncomingDeclareArgs(subject(pl)),
                        arg -> act(pl, "decline", arg, "actions"));
                case "pick_remove" -> paintArgPick(pl, gui, "§cRemove rival", RivalGuiApi.currentRivalArgs(subject(pl)),
                        arg -> act(pl, "remove", arg, "actions"));
                case "pick_replace_mutual", "replace_mutual" -> paintArgPick(pl, gui, "§eReplace mutual slot",
                        RivalGuiApi.currentRivalArgs(subject(pl)),
                        arg -> act(pl, "accept_replace", arg, "actions"));
                case "pick_challenge" -> paintNamePick(pl, gui, "§cChallenge rival", RivalGuiApi.onlinePlayerNames(subject(pl)),
                        arg -> open(player, "challenge_time:" + arg));
                case "pick_spectate" -> paintNamePick(pl, gui, "§bSpectate", RivalGuiApi.onlinePlayerNames(subject(pl)),
                        arg -> act(pl, "spectate", arg, "challenge"));
                case "admin" -> paintAdmin(pl, gui);
                default -> paintMain(pl, gui);
            }
        });
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
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§6Rival System",
                "§7RP §f" + ph.getOrDefault("rp", "?") + "  §8·  §7Tier §f" + ph.getOrDefault("tier", "?"));

        if (!"true".equalsIgnoreCase(ph.get("system_enabled"))) {
            int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of("§cRival system is disabled."), 3);
            footer(player, gui, row + 8, null);
            return;
        }

        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, RivalGuiApi.linesForPage(who, "main"), 4);
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
        CnpcGuiSupport.button(gui, 26, tpOn ? "§aTP msg ON" : "§8TP msg OFF", CnpcGuiSupport.COL_L, row, () -> act(
                player, "tpmsg", "toggle", "main"));
        if ("true".equalsIgnoreCase(ph.get("instinct_feature"))) {
            boolean instinctOn = "true".equalsIgnoreCase(ph.get("instinct"));
            CnpcGuiSupport.button(gui, 27, instinctOn ? "§aInstinct ON" : "§8Instinct OFF", CnpcGuiSupport.COL_R, row,
                    () -> act(player, "instinct", "toggle", "main"));
        }
        row += 24;
        if (StaffAccess.isStaff(player)) {
            CnpcGuiSupport.buttonSmall(gui, 98, "§cStaff admin", CnpcGuiSupport.COL_L, row, 195, () -> open(player, "admin"));
            row += 24;
        }
        footer(player, gui, row, null);
    }

    private static void paintActions(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§aRival actions", "§7Declare, accept, remove rivals");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, RivalGuiApi.linesForPage(subject(player), "actions"), 4);
        CnpcGuiSupport.button(gui, 20, "§eDeclare…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_declare"));
        CnpcGuiSupport.button(gui, 21, "§aAccept…", CnpcGuiSupport.COL_R, row, () -> open(player, "pick_accept"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§cDecline…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_decline"));
        CnpcGuiSupport.button(gui, 23, "§cRemove…", CnpcGuiSupport.COL_R, row, () -> open(player, "pick_remove"));
        row += 24;
        CnpcGuiSupport.button(gui, 24, "§8Silent…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_silent"));
        if (RivalGuiApi.needsMutualReplacePick(subject(player))) {
            CnpcGuiSupport.button(gui, 25, "§eReplace mutual…", CnpcGuiSupport.COL_R, row,
                    () -> open(player, "pick_replace_mutual"));
        }
        row += 24;
        CnpcGuiSupport.button(gui, 26, "§6Pending board", CnpcGuiSupport.COL_L, row, () -> open(player, "pending"));
        row += 24;
        footer(player, gui, row, "main");
    }

    private static void paintPending(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§6Pending invites", "§7Double-click to respond");
        int listY = CnpcGuiSupport.paintInfoBlock(gui, infoY, RivalGuiApi.linesForPage(who, "pending"), 3);
        List<String> cards = RivalGuiApi.pendingInviteCards(who);
        if (cards.isEmpty()) {
            gui.addLabel(50, "§7No pending invites.", CnpcGuiSupport.M, listY + 4, 400, 14);
            footer(player, gui, listY + 28, "actions");
        } else {
            IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M,
                    listY, 400, 120, CnpcGuiSupport.cardLabels(cards, 1));
            CnpcGuiSupport.wireScrollDoublePick(scroll, cards, 0,
                    arg -> open(player, "pending_decide:" + arg));
            footer(player, gui, listY + 128, "actions");
        }
    }

    private static void openPendingDecide(ServerPlayer player, String pickerArg) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, 240, (pl, gui) -> {
            CnpcGuiSupport.title(gui, 1, "§6Pending invite");
            CnpcGuiSupport.subtitle(gui, 2, "§7Choose accept or decline");
            int row = 100;
            CnpcGuiSupport.button(gui, 20, "§aAccept", CnpcGuiSupport.COL_L, row, () -> act(pl, "accept", pickerArg, "pending"));
            CnpcGuiSupport.button(gui, 21, "§cDecline", CnpcGuiSupport.COL_R, row, () -> act(pl, "decline", pickerArg, "pending"));
            row += 28;
            CnpcGuiSupport.buttonSmall(gui, 96, "§7« Back", CnpcGuiSupport.COL_L, row, 95, () -> open(pl, "pending"));
        });
    }

    private static void paintChallenge(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§cRival challenge", "§7Send or answer a duel");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, RivalGuiApi.challengeLines(who), 4);
        CnpcGuiSupport.button(gui, 20, "§eSend challenge…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_challenge"));
        CnpcGuiSupport.button(gui, 21, "§aAccept", CnpcGuiSupport.COL_R, row, () -> act(player, "challenge", "accept", "challenge"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§cDecline", CnpcGuiSupport.COL_L, row, () -> act(player, "challenge", "decline", "challenge"));
        CnpcGuiSupport.button(gui, 23, "§8Cancel send", CnpcGuiSupport.COL_R, row, () -> act(player, "challenge", "cancel", "challenge"));
        row += 24;
        CnpcGuiSupport.button(gui, 24, "§bSpectate…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_spectate"));
        CnpcGuiSupport.button(gui, 25, "§8Stop spectate", CnpcGuiSupport.COL_R, row, () -> act(player, "spectate_stop", "", "challenge"));
        row += 24;
        footer(player, gui, row, "main");
    }

    private static void openChallengeTime(ServerPlayer player, String targetArg) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_RIVAL, CnpcGuiSupport.W, 280, (pl, gui) -> {
            CnpcGuiSupport.title(gui, 1, "§cChallenge length");
            CnpcGuiSupport.subtitle(gui, 2, "§7Target §f" + CnpcGuiSupport.humanizePickerArg(targetArg));
            int row = 90;
            for (int min = 1; min <= 10; min++) {
                int m = min;
                int col = (min % 2 == 1) ? CnpcGuiSupport.COL_L : CnpcGuiSupport.COL_R;
                if (min > 1 && min % 2 == 1) {
                    row += 24;
                }
                String sendArg = targetArg + "@" + m;
                CnpcGuiSupport.buttonSmall(gui, 30 + min, "§f" + m + " min", col, row, 95,
                        () -> act(pl, "challenge_send", sendArg, "challenge"));
            }
            row += 36;
            CnpcGuiSupport.buttonSmall(gui, 96, "§7« Back", CnpcGuiSupport.COL_L, row, 95, () -> open(pl, "pick_challenge"));
        });
    }

    private static void paintProgress(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§bRival progress");
        CnpcGuiSupport.bodyLines(gui, 10, 44, RivalGuiApi.linesForPage(subject(player), "progress"), 4);
        int row = 100;
        CnpcGuiSupport.button(gui, 20, "§eStats", CnpcGuiSupport.COL_L, row, () -> open(player, "stats"));
        CnpcGuiSupport.button(gui, 21, "§aSeason", CnpcGuiSupport.COL_R, row, () -> open(player, "season"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§bQuests", CnpcGuiSupport.COL_L, row, () -> open(player, "quests"));
        CnpcGuiSupport.button(gui, 23, "§6More records", CnpcGuiSupport.COL_R, row, () -> open(player, "records"));
        row += 24;
        footer(player, gui, row, "main");
    }

    private static void paintRecords(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§6Rival records");
        int row = 70;
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
        CnpcGuiSupport.title(gui, 1, "§6Your rivals");
        CnpcGuiSupport.subtitle(gui, 2, "§7Double-click to remove");
        if (cards.isEmpty()) {
            CnpcGuiSupport.bodyLines(gui, 10, 48, RivalGuiApi.listLines(who), 8);
        } else {
            IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M, 44,
                    400, 150, CnpcGuiSupport.cardLabels(cards, 1));
            CnpcGuiSupport.wireScrollDoublePick(scroll, cards, 0,
                    arg -> act(player, "remove", arg, "list"));
        }
        int row = 210;
        CnpcGuiSupport.buttonSmall(gui, 95, "§aActions", CnpcGuiSupport.COL_L, row, 95, () -> open(player, "actions"));
        footer(player, gui, row, "main");
    }

    private static void paintNamePick(
            ServerPlayer player,
            ICustomGui gui,
            String title,
            List<String> names,
            java.util.function.Consumer<String> onPick
    ) {
        CnpcGuiSupport.title(gui, 1, title);
        CnpcGuiSupport.subtitle(gui, 2, "§7Double-click a player");
        if (names == null || names.isEmpty()) {
            gui.addLabel(50, "§cNo players available.", CnpcGuiSupport.M, 80, 400, 14);
        } else {
            String[] items = names.toArray(String[]::new);
            IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M, 48,
                    400, 150, items);
            scroll.setOnDoubleClick((g, sc) -> {
                g.close();
                int[] sel = sc.getSelection();
                if (sel != null && sel.length > 0 && sel[0] >= 0 && sel[0] < items.length) {
                    onPick.accept(items[sel[0]]);
                }
            });
        }
        footer(player, gui, 210, "actions");
    }

    private static void paintArgPick(
            ServerPlayer player,
            ICustomGui gui,
            String title,
            List<String> args,
            java.util.function.Consumer<String> onPick
    ) {
        CnpcGuiSupport.title(gui, 1, title);
        CnpcGuiSupport.subtitle(gui, 2, "§7Double-click an entry");
        List<String> cards = args;
        if (cards == null || cards.isEmpty()) {
            gui.addLabel(50, "§7Nothing to pick.", CnpcGuiSupport.M, 80, 400, 14);
        } else {
            String[] labels = CnpcGuiSupport.cardLabels(cards, 0);
            IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M, 48,
                    400, 150, labels);
            scroll.setOnDoubleClick((g, sc) -> {
                g.close();
                int[] sel = sc.getSelection();
                if (sel != null && sel.length > 0 && sel[0] >= 0 && sel[0] < cards.size()) {
                    onPick.accept(cards.get(sel[0]));
                }
            });
        }
        footer(player, gui, 210, "actions");
    }

    private static void paintScroll(ServerPlayer player, ICustomGui gui, String title, List<String> body, String backPage) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, "§7Scroll for details");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, body, 4);
        footer(player, gui, row + 8, backPage);
    }

    private static void paintAdmin(ServerPlayer player, ICustomGui gui) {
        if (!StaffAccess.isStaff(player)) {
            open(player, "main");
            return;
        }
        CnpcGuiSupport.title(gui, 1, "§cRival admin");
        int row = 80;
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
    }
}
