// ============================================================
// Prestige Exchange NPC
// CustomNPCs 1.20.1
//
// First right-click:
//     Shows how many prestige points will convert into Ancient
//     Tokens and starts a 10-second confirmation.
//
// Second right-click within 10 seconds:
//     Spends those prestige points and grants Ancient Tokens.
//
// Exchange (1:1 by default):
//     costPoints = wallet balance (at confirm time, capped to
//                  the amount shown on first click)
//     tokens     = costPoints / POINTS_PER_TOKEN
//
// Does NOT reset Fabled Prestige or remove the DMZ prestige skill.
// Earn points via /lm → Prestige turn-in, then convert here.
// ============================================================


// ============================================================
// CONFIGURATION
// ============================================================

// Registry ID of the rewarded item.
var REWARD_ITEM_ID = "betteritem:oform";

// Name displayed in chat.
var REWARD_ITEM_CHAT_NAME = "Ancient Token";

// Actual custom name applied to the rewarded item.
// Leave empty ("") to use the item's normal name.
var REWARD_ITEM_CUSTOM_NAME = "Ancient Token";

// Prestige points spent per token granted.
var POINTS_PER_TOKEN = 1;

// Telemetry / spend-reason tag.
var POINT_SPEND_REASON = "prestige_exchange_npc";

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


// ============================================================
// LM REFLECTION CACHE
// ============================================================

var PP_TRY_SPEND = null;
var PP_GET_POINTS = null;


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

    try {
        var cl2 = Bukkit
            .getPluginManager()
            .getPlugin("LegacyMechanics");

        if (cl2 != null) {
            return Class.forName(
                className,
                true,
                cl2.getClass().getClassLoader()
            );
        }
    } catch (ignored3) {
    }

    return Class.forName(className);
}


// ============================================================
// PRESTIGE POINTS API
// ============================================================

function getPrestigePointsApi() {
    return loadLmClass(
        "com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem"
    );
}

function getPrestigePoints(mcPlayer) {
    var pps = getPrestigePointsApi();

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
        throw (
            "PrestigePointsSystem.getPoints(...) " +
            "was not found. Update LegacyMechanics."
        );
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

function trySpendPrestigePoints(mcPlayer, amount, reason) {
    var pps = getPrestigePointsApi();

    if (PP_TRY_SPEND == null) {
        PP_TRY_SPEND = findMethod(
            pps,
            "trySpend",
            [
                "net.minecraft.server.level.ServerPlayer",
                "int",
                "java.lang.String"
            ]
        );
    }

    if (PP_TRY_SPEND == null) {
        throw (
            "PrestigePointsSystem.trySpend(...) " +
            "was not found. Update LegacyMechanics."
        );
    }

    var ok = invokeMethod(
        PP_TRY_SPEND,
        null,
        [
            mcPlayer,
            amount,
            reason
        ]
    );

    return ok == true || ("" + ok) == "true";
}

function tokensForPoints(points) {
    var rate = parseInt("" + POINTS_PER_TOKEN);

    if (isNaN(rate) || rate < 1) {
        rate = 1;
    }

    var spendable = parseInt("" + points);

    if (isNaN(spendable) || spendable < 1) {
        return {
            cost: 0,
            tokens: 0
        };
    }

    var tokens = Math.floor(spendable / rate);
    var cost = tokens * rate;

    return {
        cost: cost,
        tokens: tokens
    };
}


// ============================================================
// CREATE THE CONFIGURED REWARD ITEM
// ============================================================

function createRewardItem(player, amount) {
    var item = player
        .getWorld()
        .createItem(
            REWARD_ITEM_ID,
            amount
        );

    if (
        item == null ||
        item.isEmpty()
    ) {
        throw (
            "Unable to create reward item " +
            REWARD_ITEM_ID +
            "."
        );
    }

    if (
        REWARD_ITEM_CUSTOM_NAME != null &&
        REWARD_ITEM_CUSTOM_NAME != ""
    ) {
        item.setCustomName(
            REWARD_ITEM_CUSTOM_NAME
        );
    }

    return item;
}


// ============================================================
// GIVE THE REWARD
// ============================================================

function giveReward(
    player,
    rewardAmount,
    rewardTemplate
) {
    var inventory =
        player.getInventory();

    var inventorySize =
        inventory.getSize();

    var remaining =
        rewardAmount;

    var maxStackSize =
        rewardTemplate.getMaxStackSize();

    var i;

    // Slots 0-35 = normal inventory + hotbar.
    if (inventorySize > 36) {
        inventorySize = 36;
    }

    // Fill existing exactly matching stacks first.
    // compare(template, false) matches item + NBT (custom name).
    for (
        i = 0;
        i < inventorySize &&
        remaining > 0;
        i++
    ) {
        var existingItem =
            inventory.getSlot(i);

        if (
            existingItem == null ||
            existingItem.isEmpty()
        ) {
            continue;
        }

        if (
            !existingItem.compare(
                rewardTemplate,
                false
            )
        ) {
            continue;
        }

        var availableSpace =
            existingItem.getMaxStackSize() -
            existingItem.getStackSize();

        if (availableSpace <= 0) {
            continue;
        }

        var amountToAdd =
            availableSpace;

        if (amountToAdd > remaining) {
            amountToAdd =
                remaining;
        }

        existingItem.setStackSize(
            existingItem.getStackSize() +
            amountToAdd
        );

        inventory.setSlot(
            i,
            existingItem
        );

        remaining =
            remaining -
            amountToAdd;
    }

    // Fill empty inventory slots.
    for (
        i = 0;
        i < inventorySize &&
        remaining > 0;
        i++
    ) {
        var slotItem =
            inventory.getSlot(i);

        if (
            slotItem != null &&
            !slotItem.isEmpty()
        ) {
            continue;
        }

        var newStackAmount =
            maxStackSize;

        if (newStackAmount > remaining) {
            newStackAmount =
                remaining;
        }

        var newStack =
            rewardTemplate.copy();

        newStack.setStackSize(
            newStackAmount
        );

        inventory.setSlot(
            i,
            newStack
        );

        remaining =
            remaining -
            newStackAmount;
    }

    // Drop overflow at the player's feet.
    while (remaining > 0) {
        var dropAmount =
            maxStackSize;

        if (dropAmount > remaining) {
            dropAmount =
                remaining;
        }

        var droppedStack =
            rewardTemplate.copy();

        droppedStack.setStackSize(
            dropAmount
        );

        player.dropItem(
            droppedStack
        );

        remaining =
            remaining -
            dropAmount;
    }

    player.updatePlayerInventory();
}


// ============================================================
// CONFIRMATION PAYLOAD: "expiryMs|costPoints"
// ============================================================

function packConfirm(expiryMs, costPoints) {
    return "" + expiryMs + "|" + costPoints;
}

function unpackConfirm(raw) {
    var text = "" + raw;
    var pipe = text.indexOf("|");

    if (pipe < 0) {
        return {
            expiry: parseInt(text),
            cost: -1
        };
    }

    return {
        expiry: parseInt(text.substring(0, pipe)),
        cost: parseInt(text.substring(pipe + 1))
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
            var packed = unpackConfirm(
                npcTempdata.get(
                    confirmationKey
                )
            );

            var expirationTime =
                packed.expiry;

            var confirmedCost =
                packed.cost;

            if (
                !isNaN(expirationTime) &&
                currentTime <= expirationTime
            ) {
                // Consume confirmation first so rapid clicks
                // cannot double-spend.
                npcTempdata.remove(
                    confirmationKey
                );


                operationStage =
                    "reading Minecraft player";

                var mcPlayer =
                    player.getMCEntity();

                if (mcPlayer == null) {
                    throw (
                        "Unable to access the Minecraft player."
                    );
                }


                operationStage =
                    "reading prestige point balance";

                var balance =
                    getPrestigePoints(mcPlayer);

                if (
                    isNaN(confirmedCost) ||
                    confirmedCost < 1
                ) {
                    player.message(
                        "§cExchange expired or invalid. Try again."
                    );
                    return;
                }

                if (balance < confirmedCost) {
                    player.message(
                        "§cYou no longer have enough prestige points."
                    );
                    player.message(
                        "§7Need §e" +
                        confirmedCost +
                        "§7, have §e" +
                        balance +
                        "§7."
                    );
                    return;
                }

                var deal =
                    tokensForPoints(confirmedCost);

                if (
                    deal.tokens < 1 ||
                    deal.cost < 1
                ) {
                    player.message(
                        "§cNothing to convert."
                    );
                    return;
                }


                operationStage =
                    "creating Ancient Token template";

                var rewardTemplate =
                    createRewardItem(
                        player,
                        1
                    );


                operationStage =
                    "spending prestige points";

                var spent = trySpendPrestigePoints(
                    mcPlayer,
                    deal.cost,
                    POINT_SPEND_REASON
                );

                if (!spent) {
                    player.message(
                        "§cCould not spend prestige points. Try again."
                    );
                    return;
                }


                operationStage =
                    "granting Ancient Tokens";

                giveReward(
                    player,
                    deal.tokens,
                    rewardTemplate
                );


                var newBalance =
                    getPrestigePoints(mcPlayer);

                player.message(
                    "§a§lPRESTIGE EXCHANGE COMPLETE"
                );

                player.message(
                    "§ePrestige points spent: §c-" +
                    deal.cost
                );

                player.message(
                    "§aReceived: §f" +
                    deal.tokens +
                    "x §a" +
                    REWARD_ITEM_CHAT_NAME
                );

                player.message(
                    "§7Wallet balance: §e" +
                    newBalance +
                    " §7points"
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
            "reading Minecraft player";

        var mcPlayerFirst =
            player.getMCEntity();

        if (mcPlayerFirst == null) {
            throw (
                "Unable to access the Minecraft player."
            );
        }


        operationStage =
            "reading prestige point balance";

        var points =
            getPrestigePoints(mcPlayerFirst);

        var offer =
            tokensForPoints(points);

        if (
            offer.tokens < 1 ||
            offer.cost < 1
        ) {
            player.message(
                "§cYou have no prestige points to convert."
            );

            player.message(
                "§7Earn points in §f/lm §7→ Prestige (turn in held prestiges), then come back."
            );

            return;
        }


        operationStage =
            "preparing reward item";

        // Fail fast if the item id is bad before confirmation.
        createRewardItem(
            player,
            1
        );


        npcTempdata.put(
            confirmationKey,
            packConfirm(
                currentTime + CONFIRM_TIME_MS,
                offer.cost
            )
        );


        player.message(
            "§6§lPRESTIGE EXCHANGE"
        );

        player.message(
            "§eConvert §f" +
            offer.cost +
            " §eprestige point" +
            (offer.cost == 1 ? "" : "s") +
            " §einto §f" +
            offer.tokens +
            "x §e" +
            REWARD_ITEM_CHAT_NAME +
            "§e."
        );

        if (POINTS_PER_TOKEN != 1) {
            player.message(
                "§8Rate: §7" +
                POINTS_PER_TOKEN +
                " §8point(s) per token"
            );
        }

        player.message(
            "§7Current wallet: §e" +
            points +
            " §7points"
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
