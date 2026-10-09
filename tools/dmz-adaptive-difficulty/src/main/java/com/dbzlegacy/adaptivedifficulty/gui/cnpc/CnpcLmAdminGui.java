package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

/** Staff hub — reload, progression tools, logs (matches chest hub Admin tile). */
public final class CnpcLmAdminGui {
    private CnpcLmAdminGui() {}

    public static void open(ServerPlayer player, String page) {
        if (!StaffAccess.isStaff(player)) {
            CnpcGuiSupport.denyToHub(player, CnpcMenuFeedback.NOTICE_BODY + "Staff only.");
            return;
        }
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_ADMIN, CnpcGuiSupport.W,
                CnpcGuiSupport.window(296), (pl, gui) -> paintMain(pl, gui));
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§cStaff Admin",
                "§7Reload config, progression tools, and event log");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, java.util.List.of(
                "§7Full commands: §8/lm admin help",
                "§8/lm admin inspect … §7· §8/difficulty admin …"
        ), CnpcGuiStyle.INFO_INLINE_MAX));
        row = CnpcGuiSupport.paintSectionTag(gui, CnpcGuiSupport.ID_INLINE_NOTE, row, "Tools");
        CnpcGuiSupport.button(gui, 20, "§aReload Legacy Mechanics config", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyConfig.reload()
                        ? CnpcMenuFeedback.NOTICE_BODY + "Legacy Mechanics config reloaded."
                        : CnpcMenuFeedback.NOTICE_BODY + "Config reload failed.",
                () -> open(player, "main")));
        CnpcGuiSupport.button(gui, 21, "§9Progression panel", CnpcGuiSupport.COL_R, row,
                () -> CnpcLmGui.open(player, "progression", "main"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.buttonSmallFull(gui, 22, "§8Event log", CnpcGuiSupport.M, row,
                CnpcGuiSupport.textBandWidth(), () -> CnpcLmLogsGui.open(player, "main"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.navSystemRoot(player, gui, row);
        CnpcGuiSupport.paintSystemMainPreview(CnpcGuiSupport.target(player), gui, player);
    }
}
