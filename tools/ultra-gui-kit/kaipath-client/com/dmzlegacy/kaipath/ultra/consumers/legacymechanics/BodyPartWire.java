package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Parses {@code playerModel.parts.bodyParts} and maps each entry onto a catalog mesh.
 *
 * <p>No Minecraft types. Any mod may send ids. A missing source catalog or an unknown id
 * is skipped. The caller still draws the base entity.
 */
public final class BodyPartWire {
    public static final String SOURCE_DMZULTRA = "dmzultra";
    /** Race tail on the wire. DMZUltra's matching mesh is {@link #SAIYAN_TAIL_MESH}. */
    public static final String SAIYAN_TAIL = "saiyan_tail";
    public static final String SAIYAN_TAIL_MESH = "tail_fur";

    /**
     * Piece ids in DMZUltra 0.2.9 FormParts. Used when the mod is installed but its
     * {@code forms.json} cannot be read. A live catalog replaces this set.
     */
    public static final Set<String> DMZULTRA_PIECES = Set.of(
            "ankle_fins",
            "ankle_flame",
            "ankle_ring",
            "antennae",
            "antlers",
            "arm_blade",
            "arm_fins",
            "arm_wraps",
            "back_crystals",
            "back_spines",
            "bracer",
            "brow_gem",
            "brow_spikes",
            "cape",
            "cape_ragged",
            "cat_ears",
            "chest_orb",
            "claws",
            "collar_high",
            "crest_fin",
            "cuffs",
            "diadem",
            "dorsal_fin",
            "ear_fins",
            "flame_crown",
            "gauntlet",
            "gear_wheel",
            "greave",
            "hair_spikes",
            "halo_clock",
            "halo_disc",
            "halo_ring",
            "halo_shards",
            "headband",
            "horns_great",
            "horns_nub",
            "horns_ram",
            "horns_swept",
            "knee_spike",
            "mandala",
            "mane",
            "orbit_rings",
            "pauldron",
            "sash",
            "scarf",
            "shoulder_crystal",
            "shoulder_gear",
            "shoulder_guard",
            "shoulder_spikes",
            "spirit_orbs",
            "star_crown",
            "tabard",
            "tail_dragon",
            "tail_fur",
            "tail_lion",
            "tail_spade",
            "tail_stinger",
            "third_eye",
            "usekh",
            "whiskers",
            "wing_cases",
            "wings_bat",
            "wings_bat_small",
            "wings_feather",
            "wings_insect",
            "wings_light",
            "wrist_flame",
            "wrist_ring");

    private BodyPartWire() {}

    public record Entry(String id, String source, String slot) {}

    /** {@code mesh} is the catalog bone id ({@code part.<mesh>} in the source model). */
    public record Model(String id, String source, String slot, String mesh, boolean alias) {}

    /** Never throws. Bad elements are skipped. */
    public static List<Entry> parse(JsonArray bodyParts) {
        List<Entry> out = new ArrayList<>();
        if (bodyParts == null) {
            return out;
        }
        try {
            for (JsonElement el : bodyParts) {
                if (el == null || !el.isJsonObject()) {
                    continue;
                }
                JsonObject o = el.getAsJsonObject();
                String id = text(o, "id");
                String source = text(o, "source");
                if (id.isEmpty() || source.isEmpty()) {
                    continue;
                }
                out.add(new Entry(id, source, text(o, "slot")));
            }
        } catch (Throwable ignored) {
            return List.of();
        }
        return out;
    }

    /**
     * Map entries to meshes. {@code piecesBySource} returns the catalog for a mod id, or
     * null/empty when that mod is not installed. Unknown ids are omitted.
     */
    public static List<Model> resolve(List<Entry> entries, Function<String, Set<String>> piecesBySource) {
        List<Model> out = new ArrayList<>();
        if (entries == null || piecesBySource == null) {
            return out;
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (Entry entry : entries) {
            if (entry == null) {
                continue;
            }
            try {
                Set<String> pieces = piecesBySource.apply(entry.source());
                if (pieces == null || pieces.isEmpty()) {
                    continue;
                }
                String mesh = null;
                boolean alias = false;
                if (pieces.contains(entry.id())) {
                    mesh = entry.id();
                } else if (SAIYAN_TAIL.equals(entry.id())
                        && SOURCE_DMZULTRA.equals(entry.source())
                        && pieces.contains(SAIYAN_TAIL_MESH)) {
                    mesh = SAIYAN_TAIL_MESH;
                    alias = true;
                }
                if (mesh == null || !seen.add(entry.source() + "\0" + mesh)) {
                    continue;
                }
                out.add(new Model(entry.id(), entry.source(), entry.slot(), mesh, alias));
            } catch (Throwable ignored) {
                // one bad catalog lookup must not drop the preview
            }
        }
        return out;
    }

    private static String text(JsonObject o, String key) {
        if (o == null || key == null || !o.has(key) || o.get(key).isJsonNull()) {
            return "";
        }
        try {
            return o.get(key).getAsString().trim().toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {
            return "";
        }
    }
}
