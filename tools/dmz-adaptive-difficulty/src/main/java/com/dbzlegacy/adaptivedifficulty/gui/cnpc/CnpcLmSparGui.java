package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.gui.SparGuiApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

public final class CnpcLmSparGui {
    private CnpcLmSparGui() {}

    public static void open(ServerPlayer player, String page) {
        CnpcGuiSupport.show(player, CnpcLmGui.ID_SPAR, (pl, gui) -> paintMain(pl, gui));
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        Map<String, String> ph = SparGuiApi.placeholders(player);
        CnpcGuiSupport.title(gui, 1, "§bSparring");
        String partner = ph.getOrDefault("partner", "");
        CnpcGuiSupport.subtitle(gui, 2, "§7Session §f"
                + ("true".equals(ph.get("session_active")) ? "§aactive" : "§8idle")
                + (partner == null || partner.isBlank() ? "" : " §8· §7vs §f" + partner));
        CnpcGuiSupport.divider(gui, 3, 38);

        List<String> lines = new ArrayList<>(SparGuiApi.linesForPage(player, "main"));
        CnpcGuiSupport.bodyLines(gui, 10, 44, lines, 6);

        int row = 120;
        CnpcGuiSupport.button(gui, 20, "§aStart spar TP", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> SparGuiApi.handleDo(player, "tp", "", "main"),
                () -> open(player, "main")));
        CnpcGuiSupport.button(gui, 21, "§eEnd session", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> SparGuiApi.handleDo(player, "end", "", "main"),
                () -> open(player, "main")));
        row += 24;
        CnpcGuiSupport.buttonSmall(gui, 96, "§7« Hub", CnpcGuiSupport.COL_L, row, 95, () -> CnpcLmHubGui.open(player, "main"));
        CnpcGuiSupport.buttonSmall(gui, 98, "§cClose", CnpcGuiSupport.COL_R, row, 95, () -> {});
    }
}
