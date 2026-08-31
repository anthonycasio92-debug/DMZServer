package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Bukkit chest GUI — Prestige purchase, turn-in, and prestige-points shop. */
public final class PrestigeChestGui implements Listener {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private final AdaptiveDifficultyGuiPlugin plugin;

    public PrestigeChestGui(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
    }

    private static boolean inspecting(Player viewer, Player subject) {
        return viewer != null && subject != null
                && !viewer.getUniqueId().equals(subject.getUniqueId());
    }

    private static String invTitle(Player viewer, Player subject, String base) {
        if (inspecting(viewer, subject)) {
            return color(base + " · &c" + subject.getName());
        }
        return color(base);
    }

    public void open(Player player, String page) {
        Player viewer = player;
        Player subject = AdminInspectSessions.resolveSubject(viewer);
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        Inventory inv = switch (p) {
            case "turnin", "points" -> turnIn(viewer, subject);
            case "forms", "form", "effects", "effect" -> forms(viewer, subject);
            case "cap", "breakthrough", "breakthroughs" -> cap(viewer, subject);
            case "tiers", "tier", "difficulty" -> tiers(viewer, subject);
            default -> {
                if (p.startsWith("shop") || p.startsWith("skills")) {
                    yield shop(viewer, subject, shopPageIndex(p));
                }
                yield main(viewer, subject);
            }
        };
        GuiFeedback.openChest(viewer, inv);
    }

    private Inventory main(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(subject);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Prestige"));
        holder.bind(inv);
        frame(inv, 45);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        if (!bridgeOk || !systemOn) {
            putWallet(holder, inv, viewer, subject, "main",
                    !bridgeOk ? "&c&lUNAVAILABLE" : "&c&lPRESTIGE DISABLED",
                    unavailableLore(bridgeOk));
            put(holder, inv, 36, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
            if (ForgeBridge.isStaff(viewer)) {
                put(holder, inv, 40, tipBtn(viewer, "prestige.main.progression", Material.BREWING_STAND,
                        "&dProgression",
                        List.of("&7Skills · TP · Race · Combat flags", "&eClick to open")),
                        SlotAction.cmd("lmdo lm open progression"));
            }
            put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        putWallet(holder, inv, viewer, subject, "main", "&e&lWallet", null);

        boolean ready = "true".equalsIgnoreCase(ph.getOrDefault("ready", "false"));
        List<String> confirmDefaults = List.of(
                "&7Click to prestige (confirm within 10s)",
                "&8Resets DMZ stats · awards held Prestige");
        put(holder, inv, 20, item(
                ready ? Material.LIME_CONCRETE : Material.ORANGE_CONCRETE,
                GuiTooltips.name("prestige.main.confirm",
                        ready ? "&aConfirm Prestige" : "&eAttempt Prestige"),
                GuiTooltips.buttonLore("prestige.main.confirm", confirmDefaults)),
                SlotAction.act("confirm", "0", "main"));

        Map<String, String> vars = Map.of(
                "points", ph.getOrDefault("points", "0"),
                "level_cap", ph.getOrDefault("level_cap_fmt", "100000"));
        put(holder, inv, 22, tipBtn(viewer, "prestige.main.turnin", Material.GOLD_NUGGET, "&eTurn In Prestiges",
                List.of("&7Convert held prestiges into points",
                        "&7Balance: &e" + vars.get("points")), vars),
                SlotAction.page("turnin"));
        put(holder, inv, 24, tipBtn(viewer, "prestige.main.shop", Material.EXPERIENCE_BOTTLE, "&aSkill Shop",
                List.of("&71 point = +1 skill level (&dPotential &7+2)",
                        "&7Skill Check skills only · &apermanent",
                        "&8Survives prestige reset")),
                SlotAction.page("shop"));
        put(holder, inv, 29, tipBtn(viewer, "prestige.main.effects", Material.MAGENTA_DYE, "&dEffects",
                List.of("&7Permanent Majin / Mutant (&e5 &7pts)",
                        "&aPermanent purchase &8· unpurchase = no refund")),
                SlotAction.page("effects"));
        put(holder, inv, 31, tipBtn(viewer, "prestige.main.tiers", Material.BEACON, "&6Difficulty Tiers",
                List.of("&7Permanent unlocks with prestige points",
                        "&7T1–2 &e1pt &8· &7T3–4 &e2pt &8· &7T5–6 &e3pt &8· &7T7 &e4pt",
                        "&aPermanent &8· survives prestige")),
                SlotAction.page("tiers"));
        put(holder, inv, 33, tipBtn(viewer, "prestige.main.cap", Material.NETHER_STAR, "&bLevel Cap Breakthrough",
                List.of("&7Raise &fyour &7personal level cap +10k",
                        "&7Cap: &f" + vars.get("level_cap"),
                        "&8DMZ maxValue 150000 — soft-lock holds others at their cap"), vars),
                SlotAction.page("cap"));

        put(holder, inv, 36, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        if (ForgeBridge.isStaff(viewer)) {
            put(holder, inv, 40, tipBtn(viewer, "prestige.main.progression", Material.BREWING_STAND, "&dProgression",
                    List.of("&7Skills · TP · Race · Combat flags", "&eClick to open")),
                    SlotAction.cmd("lmdo lm open progression"));
        }
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory turnIn(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(subject);
        Holder holder = new Holder("turnin");
        Inventory inv = Bukkit.createInventory(holder, 36, invTitle(viewer, subject, "&8Prestige · Turn In"));
        holder.bind(inv);
        frame(inv, 36);

        putWallet(holder, inv, viewer, subject, "turnin", "&e&lWallet", null);

        int held = parseInt(ph.get("held"), 0);
        putTurnIn(viewer, holder, inv, 19, 1, held, ph);
        putTurnIn(viewer, holder, inv, 21, 3, held, ph);
        putTurnIn(viewer, holder, inv, 23, 5, held, ph);
        Map<String, String> allVars = Map.of(
                "held", String.valueOf(held),
                "gain", ph.getOrDefault("turnin_all_points", "0"));
        List<String> allDefaults = List.of(
                "&7Turn in all &6{held} &7held",
                "&7Gain &e{gain} &7points");
        put(holder, inv, 25, tipBtn(viewer, "prestige.turnin.all", Material.GOLD_BLOCK, "&6Turn In All",
                allDefaults, allVars, GuiBoardHelper.tips(viewer, "&7+1 bonus point per 3 turned in")),
                held > 0 ? SlotAction.act("turnin", "all", "turnin") : null);

        put(holder, inv, 27, backBtn(), SlotAction.page("main"));
        put(holder, inv, 31, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 35, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private void putTurnIn(
            Player viewer, Holder holder, Inventory inv, int slot, int amount, int held, Map<String, String> ph
    ) {
        Map<String, String> vars = Map.of(
                "amount", String.valueOf(amount),
                "gain", ph.getOrDefault("turnin_" + amount + "_points", "0"),
                "held", String.valueOf(held));
        List<String> defaults = List.of(
                "&7Spend &6{amount} &7held prestige" + (amount == 1 ? "" : "s"),
                "&7Gain &e{gain} &7points");
        List<String> extra = amount >= 3 ? List.of("&aIncludes bonus for packs of 3") : null;
        boolean ok = held >= amount;
        put(holder, inv, slot, tipBtn(viewer, "prestige.turnin.amount",
                ok ? Material.GOLD_INGOT : Material.GRAY_DYE,
                (ok ? "&eTurn In &f" : "&8Need &f") + amount,
                defaults, vars, extra),
                ok ? SlotAction.act("turnin", String.valueOf(amount), "turnin") : null);
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

    private Inventory shop(Player viewer, Player subject, int pageIndex) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(subject);
        Holder holder = new Holder(pageIndex <= 0 ? "shop" : ("shop" + (pageIndex + 1)));
        int pages = Math.max(1, parseInt(ph.get("shop_pages"), 1));
        int page = Math.max(0, Math.min(pages - 1, pageIndex));
        String shopTitle = pages > 1
                ? "&8Prestige · Skills &7(" + (page + 1) + "/" + pages + ")"
                : "&8Prestige · Skills";
        Inventory inv = Bukkit.createInventory(holder, 54,
                invTitle(viewer, subject, shopTitle));
        holder.bind(inv);
        frame(inv, 54);

        putWallet(holder, inv, viewer, subject, "shop", "&e&lWallet", null);

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
            putSkill(holder, inv, GuiPlayerPicker.CONTENT_SLOTS[placed++],
                    GuiLoreChunks.skillIcon(label), id, label, ph);
        }

        put(holder, inv, 45, backBtn(), SlotAction.page("main"));
        if (page > 0) {
            String prev = page == 1 ? "shop" : ("shop" + page);
            Map<String, String> prevVars = Map.of("page", String.valueOf(page), "pages", String.valueOf(pages));
            put(holder, inv, 48, tipBtn(viewer, "prestige.shop.prev", Material.ARROW, "&7« Prev",
                    List.of("&7Page {page}/{pages}"), prevVars),
                    SlotAction.page(prev));
        }
        put(holder, inv, 49, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        if (page + 1 < pages) {
            Map<String, String> nextVars = Map.of(
                    "page", String.valueOf(page + 2), "pages", String.valueOf(pages));
            put(holder, inv, 50, tipBtn(viewer, "prestige.shop.next", Material.ARROW, "&7Next »",
                    List.of("&7Page {page}/{pages}"), nextVars),
                    SlotAction.page("shop" + (page + 2)));
        }
        put(holder, inv, 53, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private static List<String> shopSkillIds(Map<String, String> ph) {
        List<String> out = new ArrayList<>();
        String raw = ph.getOrDefault("shop_skill_ids", "");
        if (raw == null || raw.isBlank()) {
            // Fallback natural skills if bridge older than catalog.
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

    private void putSkill(
            Holder holder, Inventory inv, int slot, Material mat, String id, String label,
            Map<String, String> ph
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
        put(holder, inv, slot, item(mat,
                GuiTooltips.name("prestige.shop.skill", "&a" + label),
                GuiTooltips.buttonLore("prestige.shop.skill", defaults, vars, null)),
                SlotAction.act("skill", id, holder.page));
    }

    private Inventory forms(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(subject);
        Holder holder = new Holder("forms");
        Inventory inv = Bukkit.createInventory(holder, 36, invTitle(viewer, subject, "&8Prestige · Effects"));
        holder.bind(inv);
        frame(inv, 36);

        putWallet(holder, inv, viewer, subject, "forms", "&e&lWallet", null);

        boolean hasMajin = "true".equalsIgnoreCase(ph.getOrDefault("majin", "false"));
        boolean hasMutant = "true".equalsIgnoreCase(ph.getOrDefault("mutant", "false"));
        String cost = ph.getOrDefault("form_cost", "5");
        Map<String, String> costVars = Map.of("cost", cost);

        if (hasMajin) {
            put(holder, inv, 20, tipBtn(viewer, "prestige.effects.majin_owned", Material.LIME_DYE,
                    "&aPermanent Majin",
                    List.of("&aOwned", "&aPermanent purchase &8· only one at a time"), costVars));
        } else {
            List<String> majinDefaults = new ArrayList<>();
            majinDefaults.add("&7Cost: &e{cost} &7points");
            if (hasMutant) {
                majinDefaults.add("&8Buying removes Mutant (no refund)");
            }
            majinDefaults.add("&aPermanent purchase &8· only one at a time");
            put(holder, inv, 20, tipBtn(viewer, "prestige.effects.majin", Material.PINK_DYE,
                    "&dBuy Permanent Majin", majinDefaults, costVars),
                    SlotAction.act("majin", "0", "forms"));
        }

        if (hasMutant) {
            put(holder, inv, 22, tipBtn(viewer, "prestige.effects.mutant_owned", Material.LIME_DYE,
                    "&aPermanent Mutant",
                    List.of("&aOwned", "&aPermanent purchase &8· only one at a time"), costVars));
        } else {
            List<String> mutantDefaults = new ArrayList<>();
            mutantDefaults.add("&7Cost: &e{cost} &7points");
            if (hasMajin) {
                mutantDefaults.add("&8Buying removes Majin (no refund)");
            }
            mutantDefaults.add("&aPermanent purchase &8· only one at a time");
            put(holder, inv, 22, tipBtn(viewer, "prestige.effects.mutant", Material.SLIME_BALL,
                    "&aBuy Permanent Mutant", mutantDefaults, costVars),
                    SlotAction.act("mutant", "0", "forms"));
        }

        if (hasMajin) {
            put(holder, inv, 24, tipBtn(viewer, "prestige.effects.unmajin", Material.BARRIER, "&cUnpurchase Majin",
                    List.of("&7Removes Majin · &cno point refund")),
                    SlotAction.act("unmajin", "0", "forms"));
        } else if (hasMutant) {
            put(holder, inv, 24, tipBtn(viewer, "prestige.effects.unmutant", Material.BARRIER, "&cUnpurchase Mutant",
                    List.of("&7Removes Mutant · &cno point refund")),
                    SlotAction.act("unmutant", "0", "forms"));
        }

        put(holder, inv, 27, backBtn(), SlotAction.page("main"));
        put(holder, inv, 31, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 35, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory cap(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(subject);
        Holder holder = new Holder("cap");
        Inventory inv = Bukkit.createInventory(holder, 36, invTitle(viewer, subject, "&8Prestige · Level Cap"));
        holder.bind(inv);
        frame(inv, 36);

        putWallet(holder, inv, viewer, subject, "cap", "&e&lWallet", null);

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
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Your level cap: &f" + levelCapFmt);
        lore.add("&7Breakthroughs: &f" + bt + "&7/&f" + max);
        lore.add("&8DMZ maxValue 150000 — soft-lock holds others at their cap");
        if (canBuy) {
            lore.addAll(GuiTooltips.lore("prestige.cap.buy",
                    List.of("&7Next: &a+10,000 &7personal cap for &e{cost} &7points"),
                    capVars));
            lore.add("&8Then keep leveling with TP into the new cap");
            lore.add("&8Future prestige Need scales up to your new cap");
            lore.add("&8Costs: 15 → 20 → 25 → 30 → 35");
        } else {
            lore.add("&aMax personal cap (150000)");
        }
        put(holder, inv, 22, item(
                canBuy ? Material.NETHER_STAR : Material.BEACON,
                canBuy ? GuiTooltips.name("prestige.cap.buy", "&bBuy Breakthrough") : "&aCap Maxed",
                lore),
                canBuy ? SlotAction.act("breakthrough", "0", "cap") : null);

        put(holder, inv, 27, backBtn(), SlotAction.page("main"));
        put(holder, inv, 31, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 35, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory tiers(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(subject);
        Holder holder = new Holder("tiers");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Prestige · Difficulty Tiers"));
        holder.bind(inv);
        frame(inv, 45);

        putWallet(holder, inv, viewer, subject, "tiers", "&e&lWallet", null);

        Material[] mats = {
                Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT,
                Material.EMERALD, Material.DIAMOND, Material.NETHERITE_INGOT, Material.NETHER_STAR
        };
        // Centered row for T1–T7 across content slots.
        int[] slots = {19, 20, 21, 22, 23, 24, 25};
        for (int t = 1; t <= 7; t++) {
            boolean owned = "true".equalsIgnoreCase(ph.getOrDefault("tier_" + t + "_owned", "false"));
            boolean unlocked = "true".equalsIgnoreCase(ph.getOrDefault("tier_" + t + "_unlocked", "false"));
            boolean canBuy = "true".equalsIgnoreCase(ph.getOrDefault("tier_" + t + "_can_buy", "false"));
            String label = ph.getOrDefault("tier_" + t + "_label", "T" + t);
            String cost = ph.getOrDefault("tier_" + t + "_cost", String.valueOf((t + 1) / 2));
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
                                "&8Then activate with Ancient Coins via /difficulty → Buy Tier"),
                        tierVars));
            } else {
                lore.add("&8Then activate with Ancient Coins via /difficulty → Buy Tier");
            }
            String title;
            if (owned) {
                title = "&aT" + t + " " + label;
            } else if (canBuy) {
                title = "&6T" + t + " " + label;
            } else {
                title = "&8T" + t + " " + label;
            }
            put(holder, inv, slots[t - 1], tipBtn(viewer,
                    canBuy || owned ? mats[t - 1] : Material.GRAY_DYE, title, lore),
                    canBuy ? SlotAction.act("tier", String.valueOf(t), "tiers") : null);
        }

        put(holder, inv, 36, backBtn(), SlotAction.page("main"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    /**
     * Stats/points display as a wallet (slot 4 center) — not a thematic top-right icon.
     */
    private void putWallet(
            Holder holder, Inventory inv, Player viewer, Player subject,
            String page, String title, List<String> overrideLore
    ) {
        if (overrideLore != null && !overrideLore.isEmpty()) {
            List<String> lore = new ArrayList<>(overrideLore);
            if (lore.isEmpty() || !lore.get(0).isBlank()) {
                lore.add(0, "");
            }
            put(holder, inv, 4, item(Material.GOLD_INGOT, title, lore));
            return;
        }
        List<String> lore = prependBlank(toAmp(ForgeBridge.prestigeLines(subject, page == null ? "main" : page)));
        // Emphasize wallet balance at the top of the status block.
        Map<String, String> ph = ForgeBridge.prestigePlaceholders(subject);
        Map<String, String> vars = Map.of("points", ph.getOrDefault("points", "0"));
        List<String> walletLine = GuiTooltips.lore("prestige.main.wallet",
                List.of("&6Wallet: &e{points} &7prestige points"), vars);
        lore.add(1, walletLine.isEmpty() ? "" : walletLine.get(0));
        put(holder, inv, 4, item(Material.GOLD_INGOT,
                GuiTooltips.name("prestige.main.wallet", title), lore));
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

    private static List<String> prependBlank(List<String> tip) {
        List<String> out = new ArrayList<>();
        out.add("");
        out.addAll(tip);
        return out;
    }

    private static void frame(Inventory inv, int size) {
        for (int i = 0; i < size; i++) {
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            inv.setItem(i, item(edge ? ACCENT : FILL, " ", List.of()));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        SlotAction slotAction = holder.actionAt(event.getSlot());
        if (slotAction == null) {
            return;
        }
        if (slotAction.shouldClose) {
            player.closeInventory();
            return;
        }
        if (slotAction.page != null) {
            final String targetPage = slotAction.page;
            Bukkit.getScheduler().runTask(plugin, () -> open(player, targetPage));
            return;
        }
        if (slotAction.rawCommand != null && !slotAction.rawCommand.isBlank()) {
            final String cmd = slotAction.rawCommand;
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                player.performCommand(cmd);
            });
            return;
        }
        if (slotAction.action == null || slotAction.action.isBlank()) {
            return;
        }
        final String ret = slotAction.returnPage == null || slotAction.returnPage.isBlank()
                ? "main" : slotAction.returnPage;
        final String action = slotAction.action;
        final String arg = slotAction.arg == null || slotAction.arg.isBlank() ? "0" : slotAction.arg;
        final Player subject = AdminInspectSessions.resolveSubject(player);
        Bukkit.getScheduler().runTask(plugin, () -> {
            String msg = ForgeBridge.prestigeHandleDo(subject, action, arg, ret);
            if (msg != null && !msg.isBlank()) {
                GuiChat.sendResult(player, msg);
            }
            open(player, ret);
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private static void put(Holder holder, Inventory inv, int slot, ItemStack stack) {
        put(holder, inv, slot, stack, null);
    }

    private static void put(Holder holder, Inventory inv, int slot, ItemStack stack, SlotAction action) {
        inv.setItem(slot, stack);
        if (holder != null && action != null) {
            holder.bindAction(slot, action);
        }
    }

    private static ItemStack tipBtn(Player player, Material mat, String name, List<String> tip) {
        return tipBtn(player, null, mat, name, tip, null);
    }

    private static ItemStack tipBtn(
            Player player, String key, Material mat, String name, List<String> tip
    ) {
        return tipBtn(player, key, mat, name, tip, null);
    }

    private static ItemStack tipBtn(
            Player player, String key, Material mat, String name, List<String> tip,
            Map<String, String> vars
    ) {
        return tipBtn(player, key, mat, name, tip, vars, null);
    }

    private static ItemStack tipBtn(
            Player player, String key, Material mat, String name, List<String> tip,
            Map<String, String> vars, List<String> extra
    ) {
        if (key == null || key.isBlank()) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            if (tip != null) {
                lore.addAll(tip);
            }
            if (extra != null) {
                lore.addAll(extra);
            }
            return item(mat, name, lore);
        }
        return item(mat, GuiTooltips.name(key, name), GuiTooltips.buttonLore(key, tip, vars, extra));
    }

    private static ItemStack hubBtn() {
        return item(Material.COMPASS, "&7« Hub", List.of());
    }

    private static ItemStack backBtn() {
        return item(Material.ARROW, "&7« Prestige", List.of());
    }

    private static ItemStack closeBtn() {
        return item(Material.BARRIER, "&cClose", List.of());
    }

    private static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) {
            colored.add(color(line));
        }
        meta.setLore(colored);
        stack.setItemMeta(meta);
        return stack;
    }

    private static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }

    private static final class SlotAction {
        final String action;
        final String arg;
        final String returnPage;
        final String page;
        final String rawCommand;
        final boolean shouldClose;

        private SlotAction(
                String action, String arg, String returnPage, String page, String rawCommand, boolean shouldClose) {
            this.action = action;
            this.arg = arg;
            this.returnPage = returnPage;
            this.page = page;
            this.rawCommand = rawCommand;
            this.shouldClose = shouldClose;
        }

        static SlotAction act(String action, String arg, String returnPage) {
            return new SlotAction(action, arg, returnPage, null, null, false);
        }

        static SlotAction page(String page) {
            return new SlotAction(null, null, null, page, null, false);
        }

        static SlotAction cmd(String command) {
            return new SlotAction(null, null, null, null, command, false);
        }

        static SlotAction dismiss() {
            return new SlotAction(null, null, null, null, null, true);
        }
    }

    static final class Holder implements InventoryHolder {
        final String page;
        final Map<Integer, SlotAction> actions = new HashMap<>();
        Inventory inventory;

        Holder(String page) {
            this.page = page;
        }

        void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        void bindAction(int slot, SlotAction action) {
            if (action != null) {
                actions.put(slot, action);
            }
        }

        SlotAction actionAt(int slot) {
            return actions.get(slot);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
