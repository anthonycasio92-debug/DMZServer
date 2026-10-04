package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import java.util.List;
import noppes.npcs.api.IScreenSize;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.gui.IEntityDisplay;
import noppes.npcs.api.gui.ILabel;
import noppes.npcs.api.wrapper.gui.GuiComponentsScrollableWrapper;

/**
 * Fits Legacy Mechanics CNPC menus to the player's UI scale.
 *
 * <p>CustomNPCs reports the framebuffer size. Minecraft draws GUI widgets in scaled pixels
 * ({@code framebuffer / guiScale}). The largest scale the video settings allow is the same
 * value Auto picks ({@code Window.calculateScale}). Menus are sized for that scale, then
 * squeezed so buttons and the character preview stay inside the window.
 */
public final class CnpcUiFit {
    /** Same floor Minecraft uses when choosing GUI scale. */
    static final int MIN_SCALED_WIDTH = 320;
    static final int MIN_SCALED_HEIGHT = 240;

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
        return new int[] {
                fitWindow(designedWidth, guiPixels(fb[0], scale)),
                fitWindow(designedHeight, guiPixels(fb[1], scale))
        };
    }

    /**
     * Shrink widget positions so a layout drawn for the design size stays inside the window.
     * Width and height scale independently: a short screen compresses rows without narrowing
     * buttons unless the window is also narrower than the layout.
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
        float sx = maxRight > width ? (width - 1f) / maxRight : 1f;
        float sy = maxBottom > height ? (height - 1f) / maxBottom : 1f;
        if (sx > 0.999f && sy > 0.999f) {
            return;
        }
        sx = Math.min(1f, Math.max(0.05f, sx));
        sy = Math.min(1f, Math.max(0.05f, sy));
        if (components != null) {
            for (ICustomGuiComponent component : components) {
                scaleComponent(component, sx, sy);
            }
        }
        if (panel != null && panel.width > 0 && panel.height > 0) {
            panel.x = Math.round(panel.x * sx);
            panel.y = Math.round(panel.y * sy);
            panel.width = Math.max(1, Math.round(panel.width * sx));
            panel.height = Math.max(1, Math.round(panel.height * sy));
            List<ICustomGuiComponent> inner = panel.getComponents();
            if (inner != null) {
                for (ICustomGuiComponent component : inner) {
                    scaleComponent(component, sx, sy);
                }
            }
        }
    }

    private static void scaleComponent(ICustomGuiComponent component, float sx, float sy) {
        if (component == null) {
            return;
        }
        component.setPos(Math.round(component.getPosX() * sx), Math.round(component.getPosY() * sy));
        int w = component.getWidth();
        int h = component.getHeight();
        if (w > 0) {
            w = Math.max(1, Math.round(w * sx));
        }
        if (h > 0) {
            h = Math.max(1, Math.round(h * sy));
        }
        component.setSize(w, h);
        float fit = Math.min(sx, sy);
        if (fit >= 0.999f) {
            return;
        }
        if (component instanceof ILabel label) {
            float scale = label.getScale();
            label.setScale((scale <= 0f ? 1f : scale) * fit);
        } else if (component instanceof IEntityDisplay display) {
            float scale = display.getScale();
            display.setScale((scale <= 0f ? 1f : scale) * fit);
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
