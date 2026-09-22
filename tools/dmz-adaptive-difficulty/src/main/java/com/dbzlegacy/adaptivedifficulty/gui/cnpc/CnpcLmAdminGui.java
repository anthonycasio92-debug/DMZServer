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
            CnpcGuiSupport.denyToHub(player, "§cStaff only — that panel is for staff.");
            return;
        }
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_ADMIN, CnpcGuiSupport.W, 280, (pl, gui) -> paint(pl, gui));
    }

    private static void paint(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§cStaff Admin",
                "§7Reload config, progression tools, and event log");
        int row = CnpcGuiSupport.contentStartY(CnpcGuiSupport.paintInfoBlock(gui, infoY, java.util.List.of(
                "§7Full commands: §8/lm admin help",
                "§8/lm admin inspect … §7· §8/difficulty admin …"
        ), 2), infoY);
        row += 4;
        CnpcGuiSupport.button(gui, 20, "§aReload LM config", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyConfig.reload()
                        ? "§aLegacy Mechanics config reloaded."
                        : "§cConfig reload failed.",
                () -> open(player, "main")));
        CnpcGuiSupport.button(gui, 21, "§5Progression panel", CnpcGuiSupport.COL_R, row,
                () -> CnpcLmGui.open(player, "progression", "main"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§8Event log", CnpcGuiSupport.COL_L, row,
                () -> CnpcLmLogsGui.open(player, "main"));
        CnpcGuiSupport.button(gui, 23, "§eCNPC data migrate", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> com.dbzlegacy.adaptivedifficulty.gui.MechanicsGuiApi.handleDo(
                        player, "migrate-cnpc", "", "admin"),
                () -> open(player, "main")));
        row += CnpcGuiSupport.ROW_STEP + 4;
        CnpcGuiSupport.navSystemRoot(player, gui, row);
        CnpcGuiSupport.paintSubjectPreview(CnpcGuiSupport.target(player), gui, infoY);
    }
}
