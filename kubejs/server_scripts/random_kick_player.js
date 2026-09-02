/*
 * Kick zapzxv2 when they look up (toward the sky).
 *
 * Pure ASCII only (KubeJS MalformedInputException on non-ASCII).
 *
 * Toggle (op / permission 2):
 *   /skykick on
 *   /skykick off
 *   /skykick toggle
 *   /skykick status
 * Chat fallback (op): !skykick on|off|toggle|status
 *
 *   /kubejs reload server_scripts
 */

var ENABLED = true; // runtime toggle; /skykick flips this
var TARGET = "zapzxv2";
var KICK_MESSAGE = "the sky";
var LOOK_UP_PITCH = -30.0;
var CHECK_EVERY_TICKS = 2;
var COOLDOWN_TICKS = 40;

var tickAccum = {};
var cooldownUntil = {};
var debugEvery = 100;
var debugCount = {};
var SKYKICK_REGISTERED = false;

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
    var mc = unwrapPlayer(player);
    try {
        if (mc.getXRot) return Number(mc.getXRot());
    } catch (e4) {}
    try {
        if (mc.xRot !== undefined) return Number(mc.xRot);
    } catch (e5) {}
    return 0;
}

function statusText() {
    return (
        "skykick " +
        (ENABLED ? "ON" : "OFF") +
        " target=" +
        TARGET +
        " pitch<=" +
        LOOK_UP_PITCH
    );
}

function setEnabled(on, source) {
    ENABLED = !!on;
    var msg = "[sky_kick] " + statusText();
    console.info(msg);
    try {
        if (source && source.tell) source.tell(msg);
    } catch (e1) {}
    try {
        if (source && source.sendSuccess) {
            if (typeof Component !== "undefined" && Component.literal) {
                source.sendSuccess(Component.literal(msg), true);
            }
        }
    } catch (e2) {}
    return 1;
}

function handleSub(sub, source) {
    sub = String(sub || "").toLowerCase();
    if (sub === "on" || sub === "enable" || sub === "true" || sub === "1") {
        return setEnabled(true, source);
    }
    if (sub === "off" || sub === "disable" || sub === "false" || sub === "0") {
        return setEnabled(false, source);
    }
    if (sub === "toggle" || sub === "t") {
        return setEnabled(!ENABLED, source);
    }
    if (sub === "status" || sub === "state" || sub === "") {
        try {
            if (source && source.tell) source.tell("[sky_kick] " + statusText());
        } catch (e1) {}
        try {
            if (source && source.sendSuccess && typeof Component !== "undefined") {
                source.sendSuccess(Component.literal("[sky_kick] " + statusText()), false);
            }
        } catch (e2) {}
        console.info("[sky_kick] " + statusText());
        return 1;
    }
    try {
        if (source && source.tell) {
            source.tell("[sky_kick] usage: /skykick on|off|toggle|status");
        }
    } catch (e3) {}
    return 0;
}

function doKick(player, server, name) {
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

function playerIsOp(player) {
    try {
        if (player.isOp && player.isOp()) return true;
    } catch (e1) {}
    try {
        if (player.op) return true;
    } catch (e2) {}
    try {
        if (player.hasPermissions && player.hasPermissions(2)) return true;
    } catch (e3) {}
    try {
        if (player.permissionLevel >= 2) return true;
    } catch (e4) {}
    return false;
}

function tell(player, msg) {
    try {
        player.tell(msg);
        return;
    } catch (e1) {}
    try {
        if (typeof Component !== "undefined" && Component.literal) {
            player.displayClientMessage(Component.literal(msg), false);
        }
    } catch (e2) {}
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

/* Brigadier via commandRegistry (full server start). */
ServerEvents.commandRegistry(function (event) {
    try {
        var Commands = event.commands;
        if (!Commands) return;
        event.register(
            Commands.literal("skykick")
                .requires(function (src) {
                    try {
                        return src.hasPermission(2);
                    } catch (e) {
                        return false;
                    }
                })
                .executes(function (ctx) {
                    return handleSub("status", ctx.source);
                })
                .then(
                    Commands.literal("on").executes(function (ctx) {
                        return handleSub("on", ctx.source);
                    })
                )
                .then(
                    Commands.literal("off").executes(function (ctx) {
                        return handleSub("off", ctx.source);
                    })
                )
                .then(
                    Commands.literal("toggle").executes(function (ctx) {
                        return handleSub("toggle", ctx.source);
                    })
                )
                .then(
                    Commands.literal("status").executes(function (ctx) {
                        return handleSub("status", ctx.source);
                    })
                )
        );
        SKYKICK_REGISTERED = true;
        console.info("[sky_kick] /skykick registered via commandRegistry");
    } catch (err) {
        console.error("[sky_kick] commandRegistry failed: " + err);
    }
});

/* Live Brigadier register so /kubejs reload picks up the command. */
function tryRegisterLive() {
    if (SKYKICK_REGISTERED) return true;
    try {
        var server = null;
        try {
            if (typeof Utils !== "undefined" && Utils.server) server = Utils.server;
        } catch (e0) {}
        try {
            if (!server && typeof Platform !== "undefined" && Platform.server) {
                server = Platform.server;
            }
        } catch (e1) {}
        if (!server) return false;
        var mc = server;
        try {
            if (server.minecraftServer) mc = server.minecraftServer;
        } catch (e2) {}
        var dispatcher = null;
        try {
            dispatcher = mc.getCommands().getDispatcher();
        } catch (e3) {
            try {
                dispatcher = mc.commands.dispatcher;
            } catch (e4) {}
        }
        if (!dispatcher) return false;

        var CommandsMc = Java.loadClass("net.minecraft.commands.Commands");
        var root = CommandsMc.literal("skykick").requires(function (src) {
            try {
                return src.hasPermission(2);
            } catch (e) {
                return false;
            }
        });
        root = root
            .executes(function (ctx) {
                return handleSub("status", ctx.getSource());
            })
            .then(
                CommandsMc.literal("on").executes(function (ctx) {
                    return handleSub("on", ctx.getSource());
                })
            )
            .then(
                CommandsMc.literal("off").executes(function (ctx) {
                    return handleSub("off", ctx.getSource());
                })
            )
            .then(
                CommandsMc.literal("toggle").executes(function (ctx) {
                    return handleSub("toggle", ctx.getSource());
                })
            )
            .then(
                CommandsMc.literal("status").executes(function (ctx) {
                    return handleSub("status", ctx.getSource());
                })
            );
        dispatcher.register(root);
        SKYKICK_REGISTERED = true;
        console.info("[sky_kick] /skykick registered live on dispatcher");
        return true;
    } catch (err) {
        console.error("[sky_kick] live register failed: " + err);
        return false;
    }
}

ServerEvents.loaded(function () {
    tryRegisterLive();
});
tryRegisterLive();

/* Mohist chat fallback: !skykick on|off|toggle|status */
PlayerEvents.chat(function (event) {
    var msg = "";
    try {
        msg = String(event.message || "");
    } catch (e1) {
        return;
    }
    if (msg.length < 8) return;
    if (msg.charAt(0) !== "!") return;
    var lower = msg.toLowerCase();
    if (lower.indexOf("!skykick") !== 0) return;
    event.cancel();
    var player = event.player;
    if (!playerIsOp(player)) {
        tell(player, "[sky_kick] op only");
        return;
    }
    var parts = msg.substring(1).trim().split(/\s+/);
    var sub = parts.length > 1 ? parts[1] : "status";
    handleSub(sub, {
        tell: function (m) {
            tell(player, m);
        },
        sendSuccess: function () {}
    });
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
