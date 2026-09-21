package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

public final class CnpcLmPrestigeGui {
    private CnpcLmPrestigeGui() {}

    public static void open(ServerPlayer player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        CnpcGuiSupport.show(player, CnpcLmGui.ID_PRESTIGE, (pl, gui) -> {
            switch (p) {
                case "turnin", "points" -> paintTurnIn(pl, gui);
                case "shop", "skills" -> paintShopHint(pl, gui);
                case "forms", "effects", "effect" -> paintEffects(pl, gui);
                case "cap", "breakthrough" -> paintCap(pl, gui);
                case "tiers", "tier" -> paintTiers(pl, gui);
                default -> paintMain(pl, gui);
            }
        });
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§dPrestige");
        CnpcGuiSupport.subtitle(gui, 2, "§7Turn-ins · shop · permanent unlocks");
        CnpcGuiSupport.divider(gui, 3, 38);

        List<String> lines = ProgressionGuiApi.prestigeLines(player, "main");
        CnpcGuiSupport.bodyLines(gui, 10, 44, lines, 6);

        int row = 118;
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
        footer(player, gui, row);
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
        footer(player, gui, row);
    }

    private static void paintShopHint(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§bPrestige skill shop");
        CnpcGuiSupport.bodyLines(gui, 10, 44, ProgressionGuiApi.prestigeLines(player, "shop"), 8);
        gui.addLabel(50, "§7Use §f/prestige shop §7or §f/lmdo prestige shop §7for the full catalog.", CnpcGuiSupport.M, 150, 400, 24);
        footer(player, gui, 200);
    }

    private static void paintEffects(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§5Permanent effects");
        CnpcGuiSupport.bodyLines(gui, 10, 44, ProgressionGuiApi.prestigeLines(player, "effects"), 6);
        int row = 130;
        CnpcGuiSupport.button(gui, 40, "§dBuy Majin", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> ProgressionGuiApi.handlePrestigeDo(player, "majin", "", "effects"),
                () -> open(player, "effects")));
        CnpcGuiSupport.button(gui, 41, "§dBuy Mutant", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> ProgressionGuiApi.handlePrestigeDo(player, "mutant", "", "effects"),
                () -> open(player, "effects")));
        row += 24;
        footer(player, gui, row);
    }

    private static void paintCap(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§3Level cap");
        CnpcGuiSupport.bodyLines(gui, 10, 44, ProgressionGuiApi.prestigeLines(player, "cap"), 8);
        CnpcGuiSupport.button(gui, 20, "§aBuy breakthrough", CnpcGuiSupport.COL_L, 160, () -> CnpcGuiSupport.act(
                player,
                () -> ProgressionGuiApi.handlePrestigeDo(player, "breakthrough", "", "cap"),
                () -> open(player, "cap")));
        footer(player, gui, 200);
    }

    private static void paintTiers(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§6Prestige tiers");
        CnpcGuiSupport.bodyLines(gui, 10, 44, ProgressionGuiApi.prestigeLines(player, "tiers"), 6);
        int row = 110;
        for (int t = 1; t <= 7; t++) {
            int tier = t;
            int col = (t % 2 == 1) ? CnpcGuiSupport.COL_L : CnpcGuiSupport.COL_R;
            if (t > 1 && t % 2 == 1) {
                row += 24;
            }
            CnpcGuiSupport.buttonSmall(gui, 50 + t, "§fTier " + tier, col, row, 95, () -> CnpcGuiSupport.act(
                    player,
                    () -> ProgressionGuiApi.handlePrestigeDo(player, "tier", String.valueOf(tier), "tiers"),
                    () -> open(player, "tiers")));
        }
        row += 36;
        footer(player, gui, row);
    }

    private static void footer(ServerPlayer player, ICustomGui gui, int row) {
        CnpcGuiSupport.buttonSmall(gui, 96, "§7« Hub", CnpcGuiSupport.COL_L, row, 95, () -> CnpcLmHubGui.open(player, "main"));
        CnpcGuiSupport.buttonSmall(gui, 97, "§7Main", CnpcGuiSupport.COL_R, row, 95, () -> open(player, "main"));
    }
}
