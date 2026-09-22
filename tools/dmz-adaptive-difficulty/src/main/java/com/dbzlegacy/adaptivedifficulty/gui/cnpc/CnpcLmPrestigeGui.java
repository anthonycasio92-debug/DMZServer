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
        int height = switch (p) {
            case "turnin", "points" -> 340;
            case "shop", "skills" -> 360;
            case "tiers", "tier" -> 360;
            default -> 320;
        };
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_PRESTIGE, CnpcGuiSupport.W, height, (pl, gui) -> {
            switch (pageKey) {
                case "turnin", "points" -> paintTurnIn(pl, gui);
                case "shop", "skills" -> paintShop(pl, gui, shopPageFinal);
                case "forms", "effects", "effect" -> paintEffects(pl, gui);
                case "cap", "breakthrough" -> paintCap(pl, gui);
                case "tiers", "tier" -> paintTiers(pl, gui);
                default -> paintMain(pl, gui);
            }
        });
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§dPrestige",
                "§7Turn-ins, skill shop, and permanent unlocks");
        List<String> lines = ProgressionGuiApi.prestigeLines(player, "main");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, CnpcGuiStyle.INFO_INLINE_MAX);
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
        CnpcGuiSupport.button(gui, 25, "§3Cap breakthrough", CnpcGuiSupport.COL_R, row, () -> open(player, "cap"));
        row += 24;
        footer(player, gui, row, null, player, infoY);
    }

    private static void paintTurnIn(ServerPlayer player, ICustomGui gui) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        int held = parseInt(ph.get("held"), 0);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§d", "Prestige", "Turn-in"),
                "§7Each button shows points gained before you confirm");
        List<String> info = new ArrayList<>(ProgressionGuiApi.prestigeLines(player, "turnin"));
        info.add("§8Pack bonus · 3→4 pts · 6→9 pts · 9→15 pts");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, info, CnpcGuiStyle.INFO_INLINE_MAX);
        row = CnpcGuiSupport.paintSectionTag(gui, 12, row + 4, "§8Choose amount");

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
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(player, gui, row, 30, grid, () -> open(player, "turnin"));
        row += 4;
        footer(player, gui, row, "main");
    }

    private static String turnInLabel(Map<String, String> ph, int amount, int held) {
        String gain = ph.getOrDefault("turnin_" + amount + "_points", "0");
        if (held >= amount) {
            return "§eTurn in ×" + amount + " §7(+" + gain + " pts)";
        }
        return "§8×" + amount + " §7(need " + amount + " held · +" + gain + " pts)";
    }

    private static void paintShop(ServerPlayer player, ICustomGui gui, int pageIndex) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        int pages = Math.max(1, parseInt(ph.get("shop_pages"), 1));
        int page = Math.min(pages - 1, Math.max(0, pageIndex));
        int pageSize = Math.max(1, parseInt(ph.get("shop_page_size"), 6));
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§d", "Prestige", "Skill shop"),
                "§7Page §f" + (page + 1) + "/" + pages + CnpcGuiStyle.SEP + "§7Points §f"
                        + ph.getOrDefault("points", "0"));

        List<String> ids = shopSkillIds(ph);
        int from = page * pageSize;
        int row = infoY + 8;
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
            String caption = label + " §7(" + bought + "/" + max + ") · §6" + cost + " pts";
            if (maxN > 0 && boughtN >= maxN) {
                grid.add(CnpcGuiLayout.GridButton.disabled("§a" + caption + " §8· maxed"));
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
                player, gui, row, 30, grid.toArray(CnpcGuiLayout.GridButton[]::new),
                () -> open(player, "shop:" + page));
        if (page > 0) {
            int prev = page - 1;
            CnpcGuiSupport.buttonSmallFull(gui, 90, "§7« Prev", CnpcGuiSupport.COL_L, row, CnpcGuiSupport.BTN_W,
                    () -> open(player, "shop:" + prev));
        }
        if (page + 1 < pages) {
            CnpcGuiSupport.buttonSmallFull(gui, 91, "§7Next »", CnpcGuiSupport.COL_R, row, CnpcGuiSupport.BTN_W,
                    () -> open(player, "shop:" + (page + 1)));
        }
        row += CnpcGuiSupport.ROW_STEP + 4;
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

        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§d", "Prestige", "Effects"),
                "§7Majin and Mutant forms · §e" + cost + " §7points each · one at a time");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, ProgressionGuiApi.prestigeLines(player, "effects"),
                CnpcGuiStyle.INFO_INLINE_MAX);

        if (hasMajin) {
            gui.addLabel(40, "§aPermanent Majin §8· owned", CnpcGuiSupport.COL_L, row + 4, CnpcGuiSupport.BTN_W, 14);
        } else if (canBuyMajin) {
            CnpcGuiSupport.button(gui, 40, "§dBuy Majin", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> ProgressionGuiApi.handlePrestigeDo(player, "majin", "", "effects"),
                    () -> open(player, "effects")));
        } else {
            gui.addLabel(40, "§8Buy Majin §7(unpurchase Mutant first)", CnpcGuiSupport.COL_L, row + 4, CnpcGuiSupport.BTN_W, 14);
        }

        if (hasMutant) {
            gui.addLabel(41, "§aPermanent Mutant §8· owned", CnpcGuiSupport.COL_R, row + 4, CnpcGuiSupport.BTN_W, 14);
        } else if (canBuyMutant) {
            CnpcGuiSupport.button(gui, 41, "§dBuy Mutant", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> ProgressionGuiApi.handlePrestigeDo(player, "mutant", "", "effects"),
                    () -> open(player, "effects")));
        } else {
            gui.addLabel(41, "§8Buy Mutant §7(unpurchase Majin first)", CnpcGuiSupport.COL_R, row + 4, CnpcGuiSupport.BTN_W, 14);
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
            gui.addLabel(42, "§7Nothing to unpurchase", CnpcGuiSupport.COL_L, row + 4, CnpcGuiSupport.BTN_W, 14);
        }
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, "main");
    }

    private static void paintCap(ServerPlayer player, ICustomGui gui) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        int bt = parseInt(ph.get("breakthroughs"), 0);
        int btMax = parseInt(ph.get("breakthroughs_max"), PrestigePointsSystem.MAX_BREAKTHROUGHS);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§d", "Prestige", "Level cap"),
                "§7Raise your level cap with breakthroughs");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, ProgressionGuiApi.prestigeLines(player, "cap"),
                CnpcGuiStyle.INFO_INLINE_MAX);
        if (bt >= btMax) {
            gui.addLabel(20, "§aCap fully raised §8· no more breakthroughs", CnpcGuiSupport.COL_L, row + 4, CnpcGuiSupport.textBandWidth(), 14);
        } else {
            String cost = ph.getOrDefault("next_breakthrough_cost", "?");
            CnpcGuiSupport.buttonSmallFull(gui, 20, "§aBuy breakthrough · §e" + cost + " pts",
                    CnpcGuiSupport.COL_L, row, CnpcGuiSupport.BTN_W, () -> CnpcGuiSupport.act(
                            player,
                            () -> ProgressionGuiApi.handlePrestigeDo(player, "breakthrough", "", "cap"),
                            () -> open(player, "cap")));
        }
        footer(player, gui, row + 28, "main");
    }

    private static void paintTiers(ServerPlayer player, ICustomGui gui) {
        Map<String, String> ph = ProgressionGuiApi.prestigePlaceholders(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§d", "Prestige", "Tiers"),
                "§7Permanent unlocks · tiers T1 through T7");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, ProgressionGuiApi.prestigeLines(player, "tiers"),
                CnpcGuiStyle.INFO_INLINE_MAX);
        row = CnpcGuiSupport.paintSectionTag(gui, 11, row + 4, "§8Permanent difficulty tiers");
        CnpcGuiLayout.GridButton[] grid = new CnpcGuiLayout.GridButton[7];
        for (int t = 1; t <= 7; t++) {
            int tier = t;
            boolean owned = "true".equalsIgnoreCase(ph.get("tier_" + t + "_owned"));
            boolean canBuy = "true".equalsIgnoreCase(ph.get("tier_" + t + "_can_buy"));
            String cost = ph.getOrDefault("tier_" + t + "_cost", "?");
            if (owned) {
                grid[t - 1] = CnpcGuiLayout.GridButton.disabled("§aT" + tier + " §8· owned");
            } else if (canBuy) {
                grid[t - 1] = CnpcGuiLayout.GridButton.action(
                        "§fBuy T" + tier + " · §6" + cost + " pts",
                        () -> ProgressionGuiApi.handlePrestigeDo(player, "tier", String.valueOf(tier), "tiers"),
                        () -> open(player, "tiers"));
            } else {
                int prev = tier - 1;
                String hint = tier > 1 ? "Buy T" + prev + " first" : "Unavailable";
                grid[t - 1] = CnpcGuiLayout.GridButton.disabled("§8T" + tier + " · " + hint);
            }
        }
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(player, gui, row, 50, grid, () -> open(player, "tiers"));
        row += 4;
        footer(player, gui, row, "main");
    }

    private static void footer(ServerPlayer player, ICustomGui gui, int row) {
        footer(player, gui, row, null);
    }

    private static void footer(
            ServerPlayer player,
            ICustomGui gui,
            int row,
            String parentPage,
            ServerPlayer previewSubject,
            int previewAnchorY) {
        if (parentPage == null || parentPage.isBlank()) {
            CnpcGuiSupport.navSystemRoot(player, gui, row);
        } else {
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, parentPage), "§7« Back");
        }
        if (previewSubject != null && previewAnchorY >= 0) {
            CnpcGuiSupport.paintPlayerPreviewSlot(previewSubject, gui, previewAnchorY);
        }
    }

    private static void footer(ServerPlayer player, ICustomGui gui, int row, String parentPage) {
        footer(player, gui, row, parentPage, null, -1);
    }
}
