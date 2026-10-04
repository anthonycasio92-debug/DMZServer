package com.dbzlegacy.adaptivedifficulty.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Staff-facing map from progression module flags to config fields and Java sources.
 * Full behavior lives in code (ports of old KubeJS/CNPC scripts), not in-game script files.
 */
public final class ProgressionModuleCatalog {
    private ProgressionModuleCatalog() {}

    public record ModuleInfo(String title, String configKey, String javaClass, String legacyScript) {}

    private static final Map<String, ModuleInfo> BY_FLAG = Map.ofEntries(
            Map.entry("flight", info("Flight", "enableFlightProgression",
                    "progression.skills.FlightProgression", "kubejs fly progression")),
            Map.entry("sprint", info("Sprint Jump", "enableSprintJump",
                    "progression.skills.SprintJumpProgression", "kubejs sprint/jump")),
            Map.entry("meditation", info("Meditation", "enableMeditation",
                    "progression.skills.MeditationProgression", "kubejs meditation trial")),
            Map.entry("potential", info("Potential", "enablePotential",
                    "progression.skills.PotentialProgression", "kubejs potential / spar")),
            Map.entry("farming", info("Farming TP", "enableFarmingTp",
                    "progression.tp.FarmingTp", "kubejs farming_tp")),
            Map.entry("building", info("Building TP", "enableBuildingTp",
                    "progression.tp.BuildingTp", "kubejs building_tp_place.js")),
            Map.entry("boost", info("Global TP Boost", "enableGlobalTpBoost",
                    "progression.tp.GlobalTpBoost", "kubejs global_tp_boost")),
            Map.entry("bio", info("Bio-Android", "enableBioAndroid",
                    "progression.tp.BioAndroidAbsorb", "kubejs bio android absorb")),
            Map.entry("racelock", info("Race Lock", "enableRaceLock",
                    "progression.race.RaceLock", "kubejs dmz race lock")),
            Map.entry("yardrat", info("Yardrat", "enableYardrat",
                    "progression.race.YardratProgression", "kubejs yardrat")),
            Map.entry("spiritualist", info("Spiritualist Ki", "enableSpiritualistKi",
                    "progression.race.SpiritualistKiControl", "kubejs spiritualist")),
            Map.entry("android", info("Android Conversion", "enableAndroidConversion",
                    "progression.race.AndroidConversion", "CNPC Dr. Gero script")),
            Map.entry("kiweapons", info("Ki Weapons", "enableKiWeapons",
                    "progression.combat.KiWeapons", "kubejs ki weapons")),
            Map.entry("piercing", info("Piercing", "enablePiercingBonus",
                    "progression.combat.PiercingBonus", "kubejs piercing")),
            Map.entry("dot", info("DoT Extra", "enableDotExtraDamage",
                    "progression.combat.DotExtraDamage", "kubejs dot extra")),
            Map.entry("apothic", info("Apothic Elemental", "enableApothicElemental",
                    "progression.combat.ApothicElemental", "kubejs apothic elemental")),
            Map.entry("end", info("End Strength", "enableEndDimensionStrength",
                    "progression.end.EndDimensionStrength", "kubejs end dimension")),
            Map.entry("endportal", info("End Portal Guard", "enableEndPortalGuard",
                    "progression.end.EndPortalGuard", "kubejs end portal")),
            Map.entry("prestige", info("Prestige System", "enablePrestigeSystem",
                    "progression.shop.PrestigeSystem", "kubejs prestige shop")),
            Map.entry("skills", info("Skill Unlock Service", "enableSkillUnlockService",
                    "progression.shop.SkillUnlockService", "kubejs skill unlock")),
            Map.entry("fabled", info("Fabled Master", "enableFabledBridge",
                    "progression.bridge.FabledBridge", "CNPC fabled bridge scripts")),
            Map.entry("energy", info("Energy ↔ Mana", "enableEnergyManaSync",
                    "progression.bridge.EnergyManaSync", "CNPC energy/mana")),
            Map.entry("statscreen", info("Stat Screen Sync", "enableStatScreenSync",
                    "progression.bridge.StatScreenSync", "CNPC stat screen")),
            Map.entry("tpsp", info("TP ↔ SP Mirror", "enableTpSpMirror",
                    "progression.bridge.TpSpMirror", "CNPC tp/sp mirror")),
            Map.entry("attr", info("Attr Multi Bonus", "enableAttrMultiBonus",
                    "progression.bridge.AttrMultiBonus", "CNPC attr multi")),
            Map.entry("prestigeskill", info("Prestige Skill Sync", "enablePrestigeSkillSync",
                    "progression.bridge.PrestigeSkillSync", "CNPC prestige skill")),
            Map.entry("faction", info("Prestige Faction Sync", "enablePrestigeFactionSync",
                    "progression.bridge.PrestigeFactionSync", "CNPC faction sync")),
            Map.entry("cleaner", info("Value Cleaner", "enableValueCleaner",
                    "progression.bridge.ValueCleaner", "CNPC value cleaner")),
            Map.entry("raceclass", info("Race → Class Sync", "enableRaceClassSync",
                    "progression.bridge.RaceClassSync", "CNPC race class")),
            Map.entry("classperm", info("Class Permission Sync", "enableClassPermissionSync",
                    "progression.bridge.ClassPermissionSync", "CNPC class perm")),
            Map.entry("shadow", info("Shadow Dummy", "enableShadowDummyLimiter",
                    "progression.dummy.ShadowDummyLimiter", "CNPC shadow dummy")),
            Map.entry("statchecker", info("Stat Checker", "enablePlayerStatChecker",
                    "progression.PlayerStatChecker", "kubejs stat checker"))
    );

    private static ModuleInfo info(String title, String configKey, String javaClass, String legacyScript) {
        return new ModuleInfo(title, configKey, javaClass, legacyScript);
    }

    /** Flag keys shown as toggles on a progression GUI section (lowercase). */
    public static boolean isSectionPage(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "skills", "tp", "race", "combat", "end", "shop", "fabled", "utility", "flags" -> true;
            default -> false;
        };
    }

    public static String[] flagKeysForPage(String page) {
        if (page == null || page.isBlank()) {
            return new String[0];
        }
        return switch (page.toLowerCase(Locale.ROOT)) {
            case "skills" -> new String[]{"flight", "sprint", "meditation", "potential"};
            case "tp" -> new String[]{"farming", "building", "boost", "bio"};
            case "race" -> new String[]{"racelock", "yardrat", "spiritualist", "android"};
            case "combat" -> new String[]{"kiweapons", "piercing", "dot", "apothic"};
            case "end" -> new String[]{"end", "endportal"};
            case "shop" -> new String[]{"prestige", "skills"};
            case "fabled" -> new String[]{
                    "fabled", "energy", "statscreen", "tpsp", "attr",
                    "prestigeskill", "faction", "cleaner", "raceclass", "classperm"
            };
            case "utility" -> new String[]{"shadow", "statchecker"};
            default -> new String[0];
        };
    }

    public static String[] allFlagKeys() {
        return BY_FLAG.keySet().stream().sorted().toArray(String[]::new);
    }

    public static ModuleInfo module(String flagKey) {
        if (flagKey == null) {
            return null;
        }
        return BY_FLAG.get(flagKey.toLowerCase(Locale.ROOT));
    }

    public static String displayTitle(String flagKey) {
        ModuleInfo m = module(flagKey);
        if (m != null) {
            return m.title();
        }
        return flagKey == null ? "" : flagKey;
    }

    /** Short label for CNPC toggle buttons (full title stays in chat/docs). */
    public static String buttonTitle(String flagKey) {
        if (flagKey == null || flagKey.isBlank()) {
            return "";
        }
        return switch (flagKey.toLowerCase(Locale.ROOT)) {
            case "sprint" -> "Sprint jump";
            case "boost" -> "TP boost";
            case "bio" -> "Bio android";
            case "spiritualist" -> "Spiritualist ki";
            case "android" -> "Android";
            case "kiweapons" -> "Ki weapons";
            case "dot" -> "DoT bonus";
            case "apothic" -> "Apothic elem";
            case "endportal" -> "Portal guard";
            case "prestige" -> "Prestige shop";
            case "skills" -> "Skill unlocks";
            case "fabled" -> "Fabled bridge";
            case "energy" -> "Energy/mana";
            case "statscreen" -> "Stat screen";
            case "tpsp" -> "TP/SP mirror";
            case "attr" -> "Attr bonus";
            case "prestigeskill" -> "Prestige skills";
            case "faction" -> "Faction sync";
            case "raceclass" -> "Race/class";
            case "classperm" -> "Class perms";
            default -> {
                String full = displayTitle(flagKey);
                if (full.length() <= 14) {
                    yield full;
                }
                yield full.substring(0, 12).trim() + "…";
            }
        };
    }

    /** Chat lines for one module (staff). */
    public static List<String> formatModuleDoc(String flagKey) {
        List<String> out = new ArrayList<>();
        ModuleInfo m = module(flagKey);
        if (m == null) {
            out.add("§cUnknown module flag: §f" + flagKey);
            return out;
        }
        out.add("§6§l" + m.title() + " §8(§7" + flagKey + "§8)");
        out.add("§7Config §fconfig/adaptivedifficulty.json §7→ §f" + m.configKey());
        out.add("§7Code §fcom.dbzlegacy.adaptivedifficulty." + m.javaClass());
        if (m.legacyScript() != null && !m.legacyScript().isBlank()) {
            out.add("§7Legacy §f" + m.legacyScript());
        }
        out.add("§8Edit the Java module or JSON, then §f/lm admin reload§8.");
        out.add("§8Toggle in-game: progression section GUIs or §f/prog do flag " + flagKey);
        return out;
    }

    /** Chat lines summarizing every module on a section page. */
    public static List<String> formatSectionDoc(String page) {
        List<String> out = new ArrayList<>();
        out.add("§6§lProgression sources · §f" + page);
        out.add("§8Behavior is implemented in LegacyMechanics Java (not CNPC scripts).");
        out.add("");
        for (String key : flagKeysForPage(page)) {
            ModuleInfo m = module(key);
            if (m == null) {
                continue;
            }
            out.add("§e" + m.title() + " §8— §7" + m.configKey() + " §8· §f" + m.javaClass());
        }
        out.add("");
        out.add("§8Reload after file edits: §f/lm admin reload");
        return out;
    }
}
