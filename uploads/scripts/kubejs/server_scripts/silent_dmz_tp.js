// kubejs/server_scripts/silent_dmz_tp.js
// Building TP is awarded by Fabled Building.yml Command (same as Farming.yml):
//   dmzpoints add {TPB} {player}
//
// This file only keeps a silent Forge place backup using getCapability
// (StatsProvider.get is broken under KubeJS Rhino).
// Set ENABLE_SILENT_PLACE_BACKUP = true only if you remove the Fabled Command
// to avoid double TP.

console.info("[BuildingTP] loading...");

/* false while Fabled runs dmzpoints (avoids double TP). Set true if Command is removed. */
var ENABLE_SILENT_PLACE_BACKUP = false;
var VALUE_BASE = 1.0;
var VALUE_SCALE = 1.0;
var SKILL_NAME = "Building";
var MAX_AWARD = 10000;
var LOG_AWARDS = true;

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
        ServerPlayerCls = Java.loadClass("net.minecraft.server.level.ServerPlayer");
        StatsCapabilityCls = Java.loadClass("com.dragonminez.common.stats.StatsCapability");
        try {
            NetworkHandlerCls = Java.loadClass("com.dragonminez.common.network.NetworkHandler");
            ResourceSyncS2CCls = Java.loadClass("com.dragonminez.common.network.S2C.ResourceSyncS2C");
            StatsSyncS2CCls = Java.loadClass("com.dragonminez.common.network.S2C.StatsSyncS2C");
        } catch (eNet) {}
        JAVA_READY = ServerPlayerCls != null && StatsCapabilityCls != null;
    } catch (err) {
        console.error("[BuildingTP] Java init failed: " + err);
    }
    return JAVA_READY;
}

function asServerPlayer(player) {
    if (!player || !initJava()) return null;
    var p = player;
    try { if (p.minecraftPlayer) p = p.minecraftPlayer; } catch (e0) {}
    try { if (p.getMinecraftPlayer) p = p.getMinecraftPlayer(); } catch (e1) {}
    try { if (p.getHandle) p = p.getHandle(); } catch (e2) {}
    try { if (ServerPlayerCls.class.isInstance(p)) return p; } catch (e3) {}
    try { if (p instanceof ServerPlayerCls) return p; } catch (e4) {}
    return null;
}

function playerUuidString(spOrPlayer) {
    var sp = asServerPlayer(spOrPlayer) || spOrPlayer;
    try { return String(sp.getUUID()); } catch (e0) {}
    try { return String(sp.getStringUUID()); } catch (e1) {}
    try { if (spOrPlayer.uuid) return String(spOrPlayer.uuid); } catch (e2) {}
    return null;
}

function readBuildingLevelFromYaml(uuid) {
    if (!uuid) return 0;
    try {
        var Files = Java.loadClass("java.nio.file.Files");
        var Paths = Java.loadClass("java.nio.file.Paths");
        var path = Paths.get("plugins/Fabled/players/" + String(uuid).toLowerCase() + ".yml");
        if (!Files.exists(path)) return 0;
        var lines = Files.readAllLines(path);
        var found = false;
        for (var i = 0; i < lines.size(); i++) {
            var trimmed = String(lines.get(i)).trim();
            if (trimmed === SKILL_NAME + ":") { found = true; continue; }
            if (found && trimmed.indexOf("level:") === 0) {
                var level = parseInt(trimmed.replace("level:", "").trim(), 10);
                return isNaN(level) ? 0 : Math.max(0, level);
            }
            if (found && trimmed.length > 0 && trimmed.charAt(trimmed.length - 1) === ":" && trimmed.indexOf("level:") !== 0) break;
        }
    } catch (err) {}
    return 0;
}

function buildingLevelFor(playerOrEntity) {
    var uuid = playerUuidString(playerOrEntity);
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
                        var level = Math.max(0, Number(data.getSkillLevel(SKILL_NAME)) || 0);
                        if (level >= 1) return level;
                    }
                }
            }
        }
    } catch (err) {}
    return readBuildingLevelFromYaml(uuid);
}

function amountFromBuildingLevel(level) {
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
        var StatsProvider = Java.loadClass("com.dragonminez.common.stats.StatsProvider");
        var Entity = Java.loadClass("net.minecraft.world.entity.Entity");
        var Capability = Java.loadClass("net.minecraftforge.common.capabilities.Capability");
        var m = StatsProvider.class.getMethod("get", Capability, Entity);
        var lazy3 = m.invoke(null, StatsCapabilityCls.INSTANCE, sp);
        if (lazy3 != null) {
            var data3 = lazy3.orElse(null);
            if (data3 != null) return data3;
        }
    } catch (e3) {}
    return null;
}

function addTrainingPointsSilent(sp, amount, sourceTag) {
    if (!(amount > 0) || !isFinite(amount) || sp == null) return false;
    amount = Math.min(MAX_AWARD, Math.floor(Number(amount)));
    if (amount < 1) return false;

    var tick = 0;
    try { tick = Number(sp.level.getGameTime()); } catch (e0) {
        try { tick = Number(sp.getLevel().getGameTime()); } catch (e1) {}
    }
    var id = playerUuidString(sp);
    if (id && lastAwardTick[id] === tick) return false;

    try {
        var data = getStatsData(sp);
        if (data == null) {
            console.error("[BuildingTP] StatsData missing (" + sourceTag + ") for " + id);
            return false;
        }
        var resources = data.getResources();
        if (resources == null) return false;
        try {
            resources.addTrainingPoints(amount * 1.0);
        } catch (addErr) {
            try { resources.addTrainingPoints(amount * 1.0, false); }
            catch (addErr2) {
                var cur = Number(resources.getTrainingPoints());
                if (!isFinite(cur) || cur < 0) cur = 0;
                resources.setTrainingPoints(cur + amount);
            }
        }
        try {
            if (NetworkHandlerCls != null && ResourceSyncS2CCls != null) {
                NetworkHandlerCls.sendToTrackingEntityAndSelf(new ResourceSyncS2CCls(sp), sp);
            }
        } catch (syncErr) {}
        try {
            if (NetworkHandlerCls != null && StatsSyncS2CCls != null) {
                NetworkHandlerCls.sendToTrackingEntityAndSelf(new StatsSyncS2CCls(sp), sp);
            }
        } catch (syncErr2) {}
        if (id) lastAwardTick[id] = tick;
        if (LOG_AWARDS) console.info("[BuildingTP] +" + amount + " TP (" + sourceTag + ") -> " + id);
        return true;
    } catch (err) {
        console.error("[BuildingTP] add TP failed (" + sourceTag + "): " + err);
        return false;
    }
}

function tryAwardFromEntity(entity, sourceTag) {
    if (!ENABLE_SILENT_PLACE_BACKUP) return;
    if (entity == null) return;
    var sp = asServerPlayer(entity);
    if (sp == null) {
        /* entity may already be ServerPlayer */
        try {
            if (initJava() && ServerPlayerCls.class.isInstance(entity)) sp = entity;
        } catch (e) {}
    }
    if (sp == null) {
        console.info("[BuildingTP] " + sourceTag + ": not a ServerPlayer");
        return;
    }
    var level = buildingLevelFor(sp);
    var amount = amountFromBuildingLevel(level);
    console.info("[BuildingTP] " + sourceTag + " Building Lv." + level + " amount=" + amount);
    if (amount < 1) return;
    addTrainingPointsSilent(sp, amount, sourceTag);
}

/* KubeJS block place (may not fire on all Mohist paths). */
BlockEvents.placed(function (event) {
    try {
        tryAwardFromEntity(event.player, "BlockEvents.placed");
    } catch (err) {
        console.error("[BuildingTP] BlockEvents.placed: " + err);
    }
});

/* Forge EntityPlaceEvent — more reliable on Mohist. */
try {
    var EntityPlaceEvent = Java.loadClass(
        "net.minecraftforge.event.level.BlockEvent$EntityPlaceEvent"
    );
    NativeEvents.onEvent(EntityPlaceEvent, function (event) {
        try {
            var entity = null;
            try { entity = event.getEntity(); } catch (e1) {}
            try { if (entity == null) entity = event.entity; } catch (e2) {}
            tryAwardFromEntity(entity, "EntityPlaceEvent");
        } catch (err) {
            console.error("[BuildingTP] EntityPlaceEvent: " + err);
        }
    });
    console.info("[BuildingTP] NativeEvents EntityPlaceEvent hooked");
} catch (eHook) {
    console.error("[BuildingTP] NativeEvents hook failed: " + eHook);
}

console.info(
    "[BuildingTP] ready — silent backup=" +
        ENABLE_SILENT_PLACE_BACKUP +
        "; Fabled should also run: dmzpoints add {TPB} {player}"
);
