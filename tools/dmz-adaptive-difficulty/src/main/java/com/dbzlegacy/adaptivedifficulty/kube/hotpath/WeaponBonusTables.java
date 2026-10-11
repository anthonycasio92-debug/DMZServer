package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Weapon family tables transcribed from kubejs/server_scripts/dmz_weapon_bonus_scale.js.
 * Absent stats are {@link Double#NaN}.
 */
public final class WeaponBonusTables {
    public static final String[] STATS = {"STR", "SKP", "RES", "VIT", "PWR", "ENE"};
    public static final String[] THIRD_ORDER = {"VIT", "ENE", "RES", "PWR", "SKP", "STR"};
    public static final String[][] SIMPLY_SUFFIXES = {
        {"_backhand_blade", "backhand_blade"},
        {"_quarterstaff", "quarterstaff"},
        {"_great_katana", "great_katana"},
        {"_great_spear", "great_spear"},
        {"_deer_horns", "deer_horns"},
        {"_greathammer", "greathammer"},
        {"_grandsword", "grandsword"},
        {"_twinblade", "twinblade"},
        {"_warglaive", "warglaive"},
        {"_longsword", "longsword"},
        {"_greataxe", "greataxe"},
        {"_chakram", "chakram"},
        {"_claymore", "claymore"},
        {"_cutlass", "cutlass"},
        {"_halberd", "halberd"},
        {"_khopesh", "khopesh"},
        {"_pernach", "pernach"},
        {"_rapier", "rapier"},
        {"_scythe", "scythe"},
        {"_glaive", "glaive"},
        {"_katana", "katana"},
        {"_dagger", "dagger"},
        {"_lance", "lance"},
        {"_spear", "spear"},
        {"_sai", "sai"},
    };

    public static final class WeaponDef {
        public final String family;
        public final double[] profile;

        WeaponDef(String family, double[] profile) {
            this.family = family;
            this.profile = profile;
        }
    }

    private static final Map<String, double[]> FAMILY;
    private static final Map<String, String> SIMPLY_EXACT;
    private static final Map<String, String> UNIQUE_THIRD;
    private static final Map<String, WeaponDef> IRONS;
    private static final Map<String, WeaponDef> DMZ;
    private static final Map<String, double[]> TINKERS;

    static {
        Map<String, double[]> family = new LinkedHashMap<>();
        family.put("backhand_blade", p(Double.NaN, 0.12, Double.NaN, Double.NaN, Double.NaN, 0.05));
        family.put("chakram", p(Double.NaN, 0.08, Double.NaN, Double.NaN, Double.NaN, 0.12));
        family.put("claymore", p(0.16, Double.NaN, 0.07, Double.NaN, Double.NaN, Double.NaN));
        family.put("cutlass", p(0.09, 0.09, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        family.put("dagger", p(Double.NaN, 0.15, Double.NaN, Double.NaN, Double.NaN, 0.04));
        family.put("deer_horns", p(Double.NaN, 0.12, 0.05, Double.NaN, Double.NaN, Double.NaN));
        family.put("glaive", p(0.08, 0.11, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        family.put("grandsword", p(0.17, Double.NaN, 0.08, Double.NaN, Double.NaN, Double.NaN));
        family.put("great_katana", p(0.07, 0.15, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        family.put("great_spear", p(0.10, 0.11, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        family.put("greataxe", p(0.18, Double.NaN, 0.06, Double.NaN, Double.NaN, Double.NaN));
        family.put("greathammer", p(0.17, Double.NaN, 0.09, Double.NaN, Double.NaN, Double.NaN));
        family.put("halberd", p(0.11, 0.09, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        family.put("katana", p(Double.NaN, 0.15, Double.NaN, Double.NaN, Double.NaN, 0.04));
        family.put("khopesh", p(0.09, 0.10, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        family.put("lance", p(0.12, 0.08, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        family.put("longsword", p(0.10, 0.10, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        family.put("pernach", p(0.15, Double.NaN, 0.07, Double.NaN, Double.NaN, Double.NaN));
        family.put("quarterstaff", p(Double.NaN, 0.10, 0.09, Double.NaN, Double.NaN, Double.NaN));
        family.put("rapier", p(Double.NaN, 0.17, Double.NaN, Double.NaN, Double.NaN, 0.03));
        family.put("sai", p(Double.NaN, 0.15, Double.NaN, Double.NaN, Double.NaN, 0.04));
        family.put("scythe", p(Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.10, 0.18));
        family.put("sickle", p(Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.08, 0.15));
        family.put("spear", p(0.08, 0.12, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        family.put("twinblade", p(0.07, 0.12, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        family.put("warglaive", p(0.10, 0.10, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        family.put("mace", p(0.15, Double.NaN, 0.08, Double.NaN, Double.NaN, Double.NaN));
        FAMILY = Collections.unmodifiableMap(family);

        Map<String, String> simply = new LinkedHashMap<>();
        simply.put("simplyswords:emberlash", "backhand_blade");
        simply.put("simplymore:boas_fang", "backhand_blade");
        simply.put("simplymore:smouldering_ruin", "backhand_blade");
        simply.put("simplyswords:mjolnir", "chakram");
        simply.put("simplyswords:tempest", "chakram");
        simply.put("simplymore:vipers_call", "chakram");
        simply.put("simplymore:blade_of_the_grotesque", "claymore");
        simply.put("simplyswords:caelestis", "claymore");
        simply.put("simplymore:deaths_eyrie", "claymore");
        simply.put("simplyswords:twisted_blade", "claymore");
        simply.put("simplyswords:slumbering_lichblade", "claymore");
        simply.put("simplyswords:waking_lichblade", "claymore");
        simply.put("simplyswords:awakened_lichblade", "claymore");
        simply.put("simplyswords:waxweaver", "claymore");
        simply.put("simplymore:black_pearl", "cutlass");
        simply.put("simplymore:the_vessel_breach", "dagger");
        simply.put("simplymore:culterex", "dagger");
        simply.put("simplymore:tidebreaker", "deer_horns");
        simply.put("simplymore:brassturn", "deer_horns");
        simply.put("simplymore:soul_foreseer", "glaive");
        simply.put("simplyswords:flamewind", "glaive");
        simply.put("simplymore:grandfrost", "grandsword");
        simply.put("simplyswords:enigma", "grandsword");
        simply.put("simplyswords:ribboncleaver", "grandsword");
        simply.put("simplymore:molten_flare", "grandsword");
        simply.put("simplymore:great_slither", "great_katana");
        simply.put("simplymore:lustrous_moxie", "great_katana");
        simply.put("simplymore:perforiscus", "great_spear");
        simply.put("simplymore:serpentine_valour", "great_spear");
        simply.put("simplymore:earthshatter", "greataxe");
        simply.put("simplyswords:soulpyre", "greataxe");
        simply.put("simplyswords:hearthflame", "greathammer");
        simply.put("simplyswords:soulkeeper", "greathammer");
        simply.put("simplyswords:hiveheart", "greathammer");
        simply.put("simplyswords:icewhisper", "halberd");
        simply.put("simplyswords:arcanethyst", "halberd");
        simply.put("simplyswords:thunderbrand", "halberd");
        simply.put("simplymore:revvengine", "katana");
        simply.put("simplyswords:whisperwind", "katana");
        simply.put("simplymore:timekeeper", "khopesh");
        simply.put("simplymore:myrmedge", "khopesh");
        simply.put("simplymore:glimmerstep", "lance");
        simply.put("simplymore:jester_penetrate", "lance");
        simply.put("simplymore:exedrill", "lance");
        simply.put("simplymore:matterbane", "longsword");
        simply.put("simplyswords:decaying_relic", "longsword");
        simply.put("simplyswords:stormbringer", "longsword");
        simply.put("simplyswords:emberblade", "longsword");
        simply.put("simplyswords:dormant_relic", "longsword");
        simply.put("simplyswords:righteous_relic", "longsword");
        simply.put("simplyswords:tainted_relic", "longsword");
        simply.put("simplyswords:sunfire", "longsword");
        simply.put("simplyswords:harbinger", "longsword");
        simply.put("simplymore:the_pan", "pernach");
        simply.put("simplymore:cindergorge", "pernach");
        simply.put("simplymore:ruptured_idol", "pernach");
        simply.put("simplymore:ascended_idol", "pernach");
        simply.put("simplymore:tarnished_idol", "pernach");
        simply.put("simplymore:holylight", "pernach");
        simply.put("simplymore:darksent", "pernach");
        simply.put("simplymore:stasis", "quarterstaff");
        simply.put("simplymore:ruyi_jingu_bang", "quarterstaff");
        simply.put("simplyswords:bramblethorn", "rapier");
        simply.put("simplyswords:shadowsting", "rapier");
        simply.put("simplyswords:soulstealer", "sai");
        simply.put("simplyswords:magiscythe", "scythe");
        simply.put("simplyswords:soulrender", "scythe");
        simply.put("simplymore:the_blood_harvester", "scythe");
        simply.put("simplyswords:wickpiercer", "spear");
        simply.put("simplyswords:magispear", "spear");
        simply.put("simplyswords:sword_on_a_stick", "spear");
        simply.put("simplyswords:storms_edge", "twinblade");
        simply.put("simplyswords:magiblade", "twinblade");
        simply.put("simplyswords:stars_edge", "twinblade");
        simply.put("simplyswords:dreadtide", "twinblade");
        simply.put("simplyswords:frostfall", "mace");
        simply.put("simplyswords:livyatan", "sickle");
        simply.put("simplyswords:molten_edge", "sickle");
        SIMPLY_EXACT = Collections.unmodifiableMap(simply);

        Map<String, String> unique = new LinkedHashMap<>();
        unique.put("simplyswords:watcher_claymore", "VIT");
        unique.put("simplyswords:brimstone_claymore", "PWR");
        unique.put("simplyswords:storms_edge", "ENE");
        unique.put("simplyswords:stormbringer", "VIT");
        unique.put("simplyswords:bramblethorn", "PWR");
        unique.put("simplyswords:watching_warglaive", "ENE");
        unique.put("simplyswords:toxic_longsword", "PWR");
        unique.put("simplyswords:emberblade", "PWR");
        unique.put("simplyswords:frostfall", "ENE");
        unique.put("simplyswords:soulpyre", "PWR");
        unique.put("simplyswords:molten_edge", "VIT");
        unique.put("simplyswords:livyatan", "RES");
        unique.put("simplyswords:icewhisper", "ENE");
        unique.put("simplyswords:arcanethyst", "PWR");
        unique.put("simplyswords:thunderbrand", "ENE");
        unique.put("simplyswords:hearthflame", "VIT");
        unique.put("simplyswords:twisted_blade", "VIT");
        unique.put("simplyswords:soulrender", "PWR");
        unique.put("simplyswords:soulkeeper", "VIT");
        unique.put("simplyswords:soulstealer", "ENE");
        unique.put("simplyswords:mjolnir", "PWR");
        unique.put("simplyswords:slumbering_lichblade", "PWR");
        unique.put("simplyswords:waking_lichblade", "PWR");
        unique.put("simplyswords:awakened_lichblade", "PWR");
        unique.put("simplyswords:shadowsting", "PWR");
        unique.put("simplyswords:dormant_relic", "RES");
        unique.put("simplyswords:decaying_relic", "RES");
        unique.put("simplyswords:tainted_relic", "PWR");
        unique.put("simplyswords:righteous_relic", "VIT");
        unique.put("simplyswords:sunfire", "VIT");
        unique.put("simplyswords:harbinger", "PWR");
        unique.put("simplyswords:whisperwind", "ENE");
        unique.put("simplyswords:emberlash", "ENE");
        unique.put("simplyswords:waxweaver", "VIT");
        unique.put("simplyswords:hiveheart", "VIT");
        unique.put("simplyswords:stars_edge", "ENE");
        unique.put("simplyswords:wickpiercer", "ENE");
        unique.put("simplyswords:tempest", "PWR");
        unique.put("simplyswords:flamewind", "PWR");
        unique.put("simplyswords:ribboncleaver", "VIT");
        unique.put("simplyswords:magiscythe", "RES");
        unique.put("simplyswords:magiblade", "PWR");
        unique.put("simplyswords:magispear", "PWR");
        unique.put("simplyswords:enigma", "ENE");
        unique.put("simplyswords:caelestis", "ENE");
        unique.put("simplyswords:dreadtide", "PWR");
        unique.put("simplymore:great_slither", "PWR");
        unique.put("simplymore:molten_flare", "PWR");
        unique.put("simplymore:grandfrost", "RES");
        unique.put("simplymore:glimmerstep", "PWR");
        unique.put("simplymore:the_blood_harvester", "VIT");
        unique.put("simplymore:myrmedge", "VIT");
        unique.put("simplymore:black_pearl", "VIT");
        unique.put("simplymore:the_vessel_breach", "VIT");
        unique.put("simplymore:blade_of_the_grotesque", "VIT");
        unique.put("simplymore:vipers_call", "PWR");
        unique.put("simplymore:timekeeper", "PWR");
        unique.put("simplymore:matterbane", "PWR");
        unique.put("simplymore:smouldering_ruin", "PWR");
        unique.put("simplymore:stasis", "ENE");
        unique.put("simplymore:tidebreaker", "RES");
        unique.put("simplymore:ruyi_jingu_bang", "VIT");
        unique.put("simplymore:ruptured_idol", "RES");
        unique.put("simplymore:ascended_idol", "VIT");
        unique.put("simplymore:tarnished_idol", "PWR");
        unique.put("simplymore:holylight", "VIT");
        unique.put("simplymore:darksent", "PWR");
        unique.put("simplymore:boas_fang", "PWR");
        unique.put("simplymore:earthshatter", "VIT");
        unique.put("simplymore:soul_foreseer", "ENE");
        unique.put("simplymore:serpentine_valour", "PWR");
        unique.put("simplymore:lustrous_moxie", "ENE");
        unique.put("simplymore:brassturn", "RES");
        unique.put("simplymore:cindergorge", "VIT");
        unique.put("simplymore:deaths_eyrie", "VIT");
        unique.put("simplymore:perforiscus", "VIT");
        unique.put("simplymore:revvengine", "VIT");
        unique.put("simplymore:exedrill", "VIT");
        unique.put("simplymore:culterex", "PWR");
        UNIQUE_THIRD = Collections.unmodifiableMap(unique);

        Map<String, WeaponDef> irons = new LinkedHashMap<>();
        irons.put("irons_spellbooks:graybeard_staff", def("staff", Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.12, 0.16));
        irons.put("irons_spellbooks:pyrium_staff", def("staff", Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.14, 0.14));
        irons.put("irons_spellbooks:artificer_cane", def("staff", Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.10, 0.15));
        irons.put("irons_spellbooks:ice_staff", def("staff", Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.13, 0.15));
        irons.put("irons_spellbooks:lightning_rod", def("staff", Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.14, 0.13));
        irons.put("irons_spellbooks:blood_staff", def("staff", Double.NaN, Double.NaN, Double.NaN, 0.08, 0.13, Double.NaN));
        irons.put("irons_spellbooks:improved_blood_staff", def("staff", Double.NaN, Double.NaN, Double.NaN, 0.10, 0.16, Double.NaN));
        irons.put("irons_spellbooks:staff_of_the_nines", def("staff", Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.16, 0.16));
        irons.put("irons_spellbooks:hither_thither_wand", def("wand", Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.08, 0.14));
        irons.put("irons_spellbooks:dev_staff", def("staff", Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.12, 0.12));
        irons.put("irons_spellbooks:amethyst_rapier", def("rapier", Double.NaN, 0.17, Double.NaN, Double.NaN, Double.NaN, 0.04));
        irons.put("irons_spellbooks:claymore", def("claymore", 0.16, Double.NaN, 0.07, Double.NaN, Double.NaN, Double.NaN));
        irons.put("irons_spellbooks:keeper_flamberge", def("flamberge", 0.13, 0.09, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        irons.put("irons_spellbooks:legionnaire_flamberge", def("flamberge", 0.14, 0.09, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        irons.put("irons_spellbooks:magehunter", def("sword", 0.10, 0.10, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        irons.put("irons_spellbooks:spellbreaker", def("sword", 0.09, Double.NaN, 0.10, Double.NaN, Double.NaN, Double.NaN));
        irons.put("irons_spellbooks:misery", def("energy_sword", 0.11, Double.NaN, Double.NaN, Double.NaN, 0.10, Double.NaN));
        irons.put("irons_spellbooks:hellrazor", def("energy_sword", 0.13, Double.NaN, Double.NaN, Double.NaN, 0.10, Double.NaN));
        irons.put("irons_spellbooks:decrepit_scythe", def("scythe", Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.09, 0.16));
        irons.put("irons_spellbooks:boreal_blade", def("energy_sword", Double.NaN, 0.10, Double.NaN, Double.NaN, 0.11, Double.NaN));
        irons.put("irons_spellbooks:twilight_gale", def("energy_sword", Double.NaN, 0.08, Double.NaN, Double.NaN, 0.13, Double.NaN));
        irons.put("irons_spellbooks:truthseeker", def("greatsword", 0.10, 0.10, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        irons.put("irons_spellbooks:autoloader_crossbow", def("crossbow", Double.NaN, 0.08, Double.NaN, Double.NaN, 0.10, Double.NaN));
        IRONS = Collections.unmodifiableMap(irons);

        Map<String, WeaponDef> dmz = new LinkedHashMap<>();
        dmz.put("dragonminez:yajirobe_katana", def("katana", 0.06, 0.16, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        dmz.put("dragonminez:z_sword", def("heavy_sword", 0.18, Double.NaN, 0.08, Double.NaN, Double.NaN, Double.NaN));
        dmz.put("dragonminez:brave_sword", def("sword", 0.12, 0.12, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        dmz.put("dragonminez:dimensional_sword", def("energy_sword", Double.NaN, 0.10, Double.NaN, Double.NaN, 0.12, Double.NaN));
        dmz.put("dragonminez:power_pole", def("staff", 0.10, 0.12, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        dmz.put("dragonminez:laser_merus", def("energy_weapon", Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.14, 0.12));
        dmz.put("dragonminez:blaster_cannon", def("energy_weapon", Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.12, 0.10));
        DMZ = Collections.unmodifiableMap(dmz);

        Map<String, double[]> tinkers = new LinkedHashMap<>();
        tinkers.put("scythe", p(Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.10, 0.18));
        tinkers.put("cleaver", p(0.18, Double.NaN, 0.06, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("sword", p(0.10, 0.10, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("dagger", p(Double.NaN, 0.15, Double.NaN, Double.NaN, Double.NaN, 0.04));
        tinkers.put("kama", p(Double.NaN, 0.12, Double.NaN, Double.NaN, Double.NaN, 0.05));
        tinkers.put("hand_axe", p(0.14, 0.05, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("broad_axe", p(0.18, Double.NaN, 0.07, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("sledge_hammer", p(0.17, Double.NaN, 0.09, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("vein_hammer", p(0.16, Double.NaN, 0.08, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("war_pick", p(0.12, 0.08, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("mattock", p(0.10, 0.07, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("pickadze", p(0.10, 0.07, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("excavator", p(0.15, Double.NaN, 0.06, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("battlesign", p(Double.NaN, Double.NaN, 0.14, 0.04, Double.NaN, Double.NaN));
        tinkers.put("swasher", p(0.09, 0.10, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("minotaur_axe", p(0.18, Double.NaN, 0.06, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("melting_pan", p(0.12, Double.NaN, 0.07, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("javelin", p(0.07, 0.12, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("shuriken", p(Double.NaN, 0.12, Double.NaN, Double.NaN, Double.NaN, 0.05));
        tinkers.put("throwing_axe", p(0.08, 0.10, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        tinkers.put("crossbow", p(Double.NaN, 0.08, Double.NaN, Double.NaN, 0.10, Double.NaN));
        tinkers.put("longbow", p(Double.NaN, 0.08, Double.NaN, Double.NaN, 0.10, Double.NaN));
        tinkers.put("sky_staff", p(Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.12, 0.15));
        tinkers.put("earth_staff", p(Double.NaN, Double.NaN, 0.10, Double.NaN, 0.10, Double.NaN));
        tinkers.put("ichor_staff", p(Double.NaN, Double.NaN, Double.NaN, 0.08, 0.14, Double.NaN));
        tinkers.put("ender_staff", p(Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0.13, 0.14));
        TINKERS = Collections.unmodifiableMap(tinkers);
    }

    private WeaponBonusTables() {}

    public static double[] family(String name) {
        return name == null ? null : FAMILY.get(name);
    }

    public static String simplyExact(String id) {
        return id == null ? null : SIMPLY_EXACT.get(id);
    }

    public static String uniqueThird(String id) {
        return id == null ? null : UNIQUE_THIRD.get(id);
    }

    public static WeaponDef irons(String id) {
        return id == null ? null : IRONS.get(id);
    }

    public static WeaponDef dmz(String id) {
        return id == null ? null : DMZ.get(id);
    }

    public static double[] tinkers(String family) {
        return family == null ? null : TINKERS.get(family);
    }

    private static double[] p(double str, double skp, double res, double vit, double pwr, double ene) {
        return new double[] {str, skp, res, vit, pwr, ene};
    }

    private static WeaponDef def(String family, double str, double skp, double res, double vit, double pwr, double ene) {
        return new WeaponDef(family, p(str, skp, res, vit, pwr, ene));
    }
}

