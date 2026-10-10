package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import java.util.List;
import noppes.npcs.api.gui.ICustomGui;

/**
 * List rows with a title, a gray status line, and a 6px gap.
 * Scroll pick-lists stay one entry per card so the selected index does not move.
 */
public final class CnpcRowList {
    public static final int ROW_GAP = 6;

    private CnpcRowList() {}

    /**
     * Paints {@code color e▸ title} and {@code color 7 subtitle}, then leaves {@link #ROW_GAP} pixels.
     * Returns the Y of the next row. {@code action} is the existing click id, recorded on the title.
     */
    public static int paintRow(ICustomGui gui, int labelId, int y, String title, String subtitle, String action) {
        int width = CnpcGuiSupport.textBandWidth();
        String shown = CnpcUltraStyle.cardTitle(title);
        if (action != null && !action.isBlank()) {
            shown = shown + " " + CnpcUltraStyle.DIM + "· " + CnpcUltraStyle.SUBTITLE + CnpcUltraStyle.plain(action);
        }
        gui.addLabel(labelId, shown, CnpcGuiSupport.M, y, width, 12);
        int next = y + CnpcGuiSupport.LINE_H;
        if (subtitle != null && !subtitle.isBlank()) {
            gui.addLabel(labelId + 1, CnpcUltraStyle.cardBody(subtitle), CnpcGuiSupport.M, next, width, 12);
            next += CnpcGuiSupport.LINE_H;
        }
        return next + ROW_GAP;
    }

    /** Head-part scroll text. One string per card, same order as {@code cards}. */
    public static String[] headBoneRows(List<String> cards) {
        if (cards == null || cards.isEmpty()) {
            return new String[0];
        }
        String[] rows = new String[cards.size()];
        for (int i = 0; i < cards.size(); i++) {
            String[] parts = cards.get(i).split("\t", -1);
            String name = parts.length > 1 ? parts[1] : parts[0];
            String state = parts.length > 2 ? parts[2] : "";
            String cost = parts.length > 3 ? parts[3] : "";
            String status = switch (state) {
                case "E" -> "Equipped";
                case "U" -> "Unlocked";
                case "N" -> "Included with your race";
                case "L" -> cost == null || cost.isBlank() ? "Locked" : "Locked · " + cost;
                default -> "";
            };
            rows[i] = status.isEmpty()
                    ? CnpcUltraStyle.cardTitle(name)
                    : CnpcUltraStyle.cardTitle(name) + "  " + CnpcUltraStyle.cardBody(status).trim();
        }
        return rows;
    }
}
