// kubejs/server_scripts/silent_dmz_tp.js
// Building TP on block place — Farming-style award (silent, no /dmzpoints).
//
// Why not StatsProvider.get()?
//   KubeJS Rhino fails: get(Capability, boolean) — cannot resolve Entity overload.
//   Use entity.getCapability(StatsCapability.INSTANCE) instead (works).
//
// Formula: TP = BuildingLevel * 1  (same as Building.yml value-base/scale)
// Fabled Building.yml should NOT run a Command (Value Set only for skill XP).
//
// Reload: /kubejs reload server_scripts

console.info("[BuildingTP] loading (getCapability path)...");

var VALUE_BASE = 1.0;
var VALUE_SCALE = 1.0;
var SKILL_NAME = "Building";
var MAX_AWARD = 10000;
var DEBUG = true; /* temporarily verbose so we can confirm awards in kubejs log */

var lastAwardTick = {};

var ServerPlayerCls = null;
var StatsCapabilityCls = null;
var NetworkHandlerCls = null;
var ResourceSyncS2CCls = null;
var StatsSyncS2CCls = null;
var JAVA_READY = false;

function initJava() {
    if (JAVA_READY) return true;
    try {
        ServerPlayerCls = Java.loadClass(
            "net.minecraft.server.level.ServerPlayer"
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
            StatsSyncS2CCls = Java.loadClass(
                "com.dragonminez.common.network.S2C.StatsSyncS2C"
            );
        } catch (eNet) {}
        JAVA_READY = ServerPlayerCls != null && StatsCapabilityCls != null;
        if (JAVA_READY) {
            console.info("[BuildingTP] Java classes ready.");
        }
    } catch (err) {
        console.error("[BuildingTP] Java init failed: " + err);
        JAVA_READY = false;
    }
    return JAVA_READY;
}

function asServerPlayer(player) {
    if (!player) return null;
    if (!initJava()) return null;
    var p = player;
    try {
        if (p.minecraftPlayer) p = p.minecraftPlayer;
    } catch (e0) {}
    try {
        if (p.getMinecraftPlayer) p = p.getMinecraftPlayer();
    } catch (e1) {}
    try {
        if (p.self && p.self !== player) p = p.self;
    } catch (e2) {}
    try {
        if (p.getHandle) p = p.getHandle();
    } catch (e3) {}
    try {
        if (ServerPlayerCls.class.isInstance(p)) return p;
    } catch (e4) {}
    try {
        if (p instanceof ServerPlayerCls) return p;
    } catch (e5) {}
    return null;
}

function playerUuidString(player) {
    var sp = asServerPlayer(player);
    if (sp != null) {
        try {
            return String(sp.getUUID());
        } catch (e0) {}
        try {
            return String(sp.getStringUUID());
        } catch (e1) {}
    }
    try {
        if (player.uuid) return String(player.uuid);
    } catch (e2) {}
    return null;
}

function readBuildingLevelFromYaml(uuid) {
    if (!uuid) return 0;
    try {
        var Files = Java.loadClass("java.nio.file.Files");
        var Paths = Java.loadClass("java.nio.file.Paths");
        var path = Paths.get(
            "plugins/Fabled/players/" + String(uuid).toLowerCase() + ".yml"
        );
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
    } catch (err) {
        if (DEBUG) console.error("[BuildingTP] yaml read failed: " + err);
    }
    return 0;
}

function buildingLevelFor(player) {
    var uuid = playerUuidString(player);
    /* Prefer live Fabled API when Bukkit player is available. */
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
    } catch (err) {
        if (DEBUG) console.info("[BuildingTP] Fabled API miss: " + err);
    }
    return readBuildingLevelFromYaml(uuid);
}

function amountFromBuildingLevel(level) {
    if (!(level >= 1)) return 0;
    var amount = VALUE_BASE + (level - 1) * VALUE_SCALE;
    if (!isFinite(amount) || amount < 1) return 0;
    return Math.min(MAX_AWARD, Math.floor(amount));
}

/*
 * Farming uses StatsProvider.get(cap, entity) from CNPC Nashorn.
 * KubeJS Rhino cannot resolve that overload — use getCapability instead.
 */
function getStatsData(sp) {
    if (sp == null || !initJava()) return null;

    /* 1) Entity.getCapability(Capability) */
    try {
        var lazy = sp.getCapability(StatsCapabilityCls.INSTANCE);
        if (lazy != null) {
            var data = lazy.orElse(null);
            if (data != null) return data;
        }
    } catch (e1) {
        if (DEBUG) console.info("[BuildingTP] getCapability: " + e1);
    }

    /* 2) Entity.getCapability(Capability, Direction) with null side */
    try {
        var lazy2 = sp.getCapability(StatsCapabilityCls.INSTANCE, null);
        if (lazy2 != null) {
            var data2 = lazy2.orElse(null);
            if (data2 != null) return data2;
        }
    } catch (e2) {
        if (DEBUG) console.info("[BuildingTP] getCapability(null): " + e2);
    }

    /* 3) Reflect StatsProvider.get(Capability, Entity) — bypass Rhino overload */
    try {
        var StatsProvider = Java.loadClass(
            "com.dragonminez.common.stats.StatsProvider"
        );
        var Entity = Java.loadClass("net.minecraft.world.entity.Entity");
        var Capability = Java.loadClass(
            "net.minecraftforge.common.capabilities.Capability"
        );
        var m = StatsProvider.class.getMethod(
            "get",
            Capability,
            Entity
        );
        var lazy3 = m.invoke(null, StatsCapabilityCls.INSTANCE, sp);
        if (lazy3 != null) {
            var data3 = lazy3.orElse(null);
            if (data3 != null) return data3;
        }
    } catch (e3) {
        if (DEBUG) console.info("[BuildingTP] reflect StatsProvider.get: " + e3);
    }

    return null;
}

function addTrainingPointsSilent(player, amount) {
    if (!(amount > 0) || !isFinite(amount)) return false;
    amount = Math.min(MAX_AWARD, Math.floor(Number(amount)));
    if (amount < 1) return false;

    var sp = asServerPlayer(player);
    if (sp == null) {
        if (DEBUG) console.warn("[BuildingTP] no ServerPlayer for award");
        return false;
    }

    var tick = 0;
    try {
        tick = Number(sp.level.getGameTime());
    } catch (e0) {
        try {
            tick = Number(sp.getLevel().getGameTime());
        } catch (e1) {}
    }
    var id = playerUuidString(sp);
    if (id && lastAwardTick[id] === tick) {
        return false;
    }

    try {
        var data = getStatsData(sp);
        if (data == null) {
            console.error("[BuildingTP] StatsData missing for " + id);
            return false;
        }
        var resources = data.getResources();
        if (resources == null) return false;

        /* One-arg add — same as Farming TP Skill.js */
        try {
            resources.addTrainingPoints(amount * 1.0);
        } catch (addErr) {
            try {
                resources.addTrainingPoints(amount * 1.0, false);
            } catch (addErr2) {
                var cur = Number(resources.getTrainingPoints());
                if (!isFinite(cur) || cur < 0) cur = 0;
                resources.setTrainingPoints(cur + amount);
            }
        }

        try {
            if (NetworkHandlerCls != null && ResourceSyncS2CCls != null) {
                NetworkHandlerCls.sendToTrackingEntityAndSelf(
                    new ResourceSyncS2CCls(sp),
                    sp
                );
            }
        } catch (syncErr) {}
        try {
            if (NetworkHandlerCls != null && StatsSyncS2CCls != null) {
                NetworkHandlerCls.sendToTrackingEntityAndSelf(
                    new StatsSyncS2CCls(sp),
                    sp
                );
            }
        } catch (syncErr2) {}

        if (id) lastAwardTick[id] = tick;
        if (DEBUG) {
            console.info("[BuildingTP] +" + amount + " TP -> " + id);
        }
        return true;
    } catch (err) {
        console.error("[BuildingTP] add TP failed: " + err);
        return false;
    }
}

BlockEvents.placed(function (event) {
    try {
        var player = event.player;
        if (!player) return;
        try {
            if (player.level && player.level.clientSide) return;
        } catch (e0) {}

        var level = buildingLevelFor(player);
        var amount = amountFromBuildingLevel(level);
        if (amount < 1) {
            if (DEBUG) {
                console.info(
                    "[BuildingTP] skip place — Building Lv." + level
                );
            }
            return;
        }

        addTrainingPointsSilent(player, amount);
    } catch (err) {
        console.error("[BuildingTP] place handler: " + err);
    }
});

console.info(
    "[BuildingTP] ready — place blocks with Building >= 1 for silent TP"
);
