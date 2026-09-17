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
            Map.entry("meditation", new String[]{"Meditation", "Charge Ki in the trial biome and meet the trial (/progression meditation).", "/progression meditation"}),
            Map.entry("potential", new String[]{"Potential", "Spar others to raise it (soft-caps at 10 until you beat Piccolo in the skill saga, then to 30).", "/skillcheck"}),
            Map.entry("farming", new String[]{"Farming TP", "Break mature crops / Pam's harvest for TP.", "Passive while farming"}),
            Map.entry("building", new String[]{"Building TP", "Place blocks for silent building TP.", "Passive while placing"}),
            Map.entry("boost", new String[]{"Global TP Boost", "Timed world TP multiplier.", "/progression boost start|end"}),
            Map.entry("bio", new String[]{"Bio-Android", "Absorb TP / steal skills from drains.", "Passive as Bio-Android"}),
            Map.entry("racelock", new String[]{"Race Lock", "Ancient/Sento need Fabled unlock skills.", "Passive on race select"}),
            Map.entry("yardrat", new String[]{"Yardrat", "Form mastery double-gain + starter ki.", "Passive for Yardrat"}),
            Map.entry("spiritualist", new String[]{"Spiritualist Ki", "Class confirm grants/removes kicontrol.", "Passive on class change"}),
            Map.entry("android", new String[]{"Android Tools", "Staff convert or remove Android upgrade (Gero path).", "/progression android · android remove"}),
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
            case "skills" -> sectionFlags(viewer, subject, "skills", "&eSkills", Material.BOOK,
                    new String[]{"flight", "sprint", "meditation", "potential"});
            case "tp" -> sectionFlags(viewer, subject, "tp", "&6TP Gains", Material.GOLDEN_CARROT,
                    new String[]{"farming", "building", "boost", "bio"});
            case "boost_panel", "tpboost" ->
                    ForgeBridge.isStaff(viewer) ? boostPanel(viewer, subject) : main(viewer, subject);
            case "race" -> sectionFlags(viewer, subject, "race", "&bRace & Form", Material.TOTEM_OF_UNDYING,
                    new String[]{"racelock", "yardrat", "spiritualist", "android"});
            case "android_panel", "android_tools", "androidtools" ->
                    ForgeBridge.isStaff(viewer) ? androidPanel(viewer, subject) : main(viewer, subject);
            case "android_convert", "androidconvert", "convert_android" ->
                    ForgeBridge.isStaff(viewer) ? androidConvertPicker(viewer, subject) : main(viewer, subject);
            case "android_remove", "androidremove", "remove_android", "deandroid" ->
                    androidRemovePicker(viewer, subject);
            case "combat" -> sectionFlags(viewer, subject, "combat", "&cCombat", Material.NETHERITE_SWORD,
                    new String[]{"kiweapons", "piercing", "dot", "apothic"});
            case "end" -> sectionFlags(viewer, subject, "end", "&5End", Material.END_CRYSTAL,
                    new String[]{"end", "endportal"});
            case "fabled" -> sectionFlags(viewer, subject, "fabled", "&dFabled Bridges", Material.AMETHYST_SHARD,
                    new String[]{"fabled"});
            case "utility" -> sectionFlags(viewer, subject, "utility", "&7Utility", Material.SPYGLASS,
                    new String[]{"shadow", "statchecker"});
            case "status" -> sectionFlags(viewer, subject, "status", "&eStatus", Material.WRITABLE_BOOK,
                    new String[]{"flight", "sprint", "meditation", "potential", "farming", "building"});
            case "economy", "ancient_coins", "coins" ->
                    ForgeBridge.isStaff(viewer) ? economy(viewer, subject) : main(viewer, subject);
            case "admin", "flags", "disable" -> ForgeBridge.isStaff(viewer) ? flags(viewer, subject) : main(viewer, subject);
            case "flags_fabled", "fabled_flags" -> ForgeBridge.isStaff(viewer) ? fabledFlags(viewer, subject) : main(viewer, subject);
            default -> main(viewer, subject);
        };
        GuiFeedback.openChest(viewer, inv);
    }

    private Inventory main(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(subject);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 54, invTitle(viewer, subject, "&8Progression"));
        holder.bind(inv);
        frame(inv, 54);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        if (!bridgeOk || !systemOn) {
            put(holder, inv, 4, item(Material.BREWING_STAND,
                    !bridgeOk ? "&c&lUNAVAILABLE" : "&c&lPROGRESSION DISABLED",
                    unavailableLore(bridgeOk)));
            put(holder, inv, 49, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
            put(holder, inv, 53, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        put(holder, inv, 4, item(Material.BREWING_STAND, "&d&lProgression",
                List.of("", "&7Pick a section", "&7Toggle flags inside each page")));

        int[] slots = GuiBoardHelper.centeredSlots(7);
        String[] pages = {"skills", "tp", "race", "combat", "end", "fabled", "utility"};
        Material[] mats = {
                Material.BOOK, Material.GOLDEN_CARROT, Material.TOTEM_OF_UNDYING,
                Material.NETHERITE_SWORD, Material.END_CRYSTAL, Material.AMETHYST_SHARD, Material.SPYGLASS
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
            put(holder, inv, slots[i], tipBtn("progression.main." + pages[i], mats[i], titles[i],
                    List.of(tips[i], "", "&eOpen")),
                    SlotAction.page(pages[i]));
        }

        if (ForgeBridge.isStaff(viewer)) {
            put(holder, inv, 40, tipBtn("progression.main.economy", Material.GOLD_INGOT, "&6Ancient Coins",
                    List.of("&7Staff pricing for all LM paid features",
                            "&8Tiers · Character Services · End dragon · …",
                            "&eOpen")),
                    SlotAction.page("economy"));
            put(holder, inv, 41, tipBtn("progression.main.admin", Material.REPEATER, "&cAll Flags",
                    List.of("&7Full flag board")), SlotAction.page("admin"));
        }
        put(holder, inv, 49, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 53, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory sectionFlags(Player viewer, Player subject, String page, String title, Material mat, String[] keys) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(subject);
        Holder holder = new Holder(page);
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Progression"));
        holder.bind(inv);
        frame(inv, 45);
        boolean staff = ForgeBridge.isStaff(viewer);
        put(holder, inv, 4, item(mat, title, List.of("",
                staff ? "&7Click a module to toggle ON/OFF" : "&7Module status",
                staff ? "&8Description + commands on each item" : "&8Player-facing modules")));
        int[] slots = GuiBoardHelper.centeredSlots(keys.length);
        for (int i = 0; i < keys.length && i < slots.length; i++) {
            String key = keys[i];
            boolean on = "true".equalsIgnoreCase(ph.getOrDefault("flag_" + key, "false"));
            String[] info = FLAG_INFO.getOrDefault(key, new String[]{key, "Progression module.", ""});
            // Race page: Android opens convert/remove tools (not just the enable flag).
            if ("android".equals(key) && "race".equals(page)) {
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(on ? "&aModule enabled" : "&cModule disabled");
                lore.add(staff
                        ? "&7Convert or remove Android upgrade (Gero path)."
                        : "&7Android upgrade path (Gero).");
                if (staff) {
                    lore.add("&8Cmd: &f/progression android [player]");
                    lore.add("&8Cmd: &f/progression android remove [player]");
                    lore.add("");
                    lore.add("&eClick · Convert / Remove");
                }
                ItemStack stack = tipBtn("progression.race.android_tools", Material.IRON_INGOT,
                        staff ? "&bAndroid Tools" : "&bAndroid", lore);
                if (staff) {
                    put(holder, inv, slots[i], stack, SlotAction.page("android_panel"));
                } else {
                    put(holder, inv, slots[i], stack);
                }
                continue;
            }
            // TP page: Boost opens start/end panel (flag stays on Flags board).
            if ("boost".equals(key) && "tp".equals(page)) {
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(on ? "&aModule enabled" : "&cModule disabled");
                lore.add("&7Timed world TP multiplier.");
                if (staff) {
                    lore.add("&8Cmd: &f/progression boost start|end");
                    lore.add("");
                    lore.add("&eClick · start / end boost");
                }
                ItemStack stack = tipBtn("progression.tp.boost_panel", Material.GOLDEN_APPLE,
                        "&6Global TP Boost", lore);
                if (staff) {
                    put(holder, inv, slots[i], stack, SlotAction.page("boost_panel"));
                } else {
                    put(holder, inv, slots[i], stack);
                }
                continue;
            }
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(on ? "&aEnabled" : "&cDisabled");
            lore.add("&7" + info[1]);
            // Staff commands never appear on non-op lore.
            if (staff && info.length > 2 && info[2] != null && !info[2].isBlank()) {
                lore.add("&8Cmd: &f" + info[2]);
            }
            if (staff) {
                lore.add("");
                lore.add("&eSelect to switch");
            }
            String flagTitle = (on ? "&a" : "&8") + info[0] + (on ? " ON" : " OFF");
            Map<String, String> flagVars = Map.of(
                    "name", flagTitle,
                    "title", info[0],
                    "status", on ? "ON" : "OFF",
                    "desc", info[1]);
            ItemStack stack = tipBtn("progression." + page + ".flag",
                    on ? Material.LIME_DYE : Material.GRAY_DYE, flagTitle, lore, flagVars);
            if (staff) {
                put(holder, inv, slots[i], stack, SlotAction.act("flag", key, page));
            } else {
                put(holder, inv, slots[i], stack);
            }
        }
        put(holder, inv, 36, pageBtn("common.back", Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory economy(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(subject);
        boolean staffFree = "true".equalsIgnoreCase(ph.getOrDefault("staff_free_ancient_coin_costs", "false"));
        Holder holder = new Holder("economy");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Ancient Coins"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.GOLD_INGOT, "&6&lAncient Coin Economy",
                List.of("",
                        "&7Server-wide staff pricing for anything",
                        "&7that charges Ancient Coins in LM.",
                        "",
                        "&8Includes: AD tiers, Character Services,",
                        "&8End dragon summon, cosmetic head bones,",
                        "&8and future paid LM features.",
                        "",
                        staffFree ? "&aStaff free costs: ON" : "&7Staff free costs: OFF")));
        Map<String, String> staffFreeVars = Map.of(
                "action", staffFree ? "&8Tap to turn OFF" : "&8Tap to turn ON");
        put(holder, inv, 22, tipBtn("progression.economy.staff_free",
                staffFree ? Material.LIME_DYE : Material.GRAY_DYE,
                staffFree ? "&aStaff free coins ON" : "&7Staff free coins OFF",
                List.of(
                        "&7When ON, staff and OP pay no Ancient Coins",
                        "&7on any LM paid feature.",
                        "",
                        staffFreeVars.get("action")),
                staffFreeVars),
                SlotAction.act("toggle_staff_free_coins", staffFree ? "off" : "on", "economy"));
        put(holder, inv, 36, pageBtn("common.back", Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory flags(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(subject);
        Holder holder = new Holder("admin");
        Inventory inv = Bukkit.createInventory(holder, 54, invTitle(viewer, subject, "&8Flags"));
        holder.bind(inv);
        frame(inv, 54);
        put(holder, inv, 4, item(Material.REPEATER, "&c&lStaff Flags",
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
                String flagTitle = (on ? "&a" : "&8") + key + (on ? " ON" : " OFF");
                Map<String, String> flagVars = Map.of(
                        "name", flagTitle,
                        "title", key,
                        "status", on ? "ON" : "OFF",
                        "desc", stripAmp(sectionTitle));
                put(holder, inv, slots[si++], tipBtn("progression.admin.flag",
                        on ? Material.LIME_DYE : Material.GRAY_DYE,
                        flagTitle,
                        List.of("&8" + stripAmp(sectionTitle), "&7Click to toggle " + key), flagVars),
                        SlotAction.act("flag", key, "admin"));
            }
        }
        put(holder, inv, 47, pageBtn("progression.fabled.subflags", Material.AMETHYST_SHARD, "&dFabled Subflags",
                "&7Energy, TP/SP, race class, etc."), SlotAction.page("flags_fabled"));
        put(holder, inv, 45, pageBtn("common.back", Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 49, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 53, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory fabledFlags(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(subject);
        Holder holder = new Holder("flags_fabled");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Fabled Flags"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.AMETHYST_SHARD, "&d&lFabled Subflags",
                List.of("", "&7Soft-dependency bridge toggles", "&7Click to toggle")));
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30};
        for (int i = 0; i < FABLED_FLAG_KEYS.length && i < slots.length; i++) {
            String key = FABLED_FLAG_KEYS[i];
            boolean on = "true".equalsIgnoreCase(ph.getOrDefault("flag_" + key, "false"));
            String flagTitle = (on ? "&a" : "&8") + key + (on ? " ON" : " OFF");
            Map<String, String> flagVars = Map.of(
                    "name", flagTitle,
                    "title", key,
                    "status", on ? "ON" : "OFF",
                    "desc", "Fabled");
            put(holder, inv, slots[i], tipBtn("progression.fabled.flag",
                    on ? Material.LIME_DYE : Material.GRAY_DYE,
                    flagTitle,
                    List.of("&8Fabled", "&7Click to toggle " + key), flagVars),
                    SlotAction.act("flag", key, "flags_fabled"));
        }
        put(holder, inv, 36, pageBtn("progression.fabled.back", Material.ARROW, "&7Back", "&7Return to Flags"),
                SlotAction.page("admin"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory boostPanel(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(subject);
        Holder holder = new Holder("boost_panel");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8TP Boost"));
        holder.bind(inv);
        frame(inv, 45);
        String status = ph.getOrDefault("boost", "§7Global TP boost: §cOFF").replace('§', '&');
        put(holder, inv, 4, item(Material.GOLDEN_APPLE, "&6&lGlobal TP Boost",
                List.of("", status,
                        "&8/progression boost",
                        "&8/progression boost start <mult> <min>",
                        "&8/progression boost end")));
        // Presets: mult:minutes
        put(holder, inv, 19, tipBtn("progression.boost.n125_30", Material.GOLD_NUGGET, "&e1.25x · 30m",
                List.of("&7Start 1.25x for 30 minutes", "", "&eStart")),
                SlotAction.act("boost", "1.25:30", "boost_panel"));
        put(holder, inv, 20, tipBtn("progression.boost.n15_30", Material.GOLD_INGOT, "&e1.5x · 30m",
                List.of("&7Start 1.5x for 30 minutes", "", "&eStart")),
                SlotAction.act("boost", "1.5:30", "boost_panel"));
        put(holder, inv, 21, tipBtn("progression.boost.n2_30", Material.GOLD_BLOCK, "&62x · 30m",
                List.of("&7Start 2x for 30 minutes", "", "&eStart")),
                SlotAction.act("boost", "2:30", "boost_panel"));
        put(holder, inv, 22, tipBtn("progression.boost.n2_60", Material.GOLD_BLOCK, "&62x · 60m",
                List.of("&7Start 2x for 60 minutes", "", "&eStart")),
                SlotAction.act("boost", "2:60", "boost_panel"));
        put(holder, inv, 23, tipBtn("progression.boost.n3_30", Material.CLOCK, "&e3x · 30m",
                List.of("&7Start 3x for 30 minutes", "", "&eStart")),
                SlotAction.act("boost", "3:30", "boost_panel"));
        put(holder, inv, 25, tipBtn("progression.boost.end", Material.BARRIER, "&cEnd Boost",
                List.of("&7Stop the active global TP boost", "", "&eEnd boost")),
                SlotAction.act("boost", "end", "boost_panel"));
        put(holder, inv, 31, tipBtn("progression.boost.refresh", Material.CLOCK, "&7Refresh Status",
                List.of("&7Reload this panel", "", "&eClick")),
                SlotAction.page("boost_panel"));
        put(holder, inv, 36, pageBtn("progression.boost.back", Material.ARROW, "&7Back", "&7TP Gains"),
                SlotAction.page("tp"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory androidPanel(Player viewer, Player subject) {
        Holder holder = new Holder("android_panel");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Android Tools"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.IRON_INGOT,
                GuiTooltips.name("progression.android.panel_header", "&b&lAndroid Tools"),
                GuiTooltips.buttonLore("progression.android.panel_header", List.of(
                        "&7Dr. Gero upgrade path",
                        "&7Convert or remove the Android upgrade",
                        "&8Race and stats stay on remove"))));
        put(holder, inv, 20, tipBtn("progression.android.convert_entry", Material.NETHERITE_INGOT, "&aConvert to Android",
                List.of("&7Gero upgrade — keeps your race",
                        "&7Unlocks the Android form path",
                        "&8Human · Saiyan · Frost Demon · Viltrumite",
                        "", "&eClick · choose player")),
                SlotAction.page("android_convert"));
        put(holder, inv, 24, tipBtn("progression.android.remove_entry", Material.REDSTONE, "&cRemove Android",
                List.of("&7Restore normal form skills",
                        "&8Confirm within 10s by clicking again",
                        "", "&eClick · choose player")),
                SlotAction.page("android_remove"));
        put(holder, inv, 36, pageBtn("progression.android.back", Material.ARROW, "&7Back", "&7Race section"),
                SlotAction.page("race"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory androidRemovePicker(Player viewer, Player subject) {
        Holder holder = new Holder("android_remove");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Remove Android"));
        holder.bind(inv);
        frame(inv, 45);
        boolean staff = ForgeBridge.isStaff(viewer);
        put(holder, inv, 4, item(Material.REDSTONE,
                GuiTooltips.name("progression.android.remove_header", "&c&lRemove Android"),
                GuiTooltips.buttonLore("progression.android.remove_header", List.of(
                        "&7Removes the Android upgrade",
                        "&7Restores normal form skills",
                        "&8Click twice within 10s to confirm",
                        staff
                                ? "&8/progression android remove [player]"
                                : "&8/progression android remove"))));
        // Self remove — available to everyone (subject when inspecting, else viewer).
        Player selfTarget = subject != null ? subject : viewer;
        put(holder, inv, staff ? 8 : 22, tipBtn("progression.android.remove_self", Material.NETHERITE_SCRAP,
                "&cRemove Android Upgrade",
                List.of("&7Remove the Android upgrade",
                        "&7Race, stats, and progression stay",
                        "", "&eClick · confirm within 10s")),
                SlotAction.act("android_remove", selfTarget.getName(), "android_remove"));
        if (staff) {
            List<Player> online = GuiPlayerPicker.onlineExcept(subject);
            int placed = 0;
            for (Player other : online) {
                if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                    break;
                }
                int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
                put(holder, inv, slot,
                        GuiPlayerPicker.head(other, "&f" + other.getName(),
                                List.of("&7Remove Android upgrade", "", "&eClick · confirm within 10s")),
                        SlotAction.act("android_remove", other.getName(), "android_remove"));
            }
            if (online.isEmpty()) {
                put(holder, inv, 22, item(Material.BARRIER, "&7No other players online",
                        List.of("", "&7Use Remove Yourself above",
                                "&8or /progression android remove <name>")));
            }
            put(holder, inv, 36, pageBtn("progression.android.remove_back", Material.ARROW, "&7Back",
                    "&7Android tools"), SlotAction.page("android_panel"));
        } else {
            put(holder, inv, 36, pageBtn("progression.android.remove_back_hub", Material.ARROW, "&7Back",
                    "&7Return to the main menu"), SlotAction.cmd("lmdo lm open hub"));
        }
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory androidConvertPicker(Player viewer, Player subject) {
        Holder holder = new Holder("android_convert");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Android Convert"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.IRON_INGOT,
                GuiTooltips.name("progression.android.convert_header", "&b&lAndroid Convert"),
                GuiTooltips.buttonLore("progression.android.convert_header", List.of(
                        "&7Gero upgrade — keeps your race",
                        "&7Unlocks the Android form path",
                        "&8Human · Saiyan · Frost Demon · Viltrumite",
                        "&8/progression android [player]"))));
        put(holder, inv, 8, tipBtn("progression.android.convert_self", Material.NETHERITE_INGOT,
                "&aConvert Yourself",
                List.of("&7Apply conversion to you", "", "&eConvert")),
                SlotAction.act("android", subject.getName(), "android_convert"));
        List<Player> online = GuiPlayerPicker.onlineExcept(subject);
        int placed = 0;
        for (Player other : online) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            put(holder, inv, slot,
                    GuiPlayerPicker.head(other, "&f" + other.getName(),
                            List.of("&7Convert to Android", "", "&eConvert")),
                    SlotAction.act("android", other.getName(), "android_convert"));
        }
        if (online.isEmpty()) {
            put(holder, inv, 22, item(Material.BARRIER, "&7No other players online",
                    List.of("", "&7Use Convert Yourself above",
                            "&8or /progression android <name>")));
        }
        put(holder, inv, 36, pageBtn("progression.android.convert_back", Material.ARROW, "&7Back",
                "&7Android tools"), SlotAction.page("android_panel"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
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
        final Player subject = AdminInspectSessions.resolveSubject(player);
        Bukkit.getScheduler().runTask(plugin, () -> {
            String msg = ForgeBridge.progressionHandleDo(subject, action, arg, ret);
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

    private static ItemStack tipBtn(Material mat, String name, List<String> tip) {
        return tipBtn(null, mat, name, tip, null);
    }

    private static ItemStack tipBtn(String key, Material mat, String name, List<String> tip) {
        return tipBtn(key, mat, name, tip, null);
    }

    private static ItemStack tipBtn(
            String key, Material mat, String name, List<String> tip, Map<String, String> vars
    ) {
        if (key == null || key.isBlank()) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            if (tip != null) {
                lore.addAll(tip);
            }
            return item(mat, name, lore);
        }
        return item(mat, GuiTooltips.name(key, name, vars), GuiTooltips.buttonLore(key, tip, vars, null));
    }

    private static ItemStack pageBtn(Material mat, String name, String... tips) {
        return pageBtn(null, mat, name, tips);
    }

    private static ItemStack pageBtn(String key, Material mat, String name, String... tips) {
        List<String> defaults = new ArrayList<>();
        if (tips != null) {
            for (String tip : tips) {
                if (tip != null) {
                    defaults.add(tip);
                }
            }
        }
        if (key == null || key.isBlank()) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.addAll(defaults);
            return item(mat, name, lore);
        }
        return item(mat, GuiTooltips.name(key, name), GuiTooltips.buttonLore(key, defaults));
    }

    private static ItemStack hubBtn() {
        return item(Material.COMPASS, "&7« Hub", List.of("", "&7Return to the main menu"));
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
