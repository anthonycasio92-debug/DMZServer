package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Canonical Ultra {@code screenId} values for {@code GuiOpenPacket}.
 *
 * <p>Protocol / LM builders send the full set. KaiPath owns client handlers; LM always wires
 * every builder id via {@code GuiOpenPacket} when Ultra is active (no CNPC submenu fallback).
 * {@link #KAIPATH_REGISTERED_IDS} is informational — mismatches log a warning at send time.
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
     * Screen ids last observed in KaiPath {@code LmScreens.registerDefaults()} (1.3.0: hub only).
     * Informational — Ultra still sends every builder {@code GuiOpenPacket}; update when KaiPath grows.
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

    /**
     * True when the id is in the last-known KaiPath registry set. Does not gate sends —
     * Ultra always wires {@code GuiOpenPacket} for builder ids.
     */
    public static boolean kaipathCanOpen(String screenId) {
        if (screenId == null || screenId.isBlank()) {
            return true;
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
     * Startup check: every builder id must be in {@link #PROTOCOL_IDS}. Warn when a protocol
     * id is absent from the last-known KaiPath registry (informational).
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
                "[{}] Ultra screen ids: builders={} protocol={} kaipathKnown={}",
                AdaptiveDifficultyMod.MOD_ID,
                found,
                PROTOCOL_IDS,
                KAIPATH_REGISTERED_IDS);
        if (!missingOnKaiPath.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra will still send GuiOpenPacket for {}; update "
                            + "UltraScreenIds.KAIPATH_REGISTERED_IDS when KaiPath LmScreens grows "
                            + "(or shrink the set if a client lacks a handler).",
                    AdaptiveDifficultyMod.MOD_ID,
                    missingOnKaiPath);
        }
    }
}
