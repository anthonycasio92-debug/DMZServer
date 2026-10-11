package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
import com.dbzlegacy.adaptivedifficulty.gui.RivalGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.SparGuiApi;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/** Shared player snapshot lines for CNPC hub and main menus. */
public final class CnpcPlayerSnapshot {
    /** Max lines shown in hub info band — keep readable with {@link CnpcGuiStyle#readableInfoLine}. */
    public static final int HUB_INFO_LINES = 6;

    private CnpcPlayerSnapshot() {}

    public static List<String> hubLines(ServerPlayer who, Map<String, String> hubPh, boolean staff, boolean skillCheck) {
        List<String> lines = new ArrayList<>();
        if (who == null) {
            return lines;
        }
        try {
            int level = DmzProgression.guiDisplayDmzLevel(who);
            String scale = hubPh == null ? "x1" : hubPh.getOrDefault("overhaul_scale", "x1");
            lines.add(CnpcUltraStyle.SUBTITLE + "DMZ level " + CnpcUltraStyle.BODY + DmzRewards.formatWhole(level) + " " + CnpcUltraStyle.DIM + "· " + CnpcUltraStyle.SUBTITLE + "Overhaul scale " + CnpcUltraStyle.BODY + scale);
        } catch (Throwable ignored) {
        }

        appendDifficulty(lines, who);
        appendRivals(lines, who);
        appendSparring(lines, who);

        if (skillCheck && hubPh != null && "true".equals(hubPh.get("skillcheck_session"))) {
            lines.add(CnpcUltraStyle.SUBTITLE + "Skill Check session is open — finish it from the hub when ready.");
        } else if (staff) {
            lines.add(CnpcUltraStyle.SUBTITLE + "Staff tip: " + CnpcUltraStyle.BODY + "Staff Admin " + CnpcUltraStyle.SUBTITLE + "lives under this hub (Progression is inside it).");
        }

        return lines;
    }

    private static void appendDifficulty(List<String> lines, ServerPlayer who) {
        if (!DifficultyConfig.isEnabled()) {
            return;
        }
        try {
            DifficultySnapshot snap = DifficultyCache.refresh(who);
            if (!snap.personalEnabled) {
                lines.add(CnpcUltraStyle.SUBTITLE + "Difficulty " + CnpcUltraStyle.INFO + "Personal OFF " + CnpcUltraStyle.DIM + "— open " + CnpcUltraStyle.BODY + "Difficulty " + CnpcUltraStyle.DIM + "to turn it on and pick a tier");
                return;
            }
            if (snap.activeTier <= 0 || snap.activeDifficulty <= 0) {
                lines.add(CnpcUltraStyle.SUBTITLE + "Difficulty " + CnpcUltraStyle.SUBTITLE + "not active " + CnpcUltraStyle.DIM + "— unlock a tier in " + CnpcUltraStyle.BODY + "Difficulty");
                return;
            }
            String state = snap.state();
            lines.add(CnpcUltraStyle.SUBTITLE + "Difficulty " + CnpcUltraStyle.BODY + snap.activeTierName + " " + CnpcUltraStyle.DIM + "· " + CnpcUltraStyle.MARK + snap.stateColorCode() + state
                    + " " + CnpcUltraStyle.DIM + "(CR " + CnpcUltraStyle.BODY + DmzRewards.formatWhole(snap.combatRating) + CnpcUltraStyle.DIM + ")");
            lines.add(CnpcUltraStyle.SUBTITLE + "Team mode " + CnpcUltraStyle.BODY + teamLabel(snap.teamMode) + " " + CnpcUltraStyle.DIM + "· " + CnpcUltraStyle.SUBTITLE + "Active value " + CnpcUltraStyle.BODY
                    + DmzRewards.formatWhole(snap.activeDifficulty));
        } catch (Throwable ignored) {
        }
    }

    private static void appendRivals(List<String> lines, ServerPlayer who) {
        try {
            Map<String, String> rph = RivalGuiApi.placeholders(who);
            if (!"true".equals(rph.get("system_enabled"))) {
                return;
            }
            int mutual = parseInt(rph.get("mutual"), 0);
            int max = parseInt(rph.get("mutual_max"), 3);
            lines.add(CnpcUltraStyle.SUBTITLE + "Rivals " + CnpcUltraStyle.BODY + mutual + CnpcUltraStyle.SUBTITLE + "/" + CnpcUltraStyle.BODY + max + " mutual " + CnpcUltraStyle.DIM + "· " + CnpcUltraStyle.SUBTITLE + "Record " + CnpcUltraStyle.BODY
                    + rph.getOrDefault("wins", "0") + "W "
                    + rph.getOrDefault("losses", "0") + "L");
        } catch (Throwable ignored) {
        }
    }

    private static void appendSparring(List<String> lines, ServerPlayer who) {
        try {
            Map<String, String> sph = SparGuiApi.placeholders(who);
            if (!"true".equals(sph.get("system_enabled"))) {
                return;
            }
            if ("true".equals(sph.get("session_active"))) {
                String partner = sph.getOrDefault("partner", "").trim();
                lines.add(CnpcUltraStyle.SUBTITLE + "Spar session " + CnpcUltraStyle.CONFIRM + "ACTIVE"
                        + (partner.isBlank() ? "" : " " + CnpcUltraStyle.DIM + "· " + CnpcUltraStyle.SUBTITLE + "with " + CnpcUltraStyle.BODY + partner));
                return;
            }
            int streak = parseInt(sph.get("streak"), 0);
            int best = parseInt(sph.get("streak_best"), 0);
            if (streak > 0) {
                lines.add(CnpcUltraStyle.SUBTITLE + "Training streak " + CnpcUltraStyle.BODY + streak + " day" + (streak == 1 ? "" : "s")
                        + " " + CnpcUltraStyle.DIM + "(best " + CnpcUltraStyle.BODY + best + CnpcUltraStyle.DIM + ") " + CnpcUltraStyle.SUBTITLE + "— keep sparring with your bond partner");
                return;
            }
            if ("true".equals(sph.get("mentor_bonded"))) {
                String bond = sph.getOrDefault("mentor", "").trim();
                if (!bond.isBlank()) {
                    lines.add(CnpcUltraStyle.SUBTITLE + "Spar bond " + CnpcUltraStyle.BODY + bond + " " + CnpcUltraStyle.DIM + "· " + CnpcUltraStyle.SUBTITLE + "Spar today to start a day streak");
                    return;
                }
            }
            String dojo = sph.getOrDefault("dojo_name", "").trim();
            if (!dojo.isBlank()) {
                lines.add(CnpcUltraStyle.SUBTITLE + "Home dojo " + CnpcUltraStyle.BODY + dojo + " " + CnpcUltraStyle.DIM + "· " + CnpcUltraStyle.SUBTITLE + "Open " + CnpcUltraStyle.BODY + "Spar " + CnpcUltraStyle.SUBTITLE + "for mentor or spar TP");
            }
        } catch (Throwable ignored) {
        }
    }

    private static String teamLabel(TeamMode mode) {
        if (mode == null) {
            return "Solo";
        }
        return switch (mode) {
            case THRESHOLD_BONUS_ONLY -> "Team bonus";
            case FULL_TEAM_SCALING -> "Full team";
            default -> "Personal only";
        };
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
