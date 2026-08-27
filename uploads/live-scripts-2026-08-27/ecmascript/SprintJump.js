/*
============================================================
 DBZ Legacy Reborn - Sprint + Jump Skills
 Version: 1.1.0

 PLACE AS:
 CustomNPCs Global Player Script
 Enable event: tick

 REPLACES:
 - Sprint.js
 - Jump.js
 - JumpSprint.js (same file; keep one tab only)

 Disable the old Sprint / Jump / JumpSprint tabs after installing
 this one so they do not double-level the same skills.

 CHANGELOG:
 - v1.1.0: Unlock from STR points invested (current STR minus
   race/class baseStats.STR), not raw getStrength(). Races/classes
   that start with higher STR no longer get free sprint/jump
   levels, and low-base races are not gated on total STR alone.
============================================================
*/

var StatsProvider = Java.type("com.dragonminez.common.stats.StatsProvider");
var StatsCapability = Java.type("com.dragonminez.common.stats.StatsCapability");
var StatsSyncS2C = Java.type("com.dragonminez.common.network.S2C.StatsSyncS2C");
var NetworkHandler = Java.type("com.dragonminez.common.network.NetworkHandler");
var ConfigManager = Java.type("com.dragonminez.common.config.ConfigManager");
var System = Java.type("java.lang.System");

var COLOR = "\u00A7";
var CHECK_INTERVAL_MS = 1000;
var TEMP_NEXT_CHECK = "sprintjump_skill_next_check";
var MAX_LEVEL = 10;

/*
 * Thresholds are invested STR points (TP spent into Strength),
 * not absolute getStrength() and not DMZ overall level.
 */
var STRENGTH_REQUIREMENTS = {
    1: 20,
    2: 100,
    3: 250,
    4: 500,
    5: 1000,
    6: 1500,
    7: 2000,
    8: 2500,
    9: 3000,
    10: 3500
};

var SKILLS = [
    {
        id: "sprint",
        label: "Sprint",
        tag: "SPRINT"
    },
    {
        id: "jump",
        label: "Jump",
        tag: "Jump"
    }
];

function tick(event) {
    var player = event.player;
    if (player == null) return;

    var temp = player.getTempdata();
    var now = Number(System.currentTimeMillis());

    if (temp.has(TEMP_NEXT_CHECK)) {
        var next = parseInt("" + temp.get(TEMP_NEXT_CHECK), 10);
        if (!isNaN(next) && now < next) return;
    }

    temp.put(TEMP_NEXT_CHECK, "" + (now + CHECK_INTERVAL_MS));

    try {
        var mcPlayer = player.getMCEntity ? player.getMCEntity() : player;
        var data = StatsProvider
            .get(StatsCapability.INSTANCE, mcPlayer)
            .orElse(null);

        if (data == null) return;

        var stats = data.getStats();
        var skills = data.getSkills();
        if (stats == null || skills == null) return;

        var strengthPoints = getInvestedStrengthPoints(data, stats);

        var changed = false;

        for (var i = 0; i < SKILLS.length; i++) {
            if (updateSkillFromStrength(player, skills, SKILLS[i], strengthPoints)) {
                changed = true;
            }
        }

        if (changed) {
            NetworkHandler.sendToTrackingEntityAndSelf(
                new StatsSyncS2C(mcPlayer),
                mcPlayer
            );
        }
    } catch (err) {
        try {
            player.message(
                COLOR + "4[SprintJump Script Error] " + COLOR + "f" + err
            );
        } catch (ignored) {}
    }
}

/**
 * Race/class starting STR from DMZ config (baseStats.STR).
 * Returns 0 if lookup fails so we never invent a fake base.
 */
function getRaceClassBaseStrength(data) {
    try {
        var character = data.getCharacter();
        if (character == null) return 0;

        var race = null;
        try { race = character.getRaceName(); } catch (e1) {}
        if (race == null || race === "") {
            try { race = character.getRace(); } catch (e2) {}
        }

        var cls = null;
        try { cls = character.getCharacterClass(); } catch (e3) {}

        if (race == null || race === "" || cls == null || cls === "") return 0;

        var raceStats = ConfigManager.getRaceStats(String(race));
        if (raceStats == null) return 0;

        var classStats = raceStats.getClassStats(String(cls));
        if (classStats == null) return 0;

        var base = classStats.getBaseStats();
        if (base == null) return 0;

        var strObj = base.getStrength();
        if (strObj == null) return 0;

        var baseStr = Number(strObj);
        if (isNaN(baseStr) || baseStr < 0) return 0;
        return baseStr;
    } catch (err) {
        return 0;
    }
}

/**
 * Points the player put into Strength (stored STR minus race/class base).
 * Does not use DMZ getLevel() or form-scaled combat values.
 */
function getInvestedStrengthPoints(data, stats) {
    var total = Number(stats.getStrength());
    if (isNaN(total) || total < 0) total = 0;

    var base = getRaceClassBaseStrength(data);
    var invested = total - base;
    if (invested < 0) invested = 0;
    return invested;
}

function updateSkillFromStrength(player, skills, skillInfo, strengthPoints) {
    var skillId = skillInfo.id;

    skills.registerDefaultSkill(skillId, MAX_LEVEL);
    skills.refreshNonFormSkillMaxLevels();

    var current = Number(skills.getSkillLevel(skillId));
    var max = Number(skills.getMaxSkillLevel(skillId));

    if (isNaN(current) || current < 0) current = 0;
    if (isNaN(max) || max <= 0) max = MAX_LEVEL;
    if (max > MAX_LEVEL) max = MAX_LEVEL;

    var target = current;

    for (var level = 1; level <= max; level++) {
        var need = STRENGTH_REQUIREMENTS[level];
        if (need != null && strengthPoints >= need) {
            target = level;
        }
    }

    if (target <= current) return false;

    skills.setSkillLevel(skillId, target);

    if (current < 1) {
        player.message(
            COLOR + "a[" + skillInfo.tag + "] " +
            skillInfo.label + " unlocked at level " + target +
            " (" + strengthPoints + " STR invested)."
        );
    } else {
        player.message(
            COLOR + "a[" + skillInfo.tag + "] Increased to level " +
            target + " (" + strengthPoints + " STR invested)."
        );
    }

    if (target >= max) {
        player.message(
            COLOR + "6[" + skillInfo.tag + "] " +
            skillInfo.label + " is now maxed."
        );
    }

    return true;
}
