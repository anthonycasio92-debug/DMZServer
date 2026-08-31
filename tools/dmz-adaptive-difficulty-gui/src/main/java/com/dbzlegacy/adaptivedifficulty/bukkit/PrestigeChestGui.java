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

/** Bukkit chest GUI — Prestige purchase, turn-in, and prestige-points shop. */
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
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        Inventory inv = switch (p) {
            case "turnin", "points" -> turnIn(viewer, subject);
            case "shop", "skills" -> shop(viewer, subject);
            case "forms", "form" -> forms(viewer, subject);
            case "cap", "breakthrough", "breakthroughs" -> cap(viewer, subject);
            default -> main(viewer, subject);
        };
        GuiFeedback.openChest(viewer, inv);
    }

    private Inventory main(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(subject);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Prestige"));
        holder.bind(inv);
        frame(inv, 45);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        if (!bridgeOk || !systemOn) {
            put(holder, inv, 4, item(Material.GOLDEN_APPLE,
                    !bridgeOk ? "&c&lUNAVAILABLE" : "&c&lPRESTIGE DISABLED",
                    unavailableLore(bridgeOk)));
            put(holder, inv, 36, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
            if (ForgeBridge.isStaff(viewer)) {
                put(holder, inv, 40, tipBtn(viewer, Material.BREWING_STAND, "&dProgression",
                        List.of("&7Staff progression flags")),
                        SlotAction.cmd("lmdo lm open progression"));
            }
            put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        put(holder, inv, 4, item(Material.GOLDEN_APPLE, "&6&lPrestige",
                prependBlank(toAmp(ForgeBridge.prestigeLines(subject, "main")))));

        boolean ready = "true".equalsIgnoreCase(ph.getOrDefault("ready", "false"));
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("");
        confirmLore.addAll(GuiBoardHelper.tips(viewer, "&7Click to prestige (confirm within 10s)"));
        confirmLore.add("&8Resets DMZ stats · awards held Prestige");
        put(holder, inv, 20, item(
                ready ? Material.LIME_CONCRETE : Material.ORANGE_CONCRETE,
                ready ? "&aConfirm Prestige" : "&eAttempt Prestige",
                confirmLore), SlotAction.act("confirm", "0", "main"));

        put(holder, inv, 22, tipBtn(viewer, Material.GOLD_NUGGET, "&eTurn In Prestiges",
                List.of("&7Convert held prestiges into points",
                        "&7Balance: &e" + ph.getOrDefault("points", "0"))),
                SlotAction.page("turnin"));
        put(holder, inv, 24, tipBtn(viewer, Material.EXPERIENCE_BOTTLE, "&aSkill Shop",
                List.of("&71 point = +1 permanent skill level")),
                SlotAction.page("shop"));
        put(holder, inv, 30, tipBtn(viewer, Material.MAGENTA_DYE, "&dForms",
                List.of("&7Permanent Majin / Mutant (&e5 &7pts)")),
                SlotAction.page("forms"));
        put(holder, inv, 32, tipBtn(viewer, Material.NETHER_STAR, "&bHard-Stat Breakthrough",
                List.of("&7+10k hard stats per core stat (past 100k body)",
                        "&7Equiv power: &f" + ph.getOrDefault("level_cap_fmt", "100000"),
                        "&8Server level cap stays &f100000")),
                SlotAction.page("cap"));

        put(holder, inv, 36, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        if (ForgeBridge.isStaff(viewer)) {
            put(holder, inv, 40, tipBtn(viewer, Material.BREWING_STAND, "&dProgression",
                    List.of("&7Staff progression flags")),
                    SlotAction.cmd("lmdo lm open progression"));
        }
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory turnIn(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(subject);
        Holder holder = new Holder("turnin");
        Inventory inv = Bukkit.createInventory(holder, 36, invTitle(viewer, subject, "&8Prestige · Turn In"));
        holder.bind(inv);
        frame(inv, 36);

        put(holder, inv, 4, item(Material.GOLD_NUGGET, "&e&lTurn In Prestiges",
                prependBlank(toAmp(ForgeBridge.prestigeLines(subject, "turnin")))));

        int held = parseInt(ph.get("held"), 0);
        putTurnIn(holder, inv, 19, 1, held, ph);
        putTurnIn(holder, inv, 21, 3, held, ph);
        putTurnIn(holder, inv, 23, 5, held, ph);
        List<String> allLore = new ArrayList<>();
        allLore.add("");
        allLore.add("&7Turn in all &6" + held + " &7held");
        allLore.add("&7Gain &e" + ph.getOrDefault("turnin_all_points", "0") + " &7points");
        allLore.addAll(GuiBoardHelper.tips(viewer, "&7+1 bonus point per 3 turned in"));
        put(holder, inv, 25, item(Material.GOLD_BLOCK, "&6Turn In All", allLore),
                held > 0 ? SlotAction.act("turnin", "all", "turnin") : null);

        put(holder, inv, 27, backBtn(), SlotAction.page("main"));
        put(holder, inv, 31, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 35, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private void putTurnIn(
            Holder holder, Inventory inv, int slot, int amount, int held, Map<String, String> ph
    ) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Spend &6" + amount + " &7held prestige" + (amount == 1 ? "" : "s"));
        lore.add("&7Gain &e" + ph.getOrDefault("turnin_" + amount + "_points", "0") + " &7points");
        if (amount >= 3) {
            lore.add("&aIncludes bonus for packs of 3");
        }
        boolean ok = held >= amount;
        put(holder, inv, slot, item(
                ok ? Material.GOLD_INGOT : Material.GRAY_DYE,
                (ok ? "&eTurn In &f" : "&8Need &f") + amount,
                lore),
                ok ? SlotAction.act("turnin", String.valueOf(amount), "turnin") : null);
    }

    private Inventory shop(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(subject);
        Holder holder = new Holder("shop");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Prestige · Skills"));
        holder.bind(inv);
        frame(inv, 45);

        put(holder, inv, 4, item(Material.EXPERIENCE_BOTTLE, "&a&lSkill Shop",
                prependBlank(toAmp(ForgeBridge.prestigeLines(subject, "shop")))));

        putSkill(holder, inv, 19, Material.ENCHANTED_BOOK, "meditation", "Meditation", ph);
        putSkill(holder, inv, 20, Material.FEATHER, "fly", "Fly", ph);
        putSkill(holder, inv, 21, Material.SUGAR, "sprint", "Sprint", ph);
        putSkill(holder, inv, 22, Material.RABBIT_FOOT, "jump", "Jump", ph);
        putSkill(holder, inv, 23, Material.NETHER_STAR, "potentialunlock", "Potential Unlock", ph);

        put(holder, inv, 36, backBtn(), SlotAction.page("main"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private void putSkill(
            Holder holder, Inventory inv, int slot, Material mat, String id, String label,
            Map<String, String> ph
    ) {
        String bought = ph.getOrDefault("skill_" + id, "0");
        String max = ph.getOrDefault("skill_" + id + "_max", "10");
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Prestige floor: &f" + bought + "&7/&f" + max);
        lore.add("&7Cost: &e1 &7point → &a+1 &7level");
        lore.add("&8Survives prestige reset");
        put(holder, inv, slot, item(mat, "&a" + label, lore),
                SlotAction.act("skill", id, "shop"));
    }

    private Inventory forms(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(subject);
        Holder holder = new Holder("forms");
        Inventory inv = Bukkit.createInventory(holder, 36, invTitle(viewer, subject, "&8Prestige · Forms"));
        holder.bind(inv);
        frame(inv, 36);

        put(holder, inv, 4, item(Material.MAGENTA_DYE, "&d&lPermanent Forms",
                prependBlank(toAmp(ForgeBridge.prestigeLines(subject, "forms")))));

        boolean hasMajin = "true".equalsIgnoreCase(ph.getOrDefault("majin", "false"));
        boolean hasMutant = "true".equalsIgnoreCase(ph.getOrDefault("mutant", "false"));
        String cost = ph.getOrDefault("form_cost", "5");

        List<String> majinLore = new ArrayList<>();
        majinLore.add("");
        majinLore.add(hasMajin ? "&aOwned" : "&7Cost: &e" + cost + " &7points");
        if (hasMutant && !hasMajin) {
            majinLore.add("&8Buying removes Mutant (no refund)");
        }
        majinLore.add("&8Only one form at a time");
        put(holder, inv, 20, item(
                hasMajin ? Material.LIME_DYE : Material.PINK_DYE,
                hasMajin ? "&aPermanent Majin" : "&dBuy Permanent Majin",
                majinLore),
                hasMajin ? null : SlotAction.act("majin", "0", "forms"));

        List<String> mutantLore = new ArrayList<>();
        mutantLore.add("");
        mutantLore.add(hasMutant ? "&aOwned" : "&7Cost: &e" + cost + " &7points");
        if (hasMajin && !hasMutant) {
            mutantLore.add("&8Buying removes Majin (no refund)");
        }
        mutantLore.add("&8Only one form at a time");
        put(holder, inv, 22, item(
                hasMutant ? Material.LIME_DYE : Material.SLIME_BALL,
                hasMutant ? "&aPermanent Mutant" : "&aBuy Permanent Mutant",
                mutantLore),
                hasMutant ? null : SlotAction.act("mutant", "0", "forms"));

        if (hasMajin) {
            put(holder, inv, 24, tipBtn(viewer, Material.BARRIER, "&cUnpurchase Majin",
                    List.of("&7Removes Majin · &cno point refund")),
                    SlotAction.act("unmajin", "0", "forms"));
        } else if (hasMutant) {
            put(holder, inv, 24, tipBtn(viewer, Material.BARRIER, "&cUnpurchase Mutant",
                    List.of("&7Removes Mutant · &cno point refund")),
                    SlotAction.act("unmutant", "0", "forms"));
        }

        put(holder, inv, 27, backBtn(), SlotAction.page("main"));
        put(holder, inv, 31, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 35, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory cap(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(subject);
        Holder holder = new Holder("cap");
        Inventory inv = Bukkit.createInventory(holder, 36, invTitle(viewer, subject, "&8Prestige · Breakthrough"));
        holder.bind(inv);
        frame(inv, 36);

        put(holder, inv, 4, item(Material.NETHER_STAR, "&b&lHard-Stat Breakthrough",
                prependBlank(toAmp(ForgeBridge.prestigeLines(subject, "cap")))));

        int bt = parseInt(ph.get("breakthroughs"), 0);
        int max = parseInt(ph.get("breakthroughs_max"), 5);
        boolean canBuy = bt < max;
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Equivalent power: &f" + ph.getOrDefault("level_cap_fmt", "100000"));
        lore.add("&7Breakthroughs: &f" + bt + "&7/&f" + max);
        lore.add("&8DMZ level cap stays &f100000 &8for everyone");
        if (canBuy) {
            lore.add("&7Next: &a+10,000 &7to each hard stat for &e"
                    + ph.getOrDefault("next_breakthrough_cost", "15") + " &7points");
            lore.add("&8Costs: 15 → 20 → 25 → 30 → 35");
        } else {
            lore.add("&aMax breakthroughs (~150000 power)");
        }
        put(holder, inv, 22, item(
                canBuy ? Material.NETHER_STAR : Material.BEACON,
                canBuy ? "&bBuy Breakthrough" : "&aBreakthroughs Maxed",
                lore),
                canBuy ? SlotAction.act("breakthrough", "0", "cap") : null);

        put(holder, inv, 27, backBtn(), SlotAction.page("main"));
        put(holder, inv, 31, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 35, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (Exception e) {
            return fallback;
        }
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

    private static ItemStack backBtn() {
        return item(Material.ARROW, "&7« Prestige", List.of());
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
