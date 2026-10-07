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
                case "stats" -> paintScroll(pl, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Spar stats"), SparGuiApi.linesForPage(subject(pl), "stats"), "main");
                case "top", "leaderboard" -> paintTop(pl, gui, "tp");
                case "dojo_top", "dojo_rank", "dojo_rankings" -> paintDojoTop(pl, gui, "rp");
                case "mentor", "actions" -> paintMentor(pl, gui);
                case "pending", "invites" -> paintPending(pl, gui);
                case "dojo", "roster" -> paintDojo(pl, gui);
                case "dojo_war" -> paintDojoWar(pl, gui);
                case "dojo_war_pending" -> paintDojoWarPending(pl, gui);
                case "dojo_hof" -> paintScroll(pl, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Dojo hall of fame"),
                        SparGuiApi.linesForPage(subject(pl), "dojo_hof"), "dojo");
                case "dojo_members" -> paintDojoMembers(pl, gui);
                case "pick_apprentice" -> paintOnlinePick(pl, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Invite apprentice"), "mentor_invite", "mentor");
                case "pick_mentor" -> paintOnlinePick(pl, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Ask as apprentice"), "apprentice_invite", "mentor");
                case "pick_accept" -> paintMentorArgPick(pl, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Accept mentor invite"), "mentor_accept", "pending");
                case "pick_decline" -> paintMentorArgPick(pl, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Decline mentor invite"), "mentor_decline", "pending");
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
        CnpcGuiSupport.button(gui, 21, "§6Dojo rankings", CnpcGuiSupport.COL_R, row, () -> open(player, "dojo_rank"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 22, "§dLeaderboard", CnpcGuiSupport.COL_L, row, () -> open(player, "top"));
        CnpcGuiSupport.button(gui, 23, "§bTraining bonds", CnpcGuiSupport.COL_R, row, () -> open(player, "mentor"));
        row += CnpcGuiSupport.ROW_STEP;
        boolean tpOn = "true".equalsIgnoreCase(ph.get("tpMsg"));
        CnpcGuiSupport.button(gui, 24, tpOn ? CnpcGuiStyle.toggleOn("TP")
                : CnpcGuiStyle.toggleOff("TP"), CnpcGuiSupport.COL_L, row,
                () -> act(player, "tpmsg", "toggle", "main"));
        boolean mentorTpOn = "true".equalsIgnoreCase(ph.get("mentorTpMsg"));
        CnpcGuiSupport.button(gui, 25, mentorTpOn ? CnpcGuiStyle.toggleOn("Mentor TP")
                : CnpcGuiStyle.toggleOff("Mentor TP"), CnpcGuiSupport.COL_R, row,
                () -> act(player, "mentor_tpmsg", "toggle", "main"));
        row += CnpcGuiSupport.ROW_STEP;
        if (StaffAccess.isStaff(player)) {
            CnpcGuiSupport.buttonSmall(gui, CnpcGuiSupport.ID_STAFF_EXTRA, "§cStaff Admin", CnpcGuiSupport.COL_L, row, CnpcGuiSupport.BTN_W,
                    () -> open(player, "admin"));
            row += CnpcGuiSupport.ROW_STEP;
        }
        footer(player, gui, row, null);
    }

    private static void paintMentor(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Training bonds"),
                "§7Masters recruit · students request a master");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.mentorLines(who), CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, "§aRecruit apprentice…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_apprentice"));
        CnpcGuiSupport.button(gui, 21, "§bRequest a master…", CnpcGuiSupport.COL_R, row, () -> open(player, "pick_mentor"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 22, "§6Bond invites", CnpcGuiSupport.COL_L, row, () -> open(player, "pending"));
        CnpcGuiSupport.button(gui, 23, "§eRelease apprentice…", CnpcGuiSupport.COL_R, row, () -> open(player, "pick_release"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 24, "§cLeave mentor", CnpcGuiSupport.COL_L, row,
                () -> open(player, "pick_confirm:mentor|mentor|leave"));
        CnpcGuiSupport.button(gui, 25, "§5Dojo home", CnpcGuiSupport.COL_R, row, () -> open(player, "dojo"));
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, "main");
    }

    private static void paintPending(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Bond invites"),
                "§eMentor ↔ apprentice requests only");
        int listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY,
                SparGuiApi.pendingMentorLines(who), CnpcGuiStyle.INFO_LIST_HEADER_MAX));
        List<String> cards = SparGuiApi.pendingMentorInviteCards(who);
        if (!cards.isEmpty()) {
            int rowsBelow = 1;
            String[] labels = CnpcGuiSupport.cardLabels(cards, 1);
            int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
            int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
            IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
            scroll.setOnClick((g, sc) -> CnpcGuiSupport.afterGuiClosed(g, () -> {
                String arg = CnpcGuiSupport.cardField(cards, sc, 0);
                String dir = CnpcGuiSupport.cardField(cards, sc, 2);
                if (arg != null) {
                    String picker = arg.contains(":") ? arg : "uuid:" + arg;
                    String direction = "OUT".equalsIgnoreCase(dir) ? "OUT" : "IN";
                    open(player, "pending_decide:" + direction + "|" + picker);
                }
            }));
            footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), "mentor");
        } else {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    "§7No bond invites waiting.",
                    CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, "mentor");
        }
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
        String who = CnpcGuiSupport.humanizePickerArg(picker);
        String pickerFinal = picker;
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SPAR, CnpcGuiSupport.W, 300, (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui,
                    CnpcGuiStyle.subPage("§b", "Sparring", "Training bond invite"), "§f" + who);
            List<String> body = outgoing
                    ? List.of("§7You invited §f" + who + "§7.")
                    : List.of(
                            "§fAccept §7to form this training bond.",
                            "§fDecline §7to dismiss the invite.");
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, body, CnpcGuiStyle.INFO_INLINE_MAX));
            row += 8;
            if (outgoing) {
                CnpcGuiSupport.button(gui, 20, "§cWithdraw invite", CnpcGuiSupport.COL_L, row,
                        () -> act(pl, "mentor", "cancel:" + pickerFinal, "pending"));
            } else {
                CnpcGuiSupport.button(gui, 20, "§aAccept", CnpcGuiSupport.COL_L, row,
                        () -> act(pl, "mentor_accept", pickerFinal, "pending"));
                CnpcGuiSupport.button(gui, 21, "§cDecline", CnpcGuiSupport.COL_R, row,
                        () -> act(pl, "mentor_decline", pickerFinal, "pending"));
            }
            row += CnpcGuiSupport.ROW_STEP;
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
        String[] copy = confirmCopy(action);
        String who = "mentor".equals(action) && "leave".equalsIgnoreCase(targetArg)
                ? "Your master"
                : CnpcGuiSupport.humanizePickerArg(targetArg);
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SPAR, CnpcGuiSupport.W, 280, (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, copy[0], "§f" + who);
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(copy[1]), 3));
            row += 8;
            CnpcGuiSupport.button(gui, 20, "§aConfirm", CnpcGuiSupport.COL_L, row,
                    () -> act(pl, action, targetArg, returnPage));
            CnpcGuiSupport.button(gui, 21, "§7Cancel", CnpcGuiSupport.COL_R, row, () -> open(pl, returnPage));
            row += CnpcGuiSupport.ROW_STEP;
            CnpcGuiSupport.navSubmenu(pl, gui, row, () -> open(pl, returnPage), "§7« Back");
        });
    }

    /** Plain confirm title and sentence. Never shows the raw action id. */
    private static String[] confirmCopy(String action) {
        return switch (action) {
            case "mentor_invite" -> new String[] {
                    CnpcGuiStyle.subPage("§b", "Sparring", "Invite apprentice"),
                    "§fInvite §7this player to train under you."
            };
            case "apprentice_invite" -> new String[] {
                    CnpcGuiStyle.subPage("§b", "Sparring", "Ask as apprentice"),
                    "§fAsk §7this player to be your master."
            };
            case "mentor_release" -> new String[] {
                    CnpcGuiStyle.subPage("§b", "Sparring", "Release apprentice"),
                    "§fRelease §7this apprentice. A 12-hour cooldown starts."
            };
            case "mentor" -> new String[] {
                    CnpcGuiStyle.subPage("§b", "Sparring", "Leave mentor"),
                    "§fLeave §7your master. A 12-hour cooldown starts."
            };
            case "mentor_accept" -> new String[] {
                    CnpcGuiStyle.subPage("§b", "Sparring", "Accept mentor invite"),
                    "§fAccept §7to form this training bond."
            };
            case "mentor_decline" -> new String[] {
                    CnpcGuiStyle.subPage("§b", "Sparring", "Decline mentor invite"),
                    "§fDecline §7to dismiss the invite."
            };
            case "dojo_challenge" -> new String[] {
                    CnpcGuiStyle.subPage("§b", "Sparring", "Challenge rival dojo"),
                    "§fChallenge §7this dojo to a war."
            };
            default -> new String[] {
                    CnpcGuiStyle.subPage("§b", "Sparring", "Confirm action"),
                    "§fConfirm §7to continue."
            };
        };
    }

    private static void paintDojo(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Dojo home"),
                "§7Rankings, war, and members");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.linesForPage(who, "dojo"),
                CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, "§6Rankings", CnpcGuiSupport.COL_L, row, () -> open(player, "dojo_rank"));
        CnpcGuiSupport.button(gui, 21, "§cDojo war", CnpcGuiSupport.COL_R, row, () -> open(player, "dojo_war"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 22, "§eMembers", CnpcGuiSupport.COL_L, row, () -> open(player, "dojo_members"));
        CnpcGuiSupport.button(gui, 23, "§6Hall of fame", CnpcGuiSupport.COL_R, row, () -> open(player, "dojo_hof"));
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, "mentor");
    }

    private static void paintDojoWar(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Dojo war"),
                "§7Challenge rival dojos");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.dojoWarLines(who), CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, "§eChallenge rival dojo…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_dojo_challenge"));
        int warPending = SparGuiApi.pendingDojoWarCards(who).size();
        String warPendingLabel = warPending > 0 ? "§eWar pending §f(" + warPending + ")" : "§6War pending";
        CnpcGuiSupport.button(gui, 21, warPendingLabel, CnpcGuiSupport.COL_R, row, () -> open(player, "dojo_war_pending"));
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, "dojo");
    }

    private static void paintDojoWarPending(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Dojo war pending"),
                CnpcGuiStyle.HINT_CLICK_ENTRY);
        List<String> cards = SparGuiApi.pendingDojoWarCards(who);
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (cards.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    "§7No dojo war challenges waiting — declare war from Dojo when your roster is ready.",
                    CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, "dojo_war");
            return;
        }
        int rowsBelow = 1;
        String[] warLabels = CnpcGuiSupport.cardLabels(cards, 1);
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, warLabels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, warLabels);
        scroll.setOnClick((g, sc) -> CnpcGuiSupport.afterGuiClosed(g, () -> {
            String arg = CnpcGuiSupport.cardField(cards, sc, 0);
            if (arg != null) {
                open(player, "dojo_war_pending_decide:" + arg);
            }
        }));
        footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), "dojo_war");
    }

    private static void openDojoWarDecide(ServerPlayer player, String pickerArg) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SPAR, CnpcGuiSupport.W, 300, (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Dojo war request"),
                    "§f" + CnpcGuiSupport.humanizePickerArg(pickerArg));
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                    "§fAccept §7to start the war (2× RP vs that dojo).",
                    "§fCancel §7to drop this war challenge."), 3));
            row += 8;
            CnpcGuiSupport.button(gui, 20, "§aAccept", CnpcGuiSupport.COL_L, row,
                    () -> act(pl, "dojo_accept", pickerArg, "dojo_war_pending"));
            CnpcGuiSupport.button(gui, 21, "§cCancel challenge", CnpcGuiSupport.COL_R, row,
                    () -> act(pl, "dojo_war_cancel", pickerArg, "dojo_war_pending"));
            row += CnpcGuiSupport.ROW_STEP;
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
        // Wins and win streak are not stored on the spar leaderboard. Those tabs used to show Training Points.
        if ("wins".equalsIgnoreCase(cat) || "win".equalsIgnoreCase(cat)
                || "streak".equalsIgnoreCase(cat)) {
            cat = "tp";
        }
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage("§b", "Sparring", "Leaderboard · " + CnpcGuiStyle.sparLeaderboardTab(cat)),
                "§7Top sparring players");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.topLines(subject(player), cat),
                CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiLayout.GridButton[] tabs = new CnpcGuiLayout.GridButton[] {
                leaderboardTab(player, "§7Training points", "top_tp"),
                leaderboardTab(player, "§7Sessions", "top_sessions"),
                leaderboardTab(player, "§7Perfect spars", "top_perfect"),
                leaderboardTab(player, "§7Combo", "top_combo"),
                leaderboardTab(player, "§7Time", "top_time"),
        };
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(player, gui, row, CnpcGuiSupport.ID_GRID_BASE, tabs,
                () -> open(player, "top_" + cat));
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
                leaderboardTab(player, "§7Reputation", "dojo_top_rp"),
                leaderboardTab(player, "§7Wars", "dojo_top_wars"),
        };
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(player, gui, row, CnpcGuiSupport.ID_GRID_BASE, tabs,
                () -> open(player, "dojo_top_" + cat));
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
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER, "§7No other players are online.", CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, back);
            return;
        }
        int rowsBelow = 1;
        String[] nameItems = names.toArray(String[]::new);
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, nameItems.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, nameItems);
        scroll.setOnClick((g, sc) -> CnpcGuiSupport.afterGuiClosed(g, () -> {
            int[] sel = sc.getSelection();
            if (sel != null && sel.length > 0 && sel[0] >= 0 && sel[0] < nameItems.length) {
                open(player, "pick_confirm:" + action + "|" + back + "|" + nameItems[sel[0]]);
            }
        }));
        footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), back);
    }

    private static void paintMentorArgPick(ServerPlayer player, ICustomGui gui, String title, String action, String back) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, CnpcGuiStyle.HINT_CLICK_ENTRY);
        List<String> args = SparGuiApi.pendingIncomingMentorArgs(subject(player));
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (args == null || args.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    "§7No bond invites waiting.",
                    CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, back);
            return;
        }
        int footerRow = paintArgScroll(player, gui, listY, args,
                arg -> open(player, "pick_confirm:" + action + "|" + back + "|" + arg));
        footer(player, gui, footerRow, back);
    }

    private static void paintReleasePick(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Release apprentice"),
                CnpcGuiStyle.HINT_CLICK_ENTRY);
        List<String> cards = SparGuiApi.apprenticeCards(subject(player));
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (cards.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    "§7No apprentices to release.",
                    CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, "mentor");
            return;
        }
        int rowsBelow = 1;
        String[] releaseLabels = CnpcGuiSupport.cardLabels(cards, 1);
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, releaseLabels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, releaseLabels);
        scroll.setOnClick((g, sc) -> CnpcGuiSupport.afterGuiClosed(g, () -> {
            String arg = CnpcGuiSupport.cardField(cards, sc, 0);
            if (arg != null) {
                open(player, "pick_confirm:mentor_release|mentor|" + arg);
            }
        }));
        footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), "mentor");
    }

    private static void paintDojoChallengePick(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§b", "Sparring", "Challenge rival dojo"),
                CnpcGuiStyle.HINT_REVIEW_DOJO);
        List<String> cards = SparGuiApi.rivalDojoCards(subject(player));
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (cards.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    "§7No rival dojo masters are online — challenge when one is on.",
                    CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, "dojo_war");
            return;
        }
        int rowsBelow = 1;
        String[] dojoLabels = CnpcGuiSupport.cardLabels(cards, 1);
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, dojoLabels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, dojoLabels);
        scroll.setOnClick((g, sc) -> CnpcGuiSupport.afterGuiClosed(g, () -> {
            String arg = CnpcGuiSupport.cardField(cards, sc, 0);
            if (arg != null) {
                open(player, "pick_confirm:dojo_challenge|dojo_war|" + arg);
            }
        }));
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
        String[] labels = CnpcGuiSupport.cardLabels(args, 0);
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
        scroll.setOnClick((g, sc) -> CnpcGuiSupport.afterGuiClosed(g, () -> {
            int[] sel = sc.getSelection();
            if (sel != null && sel.length > 0 && sel[0] >= 0 && sel[0] < args.size()) {
                onPick.accept(args.get(sel[0]));
            }
        }));
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
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, "main");
    }

    /** Board switches acknowledge in the notice band, same as other menu actions. */
    private static CnpcGuiLayout.GridButton leaderboardTab(ServerPlayer player, String label, String page) {
        String plain = label.replaceAll("§.", "");
        return CnpcGuiLayout.GridButton.action(label,
                () -> "§7Showing " + plain + ".",
                () -> open(player, page));
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
