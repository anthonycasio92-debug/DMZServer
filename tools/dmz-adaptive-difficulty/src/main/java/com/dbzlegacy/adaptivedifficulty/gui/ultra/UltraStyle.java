package com.dbzlegacy.adaptivedifficulty.gui.ultra;

/**
 * Section-sign colors KaiPath renders. Matches CNPC Ultra standards.
 *
 * <p>Marks are initialized in a static block (not compile-time constant expressions) so
 * {@code UltraStyle.LABEL + "text"} does not fold the section-sign into caller classfiles.
 * Screen builders must use these constants - never bare section-sign literals.
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
    public static final String AQUA;
    public static final String LIGHT_PURPLE;
    public static final String DARK_GREEN;
    public static final String DARK_PURPLE;
    public static final String DARK_RED;

    public static final String HEADER;
    public static final String LABEL;
    public static final String VALUE;
    public static final String CONFIRM;
    public static final String DANGER;
    public static final String DIM;
    public static final String NOTICE_HEAD;
    public static final String NOTICE_BODY;

    /**
     * Per-system accent inks (body/chat lines that allow section-sign). Hub tiles / tabs /
     * titles cannot embed these in labels — use {@link #systemAccentCode(String)} on the wire.
     */
    public static final String ACCENT_DIFFICULTY;
    public static final String ACCENT_RIVAL;
    public static final String ACCENT_SPAR;
    public static final String ACCENT_PRESTIGE;
    public static final String ACCENT_CHARACTER;
    public static final String ACCENT_SKILLCHECK;
    public static final String ACCENT_ADMIN;

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
     * Compact Ultra centered dialog (not fullscreen). Defaults 350x300; grows toward
     * {@link #PANEL_MAX_H} when body text or buttons need more room. Body text scrolls
     * rather than compressing when content still exceeds the grown panel.
     */
    public static final int PANEL_W = 350;
    public static final int PANEL_H = 300;
    public static final int PANEL_MIN_W = 300;
    public static final int PANEL_MIN_H = 220;
    /**
     * Floor for tile-only menus (no body copy), e.g. main hub. Lower than {@link #PANEL_MIN_H}
     * so hub rows are not padded with a hollow band under the last tile.
     */
    public static final int PANEL_COMPACT_MIN_H = 150;
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
    /**
     * Tab button row Y (panel-relative), under the Ultra title/header line — same band as
     * hub tiles ({@link #CONTENT_ROW}), not KaiPath's default tab strip that sits in the
     * title chrome.
     */
    public static final int TAB_ROW_Y = 28;
    /**
     * {@code layoutJson.headerBottom} when there is no under-title tab row — client chrome
     * (title + yellow divider) must end at or above this panel-relative Y.
     */
    public static final int HEADER_BOTTOM = BODY_TOP;
    /**
     * {@code layoutJson.headerBottom} when {@code tabsUnderTitle} is true. Tab buttons sit at
     * {@link #TAB_ROW_Y}; client title + divider must stay strictly above this line.
     */
    public static final int HEADER_BOTTOM_TABS = TAB_ROW_Y;
    /** Y where the body starts when a tab button row is present. */
    public static final int BODY_TOP_TABS = TAB_ROW_Y + BTN_H + BTN_GAP;
    /** Minimum body scroll region height so lines stay readable. */
    public static final int BODY_MIN_H = 140;
    /** Default content row after title (no tabs) — hub home tiles. */
    public static final int CONTENT_ROW = 40;
    /** Default content row after title + tab button row. */
    public static final int CONTENT_ROW_TABS = TAB_ROW_Y + BTN_H + BTN_GAP;
    /** Title alignment in layoutJson: {@code center} or {@code left}. */
    public static final String TITLE_ALIGN = "center";

    /**
     * Button {@code style} wire values (KaiPath maps these to themed colors). Prefer these
     * constants over string literals so locked/unavailable actions always send {@link #STYLE_DIM}.
     */
    public static final String STYLE_NORMAL = "normal";
    public static final String STYLE_CONFIRM = "confirm";
    public static final String STYLE_DANGER = "danger";
    public static final String STYLE_DIM = "dim";

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
        AQUA = MARK + "b";
        LIGHT_PURPLE = MARK + "d";
        DARK_GREEN = MARK + "2";
        DARK_PURPLE = MARK + "5";
        DARK_RED = MARK + "4";
        HEADER = GOLD + BOLD;
        LABEL = GRAY;
        VALUE = WHITE;
        CONFIRM = GREEN;
        DANGER = RED;
        DIM = DARK_GRAY;
        NOTICE_HEAD = HEADER;
        NOTICE_BODY = YELLOW;
        ACCENT_DIFFICULTY = RED;
        ACCENT_RIVAL = DARK_PURPLE;
        ACCENT_SPAR = GOLD;
        ACCENT_PRESTIGE = AQUA;
        ACCENT_CHARACTER = GREEN;
        ACCENT_SKILLCHECK = YELLOW;
        ACCENT_ADMIN = DARK_RED;
        // ASCII-only separators: KaiPath/JLDK still mojibakes multi-byte punctuation.
        SEP = "|";
        TIMES = "x";
        PAGE_ANDROID_REMOVE = "android_remove";
        // No leading "<" - some clients render it as garbage next to color codes.
        BACK = LABEL + "Back";
        BACK_TO_MAIN = LABEL + "Back to main menu";
    }

    private UltraStyle() {}

    /**
     * Contextual back that names its destination (e.g. "Weights shop", "Rival list").
     * Use beside {@link UltraScreenSpec#footerBackHome()} when both exits are intentional —
     * footer stays generic hub Back; this names the specific parent page.
     */
    public static String backTo(String destination) {
        if (destination == null || destination.isBlank()) {
            return BACK;
        }
        return LABEL + destination.trim();
    }

    public static String plain(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replaceAll(MARK + ".", "").trim();
    }

    /**
     * Sanitize text for the KaiPath Ultra wire. Keeps Minecraft section-sign color codes
     * (single U+00A7) but replaces fancy punctuation that mojibakes (middle-dot / em-dash
     * UTF-8 misread as Latin-1) on clients that mishandle multi-byte UTF-8.
     */
    public static String wireText(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        String s = text;
        // Common punctuation -> ASCII (char codes so source stays ASCII-audit clean).
        s = s.replace((char) 0x2014, '-').replace((char) 0x2013, '-').replace((char) 0x2212, '-');
        s = s.replace(String.valueOf((char) 0x2026), "...");
        s = s.replace((char) 0x00B7, '-').replace((char) 0x2022, '*');
        s = s.replace((char) 0x25CF, '*').replace((char) 0x00D7, 'x').replace((char) 0x00B1, '+');
        s = s.replace((char) 0x2192, '>').replace((char) 0x2190, '<').replace((char) 0x2194, '-');
        s = s.replace((char) 0x00AB, '<').replace((char) 0x00BB, '>');
        s = s.replace((char) 0x201C, '"').replace((char) 0x201D, '"');
        s = s.replace((char) 0x2018, '\'').replace((char) 0x2019, '\'');
        s = s.replace((char) 0x00A0, ' ').replace((char) 0x2500, '-');
        // Already-mojibaked UTF-8-as-Latin-1 sequences (C2 A7 / E2 80 xx prefixes).
        s = s.replace(new String(new char[] {(char) 0xE2, (char) 0x20AC}), "-");
        s = s.replace(new String(new char[] {(char) 0xC2, (char) 0xB7}), "-");
        s = s.replace(String.valueOf((char) 0xC2), "");
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == (char) 0xA7 || c <= 0x7F) {
                out.append(c);
            } else {
                out.append('?');
            }
        }
        return out.toString();
    }

    public static String header(String title) {
        return wireText(HEADER + plain(title));
    }

    public static String labelValue(String label, String value) {
        return wireText(LABEL + plain(label) + " " + VALUE + plain(value));
    }

    public static String notice(String body) {
        return wireText(NOTICE_HEAD + "Notice " + NOTICE_BODY + plain(body));
    }

    /**
     * Conservative average glyph width (GUI px) for Minecraft's default font. Used to
     * pre-fit labels so stock KaiPath never runs its {@code fitLabel} path — that helper
     * appends a mojibaked ellipsis ({@code â€¦}) which shows up as stray A/E glyphs.
     */
    public static final int CHAR_W_EST = 6;

    /**
     * Truncate for buttons. Returns plain ASCII only — KaiPath button {@code style} carries
     * color; section-sign prefixes mojibake as stray A/E glyphs on some clients.
     */
    public static String buttonLabel(String styled) {
        return fitAsciiLabel(styled, 28 * CHAR_W_EST);
    }

    /**
     * Plain ASCII label clipped to {@code maxWidthPx} with an ASCII {@code ...} ellipsis.
     * Prefer this over letting the client truncate.
     */
    public static String fitAsciiLabel(String styled, int maxWidthPx) {
        if (styled == null) {
            return "";
        }
        String text = plain(wireText(styled));
        if (text.isEmpty()) {
            return "";
        }
        int maxPx = Math.max(CHAR_W_EST, maxWidthPx);
        int maxChars = Math.max(1, maxPx / CHAR_W_EST);
        if (text.length() <= maxChars) {
            return text;
        }
        if (maxChars <= 3) {
            return text.substring(0, maxChars);
        }
        return text.substring(0, maxChars - 3) + "...";
    }

    /** Usable text width inside a button/tab of width {@code controlW} with {@link #BTN_PAD}. */
    public static int labelMaxWidth(int controlW) {
        return Math.max(CHAR_W_EST, controlW - BTN_PAD * 2);
    }

    public static String confirm(String label) {
        return wireText(CONFIRM + plain(label));
    }

    public static String danger(String label) {
        return wireText(DANGER + plain(label));
    }

    public static String dim(String label) {
        return wireText(DIM + plain(label));
    }

    /** Chat/status line with yellow notice body. */
    public static String noticeLine(String body) {
        return wireText(NOTICE_BODY + plain(body));
    }

    /** Chat/status line with danger red. */
    public static String dangerLine(String body) {
        return wireText(DANGER + plain(body));
    }

    /** Boost preset caption, e.g. {@code 1.25x - 30m}. */
    public static String boostPreset(String mult, String minutes) {
        return wireText(plain(mult) + TIMES + " " + SEP + " " + plain(minutes) + "m");
    }

    /**
     * Apply a single-letter Minecraft color code (e.g. {@code "a"}, {@code "e"}) from
     * APIs like {@code DifficultySnapshot.stateColorCode()}. Prefer named constants when
     * the color is known at the call site.
     */
    public static String color(String codeLetter, String text) {
        String letter = codeLetter == null || codeLetter.isBlank()
                ? "f"
                : codeLetter.trim().substring(0, 1);
        return wireText(MARK + letter + plain(text));
    }

    /**
     * Single-letter Minecraft color code for {@code layoutJson.accent} / button {@code accent}.
     * Empty when the screen uses the shared hub gold header (no per-system tint).
     *
     * <p>Labels stay plain ASCII — KaiPath mojibakes section-sign in title/button text.
     * JLDK paints title chrome and active-tab highlight from this code.
     */
    public static String systemAccentCode(String screenId) {
        if (screenId == null || screenId.isBlank()) {
            return "";
        }
        String id = screenId.trim().toLowerCase();
        return switch (id) {
            case "difficulty", "weights" -> "c";
            case "rival" -> "5";
            case "spar" -> "6";
            case "prestige" -> "b";
            case "character" -> "a";
            case "skillcheck", "skills" -> "e";
            case "admin", "config", "progression" -> "4";
            // Main hub title keeps shared HEADER/GOLD chrome — no per-system accent override.
            case "hub" -> "";
            default -> "";
        };
    }

    /** Accent ink for body lines (section-sign OK). Empty string when none. */
    public static String systemAccent(String screenId) {
        String code = systemAccentCode(screenId);
        if (code.isEmpty()) {
            return "";
        }
        return MARK + code;
    }

    /** Hub tile accent letter for a system id ({@code difficulty}, {@code admin}, …). */
    public static String hubTileAccentCode(String system) {
        if (system == null || system.isBlank()) {
            return "";
        }
        String s = system.trim().toLowerCase();
        return switch (s) {
            case "difficulty" -> "c";
            case "rival" -> "5";
            case "spar" -> "6";
            case "prestige" -> "b";
            case "character" -> "a";
            case "skillcheck", "skills", "skillcheck_denied" -> "e";
            case "admin" -> "4";
            default -> "";
        };
    }
}
