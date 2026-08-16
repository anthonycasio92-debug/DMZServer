// Building TP Block Place Bonus
// CNPC Global FORGE Script (ScriptList: building tp skill.js)
// Event: blockEventEntityPlaceEvent  (BlockEvent$EntityPlaceEvent)
// Fabled skill: Building
// Award: 1 TP per Building level (same curve as Building.yml value-base/scale)
//
// Mirrors Farming TP Skill.js (StatsProvider.addTrainingPoints via CNPC Java.type).
// CNPC player scripts have no place hook, so this runs on the Forge script tab.

var DEBUG = false;
var SKILL_NAME = "Building";
var TP_PER_LEVEL = 1;

function blockEventEntityPlaceEvent(event) {
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

        /* Only real players. */
        var isPlayer = false;
        try {
            var Player = Java.type("net.minecraft.world.entity.player.Player");
            isPlayer = Player.class.isInstance(entity);
        } catch (e3) {
            try {
                isPlayer = ("" + entity.getClass().getName()).indexOf("Player") >= 0;
            } catch (e4) {
                return;
            }
        }
        if (!isPlayer) return;

        /* Ignore client. */
        try {
            var level = entity.m_9236_ ? entity.m_9236_() : entity.level();
            if (level != null && level.m_5776_ && level.m_5776_()) return;
            if (level != null && level.isClientSide) return;
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

        /* Light debounce per block pos (matches Farming 500ms style). */
        var posX = 0;
        var posY = 0;
        var posZ = 0;
        try {
            var pos = null;
            if (typeof forge.getPos === "function") pos = forge.getPos();
            else if (forge.getState != null && typeof forge.getPos === "function") {
                pos = forge.getPos();
            }
            if (pos != null) {
                posX = pos.m_123341_ ? pos.m_123341_() : pos.getX();
                posY = pos.m_123342_ ? pos.m_123342_() : pos.getY();
                posZ = pos.m_123343_ ? pos.m_123343_() : pos.getZ();
            }
        } catch (ePos) {}

        var now = new Date().getTime();
        if (typeof global === "undefined") {
            /* Nashorn/CNPC: stash on Java static-less map via binding */
        }
        if (!blockEventEntityPlaceEvent._last) {
            blockEventEntityPlaceEvent._last = {};
        }
        var posKey = uuid + "_" + posX + "_" + posY + "_" + posZ;
        var last = blockEventEntityPlaceEvent._last[posKey];
        if (last != null && now - last < 500) return;
        blockEventEntityPlaceEvent._last[posKey] = now;

        var skillLevel = 0;
        try {
            var File = Java.type("java.io.File");
            var Scanner = Java.type("java.util.Scanner");

            var file = new File("plugins/Fabled/players/" + uuid + ".yml");
            if (file.exists()) {
                var scan = new Scanner(file);
                var foundSkill = false;

                while (scan.hasNextLine()) {
                    var line = "" + scan.nextLine();
                    var trimmed = line.trim();

                    if (trimmed == SKILL_NAME + ":") {
                        foundSkill = true;
                        continue;
                    }

                    if (foundSkill && trimmed.indexOf("level:") == 0) {
                        skillLevel = parseInt(trimmed.replace("level:", "").trim());
                        if (isNaN(skillLevel)) skillLevel = 0;
                        break;
                    }

                    /* Left the Building block. */
                    if (
                        foundSkill &&
                        trimmed.length > 0 &&
                        trimmed.indexOf("level:") != 0 &&
                        trimmed.charAt(trimmed.length - 1) == ":" &&
                        trimmed.indexOf(" ") != 0
                    ) {
                        break;
                    }
                }

                scan.close();
            }
        } catch (skillErr) {
            if (DEBUG) {
                print("[Building TP] skill read error: " + skillErr);
            }
        }

        if (skillLevel <= 0) {
            if (DEBUG) print("[Building TP] place ignored — Building Lv.0 for " + uuid);
            return;
        }

        var tpAward = skillLevel * TP_PER_LEVEL;
        if (tpAward < 1) return;

        var StatsProvider = Java.type("com.dragonminez.common.stats.StatsProvider");
        var StatsCapability = Java.type("com.dragonminez.common.stats.StatsCapability");

        var lazy = StatsProvider.get(StatsCapability.INSTANCE, entity);
        if (lazy == null) return;

        var dmzData = lazy.orElse(null);
        if (dmzData == null) return;

        var resources = dmzData.getResources();
        if (resources == null) return;

        resources.addTrainingPoints(tpAward);

        if (DEBUG) {
            print("[Building TP] +" + tpAward + " TP | Lv." + skillLevel + " | " + uuid);
        }
    } catch (e) {
        try {
            print("[Building TP] error: " + e);
        } catch (eLog) {}
    }
}
