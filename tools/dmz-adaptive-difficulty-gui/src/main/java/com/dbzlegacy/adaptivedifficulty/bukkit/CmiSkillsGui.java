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
 * CMILib inventory GUI — Legacy Mechanics Skills / Skill Check.
 * Pages: core · advanced · saga.
 */
public final class CmiSkillsGui {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private CmiSkillsGui() {}

    public static boolean available() {
        return CmiDifficultyGui.available();
    }

    public static boolean open(Player player, String page) {
        if (player == null || !available()) {
            return false;
        }
        String p = page == null || page.isBlank() ? "core" : page.toLowerCase(Locale.ROOT);
        try {
            switch (p) {
                case "advanced", "dmz" -> openPage(player, "advanced", "&bAdvanced", Material.DIAMOND);
                case "saga" -> openPage(player, "saga", "&dSaga", Material.AMETHYST_SHARD);
                case "help" -> openPage(player, "help", "&7Help", Material.PAPER);
                default -> openPage(player, "core", "&eCore", Material.ENCHANTED_BOOK);
            }
            return true;
        } catch (Throwable t) {
            player.sendMessage("§cSkills CMI GUI failed: " + t.getMessage());
            return false;
        }
    }

    private static void openPage(Player player, String page, String title, Material mat) {
        Map<String, String> ph = ForgeBridge.skillsPlaceholders(player);
        boolean skillCheckUi = ForgeBridge.inSkillCheckSession(player);
        String windowTitle = skillCheckUi
                ? "&8Legacy Mechanics · Skill Check"
                : "&8Legacy Mechanics · Skills";
        CMIGui gui = base(player, windowTitle, 5);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        String statusName = !bridgeOk ? "&c&lUNAVAILABLE"
                : !systemOn ? "&c&lSKILLS DISABLED"
                : skillCheckUi ? title + " Skill Check" : title + " Skills";
        CMIGuiButton status = new CMIGuiButton(4, mat, statusName);
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
        List<String> lore = toAmp(ForgeBridge.skillsLines(player, page));
        if (lore.isEmpty()) {
            lore = List.of("", "&7Nothing here yet.");
        } else {
            List<String> withBlank = new ArrayList<>();
            withBlank.add("");
            withBlank.addAll(lore);
            lore = withBlank;
        }
        status.addLore(lore);
        gui.addButton(status);

        String pageCmd = skillCheckUi ? "skillcheck" : "skills";
        gui.addButton(pageBtn(19, Material.ENCHANTED_BOOK, "&eCore", pageCmd, "core",
                "&7Core skill unlocks"));
        gui.addButton(pageBtn(21, Material.DIAMOND, "&bAdvanced", pageCmd, "advanced",
                "&7DMZ 2.1 skills"));
        gui.addButton(pageBtn(23, Material.AMETHYST_SHARD, "&dSaga", pageCmd, "saga",
                "&7Saga unlocks"));

        gui.addButton(hubBtn(36));
        if (ForgeBridge.isStaff(player) && !ForgeBridge.inSkillCheckSession(player)) {
            gui.addButton(progBtn(40));
        }
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static List<String> unavailableLore(boolean bridgeOk) {
        if (!bridgeOk) {
            return List.of("", "&cForge LegacyMechanics mod unreachable");
        }
        return List.of("", "&cSkill unlock service is disabled");
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

    private static CMIGuiButton pageBtn(
            int slot, Material mat, String name, String cmdRoot, String page, String... tips) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String tip : tips) {
            lore.add(tip);
        }
        btn.addLore(lore);
        btn.addCommand(cmdRoot + " do page " + page);
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

    private static CMIGuiButton progBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.EXPERIENCE_BOTTLE, "&dProgression");
        btn.lockField();
        btn.addLore(List.of("", "&7Back to progression"));
        btn.addCommand("progression");
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
