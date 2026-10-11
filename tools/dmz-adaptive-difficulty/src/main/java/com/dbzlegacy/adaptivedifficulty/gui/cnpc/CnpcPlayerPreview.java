package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IEntityDisplay;
import noppes.npcs.api.wrapper.gui.CustomGuiEntityDisplayWrapper;

/**
 * CNPC {@link IEntityDisplay} player preview (ProfTools cursor yaw + inventory-style live player).
 * <p>
 * Prefer syncing the real {@link ServerPlayer} (same entity the client renders in-world and in the
 * inventory screen) via {@code setEntitySyncedById}. Fall back to a Gecko clone when live sync is
 * unavailable (dedicated servers without a trackable entity id).
 */
public final class CnpcPlayerPreview {
    /** Right column reserved for the model (buttons must stay left of {@link #contentRightEdge()}). */
    public static final int SLOT_W = 88;
    public static final int SLOT_H = 110;
    /** Gap between the text column and the preview slot. Same on every page. */
    public static final int SLOT_GAP = 12;

    /** Tuned for a 1.8-block player inside the 88×110 slot. */
    private static final float PREVIEW_SCALE = 0.78f;
    private static final float REF_PLAYER_HEIGHT = 1.8f;

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

    public static void paint(ServerPlayer player, ICustomGui gui) {
        paint(player, gui, CnpcGuiSupport.ID_ENTITY_PREVIEW);
    }

    public static void paint(ServerPlayer player, ICustomGui gui, int componentId) {
        CnpcGuiSupport.PreviewSlot slot = CnpcGuiSupport.previewAnchor();
        paint(player, gui, componentId, slot);
    }

    /**
     * Head-part shop: show this player's live entity. {@code previewBone} already
     * wrote the bone and synced it, so DMZ's own renderer draws the part.
     * A Gecko CNPC clone does not.
     */
    public static void paintLive(ServerPlayer player, ICustomGui gui) {
        if (player == null || gui == null) {
            return;
        }
        CnpcGuiSupport.PreviewSlot slot = CnpcGuiSupport.previewAnchor();
        try {
            if (!NpcAPI.IsAvailable()) {
                return;
            }
            IEntity live = NpcAPI.Instance().getIEntity(player);
            if (live == null) {
                return;
            }
            IEntityDisplay display = gui.addEntityDisplay(CnpcGuiSupport.ID_ENTITY_PREVIEW, slot.x, slot.y, live);
            if (!tryBindLivePlayer(display, live)) {
                display.setVisible(false);
                return;
            }
            float scale = previewScaleFor(player, slot.height);
            tuneDisplay(display, scale, slot.height);
            display.setSize(slot.width, slot.height);
            display.setScale(scale);
            display.setBackground(false);
            try {
                display.setFollowingCursor(true);
            } catch (Throwable ignored) {
            }
            display.setVisible(true);
            display.setEnabled(true);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug("[{}] live player preview skipped: {}",
                    AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    private static void paint(ServerPlayer player, ICustomGui gui, int componentId, CnpcGuiSupport.PreviewSlot slot) {
        if (player == null || gui == null || slot == null) {
            return;
        }
        try {
            IEntity live = NpcAPI.Instance().getIEntity(player);
            IEntity entity = live;

            if (entity == null) {
                entity = CnpcGeckoPreviewBridge.previewEntity(player);
            }

            if (entity == null) {
                return;
            }

            IEntityDisplay display = gui.addEntityDisplay(componentId, slot.x, slot.y, entity);
            if (live == null || !tryBindLivePlayer(display, live)) {
                IEntity gecko = entity == live ? CnpcGeckoPreviewBridge.previewEntity(player) : entity;
                if (gecko != null) {
                    forceNbtSnapshot(display, gecko);
                } else {
                    forceNbtSnapshot(display, entity);
                }
            }

            float scale = previewScaleFor(player, slot.height);
            tuneDisplay(display, scale, slot.height);
            display.setSize(slot.width, slot.height);
            display.setScale(scale);
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

    /**
     * Inventory-style preview: client tracks the player's live entity (skin, armor, DMZ forms).
     */
    private static boolean tryBindLivePlayer(IEntityDisplay display, IEntity live) {
        try {
            display.setEntitySyncedById(live);
            return true;
        } catch (Throwable ignored) {
        }
        try {
            display.setEntity(live);
            if (display instanceof CustomGuiEntityDisplayWrapper wrapper) {
                try {
                    wrapper.entityId = live.getMCEntity().m_19879_();
                } catch (Throwable ignored) {
                }
            } else {
                try {
                    var idField = display.getClass().getField("entityId");
                    idField.setInt(display, live.getMCEntity().m_19879_());
                } catch (Throwable ignored) {
                }
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Scale the slot model to the player's real height and to the slot height.
     * A giant form, or a body taller than the slot, shrinks so it stays inside.
     * A short form uses a larger scale so it does not disappear.
     */
    private static float previewScaleFor(ServerPlayer player, int slotHeight) {
        float bbHeight = REF_PLAYER_HEIGHT;
        try {
            if (player != null) {
                bbHeight = Math.max(0.6f, player.m_20206_());
            }
        } catch (Throwable ignored) {
        }
        float slot = Math.max(1, slotHeight);
        float scale = PREVIEW_SCALE * (REF_PLAYER_HEIGHT / bbHeight) * (slot / (float) SLOT_H);
        return Math.min(1.0f, Math.max(0.2f, scale));
    }

    /** {@code scaleRatio} keeps the feet planted when the model is larger or smaller than 1.8 blocks. */
    private static void tuneDisplay(IEntityDisplay display, float scale, int slotHeight) {
        float scaleRatio = PREVIEW_SCALE <= 0f ? 1f : scale / PREVIEW_SCALE;
        try {
            var x = display.getClass().getField("offsetX");
            var y = display.getClass().getField("offsetY");
            x.setFloat(display, 0f);
            float room = Math.max(0f, slotHeight - SLOT_H);
            y.setFloat(display, (4f + room * 0.5f) * scaleRatio);
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
