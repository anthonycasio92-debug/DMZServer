package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionSystem;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.FabledBridge;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem;
import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillUnlockService;
import com.dbzlegacy.adaptivedifficulty.progression.skills.MeditationProgression;
import com.dbzlegacy.adaptivedifficulty.progression.tp.GlobalTpBoost;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.dragonminez.common.stats.StatsData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Public static API for Bukkit companion reflection ({@code LegacyMechanicsGUI}).
 * Progression / Prestige / Skills status maps, lore lines, and {@code do} dispatch —
 * the companion plugin owns inventory reopen.
 */
public final class ProgressionGuiApi {
    private ProgressionGuiApi() {}

    // ── Progression ────────────────────────────────────────────────────

    public static Map<String, String> placeholders(ServerPlayer player) {
        Map<String, String> out = new HashMap<>();
        out.put("bridge_ok", "false");
        out.put("system_enabled", "false");
        if (player == null) {
            return out;
        }
        DifficultyConfig c = DifficultyConfig.get();
        boolean enabled = c.enableProgression;
        out.put("bridge_ok", "true");
        out.put("system_enabled", enabled ? "true" : "false");
        out.put("staff", StaffAccess.isStaff(player) ? "true" : "false");
        out.put("boost", GlobalTpBoost.statusLine());
        out.put("meditation", MeditationProgression.statusLine());
        out.put("flags", ProgressionConfig.statusSummary());
        out.put("prestige_enabled", c.enablePrestigeSystem ? "true" : "false");
        out.put("skills_enabled", c.enableSkillUnlockService ? "true" : "false");
        out.put("fabled_enabled", c.enableFabledBridge ? "true" : "false");
        if (!enabled) {
            return out;
        }
        // Individual flags for staff Flags page toggles
        out.put("flag_master", c.enableProgression ? "true" : "false");
        out.put("flag_flight", c.enableFlightProgression ? "true" : "false");
        out.put("flag_sprint", c.enableSprintJump ? "true" : "false");
        out.put("flag_meditation", c.enableMeditation ? "true" : "false");
        out.put("flag_potential", c.enablePotential ? "true" : "false");
        out.put("flag_farming", c.enableFarmingTp ? "true" : "false");
        out.put("flag_building", c.enableBuildingTp ? "true" : "false");
        out.put("flag_boost", c.enableGlobalTpBoost ? "true" : "false");
        out.put("flag_bio", c.enableBioAndroid ? "true" : "false");
        out.put("flag_racelock", c.enableRaceLock ? "true" : "false");
        out.put("flag_yardrat", c.enableYardrat ? "true" : "false");
        out.put("flag_spiritualist", c.enableSpiritualistKi ? "true" : "false");
        out.put("flag_android", c.enableAndroidConversion ? "true" : "false");
        out.put("flag_endportal", c.enableEndPortalGuard ? "true" : "false");
        out.put("flag_skills", c.enableSkillUnlockService ? "true" : "false");
        out.put("flag_prestige", c.enablePrestigeSystem ? "true" : "false");
        out.put("flag_fabled", c.enableFabledBridge ? "true" : "false");
        return out;
    }

    public static List<String> linesForPage(ServerPlayer player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        if (!DifficultyConfig.get().enableProgression && !"help".equals(p)) {
            return List.of("§cProgression system is disabled.");
        }
        return switch (p) {
            case "status" -> statusLines(player);
            case "fabled" -> fabledLines(player);
            case "admin", "flags", "disable" -> {
                if (player == null || !StaffAccess.isStaff(player)) {
                    yield List.of("§cStaff only.");
                }
                yield flagLines(player);
            }
            case "help" -> List.of(
                    "§6§l/progression §8— Natural Progression",
                    "§e/progression §7— This menu",
                    "§e/progression boost start|end §7— Global TP boost",
                    "§e/progression meditation next §7— Cycle trial",
                    "§e/progression android §7— Android convert",
                    "§e/prestige §7— Prestige levels",
                    "§e/skills §7— Skill unlock progress",
                    "§8Staff: /prog admin <flag> on|off"
            );
            default -> {
                Map<String, String> ph = placeholders(player);
                List<String> lore = new ArrayList<>();
                lore.add(stripSection(ph.getOrDefault("boost", "§7Global TP boost: §cOFF")));
                lore.add(stripSection(ph.getOrDefault("meditation", "§7No active meditation trial.")));
                lore.add("§8" + ph.getOrDefault("flags", ""));
                yield lore;
            }
        };
    }

    private static List<String> statusLines(ServerPlayer player) {
        List<String> lore = new ArrayList<>();
        for (String line : ProgressionSystem.statusSummary().split("\n")) {
            lore.add("§7" + line.replace('§', '§'));
        }
        return lore;
    }

    private static List<String> fabledLines(ServerPlayer player) {
        List<String> lore = new ArrayList<>();
        lore.add("§6§lFabled Bridge");
        for (String part : FabledBridge.statusSummary().split(" ")) {
            lore.add("§7" + part);
        }
        return lore;
    }

    private static List<String> flagLines(ServerPlayer player) {
        DifficultyConfig c = DifficultyConfig.get();
        List<String> lore = new ArrayList<>();
        lore.add("§c§lStaff Flags");
        lore.add(flag("master", c.enableProgression));
        lore.add(flag("flight", c.enableFlightProgression));
        lore.add(flag("sprint", c.enableSprintJump));
        lore.add(flag("meditation", c.enableMeditation));
        lore.add(flag("potential", c.enablePotential));
        lore.add(flag("farming", c.enableFarmingTp));
        lore.add(flag("building", c.enableBuildingTp));
        lore.add(flag("boost", c.enableGlobalTpBoost));
        lore.add(flag("bio", c.enableBioAndroid));
        lore.add(flag("racelock", c.enableRaceLock));
        lore.add(flag("yardrat", c.enableYardrat));
        lore.add(flag("spiritualist", c.enableSpiritualistKi));
        lore.add(flag("android", c.enableAndroidConversion));
        lore.add(flag("endportal", c.enableEndPortalGuard));
        lore.add(flag("skills", c.enableSkillUnlockService));
        lore.add(flag("prestige", c.enablePrestigeSystem));
        lore.add(flag("fabled", c.enableFabledBridge));
        return lore;
    }

    private static String flag(String key, boolean on) {
        return "§7" + key + " §f" + (on ? "§aON" : "§cOFF");
    }

    /**
     * Dispatch {@code /progression do} actions. Does not reopen GUI — caller reopens.
     *
     * @param action {@code page}, {@code flag}, {@code refresh}
     * @param arg    page name, or flag key (toggles), or {@code key:on}/{@code key:off}
     */
    public static String handleDo(ServerPlayer player, String action, String arg, String page) {
        if (player == null) {
            return "§cPlayers only.";
        }
        if (!DifficultyConfig.get().enableProgression) {
            return "§cProgression system is disabled.";
        }
        String act = action == null ? "" : action.toLowerCase(Locale.ROOT).trim();
        String a = arg == null ? "" : arg.trim();
        if ("page".equals(act) || "refresh".equals(act)) {
            return "";
        }
        if ("flag".equals(act) || "toggle".equals(act)) {
            if (!StaffAccess.isStaff(player)) {
                return "§cStaff only.";
            }
            String key = a;
            Boolean force = null;
            int colon = a.indexOf(':');
            if (colon > 0) {
                key = a.substring(0, colon).trim();
                String val = a.substring(colon + 1).trim();
                if ("on".equalsIgnoreCase(val) || "true".equalsIgnoreCase(val) || "1".equals(val)) {
                    force = true;
                } else if ("off".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val) || "0".equals(val)) {
                    force = false;
                }
            }
            if (key.isBlank()) {
                return "§cUsage: progression do flag <key>[:on|off]";
            }
            boolean next;
            if (force != null) {
                next = force;
            } else {
                // Toggle: read current from placeholders
                Map<String, String> ph = placeholders(player);
                String cur = ph.getOrDefault("flag_" + key.toLowerCase(Locale.ROOT), "false");
                next = !"true".equalsIgnoreCase(cur);
            }
            if (!ProgressionSystem.setFlag(key, next)) {
                return "§cUnknown flag: " + key;
            }
            return "§aProgression §f" + key + " §7→ §f" + (next ? "ON" : "OFF");
        }
        return "§cUnknown progression action: " + act;
    }

    // ── Prestige ───────────────────────────────────────────────────────

    public static Map<String, String> prestigePlaceholders(ServerPlayer player) {
        Map<String, String> out = new HashMap<>();
        out.put("bridge_ok", "false");
        out.put("system_enabled", "false");
        if (player == null) {
            return out;
        }
        boolean enabled = DifficultyConfig.get().enablePrestigeSystem;
        out.put("bridge_ok", "true");
        out.put("system_enabled", enabled ? "true" : "false");
        if (!enabled) {
            return out;
        }
        StatsData data = DmzProgression.stats(player);
        int level = 0;
        if (data != null) {
            try {
                level = Math.max(0, data.getLevel());
            } catch (Throwable ignored) {
            }
        }
        int completed = PrestigeSystem.getCompleted(player);
        int held = PrestigeSystem.getHeld(player);
        int required = PrestigeSystem.requiredLevel(completed);
        out.put("level", String.valueOf(level));
        out.put("level_fmt", DmzRewards.formatWhole(level));
        out.put("completed", String.valueOf(completed));
        out.put("held", String.valueOf(held));
        out.put("held_max", "10");
        out.put("required", String.valueOf(required));
        out.put("required_fmt", DmzRewards.formatWhole(required));
        out.put("ready", level >= required && held < 10 ? "true" : "false");
        return out;
    }

    public static List<String> prestigeLines(ServerPlayer player, String page) {
        Map<String, String> ph = prestigePlaceholders(player);
        if (!"true".equalsIgnoreCase(ph.get("bridge_ok"))) {
            return List.of("§cLegacyMechanics mod unreachable.");
        }
        if (!"true".equalsIgnoreCase(ph.get("system_enabled"))) {
            return List.of("§cPrestige system is disabled.");
        }
        List<String> lore = new ArrayList<>();
        lore.add("§7Completed: §f" + ph.getOrDefault("completed", "0")
                + " §8| §7Held: §6" + ph.getOrDefault("held", "0")
                + "§7/§f" + ph.getOrDefault("held_max", "10"));
        lore.add("§7DMZ Level: §f" + ph.getOrDefault("level_fmt", "0")
                + " §8| §7Need: §e" + ph.getOrDefault("required_fmt", "0"));
        if ("true".equalsIgnoreCase(ph.get("ready"))) {
            lore.add("§aReady to prestige");
        } else {
            lore.add("§cNot ready yet");
        }
        return lore;
    }

    /**
     * Dispatch {@code /prestige do} — {@code confirm} runs {@link PrestigeSystem#confirmOrPrompt}.
     */
    public static String handlePrestigeDo(ServerPlayer player, String action, String arg, String page) {
        if (player == null) {
            return "§cPlayers only.";
        }
        if (!DifficultyConfig.get().enablePrestigeSystem) {
            return "§cPrestige system is disabled.";
        }
        String act = action == null ? "" : action.toLowerCase(Locale.ROOT).trim();
        if ("page".equals(act) || "refresh".equals(act)) {
            return "";
        }
        if ("confirm".equals(act) || "buy".equals(act) || "purchase".equals(act)) {
            PrestigeSystem.confirmOrPrompt(player);
            return "";
        }
        return "§cUnknown prestige action: " + act;
    }

    // ── Skills ─────────────────────────────────────────────────────────

    public static Map<String, String> skillsPlaceholders(ServerPlayer player) {
        Map<String, String> out = new HashMap<>();
        out.put("bridge_ok", "false");
        out.put("system_enabled", "false");
        if (player == null) {
            return out;
        }
        boolean enabled = DifficultyConfig.get().enableSkillUnlockService;
        out.put("bridge_ok", "true");
        out.put("system_enabled", enabled ? "true" : "false");
        if (!enabled) {
            return out;
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            out.put("has_data", "false");
            return out;
        }
        out.put("has_data", "true");
        try {
            out.put("level", String.valueOf(Math.max(1, data.getLevel())));
        } catch (Throwable t) {
            out.put("level", "1");
        }
        try {
            out.put("ki_damage", String.format(Locale.ROOT, "%.1f", Math.max(0.0, data.getKiDamage())));
        } catch (Throwable t) {
            out.put("ki_damage", "0");
        }
        try {
            out.put("max_energy", String.format(Locale.ROOT, "%.1f", Math.max(0.0, data.getMaxEnergy())));
        } catch (Throwable t) {
            out.put("max_energy", "0");
        }
        try {
            out.put("strength", String.valueOf(Math.max(0, data.getStats().getStrength())));
        } catch (Throwable t) {
            out.put("strength", "0");
        }
        return out;
    }

    public static List<String> skillsLines(ServerPlayer player, String page) {
        if (player == null || !DifficultyConfig.get().enableSkillUnlockService) {
            return List.of("§cSkill unlock service is disabled.");
        }
        String p = page == null || page.isBlank() ? "core" : page.toLowerCase(Locale.ROOT);
        return switch (p) {
            case "advanced", "dmz" -> SkillUnlockService.advancedLines(player);
            case "saga" -> SkillUnlockService.sagaLines(player);
            case "help" -> List.of(
                    "§6§l/skills §8— Skill Progress",
                    "§e/skills §7— Core skills",
                    "§e/skills do page advanced §7— DMZ 2.1",
                    "§e/skills do page saga §7— Saga unlocks"
            );
            default -> SkillUnlockService.coreLines(player);
        };
    }

    /**
     * Dispatch {@code /skills do} — {@code page} with core/advanced/saga (reopen only).
     */
    public static String handleSkillsDo(ServerPlayer player, String action, String arg, String page) {
        if (player == null) {
            return "§cPlayers only.";
        }
        if (!DifficultyConfig.get().enableSkillUnlockService) {
            return "§cSkill unlock service is disabled.";
        }
        String act = action == null ? "" : action.toLowerCase(Locale.ROOT).trim();
        if ("page".equals(act) || "refresh".equals(act)) {
            return "";
        }
        return "§cUnknown skills action: " + act;
    }

    private static String stripSection(String s) {
        return s == null ? "" : s;
    }
}
