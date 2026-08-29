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

/** Bukkit chest GUI fallback — Legacy Mechanics Prestige. */
public final class PrestigeChestGui implements Listener {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private final AdaptiveDifficultyGuiPlugin plugin;

    public PrestigeChestGui(AdaptiveDifficultyGuiPlugin plugin) {
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
        GuiFeedback.openChest(viewer, main(viewer, subject));
    }

    private Inventory main(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(subject);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 36, invTitle(viewer, subject, "&8Prestige"));
        holder.bind(inv);
        frame(inv, 36);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        if (!bridgeOk || !systemOn) {
            put(holder, inv, 4, item(Material.GOLDEN_APPLE,
                    !bridgeOk ? "&c&lUNAVAILABLE" : "&c&lPRESTIGE DISABLED",
                    unavailableLore(bridgeOk)));
            put(holder, inv, 27, hubBtn(), SlotAction.cmd("lm"));
            put(holder, inv, 31, tipBtn(viewer, Material.BREWING_STAND, "&dProgression",
                    List.of("&7Back to progression")), SlotAction.cmd("progression"));
            put(holder, inv, 35, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        put(holder, inv, 4, item(Material.GOLDEN_APPLE, "&6&lPrestige",
                prependBlank(toAmp(ForgeBridge.prestigeLines(subject, "main")))));
        boolean ready = "true".equalsIgnoreCase(ph.getOrDefault("ready", "false"));
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("");
        confirmLore.addAll(GuiBoardHelper.tips(viewer, "&7Click to prestige (confirm within 10s)"));
        confirmLore.add("&8Resets DMZ stats · awards held Prestige");
        put(holder, inv, 22, item(
                ready ? Material.LIME_CONCRETE : Material.ORANGE_CONCRETE,
                ready ? "&aConfirm Prestige" : "&eAttempt Prestige",
                confirmLore), SlotAction.act("confirm", "0", "main"));
        put(holder, inv, 27, hubBtn(), SlotAction.cmd("lm"));
        put(holder, inv, 31, tipBtn(viewer, Material.BREWING_STAND, "&dProgression",
                List.of("&7Back to progression")), SlotAction.cmd("progression"));
        put(holder, inv, 35, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private static List<String> unavailableLore(boolean bridgeOk) {
        if (!bridgeOk) {
            return List.of("", "&cForge LegacyMechanics mod unreachable");
        }
        return List.of("", "&cPrestige system is disabled");
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
            String msg = ForgeBridge.prestigeHandleDo(subject, action, arg, ret);
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
