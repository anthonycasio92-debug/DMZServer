package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.character.Character;
import java.util.Locale;

/** Resolves DMZ {@link Character} appearance to CNPC-Gecko model paths (SDU-style). */
final class DmzPreviewGeo {
    static final String SAGA_BASE_ANIM = "dragonminez:animations/entity/sagas/saga_base.animation.json";

    private DmzPreviewGeo() {}

    static String resolveModelGeo(Character ch) {
        if (ch == null) {
            return raceGeo("human", false);
        }
        String key = ch.getRenderLogicKey();
        if (key == null || key.isBlank()) {
            key = "human";
        }
        key = key.toLowerCase(Locale.ROOT).trim();

        if (key.startsWith("saga_")) {
            return "dragonminez:geo/entity/sagas/" + key + ".geo.json";
        }

        RaceCharacterConfig raceCfg = null;
        try {
            String raceName = ch.getRaceName();
            if (raceName != null && !raceName.isBlank()) {
                raceCfg = ConfigManager.getRaceCharacter(raceName);
            }
        } catch (Throwable ignored) {
        }
        if (raceCfg != null && Boolean.TRUE.equals(raceCfg.hasCustomModel())) {
            String cm = raceCfg.getCustomModel();
            if (cm != null && !cm.isBlank()) {
                key = cm.toLowerCase(Locale.ROOT).trim();
            }
        }

        if (key.startsWith("saga_")) {
            return "dragonminez:geo/entity/sagas/" + key + ".geo.json";
        }

        key = mapRaceRenderKey(key, ch);
        return raceGeo(key, slimBody(ch));
    }

    private static String mapRaceRenderKey(String key, Character ch) {
        return switch (key) {
            case "saiyan", "ancient_saiyan", "sento_saiyan", "half_saiyan" -> slimBody(ch) ? "human_slim" : "human";
            case "human" -> slimBody(ch) ? "human_slim" : "human";
            case "majin" -> slimBody(ch) ? "majin_slim" : "majin";
            case "buffed", "h_buffed" -> slimBody(ch) ? "hbuffed_slim" : "hbuffed";
            case "h4arms", "four_arms" -> slimBody(ch) ? "h4armsslim" : "h4arms";
            default -> key.replace(' ', '_');
        };
    }

    private static String raceGeo(String fileStem, boolean slimHint) {
        if (fileStem.endsWith("_slim") || fileStem.startsWith("saga_")) {
            return "dragonminez:geo/entity/races/" + fileStem + ".geo.json";
        }
        if (slimHint && !fileStem.contains("slim")) {
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
}
