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
 * One Skills page. Centered skill tiles; chrome matches Spar/Prestige.
 */
public final class CmiSkillsGui {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private CmiSkillsGui() {}

    public static boolean available() {
        return CmiDifficultyGui.available();
    }

    public static boolean open(Player player, String page) {
        if (ForgeBridge.hasSkillCheck(player)) {
            return open(player, page, true);
        }
        if (ForgeBridge.isStaff(player)) {
            return open(player, page, false);
        }
        player.sendMessage("§cSkill Check requires donator access.");
        return false;
    }

    public static boolean open(Player player, String page, boolean skillCheckMode) {
        if (player == null || !available()) {
            return false;
        }
        if (skillCheckMode) {
            if (!ForgeBridge.hasSkillCheck(player)) {
                player.sendMessage("§cSkill Check requires donator access.");
                return false;
            }
        } else if (!ForgeBridge.isStaff(player)) {
            player.sendMessage("§cStaff only.");
            return false;
        }
        String p = page == null || page.isBlank() ? "core" : page.toLowerCase(Locale.ROOT);
        try {
            if ("help".equals(p)) {
                openPage(player, "help", "&eSkills", Material.BOOK, skillCheckMode);
            } else {
                openPage(player, "core", "&eSkills", Material.BOOK, skillCheckMode);
            }
            return true;
        } catch (Throwable t) {
            player.sendMessage("§cSkills CMI GUI failed: " + t.getMessage());
            return false;
        }
    }

    private static void openPage(Player player, String page, String title, Material mat, boolean skillCheckMode) {
        Map<String, String> ph = ForgeBridge.skillsPlaceholders(player);
        boolean skillCheckUi = skillCheckMode || (ForgeBridge.hasSkillCheck(player)
                && ForgeBridge.inSkillCheckSession(player));
        boolean staffAdmin = !skillCheckUi && ForgeBridge.isStaff(player);
        String windowTitle = skillCheckUi
                ? "&8Skill Check"
                : staffAdmin ? "&8Skills (Admin)" : "&8Skills";
        CMIGui gui = base(player, windowTitle, 6);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        String statusName = !bridgeOk ? "&c&lUNAVAILABLE"
                : !systemOn ? "&c&lSKILLS DISABLED"
                : skillCheckUi ? title + " Skill Check"
                : staffAdmin ? title + " (Admin)" : title + " Skills";
        // Skill Check: EXPERIENCE_BOTTLE header. Staff Skills: BOOK.
        Material headerMat = skillCheckUi ? Material.EXPERIENCE_BOTTLE : Material.BOOK;
        CMIGuiButton status = new CMIGuiButton(4, headerMat, statusName);
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

        List<String> raw = toAmp(ForgeBridge.skillsLines(player, page));
        if ("help".equals(page)) {
            status.addLore(prependBlank(raw.isEmpty()
                    ? List.of("&7Every tracked skill is on this page.")
                    : raw));
            gui.addButton(status);
        } else {
            GuiLoreChunks.SkillPage split = GuiLoreChunks.splitSkillsPage(raw);
            List<String> headerLore = new ArrayList<>();
            headerLore.add("");
            if (split.header.isEmpty()) {
                headerLore.add("&7DMZ stats unavailable");
            } else {
                headerLore.add(split.header.get(0));
            }
            headerLore.add("");
            headerLore.addAll(GuiTooltips.lore("skills.main.header",
                    List.of("&7Every tracked skill")));
            status.addLore(headerLore);
            gui.addButton(status);

            int[] slots = GuiBoardHelper.centeredSlots(split.skills.size());
            int placed = 0;
            for (List<String> skill : split.skills) {
                if (placed >= slots.length) {
                    break;
                }
                int slot = slots[placed++];
                String name = GuiLoreChunks.skillDisplayName(skill);
                Material icon = GuiLoreChunks.skillIcon(name);
                CMIGuiButton btn = new CMIGuiButton(slot, icon, name);
                btn.lockField();
                List<String> lore = new ArrayList<>();
                lore.add("");
                if (skill != null && !skill.isEmpty()) {
                    String levelLine = skill.get(0);
                    int sep = Math.max(levelLine.indexOf("§7:"), levelLine.indexOf("&7:"));
                    if (sep >= 0 && sep + 3 < levelLine.length()) {
                        lore.add("&7" + levelLine.substring(sep + 3).trim());
                    }
                    for (int i = 1; i < skill.size(); i++) {
                        lore.add(skill.get(i));
                    }
                }
                lore.add("");
                lore.add(GuiLoreChunks.skillStatusFooter(skill));
                btn.addLore(lore);
                gui.addButton(btn);
            }
            if (placed == 0) {
                CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                        GuiTooltips.name("skills.empty", "&cNo skills listed"));
                empty.lockField();
                empty.addLore(GuiTooltips.buttonLore("skills.empty", List.of("&7Nothing to show right now")));
                gui.addButton(empty);
            }
        }

        gui.addButton(pageBtn(player, 45, "skills.main.skills", Material.BOOK, "&eSkills", "core", skillCheckUi,
                "&7Potential Unlock and saga skills"));

        gui.addButton(hubBtn(49));
        if (staffAdmin) {
            gui.addButton(progBtn(player, 51));
        }
        gui.addButton(closeBtn(53));
        fillEmpty(gui, 6);
        GuiFeedback.openCmi(gui);
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
            Player player, int slot, String key, Material mat, String name, String page,
            boolean skillCheckUi, String... tips) {
        String display = key == null || key.isBlank() ? name : GuiTooltips.name(key, name);
        CMIGuiButton btn = new CMIGuiButton(slot, mat, display);
        btn.lockField();
        List<String> defaults = GuiBoardHelper.tips(player, tips);
        btn.addLore(key == null || key.isBlank()
                ? withBlank(defaults)
                : GuiTooltips.buttonLore(key, defaults));
        // Skill Check pages must keep the session route — lmdo skills is staff-only
        // and blocked normal players from Natural ↔ Saga.
        String cmdRoot = skillCheckUi ? "skillcheck" : "skills";
        btn.addCommand("lmdo " + cmdRoot + " page " + page);
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
        return GuiNav.cmiHubButton(slot);
    }

    private static CMIGuiButton progBtn(Player player, int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.BREWING_STAND,
                GuiTooltips.name("skills.main.progression", "&dProgression"));
        btn.lockField();
        btn.addLore(GuiTooltips.buttonLore("skills.main.progression",
                GuiBoardHelper.tipsList(player, List.of("&7Skills · TP · Race · Combat flags", "&eOpen"))));
        btn.addCommand("lmdo lm open progression");
        btn.setCloseInv(true);
        return btn;
    }

    private static CMIGuiButton closeBtn(int slot) {
        return GuiNav.cmiCloseButton(slot);
    }
}
