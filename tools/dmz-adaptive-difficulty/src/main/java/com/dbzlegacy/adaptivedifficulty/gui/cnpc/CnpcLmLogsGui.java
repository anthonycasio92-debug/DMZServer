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
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_LOGS, CnpcGuiSupport.W, 280, (pl, gui) -> paint(pl, gui));
    }

    private static void paint(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§8Server event log");
        CnpcGuiSupport.subtitle(gui, 2, "§7Telemetry for staff debugging");
        CnpcGuiSupport.divider(gui, 3, 38);
        List<String> lines = MechanicsGuiApi.linesForPage(player, "logs");
        CnpcGuiSupport.bodyLines(gui, 10, 46, lines, 8);
        int row = 150;
        CnpcGuiSupport.button(gui, 20, "§aTurn logging on", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> MechanicsGuiApi.handleDo(player, "syslog", "on", "logs"),
                () -> open(player, "main")));
        CnpcGuiSupport.button(gui, 21, "§eTurn logging off", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> MechanicsGuiApi.handleDo(player, "syslog", "off", "logs"),
                () -> open(player, "main")));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§7Flush to disk", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> MechanicsGuiApi.handleDo(player, "syslog", "flush", "logs"),
                () -> open(player, "main")));
        row += 24;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> CnpcLmAdminGui.open(player, "main"), "§7« Back");
    }
}
