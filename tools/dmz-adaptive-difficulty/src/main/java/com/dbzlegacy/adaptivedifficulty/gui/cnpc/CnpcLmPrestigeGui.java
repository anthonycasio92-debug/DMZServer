package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

public final class CnpcLmPrestigeGui {
    private CnpcLmPrestigeGui() {}

    public static void open(ServerPlayer player, String page) {
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
        } else if (p.startsWith("shop") && p.length() > 4 && Character.isDigit(p.charAt(4))) {
            try {
                shopPage = Integer.parseInt(p.substring(4)) - 1;
            } catch (NumberFormatException ignored) {
                shopPage = 0;
            }
            p = "shop";
        }
        final int shopPageFinal = Math.max(0, shopPage);
        final String pageKey = p;
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_PRESTIGE, CnpcGuiSupport.W, 320, (pl, gui) -> {
            switch (pageKey) {
                case "turnin", "points" -> paintTurnIn(pl, gui);
                case "shop", "skills" -> paintShop(pl, gui, shopPageFinal);
                case "forms", "effects", "effect" -> paintEffects(pl, gui);
                case "cap", "breakthrough" -> paintMain(pl, gui);
                case "tiers", "tier" -> paintTiers(pl, gui);
                default -> paintMain(pl, gui);
            }
        });
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§dPrestige", "§7Turn-ins · shop · permanent unlocks");
        List<String> lines = ProgressionGuiApi.prestigeLines(player, "main");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 4);
        CnpcGuiSupport.button(gui, 20, "§aPrestige now", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> ProgressionGuiApi.handlePrestigeDo(player, "confirm", "", "main"),
                () -> open(player, "main")));
        CnpcGuiSupport.button(gui, 21, "§eTurn in held", CnpcGuiSupport.COL_R, row, () -> open(player, "turnin"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§bSkill shop", CnpcGuiSupport.COL_L, row, () -> open(player, "shop"));
        CnpcGuiSupport.button(gui, 23, "§5Effects", CnpcGuiSupport.COL_R, row, () -> open(player, "effects"));
        row += 24;
        CnpcGuiSupport.button(gui, 24, "§6Difficulty tiers", CnpcGuiSupport.COL_L, row, () -> open(player, "tiers"));
        row += 24;
        footer(player, gui, row, null);
    }

    private static void paintTurnIn(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§eTurn in prestiges");
        CnpcGuiSupport.bodyLines(gui, 10, 44, ProgressionGuiApi.prestigeLines(player, "turnin"), 5);
        int row = 100;
        for (int n : PrestigePointsSystem.TURN_IN_AMOUNTS) {
            int amount = n;
            int col = (amount == 1 || amount == 3 || amount == 9) ? CnpcGuiSupport.COL_L : CnpcGuiSupport.COL_R;
            if (amount == 2 || amount == 6) {
                row += 24;
            }
            CnpcGuiSupport.buttonSmall(gui, 30 + amount, "§f×" + amount, col, row, 95, () -> CnpcGuiSupport.act(
                    player,
                    () -> ProgressionGuiApi.handlePrestigeDo(player, "turnin", String.valueOf(amount), "turnin"),
                    () -> open(player, "turnin")));
        }
        row += 36;
        footer(player, gui, row, "main");
    }

    private static void paintShop(ServerPlayer player, ICustomGui gui, int pageIndex) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        int pages = Math.max(1, parseInt(ph.get("shop_pages"), 1));
        int page = Math.min(pages - 1, Math.max(0, pageIndex));
        int pageSize = Math.max(1, parseInt(ph.get("shop_page_size"), 6));
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§bPrestige skill shop",
                "§7Page §f" + (page + 1) + "/" + pages + "  §8·  §ePoints §f" + ph.getOrDefault("points", "0"));

        List<String> ids = shopSkillIds(ph);
        int from = page * pageSize;
        int row = infoY + 4;
        int placed = 0;
        for (int i = from; i < ids.size() && placed < pageSize; i++, placed++) {
            String id = ids.get(i);
            String label = CnpcGuiSupport.humanizeSkillLabel(id, ph.getOrDefault("skill_" + id + "_label", id));
            String bought = ph.getOrDefault("skill_" + id, "0");
            String max = ph.getOrDefault("skill_" + id + "_max", "?");
            int col = (placed % 2 == 0) ? CnpcGuiSupport.COL_L : CnpcGuiSupport.COL_R;
            if (placed > 0 && placed % 2 == 0) {
                row += 24;
            }
            int boughtN = parseInt(bought, 0);
            int maxN = parseInt(max, 0);
            String caption = label + " §7(" + bought + "/" + max + ")";
            if (maxN > 0 && boughtN >= maxN) {
                gui.addLabel(30 + placed, "§a" + caption + " §8· maxed", col, row + 4, 195, 14);
            } else {
                String skillId = id;
                CnpcGuiSupport.buttonSmall(gui, 30 + placed, caption, col, row, 195,
                        () -> CnpcGuiSupport.act(
                                player,
                                () -> ProgressionGuiApi.handlePrestigeDo(player, "skill", skillId, "shop"),
                                () -> open(player, "shop:" + page)));
            }
        }
        row += 36;
        if (page > 0) {
            int prev = page - 1;
            CnpcGuiSupport.buttonSmall(gui, 90, "§7« Prev", CnpcGuiSupport.COL_L, row, 95,
                    () -> open(player, "shop:" + prev));
        }
        if (page + 1 < pages) {
            CnpcGuiSupport.buttonSmall(gui, 91, "§7Next »", CnpcGuiSupport.COL_R, row, 95,
                    () -> open(player, "shop:" + (page + 1)));
        }
        row += 28;
        footer(player, gui, row, "main");
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

    private static int parseInt(String s, int def) {
        if (s == null || s.isBlank()) {
            return def;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static void paintEffects(ServerPlayer player, ICustomGui gui) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        boolean hasMajin = "true".equalsIgnoreCase(ph.get("majin"));
        boolean hasMutant = "true".equalsIgnoreCase(ph.get("mutant"));
        boolean canBuyMajin = "true".equalsIgnoreCase(ph.get("majin_can_buy"));
        boolean canBuyMutant = "true".equalsIgnoreCase(ph.get("mutant_can_buy"));
        String cost = ph.getOrDefault("form_cost", "5");

        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§5Permanent effects",
                "§7Majin & Mutant · §e" + cost + " §7points · one at a time");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, ProgressionGuiApi.prestigeLines(player, "effects"), 5);

        if (hasMajin) {
            gui.addLabel(40, "§aPermanent Majin §8· owned", CnpcGuiSupport.COL_L, row + 4, 195, 14);
        } else if (canBuyMajin) {
            CnpcGuiSupport.button(gui, 40, "§dBuy Majin", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> ProgressionGuiApi.handlePrestigeDo(player, "majin", "", "effects"),
                    () -> open(player, "effects")));
        } else {
            gui.addLabel(40, "§8Buy Majin §7(unpurchase Mutant first)", CnpcGuiSupport.COL_L, row + 4, 195, 14);
        }

        if (hasMutant) {
            gui.addLabel(41, "§aPermanent Mutant §8· owned", CnpcGuiSupport.COL_R, row + 4, 195, 14);
        } else if (canBuyMutant) {
            CnpcGuiSupport.button(gui, 41, "§dBuy Mutant", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> ProgressionGuiApi.handlePrestigeDo(player, "mutant", "", "effects"),
                    () -> open(player, "effects")));
        } else {
            gui.addLabel(41, "§8Buy Mutant §7(unpurchase Majin first)", CnpcGuiSupport.COL_R, row + 4, 195, 14);
        }
        row += CnpcGuiSupport.ROW_STEP;

        if (hasMajin) {
            CnpcGuiSupport.button(gui, 42, "§cUnpurchase Majin", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> ProgressionGuiApi.handlePrestigeDo(player, "unmajin", "", "effects"),
                    () -> open(player, "effects")));
        } else if (hasMutant) {
            CnpcGuiSupport.button(gui, 42, "§cUnpurchase Mutant", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> ProgressionGuiApi.handlePrestigeDo(player, "unmutant", "", "effects"),
                    () -> open(player, "effects")));
        } else {
            gui.addLabel(42, "§7Nothing to unpurchase", CnpcGuiSupport.COL_L, row + 4, 195, 14);
        }
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, "main");
    }

    private static void paintCap(ServerPlayer player, ICustomGui gui) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        int bt = parseInt(ph.get("breakthroughs"), 0);
        int btMax = parseInt(ph.get("breakthroughs_max"), PrestigePointsSystem.MAX_BREAKTHROUGHS);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§3Level cap", "§7Breakthrough purchases");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, ProgressionGuiApi.prestigeLines(player, "cap"), 5);
        if (bt >= btMax) {
            gui.addLabel(20, "§aCap fully raised §8· no more breakthroughs", CnpcGuiSupport.COL_L, row + 4, 400, 14);
        } else {
            CnpcGuiSupport.button(gui, 20, "§aBuy breakthrough §7(§e" + ph.getOrDefault("next_breakthrough_cost", "?")
                    + "§7 pts)", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> ProgressionGuiApi.handlePrestigeDo(player, "breakthrough", "", "cap"),
                    () -> open(player, "cap")));
        }
        footer(player, gui, row + 28, "main");
    }

    private static void paintTiers(ServerPlayer player, ICustomGui gui) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§6Prestige tiers", "§7Permanent unlocks · T1→T7 ladder");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, ProgressionGuiApi.prestigeLines(player, "tiers"), 4);
        for (int t = 1; t <= 7; t++) {
            int tier = t;
            int col = (t % 2 == 1) ? CnpcGuiSupport.COL_L : CnpcGuiSupport.COL_R;
            if (t > 1 && t % 2 == 1) {
                row += 24;
            }
            boolean owned = "true".equalsIgnoreCase(ph.get("tier_" + t + "_owned"));
            boolean canBuy = "true".equalsIgnoreCase(ph.get("tier_" + t + "_can_buy"));
            String label = ph.getOrDefault("tier_" + t + "_label", "T" + t);
            if (owned) {
                gui.addLabel(50 + t, "§aT" + tier + " §8· owned", col, row + 4, 95, 14);
            } else if (canBuy) {
                CnpcGuiSupport.buttonSmall(gui, 50 + t, "§fBuy T" + tier, col, row, 95, () -> CnpcGuiSupport.act(
                        player,
                        () -> ProgressionGuiApi.handlePrestigeDo(player, "tier", String.valueOf(tier), "tiers"),
                        () -> open(player, "tiers")));
            } else {
                int prev = tier - 1;
                String hint = tier > 1 ? "§8Buy T" + prev + " first" : "§8Unavailable";
                gui.addLabel(50 + t, "§7T" + tier + " " + label + " §8· " + hint, col, row + 2, 195, 12);
            }
        }
        row += 36;
        footer(player, gui, row, "main");
    }

    private static void footer(ServerPlayer player, ICustomGui gui, int row) {
        footer(player, gui, row, null);
    }

    private static void footer(ServerPlayer player, ICustomGui gui, int row, String parentPage) {
        if (parentPage == null || parentPage.isBlank()) {
            CnpcGuiSupport.navSystemRoot(player, gui, row);
        } else {
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, parentPage), "§7« Back");
        }
    }
}
