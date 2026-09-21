package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyTeamGuiApi;
import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

public final class CnpcLmDifficultyGui {
    private CnpcLmDifficultyGui() {}

    public static void open(ServerPlayer player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase();
        CnpcGuiSupport.show(player, CnpcLmGui.ID_DIFFICULTY, (pl, gui) -> {
            switch (p) {
                case "tiers", "buy", "tier" -> paintTiers(pl, gui);
                case "titles", "title" -> paintTitles(pl, gui);
                case "team", "teams" -> paintTeam(pl, gui);
                default -> paintMain(pl, gui);
            }
        });
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        DifficultyActions.prepareGui(player);
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        CnpcGuiSupport.title(gui, 1, "§aDifficulty");
        CnpcGuiSupport.subtitle(gui, 2, "§7Adaptive scaling & unlock tiers");

        List<String> lines = new ArrayList<>();
        if (!DifficultyConfig.isEnabled()) {
            lines.add("§cSystem disabled.");
        } else if (!SystemGate.allows(player)) {
            lines.add("§eNot available on this account.");
        } else {
            PlayerDifficultyData data = DifficultyCache.data(player);
            if (!data.isPersonalEnabled()) {
                lines.add("§cPersonal difficulty OFF");
            } else {
                lines.add("§7Tier §f" + snap.activeTierName + "  §8·  §7T" + snap.highestUnlockedTier);
            }
            lines.add("§6Coins §f" + AncientCoinEconomy.inventoryBreakdown(player));
            String title = TitleSystem.activeDisplay(player);
            if (title != null && !"None".equals(title)) {
                lines.add("§7Title §e" + title);
            }
            lines.add("§8Scaled mobs may hit other players.");
        }
        CnpcGuiSupport.bodyLines(gui, 10, 48, lines, 6);

        int row = 120;
        CnpcGuiSupport.button(gui, 20, "§eUnlock tiers", CnpcGuiSupport.COL_L, row, () -> open(player, "tiers"));
        CnpcGuiSupport.button(gui, 21, "§dTitles", CnpcGuiSupport.COL_R, row, () -> open(player, "titles"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§bTeam scaling", CnpcGuiSupport.COL_L, row, () -> open(player, "team"));
        CnpcGuiSupport.button(gui, 23, "§7Toggle personal", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArg(player, "toggle_personal", "0", "main").message(),
                () -> open(player, "main")));
        row += 24;
        navFooter(player, gui, row);
    }

    private static void paintTiers(ServerPlayer player, ICustomGui gui) {
        DifficultyActions.prepareGui(player);
        CnpcGuiSupport.title(gui, 1, "§eDifficulty · Tiers");
        CnpcGuiSupport.subtitle(gui, 2, "§7Activate an unlocked tier (0 = none)");

        DifficultySnapshot snap = DifficultyCache.refresh(player);
        PlayerDifficultyData data = DifficultyCache.data(player);
        int max = Math.max(0, snap.highestUnlockedTier);
        List<String> lines = new ArrayList<>();
        lines.add("§7Active §fT" + data.getActiveTier() + "  §8·  §7Max unlocked §fT" + max);
        CnpcGuiSupport.bodyLines(gui, 10, 48, lines, 3);

        int row = 90;
        for (int t = 0; t <= Math.min(6, max); t++) {
            int tier = t;
            int col = (t % 2 == 0) ? CnpcGuiSupport.COL_L : CnpcGuiSupport.COL_R;
            if (t % 2 == 0 && t > 0) {
                row += 24;
            }
            CnpcGuiSupport.buttonSmall(gui, 30 + t, "§fTier " + tier, col, row, 95, () -> CnpcGuiSupport.act(
                    player,
                    () -> DifficultyActions.handleArg(player, "activate", String.valueOf(tier), "tiers").message(),
                    () -> open(player, "tiers")));
        }
        row += 36;
        navFooter(player, gui, row);
    }

    private static void paintTitles(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§dDifficulty · Titles");
        CnpcGuiSupport.subtitle(gui, 2, "§7Equip a title you have unlocked");
        CnpcGuiSupport.button(gui, 20, "§7Clear title", CnpcGuiSupport.COL_L, 100, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArg(player, "title", "none", "titles").message(),
                () -> open(player, "titles")));
        CnpcGuiSupport.button(gui, 21, "§eRefresh list", CnpcGuiSupport.COL_R, 100, () -> open(player, "titles"));
        navFooter(player, gui, 200);
    }

    private static void paintTeam(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§bDifficulty · Teams");
        CnpcGuiSupport.subtitle(gui, 2, "§7Mutual rival team bonuses");
        CnpcGuiSupport.bodyLines(gui, 10, 48, DifficultyTeamGuiApi.linesForPage(player, "team"), 8);
        int row = 170;
        CnpcGuiSupport.button(gui, 40, "§7Personal only", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyTeamGuiApi.handleDo(player, "mode", "personal_only", "team"),
                () -> open(player, "team")));
        CnpcGuiSupport.button(gui, 41, "§aFull team scaling", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyTeamGuiApi.handleDo(player, "mode", "full_team_scaling", "team"),
                () -> open(player, "team")));
        row += 24;
        navFooter(player, gui, row);
    }

    private static void navFooter(ServerPlayer player, ICustomGui gui, int row) {
        CnpcGuiSupport.buttonSmall(gui, 96, "§7« Hub", CnpcGuiSupport.COL_L, row, 95, () -> CnpcLmHubGui.open(player, "main"));
        CnpcGuiSupport.buttonSmall(gui, 97, "§7Main", CnpcGuiSupport.COL_R, row, 95, () -> open(player, "main"));
        if (StaffAccess.isStaff(player)) {
            row += 24;
            CnpcGuiSupport.buttonSmall(gui, 98, "§8Staff settings", CnpcGuiSupport.COL_L, row, 195, () -> {
                player.m_213846_(net.minecraft.network.chat.Component.m_237113_(
                        "§7Staff difficulty settings: use §f/difficulty admin §7or chat settings page."));
            });
        }
    }
}
