package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;

/** Two-column button grids aligned to {@link CnpcGuiSupport#COL_L} / {@link CnpcGuiSupport#COL_R}. */
public final class CnpcGuiLayout {
    private CnpcGuiLayout() {}

    public static int columnX(int index) {
        return (index % 2 == 0) ? CnpcGuiSupport.COL_L : CnpcGuiSupport.COL_R;
    }

    public static int rowY(int startY, int index) {
        return startY + (index / 2) * CnpcGuiSupport.ROW_STEP;
    }

    /** Y coordinate below the last row of a grid with {@code itemCount} entries. */
    public static int belowGrid(int startY, int itemCount) {
        if (itemCount <= 0) {
            return startY + 4;
        }
        int rows = (itemCount + 1) / 2;
        return startY + rows * CnpcGuiSupport.ROW_STEP + 8;
    }

    /**
     * Paints a 2-column grid of full-width column buttons ({@link CnpcGuiSupport#BTN_W}).
     * Returns Y below the grid.
     */
    public static int paintTwoColumnButtonGrid(
            ServerPlayer player,
            ICustomGui gui,
            int startY,
            int idBase,
            GridButton[] buttons,
            Runnable reopen
    ) {
        if (buttons == null || buttons.length == 0) {
            return startY + 4;
        }
        for (int i = 0; i < buttons.length; i++) {
            GridButton b = buttons[i];
            if (b == null) {
                continue;
            }
            int x = columnX(i);
            int y = rowY(startY, i);
            int id = idBase + i;
            if (b.clickable() && b.messageAction() != null) {
                Runnable after = b.reopenAction() != null ? b.reopenAction() : reopen;
                CnpcGuiSupport.buttonSmallFull(gui, id, b.label(), x, y, CnpcGuiSupport.BTN_W, () ->
                        CnpcGuiSupport.act(player, b.messageAction(), after));
            } else if (b.clickable() && b.directAction() != null) {
                CnpcGuiSupport.buttonSmallFull(gui, id, b.label(), x, y, CnpcGuiSupport.BTN_W, b.directAction());
            } else {
                gui.addLabel(id, CnpcGuiSupport.safeChat(b.label()), x, y + 4, CnpcGuiSupport.BTN_W, 14);
            }
        }
        return belowGrid(startY, buttons.length);
    }

    public record GridButton(
            String label,
            Supplier<String> messageAction,
            Runnable directAction,
            Runnable reopenAction,
            boolean clickable
    ) {
        public static GridButton action(String label, Supplier<String> messageAction, Runnable reopen) {
            return new GridButton(label, messageAction, null, reopen, true);
        }

        public static GridButton run(String label, Runnable directAction) {
            return new GridButton(label, null, directAction, null, true);
        }

        public static GridButton disabled(String label) {
            return new GridButton(label, null, null, null, false);
        }
    }
}
