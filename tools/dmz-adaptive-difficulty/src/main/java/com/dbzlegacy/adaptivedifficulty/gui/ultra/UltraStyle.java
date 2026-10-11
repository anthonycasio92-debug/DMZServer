package com.dbzlegacy.adaptivedifficulty.gui.ultra;

/** Section-sign colors KaiPath renders. Matches CNPC Ultra standards. */
public final class UltraStyle {
    public static final String HEADER = "§6§l";
    public static final String LABEL = "§7";
    public static final String VALUE = "§f";
    public static final String CONFIRM = "§a";
    public static final String DANGER = "§c";
    public static final String DIM = "§8";
    public static final String NOTICE_HEAD = "§6§l";
    public static final String NOTICE_BODY = "§e";
    public static final String BACK = LABEL + "« Back";
    public static final String BACK_TO_MAIN = LABEL + "« Back to main menu";
    public static final int BTN_MIN_W = 80;
    public static final int BTN_PAD = 8;
    public static final int BTN_H = 20;
    public static final int COL_L = 20;
    public static final int COL_R = 178;
    public static final int BTN_W = 150;

    private UltraStyle() {}

    public static String plain(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replaceAll("§.", "").trim();
    }

    public static String header(String title) {
        return HEADER + plain(title);
    }

    public static String labelValue(String label, String value) {
        return LABEL + plain(label) + " " + VALUE + plain(value);
    }

    public static String notice(String body) {
        return NOTICE_HEAD + "Notice " + NOTICE_BODY + plain(body);
    }

    /** Truncate for buttons; keep room for style codes. */
    public static String buttonLabel(String styled) {
        if (styled == null) {
            return "";
        }
        String plain = plain(styled);
        if (plain.length() <= 28) {
            return styled;
        }
        String cut = plain.substring(0, 27) + "…";
        int mark = styled.indexOf(plain);
        if (mark > 0) {
            return styled.substring(0, mark) + cut;
        }
        return cut;
    }

    public static String confirm(String label) {
        return CONFIRM + plain(label);
    }

    public static String danger(String label) {
        return DANGER + plain(label);
    }

    public static String dim(String label) {
        return DIM + plain(label);
    }
}
