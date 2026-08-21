// ============================================================================
// DMZ LEGACY - WEAPON MULTIPLIER BRIDGE V9
// CustomNPCs Global Player Script
// Minecraft 1.20.1
// DragonMineZ 2.1.3
//
// IMPORTANT:
// THIS IS A CUSTOMNPCS NASHORN SCRIPT.
// NOT KUBEJS.
//
// PURPOSE:
// Weapons grant ONLY multiplicative DMZ stat bonuses.
//
// Example:
//
// +40% STR
// = x1.40
//
// Every modifier is added as:
//
// bonus.addBonus(stat, name, "*", factor, true);
//
// Resistance uses:
//
// bonus.addBonusSplit("RES", name, "*", factor, true);
//
// THERE ARE NO "+" WEAPON BONUSES.
// THERE IS NO UPPER MULTIPLIER CAP.
//
// ============================================================================


// ============================================================================
// JAVA CLASSES
// ============================================================================

var StatsProvider =
    Java.type("com.dragonminez.common.stats.StatsProvider");

var StatsCapability =
    Java.type("com.dragonminez.common.stats.StatsCapability");

var StatsSyncS2C =
    Java.type("com.dragonminez.common.network.S2C.StatsSyncS2C");

var NetworkHandler =
    Java.type("com.dragonminez.common.network.NetworkHandler");

var EquipmentSlot =
    Java.type("net.minecraft.world.entity.EquipmentSlot");

var Attributes =
    Java.type("net.minecraft.world.entity.ai.attributes.Attributes");

var ShieldItem =
    Java.type("net.minecraft.world.item.ShieldItem");


// ============================================================================
// CONFIG
// ============================================================================

var DEBUG_WEAPON_STATS = true;

// Recalculate equipment twice per second.
var UPDATE_INTERVAL = 10;

// Hotbar position 9 = inventory index 8.
var SLOT_9_INDEX = 8;

// Simply Swords / Simply More uniques:
//
// 1.80 means 80% stronger base stat scaling.
var UNIQUE_BASE_MULTIPLIER = 1.80;

// Third-stat multiplier.
var UNIQUE_THIRD_MULTIPLIER = 1.35;

// Removes ApothicDMZ's aggregated:
// "Attribute Bonus STR flat"
// "Attribute Bonus SKP flat"
// etc.
//
// This is enabled because you specifically want multiplier-only stats.
var REMOVE_APOTHIC_FLAT_BONUSES = false;

var SCRIPT_VERSION =
    "DMZ_WEAPON_MULTIPLIERS_V9";

var TEMP_SIGNATURE =
    "DMZWeaponMultiplierSignatureV9";


// ============================================================================
// SLOT IDs
// ============================================================================

var SLOT_MAIN = "MAIN";
var SLOT_OFF = "OFF";
var SLOT_9 = "SLOT9";


// ============================================================================
// BONUS NAMES
//
// These are intentionally different for main/off/slot9.
//
// If main gives STR x1.20
// and offhand gives STR x1.30:
//
// DMZ calculates:
//
// 1.20 * 1.30 = x1.56
//
// ============================================================================

var BONUS_MAIN =
    "DMZ Weapon Mainhand";

var BONUS_OFF =
    "DMZ Weapon Offhand";

var BONUS_SLOT9 =
    "DMZ Weapon Slot 9";


// ============================================================================
// STAT KEYS
// ============================================================================

var STAT_KEYS = [
    "STR",
    "SKP",
    "RES",
    "VIT",
    "PWR",
    "ENE"
];


// ============================================================================
// BASE WEAPON FAMILY PROFILES
//
// Values are percentages.
//
// 0.10 = +10%
// 0.18 = +18%
//
// These percentages are then scaled by actual weapon attack damage.
//
// ============================================================================

var FAMILY_PROFILES = {

    "backhand_blade": {
        "SKP": 0.12,
        "ENE": 0.05
    },

    "chakram": {
        "SKP": 0.08,
        "ENE": 0.12
    },

    "claymore": {
        "STR": 0.16,
        "RES": 0.07
    },

    "cutlass": {
        "STR": 0.09,
        "SKP": 0.09
    },

    "dagger": {
        "SKP": 0.15,
        "ENE": 0.04
    },

    "deer_horns": {
        "SKP": 0.12,
        "RES": 0.05
    },

    "glaive": {
        "STR": 0.08,
        "SKP": 0.11
    },

    "grandsword": {
        "STR": 0.17,
        "RES": 0.08
    },

    "great_katana": {
        "STR": 0.07,
        "SKP": 0.15
    },

    "great_spear": {
        "STR": 0.10,
        "SKP": 0.11
    },

    "greataxe": {
        "STR": 0.18,
        "RES": 0.06
    },

    "greathammer": {
        "STR": 0.17,
        "RES": 0.09
    },

    "halberd": {
        "STR": 0.11,
        "SKP": 0.09
    },

    "katana": {
        "SKP": 0.15,
        "ENE": 0.04
    },

    "khopesh": {
        "STR": 0.09,
        "SKP": 0.10
    },

    "lance": {
        "STR": 0.12,
        "SKP": 0.08
    },

    "longsword": {
        "STR": 0.10,
        "SKP": 0.10
    },

    "pernach": {
        "STR": 0.15,
        "RES": 0.07
    },

    "quarterstaff": {
        "SKP": 0.10,
        "RES": 0.09
    },

    "rapier": {
        "SKP": 0.17,
        "ENE": 0.03
    },

    "sai": {
        "SKP": 0.15,
        "ENE": 0.04
    },

    // Scythes are intentionally energy-focused.
    "scythe": {
        "PWR": 0.10,
        "ENE": 0.18
    },

    "sickle": {
        "PWR": 0.08,
        "ENE": 0.15
    },

    "spear": {
        "STR": 0.08,
        "SKP": 0.12
    },

    "twinblade": {
        "STR": 0.07,
        "SKP": 0.12
    },

    "warglaive": {
        "STR": 0.10,
        "SKP": 0.10
    },

    "mace": {
        "STR": 0.15,
        "RES": 0.08
    }
};


// ============================================================================
// SIMPLY SWORDS / SIMPLY MORE
//
// CustomNPCs Nashorn cannot reliably construct Minecraft TagKey instances.
//
// Instead, I extracted the installed Simply More weapon-family tags.
//
// There are 904 direct weapon-family entries.
//
// 829 of those identify cleanly from the final registry-name suffix.
//
// The remaining 75 entries are listed below explicitly.
//
// Frostfall, Livyatan, and Molten Edge are additional special cases from
// Simply Swords' own weapon metadata.
//
// ============================================================================

var SIMPLY_EXACT_FAMILY = {

    // ------------------------------------------------------------------------
    // BACKHAND BLADE
    // ------------------------------------------------------------------------

    "simplyswords:emberlash": "backhand_blade",
    "simplymore:boas_fang": "backhand_blade",
    "simplymore:smouldering_ruin": "backhand_blade",


    // ------------------------------------------------------------------------
    // CHAKRAM
    // ------------------------------------------------------------------------

    "simplyswords:mjolnir": "chakram",
    "simplyswords:tempest": "chakram",
    "simplymore:vipers_call": "chakram",


    // ------------------------------------------------------------------------
    // CLAYMORE
    // ------------------------------------------------------------------------

    "simplymore:blade_of_the_grotesque": "claymore",
    "simplyswords:caelestis": "claymore",
    "simplymore:deaths_eyrie": "claymore",
    "simplyswords:twisted_blade": "claymore",
    "simplyswords:slumbering_lichblade": "claymore",
    "simplyswords:waking_lichblade": "claymore",
    "simplyswords:awakened_lichblade": "claymore",
    "simplyswords:waxweaver": "claymore",


    // ------------------------------------------------------------------------
    // CUTLASS
    // ------------------------------------------------------------------------

    "simplymore:black_pearl": "cutlass",


    // ------------------------------------------------------------------------
    // DAGGER
    // ------------------------------------------------------------------------

    "simplymore:the_vessel_breach": "dagger",
    "simplymore:culterex": "dagger",


    // ------------------------------------------------------------------------
    // DEER HORNS
    // ------------------------------------------------------------------------

    "simplymore:tidebreaker": "deer_horns",
    "simplymore:brassturn": "deer_horns",


    // ------------------------------------------------------------------------
    // GLAIVE
    // ------------------------------------------------------------------------

    "simplymore:soul_foreseer": "glaive",
    "simplyswords:flamewind": "glaive",


    // ------------------------------------------------------------------------
    // GRANDSWORD
    // ------------------------------------------------------------------------

    "simplymore:grandfrost": "grandsword",
    "simplyswords:enigma": "grandsword",
    "simplyswords:ribboncleaver": "grandsword",
    "simplymore:molten_flare": "grandsword",


    // ------------------------------------------------------------------------
    // GREAT KATANA
    // ------------------------------------------------------------------------

    "simplymore:great_slither": "great_katana",
    "simplymore:lustrous_moxie": "great_katana",


    // ------------------------------------------------------------------------
    // GREAT SPEAR
    // ------------------------------------------------------------------------

    "simplymore:perforiscus": "great_spear",
    "simplymore:serpentine_valour": "great_spear",


    // ------------------------------------------------------------------------
    // GREATAXE
    // ------------------------------------------------------------------------

    "simplymore:earthshatter": "greataxe",
    "simplyswords:soulpyre": "greataxe",


    // ------------------------------------------------------------------------
    // GREATHAMMER
    // ------------------------------------------------------------------------

    "simplyswords:hearthflame": "greathammer",
    "simplyswords:soulkeeper": "greathammer",
    "simplyswords:hiveheart": "greathammer",


    // ------------------------------------------------------------------------
    // HALBERD
    // ------------------------------------------------------------------------

    "simplyswords:icewhisper": "halberd",
    "simplyswords:arcanethyst": "halberd",
    "simplyswords:thunderbrand": "halberd",


    // ------------------------------------------------------------------------
    // KATANA
    // ------------------------------------------------------------------------

    "simplymore:revvengine": "katana",
    "simplyswords:whisperwind": "katana",


    // ------------------------------------------------------------------------
    // KHOPESH
    // ------------------------------------------------------------------------

    "simplymore:timekeeper": "khopesh",
    "simplymore:myrmedge": "khopesh",


    // ------------------------------------------------------------------------
    // LANCE
    // ------------------------------------------------------------------------

    "simplymore:glimmerstep": "lance",
    "simplymore:jester_penetrate": "lance",
    "simplymore:exedrill": "lance",


    // ------------------------------------------------------------------------
    // LONGSWORD
    // ------------------------------------------------------------------------

    "simplymore:matterbane": "longsword",
    "simplyswords:decaying_relic": "longsword",
    "simplyswords:stormbringer": "longsword",
    "simplyswords:emberblade": "longsword",
    "simplyswords:dormant_relic": "longsword",
    "simplyswords:righteous_relic": "longsword",
    "simplyswords:tainted_relic": "longsword",
    "simplyswords:sunfire": "longsword",
    "simplyswords:harbinger": "longsword",


    // ------------------------------------------------------------------------
    // PERNACH
    // ------------------------------------------------------------------------

    "simplymore:the_pan": "pernach",
    "simplymore:cindergorge": "pernach",
    "simplymore:ruptured_idol": "pernach",
    "simplymore:ascended_idol": "pernach",
    "simplymore:tarnished_idol": "pernach",
    "simplymore:holylight": "pernach",
    "simplymore:darksent": "pernach",


    // ------------------------------------------------------------------------
    // QUARTERSTAFF
    // ------------------------------------------------------------------------

    "simplymore:stasis": "quarterstaff",
    "simplymore:ruyi_jingu_bang": "quarterstaff",


    // ------------------------------------------------------------------------
    // RAPIER
    // ------------------------------------------------------------------------

    "simplyswords:bramblethorn": "rapier",
    "simplyswords:shadowsting": "rapier",


    // ------------------------------------------------------------------------
    // SAI
    // ------------------------------------------------------------------------

    "simplyswords:soulstealer": "sai",


    // ------------------------------------------------------------------------
    // SCYTHE
    // ------------------------------------------------------------------------

    "simplyswords:magiscythe": "scythe",
    "simplyswords:soulrender": "scythe",
    "simplymore:the_blood_harvester": "scythe",


    // ------------------------------------------------------------------------
    // SPEAR
    // ------------------------------------------------------------------------

    "simplyswords:wickpiercer": "spear",
    "simplyswords:magispear": "spear",
    "simplyswords:sword_on_a_stick": "spear",


    // ------------------------------------------------------------------------
    // TWINBLADE
    // ------------------------------------------------------------------------

    "simplyswords:storms_edge": "twinblade",
    "simplyswords:magiblade": "twinblade",
    "simplyswords:stars_edge": "twinblade",
    "simplyswords:dreadtide": "twinblade",


    // ------------------------------------------------------------------------
    // SPECIAL SIMPLY SWORDS METADATA
    // ------------------------------------------------------------------------

    "simplyswords:frostfall": "mace",

    "simplyswords:livyatan": "sickle",

    "simplyswords:molten_edge": "sickle"
};


// ============================================================================
// NORMAL SIMPLY WEAPON SUFFIXES
//
// ORDER MATTERS.
//
// More-specific suffixes come before simpler suffixes.
//
// ============================================================================

var SIMPLY_SUFFIXES = [

    ["_backhand_blade", "backhand_blade"],

    ["_quarterstaff", "quarterstaff"],

    ["_great_katana", "great_katana"],

    ["_great_spear", "great_spear"],

    ["_deer_horns", "deer_horns"],

    ["_greathammer", "greathammer"],

    ["_grandsword", "grandsword"],

    ["_twinblade", "twinblade"],

    ["_warglaive", "warglaive"],

    ["_longsword", "longsword"],

    ["_greataxe", "greataxe"],

    ["_chakram", "chakram"],

    ["_claymore", "claymore"],

    ["_cutlass", "cutlass"],

    ["_halberd", "halberd"],

    ["_khopesh", "khopesh"],

    ["_pernach", "pernach"],

    ["_rapier", "rapier"],

    ["_scythe", "scythe"],

    ["_glaive", "glaive"],

    ["_katana", "katana"],

    ["_dagger", "dagger"],

    ["_lance", "lance"],

    ["_spear", "spear"],

    ["_sai", "sai"]
];


// ============================================================================
// UNIQUE THIRD STATS
//
// Presence in this table marks the normal Simply unique weapons.
//
// Mimicry weapons are handled separately.
//
// ============================================================================

var UNIQUE_THIRD = {

    // ------------------------------------------------------------------------
    // SIMPLY SWORDS
    // ------------------------------------------------------------------------

    "simplyswords:watcher_claymore": "VIT",
    "simplyswords:brimstone_claymore": "PWR",

    "simplyswords:storms_edge": "ENE",
    "simplyswords:stormbringer": "VIT",

    "simplyswords:bramblethorn": "PWR",
    "simplyswords:watching_warglaive": "ENE",

    "simplyswords:toxic_longsword": "PWR",
    "simplyswords:emberblade": "PWR",

    "simplyswords:frostfall": "ENE",
    "simplyswords:soulpyre": "PWR",

    "simplyswords:molten_edge": "VIT",
    "simplyswords:livyatan": "RES",

    "simplyswords:icewhisper": "ENE",
    "simplyswords:arcanethyst": "PWR",

    "simplyswords:thunderbrand": "ENE",
    "simplyswords:hearthflame": "VIT",

    "simplyswords:twisted_blade": "VIT",

    "simplyswords:soulrender": "PWR",
    "simplyswords:soulkeeper": "VIT",
    "simplyswords:soulstealer": "ENE",

    "simplyswords:mjolnir": "PWR",

    "simplyswords:slumbering_lichblade": "PWR",
    "simplyswords:waking_lichblade": "PWR",
    "simplyswords:awakened_lichblade": "PWR",

    "simplyswords:shadowsting": "PWR",

    "simplyswords:dormant_relic": "RES",
    "simplyswords:decaying_relic": "RES",
    "simplyswords:tainted_relic": "PWR",
    "simplyswords:righteous_relic": "VIT",

    "simplyswords:sunfire": "VIT",
    "simplyswords:harbinger": "PWR",

    "simplyswords:whisperwind": "ENE",
    "simplyswords:emberlash": "ENE",

    "simplyswords:waxweaver": "VIT",
    "simplyswords:hiveheart": "VIT",

    "simplyswords:stars_edge": "ENE",
    "simplyswords:wickpiercer": "ENE",

    "simplyswords:tempest": "PWR",
    "simplyswords:flamewind": "PWR",

    "simplyswords:ribboncleaver": "VIT",

    "simplyswords:magiscythe": "RES",
    "simplyswords:magiblade": "PWR",
    "simplyswords:magispear": "PWR",

    "simplyswords:enigma": "ENE",
    "simplyswords:caelestis": "ENE",

    "simplyswords:dreadtide": "PWR",


    // ------------------------------------------------------------------------
    // SIMPLY MORE
    // ------------------------------------------------------------------------

    "simplymore:great_slither": "PWR",
    "simplymore:molten_flare": "PWR",

    "simplymore:grandfrost": "RES",
    "simplymore:glimmerstep": "PWR",

    "simplymore:the_blood_harvester": "VIT",
    "simplymore:myrmedge": "VIT",

    "simplymore:black_pearl": "VIT",
    "simplymore:the_vessel_breach": "VIT",

    "simplymore:blade_of_the_grotesque": "VIT",

    "simplymore:vipers_call": "PWR",

    "simplymore:timekeeper": "PWR",
    "simplymore:matterbane": "PWR",

    "simplymore:smouldering_ruin": "PWR",

    "simplymore:stasis": "ENE",

    "simplymore:tidebreaker": "RES",

    "simplymore:ruyi_jingu_bang": "VIT",

    "simplymore:ruptured_idol": "RES",
    "simplymore:ascended_idol": "VIT",
    "simplymore:tarnished_idol": "PWR",

    "simplymore:holylight": "VIT",
    "simplymore:darksent": "PWR",

    "simplymore:boas_fang": "PWR",

    "simplymore:earthshatter": "VIT",

    "simplymore:soul_foreseer": "ENE",

    "simplymore:serpentine_valour": "PWR",

    "simplymore:lustrous_moxie": "ENE",

    "simplymore:brassturn": "RES",

    "simplymore:cindergorge": "VIT",

    "simplymore:deaths_eyrie": "VIT",

    "simplymore:perforiscus": "VIT",

    "simplymore:revvengine": "VIT",

    "simplymore:exedrill": "VIT",

    "simplymore:culterex": "PWR"
};


// ============================================================================
// THIRD STAT FALLBACK ORDER
// ============================================================================

var THIRD_STAT_ORDER = [
    "VIT",
    "ENE",
    "RES",
    "PWR",
    "SKP",
    "STR"
];


// ============================================================================
// IRON'S SPELLBOOKS
// ============================================================================

var IRONS_WEAPONS = {

    // ------------------------------------------------------------------------
    // SPELLCASTING WEAPONS
    // ------------------------------------------------------------------------

    "irons_spellbooks:graybeard_staff": {
        "family": "staff",
        "profile": {
            "PWR": 0.12,
            "ENE": 0.16
        }
    },

    "irons_spellbooks:pyrium_staff": {
        "family": "staff",
        "profile": {
            "PWR": 0.14,
            "ENE": 0.14
        }
    },

    "irons_spellbooks:artificer_cane": {
        "family": "staff",
        "profile": {
            "PWR": 0.10,
            "ENE": 0.15
        }
    },

    "irons_spellbooks:ice_staff": {
        "family": "staff",
        "profile": {
            "PWR": 0.13,
            "ENE": 0.15
        }
    },

    "irons_spellbooks:lightning_rod": {
        "family": "staff",
        "profile": {
            "PWR": 0.14,
            "ENE": 0.13
        }
    },

    "irons_spellbooks:blood_staff": {
        "family": "staff",
        "profile": {
            "PWR": 0.13,
            "VIT": 0.08
        }
    },

    "irons_spellbooks:improved_blood_staff": {
        "family": "staff",
        "profile": {
            "PWR": 0.16,
            "VIT": 0.10
        }
    },

    "irons_spellbooks:staff_of_the_nines": {
        "family": "staff",
        "profile": {
            "PWR": 0.16,
            "ENE": 0.16
        }
    },

    "irons_spellbooks:hither_thither_wand": {
        "family": "wand",
        "profile": {
            "PWR": 0.08,
            "ENE": 0.14
        }
    },

    "irons_spellbooks:dev_staff": {
        "family": "staff",
        "profile": {
            "PWR": 0.12,
            "ENE": 0.12
        }
    },


    // ------------------------------------------------------------------------
    // PHYSICAL / HYBRID WEAPONS
    // ------------------------------------------------------------------------

    "irons_spellbooks:amethyst_rapier": {
        "family": "rapier",
        "profile": {
            "SKP": 0.17,
            "ENE": 0.04
        }
    },

    "irons_spellbooks:claymore": {
        "family": "claymore",
        "profile": {
            "STR": 0.16,
            "RES": 0.07
        }
    },

    "irons_spellbooks:keeper_flamberge": {
        "family": "flamberge",
        "profile": {
            "STR": 0.13,
            "SKP": 0.09
        }
    },

    "irons_spellbooks:legionnaire_flamberge": {
        "family": "flamberge",
        "profile": {
            "STR": 0.14,
            "SKP": 0.09
        }
    },

    "irons_spellbooks:magehunter": {
        "family": "sword",
        "profile": {
            "STR": 0.10,
            "SKP": 0.10
        }
    },

    "irons_spellbooks:spellbreaker": {
        "family": "sword",
        "profile": {
            "STR": 0.09,
            "RES": 0.10
        }
    },

    "irons_spellbooks:misery": {
        "family": "energy_sword",
        "profile": {
            "STR": 0.11,
            "PWR": 0.10
        }
    },

    "irons_spellbooks:hellrazor": {
        "family": "energy_sword",
        "profile": {
            "STR": 0.13,
            "PWR": 0.10
        }
    },

    "irons_spellbooks:decrepit_scythe": {
        "family": "scythe",
        "profile": {
            "PWR": 0.09,
            "ENE": 0.16
        }
    },

    "irons_spellbooks:boreal_blade": {
        "family": "energy_sword",
        "profile": {
            "SKP": 0.10,
            "PWR": 0.11
        }
    },

    "irons_spellbooks:twilight_gale": {
        "family": "energy_sword",
        "profile": {
            "SKP": 0.08,
            "PWR": 0.13
        }
    },

    "irons_spellbooks:truthseeker": {
        "family": "greatsword",
        "profile": {
            "STR": 0.10,
            "SKP": 0.10
        }
    },

    "irons_spellbooks:autoloader_crossbow": {
        "family": "crossbow",
        "profile": {
            "SKP": 0.08,
            "PWR": 0.10
        }
    }
};


// ============================================================================
// NATIVE DRAGONMINEZ WEAPONS
// ============================================================================

var DMZ_WEAPONS = {

    "dragonminez:yajirobe_katana": {
        "family": "katana",
        "profile": {
            "STR": 0.06,
            "SKP": 0.16
        }
    },

    "dragonminez:z_sword": {
        "family": "heavy_sword",
        "profile": {
            "STR": 0.18,
            "RES": 0.08
        }
    },

    "dragonminez:brave_sword": {
        "family": "sword",
        "profile": {
            "STR": 0.12,
            "SKP": 0.12
        }
    },

    "dragonminez:dimensional_sword": {
        "family": "energy_sword",
        "profile": {
            "SKP": 0.10,
            "PWR": 0.12
        }
    },

    "dragonminez:power_pole": {
        "family": "staff",
        "profile": {
            "STR": 0.10,
            "SKP": 0.12
        }
    },

    "dragonminez:laser_merus": {
        "family": "energy_weapon",
        "profile": {
            "PWR": 0.14,
            "ENE": 0.12
        }
    },

    "dragonminez:blaster_cannon": {
        "family": "energy_weapon",
        "profile": {
            "PWR": 0.12,
            "ENE": 0.10
        }
    }
};


// ============================================================================
// TINKERS TOOL PROFILES
// ============================================================================

var TINKERS_PROFILES = {

    "scythe": {
        "PWR": 0.10,
        "ENE": 0.18
    },

    "cleaver": {
        "STR": 0.18,
        "RES": 0.06
    },

    "sword": {
        "STR": 0.10,
        "SKP": 0.10
    },

    "dagger": {
        "SKP": 0.15,
        "ENE": 0.04
    },

    "kama": {
        "SKP": 0.12,
        "ENE": 0.05
    },

    "hand_axe": {
        "STR": 0.14,
        "SKP": 0.05
    },

    "broad_axe": {
        "STR": 0.18,
        "RES": 0.07
    },

    "sledge_hammer": {
        "STR": 0.17,
        "RES": 0.09
    },

    "vein_hammer": {
        "STR": 0.16,
        "RES": 0.08
    },

    "war_pick": {
        "STR": 0.12,
        "SKP": 0.08
    },

    "mattock": {
        "STR": 0.10,
        "SKP": 0.07
    },

    "pickadze": {
        "STR": 0.10,
        "SKP": 0.07
    },

    "excavator": {
        "STR": 0.15,
        "RES": 0.06
    },

    "battlesign": {
        "RES": 0.14,
        "VIT": 0.04
    },

    "swasher": {
        "STR": 0.09,
        "SKP": 0.10
    },

    "minotaur_axe": {
        "STR": 0.18,
        "RES": 0.06
    },

    "melting_pan": {
        "STR": 0.12,
        "RES": 0.07
    },

    "javelin": {
        "STR": 0.07,
        "SKP": 0.12
    },

    "shuriken": {
        "SKP": 0.12,
        "ENE": 0.05
    },

    "throwing_axe": {
        "STR": 0.08,
        "SKP": 0.10
    },

    "crossbow": {
        "SKP": 0.08,
        "PWR": 0.10
    },

    "longbow": {
        "SKP": 0.08,
        "PWR": 0.10
    },

    "sky_staff": {
        "PWR": 0.12,
        "ENE": 0.15
    },

    "earth_staff": {
        "PWR": 0.10,
        "RES": 0.10
    },

    "ichor_staff": {
        "PWR": 0.14,
        "VIT": 0.08
    },

    "ender_staff": {
        "PWR": 0.13,
        "ENE": 0.14
    }
};


// ============================================================================
// STRING HELPERS
// ============================================================================

function endsWithWeapon(text, ending) {

    text =
        String(text);

    ending =
        String(ending);


    if (
        ending.length >
        text.length
    ) {

        return false;
    }


    return (

        text.substring(
            text.length -
            ending.length
        )

        ==

        ending
    );
}


function getPath(id) {

    var value =
        String(id);


    var colon =
        value.indexOf(":");


    if (
        colon < 0
    ) {

        return value;
    }


    return value.substring(
        colon + 1
    );
}


// ============================================================================
// ITEM HELPERS
// ============================================================================

function getItemId(stack) {

    if (
        stack == null
    ) {

        return "";
    }


    try {

        if (
            stack.isEmpty()
        ) {

            return "";
        }


        return String(
            stack.getName()
        );

    } catch (err) {

        return "";
    }
}


function getMCStack(stack) {

    if (
        stack == null
    ) {

        return null;
    }


    try {

        if (
            stack.isEmpty()
        ) {

            return null;
        }


        return stack.getMCItemStack();

    } catch (err) {

        return null;
    }
}


// ============================================================================
// SIMPLY FAMILY DETECTION
// ============================================================================

function getSimplyFamily(id) {

    if (
        id == null ||
        id == ""
    ) {

        return null;
    }


    // ------------------------------------------------------------------------
    // EXACT EXCEPTION
    // ------------------------------------------------------------------------

    if (
        SIMPLY_EXACT_FAMILY[id]
        != null
    ) {

        return SIMPLY_EXACT_FAMILY[id];
    }


    var path =
        getPath(
            id
        );


    // ------------------------------------------------------------------------
    // NORMAL / COMPAT / MIMICRY SUFFIX
    //
    // Works even with paths such as:
    //
    // simplyswords:gobber_compat/gobber/gobber_scythe
    //
    // because the final suffix is still "_scythe".
    // ------------------------------------------------------------------------

    var i;


    for (
        i = 0;
        i < SIMPLY_SUFFIXES.length;
        i++
    ) {

        if (
            endsWithWeapon(
                path,
                SIMPLY_SUFFIXES[i][0]
            )
        ) {

            return SIMPLY_SUFFIXES[i][1];
        }
    }


    return null;
}


// ============================================================================
// SIMPLY UNIQUE DETECTION
// ============================================================================

function isSimplyUnique(id) {

    if (
        UNIQUE_THIRD[id]
        != null
    ) {

        return true;
    }


    if (
        id.indexOf(
            "simplymore:mimicry_"
        )
        == 0
    ) {

        return true;
    }


    return false;
}


// ============================================================================
// SHIELD DETECTION
//
// Tinkers' plate shield is not necessarily a vanilla ShieldItem subclass,
// so registry IDs containing "shield" under tconstruct are also recognized.
//
// ============================================================================

function isShieldItem(stack, id) {

    var mcStack =
        getMCStack(
            stack
        );


    if (
        mcStack != null
    ) {

        try {

            if (
                mcStack.getItem()
                instanceof ShieldItem
            ) {

                return true;
            }

        } catch (err) {}
    }


    if (
        id != null &&
        id.indexOf(
            "tconstruct:"
        ) == 0 &&
        getPath(id)
            .toLowerCase()
            .indexOf("shield") >= 0
    ) {

        return true;
    }


    return false;
}


// ============================================================================
// SHIELD PROFILE
//
// No upper cap.
//
// ============================================================================

function getShieldProfile(stack) {

    var durability =
        336.0;


    try {

        durability =
            Number(
                stack.getMaxDamage()
            );

    } catch (err) {}


    if (
        !isFinite(
            durability
        ) ||
        durability <= 0
    ) {

        durability =
            336.0;
    }


    var scale =
        durability /
        336.0;


    return {

        "RES":
            0.10 *
            scale,

        "VIT":
            0.03 *
            scale
    };
}


// ============================================================================
// TINKERS TOOL DEFINITION
// ============================================================================

function getTinkersDefinition(stack) {

    var mcStack =
        getMCStack(
            stack
        );


    if (
        mcStack == null
    ) {

        return "";
    }


    try {

        var item =
            mcStack.getItem();


        if (
            item == null
        ) {

            return "";
        }


        var definition =
            item.getToolDefinition();


        if (
            definition != null &&
            definition.getId() != null
        ) {

            return String(
                definition.getId()
            );
        }

    } catch (err) {}


    return "";
}


// ============================================================================
// TINKERS FAMILY NORMALIZER
//
// No generic STR/SKP fallback.
//
// ============================================================================

function normalizeTinkersFamily(value) {

    if (
        value == null ||
        String(value) == ""
    ) {

        return null;
    }


    var path =
        getPath(
            String(value)
        )
        .toLowerCase();


    // ------------------------------------------------------------------------
    // EXACT PROFILE
    // ------------------------------------------------------------------------

    if (
        TINKERS_PROFILES[path]
        != null
    ) {

        return path;
    }


    // ------------------------------------------------------------------------
    // SPECIFIC FAMILIES FIRST
    // ------------------------------------------------------------------------

    if (
        path.indexOf(
            "scythe"
        ) >= 0
    ) {

        return "scythe";
    }


    if (
        path.indexOf(
            "cleaver"
        ) >= 0
    ) {

        return "cleaver";
    }


    if (
        path.indexOf(
            "broad_axe"
        ) >= 0
        ||
        path.indexOf(
            "broadaxe"
        ) >= 0
    ) {

        return "broad_axe";
    }


    if (
        path.indexOf(
            "hand_axe"
        ) >= 0
        ||
        path.indexOf(
            "handaxe"
        ) >= 0
    ) {

        return "hand_axe";
    }


    if (
        path.indexOf(
            "sledge_hammer"
        ) >= 0
        ||
        path.indexOf(
            "sledgehammer"
        ) >= 0
    ) {

        return "sledge_hammer";
    }


    if (
        path.indexOf(
            "vein_hammer"
        ) >= 0
        ||
        path.indexOf(
            "veinhammer"
        ) >= 0
    ) {

        return "vein_hammer";
    }


    if (
        path.indexOf(
            "war_pick"
        ) >= 0
        ||
        path.indexOf(
            "warpick"
        ) >= 0
    ) {

        return "war_pick";
    }


    if (
        path.indexOf(
            "mattock"
        ) >= 0
    ) {

        return "mattock";
    }


    if (
        path.indexOf(
            "pickadze"
        ) >= 0
    ) {

        return "pickadze";
    }


    if (
        path.indexOf(
            "excavator"
        ) >= 0
    ) {

        return "excavator";
    }


    if (
        path.indexOf(
            "battlesign"
        ) >= 0
        ||
        path.indexOf(
            "battle_sign"
        ) >= 0
    ) {

        return "battlesign";
    }


    if (
        path.indexOf(
            "swasher"
        ) >= 0
    ) {

        return "swasher";
    }


    if (
        path.indexOf(
            "minotaur_axe"
        ) >= 0
    ) {

        return "minotaur_axe";
    }


    if (
        path.indexOf(
            "melting_pan"
        ) >= 0
        ||
        path.indexOf(
            "meltingpan"
        ) >= 0
    ) {

        return "melting_pan";
    }


    if (
        path.indexOf(
            "dagger"
        ) >= 0
    ) {

        return "dagger";
    }


    if (
        path.indexOf(
            "kama"
        ) >= 0
    ) {

        return "kama";
    }


    if (
        path.indexOf(
            "javelin"
        ) >= 0
    ) {

        return "javelin";
    }


    if (
        path.indexOf(
            "shuriken"
        ) >= 0
    ) {

        return "shuriken";
    }


    if (
        path.indexOf(
            "throwing_axe"
        ) >= 0
    ) {

        return "throwing_axe";
    }


    if (
        path.indexOf(
            "crossbow"
        ) >= 0
    ) {

        return "crossbow";
    }


    if (
        path.indexOf(
            "longbow"
        ) >= 0
    ) {

        return "longbow";
    }


    if (
        path.indexOf(
            "sky_staff"
        ) >= 0
    ) {

        return "sky_staff";
    }


    if (
        path.indexOf(
            "earth_staff"
        ) >= 0
    ) {

        return "earth_staff";
    }


    if (
        path.indexOf(
            "ichor_staff"
        ) >= 0
    ) {

        return "ichor_staff";
    }


    if (
        path.indexOf(
            "ender_staff"
        ) >= 0
    ) {

        return "ender_staff";
    }


    // ------------------------------------------------------------------------
    // SWORD LAST
    // ------------------------------------------------------------------------

    if (
        path == "sword"
        ||
        path.indexOf(
            "_sword"
        ) >= 0
        ||
        path.indexOf(
            "sword_"
        ) >= 0
    ) {

        return "sword";
    }


    return null;
}


// ============================================================================
// ATTACK DAMAGE READER
//
// ATTACK DAMAGE IS ONLY USED TO CALCULATE THE MULTIPLIER.
//
// THE ATTACK DAMAGE VALUE ITSELF IS NEVER ADDED TO A DMZ STAT.
//
// ============================================================================

function getWeaponAttackDamage(stack) {

    var mcStack =
        getMCStack(
            stack
        );


    if (
        mcStack == null
    ) {

        return 1.0;
    }


    // ========================================================================
    // MINECRAFT ATTRIBUTE MAP
    // ========================================================================

    try {

        var modifiers =
            mcStack
                .getAttributeModifiers(
                    EquipmentSlot.MAINHAND
                )
                .get(
                    Attributes.ATTACK_DAMAGE
                );


        var iterator =
            modifiers.iterator();


        var base =
            1.0;


        var addition =
            0.0;


        var multiplyBase =
            0.0;


        var multiplyTotal =
            1.0;


        while (
            iterator.hasNext()
        ) {

            var modifier =
                iterator.next();


            var amount =
                Number(
                    modifier.getAmount()
                );


            var operation =
                String(
                    modifier.getOperation()
                );


            if (
                operation.indexOf(
                    "ADDITION"
                ) >= 0
            ) {

                addition +=
                    amount;

            } else if (
                operation.indexOf(
                    "MULTIPLY_BASE"
                ) >= 0
            ) {

                multiplyBase +=
                    amount;

            } else if (
                operation.indexOf(
                    "MULTIPLY_TOTAL"
                ) >= 0
            ) {

                multiplyTotal *=
                    (
                        1.0 +
                        amount
                    );
            }
        }


        var result =

            (
                base +
                addition
            )

            *

            (
                1.0 +
                multiplyBase
            )

            *

            multiplyTotal;


        if (
            isFinite(
                result
            ) &&
            result > 0.0
        ) {

            return result;
        }

    } catch (attributeErr) {}


    // ========================================================================
    // CUSTOMNPCS FALLBACK
    // ========================================================================

    try {

        var fallback =
            Number(
                stack.getAttackDamage()
            );


        if (
            isFinite(
                fallback
            )
            &&
            fallback >= 0.0
        ) {

            return (
                fallback +
                1.0
            );
        }

    } catch (fallbackErr) {}


    return 1.0;
}


// ============================================================================
// DAMAGE SCALE
//
// NO UPPER CAP.
//
// Old:
// clamp(0.75 + damage / 18, 0.90, 2.75)
//
// New:
// 0.75 + damage / 18
//
// A 1000 damage weapon is NOT clamped back down.
//
// ============================================================================

function getDamageScale(damage) {

    damage =
        Number(
            damage
        );


    if (
        !isFinite(
            damage
        )
    ) {

        damage =
            1.0;
    }


    return (

        0.75 +

        (
            damage /
            18.0
        )
    );
}


// ============================================================================
// IDENTIFY WEAPON
// ============================================================================

function identifyWeapon(stack) {

    if (
        stack == null
    ) {

        return null;
    }


    try {

        if (
            stack.isEmpty()
        ) {

            return null;
        }

    } catch (err) {

        return null;
    }


    var id =
        getItemId(
            stack
        );


    if (
        id == ""
    ) {

        return null;
    }


    // ========================================================================
    // SHIELDS FIRST
    //
    // This prevents Tinkers plate shields being treated as unknown tools.
    // ========================================================================

    if (
        isShieldItem(
            stack,
            id
        )
    ) {

        return {

            "id":
                id,

            "source":
                "Shield",

            "family":
                "shield",

            "profile":
                getShieldProfile(
                    stack
                ),

            "unique":
                false,

            "weapon":
                false,

            "shield":
                true,

            "definition":
                ""
        };
    }


    // ========================================================================
    // DRAGONMINEZ
    // ========================================================================

    if (
        DMZ_WEAPONS[id]
        != null
    ) {

        return {

            "id":
                id,

            "source":
                "DragonMineZ",

            "family":
                DMZ_WEAPONS[id]
                    .family,

            "profile":
                DMZ_WEAPONS[id]
                    .profile,

            "unique":
                false,

            "weapon":
                true,

            "shield":
                false,

            "definition":
                ""
        };
    }


    // ========================================================================
    // IRON'S SPELLBOOKS
    // ========================================================================

    if (
        IRONS_WEAPONS[id]
        != null
    ) {

        return {

            "id":
                id,

            "source":
                "Iron's Spellbooks",

            "family":
                IRONS_WEAPONS[id]
                    .family,

            "profile":
                IRONS_WEAPONS[id]
                    .profile,

            "unique":
                false,

            "weapon":
                true,

            "shield":
                false,

            "definition":
                ""
        };
    }


    // ========================================================================
    // SIMPLY SWORDS / SIMPLY MORE
    // ========================================================================

    if (
        id.indexOf(
            "simplyswords:"
        ) == 0
        ||
        id.indexOf(
            "simplymore:"
        ) == 0
    ) {

        var simplyFamily =
            getSimplyFamily(
                id
            );


        if (
            simplyFamily == null
        ) {

            if (
                DEBUG_WEAPON_STATS
            ) {

                print(
                    "[DMZ Weapon V9] UNMATCHED SIMPLY ITEM: " +
                    id
                );
            }


            return null;
        }


        var simplyProfile =
            FAMILY_PROFILES[
                simplyFamily
            ];


        if (
            simplyProfile == null
        ) {

            return null;
        }


        return {

            "id":
                id,

            "source":
                (
                    id.indexOf(
                        "simplymore:"
                    ) == 0
                )
                ?
                "Simply More"
                :
                "Simply Swords",

            "family":
                simplyFamily,

            "profile":
                simplyProfile,

            "unique":
                isSimplyUnique(
                    id
                ),

            "weapon":
                true,

            "shield":
                false,

            "definition":
                ""
        };
    }


    // ========================================================================
    // TINKERS
    // ========================================================================

    var definition =
        getTinkersDefinition(
            stack
        );


    var tinkersFamily =
        null;


    // ------------------------------------------------------------------------
    // TOOL DEFINITION TAKES PRIORITY
    // ------------------------------------------------------------------------

    if (
        definition != ""
    ) {

        tinkersFamily =
            normalizeTinkersFamily(
                definition
            );
    }


    // ------------------------------------------------------------------------
    // REGISTRY-ID FALLBACK
    //
    // Only known family names are recognized.
    //
    // This does NOT mean every tconstruct item is treated as a weapon.
    //
    // Therefore:
    //
    // tconstruct:encyclopedia
    //
    // is ignored.
    // ------------------------------------------------------------------------

    if (
        tinkersFamily == null
        &&
        id.indexOf(
            "tconstruct:"
        ) == 0
    ) {

        tinkersFamily =
            normalizeTinkersFamily(
                id
            );
    }


    if (
        tinkersFamily != null
        &&
        TINKERS_PROFILES[
            tinkersFamily
        ] != null
    ) {

        return {

            "id":
                id,

            "source":
                "Tinkers",

            "family":
                tinkersFamily,

            "profile":
                TINKERS_PROFILES[
                    tinkersFamily
                ],

            "unique":
                false,

            "weapon":
                true,

            "shield":
                false,

            "definition":
                definition
        };
    }


    // ------------------------------------------------------------------------
    // A DEFINITION EXISTS BUT WE DO NOT RECOGNIZE IT.
    //
    // Count it as a wielded weapon so slot 9 cannot be exploited,
    // but give NO guessed stats.
    // ------------------------------------------------------------------------

    if (
        definition != ""
    ) {

        return {

            "id":
                id,

            "source":
                "Tinkers",

            "family":
                "UNMATCHED",

            "profile":
                null,

            "unique":
                false,

            "weapon":
                true,

            "shield":
                false,

            "definition":
                definition
        };
    }


    return null;
}


// ============================================================================
// UNIQUE THIRD STAT
// ============================================================================

function chooseThirdStat(
    weaponId,
    profile
) {

    var preferred =
        UNIQUE_THIRD[
            weaponId
        ];


    // Mimicry defaults to VIT if that stat is not already used.
    if (
        preferred == null
        &&
        weaponId.indexOf(
            "simplymore:mimicry_"
        ) == 0
    ) {

        preferred =
            "VIT";
    }


    if (
        preferred != null
        &&
        profile[preferred] == null
    ) {

        return preferred;
    }


    var i;


    for (
        i = 0;
        i < THIRD_STAT_ORDER.length;
        i++
    ) {

        var stat =
            THIRD_STAT_ORDER[i];


        if (
            profile[stat] == null
        ) {

            return stat;
        }
    }


    return "VIT";
}


// ============================================================================
// CALCULATE WEAPON MULTIPLIERS
//
// THERE IS NO UPPER CLAMP.
//
// ============================================================================

function calculateWeapon(
    stack,
    slot,
    info
) {

    if (
        info == null
        ||
        info.profile == null
    ) {

        return null;
    }


    var damage =
        1.0;


    var damageScale =
        1.0;


    if (
        !info.shield
    ) {

        damage =
            getWeaponAttackDamage(
                stack
            );


        damageScale =
            getDamageScale(
                damage
            );
    }


    var profileScale =
        damageScale;


    if (
        info.unique
    ) {

        profileScale *=
            UNIQUE_BASE_MULTIPLIER;
    }


    var result = {

        "slot":
            slot,

        "id":
            info.id,

        "source":
            info.source,

        "family":
            info.family,

        "definition":
            info.definition,

        "damage":
            damage,

        "damageScale":
            damageScale,

        "unique":
            info.unique,

        "STR":
            0.0,

        "SKP":
            0.0,

        "RES":
            0.0,

        "VIT":
            0.0,

        "PWR":
            0.0,

        "ENE":
            0.0
    };


    var i;


    for (
        i = 0;
        i < STAT_KEYS.length;
        i++
    ) {

        var stat =
            STAT_KEYS[i];


        if (
            info.profile[stat]
            != null
        ) {

            result[stat] =

                Number(
                    info.profile[stat]
                )

                *

                profileScale;
        }
    }


    // ========================================================================
    // UNIQUE THIRD STAT
    // ========================================================================

    if (
        info.unique
    ) {

        var third =
            chooseThirdStat(
                info.id,
                info.profile
            );


        result[third] +=

            0.12

            *

            damageScale

            *

            UNIQUE_THIRD_MULTIPLIER;
    }


    // ========================================================================
    // IMPORTANT:
    //
    // NO:
    //
    // Math.min()
    // clamp()
    // MAX_PERCENT
    //
    // IS USED HERE.
    //
    // ========================================================================


    return result;
}


// ============================================================================
// BONUS NAME FOR SLOT
// ============================================================================

function getSlotBonusName(slot) {

    if (
        slot == SLOT_MAIN
    ) {

        return BONUS_MAIN;
    }


    if (
        slot == SLOT_OFF
    ) {

        return BONUS_OFF;
    }


    return BONUS_SLOT9;
}


// ============================================================================
// REMOVE ONE BONUS NAME FROM ALL DMZ STATS
// ============================================================================

function removeBonusName(
    bonus,
    name
) {

    try {
        bonus.removeBonus(
            "STR",
            name
        );
    } catch (err1) {}


    try {
        bonus.removeBonus(
            "SKP",
            name
        );
    } catch (err2) {}


    try {
        bonus.removeBonus(
            "DEF",
            name
        );
    } catch (err3) {}


    try {
        bonus.removeBonus(
            "STM",
            name
        );
    } catch (err4) {}


    try {
        bonus.removeBonus(
            "VIT",
            name
        );
    } catch (err5) {}


    try {
        bonus.removeBonus(
            "PWR",
            name
        );
    } catch (err6) {}


    try {
        bonus.removeBonus(
            "ENE",
            name
        );
    } catch (err7) {}
}


// ============================================================================
// CLEAR ALL OF OUR CURRENT AND OLD WEAPON BONUSES
// ============================================================================

function clearWeaponBonuses(bonus) {

    // ------------------------------------------------------------------------
    // V9
    // ------------------------------------------------------------------------

    removeBonusName(
        bonus,
        BONUS_MAIN
    );


    removeBonusName(
        bonus,
        BONUS_OFF
    );


    removeBonusName(
        bonus,
        BONUS_SLOT9
    );


    // ------------------------------------------------------------------------
    // V7 CUSTOMNPCS NAMES
    // ------------------------------------------------------------------------

    var oldSlots = [
        "MAIN",
        "OFF",
        "SLOT9"
    ];


    var i;
    var j;


    for (
        i = 0;
        i < oldSlots.length;
        i++
    ) {

        for (
            j = 0;
            j < STAT_KEYS.length;
            j++
        ) {

            var oldName =

                "DMZLegacyCNPCWeapon_" +

                oldSlots[i] +

                "_" +

                STAT_KEYS[j];


            removeBonusName(
                bonus,
                oldName
            );
        }
    }


    // ------------------------------------------------------------------------
    // OLD KUBEJS NAMES
    // ------------------------------------------------------------------------

    for (
        i = 0;
        i < oldSlots.length;
        i++
    ) {

        for (
            j = 0;
            j < STAT_KEYS.length;
            j++
        ) {

            var kubeName =

                "DMZLegacyWeapon_" +

                oldSlots[i] +

                "_" +

                STAT_KEYS[j];


            removeBonusName(
                bonus,
                kubeName
            );
        }
    }


    // ------------------------------------------------------------------------
    // ANCIENT AGGREGATED NAMES
    // ------------------------------------------------------------------------

    removeBonusName(
        bonus,
        "DMZLegacyWeapon_STR"
    );


    removeBonusName(
        bonus,
        "DMZLegacyWeapon_SKP"
    );


    removeBonusName(
        bonus,
        "DMZLegacyWeapon_RES"
    );


    removeBonusName(
        bonus,
        "DMZLegacyWeapon_VIT"
    );


    removeBonusName(
        bonus,
        "DMZLegacyWeapon_PWR"
    );


    removeBonusName(
        bonus,
        "DMZLegacyWeapon_ENE"
    );
}


// ============================================================================
// REMOVE APOTHICDMZ FLAT ATTRIBUTE BONUSES
//
// I inspected ApothicDMZ 1.0.2.
//
// Its current DMZ bridge creates names exactly like:
//
// Attribute Bonus STR flat
// Attribute Bonus STR multiplier
//
// The FLAT version uses:
//
// operation = "+"
// applyMultipliers = false
//
// The MULTIPLIER version uses:
//
// operation = "*"
// applyMultipliers = true
//
// We remove ONLY the "... flat" entries.
//
// ============================================================================

function removeApothicFlatBonuses(bonus) {

    if (
        !REMOVE_APOTHIC_FLAT_BONUSES
    ) {

        return false;
    }


    var stats = [
        "STR",
        "SKP",
        "DEF",
        "STM",
        "VIT",
        "PWR",
        "ENE"
    ];


    var removed =
        false;


    var i;


    for (
        i = 0;
        i < stats.length;
        i++
    ) {

        var stat =
            stats[i];


        var name =

            "Attribute Bonus " +

            stat +

            " flat";


        try {

            if (
                bonus.hasBonus(
                    stat,
                    name
                )
            ) {

                bonus.removeBonus(
                    stat,
                    name
                );


                removed =
                    true;
            }

        } catch (err) {}
    }


    return removed;
}


// ============================================================================
// APPLY A SINGLE MULTIPLIER
//
// THIS FUNCTION HAS NO "+" PATH.
//
// It ONLY uses:
//
// "*"
//
// and the final boolean is ALWAYS:
//
// true
//
// ============================================================================

function applyMultiplier(
    bonus,
    stat,
    slot,
    percent
) {

    percent =
        Number(
            percent
        );


    if (
        !isFinite(
            percent
        )
        ||
        percent <= 0.0
    ) {

        return;
    }


    var factor =

        1.0 +

        percent;


    var name =
        getSlotBonusName(
            slot
        );


    if (
        stat == "RES"
    ) {

        bonus.addBonusSplit(

            "RES",

            name,

            "*",

            factor,

            false
        );

    } else {

        bonus.addBonus(

            stat,

            name,

            "*",

            factor,

            false
        );
    }
}


// ============================================================================
// APPLY WHOLE SLOT
// ============================================================================

function applySlot(
    bonus,
    result
) {

    if (
        result == null
    ) {

        return;
    }


    applyMultiplier(
        bonus,
        "STR",
        result.slot,
        result.STR
    );


    applyMultiplier(
        bonus,
        "SKP",
        result.slot,
        result.SKP
    );


    applyMultiplier(
        bonus,
        "RES",
        result.slot,
        result.RES
    );


    applyMultiplier(
        bonus,
        "VIT",
        result.slot,
        result.VIT
    );


    applyMultiplier(
        bonus,
        "PWR",
        result.slot,
        result.PWR
    );


    applyMultiplier(
        bonus,
        "ENE",
        result.slot,
        result.ENE
    );
}


// ============================================================================
// RESULT SIGNATURE
// ============================================================================

function makeResultSignature(result) {

    if (
        result == null
    ) {

        return "NONE";
    }


    return (

        result.slot +

        ":" +

        result.id +

        ":" +

        result.family +

        ":" +

        result.definition +

        ":" +

        result.damage.toFixed(4) +

        ":" +

        result.STR.toFixed(6) +

        ":" +

        result.SKP.toFixed(6) +

        ":" +

        result.RES.toFixed(6) +

        ":" +

        result.VIT.toFixed(6) +

        ":" +

        result.PWR.toFixed(6) +

        ":" +

        result.ENE.toFixed(6)
    );
}


// ============================================================================
// RAW HAND SIGNATURE
//
// Ensures changing to an unsupported tool is also noticed.
//
// ============================================================================

function makeRawSignature(
    stack,
    info
) {

    var id =
        getItemId(
            stack
        );


    if (
        id == ""
    ) {

        return "EMPTY";
    }


    if (
        info == null
    ) {

        return (
            id +
            ":NONE"
        );
    }


    return (

        id +

        ":" +

        info.family +

        ":" +

        info.definition
    );
}


// ============================================================================
// DEBUG RESULT
// ============================================================================

function debugResult(
    playerName,
    result
) {

    if (
        !DEBUG_WEAPON_STATS
        ||
        result == null
    ) {

        return;
    }


    print(

        "[DMZ Weapon V9] " +

        playerName +

        " " +

        result.slot +

        " item=" +

        result.id +

        " source=" +

        result.source +

        " family=" +

        result.family +

        (
            result.definition != ""
            ?
            " definition=" +
            result.definition
            :
            ""
        ) +

        " damage=" +

        result.damage.toFixed(2) +

        " scale=" +

        result.damageScale.toFixed(4) +

        " unique=" +

        result.unique +

        " | STR x" +

        (
            1.0 +
            result.STR
        ).toFixed(4) +

        " SKP x" +

        (
            1.0 +
            result.SKP
        ).toFixed(4) +

        " RES x" +

        (
            1.0 +
            result.RES
        ).toFixed(4) +

        " VIT x" +

        (
            1.0 +
            result.VIT
        ).toFixed(4) +

        " PWR x" +

        (
            1.0 +
            result.PWR
        ).toFixed(4) +

        " ENE x" +

        (
            1.0 +
            result.ENE
        ).toFixed(4)
    );
}


// ============================================================================
// DMZ DATA
// ============================================================================

function getDmzData(mcPlayer) {

    try {

        var optional =
            StatsProvider.get(

                StatsCapability.INSTANCE,

                mcPlayer
            );


        if (
            optional == null
        ) {

            return null;
        }


        return optional.orElse(
            null
        );

    } catch (err) {

        return null;
    }
}


// ============================================================================
// SYNC DMZ
// ============================================================================

function syncDmz(mcPlayer) {

    try {

        NetworkHandler.sendToPlayer(

            new StatsSyncS2C(
                mcPlayer
            ),

            mcPlayer
        );

    } catch (err) {

        if (
            DEBUG_WEAPON_STATS
        ) {

            print(
                "[DMZ Weapon V9] Sync error: " +
                err
            );
        }
    }
}


// ============================================================================
// MAIN RECALCULATION
// ============================================================================

function updateWeaponMultipliers(
    player,
    force
) {

    try {

        var mcPlayer =
            player.getMCEntity();


        if (
            mcPlayer == null
        ) {

            return;
        }


        var data =
            getDmzData(
                mcPlayer
            );


        if (
            data == null
        ) {

            return;
        }


        var bonus =
            data.getBonusStats();


        // ====================================================================
        // REMOVE APOTHIC FLAT ATTRIBUTE ENTRIES
        // ====================================================================

        var flatRemoved =
            removeApothicFlatBonuses(
                bonus
            );


        // ====================================================================
        // EQUIPMENT
        // ====================================================================

        var mainStack =
            player.getMainhandItem();


        var offStack =
            player.getOffhandItem();


        var slot9Stack =
            player
                .getInventory()
                .getSlot(
                    SLOT_9_INDEX
                );


        // ====================================================================
        // IDENTIFY
        // ====================================================================

        var mainInfo =
            identifyWeapon(
                mainStack
            );


        var offInfo =
            identifyWeapon(
                offStack
            );


        var slot9Info =
            identifyWeapon(
                slot9Stack
            );


        // ====================================================================
        // REAL WEAPON IN HAND?
        //
        // Shields do NOT disable the slot-9 fallback.
        //
        // Empty main + shield off + slot9 weapon:
        //
        // shield + slot9 weapon
        //
        // Sword main + shield off + slot9 weapon:
        //
        // sword + shield
        //
        // ====================================================================

        var handWeaponPresent =
            false;


        if (
            mainInfo != null
            &&
            mainInfo.weapon
        ) {

            handWeaponPresent =
                true;
        }


        if (
            offInfo != null
            &&
            offInfo.weapon
        ) {

            handWeaponPresent =
                true;
        }


        // ====================================================================
        // RESULTS
        // ====================================================================

        var mainResult =
            null;


        var offResult =
            null;


        var slot9Result =
            null;


        if (
            mainInfo != null
            &&
            mainInfo.profile != null
        ) {

            mainResult =
                calculateWeapon(

                    mainStack,

                    SLOT_MAIN,

                    mainInfo
                );
        }


        if (
            offInfo != null
            &&
            offInfo.profile != null
        ) {

            offResult =
                calculateWeapon(

                    offStack,

                    SLOT_OFF,

                    offInfo
                );
        }


        if (
            !handWeaponPresent
            &&
            slot9Info != null
            &&
            slot9Info.weapon
            &&
            slot9Info.profile != null
        ) {

            slot9Result =
                calculateWeapon(

                    slot9Stack,

                    SLOT_9,

                    slot9Info
                );
        }


        // ====================================================================
        // SIGNATURE
        // ====================================================================

        var signature =

            SCRIPT_VERSION +

            "|MAINRAW=" +

            makeRawSignature(
                mainStack,
                mainInfo
            ) +

            "|OFFRAW=" +

            makeRawSignature(
                offStack,
                offInfo
            ) +

            "|SLOT9RAW=" +

            makeRawSignature(
                slot9Stack,
                slot9Info
            ) +

            "|MAIN=" +

            makeResultSignature(
                mainResult
            ) +

            "|OFF=" +

            makeResultSignature(
                offResult
            ) +

            "|SLOT9=" +

            makeResultSignature(
                slot9Result
            );


        var temp =
            player.getTempdata();


        var same =
            false;


        if (
            temp.has(
                TEMP_SIGNATURE
            )
        ) {

            same =
                String(
                    temp.get(
                        TEMP_SIGNATURE
                    )
                )
                ==
                signature;
        }


        // ====================================================================
        // NOTHING CHANGED
        //
        // If a flat Apothic bonus was removed, sync that removal.
        // ====================================================================

        if (
            !force
            &&
            same
        ) {

            if (
                flatRemoved
            ) {

                syncDmz(
                    mcPlayer
                );
            }


            return;
        }


        // ====================================================================
        // CLEAR OLD WEAPON BONUSES
        // ====================================================================

        clearWeaponBonuses(
            bonus
        );


        // Remove flat bridge again in case anything recreated it during
        // recalculation.
        removeApothicFlatBonuses(
            bonus
        );


        // ====================================================================
        // APPLY MULTIPLIERS ONLY
        // ====================================================================

        applySlot(
            bonus,
            mainResult
        );


        applySlot(
            bonus,
            offResult
        );


        applySlot(
            bonus,
            slot9Result
        );


        // ====================================================================
        // SAVE
        // ====================================================================

        temp.put(

            TEMP_SIGNATURE,

            signature
        );


        // ====================================================================
        // SYNC
        // ====================================================================

        syncDmz(
            mcPlayer
        );


        // ====================================================================
        // DEBUG
        // ====================================================================

        if (
            DEBUG_WEAPON_STATS
        ) {

            var playerName =
                player.getName();


            print(
                "======================================================"
            );


            print(

                "[DMZ Weapon V9] Recalculated " +

                playerName
            );


            // ---------------------------------------------------------------
            // UNKNOWN TINKERS
            // ---------------------------------------------------------------

            if (
                mainInfo != null
                &&
                mainInfo.source == "Tinkers"
                &&
                mainInfo.family == "UNMATCHED"
            ) {

                print(

                    "[DMZ Weapon V9] UNMATCHED TINKERS MAIN: " +

                    mainInfo.id +

                    " definition=" +

                    mainInfo.definition
                );
            }


            if (
                offInfo != null
                &&
                offInfo.source == "Tinkers"
                &&
                offInfo.family == "UNMATCHED"
            ) {

                print(

                    "[DMZ Weapon V9] UNMATCHED TINKERS OFF: " +

                    offInfo.id +

                    " definition=" +

                    offInfo.definition
                );
            }


            if (
                slot9Info != null
                &&
                slot9Info.source == "Tinkers"
                &&
                slot9Info.family == "UNMATCHED"
            ) {

                print(

                    "[DMZ Weapon V9] UNMATCHED TINKERS SLOT9: " +

                    slot9Info.id +

                    " definition=" +

                    slot9Info.definition
                );
            }


            // ---------------------------------------------------------------
            // RESULTS
            // ---------------------------------------------------------------

            debugResult(
                playerName,
                mainResult
            );


            debugResult(
                playerName,
                offResult
            );


            debugResult(
                playerName,
                slot9Result
            );


            if (
                mainResult == null
                &&
                offResult == null
                &&
                slot9Result == null
            ) {

                print(
                    "[DMZ Weapon V9] No active weapon multiplier bonuses."
                );
            }


            print(
                "[DMZ Weapon V9] Weapon operation: * ONLY"
            );


            print(
                "[DMZ Weapon V9] applyMultipliers: TRUE"
            );


            print(
                "[DMZ Weapon V9] Upper multiplier cap: NONE"
            );


            print(
                "[DMZ Weapon V9] TagKey usage: NONE"
            );


            print(
                "======================================================"
            );
        }


    } catch (err) {

        print(

            "[DMZ Weapon V9 ERROR] " +

            err
        );
    }
}


// ============================================================================
// APOTHIC FLAT CLEANUP
//
// ApothicDMZ can recreate the flat attribute entry a few ticks after an
// equipment event.
//
// We therefore remove ONLY its named flat entries every tick.
//
// This does NOT continuously rebuild our weapon multipliers.
//
// ============================================================================

function cleanupApothicFlats(player) {

    if (
        !REMOVE_APOTHIC_FLAT_BONUSES
    ) {

        return;
    }


    try {

        var mcPlayer =
            player.getMCEntity();


        if (
            mcPlayer == null
        ) {

            return;
        }


        var data =
            getDmzData(
                mcPlayer
            );


        if (
            data == null
        ) {

            return;
        }


        var removed =
            removeApothicFlatBonuses(

                data.getBonusStats()
            );


        if (
            removed
        ) {

            syncDmz(
                mcPlayer
            );
        }

    } catch (err) {}
}


// ============================================================================
// INIT
// ============================================================================

function init(event) {

    try {

        var player =
            event.player;


        if (
            player == null
        ) {

            return;
        }


        player
            .getTempdata()
            .remove(
                TEMP_SIGNATURE
            );


        updateWeaponMultipliers(

            player,

            true
        );

    } catch (err) {

        print(

            "[DMZ Weapon V9 INIT ERROR] " +

            err
        );
    }
}


// ============================================================================
// TICK
// ============================================================================

function tick(event) {

    try {

        var player =
            event.player;


        if (
            player == null
        ) {

            return;
        }


        // ====================================================================
        // FLAT CLEANUP
        // ====================================================================

        cleanupApothicFlats(
            player
        );


        // ====================================================================
        // EQUIPMENT RECALCULATION
        // ====================================================================

        if (
            (
                player.getAge()
                %
                UPDATE_INTERVAL
            )
            != 0
        ) {

            return;
        }


        updateWeaponMultipliers(

            player,

            false
        );

    } catch (err) {

        print(

            "[DMZ Weapon V9 TICK ERROR] " +

            err
        );
    }
}