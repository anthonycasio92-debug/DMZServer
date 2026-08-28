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

    public void open(Player player, String page) {
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("challenge_time:")) {
            player.openInventory(challengeTime(player, raw.substring("challenge_time:".length()).trim()));
            return;
        }
        Inventory inv = switch (lower) {
            case "list" -> list(player);
            case "actions" -> actions(player);
            case "pending", "invites", "pendinginvites" -> pending(player);
            case "history", "past", "previous" -> history(player);
            case "pick_declare" -> picker(player, "declare", "actions",
                    "&6Declare Rival", "&7Click to declare this player");
            case "pick_accept" -> pendingPicker(player, "accept", "actions",
                    "&aAccept Rivalry", "&7Pending declare or Declared → Mutual", true);
            case "pick_decline" -> pendingPicker(player, "decline", "actions",
                    "&cDecline Declare", "&7Click to decline their declare", false);
            case "pick_remove" -> currentRivalPicker(player, "remove", "actions",
                    "&cRemove Rival", "&7Click to remove this rivalry");
            case "pick_challenge" -> challengeTargetPicker(player);
            case "pick_spectate" -> picker(player, "spectate", "challenge",
                    "&bSpectate", "&7Watch their active challenge");
            case "pick_silent" -> picker(player, "silent", "actions",
                    "&8Silent Rival", "&7Click for silent rivalry");
            case "stats", "statistics" -> detailBoard(player, "stats", "&eRival Stats", Material.BOOK, "progress");
            case "challenge", "challenges" -> challenge(player);
            case "top", "leaderboard" -> topBoard(player, "top", "&fRP Top", "main");
            case "progress" -> progress(player);
            case "season" -> detailBoard(player, "season", "&aSeason", Material.CLOCK, "progress");
            case "quests", "quest" -> detailBoard(player, "quests", "&bQuests", Material.WRITABLE_BOOK, "progress");
            case "achievements", "achs", "ach" ->
                    detailBoard(player, "achievements", "&dAchievements", Material.DIAMOND, "progress");
            case "hof", "hall" -> detailBoard(player, "hof", "&6Hall of Fame", Material.GOLD_BLOCK, "progress");
            case "journal" -> detailBoard(player, "journal", "&fJournal", Material.MAP, "progress");
            case "title", "titles" -> detailBoard(player, "title", "&eTitle", Material.NAME_TAG, "progress");
            case "help" -> detailBoard(player, "help", "&7Help", Material.PAPER, "main");
            case "admin" -> ForgeBridge.isStaff(player) ? admin(player) : main(player);
            default -> main(player);
        };
        player.openInventory(inv);
    }

    private Inventory main(Player player) {
        Map<String, String> ph = ForgeBridge.rivalPlaceholders(player);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival"));
        holder.bind(inv);
        frame(inv, 45);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        if (!bridgeOk || !systemOn) {
            put(holder, inv, 4, item(Material.NETHER_STAR,
                    !bridgeOk ? "&c&lUNAVAILABLE" : "&c&lRIVAL DISABLED",
                    unavailableLore(bridgeOk)));
            put(holder, inv, 40, hubBtn(), SlotAction.cmd("lm"));
            put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        put(holder, inv, 4, item(Material.NETHER_STAR, "&f&lRival", statusLore(ph)));
        put(holder, inv, 19, pageBtn(Material.PLAYER_HEAD, "&6List",
                "&7Current rivals", "&8Heads · hover for stats"), SlotAction.page("list"));
        int pendingCount = 0;
        try {
            pendingCount = Integer.parseInt(ph.getOrDefault("pending_invites", "0"));
        } catch (NumberFormatException ignored) {
            pendingCount = 0;
        }
        put(holder, inv, 21, pageBtn(Material.LIME_CONCRETE, "&aActions",
                "&7Declare · accept · decline · remove",
                pendingCount > 0
                        ? "&e" + pendingCount + " pending invite" + (pendingCount == 1 ? "" : "s")
                        : "&8Pending invites live here"),
                SlotAction.page("actions"));
        put(holder, inv, 23, pageBtn(Material.IRON_SWORD, "&cChallenge",
                "&7Send · accept · decline · spectate"), SlotAction.page("challenge"));
        put(holder, inv, 25, pageBtn(Material.GOLDEN_HELMET, "&fTop",
                "&7RP leaderboard"), SlotAction.page("top"));
        put(holder, inv, 29, pageBtn(Material.SKELETON_SKULL, "&8History",
                "&7Previous rivals", "&8Archived when removed"), SlotAction.page("history"));
        put(holder, inv, 31, pageBtn(Material.WRITABLE_BOOK, "&bProgress",
                "&7Season · quests · achs · HOF · journal · title"), SlotAction.page("progress"));

        boolean tpOn = "true".equalsIgnoreCase(ph.getOrDefault("tpMsg", "false"));
        put(holder, inv, 33, tipBtn(
                tpOn ? Material.BELL : Material.PAPER,
                tpOn ? "&aTP Msg ON" : "&8TP Msg OFF",
                List.of(
                        tpOn ? "&7Click to mute rival TP messages" : "&7Click to show rival TP messages",
                        "&8Only affects rivalry TP chat"
                )), SlotAction.act("tpmsg", "toggle", "main"));

        boolean instinctFeature = "true".equalsIgnoreCase(ph.getOrDefault("instinct_feature", "false"));
        if (instinctFeature) {
            boolean instinctOn = "true".equalsIgnoreCase(ph.getOrDefault("instinct", "false"));
            put(holder, inv, 34, tipBtn(
                    instinctOn ? Material.LIME_DYE : Material.GRAY_DYE,
                    instinctOn ? "&aInstinct ON" : "&8Instinct OFF",
                    List.of(
                            instinctOn ? "&7Click to disable Rival Instinct" : "&7Click to enable Rival Instinct",
                            "&8Alerts for mutual / nemesis rivals"
                    )), SlotAction.act("instinct", "toggle", "main"));
        }

        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lm"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        if (ForgeBridge.isStaff(player)) {
            put(holder, inv, 37, pageBtn(Material.REDSTONE, "&cAdmin",
                    "&7Save · refresh · status · commands"), SlotAction.page("admin"));
        }
        return inv;
    }

    private Inventory progress(Player player) {
        Holder holder = new Holder("progress");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival Progress"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.WRITABLE_BOOK, "&b&lProgress",
                List.of("", "&7Each section is its own board",
                        "&7Stats · Season · Quests · Achs · HOF · Journal · Title")));

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
            lore.add("&eClick to open");
            put(holder, inv, slots[i], tipBtn(mats[i], titles[i], lore), SlotAction.page(pages[i]));
        }
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory topBoard(Player player, String page, String title, String backPage) {
        Holder holder = new Holder(page);
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival"));
        holder.bind(inv);
        frame(inv, 45);
        String back = backPage == null || backPage.isBlank() ? "main" : backPage;
        List<String> raw = toAmp(ForgeBridge.rivalLines(player, page));
        List<GuiBoardHelper.TopEntry> entries = GuiBoardHelper.parseTopEntries(raw);
        put(holder, inv, 4, item(Material.GOLDEN_HELMET, title,
                List.of("", "&7Top rivals by RP", "&8Player heads below")));
        if (entries.isEmpty()) {
            put(holder, inv, 22, tipBtn(Material.BARRIER, "&7No rivalry data yet",
                    List.of("&7Challenge rivals to earn RP")));
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(entries.size(), 21));
            for (int i = 0; i < slots.length && i < entries.size(); i++) {
                put(holder, inv, slots[i], GuiBoardHelper.topHead(entries.get(i)));
            }
        }
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page(back));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory detailBoard(Player player, String page, String title, Material mat, String backPage) {
        Holder holder = new Holder(page);
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival"));
        holder.bind(inv);
        frame(inv, 45);
        String back = backPage == null || backPage.isBlank() ? "main" : backPage;
        List<String> lore = toAmp(ForgeBridge.rivalLines(player, page));
        if (lore.isEmpty()) {
            lore = List.of("&7Nothing here yet.", "&8Data: config/legacymechanics/");
        }
        put(holder, inv, 4, item(mat, title, List.of("", "&7One item per entry", "&8Centered below")));
        List<GuiBoardHelper.DetailTile> tiles = GuiBoardHelper.detailTiles(lore);
        int[] slots = GuiBoardHelper.centeredSlots(Math.min(tiles.size(), 21));
        for (int i = 0; i < slots.length && i < tiles.size(); i++) {
            GuiBoardHelper.DetailTile tile = tiles.get(i);
            List<String> tip = new ArrayList<>();
            tip.add("");
            tip.addAll(tile.lore);
            put(holder, inv, slots[i], tipBtn(tile.icon, tile.title, tip));
        }
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page(back));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory admin(Player player) {
        Holder holder = new Holder("admin");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival Admin"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.REDSTONE, "&c&lRival Admin",
                List.of("", "&7Staff-only tools",
                        "&8Save · refresh · status · help",
                        "&8Player menus stay on the main Rival GUI")));
        put(holder, inv, 20, tipBtn(Material.WRITABLE_BOOK, "&aSave",
                List.of("&7Write rivalry-v4 + progression-v4", "&8/rival admin save")),
                SlotAction.act("admin", "save", "admin"));
        put(holder, inv, 22, tipBtn(Material.CLOCK, "&eRefresh",
                List.of("&7Reload stores from disk", "&8/rival admin refresh")),
                SlotAction.act("admin", "refresh", "admin"));
        put(holder, inv, 24, tipBtn(Material.COMPASS, "&bStatus",
                List.of("&7Enabled + path summary", "&8/rival admin status")),
                SlotAction.act("admin", "status", "admin"));
        put(holder, inv, 30, tipBtn(Material.PAPER, "&7Admin Help",
                List.of("&7Print admin command list", "&8/rival admin help")),
                SlotAction.act("admin", "help", "admin"));
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Player Rival menu"), SlotAction.page("main"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lm"));
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

    private Inventory list(Player player) {
        Holder holder = new Holder("list");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival List"));
        holder.bind(inv);
        frame(inv, 45);
        List<GuiBoardHelper.RivalCard> cards = GuiBoardHelper.parseRivalCards(
                ForgeBridge.rivalCurrentCards(player));
        put(holder, inv, 4, item(Material.PLAYER_HEAD, "&6&lCurrent Rivals",
                List.of("",
                        cards.isEmpty() ? "&7No rivals yet." : "&7" + cards.size() + " rival(s)",
                        "&8Hover a head for stats",
                        "&8Manage relationships in Actions")));
        if (cards.isEmpty()) {
            put(holder, inv, 22, tipBtn(Material.BARRIER, "&7No rivals yet",
                    List.of("&7Use Actions → Declare to start")));
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(cards.size(), 21));
            for (int i = 0; i < slots.length && i < cards.size(); i++) {
                put(holder, inv, slots[i], GuiBoardHelper.rivalHead(cards.get(i)));
            }
        }
        put(holder, inv, 37, pageBtn(Material.LIME_CONCRETE, "&aActions",
                "&7Declare · accept · remove · pending"), SlotAction.page("actions"));
        put(holder, inv, 39, pageBtn(Material.SKELETON_SKULL, "&8History",
                "&7Previous rivals"), SlotAction.page("history"));
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory pending(Player player) {
        Holder holder = new Holder("pending");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Pending Invites"));
        holder.bind(inv);
        frame(inv, 45);
        List<GuiBoardHelper.PendingInvite> invites = GuiBoardHelper.parsePendingInvites(
                ForgeBridge.rivalPendingInviteCards(player));
        put(holder, inv, 4, item(Material.YELLOW_DYE, "&e&lPending Invites",
                List.of("",
                        invites.isEmpty() ? "&7No pending declares." : "&7" + invites.size() + " pending",
                        "&a◀ Incoming &7= they Declared you",
                        "&6▶ Outgoing &7= waiting on them")));
        if (invites.isEmpty()) {
            put(holder, inv, 22, tipBtn(Material.BARRIER, "&7No pending invites",
                    List.of("&7Declare someone to send an invite",
                            "&7Incoming shows when they Declare you")));
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(invites.size(), 21));
            for (int i = 0; i < slots.length && i < invites.size(); i++) {
                GuiBoardHelper.PendingInvite invite = invites.get(i);
                ItemStack head = GuiBoardHelper.pendingInviteHead(invite);
                if (invite.incoming) {
                    put(holder, inv, slots[i], head, SlotAction.act("accept", invite.pickerArg(), "pending"));
                } else {
                    put(holder, inv, slots[i], head);
                }
            }
        }
        put(holder, inv, 37, pageBtn(Material.YELLOW_DYE, "&eAccept…",
                "&7Accept incoming / Declared"), SlotAction.page("pick_accept"));
        put(holder, inv, 38, pageBtn(Material.ORANGE_DYE, "&6Decline…",
                "&7Decline an incoming declare"), SlotAction.page("pick_decline"));
        put(holder, inv, 39, pageBtn(Material.LIME_CONCRETE, "&aActions",
                "&7Full actions menu"), SlotAction.page("actions"));
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Actions"), SlotAction.page("actions"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory actions(Player player) {
        Holder holder = new Holder("actions");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival Actions"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.LIME_CONCRETE, "&a&lRival Actions",
                prependBlank(toAmp(ForgeBridge.rivalLines(player, "actions")))));
        put(holder, inv, 19, pageBtn(Material.LIME_DYE, "&aDeclare…",
                "&7Visible declare → they Accept → Mutual"), SlotAction.page("pick_declare"));
        Map<String, String> ph = ForgeBridge.rivalPlaceholders(player);
        int pendingCount = 0;
        try {
            pendingCount = Integer.parseInt(ph.getOrDefault("pending_invites", "0"));
        } catch (NumberFormatException ignored) {
            pendingCount = 0;
        }
        put(holder, inv, 20, pageBtn(Material.CLOCK,
                pendingCount > 0 ? "&ePending &f(" + pendingCount + ")" : "&ePending",
                "&7View incoming + outgoing invites",
                pendingCount > 0 ? "&aYou have pending invites" : "&8No pending invites"),
                SlotAction.page("pending"));
        put(holder, inv, 21, pageBtn(Material.YELLOW_DYE, "&eAccept…",
                "&7Pending declares, or Declared → Mutual",
                "&8Both Silent → Declared shows here"), SlotAction.page("pick_accept"));
        put(holder, inv, 22, pageBtn(Material.ORANGE_DYE, "&6Decline…",
                "&7Decline a pending declare"), SlotAction.page("pick_decline"));
        put(holder, inv, 23, pageBtn(Material.RED_DYE, "&cRemove…",
                "&7Pick one of your rivals to remove"), SlotAction.page("pick_remove"));
        put(holder, inv, 25, pageBtn(Material.GRAY_DYE, "&8Silent…",
                "&7One-sided Unknown (they are not told)",
                "&8Both Silent → Declared (both notified)"), SlotAction.page("pick_silent"));
        put(holder, inv, 37, pageBtn(Material.PLAYER_HEAD, "&6List",
                "&7Back to current rivals"), SlotAction.page("list"));
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory history(Player player) {
        Holder holder = new Holder("history");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival History"));
        holder.bind(inv);
        frame(inv, 45);
        List<GuiBoardHelper.RivalCard> cards = GuiBoardHelper.parseRivalCards(
                ForgeBridge.rivalPastCards(player));
        put(holder, inv, 4, item(Material.SKELETON_SKULL, "&8&lPrevious Rivals",
                List.of("",
                        cards.isEmpty() ? "&7No previous rivals yet." : "&7" + cards.size() + " archived",
                        "&8Removed rivalries appear here",
                        "&8Hover a head for final stats")));
        if (cards.isEmpty()) {
            put(holder, inv, 22, tipBtn(Material.BARRIER, "&7No history yet",
                    List.of("&7Removed rivals show here")));
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(cards.size(), 21));
            for (int i = 0; i < slots.length && i < cards.size(); i++) {
                put(holder, inv, slots[i], GuiBoardHelper.rivalHead(cards.get(i)));
            }
        }
        put(holder, inv, 37, pageBtn(Material.PLAYER_HEAD, "&6List",
                "&7Current rivals"), SlotAction.page("list"));
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory challenge(Player player) {
        Holder holder = new Holder("challenge");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.IRON_SWORD, "&c&lChallenge",
                prependBlank(toAmp(ForgeBridge.rivalLines(player, "challenge")))));
        put(holder, inv, 19, pageBtn(Material.GOLDEN_SWORD, "&eSend Challenge…",
                "&7Pick rival, then choose 1–10 minutes"), SlotAction.page("pick_challenge"));
        put(holder, inv, 21, tipBtn(Material.LIME_CONCRETE, "&aAccept",
                List.of("&7Accept pending challenge")),
                SlotAction.act("challenge", "accept", "challenge"));
        put(holder, inv, 23, tipBtn(Material.RED_CONCRETE, "&cDecline",
                List.of("&7Decline pending challenge")),
                SlotAction.act("challenge", "decline", "challenge"));
        put(holder, inv, 25, tipBtn(Material.GRAY_CONCRETE, "&8Cancel",
                List.of("&7Cancel your outgoing challenge")),
                SlotAction.act("challenge", "cancel", "challenge"));
        put(holder, inv, 29, pageBtn(Material.ENDER_EYE, "&bSpectate…",
                "&7Watch an online player's challenge"), SlotAction.page("pick_spectate"));
        put(holder, inv, 31, tipBtn(Material.GRAY_DYE, "&8Stop Spectate",
                List.of("&7End spectating early")),
                SlotAction.act("spectate_stop", "0", "challenge"));
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    /** Step 1: pick who to challenge — opens duration picker next. */
    private Inventory challengeTargetPicker(Player player) {
        Holder holder = new Holder("pick_challenge");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.GOLDEN_SWORD, "&cSend Challenge",
                List.of("", "&7Online rivals", "&8Click a head, then pick duration")));
        List<Player> online = GuiPlayerPicker.onlineExcept(player);
        int placed = 0;
        for (Player other : online) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            put(holder, inv, slot,
                    GuiPlayerPicker.head(other, "&f" + other.getName(),
                            List.of("&7Next: choose fight length", "&8(1–10 minutes)")),
                    SlotAction.page("challenge_time:uuid:" + other.getUniqueId()));
        }
        if (placed == 0) {
            put(holder, inv, 22, tipBtn(Material.BARRIER, "&cNo one online",
                    List.of("&7Other players must be online")));
        }
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("challenge"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    /** Step 2: pick challenge duration (1–10 min) for a chosen target. */
    private Inventory challengeTime(Player player, String targetArg) {
        Holder holder = new Holder("challenge_time");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival"));
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
                                List.of("&7Choose fight length", "&81–10 minutes"))
                        : GuiPlayerPicker.headByName(display, "&f" + display,
                                List.of("&cPlayer offline", "&7Pick someone else"));
            } catch (IllegalArgumentException e) {
                head = GuiPlayerPicker.headByName(display, "&f" + display, List.of("&7Choose minutes"));
            }
        } else {
            Player online = Bukkit.getPlayerExact(targetArg);
            display = online != null ? online.getName() : targetArg;
            head = online != null
                    ? GuiPlayerPicker.head(online, "&f" + display, List.of("&7Choose fight length"))
                    : GuiPlayerPicker.headByName(display, "&f" + display, List.of("&7Choose minutes"));
        }
        put(holder, inv, 4, head);

        int[] slots = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};
        for (int i = 0; i < slots.length; i++) {
            int minutes = i + 1;
            ItemStack clock = new ItemStack(Material.CLOCK, minutes);
            ItemMeta meta = clock.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color("&e" + minutes + " minute" + (minutes == 1 ? "" : "s")));
                meta.setLore(List.of(
                        color(""),
                        color("&7Challenge &f" + display),
                        color("&8Click to send")
                ));
                clock.setItemMeta(meta);
            }
            put(holder, inv, slots[i], clock,
                    SlotAction.act("challenge_send", targetArg + "@" + minutes, "challenge"));
        }

        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Pick another player"),
                SlotAction.page("pick_challenge"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory picker(
            Player player, String action, String backPage, String title, String tip) {
        Holder holder = new Holder("pick_" + action);
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.PLAYER_HEAD, title,
                List.of("", "&7Online players", "&8Click a head to confirm")));
        List<Player> online = GuiPlayerPicker.onlineExcept(player);
        int placed = 0;
        for (Player other : online) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            put(holder, inv, slot,
                    GuiPlayerPicker.head(other, "&f" + other.getName(), List.of(tip)),
                    SlotAction.act(action, "uuid:" + other.getUniqueId(), backPage));
        }
        if (placed == 0) {
            put(holder, inv, 22, tipBtn(Material.BARRIER, "&cNo one online",
                    List.of("&7Other players must be online")));
        }
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page(backPage));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    /** Remove picker — only current rivals (heads + hover stats). */
    private Inventory currentRivalPicker(
            Player player, String action, String backPage, String title, String tip) {
        Holder holder = new Holder("pick_" + action);
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival"));
        holder.bind(inv);
        frame(inv, 45);
        List<GuiBoardHelper.RivalCard> cards = GuiBoardHelper.parseRivalCards(
                ForgeBridge.rivalCurrentCards(player));
        put(holder, inv, 4, item(Material.PLAYER_HEAD, title,
                List.of("", "&7Your current rivals", "&8Click a head to " + action)));
        if (cards.isEmpty()) {
            put(holder, inv, 22, tipBtn(Material.BARRIER, "&cNo rivals",
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
                    lore.add(color(""));
                    lore.add(color(tip));
                    meta.setLore(lore);
                    head.setItemMeta(meta);
                }
                put(holder, inv, slots[i], head, SlotAction.act(action, card.pickerArg(), backPage));
            }
        }
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page(backPage));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    /** Accept/decline picker — Accept includes Declared; Decline is invite-only. */
    private Inventory pendingPicker(
            Player player, String action, String backPage, String title, String tip,
            boolean acceptMode) {
        Holder holder = new Holder("pick_" + action);
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.PLAYER_HEAD, title,
                List.of("", acceptMode
                                ? "&7Pending declares + Declared"
                                : "&7Pending declares",
                        "&8Online first · offline by name")));
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
            if (arg.regionMatches(true, 0, "uuid:", 0, 5)) {
                try {
                    java.util.UUID id = java.util.UUID.fromString(arg.substring(5).trim());
                    Player online = Bukkit.getPlayer(id);
                    display = online != null ? online.getName() : arg.substring(5).trim();
                    head = online != null
                            ? GuiPlayerPicker.head(online, "&f" + display, List.of(tip, "&aOnline"))
                            : GuiPlayerPicker.headByName(display, "&f" + display, List.of(tip));
                } catch (IllegalArgumentException e) {
                    display = arg;
                    head = GuiPlayerPicker.headByName(display, "&f" + display, List.of(tip));
                }
            } else {
                display = arg;
                Player online = Bukkit.getPlayerExact(arg);
                head = online != null
                        ? GuiPlayerPicker.head(online, "&f" + display, List.of(tip, "&7Offline pending · name"))
                        : GuiPlayerPicker.headByName(display, "&f" + display,
                                List.of(tip, "&8Offline — accept by name"));
            }
            put(holder, inv, slot, head, SlotAction.act(action, arg, backPage));
        }
        if (placed == 0) {
            put(holder, inv, 22, tipBtn(Material.BARRIER,
                    acceptMode ? "&eNothing to accept" : "&eNo pending declares",
                    acceptMode
                            ? List.of("&7Pending Declares and Declared rivals",
                                    "&7(both Silent) appear here.")
                            : List.of("&7When someone declares you,",
                                    "&7they appear here to decline.")));
        }
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page(backPage));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private static List<String> statusLore(Map<String, String> ph) {
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
        lore.add("&8List · Actions · Challenge · Top · Progress");
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
        Bukkit.getScheduler().runTask(plugin, () -> {
            String msg = ForgeBridge.rivalHandleDo(player, action, arg, ret);
            if (msg != null && !msg.isBlank()) {
                for (String line : msg.split("\n")) {
                    if (line == null || line.isBlank()) {
                        continue;
                    }
                    player.sendMessage(line.startsWith("§") ? line : "§a" + line);
                }
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

    private static ItemStack tipBtn(Material mat, String name, List<String> tip) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(tip);
        return item(mat, name, lore);
    }

    private static ItemStack pageBtn(Material mat, String name, String... tips) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String tip : tips) {
            lore.add(tip);
        }
        return item(mat, name, lore);
    }

    private static ItemStack hubBtn() {
        return item(Material.COMPASS, "&7« Hub", List.of("", "&7Legacy Mechanics hub"));
    }

    private static ItemStack closeBtn() {
        return item(Material.BARRIER, "&cClose", List.of("&7Close menu"));
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
