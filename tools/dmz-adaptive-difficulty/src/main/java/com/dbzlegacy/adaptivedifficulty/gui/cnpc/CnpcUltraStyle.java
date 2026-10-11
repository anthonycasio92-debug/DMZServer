package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

/**
 * Server-side CNPC colors that follow DMZUltra: gold headers, white body, gray detail.
 * No client widgets. Callers keep their own buttons and page ids.
 */
public final class CnpcUltraStyle {
    public static final String HEADER = "§6§l";
    public static final String SUBTITLE = "§7";
    public static final String DIVIDER = "§6";
    /** Left-aligned section title. Same gold bold as the page header. */
    public static final String SECTION = HEADER;
    /** Page header and active tab only. Body, rows, and buttons stay on the shared colors. */
    public static final String ACCENT_CHARACTER = "§6";
    public static final String ACCENT_RIVAL = "§c";
    public static final String ACCENT_SPARRING = "§9";
    public static final String ACCENT_PROGRESSION = "§a";
    public static final String ACCENT_PRESTIGE = "§d";
    public static final String ACCENT_DIFFICULTY = "§e";
    public static final String ACCENT_SAGA = "§b";
    public static final String ACCENT_ADMIN = "§4";
    public static final String ACCENT_CONFIG = "§7";
    private static String activeAccent = ACCENT_CHARACTER;
    public static final String BODY = "§f";
    public static final String ACCENT = "§6";
    public static final String CONFIRM = "§a";
    public static final String DANGER = "§c";
    public static final String DIM = "§8";
    /** Card title: white and bold. */
    public static final String CARD_TITLE = "§f§l";
    public static final String CARD_BODY = "§7 ";
    /** Yellow notice body. Screens do not pick another notice color. */
    public static final String INFO = "§e";
    public static final String BOLD = "§l";
    public static final String RESET = "§r";
    /** Section-sign prefix for a color that is chosen at runtime. */
    public static final String MARK = "§";
    public static final String CONFIRM_BOLD = "§2§l";
    public static final String BACK = SUBTITLE + "« Back";
    /** Home tab only. Returns to the Legacy Mechanics hub. */
    public static final String BACK_TO_MAIN = SUBTITLE + "« Back to main menu";
    public static final String HUB = SUBTITLE + "« Hub";
    public static final String ON = CONFIRM_BOLD + "ON ";
    public static final String OFF = DIM + BOLD + "OFF ";

    private CnpcUltraStyle() {}

    /** Header and active-tab color for the menu currently being painted. */
    public static void withAccent(String accent, Runnable work) {
        String previous = activeAccent;
        activeAccent = accent == null || accent.isBlank() ? ACCENT_CHARACTER : accent;
        try {
            work.run();
        } finally {
            activeAccent = previous;
        }
    }

    /** Strip formatting so a screen can pass its old colored title. */
    public static String plain(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replaceAll(MARK + ".", "").trim();
    }

    public static String header(String title) {
        return activeAccent + BOLD + plain(title);
    }

    public static String subtitle(String text) {
        String plain = plain(text);
        return plain.isEmpty() ? "" : SUBTITLE + plain;
    }

    public static String section(String name) {
        String plain = plain(name);
        if (plain.startsWith("▸")) {
            plain = plain.substring(1).trim();
        }
        return SECTION + plain;
    }

    public static String tabActive(String label) {
        return activeAccent + BOLD + plain(label);
    }

    public static String tabInactive(String label) {
        return SUBTITLE + plain(label);
    }

    public static String cardTitle(String title) {
        return CARD_TITLE + plain(title);
    }

    public static String cardBody(String description) {
        return CARD_BODY + plain(description);
    }
}
