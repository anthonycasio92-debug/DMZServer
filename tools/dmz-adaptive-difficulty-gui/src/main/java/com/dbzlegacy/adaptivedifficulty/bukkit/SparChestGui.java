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

/** Bukkit chest GUI fallback — Legacy Mechanics Sparring. */
public final class SparChestGui implements Listener {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private final AdaptiveDifficultyGuiPlugin plugin;

    public SparChestGui(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String page) {
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String p = raw.toLowerCase(Locale.ROOT);
        Inventory inv;
        if (p.startsWith("top_") || p.startsWith("top ") || "top".equals(p) || "leaderboard".equals(p)) {
            inv = top(player, p);
        } else if ("stats".equals(p) || "statistics".equals(p)) {
            inv = detailBoard(player, "stats", "&eSpar Stats", Material.BOOK);
        } else if ("mentor".equals(p)) {
            inv = mentor(player);
        } else if ("pending".equals(p) || "invites".equals(p) || "pendinginvites".equals(p)) {
            inv = pending(player);
        } else if ("pick_apprentice".equals(p)) {
            inv = picker(player, "mentor_invite", "mentor",
                    "&aInvite Apprentice", "&7Ask them to be your apprentice");
        } else if ("pick_mentor".equals(p)) {
            inv = picker(player, "apprentice_invite", "mentor",
                    "&bAsk Mentor", "&7Ask them to be your mentor");
        } else if ("pick_accept".equals(p)) {
            inv = pendingPicker(player, "mentor_accept", "pending",
                    "&aAccept Invite", "&7Accept this mentor invite", true);
        } else if ("pick_decline".equals(p)) {
            inv = pendingPicker(player, "mentor_decline", "pending",
                    "&cDecline Invite", "&7Decline this mentor invite", false);
        } else if ("help".equals(p)) {
            inv = main(player);
        } else if ("admin".equals(p)) {
            inv = ForgeBridge.isStaff(player) ? admin(player) : main(player);
        } else {
            inv = main(player);
        }
        player.openInventory(inv);
    }

    private Inventory main(Player player) {
        Map<String, String> ph = ForgeBridge.sparPlaceholders(player);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Sparring"));
        holder.bind(inv);
        frame(inv, 45);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        if (!bridgeOk || !systemOn) {
            put(holder, inv, 4, item(Material.NETHER_STAR,
                    !bridgeOk ? "&c&lUNAVAILABLE" : "&c&lSPARRING DISABLED",
                    unavailableLore(bridgeOk)));
            put(holder, inv, 40, hubBtn(), SlotAction.cmd("lm"));
            put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        put(holder, inv, 4, item(Material.GOLDEN_SWORD, "&b&lSparring", statusLore(player, ph)));
        // Main: Status · Stats · Top · Mentor · End Session · Hub · Close (pickers on Mentor only)
        put(holder, inv, 19, pageBtn(player, Material.PAPER, "&eStats", "&7Your spar stats"),
                SlotAction.page("stats"));
        put(holder, inv, 21, pageBtn(player, Material.GOLDEN_HELMET, "&fTop", "&7Leaderboard"),
                SlotAction.page("top"));
        put(holder, inv, 23, pageBtn(player, Material.EMERALD, "&bMentor",
                "&7Invite · pending · accept · remove"), SlotAction.page("mentor"));

        boolean session = "true".equalsIgnoreCase(ph.getOrDefault("sessionActive", "false"));
        if (session) {
            put(holder, inv, 31, tipBtn(player, Material.RED_DYE, "&cEnd Session",
                    List.of("&7End your active spar session")),
                    SlotAction.act("end", "0", "main"));
        }

        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lm"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        if (ForgeBridge.isStaff(player)) {
            put(holder, inv, 37, pageBtn(player, Material.COMMAND_BLOCK, "&cAdmin",
                    "&7Save · status · mentor resetcd"), SlotAction.page("admin"));
        }
        return inv;
    }

    private Inventory top(Player player, String pageKey) {
        String cat = "tp";
        String lower = pageKey.toLowerCase(Locale.ROOT);
        if (lower.startsWith("top_")) {
            cat = lower.substring(4).trim();
        } else if (lower.startsWith("top ")) {
            cat = lower.substring(4).trim();
        }
        if (cat.isBlank() || "top".equals(cat) || "leaderboard".equals(cat)) {
            cat = "tp";
        }
        Holder holder = new Holder("top_" + cat);
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Sparring"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> raw = toAmp(ForgeBridge.sparLines(player, "top_" + cat));
        List<GuiBoardHelper.TopEntry> entries = GuiBoardHelper.parseTopEntries(raw);
        List<String> topHeader = new ArrayList<>();
        topHeader.add("");
        topHeader.addAll(GuiBoardHelper.tips(player,
                "&7Sparring leaderboard", "&8Player heads below · categories on bottom"));
        put(holder, inv, 4, item(Material.GOLDEN_HELMET, "&f&lTop — " + cat, topHeader));
        if (entries.isEmpty()) {
            put(holder, inv, 13, tipBtn(player, Material.BARRIER, "&7No sparring data yet",
                    List.of("&7Spar nearby to earn TP", "&8Categories: TP · Sessions · Perfect")));
        } else {
            // Heads on rows 1–2 only; category buttons sit on row 3 (29/31/33).
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(entries.size(), 14));
            for (int i = 0; i < slots.length && i < entries.size(); i++) {
                put(holder, inv, slots[i], GuiBoardHelper.topHead(entries.get(i)));
            }
        }
        put(holder, inv, 29, pageBtn(player, Material.GOLD_INGOT, "&eTP", "&7Total TP"),
                SlotAction.page("top_tp"));
        put(holder, inv, 31, pageBtn(player, Material.CLOCK, "&aSessions", "&7Sessions"),
                SlotAction.page("top_sessions"));
        put(holder, inv, 33, pageBtn(player, Material.NETHER_STAR, "&bPerfect", "&7Perfect spars"),
                SlotAction.page("top_perfect"));
        put(holder, inv, 36, pageBtn(player, Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory detailBoard(Player player, String page, String title, Material mat) {
        Holder holder = new Holder(page);
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Sparring"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> lore = toAmp(ForgeBridge.sparLines(player, page));
        if (lore.isEmpty()) {
            lore = List.of("&7Nothing here yet.");
        }
        List<String> detailHeader = new ArrayList<>();
        detailHeader.add("");
        detailHeader.addAll(GuiBoardHelper.tips(player, "&7One item per entry", "&8Centered below"));
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
        put(holder, inv, 36, pageBtn(player, Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory admin(Player player) {
        Holder holder = new Holder("admin");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Spar Admin"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.COMMAND_BLOCK, "&c&lSpar Admin",
                List.of("", "&7Staff-only tools",
                        "&8Save · status · mentor resetcd",
                        "&8Player menus stay on the main Spar GUI")));
        put(holder, inv, 20, tipBtn(player, Material.WRITABLE_BOOK, "&aSave",
                List.of("&7Write sparring.json", "&8/spar admin save")),
                SlotAction.act("admin", "save", "admin"));
        put(holder, inv, 22, tipBtn(player, Material.COMPASS, "&bStatus",
                List.of("&7Enabled + path", "&8/spar admin status")),
                SlotAction.act("admin", "status", "admin"));
        put(holder, inv, 24, tipBtn(player, Material.EMERALD, "&eReset Mentor CD",
                List.of("&7Clear your mentor cooldown", "&8/spar admin mentor resetcd")),
                SlotAction.act("admin", "resetcd", "admin"));
        put(holder, inv, 36, pageBtn(player, Material.ARROW, "&7Back", "&7Player Spar menu"), SlotAction.page("main"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lm"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory mentor(Player player) {
        Holder holder = new Holder("mentor");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Spar Mentor"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.EMERALD, "&b&lMentor",
                prependBlank(toAmp(ForgeBridge.sparLines(player, "mentor")))));
        put(holder, inv, 19, pageBtn(player, Material.LIME_DYE, "&aInvite apprentice…",
                "&7Pick a player to mentor"), SlotAction.page("pick_apprentice"));
        put(holder, inv, 20, pageBtn(player, Material.LIGHT_BLUE_DYE, "&bAsk mentor…",
                "&7Pick a player to ask as mentor"), SlotAction.page("pick_mentor"));
        Map<String, String> ph = ForgeBridge.sparPlaceholders(player);
        int pendingCount = 0;
        try {
            pendingCount = Integer.parseInt(ph.getOrDefault("pending_invites", "0"));
        } catch (NumberFormatException ignored) {
            pendingCount = 0;
        }
        put(holder, inv, 21, pageBtn(player, Material.CLOCK,
                pendingCount > 0 ? "&ePending &f(" + pendingCount + ")" : "&ePending",
                "&7View incoming + outgoing invites",
                pendingCount > 0 ? "&aYou have pending invites" : "&8No pending invites"),
                SlotAction.page("pending"));
        put(holder, inv, 22, pageBtn(player, Material.YELLOW_DYE, "&eAccept…",
                "&7Accept an incoming mentor invite"), SlotAction.page("pick_accept"));
        put(holder, inv, 23, pageBtn(player, Material.ORANGE_DYE, "&6Decline…",
                "&7Decline an incoming mentor invite"), SlotAction.page("pick_decline"));
        put(holder, inv, 25, tipBtn(player, Material.RED_DYE, "&cRemove bond",
                List.of("&7Leave mentor or release apprentice")),
                SlotAction.act("mentor", "remove", "mentor"));
        put(holder, inv, 36, pageBtn(player, Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory pending(Player player) {
        Holder holder = new Holder("pending");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Pending Mentor Invites"));
        holder.bind(inv);
        frame(inv, 45);
        List<GuiBoardHelper.PendingInvite> invites = GuiBoardHelper.parsePendingInvites(
                ForgeBridge.sparPendingMentorInviteCards(player));
        List<String> pendingHeader = new ArrayList<>();
        pendingHeader.add("");
        pendingHeader.add(invites.isEmpty() ? "&7No pending invites." : "&7" + invites.size() + " pending");
        pendingHeader.addAll(GuiBoardHelper.tips(player,
                "&a◀ Incoming &7= they invited you",
                "&6▶ Outgoing &7= waiting on them"));
        put(holder, inv, 4, item(Material.YELLOW_DYE, "&e&lPending Invites", pendingHeader));
        if (invites.isEmpty()) {
            put(holder, inv, 22, tipBtn(player, Material.BARRIER, "&7No pending invites",
                    List.of("&7Invite apprentice or ask a mentor",
                            "&7Incoming shows when they invite you")));
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(invites.size(), 21));
            for (int i = 0; i < slots.length && i < invites.size(); i++) {
                GuiBoardHelper.PendingInvite invite = invites.get(i);
                ItemStack head = GuiBoardHelper.pendingInviteHead(player, invite);
                if (invite.incoming) {
                    put(holder, inv, slots[i], head,
                            SlotAction.act("mentor_accept", invite.pickerArg(), "pending"));
                } else {
                    put(holder, inv, slots[i], head,
                            SlotAction.act("mentor_cancel", invite.pickerArg(), "pending"));
                }
            }
        }
        put(holder, inv, 37, pageBtn(player, Material.YELLOW_DYE, "&eAccept…",
                "&7Accept an incoming invite"), SlotAction.page("pick_accept"));
        put(holder, inv, 38, pageBtn(player, Material.ORANGE_DYE, "&6Decline…",
                "&7Decline an incoming invite"), SlotAction.page("pick_decline"));
        put(holder, inv, 39, pageBtn(player, Material.EMERALD, "&bMentor",
                "&7Full mentor menu"), SlotAction.page("mentor"));
        put(holder, inv, 36, pageBtn(player, Material.ARROW, "&7Back", "&7Mentor"), SlotAction.page("mentor"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    /** Accept/decline picker — only incoming mentor invites. */
    private Inventory pendingPicker(
            Player player, String action, String backPage, String title, String tip, boolean acceptMode) {
        Holder holder = new Holder("pick_" + (acceptMode ? "accept" : "decline"));
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Spar Mentor"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> pendingPickHeader = new ArrayList<>();
        pendingPickHeader.add("");
        pendingPickHeader.add("&7Incoming mentor invites");
        pendingPickHeader.addAll(GuiBoardHelper.tips(player, "&8Click a head to " + (acceptMode ? "accept" : "decline")));
        put(holder, inv, 4, item(Material.PLAYER_HEAD, title, pendingPickHeader));
        List<String> pending = ForgeBridge.sparPendingIncomingMentorArgs(player);
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
                    Player online = Bukkit.getPlayer(id);
                    display = online != null ? online.getName() : arg.substring(5).trim();
                    List<String> headLore = new ArrayList<>(tipLore);
                    if (online != null) {
                        headLore.add("&aOnline");
                        head = GuiPlayerPicker.head(online, "&f" + display, headLore);
                    } else {
                        head = GuiPlayerPicker.headByName(display, "&f" + display, tipLore);
                    }
                } catch (IllegalArgumentException e) {
                    display = arg;
                    head = GuiPlayerPicker.headByName(display, "&f" + display, tipLore);
                }
            } else {
                display = arg;
                Player online = Bukkit.getPlayerExact(arg);
                if (online != null) {
                    tipLore.add("&aOnline");
                    head = GuiPlayerPicker.head(online, "&f" + display, tipLore);
                } else {
                    head = GuiPlayerPicker.headByName(display, "&f" + display, tipLore);
                }
            }
            put(holder, inv, slot, head, SlotAction.act(action, arg, backPage));
        }
        if (placed == 0) {
            put(holder, inv, 22, tipBtn(player, Material.BARRIER,
                    acceptMode ? "&eNothing to accept" : "&eNo pending invites",
                    List.of("&7When someone invites you,",
                            "&7they appear here.")));
        }
        put(holder, inv, 36, pageBtn(player, Material.ARROW, "&7Back", "&7Return"), SlotAction.page(backPage));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory picker(
            Player player, String action, String backPage, String title, String tip) {
        Holder holder = new Holder("pick_" + action);
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Sparring"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> pickerHeader = new ArrayList<>();
        pickerHeader.add("");
        pickerHeader.add("&7Online players");
        pickerHeader.addAll(GuiBoardHelper.tips(player, "&8Click a head to confirm"));
        put(holder, inv, 4, item(Material.PLAYER_HEAD, title, pickerHeader));
        List<Player> online = GuiPlayerPicker.onlineExcept(player);
        int placed = 0;
        for (Player other : online) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            put(holder, inv, slot,
                    GuiPlayerPicker.head(other, "&f" + other.getName(), GuiBoardHelper.tips(player, tip)),
                    SlotAction.act(action, "uuid:" + other.getUniqueId(), backPage));
        }
        if (placed == 0) {
            put(holder, inv, 22, tipBtn(player, Material.BARRIER, "&cNo one online",
                    List.of("&7Other players must be online")));
        }
        put(holder, inv, 36, pageBtn(player, Material.ARROW, "&7Back", "&7Return"), SlotAction.page(backPage));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private static List<String> statusLore(Player player, Map<String, String> ph) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        if ("true".equalsIgnoreCase(ph.getOrDefault("sessionActive", "false"))) {
            lore.add("&aSession ACTIVE &8with &f" + blank(ph.get("partner"), "?")
                    + "  &7TP &f" + ph.getOrDefault("session_tp", "0"));
            if ("true".equalsIgnoreCase(ph.get("perfect"))) {
                lore.add("&6&lPERFECT TRAINING");
            }
        } else {
            lore.add("&7No active spar");
            lore.addAll(GuiBoardHelper.tips(player, "&7Trade hits within 30 blocks to start."));
        }
        if ("true".equalsIgnoreCase(ph.getOrDefault("mentor_bonded", "false"))) {
            lore.add("&bMentor bond &7as &f" + ph.getOrDefault("mentor_role", "?")
                    + " &8with &f" + blank(ph.get("mentor"), "?")
                    + "  &7streak &f" + ph.getOrDefault("streak", "0"));
        } else {
            lore.add("&7No mentor bond");
            lore.addAll(GuiBoardHelper.tips(player, "&8Use Mentor page to invite"));
        }
        if (ForgeBridge.isStaff(player)) {
            lore.add("");
            lore.add("&8Stats · Top · Mentor");
        }
        return lore;
    }

    private static List<String> unavailableLore(boolean bridgeOk) {
        if (!bridgeOk) {
            return List.of("", "&cForge LegacyMechanics mod unreachable",
                    "&7Check mods/ for LegacyMechanics-*.jar");
        }
        return List.of("", "&cSparring system is disabled", "&7Ask an admin if you need access");
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
            String msg = ForgeBridge.sparHandleDo(player, action, arg, ret);
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

    private static ItemStack tipBtn(Player player, Material mat, String name, List<String> tip) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(GuiBoardHelper.tipsList(player, tip));
        return item(mat, name, lore);
    }

    private static ItemStack pageBtn(Player player, Material mat, String name, String... tips) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(GuiBoardHelper.tips(player, tips));
        return item(mat, name, lore);
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
