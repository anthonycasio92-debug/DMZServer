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

/** CMILib inventory GUI — Prestige purchase, turn-in, and prestige-points shop. */
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
            String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
            switch (p) {
                case "turnin", "points" -> openTurnIn(player);
                case "shop", "skills" -> openShop(player);
                case "forms", "form" -> openForms(player);
                case "cap", "breakthrough", "breakthroughs" -> openCap(player);
                default -> openMain(player);
            }
            return true;
        } catch (Throwable t) {
            player.sendMessage("§cPrestige CMI GUI failed: " + t.getMessage());
            return false;
        }
    }

    private static void openMain(Player player) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(player);
        CMIGui gui = base(player, "&8Prestige", 5);

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
            gui.addButton(hubBtn(36));
            if (ForgeBridge.isStaff(player)) {
                gui.addButton(progBtn(40));
            }
            gui.addButton(closeBtn(44));
            fillEmpty(gui, 5);
            GuiFeedback.openCmi(gui);
            return;
        }
        status.addLore(toAmp(ForgeBridge.prestigeLines(player, "main")));
        gui.addButton(status);

        boolean ready = "true".equalsIgnoreCase(ph.getOrDefault("ready", "false"));
        CMIGuiButton confirm = new CMIGuiButton(20,
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

        gui.addButton(navBtn(22, Material.GOLD_NUGGET, "&eTurn In Prestiges",
                List.of("&7Convert held prestiges into points",
                        "&7Balance: &e" + ph.getOrDefault("points", "0")),
                "turnin"));
        gui.addButton(navBtn(24, Material.EXPERIENCE_BOTTLE, "&aSkill Shop",
                List.of("&71 point = +1 permanent skill level"), "shop"));
        gui.addButton(navBtn(30, Material.MAGENTA_DYE, "&dForms",
                List.of("&7Permanent Majin / Mutant (&e5 &7pts)"), "forms"));
        gui.addButton(navBtn(32, Material.NETHER_STAR, "&bLevel Cap Breakthrough",
                List.of("&7Raise &fyour &7personal level cap +10k",
                        "&7Cap: &f" + ph.getOrDefault("level_cap_fmt", "100000"),
                        "&8Server hardcap stays &f100000"),
                "cap"));

        gui.addButton(hubBtn(36));
        if (ForgeBridge.isStaff(player)) {
            gui.addButton(progBtn(40));
        }
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openTurnIn(Player player) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(player);
        CMIGui gui = base(player, "&8Prestige · Turn In", 4);
        CMIGuiButton status = new CMIGuiButton(4, Material.GOLD_NUGGET, "&e&lTurn In Prestiges");
        status.lockField();
        status.addLore(toAmp(ForgeBridge.prestigeLines(player, "turnin")));
        gui.addButton(status);

        int held = parseInt(ph.get("held"), 0);
        addTurnIn(gui, 19, 1, held, ph);
        addTurnIn(gui, 21, 3, held, ph);
        addTurnIn(gui, 23, 5, held, ph);
        CMIGuiButton all = new CMIGuiButton(25, Material.GOLD_BLOCK, "&6Turn In All");
        all.lockField();
        all.addLore(List.of("", "&7Turn in all &6" + held + " &7held",
                "&7Gain &e" + ph.getOrDefault("turnin_all_points", "0") + " &7points"));
        if (held > 0) {
            all.addCommand("lmdo prestige turnin all turnin");
        }
        gui.addButton(all);

        gui.addButton(backBtn(27));
        gui.addButton(hubBtn(31));
        gui.addButton(closeBtn(35));
        fillEmpty(gui, 4);
        GuiFeedback.openCmi(gui);
    }

    private static void addTurnIn(CMIGui gui, int slot, int amount, int held, Map<String, String> ph) {
        boolean ok = held >= amount;
        CMIGuiButton btn = new CMIGuiButton(slot,
                ok ? Material.GOLD_INGOT : Material.GRAY_DYE,
                (ok ? "&eTurn In &f" : "&8Need &f") + amount);
        btn.lockField();
        btn.addLore(List.of("",
                "&7Spend &6" + amount + " &7held",
                "&7Gain &e" + ph.getOrDefault("turnin_" + amount + "_points", "0") + " &7points"));
        if (ok) {
            btn.addCommand("lmdo prestige turnin " + amount + " turnin");
        }
        gui.addButton(btn);
    }

    private static void openShop(Player player) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(player);
        CMIGui gui = base(player, "&8Prestige · Skills", 5);
        CMIGuiButton status = new CMIGuiButton(4, Material.EXPERIENCE_BOTTLE, "&a&lSkill Shop");
        status.lockField();
        status.addLore(toAmp(ForgeBridge.prestigeLines(player, "shop")));
        gui.addButton(status);

        addSkill(gui, 19, Material.ENCHANTED_BOOK, "meditation", "Meditation", ph);
        addSkill(gui, 20, Material.FEATHER, "fly", "Fly", ph);
        addSkill(gui, 21, Material.SUGAR, "sprint", "Sprint", ph);
        addSkill(gui, 22, Material.RABBIT_FOOT, "jump", "Jump", ph);
        addSkill(gui, 23, Material.NETHER_STAR, "potentialunlock", "Potential Unlock", ph);

        gui.addButton(backBtn(36));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void addSkill(
            CMIGui gui, int slot, Material mat, String id, String label, Map<String, String> ph
    ) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, "&a" + label);
        btn.lockField();
        btn.addLore(List.of("",
                "&7Prestige floor: &f" + ph.getOrDefault("skill_" + id, "0")
                        + "&7/&f" + ph.getOrDefault("skill_" + id + "_max", "10"),
                "&7Cost: &e1 &7point → &a+1 &7level",
                "&8Survives prestige reset"));
        btn.addCommand("lmdo prestige skill " + id + " shop");
        gui.addButton(btn);
    }

    private static void openForms(Player player) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(player);
        CMIGui gui = base(player, "&8Prestige · Forms", 4);
        CMIGuiButton status = new CMIGuiButton(4, Material.MAGENTA_DYE, "&d&lPermanent Forms");
        status.lockField();
        status.addLore(toAmp(ForgeBridge.prestigeLines(player, "forms")));
        gui.addButton(status);

        boolean hasMajin = "true".equalsIgnoreCase(ph.getOrDefault("majin", "false"));
        boolean hasMutant = "true".equalsIgnoreCase(ph.getOrDefault("mutant", "false"));
        String cost = ph.getOrDefault("form_cost", "5");

        CMIGuiButton majin = new CMIGuiButton(20,
                hasMajin ? Material.LIME_DYE : Material.PINK_DYE,
                hasMajin ? "&aPermanent Majin" : "&dBuy Permanent Majin");
        majin.lockField();
        majin.addLore(List.of("", hasMajin ? "&aOwned" : "&7Cost: &e" + cost + " &7points",
                "&8Only one form at a time"));
        if (!hasMajin) {
            majin.addCommand("lmdo prestige majin 0 forms");
        }
        gui.addButton(majin);

        CMIGuiButton mutant = new CMIGuiButton(22,
                hasMutant ? Material.LIME_DYE : Material.SLIME_BALL,
                hasMutant ? "&aPermanent Mutant" : "&aBuy Permanent Mutant");
        mutant.lockField();
        mutant.addLore(List.of("", hasMutant ? "&aOwned" : "&7Cost: &e" + cost + " &7points",
                "&8Only one form at a time"));
        if (!hasMutant) {
            mutant.addCommand("lmdo prestige mutant 0 forms");
        }
        gui.addButton(mutant);

        if (hasMajin) {
            CMIGuiButton un = new CMIGuiButton(24, Material.BARRIER, "&cUnpurchase Majin");
            un.lockField();
            un.addLore(List.of("", "&7Removes Majin · &cno point refund"));
            un.addCommand("lmdo prestige unmajin 0 forms");
            gui.addButton(un);
        } else if (hasMutant) {
            CMIGuiButton un = new CMIGuiButton(24, Material.BARRIER, "&cUnpurchase Mutant");
            un.lockField();
            un.addLore(List.of("", "&7Removes Mutant · &cno point refund"));
            un.addCommand("lmdo prestige unmutant 0 forms");
            gui.addButton(un);
        }

        gui.addButton(backBtn(27));
        gui.addButton(hubBtn(31));
        gui.addButton(closeBtn(35));
        fillEmpty(gui, 4);
        GuiFeedback.openCmi(gui);
    }

    private static void openCap(Player player) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(player);
        CMIGui gui = base(player, "&8Prestige · Level Cap", 4);
        CMIGuiButton status = new CMIGuiButton(4, Material.NETHER_STAR, "&b&lPersonal Level Cap");
        status.lockField();
        status.addLore(toAmp(ForgeBridge.prestigeLines(player, "cap")));
        gui.addButton(status);

        int bt = parseInt(ph.get("breakthroughs"), 0);
        int max = parseInt(ph.get("breakthroughs_max"), 5);
        boolean canBuy = bt < max;
        CMIGuiButton buy = new CMIGuiButton(22,
                canBuy ? Material.NETHER_STAR : Material.BEACON,
                canBuy ? "&bBuy Breakthrough" : "&aCap Maxed");
        buy.lockField();
        buy.addLore(List.of("",
                "&7Your level cap: &f" + ph.getOrDefault("level_cap_fmt", "100000"),
                "&7Breakthroughs: &f" + bt + "&7/&f" + max,
                "&8Server hardcap stays &f100000 &8for everyone else"));
        if (canBuy) {
            buy.addLore("&7Next: &a+10,000 &7personal cap for &e"
                    + ph.getOrDefault("next_breakthrough_cost", "15") + " &7points");
            buy.addCommand("lmdo prestige breakthrough 0 cap");
        } else {
            buy.addLore("&aMax personal cap (150000)");
        }
        gui.addButton(buy);

        gui.addButton(backBtn(27));
        gui.addButton(hubBtn(31));
        gui.addButton(closeBtn(35));
        fillEmpty(gui, 4);
        GuiFeedback.openCmi(gui);
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (Exception e) {
            return fallback;
        }
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

    private static CMIGuiButton navBtn(
            int slot, Material mat, String name, List<String> tip, String page
    ) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(tip);
        btn.addLore(lore);
        btn.addCommand("lmdo lm open prestige " + page);
        return btn;
    }

    private static CMIGuiButton hubBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.COMPASS, "&7« Hub");
        btn.lockField();
        btn.addCommand("lmdo lm open hub");
        btn.setCloseInv(true);
        return btn;
    }

    private static CMIGuiButton backBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.ARROW, "&7« Prestige");
        btn.lockField();
        btn.addCommand("lmdo lm open prestige main");
        return btn;
    }

    private static CMIGuiButton progBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.BREWING_STAND, "&dProgression");
        btn.lockField();
        btn.addCommand("lmdo lm open progression");
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
