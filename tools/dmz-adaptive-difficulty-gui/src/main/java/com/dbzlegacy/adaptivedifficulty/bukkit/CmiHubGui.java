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
                case "help" -> openLines(player, "help", "&7Help", Material.PAPER);
                case "logs", "syslog" -> {
                    if (ForgeBridge.isStaff(player)) {
                        openLines(player, "logs", "&8Logs", Material.WRITABLE_BOOK);
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
        CMIGui gui = base(player, "&8Legacy Mechanics", 5);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        CMIGuiButton status = new CMIGuiButton(4, Material.NETHER_STAR,
                !bridgeOk ? "&c&lUNAVAILABLE" : "&f&lLegacy Mechanics");
        status.lockField();
        if (!bridgeOk) {
            status.addLore(List.of("", "&cForge LegacyMechanics mod unreachable",
                    "&7Check mods/ for LegacyMechanics-*.jar"));
            gui.addButton(status);
            gui.addButton(closeBtn(44));
            fillEmpty(gui, 5);
            gui.open();
            return;
        }
        status.addLore(toAmp(ForgeBridge.hubLines(player, "main")));
        gui.addButton(status);

        gui.addButton(cmdBtn(19, Material.DIAMOND_SWORD, "&aDifficulty", "difficulty",
                "&7Unlock tiers & scaling"));
        gui.addButton(cmdBtn(20, Material.IRON_SWORD, "&6Rival", "rival",
                "&7Rivalry & challenges"));
        gui.addButton(cmdBtn(21, Material.GOLDEN_SWORD, "&bSpar", "spar",
                "&7Sparring TP & mentor"));
        gui.addButton(pageBtn(22, Material.PAPER, "&7Help", "help",
                "&7Command overview"));

        boolean staff = ForgeBridge.isStaff(player);
        boolean skillCheck = ForgeBridge.hasSkillCheck(player);
        if (staff) {
            gui.addButton(cmdBtn(28, Material.EXPERIENCE_BOTTLE, "&dProgression", "progression",
                    "&7Natural skills / TP / race"));
            gui.addButton(cmdBtn(29, Material.NETHER_STAR, "&ePrestige", "prestige",
                    "&7Prestige levels"));
            gui.addButton(cmdBtn(30, Material.ENCHANTED_BOOK, "&fSkills", "skills",
                    "&7Skill unlock admin"));
            gui.addButton(pageBtn(31, Material.WRITABLE_BOOK, "&8Logs", "logs",
                    "&7System telemetry status"));
        } else if (skillCheck) {
            gui.addButton(cmdBtn(23, Material.ENCHANTED_BOOK, "&eSkill Check", "skillcheck",
                    "&7View skill progress"));
        }

        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openLines(Player player, String page, String title, Material mat) {
        CMIGui gui = base(player, "&8Legacy Mechanics", 5);
        CMIGuiButton info = new CMIGuiButton(4, mat, title);
        info.lockField();
        List<String> lore = toAmp(ForgeBridge.hubLines(player, page));
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
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
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

    private static CMIGuiButton closeBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.BARRIER, "&cClose");
        btn.lockField();
        btn.setCloseInv(true);
        return btn;
    }
}
