package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.Zrips.CMILib.GUI.CMIGui;
import net.Zrips.CMILib.GUI.CMIGuiButton;
import net.Zrips.CMILib.GUI.GUIManager.InvType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** CMILib inventory GUI — Legacy Mechanics Prestige. */
public final class CmiPrestigeGui {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private CmiPrestigeGui() {}

    public static boolean available() {
        return CmiDifficultyGui.available();
    }

    public static boolean open(Player player, String page) {
        if (player == null || !available()) {
            return false;
        }
        try {
            openMain(player);
            return true;
        } catch (Throwable t) {
            player.sendMessage("§cPrestige CMI GUI failed: " + t.getMessage());
            return false;
        }
    }

    private static void openMain(Player player) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(player);
        CMIGui gui = base(player, "&8Prestige", 4);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        CMIGuiButton status = new CMIGuiButton(4, Material.GOLDEN_APPLE,
                !bridgeOk ? "&c&lUNAVAILABLE"
                        : !systemOn ? "&c&lPRESTIGE DISABLED"
                        : "&6&lPrestige");
        status.lockField();
        if (!bridgeOk || !systemOn) {
            status.addLore(unavailableLore(bridgeOk));
            gui.addButton(status);
            gui.addButton(hubBtn(27));
            gui.addButton(progBtn(31));
            gui.addButton(closeBtn(35));
            fillEmpty(gui, 4);
            gui.open();
            return;
        }
        status.addLore(toAmp(ForgeBridge.prestigeLines(player, "main")));
        gui.addButton(status);

        boolean ready = "true".equalsIgnoreCase(ph.getOrDefault("ready", "false"));
        CMIGuiButton confirm = new CMIGuiButton(22,
                ready ? Material.LIME_CONCRETE : Material.ORANGE_CONCRETE,
                ready ? "&aConfirm Prestige" : "&eAttempt Prestige");
        confirm.lockField();
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("");
        confirmLore.addAll(GuiBoardHelper.tips(player, "&7Click to prestige (confirm within 10s)"));
        confirmLore.add("&8Resets DMZ stats · awards held Prestige");
        confirm.addLore(confirmLore);
        confirm.addCommand("lmdo prestige confirm 0 main");
        gui.addButton(confirm);

        gui.addButton(hubBtn(27));
        gui.addButton(progBtn(31));
        gui.addButton(closeBtn(35));
        fillEmpty(gui, 4);
        gui.open();
    }

    private static List<String> unavailableLore(boolean bridgeOk) {
        if (!bridgeOk) {
            return List.of("", "&cForge LegacyMechanics mod unreachable");
        }
        return List.of("", "&cPrestige system is disabled");
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

    private static CMIGuiButton hubBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.COMPASS, "&7« Hub");
        btn.lockField();
        btn.addCommand("lm");
        btn.setCloseInv(true);
        return btn;
    }

    private static CMIGuiButton progBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.BREWING_STAND, "&dProgression");
        btn.lockField();
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
