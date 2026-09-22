package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.Zrips.CMILib.GUI.CMIGui;
import net.Zrips.CMILib.GUI.CMIGuiButton;
import net.Zrips.CMILib.GUI.GUIManager.InvType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * CMILib inventory GUI — Legacy Mechanics Rival.
 * Pages: main · list · challenge · progress · pick_* · stats · top · season · quests · achs · hof · journal · title.
 */
public final class CmiRivalGui {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private CmiRivalGui() {}

    public static boolean available() {
        return CmiDifficultyGui.available();
    }

    public static boolean open(Player player, String page) {
        if (player == null || !available()) {
            return false;
        }
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String p = raw.toLowerCase(Locale.ROOT);
        try {
            if (p.startsWith("challenge_time:")) {
                openChallengeTime(player, raw.substring("challenge_time:".length()).trim());
                return true;
            }
            if (p.startsWith("pending_decide:")) {
                openPendingDecide(player, raw.substring("pending_decide:".length()).trim());
                return true;
            }
            if (p.startsWith("challenge_decide:")) {
                openChallengeDecide(player, raw.substring("challenge_decide:".length()).trim());
                return true;
            }
            if (p.startsWith("list_detail:")) {
                openListDetail(player, raw.substring("list_detail:".length()).trim());
                return true;
            }
            switch (p) {
                case "list" -> openList(player);
                case "actions" -> openActions(player);
                case "pending", "invites", "pendinginvites" -> openPending(player);
                case "history", "past", "previous" -> openHistory(player);
                case "pick_declare" -> openPicker(player, "declare", "actions",
                        "&6Declare Rival", "&7Click to declare this player");
                case "pick_accept", "pick_decline" -> openPending(player);
                case "pick_remove" -> openList(player);
                case "pick_replace_mutual", "replace_mutual" -> openMutualReplacePicker(player);
                case "pick_challenge" -> openChallengeTargetPicker(player);
                case "pick_spectate" -> openPicker(player, "spectate", "challenge",
                        "&bSpectate", "&7Watch their active challenge");
                case "pick_silent" -> openPicker(player, "silent", "actions",
                        "&8Silent Rival", "&7Click for silent rivalry");
                case "stats", "statistics" -> openDetail(player, "stats", "&eRival Stats", Material.BOOK, "progress");
                case "challenge", "challenges" -> openChallenge(player);
                case "challenge_pending", "challenge_requests" -> openChallengePending(player);
                case "top", "leaderboard" -> openTop(player);
                case "progress" -> openProgress(player);
                case "records", "more" -> openRecords(player);
                case "season" -> openDetail(player, "season", "&aSeason", Material.CLOCK, "progress");
                case "quests", "quest" -> openDetail(player, "quests", "&bQuests", Material.WRITABLE_BOOK, "progress");
                case "achievements", "achs", "ach" ->
                        openDetail(player, "achievements", "&dAchievements", Material.DIAMOND, "records");
                case "hof", "hall" -> openDetail(player, "hof", "&6Hall of Fame", Material.GOLD_BLOCK, "records");
                case "journal" -> openDetail(player, "journal", "&fJournal", Material.MAP, "records");
                case "title", "titles" -> openDetail(player, "title", "&eTitle", Material.NAME_TAG, "records");
                case "help" -> openMain(player);
                case "admin" -> {
                    if (ForgeBridge.isStaff(player)) {
                        openAdmin(player);
                    } else {
                        openMain(player);
                    }
                }
                default -> openMain(player);
            }
            return true;
        } catch (Throwable t) {
            player.sendMessage("§cRival CMI GUI failed: " + t.getMessage());
            return false;
        }
    }

    private static void openMain(Player player) {
        Map<String, String> ph = ForgeBridge.rivalPlaceholders(player);
        CMIGui gui = base(player, "&8Rival", 5);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        CMIGuiButton status = new CMIGuiButton(4, Material.NAME_TAG,
                !bridgeOk ? "&c&lUNAVAILABLE"
                        : !systemOn ? "&c&lRIVAL DISABLED"
                        : "&6&lRival");
        status.lockField();
        if (!bridgeOk || !systemOn) {
            status.addLore(unavailableLore(bridgeOk));
            gui.addButton(status);
            gui.addButton(hubBtn(40));
            gui.addButton(closeBtn(44));
            fillEmpty(gui, 5);
            GuiFeedback.openCmi(gui);
            return;
        }
        status.addLore(statusLore(player, ph));
        gui.addButton(status);

        // Main: List · Actions · Challenge · Top · History · Progress · toggles
        gui.addButton(pageBtn(player, 19, "rival.main.list", Material.PLAYER_HEAD, "&6List", "list",
                "&7Current rivals", "&8Heads · hover for stats"));
        int pendingCount = 0;
        try {
            pendingCount = Integer.parseInt(ph.getOrDefault("pending_invites", "0"));
        } catch (NumberFormatException ignored) {
            pendingCount = 0;
        }
        String pendingLine = pendingCount > 0
                ? "&e" + pendingCount + " pending invite" + (pendingCount == 1 ? "" : "s")
                : "&8Pending invites live here";
        gui.addButton(pageBtn(player, 21, "rival.main.actions", Material.EMERALD, "&aActions", "actions",
                Map.of("pending", pendingLine),
                "&7Declare · Pending · Remove · Silent", pendingLine));
        gui.addButton(pageBtn(player, 23, "rival.main.challenge", Material.DIAMOND_SWORD, "&cChallenge", "challenge",
                "&7Send · accept · decline · spectate"));
        gui.addButton(pageBtn(player, 25, "rival.main.top", Material.GOLDEN_HELMET, "&fTop", "top",
                "&7RP leaderboard"));
        gui.addButton(pageBtn(player, 29, "rival.main.history", Material.SKELETON_SKULL, "&8History", "history",
                "&7Previous rivals", "&8Archived when removed"));
        gui.addButton(pageBtn(player, 31, "rival.main.progress", Material.BOOK, "&bProgress", "progress",
                "&7Stats · season · quests"));

        boolean tpOn = "true".equalsIgnoreCase(ph.getOrDefault("tpMsg", "false"));
        gui.addButton(actionBtn(player, 33,
                tpOn ? "rival.main.tpmsg_on" : "rival.main.tpmsg_off",
                tpOn ? Material.BELL : Material.GRAY_DYE,
                tpOn ? "&aTP Msg ON" : "&8TP Msg OFF",
                "tpmsg", "toggle", "main",
                List.of(
                        tpOn ? "&8Hides rival TP chat messages" : "&8Shows rival TP chat messages again",
                        "&8Only affects rivalry TP chat"
                )));

        boolean instinctFeature = "true".equalsIgnoreCase(ph.getOrDefault("instinct_feature", "false"));
        if (instinctFeature) {
            boolean instinctOn = "true".equalsIgnoreCase(ph.getOrDefault("instinct", "false"));
            gui.addButton(actionBtn(player, 34,
                    instinctOn ? "rival.main.instinct_on" : "rival.main.instinct_off",
                    instinctOn ? Material.LIME_DYE : Material.GRAY_DYE,
                    instinctOn ? "&aInstinct ON" : "&8Instinct OFF",
                    "instinct", "toggle", "main",
                    List.of(
                            instinctOn ? "&8Turns rival proximity alerts off" : "&8Turns rival proximity alerts on",
                            "&8Alerts for mutual / nemesis rivals"
                    )));
        }

        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        if (ForgeBridge.isStaff(player)) {
            gui.addButton(pageBtn(player, 37, "rival.main.admin", Material.COMMAND_BLOCK, "&cAdmin", "admin",
                    "&7Save · refresh · status"));
        }
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openProgress(Player player) {
        CMIGui gui = base(player, "&8Rival Progress", 3);
        CMIGuiButton info = new CMIGuiButton(4, Material.BOOK, "&b&lProgress");
        info.lockField();
        info.addLore(List.of("", "&7Pick a page"));
        gui.addButton(info);

        gui.addButton(pageBtn(player, 11, "rival.progress.stats", Material.BOOK, "&eStats", "stats",
                "&7Wins · RP · rivals"));
        gui.addButton(pageBtn(player, 12, "rival.progress.season", Material.CLOCK, "&aSeason", "season",
                "&7Season RP · top 5"));
        gui.addButton(pageBtn(player, 13, "rival.progress.quests", Material.WRITABLE_BOOK, "&bQuests", "quests",
                "&7Weekly goals"));
        gui.addButton(pageBtn(player, 15, "rival.progress.more", Material.CHEST, "&6More", "records",
                "&7Title · achs · HOF · journal"));

        gui.addButton(pageBtn(player, 18, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(22));
        gui.addButton(closeBtn(26));
        fillEmpty(gui, 3);
        GuiFeedback.openCmi(gui);
    }

    private static void openRecords(Player player) {
        CMIGui gui = base(player, "&8Rival Records", 3);
        CMIGuiButton info = new CMIGuiButton(4, Material.CHEST, "&6&lRecords");
        info.lockField();
        info.addLore(List.of("", "&7Pick a page"));
        gui.addButton(info);
        gui.addButton(pageBtn(player, 11, "rival.progress.title", Material.NAME_TAG, "&eTitle", "title",
                "&7RP tier · perk"));
        gui.addButton(pageBtn(player, 12, "rival.progress.achs", Material.DIAMOND, "&dAchs", "achievements",
                "&7Unlocked achievements"));
        gui.addButton(pageBtn(player, 14, "rival.progress.hof", Material.GOLD_BLOCK, "&6HOF", "hof",
                "&7Hall of Fame"));
        gui.addButton(pageBtn(player, 15, "rival.progress.journal", Material.MAP, "&fJournal", "journal",
                "&7Recent battles"));
        gui.addButton(pageBtn(player, 18, "common.back", Material.ARROW, "&7Back", "progress", "&7Progress"));
        gui.addButton(hubBtn(22));
        gui.addButton(closeBtn(26));
        fillEmpty(gui, 3);
        GuiFeedback.openCmi(gui);
    }

    private static void openTop(Player player) {
        CMIGui gui = base(player, "&8Rival", 5);
        List<String> raw = toAmp(ForgeBridge.rivalLines(player, "top"));
        List<GuiBoardHelper.TopEntry> entries = GuiBoardHelper.parseTopEntries(raw);
        CMIGuiButton header = new CMIGuiButton(4, Material.GOLDEN_HELMET, "&fRP Top");
        header.lockField();
        List<String> topHeader = new ArrayList<>();
        topHeader.add("");
        topHeader.addAll(GuiBoardHelper.tips(player, "&7Top rivals by RP", "&8Player heads below"));
        header.addLore(topHeader);
        gui.addButton(header);
        if (entries.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("rival.empty.no_top", "&7No rivalry data yet"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("rival.empty.no_top",
                    GuiBoardHelper.tipsList(player, List.of("&7Challenge rivals to earn RP"))));
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(entries.size(), 21));
            for (int i = 0; i < slots.length && i < entries.size(); i++) {
                ItemStack head = GuiBoardHelper.topHead(entries.get(i));
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.lockField();
                gui.addButton(btn);
            }
        }
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openDetail(Player player, String page, String title, Material mat, String backPage) {
        CMIGui gui = base(player, "&8Rival", 3);
        String back = backPage == null || backPage.isBlank() ? "progress" : backPage;
        List<String> lore = toAmp(ForgeBridge.rivalLines(player, page));
        if (lore.isEmpty()) {
            lore = List.of("&7Nothing here yet.");
        }
        List<String> body = new ArrayList<>();
        body.add("");
        int shown = 0;
        for (String line : lore) {
            if (line == null) {
                continue;
            }
            String plain = GuiBoardHelper.strip(line).trim();
            if (plain.isEmpty() || plain.startsWith("---") || plain.startsWith("──")) {
                continue;
            }
            body.add(line);
            shown++;
            if (shown >= 18) {
                body.add("&8…");
                break;
            }
        }
        CMIGuiButton summary = new CMIGuiButton(13, mat, title);
        summary.lockField();
        summary.addLore(body);
        gui.addButton(summary);
        gui.addButton(pageBtn(player, 18, "common.back", Material.ARROW, "&7Back", back, "&7Return"));
        gui.addButton(hubBtn(22));
        gui.addButton(closeBtn(26));
        fillEmpty(gui, 3);
        GuiFeedback.openCmi(gui);
    }

    private static void openAdmin(Player player) {
        CMIGui gui = base(player, "&8Rival Admin", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.COMMAND_BLOCK, "&c&lRival Admin");
        info.lockField();
        info.addLore(List.of("", "&7Staff-only tools",
                "&8Save · refresh · status",
                "&8Player menus stay on the main Rival GUI"));
        gui.addButton(info);
        gui.addButton(actionBtn(player, 20, "rival.admin.save", Material.WRITABLE_BOOK, "&aSave",
                "admin", "save", "admin",
                List.of("&7Save rivalry and progress data to disk", "&8/rival admin save")));
        gui.addButton(actionBtn(player, 22, "rival.admin.refresh", Material.CLOCK, "&eRefresh",
                "admin", "refresh", "admin",
                List.of("&7Reload stores from disk", "&8/rival admin refresh")));
        gui.addButton(actionBtn(player, 24, "rival.admin.status", Material.SPYGLASS, "&bStatus",
                "admin", "status", "admin",
                List.of("&7Enabled + path summary", "&8/rival admin status")));
        gui.addButton(pageBtn(player, 36, "rival.admin.back", Material.ARROW, "&7Back", "main",
                "&7Player Rival menu"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static List<String> previewLines(List<String> lines, int max) {
        List<String> out = new ArrayList<>();
        if (lines == null) {
            out.add("&7…");
            return out;
        }
        int n = 0;
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String plain = GuiBoardHelper.strip(line).trim();
            if (plain.startsWith("---") || plain.isEmpty()) {
                continue;
            }
            if (n == 0 && (plain.toLowerCase(Locale.ROOT).contains("stats")
                    || plain.toLowerCase(Locale.ROOT).contains("season")
                    || plain.toLowerCase(Locale.ROOT).contains("quest")
                    || plain.toLowerCase(Locale.ROOT).contains("achievement")
                    || plain.toLowerCase(Locale.ROOT).contains("hall")
                    || plain.toLowerCase(Locale.ROOT).contains("journal")
                    || plain.toLowerCase(Locale.ROOT).contains("title"))) {
                continue;
            }
            out.add(line.replace('§', '&'));
            n++;
            if (n >= max) {
                break;
            }
        }
        if (out.isEmpty()) {
            out.add("&7Open for details");
        }
        return out;
    }

    private static void openList(Player player) {
        CMIGui gui = base(player, "&8Rival List", 5);
        List<GuiBoardHelper.RivalCard> cards = GuiBoardHelper.parseRivalCards(
                ForgeBridge.rivalCurrentCards(player));
        CMIGuiButton info = new CMIGuiButton(4, Material.PLAYER_HEAD, "&6&lCurrent Rivals");
        info.lockField();
        List<String> listHeader = new ArrayList<>();
        listHeader.add("");
        listHeader.add(cards.isEmpty() ? "&7No rivals yet." : "&7" + cards.size() + " rival(s)");
        listHeader.addAll(GuiBoardHelper.tips(player,
                "&7Tap a head for profile and remove",
                "&8Declare and pending live in Actions"));
        info.addLore(listHeader);
        gui.addButton(info);

        if (cards.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("rival.empty.no_rivals", "&7No rivals yet"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("rival.empty.no_rivals",
                    GuiBoardHelper.tipsList(player, List.of("&7Use Actions → Declare to start"))));
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(cards.size(), 21));
            for (int i = 0; i < slots.length && i < cards.size(); i++) {
                GuiBoardHelper.RivalCard card = cards.get(i);
                ItemStack head = GuiBoardHelper.rivalHead(card);
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.lockField();
                btn.addCommand("lmdo rival page list_detail:" + card.pickerArg());
                gui.addButton(btn);
            }
        }

        gui.addButton(pageBtn(player, 37, "rival.list.nav_actions", Material.EMERALD, "&aActions", "actions",
                "&7Declare · pending · silent"));
        gui.addButton(pageBtn(player, 39, "rival.list.nav_history", Material.SKELETON_SKULL, "&8History", "history",
                "&7Previous rivals"));
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openPending(Player player) {
        CMIGui gui = base(player, "&8Pending Invites", 5);
        List<GuiBoardHelper.PendingInvite> invites = GuiBoardHelper.parsePendingInvites(
                ForgeBridge.rivalPendingInviteCards(player));
        CMIGuiButton info = new CMIGuiButton(4, Material.YELLOW_DYE, "&e&lPending Invites");
        info.lockField();
        List<String> pendingHeader = new ArrayList<>();
        pendingHeader.add("");
        pendingHeader.add(invites.isEmpty() ? "&7No pending declares." : "&7" + invites.size() + " pending");
        pendingHeader.addAll(GuiBoardHelper.tips(player,
                "&7Tap a name to respond",
                "&a◀ Incoming &7— Accept or Decline",
                "&6▶ Outgoing &7— Withdraw or keep waiting"));
        info.addLore(pendingHeader);
        gui.addButton(info);

        if (invites.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("rival.empty.no_pending", "&7No pending invites"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("rival.empty.no_pending", GuiBoardHelper.tipsList(player, List.of(
                    "&7Declare someone to send an invite",
                    "&7Incoming shows when they Declare you"))));
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(invites.size(), 21));
            for (int i = 0; i < slots.length && i < invites.size(); i++) {
                GuiBoardHelper.PendingInvite invite = invites.get(i);
                ItemStack head = GuiBoardHelper.pendingInviteHead(player, invite);
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.lockField();
                btn.addCommand("lmdo rival page pending_decide:" + invite.pickerArg());
                gui.addButton(btn);
            }
        }

        gui.addButton(pageBtn(player, 39, "rival.pending.nav_actions", Material.EMERALD, "&aActions", "actions",
                "&7Full actions menu"));
        gui.addButton(pageBtn(player, 36, "rival.pending.back", Material.ARROW, "&7Back", "actions", "&7Actions"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openPendingDecide(Player player, String arg) {
        CMIGui gui = base(player, "&8Pending Request", 5);
        GuiBoardHelper.PendingInvite invite = findPendingInvite(player, arg);
        String display = invite != null ? invite.name : (arg == null || arg.isBlank() ? "?" : arg.trim());
        if (display.regionMatches(true, 0, "uuid:", 0, 5)) {
            display = display.substring(5).trim();
        }
        String pickerArg = invite != null ? invite.pickerArg()
                : (arg == null || arg.isBlank() ? display : arg.trim());
        boolean mutualConfirm = invite != null && invite.isMutualConfirm();

        CMIGuiButton info = new CMIGuiButton(4, Material.YELLOW_DYE,
                GuiTooltips.name("rival.pending.decide_info",
                        mutualConfirm ? "&e&lMutual Confirm" : "&e&lRespond"));
        info.lockField();
        boolean outgoing = invite != null && !invite.incoming;
        if (outgoing) {
            info.addLore(GuiTooltips.buttonLore("rival.pending.decide_info", List.of(
                    "&7Waiting on &f" + display,
                    "&cWithdraw &7— cancel your declare",
                    "&7Keep waiting — return to the list")));
        } else if (mutualConfirm) {
            info.addLore(GuiTooltips.buttonLore("rival.pending.decide_info", List.of(
                    "&7Declared with &f" + display,
                    "&7You both Silent'd each other",
                    "&aAccept &7→ your side agrees (both needed)",
                    "&cDecline &7→ stay Declared, cancel confirm")));
        } else {
            info.addLore(GuiTooltips.buttonLore("rival.pending.decide_info", List.of(
                    "&7Incoming declare from &f" + display,
                    "&aAccept &7→ Mutual",
                    "&cDecline &7→ refuse")));
        }
        gui.addButton(info);

        ItemStack head = invite != null
                ? GuiBoardHelper.pendingInviteHead(player, invite)
                : new ItemStack(Material.PLAYER_HEAD);
        CMIGuiButton headBtn = new CMIGuiButton(13, head);
        headBtn.lockField();
        gui.addButton(headBtn);

        if (outgoing) {
            gui.addButton(actionBtn(player, 20, "rival.pending.withdraw", Material.ORANGE_DYE, "&cWithdraw declare",
                    "remove", pickerArg, "pending",
                    List.of("&7Cancel your declare to " + display)));
            gui.addButton(pageBtn(player, 24, "rival.pending.keep_waiting", Material.GRAY_DYE, "&7Keep waiting",
                    "pending", "&7Return to pending list"));
        } else if (mutualConfirm) {
            gui.addButton(actionBtn(player, 20, "rival.pending.accept", Material.LIME_DYE, "&aAccept Mutual",
                    "accept", pickerArg, "pending",
                    List.of("&7Accept Mutual with " + display,
                            "&8Both must Accept")));
            gui.addButton(actionBtn(player, 24, "rival.pending.decline", Material.ORANGE_DYE, "&cDecline Mutual",
                    "decline", pickerArg, "pending",
                    List.of("&7Decline Mutual with " + display,
                            "&8Stay Declared on both lists")));
        } else {
            gui.addButton(actionBtn(player, 20, "rival.pending.accept", Material.LIME_DYE, "&aAccept",
                    "accept", pickerArg, "pending",
                    List.of("&7Accept " + display + "'s declare",
                            "&8→ Mutual rivalry")));
            gui.addButton(actionBtn(player, 24, "rival.pending.decline", Material.ORANGE_DYE, "&cDecline",
                    "decline", pickerArg, "pending",
                    List.of("&7Decline " + display + "'s declare",
                            "&8They stay Declared on their list")));
        }
        gui.addButton(pageBtn(player, 36, "rival.pending.decide_back", Material.ARROW, "&7Back", "pending",
                "&7Pending invites"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openListDetail(Player player, String arg) {
        CMIGui gui = base(player, "&8Rival Profile", 5);
        GuiBoardHelper.RivalCard card = GuiBoardHelper.findCurrentRival(
                ForgeBridge.rivalCurrentCards(player), arg);
        String pickerArg = card != null ? card.pickerArg()
                : (arg == null || arg.isBlank() ? "?" : arg.trim());
        String display = card != null ? card.name : pickerArg;
        if (display.regionMatches(true, 0, "uuid:", 0, 5)) {
            display = display.substring(5).trim();
        }

        CMIGuiButton info = new CMIGuiButton(4, Material.PLAYER_HEAD,
                GuiTooltips.name("rival.list.profile_info", "&6&lRival profile"));
        info.lockField();
        info.addLore(GuiTooltips.buttonLore("rival.list.profile_info", List.of(
                "&7Stats on the head · &cRemove &7below",
                "&8Removed rivals move to History")));
        gui.addButton(info);

        ItemStack head = card != null
                ? GuiBoardHelper.rivalHead(card)
                : new ItemStack(Material.PLAYER_HEAD);
        CMIGuiButton headBtn = new CMIGuiButton(13, head);
        headBtn.lockField();
        gui.addButton(headBtn);

        gui.addButton(actionBtn(player, 20, "rival.list.remove_rival", Material.RED_DYE, "&cRemove rival",
                "remove", pickerArg, "list",
                List.of("&7Remove " + display + " from your list")));
        gui.addButton(pageBtn(player, 36, "rival.list.detail_back", Material.ARROW, "&7Back", "list",
                "&7Rival list"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static GuiBoardHelper.PendingChallenge findPendingChallenge(Player player, String arg) {
        if (arg == null || arg.isBlank()) {
            return null;
        }
        String raw = arg.trim();
        String uuid = "";
        String name = raw;
        if (raw.regionMatches(true, 0, "uuid:", 0, 5)) {
            uuid = raw.substring(5).trim();
            name = "";
        }
        for (GuiBoardHelper.PendingChallenge req : GuiBoardHelper.parsePendingChallenges(
                ForgeBridge.rivalPendingChallengeCards(player))) {
            if (!uuid.isBlank() && uuid.equalsIgnoreCase(req.uuid)) {
                return req;
            }
            if (!name.isBlank() && name.equalsIgnoreCase(req.name)) {
                return req;
            }
            if (raw.equalsIgnoreCase(req.pickerArg())) {
                return req;
            }
        }
        return null;
    }

    private static GuiBoardHelper.PendingInvite findPendingInvite(Player player, String arg) {
        if (arg == null || arg.isBlank()) {
            return null;
        }
        String raw = arg.trim();
        String uuid = "";
        String name = raw;
        if (raw.regionMatches(true, 0, "uuid:", 0, 5)) {
            uuid = raw.substring(5).trim();
            name = "";
        }
        for (GuiBoardHelper.PendingInvite invite : GuiBoardHelper.parsePendingInvites(
                ForgeBridge.rivalPendingInviteCards(player))) {
            if (!uuid.isBlank() && uuid.equalsIgnoreCase(invite.uuid)) {
                return invite;
            }
            if (!name.isBlank() && name.equalsIgnoreCase(invite.name)) {
                return invite;
            }
            if (raw.equalsIgnoreCase(invite.pickerArg())) {
                return invite;
            }
        }
        return null;
    }

    private static void openActions(Player player) {
        CMIGui gui = base(player, "&8Rival Actions", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.EMERALD, "&a&lRival Actions");
        info.lockField();
        info.addLore(toAmp(ForgeBridge.rivalLines(player, "actions")));
        gui.addButton(info);

        gui.addButton(pageBtn(player, 19, "rival.actions.declare", Material.LIME_DYE, "&aDeclare…", "pick_declare",
                "&7Shows on your list as Declared",
                "&7They get Pending → Accept → Mutual"));
        Map<String, String> ph = ForgeBridge.rivalPlaceholders(player);
        int pendingCount = 0;
        try {
            pendingCount = Integer.parseInt(ph.getOrDefault("pending_invites", "0"));
        } catch (NumberFormatException ignored) {
            pendingCount = 0;
        }
        gui.addButton(pageBtn(player, 21, "rival.actions.pending", Material.CLOCK,
                pendingCount > 0 ? "&ePending &f(" + pendingCount + ")" : "&ePending",
                "pending",
                "&7Tap a name on the Pending board",
                pendingCount > 0 ? "&aYou have pending invites" : "&8No pending invites"));
        gui.addButton(pageBtn(player, 23, "rival.actions.silent", Material.GRAY_DYE, "&8Silent…", "pick_silent",
                "&7One-sided Silent (they are not told)",
                "&8Both Silent → Declared"));

        gui.addButton(pageBtn(player, 37, "rival.actions.nav_list", Material.PLAYER_HEAD, "&6List", "list",
                "&7Back to current rivals"));
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openHistory(Player player) {
        CMIGui gui = base(player, "&8Rival History", 5);
        List<GuiBoardHelper.RivalCard> cards = GuiBoardHelper.parseRivalCards(
                ForgeBridge.rivalPastCards(player));
        CMIGuiButton info = new CMIGuiButton(4, Material.SKELETON_SKULL, "&8&lPrevious Rivals");
        info.lockField();
        List<String> historyHeader = new ArrayList<>();
        historyHeader.add("");
        historyHeader.add(cards.isEmpty() ? "&7No previous rivals yet." : "&7" + cards.size() + " archived");
        historyHeader.addAll(GuiBoardHelper.tips(player,
                "&8Removed rivalries appear here",
                "&8Hover a head for final stats"));
        info.addLore(historyHeader);
        gui.addButton(info);

        if (cards.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("rival.empty.no_history", "&7No history yet"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("rival.empty.no_history",
                    GuiBoardHelper.tipsList(player, List.of("&7Removed rivals show here"))));
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(cards.size(), 21));
            for (int i = 0; i < slots.length && i < cards.size(); i++) {
                ItemStack head = GuiBoardHelper.rivalHead(cards.get(i));
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.lockField();
                gui.addButton(btn);
            }
        }

        gui.addButton(pageBtn(player, 37, "rival.history.nav_list", Material.PLAYER_HEAD, "&6List", "list",
                "&7Current rivals"));
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openChallenge(Player player) {
        CMIGui gui = base(player, "&8Rival", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.IRON_SWORD, "&c&lChallenge");
        info.lockField();
        info.addLore(toAmp(ForgeBridge.rivalLines(player, "challenge")));
        gui.addButton(info);

        gui.addButton(pageBtn(player, 19, "rival.challenge.send", Material.GOLDEN_SWORD, "&eSend Challenge…",
                "pick_challenge", "&7Pick rival, then choose 1–10 minutes"));
        int pendingCh = GuiBoardHelper.parsePendingChallenges(ForgeBridge.rivalPendingChallengeCards(player)).size();
        gui.addButton(pageBtn(player, 21, "rival.challenge.pending", Material.CLOCK,
                pendingCh > 0 ? "&ePending &f(" + pendingCh + ")" : "&ePending Requests",
                "challenge_pending",
                "&7Tap a name — Accept, Decline, or Cancel"));
        gui.addButton(pageBtn(player, 29, "rival.challenge.spectate", Material.ENDER_EYE, "&bSpectate…",
                "pick_spectate", "&7Watch an online player's challenge"));
        gui.addButton(actionBtn(player, 31, "rival.challenge.spectate_stop", Material.GRAY_DYE, "&8Stop Spectate",
                "spectate_stop", "0", "challenge",
                List.of("&7End spectating early")));

        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openChallengePending(Player player) {
        CMIGui gui = base(player, "&8Pending Requests", 5);
        List<GuiBoardHelper.PendingChallenge> requests = GuiBoardHelper.parsePendingChallenges(
                ForgeBridge.rivalPendingChallengeCards(player));
        CMIGuiButton info = new CMIGuiButton(4, Material.IRON_SWORD, "&c&lPending Requests");
        info.lockField();
        List<String> pendingHeader = new ArrayList<>();
        pendingHeader.add("");
        pendingHeader.add(requests.isEmpty() ? "&7No pending challenge requests." : "&7" + requests.size() + " pending");
        pendingHeader.addAll(GuiBoardHelper.tips(player,
                "&7Tap a name to respond",
                "&c◀ Incoming &7— Accept or Decline",
                "&6▶ Outgoing &7— Cancel or keep waiting"));
        info.addLore(pendingHeader);
        gui.addButton(info);

        if (requests.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("rival.challenge.empty_pending", "&7No pending requests"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("rival.challenge.empty_pending", GuiBoardHelper.tipsList(player,
                    List.of("&7Send a challenge from the Challenge menu"))));
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(requests.size(), 21));
            for (int i = 0; i < slots.length && i < requests.size(); i++) {
                GuiBoardHelper.PendingChallenge req = requests.get(i);
                ItemStack head = GuiBoardHelper.pendingChallengeHead(player, req);
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.lockField();
                btn.addCommand("lmdo rival page challenge_decide:" + req.pickerArg());
                gui.addButton(btn);
            }
        }

        gui.addButton(pageBtn(player, 39, "rival.challenge.nav_challenge", Material.GOLDEN_SWORD, "&cChallenge",
                "challenge", "&7Send · spectate"));
        gui.addButton(pageBtn(player, 36, "rival.challenge.pending_back", Material.ARROW, "&7Back", "challenge",
                "&7Challenge menu"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openChallengeDecide(Player player, String arg) {
        CMIGui gui = base(player, "&8Challenge Request", 5);
        GuiBoardHelper.PendingChallenge req = findPendingChallenge(player, arg);
        String display = req != null ? req.name : (arg == null || arg.isBlank() ? "?" : arg.trim());
        if (display.regionMatches(true, 0, "uuid:", 0, 5)) {
            display = display.substring(5).trim();
        }
        String pickerArg = req != null ? req.pickerArg()
                : (arg == null || arg.isBlank() ? display : arg.trim());
        boolean outgoing = req != null && !req.incoming;

        CMIGuiButton info = new CMIGuiButton(4, Material.IRON_SWORD,
                GuiTooltips.name("rival.challenge.decide_info",
                        outgoing ? "&6&lOutgoing Challenge" : "&c&lIncoming Challenge"));
        info.lockField();
        if (outgoing) {
            info.addLore(GuiTooltips.buttonLore("rival.challenge.decide_info", List.of(
                    "&7Waiting on &f" + display,
                    "&cCancel &7— withdraw the request",
                    "&7Keep waiting — return to the list")));
        } else {
            info.addLore(GuiTooltips.buttonLore("rival.challenge.decide_info", List.of(
                    "&7Challenge from &f" + display,
                    "&7Length &f" + (req != null ? req.durationMin : "?") + " min",
                    "&aAccept &7starts countdown",
                    "&cDecline &7refuses")));
        }
        gui.addButton(info);

        ItemStack head = req != null
                ? GuiBoardHelper.pendingChallengeHead(player, req)
                : new ItemStack(Material.PLAYER_HEAD);
        CMIGuiButton headBtn = new CMIGuiButton(13, head);
        headBtn.lockField();
        gui.addButton(headBtn);

        if (outgoing) {
            gui.addButton(actionBtn(player, 20, "rival.challenge.cancel", Material.ORANGE_DYE, "&cCancel challenge",
                    "challenge_cancel", pickerArg, "challenge_pending",
                    List.of("&7Withdraw request to " + display)));
            gui.addButton(pageBtn(player, 24, "rival.challenge.keep_waiting", Material.GRAY_DYE, "&7Keep waiting",
                    "challenge_pending", "&7Return to pending list"));
        } else {
            gui.addButton(actionBtn(player, 20, "rival.challenge.accept", Material.LIME_DYE, "&aAccept",
                    "challenge_accept", pickerArg, "challenge_pending",
                    List.of("&7Accept duel vs " + display)));
            gui.addButton(actionBtn(player, 24, "rival.challenge.decline", Material.ORANGE_DYE, "&cDecline",
                    "challenge_decline", pickerArg, "challenge_pending",
                    List.of("&7Decline challenge from " + display)));
        }
        gui.addButton(pageBtn(player, 36, "rival.challenge.decide_back", Material.ARROW, "&7Back",
                "challenge_pending", "&7Pending requests"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openChallengeTargetPicker(Player player) {
        CMIGui gui = base(player, "&8Rival", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.GOLDEN_SWORD, "&cSend Challenge");
        info.lockField();
        List<String> challengePickHeader = new ArrayList<>();
        challengePickHeader.add("");
        challengePickHeader.add("&7Online rivals");
        challengePickHeader.addAll(GuiBoardHelper.tips(player, "&8Click a head, then pick duration"));
        info.addLore(challengePickHeader);
        gui.addButton(info);

        List<Player> online = GuiPlayerPicker.onlineExcept(player);
        int placed = 0;
        for (Player other : online) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            ItemStack head = GuiPlayerPicker.head(other, "&f" + other.getName(),
                    pickerTip(player, "&7Next: choose fight length", "&8(1–10 minutes)"));
            CMIGuiButton btn = new CMIGuiButton(slot, head);
            btn.lockField();
            btn.addCommand("lmdo rival page challenge_time:uuid:" + other.getUniqueId());
            gui.addButton(btn);
        }
        if (placed == 0) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("rival.empty.no_online", "&cNo one online"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("rival.empty.no_online",
                    GuiBoardHelper.tipsList(player, List.of("&7Other players must be online"))));
            gui.addButton(empty);
        }

        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "challenge", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openChallengeTime(Player player, String targetArg) {
        CMIGui gui = base(player, "&8Rival", 5);

        String display = targetArg;
        ItemStack head;
        if (targetArg.regionMatches(true, 0, "uuid:", 0, 5)) {
            try {
                java.util.UUID id = java.util.UUID.fromString(targetArg.substring(5).trim());
                Player online = org.bukkit.Bukkit.getPlayer(id);
                display = online != null ? online.getName() : targetArg.substring(5).trim();
                head = online != null
                        ? GuiPlayerPicker.head(online, "&f" + display,
                                pickerTip(player, "&7Choose fight length", "&81–10 minutes"))
                        : GuiPlayerPicker.headByName(display, "&f" + display,
                                pickerTip(player, "&cPlayer offline", "&7Pick someone else"));
            } catch (IllegalArgumentException e) {
                head = GuiPlayerPicker.headByName(display, "&f" + display,
                        pickerTip(player, "&7Choose minutes"));
            }
        } else {
            Player online = org.bukkit.Bukkit.getPlayerExact(targetArg);
            display = online != null ? online.getName() : targetArg;
            head = online != null
                    ? GuiPlayerPicker.head(online, "&f" + display,
                            pickerTip(player, "&7Choose fight length"))
                    : GuiPlayerPicker.headByName(display, "&f" + display,
                            pickerTip(player, "&7Choose minutes"));
        }
        CMIGuiButton info = new CMIGuiButton(4, head);
        info.lockField();
        gui.addButton(info);

        int[] slots = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};
        for (int i = 0; i < slots.length; i++) {
            int minutes = i + 1;
            ItemStack clock = new ItemStack(Material.CLOCK, minutes);
            org.bukkit.inventory.meta.ItemMeta meta = clock.getItemMeta();
            if (meta != null) {
                Map<String, String> durVars = Map.of("minutes", String.valueOf(minutes));
                String fallback = "&e" + minutes + " minute" + (minutes == 1 ? "" : "s");
                meta.setDisplayName(color(GuiTooltips.name(
                        "rival.challenge.duration", fallback, durVars)));
                List<String> clockLore = new ArrayList<>();
                clockLore.add(color(""));
                clockLore.add(color("&7Challenge &f" + display));
                for (String tip : GuiTooltips.lore("rival.challenge.duration",
                        GuiBoardHelper.tips(player, "&8Click to send"), durVars)) {
                    clockLore.add(color(tip));
                }
                meta.setLore(clockLore);
                clock.setItemMeta(meta);
            }
            CMIGuiButton btn = new CMIGuiButton(slots[i], clock);
            btn.lockField();
            btn.addCommand("lmdo rival challenge_send " + targetArg + "@" + minutes + " challenge");
            gui.addButton(btn);
        }

        gui.addButton(pageBtn(player, 36, "rival.challenge.back_picker", Material.ARROW, "&7Back",
                "pick_challenge", "&7Pick another player"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openPicker(
            Player player, String action, String backPage, String title, String tip) {
        CMIGui gui = base(player, "&8Rival", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.PLAYER_HEAD, title);
        info.lockField();
        List<String> pickerHeader = new ArrayList<>();
        pickerHeader.add("");
        pickerHeader.add("&7Online players");
        pickerHeader.addAll(GuiBoardHelper.tips(player, "&8Click a head to confirm"));
        info.addLore(pickerHeader);
        gui.addButton(info);

        List<Player> online = GuiPlayerPicker.onlineExcept(player);
        int placed = 0;
        for (Player other : online) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            ItemStack head = GuiPlayerPicker.head(other, "&f" + other.getName(),
                    pickerTip(player, tip));
            CMIGuiButton btn = new CMIGuiButton(slot, head);
            btn.lockField();
            btn.addCommand("lmdo rival " + action + " uuid:" + other.getUniqueId() + " " + backPage);
            gui.addButton(btn);
        }
        if (placed == 0) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("rival.empty.no_online", "&cNo one online"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("rival.empty.no_online",
                    GuiBoardHelper.tipsList(player, List.of("&7Other players must be online"))));
            gui.addButton(empty);
        }

        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", backPage, "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openCurrentRivalPicker(
            Player player, String action, String backPage, String title, String tip) {
        CMIGui gui = base(player, "&8Rival", 5);
        List<GuiBoardHelper.RivalCard> cards = GuiBoardHelper.parseRivalCards(
                ForgeBridge.rivalCurrentCards(player));
        CMIGuiButton info = new CMIGuiButton(4, Material.PLAYER_HEAD, title);
        info.lockField();
        List<String> rivalPickHeader = new ArrayList<>();
        rivalPickHeader.add("");
        rivalPickHeader.add("&7Your current rivals");
        rivalPickHeader.addAll(GuiBoardHelper.tips(player, "&8Click a head to " + action));
        info.addLore(rivalPickHeader);
        gui.addButton(info);

        if (cards.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("rival.empty.no_rivals", "&cNo rivals"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("rival.empty.no_rivals",
                    GuiBoardHelper.tipsList(player, List.of("&7You have no rivals to " + action))));
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(cards.size(), 21));
            for (int i = 0; i < slots.length && i < cards.size(); i++) {
                GuiBoardHelper.RivalCard card = cards.get(i);
                ItemStack head = GuiBoardHelper.rivalHead(card);
                org.bukkit.inventory.meta.ItemMeta meta = head.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() && meta.getLore() != null
                            ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    List<String> tipLines = pickerTip(player, tip);
                    if (!tipLines.isEmpty()) {
                        lore.add(color(""));
                        for (String tipLine : tipLines) {
                            lore.add(color(tipLine));
                        }
                    }
                    meta.setLore(lore);
                    head.setItemMeta(meta);
                }
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.lockField();
                btn.addCommand("lmdo rival " + action + " " + card.pickerArg() + " " + backPage);
                gui.addButton(btn);
            }
        }

        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", backPage, "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openMutualReplacePicker(Player player) {
        CMIGui gui = base(player, "&8Replace Mutual", 5);
        Map<String, String> ph = ForgeBridge.rivalPlaceholders(player);
        String pendingName = ph.getOrDefault("pending_mutual_accept_name", "");
        if (pendingName == null || pendingName.isBlank()) {
            pendingName = "new rival";
        }
        CMIGuiButton info = new CMIGuiButton(4, Material.GOLDEN_SWORD, "&e&lReplace Mutual Slot");
        info.lockField();
        List<String> header = new ArrayList<>();
        header.add("");
        header.add("&7Accepting &f" + pendingName);
        header.add("&7Mutual slots full — pick who to replace");
        header.addAll(GuiBoardHelper.tips(player, "&8They become Declared · you get the new Mutual"));
        info.addLore(header);
        gui.addButton(info);

        List<GuiBoardHelper.RivalCard> cards = GuiBoardHelper.parseRivalCards(
                ForgeBridge.rivalCurrentCards(player));
        List<GuiBoardHelper.RivalCard> mutuals = new ArrayList<>();
        for (GuiBoardHelper.RivalCard card : cards) {
            String st = card.status == null ? "" : card.status.trim();
            if ("Mutual".equalsIgnoreCase(st) || "Nemesis".equalsIgnoreCase(st)) {
                mutuals.add(card);
            }
        }
        if (mutuals.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("rival.empty.no_mutual", "&cNo Mutuals"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("rival.empty.no_mutual",
                    GuiBoardHelper.tipsList(player, List.of("&7You have no Mutual rivals to replace"))));
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(mutuals.size(), 21));
            for (int i = 0; i < slots.length && i < mutuals.size(); i++) {
                GuiBoardHelper.RivalCard card = mutuals.get(i);
                ItemStack head = GuiBoardHelper.rivalHead(card);
                org.bukkit.inventory.meta.ItemMeta meta = head.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() && meta.getLore() != null
                            ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add(color(""));
                    lore.add(color("&eClick to replace with &f" + pendingName));
                    lore.add(color("&8" + card.name + " becomes Declared"));
                    meta.setLore(lore);
                    head.setItemMeta(meta);
                }
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.lockField();
                btn.addCommand("lmdo rival accept_replace " + card.pickerArg() + " list");
                gui.addButton(btn);
            }
        }
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "pending", "&7Pending"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openPendingPicker(
            Player player, String action, String backPage, String title, String tip,
            boolean acceptMode) {
        CMIGui gui = base(player, "&8Rival", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.PLAYER_HEAD, title);
        info.lockField();
        List<String> pendingPickHeader = new ArrayList<>();
        pendingPickHeader.add("");
        pendingPickHeader.add(acceptMode
                ? "&7Pending declares + Declared"
                : "&7Pending declares");
        pendingPickHeader.addAll(GuiBoardHelper.tips(player, "&8Online first · offline by name"));
        info.addLore(pendingPickHeader);
        gui.addButton(info);

        List<String> pending = acceptMode
                ? ForgeBridge.rivalAcceptCandidateArgs(player)
                : ForgeBridge.rivalPendingIncomingDeclareArgs(player);
        int placed = 0;
        for (String arg : pending) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            String display;
            ItemStack head;
            List<String> tipLore = new ArrayList<>(pickerTip(player, tip));
            if (arg.regionMatches(true, 0, "uuid:", 0, 5)) {
                try {
                    java.util.UUID id = java.util.UUID.fromString(arg.substring(5).trim());
                    Player online = org.bukkit.Bukkit.getPlayer(id);
                    display = online != null ? online.getName() : arg.substring(5).trim();
                    List<String> headLore = new ArrayList<>(tipLore);
                    if (online != null) {
                        headLore.add("&aOnline");
                    }
                    head = online != null
                            ? GuiPlayerPicker.head(online, "&f" + display, headLore)
                            : GuiPlayerPicker.headByName(display, "&f" + display, tipLore);
                } catch (IllegalArgumentException e) {
                    display = arg;
                    head = GuiPlayerPicker.headByName(display, "&f" + display, tipLore);
                }
            } else {
                display = arg;
                Player online = org.bukkit.Bukkit.getPlayerExact(arg);
                List<String> headLore = new ArrayList<>(tipLore);
                if (online != null) {
                    headLore.addAll(pickerTip(player, "&7Offline pending · name"));
                    head = GuiPlayerPicker.head(online, "&f" + display, headLore);
                } else {
                    headLore.addAll(pickerTip(player, "&8Offline — accept by name"));
                    head = GuiPlayerPicker.headByName(display, "&f" + display, headLore);
                }
            }
            CMIGuiButton btn = new CMIGuiButton(slot, head);
            btn.lockField();
            btn.addCommand("lmdo rival " + action + " " + arg + " " + backPage);
            gui.addButton(btn);
        }
        if (placed == 0) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("rival.empty.no_pending",
                            acceptMode ? "&eNothing to accept" : "&eNo pending declares"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("rival.empty.no_pending", GuiBoardHelper.tipsList(player, acceptMode
                    ? List.of("&7Pending Declares and Declared rivals",
                            "&7(both Silent) appear here.")
                    : List.of("&7When someone declares you,",
                            "&7they appear here to decline."))));
            gui.addButton(empty);
        }

        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", backPage, "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openLines(Player player, String page, String title, Material mat, String backPage) {
        openDetail(player, page, title, mat, backPage);
    }

    private static List<String> statusLore(Player player, Map<String, String> ph) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7RP &f" + ph.getOrDefault("rp", "0")
                + " &8(&" + ph.getOrDefault("tier_color", "7")
                + ph.getOrDefault("tier", "?") + "&8)");
        lore.add("&7Mutual &f" + ph.getOrDefault("mutual", "0")
                + "&8/&f" + ph.getOrDefault("mutual_max", "3"));
        lore.add("&7Record &a" + ph.getOrDefault("wins", "0")
                + "&7/&c" + ph.getOrDefault("losses", "0")
                + "&7/&e" + ph.getOrDefault("draws", "0"));
        lore.add("&7TP msg &f"
                + ("true".equalsIgnoreCase(ph.get("tpMsg")) ? "ON" : "OFF"));
        if ("true".equalsIgnoreCase(ph.getOrDefault("challengeActive", "false"))) {
            lore.add("&eChallenge active");
        }
        lore.add("");
        GuiBoardHelper.addOverhaulCombat(lore, ph);
        if (ForgeBridge.isStaff(player)) {
            lore.add("");
            lore.add("&8List · Actions · Challenge · Top · Progress");
        }
        return lore;
    }

    private static List<String> unavailableLore(boolean bridgeOk) {
        if (!bridgeOk) {
            return List.of("", "&cForge LegacyMechanics mod unreachable",
                    "&7Check mods/ for LegacyMechanics-*.jar");
        }
        return List.of("", "&cRival system is disabled", "&7Ask an admin if you need access");
    }

    private static List<String> toAmp(List<String> lines) {
        List<String> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        for (String line : lines) {
            out.add(line == null ? "" : line.replace('§', '&'));
        }
        return out;
    }

    private static void fillEmpty(CMIGui gui, int rows) {
        int size = rows * 9;
        Map<Integer, CMIGuiButton> existing = gui.getButtons();
        for (int i = 0; i < size; i++) {
            if (existing != null && existing.containsKey(i)) {
                continue;
            }
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            CMIGuiButton pane = new CMIGuiButton(i, edge ? ACCENT : FILL, " ");
            pane.lockField();
            gui.addButton(pane);
        }
    }

    private static CMIGui base(Player player, String title, int rows) {
        CMIGui gui = new CMIGui(player);
        gui.setTitle(title);
        gui.setInvSize(rows);
        gui.addLock(InvType.Gui);
        return gui;
    }

    private static CMIGuiButton actionBtn(
            Player player, int slot, Material mat, String name, String action, String arg, String returnPage,
            List<String> tip) {
        return actionBtn(player, slot, null, mat, name, action, arg, returnPage, tip, null);
    }

    private static CMIGuiButton actionBtn(
            Player player, int slot, String key, Material mat, String name, String action, String arg,
            String returnPage, List<String> tip
    ) {
        return actionBtn(player, slot, key, mat, name, action, arg, returnPage, tip, null);
    }

    private static CMIGuiButton actionBtn(
            Player player, int slot, String key, Material mat, String name, String action, String arg,
            String returnPage, List<String> tip, Map<String, String> vars
    ) {
        String display = key == null || key.isBlank() ? name : GuiTooltips.name(key, name, vars);
        CMIGuiButton btn = new CMIGuiButton(slot, mat, display);
        btn.lockField();
        btn.addLore(key == null || key.isBlank()
                ? withBlank(tip)
                : GuiTooltips.buttonLore(key, tip, vars, null));
        btn.addCommand("lmdo rival " + action + " " + arg + " " + returnPage);
        return btn;
    }

    /** Player-head tip lines, staff-only, wired through the picker's GuiTooltips key. */
    private static List<String> pickerTip(Player player, String... tips) {
        List<String> defaults = GuiBoardHelper.tips(player, tips);
        List<String> out = new ArrayList<>();
        for (String tip : defaults) {
            if (tip == null) {
                continue;
            }
            out.addAll(GuiTooltips.lore(
                    "rival.picker.tip",
                    List.of(tip),
                    Map.of("tip", tip)));
        }
        return out;
    }

    private static CMIGuiButton pageBtn(Player player, int slot, Material mat, String name, String page, String... tips) {
        return pageBtn(player, slot, null, mat, name, page, null, tips);
    }

    private static CMIGuiButton pageBtn(
            Player player, int slot, String key, Material mat, String name, String page, String... tips
    ) {
        return pageBtn(player, slot, key, mat, name, page, null, tips);
    }

    private static CMIGuiButton pageBtn(
            Player player, int slot, String key, Material mat, String name, String page,
            Map<String, String> vars, String... tips
    ) {
        List<String> defaults = new ArrayList<>();
        if (tips != null) {
            for (String tip : tips) {
                if (tip != null) {
                    defaults.add(tip);
                }
            }
        }
        String display = key == null || key.isBlank() ? name : GuiTooltips.name(key, name, vars);
        CMIGuiButton btn = new CMIGuiButton(slot, mat, display);
        btn.lockField();
        btn.addLore(key == null || key.isBlank()
                ? withBlank(defaults)
                : GuiTooltips.buttonLore(key, defaults, vars, null));
        btn.addCommand("lmdo rival page " + page);
        return btn;
    }

    private static List<String> withBlank(List<String> tip) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        if (tip != null) {
            lore.addAll(tip);
        }
        return lore;
    }

    private static CMIGuiButton cmdBtn(int slot, Material mat, String name, String command, String... tips) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String tip : tips) {
            lore.add(tip);
        }
        btn.addLore(lore);
        btn.addCommand(command);
        btn.setCloseInv(true);
        return btn;
    }

    private static CMIGuiButton hubBtn(int slot) {
        return GuiNav.cmiHubButton(slot);
    }

    private static CMIGuiButton closeBtn(int slot) {
        return GuiNav.cmiCloseButton(slot);
    }

    private static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }
}
