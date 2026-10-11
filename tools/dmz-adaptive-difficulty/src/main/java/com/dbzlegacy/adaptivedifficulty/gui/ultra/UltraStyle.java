package com.dbzlegacy.adaptivedifficulty.gui.ultra;

/**
 * Section-sign colors KaiPath renders. Matches CNPC Ultra standards.
 *
 * <p>Color strings are built at class-init (not compile-time constant expressions) so
 * {@code UltraStyle.LABEL + "text"} does not fold a section-sign into caller classfiles.
 * Screen builders must use these constants — never bare section-sign literals.
 */
public final class UltraStyle {
    /** Single section-sign mark. Not a compile-time constant. */
    public static final String MARK = mark();

    public static final String BOLD = code('l');
    public static final String RESET = code('r');

    public static final String GOLD = code('6');
    public static final String GRAY = code('7');
    public static final String WHITE = code('f');
    public static final String GREEN = code('a');
    public static final String RED = code('c');
    public static final String DARK_GRAY = code('8');
    public static final String YELLOW = code('e');

    public static final String HEADER = GOLD + BOLD;
    public static final String LABEL = GRAY;
    public static final String VALUE = WHITE;
    public static final String CONFIRM = GREEN;
    public static final String DANGER = RED;
    public static final String DIM = DARK_GRAY;
    public static final String NOTICE_HEAD = HEADER;
    public static final String NOTICE_BODY = YELLOW;

    public static final String BACK = LABEL + "« Back";
    public static final String BACK_TO_MAIN = LABEL + "« Back to main menu";
    public static final int BTN_MIN_W = 80;
    public static final int BTN_PAD = 8;
    public static final int BTN_H = 20;
    public static final int COL_L = 20;
    public static final int COL_R = 178;
    public static final int BTN_W = 150;

    private UltraStyle() {}

    /** Runtime section-sign so callers never embed a folded color mark in their classfile. */
    private static String mark() {
        return String.valueOf((char) 0xA7);
    }

    private static String code(char color) {
        return mark() + color;
    }

    public static String plain(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replaceAll(MARK + ".", "").trim();
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

    /** Chat/status line with yellow notice body (errors stay humanized, not raw IDs). */
    public static String noticeLine(String body) {
        return NOTICE_BODY + plain(body);
    }

    /** Chat/status line with danger red. */
    public static String dangerLine(String body) {
        return DANGER + plain(body);
    }
}
