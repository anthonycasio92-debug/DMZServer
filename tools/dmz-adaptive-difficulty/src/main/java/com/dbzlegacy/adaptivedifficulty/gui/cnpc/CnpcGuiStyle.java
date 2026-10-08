package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import java.util.Locale;
import java.util.regex.Pattern;

/** Shared CNPC menu titles, hints, and line formatting for Legacy Mechanics. */
public final class CnpcGuiStyle {
    /** Default max lines before status uses scroll wheel. */
    public static final int INFO_INLINE_MAX = 3;
    /** Short header above a scroll list (equipped title, etc.). */
    public static final int INFO_LIST_HEADER_MAX = 2;

    public static final String SEP = " §8· ";

    public static final String HINT_CLICK_ENTRY = "§7Select an entry below";
    public static final String HINT_CLICK_PLAYER = "§7Select a player below";
    public static final String HINT_CLICK_INVITE = "§7Tap a name — Accept or Decline (or withdraw outgoing)";
    /** Shown above a scrollable read-only status band (mouse wheel). */
    public static final String HINT_SCROLL_STATUS = "§7Scroll this section with your mouse wheel";
    /** Shown above CNPC {@code IScroll} pick lists only when rows do not fit. */
    public static final String HINT_PICK_LIST =
            "§7Use search to filter · drag the list scrollbar to browse";
    public static final String HINT_DOUBLE_CLICK_PLAYER = "§7Double-click a player to choose them";
    public static final String HINT_REVIEW_PAY = "§7Read the summary before you confirm";
    public static final String HINT_TOGGLE_STAFF = "§7Staff: tap a row to turn a flag on or off";
    public static final String HINT_READ_ONLY = "§7View-only — nothing to change here";
    public static final String HINT_TITLE_EQUIP =
            "§7Select a row, then Details · double-click to equip";
    public static final String HINT_REVIEW_DOJO = "§7Select a rival dojo below";

    public static final String MSG_RIVALS_OFF = "§cRivals are turned off on this server.";
    public static final String MSG_SPAR_OFF = "§cSparring is turned off on this server.";

    private static final Pattern MULTI_SPACE = Pattern.compile(" {2,}");
    private static final Pattern SEP_PIPE = Pattern.compile(" ?\\| ?");

    private CnpcGuiStyle() {}

    /** Colored system name + optional subpage (Title Case segment after middle dot). */
    public static String subPage(String colorPrefix, String system, String sub) {
        if (sub == null || sub.isBlank()) {
            return colorPrefix + system;
        }
        return colorPrefix + system + " · " + sub;
    }

    public static String subGray(String sentence) {
        if (sentence == null || sentence.isBlank()) {
            return "";
        }
        String s = sentence.trim();
        if (s.startsWith("§")) {
            return s;
        }
        return "§7" + s;
    }

    /**
     * Info blocks and subtitles. Grey and dark-grey copy becomes white and yellow
     * so CNPC labels stay readable. Flash notices do not use this — their body is
     * always {@link CnpcMenuFeedback#NOTICE_BODY}.
     */
    public static String readableInfoLine(String line) {
        if (line == null || line.isBlank()) {
            return line == null ? "" : line;
        }
        String s = line.trim();
        s = s.replace("§7", "§f");
        s = s.replace("§8", "§e");
        return s;
    }

    /** Collapse duplicate spaces and normalize separators for CNPC labels/scroll rows. */
    public static String normalizeLine(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        String s = line.trim();
        s = SEP_PIPE.matcher(s).replaceAll(" · ");
        s = s.replace(" §8| ", SEP).replace(" | ", " · ");
        while (s.contains("§8··")) {
            s = s.replace("§8··", "§8·");
        }
        s = s.replace("§8·§8·", "§8·");
        s = s.replace("  §8·  ", SEP).replace(" §8·  ", SEP).replace("  §8· ", SEP);
        s = MULTI_SPACE.matcher(s).replaceAll(" ");
        return s;
    }

    public static String toggleOn(String feature) {
        return "§2§lON §r§a" + feature;
    }

    public static String toggleOff(String feature) {
        return "§8§lOFF §r§7" + feature;
    }

    /** Friendly spar leaderboard tab name (replaces raw keys like tp / rp). */
    public static String sparLeaderboardTab(String category) {
        if (category == null || category.isBlank()) {
            return "Training points";
        }
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "tp", "wins", "win", "streak" -> "Training points";
            case "sessions", "session" -> "Sessions";
            case "perfect", "perfects" -> "Perfect spars";
            case "combo" -> "Combo";
            case "time" -> "Time";
            case "rp" -> "Reputation";
            case "wars", "war" -> "War wins";
            default -> capitalize(category.replace('_', ' '));
        };
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        s = s.trim();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
