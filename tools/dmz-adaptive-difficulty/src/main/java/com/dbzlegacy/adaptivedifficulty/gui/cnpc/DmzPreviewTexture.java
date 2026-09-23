package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.character.Character;
import java.util.Locale;

/** DMZ race skin paths for CNPC Gecko preview ({@code applyTextureSkin}). */
final class DmzPreviewTexture {
    private static final String NS = "dragonminez:";

    private DmzPreviewTexture() {}

    /**
     * Primary layered texture for CNPC Gecko, or {@code null} to use the player's vanilla skin
     * (human / saiyan / slim humanoid races).
     */
    static String resolveSkinPath(Character ch) {
        if (ch == null) {
            return null;
        }
        String race = normalizeRace(ch.getRaceName());
        if (race.isBlank()) {
            race = normalizeRace(fallbackRace(ch));
        }
        try {
            RaceCharacterConfig rc = ConfigManager.getRaceCharacter(race);
            if (rc != null && Boolean.TRUE.equals(rc.getUseVanillaSkin())) {
                return null;
            }
        } catch (Throwable ignored) {
        }
        String appearance = normalizeKey(DmzPreviewGeo.resolveAppearanceKeyForPreview(ch));
        String path = layeredPrimaryPath(race, appearance, bodyType(ch));
        if (path != null) {
            return NS + path;
        }
        return null;
    }

    static boolean usesPlayerSkin(Character ch) {
        return resolveSkinPath(ch) == null;
    }

    private static String layeredPrimaryPath(String race, String appearance, int bodyType) {
        int bt = Math.max(0, Math.min(2, bodyType));
        if ("namekian".equals(race) || appearance.startsWith("namekian")) {
            return "textures/entity/races/namekian/bodytype_" + bt + "_layer1.png";
        }
        if ("bioandroid".equals(race) || appearance.contains("bioandroid")) {
            return bioAndroidLayer1(appearance, bt);
        }
        if ("frostdemon".equals(race) || appearance.startsWith("frostdemon")) {
            return frostDemonLayer1(appearance, bt);
        }
        if ("majin".equals(race) || appearance.startsWith("majin")) {
            return "textures/entity/races/majin/bodytype_" + bt + "_layer1.png";
        }
        if ("oozaru".equals(appearance) || "goldenoozaru".equals(appearance)) {
            return "textures/entity/races/oozaru/bodytype_" + bt + "_layer1.png";
        }
        return null;
    }

    private static String bioAndroidLayer1(String appearance, int bodyType) {
        return switch (appearance) {
            case "bioandroid_semi" -> "textures/entity/races/bioandroid/semiperfect_" + bodyType + "_layer1.png";
            case "bioandroid_perfect" -> "textures/entity/races/bioandroid/perfect_" + bodyType + "_layer1.png";
            case "bioandroid_ultra" -> "textures/entity/races/bioandroid/perfect_" + bodyType + "_layer1.png";
            case "bioandroid_xeno" -> "textures/entity/races/bioandroid/xenoform_layer1.png";
            default -> "textures/entity/races/bioandroid/base_" + bodyType + "_layer1.png";
        };
    }

    private static String frostDemonLayer1(String appearance, int bodyType) {
        return switch (appearance) {
            case "frostdemon_second" -> "textures/entity/races/frostdemon/second_bodytype_" + bodyType + "_layer1.png";
            case "frostdemon_third" -> "textures/entity/races/frostdemon/third_bodytype_" + bodyType + "_layer1.png";
            case "frostdemon_fifth" -> "textures/entity/races/frostdemon/fifth_bodytype_" + bodyType + "_layer1.png";
            case "frostdemon_fp", "frostdemon_final" ->
                    "textures/entity/races/frostdemon/finalform_bodytype_" + bodyType + "_layer1.png";
            case "frostdemon_metalcore" ->
                    "textures/entity/races/frostdemon/metalcore_bodytype_" + bodyType + "_layer1.png";
            default -> "textures/entity/races/frostdemon/bodytype_" + bodyType + "_layer1.png";
        };
    }

    private static String normalizeRace(String race) {
        if (race == null) {
            return "";
        }
        String s = race.toLowerCase(Locale.ROOT).trim().replace(' ', '_').replace('-', '_');
        return switch (s) {
            case "bio_android", "bio-android", "bioandroid_base" -> "bioandroid";
            default -> s;
        };
    }

    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        return key.toLowerCase(Locale.ROOT).trim().replace(' ', '_');
    }

    private static String fallbackRace(Character ch) {
        try {
            return ch.getRaceName();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static int bodyType(Character ch) {
        try {
            return ch.getBodyType();
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
