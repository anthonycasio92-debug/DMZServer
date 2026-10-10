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
            CnpcGuiSupport.denyToHub(player, CnpcMenuFeedback.NOTICE_BODY + "Staff only.");
            return;
        }
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_LOGS, CnpcGuiSupport.W,
                CnpcGuiSupport.window(280), (pl, gui) -> paintMain(pl, gui));
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage(CnpcUltraStyle.DANGER, "Staff Admin", "Event log"),
                CnpcUltraStyle.SUBTITLE + "Staff event log — toggle or write logs to disk");
        List<String> lines = MechanicsGuiApi.linesForPage(player, "logs");
        var ph = MechanicsGuiApi.placeholders(player);
        boolean syslogOn = "true".equalsIgnoreCase(ph.getOrDefault("syslog", "false"));
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20,
                syslogOn ? CnpcGuiStyle.toggleOn("Event log") : CnpcGuiStyle.toggleOff("Event log"),
                CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> MechanicsGuiApi.handleDo(player, "syslog", syslogOn ? "off" : "on", "logs"),
                () -> open(player, "main")));
        CnpcGuiSupport.button(gui, 22, CnpcUltraStyle.SUBTITLE + "Write logs to disk", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> MechanicsGuiApi.handleDo(player, "syslog", "flush", "logs"),
                () -> open(player, "main")));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> CnpcLmAdminGui.open(player, "main"), CnpcUltraStyle.BACK);
        CnpcGuiSupport.paintSystemMainPreview(CnpcGuiSupport.target(player), gui, player);
    }
}
