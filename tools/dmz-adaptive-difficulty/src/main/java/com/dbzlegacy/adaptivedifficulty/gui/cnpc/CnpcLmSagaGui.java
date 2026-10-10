package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetConfig;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IScroll;

/** Player menu for replaying one loaded saga. */
public final class CnpcLmSagaGui {
    private CnpcLmSagaGui() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        CnpcUltraPreview.leave(player);
        String target = page == null || page.isBlank() ? "main" : page;
        if (target.regionMatches(true, 0, "confirm:", 0, "confirm:".length())) {
            paintConfirm(player, target.substring("confirm:".length()));
            return;
        }
        paintMain(player);
    }

    private static void paintMain(ServerPlayer player) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SAGA, CnpcGuiSupport.W,
                CnpcGuiSupport.window(420), (pl, gui) -> paintMain(pl, gui));
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage("§6", "Saga", "Reset"),
                CnpcGuiStyle.HINT_CLICK_ENTRY);
        List<String> info = new ArrayList<>();
        if (!SagaResetConfig.get().enabled) {
            info.add("Saga reset is turned off.");
        } else {
            info.add("You have " + AncientCoinEconomy.inventoryBreakdown(player));
            info.add("A saga that is not listed in config uses the default cost.");
        }
        int listY = CnpcGuiSupport.bodyBelowInfo(
                CnpcGuiSupport.paintInfoBeforePickList(gui, infoY, info, 2));
        List<String> cards = SagaResetService.cards(player);
        String[] labels = labels(cards);
        boolean canReset = SagaResetConfig.get().enabled && !cards.isEmpty();
        int rowsBelow = canReset ? 2 : 1;
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        if (canReset) {
            CnpcGuiSupport.selectionButton(player, gui, 30, "§eReset this saga",
                    CnpcGuiSupport.COL_L, row, CnpcGuiSupport.BTN_W,
                    () -> selectedId(cards, scroll),
                    id -> open(player, "confirm:" + id),
                    () -> open(player, "main"));
            row += CnpcGuiSupport.ROW_STEP;
        }
        CnpcGuiSupport.navSystemRoot(player, gui, row);
    }

    private static void paintConfirm(ServerPlayer player, String sagaId) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SAGA, CnpcGuiSupport.W,
                CnpcGuiSupport.window(320), (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui,
                    CnpcGuiStyle.subPage("§6", "Saga", "Confirm reset"),
                    CnpcGuiStyle.HINT_REVIEW_PAY);
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintReadOnlyScroll(
                    gui, infoY, SagaResetService.confirmLines(player, sagaId)));
            CnpcGuiSupport.button(gui, 20, "§aConfirm and pay", CnpcGuiSupport.COL_L, row,
                    () -> CnpcGuiSupport.act(
                            player,
                            () -> SagaResetService.reset(player, sagaId),
                            () -> open(player, "main")));
            row += CnpcGuiSupport.ROW_STEP + 8;
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "main"), "§7« Back");
        });
    }

    private static String[] labels(List<String> cards) {
        if (cards == null || cards.isEmpty()) {
            return new String[] {"No sagas are loaded."};
        }
        String[] labels = new String[cards.size()];
        for (int i = 0; i < cards.size(); i++) {
            String[] parts = cards.get(i).split("\t", -1);
            String name = parts.length > 1 ? parts[1] : parts[0];
            String started = parts.length > 2 ? parts[2] : "0";
            String total = parts.length > 3 ? parts[3] : "0";
            String cost = "free";
            if (parts.length > 4) {
                try {
                    cost = AncientCoinEconomy.formatExactCost(Long.parseLong(parts[4]));
                } catch (NumberFormatException ignored) {
                    cost = parts[4];
                }
            }
            labels[i] = name + CnpcGuiStyle.SEP + started + "/" + total + CnpcGuiStyle.SEP + cost;
        }
        return labels;
    }

    private static String selectedId(List<String> cards, IScroll scroll) {
        if (cards == null || cards.isEmpty() || scroll == null) {
            return null;
        }
        int[] sel = scroll.getSelection();
        if (sel == null || sel.length == 0 || sel[0] < 0 || sel[0] >= cards.size()) {
            return null;
        }
        String[] parts = cards.get(sel[0]).split("\t", -1);
        return parts.length > 0 && !parts[0].isBlank() ? parts[0] : null;
    }
}
