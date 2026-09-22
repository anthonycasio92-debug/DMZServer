package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.character.Character;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves DMZ {@link Character} appearance to CNPC-Gecko model paths (aligned with
 * {@code DMZPlayerModel.resolveCustomModel} — many form keys use race geos + textures, not a 1:1 geo file name).
 */
final class DmzPreviewGeo {
    static final String SAGA_BASE_ANIM = "dragonminez:animations/entity/sagas/saga_base.animation.json";
    static final String RACE_MOVE_ANIM = "dragonminez:animations/entity/races/movement.animation.json";

    private static final Set<String> RACE_GEO_STEMS = Set.of(
            "bioandroid",
            "bioandroid_perfect",
            "bioandroid_semi",
            "bioandroid_ultra",
            "bioandroid_xeno",
            "candy",
            "frostdemon",
            "frostdemon_fifth",
            "frostdemon_fp",
            "frostdemon_metalcore",
            "frostdemon_second",
            "frostdemon_third",
            "h4arms",
            "h4armsfem",
            "h4armsslim",
            "hbuffed",
            "hbuffed_fem",
            "hbuffed_slim",
            "human",
            "human_slim",
            "janemba_fat",
            "janemba_super",
            "majin",
            "majin_slim",
            "oozaru");

    private DmzPreviewGeo() {}

    static String resolveModelGeo(Character ch) {
        if (ch == null) {
            return raceGeo("human", false);
        }
        String key = resolveAppearanceKey(ch);
        if (key == null || key.isBlank()) {
            key = "human";
        }
        key = key.toLowerCase(Locale.ROOT).trim();

        if (key.startsWith("saga_")) {
            return sagaGeo(key);
        }

        String stem = mapRaceRenderKey(key, ch);
        if (!RACE_GEO_STEMS.contains(stem)) {
            stem = mapRaceRenderKey(fallbackRaceStem(ch), ch);
        }
        if (!RACE_GEO_STEMS.contains(stem)) {
            stem = slimBody(ch) ? "human_slim" : "human";
        }
        return raceGeo(stem, slimBody(ch));
    }

    static String resolveAnimFile(Character ch) {
        String key = resolveAppearanceKey(ch);
        if (key != null && key.toLowerCase(Locale.ROOT).startsWith("saga_")) {
            return SAGA_BASE_ANIM;
        }
        return RACE_MOVE_ANIM;
    }

    /** Same priority as in-game rendering: stack form, active form, then race/base. */
    private static String resolveAppearanceKey(Character ch) {
        try {
            String resolved = ch.getResolvedCustomModel();
            if (resolved != null && !resolved.isBlank()) {
                return resolved;
            }
            FormConfig.FormData stack = ch.getActiveStackFormData();
            if (stack != null) {
                String cm = stack.getCustomModel();
                if (cm != null && !cm.isBlank()) {
                    return cm;
                }
            }
            FormConfig.FormData form = ch.getActiveFormData();
            if (form != null) {
                String cm = form.getCustomModel();
                if (cm != null && !cm.isBlank()) {
                    return cm;
                }
            }
        } catch (Throwable ignored) {
        }
        return ch.getRenderLogicKey();
    }

    private static String fallbackRaceStem(Character ch) {
        try {
            String race = ch.getRaceName();
            if (race != null && !race.isBlank()) {
                return race.toLowerCase(Locale.ROOT).trim();
            }
        } catch (Throwable ignored) {
        }
        return "human";
    }

    /** Mirrors {@code DMZPlayerModel.resolveCustomModel} geo targets (file stems under races/). */
    private static String mapRaceRenderKey(String key, Character ch) {
        boolean slim = slimBody(ch);
        boolean female = isFemale(ch);
        int bodyType = bodyType(ch);

        return switch (key) {
            case "saiyan", "ancient_saiyan", "sento_saiyan", "half_saiyan", "human", "viltrumite", "monkey" ->
                    slim ? "human_slim" : "human";
            case "oozaru", "goldenoozaru" -> "oozaru";
            case "ssj4gt", "ssj4d" -> slim ? "human_slim" : "human";
            case "buffed", "h_buffed" -> {
                if (female && !slim) {
                    yield "hbuffed_fem";
                }
                yield slim ? "hbuffed_slim" : "hbuffed";
            }
            case "4arms", "h4arms", "four_arms" -> {
                if (female) {
                    yield "h4armsfem";
                }
                yield slim ? "h4armsslim" : "h4arms";
            }
            case "namekian" -> slim ? "human_slim" : "human";
            case "namekian_orange", "namekian_buffed" -> "hbuffed";
            case "majin" -> bodyType == 2 ? "majin" : "majin_slim";
            case "majin_super" -> slim ? "human_slim" : "majin_slim";
            case "majin_ultra" -> female ? "hbuffed_fem" : "hbuffed";
            case "majin_evil", "majin_kid" -> slim ? "human_slim" : "majin_slim";
            case "janemba_fat" -> "janemba_fat";
            case "janemba_super" -> "janemba_super";
            case "frostdemon", "frostdemon_final", "frostdemon_mecha" -> "frostdemon";
            case "frostdemon_second" -> "frostdemon_second";
            case "frostdemon_fifth" -> "frostdemon_fifth";
            case "frostdemon_fp" -> "frostdemon_fp";
            case "frostdemon_third" -> "frostdemon_third";
            case "frostdemon_metalcore" -> "frostdemon_metalcore";
            case "bioandroid", "bioandroid_base" -> "bioandroid";
            case "bioandroid_semi" -> "bioandroid_semi";
            case "bioandroid_perfect" -> "bioandroid_perfect";
            case "bioandroid_ultra" -> "bioandroid_ultra";
            case "bioandroid_xeno" -> "bioandroid_xeno";
            default -> key.replace(' ', '_');
        };
    }

    private static String sagaGeo(String sagaKey) {
        return "dragonminez:geo/entity/sagas/" + sagaKey + ".geo.json";
    }

    private static String raceGeo(String fileStem, boolean slimHint) {
        if (fileStem.endsWith("_slim") || fileStem.startsWith("saga_")) {
            return "dragonminez:geo/entity/races/" + fileStem + ".geo.json";
        }
        if (slimHint && !fileStem.contains("slim") && !fileStem.contains("fem")) {
            return "dragonminez:geo/entity/races/" + fileStem + "_slim.geo.json";
        }
        return "dragonminez:geo/entity/races/" + fileStem + ".geo.json";
    }

    private static boolean slimBody(Character ch) {
        try {
            return ch.getBodyType() == 1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int bodyType(Character ch) {
        try {
            return ch.getBodyType();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static boolean isFemale(Character ch) {
        try {
            if (ch.getBodyType() == 1) {
                return true;
            }
            String gender = ch.getGender();
            return gender != null && gender.equalsIgnoreCase("female");
        } catch (Throwable ignored) {
            return false;
        }
    }
}
