package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IEntityDisplay;
import noppes.npcs.api.wrapper.gui.CustomGuiEntityDisplayWrapper;

/**
 * CNPC {@link IEntityDisplay} player preview (ProfTools / SDU-style right column).
 * Uses the live {@code IPlayer} entity when possible (armor, transforms, forms) and
 * {@code setFollowingCursor(true)} so rotation tracks the mouse like ProfTools race studio.
 */
public final class CnpcPlayerPreview {
    /** Right column reserved for the model (buttons must stay left of {@link #contentRightEdge()}). */
    public static final int SLOT_W = 88;
    public static final int SLOT_H = 110;
    /** Gap between button columns and the preview slot. */
    public static final int SLOT_GAP = 8;

    private static final float PREVIEW_SCALE = 0.78f;

    private CnpcPlayerPreview() {}

    public static int slotX() {
        return CnpcGuiSupport.W - CnpcGuiSupport.M - SLOT_W;
    }

    /** Right edge (exclusive) of text, scroll lists, and two-column buttons. */
    public static int contentRightEdge() {
        return slotX() - SLOT_GAP;
    }

    /** Main text / scroll band width when the preview column is shown. */
    public static int textBandWidth() {
        return contentRightEdge() - CnpcGuiSupport.M;
    }

    /**
     * Paint the subject in the top-right preview slot. Call <b>last</b> on the screen (after footer
     * buttons) so this component is last in CNPC's main layer.
     */
    public static void paint(ServerPlayer player, ICustomGui gui, int anchorY) {
        paint(player, gui, CnpcGuiSupport.ID_ENTITY_PREVIEW, anchorY);
    }

    public static void paint(ServerPlayer player, ICustomGui gui, int componentId, int anchorY) {
        int y = Math.max(CnpcGuiSupport.M, anchorY);
        paint(player, gui, componentId, slotX(), y);
    }

    public static void paint(ServerPlayer player, ICustomGui gui, int componentId, int x, int y) {
        if (player == null || gui == null) {
            return;
        }
        try {
            IEntity entity = NpcAPI.Instance().getIEntity(player);
            if (entity == null) {
                entity = CnpcGeckoPreviewBridge.previewEntity(player);
            }
            if (entity == null) {
                return;
            }
            IEntityDisplay display = gui.addEntityDisplay(componentId, x, y, entity);
            forceNbtSnapshot(display, entity);
            tuneDisplay(display);
            display.setSize(SLOT_W, SLOT_H);
            display.setScale(PREVIEW_SCALE);
            display.setBackground(false);
            try {
                display.setFollowingCursor(true);
            } catch (Throwable ignored) {
            }
            display.setVisible(true);
            display.setEnabled(true);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug("[{}] CNPC player preview skipped: {}",
                    AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    private static void tuneDisplay(IEntityDisplay display) {
        try {
            var x = display.getClass().getField("offsetX");
            var y = display.getClass().getField("offsetY");
            x.setFloat(display, 0f);
            y.setFloat(display, 8f);
        } catch (Throwable ignored) {
        }
    }

    private static void forceNbtSnapshot(IEntityDisplay display, IEntity entity) {
        if (display instanceof CustomGuiEntityDisplayWrapper wrapper) {
            wrapper.setEntity(entity);
            wrapper.entityId = -1;
            return;
        }
        display.setEntity(entity);
        try {
            var idField = display.getClass().getField("entityId");
            idField.setInt(display, -1);
        } catch (Throwable ignored) {
            AdaptiveDifficultyMod.LOGGER.debug("[{}] CNPC preview could not force NBT snapshot on {}",
                    AdaptiveDifficultyMod.MOD_ID, display.getClass().getName());
        }
    }
}
