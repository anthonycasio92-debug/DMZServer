package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

public final class CnpcLmSkillCheckGui {
    private CnpcLmSkillCheckGui() {}

    public static void open(ServerPlayer player, String page) {
        if (!SkillCheckService.canUse(player) && !StaffAccess.isStaff(player)) {
            player.m_213846_(net.minecraft.network.chat.Component.m_237113_("§cSkill Check requires donator access."));
            return;
        }
        String p = page == null || page.isBlank() ? "core" : page;
        CnpcGuiSupport.show(player, CnpcLmGui.ID_SKILLCHECK, (pl, gui) -> paint(pl, gui, p));
    }

    public static void openSkillsAdmin(ServerPlayer player, String page) {
        if (!StaffAccess.isStaff(player)) {
            player.m_213846_(net.minecraft.network.chat.Component.m_237113_("§cStaff only."));
            return;
        }
        open(player, page == null ? "core" : page);
    }

    private static void paint(ServerPlayer player, ICustomGui gui, String page) {
        CnpcGuiSupport.title(gui, 1, "§eSkill Check");
        CnpcGuiSupport.subtitle(gui, 2, "§7Natural · Saga progression");
        gui.addLabel(10, "§7Page §f" + page, CnpcGuiSupport.M, 48, 400, 12);
        int row = 90;
        CnpcGuiSupport.button(gui, 20, "§aNatural", CnpcGuiSupport.COL_L, row, () -> {
            SkillCheckService.open(player, "core");
            open(player, "core");
        });
        CnpcGuiSupport.button(gui, 21, "§bSaga", CnpcGuiSupport.COL_R, row, () -> {
            SkillCheckService.open(player, "saga");
            open(player, "saga");
        });
        row += 24;
        CnpcGuiSupport.buttonSmall(gui, 96, "§7« Hub", CnpcGuiSupport.COL_L, row, 95, () -> CnpcLmHubGui.open(player, "main"));
    }
}
