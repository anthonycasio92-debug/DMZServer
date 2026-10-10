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

/** Saga menu. Reset is its own page, not a Character Services tab. */
public final class CnpcLmSagaGui {
    private static final int ROWS = 6;

    private CnpcLmSagaGui() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page.trim();
        String lower = target.toLowerCase(Locale.ROOT);
        if (lower.startsWith("confirm:")) {
            openConfirm(player, target.substring("confirm:".length()));
            return;
        }
        if (lower.startsWith("saga_confirm:")) {
            openConfirm(player, target.substring("saga_confirm:".length()));
            return;
        }
        if (isResetPage(lower)) {
            openReset(player, lower);
            return;
        }
        openHome(player, lower);
    }

    static void openList(ServerPlayer player, String page) {
        open(player, page == null || page.isBlank() ? "reset" : page);
    }

    private static boolean isResetPage(String page) {
        return "reset".equals(page)
                || page.startsWith("reset:")
                || "saga".equals(page)
                || page.startsWith("saga:");
    }

    private static void openHome(ServerPlayer player, String page) {
        CnpcUltraStyle.withAccent(CnpcUltraStyle.ACCENT_SAGA, () -> {
            List<Offer> offers = SagaResetService.offers(player);
            int pages = Math.max(1, (offers.size() + ROWS - 1) / ROWS);
            int pg = Math.min(pages - 1, Math.max(0, parsePage(page, "sagas:")));
            int shown = offers.isEmpty() ? 1 : Math.min(ROWS, offers.size() - pg * ROWS);
            int pager = pages > 1 ? 1 : 0;
            int designed = 168 + CnpcGuiSupport.TAB_BAR_H
                    + shown * (CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP)
                    + pager * CnpcGuiSupport.ROW_STEP + CnpcGuiSupport.ROW_STEP * 2;
            int pageIndex = pg;
            CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SAGA, CnpcGuiSupport.W,
                    CnpcGuiSupport.window(designed), (pl, gui) -> paintHome(pl, gui, pageIndex));
        });
    }

    private static void openReset(ServerPlayer player, String page) {
        CnpcUltraStyle.withAccent(CnpcUltraStyle.ACCENT_SAGA, () -> {
            List<Offer> offers = SagaResetService.offers(player);
            int pages = Math.max(1, (offers.size() + ROWS - 1) / ROWS);
            int pg = Math.min(pages - 1, Math.max(0, resetPage(page)));
            int shown = offers.isEmpty() ? 1 : Math.min(ROWS, offers.size() - pg * ROWS);
            int pager = pages > 1 ? 1 : 0;
            int designed = 188 + CnpcGuiSupport.TAB_BAR_H
                    + shown * (CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP)
                    + pager * CnpcGuiSupport.ROW_STEP + CnpcGuiSupport.ROW_STEP;
            int pageIndex = pg;
            CnpcGuiSupport.showSized(player, CnpcLmGui.ID_SAGA, CnpcGuiSupport.W,
                    CnpcGuiSupport.window(designed), (pl, gui) -> paintReset(pl, gui, pageIndex));
        });
    }

    static void openConfirm(ServerPlayer player, String sagaId) {
        CnpcUltraStyle.withAccent(CnpcUltraStyle.ACCENT_SAGA, () -> CnpcGuiSupport.showSized(
                player, CnpcLmGui.ID_SAGA, CnpcGuiSupport.W,
                CnpcGuiSupport.window(280), (pl, gui) -> paintConfirm(pl, gui, sagaId)));
    }

    private static int sagaTabs(ServerPlayer player, ICustomGui gui, int y, String active) {
        return CnpcGuiSupport.paintTabBar(gui, y, new String[] {
                "sagas|Sagas", "reset|Reset"
        }, active, action -> {
            String id = action.startsWith("tab:") ? action.substring(4) : action;
            if ("reset".equals(id)) {
                open(player, "reset");
            } else {
                open(player, "main");
            }
        });
    }

    private static void paintHome(ServerPlayer player, ICustomGui gui, int page) {
        List<Offer> offers = SagaResetService.offers(player);
        int pages = Math.max(1, (offers.size() + ROWS - 1) / ROWS);
        int pg = Math.min(pages - 1, Math.max(0, page));
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcUltraStyle.header("Saga"),
                CnpcUltraStyle.subtitle("Loaded story progress"));
        infoY = sagaTabs(player, gui, infoY, "sagas");
        List<String> info = new ArrayList<>();
        info.add(offers.isEmpty()
                ? "No sagas are loaded. Ask staff to load one, then open this page again."
                : offers.size() + " loaded. Reset saga progress replays one.");
        int y = CnpcGuiSupport.bodyBelowInfo(
                CnpcGuiSupport.paintInfoBeforePickList(gui, infoY, info, 1));
        y = CnpcRowList.paintRow(gui, 184, y, "Sagas",
                offers.isEmpty() ? "None loaded" : offers.size() + " loaded", null);
        y = paintOfferRows(gui, y, offers, pg);
        if (pages > 1) {
            if (pg > 0) {
                int previous = pg - 1;
                CnpcGuiSupport.buttonSmall(gui, 62, CnpcUltraStyle.SUBTITLE + "« Previous",
                        CnpcGuiSupport.COL_L, y, 95, () -> open(player, "sagas:" + previous));
            }
            if (pg + 1 < pages) {
                int next = pg + 1;
                CnpcGuiSupport.buttonSmall(gui, 63, CnpcUltraStyle.SUBTITLE + "Next »",
                        CnpcGuiSupport.COL_R, y, 95, () -> open(player, "sagas:" + next));
            }
            y += CnpcGuiSupport.ROW_STEP;
        }
        CnpcGuiSupport.buttonSmallFull(gui, 66, CnpcUltraStyle.ACCENT_SAGA + "Reset saga progress",
                CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(),
                () -> open(player, "reset"));
        y += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.navSystemRoot(player, gui, y);
    }

    private static void paintReset(ServerPlayer player, ICustomGui gui, int page) {
        List<Offer> offers = SagaResetService.offers(player);
        int pages = Math.max(1, (offers.size() + ROWS - 1) / ROWS);
        int pg = Math.min(pages - 1, Math.max(0, page));
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcUltraStyle.header("Reset"),
                CnpcUltraStyle.subtitle("Pay Ancient Coins to replay one saga"));
        infoY = sagaTabs(player, gui, infoY, "reset");
        List<String> info = new ArrayList<>();
        if (!SagaResetConfig.get().enabled) {
            info.add(SagaResetService.RESET_OFF);
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
                        CnpcGuiSupport.COL_L, y, 95, () -> open(player, "reset:" + previous));
            }
            if (pg + 1 < pages) {
                int next = pg + 1;
                CnpcGuiSupport.buttonSmall(gui, 63, CnpcUltraStyle.SUBTITLE + "Next »",
                        CnpcGuiSupport.COL_R, y, 95, () -> open(player, "reset:" + next));
            }
            y += CnpcGuiSupport.ROW_STEP;
        }
        CnpcGuiSupport.navSubmenu(player, gui, y, () -> open(player, "main"), CnpcUltraStyle.BACK);
    }

    private static int paintOfferRows(ICustomGui gui, int y, List<Offer> offers, int page) {
        int rowStep = CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
        if (offers.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER,
                    CnpcUltraStyle.subtitle("No sagas are loaded. Ask staff to load one, then open this page again."),
                    CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(), 14);
            return y + rowStep;
        }
        int from = page * ROWS;
        int to = Math.min(offers.size(), from + ROWS);
        for (int i = from; i < to; i++) {
            Offer offer = offers.get(i);
            gui.addLabel(CnpcGuiSupport.ID_GRID_BASE + (i - from), statusLine(offer),
                    CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(), 14);
            y += rowStep;
        }
        return y;
    }

    private static String statusLine(Offer offer) {
        String status = switch (offer.status()) {
            case COMPLETED -> CnpcUltraStyle.CONFIRM + offer.statusLabel();
            case IN_PROGRESS -> CnpcUltraStyle.INFO + offer.statusLabel();
            default -> CnpcUltraStyle.DIM + offer.statusLabel();
        };
        return CnpcUltraStyle.BODY + offer.name() + "  " + status;
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
                        String notice = offer == null ? SagaResetService.NOT_LOADED
                                : offer.status() == SagaResetService.Status.LOCKED ? SagaResetService.SAGA_LOCKED
                                : offer.status() == SagaResetService.Status.NOT_STARTED ? SagaResetService.NOTHING_TO_RESET
                                : SagaResetService.RESET_OFF;
                        CnpcGuiSupport.feedback(player, notice);
                        open(player, "reset");
                        return;
                    }
                    CnpcGuiSupport.act(
                            player,
                            () -> SagaResetService.reset(player, offer.id()),
                            () -> open(player, "reset"));
                });
        row += CnpcGuiSupport.ROW_STEP + 8;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "reset"), CnpcUltraStyle.BACK);
    }

    private static void click(ServerPlayer player, Offer offer, int page) {
        if (offer == null) {
            open(player, "reset:" + page);
            return;
        }
        if (!offer.resettable()) {
            String notice = offer.status() == SagaResetService.Status.LOCKED
                    ? SagaResetService.SAGA_LOCKED
                    : SagaResetService.NOTHING_TO_RESET;
            CnpcGuiSupport.feedback(player, notice);
            open(player, "reset:" + page);
            return;
        }
        openConfirm(player, offer.id());
    }

    private static String caption(Offer offer) {
        String name = offer.name();
        String detail = offer.rowDetail();
        if (offer.resettable()) {
            return CnpcUltraStyle.cardTitle(name) + "  " + CnpcUltraStyle.cardBody(detail).trim();
        }
        return CnpcUltraStyle.DIM + "▸ " + CnpcUltraStyle.plain(name) + "  " + CnpcUltraStyle.subtitle(detail);
    }

    private static int resetPage(String page) {
        if (page.startsWith("reset:")) {
            return parsePage(page, "reset:");
        }
        if (page.startsWith("saga:")) {
            return parsePage(page, "saga:");
        }
        return 0;
    }

    private static int parsePage(String page, String prefix) {
        if (page == null || !page.startsWith(prefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(page.substring(prefix.length()).trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
