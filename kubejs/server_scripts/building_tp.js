// kubejs/server_scripts/building_tp.js
// Silent Building TP backup via BlockEvents.placed.
// Primary hook is startup_scripts/building_tp_place.js (Forge EntityPlaceEvent).
// Same-tick dedupe prevents double awards if both fire.
//
// Reload: /kubejs reload server_scripts
// Startup hook needs a full restart once.

console.info("[BuildingTP] server BlockEvents.placed backup loading...");

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
var JAVA_READY = false;

function initJava() {
    if (JAVA_READY) return true;
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
        JAVA_READY =
            ServerPlayerCls != null &&
            StatsProviderCls != null &&
            StatsCapabilityCls != null;
    } catch (err) {
        console.error("[BuildingTP] Java init failed: " + err);
    }
    return JAVA_READY;
}

function className(obj) {
    try {
        return String(obj.getClass().getName());
    } catch (e) {
        return String(obj);
    }
}

function isServerPlayer(entity) {
    if (entity == null || !initJava()) return false;
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

function asServerPlayer(player) {
    if (!player || !initJava()) return null;
    var p = player;
    try {
        if (p.minecraftPlayer) p = p.minecraftPlayer;
    } catch (e0) {}
    try {
        if (p.getMinecraftPlayer) p = p.getMinecraftPlayer();
    } catch (e1) {}
    try {
        if (p.getHandle) p = p.getHandle();
    } catch (e2) {}
    try {
        if (p.getMCEntity) p = p.getMCEntity();
    } catch (e3) {}
    if (isServerPlayer(p)) return p;
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

function normalizeUuid(raw) {
    if (raw == null) return null;
    var s = String(raw).trim().toLowerCase();
    if (!s || s === "null" || s === "undefined") return null;
    return s;
}

function getBukkitPlayer(sp) {
    try {
        var Bukkit = Java.loadClass("org.bukkit.Bukkit");
        var name = playerName(sp);
        if (name && name !== "?") {
            try {
                var byExact = Bukkit.getPlayerExact(name);
                if (byExact != null) return byExact;
            } catch (eExact) {}
            try {
                var byName = Bukkit.getPlayer(name);
                if (byName != null) return byName;
            } catch (eName) {}
        }
    } catch (err) {}
    return null;
}

function playerUuid(sp) {
    try {
        var gp = sp.getGameProfile();
        if (gp != null) {
            var gid = normalizeUuid(gp.getId());
            if (gid) return gid;
        }
    } catch (e0) {}
    try {
        var u0 = normalizeUuid(sp.getUUID());
        if (u0) return u0;
    } catch (e1) {}
    try {
        var u1 = normalizeUuid(sp.getStringUUID());
        if (u1) return u1;
    } catch (e2) {}
    try {
        if (sp.uuid) {
            var u2 = normalizeUuid(sp.uuid);
            if (u2) return u2;
        }
    } catch (e3) {}
    try {
        var bp = getBukkitPlayer(sp);
        if (bp != null) {
            var u3 = normalizeUuid(bp.getUniqueId());
            if (u3) return u3;
        }
    } catch (e4) {}
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

function fabledSkillLevel(bp) {
    if (bp == null) return 0;
    try {
        var Fabled = Java.loadClass("studio.magemonkey.fabled.Fabled");
        if (!Fabled.isLoaded()) return 0;
        var data = Fabled.getData(bp);
        if (data == null) return 0;
        var level = Number(data.getSkillLevel(SKILL_NAME));
        if (!isFinite(level) || level < 0) return 0;
        return level;
    } catch (err) {
        return 0;
    }
}

function buildingLevelFor(sp) {
    try {
        var bp = getBukkitPlayer(sp);
        var apiLevel = fabledSkillLevel(bp);
        if (apiLevel >= 1) return apiLevel;
        if (bp != null) {
            try {
                var bpUuid = normalizeUuid(bp.getUniqueId());
                var yamlFromBp = readBuildingLevelYaml(bpUuid);
                if (yamlFromBp >= 1) return yamlFromBp;
            } catch (eYamlBp) {}
        }
    } catch (err) {}

    var uuid = playerUuid(sp);
    var yamlLevel = readBuildingLevelYaml(uuid);
    if (yamlLevel >= 1) return yamlLevel;
    return 0;
}

function amountFromLevel(level) {
    if (!(level >= 1)) return 0;
    var amount = VALUE_BASE + (level - 1) * VALUE_SCALE;
    if (!isFinite(amount) || amount < 1) return 0;
    return Math.min(MAX_AWARD, Math.floor(amount));
}

function getStatsData(sp) {
    if (sp == null || !initJava()) return null;
    try {
        var lazy = StatsProviderCls.get(StatsCapabilityCls.INSTANCE, sp);
        if (lazy != null) {
            var data = lazy.orElse(null);
            if (data != null) return data;
        }
    } catch (e1) {
        if (DEBUG) {
            console.error("[BuildingTP] StatsProvider.get failed: " + e1);
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
        "[BuildingTP] skip " + playerName(sp) + " (" + id + "): " + reason
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
            "[BuildingTP] +" + amount + " TP -> " + playerName(sp) + " (silent)"
        );
    }
    return true;
}

BlockEvents.placed(function (event) {
    try {
        var sp = asServerPlayer(event.player);
        if (sp == null) {
            if (DEBUG && DEBUG_SKIPS) {
                console.info(
                    "[BuildingTP] BlockEvents.placed: could not unwrap player (" +
                        className(event.player) +
                        ")"
                );
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
    } catch (err) {
        console.error("[BuildingTP] BlockEvents.placed: " + err);
    }
});

console.info(
    "[BuildingTP] BlockEvents.placed backup ready (DEBUG=" + DEBUG + ")"
);
