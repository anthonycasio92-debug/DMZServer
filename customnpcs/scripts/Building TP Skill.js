// Building TP Block Place Bonus
// CNPC Global FORGE Script (ScriptList: building tp skill.js)
// Event: blockEventEntityPlaceEvent  (BlockEvent$EntityPlaceEvent)
// Fabled skill: Building
// Award: 1 TP per Building level (matches Building.yml value-base/scale)
//
// Mirrors Farming TP Skill.js:
//   - read Fabled skill level from plugins/Fabled/players/<uuid>.yml
//   - StatsProvider.get(...).addTrainingPoints(tpAward)
//
// CNPC player scripts have no place hook, so this file must be on the
// Forge scripts tab (not the Player scripts tab).

var DEBUG = false;
var SKILL_NAME = "Building";
var TP_PER_LEVEL = 1;
var LAST_BUILD_TP = {};

function blockEventEntityPlaceEvent(event) {
    awardBuildingTpFromPlace(event);
}

/* Aliases in case CNPC names the nested event differently. */
function entityPlaceEvent(event) {
    awardBuildingTpFromPlace(event);
}

function blockEventPlaceEvent(event) {
    awardBuildingTpFromPlace(event);
}

function awardBuildingTpFromPlace(event) {
    try {
        var forge = null;
        try {
            forge = event.event;
        } catch (e0) {
            forge = event;
        }
        if (forge == null) return;

        var entity = null;
        try {
            if (typeof forge.getEntity === "function") {
                entity = forge.getEntity();
            }
        } catch (e1) {}
        if (entity == null) {
            try {
                entity = forge.entity;
            } catch (e2) {}
        }
        if (entity == null) return;

        var isPlayer = false;
        try {
            var Player = Java.type("net.minecraft.world.entity.player.Player");
            isPlayer = Player.class.isInstance(entity);
        } catch (e3) {
            try {
                isPlayer =
                    ("" + entity.getClass().getName()).indexOf("Player") >= 0;
            } catch (e4) {
                return;
            }
        }
        if (!isPlayer) return;

        try {
            var level = null;
            try {
                level = entity.level();
            } catch (eLvl1) {
                try {
                    level = entity.m_9236_();
                } catch (eLvl2) {}
            }
            if (level != null) {
                try {
                    if (level.isClientSide) return;
                } catch (eCS1) {}
                try {
                    if (level.m_5776_ && level.m_5776_()) return;
                } catch (eCS2) {}
            }
        } catch (e5) {}

        var uuid = "";
        try {
            uuid = ("" + entity.getUUID()).toLowerCase();
        } catch (e6) {
            try {
                uuid = ("" + entity.getStringUUID()).toLowerCase();
            } catch (e7) {
                return;
            }
        }
        if (uuid === "") return;

        var posX = 0;
        var posY = 0;
        var posZ = 0;
        try {
            var pos = null;
            if (typeof forge.getPos === "function") {
                pos = forge.getPos();
            }
            if (pos != null) {
                try {
                    posX = pos.getX();
                    posY = pos.getY();
                    posZ = pos.getZ();
                } catch (ePosN) {
                    try {
                        posX = pos.m_123341_();
                        posY = pos.m_123342_();
                        posZ = pos.m_123343_();
                    } catch (ePosO) {}
                }
            }
        } catch (ePos) {}

        var now = new Date().getTime();
        var posKey = uuid + "_" + posX + "_" + posY + "_" + posZ;
        var last = LAST_BUILD_TP[posKey];
        if (last != null && now - last < 500) return;
        LAST_BUILD_TP[posKey] = now;

        var skillLevel = readFabledSkillLevel(uuid, SKILL_NAME);
        if (skillLevel <= 0) {
            if (DEBUG) {
                print(
                    "[Building TP] place ignored — Building Lv.0 for " + uuid
                );
            }
            return;
        }

        var tpAward = skillLevel * TP_PER_LEVEL;
        if (tpAward < 1) return;

        var StatsProvider = Java.type(
            "com.dragonminez.common.stats.StatsProvider"
        );
        var StatsCapability = Java.type(
            "com.dragonminez.common.stats.StatsCapability"
        );

        var lazy = StatsProvider.get(StatsCapability.INSTANCE, entity);
        if (lazy == null) return;

        var dmzData = lazy.orElse(null);
        if (dmzData == null) return;

        var resources = dmzData.getResources();
        if (resources == null) return;

        resources.addTrainingPoints(tpAward);

        if (DEBUG) {
            print(
                "[Building TP] +" +
                    tpAward +
                    " TP | Lv." +
                    skillLevel +
                    " | " +
                    uuid
            );
        }
    } catch (e) {
        try {
            print("[Building TP] error: " + e);
        } catch (eLog) {}
    }
}

function readFabledSkillLevel(uuid, skillName) {
    var skillLevel = 0;
    try {
        var File = Java.type("java.io.File");
        var Scanner = Java.type("java.util.Scanner");

        var file = new File("plugins/Fabled/players/" + uuid + ".yml");
        if (!file.exists()) return 0;

        var scan = new Scanner(file);
        var foundSkill = false;

        while (scan.hasNextLine()) {
            var line = "" + scan.nextLine();
            var trimmed = line.trim();

            if (trimmed == skillName + ":") {
                foundSkill = true;
                continue;
            }

            if (foundSkill && trimmed.indexOf("level:") == 0) {
                skillLevel = parseInt(
                    trimmed.replace("level:", "").trim(),
                    10
                );
                if (isNaN(skillLevel)) skillLevel = 0;
                break;
            }

            if (
                foundSkill &&
                trimmed.length > 0 &&
                trimmed.indexOf("level:") != 0 &&
                trimmed.charAt(trimmed.length - 1) == ":"
            ) {
                break;
            }
        }

        scan.close();
    } catch (skillErr) {
        if (DEBUG) print("[Building TP] skill read error: " + skillErr);
        skillLevel = 0;
    }
    return skillLevel;
}
