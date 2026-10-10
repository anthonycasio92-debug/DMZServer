package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesAccess;
import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesConfig;
import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

/**
 * Staff shortcut menu for {@code /lm admin testgui}.
 * Each button opens that screen. Hub on the screen opens the main hub.
 */
public final class CnpcStyledTestHub {
    private CnpcStyledTestHub() {}

    public static void open(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int buttons = 8;
        if (headParts(player)) {
            buttons++;
        }
        if (SkillCheckService.canUse(player)) {
            buttons++;
        }
        int height = CnpcGuiSupport.suggestHeight(
                88 + buttons * (CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP) + 28);
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_HUB, CnpcGuiSupport.W, height,
                CnpcStyledTestHub::paint);
    }

    private static void paint(ServerPlayer player, ICustomGui gui) {
        int y = CnpcGuiSupport.paintHeader(player, gui, CnpcUltraStyle.header("Test menu"),
                CnpcUltraStyle.subtitle("Each button opens that screen."));
        y = CnpcGuiSupport.bodyBelowInfo(y);
        int id = CnpcGuiSupport.ID_GRID_BASE;
        y = link(gui, player, y, id++, "Difficulty", "difficulty", "main");
        y = link(gui, player, y, id++, "Rival", "rival", "main");
        y = link(gui, player, y, id++, "Sparring", "spar", "main");
        y = link(gui, player, y, id++, "Prestige", "prestige", "main");
        y = link(gui, player, y, id++, "Character", "character", "main");
        if (headParts(player)) {
            y = link(gui, player, y, id++, "Head parts", "character", "bones:0");
        }
        if (SkillCheckService.canUse(player)) {
            y = link(gui, player, y, id++, "Skill Check", "skillcheck", "main");
        }
        y = link(gui, player, y, id++, "Skills", "skills", "core");
        y = link(gui, player, y, id++, "Staff Admin", "admin", "main");
        y = link(gui, player, y, id, "Saga Reset", "saga", "main");
        CnpcGuiSupport.footerCloseRefresh(player, gui, y, () -> open(player));
        CnpcGuiSupport.paintSystemMainPreview(CnpcGuiSupport.target(player), gui, player);
    }

    /** Opens that screen. Does not send Hub back here. */
    private static int link(ICustomGui gui, ServerPlayer player, int y, int id, String label,
            String system, String page) {
        CnpcGuiSupport.buttonSmallFull(gui, id, CnpcUltraStyle.BODY + label,
                CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(),
                () -> CnpcLmGui.open(player, system, page));
        return y + CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
    }

    private static boolean headParts(ServerPlayer player) {
        return CharacterServicesConfig.get().headBoneShop.enabled
                && CharacterServicesAccess.canHeadBoneShop(player);
    }
}
