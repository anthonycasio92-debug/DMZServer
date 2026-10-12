package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import java.util.List;

/**
 * Shared Ultra name/arg pickers — two-column button grids with human labels (no raw IDs on
 * buttons when a display name is provided).
 */
public final class UltraPickHelper {
    private UltraPickHelper() {}

    /**
     * Place up to {@code max} choices as left/right buttons. Each choice is either a bare name
     * (used as both label and {@code do:} arg) or {@code arg|Label}.
     */
    public static void placeNameButtons(
            UltraScreenSpec spec,
            String doAction,
            List<String> choices,
            int max) {
        if (spec == null || doAction == null || doAction.isBlank() || choices == null) {
            return;
        }
        int limit = Math.max(1, max);
        int placed = 0;
        for (String raw : choices) {
            if (placed >= limit || raw == null || raw.isBlank()) {
                continue;
            }
            String arg;
            String label;
            int bar = raw.indexOf('|');
            if (bar > 0) {
                arg = raw.substring(0, bar).trim();
                label = raw.substring(bar + 1).trim();
            } else {
                arg = raw.trim();
                label = arg;
            }
            if (arg.isBlank()) {
                continue;
            }
            if (label.isBlank()) {
                label = arg;
            }
            String action = "do:" + doAction + ":" + arg;
            String styled = UltraStyle.confirm(label);
            if (placed % 2 == 0) {
                spec.buttonLeft(action, styled, "confirm");
            } else {
                spec.buttonRight(action, styled, "confirm");
                spec.rowGap();
            }
            placed++;
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        if (placed == 0) {
            spec.line(UltraStyle.dim("Nobody is available right now."));
        }
    }

    /** Navigation buttons that reopen a page on the current system. */
    public static void placePageButtons(UltraScreenSpec spec, String... pageLabelPairs) {
        if (spec == null || pageLabelPairs == null) {
            return;
        }
        int placed = 0;
        for (String pair : pageLabelPairs) {
            if (pair == null || pair.isBlank()) {
                continue;
            }
            int bar = pair.indexOf('|');
            String page = bar > 0 ? pair.substring(0, bar).trim() : pair.trim();
            String label = bar > 0 ? pair.substring(bar + 1).trim() : page;
            if (page.isBlank()) {
                continue;
            }
            String action = "page:" + page;
            if (placed % 2 == 0) {
                spec.buttonLeft(action, label, "normal");
            } else {
                spec.buttonRight(action, label, "normal");
                spec.rowGap();
            }
            placed++;
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
    }
}
