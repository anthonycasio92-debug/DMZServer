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
        CnpcUltraStyle.withAccent(CnpcUltraStyle.ACCENT_PRESTIGE, () -> openPrestige(player, page));
    }

    private static void openPrestige(ServerPlayer player, String page) {
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
        int height = CnpcGuiSupport.window(CnpcGuiSupport.TAB_BAR_H + switch (p) {
            case "turnin", "points" -> 360;
            case "shop", "skills" -> 360;
            case "tiers", "tier" -> 380;
            case "forms", "effects", "effect" -> 380;
            default -> 400;
        });
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_PRESTIGE, CnpcGuiSupport.W, height, (pl, gui) -> {
            switch (pageKey) {
                case "turnin", "points" -> paintTurnIn(pl, gui);
                case "shop", "skills" -> paintShop(pl, gui, shopPageFinal);
                case "forms", "effects", "effect" -> paintEffects(pl, gui);
                case "cap", "breakthrough" -> paintMain(pl, gui);
                case "tiers", "tier" -> paintTiers(pl, gui);
                case "modules" -> paintMain(pl, gui);
                case "home" -> paintHome(pl, gui);
                default -> paintHome(pl, gui);
            }
        });
    }

    private static int prestigeTabs(ServerPlayer player, ICustomGui gui, int y, String active) {
        return CnpcGuiSupport.paintTabBar(gui, y, new String[] {
                "home|Home", "prestige|Prestige", "shop|Shop"
        }, active, action -> {
            String id = action.startsWith("tab:") ? action.substring(4) : action;
            switch (id) {
                case "prestige" -> open(player, "turnin");
                case "shop" -> open(player, "shop");
                default -> open(player, "main");
            }
        });
    }

    /** Overview and the pages that are not the turn-in flow or the shop. */
    private static void paintHome(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcUltraStyle.header("Prestige"),
                CnpcUltraStyle.SUBTITLE + "What prestige does, then where to spend it");
        infoY = prestigeTabs(player, gui, infoY, "home");
        List<String> lines = new ArrayList<>(ProgressionGuiApi.prestigeLines(player, "main"));
        lines.add(CnpcUltraStyle.SUBTITLE + "Prestige raises your lifetime count and resets stats. Exchange current prestige for favor on the Prestige tab.");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 8));
        CnpcGuiSupport.button(gui, 24, CnpcUltraStyle.ACCENT + "Permanent tiers", CnpcGuiSupport.COL_L, row,
                () -> open(player, "tiers"));
        CnpcGuiSupport.button(gui, 25, CnpcUltraStyle.INFO + "Forms", CnpcGuiSupport.COL_R, row,
                () -> open(player, "forms"));
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, null);
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        paintHome(player, gui);
    }

    private static void paintTurnIn(ServerPlayer player, ICustomGui gui) {
        if (!prestigeReady(player, gui, "main")) {
            return;
        }
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        int held = parseInt(ph.get("held"), 0);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Prestige", "Prestige"),
                CnpcUltraStyle.SUBTITLE + "Each button shows the favor you'll get");
        infoY = prestigeTabs(player, gui, infoY, "prestige");
        List<String> info = new ArrayList<>(ProgressionGuiApi.prestigeLines(player, "turnin"));
        info.add(CnpcUltraStyle.SUBTITLE + "Bulk bonus: " + CnpcUltraStyle.BODY + "3→4 " + CnpcUltraStyle.DIM + "· " + CnpcUltraStyle.BODY + "6→9 " + CnpcUltraStyle.DIM + "· " + CnpcUltraStyle.BODY + "9→15 " + CnpcUltraStyle.SUBTITLE + "favor");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, info, CnpcGuiStyle.INFO_INLINE_MAX));
        gui.addLabel(116, CnpcGuiSupport.safeChat(
                CnpcUltraStyle.DANGER + "Turning in prestiges removes your stat bonus. This cannot be undone."),
                CnpcGuiSupport.M, row, CnpcGuiSupport.textBandWidth(), 14);
        row += CnpcGuiSupport.LINE_H;
        gui.addLabel(117, CnpcGuiSupport.safeChat(
                CnpcUltraStyle.DANGER + "Exchanging prestige for favor removes the stat bonus from the exchanged prestiges."),
                CnpcGuiSupport.M, row, CnpcGuiSupport.textBandWidth(), 14);
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Prestige now", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> ProgressionGuiApi.handlePrestigeDo(player, "confirm", "", "turnin"),
                () -> open(player, "turnin")));
        row += CnpcGuiSupport.ROW_STEP;
        row = CnpcGuiSupport.paintSectionTag(gui, CnpcGuiSupport.ID_INLINE_NOTE, row + 4, CnpcUltraStyle.INFO + "Choose amount");

        int[] amounts = PrestigePointsSystem.TURN_IN_AMOUNTS;
        CnpcGuiLayout.GridButton[] grid = new CnpcGuiLayout.GridButton[amounts.length];
        for (int i = 0; i < amounts.length; i++) {
            int amount = amounts[i];
            String label = turnInLabel(ph, amount, held);
            if (held >= amount) {
                int n = amount;
                grid[i] = CnpcGuiLayout.GridButton.action(
                        label,
                        () -> ProgressionGuiApi.handlePrestigeDo(player, "turnin", String.valueOf(n), "turnin"),
                        () -> open(player, "turnin"));
            } else {
                grid[i] = CnpcGuiLayout.GridButton.disabled(label);
            }
        }
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(
                player, gui, row, CnpcGuiSupport.ID_GRID_BASE, grid, () -> open(player, "turnin"));
        row += 4;
        footer(player, gui, row, "main");
    }

    private static String turnInLabel(Map<String, String> ph, int amount, int held) {
        String gain = ph.getOrDefault("turnin_" + amount + "_points", "0");
        if (held >= amount) {
            return CnpcUltraStyle.INFO + "Exchange " + amount + " " + CnpcUltraStyle.SUBTITLE + "(+" + gain + " favor)";
        }
        return CnpcUltraStyle.SUBTITLE + "Exchange " + amount + " " + CnpcUltraStyle.DIM + "· need " + amount + " current prestige · +" + gain + " favor";
    }

    private static void paintShop(ServerPlayer player, ICustomGui gui, int pageIndex) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        int pages = Math.max(1, parseInt(ph.get("shop_pages"), 1));
        int page = Math.min(pages - 1, Math.max(0, pageIndex));
        int pageSize = Math.max(1, parseInt(ph.get("shop_page_size"), 6));
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Prestige", "Skill shop"),
                CnpcUltraStyle.SUBTITLE + "Page " + CnpcUltraStyle.BODY + (page + 1) + "/" + pages + CnpcGuiStyle.SEP + CnpcUltraStyle.SUBTITLE + "Favor " + CnpcUltraStyle.BODY
                        + ph.getOrDefault("points", "0"));
        infoY = prestigeTabs(player, gui, infoY, "shop");

        List<String> ids = shopSkillIds(ph);
        int from = page * pageSize;
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                CnpcUltraStyle.SUBTITLE + "Permanent skill levels · survive prestige"
        ), CnpcGuiStyle.INFO_INLINE_MAX));
        int placed = 0;
        List<CnpcGuiLayout.GridButton> grid = new ArrayList<>();
        for (int i = from; i < ids.size() && placed < pageSize; i++, placed++) {
            String id = ids.get(i);
            String label = CnpcGuiSupport.humanizeSkillLabel(id, ph.getOrDefault("skill_" + id + "_label", id));
            String bought = ph.getOrDefault("skill_" + id, "0");
            String max = ph.getOrDefault("skill_" + id + "_max", "?");
            int boughtN = parseInt(bought, 0);
            int maxN = parseInt(max, 0);
            String cost = ph.getOrDefault("skill_" + id + "_cost", "?");
            String caption = CnpcGuiSupport.compactShopLabel(label, boughtN, maxN > 0 ? maxN : 0, cost);
            if (maxN > 0 && boughtN >= maxN) {
                grid.add(CnpcGuiLayout.GridButton.disabled(CnpcUltraStyle.CONFIRM + caption + " " + CnpcUltraStyle.DIM + "· max"));
            } else {
                String skillId = id;
                int pageFinal = page;
                grid.add(CnpcGuiLayout.GridButton.action(
                        caption,
                        () -> ProgressionGuiApi.handlePrestigeDo(player, "skill", skillId, "shop"),
                        () -> open(player, "shop:" + pageFinal)));
            }
        }
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(
                player, gui, row, CnpcGuiSupport.ID_GRID_BASE, grid.toArray(CnpcGuiLayout.GridButton[]::new),
                () -> open(player, "shop:" + page));
        if (page > 0) {
            int prev = page - 1;
            CnpcGuiSupport.buttonSmallFull(gui, 90, CnpcUltraStyle.SUBTITLE + "« Prev", CnpcGuiSupport.COL_L, row, CnpcGuiSupport.BTN_W,
                    () -> open(player, "shop:" + prev));
        }
        if (page + 1 < pages) {
            CnpcGuiSupport.buttonSmallFull(gui, 91, CnpcUltraStyle.SUBTITLE + "Next »", CnpcGuiSupport.COL_R, row, CnpcGuiSupport.BTN_W,
                    () -> open(player, "shop:" + (page + 1)));
        }
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, "home");
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
        if (!prestigeReady(player, gui, "forms")) {
            return;
        }
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        boolean hasMajin = "true".equalsIgnoreCase(ph.get("majin"));
        boolean hasMutant = "true".equalsIgnoreCase(ph.get("mutant"));
        boolean canBuyMajin = "true".equalsIgnoreCase(ph.get("majin_can_buy"));
        boolean canBuyMutant = "true".equalsIgnoreCase(ph.get("mutant_can_buy"));
        String cost = ph.getOrDefault("form_cost", "5");

        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Prestige", "Forms"),
                CnpcUltraStyle.SUBTITLE + "Majin and Mutant · " + CnpcUltraStyle.INFO + cost + " " + CnpcUltraStyle.SUBTITLE + "prestige favor · one form at a time");
        infoY = prestigeTabs(player, gui, infoY, "home");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY,
                ProgressionGuiApi.prestigeLines(player, "effects"), CnpcGuiStyle.INFO_INLINE_MAX));
        row = CnpcGuiSupport.paintSectionTag(gui, CnpcGuiSupport.ID_INLINE_NOTE, row, CnpcUltraStyle.ACCENT + "Forms");
        List<CnpcGuiLayout.GridButton> grid = new ArrayList<>();
        if (hasMajin) {
            grid.add(CnpcGuiLayout.GridButton.disabled(CnpcUltraStyle.CONFIRM + "Majin · owned"));
        } else if (canBuyMajin) {
            grid.add(CnpcGuiLayout.GridButton.action(
                    CnpcUltraStyle.ACCENT + "Buy Majin · " + cost + " prestige favor",
                    () -> ProgressionGuiApi.handlePrestigeDo(player, "majin", "", "effects"),
                    () -> open(player, "effects")));
        } else {
            grid.add(CnpcGuiLayout.GridButton.disabled(CnpcUltraStyle.DIM + "Majin · remove Mutant first"));
        }
        if (hasMutant) {
            grid.add(CnpcGuiLayout.GridButton.disabled(CnpcUltraStyle.CONFIRM + "Mutant · owned"));
        } else if (canBuyMutant) {
            grid.add(CnpcGuiLayout.GridButton.action(
                    CnpcUltraStyle.ACCENT + "Buy Mutant · " + cost + " prestige favor",
                    () -> ProgressionGuiApi.handlePrestigeDo(player, "mutant", "", "effects"),
                    () -> open(player, "effects")));
        } else {
            grid.add(CnpcGuiLayout.GridButton.disabled(CnpcUltraStyle.DIM + "Mutant · remove Majin first"));
        }
        if (hasMajin) {
            grid.add(CnpcGuiLayout.GridButton.action(
                    CnpcUltraStyle.DANGER + "Remove Majin",
                    () -> ProgressionGuiApi.handlePrestigeDo(player, "unmajin", "", "effects"),
                    () -> open(player, "effects")));
        } else if (hasMutant) {
            grid.add(CnpcGuiLayout.GridButton.action(
                    CnpcUltraStyle.DANGER + "Remove Mutant",
                    () -> ProgressionGuiApi.handlePrestigeDo(player, "unmutant", "", "effects"),
                    () -> open(player, "effects")));
        } else {
            grid.add(CnpcGuiLayout.GridButton.disabled(CnpcUltraStyle.SUBTITLE + "Nothing to remove"));
        }
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(
                player, gui, row, CnpcGuiSupport.ID_GRID_BASE,
                grid.toArray(CnpcGuiLayout.GridButton[]::new),
                () -> open(player, "effects"));
        footer(player, gui, row, "home");
    }

    private static void paintTiers(ServerPlayer player, ICustomGui gui) {
        if (!prestigeReady(player, gui, "home")) {
            return;
        }
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.ACCENT, "Prestige", "Tiers"),
                CnpcUltraStyle.SUBTITLE + "Permanent unlocks · tiers T1 through T7");
        infoY = prestigeTabs(player, gui, infoY, "home");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, ProgressionGuiApi.prestigeLines(player, "tiers"),
                        CnpcGuiStyle.INFO_INLINE_MAX));
        row = CnpcGuiSupport.paintSectionTag(gui, CnpcGuiSupport.ID_INLINE_NOTE, row + 4, CnpcUltraStyle.INFO + "Buy permanent tiers");
        CnpcGuiLayout.GridButton[] grid = new CnpcGuiLayout.GridButton[7];
        for (int t = 1; t <= 7; t++) {
            int tier = t;
            boolean owned = "true".equalsIgnoreCase(ph.get("tier_" + t + "_owned"));
            boolean canBuy = "true".equalsIgnoreCase(ph.get("tier_" + t + "_can_buy"));
            String cost = ph.getOrDefault("tier_" + t + "_cost", "?");
            if (owned) {
                grid[t - 1] = CnpcGuiLayout.GridButton.disabled(CnpcUltraStyle.CONFIRM + "T" + tier + " " + CnpcUltraStyle.DIM + "· owned");
            } else if (canBuy) {
                grid[t - 1] = CnpcGuiLayout.GridButton.action(
                        CnpcUltraStyle.BODY + "Buy T" + tier + " · " + cost + " prestige favor",
                        () -> ProgressionGuiApi.handlePrestigeDo(player, "tier", String.valueOf(tier), "tiers"),
                        () -> open(player, "tiers"));
            } else {
                int prev = tier - 1;
                String hint = tier > 1 ? "Buy T" + prev + " first" : "Unavailable";
                grid[t - 1] = CnpcGuiLayout.GridButton.disabled(CnpcUltraStyle.DIM + "T" + tier + " · " + hint);
            }
        }
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(
                player, gui, row, CnpcGuiSupport.ID_PRESTIGE_TIER_GRID, grid, () -> open(player, "tiers"));
        row += 4;
        footer(player, gui, row, "main");
    }

    private static boolean prestigeReady(ServerPlayer player, ICustomGui gui, String backPage) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        if ("true".equalsIgnoreCase(ph.get("bridge_ok")) && "true".equalsIgnoreCase(ph.get("system_enabled"))) {
            return true;
        }
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcUltraStyle.ACCENT + "Prestige", CnpcUltraStyle.DANGER + "Unavailable");
        infoY = prestigeTabs(player, gui, infoY, "prestige".equals(backPage) || "main".equals(backPage) ? "prestige" : "home");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY,
                ProgressionGuiApi.prestigeLines(player, "main"), CnpcGuiStyle.INFO_INLINE_MAX));
        footer(player, gui, row + 8, "forms".equals(backPage) ? null : backPage);
        return false;
    }

    private static void footer(ServerPlayer player, ICustomGui gui, int row, String parentPage) {
        if (parentPage == null || parentPage.isBlank()) {
            CnpcGuiSupport.navBackToMainMenu(player, gui, row);
        } else {
            CnpcGuiSupport.navBackToParent(player, gui, row, () -> open(player, "main"));
        }
        if (parentPage == null || parentPage.isBlank()) {
            CnpcGuiSupport.paintSystemMainPreview(CnpcGuiSupport.target(player), gui, player);
        }
    }
}
