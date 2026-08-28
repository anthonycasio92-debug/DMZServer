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
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        Inventory inv = switch (p) {
            case "list" -> list(player);
            case "pick_declare" -> picker(player, "declare", "list",
                    "&6Declare Rival", "&7Click to declare this player");
            case "pick_accept" -> pendingPicker(player, "accept", "list",
                    "&aAccept Declare", "&7Click to accept their declare");
            case "pick_decline" -> pendingPicker(player, "decline", "list",
                    "&cDecline Declare", "&7Click to decline their declare");
            case "pick_remove" -> picker(player, "remove", "list",
                    "&cRemove Rival", "&7Click to remove this rivalry");
            case "pick_challenge" -> picker(player, "challenge_send", "challenge",
                    "&cSend Challenge", "&7Click to challenge (1 min)");
            case "pick_spectate" -> picker(player, "spectate", "challenge",
                    "&bSpectate", "&7Watch their active challenge");
            case "pick_silent" -> picker(player, "silent", "list",
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
                "&7Your rivals", "&8Declare · accept · remove"), SlotAction.page("list"));
        put(holder, inv, 21, pageBtn(Material.IRON_SWORD, "&cChallenge",
                "&7Send · accept · decline · spectate"), SlotAction.page("challenge"));
        put(holder, inv, 23, pageBtn(Material.GOLDEN_HELMET, "&fTop",
                "&7RP leaderboard"), SlotAction.page("top"));
        put(holder, inv, 25, pageBtn(Material.WRITABLE_BOOK, "&bProgress",
                "&7Season · quests · achs · HOF · journal · title"), SlotAction.page("progress"));

        boolean tpOn = "true".equalsIgnoreCase(ph.getOrDefault("tpMsg", "false"));
        put(holder, inv, 29, tipBtn(
                tpOn ? Material.BELL : Material.PAPER,
                tpOn ? "&aTP Msg ON" : "&8TP Msg OFF",
                List.of(
                        tpOn ? "&7Click to mute rival TP messages" : "&7Click to show rival TP messages",
                        "&8Only affects rivalry TP chat"
                )), SlotAction.act("tpmsg", "toggle", "main"));

        boolean instinctFeature = "true".equalsIgnoreCase(ph.getOrDefault("instinct_feature", "false"));
        if (instinctFeature) {
            boolean instinctOn = "true".equalsIgnoreCase(ph.getOrDefault("instinct", "false"));
            put(holder, inv, 31, tipBtn(
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
                List.of("", "&7Staff tools for the Rival system",
                        "&8Click a button to run the linked command")));
        put(holder, inv, 19, tipBtn(Material.WRITABLE_BOOK, "&aSave",
                List.of("&7Write rivalry-v4 + progression-v4", "&8/rival admin save")),
                SlotAction.cmd("rival admin save"));
        put(holder, inv, 21, tipBtn(Material.CLOCK, "&eRefresh",
                List.of("&7Reload stores from disk", "&8/rival admin refresh")),
                SlotAction.cmd("rival admin refresh"));
        put(holder, inv, 23, tipBtn(Material.COMPASS, "&bStatus",
                List.of("&7Enabled + path summary", "&8/rival admin status")),
                SlotAction.cmd("rival admin status"));
        put(holder, inv, 25, pageBtn(Material.NETHER_STAR, "&fOpen Rival GUI",
                "&7Player rival menu"), SlotAction.page("main"));
        put(holder, inv, 29, tipBtn(Material.PAPER, "&7Help (chat)",
                List.of("&7Print admin command list", "&8/rival admin help")),
                SlotAction.cmd("rival admin help"));
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
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
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> lore = toAmp(ForgeBridge.rivalLines(player, "list"));
        put(holder, inv, 4, item(Material.PLAYER_HEAD, "&6&lRivals", prependBlank(
                lore.isEmpty() ? List.of("&7No rivals yet.") : lore)));
        put(holder, inv, 19, pageBtn(Material.LIME_CONCRETE, "&aDeclare…",
                "&7Pick an online player to declare"), SlotAction.page("pick_declare"));
        put(holder, inv, 21, pageBtn(Material.YELLOW_CONCRETE, "&eAccept…",
                "&7Pending declares (no name guessing)"), SlotAction.page("pick_accept"));
        put(holder, inv, 22, pageBtn(Material.ORANGE_CONCRETE, "&6Decline…",
                "&7Decline a pending declare"), SlotAction.page("pick_decline"));
        put(holder, inv, 23, pageBtn(Material.RED_CONCRETE, "&cRemove…",
                "&7Pick a rival to remove"), SlotAction.page("pick_remove"));
        put(holder, inv, 25, pageBtn(Material.GRAY_CONCRETE, "&8Silent…",
                "&7Pick a player for silent rival"), SlotAction.page("pick_silent"));
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
                "&7Pick an online rival to challenge"), SlotAction.page("pick_challenge"));
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

    /** Accept/decline picker — pending incoming declares (online first; offline by name). */
    private Inventory pendingPicker(
            Player player, String action, String backPage, String title, String tip) {
        Holder holder = new Holder("pick_" + action);
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.PLAYER_HEAD, title,
                List.of("", "&7Pending declares", "&8Online first · offline by name")));
        List<String> pending = ForgeBridge.rivalPendingIncomingDeclareArgs(player);
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
            put(holder, inv, 22, tipBtn(Material.BARRIER, "&eNo pending declares",
                    List.of("&7When someone declares you,", "&7they appear here to accept or decline.")));
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
        lore.add("&8List · Challenge · Top · Progress");
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
                if (!msg.startsWith("§")) {
                    msg = "§a" + msg;
                }
                player.sendMessage(msg);
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
