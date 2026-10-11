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
        CnpcUltraStyle.withAccent(CnpcUltraStyle.ACCENT_ADMIN, () ->
                CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SKILLCHECK, CnpcGuiSupport.W, CnpcGuiSupport.window(H),
                        (pl, gui) -> paint(pl, gui, p, staffAdminBrowser)));
    }

    private static void paint(ServerPlayer player, ICustomGui gui, String page, boolean staffAdminBrowser) {
        boolean staffAdmin = staffAdminBrowser;
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                staffAdmin ? CnpcGuiStyle.subPage(CnpcUltraStyle.BODY, "Skills", "Staff") : "Skill Check",
                CnpcUltraStyle.SUBTITLE + "Level, locked, unlocked, or max.");

        // Natural skills and saga skills share this one Skills tab.
        List<String> lines = ProgressionGuiApi.skillsLines(CnpcGuiSupport.target(player), page);
        int row;
        if (skillListMissing(lines)) {
            row = CnpcGuiSupport.bodyBelowInfo(infoY);
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    CnpcUltraStyle.subtitle("No skills are listed. Ask staff to turn skill unlock on, then open this page again."),
                    CnpcGuiSupport.M, row, CnpcGuiSupport.textBandWidth(), 14);
            row += CnpcGuiSupport.ROW_STEP;
        } else {
            row = CnpcGuiSupport.bodyBelowInfo(
                    CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, CnpcGuiStyle.INFO_INLINE_MAX));
        }
        CnpcGuiSupport.navBackToMainMenu(player, gui, row,
                CnpcUltraStyle.SUBTITLE + "« Back to main menu");
        CnpcGuiSupport.paintSystemMainPreview(CnpcGuiSupport.target(player), gui, player);
    }

    /** A one-line service error is not a skill list. */
    private static boolean skillListMissing(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return true;
        }
        if (lines.size() != 1) {
            return false;
        }
        String plain = CnpcUltraStyle.plain(lines.get(0)).toLowerCase(java.util.Locale.ROOT);
        return plain.contains("error") || plain.contains("disabled");
    }
}
