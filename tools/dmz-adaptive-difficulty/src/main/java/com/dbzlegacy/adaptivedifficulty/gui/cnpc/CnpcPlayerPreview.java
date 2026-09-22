package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IEntityDisplay;
import noppes.npcs.api.wrapper.gui.CustomGuiEntityDisplayWrapper;

/**
 * CNPC {@link IEntityDisplay} player preview (SDU-style right-column slot).
 * <p>Uses entity NBT on the client ({@code entityId = -1}) — same idea as SDU clone preview configs —
 * because server network entity ids do not match the viewing client on dedicated servers.
 */
public final class CnpcPlayerPreview {
    /** Right column width reserved on hub-style screens. */
    public static final int SLOT_W = 92;
    public static final int SLOT_H = 118;

    private CnpcPlayerPreview() {}

    public static int slotX() {
        return CnpcGuiSupport.W - CnpcGuiSupport.M - SLOT_W;
    }

    /** Main text / scroll band width when the preview column is shown. */
    public static int textBandWidth() {
        return CnpcGuiSupport.W - CnpcGuiSupport.M * 2 - SLOT_W - 8;
    }

    /**
     * Paint the subject in the top-right preview slot. Call <b>last</b> on the screen (after footer
     * buttons) so this component is last in CNPC's main layer.
     */
    public static void paint(ServerPlayer player, ICustomGui gui, int anchorY) {
        paint(player, gui, CnpcGuiSupport.ID_ENTITY_PREVIEW, anchorY);
    }

    public static void paint(ServerPlayer player, ICustomGui gui, int componentId, int anchorY) {
        int y = anchorY + 8;
        paint(player, gui, componentId, slotX(), y);
    }

    public static void paint(ServerPlayer player, ICustomGui gui, int componentId, int x, int y) {
        if (player == null || gui == null) {
            return;
        }
        try {
            IEntity entity = CnpcGeckoPreviewBridge.previewEntity(player);
            if (entity == null) {
                entity = NpcAPI.Instance().getIEntity(player);
            }
            if (entity == null) {
                return;
            }
            String caption = CnpcGeckoPreviewBridge.geckoAvailable()
                    ? "§8Character"
                    : "§8Preview §7(needs CNPC Gecko)";
            gui.addLabel(CnpcGuiSupport.ID_PREVIEW_CAPTION, caption, x, y - 10, SLOT_W, 10);
            IEntityDisplay display = gui.addEntityDisplay(componentId, x, y, entity);
            forceNbtSnapshot(display, entity);
            display.setSize(SLOT_W, SLOT_H);
            display.setScale(0.9f);
            display.setRotation(215);
            display.setBackground(false);
            try {
                display.setFollowingCursor(false);
            } catch (Throwable ignored) {
            }
            display.setVisible(true);
            display.setEnabled(true);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug("[{}] CNPC player preview skipped: {}",
                    AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    /**
     * CNPC {@link CustomGuiEntityDisplayWrapper#setEntity} assigns the server's player entity id;
     * the client then fails {@code Level.getEntity(id)}. Force NBT snapshot rendering instead.
     */
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
