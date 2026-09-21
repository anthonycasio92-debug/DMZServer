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
                case "forms", "form", "effects", "effect" -> openForms(player);
                case "cap", "breakthrough", "breakthroughs" -> openCap(player);
                case "tiers", "tier", "difficulty" -> openTiers(player);
                default -> {
                    if (p.startsWith("shop") || p.startsWith("skills")) {
                        openShop(player, shopPageIndex(p));
                    } else {
                        openMain(player);
                    }
                }
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
        if (!bridgeOk || !systemOn) {
            gui.addButton(walletBtn(player, "main",
                    !bridgeOk ? "&c&lUNAVAILABLE" : "&c&lPRESTIGE DISABLED",
                    unavailableLore(bridgeOk)));
            gui.addButton(hubBtn(40));
            if (ForgeBridge.isStaff(player)) {
                gui.addButton(progBtn(38));
            }
            gui.addButton(closeBtn(44));
            fillEmpty(gui, 5);
            GuiFeedback.openCmi(gui);
            return;
        }
        gui.addButton(walletBtn(player, "main", "&e&lWallet", null));

        boolean ready = "true".equalsIgnoreCase(ph.getOrDefault("ready", "false"));
        List<String> confirmDefaults = List.of(
                "&8Prestige — confirm again within 10 seconds",
                "&8Resets DMZ stats · awards held Prestige");
        CMIGuiButton confirm = new CMIGuiButton(20,
                ready ? Material.LIME_CONCRETE : Material.ORANGE_CONCRETE,
                GuiTooltips.name("prestige.main.confirm",
                        ready ? "&aConfirm Prestige" : "&eAttempt Prestige"));
        confirm.lockField();
        confirm.addLore(GuiTooltips.buttonLore("prestige.main.confirm", confirmDefaults));
        confirm.addCommand("lmdo prestige confirm 0 main");
        gui.addButton(confirm);

        Map<String, String> vars = Map.of(
                "points", ph.getOrDefault("points", "0"),
                "level_cap", ph.getOrDefault("level_cap_fmt", "100000"));
        gui.addButton(navBtn(22, "prestige.main.turnin", Material.GOLD_NUGGET, "&eTurn In Prestiges",
                List.of("&7Convert held prestiges into points",
                        "&7Balance: &e" + vars.get("points")),
                vars, "turnin"));
        gui.addButton(navBtn(24, "prestige.main.shop", Material.EXPERIENCE_BOTTLE, "&aSkill Shop",
                List.of("&71 point = +1 skill level (&dPotential &7+2)",
                        "&7Skill Check skills only · &apermanent",
                        "&8Survives prestige reset"), null, "shop"));
        gui.addButton(navBtn(29, "prestige.main.effects", Material.MAGENTA_DYE, "&dEffects",
                List.of("&7Permanent Majin / Mutant (&e5 &7pts)",
                        "&aPermanent purchase &8· unpurchase = no refund"), null, "effects"));
        gui.addButton(navBtn(31, "prestige.main.tiers", Material.BEACON, "&6Difficulty Tiers",
                List.of("&7Permanent unlocks with prestige points",
                        "&7T1–2 &e1pt &8· &7T3–4 &e2pt &8· &7T5–6 &e3pt &8· &7T7 &e4pt",
                        "&aPermanent &8· survives prestige"), null, "tiers"));
        gui.addButton(navBtn(33, "prestige.main.cap", Material.NETHER_STAR, "&bLevel Cap Breakthrough",
                List.of("&7Raise &fyour &7personal level cap +10k",
                        "&7Cap: &f" + vars.get("level_cap"),
                        "&8DMZ maxValue 150000 — soft-lock holds others at their cap"),
                vars, "cap"));

        gui.addButton(hubBtn(40));
        if (ForgeBridge.isStaff(player)) {
            gui.addButton(progBtn(38));
        }
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openTurnIn(Player player) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(player);
        CMIGui gui = base(player, "&8Prestige · Turn In", 4);
        gui.addButton(walletBtn(player, "turnin", "&e&lWallet", null));

        int held = parseInt(ph.get("held"), 0);
        addTurnIn(gui, 19, 1, held, ph);
        addTurnIn(gui, 20, 2, held, ph);
        addTurnIn(gui, 21, 3, held, ph);
        addTurnIn(gui, 23, 6, held, ph);
        addTurnIn(gui, 24, 9, held, ph);

        gui.addButton(backBtn(27));
        gui.addButton(hubBtn(31));
        gui.addButton(closeBtn(35));
        fillEmpty(gui, 4);
        GuiFeedback.openCmi(gui);
    }

    private static void addTurnIn(CMIGui gui, int slot, int amount, int held, Map<String, String> ph) {
        boolean ok = held >= amount;
        Map<String, String> vars = Map.of(
                "amount", String.valueOf(amount),
                "gain", ph.getOrDefault("turnin_" + amount + "_points", "0"),
                "held", String.valueOf(held));
        List<String> defaults = List.of(
                "&7Spend &6{amount} &7held prestige" + (amount == 1 ? "" : "s"),
                "&7Gain &e{gain} &7points");
        String fallback = (ok ? "&eTurn In &f" : "&8Need &f") + amount;
        // When unaffordable, keep Need styling (JSON name is always "Turn In {amount}").
        String display = ok
                ? GuiTooltips.name("prestige.turnin.amount", fallback, vars)
                : fallback;
        CMIGuiButton btn = new CMIGuiButton(slot,
                ok ? Material.GOLD_INGOT : Material.GRAY_DYE,
                display);
        btn.lockField();
        btn.addLore(GuiTooltips.buttonLore("prestige.turnin.amount", defaults, vars,
                (amount == 3 || amount == 6 || amount == 9)
                        ? List.of("&aPack bonus: &f3→4 &8· &f6→9 &8· &f9→15")
                        : null));
        if (ok) {
            btn.addCommand("lmdo prestige turnin " + amount + " turnin");
        }
        gui.addButton(btn);
    }

    private static int shopPageIndex(String page) {
        if (page == null || page.isBlank()) {
            return 0;
        }
        String p = page.toLowerCase(Locale.ROOT);
        if ("shop".equals(p) || "skills".equals(p)) {
            return 0;
        }
        String num = null;
        if (p.startsWith("shop") && p.length() > 4) {
            num = p.substring(4);
        } else if (p.startsWith("skills") && p.length() > 6) {
            num = p.substring(6);
        }
        if (num != null) {
            try {
                return Math.max(0, Integer.parseInt(num) - 1);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private static void openShop(Player player, int pageIndex) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(player);
        int pages = Math.max(1, parseInt(ph.get("shop_pages"), 1));
        int page = Math.max(0, Math.min(pages - 1, pageIndex));
        String pageKey = page <= 0 ? "shop" : ("shop" + (page + 1));
        String shopTitle = pages > 1
                ? "&8Prestige · Skills &7(" + (page + 1) + "/" + pages + ")"
                : "&8Prestige · Skills";
        CMIGui gui = base(player, shopTitle, 6);
        gui.addButton(walletBtn(player, "shop", "&e&lWallet", null));

        List<String> ids = shopSkillIds(ph);
        int pageSize = Math.max(1, parseInt(ph.get("shop_page_size"), GuiPlayerPicker.CONTENT_SLOTS.length));
        int from = page * pageSize;
        int placed = 0;
        for (int i = from; i < ids.size() && placed < GuiPlayerPicker.CONTENT_SLOTS.length; i++) {
            String id = ids.get(i);
            if (id == null || id.isBlank()) {
                continue;
            }
            String label = ph.getOrDefault("skill_" + id + "_label", prettyId(id));
            addSkill(gui, GuiPlayerPicker.CONTENT_SLOTS[placed++],
                    GuiLoreChunks.skillIcon(label), id, label, ph, pageKey);
        }

        gui.addButton(backBtn(45));
        if (page > 0) {
            String prev = page == 1 ? "shop" : ("shop" + page);
            Map<String, String> prevVars = Map.of("page", String.valueOf(page), "pages", String.valueOf(pages));
            gui.addButton(navBtn(48, "prestige.shop.prev", Material.ARROW, "&7« Prev",
                    List.of("&7Page {page}/{pages}"), prevVars, prev));
        }
        gui.addButton(hubBtn(49));
        if (page + 1 < pages) {
            Map<String, String> nextVars = Map.of(
                    "page", String.valueOf(page + 2), "pages", String.valueOf(pages));
            gui.addButton(navBtn(50, "prestige.shop.next", Material.ARROW, "&7Next »",
                    List.of("&7Page {page}/{pages}"), nextVars,
                    "shop" + (page + 2)));
        }
        gui.addButton(closeBtn(53));
        fillEmpty(gui, 6);
        GuiFeedback.openCmi(gui);
    }

    private static List<String> shopSkillIds(Map<String, String> ph) {
        List<String> out = new ArrayList<>();
        String raw = ph.getOrDefault("shop_skill_ids", "");
        if (raw == null || raw.isBlank()) {
            return List.of("meditation", "fly", "sprint", "jump", "potentialunlock");
        }
        for (String part : raw.split(",")) {
            if (part != null && !part.isBlank()) {
                out.add(part.trim().toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    private static String prettyId(String id) {
        if (id == null || id.isBlank()) {
            return "Skill";
        }
        String[] parts = id.split("[_\\-]+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1));
            }
        }
        return sb.length() == 0 ? id : sb.toString();
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static void addSkill(
            CMIGui gui, int slot, Material mat, String id, String label,
            Map<String, String> ph, String reopenPage
    ) {
        String bought = ph.getOrDefault("skill_" + id, "0");
        String max = ph.getOrDefault("skill_" + id + "_max", "10");
        String gainLevels = "potentialunlock".equalsIgnoreCase(id) ? "2" : "1";
        Map<String, String> vars = Map.of(
                "label", label,
                "bought", bought,
                "max", max,
                "gain_levels", gainLevels);
        List<String> defaults = List.of(
                "&7Prestige floor: &f{bought}&7/&f{max}",
                "&7Cost: &e1 &7point → &a+{gain_levels} &7level" + ("1".equals(gainLevels) ? "" : "s"),
                "&aPermanent &8· survives prestige reset");
        CMIGuiButton btn = new CMIGuiButton(slot, mat,
                GuiTooltips.name("prestige.shop.skill", "&a" + label, vars));
        btn.lockField();
        btn.addLore(GuiTooltips.buttonLore("prestige.shop.skill", defaults, vars, null));
        btn.addCommand("lmdo prestige skill " + id + " " + reopenPage);
        gui.addButton(btn);
    }

    private static void openForms(Player player) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(player);
        CMIGui gui = base(player, "&8Prestige · Effects", 4);
        gui.addButton(walletBtn(player, "forms", "&e&lWallet", null));

        boolean hasMajin = "true".equalsIgnoreCase(ph.getOrDefault("majin", "false"));
        boolean hasMutant = "true".equalsIgnoreCase(ph.getOrDefault("mutant", "false"));
        String cost = ph.getOrDefault("form_cost", "5");
        Map<String, String> costVars = Map.of("cost", cost);

        if (hasMajin) {
            CMIGuiButton majin = new CMIGuiButton(20, Material.LIME_DYE,
                    GuiTooltips.name("prestige.effects.majin_owned", "&aPermanent Majin"));
            majin.lockField();
            majin.addLore(GuiTooltips.buttonLore("prestige.effects.majin_owned",
                    List.of("&aOwned", "&aPermanent purchase &8· only one at a time"), costVars, null));
            gui.addButton(majin);
        } else {
            List<String> majinDefaults = new ArrayList<>();
            majinDefaults.add("&7Cost: &e{cost} &7points");
            if (hasMutant) {
                majinDefaults.add("&8Buying removes Mutant (no refund)");
            }
            majinDefaults.add("&aPermanent purchase &8· only one at a time");
            CMIGuiButton majin = new CMIGuiButton(20, Material.PINK_DYE,
                    GuiTooltips.name("prestige.effects.majin", "&dBuy Permanent Majin", costVars));
            majin.lockField();
            majin.addLore(GuiTooltips.buttonLore("prestige.effects.majin", majinDefaults, costVars, null));
            majin.addCommand("lmdo prestige majin 0 forms");
            gui.addButton(majin);
        }

        if (hasMutant) {
            CMIGuiButton mutant = new CMIGuiButton(22, Material.LIME_DYE,
                    GuiTooltips.name("prestige.effects.mutant_owned", "&aPermanent Mutant"));
            mutant.lockField();
            mutant.addLore(GuiTooltips.buttonLore("prestige.effects.mutant_owned",
                    List.of("&aOwned", "&aPermanent purchase &8· only one at a time"), costVars, null));
            gui.addButton(mutant);
        } else {
            List<String> mutantDefaults = new ArrayList<>();
            mutantDefaults.add("&7Cost: &e{cost} &7points");
            if (hasMajin) {
                mutantDefaults.add("&8Buying removes Majin (no refund)");
            }
            mutantDefaults.add("&aPermanent purchase &8· only one at a time");
            CMIGuiButton mutant = new CMIGuiButton(22, Material.SLIME_BALL,
                    GuiTooltips.name("prestige.effects.mutant", "&aBuy Permanent Mutant", costVars));
            mutant.lockField();
            mutant.addLore(GuiTooltips.buttonLore("prestige.effects.mutant", mutantDefaults, costVars, null));
            mutant.addCommand("lmdo prestige mutant 0 forms");
            gui.addButton(mutant);
        }

        if (hasMajin) {
            CMIGuiButton un = new CMIGuiButton(24, Material.BARRIER,
                    GuiTooltips.name("prestige.effects.unmajin", "&cUnpurchase Majin"));
            un.lockField();
            un.addLore(GuiTooltips.buttonLore("prestige.effects.unmajin",
                    List.of("&7Removes Majin · &cno point refund")));
            un.addCommand("lmdo prestige unmajin 0 forms");
            gui.addButton(un);
        } else if (hasMutant) {
            CMIGuiButton un = new CMIGuiButton(24, Material.BARRIER,
                    GuiTooltips.name("prestige.effects.unmutant", "&cUnpurchase Mutant"));
            un.lockField();
            un.addLore(GuiTooltips.buttonLore("prestige.effects.unmutant",
                    List.of("&7Removes Mutant · &cno point refund")));
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
        gui.addButton(walletBtn(player, "cap", "&e&lWallet", null));

        int bt = parseInt(ph.get("breakthroughs"), 0);
        int max = parseInt(ph.get("breakthroughs_max"), 5);
        boolean canBuy = bt < max;
        String levelCapFmt = ph.getOrDefault("level_cap_fmt", "100000");
        String nextCost = ph.getOrDefault("next_breakthrough_cost", "15");
        Map<String, String> capVars = Map.of(
                "level_cap", levelCapFmt,
                "breakthroughs", String.valueOf(bt),
                "max", String.valueOf(max),
                "cost", nextCost);
        CMIGuiButton buy = new CMIGuiButton(22,
                canBuy ? Material.NETHER_STAR : Material.BEACON,
                canBuy ? GuiTooltips.name("prestige.cap.buy", "&bBuy Breakthrough", capVars) : "&aCap Maxed");
        buy.lockField();
        buy.addLore(List.of("",
                "&7Your level cap: &f" + levelCapFmt,
                "&7Breakthroughs: &f" + bt + "&7/&f" + max,
                "&8DMZ maxValue 150000 — soft-lock holds others at their cap"));
        if (canBuy) {
            buy.addLore(GuiTooltips.lore("prestige.cap.buy",
                    List.of("&7Next: &a+10,000 &7personal cap for &e{cost} &7points"),
                    capVars));
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

    private static void openTiers(Player player) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(player);
        CMIGui gui = base(player, "&8Prestige · Difficulty Tiers", 5);
        gui.addButton(walletBtn(player, "tiers", "&e&lWallet", null));

        Material[] mats = {
                Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT,
                Material.EMERALD, Material.DIAMOND, Material.NETHERITE_INGOT, Material.NETHER_STAR
        };
        int[] slots = {19, 20, 21, 22, 23, 24, 25};
        for (int t = 1; t <= 7; t++) {
            boolean owned = "true".equalsIgnoreCase(ph.getOrDefault("tier_" + t + "_owned", "false"));
            boolean unlocked = "true".equalsIgnoreCase(ph.getOrDefault("tier_" + t + "_unlocked", "false"));
            boolean canBuy = "true".equalsIgnoreCase(ph.getOrDefault("tier_" + t + "_can_buy", "false"));
            String label = ph.getOrDefault("tier_" + t + "_label", "T" + t);
            String cost = ph.getOrDefault("tier_" + t + "_cost", String.valueOf((t + 1) / 2));
            String title;
            if (owned) {
                title = "&aT" + t + " " + label;
            } else if (canBuy) {
                title = "&6T" + t + " " + label;
            } else {
                title = "&8T" + t + " " + label;
            }
            CMIGuiButton btn = new CMIGuiButton(slots[t - 1],
                    canBuy || owned ? mats[t - 1] : Material.GRAY_DYE, title);
            btn.lockField();
            Map<String, String> tierVars = Map.of(
                    "tier", String.valueOf(t),
                    "prev", String.valueOf(Math.max(1, t - 1)),
                    "cost", cost,
                    "label", label);
            List<String> lore = new ArrayList<>();
            lore.add("");
            if (owned) {
                lore.addAll(GuiTooltips.lore("prestige.tiers.owned",
                        List.of("&aOwned · permanent"), tierVars));
            } else if (!canBuy) {
                lore.addAll(GuiTooltips.lore("prestige.tiers.locked",
                        List.of("&cLocked",
                                "&7Buy permanent &fT" + (t - 1) + " &7first (shop ladder)"),
                        tierVars));
            } else if (unlocked) {
                lore.addAll(GuiTooltips.lore("prestige.tiers.buyable_unlocked",
                        List.of("&7Cost: &e" + cost + " &7point" + ("1".equals(cost) ? "" : "s"),
                                "&eCurrently unlocked via level/prestige",
                                "&7Buy to make it &apermanent &7(survives prestige)"),
                        tierVars));
            } else {
                lore.addAll(GuiTooltips.lore("prestige.tiers.buyable",
                        List.of("&7Cost: &e" + cost + " &7point" + ("1".equals(cost) ? "" : "s"),
                                "&aPermanent unlock &8· survives prestige"),
                        tierVars));
            }
            if (!owned && t > 1) {
                lore.addAll(GuiTooltips.lore("prestige.tiers.footer",
                        List.of("&8Requires permanent T1–T" + (t - 1) + " from this shop",
                                "&8Then activate with Ancient Coins via /difficulty → Tiers"),
                        tierVars));
            } else {
                lore.add("&8Then activate with Ancient Coins via /difficulty → Tiers");
            }
            btn.addLore(lore);
            if (canBuy) {
                btn.addCommand("lmdo prestige tier " + t + " tiers");
            }
            gui.addButton(btn);
        }

        gui.addButton(backBtn(36));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static CMIGuiButton walletBtn(Player player, String page, String title, List<String> overrideLore) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(player);
        if (overrideLore != null && !overrideLore.isEmpty()) {
            CMIGuiButton btn = new CMIGuiButton(4, Material.GOLD_INGOT, title);
            btn.lockField();
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("&6Wallet: &e" + ph.getOrDefault("points", "0") + " &7prestige points");
            lore.addAll(overrideLore);
            btn.addLore(lore);
            return btn;
        }
        Map<String, String> vars = Map.of("points", ph.getOrDefault("points", "0"));
        List<String> walletLine = GuiTooltips.lore("prestige.main.wallet",
                List.of("&6Wallet: &e{points} &7prestige points"), vars);
        CMIGuiButton btn = new CMIGuiButton(4, Material.GOLD_INGOT,
                GuiTooltips.name("prestige.main.wallet", title, vars));
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(walletLine.isEmpty() ? List.of("") : walletLine);
        lore.addAll(toAmp(ForgeBridge.prestigeLines(player, page == null ? "main" : page)));
        btn.addLore(lore);
        return btn;
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
        return navBtn(slot, null, mat, name, tip, null, page);
    }

    private static CMIGuiButton navBtn(
            int slot, String key, Material mat, String name, List<String> tip,
            Map<String, String> vars, String page
    ) {
        String display = key == null || key.isBlank() ? name : GuiTooltips.name(key, name, vars);
        CMIGuiButton btn = new CMIGuiButton(slot, mat, display);
        btn.lockField();
        btn.addLore(key == null || key.isBlank()
                ? withBlank(tip)
                : GuiTooltips.buttonLore(key, tip, vars, null));
        btn.addCommand("lmdo lm open prestige " + page);
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

    private static CMIGuiButton backBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.ARROW, "&7« Prestige");
        btn.lockField();
        btn.addCommand("lmdo lm open prestige main");
        return btn;
    }

    private static CMIGuiButton progBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.BREWING_STAND,
                GuiTooltips.name("prestige.main.progression", "&dProgression"));
        btn.lockField();
        btn.addLore(GuiTooltips.buttonLore("prestige.main.progression",
                List.of("&7Skills · TP · Race · Combat flags", "&eOpen")));
        btn.addCommand("lmdo lm open progression");
        btn.setCloseInv(true);
        return btn;
    }

    private static CMIGuiButton closeBtn(int slot) {
        return GuiNav.cmiCloseButton(slot);
    }
}
