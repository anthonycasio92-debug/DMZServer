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
 * CMILib inventory GUI — Legacy Mechanics Hub ({@code /lm}).
 * Pages: main · logs (staff). Help page removed.
 */
public final class CmiHubGui {
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private CmiHubGui() {}

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
                case "logs", "syslog" -> {
                    if (ForgeBridge.isStaff(player)) {
                        openLogs(player);
                    } else {
                        openMain(player);
                    }
                }
                default -> openMain(player);
            }
            return true;
        } catch (Throwable t) {
            player.sendMessage("§cHub CMI GUI failed: " + t.getMessage());
            return false;
        }
    }

    private static void openMain(Player player) {
        Map<String, String> ph = ForgeBridge.hubPlaceholders(player);
        CMIGui gui = base(player, "&8Legacy Mechanics", 6);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        CMIGuiButton status = new CMIGuiButton(4, Material.NETHER_STAR,
                !bridgeOk ? "&c&lUNAVAILABLE" : GuiTooltips.name("hub.main.header", "&f&lLegacy Mechanics"));
        status.lockField();
        if (!bridgeOk) {
            status.addLore(List.of("", "&cForge LegacyMechanics mod unreachable",
                    "&7Check mods/ for LegacyMechanics-*.jar"));
            gui.addButton(status);
            gui.addButton(closeBtn(53));
            fillFrameOnly(gui, 6);
            GuiFeedback.openCmi(gui);
            return;
        }
        List<String> hubHeaderLore = new ArrayList<>();
        hubHeaderLore.add("");
        hubHeaderLore.addAll(GuiTooltips.lore("hub.main.header", List.of("&7Choose a system", "&8/lm")));
        status.addLore(hubHeaderLore);
        gui.addButton(status);

        // Row 2 — core
        gui.addButton(openBtn(player, 20, "hub.main.difficulty", Material.BEACON, "&aDifficulty", "difficulty",
                "&7Unlock tiers & world scaling",
                "&cWarning: &7Scaled mobs can attack other players as well",
                "&eClick to open"));
        gui.addButton(openBtn(player, 22, "hub.main.rival", Material.NAME_TAG, "&6Rival", "rival",
                "&7Rivalry, challenges & RP", "&eClick to open"));
        gui.addButton(openBtn(player, 24, "hub.main.spar", Material.GOLDEN_SWORD, "&bSpar", "spar",
                "&7Sparring TP & mentor bonds", "&eClick to open"));

        boolean staff = ForgeBridge.isStaff(player);
        boolean skillCheck = ForgeBridge.hasSkillCheck(player);

        // Row 3 — Skill Check (donator) / Skills (staff) · Prestige (everyone)
        if (skillCheck) {
            gui.addButton(openBtn(player, 21, "hub.main.skillcheck", Material.EXPERIENCE_BOTTLE, "&eSkill Check", "skillcheck",
                    "&7Natural · Saga progress", "&eClick to open"));
        } else if (staff) {
            gui.addButton(openBtn(player, 21, "hub.main.skills", Material.BOOK, "&eSkills", "skills",
                    "&7Skill unlock admin browser", "&8No Skill Check perm", "&eClick to open"));
        }
        gui.addButton(openBtn(player, 23, "hub.main.prestige", Material.GOLDEN_APPLE, "&6Prestige", "prestige",
                "&7Turn in prestiges · skill/forms shop · level-cap",
                "&eClick to open"));

        // Row 4 — Character Services · Android remove + staff tools
        gui.addButton(openBtn(player, 30, "hub.main.character", Material.PLAYER_HEAD, "&dCharacter Services", "character",
                "&7Change race, class, or looks",
                "&7without wiping your whole build",
                "&8Paid with Ancient Coins",
                "&eClick to open"));
        gui.addButton(openBtn(player, 31, "hub.main.android_remove", Material.REDSTONE, "&cRemove Android", "android_remove",
                "&7Remove your Android upgrade",
                "&8Two-click confirm · forms restored",
                "&eClick to open"));
        if (staff) {
            gui.addButton(openBtn(player, 38, "hub.main.progression", Material.BREWING_STAND, "&dProgression", "progression",
                    "&7Skills · TP · Race · Combat flags", "&eClick to open"));
            gui.addButton(openBtn(player, 40, "hub.main.admin", Material.COMMAND_BLOCK, "&cAdmin", "admin",
                    "&7Reload configs and open staff tools", "&8/lm admin"));
            gui.addButton(pageBtn(player, 42, "hub.main.logs", Material.CLOCK, "&8Logs", "logs",
                    "&7Server event logs", "&eClick to open"));
        }

        gui.addButton(closeBtn(53));
        fillFrameOnly(gui, 6);
        GuiFeedback.openCmi(gui);
    }

    private static void openLogs(Player player) {
        CMIGui gui = base(player, "&8Logs", 5);
        Map<String, String> ph = ForgeBridge.hubPlaceholders(player);
        boolean on = "true".equalsIgnoreCase(ph.getOrDefault("syslog", "false"));
        String statusLine = ph.getOrDefault("syslog_status", "unknown");

        CMIGuiButton header = new CMIGuiButton(4, Material.CLOCK,
                GuiTooltips.name("hub.logs.header", "&8&lLogs"));
        header.lockField();
        List<String> logsHeader = new ArrayList<>();
        logsHeader.add("");
        logsHeader.add("&7Server event logs &f" + (on ? "ON" : "OFF"));
        logsHeader.add("&8" + statusLine.replace('§', '&'));
        logsHeader.add("");
        logsHeader.addAll(GuiTooltips.lore("hub.logs.header",
                GuiBoardHelper.tips(player, "&7Use buttons below to toggle / flush")));
        header.addLore(logsHeader);
        gui.addButton(header);

        List<String> lore = toAmp(ForgeBridge.hubLines(player, "logs"));
        List<List<String>> parts = GuiLoreChunks.chunk(lore);
        int placed = 0;
        for (List<String> part : parts) {
            if (placed >= 3) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed];
            CMIGuiButton chunk = new CMIGuiButton(slot, Material.PAPER,
                    parts.size() == 1 ? "&fStatus" : "&fStatus &8(" + (placed + 1) + ")");
            chunk.lockField();
            List<String> withBlank = new ArrayList<>();
            withBlank.add("");
            withBlank.addAll(part);
            chunk.addLore(withBlank);
            gui.addButton(chunk);
            placed++;
        }

        gui.addButton(actionBtn(player, 29, "hub.logs.syslog_on", Material.LIME_DYE, "&aEvent Log ON",
                "syslog", "on", "logs", List.of("&7Server event logging is on", "&8Staff only")));
        gui.addButton(actionBtn(player, 31, "hub.logs.syslog_off", Material.GRAY_DYE, "&cEvent Log OFF",
                "syslog", "off", "logs", List.of("&7Turn server event logging back on", "&8Staff only")));
        gui.addButton(actionBtn(player, 33, "hub.logs.flush", Material.HOPPER, "&eFlush Logs",
                "syslog", "flush", "logs", List.of("&7Write buffered logs to disk", "&8Staff only")));

        gui.addButton(pageBtn(player, 36, "hub.logs.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(44));
        fillFrameOnly(gui, 5);
        GuiFeedback.openCmi(gui);
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

    private static void fillFrameOnly(CMIGui gui, int rows) {
        int size = rows * 9;
        Map<Integer, CMIGuiButton> existing = gui.getButtons();
        for (int i = 0; i < size; i++) {
            if (existing != null && existing.containsKey(i)) {
                continue;
            }
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            if (!edge) {
                continue;
            }
            CMIGuiButton pane = new CMIGuiButton(i, ACCENT, " ");
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

    private static CMIGuiButton pageBtn(Player player, int slot, Material mat, String name, String page, String... tips) {
        return pageBtn(player, slot, null, mat, name, page, tips);
    }

    private static CMIGuiButton pageBtn(
            Player player, int slot, String key, Material mat, String name, String page, String... tips
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
        btn.addCommand("lmdo lm page " + page);
        return btn;
    }

    private static CMIGuiButton openBtn(Player player, int slot, Material mat, String name, String system, String... tips) {
        return openBtn(player, slot, null, mat, name, system, tips);
    }

    private static CMIGuiButton openBtn(
            Player player, int slot, String key, Material mat, String name, String system, String... tips
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
        btn.addCommand("lmdo lm open " + system);
        btn.setCloseInv(true);
        return btn;
    }

    private static CMIGuiButton actionBtn(
            Player player, int slot, Material mat, String name, String action, String arg, String returnPage,
            List<String> tip) {
        return actionBtn(player, slot, null, mat, name, action, arg, returnPage, tip);
    }

    private static CMIGuiButton actionBtn(
            Player player, int slot, String key, Material mat, String name, String action, String arg,
            String returnPage, List<String> tip
    ) {
        String display = key == null || key.isBlank() ? name : GuiTooltips.name(key, name);
        CMIGuiButton btn = new CMIGuiButton(slot, mat, display);
        btn.lockField();
        btn.addLore(key == null || key.isBlank()
                ? withBlank(tip)
                : GuiTooltips.buttonLore(key, tip));
        btn.addCommand("lmdo lm " + action + " " + arg + " " + returnPage);
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

    private static CMIGuiButton closeBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.BARRIER, "&cClose");
        btn.lockField();
        btn.setCloseInv(true);
        return btn;
    }
}
