/*
 * DBZ Legacy Reborn - Console / Saga Android Conversion
 *
 * Pure ASCII so KubeJS UTF-8 reader never hits MalformedInputException.
 *
 * Dr. Gero NPC path (NPCActionC2S.handleGero) is blocked by melee-fix.
 * This command applies the SAME DragonMineZ 2.1.3 conversion directly:
 *   1. setAndroidUpgraded(true)
 *   2. setSkillLevel("androidforms", 1)
 *   3. removeSkill("superforms") + removeSkill("legendaryforms")
 *   4. updateTransformationSkillLimits(race)
 *   5. select + activate androidforms.androidbase
 *   6. clearActiveStackForm + refresh + sync
 *
 * Console / Saga / Fabled (run as console):
 *   androidify <playerName>
 *   androidify <playerName> force
 *   androidification <playerName>
 *
 * In-game (op):
 *   /androidify <player>
 *   /androidify me
 *   /androidify <player> force
 *
 * Reload: /kubejs reload server_scripts
 * Notes: kubejs/ANDROIDIFY.md
 */

console.info("[Androidify] script file evaluating...");

var ANDROID_FORM_GROUP = "androidforms";
var ANDROID_BASE_FORM = "androidbase";
var ANDROID_FORM_SKILL_LEVEL = 1;
var REMOVE_SKILLS = ["superforms", "legendaryforms"];
var REQUIRE_ALLOWED_RACE = true;
var ALLOWED_RACES = ["human"];
var BLOCKED_RACES = ["bioandroid"];

var StatsProvider = null;
var StatsCapability = null;
var StatsSyncS2C = null;
var NetworkHandler = null;
var ConfigManager = null;
var DMZ_READY = false;

function initDmz() {
    if (DMZ_READY) return true;
    try {
        StatsProvider = Java.loadClass("com.dragonminez.common.stats.StatsProvider");
        StatsCapability = Java.loadClass(
            "com.dragonminez.common.stats.StatsCapability"
        );
        StatsSyncS2C = Java.loadClass(
            "com.dragonminez.common.network.S2C.StatsSyncS2C"
        );
        NetworkHandler = Java.loadClass(
            "com.dragonminez.common.network.NetworkHandler"
        );
        try {
            ConfigManager = Java.loadClass(
                "com.dragonminez.common.config.ConfigManager"
            );
        } catch (eCfg) {
            ConfigManager = null;
        }
        DMZ_READY = true;
        console.info("[Androidify] DragonMineZ classes ready");
        return true;
    } catch (err) {
        console.error("[Androidify] DMZ class load failed: " + err);
        return false;
    }
}

function str(v) {
    if (v == null) return "";
    return String(v);
}

function lower(v) {
    return str(v).toLowerCase();
}

function tellPlayer(player, msg) {
    try {
        if (player.tell) {
            player.tell(msg);
            return;
        }
    } catch (e1) {}
    try {
        if (player.displayClientMessage) {
            player.displayClientMessage(Text.of(msg), false);
            return;
        }
    } catch (e2) {}
    try {
        player.sendSystemMessage(Text.of(msg));
    } catch (e3) {}
}

function cmdFeedback(src, msg, failure) {
    try {
        if (failure && src.sendFailure) {
            src.sendFailure(Text.of(msg));
            return;
        }
        if (!failure && src.sendSuccess) {
            src.sendSuccess(Text.of(msg), true);
            return;
        }
    } catch (e1) {}
    try {
        if (src.player) tellPlayer(src.player, (failure ? "\u00A7c" : "\u00A7a") + msg);
    } catch (e2) {}
    console.info("[Androidify] " + msg);
}

function playerName(player) {
    try {
        if (player.username) return String(player.username);
    } catch (e1) {}
    try {
        if (player.getGameProfile) {
            return String(player.getGameProfile().getName());
        }
    } catch (e2) {}
    try {
        if (player.name && player.name.getString) {
            return String(player.name.getString());
        }
    } catch (e3) {}
    try {
        return String(player.name);
    } catch (e4) {}
    return "";
}

function asMcPlayer(player) {
    if (player == null) return null;
    try {
        if (player.minecraftPlayer) return player.minecraftPlayer;
    } catch (e1) {}
    try {
        if (player.getMcPlayer) return player.getMcPlayer();
    } catch (e2) {}
    return player;
}

function getLiveServer() {
    try {
        if (typeof Utils !== "undefined" && Utils.server) return Utils.server;
    } catch (e1) {}
    try {
        if (typeof Platform !== "undefined" && Platform.server) {
            return Platform.server;
        }
    } catch (e2) {}
    return null;
}

function findOnlinePlayer(server, name) {
    if (!server || !name) return null;
    var want = lower(name);
    try {
        var list = server.players;
        for (var i = 0; i < list.length; i++) {
            if (lower(playerName(list[i])) === want) return list[i];
        }
    } catch (e1) {}
    try {
        var pl = server.getPlayerList().getPlayerByName(name);
        if (pl != null) return pl;
    } catch (e2) {}
    return null;
}

function getDmzData(mcPlayer) {
    if (!initDmz()) return null;
    try {
        return StatsProvider.get(StatsCapability.INSTANCE, mcPlayer).orElse(null);
    } catch (e) {
        return null;
    }
}

function getRaceName(character) {
    try {
        var r = character.getRaceName();
        if (r) return str(r);
    } catch (e1) {}
    try {
        var r2 = character.getRace();
        if (r2) return str(r2);
    } catch (e2) {}
    return "";
}

function listContains(list, value) {
    var needle = lower(value);
    for (var i = 0; i < list.length; i++) {
        if (lower(list[i]) === needle) return true;
    }
    return false;
}

function raceAllowsAndroidForms(raceName) {
    if (ConfigManager == null) return false;
    try {
        var cfg = ConfigManager.getRaceCharacter(raceName);
        if (cfg == null) return false;
        var costs = cfg.getFormSkillTpCosts(ANDROID_FORM_GROUP);
        return costs != null && costs.length > 0;
    } catch (e) {
        return false;
    }
}

function refreshPlayer(mcPlayer) {
    try {
        if (mcPlayer.refreshDimensions) {
            mcPlayer.refreshDimensions();
            return;
        }
    } catch (e1) {}
    try {
        if (mcPlayer.m_6210_) mcPlayer.m_6210_();
    } catch (e2) {}
}

function syncDmz(mcPlayer) {
    if (!initDmz()) return;
    try {
        NetworkHandler.sendToTrackingEntityAndSelf(
            new StatsSyncS2C(mcPlayer),
            mcPlayer
        );
        return;
    } catch (e1) {}
    try {
        NetworkHandler.sendToPlayer(new StatsSyncS2C(mcPlayer), mcPlayer);
    } catch (e2) {}
}

/**
 * @return {{ok:boolean, message:string}}
 */
function convertToAndroid(player, force) {
    if (!initDmz()) {
        return { ok: false, message: "DragonMineZ not available" };
    }

    var mcPlayer = asMcPlayer(player);
    if (mcPlayer == null) {
        return { ok: false, message: "Could not resolve Minecraft player" };
    }

    var data = getDmzData(mcPlayer);
    if (data == null) {
        return { ok: false, message: "DragonMineZ data missing for player" };
    }

    var character = null;
    var status = null;
    var skills = null;
    try {
        character = data.getCharacter();
        status = data.getStatus();
        skills = data.getSkills();
    } catch (eGet) {
        return { ok: false, message: "Missing character/status/skills: " + eGet };
    }
    if (character == null || status == null || skills == null) {
        return { ok: false, message: "Missing character/status/skills data" };
    }

    try {
        if (status.isAndroidUpgraded() === true) {
            return {
                ok: false,
                message: playerName(player) + " is already an Android"
            };
        }
    } catch (eAlready) {}

    var raceName = getRaceName(character);
    if (raceName === "") {
        return { ok: false, message: "Could not read player race" };
    }

    if (listContains(BLOCKED_RACES, raceName)) {
        return {
            ok: false,
            message: raceName + " cannot be android-upgraded"
        };
    }

    if (!force && REQUIRE_ALLOWED_RACE && !listContains(ALLOWED_RACES, raceName)) {
        if (!raceAllowsAndroidForms(raceName)) {
            return {
                ok: false,
                message:
                    "Only humans can be converted (got " +
                    raceName +
                    "). Use force to override."
            };
        }
    }

    try {
        status.setAndroidUpgraded(true);
    } catch (eSet) {
        return { ok: false, message: "setAndroidUpgraded failed: " + eSet };
    }

    try {
        skills.setSkillLevel(ANDROID_FORM_GROUP, ANDROID_FORM_SKILL_LEVEL);
    } catch (eSkill) {
        return {
            ok: false,
            message: "androidforms skill set failed: " + eSkill
        };
    }

    for (var i = 0; i < REMOVE_SKILLS.length; i++) {
        try {
            skills.removeSkill(REMOVE_SKILLS[i]);
        } catch (eRem) {}
    }

    try {
        data.updateTransformationSkillLimits(raceName);
    } catch (eLim) {
        console.info(
            "[Androidify] updateTransformationSkillLimits: " + eLim
        );
    }

    try {
        character.setSelectedFormGroup(ANDROID_FORM_GROUP);
        character.setSelectedForm(ANDROID_BASE_FORM);
        character.setActiveForm(ANDROID_FORM_GROUP, ANDROID_BASE_FORM);
    } catch (eForm) {
        return { ok: false, message: "form activate failed: " + eForm };
    }

    try {
        character.clearActiveStackForm();
    } catch (eStack) {}

    refreshPlayer(mcPlayer);
    syncDmz(mcPlayer);

    tellPlayer(
        player,
        "\u00A7a[Android] \u00A7fConversion complete. \u00A77Android forms unlocked."
    );

    return {
        ok: true,
        message:
            "Converted " +
            playerName(player) +
            " to Android (androidforms.androidbase)"
    };
}

function resolveTarget(server, src, nameOrMe) {
    var key = str(nameOrMe).replace(/^\s+|\s+$/g, "");
    if (key === "") return null;
    if (lower(key) === "me" || lower(key) === "@s" || lower(key) === "@p") {
        try {
            if (src && src.player) return src.player;
        } catch (e1) {}
        try {
            if (src && src.getPlayer) return src.getPlayer();
        } catch (e2) {}
        return null;
    }
    return findOnlinePlayer(server, key);
}

function runAndroidify(server, src, playerNameArg, force) {
    var target = resolveTarget(server, src, playerNameArg);
    if (target == null) {
        return {
            ok: false,
            message:
                "Online player not found: " +
                playerNameArg +
                " (must be online)"
        };
    }
    return convertToAndroid(target, !!force);
}

function registerOnDispatcher(dispatcher) {
    if (dispatcher == null) return false;

    var CommandsMc = Java.loadClass("net.minecraft.commands.Commands");
    var StringArgumentType = Java.loadClass(
        "com.mojang.brigadier.arguments.StringArgumentType"
    );

    function buildRoot(name) {
        return CommandsMc.literal(name)
            .requires(function (src) {
                try {
                    /* Console is always allowed; players need op / level 2. */
                    if (src.getEntity && src.getEntity() == null) return true;
                } catch (eCon) {}
                try {
                    return src.hasPermission(2);
                } catch (e) {
                    return true;
                }
            })
            .then(
                CommandsMc.argument("player", StringArgumentType.word())
                    .executes(function (ctx) {
                        var src = ctx.getSource();
                        var pname = StringArgumentType.getString(ctx, "player");
                        var server = null;
                        try {
                            server = src.getServer();
                        } catch (eS) {
                            server = getLiveServer();
                        }
                        var result = runAndroidify(server, src, pname, false);
                        cmdFeedback(src, result.message, !result.ok);
                        return result.ok ? 1 : 0;
                    })
                    .then(
                        CommandsMc.literal("force").executes(function (ctx) {
                            var src = ctx.getSource();
                            var pname = StringArgumentType.getString(
                                ctx,
                                "player"
                            );
                            var server = null;
                            try {
                                server = src.getServer();
                            } catch (eS) {
                                server = getLiveServer();
                            }
                            var result = runAndroidify(
                                server,
                                src,
                                pname,
                                true
                            );
                            cmdFeedback(src, result.message, !result.ok);
                            return result.ok ? 1 : 0;
                        })
                    )
            );
    }

    try {
        dispatcher.register(buildRoot("androidify"));
        dispatcher.register(buildRoot("androidification"));
        return true;
    } catch (err) {
        console.error("[Androidify] dispatcher.register failed: " + err);
        return false;
    }
}

function tryRegisterLive() {
    try {
        var server = getLiveServer();
        if (server == null) {
            console.info("[Androidify] no live server yet");
            return false;
        }
        var commands = null;
        try {
            commands = server.getCommands();
        } catch (e1) {
            try {
                commands = server.commands;
            } catch (e2) {}
        }
        if (commands == null) return false;
        var dispatcher = null;
        try {
            dispatcher = commands.getDispatcher();
        } catch (e3) {
            try {
                dispatcher = commands.dispatcher;
            } catch (e4) {}
        }
        if (dispatcher == null) return false;

        if (registerOnDispatcher(dispatcher)) {
            console.info(
                "[Androidify] /androidify + /androidification on live dispatcher"
            );
            return true;
        }
    } catch (err) {
        console.error("[Androidify] live register failed: " + err);
    }
    return false;
}

ServerEvents.commandRegistry(function (event) {
    try {
        var Commands = event.commands;
        var Arguments = event.arguments;
        if (!Commands || !Arguments) return;

        function registerName(name) {
            event.register(
                Commands.literal(name)
                    .requires(function (src) {
                        return src.hasPermission(2);
                    })
                    .then(
                        Commands.argument(
                            "player",
                            Arguments.STRING.create(event)
                        )
                            .executes(function (ctx) {
                                var pname = Arguments.STRING.getResult(
                                    ctx,
                                    "player"
                                );
                                var result = runAndroidify(
                                    ctx.source.server || getLiveServer(),
                                    ctx.source,
                                    pname,
                                    false
                                );
                                cmdFeedback(
                                    ctx.source,
                                    result.message,
                                    !result.ok
                                );
                                return result.ok ? 1 : 0;
                            })
                            .then(
                                Commands.literal("force").executes(function (
                                    ctx
                                ) {
                                    var pname = Arguments.STRING.getResult(
                                        ctx,
                                        "player"
                                    );
                                    var result = runAndroidify(
                                        ctx.source.server || getLiveServer(),
                                        ctx.source,
                                        pname,
                                        true
                                    );
                                    cmdFeedback(
                                        ctx.source,
                                        result.message,
                                        !result.ok
                                    );
                                    return result.ok ? 1 : 0;
                                })
                            )
                    )
            );
        }

        registerName("androidify");
        registerName("androidification");
        console.info("[Androidify] registered via commandRegistry");
    } catch (err) {
        console.error("[Androidify] commandRegistry failed: " + err);
    }
});

ServerEvents.loaded(function () {
    initDmz();
    tryRegisterLive();
});

initDmz();
tryRegisterLive();

console.info(
    "[Androidify] ready — console: androidify <player>  |  androidify <player> force"
);
