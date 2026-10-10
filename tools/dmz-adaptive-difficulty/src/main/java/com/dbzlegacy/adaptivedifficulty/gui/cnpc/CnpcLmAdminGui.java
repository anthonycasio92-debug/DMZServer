package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

/** Staff hub — reload, progression tools, logs (matches chest hub Admin tile). */
public final class CnpcLmAdminGui {
    private CnpcLmAdminGui() {}

    public static void open(ServerPlayer player, String page) {
        CnpcUltraStyle.withAccent(CnpcUltraStyle.ACCENT_CHARACTER, () -> openAdmin(player, page));
    }

    private static void openAdmin(ServerPlayer player, String page) {
        if (!StaffAccess.isStaff(player)) {
            CnpcGuiSupport.denyToHub(player, CnpcMenuFeedback.NOTICE_BODY + "Staff only.");
            return;
        }
        int designed = 296;
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_ADMIN, CnpcGuiSupport.W,
                CnpcGuiSupport.window(designed), (pl, gui) -> paintMain(pl, gui));
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcUltraStyle.DANGER + "Staff Admin",
                CnpcUltraStyle.SUBTITLE + "Reload, edit config, progression tools, and the event log");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, java.util.List.of(
                CnpcUltraStyle.SUBTITLE + "Full commands: " + CnpcUltraStyle.DIM + "/lm admin help",
                CnpcUltraStyle.DIM + "/lm admin inspect … " + CnpcUltraStyle.SUBTITLE + "· " + CnpcUltraStyle.DIM + "/difficulty admin …"
        ), CnpcGuiStyle.INFO_INLINE_MAX));
        row = CnpcGuiSupport.paintSectionTag(gui, CnpcGuiSupport.ID_INLINE_NOTE, row, "Tools");
        CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Reload Legacy Mechanics config", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyConfig.reload()
                        ? CnpcMenuFeedback.NOTICE_BODY + "Legacy Mechanics config reloaded."
                        : CnpcMenuFeedback.NOTICE_BODY + "Config reload failed.",
                () -> open(player, "main")));
        CnpcGuiSupport.button(gui, 21, CnpcUltraStyle.ACCENT_PROGRESSION + "Progression panel", CnpcGuiSupport.COL_R, row,
                () -> CnpcLmGui.open(player, "progression", "main"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 23, CnpcUltraStyle.ACCENT_CONFIG + "Config editor", CnpcGuiSupport.COL_L, row,
                () -> CnpcLmGui.open(player, "config", "main"));
        CnpcGuiSupport.button(gui, 22, CnpcUltraStyle.ACCENT_ADMIN + "Event log", CnpcGuiSupport.COL_R, row,
                () -> CnpcLmLogsGui.open(player, "main"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.navSystemRoot(player, gui, row);
        CnpcGuiSupport.paintSystemMainPreview(CnpcGuiSupport.target(player), gui, player);
    }
}
