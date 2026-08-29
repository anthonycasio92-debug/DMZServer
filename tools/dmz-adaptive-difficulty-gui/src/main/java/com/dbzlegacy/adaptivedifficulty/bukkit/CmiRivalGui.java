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
            switch (p) {
                case "list" -> openList(player);
                case "actions" -> openActions(player);
                case "pending", "invites", "pendinginvites" -> openPending(player);
                case "history", "past", "previous" -> openHistory(player);
                case "pick_declare" -> openPicker(player, "declare", "actions",
                        "&6Declare Rival", "&7Click to declare this player");
                case "pick_accept" -> openPendingPicker(player, "accept", "actions",
                        "&aAccept Rivalry", "&7Pending declare or Declared → Mutual", true);
                case "pick_decline" -> openPendingPicker(player, "decline", "actions",
                        "&cDecline Declare", "&7Click to decline their declare", false);
                case "pick_remove" -> openCurrentRivalPicker(player, "remove", "actions",
                        "&cRemove Rival", "&7Click to remove this rivalry");
                case "pick_challenge" -> openChallengeTargetPicker(player);
                case "pick_spectate" -> openPicker(player, "spectate", "challenge",
                        "&bSpectate", "&7Watch their active challenge");
                case "pick_silent" -> openPicker(player, "silent", "actions",
                        "&8Silent Rival", "&7Click for silent rivalry");
                case "stats", "statistics" -> openDetail(player, "stats", "&eRival Stats", Material.BOOK, "progress");
                case "challenge", "challenges" -> openChallenge(player);
                case "top", "leaderboard" -> openTop(player);
                case "progress" -> openProgress(player);
                case "season" -> openDetail(player, "season", "&aSeason", Material.CLOCK, "progress");
                case "quests", "quest" -> openDetail(player, "quests", "&bQuests", Material.WRITABLE_BOOK, "progress");
                case "achievements", "achs", "ach" ->
                        openDetail(player, "achievements", "&dAchievements", Material.DIAMOND, "progress");
                case "hof", "hall" -> openDetail(player, "hof", "&6Hall of Fame", Material.GOLD_BLOCK, "progress");
                case "journal" -> openDetail(player, "journal", "&fJournal", Material.MAP, "progress");
                case "title", "titles" -> openDetail(player, "title", "&eTitle", Material.NAME_TAG, "progress");
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
            gui.open();
            return;
        }
        status.addLore(statusLore(player, ph));
        gui.addButton(status);

        // Main: List · Actions · Challenge · Top · History · Progress · toggles
        gui.addButton(pageBtn(player, 19, Material.PLAYER_HEAD, "&6List", "list",
                "&7Current rivals", "&8Heads · hover for stats"));
        int pendingCount = 0;
        try {
            pendingCount = Integer.parseInt(ph.getOrDefault("pending_invites", "0"));
        } catch (NumberFormatException ignored) {
            pendingCount = 0;
        }
        gui.addButton(pageBtn(player, 21, Material.EMERALD, "&aActions", "actions",
                "&7Declare · accept · decline · remove",
                pendingCount > 0
                        ? "&e" + pendingCount + " pending invite" + (pendingCount == 1 ? "" : "s")
                        : "&8Pending invites live here"));
        gui.addButton(pageBtn(player, 23, Material.DIAMOND_SWORD, "&cChallenge", "challenge",
                "&7Send · accept · decline · spectate"));
        gui.addButton(pageBtn(player, 25, Material.GOLDEN_HELMET, "&fTop", "top",
                "&7RP leaderboard"));
        gui.addButton(pageBtn(player, 29, Material.SKELETON_SKULL, "&8History", "history",
                "&7Previous rivals", "&8Archived when removed"));
        gui.addButton(pageBtn(player, 31, Material.BOOK, "&bProgress", "progress",
                "&7Season · quests · achs · HOF · journal · title"));

        boolean tpOn = "true".equalsIgnoreCase(ph.getOrDefault("tpMsg", "false"));
        gui.addButton(actionBtn(player, 33,
                tpOn ? Material.BELL : Material.GRAY_DYE,
                tpOn ? "&aTP Msg ON" : "&8TP Msg OFF",
                "tpmsg", "toggle", "main",
                List.of(
                        tpOn ? "&7Click to mute rival TP messages" : "&7Click to show rival TP messages",
                        "&8Only affects rivalry TP chat"
                )));

        boolean instinctFeature = "true".equalsIgnoreCase(ph.getOrDefault("instinct_feature", "false"));
        if (instinctFeature) {
            boolean instinctOn = "true".equalsIgnoreCase(ph.getOrDefault("instinct", "false"));
            gui.addButton(actionBtn(player, 34,
                    instinctOn ? Material.LIME_DYE : Material.GRAY_DYE,
                    instinctOn ? "&aInstinct ON" : "&8Instinct OFF",
                    "instinct", "toggle", "main",
                    List.of(
                            instinctOn ? "&7Click to disable Rival Instinct" : "&7Click to enable Rival Instinct",
                            "&8Alerts for mutual / nemesis rivals"
                    )));
        }

        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        if (ForgeBridge.isStaff(player)) {
            gui.addButton(pageBtn(player, 37, Material.COMMAND_BLOCK, "&cAdmin", "admin",
                    "&7Save · refresh · status"));
        }
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openProgress(Player player) {
        CMIGui gui = base(player, "&8Rival Progress", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.BOOK, "&b&lProgress");
        info.lockField();
        List<String> progHeader = new ArrayList<>();
        progHeader.add("");
        progHeader.addAll(GuiBoardHelper.tips(player,
                "&7Each section is its own board",
                "&7Stats · Season · Quests · Achs · HOF · Journal · Title"));
        info.addLore(progHeader);
        gui.addButton(info);

        String[] pages = {"stats", "season", "quests", "achievements", "hof", "journal", "title"};
        Material[] mats = {
                Material.BOOK, Material.CLOCK, Material.WRITABLE_BOOK, Material.DIAMOND,
                Material.GOLD_BLOCK, Material.MAP, Material.NAME_TAG
        };
        String[] titles = {"&eStats", "&aSeason", "&bQuests", "&dAchs", "&6HOF", "&fJournal", "&eTitle"};
        int[] slots = GuiBoardHelper.centeredRow(7);
        for (int i = 0; i < pages.length && i < slots.length; i++) {
            List<String> preview = previewLines(ForgeBridge.rivalLines(player, pages[i]), 4);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.addAll(preview);
            lore.add("");
            lore.addAll(GuiBoardHelper.tips(player, "&eClick to open"));
            CMIGuiButton btn = new CMIGuiButton(slots[i], mats[i], titles[i]);
            btn.lockField();
            btn.addLore(lore);
            btn.addCommand("lmdo rival page " + pages[i]);
            gui.addButton(btn);
        }

        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
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
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&7No rivalry data yet");
            empty.lockField();
            List<String> emptyLore = new ArrayList<>();
            emptyLore.add("");
            emptyLore.addAll(GuiBoardHelper.tipsList(player, List.of("&7Challenge rivals to earn RP")));
            empty.addLore(emptyLore);
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
        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openDetail(Player player, String page, String title, Material mat, String backPage) {
        CMIGui gui = base(player, "&8Rival", 5);
        String back = backPage == null || backPage.isBlank() ? "main" : backPage;
        List<String> lore = toAmp(ForgeBridge.rivalLines(player, page));
        if (lore.isEmpty()) {
            lore = List.of("&7Nothing here yet.", "&8Data: config/legacymechanics/");
        }
        CMIGuiButton header = new CMIGuiButton(4, mat, title);
        header.lockField();
        List<String> detailHeader = new ArrayList<>();
        detailHeader.add("");
        detailHeader.addAll(GuiBoardHelper.tips(player, "&7One item per entry", "&8Centered below"));
        header.addLore(detailHeader);
        gui.addButton(header);
        List<GuiBoardHelper.DetailTile> tiles = GuiBoardHelper.detailTiles(lore);
        int[] slots = GuiBoardHelper.centeredSlots(Math.min(tiles.size(), 21));
        for (int i = 0; i < slots.length && i < tiles.size(); i++) {
            GuiBoardHelper.DetailTile tile = tiles.get(i);
            CMIGuiButton btn = new CMIGuiButton(slots[i], tile.icon, tile.title);
            btn.lockField();
            List<String> tip = new ArrayList<>();
            tip.add("");
            tip.addAll(tile.lore);
            btn.addLore(tip);
            gui.addButton(btn);
        }
        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", back, "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openAdmin(Player player) {
        CMIGui gui = base(player, "&8Rival Admin", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.COMMAND_BLOCK, "&c&lRival Admin");
        info.lockField();
        info.addLore(List.of("", "&7Staff-only tools",
                "&8Save · refresh · status",
                "&8Player menus stay on the main Rival GUI"));
        gui.addButton(info);
        gui.addButton(actionBtn(player, 20, Material.WRITABLE_BOOK, "&aSave",
                "admin", "save", "admin",
                List.of("&7Write rivalry-v4 + progression-v4", "&8/rival admin save")));
        gui.addButton(actionBtn(player, 22, Material.CLOCK, "&eRefresh",
                "admin", "refresh", "admin",
                List.of("&7Reload stores from disk", "&8/rival admin refresh")));
        gui.addButton(actionBtn(player, 24, Material.SPYGLASS, "&bStatus",
                "admin", "status", "admin",
                List.of("&7Enabled + path summary", "&8/rival admin status")));
        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "main", "&7Player Rival menu"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
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
                "&8Hover a head for stats",
                "&8Manage relationships in Actions"));
        info.addLore(listHeader);
        gui.addButton(info);

        if (cards.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&7No rivals yet");
            empty.lockField();
            List<String> emptyLore = new ArrayList<>();
            emptyLore.add("");
            emptyLore.addAll(GuiBoardHelper.tipsList(player, List.of("&7Use Actions → Declare to start")));
            empty.addLore(emptyLore);
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

        gui.addButton(pageBtn(player, 37, Material.EMERALD, "&aActions", "actions",
                "&7Declare · accept · remove · pending"));
        gui.addButton(pageBtn(player, 39, Material.SKELETON_SKULL, "&8History", "history",
                "&7Previous rivals"));
        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
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
                "&a◀ Incoming &7= they Declared you",
                "&6▶ Outgoing &7= waiting on them"));
        info.addLore(pendingHeader);
        gui.addButton(info);

        if (invites.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&7No pending invites");
            empty.lockField();
            List<String> emptyLore = new ArrayList<>();
            emptyLore.add("");
            emptyLore.addAll(GuiBoardHelper.tipsList(player, List.of(
                    "&7Declare someone to send an invite",
                    "&7Incoming shows when they Declare you")));
            empty.addLore(emptyLore);
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(invites.size(), 21));
            for (int i = 0; i < slots.length && i < invites.size(); i++) {
                GuiBoardHelper.PendingInvite invite = invites.get(i);
                ItemStack head = GuiBoardHelper.pendingInviteHead(player, invite);
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.lockField();
                if (invite.incoming) {
                    btn.addCommand("lmdo rival accept " + invite.pickerArg() + " pending");
                }
                gui.addButton(btn);
            }
        }

        gui.addButton(pageBtn(player, 37, Material.YELLOW_DYE, "&eAccept…", "pick_accept",
                "&7Accept incoming / Declared"));
        gui.addButton(pageBtn(player, 38, Material.ORANGE_DYE, "&6Decline…", "pick_decline",
                "&7Decline an incoming declare"));
        gui.addButton(pageBtn(player, 39, Material.EMERALD, "&aActions", "actions",
                "&7Full actions menu"));
        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "actions", "&7Actions"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openActions(Player player) {
        CMIGui gui = base(player, "&8Rival Actions", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.EMERALD, "&a&lRival Actions");
        info.lockField();
        info.addLore(toAmp(ForgeBridge.rivalLines(player, "actions")));
        gui.addButton(info);

        gui.addButton(pageBtn(player, 19, Material.LIME_DYE, "&aDeclare…", "pick_declare",
                "&7Visible declare → they Accept → Mutual"));
        Map<String, String> ph = ForgeBridge.rivalPlaceholders(player);
        int pendingCount = 0;
        try {
            pendingCount = Integer.parseInt(ph.getOrDefault("pending_invites", "0"));
        } catch (NumberFormatException ignored) {
            pendingCount = 0;
        }
        gui.addButton(pageBtn(player, 20, Material.CLOCK,
                pendingCount > 0 ? "&ePending &f(" + pendingCount + ")" : "&ePending",
                "pending",
                "&7View incoming + outgoing invites",
                pendingCount > 0 ? "&aYou have pending invites" : "&8No pending invites"));
        gui.addButton(pageBtn(player, 21, Material.YELLOW_DYE, "&eAccept…", "pick_accept",
                "&7Pending declares, or Declared → Mutual",
                "&8Both Silent → Declared shows here"));
        gui.addButton(pageBtn(player, 22, Material.ORANGE_DYE, "&6Decline…", "pick_decline",
                "&7Decline a pending declare"));
        gui.addButton(pageBtn(player, 23, Material.RED_DYE, "&cRemove…", "pick_remove",
                "&7Pick one of your rivals to remove"));
        gui.addButton(pageBtn(player, 25, Material.GRAY_DYE, "&8Silent…", "pick_silent",
                "&7One-sided Unknown (they are not told)",
                "&8Both Silent → Declared (both notified)"));

        gui.addButton(pageBtn(player, 37, Material.PLAYER_HEAD, "&6List", "list",
                "&7Back to current rivals"));
        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
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
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&7No history yet");
            empty.lockField();
            List<String> emptyLore = new ArrayList<>();
            emptyLore.add("");
            emptyLore.addAll(GuiBoardHelper.tipsList(player, List.of("&7Removed rivals show here")));
            empty.addLore(emptyLore);
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

        gui.addButton(pageBtn(player, 37, Material.PLAYER_HEAD, "&6List", "list",
                "&7Current rivals"));
        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openChallenge(Player player) {
        CMIGui gui = base(player, "&8Rival", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.IRON_SWORD, "&c&lChallenge");
        info.lockField();
        info.addLore(toAmp(ForgeBridge.rivalLines(player, "challenge")));
        gui.addButton(info);

        gui.addButton(pageBtn(player, 19, Material.GOLDEN_SWORD, "&eSend Challenge…", "pick_challenge",
                "&7Pick rival, then choose 1–10 minutes"));
        gui.addButton(actionBtn(player, 21, Material.LIME_CONCRETE, "&aAccept",
                "challenge", "accept", "challenge",
                List.of("&7Accept pending challenge")));
        gui.addButton(actionBtn(player, 23, Material.RED_CONCRETE, "&cDecline",
                "challenge", "decline", "challenge",
                List.of("&7Decline pending challenge")));
        gui.addButton(actionBtn(player, 25, Material.GRAY_CONCRETE, "&8Cancel",
                "challenge", "cancel", "challenge",
                List.of("&7Cancel your outgoing challenge")));
        gui.addButton(pageBtn(player, 29, Material.ENDER_EYE, "&bSpectate…", "pick_spectate",
                "&7Watch an online player's challenge"));
        gui.addButton(actionBtn(player, 31, Material.GRAY_DYE, "&8Stop Spectate",
                "spectate_stop", "0", "challenge",
                List.of("&7End spectating early")));

        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
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
                    GuiBoardHelper.tips(player, "&7Next: choose fight length", "&8(1–10 minutes)"));
            CMIGuiButton btn = new CMIGuiButton(slot, head);
            btn.lockField();
            btn.addCommand("lmdo rival page challenge_time:uuid:" + other.getUniqueId());
            gui.addButton(btn);
        }
        if (placed == 0) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&cNo one online");
            empty.lockField();
            List<String> emptyLore = new ArrayList<>();
            emptyLore.add("");
            emptyLore.addAll(GuiBoardHelper.tipsList(player, List.of("&7Other players must be online")));
            empty.addLore(emptyLore);
            gui.addButton(empty);
        }

        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "challenge", "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
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
                                GuiBoardHelper.tips(player, "&7Choose fight length", "&81–10 minutes"))
                        : GuiPlayerPicker.headByName(display, "&f" + display,
                                GuiBoardHelper.tips(player, "&cPlayer offline", "&7Pick someone else"));
            } catch (IllegalArgumentException e) {
                head = GuiPlayerPicker.headByName(display, "&f" + display,
                        GuiBoardHelper.tips(player, "&7Choose minutes"));
            }
        } else {
            Player online = org.bukkit.Bukkit.getPlayerExact(targetArg);
            display = online != null ? online.getName() : targetArg;
            head = online != null
                    ? GuiPlayerPicker.head(online, "&f" + display,
                            GuiBoardHelper.tips(player, "&7Choose fight length"))
                    : GuiPlayerPicker.headByName(display, "&f" + display,
                            GuiBoardHelper.tips(player, "&7Choose minutes"));
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
                meta.setDisplayName(color("&e" + minutes + " minute" + (minutes == 1 ? "" : "s")));
                List<String> clockLore = new ArrayList<>();
                clockLore.add(color(""));
                clockLore.add(color("&7Challenge &f" + display));
                for (String tip : GuiBoardHelper.tips(player, "&8Click to send")) {
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

        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "pick_challenge", "&7Pick another player"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
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
                    GuiBoardHelper.tips(player, tip));
            CMIGuiButton btn = new CMIGuiButton(slot, head);
            btn.lockField();
            btn.addCommand("lmdo rival " + action + " uuid:" + other.getUniqueId() + " " + backPage);
            gui.addButton(btn);
        }
        if (placed == 0) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&cNo one online");
            empty.lockField();
            List<String> emptyLore = new ArrayList<>();
            emptyLore.add("");
            emptyLore.addAll(GuiBoardHelper.tipsList(player, List.of("&7Other players must be online")));
            empty.addLore(emptyLore);
            gui.addButton(empty);
        }

        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", backPage, "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
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
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&cNo rivals");
            empty.lockField();
            List<String> emptyLore = new ArrayList<>();
            emptyLore.add("");
            emptyLore.addAll(GuiBoardHelper.tipsList(player,
                    List.of("&7You have no rivals to " + action)));
            empty.addLore(emptyLore);
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
                    List<String> tipLines = GuiBoardHelper.tips(player, tip);
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

        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", backPage, "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
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
            List<String> tipLore = new ArrayList<>(GuiBoardHelper.tips(player, tip));
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
                    headLore.addAll(GuiBoardHelper.tips(player, "&7Offline pending · name"));
                    head = GuiPlayerPicker.head(online, "&f" + display, headLore);
                } else {
                    headLore.addAll(GuiBoardHelper.tips(player, "&8Offline — accept by name"));
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
                    acceptMode ? "&eNothing to accept" : "&eNo pending declares");
            empty.lockField();
            List<String> emptyLore = new ArrayList<>();
            emptyLore.add("");
            emptyLore.addAll(GuiBoardHelper.tipsList(player, acceptMode
                    ? List.of("&7Pending Declares and Declared rivals",
                            "&7(both Silent) appear here.")
                    : List.of("&7When someone declares you,",
                            "&7they appear here to decline.")));
            empty.addLore(emptyLore);
            gui.addButton(empty);
        }

        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", backPage, "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
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
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(GuiBoardHelper.tipsList(player, tip));
        btn.addLore(lore);
        btn.addCommand("lmdo rival " + action + " " + arg + " " + returnPage);
        return btn;
    }

    private static CMIGuiButton pageBtn(Player player, int slot, Material mat, String name, String page, String... tips) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(GuiBoardHelper.tips(player, tips));
        btn.addLore(lore);
        btn.addCommand("lmdo rival page " + page);
        return btn;
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
        CMIGuiButton btn = new CMIGuiButton(slot, Material.COMPASS, "&7« Hub");
        btn.lockField();
        btn.addCommand("lm");
        btn.setCloseInv(true);
        return btn;
    }

    private static CMIGuiButton closeBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.BARRIER, "&cClose");
        btn.lockField();
        btn.setCloseInv(true);
        return btn;
    }

    private static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }
}
