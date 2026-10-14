package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Pure layout helpers for button {@code footer:true} / advertise {@code fixedFooter}.
 *
 * <p>KaiPath <b>1.3.13</b> hard-codes {@code contentBottom = panelY + panelH - 28} and puts every
 * button into the scroll clip — dual footer (Back + Main menu) scrolls with the body. Honor
 * {@code footer:true} by reserving a pinned band and keeping those buttons out of the scroll set.
 * See {@code FIXED_FOOTER.md} for the official-jar merge notes.
 */
public final class UltraBridgeFooter {
    /** Live 1.3.13 close-band inset ({@code panelH - 28}). */
    public static final int DEFAULT_CLOSE_BAND_PX = 28;

    private UltraBridgeFooter() {}

    /** True when the button object has JSON {@code "footer": true}. */
    public static boolean isFooter(JsonObject button) {
        if (button == null || !button.has("footer") || !button.get("footer").isJsonPrimitive()) {
            return false;
        }
        try {
            return button.get("footer").getAsBoolean();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Pixels to reserve at the panel bottom for pinned footer chrome (+ Close).
     * When no {@code footer:true} buttons exist, returns {@link #DEFAULT_CLOSE_BAND_PX}.
     */
    public static int footerReservePx(JsonArray buttons, int defaultButtonH, int margin) {
        int btnH = Math.max(12, defaultButtonH);
        int pad = Math.max(0, margin);
        int maxH = 0;
        boolean any = false;
        if (buttons != null) {
            for (JsonElement el : buttons) {
                if (el == null || !el.isJsonObject()) {
                    continue;
                }
                JsonObject b = el.getAsJsonObject();
                if (!isFooter(b)) {
                    continue;
                }
                any = true;
                int h = btnH;
                if (b.has("h") && b.get("h").isJsonPrimitive()) {
                    try {
                        h = Math.max(12, b.get("h").getAsInt());
                    } catch (Throwable ignored) {
                        h = btnH;
                    }
                }
                if (h > maxH) {
                    maxH = h;
                }
            }
        }
        if (!any) {
            return DEFAULT_CLOSE_BAND_PX;
        }
        return Math.max(DEFAULT_CLOSE_BAND_PX, maxH + pad);
    }

    /**
     * Panel-relative Y for a pinned footer button of height {@code buttonH} inside a
     * {@code footerReserve} band at the bottom of a panel of height {@code panelH}.
     */
    public static int pinFooterY(int panelH, int footerReserve, int buttonH) {
        int reserve = Math.max(DEFAULT_CLOSE_BAND_PX, footerReserve);
        int h = Math.max(12, buttonH);
        int y = panelH - reserve + Math.max(0, (reserve - h) / 2);
        return Math.max(0, Math.min(y, Math.max(0, panelH - h)));
    }

    /**
     * Absolute content-bottom Y (exclusive) for the scrollable body band.
     * Matches live field {@code contentBottom} once fixedFooter is honored.
     */
    public static int contentBottomAbs(int panelY, int panelH, int footerReserve) {
        int reserve = Math.max(DEFAULT_CLOSE_BAND_PX, footerReserve);
        return Math.max(panelY + 1, panelY + panelH - reserve);
    }
}
