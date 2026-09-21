package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.gui.MechanicsGuiApi;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

public final class CnpcLmLogsGui {
    private CnpcLmLogsGui() {}

    public static void open(ServerPlayer player, String page) {
        if (!StaffAccess.isStaff(player)) {
            player.m_213846_(net.minecraft.network.chat.Component.m_237113_("§cStaff only."));
            return;
        }
        CnpcGuiSupport.show(player, CnpcLmGui.ID_LOGS, (pl, gui) -> paint(pl, gui));
    }

    private static void paint(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§8Event log");
        List<String> lines = MechanicsGuiApi.linesForPage(player, "logs");
        CnpcGuiSupport.bodyLines(gui, 10, 44, lines, 8);
        int row = 150;
        CnpcGuiSupport.button(gui, 20, "§aLog ON", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> MechanicsGuiApi.handleDo(player, "syslog", "on", "logs"),
                () -> open(player, "main")));
        CnpcGuiSupport.button(gui, 21, "§eLog OFF", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> MechanicsGuiApi.handleDo(player, "syslog", "off", "logs"),
                () -> open(player, "main")));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§7Flush logs", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> MechanicsGuiApi.handleDo(player, "syslog", "flush", "logs"),
                () -> open(player, "main")));
        row += 24;
        CnpcGuiSupport.buttonSmall(gui, 96, "§7« Hub", CnpcGuiSupport.COL_L, row, 95, () -> CnpcLmHubGui.open(player, "main"));
    }
}
