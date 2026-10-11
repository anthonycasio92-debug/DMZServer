package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

/**
 * Staff shortcut menu for {@code /lm admin testgui}.
 * Head parts and Reskin sit at the top. Every other button opens that
 * system's own main page. Hub on the screen opens the main hub.
 */
public final class CnpcStyledTestHub {
    private CnpcStyledTestHub() {}

    public static void open(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int links = 10;
        if (SkillCheckService.canUse(player)) {
            links++;
        }
        int height = CnpcGuiSupport.suggestHeight(
                88 + 16 + links * (CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP) + 28);
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_HUB, CnpcGuiSupport.W, height,
                (player1, gui) -> CnpcUltraStyle.withAccent(CnpcUltraStyle.ACCENT_CONFIG,
                        () -> paint(player1, gui)));
    }

    private static void paint(ServerPlayer player, ICustomGui gui) {
        int y = CnpcGuiSupport.paintHeader(player, gui, CnpcUltraStyle.header("Test menu"),
                CnpcUltraStyle.subtitle("Each button opens that screen."));
        y = CnpcGuiSupport.bodyBelowInfo(y);
        int id = CnpcGuiSupport.ID_GRID_BASE;
        y = link(gui, y, id++, CnpcUltraStyle.ACCENT_CHARACTER, "Model customization", () -> CnpcLmCharacterGui.open(player, "bones:0"));
        y = link(gui, y, id++, CnpcUltraStyle.ACCENT_CHARACTER, "Reskin", () -> CnpcLmCharacterGui.open(player, "reskin"));
        y = CnpcGuiSupport.paintSectionTag(gui, CnpcGuiSupport.ID_STATUS_TAG, y, "Current menus");
        y = link(gui, y, id++, CnpcUltraStyle.ACCENT_DIFFICULTY, "Difficulty", () -> CnpcLmDifficultyGui.open(player, "main"));
        y = link(gui, y, id++, CnpcUltraStyle.ACCENT_RIVAL, "Rival", () -> CnpcLmRivalGui.open(player, "main"));
        y = link(gui, y, id++, CnpcUltraStyle.ACCENT_SPARRING, "Sparring", () -> CnpcLmSparGui.open(player, "main"));
        y = link(gui, y, id++, CnpcUltraStyle.ACCENT_PROGRESSION, "Progression", () -> CnpcLmProgressionGui.open(player, "main"));
        y = link(gui, y, id++, CnpcUltraStyle.ACCENT_PRESTIGE, "Prestige", () -> CnpcLmPrestigeGui.open(player, "main"));
        y = link(gui, y, id++, CnpcUltraStyle.ACCENT_ADMIN, "Staff Admin", () -> CnpcLmAdminGui.open(player, "main"));
        if (SkillCheckService.canUse(player)) {
            y = link(gui, y, id++, CnpcUltraStyle.ACCENT_ADMIN, "Skill Check", () -> CnpcLmSkillCheckGui.open(player, "main"));
        }
        y = link(gui, y, id++, CnpcUltraStyle.ACCENT_ADMIN, "Skills", () -> CnpcLmSkillCheckGui.openSkillsAdmin(player, "core"));
        y = link(gui, y, id, CnpcUltraStyle.ACCENT_SAGA, "Saga", () -> CnpcLmSagaGui.open(player, "main"));
        CnpcGuiSupport.navSystemRoot(player, gui, y);
        CnpcGuiSupport.paintSystemMainPreview(CnpcGuiSupport.target(player), gui, player);
    }

    /** Opens that screen. Does not send Hub back here. */
    private static int link(ICustomGui gui, int y, int id, String accent, String label, Runnable open) {
        CnpcGuiSupport.buttonSmallFull(gui, id, accent + label,
                CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(),
                open);
        return y + CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
    }
}
