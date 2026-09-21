package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import net.Zrips.CMILib.GUI.CMIGuiButton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Shared inventory titles and footer navigation (Back · Hub · Close). */
final class GuiNav {
    static final int BACK_45 = 36;
    static final int HUB_45 = 40;
    static final int CLOSE_45 = 44;

    static final int HUB_36 = 27;
    static final int CLOSE_36 = 35;

    static final int BACK_54 = 45;
    static final int HUB_54 = 49;
    static final int CLOSE_54 = 53;

    static final int BACK_27 = 18;
    static final int HUB_27 = 22;
    static final int CLOSE_27 = 26;

    private GuiNav() {}

    /** {@code &8Title} plus optional {@code · &cSubject} when staff inspects. */
    static String inventoryTitle(Player viewer, Player subject, String ampTitle) {
        String base = ampTitle == null ? "&8Menu" : ampTitle;
        if (!base.startsWith("&8") && !base.startsWith("§8")) {
            base = "&8" + base.replaceFirst("^[&§]8", "");
        }
        if (viewer != null && subject != null
                && !viewer.getUniqueId().equals(subject.getUniqueId())) {
            return color(base + " · &c" + subject.getName());
        }
        return color(base);
    }

    static ItemStack hubItem() {
        return labeled(
                Material.COMPASS,
                GuiTooltips.name("common.hub", "&7« Hub"),
                GuiTooltips.buttonLore("common.hub", List.of(
                        "&7Return to the main &f/lm &7menu",
                        "&8Pick any LM system from there")));
    }

    static ItemStack closeItem() {
        return labeled(
                Material.BARRIER,
                GuiTooltips.name("common.close", "&cClose"),
                GuiTooltips.buttonLore("common.close", List.of("&7Close this menu")));
    }

    static ItemStack backItem(String tooltipKey, String fallbackHint) {
        String key = tooltipKey == null || tooltipKey.isBlank() ? "common.back" : tooltipKey;
        List<String> lore = new ArrayList<>();
        lore.add(fallbackHint == null || fallbackHint.isBlank()
                ? "&7Go back to the previous screen"
                : fallbackHint);
        return labeled(
                Material.ARROW,
                GuiTooltips.name(key, "&7Back"),
                GuiTooltips.buttonLore(key, lore));
    }

    private static ItemStack labeled(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            List<String> colored = new ArrayList<>();
            for (String line : lore) {
                colored.add(color(line));
            }
            meta.setLore(colored);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    static String color(String input) {
        return input == null ? "" : ChatColor.translateAlternateColorCodes('&', input);
    }

    static CMIGuiButton cmiHubButton(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.COMPASS,
                GuiTooltips.name("common.hub", "&7« Hub"));
        btn.lockField();
        btn.addLore(GuiTooltips.buttonLore("common.hub", List.of(
                "&7Return to the main &f/lm &7menu",
                "&8Pick any LM system from there")));
        btn.addCommand("lmdo lm open hub");
        btn.setCloseInv(true);
        return btn;
    }

    static CMIGuiButton cmiCloseButton(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.BARRIER,
                GuiTooltips.name("common.close", "&cClose"));
        btn.lockField();
        btn.addLore(GuiTooltips.buttonLore("common.close", List.of("&7Close this menu")));
        btn.setCloseInv(true);
        return btn;
    }
}
