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
            case "stats", "statistics" -> lines(player, "stats", "&eRival Stats", Material.BOOK);
            case "challenge", "challenges" -> challenge(player);
            case "top", "leaderboard" -> lines(player, "top", "&fRP Top", Material.GOLDEN_HELMET);
            case "season" -> lines(player, "season", "&aSeason", Material.CLOCK);
            case "quests", "quest" -> lines(player, "quests", "&bQuests", Material.WRITABLE_BOOK);
            case "achievements", "achs", "ach" ->
                    lines(player, "achievements", "&dAchievements", Material.DIAMOND);
            case "hof", "hall" -> lines(player, "hof", "&6Hall of Fame", Material.GOLD_BLOCK);
            case "journal" -> lines(player, "journal", "&fJournal", Material.MAP);
            case "title", "titles" -> lines(player, "title", "&eTitle", Material.NAME_TAG);
            case "help" -> lines(player, "help", "&7Help", Material.PAPER);
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
        put(holder, inv, 10, pageBtn(Material.LIME_CONCRETE, "&aDeclare…",
                "&7Pick an online player to declare"), SlotAction.page("pick_declare"));
        put(holder, inv, 12, pageBtn(Material.GOLDEN_SWORD, "&eSend Challenge…",
                "&7Pick an online rival to challenge"), SlotAction.page("pick_challenge"));
        put(holder, inv, 19, pageBtn(Material.PLAYER_HEAD, "&6List",
                "&7Your rivals", "&8Declare · accept · remove"), SlotAction.page("list"));
        put(holder, inv, 20, pageBtn(Material.BOOK, "&eStats",
                "&7Career stats", "&8Wins · losses · RP"), SlotAction.page("stats"));
        put(holder, inv, 21, pageBtn(Material.IRON_SWORD, "&cChallenge",
                "&7Challenge controls", "&8Send · accept · decline"), SlotAction.page("challenge"));
        put(holder, inv, 22, pageBtn(Material.GOLDEN_HELMET, "&fTop",
                "&7RP leaderboard"), SlotAction.page("top"));
        put(holder, inv, 23, pageBtn(Material.CLOCK, "&aSeason",
                "&7Season RP"), SlotAction.page("season"));
        put(holder, inv, 24, pageBtn(Material.WRITABLE_BOOK, "&bQuests",
                "&7Weekly quests"), SlotAction.page("quests"));
        put(holder, inv, 25, pageBtn(Material.DIAMOND, "&dAchs",
                "&7Achievements"), SlotAction.page("achievements"));
        put(holder, inv, 28, pageBtn(Material.GOLD_BLOCK, "&6HOF",
                "&7Hall of Fame"), SlotAction.page("hof"));
        put(holder, inv, 29, pageBtn(Material.MAP, "&fJournal",
                "&7Battle journal"), SlotAction.page("journal"));
        put(holder, inv, 30, pageBtn(Material.NAME_TAG, "&eTitle",
                "&7Rival title"), SlotAction.page("title"));

        boolean tpOn = "true".equalsIgnoreCase(ph.getOrDefault("tpMsg", "false"));
        put(holder, inv, 32, tipBtn(
                tpOn ? Material.BELL : Material.PAPER,
                tpOn ? "&aTP Msg ON" : "&8TP Msg OFF",
                List.of(
                        tpOn ? "&7Click to mute rival TP messages" : "&7Click to show rival TP messages",
                        "&8Only affects rivalry TP chat"
                )), SlotAction.act("tpmsg", "toggle", "main"));

        boolean instinctFeature = "true".equalsIgnoreCase(ph.getOrDefault("instinct_feature", "false"));
        if (instinctFeature) {
            boolean instinctOn = "true".equalsIgnoreCase(ph.getOrDefault("instinct", "false"));
            put(holder, inv, 33, tipBtn(
                    instinctOn ? Material.LIME_DYE : Material.GRAY_DYE,
                    instinctOn ? "&aInstinct ON" : "&8Instinct OFF",
                    List.of(
                            instinctOn ? "&7Click to disable Rival Instinct" : "&7Click to enable Rival Instinct",
                            "&8Alerts for mutual / nemesis rivals"
                    )), SlotAction.act("instinct", "toggle", "main"));
        }

        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lm"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
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

    private Inventory lines(Player player, String page, String title, Material mat) {
        Holder holder = new Holder(page);
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Rival"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> lore = toAmp(ForgeBridge.rivalLines(player, page));
        put(holder, inv, 4, item(mat, title, prependBlank(
                lore.isEmpty() ? List.of("&7Nothing here yet.") : lore)));
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
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
        lore.add("&8Browse pages below · click to declare");
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
