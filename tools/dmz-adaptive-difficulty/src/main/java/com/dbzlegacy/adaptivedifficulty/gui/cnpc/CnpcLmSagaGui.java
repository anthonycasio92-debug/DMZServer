package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetConfig;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetService;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetService.Offer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

/** Character Services page for replaying one loaded saga. */
public final class CnpcLmSagaGui {
    private static final int ROWS = 6;

    private CnpcLmSagaGui() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "saga" : page;
        if (target.regionMatches(true, 0, "confirm:", 0, "confirm:".length())) {
            CnpcLmCharacterGui.open(player, "saga_confirm:" + target.substring("confirm:".length()));
            return;
        }
        if (target.regionMatches(true, 0, "saga_confirm:", 0, "saga_confirm:".length())) {
            openConfirm(player, target.substring("saga_confirm:".length()));
            return;
        }
        if ("saga".equalsIgnoreCase(target) || target.regionMatches(true, 0, "saga:", 0, "saga:".length())) {
            openList(player, target);
            return;
        }
        CnpcLmCharacterGui.open(player, "saga");
    }

    static void openList(ServerPlayer player, String page) {
        CnpcUltraStyle.withAccent(CnpcUltraStyle.ACCENT_SAGA, () -> openListAccent(player, page));
    }

    private static void openListAccent(ServerPlayer player, String page) {
        List<Offer> offers = SagaResetService.offers(player);
        int pages = Math.max(1, (offers.size() + ROWS - 1) / ROWS);
        int pg = Math.min(pages - 1, Math.max(0, parsePage(page)));
        int shown = offers.isEmpty() ? 1 : Math.min(ROWS, offers.size() - pg * ROWS);
        int pager = pages > 1 ? 1 : 0;
        int designed = 188 + shown * (CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP)
                + pager * CnpcGuiSupport.ROW_STEP + CnpcGuiSupport.ROW_STEP;
        int pageIndex = pg;
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W,
                CnpcGuiSupport.window(designed), (pl, gui) -> paintList(pl, gui, pageIndex));
    }

    static void openConfirm(ServerPlayer player, String sagaId) {
        CnpcUltraStyle.withAccent(CnpcUltraStyle.ACCENT_SAGA, () -> openConfirmAccent(player, sagaId));
    }

    private static void openConfirmAccent(ServerPlayer player, String sagaId) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W,
                CnpcGuiSupport.window(280), (pl, gui) -> paintConfirm(pl, gui, sagaId));
    }

    private static void paintList(ServerPlayer player, ICustomGui gui, int page) {
        List<Offer> offers = SagaResetService.offers(player);
        int pages = Math.max(1, (offers.size() + ROWS - 1) / ROWS);
        int pg = Math.min(pages - 1, Math.max(0, page));
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcUltraStyle.header("Saga reset"),
                CnpcUltraStyle.subtitle("Pay Ancient Coins to replay one saga"));
        infoY = CnpcLmCharacterGui.characterTabs(player, gui, infoY, "saga");
        List<String> info = new ArrayList<>();
        if (!SagaResetConfig.get().enabled) {
            info.add("Saga reset is turned off. Ask staff to turn it on if you need to replay a saga.");
        } else {
            info.add("You have " + AncientCoinEconomy.inventoryBreakdown(player));
            info.add("Cost is the saga base plus your level. A saga that is not listed uses the default base.");
        }
        int y = CnpcGuiSupport.bodyBelowInfo(
                CnpcGuiSupport.paintInfoBeforePickList(gui, infoY, info, 2));
        y = CnpcRowList.paintRow(gui, 184, y, "Loaded sagas",
                offers.isEmpty() ? "None loaded" : offers.size() + " loaded", null);

        int rowStep = CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
        if (!SagaResetConfig.get().enabled) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    CnpcUltraStyle.subtitle("Saga reset is turned off. Ask staff to turn it on if you need to replay a saga."),
                    CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(), 14);
            y += rowStep;
        } else if (offers.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    CnpcUltraStyle.subtitle("No sagas are loaded. Ask staff to load one, then open this page again."),
                    CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(), 14);
            y += rowStep;
        } else {
            int from = pg * ROWS;
            int to = Math.min(offers.size(), from + ROWS);
            for (int i = from; i < to; i++) {
                Offer offer = offers.get(i);
                int slot = i - from;
                CnpcGuiSupport.buttonSmallFull(gui, CnpcGuiSupport.ID_GRID_BASE + slot, caption(offer),
                        CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(),
                        () -> click(player, offer, pg));
                y += rowStep;
            }
        }
        if (pages > 1) {
            if (pg > 0) {
                int previous = pg - 1;
                CnpcGuiSupport.buttonSmall(gui, 62, CnpcUltraStyle.SUBTITLE + "« Previous",
                        CnpcGuiSupport.COL_L, y, 95, () -> openList(player, "saga:" + previous));
            }
            if (pg + 1 < pages) {
                int next = pg + 1;
                CnpcGuiSupport.buttonSmall(gui, 63, CnpcUltraStyle.SUBTITLE + "Next »",
                        CnpcGuiSupport.COL_R, y, 95, () -> openList(player, "saga:" + next));
            }
            y += CnpcGuiSupport.ROW_STEP;
        }
        CnpcGuiSupport.navSubmenu(player, gui, y, () -> CnpcLmCharacterGui.open(player, "bones:0"), CnpcUltraStyle.BACK);
    }

    private static void paintConfirm(ServerPlayer player, ICustomGui gui, String sagaId) {
        Offer offer = SagaResetService.findOffer(player, sagaId);
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcUltraStyle.header("Confirm reset"),
                CnpcUltraStyle.subtitle("Your saga progress will be lost"));
        List<String> lines = SagaResetService.confirmLines(player, sagaId);
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 4));
        boolean ready = offer != null && offer.resettable() && SagaResetConfig.get().enabled;
        CnpcGuiSupport.button(gui, 20, ready ? CnpcUltraStyle.CONFIRM + "Confirm" : CnpcUltraStyle.DIM + "Confirm",
                CnpcGuiSupport.COL_L, row,
                () -> {
                    if (!ready) {
                        String notice = offer == null ? "That saga is not loaded."
                                : offer.status() == SagaResetService.Status.LOCKED ? "That saga is locked."
                                : offer.status() == SagaResetService.Status.NOT_STARTED ? "Nothing to reset"
                                : "Saga reset is turned off.";
                        CnpcGuiSupport.feedback(player, notice);
                        openList(player, "saga");
                        return;
                    }
                    CnpcGuiSupport.act(
                            player,
                            () -> SagaResetService.reset(player, offer.id()),
                            () -> openList(player, "saga"));
                });
        row += CnpcGuiSupport.ROW_STEP + 8;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> openList(player, "saga"), CnpcUltraStyle.BACK);
    }

    private static void click(ServerPlayer player, Offer offer, int page) {
        if (offer == null) {
            openList(player, "saga:" + page);
            return;
        }
        if (!offer.resettable()) {
            String notice = offer.status() == SagaResetService.Status.LOCKED
                    ? "That saga is locked."
                    : "Nothing to reset";
            CnpcGuiSupport.feedback(player, notice);
            openList(player, "saga:" + page);
            return;
        }
        CnpcLmCharacterGui.open(player, "saga_confirm:" + offer.id());
    }

    private static String caption(Offer offer) {
        String name = offer.name();
        String detail = offer.rowDetail();
        if (offer.resettable()) {
            return CnpcUltraStyle.cardTitle(name) + "  " + CnpcUltraStyle.cardBody(detail).trim();
        }
        return CnpcUltraStyle.DIM + "▸ " + CnpcUltraStyle.plain(name) + "  " + CnpcUltraStyle.subtitle(detail);
    }

    private static int parsePage(String page) {
        if (page == null) {
            return 0;
        }
        String p = page.toLowerCase(Locale.ROOT);
        if (!p.startsWith("saga:")) {
            return 0;
        }
        try {
            return Integer.parseInt(p.substring("saga:".length()).trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
