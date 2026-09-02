package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Bukkit chest GUI fallback — Legacy Mechanics Rival. */
public final class RivalChestGui implements Listener {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private final AdaptiveDifficultyGuiPlugin plugin;

    public RivalChestGui(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
    }


    private static boolean inspecting(Player viewer, Player subject) {
        return viewer != null && subject != null
                && !viewer.getUniqueId().equals(subject.getUniqueId());
    }

    private static String invTitle(Player viewer, Player subject, String base) {
        if (inspecting(viewer, subject)) {
            return color(base + " · &c" + subject.getName());
        }
        return color(base);
    }

    public void open(Player player, String page) {
        Player viewer = player;
        Player subject = AdminInspectSessions.resolveSubject(viewer);
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("challenge_time:")) {
            GuiFeedback.openChest(viewer, challengeTime(viewer, subject, raw.substring("challenge_time:".length()).trim()));
            return;
        }
        Inventory inv = switch (lower) {
            case "list" -> list(viewer, subject);
            case "actions" -> actions(viewer, subject);
            case "pending", "invites", "pendinginvites" -> pending(viewer, subject);
            case "history", "past", "previous" -> history(viewer, subject);
            case "pick_declare" -> picker(viewer, subject, "declare", "actions",
                    "&6Declare Rival", "&7Click to declare this player");
            case "pick_accept" -> pendingPicker(viewer, subject, "accept", "actions",
                    "&aAccept Rivalry", "&7Pending declare or Declared → Mutual", true);
            case "pick_decline" -> pendingPicker(viewer, subject, "decline", "actions",
                    "&cDecline Declare", "&7Click to decline their declare", false);
            case "pick_remove" -> currentRivalPicker(viewer, subject, "remove", "actions",
                    "&cRemove Rival", "&7Click to remove this rivalry");
            case "pick_challenge" -> challengeTargetPicker(viewer, subject);
            case "pick_spectate" -> picker(viewer, subject, "spectate", "challenge",
                    "&bSpectate", "&7Watch their active challenge");
            case "pick_silent" -> picker(viewer, subject, "silent", "actions",
                    "&8Silent Rival", "&7Click for silent rivalry");
            case "stats", "statistics" -> detailBoard(viewer, subject, "stats", "&eRival Stats", Material.BOOK, "progress");
            case "challenge", "challenges" -> challenge(viewer, subject);
            case "top", "leaderboard" -> topBoard(viewer, subject, "top", "&fRP Top", "main");
            case "progress" -> progress(viewer, subject);
            case "season" -> detailBoard(viewer, subject, "season", "&aSeason", Material.CLOCK, "progress");
            case "quests", "quest" -> detailBoard(viewer, subject, "quests", "&bQuests", Material.WRITABLE_BOOK, "progress");
            case "achievements", "achs", "ach" ->
                    detailBoard(viewer, subject, "achievements", "&dAchievements", Material.DIAMOND, "progress");
            case "hof", "hall" -> detailBoard(viewer, subject, "hof", "&6Hall of Fame", Material.GOLD_BLOCK, "progress");
            case "journal" -> detailBoard(viewer, subject, "journal", "&fJournal", Material.MAP, "progress");
            case "title", "titles" -> detailBoard(viewer, subject, "title", "&eTitle", Material.NAME_TAG, "progress");
            // Help page removed — open hub main.
            case "help" -> main(viewer, subject);
            case "admin" -> ForgeBridge.isStaff(viewer) ? admin(viewer, subject) : main(viewer, subject);
            default -> main(viewer, subject);
        };
        GuiFeedback.openChest(viewer, inv);
    }

    private Inventory main(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.rivalPlaceholders(subject);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Rival"));
        holder.bind(inv);
        frame(inv, 45);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        if (!bridgeOk || !systemOn) {
            put(holder, inv, 4, item(Material.NETHER_STAR,
                    !bridgeOk ? "&c&lUNAVAILABLE" : "&c&lRIVAL DISABLED",
                    unavailableLore(bridgeOk)));
            put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
            put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        put(holder, inv, 4, item(Material.NAME_TAG, "&6&lRival", statusLore(viewer, ph)));
        put(holder, inv, 19, tipBtn(viewer, "rival.main.list", Material.PLAYER_HEAD, "&6List",
                List.of("&7Current rivals", "&8Heads · hover for stats")), SlotAction.page("list"));
        int pendingCount = 0;
        try {
            pendingCount = Integer.parseInt(ph.getOrDefault("pending_invites", "0"));
        } catch (NumberFormatException ignored) {
            pendingCount = 0;
        }
        String pendingLine = pendingCount > 0
                ? "&e" + pendingCount + " pending invite" + (pendingCount == 1 ? "" : "s")
                : "&8Pending invites live here";
        put(holder, inv, 21, tipBtn(viewer, "rival.main.actions", Material.EMERALD, "&aActions",
                List.of("&7Declare · accept · decline · remove", pendingLine),
                Map.of("pending", pendingLine)),
                SlotAction.page("actions"));
        put(holder, inv, 23, tipBtn(viewer, "rival.main.challenge", Material.DIAMOND_SWORD, "&cChallenge",
                List.of("&7Send · accept · decline · spectate")), SlotAction.page("challenge"));
        put(holder, inv, 25, tipBtn(viewer, "rival.main.top", Material.GOLDEN_HELMET, "&fTop",
                List.of("&7RP leaderboard")), SlotAction.page("top"));
        put(holder, inv, 29, tipBtn(viewer, "rival.main.history", Material.SKELETON_SKULL, "&8History",
                List.of("&7Previous rivals", "&8Archived when removed")), SlotAction.page("history"));
        put(holder, inv, 31, tipBtn(viewer, "rival.main.progress", Material.BOOK, "&bProgress",
                List.of("&7Season · quests · records")), SlotAction.page("progress"));

        boolean tpOn = "true".equalsIgnoreCase(ph.getOrDefault("tpMsg", "false"));
        put(holder, inv, 33, tipBtn(viewer,
                tpOn ? "rival.main.tpmsg_on" : "rival.main.tpmsg_off",
                tpOn ? Material.BELL : Material.GRAY_DYE,
                tpOn ? "&aTP Msg ON" : "&8TP Msg OFF",
                List.of(
                        tpOn ? "&7Click to mute rival TP messages" : "&7Click to show rival TP messages",
                        "&8Only affects rivalry TP chat"
                )), SlotAction.act("tpmsg", "toggle", "main"));

        boolean instinctFeature = "true".equalsIgnoreCase(ph.getOrDefault("instinct_feature", "false"));
        if (instinctFeature) {
            boolean instinctOn = "true".equalsIgnoreCase(ph.getOrDefault("instinct", "false"));
            put(holder, inv, 34, tipBtn(viewer,
                    instinctOn ? "rival.main.instinct_on" : "rival.main.instinct_off",
                    instinctOn ? Material.LIME_DYE : Material.GRAY_DYE,
                    instinctOn ? "&aInstinct ON" : "&8Instinct OFF",
                    List.of(
                            instinctOn ? "&7Click to disable Rival Instinct" : "&7Click to enable Rival Instinct",
                            "&8Alerts for mutual / nemesis rivals"
                    )), SlotAction.act("instinct", "toggle", "main"));
        }

        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        if (ForgeBridge.isStaff(viewer)) {
            put(holder, inv, 37, tipBtn(viewer, "rival.main.admin", Material.COMMAND_BLOCK, "&cAdmin",
                    List.of("&7Save · refresh · status")), SlotAction.page("admin"));
        }
        return inv;
    }

    private Inventory progress(Player viewer, Player subject) {
        Holder holder = new Holder("progress");
        Inventory inv = Bukkit.createInventory(holder, 27, invTitle(viewer, subject, "&8Rival Progress"));
        holder.bind(inv);
        frame(inv, 27);
        put(holder, inv, 4, item(Material.BOOK, "&b&lProgress",
                List.of("", "&7Season · quests · records")));

        // Compact hub: Stats · Season · Quests · Achievements · Title
        String[] pages = {"stats", "season", "quests", "achievements", "title"};
        Material[] mats = {
                Material.BOOK, Material.CLOCK, Material.WRITABLE_BOOK, Material.DIAMOND, Material.NAME_TAG
        };
        String[] titles = {"&eStats", "&aSeason", "&bQuests", "&dAchs", "&eTitle"};
        String[] tipKeys = {
                "rival.progress.stats", "rival.progress.season", "rival.progress.quests",
                "rival.progress.achs", "rival.progress.title"
        };
        int[] slots = {11, 12, 13, 14, 15};
        for (int i = 0; i < pages.length; i++) {
            put(holder, inv, slots[i],
                    tipBtn(viewer, tipKeys[i], mats[i], titles[i], List.of("&eClick to open")),
                    SlotAction.page(pages[i]));
        }
        // Secondary: HOF + Journal tucked beside nav
        put(holder, inv, 20, tipBtn(viewer, "rival.progress.hof", Material.GOLD_BLOCK, "&6HOF",
                List.of("&7Hall of Fame")), SlotAction.page("hof"));
        put(holder, inv, 21, tipBtn(viewer, "rival.progress.journal", Material.MAP, "&fJournal",
                List.of("&7Recent rivalry notes")), SlotAction.page("journal"));
        put(holder, inv, 18, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page("main"));
        put(holder, inv, 22, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 26, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory topBoard(Player viewer, Player subject, String page, String title, String backPage) {
        Holder holder = new Holder(page);
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Rival"));
        holder.bind(inv);
        frame(inv, 45);
        String back = backPage == null || backPage.isBlank() ? "main" : backPage;
        List<String> raw = toAmp(ForgeBridge.rivalLines(subject, page));
        List<GuiBoardHelper.TopEntry> entries = GuiBoardHelper.parseTopEntries(raw);
        List<String> topHeader = new ArrayList<>();
        topHeader.add("");
        topHeader.addAll(GuiBoardHelper.tips(viewer, "&7Top rivals by RP", "&8Player heads below"));
        put(holder, inv, 4, item(Material.GOLDEN_HELMET, title, topHeader));
        if (entries.isEmpty()) {
            put(holder, inv, 22, tipBtn(viewer, "rival.empty.no_top", Material.BARRIER, "&7No rivalry data yet",
                    List.of("&7Challenge rivals to earn RP")));
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(entries.size(), 21));
            for (int i = 0; i < slots.length && i < entries.size(); i++) {
                put(holder, inv, slots[i], GuiBoardHelper.topHead(entries.get(i)));
            }
        }
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page(back));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory detailBoard(Player viewer, Player subject, String page, String title, Material mat, String backPage) {
        Holder holder = new Holder(page);
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Rival"));
        holder.bind(inv);
        frame(inv, 45);
        String back = backPage == null || backPage.isBlank() ? "main" : backPage;
        List<String> lore = toAmp(ForgeBridge.rivalLines(subject, page));
        if (lore.isEmpty()) {
            lore = List.of("&7Nothing here yet.", "&8Data: config/legacymechanics/");
        }
        List<String> detailHeader = new ArrayList<>();
        detailHeader.add("");
        detailHeader.addAll(GuiBoardHelper.tips(viewer, "&7One item per entry", "&8Centered below"));
        put(holder, inv, 4, item(mat, title, detailHeader));
        List<GuiBoardHelper.DetailTile> tiles = GuiBoardHelper.detailTiles(lore);
        int[] slots = GuiBoardHelper.centeredSlots(Math.min(tiles.size(), 21));
        for (int i = 0; i < slots.length && i < tiles.size(); i++) {
            GuiBoardHelper.DetailTile tile = tiles.get(i);
            List<String> tip = new ArrayList<>();
            tip.add("");
            tip.addAll(tile.lore);
            put(holder, inv, slots[i], item(tile.icon, tile.title, tip));
        }
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page(back));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory admin(Player viewer, Player subject) {
        Holder holder = new Holder("admin");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Rival Admin"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.COMMAND_BLOCK, "&c&lRival Admin",
                List.of("", "&7Staff-only tools",
                        "&8Save · refresh · status",
                        "&8Player menus stay on the main Rival GUI")));
        put(holder, inv, 20, tipBtn(viewer, "rival.admin.save", Material.WRITABLE_BOOK, "&aSave",
                List.of("&7Write rivalry-v4 + progression-v4", "&8/rival admin save")),
                SlotAction.act("admin", "save", "admin"));
        put(holder, inv, 22, tipBtn(viewer, "rival.admin.refresh", Material.CLOCK, "&eRefresh",
                List.of("&7Reload stores from disk", "&8/rival admin refresh")),
                SlotAction.act("admin", "refresh", "admin"));
        put(holder, inv, 24, tipBtn(viewer, "rival.admin.status", Material.SPYGLASS, "&bStatus",
                List.of("&7Enabled + path summary", "&8/rival admin status")),
                SlotAction.act("admin", "status", "admin"));
        put(holder, inv, 36, pageBtn(viewer, "rival.admin.back", Material.ARROW, "&7Back", "&7Player Rival menu"),
                SlotAction.page("main"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
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
            // Skip first header-ish line for compact preview
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

    private Inventory list(Player viewer, Player subject) {
        Holder holder = new Holder("list");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Rival List"));
        holder.bind(inv);
        frame(inv, 45);
        List<GuiBoardHelper.RivalCard> cards = GuiBoardHelper.parseRivalCards(
                ForgeBridge.rivalCurrentCards(subject));
        List<String> listHeader = new ArrayList<>();
        listHeader.add("");
        listHeader.add(cards.isEmpty() ? "&7No rivals yet." : "&7" + cards.size() + " rival(s)");
        listHeader.addAll(GuiBoardHelper.tips(viewer,
                "&8Hover a head for stats",
                "&8Manage relationships in Actions"));
        put(holder, inv, 4, item(Material.PLAYER_HEAD, "&6&lCurrent Rivals", listHeader));
        if (cards.isEmpty()) {
            put(holder, inv, 22, tipBtn(viewer, "rival.empty.no_rivals", Material.BARRIER, "&7No rivals yet",
                    List.of("&7Use Actions → Declare to start")));
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(cards.size(), 21));
            for (int i = 0; i < slots.length && i < cards.size(); i++) {
                put(holder, inv, slots[i], GuiBoardHelper.rivalHead(cards.get(i)));
            }
        }
        put(holder, inv, 37, pageBtn(viewer, "rival.list.nav_actions", Material.EMERALD, "&aActions",
                "&7Declare · accept · remove · pending"), SlotAction.page("actions"));
        put(holder, inv, 39, pageBtn(viewer, "rival.list.nav_history", Material.SKELETON_SKULL, "&8History",
                "&7Previous rivals"), SlotAction.page("history"));
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory pending(Player viewer, Player subject) {
        Holder holder = new Holder("pending");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Pending Invites"));
        holder.bind(inv);
        frame(inv, 45);
        List<GuiBoardHelper.PendingInvite> invites = GuiBoardHelper.parsePendingInvites(
                ForgeBridge.rivalPendingInviteCards(subject));
        List<String> pendingHeader = new ArrayList<>();
        pendingHeader.add("");
        pendingHeader.add(invites.isEmpty() ? "&7No pending declares." : "&7" + invites.size() + " pending");
        pendingHeader.addAll(GuiBoardHelper.tips(viewer,
                "&a◀ Incoming &7= they Declared you",
                "&6▶ Outgoing &7= waiting on them"));
        put(holder, inv, 4, item(Material.YELLOW_DYE, "&e&lPending Invites", pendingHeader));
        if (invites.isEmpty()) {
            put(holder, inv, 22, tipBtn(viewer, "rival.empty.no_pending", Material.BARRIER, "&7No pending invites",
                    List.of("&7Declare someone to send an invite",
                            "&7Incoming shows when they Declare you")));
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(invites.size(), 21));
            for (int i = 0; i < slots.length && i < invites.size(); i++) {
                GuiBoardHelper.PendingInvite invite = invites.get(i);
                ItemStack head = GuiBoardHelper.pendingInviteHead(viewer, invite);
                if (invite.incoming) {
                    put(holder, inv, slots[i], head, SlotAction.act("accept", invite.pickerArg(), "pending"));
                } else {
                    put(holder, inv, slots[i], head);
                }
            }
        }
        put(holder, inv, 37, pageBtn(viewer, "rival.pending.accept_pick", Material.YELLOW_DYE, "&eAccept…",
                "&7Accept incoming / Declared"), SlotAction.page("pick_accept"));
        put(holder, inv, 38, pageBtn(viewer, "rival.pending.decline_pick", Material.ORANGE_DYE, "&6Decline…",
                "&7Decline an incoming declare"), SlotAction.page("pick_decline"));
        put(holder, inv, 39, pageBtn(viewer, "rival.pending.nav_actions", Material.EMERALD, "&aActions",
                "&7Full actions menu"), SlotAction.page("actions"));
        put(holder, inv, 36, pageBtn(viewer, "rival.pending.back", Material.ARROW, "&7Back", "&7Actions"),
                SlotAction.page("actions"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory actions(Player viewer, Player subject) {
        Holder holder = new Holder("actions");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Rival Actions"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.EMERALD, "&a&lRival Actions",
                prependBlank(toAmp(ForgeBridge.rivalLines(subject, "actions")))));
        put(holder, inv, 19, pageBtn(viewer, "rival.actions.declare", Material.LIME_DYE, "&aDeclare…",
                "&7Visible declare → they Accept → Mutual"), SlotAction.page("pick_declare"));
        Map<String, String> ph = ForgeBridge.rivalPlaceholders(subject);
        int pendingCount = 0;
        try {
            pendingCount = Integer.parseInt(ph.getOrDefault("pending_invites", "0"));
        } catch (NumberFormatException ignored) {
            pendingCount = 0;
        }
        put(holder, inv, 20, pageBtn(viewer, "rival.actions.pending", Material.CLOCK,
                pendingCount > 0 ? "&ePending &f(" + pendingCount + ")" : "&ePending",
                "&7View incoming + outgoing invites",
                pendingCount > 0 ? "&aYou have pending invites" : "&8No pending invites"),
                SlotAction.page("pending"));
        put(holder, inv, 21, pageBtn(viewer, "rival.actions.accept", Material.YELLOW_DYE, "&eAccept…",
                "&7Pending declares, or Declared → Mutual",
                "&8Both Silent → Declared shows here"), SlotAction.page("pick_accept"));
        put(holder, inv, 22, pageBtn(viewer, "rival.actions.decline", Material.ORANGE_DYE, "&6Decline…",
                "&7Decline a pending declare"), SlotAction.page("pick_decline"));
        put(holder, inv, 23, pageBtn(viewer, "rival.actions.remove", Material.RED_DYE, "&cRemove…",
                "&7Pick one of your rivals to remove"), SlotAction.page("pick_remove"));
        put(holder, inv, 25, pageBtn(viewer, "rival.actions.silent", Material.GRAY_DYE, "&8Silent…",
                "&7One-sided Silent (they are not told)",
                "&8Both Silent → Declared → Accept for Mutual"), SlotAction.page("pick_silent"));
        put(holder, inv, 37, pageBtn(viewer, "rival.actions.nav_list", Material.PLAYER_HEAD, "&6List",
                "&7Back to current rivals"), SlotAction.page("list"));
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory history(Player viewer, Player subject) {
        Holder holder = new Holder("history");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Rival History"));
        holder.bind(inv);
        frame(inv, 45);
        List<GuiBoardHelper.RivalCard> cards = GuiBoardHelper.parseRivalCards(
                ForgeBridge.rivalPastCards(subject));
        List<String> historyHeader = new ArrayList<>();
        historyHeader.add("");
        historyHeader.add(cards.isEmpty() ? "&7No previous rivals yet." : "&7" + cards.size() + " archived");
        historyHeader.addAll(GuiBoardHelper.tips(viewer,
                "&8Removed rivalries appear here",
                "&8Hover a head for final stats"));
        put(holder, inv, 4, item(Material.SKELETON_SKULL, "&8&lPrevious Rivals", historyHeader));
        if (cards.isEmpty()) {
            put(holder, inv, 22, tipBtn(viewer, "rival.empty.no_history", Material.BARRIER, "&7No history yet",
                    List.of("&7Removed rivals show here")));
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(cards.size(), 21));
            for (int i = 0; i < slots.length && i < cards.size(); i++) {
                put(holder, inv, slots[i], GuiBoardHelper.rivalHead(cards.get(i)));
            }
        }
        put(holder, inv, 37, pageBtn(viewer, "rival.history.nav_list", Material.PLAYER_HEAD, "&6List",
                "&7Current rivals"), SlotAction.page("list"));
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory challenge(Player viewer, Player subject) {
        Holder holder = new Holder("challenge");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Rival"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.IRON_SWORD, "&c&lChallenge",
                prependBlank(toAmp(ForgeBridge.rivalLines(subject, "challenge")))));
        put(holder, inv, 19, pageBtn(viewer, "rival.challenge.send", Material.GOLDEN_SWORD, "&eSend Challenge…",
                "&7Pick rival, then choose 1–10 minutes"), SlotAction.page("pick_challenge"));
        put(holder, inv, 21, tipBtn(viewer, "rival.challenge.accept", Material.LIME_CONCRETE, "&aAccept",
                List.of("&7Accept pending challenge")),
                SlotAction.act("challenge", "accept", "challenge"));
        put(holder, inv, 23, tipBtn(viewer, "rival.challenge.decline", Material.RED_CONCRETE, "&cDecline",
                List.of("&7Decline pending challenge")),
                SlotAction.act("challenge", "decline", "challenge"));
        put(holder, inv, 25, tipBtn(viewer, "rival.challenge.cancel", Material.GRAY_CONCRETE, "&8Cancel",
                List.of("&7Cancel your outgoing challenge")),
                SlotAction.act("challenge", "cancel", "challenge"));
        put(holder, inv, 29, pageBtn(viewer, "rival.challenge.spectate", Material.ENDER_EYE, "&bSpectate…",
                "&7Watch an online player's challenge"), SlotAction.page("pick_spectate"));
        put(holder, inv, 31, tipBtn(viewer, "rival.challenge.spectate_stop", Material.GRAY_DYE, "&8Stop Spectate",
                List.of("&7End spectating early")),
                SlotAction.act("spectate_stop", "0", "challenge"));
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    /** Step 1: pick who to challenge — opens duration picker next. */
    private Inventory challengeTargetPicker(Player viewer, Player subject) {
        Holder holder = new Holder("pick_challenge");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Rival"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> challengePickHeader = new ArrayList<>();
        challengePickHeader.add("");
        challengePickHeader.add("&7Online rivals");
        challengePickHeader.addAll(GuiBoardHelper.tips(viewer, "&8Click a head, then pick duration"));
        put(holder, inv, 4, item(Material.GOLDEN_SWORD, "&cSend Challenge", challengePickHeader));
        List<Player> online = GuiPlayerPicker.onlineExcept(subject);
        int placed = 0;
        for (Player other : online) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            put(holder, inv, slot,
                    GuiPlayerPicker.head(other, "&f" + other.getName(),
                            pickerTip(viewer, "&7Next: choose fight length", "&8(1–10 minutes)")),
                    SlotAction.page("challenge_time:uuid:" + other.getUniqueId()));
        }
        if (placed == 0) {
            put(holder, inv, 22, tipBtn(viewer, "rival.empty.no_online", Material.BARRIER, "&cNo one online",
                    List.of("&7Other players must be online")));
        }
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page("challenge"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    /** Step 2: pick challenge duration (1–10 min) for a chosen target. */
    private Inventory challengeTime(Player viewer, Player subject, String targetArg) {
        Holder holder = new Holder("challenge_time");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Rival"));
        holder.bind(inv);
        frame(inv, 45);

        String display = targetArg;
        ItemStack head;
        if (targetArg.regionMatches(true, 0, "uuid:", 0, 5)) {
            try {
                java.util.UUID id = java.util.UUID.fromString(targetArg.substring(5).trim());
                Player online = Bukkit.getPlayer(id);
                display = online != null ? online.getName() : targetArg.substring(5).trim();
                head = online != null
                        ? GuiPlayerPicker.head(online, "&f" + display,
                                pickerTip(viewer, "&7Choose fight length", "&81–10 minutes"))
                        : GuiPlayerPicker.headByName(display, "&f" + display,
                                pickerTip(viewer, "&cPlayer offline", "&7Pick someone else"));
            } catch (IllegalArgumentException e) {
                head = GuiPlayerPicker.headByName(display, "&f" + display,
                        pickerTip(viewer, "&7Choose minutes"));
            }
        } else {
            Player online = Bukkit.getPlayerExact(targetArg);
            display = online != null ? online.getName() : targetArg;
            head = online != null
                    ? GuiPlayerPicker.head(online, "&f" + display,
                            pickerTip(viewer, "&7Choose fight length"))
                    : GuiPlayerPicker.headByName(display, "&f" + display,
                            pickerTip(viewer, "&7Choose minutes"));
        }
        put(holder, inv, 4, head);

        int[] slots = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};
        for (int i = 0; i < slots.length; i++) {
            int minutes = i + 1;
            ItemStack clock = new ItemStack(Material.CLOCK, minutes);
            ItemMeta meta = clock.getItemMeta();
            if (meta != null) {
                Map<String, String> durVars = Map.of("minutes", String.valueOf(minutes));
                String fallback = "&e" + minutes + " minute" + (minutes == 1 ? "" : "s");
                meta.setDisplayName(color(GuiTooltips.name(
                        "rival.challenge.duration", fallback, durVars)));
                List<String> clockLore = new ArrayList<>();
                clockLore.add(color(""));
                clockLore.add(color("&7Challenge &f" + display));
                for (String tip : GuiTooltips.lore("rival.challenge.duration",
                        GuiBoardHelper.tips(viewer, "&8Click to send"), durVars)) {
                    clockLore.add(color(tip));
                }
                meta.setLore(clockLore);
                clock.setItemMeta(meta);
            }
            put(holder, inv, slots[i], clock,
                    SlotAction.act("challenge_send", targetArg + "@" + minutes, "challenge"));
        }

        put(holder, inv, 36, pageBtn(viewer, "rival.challenge.back_picker", Material.ARROW, "&7Back",
                "&7Pick another player"), SlotAction.page("pick_challenge"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory picker(
            Player viewer, Player subject, String action, String backPage, String title, String tip) {
        Holder holder = new Holder("pick_" + action);
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Rival"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> pickerHeader = new ArrayList<>();
        pickerHeader.add("");
        pickerHeader.add("&7Online players");
        pickerHeader.addAll(GuiBoardHelper.tips(viewer, "&8Click a head to confirm"));
        put(holder, inv, 4, item(Material.PLAYER_HEAD, title, pickerHeader));
        List<Player> online = GuiPlayerPicker.onlineExcept(subject);
        int placed = 0;
        for (Player other : online) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            put(holder, inv, slot,
                    GuiPlayerPicker.head(other, "&f" + other.getName(), pickerTip(viewer, tip)),
                    SlotAction.act(action, "uuid:" + other.getUniqueId(), backPage));
        }
        if (placed == 0) {
            put(holder, inv, 22, tipBtn(viewer, "rival.empty.no_online", Material.BARRIER, "&cNo one online",
                    List.of("&7Other players must be online")));
        }
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page(backPage));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    /** Remove picker — only current rivals (heads + hover stats). */
    private Inventory currentRivalPicker(
            Player viewer, Player subject, String action, String backPage, String title, String tip) {
        Holder holder = new Holder("pick_" + action);
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Rival"));
        holder.bind(inv);
        frame(inv, 45);
        List<GuiBoardHelper.RivalCard> cards = GuiBoardHelper.parseRivalCards(
                ForgeBridge.rivalCurrentCards(subject));
        List<String> rivalPickHeader = new ArrayList<>();
        rivalPickHeader.add("");
        rivalPickHeader.add("&7Your current rivals");
        rivalPickHeader.addAll(GuiBoardHelper.tips(viewer, "&8Click a head to " + action));
        put(holder, inv, 4, item(Material.PLAYER_HEAD, title, rivalPickHeader));
        if (cards.isEmpty()) {
            put(holder, inv, 22, tipBtn(viewer, "rival.empty.no_rivals", Material.BARRIER, "&cNo rivals",
                    List.of("&7You have no rivals to " + action)));
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(cards.size(), 21));
            for (int i = 0; i < slots.length && i < cards.size(); i++) {
                GuiBoardHelper.RivalCard card = cards.get(i);
                ItemStack head = GuiBoardHelper.rivalHead(card);
                ItemMeta meta = head.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() && meta.getLore() != null
                            ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    List<String> tipLines = pickerTip(viewer, tip);
                    if (!tipLines.isEmpty()) {
                        lore.add(color(""));
                        for (String tipLine : tipLines) {
                            lore.add(color(tipLine));
                        }
                    }
                    meta.setLore(lore);
                    head.setItemMeta(meta);
                }
                put(holder, inv, slots[i], head, SlotAction.act(action, card.pickerArg(), backPage));
            }
        }
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page(backPage));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    /** Accept/decline picker — Accept includes Declared; Decline is invite-only. */
    private Inventory pendingPicker(
            Player viewer, Player subject, String action, String backPage, String title, String tip,
            boolean acceptMode) {
        Holder holder = new Holder("pick_" + action);
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Rival"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> pendingPickHeader = new ArrayList<>();
        pendingPickHeader.add("");
        pendingPickHeader.add(acceptMode
                ? "&7Pending declares + Declared"
                : "&7Pending declares");
        pendingPickHeader.addAll(GuiBoardHelper.tips(viewer, "&8Online first · offline by name"));
        put(holder, inv, 4, item(Material.PLAYER_HEAD, title, pendingPickHeader));
        List<String> pending = acceptMode
                ? ForgeBridge.rivalAcceptCandidateArgs(subject)
                : ForgeBridge.rivalPendingIncomingDeclareArgs(subject);
        int placed = 0;
        for (String arg : pending) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            String display;
            ItemStack head;
            List<String> tipLore = new ArrayList<>(pickerTip(viewer, tip));
            if (arg.regionMatches(true, 0, "uuid:", 0, 5)) {
                try {
                    java.util.UUID id = java.util.UUID.fromString(arg.substring(5).trim());
                    Player online = Bukkit.getPlayer(id);
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
                Player online = Bukkit.getPlayerExact(arg);
                List<String> headLore = new ArrayList<>(tipLore);
                if (online != null) {
                    headLore.addAll(pickerTip(viewer, "&7Offline pending · name"));
                    head = GuiPlayerPicker.head(online, "&f" + display, headLore);
                } else {
                    headLore.addAll(pickerTip(viewer, "&8Offline — accept by name"));
                    head = GuiPlayerPicker.headByName(display, "&f" + display, headLore);
                }
            }
            put(holder, inv, slot, head, SlotAction.act(action, arg, backPage));
        }
        if (placed == 0) {
            put(holder, inv, 22, tipBtn(viewer, "rival.empty.no_pending", Material.BARRIER,
                    acceptMode ? "&eNothing to accept" : "&eNo pending declares",
                    acceptMode
                            ? List.of("&7Pending Declares and Declared rivals",
                                    "&7(both Silent) appear here.")
                            : List.of("&7When someone declares you,",
                                    "&7they appear here to decline.")));
        }
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page(backPage));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
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

    private static List<String> prependBlank(List<String> tip) {
        List<String> out = new ArrayList<>();
        out.add("");
        out.addAll(tip);
        return out;
    }

    private static void frame(Inventory inv, int size) {
        for (int i = 0; i < size; i++) {
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            inv.setItem(i, item(edge ? ACCENT : FILL, " ", List.of()));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        SlotAction slotAction = holder.actionAt(event.getSlot());
        if (slotAction == null) {
            return;
        }
        if (slotAction.shouldClose) {
            player.closeInventory();
            return;
        }
        if (slotAction.page != null) {
            final String targetPage = slotAction.page;
            Bukkit.getScheduler().runTask(plugin, () -> open(player, targetPage));
            return;
        }
        if (slotAction.rawCommand != null && !slotAction.rawCommand.isBlank()) {
            final String cmd = slotAction.rawCommand;
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                player.performCommand(cmd);
            });
            return;
        }
        if (slotAction.action == null || slotAction.action.isBlank()) {
            return;
        }
        final String ret = slotAction.returnPage == null || slotAction.returnPage.isBlank()
                ? "main" : slotAction.returnPage;
        final String action = slotAction.action;
        final String arg = slotAction.arg == null || slotAction.arg.isBlank() ? "0" : slotAction.arg;
        final Player subject = AdminInspectSessions.resolveSubject(player);
        Bukkit.getScheduler().runTask(plugin, () -> {
            String msg = ForgeBridge.rivalHandleDo(subject, action, arg, ret);
            if (msg != null && !msg.isBlank()) {
                GuiChat.sendResult(player, msg);
            }
            open(player, ret);
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private static void put(Holder holder, Inventory inv, int slot, ItemStack stack) {
        put(holder, inv, slot, stack, null);
    }

    private static void put(Holder holder, Inventory inv, int slot, ItemStack stack, SlotAction action) {
        inv.setItem(slot, stack);
        if (holder != null && action != null) {
            holder.bindAction(slot, action);
        }
    }

    private static ItemStack tipBtn(Player player, Material mat, String name, List<String> tip) {
        return tipBtn(player, null, mat, name, tip, null);
    }

    private static ItemStack tipBtn(
            Player player, String key, Material mat, String name, List<String> tip
    ) {
        return tipBtn(player, key, mat, name, tip, null);
    }

    private static ItemStack tipBtn(
            Player player, String key, Material mat, String name, List<String> tip,
            Map<String, String> vars
    ) {
        if (key == null || key.isBlank()) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            if (tip != null) {
                lore.addAll(tip);
            }
            return item(mat, name, lore);
        }
        return item(mat, GuiTooltips.name(key, name, vars), GuiTooltips.buttonLore(key, tip, vars, null));
    }

    private static ItemStack pageBtn(Player player, Material mat, String name, String... tips) {
        return pageBtn(player, null, mat, name, tips);
    }

    private static ItemStack pageBtn(Player player, String key, Material mat, String name, String... tips) {
        List<String> defaults = new ArrayList<>();
        if (tips != null) {
            for (String tip : tips) {
                if (tip != null) {
                    defaults.add(tip);
                }
            }
        }
        if (key == null || key.isBlank()) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.addAll(defaults);
            return item(mat, name, lore);
        }
        return item(mat, GuiTooltips.name(key, name), GuiTooltips.buttonLore(key, defaults));
    }

    /** Player-head tip lines, staff-only, wired through the picker's GuiTooltips key. */
    private static List<String> pickerTip(Player viewer, String... tips) {
        List<String> defaults = GuiBoardHelper.tips(viewer, tips);
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

    private static ItemStack hubBtn() {
        return item(Material.COMPASS, "&7« Hub", List.of());
    }

    private static ItemStack closeBtn() {
        return item(Material.BARRIER, "&cClose", List.of());
    }

    private static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) {
            colored.add(color(line));
        }
        meta.setLore(colored);
        stack.setItemMeta(meta);
        return stack;
    }

    private static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }

    private static final class SlotAction {
        final String action;
        final String arg;
        final String returnPage;
        final String page;
        final String rawCommand;
        final boolean shouldClose;

        private SlotAction(
                String action, String arg, String returnPage, String page, String rawCommand, boolean shouldClose) {
            this.action = action;
            this.arg = arg;
            this.returnPage = returnPage;
            this.page = page;
            this.rawCommand = rawCommand;
            this.shouldClose = shouldClose;
        }

        static SlotAction act(String action, String arg, String returnPage) {
            return new SlotAction(action, arg, returnPage, null, null, false);
        }

        static SlotAction page(String page) {
            return new SlotAction(null, null, null, page, null, false);
        }

        static SlotAction cmd(String command) {
            return new SlotAction(null, null, null, null, command, false);
        }

        static SlotAction dismiss() {
            return new SlotAction(null, null, null, null, null, true);
        }
    }

    static final class Holder implements InventoryHolder {
        final String page;
        final Map<Integer, SlotAction> actions = new HashMap<>();
        Inventory inventory;

        Holder(String page) {
            this.page = page;
        }

        void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        void bindAction(int slot, SlotAction action) {
            if (action != null) {
                actions.put(slot, action);
            }
        }

        SlotAction actionAt(int slot) {
            return actions.get(slot);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
