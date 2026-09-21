package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

public final class CnpcLmProgressionGui {
    private CnpcLmProgressionGui() {}

    public static void open(ServerPlayer player, String page) {
        if (!StaffAccess.isStaff(player) && !"android_remove".equalsIgnoreCase(page)) {
            player.m_213846_(net.minecraft.network.chat.Component.m_237113_("§cStaff only."));
            return;
        }
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase();
        CnpcGuiSupport.show(player, CnpcLmGui.ID_PROGRESSION, (pl, gui) -> paint(pl, gui, p));
    }

    private static void paint(ServerPlayer player, ICustomGui gui, String page) {
        if ("android_remove".equals(page)) {
            CnpcGuiSupport.title(gui, 1, "§cRemove Android");
            CnpcGuiSupport.subtitle(gui, 2, "§7Confirm to restore prior forms");
            CnpcGuiSupport.button(gui, 20, "§cConfirm remove", CnpcGuiSupport.COL_L, 100, () -> CnpcGuiSupport.act(
                    player,
                    () -> ProgressionGuiApi.androidRemove(player, "confirm"),
                    () -> CnpcLmHubGui.open(player, "main")));
            CnpcGuiSupport.buttonSmall(gui, 96, "§7« Hub", CnpcGuiSupport.COL_L, 140, 95, () -> CnpcLmHubGui.open(player, "main"));
            return;
        }
        CnpcGuiSupport.title(gui, 1, "§5Progression §8(staff)");
        List<String> lines = ProgressionGuiApi.linesForPage(player, page);
        CnpcGuiSupport.bodyLines(gui, 10, 48, lines, 10);
        int row = 170;
        CnpcGuiSupport.button(gui, 30, "§eAdmin flags", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> ProgressionGuiApi.handleDo(player, "page", "admin", "main"),
                () -> open(player, "admin")));
        CnpcGuiSupport.buttonSmall(gui, 96, "§7« Hub", CnpcGuiSupport.COL_R, row, 95, () -> CnpcLmHubGui.open(player, "main"));
    }
}
