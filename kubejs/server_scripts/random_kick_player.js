/*
 * Joke punishments (pure ASCII - KubeJS MalformedInputException on non-ASCII).
 *
 * 1) Sky kick: zapzxv2 looking up gets kicked with "the sky"
 *    /skykick on|off|toggle|status
 *    !skykick on|off|toggle|status
 *
 * 2) Chat 67: any player saying 67 / variations gets smited (lightning)
 *    /smite67 on|off|toggle|status
 *    !smite67 on|off|toggle|status
 *
 * Op / permission 2 for toggles.
 *   /kubejs reload server_scripts
 */

var SKY_ENABLED = true;
var SMITE67_ENABLED = true;
try {
    global.smite67Enabled = SMITE67_ENABLED;
} catch (eGlobal0) {}

var TARGET = "zapzxv2";
var KICK_MESSAGE = "the sky";
var LOOK_UP_PITCH = -30.0;
var CHECK_EVERY_TICKS = 2;
var COOLDOWN_TICKS = 40;

var SMITE_COOLDOWN_TICKS = 40;
var SMITE_MESSAGE = "Thou shalt not speak of 67.";

var tickAccum = {};
var cooldownUntil = {};
var debugEvery = 100;
var debugCount = {};
var smiteCooldownUntil = {};
var SKYKICK_REGISTERED = false;
var SMITE67_REGISTERED = false;

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

function replySource(source, msg) {
    try {
        if (source && source.tell) source.tell(msg);
    } catch (e1) {}
    try {
        if (source && source.sendSuccess && typeof Component !== "undefined") {
            source.sendSuccess(Component.literal(msg), true);
        }
    } catch (e2) {}
}

function skyStatus() {
    return (
        "skykick " +
        (SKY_ENABLED ? "ON" : "OFF") +
        " target=" +
        TARGET +
        " pitch<=" +
        LOOK_UP_PITCH
    );
}

function smite67Status() {
    return "smite67 " + (SMITE67_ENABLED ? "ON" : "OFF");
}

function handleSkySub(sub, source) {
    sub = String(sub || "").toLowerCase();
    if (sub === "on" || sub === "enable" || sub === "true" || sub === "1") {
        SKY_ENABLED = true;
    } else if (sub === "off" || sub === "disable" || sub === "false" || sub === "0") {
        SKY_ENABLED = false;
    } else if (sub === "toggle" || sub === "t") {
        SKY_ENABLED = !SKY_ENABLED;
    } else if (sub === "status" || sub === "state" || sub === "") {
        replySource(source, "[sky_kick] " + skyStatus());
        console.info("[sky_kick] " + skyStatus());
        return 1;
    } else {
        replySource(source, "[sky_kick] usage: /skykick on|off|toggle|status");
        return 0;
    }
    replySource(source, "[sky_kick] " + skyStatus());
    console.info("[sky_kick] " + skyStatus());
    return 1;
}

function syncSmite67Global() {
    try {
        global.smite67Enabled = SMITE67_ENABLED;
    } catch (e1) {}
}

function handleSmite67Sub(sub, source) {
    sub = String(sub || "").toLowerCase();
    if (sub === "on" || sub === "enable" || sub === "true" || sub === "1") {
        SMITE67_ENABLED = true;
    } else if (sub === "off" || sub === "disable" || sub === "false" || sub === "0") {
        SMITE67_ENABLED = false;
    } else if (sub === "toggle" || sub === "t") {
        SMITE67_ENABLED = !SMITE67_ENABLED;
    } else if (sub === "status" || sub === "state" || sub === "") {
        syncSmite67Global();
        replySource(source, "[smite67] " + smite67Status());
        console.info("[smite67] " + smite67Status());
        return 1;
    } else {
        replySource(source, "[smite67] usage: /smite67 on|off|toggle|status");
        return 0;
    }
    syncSmite67Global();
    replySource(source, "[smite67] " + smite67Status());
    console.info("[smite67] " + smite67Status());
    return 1;
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

function contains67(msg) {
    if (!msg) return false;
    var s = String(msg).toLowerCase();
    // words: sixty-seven / sixty seven / sixtyseven
    if (s.indexOf("sixtyseven") >= 0) return true;
    if (/sixty[\s\-_]*seven/.test(s)) return true;
    // roman numeral
    if (/\blxvii\b/.test(s)) return true;
    // digit forms: 67, 6 7, 6-7, 6_7, 6.7 (meme variants)
    // avoid matching longer numbers like 167 or 670 by checking neighbors
    if (/(^|[^0-9])6[\s\-_./\\]*7([^0-9]|$)/.test(s)) return true;
    // spaced letters sometimes used: s i x t y   s e v e n - skip
    // leet: 6even with 7? keep simple
    if (/\b6even\b/.test(s)) return true;
    return false;
}

function smitePlayerNow(player, server) {
    var name = playerName(player);
    var ok = false;
    try {
        server.runCommandSilent(
            "execute as " +
                name +
                " at @s run summon minecraft:lightning_bolt ~ ~ ~"
        );
        ok = true;
    } catch (e1) {
        console.error("[smite67] summon failed: " + e1);
    }
    try {
        server.runCommand(
            "execute as " +
                name +
                " at @s run summon minecraft:lightning_bolt ~ ~ ~"
        );
        ok = true;
    } catch (e1b) {}
    try {
        if (player.setSecondsOnFire) player.setSecondsOnFire(3);
    } catch (e5) {}
    try {
        tell(player, SMITE_MESSAGE);
    } catch (e6) {}
    console.info("[smite67] smote " + name);
    return ok;
}

function smitePlayer(player, server) {
    try {
        if (server && server.scheduleInTicks) {
            server.scheduleInTicks(1, function () {
                smitePlayerNow(player, server);
            });
            return true;
        }
    } catch (e0) {}
    return smitePlayerNow(player, server);
}

PlayerEvents.tick(function (event) {
    if (!SKY_ENABLED) return;
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
    if (!SKY_ENABLED) return;
    var name = playerName(event.player);
    if (name.toLowerCase() !== TARGET.toLowerCase()) return;
    console.info("[sky_kick] target online: " + name + " (look up to get kicked)");
});

function registerLiteralToggle(Commands, name, handler) {
    return Commands.literal(name)
        .requires(function (src) {
            try {
                return src.hasPermission(2);
            } catch (e) {
                return false;
            }
        })
        .executes(function (ctx) {
            return handler("status", ctx.source);
        })
        .then(
            Commands.literal("on").executes(function (ctx) {
                return handler("on", ctx.source);
            })
        )
        .then(
            Commands.literal("off").executes(function (ctx) {
                return handler("off", ctx.source);
            })
        )
        .then(
            Commands.literal("toggle").executes(function (ctx) {
                return handler("toggle", ctx.source);
            })
        )
        .then(
            Commands.literal("status").executes(function (ctx) {
                return handler("status", ctx.source);
            })
        );
}

ServerEvents.commandRegistry(function (event) {
    try {
        var Commands = event.commands;
        if (!Commands) return;
        event.register(registerLiteralToggle(Commands, "skykick", handleSkySub));
        SKYKICK_REGISTERED = true;
        event.register(registerLiteralToggle(Commands, "smite67", handleSmite67Sub));
        SMITE67_REGISTERED = true;
        console.info("[sky_kick] /skykick and /smite67 registered via commandRegistry");
    } catch (err) {
        console.error("[sky_kick] commandRegistry failed: " + err);
    }
});

function tryRegisterLiveOne(cmdName, handler, flagName) {
    try {
        var already =
            flagName === "sky" ? SKYKICK_REGISTERED : SMITE67_REGISTERED;
        if (already) return true;
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
        var root = CommandsMc.literal(cmdName).requires(function (src) {
            try {
                return src.hasPermission(2);
            } catch (e) {
                return false;
            }
        });
        root = root
            .executes(function (ctx) {
                return handler("status", ctx.getSource());
            })
            .then(
                CommandsMc.literal("on").executes(function (ctx) {
                    return handler("on", ctx.getSource());
                })
            )
            .then(
                CommandsMc.literal("off").executes(function (ctx) {
                    return handler("off", ctx.getSource());
                })
            )
            .then(
                CommandsMc.literal("toggle").executes(function (ctx) {
                    return handler("toggle", ctx.getSource());
                })
            )
            .then(
                CommandsMc.literal("status").executes(function (ctx) {
                    return handler("status", ctx.getSource());
                })
            );
        dispatcher.register(root);
        if (flagName === "sky") SKYKICK_REGISTERED = true;
        else SMITE67_REGISTERED = true;
        console.info("[sky_kick] /" + cmdName + " registered live");
        return true;
    } catch (err) {
        console.error("[sky_kick] live register /" + cmdName + " failed: " + err);
        return false;
    }
}

function tryRegisterLive() {
    tryRegisterLiveOne("skykick", handleSkySub, "sky");
    tryRegisterLiveOne("smite67", handleSmite67Sub, "smite");
}

tryRegisterLive();

var BUKKIT_CHAT_REGISTERED = false;

function findBukkitPlugin() {
    try {
        var Bukkit = Java.loadClass("org.bukkit.Bukkit");
        var pm = Bukkit.getPluginManager();
        var names = [
            "KubeJS",
            "kubejs",
            "Mohist",
            "CMI",
            "PlaceholderAPI",
            "LuckPerms",
            "WorldEdit"
        ];
        for (var i = 0; i < names.length; i++) {
            try {
                var p = pm.getPlugin(names[i]);
                if (p != null) return p;
            } catch (e1) {}
        }
        var all = pm.getPlugins();
        if (all != null && all.length > 0) return all[0];
    } catch (err) {
        console.error("[smite67] Bukkit plugin lookup failed: " + err);
    }
    return null;
}

function smiteByNameConsole(name) {
    try {
        var Bukkit = Java.loadClass("org.bukkit.Bukkit");
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(),
            "execute as " +
                name +
                " at @s run summon minecraft:lightning_bolt ~ ~ ~"
        );
        try {
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "execute as " + name + " at @s run data merge entity @s {Fire:60s}"
            );
        } catch (eFire) {}
        console.info("[smite67] smote " + name + " (Bukkit chat)");
        return true;
    } catch (err) {
        console.error("[smite67] Bukkit smite failed: " + err);
        return false;
    }
}

function markSmiteCd(name) {
    try {
        if (!global.smite67Cd) global.smite67Cd = {};
        var key = String(name).toLowerCase();
        var now = new Date().getTime();
        var last = global.smite67Cd[key] || 0;
        if (now - last < 2000) return false;
        global.smite67Cd[key] = now;
        return true;
    } catch (e1) {
        return true;
    }
}

function registerBukkitChatSmite() {
    if (BUKKIT_CHAT_REGISTERED) return true;
    if (!SMITE67_ENABLED && false) {
        // still register; enabled checked at event time
    }
    try {
        var Bukkit = Java.loadClass("org.bukkit.Bukkit");
        var EventPriority = Java.loadClass("org.bukkit.event.EventPriority");
        var Listener = Java.loadClass("org.bukkit.event.Listener");
        var EventExecutor = Java.loadClass("org.bukkit.plugin.EventExecutor");
        var Runnable = Java.loadClass("java.lang.Runnable");
        var AsyncPlayerChatEvent = Java.loadClass(
            "org.bukkit.event.player.AsyncPlayerChatEvent"
        );
        var plugin = findBukkitPlugin();
        if (plugin == null) {
            console.error("[smite67] no Bukkit plugin found to register chat listener");
            return false;
        }

        var listener = new JavaAdapter(Listener, {});
        var executor = new JavaAdapter(EventExecutor, {
            execute: function (l, event) {
                try {
                    if (!SMITE67_ENABLED) return;
                    try {
                        if (global.smite67Enabled === false) return;
                    } catch (eG) {}
                    var msg = "";
                    try {
                        msg = String(event.getMessage());
                    } catch (eM) {
                        return;
                    }
                    if (!contains67(msg)) return;
                    var bp = event.getPlayer();
                    var name = String(bp.getName());
                    if (!markSmiteCd(name)) return;
                    console.info("[smite67] " + name + " said (bukkit): " + msg);
                    var task = new JavaAdapter(Runnable, {
                        run: function () {
                            smiteByNameConsole(name);
                        }
                    });
                    Bukkit.getScheduler().runTask(plugin, task);
                } catch (err) {
                    console.error("[smite67] Bukkit chat handler error: " + err);
                }
            }
        });

        Bukkit.getPluginManager().registerEvent(
            AsyncPlayerChatEvent,
            listener,
            EventPriority.MONITOR,
            executor,
            plugin,
            false
        );
        BUKKIT_CHAT_REGISTERED = true;
        console.info(
            "[smite67] Bukkit AsyncPlayerChatEvent registered via plugin=" +
                plugin.getName()
        );
        return true;
    } catch (err) {
        console.error("[smite67] Bukkit chat register failed: " + err);
        return false;
    }
}

ServerEvents.loaded(function () {
    tryRegisterLive();
    registerBukkitChatSmite();
});

/* Also try immediately after reload when server already running. */
try {
    registerBukkitChatSmite();
} catch (eBoot) {}


function extractChatText(event) {
    try {
        var m = event.message;
        if (m && m.getString) return String(m.getString());
    } catch (e1) {}
    try {
        if (event.getMessage) {
            var g = event.getMessage();
            if (g && g.getString) return String(g.getString());
            if (g != null) return String(g);
        }
    } catch (e2) {}
    try {
        if (event.message != null) return String(event.message);
    } catch (e3) {}
    try {
        if (event.component && event.component.getString) {
            return String(event.component.getString());
        }
    } catch (e4) {}
    return "";
}

PlayerEvents.chat(function (event) {
    var msg = extractChatText(event);
    if (!msg) return;

    // Op toggles via chat fallback
    if (msg.charAt(0) === "!") {
        var lower = msg.toLowerCase();
        var player = event.player;
        if (lower.indexOf("!skykick") === 0) {
            event.cancel();
            if (!playerIsOp(player)) {
                tell(player, "[sky_kick] op only");
                return;
            }
            var parts = msg.substring(1).trim().split(/\s+/);
            handleSkySub(parts.length > 1 ? parts[1] : "status", {
                tell: function (m) {
                    tell(player, m);
                }
            });
            return;
        }
        if (lower.indexOf("!smite67") === 0) {
            event.cancel();
            if (!playerIsOp(player)) {
                tell(player, "[smite67] op only");
                return;
            }
            var parts2 = msg.substring(1).trim().split(/\s+/);
            handleSmite67Sub(parts2.length > 1 ? parts2[1] : "status", {
                tell: function (m) {
                    tell(player, m);
                }
            });
            return;
        }
    }

    if (!SMITE67_ENABLED) return;
    if (!contains67(msg)) return;

    var p = event.player;
    var n = playerName(p);
    var tick = 0;
    try {
        tick = Number(p.level.time) || 0;
    } catch (e2) {
        tick = (tickAccum[n] || 0) + 1;
    }
    if (smiteCooldownUntil[n] && tick < smiteCooldownUntil[n]) return;
    smiteCooldownUntil[n] = tick + SMITE_COOLDOWN_TICKS;
    if (!markSmiteCd(n)) return;

    console.info("[smite67] " + n + " said: " + msg);
    smitePlayer(p, event.server);
});

syncSmite67Global();
console.info(
    "[sky_kick] loaded " +
        skyStatus() +
        " | " +
        smite67Status() +
        ' msg="' +
        KICK_MESSAGE +
        '"'
);
