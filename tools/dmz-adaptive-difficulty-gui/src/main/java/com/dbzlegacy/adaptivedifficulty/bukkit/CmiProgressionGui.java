package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.Zrips.CMILib.GUI.CMIGui;
import net.Zrips.CMILib.GUI.CMIGuiButton;
import net.Zrips.CMILib.GUI.GUIManager.InvType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * CMILib inventory GUI — Legacy Mechanics Progression.
 * Section hub with per-section toggleable flags (no Shop / Prestige / Skills openers).
 */
public final class CmiProgressionGui {
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

    private CmiProgressionGui() {}

    public static boolean available() {
        return CmiDifficultyGui.available();
    }

    public static boolean open(Player player, String page) {
        if (player == null || !available()) {
            return false;
        }
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        try {
            switch (p) {
                case "skills" -> openSection(player, "skills", "&eSkills", Material.BOOK,
                        new String[]{"flight", "sprint", "meditation", "potential"});
                case "tp" -> openSection(player, "tp", "&6TP Gains", Material.GOLDEN_CARROT,
                        new String[]{"farming", "building", "boost", "bio"});
                case "boost_panel", "tpboost" -> {
                    if (ForgeBridge.isStaff(player)) {
                        openBoostPanel(player);
                    } else {
                        openMain(player);
                    }
                }
                case "race" -> openSection(player, "race", "&bRace & Form", Material.TOTEM_OF_UNDYING,
                        new String[]{"racelock", "yardrat", "spiritualist", "android"});
                case "android_panel", "android_tools", "androidtools" -> {
                    if (ForgeBridge.isStaff(player)) {
                        openAndroidPanel(player);
                    } else {
                        openMain(player);
                    }
                }
                case "android_convert", "androidconvert", "convert_android" -> {
                    if (ForgeBridge.isStaff(player)) {
                        openAndroidConvert(player);
                    } else {
                        openMain(player);
                    }
                }
                case "android_remove", "androidremove", "remove_android", "deandroid" ->
                        openAndroidRemove(player);
                case "combat" -> openSection(player, "combat", "&cCombat", Material.NETHERITE_SWORD,
                        new String[]{"kiweapons", "piercing", "dot", "apothic"});
                case "end" -> openSection(player, "end", "&5End", Material.END_CRYSTAL,
                        new String[]{"end", "endportal"});
                case "fabled" -> openSection(player, "fabled", "&dFabled Bridges", Material.AMETHYST_SHARD,
                        new String[]{"fabled"});
                case "utility" -> openSection(player, "utility", "&7Utility", Material.SPYGLASS,
                        new String[]{"shadow", "statchecker"});
                case "status" -> openSection(player, "status", "&eStatus", Material.WRITABLE_BOOK,
                        new String[]{"flight", "sprint", "meditation", "potential", "farming", "building"});
                case "economy", "ancient_coins", "coins" -> {
                    if (ForgeBridge.isStaff(player)) {
                        openEconomy(player);
                    } else {
                        openMain(player);
                    }
                }
                case "admin", "flags", "disable" -> {
                    if (ForgeBridge.isStaff(player)) {
                        openFlags(player);
                    } else {
                        openMain(player);
                    }
                }
                case "flags_fabled", "fabled_flags" -> {
                    if (ForgeBridge.isStaff(player)) {
                        openFabledFlags(player);
                    } else {
                        openMain(player);
                    }
                }
                default -> openMain(player);
            }
            return true;
        } catch (Throwable t) {
            player.sendMessage("§cProgression CMI GUI failed: " + t.getMessage());
            return false;
        }
    }

    private static void openMain(Player player) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(player);
        CMIGui gui = base(player, "&8Progression", 6);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        CMIGuiButton status = new CMIGuiButton(4, Material.BREWING_STAND,
                !bridgeOk ? "&c&lUNAVAILABLE"
                        : !systemOn ? "&c&lPROGRESSION DISABLED"
                        : "&d&lProgression");
        status.lockField();
        if (!bridgeOk || !systemOn) {
            status.addLore(unavailableLore(bridgeOk));
            gui.addButton(status);
            gui.addButton(hubBtn(49));
            gui.addButton(closeBtn(53));
            fillEmpty(gui, 6);
            GuiFeedback.openCmi(gui);
            return;
        }
        status.addLore(List.of("", "&7Pick a section", "&7Toggle flags inside each page"));
        gui.addButton(status);

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
            gui.addButton(pageBtn(slots[i], "progression.main." + pages[i], mats[i], titles[i], pages[i],
                    tips[i], "", "&eOpen"));
        }

        if (ForgeBridge.isStaff(player)) {
            gui.addButton(pageBtn(40, "progression.main.economy", Material.GOLD_INGOT, "&6Ancient Coins", "economy",
                    "&7Staff pricing for all LM Ancient Coin features", "", "&eOpen"));
            gui.addButton(pageBtn(41, "progression.main.admin", Material.REPEATER, "&cAll Flags", "admin",
                    "&7Full flag board"));
        }

        gui.addButton(hubBtn(49));
        gui.addButton(closeBtn(53));
        fillEmpty(gui, 6);
        GuiFeedback.openCmi(gui);
    }

    private static void openSection(Player player, String page, String title, Material mat, String[] keys) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(player);
        CMIGui gui = base(player, "&8Progression", 5);
        boolean staff = ForgeBridge.isStaff(player);
        CMIGuiButton header = new CMIGuiButton(4, mat, title);
        header.lockField();
        header.addLore(List.of("",
                staff ? "&7Click a module to toggle ON/OFF" : "&7Module status",
                staff ? "&8Description + commands on each item" : "&8Player-facing modules"));
        gui.addButton(header);

        int[] slots = GuiBoardHelper.centeredSlots(keys.length);
        for (int i = 0; i < keys.length && i < slots.length; i++) {
            String key = keys[i];
            boolean on = "true".equalsIgnoreCase(ph.getOrDefault("flag_" + key, "false"));
            String[] info = FLAG_INFO.getOrDefault(key, new String[]{key, "Progression module.", ""});
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
                    lore.add("&eClick · Android tools");
                }
                if (staff) {
                    gui.addButton(pageBtn(slots[i], "progression.race.android_tools", Material.IRON_INGOT,
                            "&bAndroid Tools", "android_panel", lore.toArray(new String[0])));
                } else {
                    CMIGuiButton btn = new CMIGuiButton(slots[i], Material.IRON_INGOT,
                            GuiTooltips.name("progression.race.android_tools", "&bAndroid"));
                    btn.lockField();
                    btn.addLore(GuiTooltips.buttonLore("progression.race.android_tools", lore));
                    gui.addButton(btn);
                }
                continue;
            }
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
                if (staff) {
                    gui.addButton(pageBtn(slots[i], "progression.tp.boost_panel", Material.GOLDEN_APPLE,
                            "&6Global TP Boost", "boost_panel", lore.toArray(new String[0])));
                } else {
                    CMIGuiButton btn = new CMIGuiButton(slots[i], Material.GOLDEN_APPLE,
                            GuiTooltips.name("progression.tp.boost_panel", "&6Global TP Boost"));
                    btn.lockField();
                    btn.addLore(GuiTooltips.buttonLore("progression.tp.boost_panel", lore));
                    gui.addButton(btn);
                }
                continue;
            }
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(on ? "&aEnabled" : "&cDisabled");
            lore.add("&7" + info[1]);
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
            if (staff) {
                gui.addButton(actionBtn(slots[i], "progression." + page + ".flag",
                        on ? Material.LIME_DYE : Material.GRAY_DYE,
                        flagTitle, "flag", key, page, lore, flagVars));
            } else {
                CMIGuiButton btn = new CMIGuiButton(slots[i],
                        on ? Material.LIME_DYE : Material.GRAY_DYE,
                        GuiTooltips.name("progression." + page + ".flag", flagTitle));
                btn.lockField();
                btn.addLore(GuiTooltips.buttonLore("progression." + page + ".flag", lore, flagVars, null));
                gui.addButton(btn);
            }
        }

        if ("fabled".equals(page) && staff) {
            gui.addButton(pageBtn(31, "progression.fabled.subflags", Material.AMETHYST_SHARD, "&dFabled Subflags",
                    "flags_fabled", "&7Energy, TP/SP, race class, etc."));
        }

        gui.addButton(pageBtn(36, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openEconomy(Player player) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(player);
        boolean staffFree = "true".equalsIgnoreCase(ph.getOrDefault("staff_free_ancient_coin_costs", "false"));
        CMIGui gui = base(player, "&8Ancient Coins", 5);
        CMIGuiButton header = new CMIGuiButton(4, Material.GOLD_INGOT, "&6&lAncient Coin Economy");
        header.lockField();
        header.addLore(List.of("",
                "&7Server-wide staff pricing for LM features",
                "&7that charge Ancient Coins.",
                "",
                "&8Tiers · Character Services · End dragon ·",
                "&8head bones · future paid LM features.",
                "",
                staffFree ? "&aStaff free costs: ON" : "&7Staff free costs: OFF"));
        gui.addButton(header);
        Map<String, String> staffFreeVars = Map.of(
                "action", staffFree ? "&8Tap to turn OFF" : "&8Tap to turn ON");
        gui.addButton(actionBtn(22, "progression.economy.staff_free",
                staffFree ? Material.LIME_DYE : Material.GRAY_DYE,
                staffFree ? "&aStaff free coins ON" : "&7Staff free coins OFF",
                "toggle_staff_free_coins", staffFree ? "off" : "on", "economy",
                List.of(
                        "&7When ON, staff and OP pay no Ancient Coins",
                        "&7on any LM paid feature.",
                        "",
                        staffFreeVars.get("action")),
                staffFreeVars));
        gui.addButton(pageBtn(36, "common.back", Material.ARROW, "&7Back", "main", "&7Progression hub"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openBoostPanel(Player player) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(player);
        CMIGui gui = base(player, "&8TP Boost", 5);
        String status = ph.getOrDefault("boost", "§7Global TP boost: §cOFF").replace('§', '&');
        CMIGuiButton header = new CMIGuiButton(4, Material.GOLDEN_APPLE, "&6&lGlobal TP Boost");
        header.lockField();
        header.addLore(List.of("", status,
                "&8/progression boost",
                "&8/progression boost start <mult> <min>",
                "&8/progression boost end"));
        gui.addButton(header);
        gui.addButton(actionBtn(19, "progression.boost.n125_30", Material.GOLD_NUGGET, "&e1.25x · 30m",
                "boost", "1.25:30", "boost_panel",
                List.of("&7Start 1.25x for 30 minutes", "", "&eStart")));
        gui.addButton(actionBtn(20, "progression.boost.n15_30", Material.GOLD_INGOT, "&e1.5x · 30m",
                "boost", "1.5:30", "boost_panel",
                List.of("&7Start 1.5x for 30 minutes", "", "&eStart")));
        gui.addButton(actionBtn(21, "progression.boost.n2_30", Material.GOLD_BLOCK, "&62x · 30m",
                "boost", "2:30", "boost_panel",
                List.of("&7Start 2x for 30 minutes", "", "&eStart")));
        gui.addButton(actionBtn(22, "progression.boost.n2_60", Material.GOLD_BLOCK, "&62x · 60m",
                "boost", "2:60", "boost_panel",
                List.of("&7Start 2x for 60 minutes", "", "&eStart")));
        gui.addButton(actionBtn(23, "progression.boost.n3_30", Material.CLOCK, "&e3x · 30m",
                "boost", "3:30", "boost_panel",
                List.of("&7Start 3x for 30 minutes", "", "&eStart")));
        gui.addButton(actionBtn(25, "progression.boost.end", Material.BARRIER, "&cEnd Boost",
                "boost", "end", "boost_panel",
                List.of("&7Stop the active global TP boost", "", "&eEnd boost")));
        gui.addButton(pageBtn(31, "progression.boost.refresh", Material.CLOCK, "&7Refresh Status", "boost_panel",
                "&7Reload this panel"));
        gui.addButton(pageBtn(36, "progression.boost.back", Material.ARROW, "&7Back", "tp", "&7TP Gains"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openAndroidPanel(Player player) {
        CMIGui gui = base(player, "&8Android Tools", 5);
        CMIGuiButton header = new CMIGuiButton(4, Material.IRON_INGOT,
                GuiTooltips.name("progression.android.panel_header", "&b&lAndroid Tools"));
        header.lockField();
        header.addLore(GuiTooltips.buttonLore("progression.android.panel_header", List.of(
                "&7Dr. Gero upgrade path",
                "&7Convert or remove the Android upgrade",
                "&8Race and stats stay on remove")));
        gui.addButton(header);
        gui.addButton(pageBtn(20, "progression.android.convert_entry", Material.NETHERITE_INGOT, "&aConvert to Android",
                "android_convert",
                "&7Gero upgrade — keeps your race",
                "&7Unlocks the Android form path",
                "&8Human · Saiyan · Frost Demon · Viltrumite",
                "", "&eClick · choose player"));
        gui.addButton(pageBtn(24, "progression.android.remove_entry", Material.REDSTONE, "&cRemove Android",
                "android_remove",
                "&7Restore normal form skills",
                "&8Confirm within 10s by clicking again",
                "", "&eClick · choose player"));
        gui.addButton(pageBtn(36, "progression.android.back", Material.ARROW, "&7Back", "race", "&7Race section"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openAndroidRemove(Player player) {
        boolean staff = ForgeBridge.isStaff(player);
        CMIGui gui = base(player, "&8Remove Android", 5);
        CMIGuiButton header = new CMIGuiButton(4, Material.REDSTONE,
                GuiTooltips.name("progression.android.remove_header", "&c&lRemove Android"));
        header.lockField();
        header.addLore(GuiTooltips.buttonLore("progression.android.remove_header",
                List.of(
                        "&7Removes the Android upgrade",
                        "&7Restores normal form skills",
                        "&8Click twice within 10s to confirm"),
                null,
                List.of(staff
                        ? "&8/progression android remove [player]"
                        : "&8/progression android remove")));
        gui.addButton(header);
        gui.addButton(actionBtn(staff ? 8 : 22, "progression.android.remove_self", Material.NETHERITE_SCRAP,
                "&cRemove Android Upgrade",
                "android_remove", player.getName(), "android_remove",
                List.of("&7Remove the Android upgrade",
                        "&7Race, stats, and progression stay",
                        "", "&eClick · confirm within 10s")));
        if (staff) {
            List<Player> online = GuiPlayerPicker.onlineExcept(player);
            int placed = 0;
            for (Player other : online) {
                if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                    break;
                }
                int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
                CMIGuiButton btn = new CMIGuiButton(slot,
                        GuiPlayerPicker.head(other, "&f" + other.getName(),
                                List.of("&7Remove Android upgrade", "", "&eClick · confirm within 10s")));
                btn.lockField();
                btn.addCommand("lmdo progression android_remove " + other.getName() + " android_remove");
                gui.addButton(btn);
            }
            if (online.isEmpty()) {
                CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&7No other players online");
                empty.lockField();
                empty.addLore(List.of("", "&7Use Remove Yourself above",
                        "&8or /progression android remove <name>"));
                gui.addButton(empty);
            }
            gui.addButton(pageBtn(36, "progression.android.remove_back", Material.ARROW, "&7Back",
                    "android_panel", "&7Android tools"));
        } else {
            CMIGuiButton hub = new CMIGuiButton(36, Material.ARROW,
                    GuiTooltips.name("progression.android.remove_back_hub", "&7Back"));
            hub.lockField();
            hub.addLore(GuiTooltips.buttonLore("progression.android.remove_back_hub",
                    List.of("&7Return to the main menu")));
            hub.addCommand("lmdo lm open hub");
            hub.setCloseInv(true);
            gui.addButton(hub);
        }
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openAndroidConvert(Player player) {
        CMIGui gui = base(player, "&8Android Convert", 5);
        CMIGuiButton header = new CMIGuiButton(4, Material.IRON_INGOT,
                GuiTooltips.name("progression.android.convert_header", "&b&lAndroid Convert"));
        header.lockField();
        header.addLore(GuiTooltips.buttonLore("progression.android.convert_header", List.of(
                "&7Gero upgrade — keeps your race",
                "&7Unlocks the Android form path",
                "&8Human · Saiyan · Frost Demon · Viltrumite",
                "&8/progression android [player]")));
        gui.addButton(header);
        gui.addButton(actionBtn(8, "progression.android.convert_self", Material.NETHERITE_INGOT,
                "&aConvert Yourself",
                "android", player.getName(), "android_convert",
                List.of("&7Apply conversion to you", "", "&eConvert")));
        List<Player> online = GuiPlayerPicker.onlineExcept(player);
        int placed = 0;
        for (Player other : online) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            CMIGuiButton btn = new CMIGuiButton(slot,
                    GuiPlayerPicker.head(other, "&f" + other.getName(),
                            List.of("&7Convert to Android", "", "&eConvert")));
            btn.lockField();
            btn.addCommand("lmdo progression android " + other.getName() + " android_convert");
            gui.addButton(btn);
        }
        if (online.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&7No other players online");
            empty.lockField();
            empty.addLore(List.of("", "&7Use Convert Yourself above",
                    "&8or /progression android <name>"));
            gui.addButton(empty);
        }
        gui.addButton(pageBtn(36, "progression.android.convert_back", Material.ARROW, "&7Back", "android_panel",
                "&7Android tools"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openFlags(Player player) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(player);
        CMIGui gui = base(player, "&8Flags", 6);
        CMIGuiButton info = new CMIGuiButton(4, Material.REPEATER, "&c&lStaff Flags");
        info.lockField();
        info.addLore(List.of("", "&7Grouped by script category", "&7Click a flag to toggle"));
        gui.addButton(info);

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
            CMIGuiButton label = new CMIGuiButton(slots[si++], SECTION, sectionTitle);
            label.lockField();
            label.addLore(List.of("", "&8Category"));
            gui.addButton(label);
            for (int g = 1; g < group.length && si < slots.length; g++) {
                String key = group[g];
                boolean on = "true".equalsIgnoreCase(ph.getOrDefault("flag_" + key, "false"));
                String flagTitle = (on ? "&a" : "&8") + key + (on ? " ON" : " OFF");
                Map<String, String> flagVars = Map.of(
                        "name", flagTitle,
                        "title", key,
                        "status", on ? "ON" : "OFF",
                        "desc", stripSection(sectionTitle));
                gui.addButton(actionBtn(slots[si++], "progression.admin.flag",
                        on ? Material.LIME_DYE : Material.GRAY_DYE,
                        flagTitle,
                        "flag", key, "admin",
                        List.of("&8" + stripSection(sectionTitle), "&7Click to toggle " + key), flagVars));
            }
        }

        gui.addButton(pageBtn(47, "progression.fabled.subflags", Material.ENCHANTED_BOOK, "&dFabled Subflags",
                "flags_fabled", "&7Energy, TP/SP, race class, etc."));
        gui.addButton(pageBtn(45, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(49));
        gui.addButton(closeBtn(53));
        fillEmpty(gui, 6);
        GuiFeedback.openCmi(gui);
    }

    private static void openFabledFlags(Player player) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(player);
        CMIGui gui = base(player, "&8Fabled Flags", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.ENCHANTED_BOOK, "&d&lFabled Subflags");
        info.lockField();
        info.addLore(List.of("", "&7Soft-dependency bridge toggles", "&7Click to toggle"));
        gui.addButton(info);

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
            gui.addButton(actionBtn(slots[i], "progression.fabled.flag",
                    on ? Material.LIME_DYE : Material.GRAY_DYE,
                    flagTitle,
                    "flag", key, "flags_fabled",
                    List.of("&8Fabled", "&7Click to toggle " + key), flagVars));
        }

        gui.addButton(pageBtn(36, "progression.fabled.back", Material.ARROW, "&7Back", "admin",
                "&7Return to Flags"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static String stripSection(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&e", "").replace("&6", "").replace("&b", "")
                .replace("&c", "").replace("&5", "").replace("&a", "")
                .replace("&d", "").replace("&7", "").replace("§", "");
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

    private static void fillEmpty(CMIGui gui, int rows) {
        int size = rows * 9;
        Map<Integer, CMIGuiButton> existing = gui.getButtons();
        for (int i = 0; i < size; i++) {
            if (existing != null && existing.containsKey(i)) {
                continue;
            }
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            CMIGuiButton pane = new CMIGuiButton(i, edge ? ACCENT : FILL, " ");
            pane.lockField();
            gui.addButton(pane);
        }
    }

    private static CMIGui base(Player player, String title, int rows) {
        CMIGui gui = new CMIGui(player);
        gui.setTitle(title);
        gui.setInvSize(rows);
        gui.addLock(InvType.Gui);
        return gui;
    }

    private static CMIGuiButton actionBtn(
            int slot, Material mat, String name, String action, String arg, String returnPage, List<String> tip) {
        return actionBtn(slot, null, mat, name, action, arg, returnPage, tip, null);
    }

    private static CMIGuiButton actionBtn(
            int slot, String key, Material mat, String name, String action, String arg, String returnPage,
            List<String> tip) {
        return actionBtn(slot, key, mat, name, action, arg, returnPage, tip, null);
    }

    private static CMIGuiButton actionBtn(
            int slot, String key, Material mat, String name, String action, String arg, String returnPage,
            List<String> tip, Map<String, String> vars) {
        String display = key == null || key.isBlank() ? name : GuiTooltips.name(key, name, vars);
        CMIGuiButton btn = new CMIGuiButton(slot, mat, display);
        btn.lockField();
        btn.addLore(key == null || key.isBlank()
                ? withBlank(tip)
                : GuiTooltips.buttonLore(key, tip, vars, null));
        btn.addCommand("lmdo progression " + action + " " + arg + " " + returnPage);
        return btn;
    }

    private static CMIGuiButton pageBtn(int slot, Material mat, String name, String page, String... tips) {
        return pageBtn(slot, null, mat, name, page, tips);
    }

    private static CMIGuiButton pageBtn(
            int slot, String key, Material mat, String name, String page, String... tips
    ) {
        List<String> defaults = new ArrayList<>();
        if (tips != null) {
            for (String tip : tips) {
                if (tip != null) {
                    defaults.add(tip);
                }
            }
        }
        String display = key == null || key.isBlank() ? name : GuiTooltips.name(key, name);
        CMIGuiButton btn = new CMIGuiButton(slot, mat, display);
        btn.lockField();
        btn.addLore(key == null || key.isBlank()
                ? withBlank(defaults)
                : GuiTooltips.buttonLore(key, defaults));
        btn.addCommand("lmdo progression page " + page);
        return btn;
    }

    private static List<String> withBlank(List<String> tip) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        if (tip != null) {
            lore.addAll(tip);
        }
        return lore;
    }

    private static CMIGuiButton hubBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.COMPASS, "&7« Hub");
        btn.lockField();
        btn.addLore(List.of("", "&7Return to the main menu"));
        btn.addCommand("lmdo lm open hub");
        btn.setCloseInv(true);
        return btn;
    }

    private static CMIGuiButton closeBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.BARRIER, "&cClose");
        btn.lockField();
        btn.setCloseInv(true);
        return btn;
    }
}
