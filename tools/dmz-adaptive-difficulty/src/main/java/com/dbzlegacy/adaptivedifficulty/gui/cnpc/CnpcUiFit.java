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
 * ({@code framebuffer / guiScale}). Auto is the largest scale Minecraft offers.
 * GUI scale 4 and 5 are tighter than Auto on some resolutions, so the menu fits
 * the tightest of those. Width and height use one scale so the layout keeps its shape.
 */
public final class CnpcUiFit {
    /** Same floor Minecraft uses when choosing GUI scale. */
    static final int MIN_SCALED_WIDTH = 320;
    static final int MIN_SCALED_HEIGHT = 240;
    /** Gap so the window is not flush with the edge of the screen. */
    private static final int SCREEN_MARGIN = 8;

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

    /**
     * Tightest GUI scale the menu must fit: Auto, plus 4 and 5 when the framebuffer
     * can actually show that step. Scale 5 on 1080p is shorter than Auto, so a menu
     * sized only for Auto still runs off the screen.
     */
    public static int tightScale(int framebufferWidth, int framebufferHeight) {
        int tight = guiScale(framebufferWidth, framebufferHeight);
        for (int step : new int[] {4, 5}) {
            if (step <= tight) {
                continue;
            }
            if (framebufferWidth / step >= MIN_SCALED_WIDTH
                    && framebufferHeight / step >= 180) {
                tight = step;
            }
        }
        return tight;
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
        int scale = tightScale(fb[0], fb[1]);
        int boxW = Math.max(1, guiPixels(fb[0], scale) - SCREEN_MARGIN);
        int boxH = Math.max(1, guiPixels(fb[1], scale) - SCREEN_MARGIN);
        return new int[] {
                fitWindow(designedWidth, boxW),
                fitWindow(designedHeight, boxH)
        };
    }

    /**
     * Shrink widget positions so a layout drawn for the design size stays inside the window.
     * One scale is used for both axes ({@code Math.min(sx, sy)}) so buttons stay the same
     * shape. The window is then pulled in around that layout so it stays centered.
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
        float sx = maxRight > width ? (width - 2f) / maxRight : 1f;
        float sy = maxBottom > height ? (height - 2f) / maxBottom : 1f;
        float scale = Math.min(sx, sy);
        if (scale > 0.999f) {
            return;
        }
        scale = Math.min(1f, Math.max(0.05f, scale));
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
        int fittedW = Math.max(1, Math.min(width, Math.round(maxRight * scale) + 2));
        int fittedH = Math.max(1, Math.min(height, Math.round(maxBottom * scale) + 2));
        if (fittedW < width || fittedH < height) {
            gui.setSize(fittedW, fittedH);
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
