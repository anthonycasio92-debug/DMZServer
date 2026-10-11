package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/** Prestige system — Home / Prestige (turn-in) / Shop. */
public final class UltraPrestigeScreens {
    private UltraPrestigeScreens() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String p = raw.toLowerCase(Locale.ROOT);
        int shopPage = 0;
        if (p.startsWith("shop:")) {
            try {
                shopPage = Integer.parseInt(p.substring("shop:".length()).trim());
            } catch (NumberFormatException ignored) {
                shopPage = 0;
            }
            p = "shop";
        }
        switch (p) {
            case "turnin", "points", "prestige" -> openTurnIn(player);
            case "shop", "skills" -> openShop(player, Math.max(0, shopPage));
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
        spec.setRow(140);
        spec.buttonLeft("tab:prestige", UltraStyle.confirm("Prestige"), "normal")
                .buttonRight("tab:shop", "Shop", "normal")
                .rowGap();
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
                .line(UltraStyle.LABEL + "Bulk bonus: 3→4 · 6→9 · 9→15 favor")
                .setRow(140);
        spec.button("do:confirm", UltraStyle.confirm("Prestige now"), "confirm").rowGap();
        int[] amounts = PrestigePointsSystem.TURN_IN_AMOUNTS;
        for (int i = 0; i < amounts.length; i += 2) {
            int a = amounts[i];
            String gainA = ph.getOrDefault("turnin_" + a + "_points", "0");
            String labelA = held >= a
                    ? UltraStyle.confirm("Exchange " + a + " (+" + gainA + " favor)")
                    : UltraStyle.dim("Exchange " + a + " · need " + a);
            spec.buttonLeft("do:turnin:" + a, labelA, held >= a ? "confirm" : "dim");
            if (i + 1 < amounts.length) {
                int b = amounts[i + 1];
                String gainB = ph.getOrDefault("turnin_" + b + "_points", "0");
                String labelB = held >= b
                        ? UltraStyle.confirm("Exchange " + b + " (+" + gainB + " favor)")
                        : UltraStyle.dim("Exchange " + b + " · need " + b);
                spec.buttonRight("do:turnin:" + b, labelB, held >= b ? "confirm" : "dim");
            }
            spec.rowGap();
        }
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openShop(ServerPlayer player, int pageIndex) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        int pages = Math.max(1, parseInt(ph.get("shop_pages"), 1));
        int page = Math.min(pages - 1, Math.max(0, pageIndex));
        int pageSize = Math.max(1, parseInt(ph.get("shop_page_size"), 6));
        UltraScreenSpec spec = base(page > 0 ? "shop:" + page : "shop", "shop");
        paintWallet(spec, ph);
        spec.line(UltraStyle.LABEL + "Permanent skill levels · survive prestige")
                .labelValue("Shop page", (page + 1) + "/" + pages)
                .setRow(120);
        String[] ids = shopSkillIds(ph);
        int from = page * pageSize;
        int placed = 0;
        for (int i = from; i < ids.length && placed < pageSize; i++, placed++) {
            String id = ids[i];
            String label = ph.getOrDefault("skill_" + id + "_label", id);
            int bought = parseInt(ph.get("skill_" + id), 0);
            int max = parseInt(ph.get("skill_" + id + "_max"), 0);
            String cost = ph.getOrDefault("skill_" + id + "_cost", "?");
            String caption = label + " " + bought + "/" + (max > 0 ? max : "?") + " · " + cost;
            boolean maxed = max > 0 && bought >= max;
            String action = "do:skill:" + id;
            if (placed % 2 == 0) {
                spec.buttonLeft(
                        action,
                        maxed ? UltraStyle.dim(caption + " · max") : UltraStyle.confirm(caption),
                        maxed ? "dim" : "confirm");
            } else {
                spec.buttonRight(
                        action,
                        maxed ? UltraStyle.dim(caption + " · max") : UltraStyle.confirm(caption),
                        maxed ? "dim" : "confirm");
                spec.rowGap();
            }
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        if (page > 0) {
            spec.buttonLeft("open:prestige", UltraStyle.dim("« Prev shop"), "dim");
        }
        if (page + 1 < pages) {
            // Reopen next page via tab home then shop — UltraGui open with shop:N
            spec.buttonRight("tab:shop", UltraStyle.dim("Next »"), "dim");
        }
        if (page > 0 || page + 1 < pages) {
            spec.rowGap();
        }
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
