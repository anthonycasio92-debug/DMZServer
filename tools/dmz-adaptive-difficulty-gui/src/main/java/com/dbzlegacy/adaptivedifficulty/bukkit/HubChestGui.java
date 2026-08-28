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
            case "logs", "syslog" -> ForgeBridge.isStaff(player)
                    ? logs(player)
                    : main(player);
            // Help removed — any leftover /lm do page help opens the hub.
            default -> main(player);
        };
        player.openInventory(inv);
    }

    private Inventory main(Player player) {
        Player subject = AdminInspectSessions.resolveSubject(player);
        boolean inspecting = subject != null
                && !subject.getUniqueId().equals(player.getUniqueId());
        Map<String, String> ph = ForgeBridge.hubPlaceholders(subject);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 54, color(inspecting
                ? "&8Legacy Mechanics · &c" + subject.getName()
                : "&8Legacy Mechanics"));
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

        List<String> hubHeaderLore = new ArrayList<>();
        hubHeaderLore.add("");
        if (inspecting) {
            hubHeaderLore.add("&cInspecting &f" + subject.getName());
            hubHeaderLore.add("&7You see and edit their LM systems.");
            hubHeaderLore.add("");
        }
        hubHeaderLore.addAll(GuiBoardHelper.tips(player, "&7Choose a system", "&8/lm"));
        put(holder, inv, 4, item(Material.NETHER_STAR, "&f&lLegacy Mechanics", hubHeaderLore));

        // Row 2 — core (everyone): Difficulty · Rival · Spar
        put(holder, inv, 20, tipBtn(player, Material.BEACON, "&aDifficulty",
                List.of("&7Unlock tiers & world scaling", "&eClick to open")),
                SlotAction.open("difficulty"));
        put(holder, inv, 22, tipBtn(player, Material.NAME_TAG, "&6Rival",
                List.of("&7Rivalry, challenges & RP", "&eClick to open")),
                SlotAction.open("rival"));
        put(holder, inv, 24, tipBtn(player, Material.GOLDEN_SWORD, "&bSpar",
                List.of("&7Sparring TP & mentor bonds", "&eClick to open")),
                SlotAction.open("spar"));

        boolean staff = ForgeBridge.isStaff(player);
        boolean skillCheck = ForgeBridge.hasSkillCheck(player);

        // Row 3 — progress (donator / staff), centered
        if (skillCheck && staff) {
            put(holder, inv, 21, tipBtn(player, Material.FEATHER, "&eSkill Check",
                    List.of("&7Natural · Saga progress", "&eClick to open")),
                    SlotAction.open("skillcheck"));
            put(holder, inv, 23, tipBtn(player, Material.GOLDEN_APPLE, "&6Prestige",
                    List.of("&7Prestige shop / levels", "&eClick to open")),
                    SlotAction.open("prestige"));
        } else if (skillCheck) {
            put(holder, inv, 22, tipBtn(player, Material.FEATHER, "&eSkill Check",
                    List.of("&7Natural · Saga progress", "&eClick to open")),
                    SlotAction.open("skillcheck"));
        } else if (staff) {
            put(holder, inv, 21, tipBtn(player, Material.FEATHER, "&eSkills",
                    List.of("&7Skill unlock admin browser", "&eClick to open")),
                    SlotAction.open("skills"));
            put(holder, inv, 23, tipBtn(player, Material.GOLDEN_APPLE, "&6Prestige",
                    List.of("&7Prestige shop / levels", "&eClick to open")),
                    SlotAction.open("prestige"));
        }

        // Row 4 — staff tools
        if (staff) {
            put(holder, inv, 38, tipBtn(player, Material.BREWING_STAND, "&dProgression",
                    List.of("&7Skills · TP · Race · Combat flags", "&eClick to open")),
                    SlotAction.open("progression"));
            put(holder, inv, 40, tipBtn(player, Material.COMMAND_BLOCK, "&cAdmin",
                    List.of("&7Reload · syslog · open systems", "&8/lm admin")),
                    SlotAction.open("admin"));
            put(holder, inv, 42, tipBtn(player, Material.CLOCK, "&8Logs",
                    List.of("&7System telemetry", "&eClick to open")),
                    SlotAction.page("logs"));
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
        List<String> logsHeader = new ArrayList<>();
        logsHeader.add("");
        logsHeader.add("&7System telemetry &f" + (on ? "ON" : "OFF"));
        logsHeader.add("&8" + statusLine.replace('§', '&'));
        logsHeader.add("");
        logsHeader.addAll(GuiBoardHelper.tips(player, "&7Use buttons below to toggle / flush"));
        put(holder, inv, 4, item(Material.CLOCK, "&8&lLogs", logsHeader));
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
        put(holder, inv, 29, tipBtn(player, Material.LIME_DYE, "&aSyslog ON",
                List.of("&7Enable system telemetry")),
                SlotAction.act("syslog", "on", "logs"));
        put(holder, inv, 31, tipBtn(player, Material.GRAY_DYE, "&cSyslog OFF",
                List.of("&7Disable system telemetry")),
                SlotAction.act("syslog", "off", "logs"));
        put(holder, inv, 33, tipBtn(player, Material.HOPPER, "&eFlush",
                List.of("&7Flush log writers")),
                SlotAction.act("syslog", "flush", "logs"));
        put(holder, inv, 36, pageBtn(player, Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
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
