// kubejs/startup_scripts/building_tp_place.js
// Silent Building TP on block place (Forge EntityPlaceEvent).
// MUST be startup_scripts — ForgeEvents.onEvent only registers on first load.
//
// Fabled Building.yml: Value Set only (no Command / no dmzpoints).
// Award: BuildingLevel TP via getCapability + addTrainingPoints (silent).
//
// Requires full restart once after adding/changing this file.
// server_scripts/building_tp.js also has BlockEvents.placed as a backup.

console.info("[BuildingTP-Startup] registering EntityPlaceEvent...");

var SKILL_NAME = "Building";
var VALUE_BASE = 1.0;
var VALUE_SCALE = 1.0;
var MAX_AWARD = 10000;

var lastAwardTick = {};

function asServerPlayer(entity) {
    if (entity == null) return null;
    try {
        var ServerPlayer = Java.loadClass(
            "net.minecraft.server.level.ServerPlayer"
        );
        if (ServerPlayer.class.isInstance(entity)) return entity;
    } catch (e) {}
    return null;
}

function playerUuid(sp) {
    try {
        return String(sp.getUUID()).toLowerCase();
    } catch (e0) {}
    try {
        return String(sp.getStringUUID()).toLowerCase();
    } catch (e1) {}
    return null;
}

function readBuildingLevelYaml(uuid) {
    if (!uuid) return 0;
    try {
        var Files = Java.loadClass("java.nio.file.Files");
        var Paths = Java.loadClass("java.nio.file.Paths");
        var path = Paths.get("plugins/Fabled/players/" + uuid + ".yml");
        if (!Files.exists(path)) return 0;
        var lines = Files.readAllLines(path);
        var found = false;
        for (var i = 0; i < lines.size(); i++) {
            var trimmed = String(lines.get(i)).trim();
            if (trimmed === SKILL_NAME + ":") {
                found = true;
                continue;
            }
            if (found && trimmed.indexOf("level:") === 0) {
                var level = parseInt(trimmed.replace("level:", "").trim(), 10);
                return isNaN(level) ? 0 : Math.max(0, level);
            }
            if (
                found &&
                trimmed.length > 0 &&
                trimmed.charAt(trimmed.length - 1) === ":" &&
                trimmed.indexOf("level:") !== 0
            ) {
                break;
            }
        }
    } catch (err) {}
    return 0;
}

function buildingLevelFor(sp) {
    var uuid = playerUuid(sp);
    try {
        var Bukkit = Java.loadClass("org.bukkit.Bukkit");
        var UUID = Java.loadClass("java.util.UUID");
        if (uuid) {
            var bp = Bukkit.getPlayer(UUID.fromString(uuid));
            if (bp != null) {
                var Fabled = Java.loadClass("studio.magemonkey.fabled.Fabled");
                if (Fabled.isLoaded()) {
                    var data = Fabled.getData(bp);
                    if (data != null) {
                        var level = Math.max(
                            0,
                            Number(data.getSkillLevel(SKILL_NAME)) || 0
                        );
                        if (level >= 1) return level;
                    }
                }
            }
        }
    } catch (err) {}
    return readBuildingLevelYaml(uuid);
}

function amountFromLevel(level) {
    if (!(level >= 1)) return 0;
    var amount = VALUE_BASE + (level - 1) * VALUE_SCALE;
    if (!isFinite(amount) || amount < 1) return 0;
    return Math.min(MAX_AWARD, Math.floor(amount));
}

function getStatsData(sp) {
    if (sp == null) return null;
    try {
        var StatsCapability = Java.loadClass(
            "com.dragonminez.common.stats.StatsCapability"
        );
        var lazy = sp.getCapability(StatsCapability.INSTANCE);
        if (lazy != null) {
            var data = lazy.orElse(null);
            if (data != null) return data;
        }
    } catch (e1) {}
    try {
        var StatsCapability2 = Java.loadClass(
            "com.dragonminez.common.stats.StatsCapability"
        );
        var lazy2 = sp.getCapability(StatsCapability2.INSTANCE, null);
        if (lazy2 != null) {
            var data2 = lazy2.orElse(null);
            if (data2 != null) return data2;
        }
    } catch (e2) {}
    return null;
}

function addTrainingPointsSilent(sp, amount) {
    if (sp == null || !(amount > 0)) return false;
    amount = Math.min(MAX_AWARD, Math.floor(Number(amount)));
    if (amount < 1) return false;

    var tick = 0;
    try {
        tick = Number(sp.level.getGameTime());
    } catch (e0) {
        try {
            tick = Number(sp.getLevel().getGameTime());
        } catch (e1) {}
    }
    var id = playerUuid(sp);
    if (id && lastAwardTick[id] === tick) return false;

    var data = getStatsData(sp);
    if (data == null) return false;
    var resources = data.getResources();
    if (resources == null) return false;

    try {
        resources.addTrainingPoints(amount * 1.0);
    } catch (eAdd) {
        try {
            var cur = Number(resources.getTrainingPoints());
            if (!isFinite(cur) || cur < 0) cur = 0;
            resources.setTrainingPoints(cur + amount);
        } catch (eSet) {
            return false;
        }
    }

    try {
        var NetworkHandler = Java.loadClass(
            "com.dragonminez.common.network.NetworkHandler"
        );
        var ResourceSyncS2C = Java.loadClass(
            "com.dragonminez.common.network.S2C.ResourceSyncS2C"
        );
        NetworkHandler.sendToTrackingEntityAndSelf(
            new ResourceSyncS2C(sp),
            sp
        );
    } catch (eSync) {}

    if (id) lastAwardTick[id] = tick;
    return true;
}

ForgeEvents.onEvent(
    "net.minecraftforge.event.level.BlockEvent$EntityPlaceEvent",
    function (event) {
        try {
            var entity = null;
            try {
                entity = event.getEntity();
            } catch (e1) {}
            var sp = asServerPlayer(entity);
            if (sp == null) return;
            var level = buildingLevelFor(sp);
            var amount = amountFromLevel(level);
            if (amount < 1) return;
            addTrainingPointsSilent(sp, amount);
        } catch (err) {
            console.error("[BuildingTP-Startup] place error: " + err);
        }
    }
);

console.info(
    "[BuildingTP-Startup] EntityPlaceEvent registered (silent Building TP)"
);
