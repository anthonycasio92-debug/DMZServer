/*
 * Kick zapzxv2 when they look up (toward the sky).
 *
 * Pitch in Minecraft: 0 = horizon, negative = up, positive = down.
 * LOOK_UP_PITCH: kick when pitch <= this (e.g. -45 = looking well upward).
 *
 *   /kubejs reload server_scripts
 */

var ENABLED = true;
var TARGET = "zapzxv2";
var KICK_MESSAGE = "the sky";
var LOOK_UP_PITCH = -45.0; // degrees; more negative = must look higher
var CHECK_EVERY_TICKS = 5; // throttle (player ticks)
var COOLDOWN_TICKS = 40; // avoid double-fire if kick is slow

var tickAccum = {};
var cooldownUntil = {};

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

function getPitch(player) {
    try {
        if (typeof player.pitch === "number") return player.pitch;
    } catch (e1) {}
    try {
        if (player.getXRot) return player.getXRot();
    } catch (e2) {}
    try {
        if (player.xRot !== undefined) return Number(player.xRot);
    } catch (e3) {}
    return 0;
}

PlayerEvents.tick(function (event) {
    if (!ENABLED) return;
    var player = event.player;
    var name = playerName(player);
    if (name !== TARGET) return;

    var n = (tickAccum[name] || 0) + 1;
    tickAccum[name] = n;
    if (n % CHECK_EVERY_TICKS !== 0) return;

    var now = player.level && player.level.time !== undefined ? Number(player.level.time) : n;
    if (cooldownUntil[name] && now < cooldownUntil[name]) return;

    var pitch = getPitch(player);
    if (pitch > LOOK_UP_PITCH) return;

    cooldownUntil[name] = now + COOLDOWN_TICKS;
    console.info(
        "[sky_kick] " + name + " looked up (pitch=" + pitch.toFixed(1) + ") — kicking"
    );
    try {
        player.kick(KICK_MESSAGE);
    } catch (e1) {
        try {
            event.server.runCommandSilent("kick " + name + " " + KICK_MESSAGE);
        } catch (e2) {
            console.error("[sky_kick] kick failed: " + e2);
        }
    }
});

console.info(
    "[sky_kick] loaded — ENABLED=" +
        ENABLED +
        " TARGET=" +
        TARGET +
        " pitch<=" +
        LOOK_UP_PITCH +
        ' msg="' +
        KICK_MESSAGE +
        '"'
);
