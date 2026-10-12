package com.dbzlegacy.adaptivedifficulty.gui.ultra;

/**
 * Section-sign colors KaiPath renders. Matches CNPC Ultra standards.
 *
 * <p>Marks are initialized in a static block (not compile-time constant expressions) so
 * {@code UltraStyle.LABEL + "text"} does not fold the section-sign into caller classfiles.
 * Screen builders must use these constants — never bare section-sign literals.
 */
public final class UltraStyle {
    /** Single section-sign mark. Owned only by this class. */
    public static final String MARK;

    public static final String BOLD;
    public static final String RESET;

    public static final String GOLD;
    public static final String GRAY;
    public static final String WHITE;
    public static final String GREEN;
    public static final String RED;
    public static final String DARK_GRAY;
    public static final String YELLOW;

    public static final String HEADER;
    public static final String LABEL;
    public static final String VALUE;
    public static final String CONFIRM;
    public static final String DANGER;
    public static final String DIM;
    public static final String NOTICE_HEAD;
    public static final String NOTICE_BODY;

    /** Mid-dot separator for compact button labels (e.g. boost presets). */
    public static final String SEP;
    /** Multiplication sign for boost presets. */
    public static final String TIMES;

    /** Character Services android-removal page / action id. */
    public static final String PAGE_ANDROID_REMOVE;

    public static final String BACK;
    public static final String BACK_TO_MAIN;
    public static final int BTN_MIN_W = 80;
    /** Horizontal padding inside buttons (client honors via layoutJson / button pad). */
    public static final int BTN_PAD = 8;
    public static final int BTN_H = 18;
    /** Vertical gap between button rows (top-to-top advance = BTN_H + BTN_GAP). */
    public static final int BTN_GAP = 6;
    public static final int COL_L = 16;
    public static final int COL_R = 174;
    public static final int BTN_W = 150;

    /**
     * Compact Ultra centered dialog (not fullscreen). Defaults 350×300; grows toward
     * {@link #PANEL_MAX_H} when body text or buttons need more room. Body text scrolls
     * rather than compressing when content still exceeds the grown panel.
     */
    public static final int PANEL_W = 350;
    public static final int PANEL_H = 300;
    public static final int PANEL_MIN_W = 300;
    public static final int PANEL_MIN_H = 220;
    /** Upper bound for content-driven panel growth (dense menus, e.g. Difficulty lines). */
    public static final int PANEL_MAX_H = 420;
    /** Gap between panel edge and screen edge when centered. */
    public static final int PANEL_MARGIN = 10;

    /** Body-text line height (GUI pixels). Client should not squeeze below this. */
    public static final int LINE_H = 16;
    /** Extra gap between body lines (top-to-top advance = LINE_H + LINE_GAP). */
    public static final int LINE_GAP = 8;
    /** Vertical padding inside the body scroll region. */
    public static final int BODY_PAD = 10;
    /** Y where the body/scroll region starts under a title-only header. */
    public static final int BODY_TOP = 42;
    /** Y where the body starts when a tab strip is present. */
    public static final int BODY_TOP_TABS = 60;
    /** Minimum body scroll region height so lines stay readable. */
    public static final int BODY_MIN_H = 140;
    /** Default content row after title (no tabs). */
    public static final int CONTENT_ROW = 40;
    /** Default content row after title + tabs. */
    public static final int CONTENT_ROW_TABS = 58;
    /** Title alignment in layoutJson: {@code center} or {@code left}. */
    public static final String TITLE_ALIGN = "center";

    static {
        // Unicode escape keeps the mark out of other sources; static-init prevents folding.
        MARK = "\u00A7";
        BOLD = MARK + "l";
        RESET = MARK + "r";
        GOLD = MARK + "6";
        GRAY = MARK + "7";
        WHITE = MARK + "f";
        GREEN = MARK + "a";
        RED = MARK + "c";
        DARK_GRAY = MARK + "8";
        YELLOW = MARK + "e";
        HEADER = GOLD + BOLD;
        LABEL = GRAY;
        VALUE = WHITE;
        CONFIRM = GREEN;
        DANGER = RED;
        DIM = DARK_GRAY;
        NOTICE_HEAD = HEADER;
        NOTICE_BODY = YELLOW;
        SEP = "\u00B7";
        TIMES = "\u00D7";
        PAGE_ANDROID_REMOVE = "android_remove";
        BACK = LABEL + "\u00AB Back";
        BACK_TO_MAIN = LABEL + "\u00AB Back to main menu";
    }

    private UltraStyle() {}

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
        String cut = plain.substring(0, 27) + "\u2026";
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

    /** Chat/status line with yellow notice body. */
    public static String noticeLine(String body) {
        return NOTICE_BODY + plain(body);
    }

    /** Chat/status line with danger red. */
    public static String dangerLine(String body) {
        return DANGER + plain(body);
    }

    /** Boost preset caption, e.g. {@code 1.25× · 30m}. */
    public static String boostPreset(String mult, String minutes) {
        return plain(mult) + TIMES + " " + SEP + " " + plain(minutes) + "m";
    }
}
