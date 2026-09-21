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
    private static final int H = 340;

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
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SPAR, CnpcGuiSupport.W, H, (pl, gui) -> {
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
                "§7Session §f" + ("true".equals(ph.get("session_active")) ? "§aactive" : "§8idle")
                        + (partner == null || partner.isBlank() ? "" : " §8· §7vs §f" + partner));

        if (!"true".equalsIgnoreCase(ph.get("system_enabled"))) {
            int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of("§cSparring system is disabled."), 3);
            footer(player, gui, row + 8, "main");
            return;
        }

        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.linesForPage(who, "main"), 4);
        CnpcGuiSupport.button(gui, 20, "§eStats", CnpcGuiSupport.COL_L, row, () -> open(player, "stats"));
        CnpcGuiSupport.button(gui, 21, "§6Dojo rank", CnpcGuiSupport.COL_R, row, () -> open(player, "dojo_rank"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§dLeaderboard", CnpcGuiSupport.COL_L, row, () -> open(player, "top"));
        CnpcGuiSupport.button(gui, 23, "§bMentor & dojo", CnpcGuiSupport.COL_R, row, () -> open(player, "mentor"));
        row += 24;
        boolean tpOn = "true".equalsIgnoreCase(ph.get("tpMsg"));
        CnpcGuiSupport.button(gui, 24, tpOn ? "§aSpar TP msg ON" : "§8Spar TP msg OFF", CnpcGuiSupport.COL_L, row,
                () -> act(player, "tpmsg", "toggle", "main"));
        boolean mentorTpOn = "true".equalsIgnoreCase(ph.get("mentorTpMsg"));
        CnpcGuiSupport.button(gui, 25, mentorTpOn ? "§aMentor TP ON" : "§8Mentor TP OFF", CnpcGuiSupport.COL_R, row,
                () -> act(player, "mentor_tpmsg", "toggle", "main"));
        row += 24;
        CnpcGuiSupport.button(gui, 26, "§cEnd session", CnpcGuiSupport.COL_L, row, () -> act(player, "end", "", "main"));
        if (StaffAccess.isStaff(player)) {
            CnpcGuiSupport.buttonSmall(gui, 98, "§cStaff admin", CnpcGuiSupport.COL_R, row, 95, () -> open(player, "admin"));
        }
        row += 24;
        footer(player, gui, row, "main");
    }

    private static void paintMentor(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§bMentor actions", "§7Invites, release, dojo");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.mentorLines(who), 4);
        CnpcGuiSupport.button(gui, 20, "§aInvite apprentice…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_apprentice"));
        CnpcGuiSupport.button(gui, 21, "§bAsk mentor…", CnpcGuiSupport.COL_R, row, () -> open(player, "pick_mentor"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§6Pending invites", CnpcGuiSupport.COL_L, row, () -> open(player, "pending"));
        CnpcGuiSupport.button(gui, 23, "§eRelease apprentice…", CnpcGuiSupport.COL_R, row, () -> open(player, "pick_release"));
        row += 24;
        CnpcGuiSupport.button(gui, 24, "§cLeave mentor", CnpcGuiSupport.COL_L, row, () -> act(player, "mentor_leave", "", "mentor"));
        CnpcGuiSupport.button(gui, 25, "§5Dojo home", CnpcGuiSupport.COL_R, row, () -> open(player, "dojo"));
        row += 24;
        footer(player, gui, row, "mentor");
    }

    private static void paintPending(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§6Mentor pending", "§7Double-click to respond");
        int listY = CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.pendingMentorLines(who), 3);
        List<String> cards = SparGuiApi.pendingMentorInviteCards(who);
        if (!cards.isEmpty()) {
            IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M,
                    listY, 400, 120, CnpcGuiSupport.cardLabels(cards, 1));
            CnpcGuiSupport.wireScrollDoublePick(scroll, cards, 0,
                    arg -> open(player, "pending_decide:" + arg));
            footer(player, gui, listY + 128, "mentor");
        } else {
            footer(player, gui, listY + 8, "mentor");
        }
    }

    private static void openPendingDecide(ServerPlayer player, String pickerArg) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SPAR, CnpcGuiSupport.W, 240, (pl, gui) -> {
            CnpcGuiSupport.title(gui, 1, "§6Mentor invite");
            int row = 100;
            CnpcGuiSupport.button(gui, 20, "§aAccept", CnpcGuiSupport.COL_L, row,
                    () -> act(pl, "mentor_accept", pickerArg, "pending"));
            CnpcGuiSupport.button(gui, 21, "§cDecline", CnpcGuiSupport.COL_R, row,
                    () -> act(pl, "mentor_decline", pickerArg, "pending"));
            row += 28;
            CnpcGuiSupport.buttonSmall(gui, 96, "§7« Back", CnpcGuiSupport.COL_L, row, 95, () -> open(pl, "pending"));
        });
    }

    private static void paintDojo(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§5Your dojo", "§7Rankings, war, members");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.linesForPage(who, "dojo"), 4);
        CnpcGuiSupport.button(gui, 20, "§6Rankings", CnpcGuiSupport.COL_L, row, () -> open(player, "dojo_rank"));
        CnpcGuiSupport.button(gui, 21, "§cDojo war", CnpcGuiSupport.COL_R, row, () -> open(player, "dojo_war"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§eMembers", CnpcGuiSupport.COL_L, row, () -> open(player, "dojo_members"));
        CnpcGuiSupport.button(gui, 23, "§6Hall of fame", CnpcGuiSupport.COL_R, row, () -> open(player, "dojo_hof"));
        row += 24;
        footer(player, gui, row, "dojo");
    }

    private static void paintDojoWar(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§cDojo war", "§7Challenge rival dojos");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.dojoWarLines(who), 4);
        CnpcGuiSupport.button(gui, 20, "§eChallenge rival dojo…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_dojo_challenge"));
        CnpcGuiSupport.button(gui, 21, "§6War pending", CnpcGuiSupport.COL_R, row, () -> open(player, "dojo_war_pending"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§aAccept war", CnpcGuiSupport.COL_L, row, () -> act(player, "dojo_accept", "", "dojo_war"));
        CnpcGuiSupport.button(gui, 23, "§cDecline war", CnpcGuiSupport.COL_R, row, () -> act(player, "dojo_decline", "", "dojo_war"));
        row += 24;
        footer(player, gui, row, "dojo_war");
    }

    private static void paintDojoWarPending(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        CnpcGuiSupport.title(gui, 1, "§6Dojo war pending");
        List<String> cards = SparGuiApi.pendingDojoWarCards(who);
        if (cards.isEmpty()) {
            gui.addLabel(50, "§7No pending dojo wars.", CnpcGuiSupport.M, 60, 400, 14);
        } else {
            IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M, 48,
                    400, 160,
                    CnpcGuiSupport.cardLabels(cards, 1));
            CnpcGuiSupport.wireScrollDoublePick(scroll, cards, 0,
                    arg -> open(player, "dojo_war_pending_decide:" + arg));
        }
        footer(player, gui, 220, "dojo_war");
    }

    private static void openDojoWarDecide(ServerPlayer player, String pickerArg) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SPAR, CnpcGuiSupport.W, 240, (pl, gui) -> {
            CnpcGuiSupport.title(gui, 1, "§6Dojo war invite");
            int row = 100;
            CnpcGuiSupport.button(gui, 20, "§aAccept", CnpcGuiSupport.COL_L, row,
                    () -> act(pl, "dojo_accept", pickerArg, "dojo_war_pending"));
            CnpcGuiSupport.button(gui, 21, "§cDecline / revoke", CnpcGuiSupport.COL_R, row,
                    () -> act(pl, "dojo_war_cancel", pickerArg, "dojo_war_pending"));
            row += 28;
            CnpcGuiSupport.buttonSmall(gui, 96, "§7« Back", CnpcGuiSupport.COL_L, row, 95, () -> open(pl, "dojo_war_pending"));
        });
    }

    private static void paintDojoMembers(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§eDojo members", "§7Roster snapshot");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.dojoMemberLines(who), 5);
        footer(player, gui, row + 8, "dojo");
    }

    private static void paintTop(ServerPlayer player, ICustomGui gui, String category) {
        String cat = category == null || category.isBlank() ? "tp" : category;
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§fSpar top · " + cat, "§7Leaderboard");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.topLines(subject(player), cat), 5);
        row += 8;
        CnpcGuiSupport.buttonSmall(gui, 40, "§7TP", CnpcGuiSupport.COL_L, row, 60, () -> open(player, "top_tp"));
        CnpcGuiSupport.buttonSmall(gui, 41, "§7Wins", 130, row, 60, () -> open(player, "top_wins"));
        CnpcGuiSupport.buttonSmall(gui, 42, "§7Streak", 200, row, 60, () -> open(player, "top_streak"));
        row += 28;
        footer(player, gui, row, "main");
    }

    private static void paintDojoTop(ServerPlayer player, ICustomGui gui, String category) {
        String cat = category == null || category.isBlank() ? "rp" : category;
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§6Dojo rankings · " + cat, "§7Leaderboard");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.dojoTopLines(subject(player), cat), 5);
        row += 8;
        CnpcGuiSupport.buttonSmall(gui, 40, "§7RP", CnpcGuiSupport.COL_L, row, 60, () -> open(player, "dojo_top_rp"));
        CnpcGuiSupport.buttonSmall(gui, 41, "§7Wars", 130, row, 60, () -> open(player, "dojo_top_wars"));
        footer(player, gui, row + 28, "dojo");
    }

    private static void paintScroll(ServerPlayer player, ICustomGui gui, String title, List<String> body, String back) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, "§7Details");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, body, 4);
        footer(player, gui, row + 8, back);
    }

    private static void paintOnlinePick(ServerPlayer player, ICustomGui gui, String title, String action, String back) {
        CnpcGuiSupport.title(gui, 1, title);
        List<String> names = RivalGuiApi.onlinePlayerNames(subject(player));
        if (names.isEmpty()) {
            gui.addLabel(50, "§cNo other players online.", CnpcGuiSupport.M, 70, 400, 14);
        } else {
            IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M, 48,
                    400, 160,
                    names.toArray(String[]::new));
            scroll.setOnDoubleClick((g, sc) -> {
                g.close();
                int[] sel = sc.getSelection();
                if (sel != null && sel.length > 0 && sel[0] >= 0 && sel[0] < names.size()) {
                    act(player, action, names.get(sel[0]), back);
                }
            });
        }
        footer(player, gui, 220, back);
    }

    private static void paintMentorArgPick(ServerPlayer player, ICustomGui gui, String title, String action, String back) {
        CnpcGuiSupport.title(gui, 1, title);
        List<String> args = SparGuiApi.pendingIncomingMentorArgs(subject(player));
        paintArgScroll(player, gui, args, arg -> act(player, action, arg, back));
        footer(player, gui, 220, back);
    }

    private static void paintReleasePick(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§eRelease apprentice");
        List<String> cards = SparGuiApi.apprenticeCards(subject(player));
        if (cards.isEmpty()) {
            gui.addLabel(50, "§7No apprentices to release.", CnpcGuiSupport.M, 70, 400, 14);
        } else {
            IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M, 48,
                    400, 160,
                    CnpcGuiSupport.cardLabels(cards, 1));
            CnpcGuiSupport.wireScrollDoublePick(scroll, cards, 0,
                    arg -> act(player, "mentor_release", arg, "mentor"));
        }
        footer(player, gui, 220, "mentor");
    }

    private static void paintDojoChallengePick(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§cChallenge rival dojo");
        List<String> cards = SparGuiApi.rivalDojoCards(subject(player));
        if (cards.isEmpty()) {
            gui.addLabel(50, "§7No rival dojo masters online.", CnpcGuiSupport.M, 70, 400, 14);
        } else {
            IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M, 48,
                    400, 160,
                    CnpcGuiSupport.cardLabels(cards, 1));
            CnpcGuiSupport.wireScrollDoublePick(scroll, cards, 0,
                    arg -> act(player, "dojo_challenge", arg, "dojo_war"));
        }
        footer(player, gui, 220, "dojo_war");
    }

    private static void paintArgScroll(
            ServerPlayer player,
            ICustomGui gui,
            List<String> args,
            java.util.function.Consumer<String> onPick
    ) {
        if (args == null || args.isEmpty()) {
            return;
        }
        String[] labels = CnpcGuiSupport.cardLabels(args, 0);
        IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M, 48, 400,
                160, labels);
        scroll.setOnDoubleClick((g, sc) -> {
            g.close();
            int[] sel = sc.getSelection();
            if (sel != null && sel.length > 0 && sel[0] >= 0 && sel[0] < args.size()) {
                onPick.accept(args.get(sel[0]));
            }
        });
    }

    private static void paintAdmin(ServerPlayer player, ICustomGui gui) {
        if (!StaffAccess.isStaff(player)) {
            open(player, "main");
            return;
        }
        CnpcGuiSupport.title(gui, 1, "§cSpar admin");
        int row = 90;
        CnpcGuiSupport.button(gui, 20, "§aSave", CnpcGuiSupport.COL_L, row, () -> act(player, "admin", "save", "admin"));
        CnpcGuiSupport.button(gui, 21, "§7Status", CnpcGuiSupport.COL_R, row, () -> act(player, "admin", "status", "admin"));
        row += 24;
        footer(player, gui, row, "main");
    }

    private static void footer(ServerPlayer player, ICustomGui gui, int row, String mainPage) {
        CnpcGuiSupport.navHubMain(player, gui, row, () -> open(player, mainPage));
    }
}
