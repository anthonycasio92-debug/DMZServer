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
            lines.add("§7DMZ level §f" + DmzRewards.formatWhole(level) + " §8· §7Overhaul scale §f" + scale);
        } catch (Throwable ignored) {
        }

        appendDifficulty(lines, who);
        appendRivals(lines, who);
        appendSparring(lines, who);

        if (skillCheck && hubPh != null && "true".equals(hubPh.get("skillcheck_session"))) {
            lines.add("§7Skill Check session is open — finish it from the hub when ready.");
        } else if (staff) {
            lines.add("§7Staff tip: §fStaff Admin §7lives under this hub (Progression is inside it).");
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
                lines.add("§7Difficulty §ePersonal OFF §8— open §fDifficulty §8to turn it on and pick a tier");
                return;
            }
            if (snap.activeTier <= 0 || snap.activeDifficulty <= 0) {
                lines.add("§7Difficulty §7not active §8— unlock a tier in §fDifficulty");
                return;
            }
            String state = snap.state();
            lines.add("§7Difficulty §f" + snap.activeTierName + " §8· §" + snap.stateColorCode() + state
                    + " §8(CR §f" + DmzRewards.formatWhole(snap.combatRating) + "§8)");
            lines.add("§7Team mode §f" + teamLabel(snap.teamMode) + " §8· §7Active value §f"
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
            lines.add("§7Rivals §f" + mutual + "§7/§f" + max + " mutual §8· §7Record §f"
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
                lines.add("§7Spar session §aACTIVE"
                        + (partner.isBlank() ? "" : " §8· §7with §f" + partner));
                return;
            }
            int streak = parseInt(sph.get("streak"), 0);
            int best = parseInt(sph.get("streak_best"), 0);
            if (streak > 0) {
                lines.add("§7Training streak §f" + streak + " day" + (streak == 1 ? "" : "s")
                        + " §8(best §f" + best + "§8) §7— keep sparring with your bond partner");
                return;
            }
            if ("true".equals(sph.get("mentor_bonded"))) {
                String bond = sph.getOrDefault("mentor", "").trim();
                if (!bond.isBlank()) {
                    lines.add("§7Spar bond §f" + bond + " §8· §7Spar today to start a day streak");
                    return;
                }
            }
            String dojo = sph.getOrDefault("dojo_name", "").trim();
            if (!dojo.isBlank()) {
                lines.add("§7Home dojo §f" + dojo + " §8· §7Open §fSpar §7for mentor or spar TP");
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
