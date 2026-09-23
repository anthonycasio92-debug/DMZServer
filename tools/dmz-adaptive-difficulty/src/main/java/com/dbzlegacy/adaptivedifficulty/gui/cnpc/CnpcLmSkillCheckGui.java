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
        if (!SkillCheckService.canUse(player)) {
            CnpcGuiSupport.denyToHub(player,
                    "§cSkill Check needs donator access.\n§7Ask staff if you think you should have it.");
            return;
        }
        show(player, page, false);
    }

    public static void openSkillsAdmin(ServerPlayer player, String page) {
        if (!StaffAccess.isStaff(player)) {
            CnpcGuiSupport.denyToHub(player, "§cStaff only.");
            return;
        }
        show(player, page, true);
    }

    private static void show(ServerPlayer player, String page, boolean staffAdminBrowser) {
        String p = page == null || page.isBlank() ? "core" : page.toLowerCase();
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SKILLCHECK, CnpcGuiSupport.W, H,
                (pl, gui) -> paint(pl, gui, p, staffAdminBrowser));
    }

    private static void paint(ServerPlayer player, ICustomGui gui, String page, boolean staffAdminBrowser) {
        boolean staffAdmin = staffAdminBrowser;
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                staffAdmin ? CnpcGuiStyle.subPage("§e", "Skills", "Staff") : "§eSkill Check",
                "§7Natural, Saga, and Skill Check sessions");

        List<String> lines = ProgressionGuiApi.skillsLines(CnpcGuiSupport.target(player), page);
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, "§aNatural skills", CnpcGuiSupport.COL_L, row, () -> {
            if (staffAdmin) {
                com.dbzlegacy.adaptivedifficulty.gui.SkillsMenu.open(player, "core");
            } else {
                SkillCheckService.open(player, "core");
                show(player, "core", false);
            }
        });
        CnpcGuiSupport.button(gui, 21, "§dSaga skills", CnpcGuiSupport.COL_R, row, () -> {
            if (staffAdmin) {
                com.dbzlegacy.adaptivedifficulty.gui.SkillsMenu.open(player, "saga");
            } else {
                SkillCheckService.open(player, "saga");
                show(player, "saga", false);
            }
        });
        row += 24;
        if (SkillCheckService.canUse(player)) {
            CnpcGuiSupport.button(gui, 22, "§bOpen Skill Check UI", CnpcGuiSupport.COL_L, row, () -> {
                SkillCheckService.open(player, page);
                show(player, page, false);
            });
        }
        row += 24;
        if ("saga".equals(page)) {
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> {
                if (staffAdmin) {
                    show(player, "core", true);
                } else {
                    show(player, "core", false);
                }
            }, "§7« Back");
        } else {
            CnpcGuiSupport.navSystemRoot(player, gui, row);
        }
        if ("core".equals(page)) {
            CnpcGuiSupport.paintSystemMainPreview(CnpcGuiSupport.target(player), gui, player);
        }
    }
}
