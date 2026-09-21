package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

public final class CnpcLmSkillCheckGui {
    private static final int H = 320;

    private CnpcLmSkillCheckGui() {}

    public static void open(ServerPlayer player, String page) {
        if (!SkillCheckService.canUse(player) && !StaffAccess.isStaff(player)) {
            player.m_213846_(net.minecraft.network.chat.Component.m_237113_("§cSkill Check requires donator access."));
            return;
        }
        String p = page == null || page.isBlank() ? "core" : page.toLowerCase();
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SKILLCHECK, CnpcGuiSupport.W, H, (pl, gui) -> paint(pl, gui, p));
    }

    public static void openSkillsAdmin(ServerPlayer player, String page) {
        if (!StaffAccess.isStaff(player)) {
            player.m_213846_(net.minecraft.network.chat.Component.m_237113_("§cStaff only."));
            return;
        }
        open(player, page == null ? "core" : page);
    }

    private static void paint(ServerPlayer player, ICustomGui gui, String page) {
        boolean staffAdmin = StaffAccess.isStaff(player) && !SkillCheckService.canUse(player);
        CnpcGuiSupport.title(gui, 1, staffAdmin ? "§eSkills §8(staff)" : "§eSkill Check");
        CnpcGuiSupport.subtitle(gui, 2, "§7Natural · Saga · Skill Check session");
        CnpcGuiSupport.inspectBanner(player, gui);

        List<String> lines = ProgressionGuiApi.skillsLines(CnpcGuiSupport.target(player), page);
        CnpcGuiSupport.bodyLines(gui, 10, 48, lines, 12);

        int row = 200;
        CnpcGuiSupport.button(gui, 20, "§aNatural skills", CnpcGuiSupport.COL_L, row, () -> {
            SkillCheckService.open(player, "core");
            open(player, "core");
        });
        CnpcGuiSupport.button(gui, 21, "§dSaga skills", CnpcGuiSupport.COL_R, row, () -> {
            SkillCheckService.open(player, "saga");
            open(player, "saga");
        });
        row += 24;
        if (SkillCheckService.canUse(player)) {
            CnpcGuiSupport.button(gui, 22, "§bOpen Skill Check UI", CnpcGuiSupport.COL_L, row, () -> {
                SkillCheckService.open(player, page);
                open(player, page);
            });
        }
        row += 24;
        CnpcGuiSupport.navHubMain(player, gui, row, () -> open(player, "core"));
    }
}
