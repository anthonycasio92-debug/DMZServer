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
            Map.entry("sprint", new String[]{"Sprint Jump", "Jump/Sprint levels from invested Strength.", "/progression"}),
            Map.entry("meditation", new String[]{"Meditation", "Restore energy in the global trial biome. /progression meditation", "/progression meditation · next"}),
            Map.entry("potential", new String[]{"Potential", "Earn points from PvP hits/blocks (cap 10 natural).", "/skillcheck"}),
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
                case "skills" -> openSection(player, "skills", "&eSkills", Material.FEATHER,
                        new String[]{"flight", "sprint", "meditation", "potential"});
                case "tp" -> openSection(player, "tp", "&6TP Gains", Material.GOLD_INGOT,
                        new String[]{"farming", "building", "boost", "bio"});
                case "race" -> openSection(player, "race", "&bRace & Form", Material.PLAYER_HEAD,
                        new String[]{"racelock", "yardrat", "spiritualist", "android"});
                case "combat" -> openSection(player, "combat", "&cCombat", Material.IRON_SWORD,
                        new String[]{"kiweapons", "piercing", "dot", "apothic"});
                case "end" -> openSection(player, "end", "&5End", Material.END_STONE,
                        new String[]{"end", "endportal"});
                case "fabled" -> openSection(player, "fabled", "&dFabled Bridges", Material.ENCHANTED_BOOK,
                        new String[]{"fabled"});
                case "utility" -> openSection(player, "utility", "&7Utility", Material.COMPARATOR,
                        new String[]{"shadow", "statchecker"});
                case "status" -> openSection(player, "status", "&eStatus", Material.BOOK,
                        new String[]{"flight", "sprint", "meditation", "potential", "farming", "building"});
                case "help" -> openHelp(player);
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
        CMIGui gui = base(player, "&8Legacy Mechanics · Progression", 6);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        CMIGuiButton status = new CMIGuiButton(4, Material.EXPERIENCE_BOTTLE,
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
            gui.open();
            return;
        }
        status.addLore(List.of("", "&7Natural systems · click a section",
                "&7Flags toggle on each section page",
                "&8Prestige / Skills live on Hub"));
        gui.addButton(status);

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
            gui.addButton(pageBtn(slots[i], mats[i], titles[i], pages[i],
                    tips[i], "", "&eClick · toggle flags inside"));
        }

        gui.addButton(pageBtn(40, Material.PAPER, "&7Help", "help", "&7Commands"));

        if (ForgeBridge.isStaff(player)) {
            gui.addButton(pageBtn(31, Material.REDSTONE, "&cAll Flags", "admin",
                    "&7Full flag board"));
        }

        gui.addButton(hubBtn(49));
        gui.addButton(closeBtn(53));
        fillEmpty(gui, 6);
        gui.open();
    }

    private static void openSection(Player player, String page, String title, Material mat, String[] keys) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(player);
        CMIGui gui = base(player, "&8Legacy Mechanics · Progression", 5);
        boolean staff = ForgeBridge.isStaff(player);
        CMIGuiButton header = new CMIGuiButton(4, mat, title);
        header.lockField();
        header.addLore(List.of("",
                staff ? "&7Click a module to toggle ON/OFF" : "&7Module status (staff can toggle)",
                "&8Description + commands on each item"));
        gui.addButton(header);

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
            if (staff) {
                gui.addButton(actionBtn(slots[i],
                        on ? Material.LIME_DYE : Material.GRAY_DYE,
                        (on ? "&a" : "&8") + info[0] + (on ? " ON" : " OFF"),
                        "flag", key, page, lore));
            } else {
                CMIGuiButton btn = new CMIGuiButton(slots[i],
                        on ? Material.LIME_DYE : Material.GRAY_DYE,
                        (on ? "&a" : "&8") + info[0] + (on ? " ON" : " OFF"));
                btn.lockField();
                btn.addLore(lore);
                gui.addButton(btn);
            }
        }

        if ("fabled".equals(page) && staff) {
            gui.addButton(pageBtn(31, Material.ENCHANTED_BOOK, "&dFabled Subflags", "flags_fabled",
                    "&7Energy, TP/SP, race class, etc."));
        }

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openHelp(Player player) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Progression", 5);
        CMIGuiButton header = new CMIGuiButton(4, Material.PAPER, "&7Help");
        header.lockField();
        header.addLore(List.of("", "&7Module flags & tips below"));
        gui.addButton(header);
        List<String> lore = toAmp(ForgeBridge.progressionLines(player, "help"));
        if (lore.isEmpty()) {
            lore = List.of("&7/progression §8— open this menu",
                    "&7Sections hold toggleable flags",
                    "&8Prestige: /prestige · Skills: /skills · Hub: /lm");
        }
        List<GuiBoardHelper.DetailTile> tiles = GuiBoardHelper.detailTiles(lore);
        int[] slots = GuiBoardHelper.centeredSlots(Math.min(tiles.size(), 21));
        for (int i = 0; i < slots.length && i < tiles.size(); i++) {
            GuiBoardHelper.DetailTile tile = tiles.get(i);
            CMIGuiButton btn = new CMIGuiButton(slots[i], tile.icon, tile.title);
            btn.lockField();
            List<String> tip = new ArrayList<>();
            tip.add("");
            tip.addAll(tile.lore);
            btn.addLore(tip);
            gui.addButton(btn);
        }
        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openFlags(Player player) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(player);
        CMIGui gui = base(player, "&8Legacy Mechanics · Flags", 6);
        CMIGuiButton info = new CMIGuiButton(4, Material.REDSTONE, "&c&lStaff Flags");
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
                gui.addButton(actionBtn(slots[si++],
                        on ? Material.LIME_DYE : Material.GRAY_DYE,
                        (on ? "&a" : "&8") + key + (on ? " ON" : " OFF"),
                        "flag", key, "admin",
                        List.of("&8" + stripSection(sectionTitle), "&7Click to toggle")));
            }
        }

        gui.addButton(pageBtn(47, Material.ENCHANTED_BOOK, "&dFabled Subflags", "flags_fabled",
                "&7Energy, TP/SP, race class, etc."));
        gui.addButton(pageBtn(45, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(49));
        gui.addButton(closeBtn(53));
        fillEmpty(gui, 6);
        gui.open();
    }

    private static void openFabledFlags(Player player) {
        Map<String, String> ph = ForgeBridge.progressionPlaceholders(player);
        CMIGui gui = base(player, "&8Legacy Mechanics · Fabled Flags", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.ENCHANTED_BOOK, "&d&lFabled Subflags");
        info.lockField();
        info.addLore(List.of("", "&7Soft-dependency bridge toggles", "&7Click to toggle"));
        gui.addButton(info);

        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30};
        for (int i = 0; i < FABLED_FLAG_KEYS.length && i < slots.length; i++) {
            String key = FABLED_FLAG_KEYS[i];
            boolean on = "true".equalsIgnoreCase(ph.getOrDefault("flag_" + key, "false"));
            gui.addButton(actionBtn(slots[i],
                    on ? Material.LIME_DYE : Material.GRAY_DYE,
                    (on ? "&a" : "&8") + key + (on ? " ON" : " OFF"),
                    "flag", key, "flags_fabled",
                    List.of("&8Fabled", "&7Click to toggle")));
        }

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "admin", "&7Return to Flags"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
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
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(tip);
        btn.addLore(lore);
        btn.addCommand("progression do " + action + " " + arg + " " + returnPage);
        return btn;
    }

    private static CMIGuiButton pageBtn(int slot, Material mat, String name, String page, String... tips) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String tip : tips) {
            lore.add(tip);
        }
        btn.addLore(lore);
        btn.addCommand("progression do page " + page);
        return btn;
    }

    private static CMIGuiButton hubBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.COMPASS, "&7« Hub");
        btn.lockField();
        btn.addLore(List.of("", "&7Legacy Mechanics hub"));
        btn.addCommand("lm");
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
