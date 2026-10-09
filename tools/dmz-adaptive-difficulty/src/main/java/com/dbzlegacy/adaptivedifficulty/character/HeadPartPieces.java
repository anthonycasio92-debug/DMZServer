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
 * <p>DMZUltra's 68 {@code part.<name>} accessories are not in this token. {@code FormParts}
 * is client-only and keyed by the worn form. {@code DmzLookS2CPacket} MENU/RESTYLE and
 * {@code SceneFormS2CPacket} do not select those pieces, and the form editor has no
 * pre-highlight argument. {@link #ultraScrollLines()} is a read-only list.
 */
public final class HeadPartPieces {
    public static final int PAGE_HEAD = 0;
    public static final int PAGE_HORNS = 1;
    public static final int PAGE_BODY = 2;
    public static final int PAGES = 3;

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

    /** DMZUltra form-part ids (geo bone {@code part.<name>}). Not a shop catalog. */
    public static final List<String> ULTRA_HEAD = List.of(
            "antennae", "antlers", "brow_gem", "brow_spikes", "cat_ears", "crest_fin",
            "diadem", "ear_fins", "flame_crown", "hair_spikes", "halo_clock", "halo_disc",
            "halo_ring", "halo_shards", "headband", "horns_great", "horns_nub", "horns_ram",
            "horns_swept", "star_crown", "third_eye", "whiskers");
    public static final List<String> ULTRA_TORSO = List.of(
            "back_crystals", "back_spines", "cape", "cape_ragged", "chest_orb", "collar_high",
            "dorsal_fin", "mandala", "orbit_rings", "sash", "scarf", "tabard", "usekh",
            "wing_cases", "wings_bat", "wings_bat_small", "wings_feather", "wings_insect",
            "wings_light");
    public static final List<String> ULTRA_ARMS = List.of(
            "arm_blade", "arm_fins", "arm_wraps", "bracer", "claws", "cuffs", "gauntlet",
            "pauldron", "shoulder_crystal", "shoulder_gear", "shoulder_guard", "shoulder_spikes",
            "spirit_orbs", "wrist_flame", "wrist_ring");
    public static final List<String> ULTRA_LEGS = List.of(
            "ankle_fins", "ankle_flame", "ankle_ring", "gear_wheel", "greave", "knee_spike",
            "tail_dragon", "tail_fur", "tail_lion", "tail_spade", "tail_stinger");

    private static final Map<String, Integer> INDEX;

    static {
        java.util.HashMap<String, Integer> index = new java.util.HashMap<>();
        for (int i = 0; i < ATOMIC.size(); i++) {
            index.put(ATOMIC.get(i), i);
        }
        INDEX = Map.copyOf(index);
    }

    private HeadPartPieces() {}

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
            case PAGE_BODY -> "Body accessories";
            default -> "Head parts";
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

    /** Read-only DMZUltra names, grouped. These are not toggles. */
    public static List<String> ultraScrollLines() {
        List<String> lines = new ArrayList<>();
        addGroup(lines, "Head", ULTRA_HEAD);
        addGroup(lines, "Torso / Back", ULTRA_TORSO);
        addGroup(lines, "Arms", ULTRA_ARMS);
        addGroup(lines, "Legs / Tail", ULTRA_LEGS);
        return List.copyOf(lines);
    }

    private static void addGroup(List<String> lines, String title, List<String> ids) {
        lines.add("§6" + title);
        for (String id : ids) {
            lines.add("§7" + CosmeticHeadBoneCatalog.prettyId(id));
        }
    }

    private static void append(StringBuilder sb, String id) {
        if (sb.length() > 0) {
            sb.append('+');
        }
        sb.append(id);
    }
}
