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
        if (!SkillCheckService.canUse(player) && !SkillCheckService.inSession(player)) {
            CnpcGuiSupport.denyToHub(player,
                    CnpcMenuFeedback.NOTICE_BODY + "Skill Check isn't available to you.\n"
                            + CnpcMenuFeedback.NOTICE_BODY + "Ask staff if you think that's wrong.");
            return;
        }
        show(player, page, false);
    }

    public static void openSkillsAdmin(ServerPlayer player, String page) {
        if (!StaffAccess.isStaff(player)) {
            CnpcGuiSupport.denyToHub(player, CnpcMenuFeedback.NOTICE_BODY + "Staff only.");
            return;
        }
        show(player, page, true);
    }

    private static void show(ServerPlayer player, String page, boolean staffAdminBrowser) {
        String p = page == null || page.isBlank() ? "core" : page.toLowerCase();
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SKILLCHECK, CnpcGuiSupport.W, CnpcGuiSupport.window(H),
                (pl, gui) -> paint(pl, gui, p, staffAdminBrowser));
    }

    private static void paint(ServerPlayer player, ICustomGui gui, String page, boolean staffAdminBrowser) {
        boolean staffAdmin = staffAdminBrowser;
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                staffAdmin ? CnpcGuiStyle.subPage(CnpcUltraStyle.INFO, "Skills", "Staff") : CnpcUltraStyle.INFO + "Skill Check",
                CnpcUltraStyle.SUBTITLE + "Level, locked, unlocked, or max.");

        List<String> lines = ProgressionGuiApi.skillsLines(CnpcGuiSupport.target(player), "core");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, CnpcGuiStyle.INFO_INLINE_MAX));
        // Natural skills and saga skills share this one Skills tab.
        CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.INFO + "Skills", CnpcGuiSupport.COL_L, row, () -> {
            if (staffAdmin) {
                com.dbzlegacy.adaptivedifficulty.gui.SkillsMenu.open(player, "core");
            } else {
                SkillCheckService.open(player, "core");
                show(player, "core", false);
            }
        });
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.navSystemRoot(player, gui, row);
        CnpcGuiSupport.paintSystemMainPreview(CnpcGuiSupport.target(player), gui, player);
    }
}
