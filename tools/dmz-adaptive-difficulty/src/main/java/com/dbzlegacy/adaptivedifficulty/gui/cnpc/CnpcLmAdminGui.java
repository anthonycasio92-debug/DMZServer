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
            player.m_213846_(net.minecraft.network.chat.Component.m_237113_("§cStaff only."));
            return;
        }
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_ADMIN, CnpcGuiSupport.W, 300, (pl, gui) -> paint(pl, gui));
    }

    private static void paint(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§cStaff tools");
        CnpcGuiSupport.subtitle(gui, 2, "§7Reload configs · progression · telemetry");
        CnpcGuiSupport.divider(gui, 3, 38);
        CnpcGuiSupport.bodyLines(gui, 10, 46, java.util.List.of(
                "§7Use chat for full admin commands:",
                "§8/lm admin help §7· §8/lm admin inspect …",
                "§8/difficulty admin … §7· §8/progression …"
        ), 4);

        int row = 110;
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
        row += 24;
        CnpcGuiSupport.buttonSmall(gui, 96, "§7« Hub", CnpcGuiSupport.COL_L, row, 95,
                () -> CnpcLmHubGui.open(player, "main"));
    }
}
