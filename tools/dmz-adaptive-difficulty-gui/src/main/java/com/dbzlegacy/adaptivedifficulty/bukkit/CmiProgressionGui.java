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
 * Pages: main · status · help · admin/flags (staff).
 */
public final class CmiProgressionGui {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private static final String[] FLAG_KEYS = {
            "flight", "sprint", "meditation", "potential", "farming", "building",
            "boost", "bio", "racelock", "yardrat", "spiritualist", "android",
            "endportal", "skills", "prestige", "fabled"
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
                case "status" -> openLines(player, "status", "&eStatus", Material.BOOK);
                case "help" -> openLines(player, "help", "&7Help", Material.PAPER);
                case "fabled" -> openLines(player, "fabled", "&dFabled", Material.ENCHANTED_BOOK);
                case "admin", "flags", "disable" -> {
                    if (ForgeBridge.isStaff(player)) {
                        openFlags(player);
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
        CMIGui gui = base(player, "&8Legacy Mechanics · Progression", 5);

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
            gui.addButton(hubBtn(40));
            gui.addButton(closeBtn(44));
            fillEmpty(gui, 5);
            gui.open();
            return;
        }
        status.addLore(toAmp(ForgeBridge.progressionLines(player, "main")));
        gui.addButton(status);

        gui.addButton(pageBtn(19, Material.BOOK, "&eStatus", "status",
                "&7Feature flags summary"));
        gui.addButton(cmdBtn(21, Material.NETHER_STAR, "&6Prestige", "prestige",
                "&7Open prestige menu"));
        gui.addButton(cmdBtn(23, Material.ENCHANTED_BOOK, "&fSkills", "skills",
                "&7Open skills menu"));
        gui.addButton(pageBtn(25, Material.PAPER, "&7Help", "help",
                "&7Commands"));

        if (ForgeBridge.isStaff(player)) {
            gui.addButton(pageBtn(31, Material.REDSTONE, "&cFlags", "admin",
                    "&7Toggle progression features"));
        }

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
        info.addLore(List.of("", "&7Click a flag to toggle"));
        gui.addButton(info);

        int[] slots = {
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38
        };
        for (int i = 0; i < FLAG_KEYS.length && i < slots.length; i++) {
            String key = FLAG_KEYS[i];
            boolean on = "true".equalsIgnoreCase(ph.getOrDefault("flag_" + key, "false"));
            gui.addButton(actionBtn(slots[i],
                    on ? Material.LIME_DYE : Material.GRAY_DYE,
                    (on ? "&a" : "&8") + key + (on ? " ON" : " OFF"),
                    "flag", key, "admin",
                    List.of("&7Click to toggle " + key)));
        }

        gui.addButton(pageBtn(45, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(49));
        gui.addButton(closeBtn(53));
        fillEmpty(gui, 6);
        gui.open();
    }

    private static void openLines(Player player, String page, String title, Material mat) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Progression", 5);
        CMIGuiButton info = new CMIGuiButton(4, mat, title);
        info.lockField();
        List<String> lore = toAmp(ForgeBridge.progressionLines(player, page));
        if (lore.isEmpty()) {
            lore = List.of("", "&7Nothing here yet.");
        } else {
            List<String> withBlank = new ArrayList<>();
            withBlank.add("");
            withBlank.addAll(lore);
            lore = withBlank;
        }
        info.addLore(lore);
        gui.addButton(info);
        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
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
