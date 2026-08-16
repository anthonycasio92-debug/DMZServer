// kubejs/startup_scripts/building_tp_place.js
// Silent Building TP on block place (Forge EntityPlaceEvent).
// MUST be startup_scripts — ForgeEvents.onEvent only registers on first load.
//
// Fabled Building.yml: Value Set only (no Command / no dmzpoints).
// Award: BuildingLevel TP via StatsProvider.get + addTrainingPoints (silent).
//
// Requires full restart once after adding/changing this file.
// server_scripts/building_tp.js also has BlockEvents.placed as a backup.

console.info("[BuildingTP-Startup] registering EntityPlaceEvent...");

var SKILL_NAME = "Building";
var VALUE_BASE = 1.0;
var VALUE_SCALE = 1.0;
var MAX_AWARD = 10000;
var DEBUG = true;
var DEBUG_SKIPS = true;

var lastAwardTick = {};
var lastSkipLog = {};
var ServerPlayerCls = null;
var StatsProviderCls = null;
var StatsCapabilityCls = null;
var NetworkHandlerCls = null;
var ResourceSyncS2CCls = null;

function loadJava() {
    if (ServerPlayerCls != null && StatsProviderCls != null) return true;
    try {
        ServerPlayerCls = Java.loadClass(
            "net.minecraft.server.level.ServerPlayer"
        );
        StatsProviderCls = Java.loadClass(
            "com.dragonminez.common.stats.StatsProvider"
        );
        StatsCapabilityCls = Java.loadClass(
            "com.dragonminez.common.stats.StatsCapability"
        );
        try {
            NetworkHandlerCls = Java.loadClass(
                "com.dragonminez.common.network.NetworkHandler"
            );
            ResourceSyncS2CCls = Java.loadClass(
                "com.dragonminez.common.network.S2C.ResourceSyncS2C"
            );
        } catch (eNet) {}
        return true;
    } catch (err) {
        console.error("[BuildingTP-Startup] Java load failed: " + err);
        return false;
    }
}

function className(obj) {
    try {
        return String(obj.getClass().getName());
    } catch (e) {
        return String(obj);
    }
}

function isServerPlayer(entity) {
    if (entity == null || !loadJava()) return false;
    // KubeJS loadClass returns java.lang.Class — use Class.isInstance, NOT .class.isInstance
    try {
        if (ServerPlayerCls.isInstance(entity)) return true;
    } catch (e0) {}
    try {
        if (entity instanceof ServerPlayerCls) return true;
    } catch (e1) {}
    try {
        if (entity.isPlayer && entity.isPlayer()) return true;
    } catch (e2) {}
    try {
        var cn = className(entity);
        if (cn.indexOf("ServerPlayer") >= 0) return true;
    } catch (e3) {}
    return false;
}

function asServerPlayer(entity) {
    if (entity == null) return null;
    try {
        if (entity.minecraftPlayer) entity = entity.minecraftPlayer;
    } catch (e0) {}
    try {
        if (entity.getMinecraftPlayer) entity = entity.getMinecraftPlayer();
    } catch (e1) {}
    try {
        if (entity.getHandle) entity = entity.getHandle();
    } catch (e2) {}
    try {
        if (entity.getMCEntity) entity = entity.getMCEntity();
    } catch (e3) {}
    if (isServerPlayer(entity)) return entity;
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

function playerName(sp) {
    try {
        return String(sp.getGameProfile().getName());
    } catch (e0) {}
    try {
        return String(sp.getName().getString());
    } catch (e1) {}
    try {
        return String(sp.getScoreboardName());
    } catch (e2) {}
    return "?";
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
    if (sp == null || !loadJava()) return null;
    // Farming-style path (works in CNPC; same API from KubeJS when entity type is correct)
    try {
        var lazy = StatsProviderCls.get(StatsCapabilityCls.INSTANCE, sp);
        if (lazy != null) {
            var data = lazy.orElse(null);
            if (data != null) return data;
        }
    } catch (e1) {
        if (DEBUG) {
            console.error(
                "[BuildingTP-Startup] StatsProvider.get failed: " + e1
            );
        }
    }
    try {
        var lazy2 = sp.getCapability(StatsCapabilityCls.INSTANCE);
        if (lazy2 != null) {
            var data2 = lazy2.orElse(null);
            if (data2 != null) return data2;
        }
    } catch (e2) {}
    try {
        var lazy3 = sp.getCapability(StatsCapabilityCls.INSTANCE, null);
        if (lazy3 != null) {
            var data3 = lazy3.orElse(null);
            if (data3 != null) return data3;
        }
    } catch (e3) {}
    return null;
}

function logSkip(sp, reason) {
    if (!DEBUG || !DEBUG_SKIPS) return;
    var id = playerUuid(sp) || className(sp);
    var now = Date.now();
    if (lastSkipLog[id] && now - lastSkipLog[id] < 2000) return;
    lastSkipLog[id] = now;
    console.info(
        "[BuildingTP-Startup] skip " +
            playerName(sp) +
            " (" +
            id +
            "): " +
            reason
    );
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
    if (id && lastAwardTick[id] === tick) {
        logSkip(sp, "same-tick dedupe");
        return false;
    }

    var data = getStatsData(sp);
    if (data == null) {
        logSkip(sp, "no StatsData");
        return false;
    }
    var resources = data.getResources();
    if (resources == null) {
        logSkip(sp, "no Resources");
        return false;
    }

    try {
        resources.addTrainingPoints(amount * 1.0);
    } catch (eAdd) {
        try {
            var cur = Number(resources.getTrainingPoints());
            if (!isFinite(cur) || cur < 0) cur = 0;
            resources.setTrainingPoints(cur + amount);
        } catch (eSet) {
            logSkip(sp, "addTrainingPoints failed: " + eAdd + " / " + eSet);
            return false;
        }
    }

    try {
        if (NetworkHandlerCls != null && ResourceSyncS2CCls != null) {
            NetworkHandlerCls.sendToTrackingEntityAndSelf(
                new ResourceSyncS2CCls(sp),
                sp
            );
        }
    } catch (eSync) {}

    if (id) lastAwardTick[id] = tick;
    if (DEBUG) {
        console.info(
            "[BuildingTP-Startup] +" +
                amount +
                " TP -> " +
                playerName(sp) +
                " (silent)"
        );
    }
    return true;
}

function handlePlace(entity) {
    var sp = asServerPlayer(entity);
    if (sp == null) {
        if (DEBUG && DEBUG_SKIPS && entity != null) {
            var cn = className(entity);
            if (cn.indexOf("Player") >= 0) {
                console.info(
                    "[BuildingTP-Startup] skip non-ServerPlayer entity: " + cn
                );
            }
        }
        return;
    }
    var level = buildingLevelFor(sp);
    var amount = amountFromLevel(level);
    if (amount < 1) {
        logSkip(sp, "Building level=" + level + " (need >= 1)");
        return;
    }
    addTrainingPointsSilent(sp, amount);
}

ForgeEvents.onEvent(
    "net.minecraftforge.event.level.BlockEvent$EntityPlaceEvent",
    function (event) {
        try {
            var entity = null;
            try {
                entity = event.getEntity();
            } catch (e1) {
                try {
                    entity = event.entity;
                } catch (e2) {}
            }
            handlePlace(entity);
        } catch (err) {
            console.error("[BuildingTP-Startup] place error: " + err);
        }
    }
);

console.info(
    "[BuildingTP-Startup] EntityPlaceEvent registered (silent Building TP, DEBUG=" +
        DEBUG +
        ")"
);
