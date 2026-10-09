package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

/**
 * Server-side CNPC colors that follow DMZUltra: gold headers, white body, gray detail.
 * No client widgets. Callers keep their own buttons and page ids.
 */
public final class CnpcUltraStyle {
    public static final String HEADER = "§6§l";
    public static final String SUBTITLE = "§7";
    public static final String DIVIDER = "§6";
    public static final String SECTION = "§e§l ▸ ";
    public static final String BODY = "§f";
    public static final String ACCENT = "§6";
    public static final String CONFIRM = "§a";
    public static final String DANGER = "§c";
    public static final String DIM = "§8";
    public static final String CARD_TITLE = "§e▸ ";
    public static final String CARD_BODY = "§7 ";

    private CnpcUltraStyle() {}

    /** Strip formatting so a screen can pass its old colored title. */
    public static String plain(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replaceAll("§.", "").trim();
    }

    public static String header(String title) {
        return HEADER + plain(title);
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
        return "§6§l" + plain(label);
    }

    public static String tabInactive(String label) {
        return "§7" + plain(label);
    }

    public static String cardTitle(String title) {
        return CARD_TITLE + plain(title);
    }

    public static String cardBody(String description) {
        return CARD_BODY + plain(description);
    }
}
