package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionSystem;
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
        // Skills
        out.put("flag_master", c.enableProgression ? "true" : "false");
        out.put("flag_flight", c.enableFlightProgression ? "true" : "false");
        out.put("flag_sprint", c.enableSprintJump ? "true" : "false");
        out.put("flag_meditation", c.enableMeditation ? "true" : "false");
        out.put("flag_potential", c.enablePotential ? "true" : "false");
        // TP Gains
        out.put("flag_farming", c.enableFarmingTp ? "true" : "false");
        out.put("flag_building", c.enableBuildingTp ? "true" : "false");
        out.put("flag_boost", c.enableGlobalTpBoost ? "true" : "false");
        out.put("flag_bio", c.enableBioAndroid ? "true" : "false");
        // Race & Form
        out.put("flag_racelock", c.enableRaceLock ? "true" : "false");
        out.put("flag_yardrat", c.enableYardrat ? "true" : "false");
        out.put("flag_spiritualist", c.enableSpiritualistKi ? "true" : "false");
        out.put("flag_android", c.enableAndroidConversion ? "true" : "false");
        // Combat
        out.put("flag_kiweapons", c.enableKiWeapons ? "true" : "false");
        out.put("flag_piercing", c.enablePiercingBonus ? "true" : "false");
        out.put("flag_dot", c.enableDotExtraDamage ? "true" : "false");
        out.put("flag_apothic", c.enableApothicElemental ? "true" : "false");
        // End
        out.put("flag_end", c.enableEndDimensionStrength ? "true" : "false");
        out.put("flag_endportal", c.enableEndPortalGuard ? "true" : "false");
        // Shop
        out.put("flag_skills", c.enableSkillUnlockService ? "true" : "false");
        out.put("flag_prestige", c.enablePrestigeSystem ? "true" : "false");
        // Utility
        out.put("flag_shadow", c.enableShadowDummyLimiter ? "true" : "false");
        out.put("flag_statchecker", c.enablePlayerStatChecker ? "true" : "false");
        out.put("flag_playerstatchecker", c.enablePlayerStatChecker ? "true" : "false");
        // Fabled bridges
        out.put("flag_fabled", c.enableFabledBridge ? "true" : "false");
        out.put("flag_energy", c.enableEnergyManaSync ? "true" : "false");
        out.put("flag_statscreen", c.enableStatScreenSync ? "true" : "false");
        out.put("flag_tpsp", c.enableTpSpMirror ? "true" : "false");
        out.put("flag_attr", c.enableAttrMultiBonus ? "true" : "false");
        out.put("flag_prestigeskill", c.enablePrestigeSkillSync ? "true" : "false");
        out.put("flag_faction", c.enablePrestigeFactionSync ? "true" : "false");
        out.put("flag_cleaner", c.enableValueCleaner ? "true" : "false");
        out.put("flag_raceclass", c.enableRaceClassSync ? "true" : "false");
        out.put("flag_classperm", c.enableClassPermissionSync ? "true" : "false");
        return out;
    }

    public static List<String> linesForPage(ServerPlayer player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        if (!DifficultyConfig.get().enableProgression && !"help".equals(p)) {
            return List.of("§cProgression system is disabled.");
        }
        Map<String, String> ph = placeholders(player);
        return switch (p) {
            case "status" -> statusLines(player);
            case "skills" -> categoryLines(
                    "§e§lSkills",
                    "§7Passive skill unlocks from Fly, SprintJump,",
                    "§7Meditation, and Potential scripts.",
                    ph,
                    flagLine("Flight", "flag_flight"),
                    flagLine("Sprint Jump", "flag_sprint"),
                    flagLine("Meditation", "flag_meditation"),
                    flagLine("Potential", "flag_potential"));
            case "tp" -> categoryLines(
                    "§6§lTP Gains",
                    "§7Training-point sources: farming, building,",
                    "§7global boost, and Bio-Android absorb.",
                    ph,
                    flagLine("Farming TP", "flag_farming"),
                    flagLine("Building TP", "flag_building"),
                    flagLine("Global TP Boost", "flag_boost"),
                    flagLine("Bio-Android Absorb", "flag_bio"));
            case "race" -> categoryLines(
                    "§b§lRace & Form",
                    "§7Race lock, Yardrat, Spiritualist Ki,",
                    "§7and Android conversion ports.",
                    ph,
                    flagLine("DMZ Race Lock", "flag_racelock"),
                    flagLine("Yardrat", "flag_yardrat"),
                    flagLine("Spiritualist Ki", "flag_spiritualist"),
                    flagLine("Android Conversion", "flag_android"));
            case "combat" -> categoryLines(
                    "§c§lCombat",
                    "§7Ki weapons, piercing, DoT extra damage,",
                    "§7and Apothic elemental bridges.",
                    ph,
                    flagLine("Ki Weapons", "flag_kiweapons"),
                    flagLine("Piercing", "flag_piercing"),
                    flagLine("DoT Extra Damage", "flag_dot"),
                    flagLine("Apothic Elemental", "flag_apothic"));
            case "end" -> categoryLines(
                    "§5§lEnd",
                    "§7End Dimension Strength and portal guard.",
                    "",
                    ph,
                    flagLine("End Dimension Strength", "flag_end"),
                    flagLine("End Portal Guard", "flag_endportal"));
            case "shop" -> categoryLines(
                    "§a§lShop",
                    "§7Prestige levels and skill unlock service.",
                    "§7Use buttons below to open those GUIs.",
                    ph,
                    flagLine("Prestige System", "flag_prestige"),
                    flagLine("Skill Unlock Service", "flag_skills"));
            case "fabled" -> categoryLines(
                    "§d§lFabled Bridges",
                    "§7Soft Fabled / LuckPerms bridges — idle if",
                    "§7the plugin is missing (never hard-crash).",
                    ph,
                    flagLine("Fabled Master", "flag_fabled"),
                    flagLine("Energy ↔ Mana", "flag_energy"),
                    flagLine("Stat Screen Sync", "flag_statscreen"),
                    flagLine("TP ↔ SP Mirror", "flag_tpsp"),
                    flagLine("Attr Multi Bonus", "flag_attr"),
                    flagLine("Prestige Skill Sync", "flag_prestigeskill"),
                    flagLine("Prestige Faction Sync", "flag_faction"),
                    flagLine("Value Cleaner", "flag_cleaner"),
                    flagLine("Race → Class Sync", "flag_raceclass"),
                    flagLine("Class Permission Sync", "flag_classperm"));
            case "utility" -> categoryLines(
                    "§7§lUtility",
                    "§7Shadow dummy limiter and sneak-inspect",
                    "§7player stat checker.",
                    ph,
                    flagLine("Shadow Dummy Limiter", "flag_shadow"),
                    flagLine("Player Stat Checker", "flag_statchecker"));
            case "admin", "flags", "disable" -> {
                if (player == null || !StaffAccess.isStaff(player)) {
                    yield List.of("§cStaff only.");
                }
                yield flagLines(ph);
            }
            case "help" -> List.of(
                    "§6§l/progression §8— Natural Progression",
                    "§e/progression §7— Category hub (flags per section)",
                    "§e/prog do page skills|tp|race|combat|end|fabled|utility",
                    "§e/progression meditation §7— Current trial + how-to",
                    "§e/progression meditation next §7— Staff: cycle + broadcast",
                    "§e/progression boost start|end §7— Global TP boost",
                    "§e/progression android §7— Android convert",
                    "§e/prestige §7— Prestige (Hub)",
                    "§e/skills §7— Skill unlocks (Hub)",
                    "§8Staff: /prog admin · toggle flags in section GUIs"
            );
            default -> {
                List<String> lore = new ArrayList<>();
                lore.add(ph.getOrDefault("boost", "§7Global TP boost: §cOFF"));
                lore.add(ph.getOrDefault("meditation", "§7No active meditation trial."));
                lore.add("§8Browse categories to see script ports.");
                yield lore;
            }
        };
    }

    private static List<String> categoryLines(
            String title, String desc1, String desc2, Map<String, String> ph, String... featureLines) {
        List<String> lore = new ArrayList<>();
        lore.add(title);
        if (desc1 != null && !desc1.isEmpty()) {
            lore.add(desc1);
        }
        if (desc2 != null && !desc2.isEmpty()) {
            lore.add(desc2);
        }
        lore.add("");
        for (String line : featureLines) {
            // line format: "Label|flag_key"
            int bar = line.indexOf('|');
            if (bar < 0) {
                lore.add(line);
                continue;
            }
            String label = line.substring(0, bar);
            String key = line.substring(bar + 1);
            boolean on = "true".equalsIgnoreCase(ph.getOrDefault(key, "false"));
            lore.add("§7" + label + " " + (on ? "§aON" : "§cOFF"));
        }
        return lore;
    }

    private static String flagLine(String label, String placeholderKey) {
        return label + "|" + placeholderKey;
    }

    private static List<String> statusLines(ServerPlayer player) {
        List<String> lore = new ArrayList<>();
        for (String line : ProgressionSystem.statusSummary().split("\n")) {
            if (line == null || line.isBlank()) {
                continue;
            }
            lore.add(line.startsWith("§") ? line : "§7" + line);
        }
        return lore;
    }

    private static List<String> flagLines(Map<String, String> ph) {
        List<String> lore = new ArrayList<>();
        lore.add("§c§lStaff Flags");
        lore.add("§8Grouped by script category");
        lore.add("");
        lore.add("§e§lSkills");
        lore.add(flag("flight", ph));
        lore.add(flag("sprint", ph));
        lore.add(flag("meditation", ph));
        lore.add(flag("potential", ph));
        lore.add("§6§lTP Gains");
        lore.add(flag("farming", ph));
        lore.add(flag("building", ph));
        lore.add(flag("boost", ph));
        lore.add(flag("bio", ph));
        lore.add("§b§lRace & Form");
        lore.add(flag("racelock", ph));
        lore.add(flag("yardrat", ph));
        lore.add(flag("spiritualist", ph));
        lore.add(flag("android", ph));
        lore.add("§c§lCombat");
        lore.add(flag("kiweapons", ph));
        lore.add(flag("piercing", ph));
        lore.add(flag("dot", ph));
        lore.add(flag("apothic", ph));
        lore.add("§5§lEnd");
        lore.add(flag("end", ph));
        lore.add(flag("endportal", ph));
        lore.add("§a§lShop");
        lore.add(flag("prestige", ph));
        lore.add(flag("skills", ph));
        lore.add("§d§lFabled");
        lore.add(flag("fabled", ph));
        lore.add(flag("energy", ph));
        lore.add(flag("statscreen", ph));
        lore.add(flag("tpsp", ph));
        lore.add(flag("attr", ph));
        lore.add(flag("prestigeskill", ph));
        lore.add(flag("faction", ph));
        lore.add(flag("cleaner", ph));
        lore.add(flag("raceclass", ph));
        lore.add(flag("classperm", ph));
        lore.add("§7§lUtility");
        lore.add(flag("shadow", ph));
        lore.add(flag("statchecker", ph));
        return lore;
    }

    private static String flag(String key, Map<String, String> ph) {
        boolean on = "true".equalsIgnoreCase(ph.getOrDefault("flag_" + key, "false"));
        return "§7" + key + " " + (on ? "§aON" : "§cOFF");
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
                    "§6§l/skills §8— Skill Progress (staff)",
                    "§e/skills §7— Core skills",
                    "§e/skills do page advanced §7— DMZ 2.1",
                    "§e/skills do page saga §7— Saga unlocks",
                    "§e/skillcheck §7— Donator Skill Check"
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
}
