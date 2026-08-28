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

/** Bukkit chest GUI fallback — Legacy Mechanics Hub. */
public final class HubChestGui implements Listener {
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private final AdaptiveDifficultyGuiPlugin plugin;

    public HubChestGui(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        Inventory inv = switch (p) {
            case "help" -> chunked(player, "help", "&7Help", Material.PAPER);
            case "logs", "syslog" -> ForgeBridge.isStaff(player)
                    ? logs(player)
                    : main(player);
            default -> main(player);
        };
        player.openInventory(inv);
    }

    private Inventory main(Player player) {
        Map<String, String> ph = ForgeBridge.hubPlaceholders(player);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 54, color("&8Legacy Mechanics"));
        holder.bind(inv);
        frameOnly(inv, 54);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        if (!bridgeOk) {
            put(holder, inv, 4, item(Material.NETHER_STAR, "&c&lUNAVAILABLE",
                    List.of("", "&cForge LegacyMechanics mod unreachable",
                            "&7Check mods/ for LegacyMechanics-*.jar")));
            put(holder, inv, 53, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        put(holder, inv, 4, item(Material.NETHER_STAR, "&f&lLegacy Mechanics",
                List.of("", "&7Pick a system below", "&8Use &f/lm &8for this hub")));
        put(holder, inv, 19, tipBtn(Material.DIAMOND_SWORD, "&aDifficulty",
                List.of("&7Unlock tiers & world scaling", "&8Tip: buy unlocks when ready")),
                SlotAction.open("difficulty"));
        put(holder, inv, 21, tipBtn(Material.IRON_SWORD, "&6Rival",
                List.of("&7Rivalry, challenges & RP", "&8Tip: declare rivals from List")),
                SlotAction.open("rival"));
        put(holder, inv, 23, tipBtn(Material.GOLDEN_SWORD, "&bSpar",
                List.of("&7Sparring TP & mentor bonds", "&8Tip: trade hits nearby to start")),
                SlotAction.open("spar"));
        put(holder, inv, 25, pageBtn(Material.PAPER, "&7Help",
                "&7How to use /lm", "&8Guide-friendly overview"), SlotAction.page("help"));

        boolean staff = ForgeBridge.isStaff(player);
        boolean skillCheck = ForgeBridge.hasSkillCheck(player);
        if (staff || skillCheck) {
            if (skillCheck || staff) {
                put(holder, inv, 29, tipBtn(Material.ENCHANTED_BOOK,
                        staff && !skillCheck ? "&eSkills" : "&eSkill Check",
                        List.of(staff && !skillCheck
                                        ? "&7Skill unlock admin browser"
                                        : "&7View skill progress (donator)",
                                "&8Natural · Saga")),
                        SlotAction.open(staff && !skillCheck ? "skills" : "skillcheck"));
            }
            if (staff) {
                put(holder, inv, 31, tipBtn(Material.NETHER_STAR, "&6Prestige",
                        List.of("&7Prestige shop / levels", "&8Staff prestige menu")),
                        SlotAction.open("prestige"));
            }
        }
        if (staff) {
            put(holder, inv, 37, tipBtn(Material.EXPERIENCE_BOTTLE, "&dProgression",
                    List.of("&7Natural skills / TP / race", "&8Staff category hub")),
                    SlotAction.open("progression"));
            put(holder, inv, 39, tipBtn(Material.REDSTONE, "&cAdmin",
                    List.of("&7/lm admin · system toggles", "&8Reload · syslog · open")),
                    SlotAction.open("admin"));
            put(holder, inv, 41, pageBtn(Material.WRITABLE_BOOK, "&8Logs",
                    "&7System telemetry", "&8On · off · flush"), SlotAction.page("logs"));
        }
        put(holder, inv, 53, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory logs(Player player) {
        Holder holder = new Holder("logs");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Logs"));
        holder.bind(inv);
        frameOnly(inv, 45);
        Map<String, String> ph = ForgeBridge.hubPlaceholders(player);
        boolean on = "true".equalsIgnoreCase(ph.getOrDefault("syslog", "false"));
        String statusLine = ph.getOrDefault("syslog_status", "unknown");
        put(holder, inv, 4, item(Material.WRITABLE_BOOK, "&8&lLogs", List.of("",
                "&7System telemetry &f" + (on ? "ON" : "OFF"),
                "&8" + statusLine.replace('§', '&'),
                "",
                "&7Use buttons below to toggle / flush")));
        List<String> lore = toAmp(ForgeBridge.hubLines(player, "logs"));
        List<List<String>> parts = GuiLoreChunks.chunk(lore);
        int placed = 0;
        for (List<String> part : parts) {
            if (placed >= 3) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed];
            put(holder, inv, slot, item(Material.PAPER,
                    parts.size() == 1 ? "&fStatus" : "&fStatus &8(" + (placed + 1) + ")",
                    prependBlank(part)));
            placed++;
        }
        put(holder, inv, 29, tipBtn(Material.LIME_CONCRETE, "&aSyslog ON",
                List.of("&7Enable system telemetry")),
                SlotAction.act("syslog", "on", "logs"));
        put(holder, inv, 31, tipBtn(Material.RED_CONCRETE, "&cSyslog OFF",
                List.of("&7Disable system telemetry")),
                SlotAction.act("syslog", "off", "logs"));
        put(holder, inv, 33, tipBtn(Material.GOLD_INGOT, "&eFlush",
                List.of("&7Flush log writers")),
                SlotAction.act("syslog", "flush", "logs"));
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory chunked(Player player, String page, String title, Material mat) {
        Holder holder = new Holder(page);
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics"));
        holder.bind(inv);
        frameOnly(inv, 45);
        List<String> lore = toAmp(ForgeBridge.hubLines(player, page));
        if (lore.isEmpty()) {
            lore = List.of("&7Nothing here yet.", "&8Use &f/lm &8to open systems.");
        }
        put(holder, inv, 4, item(mat, title, List.of("", "&7Guide tips below",
                "&8Players: use &f/lm &8only")));
        List<List<String>> parts = GuiLoreChunks.chunk(lore);
        int placed = 0;
        for (List<String> part : parts) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed];
            String partTitle = parts.size() == 1
                    ? "&fDetails"
                    : "&fPart &e" + (placed + 1) + "&8/&e" + parts.size();
            put(holder, inv, slot, item(Material.PAPER, partTitle, prependBlank(part)));
            placed++;
        }
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
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

    private static void frameOnly(Inventory inv, int size) {
        for (int i = 0; i < size; i++) {
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            if (edge) {
                inv.setItem(i, item(ACCENT, " ", List.of()));
            }
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
        if (slotAction.openSystem != null && !slotAction.openSystem.isBlank()) {
            final String system = slotAction.openSystem;
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                plugin.openSystemFromHub(player, system);
            });
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
            String msg = ForgeBridge.hubHandleDo(player, action, arg, ret);
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
        final String openSystem;
        final boolean shouldClose;

        private SlotAction(
                String action, String arg, String returnPage, String page,
                String rawCommand, String openSystem, boolean shouldClose) {
            this.action = action;
            this.arg = arg;
            this.returnPage = returnPage;
            this.page = page;
            this.rawCommand = rawCommand;
            this.openSystem = openSystem;
            this.shouldClose = shouldClose;
        }

        static SlotAction act(String action, String arg, String returnPage) {
            return new SlotAction(action, arg, returnPage, null, null, null, false);
        }

        static SlotAction page(String page) {
            return new SlotAction(null, null, null, page, null, null, false);
        }

        static SlotAction open(String system) {
            return new SlotAction(null, null, null, null, null, system, false);
        }

        static SlotAction dismiss() {
            return new SlotAction(null, null, null, null, null, null, true);
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
