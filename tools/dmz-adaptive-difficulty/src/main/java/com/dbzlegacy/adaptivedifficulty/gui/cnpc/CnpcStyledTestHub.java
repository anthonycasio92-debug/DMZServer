package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

/**
 * Staff test menu for {@code /lm admin testgui}.
 * Head parts uses the new colors. Every other button opens the current menu.
 */
public final class CnpcStyledTestHub {
    private static final int LABEL_HEAD = 180;
    private static final int LABEL_CURRENT = 182;

    private CnpcStyledTestHub() {}

    public static void open(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int height = CnpcGuiSupport.suggestHeight(400);
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_HUB, CnpcGuiSupport.W, height,
                CnpcStyledTestHub::paint);
    }

    private static void paint(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, CnpcGuiSupport.ID_TITLE, CnpcUltraStyle.header("Test menu"));
        CnpcGuiSupport.subtitle(gui, CnpcGuiSupport.ID_SUBTITLE,
                CnpcUltraStyle.subtitle("/lm still opens the current menus."));
        int chars = Math.max(14, CnpcGuiSupport.textBandWidth() / 7);
        gui.addLabel(CnpcGuiSupport.ID_DIVIDER, CnpcUltraStyle.DIVIDER + "─".repeat(chars),
                CnpcGuiSupport.M, 38, CnpcGuiSupport.textBandWidth(), 10);

        int y = 52;
        y = CnpcRowList.paintRow(gui, LABEL_HEAD, y, "Head parts",
                "New colors. Turn several parts on at once.", null);
        CnpcGuiSupport.buttonSmallFull(gui, CnpcGuiSupport.ID_GRID_BASE,
                CnpcUltraStyle.ACCENT + "Head parts",
                CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(),
                () -> {
                    CnpcUltraPreview.enter(player);
                    CnpcLmCharacterGui.open(player, "bones:0");
                });
        y += CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;

        y = CnpcRowList.paintRow(gui, LABEL_CURRENT, y, "Current menus",
                "These still open the menus /lm uses.", null);
        y = currentButton(gui, player, y, CnpcGuiSupport.ID_GRID_BASE + 1, "Difficulty", "difficulty", "main");
        y = currentButton(gui, player, y, CnpcGuiSupport.ID_GRID_BASE + 2, "Rival", "rival", "main");
        y = currentButton(gui, player, y, CnpcGuiSupport.ID_GRID_BASE + 3, "Sparring", "spar", "main");
        y = currentButton(gui, player, y, CnpcGuiSupport.ID_GRID_BASE + 4, "Prestige", "prestige", "main");
        y = currentButton(gui, player, y, CnpcGuiSupport.ID_GRID_BASE + 5, "Character", "character", "main");
        if (SkillCheckService.canUse(player)) {
            y = currentButton(gui, player, y, CnpcGuiSupport.ID_GRID_BASE + 6, "Skill Check", "skillcheck", "main");
        } else {
            y = currentButton(gui, player, y, CnpcGuiSupport.ID_GRID_BASE + 6, "Skills", "skills", "core");
        }
        y = currentButton(gui, player, y, CnpcGuiSupport.ID_GRID_BASE + 7, "Staff Admin", "admin", "main");

        CnpcGuiSupport.footerCloseRefresh(player, gui, y, () -> open(player));
        CnpcGuiSupport.paintSystemMainPreview(CnpcGuiSupport.target(player), gui, player);
    }

    /** Leave the test colors, then open the menu {@code /lm} already uses. */
    private static int currentButton(ICustomGui gui, ServerPlayer player, int y, int id, String label,
            String system, String page) {
        CnpcGuiSupport.buttonSmallFull(gui, id, CnpcUltraStyle.BODY + label,
                CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(),
                () -> {
                    CnpcUltraPreview.leave(player);
                    CnpcLmGui.open(player, system, page);
                });
        return y + CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
    }
}
