package com.dbzlegacy.adaptivedifficulty.character;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Atomic DMZ head bones, joined with {@code +} into the single {@code activeHeadBone} token.
 * DMZ's client splits that token on {@code +} and draws each geo bone.
 *
 * <p>DMZUltra form accessories are not in this token and are not listed here.
 * {@code FormParts} is client-only and keyed by the worn form.
 */
public final class HeadPartPieces {
    public static final int PAGE_HEAD = 0;
    public static final int PAGE_HORNS = 1;
    public static final int PAGES = 2;

    /** Stable join order. DMZ skips a piece named {@code hair} inside a multi-bone token. */
    public static final List<String> ATOMIC = List.of(
            "hair",
            "ears1",
            "ears2",
            "ears3",
            "antennas1",
            "antennas2",
            "horns1",
            "horns2",
            "horns3",
            "horns4",
            "horns5",
            "majin1",
            "majin2",
            "majin3");

    private static final List<String> HEAD_PAGE = List.of(
            "hair", "ears1", "ears2", "ears3", "antennas1", "antennas2");
    private static final List<String> HORNS_PAGE = List.of(
            "horns1", "horns2", "horns3", "horns4", "horns5", "majin1", "majin2", "majin3");

    private static final Map<String, Integer> INDEX;

    static {
        java.util.HashMap<String, Integer> index = new java.util.HashMap<>();
        for (int i = 0; i < ATOMIC.size(); i++) {
            index.put(ATOMIC.get(i), i);
        }
        INDEX = Map.copyOf(index);
    }

    private HeadPartPieces() {}

    /**
     * Race group from the part id. Trailing digits are the style number
     * ({@code ears1} and {@code ears2} are group {@code ears}). No separate metadata list.
     */
    public static String raceGroup(String partId) {
        if (partId == null || partId.isBlank()) {
            return "";
        }
        String id = partId.trim().toLowerCase(Locale.ROOT);
        int end = id.length();
        while (end > 0 && Character.isDigit(id.charAt(end - 1))) {
            end--;
        }
        return end == 0 ? id : id.substring(0, end);
    }

    /** Demon horns stack. Every other group keeps a single equipped part. */
    public static boolean allowsMultipleInGroup(String group) {
        if (group == null || group.isBlank()) {
            return false;
        }
        return group.toLowerCase(Locale.ROOT).contains("horn");
    }

    public static String groupLabel(String group) {
        if (group == null || group.isBlank()) {
            return "";
        }
        String id = group.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    /**
     * One equipped id per race group. A later id replaces the earlier one in that group.
     * Groups that {@link #allowsMultipleInGroup} (demon horns) keep every id.
     */
    public static List<String> onePerGroup(Collection<String> ids) {
        List<String> out = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            return out;
        }
        java.util.HashMap<String, Integer> slot = new java.util.HashMap<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            String part = id.trim().toLowerCase(Locale.ROOT);
            String group = raceGroup(part);
            if (group.isEmpty() || allowsMultipleInGroup(group)) {
                if (!out.contains(part)) {
                    out.add(part);
                }
                continue;
            }
            Integer at = slot.get(group);
            if (at == null) {
                slot.put(group, out.size());
                out.add(part);
            } else {
                out.set(at, part);
            }
        }
        return out;
    }

    public static boolean isAtomic(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return INDEX.containsKey(id.trim().toLowerCase(Locale.ROOT));
    }

    public static List<String> pageIds(int page) {
        return switch (page) {
            case PAGE_HEAD -> HEAD_PAGE;
            case PAGE_HORNS -> HORNS_PAGE;
            default -> List.of();
        };
    }

    public static String pageTitle(int page) {
        return switch (page) {
            case PAGE_HEAD -> "Head";
            case PAGE_HORNS -> "Horns";
            default -> "Model customization";
        };
    }

    /** Split on {@code +}. Drops blanks. Does not drop unknown fragments. */
    public static List<String> fragments(String token) {
        if (token == null || token.isBlank()) {
            return List.of();
        }
        String[] raw = token.split("\\+", -1);
        List<String> out = new ArrayList<>();
        for (String part : raw) {
            if (part == null) {
                continue;
            }
            String id = part.trim().toLowerCase(Locale.ROOT);
            if (!id.isEmpty()) {
                out.add(id);
            }
        }
        return out;
    }

    /** Every fragment is an atomic shop id. */
    public static boolean isPureAtomic(String token) {
        List<String> parts = fragments(token);
        if (parts.isEmpty()) {
            return false;
        }
        for (String part : parts) {
            if (!isAtomic(part)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Canonical token. Atomic ids stay in {@link #ATOMIC} order. Any other ids follow, sorted.
     * Unknown fragments such as the truncated {@code ma} are omitted when they were never passed in.
     */
    public static String join(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        Set<String> want = new LinkedHashSet<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            want.add(id.trim().toLowerCase(Locale.ROOT));
        }
        StringBuilder sb = new StringBuilder();
        for (String id : ATOMIC) {
            if (want.remove(id)) {
                append(sb, id);
            }
        }
        List<String> rest = new ArrayList<>(want);
        rest.sort(String::compareTo);
        for (String id : rest) {
            if (!id.contains("+")) {
                append(sb, id);
            }
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, String id) {
        if (sb.length() > 0) {
            sb.append('+');
        }
        sb.append(id);
    }
}
