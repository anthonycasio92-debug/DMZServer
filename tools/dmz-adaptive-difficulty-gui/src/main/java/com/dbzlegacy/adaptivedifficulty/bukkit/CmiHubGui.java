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
 * Pages: main · help · logs (staff).
 * Hub buttons use {@code lm do open &lt;system&gt;} so inventories open without
 * relying on bare {@code /difficulty}/{@code /rival}/{@code /spar} commands.
 */
public final class CmiHubGui {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
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
                case "help" -> openChunked(player, "help", "&7Help", Material.PAPER);
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
        status.addLore(List.of("", "&7Pick a system below", "&8Use &f/lm &8for this hub"));
        gui.addButton(status);

        // Everyone — spaced core systems
        gui.addButton(openBtn(19, Material.DIAMOND_SWORD, "&aDifficulty", "difficulty",
                "&7Unlock tiers & world scaling",
                "&8Tip: buy unlocks when ready"));
        gui.addButton(openBtn(21, Material.IRON_SWORD, "&6Rival", "rival",
                "&7Rivalry, challenges & RP",
                "&8Tip: declare rivals from List"));
        gui.addButton(openBtn(23, Material.GOLDEN_SWORD, "&bSpar", "spar",
                "&7Sparring TP & mentor bonds",
                "&8Tip: trade hits nearby to start"));
        gui.addButton(pageBtn(25, Material.PAPER, "&7Help", "help",
                "&7How to use /lm", "&8Guide-friendly overview"));

        boolean staff = ForgeBridge.isStaff(player);
        boolean skillCheck = ForgeBridge.hasSkillCheck(player);

        // Prestige shop + Skill Check on main for staff/donators
        if (staff || skillCheck) {
            if (skillCheck || staff) {
                gui.addButton(openBtn(29, Material.ENCHANTED_BOOK,
                        staff && !skillCheck ? "&eSkills" : "&eSkill Check",
                        staff && !skillCheck ? "skills" : "skillcheck",
                        staff && !skillCheck
                                ? "&7Skill unlock admin browser"
                                : "&7View skill progress (donator)",
                        "&8Core · Advanced · Saga"));
            }
            if (staff) {
                gui.addButton(openBtn(31, Material.NETHER_STAR, "&6Prestige", "prestige",
                        "&7Prestige shop / levels",
                        "&8Staff prestige menu"));
            }
        }

        if (staff) {
            gui.addButton(openBtn(37, Material.EXPERIENCE_BOTTLE, "&dProgression", "progression",
                    "&7Natural skills / TP / race", "&8Staff category hub"));
            gui.addButton(openBtn(39, Material.REDSTONE, "&cAdmin", "admin",
                    "&7/lm admin · system toggles", "&8Reload · syslog · open"));
            gui.addButton(pageBtn(41, Material.WRITABLE_BOOK, "&8Logs", "logs",
                    "&7System telemetry", "&8On · off · flush"));
        }

        gui.addButton(closeBtn(53));
        fillFrameOnly(gui, 6);
        gui.open();
    }

    private static void openLogs(Player player) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Logs", 5);
        Map<String, String> ph = ForgeBridge.hubPlaceholders(player);
        boolean on = "true".equalsIgnoreCase(ph.getOrDefault("syslog", "false"));
        String statusLine = ph.getOrDefault("syslog_status", "unknown");

        CMIGuiButton header = new CMIGuiButton(4, Material.WRITABLE_BOOK, "&8&lLogs");
        header.lockField();
        header.addLore(List.of("",
                "&7System telemetry &f" + (on ? "ON" : "OFF"),
                "&8" + statusLine.replace('§', '&'),
                "",
                "&7Use buttons below to toggle / flush"));
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

        gui.addButton(actionBtn(29, Material.LIME_CONCRETE, "&aSyslog ON",
                "syslog", "on", "logs", List.of("&7Enable system telemetry")));
        gui.addButton(actionBtn(31, Material.RED_CONCRETE, "&cSyslog OFF",
                "syslog", "off", "logs", List.of("&7Disable system telemetry")));
        gui.addButton(actionBtn(33, Material.GOLD_INGOT, "&eFlush",
                "syslog", "flush", "logs", List.of("&7Flush log writers")));

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(44));
        fillFrameOnly(gui, 5);
        gui.open();
    }

    private static void openChunked(Player player, String page, String title, Material mat) {
        CMIGui gui = base(player, "&8Legacy Mechanics", 5);
        List<String> lore = toAmp(ForgeBridge.hubLines(player, page));
        if (lore.isEmpty()) {
            lore = List.of("&7Nothing here yet.", "&8Use &f/lm &8to open systems.");
        }
        CMIGuiButton header = new CMIGuiButton(4, mat, title);
        header.lockField();
        header.addLore(List.of("", "&7Guide tips below", "&8Players: use &f/lm &8only"));
        gui.addButton(header);

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

    /** Edge frame only — leave interior empty for breathing room. */
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

    private static CMIGuiButton pageBtn(int slot, Material mat, String name, String page, String... tips) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String tip : tips) {
            lore.add(tip);
        }
        btn.addLore(lore);
        btn.addCommand("lm do page " + page);
        return btn;
    }

    private static CMIGuiButton openBtn(int slot, Material mat, String name, String system, String... tips) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String tip : tips) {
            lore.add(tip);
        }
        btn.addLore(lore);
        btn.addCommand("lm do open " + system);
        btn.setCloseInv(true);
        return btn;
    }

    private static CMIGuiButton actionBtn(
            int slot, Material mat, String name, String action, String arg, String returnPage, List<String> tip) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(tip);
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
