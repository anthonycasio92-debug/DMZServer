package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Prestige system - Home / Prestige (turn-in) / Shop.
 *
 * <p>Shop is a hub with three favor catalogs: Skills, permanent Difficulty tiers, and Effects
 * (Majin / Mutant).
 */
public final class UltraPrestigeScreens {
    private static final int SKILLS_PER_PAGE = 6;

    private UltraPrestigeScreens() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String p = raw.toLowerCase(Locale.ROOT);
        int skillsPage = 0;
        if (p.startsWith("skills:") || p.startsWith("shop:skills:")) {
            String num = p.startsWith("skills:")
                    ? p.substring("skills:".length())
                    : p.substring("shop:skills:".length());
            try {
                skillsPage = Integer.parseInt(num.trim());
            } catch (NumberFormatException ignored) {
                skillsPage = 0;
            }
            openSkillsShop(player, Math.max(0, skillsPage));
            return;
        }
        // Legacy "shop:N" from older builds meant skill page N.
        if (p.startsWith("shop:")) {
            String rest = p.substring("shop:".length()).trim();
            if (rest.chars().allMatch(Character::isDigit)) {
                try {
                    skillsPage = Integer.parseInt(rest);
                } catch (NumberFormatException ignored) {
                    skillsPage = 0;
                }
                openSkillsShop(player, Math.max(0, skillsPage));
                return;
            }
        }
        switch (p) {
            case "turnin", "points", "prestige" -> openTurnIn(player);
            case "shop", "shop_hub", "catalog" -> openShopHub(player);
            case "skills", "skill" -> openSkillsShop(player, 0);
            case "tiers", "tier", "difficulty" -> openTiers(player);
            case "effects", "effect", "forms", "form" -> openEffects(player);
            default -> openHome(player);
        }
    }

    private static UltraScreenSpec base(String page, String tab) {
        return UltraScreenSpec.of("prestige")
                .title("Prestige")
                .page(page)
                .activeTab(tab)
                .tabs("home|Home", "prestige|Prestige", "shop|Shop");
    }

    private static void paintWallet(UltraScreenSpec spec, Map<String, String> ph) {
        // Exact wording required for these three labels.
        spec.labelValue("Lifetime prestiges", ph.getOrDefault("completed", "0"));
        spec.labelValue(
                "Current prestige amount",
                ph.getOrDefault("held", "0") + "/" + ph.getOrDefault("held_max", "10"));
        spec.labelValue("Prestige favor", ph.getOrDefault("points", "0"));
    }

    private static void openHome(ServerPlayer player) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        UltraScreenSpec spec = base("main", "home");
        paintWallet(spec, ph);
        if (!"true".equalsIgnoreCase(ph.get("system_enabled"))) {
            spec.line(UltraStyle.danger("Prestige system is disabled."));
        } else {
            spec.line(UltraStyle.LABEL + "Prestige raises lifetime count and resets stats.")
                    .line(UltraStyle.LABEL + "Exchange current prestige for favor on the Prestige tab.")
                    .labelValue("Need", ph.getOrDefault("required_fmt", "0"));
            if ("true".equalsIgnoreCase(ph.get("ready"))) {
                spec.line(UltraStyle.confirm("You can prestige now."));
            } else {
                spec.line(UltraStyle.danger("You need a higher level first."));
            }
        }
        spec.line(UltraStyle.LABEL + "Shop spends favor on Skills, Difficulty tiers, and Effects.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openTurnIn(ServerPlayer player) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        int held = parseInt(ph.get("held"), 0);
        UltraScreenSpec spec = base("turnin", "prestige");
        paintWallet(spec, ph);
        spec.notice("Turning in current prestige removes its stat bonus. This cannot be undone.")
                .line(UltraStyle.danger(
                        "Exchanging prestige for favor removes the stat bonus from the exchanged prestiges."))
                .line(UltraStyle.LABEL
                        + "Bulk bonus: exchange 3 for 4 favor, 6 for 9, or 9 for 15")
                .setRow(140);
        spec.button("do:confirm", UltraStyle.danger("Prestige now"), UltraStyle.STYLE_DANGER).rowGap();
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
        spec.footerBackHome();
        spec.send(player);
    }

    /** Shop tab hub - three favor catalogs. */
    private static void openShopHub(ServerPlayer player) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        UltraScreenSpec spec = base("shop", "shop");
        paintWallet(spec, ph);
        spec.line(UltraStyle.LABEL + "Spend prestige favor on permanent unlocks.")
                .line(UltraStyle.LABEL + "Purchases survive prestige.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        spec.button("page:skills", UltraStyle.confirm("Skill shop"), UltraStyle.STYLE_CONFIRM)
                .rowGap();
        spec.button(
                        "page:tiers",
                        UltraStyle.confirm("Difficulty tiers"),
                        UltraStyle.STYLE_CONFIRM)
                .rowGap();
        spec.button("page:effects", UltraStyle.confirm("Effects"), UltraStyle.STYLE_CONFIRM)
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openSkillsShop(ServerPlayer player, int pageIndex) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        String[] ids = shopSkillIds(ph);
        int pageSize = SKILLS_PER_PAGE;
        int pages = Math.max(1, (ids.length + pageSize - 1) / pageSize);
        int page = Math.min(pages - 1, Math.max(0, pageIndex));
        UltraScreenSpec spec = base(page > 0 ? "skills:" + page : "skills", "shop");
        paintWallet(spec, ph);
        spec.line(UltraStyle.LABEL + "Permanent skill levels - 1 favor each (Potential Unlock +2)")
                .labelValue("Skills page", (page + 1) + "/" + pages)
                .setRow(120);
        int from = page * pageSize;
        int placed = 0;
        for (int i = from; i < ids.length && placed < pageSize; i++, placed++) {
            String id = ids[i];
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
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        if (page > 0) {
            spec.buttonLeft(
                    "page:skills:" + (page - 1), UltraStyle.dim("Prev"), UltraStyle.STYLE_DIM);
        }
        if (page + 1 < pages) {
            if (page > 0) {
                spec.buttonRight(
                        "page:skills:" + (page + 1),
                        UltraStyle.LABEL + "Next",
                        UltraStyle.STYLE_NORMAL);
            } else {
                spec.buttonLeft(
                        "page:skills:" + (page + 1),
                        UltraStyle.LABEL + "Next",
                        UltraStyle.STYLE_NORMAL);
            }
            spec.rowGap();
        } else if (page > 0) {
            spec.rowGap();
        }
        spec.button("page:shop", UltraStyle.dim("Shop catalog"), UltraStyle.STYLE_DIM).rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openTiers(ServerPlayer player) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        UltraScreenSpec spec = base("tiers", "shop");
        paintWallet(spec, ph);
        spec.line(UltraStyle.LABEL + "Permanent difficulty tiers - survive prestige")
                .line(UltraStyle.LABEL + "Buy T1 first, then T2-T7 in order. Still activate with coins.")
                .labelValue("Highest owned", "T" + ph.getOrDefault("tier_highest_purchased", "0"))
                .setRow(120);
        for (int t = 1; t <= 7; t++) {
            String label = ph.getOrDefault("tier_" + t + "_label", "T" + t);
            String cost = ph.getOrDefault("tier_" + t + "_cost", "?");
            boolean owned = "true".equalsIgnoreCase(ph.get("tier_" + t + "_owned"));
            boolean canBuy = "true".equalsIgnoreCase(ph.get("tier_" + t + "_can_buy"));
            String caption;
            String style;
            String action = "do:tier:" + t;
            if (owned) {
                caption = "T" + t + " " + label + " - owned";
                style = UltraStyle.STYLE_DIM;
                spec.button(action, UltraStyle.dim(caption), style).rowGap();
            } else if (canBuy) {
                caption = "T" + t + " " + label + " - " + cost + " favor";
                style = UltraStyle.STYLE_CONFIRM;
                spec.button(action, UltraStyle.confirm(caption), style).rowGap();
            } else {
                caption = "T" + t + " " + label + " - " + cost + " favor (locked)";
                style = UltraStyle.STYLE_DIM;
                spec.button(action, UltraStyle.dim(caption), style).rowGap();
            }
        }
        spec.button("page:shop", UltraStyle.dim("Shop catalog"), UltraStyle.STYLE_DIM).rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openEffects(ServerPlayer player) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        String formCost = ph.getOrDefault("form_cost", String.valueOf(PrestigePointsSystem.FORM_COST));
        boolean majin = "true".equalsIgnoreCase(ph.get("majin"));
        boolean mutant = "true".equalsIgnoreCase(ph.get("mutant"));
        boolean majinCan = "true".equalsIgnoreCase(ph.get("majin_can_buy"));
        boolean mutantCan = "true".equalsIgnoreCase(ph.get("mutant_can_buy"));
        UltraScreenSpec spec = base("effects", "shop");
        paintWallet(spec, ph);
        spec.line(UltraStyle.LABEL + "Permanent Majin / Mutant - " + formCost + " favor each")
                .line(UltraStyle.LABEL + "Only one special form at a time. Remove before switching.")
                .labelValue("Majin", majin ? "Owned" : "Not owned")
                .labelValue("Mutant", mutant ? "Owned" : "Not owned")
                .setRow(140);
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
        spec.button("page:shop", UltraStyle.dim("Shop catalog"), UltraStyle.STYLE_DIM).rowGap();
        spec.footerBackHome();
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
