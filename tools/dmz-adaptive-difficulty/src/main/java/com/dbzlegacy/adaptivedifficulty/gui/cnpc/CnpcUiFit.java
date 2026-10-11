package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import noppes.npcs.api.IScreenSize;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.gui.IButton;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.gui.IScroll;
import noppes.npcs.api.wrapper.gui.GuiComponentsScrollableWrapper;

/**
 * Fits Legacy Mechanics CNPC menus to the player's UI scale.
 *
 * <p>CustomNPCs reports the framebuffer size. Minecraft draws GUI widgets in scaled pixels
 * ({@code framebuffer / guiScale}). Auto, and a manual 4 or 5 that the framebuffer
 * cannot actually show, all resolve to that same scale. The menu uses it. Width and
 * height share one layout scale so the layout keeps its shape. Label text is left at
 * the size the menu was drawn at.
 */
public final class CnpcUiFit {
    /** Same floor Minecraft uses when choosing GUI scale. */
    static final int MIN_SCALED_WIDTH = 320;
    static final int MIN_SCALED_HEIGHT = 240;
    /** Gap so the window is not flush with the edge of the screen. */
    private static final int SCREEN_MARGIN = 8;
    /**
     * Layout scale never goes below this. Further overflow scrolls instead of squeezing buttons.
     */
    private static final float MIN_COMPRESS_SCALE = 0.9f;

    private CnpcUiFit() {}

    /**
     * Largest GUI scale Minecraft will offer for this framebuffer (Auto / maximum).
     * Integer division matches {@code Window.calculateScale}.
     */
    public static int guiScale(int framebufferWidth, int framebufferHeight) {
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            return 1;
        }
        int scale = 1;
        while (scale < framebufferWidth
                && scale < framebufferHeight
                && framebufferWidth / (scale + 1) >= MIN_SCALED_WIDTH
                && framebufferHeight / (scale + 1) >= MIN_SCALED_HEIGHT) {
            scale++;
        }
        return scale;
    }

    /** GUI pixels along one axis at {@code scale} ({@code ceil(framebuffer / scale)}). */
    public static int guiPixels(int framebuffer, int scale) {
        if (framebuffer <= 0 || scale <= 0) {
            return framebuffer;
        }
        return (int) Math.ceil(framebuffer / (double) scale);
    }

    /** Designed window size, capped to the GUI-scaled screen. */
    public static int fitWindow(int designed, int guiPixels) {
        if (designed <= 0) {
            return designed;
        }
        if (guiPixels <= 0) {
            return designed;
        }
        return Math.min(designed, guiPixels);
    }

    /**
     * @return {@code {width, height}} to pass to {@code createCustomGui}, or the request unchanged
     * when the client has not reported a screen size yet
     */
    public static int[] fit(IPlayer<?> player, int designedWidth, int designedHeight) {
        int[] fb = framebuffer(player);
        if (fb == null) {
            return new int[] {designedWidth, designedHeight};
        }
        int scale = guiScale(fb[0], fb[1]);
        int boxW = Math.max(1, guiPixels(fb[0], scale) - SCREEN_MARGIN);
        int boxH = Math.max(1, guiPixels(fb[1], scale) - SCREEN_MARGIN);
        return new int[] {
                fitWindow(designedWidth, boxW),
                fitWindow(designedHeight, boxH)
        };
    }

    /**
     * Shrink a layout drawn for the design size so it stays inside the window.
     * One scale is used for both axes ({@code Math.min(sx, sy)}) so buttons stay the same
     * shape, and that scale never goes below {@link #MIN_COMPRESS_SCALE}. Past that, the
     * page scrolls instead of squeezing buttons narrower. Button width is also held at
     * {@link CnpcGuiSupport#MIN_BUTTON_WIDTH}.
     */
    public static void compressToWindow(ICustomGui gui) {
        if (gui == null) {
            return;
        }
        int width = gui.getWidth();
        int height = gui.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        List<ICustomGuiComponent> components = gui.getComponents();
        GuiComponentsScrollableWrapper panel = scrollPanel(gui);
        int[] extent = contentExtent(components, panel);
        int maxRight = extent[0];
        int maxBottom = extent[1];
        float sx = maxRight > width ? (width - 2f) / maxRight : 1f;
        float sy = maxBottom > height ? (height - 2f) / maxBottom : 1f;
        float needed = Math.min(sx, sy);
        if (needed > 0.999f) {
            enforceButtonWidths(components);
            enforceButtonWidths(panel == null ? null : panel.getComponents());
            relieveButtonOverlap(components, width);
            relieveButtonOverlap(panel == null ? null : panel.getComponents(), width);
            return;
        }
        float scale = Math.min(1f, Math.max(MIN_COMPRESS_SCALE, needed));
        if (needed < 0.9f) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] GUI {} compressed to {}x — content overflows window",
                    AdaptiveDifficultyMod.MOD_ID,
                    gui.getID(),
                    String.format(java.util.Locale.ROOT, "%.2f", scale));
        }
        if (components != null) {
            for (ICustomGuiComponent component : components) {
                scaleComponent(component, scale, scale);
            }
        }
        if (panel != null && panel.width > 0 && panel.height > 0) {
            panel.x = Math.round(panel.x * scale);
            panel.y = Math.round(panel.y * scale);
            panel.width = Math.max(1, Math.round(panel.width * scale));
            panel.height = Math.max(1, Math.round(panel.height * scale));
            List<ICustomGuiComponent> inner = panel.getComponents();
            if (inner != null) {
                for (ICustomGuiComponent component : inner) {
                    scaleComponent(component, scale, scale);
                }
            }
        }
        relieveButtonOverlap(components, width);
        relieveButtonOverlap(panel == null ? null : panel.getComponents(), width);
        int bottom = contentExtent(gui.getComponents(), scrollPanel(gui))[1];
        if (bottom > height) {
            if (hasPickList(gui.getComponents())) {
                // The pick list already owns CNPC's one scroll region. Shorten that list
                // so the buttons under it stay on screen, and leave button widths alone.
                shrinkPickList(gui, height);
            } else {
                enablePageScroll(gui, width, height);
            }
            return;
        }
        int fittedW = Math.max(1, Math.min(width, Math.round(maxRight * scale) + 2));
        int fittedH = Math.max(1, Math.min(height, Math.round(maxBottom * scale) + 2));
        if (fittedW < width || fittedH < height) {
            gui.setSize(fittedW, fittedH);
        }
    }

    private static int[] contentExtent(List<ICustomGuiComponent> components, GuiComponentsScrollableWrapper panel) {
        int maxRight = 0;
        int maxBottom = 0;
        if (components != null) {
            for (ICustomGuiComponent component : components) {
                if (component == null) {
                    continue;
                }
                maxRight = Math.max(maxRight, component.getPosX() + Math.max(0, component.getWidth()));
                maxBottom = Math.max(maxBottom, component.getPosY() + Math.max(0, component.getHeight()));
            }
        }
        if (panel != null && panel.width > 0 && panel.height > 0) {
            maxRight = Math.max(maxRight, panel.x + panel.width);
            maxBottom = Math.max(maxBottom, panel.y + panel.height);
        }
        return new int[] {maxRight, maxBottom};
    }

    private static void scaleComponent(ICustomGuiComponent component, float sx, float sy) {
        if (component == null) {
            return;
        }
        component.setPos(Math.round(component.getPosX() * sx), Math.round(component.getPosY() * sy));
        int w = component.getWidth();
        int h = component.getHeight();
        if (w > 0) {
            int scaled = Math.max(1, Math.round(w * sx));
            w = component instanceof IButton
                    ? Math.max(CnpcGuiSupport.MIN_BUTTON_WIDTH, scaled)
                    : scaled;
        }
        if (h > 0) {
            h = Math.max(1, Math.round(h * sy));
        }
        component.setSize(w, h);
    }

    /** Buttons that were laid out under the floor are widened. Positions are fixed afterward. */
    private static void enforceButtonWidths(List<ICustomGuiComponent> components) {
        if (components == null) {
            return;
        }
        for (ICustomGuiComponent component : components) {
            if (!(component instanceof IButton) || component.getWidth() <= 0) {
                continue;
            }
            if (component.getWidth() < CnpcGuiSupport.MIN_BUTTON_WIDTH) {
                component.setSize(CnpcGuiSupport.MIN_BUTTON_WIDTH, component.getHeight());
            }
        }
    }

    /**
     * Widening a button back to {@link CnpcGuiSupport#MIN_BUTTON_WIDTH} can cover the next
     * button in the row. Push that neighbor right when the window still has room.
     */
    private static void relieveButtonOverlap(List<ICustomGuiComponent> components, int windowWidth) {
        if (components == null || components.isEmpty()) {
            return;
        }
        List<ICustomGuiComponent> buttons = new ArrayList<>();
        for (ICustomGuiComponent component : components) {
            if (component instanceof IButton) {
                buttons.add(component);
            }
        }
        buttons.sort(Comparator.comparingInt(ICustomGuiComponent::getPosY)
                .thenComparingInt(ICustomGuiComponent::getPosX));
        int rowY = Integer.MIN_VALUE;
        int cursor = -1;
        for (ICustomGuiComponent button : buttons) {
            int y = button.getPosY();
            if (rowY == Integer.MIN_VALUE || Math.abs(y - rowY) > 3) {
                rowY = y;
                cursor = -1;
            }
            if (cursor >= 0 && button.getPosX() < cursor) {
                int next = cursor;
                if (next + button.getWidth() <= windowWidth - 2) {
                    button.setPos(next, y);
                }
            }
            cursor = button.getPosX() + button.getWidth() + 4;
        }
    }

    private static boolean hasPickList(List<ICustomGuiComponent> components) {
        if (components == null) {
            return false;
        }
        for (ICustomGuiComponent component : components) {
            if (component instanceof IScroll) {
                return true;
            }
        }
        return false;
    }

    /** Give the footer its pixels back by shortening the pick list, not the buttons. */
    private static void shrinkPickList(ICustomGui gui, int height) {
        List<ICustomGuiComponent> components = gui.getComponents();
        if (components == null) {
            return;
        }
        ICustomGuiComponent list = null;
        int bottom = 0;
        for (ICustomGuiComponent component : components) {
            if (component == null) {
                continue;
            }
            bottom = Math.max(bottom, component.getPosY() + Math.max(0, component.getHeight()));
            if (component instanceof IScroll) {
                list = component;
            }
        }
        if (list == null || bottom <= height) {
            return;
        }
        int overflow = bottom - height;
        int room = list.getHeight() - 48;
        if (room <= 0) {
            return;
        }
        int shrink = Math.min(overflow, room);
        int oldBottom = list.getPosY() + list.getHeight();
        list.setSize(list.getWidth(), Math.max(48, list.getHeight() - shrink));
        for (ICustomGuiComponent component : components) {
            if (component == null || component == list) {
                continue;
            }
            if (component.getPosY() >= oldBottom - 2) {
                component.setPos(component.getPosX(), component.getPosY() - shrink);
            }
        }
    }

    /**
     * Move the page into CNPC's scroll panel so the rest of the content can be reached.
     * Skipped when a pick list is present — that list is the one scroll region.
     */
    private static void enablePageScroll(ICustomGui gui, int width, int height) {
        GuiComponentsScrollableWrapper panel = scrollPanel(gui);
        if (panel == null) {
            return;
        }
        List<ICustomGuiComponent> inner = panel.getComponents();
        List<ICustomGuiComponent> root = gui.getComponents();
        Set<Integer> ids = new HashSet<>();
        if (inner != null) {
            for (ICustomGuiComponent child : inner) {
                if (child != null && !ids.add(child.getID())) {
                    return;
                }
            }
        }
        List<ICustomGuiComponent> move = new ArrayList<>();
        if (root != null) {
            for (ICustomGuiComponent component : root) {
                if (component == null) {
                    continue;
                }
                if (!ids.add(component.getID())) {
                    return;
                }
                move.add(component);
            }
        }
        if (inner != null && !inner.isEmpty()) {
            int originX = panel.x;
            int originY = panel.y;
            for (ICustomGuiComponent child : new ArrayList<>(inner)) {
                if (child != null) {
                    child.setPos(child.getPosX() + originX, child.getPosY() + originY);
                }
            }
        }
        panel.init(0, 0, Math.max(1, width - 2), Math.max(1, height - 2));
        for (ICustomGuiComponent component : move) {
            gui.removeComponent(component.getID());
        }
        for (ICustomGuiComponent component : move) {
            panel.addComponent(component);
        }
    }

    private static GuiComponentsScrollableWrapper scrollPanel(ICustomGui gui) {
        try {
            if (gui.getScrollingPanel() instanceof GuiComponentsScrollableWrapper panel) {
                return panel;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static int[] framebuffer(IPlayer<?> player) {
        if (player == null) {
            return null;
        }
        try {
            IScreenSize size = player.getScreenSize();
            if (size == null) {
                return null;
            }
            int w = size.getWidth();
            int h = size.getHeight();
            if (w < MIN_SCALED_WIDTH || h < MIN_SCALED_HEIGHT) {
                return null;
            }
            return new int[] {w, h};
        } catch (Throwable ignored) {
            return null;
        }
    }
}
