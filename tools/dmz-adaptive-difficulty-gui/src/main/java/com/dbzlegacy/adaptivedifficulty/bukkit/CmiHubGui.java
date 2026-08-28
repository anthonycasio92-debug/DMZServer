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
                !bridgeOk ? "&c&lUNAVAILABLE" : "&f&lLegacy Mechanics");
        status.lockField();
        if (!bridgeOk) {
            status.addLore(List.of("", "&cForge LegacyMechanics mod unreachable",
                    "&7Check mods/ for LegacyMechanics-*.jar"));
            gui.addButton(status);
            gui.addButton(closeBtn(53));
            fillFrameOnly(gui, 6);
            gui.open();
            return;
        }
        List<String> hubHeaderLore = new ArrayList<>();
        hubHeaderLore.add("");
        hubHeaderLore.addAll(GuiBoardHelper.tips(player, "&7Choose a system", "&8/lm"));
        status.addLore(hubHeaderLore);
        gui.addButton(status);

        // Row 2 — core
        gui.addButton(openBtn(player, 20, Material.BEACON, "&aDifficulty", "difficulty",
                "&7Unlock tiers & world scaling", "&eClick to open"));
        gui.addButton(openBtn(player, 22, Material.NAME_TAG, "&6Rival", "rival",
                "&7Rivalry, challenges & RP", "&eClick to open"));
        gui.addButton(openBtn(player, 24, Material.GOLDEN_SWORD, "&bSpar", "spar",
                "&7Sparring TP & mentor bonds", "&eClick to open"));

        boolean staff = ForgeBridge.isStaff(player);
        boolean skillCheck = ForgeBridge.hasSkillCheck(player);

        // Row 3 — progress
        if (skillCheck && staff) {
            gui.addButton(openBtn(player, 21, Material.FEATHER, "&eSkill Check", "skillcheck",
                    "&7Natural · Saga progress", "&eClick to open"));
            gui.addButton(openBtn(player, 23, Material.GOLDEN_APPLE, "&6Prestige", "prestige",
                    "&7Prestige shop / levels", "&eClick to open"));
        } else if (skillCheck) {
            gui.addButton(openBtn(player, 22, Material.FEATHER, "&eSkill Check", "skillcheck",
                    "&7Natural · Saga progress", "&eClick to open"));
        } else if (staff) {
            gui.addButton(openBtn(player, 21, Material.FEATHER, "&eSkills", "skills",
                    "&7Skill unlock admin browser", "&eClick to open"));
            gui.addButton(openBtn(player, 23, Material.GOLDEN_APPLE, "&6Prestige", "prestige",
                    "&7Prestige shop / levels", "&eClick to open"));
        }

        // Row 4 — staff tools
        if (staff) {
            gui.addButton(openBtn(player, 38, Material.BREWING_STAND, "&dProgression", "progression",
                    "&7Skills · TP · Race · Combat flags", "&eClick to open"));
            gui.addButton(openBtn(player, 40, Material.COMMAND_BLOCK, "&cAdmin", "admin",
                    "&7Reload · syslog · open systems", "&8/lm admin"));
            gui.addButton(pageBtn(player, 42, Material.CLOCK, "&8Logs", "logs",
                    "&7System telemetry", "&eClick to open"));
        }

        gui.addButton(closeBtn(53));
        fillFrameOnly(gui, 6);
        gui.open();
    }

    private static void openLogs(Player player) {
        CMIGui gui = base(player, "&8Logs", 5);
        Map<String, String> ph = ForgeBridge.hubPlaceholders(player);
        boolean on = "true".equalsIgnoreCase(ph.getOrDefault("syslog", "false"));
        String statusLine = ph.getOrDefault("syslog_status", "unknown");

        CMIGuiButton header = new CMIGuiButton(4, Material.CLOCK, "&8&lLogs");
        header.lockField();
        List<String> logsHeader = new ArrayList<>();
        logsHeader.add("");
        logsHeader.add("&7System telemetry &f" + (on ? "ON" : "OFF"));
        logsHeader.add("&8" + statusLine.replace('§', '&'));
        logsHeader.add("");
        logsHeader.addAll(GuiBoardHelper.tips(player, "&7Use buttons below to toggle / flush"));
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

        gui.addButton(actionBtn(player, 29, Material.LIME_DYE, "&aSyslog ON",
                "syslog", "on", "logs", List.of("&7Enable system telemetry")));
        gui.addButton(actionBtn(player, 31, Material.GRAY_DYE, "&cSyslog OFF",
                "syslog", "off", "logs", List.of("&7Disable system telemetry")));
        gui.addButton(actionBtn(player, 33, Material.HOPPER, "&eFlush",
                "syslog", "flush", "logs", List.of("&7Flush log writers")));

        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(44));
        fillFrameOnly(gui, 5);
        gui.open();
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
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(GuiBoardHelper.tips(player, tips));
        btn.addLore(lore);
        btn.addCommand("lm do page " + page);
        return btn;
    }

    private static CMIGuiButton openBtn(Player player, int slot, Material mat, String name, String system, String... tips) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(GuiBoardHelper.tips(player, tips));
        btn.addLore(lore);
        btn.addCommand("lm do open " + system);
        btn.setCloseInv(true);
        return btn;
    }

    private static CMIGuiButton actionBtn(
            Player player, int slot, Material mat, String name, String action, String arg, String returnPage,
            List<String> tip) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(GuiBoardHelper.tipsList(player, tip));
        btn.addLore(lore);
        btn.addCommand("lm do " + action + " " + arg + " " + returnPage);
        return btn;
    }

    private static CMIGuiButton closeBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.BARRIER, "&cClose");
        btn.lockField();
        btn.setCloseInv(true);
        return btn;
    }
}
