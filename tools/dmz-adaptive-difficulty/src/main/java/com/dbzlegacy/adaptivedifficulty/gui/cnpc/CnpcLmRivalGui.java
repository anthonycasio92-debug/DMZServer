package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.gui.RivalGuiApi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

public final class CnpcLmRivalGui {
    private CnpcLmRivalGui() {}

    public static void open(ServerPlayer player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase();
        CnpcGuiSupport.show(player, CnpcLmGui.ID_RIVAL, (pl, gui) -> {
            switch (p) {
                case "list" -> paintList(pl, gui);
                case "stats" -> paintScroll(pl, gui, "§6Rival · Stats", RivalGuiApi.statsLines(pl));
                case "top" -> paintScroll(pl, gui, "§6Rival · Leaderboard", RivalGuiApi.topLines(pl));
                case "pending" -> paintScroll(pl, gui, "§6Rival · Pending", RivalGuiApi.listLines(pl));
                default -> paintMain(pl, gui);
            }
        });
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        var ph = RivalGuiApi.placeholders(player);
        CnpcGuiSupport.title(gui, 1, "§6Rival System");
        CnpcGuiSupport.subtitle(gui, 2, "§7RP §f" + ph.getOrDefault("rp", "?") + "  §8·  §7Tier §f" + ph.getOrDefault("tier", "?"));

        List<String> lines = new ArrayList<>();
        lines.add("§7Mutual §f" + ph.getOrDefault("mutual", "0") + "/" + ph.getOrDefault("mutual_max", "3"));
        lines.add("§7Record §f" + ph.getOrDefault("wins", "0") + "W "
                + ph.getOrDefault("losses", "0") + "L " + ph.getOrDefault("draws", "0") + "D");
        lines.add("§7Pending §f" + ph.getOrDefault("pending_invites", "0"));
        CnpcGuiSupport.bodyLines(gui, 10, 48, lines, 5);

        int row = 110;
        CnpcGuiSupport.button(gui, 20, "§eRival list", CnpcGuiSupport.COL_L, row, () -> open(player, "list"));
        CnpcGuiSupport.button(gui, 21, "§bYour stats", CnpcGuiSupport.COL_R, row, () -> open(player, "stats"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§dLeaderboard", CnpcGuiSupport.COL_L, row, () -> open(player, "top"));
        CnpcGuiSupport.button(gui, 23, "§7Toggle TP msgs", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> RivalGuiApi.handleDo(player, "toggle_tp", "", "main"),
                () -> open(player, "main")));
        row += 24;
        footer(player, gui, row);
    }

    private static void paintList(ServerPlayer player, ICustomGui gui) {
        List<String> cards = RivalGuiApi.currentRivalCards(player);
        String[] items = cards.stream()
                .map(c -> {
                    String[] p = c.split("\t", -1);
                    return p.length > 1 ? p[1] : c;
                })
                .toArray(String[]::new);
        CnpcGuiSupport.title(gui, 1, "§6Your rivals");
        CnpcGuiSupport.subtitle(gui, 2, "§7Double-click a name for quick actions");
        CnpcGuiSupport.scroll(gui, 100, CnpcGuiSupport.M, 44, 400, 120, items);
        footer(player, gui, 200);
    }

    private static void paintScroll(ServerPlayer player, ICustomGui gui, String title, List<String> body) {
        CnpcGuiSupport.title(gui, 1, title);
        CnpcGuiSupport.bodyLines(gui, 10, 44, body, 12);
        footer(player, gui, 220);
    }

    private static void footer(ServerPlayer player, ICustomGui gui, int row) {
        CnpcGuiSupport.buttonSmall(gui, 96, "§7« Hub", CnpcGuiSupport.COL_L, row, 95, () -> CnpcLmHubGui.open(player, "main"));
        CnpcGuiSupport.buttonSmall(gui, 97, "§7Main", CnpcGuiSupport.COL_R, row, 95, () -> open(player, "main"));
    }
}
