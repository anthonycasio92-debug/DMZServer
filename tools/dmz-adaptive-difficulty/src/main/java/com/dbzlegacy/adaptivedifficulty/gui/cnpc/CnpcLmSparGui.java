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
        CnpcUltraStyle.withAccent(CnpcUltraStyle.ACCENT_SPARRING, () -> openSpar(player, page));
    }

    private static void openSpar(ServerPlayer player, String page) {
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
        final String pageKey = lower.isBlank() || "main".equals(lower) ? "dojo" : lower;
        int tab = sparTabs(pageKey) ? CnpcGuiSupport.TAB_BAR_H : 0;
        int height = CnpcGuiSupport.window(switch (pageKey) {
            case "stats" -> 420;
            case "dojo", "roster" -> 520;
            default -> (pageKey.startsWith("pick_") || dojoTab(pageKey) != null
                    || pageKey.startsWith("top") || pageKey.startsWith("dojo_top")
                    || "dojo_rank".equals(pageKey) || "dojo_rankings".equals(pageKey)) ? 480 : H;
        } + tab);
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SPAR, CnpcGuiSupport.W, height, (pl, gui) -> {
            if (pageKey.startsWith("top_")) {
                paintTop(pl, gui, pageKey.substring(4).trim());
                return;
            }
            if (pageKey.startsWith("dojo_top_")) {
                paintTop(pl, gui, pageKey.substring("dojo_top_".length()).trim());
                return;
            }
            if ("dojo_rank".equals(pageKey) || "dojo_rankings".equals(pageKey)) {
                paintTop(pl, gui, "rp");
                return;
            }
            String dojo = dojoTab(pageKey);
            if (dojo != null) {
                paintDojo(pl, gui, dojo);
                return;
            }
            switch (pageKey) {
                case "stats" -> paintScroll(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Spar stats"), SparGuiApi.linesForPage(subject(pl), "stats"), "dojo");
                case "top", "leaderboard" -> paintTop(pl, gui, "tp");
                case "mentor", "actions" -> paintMentor(pl, gui);
                case "pending", "invites" -> paintPending(pl, gui);
                case "dojo_war" -> paintDojoWar(pl, gui);
                case "dojo_war_pending" -> paintDojoWarPending(pl, gui);
                case "pick_apprentice" -> paintOnlinePick(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Invite apprentice"), "mentor_invite", "pick_apprentice", "mentor");
                case "pick_mentor" -> paintOnlinePick(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Ask as apprentice"), "apprentice_invite", "pick_mentor", "mentor");
                case "pick_accept" -> paintMentorArgPick(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Accept mentor invite"), "mentor_accept", "pick_accept", "pending");
                case "pick_decline" -> paintMentorArgPick(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Decline mentor invite"), "mentor_decline", "pick_decline", "pending");
                case "pick_release" -> paintReleasePick(pl, gui);
                case "pick_dojo_challenge" -> paintDojoChallengePick(pl, gui);
                case "admin" -> paintAdmin(pl, gui);
                case "settings" -> paintChatSettings(pl, gui);
                default -> paintMain(pl, gui);
            }
        });
    }

    /** Every sparring page keeps Dojo, Rankings, and Wars, including sub-pages. */
    private static boolean sparTabs(String page) {
        return page != null && !page.isBlank();
    }

    private static String sparTabId(String page) {
        if (page == null) {
            return "dojo";
        }
        String p = page.toLowerCase(Locale.ROOT);
        if (p.startsWith("top") || "leaderboard".equals(p) || p.startsWith("dojo_top")
                || "dojo_rank".equals(p) || "dojo_rankings".equals(p)) {
            return "rankings";
        }
        if (p.startsWith("dojo_war") || "pick_dojo_challenge".equals(p) || p.contains("dojo_challenge")) {
            return "wars";
        }
        return "dojo";
    }

    private static int sparTabBar(ServerPlayer player, ICustomGui gui, int y, String active) {
        return CnpcGuiSupport.paintTabBar(gui, y, new String[] {
                "dojo|Dojo", "rankings|Rankings", "wars|Wars"
        }, active, action -> {
            String id = action.startsWith("tab:") ? action.substring(4) : action;
            switch (id) {
                case "rankings" -> open(player, "top");
                case "wars" -> open(player, "dojo_war");
                default -> open(player, "dojo");
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

    /** Unknown pages land on Dojo. The old overview only repeated the tab bar. */
    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        paintDojo(player, gui, "home");
    }

    private static void paintMentor(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Training bonds"),
                CnpcUltraStyle.SUBTITLE + "Masters recruit · students request a master");
        infoY = sparTabBar(player, gui, infoY, "dojo");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.mentorLines(who), CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Recruit apprentice…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_apprentice"));
        CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.ACCENT + "Request a master…", CnpcGuiSupport.COL_R, row, () -> open(player, "pick_mentor"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 22, CnpcUltraStyle.ACCENT + "Bond invites", CnpcGuiSupport.COL_L, row, () -> open(player, "pending"));
        CnpcGuiSupport.button(gui, 23, CnpcUltraStyle.INFO + "Release apprentice…", CnpcGuiSupport.COL_R, row, () -> open(player, "pick_release"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 24, CnpcUltraStyle.DANGER + "Leave mentor", CnpcGuiSupport.COL_L, row,
                () -> open(player, "pick_confirm:mentor|mentor|leave"));
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, "dojo");
    }

    private static void paintPending(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Bond invites"),
                CnpcUltraStyle.INFO + "Mentor ↔ apprentice requests only");
        infoY = sparTabBar(player, gui, infoY, "dojo");
        int listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY,
                SparGuiApi.pendingMentorLines(who), CnpcGuiStyle.INFO_LIST_HEADER_MAX));
        List<String> cards = SparGuiApi.pendingMentorInviteCards(who);
        if (!cards.isEmpty()) {
            int rowsBelow = 2;
            String[] labels = CnpcGuiSupport.cardLabels(cards, 1);
            int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
            int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
            IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
            int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
            CnpcGuiSupport.selectionButton(player, gui, 30, CnpcUltraStyle.INFO + "Open", CnpcGuiSupport.COL_L, row,
                    CnpcGuiSupport.BTN_W, () -> {
                        String arg = CnpcGuiSupport.cardField(cards, scroll, 0);
                        if (arg == null) {
                            return null;
                        }
                        String dir = CnpcGuiSupport.cardField(cards, scroll, 2);
                        String picker = arg.contains(":") ? arg : "uuid:" + arg;
                        String direction = "OUT".equalsIgnoreCase(dir) ? "OUT" : "IN";
                        return direction + "|" + picker;
                    },
                    packed -> open(player, "pending_decide:" + packed),
                    () -> open(player, "pending"));
            footer(player, gui, row + CnpcGuiSupport.ROW_STEP, "mentor");
        } else {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    CnpcUltraStyle.SUBTITLE + "No bond invites waiting. Send one from Training bonds, or wait for someone to invite you.",
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
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SPAR, CnpcGuiSupport.W, CnpcGuiSupport.window(300), (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui,
                    CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Training bond invite"), CnpcUltraStyle.BODY + who);
            infoY = sparTabBar(pl, gui, infoY, "dojo");
            List<String> body = outgoing
                    ? List.of(CnpcUltraStyle.SUBTITLE + "You invited " + CnpcUltraStyle.BODY + who + CnpcUltraStyle.SUBTITLE + ".")
                    : List.of(
                            CnpcUltraStyle.BODY + "Accept " + CnpcUltraStyle.SUBTITLE + "to form this training bond.",
                            CnpcUltraStyle.BODY + "Decline " + CnpcUltraStyle.SUBTITLE + "to dismiss the invite.");
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, body, CnpcGuiStyle.INFO_INLINE_MAX));
            row += 8;
            if (outgoing) {
                CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.DANGER + "Withdraw invite", CnpcGuiSupport.COL_L, row,
                        () -> act(pl, "mentor", "cancel:" + pickerFinal, "pending"));
            } else {
                CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Accept", CnpcGuiSupport.COL_L, row,
                        () -> act(pl, "mentor_accept", pickerFinal, "pending"));
                CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.DANGER + "Decline", CnpcGuiSupport.COL_R, row,
                        () -> act(pl, "mentor_decline", pickerFinal, "pending"));
            }
            row += CnpcGuiSupport.ROW_STEP;
            CnpcGuiSupport.navBackToParent(pl, gui, row, () -> open(pl, "pending"));
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
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SPAR, CnpcGuiSupport.W, CnpcGuiSupport.window(280), (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, copy[0], CnpcUltraStyle.BODY + who);
            infoY = sparTabBar(pl, gui, infoY, sparTabId(returnPage));
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(copy[1]), 3));
            row += 8;
            CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Confirm", CnpcGuiSupport.COL_L, row,
                    () -> act(pl, action, targetArg, returnPage));
            CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.SUBTITLE + "Cancel", CnpcGuiSupport.COL_R, row, () -> open(pl, returnPage));
            row += CnpcGuiSupport.ROW_STEP;
            CnpcGuiSupport.navBackToParent(pl, gui, row, () -> open(pl, returnPage));
        });
    }

    /** Plain confirm title and sentence. Never shows the raw action id. */
    private static String[] confirmCopy(String action) {
        return switch (action) {
            case "mentor_invite" -> new String[] {
                    CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Invite apprentice"),
                    CnpcUltraStyle.BODY + "Invite " + CnpcUltraStyle.SUBTITLE + "this player to train under you."
            };
            case "apprentice_invite" -> new String[] {
                    CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Ask as apprentice"),
                    CnpcUltraStyle.BODY + "Ask " + CnpcUltraStyle.SUBTITLE + "this player to be your master."
            };
            case "mentor_release" -> new String[] {
                    CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Release apprentice"),
                    CnpcUltraStyle.BODY + "Release " + CnpcUltraStyle.SUBTITLE + "this apprentice. A 12-hour cooldown starts."
            };
            case "mentor" -> new String[] {
                    CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Leave mentor"),
                    CnpcUltraStyle.BODY + "Leave " + CnpcUltraStyle.SUBTITLE + "your master. A 12-hour cooldown starts."
            };
            case "mentor_accept" -> new String[] {
                    CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Accept mentor invite"),
                    CnpcUltraStyle.BODY + "Accept " + CnpcUltraStyle.SUBTITLE + "to form this training bond."
            };
            case "mentor_decline" -> new String[] {
                    CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Decline mentor invite"),
                    CnpcUltraStyle.BODY + "Decline " + CnpcUltraStyle.SUBTITLE + "to dismiss the invite."
            };
            case "dojo_challenge" -> new String[] {
                    CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Challenge rival dojo"),
                    CnpcUltraStyle.BODY + "Challenge " + CnpcUltraStyle.SUBTITLE + "this dojo to a war."
            };
            default -> new String[] {
                    CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Confirm action"),
                    CnpcUltraStyle.BODY + "Confirm " + CnpcUltraStyle.SUBTITLE + "to continue."
            };
        };
    }

    private static String dojoTab(String page) {
        if (page == null || page.isBlank()) {
            return null;
        }
        return switch (page) {
            case "dojo", "roster" -> "home";
            case "dojo_members" -> "members";
            case "dojo_hof" -> "hof";
            default -> null;
        };
    }

    private static void paintDojo(ServerPlayer player, ICustomGui gui, String tab) {
        String key = tab == null || tab.isBlank() ? "home" : tab;
        String label = switch (key) {
            case "members" -> "Members";
            case "hof" -> "Hall of fame";
            case "rankings" -> "Rankings";
            default -> "Home";
        };
        ServerPlayer who = subject(player);
        Map<String, String> ph = SparGuiApi.placeholders(who);
        String partner = ph.getOrDefault("partner", "");
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Dojo · " + label),
                CnpcUltraStyle.SUBTITLE + "Session " + CnpcUltraStyle.BODY
                        + ("true".equals(ph.get("session_active")) ? CnpcUltraStyle.CONFIRM + "in progress"
                        : CnpcUltraStyle.SUBTITLE + "none active")
                        + (partner == null || partner.isBlank() ? ""
                        : " " + CnpcUltraStyle.DIM + "· " + CnpcUltraStyle.SUBTITLE + "vs " + CnpcUltraStyle.BODY + partner));
        infoY = sparTabBar(player, gui, infoY, "dojo");
        if (!"true".equalsIgnoreCase(ph.get("system_enabled"))) {
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(CnpcGuiStyle.MSG_SPAR_OFF),
                    CnpcGuiStyle.INFO_INLINE_MAX));
            footer(player, gui, row + 8, null);
            return;
        }
        int row = CnpcGuiSupport.bodyBelowHeader(infoY);
        String[] ids = {"home", "members", "hof"};
        String[] names = {"Home", "Members", "Hall of fame"};
        CnpcGuiLayout.GridButton[] tabs = new CnpcGuiLayout.GridButton[ids.length];
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            String page = switch (id) {
                case "members" -> "dojo_members";
                case "hof" -> "dojo_hof";
                default -> "dojo";
            };
            String button = id.equals(key) ? CnpcUltraStyle.ACCENT + names[i] : CnpcUltraStyle.SUBTITLE + names[i];
            tabs[i] = CnpcGuiLayout.GridButton.run(button, () -> open(player, page));
        }
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(player, gui, row, CnpcGuiSupport.ID_GRID_BASE, tabs, () -> {});
        switch (key) {
            case "members" -> row = CnpcGuiSupport.paintLongReadOnlyBody(gui, row, SparGuiApi.dojoMemberLines(who));
            case "hof" -> row = CnpcGuiSupport.paintLongReadOnlyBody(gui, row, SparGuiApi.linesForPage(who, "dojo_hof"));
            default -> {
                row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, row,
                        SparGuiApi.linesForPage(who, "dojo"), CnpcGuiStyle.INFO_INLINE_MAX));
                row = CnpcGuiSupport.paintSectionTag(gui, CnpcGuiSupport.ID_INLINE_NOTE, row,
                        CnpcUltraStyle.ACCENT + "Dojo home");
                CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.INFO + "Stats", CnpcGuiSupport.COL_L, row,
                        () -> open(player, "stats"));
                CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.ACCENT + "Training bonds", CnpcGuiSupport.COL_R, row,
                        () -> open(player, "mentor"));
                row += CnpcGuiSupport.ROW_STEP;
                CnpcGuiSupport.button(gui, 22, CnpcUltraStyle.SUBTITLE + "Chat settings", CnpcGuiSupport.COL_L, row,
                        () -> open(player, "settings"));
                if (StaffAccess.isStaff(player)) {
                    CnpcGuiSupport.buttonSmall(gui, CnpcGuiSupport.ID_STAFF_EXTRA, CnpcUltraStyle.DANGER + "Staff Admin",
                            CnpcGuiSupport.COL_R, row, CnpcGuiSupport.BTN_W, () -> open(player, "admin"));
                }
                row += CnpcGuiSupport.ROW_STEP;
            }
        }
        footer(player, gui, row, "home".equals(key) ? null : "dojo");
    }

    private static void paintChatSettings(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        Map<String, String> ph = SparGuiApi.placeholders(who);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Chat settings"),
                CnpcUltraStyle.SUBTITLE + "Training point messages");
        infoY = sparTabBar(player, gui, infoY, "dojo");
        int row = CnpcGuiSupport.bodyBelowHeader(infoY);
        boolean tpOn = "true".equalsIgnoreCase(ph.get("tpMsg"));
        CnpcGuiSupport.button(gui, 24, tpOn ? CnpcGuiStyle.toggleOn("TP")
                : CnpcGuiStyle.toggleOff("TP"), CnpcGuiSupport.COL_L, row,
                () -> act(player, "tpmsg", "toggle", "settings"));
        boolean mentorTpOn = "true".equalsIgnoreCase(ph.get("mentorTpMsg"));
        CnpcGuiSupport.button(gui, 25, mentorTpOn ? CnpcGuiStyle.toggleOn("Mentor TP")
                : CnpcGuiStyle.toggleOff("Mentor TP"), CnpcGuiSupport.COL_R, row,
                () -> act(player, "mentor_tpmsg", "toggle", "settings"));
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, "dojo");
    }

    private static void paintDojoWar(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Dojo war"),
                CnpcUltraStyle.SUBTITLE + "Challenge rival dojos");
        infoY = sparTabBar(player, gui, infoY, "wars");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, SparGuiApi.dojoWarLines(who), CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.INFO + "Challenge rival dojo…", CnpcGuiSupport.COL_L, row, () -> open(player, "pick_dojo_challenge"));
        int warPending = SparGuiApi.pendingDojoWarCards(who).size();
        String warPendingLabel = warPending > 0 ? CnpcUltraStyle.INFO + "War pending " + CnpcUltraStyle.BODY + "(" + warPending + ")" : CnpcUltraStyle.ACCENT + "War pending";
        CnpcGuiSupport.button(gui, 21, warPendingLabel, CnpcGuiSupport.COL_R, row, () -> open(player, "dojo_war_pending"));
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, null);
    }

    private static void paintDojoWarPending(ServerPlayer player, ICustomGui gui) {
        ServerPlayer who = subject(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Dojo war pending"),
                CnpcGuiStyle.HINT_CLICK_ENTRY);
        infoY = sparTabBar(player, gui, infoY, "wars");
        List<String> cards = SparGuiApi.pendingDojoWarCards(who);
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (cards.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    CnpcUltraStyle.SUBTITLE + "No dojo war challenges waiting — declare war from Dojo when your roster is ready.",
                    CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, "dojo_war");
            return;
        }
        int rowsBelow = 2;
        String[] warLabels = CnpcGuiSupport.cardLabels(cards, 1);
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, warLabels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, warLabels);
        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.selectionButton(player, gui, 30, CnpcUltraStyle.INFO + "Open", CnpcGuiSupport.COL_L, row,
                CnpcGuiSupport.BTN_W, () -> CnpcGuiSupport.cardField(cards, scroll, 0),
                arg -> open(player, "dojo_war_pending_decide:" + arg),
                () -> open(player, "dojo_war_pending"));
        footer(player, gui, row + CnpcGuiSupport.ROW_STEP, "dojo_war");
    }

    private static void openDojoWarDecide(ServerPlayer player, String pickerArg) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SPAR, CnpcGuiSupport.W, CnpcGuiSupport.window(300), (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Dojo war request"),
                    CnpcUltraStyle.BODY + CnpcGuiSupport.humanizePickerArg(pickerArg));
            infoY = sparTabBar(pl, gui, infoY, "wars");
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                    CnpcUltraStyle.BODY + "Accept " + CnpcUltraStyle.SUBTITLE + "to start the war (2× RP vs that dojo).",
                    CnpcUltraStyle.BODY + "Cancel " + CnpcUltraStyle.SUBTITLE + "to drop this war challenge."), 3));
            row += 8;
            CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Accept", CnpcGuiSupport.COL_L, row,
                    () -> act(pl, "dojo_accept", pickerArg, "dojo_war_pending"));
            CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.DANGER + "Cancel challenge", CnpcGuiSupport.COL_R, row,
                    () -> act(pl, "dojo_war_cancel", pickerArg, "dojo_war_pending"));
            row += CnpcGuiSupport.ROW_STEP;
            CnpcGuiSupport.navBackToParent(pl, gui, row, () -> open(pl, "dojo_war_pending"));
        });
    }

    private static void paintTop(ServerPlayer player, ICustomGui gui, String category) {
        String cat = category == null || category.isBlank() ? "tp" : category;
        // Wins and win streak are not stored on the spar leaderboard. Those tabs used to show Training Points.
        if ("wins".equalsIgnoreCase(cat) || "win".equalsIgnoreCase(cat)
                || "streak".equalsIgnoreCase(cat)) {
            cat = "tp";
        }
        boolean dojoBoard = "rp".equalsIgnoreCase(cat) || "reputation".equalsIgnoreCase(cat)
                || "wars".equalsIgnoreCase(cat) || "war".equalsIgnoreCase(cat);
        if ("reputation".equalsIgnoreCase(cat)) {
            cat = "rp";
        } else if ("war".equalsIgnoreCase(cat)) {
            cat = "wars";
        }
        List<String> lines = dojoBoard
                ? SparGuiApi.dojoTopLines(subject(player), cat)
                : SparGuiApi.topLines(subject(player), cat);
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Leaderboard · " + CnpcGuiStyle.sparLeaderboardTab(cat)),
                CnpcUltraStyle.SUBTITLE + "Top sparring players");
        infoY = sparTabBar(player, gui, infoY, "rankings");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines,
                CnpcGuiStyle.INFO_INLINE_MAX));
        String shown = cat;
        CnpcGuiLayout.GridButton[] tabs = new CnpcGuiLayout.GridButton[] {
                leaderboardTab(player, boardLabel(shown, "tp", "Training points"), "top_tp"),
                leaderboardTab(player, boardLabel(shown, "sessions", "Sessions"), "top_sessions"),
                leaderboardTab(player, boardLabel(shown, "perfect", "Perfect spars"), "top_perfect"),
                leaderboardTab(player, boardLabel(shown, "combo", "Combo"), "top_combo"),
                leaderboardTab(player, boardLabel(shown, "time", "Time"), "top_time"),
                leaderboardTab(player, boardLabel(shown, "rp", "Reputation"), "dojo_top_rp"),
                leaderboardTab(player, boardLabel(shown, "wars", "War wins"), "dojo_top_wars"),
        };
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(player, gui, row, CnpcGuiSupport.ID_GRID_BASE, tabs,
                () -> {});
        footer(player, gui, row, null);
    }

    private static void paintScroll(ServerPlayer player, ICustomGui gui, String title, List<String> body, String back) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, CnpcGuiStyle.HINT_READ_ONLY);
        infoY = sparTabBar(player, gui, infoY, sparTabId(back));
        int row = CnpcGuiSupport.paintLongReadOnlyBody(gui, infoY, body);
        footer(player, gui, row + 8, back);
    }

    private static void paintOnlinePick(
            ServerPlayer player, ICustomGui gui, String title, String action, String page, String back) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, CnpcGuiStyle.HINT_CLICK_PLAYER);
        infoY = sparTabBar(player, gui, infoY, sparTabId(back));
        List<String> names = RivalGuiApi.onlinePlayerNames(subject(player));
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (names.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER, CnpcUltraStyle.SUBTITLE + "Nobody else is online right now — try again when other players are on.", CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, back);
            return;
        }
        int rowsBelow = 2;
        String[] nameItems = names.toArray(String[]::new);
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, nameItems.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, nameItems);
        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.selectionButton(player, gui, 30, CnpcUltraStyle.INFO + "Choose", CnpcGuiSupport.COL_L, row,
                CnpcGuiSupport.BTN_W, () -> CnpcGuiSupport.selectedLine(scroll, nameItems),
                name -> open(player, "pick_confirm:" + action + "|" + back + "|" + name),
                () -> open(player, page));
        footer(player, gui, row + CnpcGuiSupport.ROW_STEP, back);
    }

    private static void paintMentorArgPick(
            ServerPlayer player, ICustomGui gui, String title, String action, String page, String back) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, title, CnpcGuiStyle.HINT_CLICK_ENTRY);
        infoY = sparTabBar(player, gui, infoY, sparTabId(back));
        List<String> args = SparGuiApi.pendingIncomingMentorArgs(subject(player));
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (args == null || args.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    CnpcUltraStyle.SUBTITLE + "No bond invites waiting. Send one from Training bonds, or wait for someone to invite you.",
                    CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, back);
            return;
        }
        int footerRow = paintArgScroll(player, gui, listY, args, page,
                arg -> open(player, "pick_confirm:" + action + "|" + back + "|" + arg));
        footer(player, gui, footerRow, back);
    }

    private static void paintReleasePick(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Release apprentice"),
                CnpcGuiStyle.HINT_CLICK_ENTRY);
        infoY = sparTabBar(player, gui, infoY, "dojo");
        List<String> cards = SparGuiApi.apprenticeCards(subject(player));
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (cards.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    CnpcUltraStyle.SUBTITLE + "No apprentices to release. Take an apprentice from Training bonds first.",
                    CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, "mentor");
            return;
        }
        int rowsBelow = 2;
        String[] releaseLabels = CnpcGuiSupport.cardLabels(cards, 1);
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, releaseLabels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, releaseLabels);
        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.selectionButton(player, gui, 30, CnpcUltraStyle.INFO + "Choose", CnpcGuiSupport.COL_L, row,
                CnpcGuiSupport.BTN_W, () -> CnpcGuiSupport.cardField(cards, scroll, 0),
                arg -> open(player, "pick_confirm:mentor_release|mentor|" + arg),
                () -> open(player, "pick_release"));
        footer(player, gui, row + CnpcGuiSupport.ROW_STEP, "mentor");
    }

    private static void paintDojoChallengePick(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Challenge rival dojo"),
                CnpcGuiStyle.HINT_REVIEW_DOJO);
        infoY = sparTabBar(player, gui, infoY, "wars");
        List<String> cards = SparGuiApi.rivalDojoCards(subject(player));
        int listY = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (cards.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    CnpcUltraStyle.SUBTITLE + "No rival dojo masters are online — challenge when one is on.",
                    CnpcGuiSupport.M, listY + 4, CnpcGuiSupport.textBandWidth(), 14);
            footer(player, gui, listY + 28, "dojo_war");
            return;
        }
        int rowsBelow = 2;
        String[] dojoLabels = CnpcGuiSupport.cardLabels(cards, 1);
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, dojoLabels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, dojoLabels);
        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.selectionButton(player, gui, 30, CnpcUltraStyle.INFO + "Choose", CnpcGuiSupport.COL_L, row,
                CnpcGuiSupport.BTN_W, () -> CnpcGuiSupport.cardField(cards, scroll, 0),
                arg -> open(player, "pick_confirm:dojo_challenge|dojo_war|" + arg),
                () -> open(player, "pick_dojo_challenge"));
        footer(player, gui, row + CnpcGuiSupport.ROW_STEP, "dojo_war");
    }

    /** @return Y row for footer nav after the list */
    private static int paintArgScroll(
            ServerPlayer player,
            ICustomGui gui,
            int listY,
            List<String> args,
            String page,
            java.util.function.Consumer<String> onPick
    ) {
        if (args == null || args.isEmpty()) {
            return listY + 8;
        }
        int rowsBelow = 2;
        String[] labels = CnpcGuiSupport.cardLabels(args, 0);
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.selectionButton(player, gui, 30, CnpcUltraStyle.INFO + "Choose", CnpcGuiSupport.COL_L, row,
                CnpcGuiSupport.BTN_W, () -> {
                    int[] sel = scroll.getSelection();
                    if (sel == null || sel.length == 0 || sel[0] < 0 || sel[0] >= args.size()) {
                        return null;
                    }
                    return args.get(sel[0]);
                },
                onPick,
                () -> open(player, page));
        return row + CnpcGuiSupport.ROW_STEP;
    }

    private static void paintAdmin(ServerPlayer player, ICustomGui gui) {
        if (!StaffAccess.isStaff(player)) {
            open(player, "dojo");
            return;
        }
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Sparring", "Staff Admin"),
                CnpcUltraStyle.SUBTITLE + "Save spar data and print status");
        infoY = sparTabBar(player, gui, infoY, "dojo");
        int row = CnpcGuiSupport.bodyBelowHeader(infoY);
        CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Save", CnpcGuiSupport.COL_L, row, () -> act(player, "admin", "save", "admin"));
        CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.SUBTITLE + "Status", CnpcGuiSupport.COL_R, row, () -> act(player, "admin", "status", "admin"));
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, "dojo");
    }

    private static String boardLabel(String current, String id, String name) {
        return (id.equalsIgnoreCase(current) ? CnpcUltraStyle.ACCENT : CnpcUltraStyle.SUBTITLE) + name;
    }

    /** Board switches acknowledge in the notice band, same as other menu actions. */
    private static CnpcGuiLayout.GridButton leaderboardTab(ServerPlayer player, String label, String page) {
        String plain = label.replaceAll(CnpcUltraStyle.MARK + ".", "");
        return CnpcGuiLayout.GridButton.action(label,
                () -> CnpcUltraStyle.SUBTITLE + "Showing " + plain + ".",
                () -> open(player, page));
    }

    /** {@code parentPage} null on spar main; otherwise Back reopens that page. Main always → LM hub. */
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
