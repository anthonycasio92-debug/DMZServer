package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
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
 * Pages: Hub · Buy Tier · Lower Tier · Titles · Details. Teams = WIP only.
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
            case "stats", "statistics", "details" -> stats(player);
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

        boolean systemOn = !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "true"));
        boolean allowed = !"false".equalsIgnoreCase(ph.getOrDefault("player_allowed", "true"));
        if (!systemOn || !allowed) {
            inv.setItem(13, item(Material.NETHER_STAR,
                    !systemOn ? "&c&lSYSTEM DISABLED" : "&e&lWHITELIST ONLY",
                    !systemOn
                            ? List.of("", "&cAdaptive Difficulty is off", "&7An admin disabled the system",
                            "&8No scaling, coins, or purchases", "&8Re-enable: &f/difficulty admin on")
                            : List.of("", "&eTesting whitelist is on", "&7You are not on the whitelist",
                            "&8Ask an admin:", "&f/difficulty admin whitelist add <you>")));
            inv.setItem(31, button(Material.SUNFLOWER, "&7Refresh", "refresh", "0", "main",
                    List.of("&7Reload this menu")));
            inv.setItem(35, closeBtn());
            return inv;
        }

        String stateColor = ph.getOrDefault("state_color", "f");
        inv.setItem(13, item(Material.NETHER_STAR, "&f&lAdaptive Difficulty", statusLore(ph, stateColor)));
        inv.setItem(19, pageBtn(Material.GOLD_INGOT, "&eBuy Tier", "buy",
                "&7Purchase a higher Unlock Tier", "&8Exact Ancient Coins · scales with DMZ level"));
        inv.setItem(21, pageBtn(Material.WHITE_CONCRETE, "&fLower Tier", "lower",
                "&7Select a lower unlocked tier", "&8Or reset to None · always free"));
        inv.setItem(23, pageBtn(Material.NAME_TAG, "&dTitles", "titles",
                "&7Equip difficulty titles", "&8Higher CR / kill requirements"));
        inv.setItem(25, pageBtn(Material.COMPASS, "&8Teams &7(WIP)", "team",
                "&7Not available yet", "&ePersonal difficulty only"));

        inv.setItem(30, pageBtn(Material.BOOK, "&fDetails", "stats", "&7Coins, progression, title"));
        inv.setItem(31, button(Material.SUNFLOWER, "&7Refresh", "refresh", "0", "main",
                List.of("&7Reload this menu")));
        inv.setItem(35, closeBtn());
        return inv;
    }

    private Inventory buy(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("buy");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Buy Higher Tier"));
        holder.bind(inv);
        frame(inv, 45);

        List<String> info = new ArrayList<>();
        info.add("");
        info.add("&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"));
        info.add("&7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"));
        info.add("&7DMZ Level &f" + ph.getOrDefault("level", "?")
                + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"));
        info.add("");
        info.addAll(coinLore(ph));
        info.add("");
        info.add("&8Costs scale with your DMZ level");
        info.add("&8Exact coins only — no overpay / change");
        inv.setItem(4, item(Material.GOLD_INGOT, "&e&lBuy Higher Tier", info));

        placeTierItems(inv, ph, true);
        inv.setItem(36, pageBtn(Material.ARROW, "&7Back", "main", "&7Return"));
        inv.setItem(40, pageBtn(Material.WHITE_CONCRETE, "&fLower Tier", "lower",
                "&7Select a lower unlocked tier"));
        inv.setItem(44, closeBtn());
        return inv;
    }

    private Inventory lower(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("lower");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Lower Difficulty Tier"));
        holder.bind(inv);
        frame(inv, 45);

        inv.setItem(4, item(Material.NETHER_STAR, "&f&lLower Tier", List.of(
                "",
                "&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"),
                "&7Combat Rating &f" + ph.getOrDefault("combat_rating", "?"),
                "",
                "&8Select a lower unlocked tier",
                "&8Or reset to None — always free"
        )));
        inv.setItem(8, button(Material.BARRIER, "&cReset to None", "lower_tier", "0", "lower",
                List.of("&7Clear active tier", "&8Unlocks & coins kept", "&8Always free")));

        placeTierItems(inv, ph, false);
        inv.setItem(36, pageBtn(Material.ARROW, "&7Back", "main", "&7Return"));
        inv.setItem(40, pageBtn(Material.GOLD_INGOT, "&eBuy Tier", "buy",
                "&7Purchase a higher Unlock Tier"));
        inv.setItem(44, closeBtn());
        return inv;
    }

    private Inventory titles(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("titles");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Difficulty Titles"));
        holder.bind(inv);
        frame(inv, 45);

        inv.setItem(4, item(Material.NAME_TAG, "&d&lTitles", List.of(
                "",
                "&7Equipped &e" + blankAsNone(ph.getOrDefault("active_title", "")),
                "&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"),
                "&7CR &f" + ph.getOrDefault("combat_rating", "?"),
                "",
                "&8Tier titles need active tier + higher DMZ/Prestige",
                "&8Combat titles need harder kill feats"
        )));
        inv.setItem(8, button(Material.BARRIER, "&cClear Title", "clear_title", "0", "titles",
                List.of("&7Unequip your title")));

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
                inv.setItem(slots[i], button(mats[i], title, "equip_title", id, "titles", tipLore));
            } else {
                inv.setItem(slots[i], item(mats[i], title, prependBlank(tipLore)));
            }
        }

        inv.setItem(36, pageBtn(Material.ARROW, "&7Back", "main", "&7Return"));
        inv.setItem(44, closeBtn());
        return inv;
    }

    private Inventory teamsWip(Player player) {
        Holder holder = new Holder("team");
        Inventory inv = Bukkit.createInventory(holder, 27, color("&8Teams (WIP)"));
        holder.bind(inv);
        frame(inv, 27);
        inv.setItem(13, item(Material.COMPASS, "&8&lTeams — Work in Progress", List.of(
                "",
                "&7Team difficulty is not available yet.",
                "&eDifficulty is personal / individual only.",
                "",
                "&8No team actions can be taken from this menu."
        )));
        inv.setItem(18, pageBtn(Material.ARROW, "&7Back", "main", "&7Return"));
        inv.setItem(26, closeBtn());
        return inv;
    }

    private Inventory stats(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("stats");
        Inventory inv = Bukkit.createInventory(holder, 36, color("&8Details"));
        holder.bind(inv);
        frame(inv, 36);
        String stateColor = ph.getOrDefault("state_color", "f");

        inv.setItem(11, item(Material.NETHER_STAR, "&f&lProgression", List.of(
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
        inv.setItem(13, item(Material.COMPASS, "&8&lTeams (WIP)", List.of(
                "",
                "&7Not available yet.",
                "&ePersonal difficulty only."
        )));
        List<String> coins = new ArrayList<>();
        coins.add("");
        coins.addAll(coinLore(ph));
        coins.add("");
        coins.add("&8Tier purchases use exact Ancient Coins");
        inv.setItem(15, item(Material.GOLD_INGOT, "&f&lAncient Coins", coins));
        inv.setItem(27, pageBtn(Material.ARROW, "&7Back", "main", "&7Return"));
        inv.setItem(35, closeBtn());
        return inv;
    }

    private static void placeTierItems(Inventory inv, Map<String, String> ph, boolean buyMode) {
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
                tip.add("&8Exact Ancient Coins only");
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
                inv.setItem(TIER_SLOTS[t - 1], button(mats[t - 1], title, action, String.valueOf(t), page, tip));
            } else {
                inv.setItem(TIER_SLOTS[t - 1], item(mats[t - 1], title, prependBlank(tip)));
            }
        }
    }

    private static List<String> prependBlank(List<String> tip) {
        List<String> out = new ArrayList<>();
        out.add("");
        out.addAll(tip);
        return out;
    }

    private static List<String> statusLore(Map<String, String> ph, String stateColor) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"));
        lore.add("&7State  &" + stateColor + ph.getOrDefault("state", "?"));
        lore.add("&7CR     &f" + ph.getOrDefault("combat_rating", "?"));
        lore.add("&7Title  &e" + blankAsNone(ph.getOrDefault("active_title", "")));
        lore.add("");
        lore.addAll(coinLore(ph));
        lore.add("");
        lore.add("&7DMZ &f" + ph.getOrDefault("level", "?")
                + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"));
        lore.add("&7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"));
        lore.add("&8Buy a higher tier · Lower to step down");
        return lore;
    }

    private static List<String> coinLore(Map<String, String> ph) {
        return List.of(
                "&6Ancient Coins",
                "&eCopper &f" + ph.getOrDefault("coins_copper", "0")
                        + "  &eIron &f" + ph.getOrDefault("coins_iron", "0")
                        + "  &eGold &f" + ph.getOrDefault("coins_gold", "0"),
                "&eDiamond &f" + ph.getOrDefault("coins_diamond", "0")
                        + "  &eEmerald &f" + ph.getOrDefault("coins_emerald", "0")
                        + "  &eNetherite &f" + ph.getOrDefault("coins_netherite", "0"),
                "&eLapis &f" + ph.getOrDefault("coins_lapis", "0")
                        + "  &eDivine &f" + ph.getOrDefault("coins_divine", "0")
                        + "  &6Total &f" + ph.getOrDefault("ancient_coins", "0") + " AC"
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
        if (!(event.getInventory().getHolder() instanceof Holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta().getLore() == null) {
            return;
        }
        String action = null;
        String arg = null;
        String page = null;
        String returnPage = null;
        for (String line : clicked.getItemMeta().getLore()) {
            String plain = strip(line);
            if (plain.startsWith("ACTION:")) {
                action = plain.substring("ACTION:".length());
            } else if (plain.startsWith("ARG:")) {
                arg = plain.substring("ARG:".length());
            } else if (plain.startsWith("PAGE:")) {
                page = plain.substring("PAGE:".length());
            } else if (plain.startsWith("RETURN:")) {
                returnPage = plain.substring("RETURN:".length());
            } else if (plain.equals("CLOSE")) {
                player.closeInventory();
                return;
            }
        }
        if (page != null) {
            final String targetPage = page;
            Bukkit.getScheduler().runTask(plugin, () -> open(player, targetPage));
            return;
        }
        if (action == null) {
            return;
        }
        final String ret = returnPage == null || returnPage.isBlank() ? "main" : returnPage;
        final String cmd = "difficulty do " + action
                + (arg == null || arg.isBlank() ? " 0" : " " + arg)
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

    private static ItemStack button(
            Material mat, String name, String action, String arg, String returnPage, List<String> tip) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(tip);
        lore.add("&8ACTION:" + action);
        lore.add("&8ARG:" + arg);
        lore.add("&8RETURN:" + returnPage);
        return item(mat, name, lore);
    }

    private static ItemStack pageBtn(Material mat, String name, String page, String... tips) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String tip : tips) {
            lore.add(tip);
        }
        lore.add("&8PAGE:" + page);
        return item(mat, name, lore);
    }

    private static ItemStack closeBtn() {
        return item(Material.BARRIER, "&cClose", List.of("&8CLOSE"));
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

    private static String strip(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("§[0-9A-FK-ORa-fk-or]", "").replaceAll("&[0-9A-FK-ORa-fk-or]", "");
    }

    static final class Holder implements InventoryHolder {
        final String page;
        Inventory inventory;

        Holder(String page) {
            this.page = page;
        }

        void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
