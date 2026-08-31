// ============================================================
// Prestige Exchange NPC
// CustomNPCs 1.20.1
//
// First right-click:
//     Displays the exchange and starts a 10-second confirmation.
//
// Second right-click within 10 seconds:
//     Completes the exchange.
//
// Exchange:
//     1. Resets the Fabled Prestige class to level 1.
//     2. Removes the DMZ skill named "prestige".
//     3. Syncs LegacyMechanics held prestiges to 0.
//     4. Awards LegacyMechanics prestige points:
//
//        levelsLost = currentLevel - 1
//        reward = levelsLost * levelsLost
//
// Examples:
//     Prestige 2 -> 1 prestige point
//     Prestige 3 -> 4 prestige points
//     Prestige 5 -> 16 prestige points
//
// Spend points in /lm → Prestige (Skill Shop / Effects / Tiers / Cap).
// ============================================================


// ============================================================
// CONFIGURATION
// ============================================================

// Exact DMZ custom skill ID.
var DMZ_PRESTIGE_SKILL_ID = "prestige";

// Exact displayed name of the Fabled class.
var FABLED_PRESTIGE_CLASS_NAME = "Prestige";

// Telemetry / spend-reason tag written with the point grant.
var POINT_GRANT_REASON = "prestige_exchange_npc";

// 10000 milliseconds = 10 seconds.
var CONFIRM_TIME_MS = 10000;

// Set to true for detailed console output.
var DEBUG = false;


// ============================================================
// JAVA CLASSES
// ============================================================

var System = Java.type(
    "java.lang.System"
);

var Bukkit = Java.type(
    "org.bukkit.Bukkit"
);

var JavaInteger = Java.type(
    "java.lang.Integer"
);

var JavaDouble = Java.type(
    "java.lang.Double"
);

var JavaLong = Java.type(
    "java.lang.Long"
);

var JavaFloat = Java.type(
    "java.lang.Float"
);

var JavaShort = Java.type(
    "java.lang.Short"
);

var JavaByte = Java.type(
    "java.lang.Byte"
);

var JavaBoolean = Java.type(
    "java.lang.Boolean"
);

var StatsProvider = Java.type(
    "com.dragonminez.common.stats.StatsProvider"
);

var StatsCapability = Java.type(
    "com.dragonminez.common.stats.StatsCapability"
);

var NetworkHandler = Java.type(
    "com.dragonminez.common.network.NetworkHandler"
);

var StatsSyncS2C = Java.type(
    "com.dragonminez.common.network.S2C.StatsSyncS2C"
);


// ============================================================
// FABLED / LM REFLECTION CACHE
// ============================================================

var FABLED_GET_DATA = null;

var PLAYER_DATA_GET_CLASSES = null;
var PLAYER_DATA_UPDATE_SCOREBOARD = null;

var PLAYER_CLASS_GET_DATA = null;
var PLAYER_CLASS_GET_LEVEL = null;
var PLAYER_CLASS_SET_EXP = null;
var PLAYER_CLASS_LOSE_LEVELS = null;

var FABLED_CLASS_GET_NAME = null;

var PP_GRANT_POINTS = null;
var PP_GET_POINTS = null;
var PRESTIGE_SET_HELD = null;


// ============================================================
// FIND AN EXACT JAVA METHOD
// ============================================================

function findMethod(javaClass, methodName, parameterNames) {
    var methods = javaClass.getMethods();

    var i;
    var j;

    for (i = 0; i < methods.length; i++) {
        var method = methods[i];

        if (
            ("" + method.getName()) !=
            methodName
        ) {
            continue;
        }

        var parameterTypes =
            method.getParameterTypes();

        if (
            parameterTypes.length !=
            parameterNames.length
        ) {
            continue;
        }

        var matches = true;

        for (
            j = 0;
            j < parameterTypes.length;
            j++
        ) {
            if (
                ("" + parameterTypes[j].getName()) !=
                parameterNames[j]
            ) {
                matches = false;
                break;
            }
        }

        if (matches) {
            return method;
        }
    }

    return null;
}


// ============================================================
// INVOKE A REFLECTED METHOD
// ============================================================

function invokeMethod(method, target, argumentsArray) {
    var parameterTypes =
        method.getParameterTypes();

    var convertedArguments =
        new Array(argumentsArray.length);

    var i;

    for (
        i = 0;
        i < argumentsArray.length;
        i++
    ) {
        var parameterName =
            "" + parameterTypes[i].getName();

        var value =
            argumentsArray[i];

        if (parameterName == "int") {
            convertedArguments[i] =
                JavaInteger.valueOf(
                    "" + parseInt("" + value)
                );

        } else if (parameterName == "double") {
            convertedArguments[i] =
                JavaDouble.valueOf(
                    "" + parseFloat("" + value)
                );

        } else if (parameterName == "long") {
            convertedArguments[i] =
                JavaLong.valueOf(
                    "" + parseInt("" + value)
                );

        } else if (parameterName == "float") {
            convertedArguments[i] =
                JavaFloat.valueOf(
                    "" + parseFloat("" + value)
                );

        } else if (parameterName == "short") {
            convertedArguments[i] =
                JavaShort.valueOf(
                    "" + parseInt("" + value)
                );

        } else if (parameterName == "byte") {
            convertedArguments[i] =
                JavaByte.valueOf(
                    "" + parseInt("" + value)
                );

        } else if (parameterName == "boolean") {
            convertedArguments[i] =
                JavaBoolean.valueOf(
                    value ? "true" : "false"
                );

        } else {
            convertedArguments[i] =
                value;
        }
    }

    return method.invoke(
        target,
        Java.to(
            convertedArguments,
            "java.lang.Object[]"
        )
    );
}


// ============================================================
// LOAD A LEGACYMECHANICS CLASS
// ============================================================

function loadLmClass(className) {
    try {
        return Java.type(className).class;
    } catch (ignored) {
    }

    try {
        var cl = Bukkit
            .getPluginManager()
            .getPlugin("LegacyMechanicsGUI");

        if (cl != null) {
            return Class.forName(
                className,
                true,
                cl.getClass().getClassLoader()
            );
        }
    } catch (ignored2) {
    }

    return Class.forName(className);
}


// ============================================================
// AWARD LEGACYMECHANICS PRESTIGE POINTS
// ============================================================

function grantPrestigePoints(mcPlayer, amount, reason) {
    var pps = loadLmClass(
        "com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem"
    );

    if (PP_GRANT_POINTS == null) {
        PP_GRANT_POINTS = findMethod(
            pps,
            "grantPoints",
            [
                "net.minecraft.server.level.ServerPlayer",
                "int",
                "java.lang.String"
            ]
        );
    }

    if (PP_GRANT_POINTS == null) {
        throw (
            "PrestigePointsSystem.grantPoints(...) " +
            "was not found. Update LegacyMechanics."
        );
    }

    invokeMethod(
        PP_GRANT_POINTS,
        null,
        [
            mcPlayer,
            amount,
            reason
        ]
    );

    if (PP_GET_POINTS == null) {
        PP_GET_POINTS = findMethod(
            pps,
            "getPoints",
            [
                "net.minecraft.server.level.ServerPlayer"
            ]
        );
    }

    if (PP_GET_POINTS == null) {
        return -1;
    }

    return parseInt(
        "" + invokeMethod(
            PP_GET_POINTS,
            null,
            [
                mcPlayer
            ]
        )
    );
}


// ============================================================
// CLEAR LEGACYMECHANICS HELD PRESTIGES (sync with Fabled L1)
// ============================================================

function clearHeldPrestiges(mcPlayer) {
    var prestigeSystem = loadLmClass(
        "com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem"
    );

    if (PRESTIGE_SET_HELD == null) {
        PRESTIGE_SET_HELD = findMethod(
            prestigeSystem,
            "setHeldPublic",
            [
                "net.minecraft.server.level.ServerPlayer",
                "int"
            ]
        );
    }

    if (PRESTIGE_SET_HELD == null) {
        throw (
            "PrestigeSystem.setHeldPublic(...) " +
            "was not found. Update LegacyMechanics."
        );
    }

    invokeMethod(
        PRESTIGE_SET_HELD,
        null,
        [
            mcPlayer,
            0
        ]
    );
}


// ============================================================
// GET THE PLAYER'S FABLED PRESTIGE CLASS
// ============================================================

function getFabledPrestige(player) {
    var plugin = Bukkit
        .getPluginManager()
        .getPlugin("Fabled");

    if (
        plugin == null ||
        !plugin.isEnabled()
    ) {
        throw "Fabled is not loaded or enabled.";
    }

    var bukkitPlayer =
        Bukkit.getPlayerExact(
            "" + player.getName()
        );

    if (bukkitPlayer == null) {
        throw "Unable to access the Bukkit player.";
    }


    if (FABLED_GET_DATA == null) {
        FABLED_GET_DATA = findMethod(
            plugin.getClass(),
            "getData",
            [
                "org.bukkit.OfflinePlayer"
            ]
        );
    }

    if (FABLED_GET_DATA == null) {
        throw (
            "Fabled.getData(OfflinePlayer) " +
            "was not found."
        );
    }

    var playerData = invokeMethod(
        FABLED_GET_DATA,
        null,
        [
            bukkitPlayer
        ]
    );

    if (playerData == null) {
        throw "The player's Fabled data is unavailable.";
    }


    if (PLAYER_DATA_GET_CLASSES == null) {
        PLAYER_DATA_GET_CLASSES = findMethod(
            playerData.getClass(),
            "getClasses",
            []
        );
    }

    if (PLAYER_DATA_GET_CLASSES == null) {
        throw "PlayerData.getClasses() was not found.";
    }

    var classCollection = invokeMethod(
        PLAYER_DATA_GET_CLASSES,
        playerData,
        []
    );

    var prestigeClass = null;

    if (classCollection != null) {
        var classIterator =
            classCollection.iterator();

        while (classIterator.hasNext()) {
            var playerClass =
                classIterator.next();

            if (playerClass == null) {
                continue;
            }

            if (PLAYER_CLASS_GET_DATA == null) {
                PLAYER_CLASS_GET_DATA = findMethod(
                    playerClass.getClass(),
                    "getData",
                    []
                );
            }

            if (PLAYER_CLASS_GET_DATA == null) {
                throw (
                    "PlayerClass.getData() " +
                    "was not found."
                );
            }

            var classData = invokeMethod(
                PLAYER_CLASS_GET_DATA,
                playerClass,
                []
            );

            if (classData == null) {
                continue;
            }

            if (FABLED_CLASS_GET_NAME == null) {
                FABLED_CLASS_GET_NAME = findMethod(
                    classData.getClass(),
                    "getName",
                    []
                );
            }

            if (FABLED_CLASS_GET_NAME == null) {
                throw (
                    "FabledClass.getName() " +
                    "was not found."
                );
            }

            var className = "" + invokeMethod(
                FABLED_CLASS_GET_NAME,
                classData,
                []
            );

            if (
                className.toLowerCase() ==
                FABLED_PRESTIGE_CLASS_NAME.toLowerCase()
            ) {
                prestigeClass =
                    playerClass;

                break;
            }
        }
    }

    if (prestigeClass == null) {
        throw (
            "You do not have the Fabled class " +
            FABLED_PRESTIGE_CLASS_NAME +
            "."
        );
    }


    if (PLAYER_CLASS_GET_LEVEL == null) {
        PLAYER_CLASS_GET_LEVEL = findMethod(
            prestigeClass.getClass(),
            "getLevel",
            []
        );
    }

    if (PLAYER_CLASS_GET_LEVEL == null) {
        throw "PlayerClass.getLevel() was not found.";
    }

    var classLevel = parseInt(
        "" + invokeMethod(
            PLAYER_CLASS_GET_LEVEL,
            prestigeClass,
            []
        )
    );

    return {
        bukkitPlayer: bukkitPlayer,
        playerData: playerData,
        prestigeClass: prestigeClass,
        level: classLevel
    };
}


// ============================================================
// RIGHT-CLICK EVENT
// ============================================================

function interact(event) {
    var player = event.player;
    var npc = event.npc;

    if (
        player == null ||
        npc == null
    ) {
        return;
    }

    var operationStage =
        "starting the interaction";

    try {
        var confirmationKey =
            "prestige_exchange_confirm_" +
            player.getUUID();

        var npcTempdata =
            npc.getTempdata();

        var currentTime =
            System.currentTimeMillis();


        // ====================================================
        // SECOND RIGHT-CLICK
        // ====================================================

        if (npcTempdata.has(confirmationKey)) {
            var expirationTime = parseInt(
                "" + npcTempdata.get(
                    confirmationKey
                )
            );

            if (
                !isNaN(expirationTime) &&
                currentTime <= expirationTime
            ) {
                npcTempdata.remove(
                    confirmationKey
                );


                operationStage =
                    "reading Dragon Mine Z data";

                var mcPlayer =
                    player.getMCEntity();

                if (mcPlayer == null) {
                    throw (
                        "Unable to access the " +
                        "Minecraft player."
                    );
                }

                var dmzData = StatsProvider.get(
                    StatsCapability.INSTANCE,
                    mcPlayer
                ).orElse(null);

                if (dmzData == null) {
                    throw (
                        "Dragon Mine Z data " +
                        "is unavailable."
                    );
                }

                var dmzSkills =
                    dmzData.getSkills();

                if (
                    dmzSkills == null ||
                    !dmzSkills.hasSkill(
                        DMZ_PRESTIGE_SKILL_ID
                    )
                ) {
                    player.message(
                        "§cYou no longer have the DMZ Prestige skill."
                    );

                    return;
                }


                operationStage =
                    "reading the Fabled Prestige class";

                var fabled =
                    getFabledPrestige(player);

                var currentLevel =
                    fabled.level;

                if (
                    isNaN(currentLevel) ||
                    currentLevel <= 1
                ) {
                    player.message(
                        "§cYour Fabled Prestige class is already level 1."
                    );

                    return;
                }


                var levelsLost =
                    currentLevel - 1;

                var rewardAmount =
                    levelsLost * levelsLost;


                if (PLAYER_CLASS_SET_EXP == null) {
                    PLAYER_CLASS_SET_EXP = findMethod(
                        fabled.prestigeClass.getClass(),
                        "setExp",
                        [
                            "double"
                        ]
                    );
                }

                if (PLAYER_CLASS_LOSE_LEVELS == null) {
                    PLAYER_CLASS_LOSE_LEVELS = findMethod(
                        fabled.prestigeClass.getClass(),
                        "loseLevels",
                        [
                            "int"
                        ]
                    );
                }

                if (PLAYER_CLASS_SET_EXP == null) {
                    throw (
                        "PlayerClass.setExp(double) " +
                        "was not found."
                    );
                }

                if (PLAYER_CLASS_LOSE_LEVELS == null) {
                    throw (
                        "PlayerClass.loseLevels(int) " +
                        "was not found."
                    );
                }


                operationStage =
                    "clearing Fabled Prestige experience";

                invokeMethod(
                    PLAYER_CLASS_SET_EXP,
                    fabled.prestigeClass,
                    [
                        0.0
                    ]
                );


                operationStage =
                    "reducing the Fabled Prestige level";

                invokeMethod(
                    PLAYER_CLASS_LOSE_LEVELS,
                    fabled.prestigeClass,
                    [
                        levelsLost
                    ]
                );


                operationStage =
                    "clearing remaining Prestige experience";

                invokeMethod(
                    PLAYER_CLASS_SET_EXP,
                    fabled.prestigeClass,
                    [
                        0.0
                    ]
                );


                operationStage =
                    "verifying the new Prestige level";

                var newLevel = parseInt(
                    "" + invokeMethod(
                        PLAYER_CLASS_GET_LEVEL,
                        fabled.prestigeClass,
                        []
                    )
                );

                if (newLevel != 1) {
                    player.message(
                        "§cFabled did not reduce Prestige to level 1."
                    );

                    player.message(
                        "§cThe DMZ skill and prestige points were not changed."
                    );

                    return;
                }


                operationStage =
                    "removing the DMZ Prestige skill";

                dmzSkills.removeSkill(
                    DMZ_PRESTIGE_SKILL_ID
                );


                try {
                    NetworkHandler.sendToPlayer(
                        new StatsSyncS2C(
                            mcPlayer
                        ),
                        mcPlayer
                    );

                } catch (syncError) {
                    if (DEBUG) {
                        print(
                            "[Prestige Exchange NPC] " +
                            "DMZ sync error: " +
                            syncError
                        );
                    }
                }


                operationStage =
                    "clearing LegacyMechanics held prestiges";

                clearHeldPrestiges(mcPlayer);


                operationStage =
                    "granting LegacyMechanics prestige points";

                var newBalance =
                    grantPrestigePoints(
                        mcPlayer,
                        rewardAmount,
                        POINT_GRANT_REASON
                    );


                try {
                    if (
                        PLAYER_DATA_UPDATE_SCOREBOARD ==
                        null
                    ) {
                        PLAYER_DATA_UPDATE_SCOREBOARD =
                            findMethod(
                                fabled.playerData.getClass(),
                                "updateScoreboard",
                                []
                            );
                    }

                    if (
                        PLAYER_DATA_UPDATE_SCOREBOARD !=
                        null
                    ) {
                        invokeMethod(
                            PLAYER_DATA_UPDATE_SCOREBOARD,
                            fabled.playerData,
                            []
                        );
                    }

                } catch (scoreboardError) {
                    if (DEBUG) {
                        print(
                            "[Prestige Exchange NPC] " +
                            "scoreboard error: " +
                            scoreboardError
                        );
                    }
                }


                player.message(
                    "§a§lPRESTIGE EXCHANGE COMPLETE"
                );

                player.message(
                    "§eFabled Prestige: §f" +
                    currentLevel +
                    " §7-> §f1"
                );

                player.message(
                    "§eDMZ skill removed: §f" +
                    DMZ_PRESTIGE_SKILL_ID
                );

                player.message(
                    "§aPrestige points gained: §e+" +
                    rewardAmount
                );

                if (newBalance >= 0) {
                    player.message(
                        "§7Wallet balance: §e" +
                        newBalance +
                        " §7points"
                    );
                }

                player.message(
                    "§8Spend points in §f/lm §8→ Prestige"
                );

                return;
            }


            npcTempdata.remove(
                confirmationKey
            );
        }


        // ====================================================
        // FIRST RIGHT-CLICK
        // ====================================================

        operationStage =
            "reading Dragon Mine Z data";

        var mcPlayer =
            player.getMCEntity();

        if (mcPlayer == null) {
            throw (
                "Unable to access the Minecraft player."
            );
        }

        var dmzData = StatsProvider.get(
            StatsCapability.INSTANCE,
            mcPlayer
        ).orElse(null);

        if (dmzData == null) {
            throw (
                "Dragon Mine Z data is unavailable."
            );
        }

        var dmzSkills =
            dmzData.getSkills();

        if (
            dmzSkills == null ||
            !dmzSkills.hasSkill(
                DMZ_PRESTIGE_SKILL_ID
            )
        ) {
            player.message(
                "§cYou do not currently have the DMZ Prestige skill."
            );

            return;
        }


        operationStage =
            "reading the Fabled Prestige class";

        var fabled =
            getFabledPrestige(player);

        var currentLevel =
            fabled.level;

        if (
            isNaN(currentLevel) ||
            currentLevel <= 1
        ) {
            player.message(
                "§cYour Fabled Prestige class is already level 1."
            );

            player.message(
                "§7There are no Prestige levels available to exchange."
            );

            return;
        }


        var levelsLost =
            currentLevel - 1;

        var rewardAmount =
            levelsLost * levelsLost;


        // Fail fast if LegacyMechanics prestige API is missing.
        operationStage =
            "checking LegacyMechanics prestige points API";

        loadLmClass(
            "com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem"
        );


        npcTempdata.put(
            confirmationKey,
            "" + (
                currentTime +
                CONFIRM_TIME_MS
            )
        );


        player.message(
            "§6§lPRESTIGE EXCHANGE"
        );

        player.message(
            "§eYour Fabled Prestige class will reset from " +
            "level §f" +
            currentLevel +
            "§e to level §f1§e."
        );

        player.message(
            "§eYou will lose §f" +
            levelsLost +
            "§e Prestige level(s)."
        );

        player.message(
            "§eYour DMZ skill §f" +
            DMZ_PRESTIGE_SKILL_ID +
            "§e will be removed."
        );

        player.message(
            "§aYou will receive §e" +
            rewardAmount +
            " §aprestige point" +
            (rewardAmount == 1 ? "" : "s") +
            "§a."
        );

        player.message(
            "§8Spend them in §f/lm §8→ Prestige (Skill Shop / Effects / Tiers / Cap)."
        );

        player.message(
            "§cRight-click this NPC again within 10 seconds to confirm."
        );

    } catch (error) {
        player.message(
            "§cThe Prestige exchange encountered an error."
        );

        player.message(
            "§7Stage: " +
            operationStage
        );

        player.message(
            "§7Error: " +
            error
        );

        if (DEBUG) {
            print(
                "[Prestige Exchange NPC] " +
                "Stage: " +
                operationStage +
                " | Error: " +
                error
            );
        }
    }
}
