package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Canonical Ultra {@code screenId} values for {@code GuiOpenPacket}.
 *
 * <p>Protocol / docs list the full set LM may send. Live KaiPath 1.3.0
 * {@code LmScreens.registerDefaults()} only registers {@code hub} — any other id opens
 * {@code MissingScreen} ("Unknown screen"). LM must not wire unregistered ids; use CNPC for
 * those systems until KaiPath registers them.
 */
public final class UltraScreenIds {
    /** IDs documented in {@code docs/KAIPATH_GUI_PROTOCOL.md} / LM builders. */
    public static final Set<String> PROTOCOL_IDS = Set.of(
            "hub",
            "difficulty",
            "rival",
            "spar",
            "prestige",
            "character",
            "progression",
            "config",
            "admin");

    /**
     * Screen ids registered by KaiPath 1.3.0 {@code LmScreens.registerDefaults()}.
     * Update this when KaiPath adds handlers — LM must match exactly.
     */
    public static final Set<String> KAIPATH_REGISTERED_IDS = Set.of("hub");

    /**
     * Every {@code UltraScreenSpec.of("…")} id used by Ultra*Screens builders.
     * Keep in sync with {@code UltraHubScreens}, {@code UltraDifficultyScreens}, etc.
     */
    public static final Set<String> BUILDER_IDS = Set.of(
            "hub",
            "admin",
            "difficulty",
            "rival",
            "spar",
            "prestige",
            "character",
            "progression",
            "config");

    private UltraScreenIds() {}

    public static boolean isProtocolId(String screenId) {
        return screenId != null && PROTOCOL_IDS.contains(screenId.trim().toLowerCase(Locale.ROOT));
    }

    /** True when KaiPath 1.3.0 can open this wire {@code screenId} without MissingScreen. */
    public static boolean kaipathCanOpen(String screenId) {
        if (screenId == null || screenId.isBlank()) {
            return true; // blank → hub on client
        }
        return KAIPATH_REGISTERED_IDS.contains(screenId.trim().toLowerCase(Locale.ROOT));
    }

    public static String normalizeLogical(String system) {
        if (system == null || system.isBlank()) {
            return "hub";
        }
        return switch (system.trim().toLowerCase(Locale.ROOT)) {
            case "hub", "lm", "legacymechanics", "main" -> "hub";
            case "difficulty", "diff" -> "difficulty";
            case "rival" -> "rival";
            case "spar", "sparring" -> "spar";
            case "prestige" -> "prestige";
            case "character", "charservices", "char" -> "character";
            case "saga", "sagareset", "saga_reset" -> "character";
            case "progression", "prog" -> "progression";
            case "config", "cfgedit", "configeditor" -> "config";
            case "admin" -> "admin";
            case "android_remove", "androidremove" -> "character";
            case "team" -> "difficulty";
            case "skills", "skill", "skillcheck", "skill_check", "logs", "syslog" -> "hub";
            default -> system.trim().toLowerCase(Locale.ROOT);
        };
    }

    /**
     * Startup check: every {@code UltraScreenSpec.of("…")} id must be in {@link #PROTOCOL_IDS}.
     * Warn when protocol ids are not yet registered in KaiPath (CNPC fallback path).
     */
    public static void validateAtStartup(Set<String> builderScreenIds) {
        Set<String> found = new TreeSet<>();
        if (builderScreenIds != null) {
            for (String id : builderScreenIds) {
                if (id != null && !id.isBlank()) {
                    found.add(id.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        Set<String> unknown = new LinkedHashSet<>();
        for (String id : found) {
            if (!PROTOCOL_IDS.contains(id)) {
                unknown.add(id);
            }
        }
        if (!unknown.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra screen builders use ids not in protocol list: {} — "
                            + "fix builders or update docs/KAIPATH_GUI_PROTOCOL.md",
                    AdaptiveDifficultyMod.MOD_ID,
                    unknown);
        }
        Set<String> missingOnKaiPath = new LinkedHashSet<>();
        for (String id : found) {
            if (PROTOCOL_IDS.contains(id) && !KAIPATH_REGISTERED_IDS.contains(id)) {
                missingOnKaiPath.add(id);
            }
        }
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] Ultra screen ids: builders={} protocol={} kaipathRegistered={} "
                        + "(unregistered → CNPC fallback until KaiPath LmScreens grows)",
                AdaptiveDifficultyMod.MOD_ID,
                found,
                PROTOCOL_IDS,
                KAIPATH_REGISTERED_IDS);
        if (!missingOnKaiPath.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] KaiPath 1.3.0 LmScreens only registers {}; LM will not send wire "
                            + "screenIds {} (avoids client Unknown screen). CNPC opens those menus.",
                    AdaptiveDifficultyMod.MOD_ID,
                    KAIPATH_REGISTERED_IDS,
                    missingOnKaiPath);
        }
    }
}
