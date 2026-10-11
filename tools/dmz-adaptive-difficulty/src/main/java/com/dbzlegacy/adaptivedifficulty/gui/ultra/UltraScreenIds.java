package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Canonical Ultra {@code screenId} values for {@code GuiOpenPacket}.
 *
 * <p>{@link #KAIPATH_REGISTERED_IDS} is the source of truth for which wire ids KaiPath can open.
 * Builders still use logical ids; {@code LmGuiNetwork} tunnels any id not in that set through
 * {@code hub} on the wire (KaiPath 1.3.0 compatibility shim). Expand this set when KaiPath
 * registers more {@code LmScreens} — one-line change turns the shim off for those ids.
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
     * Wire {@code screenId} values KaiPath {@code LmScreens.registerDefaults()} can open.
     * 1.3.0 registers {@code hub} only. Add ids here as KaiPath grows to disable the hub shim.
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

    /** layoutJson / button-args key for the logical system when wire screenId is hub. */
    public static final String META_SYSTEM = "system";

    private UltraScreenIds() {}

    public static boolean isProtocolId(String screenId) {
        return screenId != null && PROTOCOL_IDS.contains(screenId.trim().toLowerCase(Locale.ROOT));
    }

    /** True when KaiPath can open this wire {@code screenId} without MissingScreen. */
    public static boolean kaipathCanOpen(String screenId) {
        if (screenId == null || screenId.isBlank()) {
            return true;
        }
        return KAIPATH_REGISTERED_IDS.contains(screenId.trim().toLowerCase(Locale.ROOT));
    }

    /** True when this logical id must be tunneled through wire {@code hub}. */
    public static boolean needsHubShim(String logicalScreenId) {
        return !kaipathCanOpen(logicalScreenId);
    }

    /** Builder / protocol ids that currently require the hub wire shim. */
    public static Set<String> hubShimTunnelIds() {
        Set<String> tunneled = new TreeSet<>();
        for (String id : BUILDER_IDS) {
            if (needsHubShim(id)) {
                tunneled.add(id);
            }
        }
        return tunneled;
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
     * Startup check: every builder id must be in {@link #PROTOCOL_IDS}. Logs a single info line
     * listing ids tunneled through hub for KaiPath compatibility.
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
        Set<String> tunneled = hubShimTunnelIds();
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] Ultra screen ids: builders={} protocol={} kaipathRegistered={}",
                AdaptiveDifficultyMod.MOD_ID,
                found,
                PROTOCOL_IDS,
                KAIPATH_REGISTERED_IDS);
        if (!tunneled.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Ultra submenu shim active: tunneling {} through hub screenId "
                            + "for KaiPath compatibility",
                    AdaptiveDifficultyMod.MOD_ID,
                    tunneled);
        }
    }
}
