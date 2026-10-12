package com.dbzlegacy.adaptivedifficulty.gui;

import java.util.regex.Pattern;

/** Shared one-line notice styling for Ultra menus and chat feedback. */
public final class GuiNotice {
    /** Yellow body prefix (section-sign + e). */
    public static final String NOTICE_BODY;

    private static final Pattern SECTION_CODE;

    static {
        String mark = "\u00A7";
        NOTICE_BODY = mark + "e";
        SECTION_CODE = Pattern.compile(mark + "[0-9A-FK-ORa-fk-or]");
    }

    private GuiNotice() {}

    /**
     * One yellow notice line. Existing color/format codes are stripped so callers
     * cannot keep a conflicting notice color.
     */
    public static String noticeBody(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        String plain = SECTION_CODE.matcher(line).replaceAll("").trim();
        if (plain.isEmpty()) {
            return "";
        }
        return NOTICE_BODY + plain;
    }
}
