/*
 * Kick zapzxv2 when they look up (toward the sky).
 *
 * Pure ASCII only (KubeJS MalformedInputException on non-ASCII).
 * Pitch: 0 = horizon, negative = up, positive = down.
 * LOOK_UP_PITCH: kick when xRot <= this (e.g. -30 = looking upward).
 *
 *   /kubejs reload server_scripts
 */

var ENABLED = true;
var TARGET = "zapzxv2";
var KICK_MESSAGE = "the sky";
var LOOK_UP_PITCH = -30.0;
var CHECK_EVERY_TICKS = 2;
var COOLDOWN_TICKS = 40;

var tickAccum = {};
var cooldownUntil = {};
var debugEvery = 100;
var debugCount = {};

function playerName(player) {
    try {
        if (player.username) return String(player.username);
    } catch (e1) {}
    try {
        if (player.name && player.name.getString) {
            return String(player.name.getString());
        }
    } catch (e2) {}
    try {
        return String(player.name);
    } catch (e3) {}
    return "";
}

function unwrapPlayer(player) {
    try {
        if (player.minecraftPlayer) return player.minecraftPlayer;
    } catch (e1) {}
    try {
        if (player.minecraftEntity) return player.minecraftEntity;
    } catch (e2) {}
    return player;
}

function getPitch(player) {
    // Prefer KubeJS xRot / pitch wrappers
    try {
        if (player.xRot !== undefined && player.xRot !== null) {
            return Number(player.xRot);
        }
    } catch (e1) {}
    try {
        if (typeof player.pitch === "number") return player.pitch;
    } catch (e2) {}
    try {
        if (player.getPitch) return Number(player.getPitch());
    } catch (e3) {}
    // Raw MC entity
    var mc = unwrapPlayer(player);
    try {
        if (mc.getXRot) return Number(mc.getXRot());
    } catch (e4) {}
    try {
        if (mc.xRot !== undefined) return Number(mc.xRot);
    } catch (e5) {}
    return 0;
}

function doKick(player, server, name) {
    // Prefer disconnect/kick APIs; fall back to console kick command.
    try {
        if (typeof Component !== "undefined" && Component.literal) {
            player.kick(Component.literal(KICK_MESSAGE));
            return true;
        }
    } catch (e1) {}
    try {
        player.kick(KICK_MESSAGE);
        return true;
    } catch (e2) {}
    try {
        var mc = unwrapPlayer(player);
        if (mc.connection && mc.connection.disconnect) {
            if (typeof Component !== "undefined" && Component.literal) {
                mc.connection.disconnect(Component.literal(KICK_MESSAGE));
            } else {
                mc.connection.disconnect(KICK_MESSAGE);
            }
            return true;
        }
    } catch (e3) {}
    try {
        server.runCommandSilent("kick " + name + " " + KICK_MESSAGE);
        return true;
    } catch (e4) {
        console.error("[sky_kick] kick failed: " + e4);
        return false;
    }
}

PlayerEvents.tick(function (event) {
    if (!ENABLED) return;
    var player = event.player;
    var name = playerName(player);
    if (!name) return;
    if (name.toLowerCase() !== TARGET.toLowerCase()) return;

    var n = (tickAccum[name] || 0) + 1;
    tickAccum[name] = n;
    if (n % CHECK_EVERY_TICKS !== 0) return;

    var pitch = getPitch(player);

    // Occasional debug so we can see pitch in logs/kubejs/server.log
    var dc = (debugCount[name] || 0) + 1;
    debugCount[name] = dc;
    if (dc % debugEvery === 1) {
        console.info("[sky_kick] " + name + " pitch=" + pitch);
    }

    var now = n;
    if (cooldownUntil[name] && now < cooldownUntil[name]) return;

    if (pitch > LOOK_UP_PITCH) return;

    cooldownUntil[name] = now + COOLDOWN_TICKS;
    console.info(
        "[sky_kick] " + name + " looked up (pitch=" + pitch + ") - kicking"
    );
    doKick(player, event.server, name);
});

PlayerEvents.loggedIn(function (event) {
    if (!ENABLED) return;
    var name = playerName(event.player);
    if (name.toLowerCase() !== TARGET.toLowerCase()) return;
    console.info("[sky_kick] target online: " + name + " (look up to get kicked)");
});

console.info(
    "[sky_kick] loaded ENABLED=" +
        ENABLED +
        " TARGET=" +
        TARGET +
        " pitch<=" +
        LOOK_UP_PITCH +
        ' msg="' +
        KICK_MESSAGE +
        '"'
);
