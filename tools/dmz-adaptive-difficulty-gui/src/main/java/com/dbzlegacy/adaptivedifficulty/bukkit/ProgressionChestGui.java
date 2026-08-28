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

/**
 * Bukkit chest GUI fallback — Legacy Mechanics Progression.
 * Category hub matching {@link CmiProgressionGui}.
 */
public final class ProgressionChestGui implements Listener {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material SECTION = Material.LIGHT_GRAY_STAINED_GLASS_PANE;

    private static final String[][] FLAG_GROUPS = {
            {"&eSkills", "flight", "sprint", "meditation", "potential"},
            {"&6TP Gains", "farming", "building", "boost", "bio"},
            {"&bRace", "racelock", "yardrat", "spiritualist", "android"},
            {"&cCombat", "kiweapons", "piercing", "dot", "apothic"},
            {"&5End", "end", "endportal"},
            {"&7Utility", "shadow", "statchecker"},
            {"&dFabled", "fabled"}
    };

    /** Per-section flag cards: key → title, description, commands. */
    private static final Map<String, String[]> FLAG_INFO = Map.ofEntries(
            Map.entry("flight", new String[]{"Flight", "Train fly by flying; Viltrumite max grant.", "/progression"}),
            Map.entry("sprint", new String[]{"Sprint Jump", "Jump/Sprint — Strength unlocked (invested STR).", "/progression"}),
            Map.entry("meditation", new String[]{"Meditation", "Restore energy in the global trial biome. Players: /progression meditation", "/progression meditation · next"}),
            Map.entry("potential", new String[]{"Potential", "Earn points from PvP hits & taking damage (cap 10 natural).", "/skillcheck"}),
            Map.entry("farming", new String[]{"Farming TP", "Break mature crops / Pam's harvest for TP.", "Passive while farming"}),
            Map.entry("building", new String[]{"Building TP", "Place blocks for silent building TP.", "Passive while placing"}),
            Map.entry("boost", new String[]{"Global TP Boost", "Timed world TP multiplier.", "/progression boost …"}),
            Map.entry("bio", new String[]{"Bio-Android", "Absorb TP / steal skills from drains.", "Passive as Bio-Android"}),
            Map.entry("racelock", new String[]{"Race Lock", "Ancient/Sento need Fabled unlock skills.", "Passive on race select"}),
            Map.entry("yardrat", new String[]{"Yardrat", "Form mastery double-gain + starter ki.", "Passive for Yardrat"}),
            Map.entry("spiritualist", new String[]{"Spiritualist Ki", "Class confirm grants/removes kicontrol.", "Passive on class change"}),
            Map.entry("android", new String[]{"Android Conversion", "Staff convert a player to Android.", "/progression android"}),
            Map.entry("kiweapons", new String[]{"Ki Weapons", "Blade/scythe/clawlance Apothic attrs.", "Passive on equip"}),
            Map.entry("piercing", new String[]{"Piercing", "PROT_PIERCE → SKP (not on ki weapons).", "Passive in combat"}),
            Map.entry("dot", new String[]{"DoT Extra", "Extra damage from DoT sources.", "Passive in combat"}),
            Map.entry("apothic", new String[]{"Apothic Elemental", "Fire/cold Apothic damage hooks.", "Passive in combat"}),
            Map.entry("end", new String[]{"End Strength", "Dragon scale, ki attacks, egg/crystal clear.", "/enddragon · /cleardragons"}),
            Map.entry("endportal", new String[]{"End Portal Guard", "Blocks End portal use when locked.", "Passive at portals"}),
            Map.entry("shadow", new String[]{"Shadow Dummy", "50% shadow + spawn protect.", "Passive near dummies"}),
            Map.entry("statchecker", new String[]{"Stat Checker", "Sneak + RMB a player to dump stats.", "Sneak + right-click"}),
            Map.entry("fabled", new String[]{"Fabled Bridges", "Master switch for soft Fabled syncs.", "See Fabled Subflags"})
    );

    private static final String[] FABLED_FLAG_KEYS = {
            "fabled", "energy", "statscreen", "tpsp", "attr",
            "prestigeskill", "faction", "cleaner", "raceclass", "classperm"
    };

    private final AdaptiveDifficultyGuiPlugin plugin;

    public ProgressionChestGui(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        Inventory inv = switch (p) {
            case "skills" -> sectionFlags(player, "skills", "&eSkills", Material.FEATHER,
                    new String[]{"flight", "sprint", "meditation", "potential"});
            case "tp" -> sectionFlags(player, "tp", "&6TP Gains", Material.GOLD_INGOT,
                    new String[]{"farming", "building", "boost", "bio"});
            case "race" -> sectionFlags(player, "race", "&bRace & Form", Material.PLAYER_HEAD,
                    new String[]{"racelock", "yardrat", "spiritualist", "android"});
            case "combat" -> sectionFlags(player, "combat", "&cCombat", Material.IRON_SWORD,
                    new String[]{"kiweapons", "piercing", "dot", "apothic"});
            case "end" -> sectionFlags(player, "end", "&5End", Material.END_STONE,
                    new String[]{"end", "endportal"});
            case "fabled" -> sectionFlags(player, "fabled", "&dFabled Bridges", Material.ENCHANTED_BOOK,
                    new String[]{"fabled"});
            case "utility" -> sectionFlags(player, "utility", "&7Utility", Material.COMPARATOR,
                    new String[]{"shadow", "statchecker"});
            case "status" -> sectionFlags(player, "status", "&eStatus", Material.BOOK,
                    new String[]{"flight", "sprint", "meditation", "potential", "farming", "building"});
            case "help" -> category(player, "help", "&7Help", Material.PAPER);
            case "admin", "flags", "disable" -> ForgeBridge.isStaff(player) ? flags(player) : main(player);
            case "flags_fabled", "fabled_flags" -> ForgeBridge.isStaff(player) ? fabledFlags(player) : main(player);
            default -> main(player);
        };
        player.openInventory(inv);
    }

    private Inventory main(Player player) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(player);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 54, color("&8Legacy Mechanics · Progression"));
        holder.bind(inv);
        frame(inv, 54);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        if (!bridgeOk || !systemOn) {
            put(holder, inv, 4, item(Material.EXPERIENCE_BOTTLE,
                    !bridgeOk ? "&c&lUNAVAILABLE" : "&c&lPROGRESSION DISABLED",
                    unavailableLore(bridgeOk)));
            put(holder, inv, 49, hubBtn(), SlotAction.cmd("lm"));
            put(holder, inv, 53, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        put(holder, inv, 4, item(Material.EXPERIENCE_BOTTLE, "&d&lProgression",
                List.of("", "&7Natural systems · click a section",
                        "&7Flags toggle on each section page",
                        "&8Prestige / Skills live on Hub")));

        int[] slots = GuiBoardHelper.centeredSlots(7);
        String[] pages = {"skills", "tp", "race", "combat", "end", "fabled", "utility"};
        Material[] mats = {
                Material.FEATHER, Material.GOLD_INGOT, Material.PLAYER_HEAD, Material.IRON_SWORD,
                Material.END_STONE, Material.ENCHANTED_BOOK, Material.COMPARATOR
        };
        String[] titles = {
                "&eSkills", "&6TP Gains", "&bRace & Form", "&cCombat",
                "&5End", "&dFabled", "&7Utility"
        };
        String[] tips = {
                "&7Flight · Sprint · Meditation · Potential",
                "&7Farming · Building · Boost · Bio",
                "&7Race lock · Yardrat · Spiritualist · Android",
                "&7Ki weapons · Piercing · DoT · Apothic",
                "&7End strength · Portal guard",
                "&7Soft Fabled bridges",
                "&7Shadow dummy · Stat checker"
        };
        for (int i = 0; i < pages.length && i < slots.length; i++) {
            put(holder, inv, slots[i], tipBtn(mats[i], titles[i],
                    List.of(tips[i], "", "&eClick · toggle flags inside")),
                    SlotAction.page(pages[i]));
        }

        put(holder, inv, 40, pageBtn(Material.PAPER, "&7Help", "&7Commands"),
                SlotAction.page("help"));

        if (ForgeBridge.isStaff(player)) {
            put(holder, inv, 31, pageBtn(Material.REDSTONE, "&cAll Flags",
                    "&7Full flag board"), SlotAction.page("admin"));
        }
        put(holder, inv, 49, hubBtn(), SlotAction.cmd("lm"));
        put(holder, inv, 53, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory sectionFlags(Player player, String page, String title, Material mat, String[] keys) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(player);
        Holder holder = new Holder(page);
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Progression"));
        holder.bind(inv);
        frame(inv, 45);
        boolean staff = ForgeBridge.isStaff(player);
        put(holder, inv, 4, item(mat, title, List.of("",
                staff ? "&7Click a module to toggle ON/OFF" : "&7Module status (staff can toggle)",
                "&8Description + commands on each item")));
        int[] slots = GuiBoardHelper.centeredSlots(keys.length);
        for (int i = 0; i < keys.length && i < slots.length; i++) {
            String key = keys[i];
            boolean on = "true".equalsIgnoreCase(ph.getOrDefault("flag_" + key, "false"));
            String[] info = FLAG_INFO.getOrDefault(key, new String[]{key, "Progression module.", ""});
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(on ? "&aEnabled" : "&cDisabled");
            lore.add("&7" + info[1]);
            if (info.length > 2 && info[2] != null && !info[2].isBlank()) {
                lore.add("&8Cmd: &f" + info[2]);
            }
            lore.add("");
            lore.add(staff ? "&eClick to toggle" : "&8Ask staff to change flags");
            ItemStack stack = tipBtn(
                    on ? Material.LIME_DYE : Material.GRAY_DYE,
                    (on ? "&a" : "&8") + info[0] + (on ? " ON" : " OFF"),
                    lore);
            if (staff) {
                put(holder, inv, slots[i], stack, SlotAction.act("flag", key, page));
            } else {
                put(holder, inv, slots[i], stack);
            }
        }
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lm"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory category(Player player, String page, String title, Material mat) {
        Holder holder = new Holder(page);
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Progression"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(mat, title, List.of("", "&7Module flags & tips below",
                "&8Each paper holds part of this page")));
        List<String> lore = toAmp(ForgeBridge.progressionLines(player, page));
        if (lore.isEmpty()) {
            lore = List.of("&7Nothing here yet.", "&8Check Flags if modules look empty.");
        }
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
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lm"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory flags(Player player) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(player);
        Holder holder = new Holder("admin");
        Inventory inv = Bukkit.createInventory(holder, 54, color("&8Legacy Mechanics · Flags"));
        holder.bind(inv);
        frame(inv, 54);
        put(holder, inv, 4, item(Material.REDSTONE, "&c&lStaff Flags",
                List.of("", "&7Grouped by script category", "&7Click a flag to toggle")));

        int[] slots = {
                1, 2, 3, 5, 6, 7,
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
        int si = 0;
        for (String[] group : FLAG_GROUPS) {
            if (si >= slots.length) {
                break;
            }
            String sectionTitle = group[0];
            put(holder, inv, slots[si++], item(SECTION, sectionTitle, List.of("", "&8Category")));
            for (int g = 1; g < group.length && si < slots.length; g++) {
                String key = group[g];
                boolean on = "true".equalsIgnoreCase(ph.getOrDefault("flag_" + key, "false"));
                put(holder, inv, slots[si++], tipBtn(
                        on ? Material.LIME_DYE : Material.GRAY_DYE,
                        (on ? "&a" : "&8") + key + (on ? " ON" : " OFF"),
                        List.of("&8" + stripAmp(sectionTitle), "&7Click to toggle " + key)),
                        SlotAction.act("flag", key, "admin"));
            }
        }
        put(holder, inv, 47, pageBtn(Material.ENCHANTED_BOOK, "&dFabled Subflags",
                "&7Energy, TP/SP, race class, etc."), SlotAction.page("flags_fabled"));
        put(holder, inv, 45, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 49, hubBtn(), SlotAction.cmd("lm"));
        put(holder, inv, 53, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory fabledFlags(Player player) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(player);
        Holder holder = new Holder("flags_fabled");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Legacy Mechanics · Fabled Flags"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.ENCHANTED_BOOK, "&d&lFabled Subflags",
                List.of("", "&7Soft-dependency bridge toggles", "&7Click to toggle")));
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30};
        for (int i = 0; i < FABLED_FLAG_KEYS.length && i < slots.length; i++) {
            String key = FABLED_FLAG_KEYS[i];
            boolean on = "true".equalsIgnoreCase(ph.getOrDefault("flag_" + key, "false"));
            put(holder, inv, slots[i], tipBtn(
                    on ? Material.LIME_DYE : Material.GRAY_DYE,
                    (on ? "&a" : "&8") + key + (on ? " ON" : " OFF"),
                    List.of("&8Fabled", "&7Click to toggle " + key)),
                    SlotAction.act("flag", key, "flags_fabled"));
        }
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return to Flags"),
                SlotAction.page("admin"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lm"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private static String stripAmp(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&e", "").replace("&6", "").replace("&b", "")
                .replace("&c", "").replace("&5", "").replace("&a", "")
                .replace("&d", "").replace("&7", "").replace("&", "");
    }

    private static List<String> unavailableLore(boolean bridgeOk) {
        if (!bridgeOk) {
            return List.of("", "&cForge LegacyMechanics mod unreachable",
                    "&7Check mods/ for LegacyMechanics-*.jar");
        }
        return List.of("", "&cProgression system is disabled", "&7Ask an admin if you need access");
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
            String msg = ForgeBridge.progressionHandleDo(player, action, arg, ret);
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
