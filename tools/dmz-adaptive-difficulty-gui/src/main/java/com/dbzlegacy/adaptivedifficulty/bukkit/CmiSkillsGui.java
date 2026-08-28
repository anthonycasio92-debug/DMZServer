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
 * Pages: core · advanced · saga. One item per skill in content slots.
 */
public final class CmiSkillsGui {
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
                case "natural" -> openPage(player, "core", "&aNatural", Material.ENCHANTED_BOOK);
                default -> openPage(player, "core", "&aNatural", Material.ENCHANTED_BOOK);
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
        boolean staffAdmin = ForgeBridge.isStaff(player) && !skillCheckUi;
        String windowTitle = skillCheckUi
                ? "&8Legacy Mechanics · Skill Check"
                : staffAdmin ? "&8Legacy Mechanics · Skills (Admin)" : "&8Legacy Mechanics · Skills";
        CMIGui gui = base(player, windowTitle, 6);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        String statusName = !bridgeOk ? "&c&lUNAVAILABLE"
                : !systemOn ? "&c&lSKILLS DISABLED"
                : skillCheckUi ? title + " Skill Check"
                : staffAdmin ? title + " (Admin)" : title + " Skills";
        CMIGuiButton status = new CMIGuiButton(4, mat, statusName);
        status.lockField();
        if (!bridgeOk || !systemOn) {
            status.addLore(unavailableLore(bridgeOk));
            gui.addButton(status);
            gui.addButton(hubBtn(49));
            gui.addButton(closeBtn(53));
            fillFrameOnly(gui, 6);
            gui.open();
            return;
        }

        List<String> raw = toAmp(ForgeBridge.skillsLines(player, page));
        if ("help".equals(page)) {
            status.addLore(prependBlank(raw.isEmpty()
                    ? List.of("&7Use Natural · Saga · Advanced tabs.")
                    : raw));
            gui.addButton(status);
        } else {
            GuiLoreChunks.SkillPage split = GuiLoreChunks.splitSkillsPage(raw);
            List<String> headerLore = new ArrayList<>();
            headerLore.add("");
            headerLore.addAll(split.header.isEmpty()
                    ? List.of("&7DMZ stats unavailable")
                    : split.header);
            headerLore.add("");
            headerLore.add(skillCheckUi ? "&eSkill Check · one item per skill"
                    : "&8One item per skill below");
            status.addLore(headerLore);
            gui.addButton(status);

            int placed = 0;
            for (List<String> skill : split.skills) {
                if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                    break;
                }
                int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
                String name = GuiLoreChunks.skillDisplayName(skill);
                Material icon = GuiLoreChunks.skillIcon(name);
                CMIGuiButton btn = new CMIGuiButton(slot, icon, name);
                btn.lockField();
                List<String> lore = new ArrayList<>();
                lore.add("");
                for (String line : skill) {
                    lore.add(line);
                }
                boolean unlocked = skillUnlocked(skill);
                lore.add("");
                lore.add(unlocked ? "&aUnlocked" : "&cLocked / in progress");
                btn.addLore(lore);
                gui.addButton(btn);
            }
            if (placed == 0) {
                CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&cNo skills listed");
                empty.lockField();
                empty.addLore(List.of("", "&7Bridge returned no skill rows"));
                gui.addButton(empty);
            }
        }

        String pageCmd = skillCheckUi ? "skillcheck" : "skills";
        gui.addButton(pageBtn(45, Material.ENCHANTED_BOOK, "&aNatural", pageCmd, "core",
                "&7Potential Unlock · Flight · Meditation · Jump · Sprint"));
        gui.addButton(pageBtn(46, Material.AMETHYST_SHARD, "&dSaga", pageCmd, "saga",
                "&7Saga skill unlocks"));
        gui.addButton(pageBtn(47, Material.DIAMOND, "&bAdvanced", pageCmd, "advanced",
                "&7DMZ 2.1 skills"));

        gui.addButton(hubBtn(49));
        if (staffAdmin) {
            gui.addButton(progBtn(51));
        }
        gui.addButton(closeBtn(53));
        fillFrameOnly(gui, 6);
        gui.open();
    }

    private static boolean skillUnlocked(List<String> skillLore) {
        if (skillLore == null || skillLore.isEmpty()) {
            return false;
        }
        String first = skillLore.get(0);
        if (first.contains("MAX")) {
            return true;
        }
        // level X/Y with X>0
        String plain = first.replace('§', '&');
        int idx = plain.lastIndexOf('/');
        if (idx > 0) {
            try {
                int slash = plain.lastIndexOf('/');
                int start = slash;
                while (start > 0 && Character.isDigit(plain.charAt(start - 1))) {
                    start--;
                }
                // find digits before /
                String before = plain.substring(Math.max(0, slash - 4), slash).replaceAll("[^0-9]", "");
                if (!before.isEmpty() && Integer.parseInt(before) > 0) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static List<String> prependBlank(List<String> tip) {
        List<String> out = new ArrayList<>();
        out.add("");
        out.addAll(tip);
        return out;
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
        btn.addCommand("lm do open progression");
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
