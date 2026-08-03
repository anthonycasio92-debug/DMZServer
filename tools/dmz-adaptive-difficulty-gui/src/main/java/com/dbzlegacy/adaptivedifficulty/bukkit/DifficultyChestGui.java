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
 * Bukkit chest GUI fallback — tier-centric Adaptive Difficulty.
 * Pages: Hub · Buy Tier · Lower Tier · Titles. Details is staff/ops only.
 */
public final class DifficultyChestGui implements Listener {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;
    private static final int[] TIER_SLOTS = {19, 20, 21, 22, 23, 24, 25};

    private final AdaptiveDifficultyGuiPlugin plugin;

    public DifficultyChestGui(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        Inventory inv = switch (p) {
            case "adjust", "change", "set", "lower" -> lower(player);
            case "buy", "purchase", "unlock" -> buy(player);
            case "titles", "title" -> titles(player);
            case "team", "teams" -> teamsWip(player);
            case "stats", "statistics", "details" -> ForgeBridge.isStaff(player) ? stats(player) : main(player);
            default -> main(player);
        };
        player.openInventory(inv);
    }

    private Inventory main(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 36, color("&8Adaptive Difficulty"));
        holder.bind(inv);
        frame(inv, 36);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        boolean allowed = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("player_allowed", "false"));
        if (!bridgeOk || !systemOn || !allowed) {
            String title = !bridgeOk ? "&c&lUNAVAILABLE"
                    : !systemOn ? "&c&lSYSTEM DISABLED" : "&e&lWHITELIST ONLY";
            put(holder, inv, 13, item(Material.NETHER_STAR, title, unavailableLore(player, systemOn, bridgeOk)));
            put(holder, inv, 31, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        boolean personalOn = "true".equalsIgnoreCase(ph.getOrDefault("personal_enabled", "false"));
        String stateColor = personalOn ? ph.getOrDefault("state_color", "f") : "c";
        put(holder, inv, 13, item(Material.NETHER_STAR,
                personalOn ? "&f&lAdaptive Difficulty" : "&c&lDIFFICULTY OFF",
                statusLore(ph, stateColor, ForgeBridge.isStaff(player), personalOn)));
        // Primary actions — centered trio
        put(holder, inv, 20, pageBtn(Material.GOLD_INGOT, "&eBuy Tier",
                "&7Purchase a higher Unlock Tier", "&8Ancient Coins · pay-up OK · no change"),
                SlotAction.page("buy"));
        put(holder, inv, 22, pageBtn(Material.WHITE_CONCRETE, "&fLower Tier",
                "&7Select a lower unlocked tier", "&8Or reset to None · always free"),
                SlotAction.page("lower"));
        put(holder, inv, 24, pageBtn(Material.NAME_TAG, "&dTitles",
                "&7Equip difficulty titles", "&8Earned from tiers and combat"),
                SlotAction.page("titles"));

        boolean coinChatOn = "true".equalsIgnoreCase(ph.getOrDefault("coin_drop_chat", "false"));
        put(holder, inv, 29, tipBtn(
                personalOn ? Material.LIME_DYE : Material.GRAY_DYE,
                personalOn ? "&aDifficulty ON" : "&cDifficulty OFF",
                List.of(
                        personalOn
                                ? "&7Click to turn OFF for you only"
                                : "&7Click to turn ON for you only",
                        personalOn
                                ? "&8OFF disables scaling, kill coins,"
                                : "&8ON restores scaling, kill coins,",
                        personalOn
                                ? "&8AI pressure, and tier buys"
                                : "&8AI pressure, and tier buys"
                )), SlotAction.act("toggle_personal", "0", "main"));
        put(holder, inv, 31, tipBtn(
                coinChatOn ? Material.BELL : Material.PAPER,
                coinChatOn ? "&aCoin Chat ON" : "&8Coin Chat OFF",
                List.of(
                        coinChatOn
                                ? "&7Click to mute drop messages"
                                : "&7Click to show drop messages",
                        "&8Only affects Ancient Coin kill chat"
                )), SlotAction.act("toggle_coin_chat", "0", "main"));
        if (ForgeBridge.isStaff(player)) {
            put(holder, inv, 33, pageBtn(Material.BOOK, "&8Details",
                    "&7Staff breakdown", "&8CR · prestige · kit gates"),
                    SlotAction.page("stats"));
        }
        put(holder, inv, 35, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory buy(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("buy");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Buy Higher Tier"));
        holder.bind(inv);
        frame(inv, 45);

        boolean personalOn = "true".equalsIgnoreCase(ph.getOrDefault("personal_enabled", "false"));
        List<String> info = new ArrayList<>();
        info.add("");
        if (!personalOn) {
            info.add("&cPersonal difficulty is OFF");
            info.add("&7Turn it ON on the main menu to buy.");
            put(holder, inv, 4, item(Material.BARRIER, "&c&lBuy Locked", info));
            put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
            put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
            return inv;
        }
        info.add("&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"));
        info.add("&7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"));
        info.add("&7DMZ Level &f" + ph.getOrDefault("level", "?")
                + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"));
        info.add("");
        info.addAll(coinLore(ph));
        info.add("");
        info.add("&8Costs scale with your DMZ level");
        info.add("&8Pay-up OK (e.g. Copper instead of Iron) — no change");
        put(holder, inv, 4, item(Material.GOLD_INGOT, "&e&lBuy Higher Tier", info));

        placeTierItems(holder, inv, ph, true);
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 40, pageBtn(Material.WHITE_CONCRETE, "&fLower Tier",
                "&7Select a lower unlocked tier"), SlotAction.page("lower"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory lower(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("lower");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Lower Difficulty Tier"));
        holder.bind(inv);
        frame(inv, 45);

        boolean personalOn = "true".equalsIgnoreCase(ph.getOrDefault("personal_enabled", "false"));
        if (!personalOn) {
            put(holder, inv, 4, item(Material.BARRIER, "&c&lLower Locked", List.of(
                    "",
                    "&cPersonal difficulty is OFF",
                    "&7Turn it ON on the main menu to change tiers."
            )));
            put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
            put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        put(holder, inv, 4, item(Material.NETHER_STAR, "&f&lLower Tier", List.of(
                "",
                "&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"),
                "&7Combat Rating &f" + ph.getOrDefault("combat_rating", "?"),
                "",
                "&8Select a lower unlocked tier",
                "&8Or reset to None — always free"
        )));
        put(holder, inv, 8, tipBtn(Material.BARRIER, "&cReset to None",
                List.of("&7Clear active tier", "&8Unlocks & coins kept", "&8Always free")),
                SlotAction.act("lower_tier", "0", "lower"));

        placeTierItems(holder, inv, ph, false);
        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 40, pageBtn(Material.GOLD_INGOT, "&eBuy Tier",
                "&7Purchase a higher Unlock Tier"), SlotAction.page("buy"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory titles(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("titles");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Difficulty Titles"));
        holder.bind(inv);
        frame(inv, 45);

        put(holder, inv, 4, item(Material.NAME_TAG, "&d&lTitles", List.of(
                "",
                "&7Equipped &e" + blankAsNone(ph.getOrDefault("active_title", "")),
                "&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"),
                "&7CR &f" + ph.getOrDefault("combat_rating", "?"),
                "",
                "&8Tier titles need active tier + higher DMZ/Prestige",
                "&8Combat titles need harder kill feats"
        )));
        put(holder, inv, 8, tipBtn(Material.BARRIER, "&cClear Title",
                List.of("&7Unequip your title")), SlotAction.act("clear_title", "0", "titles"));

        String[] ids = {
                "t1_awakened", "t2_enhanced", "t3_elite", "t4_advanced",
                "t5_master", "t6_legendary", "t7_god",
                "boss_slayer", "elite_hunter", "ascendant"
        };
        Material[] mats = {
                Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND,
                Material.EMERALD, Material.NETHERITE_INGOT, Material.NETHER_STAR,
                Material.WITHER_SKELETON_SKULL, Material.DRAGON_HEAD, Material.ENCHANTED_GOLDEN_APPLE
        };
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 29, 31, 33};
        String equipped = ph.getOrDefault("active_title_id", "");
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            boolean earned = "true".equalsIgnoreCase(ph.getOrDefault("title_" + id + "_earned", "false"));
            String name = ph.getOrDefault("title_" + id + "_name", id);
            String tip = ph.getOrDefault("title_" + id + "_req", "");
            boolean isEquipped = id.equalsIgnoreCase(equipped);
            String title = isEquipped ? "&a● " + name : earned ? "&e" + name : "&8" + name;
            List<String> tipLore = new ArrayList<>();
            tipLore.add("&7" + tip);
            if (isEquipped) {
                tipLore.add("&aCurrently equipped &8· click to unequip");
            } else if (earned) {
                tipLore.add("&aUnlocked &8· click to equip");
            } else {
                tipLore.add("&cLocked");
            }
            if (earned) {
                put(holder, inv, slots[i], tipBtn(mats[i], title, tipLore),
                        SlotAction.act("equip_title", id, "titles"));
            } else {
                put(holder, inv, slots[i], item(mats[i], title, prependBlank(tipLore)));
            }
        }

        put(holder, inv, 36, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory teamsWip(Player player) {
        Holder holder = new Holder("team");
        Inventory inv = Bukkit.createInventory(holder, 27, color("&8Teams (WIP)"));
        holder.bind(inv);
        frame(inv, 27);
        put(holder, inv, 13, item(Material.COMPASS, "&8&lTeams — Work in Progress", List.of(
                "",
                "&7Team difficulty is not available yet.",
                "&eDifficulty is personal / individual only.",
                "",
                "&8No team actions can be taken from this menu."
        )));
        put(holder, inv, 18, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 26, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory stats(Player player) {
        if (!ForgeBridge.isStaff(player)) {
            return main(player);
        }
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("stats");
        Inventory inv = Bukkit.createInventory(holder, 36, color("&8Details (staff)"));
        holder.bind(inv);
        frame(inv, 36);
        String stateColor = ph.getOrDefault("state_color", "f");

        put(holder, inv, 11, item(Material.NETHER_STAR, "&f&lProgression", List.of(
                "",
                "&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"),
                "&7Combat CR   &f" + ph.getOrDefault("combat_rating", "?"),
                "&7State       &" + stateColor + ph.getOrDefault("state", "?"),
                "",
                "&7DMZ &f" + ph.getOrDefault("level", "?")
                        + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"),
                "&7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"),
                "&7Title &e" + blankAsNone(ph.getOrDefault("active_title", ""))
        )));
        put(holder, inv, 13, item(Material.IRON_SWORD, "&c&lCounters", List.of(
                "",
                "&7Class &f" + blankAsNone(ph.getOrDefault("fighting_class", "")),
                "&7Race  &f" + blankAsNone(ph.getOrDefault("race", "")),
                "&7Style &f" + ph.getOrDefault("fighting_style", "HYBRID"),
                "",
                "&7Top stats &f" + ph.getOrDefault("top_stats", "—"),
                "&7Weak     &f" + ph.getOrDefault("weak_stat", "NONE"),
                "",
                "&8Mobs counter class, race, and top 3 stats"
        )));
        List<String> coins = new ArrayList<>();
        coins.add("");
        coins.addAll(coinLore(ph));
        coins.add("");
        coins.add("&8Tier purchases: pay-up OK, no change returned");
        put(holder, inv, 15, item(Material.GOLD_INGOT, "&f&lAncient Coins", coins));
        put(holder, inv, 27, pageBtn(Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 35, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private static void placeTierItems(Holder holder, Inventory inv, Map<String, String> ph, boolean buyMode) {
        Material[] mats = {
                Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT,
                Material.DIAMOND, Material.EMERALD, Material.NETHERITE_INGOT, Material.NETHER_STAR
        };
        int activeTier = (int) parseLong(ph.getOrDefault("active_tier", "0"));
        int highest = (int) parseLong(ph.getOrDefault("highest_unlocked", "0"));
        for (int t = 1; t <= 7; t++) {
            String cost = ph.getOrDefault("tier_" + t + "_cost", "?");
            String name = ph.getOrDefault("tier_" + t + "_name", "T" + t);
            boolean unlocked = highest >= t;
            boolean active = activeTier == t;
            List<String> tip = new ArrayList<>();
            tip.add("&7" + name);
            if (buyMode) {
                tip.add("&7Cost &e" + cost);
                tip.add("&8Scaled for your DMZ level");
            }
            if (active) {
                tip.add("&aCurrently active");
            } else if (buyMode && unlocked) {
                tip.add("&aUnlocked &8· click to purchase");
                tip.add("&8Pay-up OK · no change");
            } else if (!buyMode && unlocked && t < activeTier) {
                tip.add("&aOwned &8· click to lower here");
            } else if (!unlocked) {
                tip.add("&cLocked &8· need DMZ level or Prestige " + t);
            } else if (!buyMode) {
                tip.add("&8Higher than current — use Buy");
            }

            String title;
            if (active) {
                title = "&a● T" + t + " Active";
            } else if (buyMode) {
                title = unlocked ? "&eBuy T" + t : "&8Locked T" + t;
            } else {
                title = (unlocked && t < activeTier) ? "&fLower to T" + t
                        : unlocked ? "&8T" + t : "&8Locked T" + t;
            }

            boolean clickable = buyMode
                    ? (unlocked && !active)
                    : (unlocked && t < activeTier);
            if (clickable) {
                String action = buyMode ? "activate" : "lower_tier";
                String page = buyMode ? "buy" : "lower";
                put(holder, inv, TIER_SLOTS[t - 1], tipBtn(mats[t - 1], title, tip),
                        SlotAction.act(action, String.valueOf(t), page));
            } else {
                put(holder, inv, TIER_SLOTS[t - 1], item(mats[t - 1], title, prependBlank(tip)));
            }
        }
    }

    private static List<String> prependBlank(List<String> tip) {
        List<String> out = new ArrayList<>();
        out.add("");
        out.addAll(tip);
        return out;
    }

    /** Clean copy for players; command tips only for staff. */
    private static List<String> unavailableLore(Player player, boolean systemOn, boolean bridgeOk) {
        boolean staff = ForgeBridge.isStaff(player);
        if (!bridgeOk) {
            if (staff) {
                return List.of("", "&cForge Adaptive Difficulty mod unreachable",
                        "&7Check mods/ for dmz_adaptive_difficulty",
                        "&8GUI actions are disabled until the mod loads");
            }
            return List.of("", "&cAdaptive Difficulty is unavailable", "&7Please try again later");
        }
        if (!systemOn) {
            if (staff) {
                return List.of("", "&cAdaptive Difficulty is off",
                        "&7No scaling, coins, or purchases",
                        "&8Re-enable: &f/difficulty admin on");
            }
            return List.of("", "&cAdaptive Difficulty is off", "&7Please try again later");
        }
        if (staff) {
            return List.of("", "&eTesting whitelist is on", "&7You are not on the whitelist",
                    "&8Add: &f/difficulty admin whitelist add <you>");
        }
        return List.of("", "&eNot available right now", "&7Ask an admin if you need access");
    }

    private static List<String> statusLore(
            Map<String, String> ph, String stateColor, boolean staff, boolean personalOn) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        if (!personalOn) {
            lore.add("&cDifficulty is OFF for you");
            lore.add("&7No scaling, kill coins, AI, or tier buys");
            lore.add("&8Saved tier &f" + ph.getOrDefault("active_tier_name", "None")
                    + "  &8·  &7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"));
            lore.add("&7State &cOff");
        } else {
            lore.add("&7Tier &f" + ph.getOrDefault("active_tier_name", "None")
                    + "  &8·  &7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"));
            lore.add("&7State &" + stateColor + ph.getOrDefault("state", "?"));
        }
        String title = blankAsNone(ph.getOrDefault("active_title", ""));
        if (!"None".equals(title)) {
            lore.add("&7Title &e" + title);
        }
        lore.add("");
        lore.addAll(coinLore(ph));
        if (staff) {
            lore.add("");
            lore.add("&8CR &f" + ph.getOrDefault("combat_rating", "?")
                    + "  &8DMZ &f" + ph.getOrDefault("level", "?")
                    + "  &8Prestige &f" + ph.getOrDefault("prestige", "?"));
        }
        lore.add("");
        lore.add(personalOn
                ? "&8Buy a higher tier · Lower to step down"
                : "&8Turn Difficulty ON below to resume");
        return lore;
    }

    private static List<String> coinLore(Map<String, String> ph) {
        return List.of(
                "&6Ancient Coins",
                "&eCopper &f" + ph.getOrDefault("coins_copper", "0")
                        + "  &eIron &f" + ph.getOrDefault("coins_iron", "0")
                        + "  &eGold &f" + ph.getOrDefault("coins_gold", "0"),
                "&eEmerald &f" + ph.getOrDefault("coins_emerald", "0")
                        + "  &eDiamond &f" + ph.getOrDefault("coins_diamond", "0")
                        + "  &eNetherite &f" + ph.getOrDefault("coins_netherite", "0"),
                "&6Total &f" + ph.getOrDefault("ancient_coins", "0") + " AC"
        );
    }

    private static String blankAsNone(String value) {
        return value == null || value.isBlank() ? "None" : value;
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
        // Only top inventory slots we registered — ignore player inv / lore spoofing.
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
        if (slotAction.action == null || slotAction.action.isBlank()) {
            return;
        }
        final String ret = slotAction.returnPage == null || slotAction.returnPage.isBlank()
                ? "main" : slotAction.returnPage;
        final String cmd = "difficulty do " + slotAction.action
                + (slotAction.arg == null || slotAction.arg.isBlank() ? " 0" : " " + slotAction.arg)
                + " " + ret;
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            player.performCommand(cmd);
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

    private static long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }

    private static final class SlotAction {
        final String action;
        final String arg;
        final String returnPage;
        final String page;
        final boolean shouldClose;

        private SlotAction(String action, String arg, String returnPage, String page, boolean shouldClose) {
            this.action = action;
            this.arg = arg;
            this.returnPage = returnPage;
            this.page = page;
            this.shouldClose = shouldClose;
        }

        static SlotAction act(String action, String arg, String returnPage) {
            return new SlotAction(action, arg, returnPage, null, false);
        }

        static SlotAction page(String page) {
            return new SlotAction(null, null, null, page, false);
        }

        static SlotAction dismiss() {
            return new SlotAction(null, null, null, null, true);
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
