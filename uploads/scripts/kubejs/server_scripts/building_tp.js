// kubejs/server_scripts/building_tp.js
// Silent Building TP — same award path as Farming TP Skill.js, no /dmzpoints.
//
// Fabled Building.yml: Block Place trigger + Value Set only (NO Command).
// Award: BuildingLevel TP per block placed (value-base/scale 1).
//
// Uses ServerPlayer.getCapability (KubeJS Rhino cannot call StatsProvider.get).
// Hooks: BlockEvents.placed + Forge EntityPlaceEvent (Mohist-safe).
//
// Reload: /kubejs reload server_scripts
// Also reload Fabled after Building.yml Command removal.

console.info("[BuildingTP] loading silent place awards...");

var SKILL_NAME = "Building";
var VALUE_BASE = 1.0;
var VALUE_SCALE = 1.0;
var MAX_AWARD = 10000;
var LOG_AWARDS = false;

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
        if (JAVA_READY) console.info("[BuildingTP] Java ready.");
    } catch (err) {
        console.error("[BuildingTP] Java init failed: " + err);
    }
    return JAVA_READY;
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
        if (ServerPlayerCls.class.isInstance(p)) return p;
    } catch (e3) {}
    try {
        if (p instanceof ServerPlayerCls) return p;
    } catch (e4) {}
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
    if (sp == null || !initJava()) return null;
    try {
        var lazy = sp.getCapability(StatsCapabilityCls.INSTANCE);
        if (lazy != null) {
            var data = lazy.orElse(null);
            if (data != null) return data;
        }
    } catch (e1) {}
    try {
        var lazy2 = sp.getCapability(StatsCapabilityCls.INSTANCE, null);
        if (lazy2 != null) {
            var data2 = lazy2.orElse(null);
            if (data2 != null) return data2;
        }
    } catch (e2) {}
    try {
        var StatsProvider = Java.loadClass(
            "com.dragonminez.common.stats.StatsProvider"
        );
        var Entity = Java.loadClass("net.minecraft.world.entity.Entity");
        var Capability = Java.loadClass(
            "net.minecraftforge.common.capabilities.Capability"
        );
        var m = StatsProvider.class.getMethod("get", Capability, Entity);
        var lazy3 = m.invoke(null, StatsCapabilityCls.INSTANCE, sp);
        if (lazy3 != null) {
            var data3 = lazy3.orElse(null);
            if (data3 != null) return data3;
        }
    } catch (e3) {}
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
    if (data == null) {
        console.error("[BuildingTP] StatsData missing for " + id);
        return false;
    }
    var resources = data.getResources();
    if (resources == null) return false;

    /*
     * Same as Farming TP Skill.js / PointsCommand award core:
     * setTrainingPoints — no /dmzpoints chat feedback.
     * (addTrainingPoints one-arg defaults shareWithParty=true; we mirror
     * Farming's one-arg call when available, else set.)
     */
    try {
        resources.addTrainingPoints(amount * 1.0);
    } catch (eAdd) {
        try {
            var cur = Number(resources.getTrainingPoints());
            if (!isFinite(cur) || cur < 0) cur = 0;
            resources.setTrainingPoints(cur + amount);
        } catch (eSet) {
            console.error("[BuildingTP] award failed: " + eSet);
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
    try {
        if (NetworkHandlerCls != null && StatsSyncS2CCls != null) {
            NetworkHandlerCls.sendToTrackingEntityAndSelf(
                new StatsSyncS2CCls(sp),
                sp
            );
        }
    } catch (eSync2) {}

    if (id) lastAwardTick[id] = tick;
    if (LOG_AWARDS) console.info("[BuildingTP] +" + amount + " TP -> " + id);
    return true;
}

function tryAward(entityOrPlayer, tag) {
    var sp = asServerPlayer(entityOrPlayer);
    if (sp == null && entityOrPlayer != null && initJava()) {
        try {
            if (ServerPlayerCls.class.isInstance(entityOrPlayer)) {
                sp = entityOrPlayer;
            }
        } catch (e) {}
    }
    if (sp == null) return;

    var level = buildingLevelFor(sp);
    var amount = amountFromLevel(level);
    if (amount < 1) return;
    addTrainingPointsSilent(sp, amount);
}

BlockEvents.placed(function (event) {
    try {
        tryAward(event.player, "BlockEvents.placed");
    } catch (err) {
        console.error("[BuildingTP] BlockEvents.placed: " + err);
    }
});

try {
    var EntityPlaceEvent = Java.loadClass(
        "net.minecraftforge.event.level.BlockEvent$EntityPlaceEvent"
    );
    NativeEvents.onEvent(EntityPlaceEvent, function (event) {
        try {
            var entity = null;
            try {
                entity = event.getEntity();
            } catch (e1) {}
            try {
                if (entity == null) entity = event.entity;
            } catch (e2) {}
            tryAward(entity, "EntityPlaceEvent");
        } catch (err) {
            console.error("[BuildingTP] EntityPlaceEvent: " + err);
        }
    });
    console.info("[BuildingTP] EntityPlaceEvent hooked");
} catch (eHook) {
    console.error("[BuildingTP] EntityPlaceEvent hook failed: " + eHook);
}

console.info(
    "[BuildingTP] ready — silent TP on place (Building >= 1), no dmzpoints"
);
