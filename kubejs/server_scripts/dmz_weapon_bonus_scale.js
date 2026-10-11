// ============================================================================
// DMZ WEAPON BONUS SCALE
// Minecraft 1.20.1 Forge / KubeJS
//
// Put in:
// kubejs/server_scripts/dmz_weapon_bonus_scale.js
//
// Weapons grant ONLY multiplicative DMZ bonus stats (*).
// No flat + weapon bonuses.
//
// SLOT RULE:
// - Mainhand + Offhand = allowed
// - Mainhand + Slot 9 = allowed
// - Offhand + Slot 9 = allowed
// - Mainhand + Offhand + Slot 9 = NOT allowed
//
// If all three slots contain valid bonus items, Slot 9 is suppressed.
//
// IMPORTANT SLOT 9 FIX:
//
// Hotbar slot 9 is inventory index 8.
//
// When the player SELECTS hotbar slot 9, Minecraft exposes that exact same
// ItemStack as both:
//
// - Mainhand
// - Inventory slot 8
//
// Therefore, while slot 9 is selected, the passive Slot 9 bonus is disabled.
// The weapon counts ONLY as the Mainhand weapon.
//
// This prevents one physical weapon from receiving MAIN + SLOT9 bonuses.
//
// IMPORTANT MULTIPLIER BEHAVIOR:
//
// "*" = actual DMZ multiplicative bonus operation.
//
// applyMultipliers = false means the weapon bonus is applied outside the
// transformation multiplier stage.
//
// This prevents forms from multiplying the weapon contribution again.
//
// Debug defaults to false.
// ============================================================================


// ============================================================================
// JAVA CLASSES
// ============================================================================

var StatsProvider =
    Java.loadClass("com.dragonminez.common.stats.StatsProvider");

var StatsCapability =
    Java.loadClass("com.dragonminez.common.stats.StatsCapability");

var StatsSyncS2C =
    Java.loadClass("com.dragonminez.common.network.S2C.StatsSyncS2C");

var NetworkHandler =
    Java.loadClass("com.dragonminez.common.network.NetworkHandler");

var EquipmentSlot =
    Java.loadClass("net.minecraft.world.entity.EquipmentSlot");

var Attributes =
    Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes");

var ShieldItem =
    Java.loadClass("net.minecraft.world.item.ShieldItem");


// ============================================================================
// CONFIG
// ============================================================================

var DEBUG_WEAPON_STATS = false;

// Recalculate equipment once per second. The tick handler also returns
// before that unless the player age is a multiple of 20.
var UPDATE_INTERVAL = 20;

// Hotbar position 9 = inventory index 8.
var SLOT_9_INDEX = 8;

// Simply Swords / Simply More uniques.
//
// 1.80 means 80% stronger base stat scaling.
var UNIQUE_BASE_MULTIPLIER = 1.80;

// Third-stat multiplier.
var UNIQUE_THIRD_MULTIPLIER = 1.35;

// Removes ApothicDMZ's aggregated:
//
// "Attribute Bonus STR flat"
// "Attribute Bonus SKP flat"
// etc.
//
// Leave false unless specifically wanted.
var REMOVE_APOTHIC_FLAT_BONUSES = false;

var SCRIPT_VERSION =
    "DMZ_WEAPON_MULTIPLIERS_V11_SLOT9_SELECTED_FIX";

var TEMP_SIGNATURE =
    "DMZWeaponMultiplierSignatureV11Slot9SelectedFix";


// ============================================================================
// SLOT IDS
// ============================================================================

var SLOT_MAIN = "MAIN";
var SLOT_OFF = "OFF";
var SLOT_9 = "SLOT9";


// ============================================================================
// BONUS NAMES
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
// Percentages are scaled by actual weapon attack damage.
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
// SIMPLY SWORDS / SIMPLY MORE EXACT FAMILY MAP
// ============================================================================

var SIMPLY_EXACT_FAMILY = {

    // BACKHAND BLADE
    "simplyswords:emberlash": "backhand_blade",
    "simplymore:boas_fang": "backhand_blade",
    "simplymore:smouldering_ruin": "backhand_blade",

    // CHAKRAM
    "simplyswords:mjolnir": "chakram",
    "simplyswords:tempest": "chakram",
    "simplymore:vipers_call": "chakram",

    // CLAYMORE
    "simplymore:blade_of_the_grotesque": "claymore",
    "simplyswords:caelestis": "claymore",
    "simplymore:deaths_eyrie": "claymore",
    "simplyswords:twisted_blade": "claymore",
    "simplyswords:slumbering_lichblade": "claymore",
    "simplyswords:waking_lichblade": "claymore",
    "simplyswords:awakened_lichblade": "claymore",
    "simplyswords:waxweaver": "claymore",

    // CUTLASS
    "simplymore:black_pearl": "cutlass",

    // DAGGER
    "simplymore:the_vessel_breach": "dagger",
    "simplymore:culterex": "dagger",

    // DEER HORNS
    "simplymore:tidebreaker": "deer_horns",
    "simplymore:brassturn": "deer_horns",

    // GLAIVE
    "simplymore:soul_foreseer": "glaive",
    "simplyswords:flamewind": "glaive",

    // GRANDSWORD
    "simplymore:grandfrost": "grandsword",
    "simplyswords:enigma": "grandsword",
    "simplyswords:ribboncleaver": "grandsword",
    "simplymore:molten_flare": "grandsword",

    // GREAT KATANA
    "simplymore:great_slither": "great_katana",
    "simplymore:lustrous_moxie": "great_katana",

    // GREAT SPEAR
    "simplymore:perforiscus": "great_spear",
    "simplymore:serpentine_valour": "great_spear",

    // GREATAXE
    "simplymore:earthshatter": "greataxe",
    "simplyswords:soulpyre": "greataxe",

    // GREATHAMMER
    "simplyswords:hearthflame": "greathammer",
    "simplyswords:soulkeeper": "greathammer",
    "simplyswords:hiveheart": "greathammer",

    // HALBERD
    "simplyswords:icewhisper": "halberd",
    "simplyswords:arcanethyst": "halberd",
    "simplyswords:thunderbrand": "halberd",

    // KATANA
    "simplymore:revvengine": "katana",
    "simplyswords:whisperwind": "katana",

    // KHOPESH
    "simplymore:timekeeper": "khopesh",
    "simplymore:myrmedge": "khopesh",

    // LANCE
    "simplymore:glimmerstep": "lance",
    "simplymore:jester_penetrate": "lance",
    "simplymore:exedrill": "lance",

    // LONGSWORD
    "simplymore:matterbane": "longsword",
    "simplyswords:decaying_relic": "longsword",
    "simplyswords:stormbringer": "longsword",
    "simplyswords:emberblade": "longsword",
    "simplyswords:dormant_relic": "longsword",
    "simplyswords:righteous_relic": "longsword",
    "simplyswords:tainted_relic": "longsword",
    "simplyswords:sunfire": "longsword",
    "simplyswords:harbinger": "longsword",

    // PERNACH
    "simplymore:the_pan": "pernach",
    "simplymore:cindergorge": "pernach",
    "simplymore:ruptured_idol": "pernach",
    "simplymore:ascended_idol": "pernach",
    "simplymore:tarnished_idol": "pernach",
    "simplymore:holylight": "pernach",
    "simplymore:darksent": "pernach",

    // QUARTERSTAFF
    "simplymore:stasis": "quarterstaff",
    "simplymore:ruyi_jingu_bang": "quarterstaff",

    // RAPIER
    "simplyswords:bramblethorn": "rapier",
    "simplyswords:shadowsting": "rapier",

    // SAI
    "simplyswords:soulstealer": "sai",

    // SCYTHE
    "simplyswords:magiscythe": "scythe",
    "simplyswords:soulrender": "scythe",
    "simplymore:the_blood_harvester": "scythe",

    // SPEAR
    "simplyswords:wickpiercer": "spear",
    "simplyswords:magispear": "spear",
    "simplyswords:sword_on_a_stick": "spear",

    // TWINBLADE
    "simplyswords:storms_edge": "twinblade",
    "simplyswords:magiblade": "twinblade",
    "simplyswords:stars_edge": "twinblade",
    "simplyswords:dreadtide": "twinblade",

    // SPECIAL SIMPLY SWORDS METADATA
    "simplyswords:frostfall": "mace",
    "simplyswords:livyatan": "sickle",
    "simplyswords:molten_edge": "sickle"
};


// ============================================================================
// NORMAL SIMPLY WEAPON SUFFIXES
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
// ============================================================================

var UNIQUE_THIRD = {

    // SIMPLY SWORDS

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


    // SIMPLY MORE

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
            stack.isEmpty
            &&
            stack.isEmpty()
        ) {

            return "";
        }

    } catch (e0) {}


    try {

        if (
            stack.empty
        ) {

            return "";
        }

    } catch (e1) {}


    try {

        if (
            stack.id
        ) {

            return String(
                stack.id
            );
        }

    } catch (e2) {}


    try {

        var n =
            stack.getName();


        if (
            n
        ) {

            return String(
                n
            );
        }

    } catch (e3) {}


    try {

        var ForgeRegistries =
            Java.loadClass(
                "net.minecraftforge.registries.ForgeRegistries"
            );


        var mc =
            getMCStack(
                stack
            );


        if (
            mc != null
        ) {

            var key =
                ForgeRegistries.ITEMS.getKey(
                    mc.getItem()
                );


            if (
                key != null
            ) {

                return String(
                    key
                );
            }
        }

    } catch (e4) {}


    return "";
}


function getMCStack(stack) {

    if (
        stack == null
    ) {

        return null;
    }


    try {

        if (
            stack.isEmpty
            &&
            stack.isEmpty()
        ) {

            return null;
        }

    } catch (e0) {}


    try {

        if (
            stack.empty
        ) {

            return null;
        }

    } catch (e1) {}


    try {

        if (
            stack.getMCItemStack
        ) {

            return stack.getMCItemStack();
        }

    } catch (e2) {}


    try {

        if (
            stack.itemStack
        ) {

            return stack.itemStack;
        }

    } catch (e3) {}


    try {

        if (
            stack.minecraftItemStack
        ) {

            return stack.minecraftItemStack;
        }

    } catch (e4) {}


    return stack;
}


// ============================================================================
// SIMPLY FAMILY DETECTION
// ============================================================================

function getSimplyFamily(id) {

    if (
        id == null
        ||
        id == ""
    ) {

        return null;
    }


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
        id != null
        &&
        id.indexOf(
            "tconstruct:"
        ) == 0
        &&
        getPath(
            id
        )
        .toLowerCase()
        .indexOf(
            "shield"
        ) >= 0
    ) {

        return true;
    }


    return false;
}


// ============================================================================
// SHIELD PROFILE
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
        )
        ||
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
            definition != null
            &&
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
// ============================================================================

function normalizeTinkersFamily(value) {

    if (
        value == null
        ||
        String(value) == ""
    ) {

        return null;
    }


    var path =
        getPath(
            String(value)
        )
        .toLowerCase();


    if (
        TINKERS_PROFILES[path]
        != null
    ) {

        return path;
    }


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


    var modifierMap =
        mcStack.getAttributeModifiers(
            EquipmentSlot.MAINHAND
        );


    var modifiers =
        modifierMap == null
            ? null
            : modifierMap.get(
                Attributes.ATTACK_DAMAGE
            );


    if (
        modifiers != null
        &&
        modifiers.iterator != null
    ) {

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
            )
            &&
            result > 0.0
        ) {

            return result;
        }
    }


    if (
        stack != null
        &&
        stack.getAttackDamage != null
    ) {

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
    }


    return 1.0;
}


// ============================================================================
// DAMAGE SCALE
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
    // SHIELDS
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

                console.info(

                    "[DMZ Weapon V11] UNMATCHED SIMPLY ITEM: " +

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


    if (
        definition != ""
    ) {

        tinkersFamily =
            normalizeTinkersFamily(
                definition
            );
    }


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
        profile[
            preferred
        ] == null
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
            profile[
                stat
            ] == null
        ) {

            return stat;
        }
    }


    return "VIT";
}


// ============================================================================
// CALCULATE WEAPON MULTIPLIERS
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
            info.profile[
                stat
            ] != null
        ) {

            result[
                stat
            ] =

                Number(
                    info.profile[
                        stat
                    ]
                )

                *

                profileScale;
        }
    }


    if (
        info.unique
    ) {

        var third =
            chooseThirdStat(
                info.id,
                info.profile
            );


        result[
            third
        ] +=

            0.12

            *

            damageScale

            *

            UNIQUE_THIRD_MULTIPLIER;
    }


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

    if (
        bonus == null
        ||
        bonus.removeBonus == null
    ) {

        return;
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


    for (
        var i = 0;
        i < stats.length;
        i++
    ) {

        bonus.removeBonus(
            stats[i],
            name
        );
    }
}


// ============================================================================
// CLEAR ALL CURRENT AND OLD WEAPON BONUSES
// ============================================================================

function clearWeaponBonuses(bonus) {

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
// OPTIONAL APOTHIC FLAT BONUS REMOVAL
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
// "*" = actual multiplicative operation.
//
// false = apply outside DMZ transformation multiplier stage.
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
// RAW SLOT SIGNATURE
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


    console.info(

        "[DMZ Weapon V11] " +

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

            console.info(

                "[DMZ Weapon V11] Sync error: " +

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
        // SLOT 9 CURRENTLY SELECTED?
        //
        // Hotbar slot 9 = inventory index 8.
        //
        // When selected, Minecraft exposes inventory slot 8 as mainhand.
        //
        // In that situation the SAME physical weapon would otherwise receive:
        //
        // MAIN bonus
        // +
        // SLOT9 bonus
        //
        // We suppress the passive Slot 9 contribution while slot 9 is selected.
        // ====================================================================

        var slot9IsMainhand =
            false;


        try {

            var mcInventory =
                mcPlayer.getInventory();


            if (
                Number(
                    mcInventory.selected
                )
                ==
                SLOT_9_INDEX
            ) {

                slot9IsMainhand =
                    true;
            }

        } catch (slot9SelectedErr) {}


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
        // RESULTS
        // ====================================================================

        var mainResult =
            null;


        var offResult =
            null;


        var slot9Result =
            null;


        // ====================================================================
        // MAINHAND
        // ====================================================================

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


        // ====================================================================
        // OFFHAND
        // ====================================================================

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


        // ====================================================================
        // ACTIVE HAND BONUS COUNT
        //
        // Maximum active bonus sources = 2.
        //
        // Allowed:
        //
        // MAIN + OFF
        // MAIN + SLOT9
        // OFF + SLOT9
        //
        // Not allowed:
        //
        // MAIN + OFF + SLOT9
        //
        // A shield counts as a hand bonus source because it grants stats.
        // ====================================================================

        var activeHandBonusCount =
            0;


        if (
            mainInfo != null
            &&
            mainInfo.profile != null
        ) {

            activeHandBonusCount++;
        }


        if (
            offInfo != null
            &&
            offInfo.profile != null
        ) {

            activeHandBonusCount++;
        }


        // ====================================================================
        // SLOT 9
        //
        // Requirements:
        //
        // 1. Slot 9 must NOT currently be selected as mainhand.
        // 2. Fewer than two hand bonus sources may already be active.
        // 3. Slot 9 must contain a recognized weapon with a profile.
        //
        // This prevents both:
        //
        // - three active bonus sources
        // - the same selected slot-9 weapon counting twice
        // ====================================================================

        if (
            !slot9IsMainhand
            &&
            activeHandBonusCount < 2
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
        //
        // slot9IsMainhand is included explicitly.
        //
        // This is important because selecting/deselecting hotbar slot 9 may
        // leave the actual slot contents unchanged, but it DOES change whether
        // the passive Slot 9 bonus should exist.
        // ====================================================================

        var signature =

            SCRIPT_VERSION +

            "|SLOT9SELECTED=" +

            slot9IsMainhand +

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


        removeApothicFlatBonuses(
            bonus
        );


        // ====================================================================
        // APPLY MULTIPLIERS
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
        // SAVE SIGNATURE
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


            console.info(
                "======================================================"
            );


            console.info(

                "[DMZ Weapon V11] Recalculated " +

                playerName
            );


            console.info(

                "[DMZ Weapon V11] Active hand bonus sources: " +

                activeHandBonusCount
            );


            console.info(

                "[DMZ Weapon V11] Slot 9 selected as mainhand: " +

                slot9IsMainhand
            );


            if (
                slot9IsMainhand
                &&
                slot9Info != null
                &&
                slot9Info.weapon
                &&
                slot9Info.profile != null
            ) {

                console.info(
                    "[DMZ Weapon V11] Slot 9 passive bonus suppressed because slot 9 is currently selected as mainhand."
                );
            }


            if (
                !slot9IsMainhand
                &&
                activeHandBonusCount >= 2
                &&
                slot9Info != null
                &&
                slot9Info.weapon
                &&
                slot9Info.profile != null
            ) {

                console.info(
                    "[DMZ Weapon V11] Slot 9 suppressed because mainhand + offhand already consume both allowed bonus sources."
                );
            }


            if (
                mainInfo != null
                &&
                mainInfo.source == "Tinkers"
                &&
                mainInfo.family == "UNMATCHED"
            ) {

                console.info(

                    "[DMZ Weapon V11] UNMATCHED TINKERS MAIN: " +

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

                console.info(

                    "[DMZ Weapon V11] UNMATCHED TINKERS OFF: " +

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

                console.info(

                    "[DMZ Weapon V11] UNMATCHED TINKERS SLOT9: " +

                    slot9Info.id +

                    " definition=" +

                    slot9Info.definition
                );
            }


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

                console.info(
                    "[DMZ Weapon V11] No active weapon multiplier bonuses."
                );
            }


            console.info(
                "[DMZ Weapon V11] Maximum active bonus sources: 2"
            );


            console.info(
                "[DMZ Weapon V11] Slot9 selected -> passive Slot9 disabled"
            );


            console.info(
                "[DMZ Weapon V11] All 3 valid -> MAIN + OFF; SLOT9 suppressed"
            );


            console.info(
                "[DMZ Weapon V11] Weapon operation: * ONLY"
            );


            console.info(
                "[DMZ Weapon V11] applyMultipliers: FALSE"
            );


            console.info(
                "[DMZ Weapon V11] Form amplification: DISABLED"
            );


            console.info(
                "[DMZ Weapon V11] Upper multiplier cap: NONE"
            );


            console.info(
                "======================================================"
            );
        }


    } catch (err) {

        console.info(

            "[DMZ Weapon V11 ERROR] " +

            err
        );
    }
}


// ============================================================================
// APOTHIC FLAT CLEANUP
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
// KUBEJS PLAYER ADAPTER
// ============================================================================

var TempByPlayer = {};


function playerUuid(player) {

    if (
        player == null
    ) {

        return null;
    }


    if (
        player.uuid != null
    ) {

        return String(
            player.uuid
        ).toLowerCase();
    }


    var mc =
        unwrapMcPlayer(
            player
        );


    if (
        mc != null
        &&
        mc.getUUID != null
    ) {

        var id =
            mc.getUUID();


        if (
            id != null
        ) {

            return String(
                id
            ).toLowerCase();
        }
    }


    if (
        player.getStringUUID != null
    ) {

        var raw =
            player.getStringUUID();


        if (
            raw != null
        ) {

            return String(
                raw
            ).toLowerCase();
        }
    }


    return null;
}


function unwrapMcPlayer(player) {

    if (
        player == null
    ) {

        return null;
    }


    var p =
        player;


    if (
        p.minecraftPlayer != null
    ) {

        p =
            p.minecraftPlayer;
    }


    if (
        p != null
        &&
        p.getMinecraftPlayer != null
    ) {

        var fromKube =
            p.getMinecraftPlayer();


        if (
            fromKube != null
        ) {

            p =
                fromKube;
        }
    }


    if (
        p != null
        &&
        p.getPlayer != null
    ) {

        var fromGetPlayer =
            p.getPlayer();


        if (
            fromGetPlayer != null
        ) {

            p =
                fromGetPlayer;
        }
    }


    if (
        p != null
        &&
        p.getMCEntity != null
    ) {

        var fromEntity =
            p.getMCEntity();


        if (
            fromEntity != null
        ) {

            p =
                fromEntity;
        }
    }


    if (
        p != null
        &&
        p.getHandle != null
    ) {

        var fromHandle =
            p.getHandle();


        if (
            fromHandle != null
        ) {

            p =
                fromHandle;
        }
    }


    return p;
}


function makeTempData(uuid) {

    if (
        !uuid
    ) {

        uuid =
            "_";
    }


    if (
        !TempByPlayer[
            uuid
        ]
    ) {

        TempByPlayer[
            uuid
        ] = {};
    }


    var store =
        TempByPlayer[
            uuid
        ];


    return {

        has:
            function (key) {

                return Object.prototype.hasOwnProperty.call(
                    store,
                    key
                );
            },

        get:
            function (key) {

                return store[
                    key
                ];
            },

        put:
            function (
                key,
                value
            ) {

                store[
                    key
                ] =
                    value;
            },

        remove:
            function (key) {

                delete store[
                    key
                ];
            }
    };
}


function wrapPlayer(kjsPlayer) {

    var uuid =
        playerUuid(
            kjsPlayer
        );


    var temp =
        makeTempData(
            uuid
        );


    return {

        _kjs:
            kjsPlayer,


        getMCEntity:
            function () {

                return unwrapMcPlayer(
                    kjsPlayer
                );
            },


        getMainhandItem:
            function () {

                if (
                    kjsPlayer != null
                    &&
                    kjsPlayer.mainHandItem != null
                ) {

                    return kjsPlayer.mainHandItem;
                }


                if (
                    kjsPlayer != null
                    &&
                    kjsPlayer.getMainHandItem != null
                ) {

                    var held =
                        kjsPlayer.getMainHandItem();


                    if (
                        held != null
                    ) {

                        return held;
                    }
                }


                var mc =
                    unwrapMcPlayer(
                        kjsPlayer
                    );


                if (
                    mc != null
                    &&
                    mc.getMainHandItem != null
                ) {

                    return mc.getMainHandItem();
                }


                return null;
            },


        getOffhandItem:
            function () {

                if (
                    kjsPlayer != null
                    &&
                    kjsPlayer.offHandItem != null
                ) {

                    return kjsPlayer.offHandItem;
                }


                if (
                    kjsPlayer != null
                    &&
                    kjsPlayer.getOffHandItem != null
                ) {

                    var held =
                        kjsPlayer.getOffHandItem();


                    if (
                        held != null
                    ) {

                        return held;
                    }
                }


                var mc =
                    unwrapMcPlayer(
                        kjsPlayer
                    );


                if (
                    mc != null
                    &&
                    mc.getOffhandItem != null
                ) {

                    return mc.getOffhandItem();
                }


                return null;
            },


        getInventory:
            function () {

                return {

                    getSlot:
                        function (index) {

                            if (
                                kjsPlayer != null
                                &&
                                kjsPlayer.inventory != null
                            ) {

                                var inv =
                                    kjsPlayer.inventory;


                                if (
                                    inv.get != null
                                ) {

                                    var byGet =
                                        inv.get(
                                            index
                                        );


                                    if (
                                        byGet != null
                                    ) {

                                        return byGet;
                                    }
                                }


                                if (
                                    inv.getStackInSlot != null
                                ) {

                                    var bySlot =
                                        inv.getStackInSlot(
                                            index
                                        );


                                    if (
                                        bySlot != null
                                    ) {

                                        return bySlot;
                                    }
                                }
                            }


                            var mc =
                                unwrapMcPlayer(
                                    kjsPlayer
                                );


                            if (
                                mc != null
                                &&
                                mc.getInventory != null
                            ) {

                                var mcInv =
                                    mc.getInventory();


                                if (
                                    mcInv != null
                                    &&
                                    mcInv.getItem != null
                                ) {

                                    return mcInv.getItem(
                                        index
                                    );
                                }
                            }


                            return null;
                        }
                };
            },


        getTempdata:
            function () {

                return temp;
            },


        getName:
            function () {

                if (
                    kjsPlayer != null
                    &&
                    kjsPlayer.name != null
                    &&
                    kjsPlayer.name.string != null
                ) {

                    return String(
                        kjsPlayer.name.string
                    );
                }


                if (
                    kjsPlayer != null
                    &&
                    kjsPlayer.getName != null
                ) {

                    var component =
                        kjsPlayer.getName();


                    if (
                        component != null
                        &&
                        component.getString != null
                    ) {

                        return String(
                            component.getString()
                        );
                    }
                }


                if (
                    kjsPlayer != null
                    &&
                    kjsPlayer.username != null
                ) {

                    return String(
                        kjsPlayer.username
                    );
                }


                return "?";
            },


        getAge:
            function () {

                if (
                    kjsPlayer != null
                    &&
                    kjsPlayer.age != null
                ) {

                    return Number(
                        kjsPlayer.age
                    );
                }


                var mc =
                    unwrapMcPlayer(
                        kjsPlayer
                    );


                if (
                    mc != null
                    &&
                    mc.tickCount != null
                ) {

                    return Number(
                        mc.tickCount
                    );
                }


                return 0;
            }
    };
}


// ============================================================================
// KUBEJS EVENTS
// ============================================================================

PlayerEvents.loggedIn(
    function (event) {

        try {

            var player =
                wrapPlayer(
                    event.player
                );


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

            console.info(

                "[DMZ Weapon V11 INIT ERROR] " +

                err
            );
        }
    }
);


// Shared tick in player_tick_consolidated.js calls this. No listener here.
global.dmzWeaponBonusTick = {
    UPDATE_INTERVAL: UPDATE_INTERVAL,
    wrapPlayer: wrapPlayer,
    cleanupApothicFlats: cleanupApothicFlats,
    updateWeaponMultipliers: updateWeaponMultipliers
};


console.info(
    "[DMZ Weapon V11] KubeJS server script loaded. Max 2 active bonus sources; selected Slot9 never counts twice."
);