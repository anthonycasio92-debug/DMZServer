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
 * Category hub: Skills · TP · Race · Combat · End · Shop · Fabled · Utility · Help (+ staff Flags).
 */
public final class CmiProgressionGui {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material SECTION = Material.LIGHT_GRAY_STAINED_GLASS_PANE;

    /** Staff flag groups on the main Flags page (fabled subs on flags_fabled). */
    private static final String[][] FLAG_GROUPS = {
            {"§eSkills", "flight", "sprint", "meditation", "potential"},
            {"§6TP Gains", "farming", "building", "boost", "bio"},
            {"§bRace", "racelock", "yardrat", "spiritualist", "android"},
            {"§cCombat", "kiweapons", "piercing", "dot", "apothic"},
            {"§5End", "end", "endportal"},
            {"§aShop", "prestige", "skills"},
            {"§7Utility", "shadow", "statchecker"},
            {"§dFabled", "fabled"}
    };

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
                case "skills" -> openCategory(player, "skills", "&eSkills", Material.FEATHER);
                case "tp" -> openCategory(player, "tp", "&6TP Gains", Material.GOLD_INGOT);
                case "race" -> openCategory(player, "race", "&bRace & Form", Material.PLAYER_HEAD);
                case "combat" -> openCategory(player, "combat", "&cCombat", Material.IRON_SWORD);
                case "end" -> openCategory(player, "end", "&5End", Material.END_STONE);
                case "shop" -> openShop(player);
                case "fabled" -> openCategory(player, "fabled", "&dFabled Bridges", Material.ENCHANTED_BOOK);
                case "utility" -> openCategory(player, "utility", "&7Utility", Material.COMPARATOR);
                case "status" -> openCategory(player, "status", "&eStatus", Material.BOOK);
                case "help" -> openCategory(player, "help", "&7Help", Material.PAPER);
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
        status.addLore(toAmp(ForgeBridge.progressionLines(player, "main")));
        gui.addButton(status);

        // Category row
        gui.addButton(pageBtn(10, Material.FEATHER, "&eSkills", "skills",
                "&7Flight, sprint, meditation, potential"));
        gui.addButton(pageBtn(11, Material.GOLD_INGOT, "&6TP Gains", "tp",
                "&7Farming, building, boost, bio"));
        gui.addButton(pageBtn(12, Material.PLAYER_HEAD, "&bRace & Form", "race",
                "&7Race lock, Yardrat, Spiritualist, Android"));
        gui.addButton(pageBtn(13, Material.IRON_SWORD, "&cCombat", "combat",
                "&7Ki weapons, piercing, DoT, Apothic"));
        gui.addButton(pageBtn(14, Material.END_STONE, "&5End", "end",
                "&7End strength & portal guard"));
        gui.addButton(pageBtn(15, Material.EMERALD, "&aShop", "shop",
                "&7Prestige & skill unlock service"));
        gui.addButton(pageBtn(16, Material.ENCHANTED_BOOK, "&dFabled", "fabled",
                "&7Soft Fabled bridges"));

        gui.addButton(pageBtn(20, Material.COMPARATOR, "&7Utility", "utility",
                "&7Shadow dummy & stat checker"));
        gui.addButton(pageBtn(22, Material.PAPER, "&7Help", "help",
                "&7Commands"));
        gui.addButton(cmdBtn(24, Material.NETHER_STAR, "&6Prestige", "prestige",
                "&7Open prestige menu"));
        gui.addButton(cmdBtn(25, Material.BOOK, "&fSkills GUI", "skills",
                "&7Open skill unlock progress"));

        if (ForgeBridge.isStaff(player)) {
            gui.addButton(pageBtn(31, Material.REDSTONE, "&cFlags", "admin",
                    "&7Toggle progression features"));
        }

        gui.addButton(hubBtn(49));
        gui.addButton(closeBtn(53));
        fillEmpty(gui, 6);
        gui.open();
    }

    private static void openCategory(Player player, String page, String title, Material mat) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Progression", 5);
        CMIGuiButton header = new CMIGuiButton(4, mat, title);
        header.lockField();
        header.addLore(List.of("", "&7Module flags & tips below",
                "&8Each paper holds part of this page"));
        gui.addButton(header);

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
            CMIGuiButton chunk = new CMIGuiButton(slot, Material.PAPER, partTitle);
            chunk.lockField();
            List<String> withBlank = new ArrayList<>();
            withBlank.add("");
            withBlank.addAll(part);
            chunk.addLore(withBlank);
            gui.addButton(chunk);
            placed++;
        }

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openShop(Player player) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Shop", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.EMERALD, "&a&lShop");
        info.lockField();
        List<String> lore = toAmp(ForgeBridge.progressionLines(player, "shop"));
        List<String> withBlank = new ArrayList<>();
        withBlank.add("");
        withBlank.addAll(lore.isEmpty() ? List.of("&7Nothing here yet.") : lore);
        info.addLore(withBlank);
        gui.addButton(info);

        gui.addButton(cmdBtn(20, Material.NETHER_STAR, "&6Open Prestige", "prestige",
                "&7Prestige level purchase"));
        gui.addButton(cmdBtn(24, Material.BOOK, "&fOpen Skills", "skills",
                "&7Skill unlock progress"));

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

    private static CMIGuiButton cmdBtn(int slot, Material mat, String name, String command, String... tips) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String tip : tips) {
            lore.add(tip);
        }
        btn.addLore(lore);
        btn.addCommand(command);
        btn.setCloseInv(true);
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
