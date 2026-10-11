package com.dmzlegacy.kaipath.ultra;

import com.dmzlegacy.kaipath.ultra.internal.UltraWidgetReflect;
import java.util.List;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Public Ultra UI facade for <em>any</em> mod.
 *
 * <p>Pulls themes and widgets from {@code dmzultra} ({@code com.dmzultra.client.ui.*})
 * when that mod is installed. Falls back to plain vanilla buttons when it is not.
 * No hard compile dependency on DMZUltra — safe for packs that omit it.
 *
 * <h2>Typical use from another mod</h2>
 * <pre>{@code
 * // optional soft dep on kaipath in mods.toml
 * if (UltraUiAccess.isAvailable()) {
 *     Object theme = UltraUiAccess.forgeTheme();
 *     addRenderableWidget((AbstractWidget) UltraUiAccess.button(x, y, w, h, "OK", this::onOk, theme));
 * }
 * }</pre>
 *
 * <p>Or extend {@link UltraScreen} for a ready-made themed screen base.
 */
@OnlyIn(Dist.CLIENT)
public final class UltraUiAccess {
    private UltraUiAccess() {}

    /** Probe dmzultra once (also called from {@link UltraUiBridge#bootstrap()}). */
    public static void probe() {
        UltraWidgetReflect.probe();
    }

    /** {@code true} when Ultra theme + button classes resolve on the client. */
    public static boolean isAvailable() {
        return UltraWidgetReflect.isAvailable();
    }

    /**
     * DMZUltra {@code UltraUi.Theme.FORGE} instance, or {@code null} if Ultra is absent.
     * Treat as opaque — pass back into {@link #button} / {@link #list} / {@link #backgroundColor}.
     */
    public static Object forgeTheme() {
        return UltraWidgetReflect.forgeTheme();
    }

    /** Panel / screen background ARGB for the given theme (or a dark fallback). */
    public static int backgroundColor(Object theme) {
        return UltraWidgetReflect.backgroundColor(theme);
    }

    /**
     * Ultra-styled button when dmzultra is present; otherwise a vanilla {@code Button}.
     * Always returns a {@link GuiEventListener} suitable for {@code Screen#addRenderableWidget}.
     */
    public static GuiEventListener button(
            int x, int y, int w, int h, String label, Runnable onPress, Object theme) {
        return UltraWidgetReflect.button(x, y, w, h, label, onPress, theme);
    }

    /** Convenience: forge theme + button. */
    public static GuiEventListener button(int x, int y, int w, int h, String label, Runnable onPress) {
        return button(x, y, w, h, label, onPress, forgeTheme());
    }

    /**
     * Ultra-styled list when dmzultra is present; otherwise {@code null}
     * (caller may draw rows manually — see {@link UltraScreen}).
     */
    public static GuiEventListener list(
            int x, int y, int w, int h, List<String> rows, Object theme) {
        return UltraWidgetReflect.list(x, y, w, h, rows, theme);
    }

    public static GuiEventListener list(int x, int y, int w, int h, List<String> rows) {
        return list(x, y, w, h, rows, forgeTheme());
    }
}
