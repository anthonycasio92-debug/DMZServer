package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.gui.RivalGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.SparGuiApi;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IScroll;

public final class CnpcLmSparGui {
    private static final int H = 380;

    private CnpcLmSparGui() {}

    public static void open(ServerPlayer player, String page) {
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
        int height = switch (lower) {
            case "stats", "dojo_hof" -> 420;
            default -> lower.startsWith("pick_") ? 380 : H;
        };
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SPAR, CnpcGuiSupport.W, height, (pl, gui) -> {
            if (lower.startsWith("top_")) {
                paintTop(pl, gui, lower.substring(4).trim());
                return;
            }
            if (lower.startsWith("dojo_top_")) {
                paintDojoTop(pl, gui, lower.substring(9).trim());
                return;
            }
            switch (lower) {
                case "stats" -> paintScroll(pl, gui, "§eSpar stats", SparGuiApi.linesForPage(subject(pl), "stats"), "main");
                case "top", "leaderboard" -> paintTop(pl, gui, "tp");
                case "dojo_top", "dojo_rank", "dojo_rankings" -> paintDojoTop(pl, gui, "rp");
                case "mentor", "actions" -> paintMentor(pl, gui);
                case "pending", "invites" -> paintPending(pl, gui);
                case "dojo", "roster" -> paintDojo(pl, gui);
                case "dojo_war" -> paintDojoWar(pl, gui);
                case "dojo_war_pending" -> paintDojoWarPending(pl, gui);
                case "dojo_hof" -> paintScroll(pl, gui, "§6Dojo hall of fame",
                        SparGuiApi.linesForPage(subject(pl), "dojo_hof"), "dojo");
                case "dojo_members" -> paintDojoMembers(pl, gui);
                case "pick_apprentice" -> paintOnlinePick(pl, gui, "§aInvite apprentice", "mentor_invite", "mentor");
                case "pick_mentor" -> paintOnlinePick(pl, gui, "§bAsk as apprentice", "apprentice_invite", "mentor");
                case "pick_accept" -> paintMentorArgPick(pl, gui, "§aAccept mentor invite", "mentor_accept", "pending");
                case "pick_decline" -> paintMentorArgPick(pl, gui, "§cDecline mentor invite", "mentor_decline", "pending");
                case "pick_release" -> paintReleasePick(pl, gui);
                case "pick_dojo_challenge" -> paintDojoChallengePick(pl, gui);
                case "admin" -> paintAdmin(pl, gui);
                default -> paintMain(pl, gui);
            }
        });
    }

    private static ServerPlayer subject(ServerPlayer viewer) {
        return CnpcGuiSupport.target(viewer);
    }

    private static void act(ServerPlayer viewer, String action, String arg, String returnPage) {
        CnpcGuiSupport.act(viewer, () -> SparGuiApi.handleDo(subject(viewer), action, arg, returnPage),
                () -> open(viewer, returnPage));
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        Map<String, String> ph = SparGuiApi.placeholders(who);
        String partner = ph.getOrDefault("partner", "");
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§bSparring",
                "§7Session §f" + ("true".equals(ph.get("session_active")) ? "§ain progress" : "§7none active")
                        + (partner == null || partner.isBlank() ? "" : " §8· §7vs §f" + partner));

        if (!"true".equalsIgnoreCase(ph.get("system_enabled"))) {
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(CnpcGuiStyle.MSG_SPAR_OFF),
                    CnpcGuiStyle.INFO_INLINE_MAX));
            footer(player, gui, row + 8, null);
            return;
        }

        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.linesForPage(who, "main"),
                CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, "§eStats", CnpcGuiSupport.COL_L, row, () -> open(player, "stats"));
        CnpcGuiSupport.button(gui, 21, "§6Dojo rank", CnpcGuiSupport.COL_R, row, () -> open(player, "dojo_rank"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§dLeaderboard", CnpcGuiSupport.COL_L, row, () -> open(player, "top"));
        CnpcGuiSupport.button(gui, 23, "§bMentor & dojo", CnpcGuiSupport.COL_R, row, () -> open(player, "mentor"));
        row += 24;
        boolean tpOn = "true".equalsIgnoreCase(ph.get("tpMsg"));
        CnpcGuiSupport.button(gui, 24, tpOn ? CnpcGuiStyle.toggleOn("TP")
                : CnpcGuiStyle.toggleOff("TP"), CnpcGuiSupport.COL_L, row,
                () -> act(player, "tpmsg", "toggle", "main"));
        boolean mentorTpOn = "true".equalsIgnoreCase(ph.get("mentorTpMsg"));
        CnpcGuiSupport.button(gui, 25, mentorTpOn ? CnpcGuiStyle.toggleOn("Mentor TP")
                : CnpcGuiStyle.toggleOff("Mentor TP"), CnpcGuiSupport.COL_R, row,
                () -> act(player, "mentor_tpmsg", "toggle", "main"));
        row += 24;
        if (StaffAccess.isStaff(player)) {
            CnpcGuiSupport.buttonSmall(gui, CnpcGuiSupport.ID_STAFF_EXTRA, "§cStaff Admin", CnpcGuiSupport.COL_L, row, 95,
                    () -> open(player, "admin"));
            row += 24;
        }
        footer(player, gui, row, null);
    }

    private static void paintMentor(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Mentor"),
                "§7Invites, apprentices, and dojo tools");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.mentorLines(who), CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, "§aInvite apprentice…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_apprentice"));
        CnpcGuiSupport.button(gui, 21, "§bAsk mentor…", CnpcGuiSupport.COL_R, row, () -> open(player, "pick_mentor"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§6Pending invites", CnpcGuiSupport.COL_L, row, () -> open(player, "pending"));
        CnpcGuiSupport.button(gui, 23, "§eRelease apprentice…", CnpcGuiSupport.COL_R, row, () -> open(player, "pick_release"));
        row += 24;
        CnpcGuiSupport.button(gui, 24, "§cLeave mentor", CnpcGuiSupport.COL_L, row, () -> act(player, "mentor_leave", "", "mentor"));
        CnpcGuiSupport.button(gui, 25, "§5Dojo home", CnpcGuiSupport.COL_R, row, () -> open(player, "dojo"));
        row += 24;
        footer(player, gui, row, "main");
    }

    private static void paintPending(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Mentor pending"),
                CnpcGuiStyle.HINT_CLICK_INVITE);
        int listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY,
                SparGuiApi.pendingMentorLines(who), CnpcGuiStyle.INFO_LIST_HEADER_MAX));
        List<String> cards = SparGuiApi.pendingMentorInviteCards(who);
        if (!cards.isEmpty()) {
            int rowsBelow = 1;
            int bandY = listY + 14;
            int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
            IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, CnpcGuiSupport.cardLabels(cards, 1));
            scroll.setOnClick((g, sc) -> {
                String arg = CnpcGuiSupport.cardField(cards, sc, 0);
                if (arg != null) {
                    String picker = arg.contains(":") ? arg : "uuid:" + arg;
                    open(player, "pending_decide:" + picker);
                }
            });
            footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), "mentor");
        } else {
            footer(player, gui, listY + 8, "mentor");
        }
    }

    private static void openPendingDecide(ServerPlayer player, String pickerArg) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SPAR, CnpcGuiSupport.W, 300, (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, "§6Mentor invite",
                    "§f" + CnpcGuiSupport.humanizePickerArg(pickerArg));
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                    "§7Accept to bond as mentor/apprentice.",
                    "§7Decline to dismiss this invite."), 4));
            row += 8;
            CnpcGuiSupport.button(gui, 20, "§aAccept", CnpcGuiSupport.COL_L, row,
                    () -> act(pl, "mentor_accept", pickerArg, "pending"));
            CnpcGuiSupport.button(gui, 21, "§cDecline", CnpcGuiSupport.COL_R, row,
                    () -> act(pl, "mentor_decline", pickerArg, "pending"));
            row += CnpcGuiSupport.ROW_STEP + 4;
            CnpcGuiSupport.navSubmenu(pl, gui, row, () -> open(pl, "pending"), "§7« Back");
        });
    }

    /** {@code payload} = {@code action|returnPage|targetArg}. */
    private static void openPickConfirm(ServerPlayer player, String payload) {
        String[] parts = payload.split("\\|", 3);
        if (parts.length < 3) {
            open(player, "mentor");
            return;
        }
        String action = parts[0];
        String returnPage = parts[1];
        String targetArg = parts[2];
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SPAR, CnpcGuiSupport.W, 280, (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, "§eConfirm action",
                    "§f" + CnpcGuiSupport.humanizePickerArg(targetArg));
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                    "§7Action §f" + action.replace('_', ' '),
                    "§7Confirm to send the request."), 3));
            row += 8;
            CnpcGuiSupport.button(gui, 20, "§aConfirm", CnpcGuiSupport.COL_L, row,
                    () -> act(pl, action, targetArg, returnPage));
            CnpcGuiSupport.button(gui, 21, "§7Cancel", CnpcGuiSupport.COL_R, row, () -> open(pl, returnPage));
            row += CnpcGuiSupport.ROW_STEP + 4;
            CnpcGuiSupport.navSubmenu(pl, gui, row, () -> open(pl, returnPage), "§7« Back");
        });
    }

    private static void paintDojo(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Your dojo"),
                "§7Rankings, war, and members");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.linesForPage(who, "dojo"),
                CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, "§6Rankings", CnpcGuiSupport.COL_L, row, () -> open(player, "dojo_rank"));
        CnpcGuiSupport.button(gui, 21, "§cDojo war", CnpcGuiSupport.COL_R, row, () -> open(player, "dojo_war"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§eMembers", CnpcGuiSupport.COL_L, row, () -> open(player, "dojo_members"));
        CnpcGuiSupport.button(gui, 23, "§6Hall of fame", CnpcGuiSupport.COL_R, row, () -> open(player, "dojo_hof"));
        row += 24;
        footer(player, gui, row, "mentor");
    }

    private static void paintDojoWar(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Dojo war"),
                "§7Challenge rival dojos");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.dojoWarLines(who), CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, "§eChallenge rival dojo…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_dojo_challenge"));
        CnpcGuiSupport.button(gui, 21, "§6War pending", CnpcGuiSupport.COL_R, row, () -> open(player, "dojo_war_pending"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§aAccept war", CnpcGuiSupport.COL_L, row, () -> act(player, "dojo_accept", "", "dojo_war"));
        CnpcGuiSupport.button(gui, 23, "§cDecline war", CnpcGuiSupport.COL_R, row, () -> act(player, "dojo_decline", "", "dojo_war"));
        row += 24;
        footer(player, gui, row, "dojo");
    }

    private static void paintDojoWarPending(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Dojo war pending"),
                CnpcGuiStyle.HINT_CLICK_ENTRY);
        List<String> cards = SparGuiApi.pendingDojoWarCards(who);
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (cards.isEmpty()) {
            gui.addLabel(50, "§7No pending dojo wars.", CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, "dojo_war");
            return;
        }
        int rowsBelow = 1;
        int bandY = listY + 14;
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, CnpcGuiSupport.cardLabels(cards, 1));
        scroll.setOnClick((g, sc) -> {
            String arg = CnpcGuiSupport.cardField(cards, sc, 0);
            if (arg != null) {
                open(player, "dojo_war_pending_decide:" + arg);
            }
        });
        footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), "dojo_war");
    }

    private static void openDojoWarDecide(ServerPlayer player, String pickerArg) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SPAR, CnpcGuiSupport.W, 300, (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, "§6Dojo war invite",
                    "§f" + CnpcGuiSupport.humanizePickerArg(pickerArg));
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                    "§7Accept to start the dojo war.",
                    "§7Decline or revoke to cancel."), 3));
            row += 8;
            CnpcGuiSupport.button(gui, 20, "§aAccept", CnpcGuiSupport.COL_L, row,
                    () -> act(pl, "dojo_accept", pickerArg, "dojo_war_pending"));
            CnpcGuiSupport.button(gui, 21, "§cDecline / revoke", CnpcGuiSupport.COL_R, row,
                    () -> act(pl, "dojo_war_cancel", pickerArg, "dojo_war_pending"));
            row += CnpcGuiSupport.ROW_STEP + 4;
            CnpcGuiSupport.navSubmenu(pl, gui, row, () -> open(pl, "dojo_war_pending"), "§7« Back");
        });
    }

    private static void paintDojoMembers(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Dojo members"),
                "§7Roster snapshot");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.dojoMemberLines(who),
                CnpcGuiStyle.INFO_INLINE_MAX));
        footer(player, gui, row + 8, "dojo");
    }

    private static void paintTop(ServerPlayer player, ICustomGui gui, String category) {
        String cat = category == null || category.isBlank() ? "tp" : category;
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage("§b", "Sparring", "Leaderboard · " + CnpcGuiStyle.sparLeaderboardTab(cat)),
                "§7Top sparring players");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.topLines(subject(player), cat),
                CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiLayout.GridButton[] tabs = new CnpcGuiLayout.GridButton[] {
                CnpcGuiLayout.GridButton.run("§7Training pts", () -> open(player, "top_tp")),
                CnpcGuiLayout.GridButton.run("§7Wins", () -> open(player, "top_wins")),
                CnpcGuiLayout.GridButton.run("§7Streak", () -> open(player, "top_streak")),
        };
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(player, gui, row + 4, 40, tabs, () -> open(player, "top_" + cat));
        row += 4;
        footer(player, gui, row, "main");
    }

    private static void paintDojoTop(ServerPlayer player, ICustomGui gui, String category) {
        String cat = category == null || category.isBlank() ? "rp" : category;
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage("§b", "Sparring", "Dojo rankings · " + CnpcGuiStyle.sparLeaderboardTab(cat)),
                "§7Top dojos");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.dojoTopLines(subject(player), cat),
                CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiLayout.GridButton[] tabs = new CnpcGuiLayout.GridButton[] {
                CnpcGuiLayout.GridButton.run("§7Reputation", () -> open(player, "dojo_top_rp")),
                CnpcGuiLayout.GridButton.run("§7Wars", () -> open(player, "dojo_top_wars")),
        };
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(player, gui, row + 4, 40, tabs, () -> open(player, "dojo_top_" + cat));
        row += 4;
        footer(player, gui, row, "dojo");
    }

    private static void paintScroll(ServerPlayer player, ICustomGui gui, String title, List<String> body, String back) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, CnpcGuiStyle.HINT_READ_ONLY);
        int row = CnpcGuiSupport.paintLongReadOnlyBody(gui, infoY, body);
        footer(player, gui, row + 8, back);
    }

    private static void paintOnlinePick(ServerPlayer player, ICustomGui gui, String title, String action, String back) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, CnpcGuiStyle.HINT_CLICK_PLAYER);
        List<String> names = RivalGuiApi.onlinePlayerNames(subject(player));
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (names.isEmpty()) {
            gui.addLabel(50, "§cNo other players online.", CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, back);
            return;
        }
        int rowsBelow = 1;
        int bandY = listY + 14;
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, names.toArray(String[]::new));
        scroll.setOnClick((g, sc) -> {
            int[] sel = sc.getSelection();
            if (sel != null && sel.length > 0 && sel[0] >= 0 && sel[0] < names.size()) {
                open(player, "pick_confirm:" + action + "|" + back + "|" + names.get(sel[0]));
            }
        });
        footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), back);
    }

    private static void paintMentorArgPick(ServerPlayer player, ICustomGui gui, String title, String action, String back) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, CnpcGuiStyle.HINT_CLICK_ENTRY);
        List<String> args = SparGuiApi.pendingIncomingMentorArgs(subject(player));
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (args == null || args.isEmpty()) {
            gui.addLabel(50, "§7Nothing to pick.", CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, back);
            return;
        }
        int footerRow = paintArgScroll(player, gui, listY, args,
                arg -> open(player, "pick_confirm:" + action + "|" + back + "|" + arg));
        footer(player, gui, footerRow, back);
    }

    private static void paintReleasePick(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§eRelease apprentice",
                CnpcGuiStyle.HINT_CLICK_ENTRY);
        List<String> cards = SparGuiApi.apprenticeCards(subject(player));
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (cards.isEmpty()) {
            gui.addLabel(50, "§7No apprentices to release.", CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, "mentor");
            return;
        }
        int rowsBelow = 1;
        int bandY = listY + 14;
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, CnpcGuiSupport.cardLabels(cards, 1));
        scroll.setOnClick((g, sc) -> {
            String arg = CnpcGuiSupport.cardField(cards, sc, 0);
            if (arg != null) {
                open(player, "pick_confirm:mentor_release|mentor|" + arg);
            }
        });
        footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), "mentor");
    }

    private static void paintDojoChallengePick(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§cChallenge rival dojo",
                CnpcGuiStyle.HINT_REVIEW_DOJO);
        List<String> cards = SparGuiApi.rivalDojoCards(subject(player));
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (cards.isEmpty()) {
            gui.addLabel(50, "§7No rival dojo masters online.", CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, "dojo_war");
            return;
        }
        int rowsBelow = 1;
        int bandY = listY + 14;
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, CnpcGuiSupport.cardLabels(cards, 1));
        scroll.setOnClick((g, sc) -> {
            String arg = CnpcGuiSupport.cardField(cards, sc, 0);
            if (arg != null) {
                open(player, "pick_confirm:dojo_challenge|dojo_war|" + arg);
            }
        });
        footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), "dojo_war");
    }

    /** @return Y row for footer nav after the list */
    private static int paintArgScroll(
            ServerPlayer player,
            ICustomGui gui,
            int listY,
            List<String> args,
            java.util.function.Consumer<String> onPick
    ) {
        if (args == null || args.isEmpty()) {
            return listY + 8;
        }
        int rowsBelow = 1;
        int bandY = listY + 14;
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        String[] labels = CnpcGuiSupport.cardLabels(args, 0);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
        scroll.setOnClick((g, sc) -> {
            int[] sel = sc.getSelection();
            if (sel != null && sel.length > 0 && sel[0] >= 0 && sel[0] < args.size()) {
                onPick.accept(args.get(sel[0]));
            }
        });
        return CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
    }

    private static void paintAdmin(ServerPlayer player, ICustomGui gui) {
        if (!StaffAccess.isStaff(player)) {
            open(player, "main");
            return;
        }
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Staff Admin"),
                "§7Save spar data and print status");
        int row = CnpcGuiSupport.bodyBelowHeader(infoY);
        CnpcGuiSupport.button(gui, 20, "§aSave", CnpcGuiSupport.COL_L, row, () -> act(player, "admin", "save", "admin"));
        CnpcGuiSupport.button(gui, 21, "§7Status", CnpcGuiSupport.COL_R, row, () -> act(player, "admin", "status", "admin"));
        row += 24;
        footer(player, gui, row, "main");
    }

    /** {@code parentPage} null on spar main; otherwise Back reopens that page. Main always → LM hub. */
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
