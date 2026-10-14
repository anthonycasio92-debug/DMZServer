package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeUniverseRoleWarn;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Prestige Ultra UI - Home / Prestige / Shop.
 *
 * <p>Prestige tab: prestige now (GUI confirm) + favor wording. Exchange shop lives under Shop.
 * Shop catalogs show Prestige favor balance only (no full wallet dump).
 */
public final class UltraPrestigeScreens {
    private UltraPrestigeScreens() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String p = raw.toLowerCase(Locale.ROOT);
        // Legacy skills_pN deep-links still open the single skills page.
        if (parseSkillsPage(p) != null) {
            openSkillsShop(player);
            return;
        }
        switch (p) {
            case "turnin", "points", "prestige" -> openTurnIn(player);
            case "confirm", "prestige_confirm" -> openConfirm(player);
            case "shop", "shop_hub", "catalog" -> openShopHub(player);
            case "exchange", "turnin_shop", "favor_shop" -> openExchange(player);
            case "skills", "skill", "skills_p0" -> openSkillsShop(player);
            case "tiers", "tier", "difficulty" -> openTiers(player);
            case "effects", "effect", "forms", "form" -> openEffects(player);
            default -> openHome(player);
        }
    }

    /**
     * Legacy skill-shop page tokens ({@code skills_pN}, {@code skills:N}) - all map to one page.
     */
    private static Integer parseSkillsPage(String p) {
        if (p == null || p.isBlank()) {
            return null;
        }
        if (p.startsWith("skills_p")) {
            return parseNonNegInt(p.substring("skills_p".length()));
        }
        if (p.startsWith("skills:")) {
            return parseNonNegInt(p.substring("skills:".length()));
        }
        if (p.startsWith("shop:skills:") || p.startsWith("shop:skills_p")) {
            return 0;
        }
        if (p.startsWith("shop:")) {
            String rest = p.substring("shop:".length()).trim();
            if (!rest.isEmpty() && rest.chars().allMatch(Character::isDigit)) {
                return parseNonNegInt(rest);
            }
        }
        return null;
    }

    private static Integer parseNonNegInt(String raw) {
        try {
            int n = Integer.parseInt(raw == null ? "" : raw.trim());
            return Math.max(0, n);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static UltraScreenSpec base(String page, String tab) {
        return UltraScreenSpec.of("prestige")
                .title("Prestige")
                .page(page)
                .activeTab(tab)
                .tabs("home|Home", "prestige|Prestige", "shop|Shop");
    }

    /** Subpage: no tab row - dual footer (Back + Main menu). */
    private static UltraScreenSpec sub(String page) {
        return UltraScreenSpec.of("prestige")
                .title("Prestige")
                .page(page);
    }

    /** Full wallet for Home / Prestige tabs. */
    private static void paintWallet(UltraScreenSpec spec, Map<String, String> ph) {
        spec.labelValue("Lifetime prestiges", ph.getOrDefault("completed", "0"));
        spec.labelValue(
                "Current prestige amount",
                ph.getOrDefault("held", "0") + "/" + ph.getOrDefault("held_max", "10"));
        spec.labelValue("Prestige favor", ph.getOrDefault("points", "0"));
    }

    /** Shop pages: favor balance only (no player info dump). */
    private static void paintFavorBalance(UltraScreenSpec spec, Map<String, String> ph) {
        spec.labelValue("Prestige favor", ph.getOrDefault("points", "0"));
    }

    private static void openHome(ServerPlayer player) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        UltraScreenSpec spec = base("main", "home")
                .lineGap(2)
                .bodyPad(4);
        paintWallet(spec, ph);
        if (!"true".equalsIgnoreCase(ph.get("system_enabled"))) {
            spec.line(UltraStyle.danger("Prestige system is disabled."));
        } else {
            spec.labelValue("Need", ph.getOrDefault("required_fmt", "0"));
            if ("true".equalsIgnoreCase(ph.get("ready"))) {
                spec.line(UltraStyle.confirm("Ready to prestige."));
            } else {
                spec.line(UltraStyle.dim("Need a higher level first."));
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + 90);
        spec.footerBackToMain();
        spec.send(player);
    }

    /** Prestige tab: status + Prestige now (confirm is a GUI subpage). Exchange is under Shop. */
    private static void openTurnIn(ServerPlayer player) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        UltraScreenSpec spec = base("turnin", "prestige")
                .lineGap(2)
                .bodyPad(4);
        paintWallet(spec, ph);
        spec.line(UltraStyle.LABEL
                + "Prestige favor - exchange current prestige on the Shop tab (loses the stat bonus).");
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + 100);
        boolean ready = "true".equalsIgnoreCase(ph.get("ready"));
        if (ready) {
            spec.button("page:confirm", UltraStyle.danger("Prestige now"), UltraStyle.STYLE_DANGER)
                    .rowGap();
        } else {
            spec.button(
                            "page:confirm",
                            UltraStyle.dim("Prestige now - need higher level"),
                            UltraStyle.STYLE_DIM)
                    .rowGap();
        }
        spec.footerBackToMain();
        spec.send(player);
    }

    /**
     * GUI confirm subpage - role warnings here (not chat). Confirm runs {@code do:confirm_now}.
     */
    private static void openConfirm(ServerPlayer player) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        UltraScreenSpec spec = sub("confirm")
                .lineGap(2)
                .bodyPad(4)
                .line(UltraStyle.LABEL + "Confirm prestige")
                .labelValue("Current prestige amount",
                        ph.getOrDefault("held", "0") + "/" + ph.getOrDefault("held_max", "10"))
                .labelValue("Need", ph.getOrDefault("required_fmt", "0"))
                .notice("This resets your stats and adds one to your current prestige amount.");
        if (PrestigeUniverseRoleWarn.isGodOfDestruction(player)) {
            spec.line(UltraStyle.danger(PrestigeUniverseRoleWarn.GOD_OF_DESTRUCTION));
        }
        if (PrestigeUniverseRoleWarn.isSupremeKai(player)) {
            spec.line(UltraStyle.danger(PrestigeUniverseRoleWarn.SUPREME_KAI));
        }
        spec.setRow(UltraStyle.CONTENT_ROW + 110);
        boolean ready = "true".equalsIgnoreCase(ph.get("ready"));
        if (ready) {
            spec.button(
                            "do:confirm_now",
                            UltraStyle.danger("Confirm prestige"),
                            UltraStyle.STYLE_DANGER)
                    .rowGap();
        } else {
            spec.line(UltraStyle.danger("You need a higher level before you can prestige."));
        }
        spec.footerBack("page:turnin", "Prestige");
        spec.send(player);
    }

    /** Shop hub - favor catalogs + exchange submenu. */
    private static void openShopHub(ServerPlayer player) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        UltraScreenSpec spec = base("shop", "shop")
                .lineGap(2)
                .bodyPad(4);
        paintFavorBalance(spec, ph);
        spec.line(UltraStyle.LABEL + "Spend Prestige favor. Purchases survive prestige.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 100);
        spec.button("page:exchange", UltraStyle.confirm("Exchange shop"), UltraStyle.STYLE_CONFIRM)
                .rowGap();
        spec.button("page:skills", UltraStyle.confirm("Skill shop"), UltraStyle.STYLE_CONFIRM)
                .rowGap();
        spec.button(
                        "page:tiers",
                        UltraStyle.confirm("Difficulty tiers"),
                        UltraStyle.STYLE_CONFIRM)
                .rowGap();
        spec.button("page:effects", UltraStyle.confirm("Effects"), UltraStyle.STYLE_CONFIRM)
                .rowGap();
        spec.footerBackToMain();
        spec.send(player);
    }

    /** Exchange current prestige for favor (was on Prestige tab). */
    private static void openExchange(ServerPlayer player) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        int held = parseInt(ph.get("held"), 0);
        UltraScreenSpec spec = sub("exchange")
                .lineGap(2)
                .bodyPad(4);
        paintFavorBalance(spec, ph);
        spec.labelValue(
                "Current prestige amount",
                ph.getOrDefault("held", "0") + "/" + ph.getOrDefault("held_max", "10"));
        spec.line(UltraStyle.danger(
                        "Exchanging prestige for Prestige favor removes the stat bonus."))
                .line(UltraStyle.LABEL + "Bulk: 3 for 4 favor, 6 for 9, or 9 for 15.")
                .setRow(UltraStyle.CONTENT_ROW + 100);
        int[] amounts = PrestigePointsSystem.TURN_IN_AMOUNTS;
        for (int i = 0; i < amounts.length; i += 2) {
            int a = amounts[i];
            String gainA = ph.getOrDefault("turnin_" + a + "_points", "0");
            String labelA = held >= a
                    ? UltraStyle.confirm("Exchange " + a + " (+" + gainA + " favor)")
                    : UltraStyle.dim("Exchange " + a + " - need " + a);
            spec.buttonLeft("do:turnin:" + a, labelA, held >= a ? "confirm" : "dim");
            if (i + 1 < amounts.length) {
                int b = amounts[i + 1];
                String gainB = ph.getOrDefault("turnin_" + b + "_points", "0");
                String labelB = held >= b
                        ? UltraStyle.confirm("Exchange " + b + " (+" + gainB + " favor)")
                        : UltraStyle.dim("Exchange " + b + " - need " + b);
                spec.buttonRight("do:turnin:" + b, labelB, held >= b ? "confirm" : "dim");
            }
            spec.rowGap();
        }
        spec.footerBack("page:shop", "Shop");
        spec.send(player);
    }

    /** All skills on one page. */
    private static void openSkillsShop(ServerPlayer player) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        String[] ids = shopSkillIds(ph);
        UltraScreenSpec spec = sub("skills")
                .lineGap(2)
                .bodyPad(4);
        paintFavorBalance(spec, ph);
        spec.line(UltraStyle.LABEL + "Skill levels - 1 favor each (Potential Unlock +2)")
                .setRow(UltraStyle.CONTENT_ROW + 70);
        int placed = 0;
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            String label = ph.getOrDefault("skill_" + id + "_label", id);
            int bought = parseInt(ph.get("skill_" + id), 0);
            int max = parseInt(ph.get("skill_" + id + "_max"), 0);
            String cost = ph.getOrDefault("skill_" + id + "_cost", "1");
            String caption = label + " " + bought + "/" + (max > 0 ? max : "?") + " - " + cost;
            boolean maxed = max > 0 && bought >= max;
            String action = "do:skill:" + id;
            if (placed % 2 == 0) {
                spec.buttonLeft(
                        action,
                        maxed ? UltraStyle.dim(caption + " - max") : UltraStyle.confirm(caption),
                        maxed ? "dim" : "confirm");
            } else {
                spec.buttonRight(
                        action,
                        maxed ? UltraStyle.dim(caption + " - max") : UltraStyle.confirm(caption),
                        maxed ? "dim" : "confirm");
                spec.rowGap();
            }
            placed++;
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        if (placed == 0) {
            spec.line(UltraStyle.dim("No skills for sale right now."));
        }
        spec.footerBack("page:shop", "Shop");
        spec.send(player);
    }

    private static void openTiers(ServerPlayer player) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        UltraScreenSpec spec = sub("tiers")
                .lineGap(2)
                .bodyPad(4);
        paintFavorBalance(spec, ph);
        spec.line(UltraStyle.LABEL + "Difficulty tiers - buy in order, activate with coins.")
                .labelValue("Highest owned", "T" + ph.getOrDefault("tier_highest_purchased", "0"))
                .setRow(UltraStyle.CONTENT_ROW + 70);
        for (int t = 1; t <= 7; t++) {
            String label = ph.getOrDefault("tier_" + t + "_label", "T" + t);
            String cost = ph.getOrDefault("tier_" + t + "_cost", "?");
            boolean owned = "true".equalsIgnoreCase(ph.get("tier_" + t + "_owned"));
            boolean canBuy = "true".equalsIgnoreCase(ph.get("tier_" + t + "_can_buy"));
            String action = "do:tier:" + t;
            if (owned) {
                spec.button(
                                action,
                                UltraStyle.dim("T" + t + " " + label + " - owned"),
                                UltraStyle.STYLE_DIM)
                        .rowGap();
            } else if (canBuy) {
                spec.button(
                                action,
                                UltraStyle.confirm("T" + t + " " + label + " - " + cost + " favor"),
                                UltraStyle.STYLE_CONFIRM)
                        .rowGap();
            } else {
                spec.button(
                                action,
                                UltraStyle.dim("T" + t + " " + label + " - locked"),
                                UltraStyle.STYLE_DIM)
                        .rowGap();
            }
        }
        spec.footerBack("page:shop", "Shop");
        spec.send(player);
    }

    private static void openEffects(ServerPlayer player) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        String formCost = ph.getOrDefault("form_cost", String.valueOf(PrestigePointsSystem.FORM_COST));
        boolean majin = "true".equalsIgnoreCase(ph.get("majin"));
        boolean mutant = "true".equalsIgnoreCase(ph.get("mutant"));
        boolean majinCan = "true".equalsIgnoreCase(ph.get("majin_can_buy"));
        boolean mutantCan = "true".equalsIgnoreCase(ph.get("mutant_can_buy"));
        UltraScreenSpec spec = sub("effects")
                .lineGap(2)
                .bodyPad(4);
        paintFavorBalance(spec, ph);
        spec.line(UltraStyle.LABEL + "Majin / Mutant - " + formCost + " favor. One at a time.")
                .setRow(UltraStyle.CONTENT_ROW + 80);
        if (majin) {
            spec.button(
                            "do:unmajin",
                            UltraStyle.danger("Remove Majin (+" + formCost + " favor)"),
                            UltraStyle.STYLE_DANGER)
                    .rowGap();
        } else if (majinCan) {
            spec.button(
                            "do:majin",
                            UltraStyle.confirm("Buy Majin (" + formCost + " favor)"),
                            UltraStyle.STYLE_CONFIRM)
                    .rowGap();
        } else {
            spec.button(
                            "do:majin",
                            UltraStyle.dim("Buy Majin - remove Mutant first"),
                            UltraStyle.STYLE_DIM)
                    .rowGap();
        }
        if (mutant) {
            spec.button(
                            "do:unmutant",
                            UltraStyle.danger("Remove Mutant (+" + formCost + " favor)"),
                            UltraStyle.STYLE_DANGER)
                    .rowGap();
        } else if (mutantCan) {
            spec.button(
                            "do:mutant",
                            UltraStyle.confirm("Buy Mutant (" + formCost + " favor)"),
                            UltraStyle.STYLE_CONFIRM)
                    .rowGap();
        } else {
            spec.button(
                            "do:mutant",
                            UltraStyle.dim("Buy Mutant - remove Majin first"),
                            UltraStyle.STYLE_DIM)
                    .rowGap();
        }
        spec.footerBack("page:shop", "Shop");
        spec.send(player);
    }

    private static String[] shopSkillIds(Map<String, String> ph) {
        String raw = ph.getOrDefault("shop_skill_ids", "");
        if (raw == null || raw.isBlank()) {
            return new String[] {"meditation", "fly", "sprint", "jump", "potentialunlock"};
        }
        String[] parts = raw.split(",");
        int n = 0;
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                n++;
            }
        }
        String[] out = new String[n];
        int i = 0;
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                out[i++] = part.trim().toLowerCase(Locale.ROOT);
            }
        }
        return out;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
