/*
 * DBZ Legacy Reborn - GriefPrevention claim block vouchers.
 *
 * Pure ASCII so KubeJS UTF-8 reader never hits MalformedInputException.
 *
 * Buy vouchers from a Lightman's Item Trader (coins -> paper with NBT).
 * Right-click the paper to redeem: console runs
 *   acb <player> <blocks>
 * which is GriefPrevention adjustbonusclaimblocks.
 *
 * Staff mint stock (op / permission level 2):
 *   /claimvoucher give <player> <blocks> [count]
 *   /claimvoucher me <blocks> [count]
 *   /claimvoucher denoms
 *
 * IMPORTANT: ServerEvents.commandRegistry only runs on full server start.
 * This script ALSO registers into the live Brigadier dispatcher when
 * /kubejs reload server_scripts runs, so a restart is not required.
 *
 * Setup notes: kubejs/CLAIM_VOUCHERS.md
 */

console.info("[Claim Vouchers] script file evaluating...");

var VOUCHER_KEY = "dmzClaimVoucher";
var MAX_BLOCKS_PER_VOUCHER = 100000;
var DEFAULT_DENOMS = [100, 500, 1000];

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

function readVoucherBlocks(stack) {
    if (stack == null) return 0;
    try {
        if (stack.isEmpty && stack.isEmpty()) return 0;
    } catch (eEmpty) {}

    var nbt = null;
    try {
        nbt = stack.nbt;
    } catch (eNbt) {
        nbt = null;
    }
    if (nbt == null) return 0;

    var value = 0;
    try {
        if (nbt.contains && nbt.contains(VOUCHER_KEY)) {
            if (nbt.getInt) value = parseInt("" + nbt.getInt(VOUCHER_KEY), 10);
            else if (nbt.getInteger) {
                value = parseInt("" + nbt.getInteger(VOUCHER_KEY), 10);
            }
        }
    } catch (eGet) {
        try {
            if (nbt[VOUCHER_KEY] != null) {
                value = parseInt("" + nbt[VOUCHER_KEY], 10);
            }
        } catch (eProp) {
            value = 0;
        }
    }

    if (isNaN(value) || value <= 0) return 0;
    if (value > MAX_BLOCKS_PER_VOUCHER) return MAX_BLOCKS_PER_VOUCHER;
    return value;
}

function isVoucherStack(stack) {
    try {
        if (stack == null || (stack.isEmpty && stack.isEmpty())) return false;
        var id = String(stack.id);
        if (id !== "minecraft:paper") return false;
        return readVoucherBlocks(stack) > 0;
    } catch (e) {
        return false;
    }
}

function voucherDisplayName(blocks) {
    return (
        '{"text":"Claim Block Voucher (' +
        blocks +
        ')","color":"gold","italic":false}'
    );
}

function voucherLore(blocks) {
    return [
        '{"text":"Right-click to redeem","color":"gray","italic":false}',
        '{"text":"+' +
            blocks +
            ' GriefPrevention claim blocks","color":"green","italic":false}'
    ];
}

function makeVoucherItem(blocks, count) {
    var b = parseInt("" + blocks, 10);
    if (isNaN(b) || b <= 0) b = 100;
    if (b > MAX_BLOCKS_PER_VOUCHER) b = MAX_BLOCKS_PER_VOUCHER;

    var c = parseInt("" + count, 10);
    if (isNaN(c) || c <= 0) c = 1;
    if (c > 64) c = 64;

    var nbt = {
        dmzClaimVoucher: b,
        display: {
            Name: voucherDisplayName(b),
            Lore: voucherLore(b)
        }
    };

    try {
        return Item.of("minecraft:paper", c, nbt);
    } catch (e1) {
        try {
            return Item.of("minecraft:paper", nbt).withCount(c);
        } catch (e2) {
            return Item.of("minecraft:paper").withNBT(nbt).withCount(c);
        }
    }
}

function shrinkOne(stack, player, creative) {
    if (creative) return true;
    try {
        var count = stack.count;
        if (count <= 1) {
            stack.count = 0;
        } else {
            stack.count = count - 1;
        }
        return true;
    } catch (e) {
        try {
            player.mainHandItem = Item.empty;
            return true;
        } catch (e2) {
            return false;
        }
    }
}

function grantClaimBlocks(server, player, blocks) {
    var name = playerName(player);
    if (!name) return false;
    try {
        server.runCommandSilent("acb " + name + " " + blocks);
        return true;
    } catch (e1) {
        try {
            server.runCommandSilent(
                "adjustbonusclaimblocks " + name + " " + blocks
            );
            return true;
        } catch (e2) {
            console.error(
                "[Claim Vouchers] failed to grant blocks to " +
                    name +
                    ": " +
                    e2
            );
            return false;
        }
    }
}

function tell(player, msg) {
    try {
        player.tell(msg);
    } catch (e) {}
}

ItemEvents.rightClicked("minecraft:paper", function (event) {
    try {
        if (event.hand != "MAIN_HAND") return;

        var stack = event.item;
        if (!isVoucherStack(stack)) return;

        var blocks = readVoucherBlocks(stack);
        if (blocks <= 0) return;

        var player = event.player;
        if (player == null) return;

        var creative = false;
        try {
            creative = !!player.isCreative();
        } catch (eC) {
            try {
                creative = !!player.creative;
            } catch (eC2) {}
        }

        if (!shrinkOne(stack, player, creative)) {
            tell(player, "\u00A7cCould not consume claim voucher.");
            event.cancel();
            return;
        }

        var ok = grantClaimBlocks(event.server, player, blocks);
        if (!ok) {
            try {
                player.give(makeVoucherItem(blocks, 1));
            } catch (eGive) {}
            tell(
                player,
                "\u00A7cClaim voucher redeem failed. Item returned — tell staff."
            );
            event.cancel();
            return;
        }

        tell(
            player,
            "\u00A7aRedeemed claim voucher: \u00A7e+" +
                blocks +
                " \u00A7aclaim blocks."
        );
        event.cancel();
    } catch (err) {
        console.error("[Claim Vouchers] rightClicked error: " + err);
    }
});

function asKjsPlayer(player) {
    if (player == null) return null;
    try {
        if (player.give) return player;
    } catch (e1) {}
    try {
        return Player.of(player);
    } catch (e2) {}
    return player;
}

function giveVoucherToPlayer(target, blocks, count) {
    var kjs = asKjsPlayer(target);
    if (kjs == null) return "no target player";
    if (blocks <= 0) return "blocks must be > 0";
    if (blocks > MAX_BLOCKS_PER_VOUCHER) {
        return "blocks max is " + MAX_BLOCKS_PER_VOUCHER;
    }
    if (count <= 0) count = 1;
    if (count > 64) count = 64;

    var item = makeVoucherItem(blocks, count);
    try {
        kjs.give(item);
    } catch (eGive) {
        return "give failed: " + eGive;
    }

    tell(
        kjs,
        "\u00A7aReceived \u00A7e" +
            count +
            "x \u00A7aclaim voucher (\u00A7e" +
            blocks +
            " \u00A7ablocks each)."
    );
    return null;
}

function cmdMsg(src, msg, failure) {
    try {
        if (failure) {
            if (src.sendFailure) {
                src.sendFailure(Text.of(msg));
                return;
            }
        } else if (src.sendSuccess) {
            src.sendSuccess(Text.of(msg), true);
            return;
        }
    } catch (e1) {}
    try {
        if (src.player) {
            tell(src.player, (failure ? "\u00A7c" : "\u00A7a") + msg);
            return;
        }
    } catch (e2) {}
    try {
        if (src.getPlayer && src.getPlayer()) {
            tell(src.getPlayer(), (failure ? "\u00A7c" : "\u00A7a") + msg);
            return;
        }
    } catch (e3) {}
    console.info("[Claim Vouchers] " + msg);
}

function cmdGiveFromCtx(ctx, Arguments, count) {
    var target = Arguments.PLAYER.getResult(ctx, "player");
    var blocks = Arguments.INTEGER.getResult(ctx, "blocks");
    var err = giveVoucherToPlayer(target, blocks, count);
    if (err) {
        cmdMsg(ctx.source, err, true);
        return 0;
    }
    cmdMsg(
        ctx.source,
        "Gave " +
            count +
            "x claim voucher (" +
            blocks +
            " blocks) to " +
            playerName(target),
        false
    );
    return 1;
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

/*
 * Direct Brigadier registration against the running server.
 * Needed because ServerEvents.commandRegistry does NOT re-fire on
 * /kubejs reload server_scripts.
 */
var CLAIMVOUCHER_REGISTERED = false;

function registerClaimVoucherDispatcher(dispatcher) {
    if (dispatcher == null) return false;

    var CommandsMc = Java.loadClass("net.minecraft.commands.Commands");
    var EntityArgument = Java.loadClass(
        "net.minecraft.commands.arguments.EntityArgument"
    );
    var IntegerArgumentType = Java.loadClass(
        "com.mojang.brigadier.arguments.IntegerArgumentType"
    );

    var root = CommandsMc.literal("claimvoucher").requires(function (src) {
        try {
            return src.hasPermission(2);
        } catch (e) {
            return false;
        }
    });

    var giveNode = CommandsMc.literal("give")
        .then(
            CommandsMc.argument("player", EntityArgument.player())
                .then(
                    CommandsMc.argument(
                        "blocks",
                        IntegerArgumentType.integer(1, MAX_BLOCKS_PER_VOUCHER)
                    )
                        .executes(function (ctx) {
                            var target = EntityArgument.getPlayer(ctx, "player");
                            var blocks = IntegerArgumentType.getInteger(
                                ctx,
                                "blocks"
                            );
                            var err = giveVoucherToPlayer(target, blocks, 1);
                            if (err) {
                                cmdMsg(ctx.getSource(), err, true);
                                return 0;
                            }
                            cmdMsg(
                                ctx.getSource(),
                                "Gave 1x claim voucher (" +
                                    blocks +
                                    " blocks) to " +
                                    playerName(target),
                                false
                            );
                            return 1;
                        })
                        .then(
                            CommandsMc.argument(
                                "count",
                                IntegerArgumentType.integer(1, 64)
                            ).executes(function (ctx) {
                                var target = EntityArgument.getPlayer(
                                    ctx,
                                    "player"
                                );
                                var blocks = IntegerArgumentType.getInteger(
                                    ctx,
                                    "blocks"
                                );
                                var count = IntegerArgumentType.getInteger(
                                    ctx,
                                    "count"
                                );
                                var err = giveVoucherToPlayer(
                                    target,
                                    blocks,
                                    count
                                );
                                if (err) {
                                    cmdMsg(ctx.getSource(), err, true);
                                    return 0;
                                }
                                cmdMsg(
                                    ctx.getSource(),
                                    "Gave " +
                                        count +
                                        "x claim voucher (" +
                                        blocks +
                                        " blocks) to " +
                                        playerName(target),
                                    false
                                );
                                return 1;
                            })
                        )
                )
        );

    var meNode = CommandsMc.literal("me")
        .then(
            CommandsMc.argument(
                "blocks",
                IntegerArgumentType.integer(1, MAX_BLOCKS_PER_VOUCHER)
            )
                .executes(function (ctx) {
                    var src = ctx.getSource();
                    var self = null;
                    try {
                        self = src.getPlayerOrException();
                    } catch (e) {
                        cmdMsg(src, "players only", true);
                        return 0;
                    }
                    var blocks = IntegerArgumentType.getInteger(ctx, "blocks");
                    var err = giveVoucherToPlayer(self, blocks, 1);
                    if (err) {
                        cmdMsg(src, err, true);
                        return 0;
                    }
                    cmdMsg(
                        src,
                        "Gave yourself 1x claim voucher (" + blocks + " blocks)",
                        false
                    );
                    return 1;
                })
                .then(
                    CommandsMc.argument(
                        "count",
                        IntegerArgumentType.integer(1, 64)
                    ).executes(function (ctx) {
                        var src = ctx.getSource();
                        var self = null;
                        try {
                            self = src.getPlayerOrException();
                        } catch (e) {
                            cmdMsg(src, "players only", true);
                            return 0;
                        }
                        var blocks = IntegerArgumentType.getInteger(
                            ctx,
                            "blocks"
                        );
                        var count = IntegerArgumentType.getInteger(ctx, "count");
                        var err = giveVoucherToPlayer(self, blocks, count);
                        if (err) {
                            cmdMsg(src, err, true);
                            return 0;
                        }
                        cmdMsg(
                            src,
                            "Gave yourself " +
                                count +
                                "x claim voucher (" +
                                blocks +
                                " blocks)",
                            false
                        );
                        return 1;
                    })
                )
        );

    var denomsNode = CommandsMc.literal("denoms").executes(function (ctx) {
        cmdMsg(
            ctx.getSource(),
            "Suggested denoms: " + DEFAULT_DENOMS.join(", "),
            false
        );
        cmdMsg(
            ctx.getSource(),
            "Mint: /claimvoucher me <blocks> [count]  OR  /claimvoucher give <player> <blocks> [count]",
            false
        );
        return 1;
    });

    dispatcher.register(root.then(giveNode).then(meNode).then(denomsNode));
    CLAIMVOUCHER_REGISTERED = true;
    return true;
}

function tryRegisterLive() {
    try {
        var server = getLiveServer();
        if (server == null) {
            console.info(
                "[Claim Vouchers] no live server yet — waiting for loaded/commandRegistry"
            );
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
        if (commands == null) {
            console.error("[Claim Vouchers] server.getCommands() missing");
            return false;
        }
        var dispatcher = null;
        try {
            dispatcher = commands.getDispatcher();
        } catch (e3) {
            try {
                dispatcher = commands.dispatcher;
            } catch (e4) {}
        }
        if (dispatcher == null) {
            console.error("[Claim Vouchers] dispatcher missing");
            return false;
        }

        /* Drop prior node if reloading so we do not duplicate children oddly. */
        try {
            var root = dispatcher.getRoot();
            var child = root.getChild("claimvoucher");
            if (child != null && root.getChildren) {
                try {
                    root.getChildren().remove(child);
                } catch (eRem) {}
            }
        } catch (eDrop) {}

        if (registerClaimVoucherDispatcher(dispatcher)) {
            console.info(
                "[Claim Vouchers] /claimvoucher registered on live dispatcher (reload-safe)"
            );
            return true;
        }
    } catch (err) {
        console.error("[Claim Vouchers] live register failed: " + err);
    }
    return false;
}

ServerEvents.commandRegistry(function (event) {
    try {
        var Commands = event.commands;
        var Arguments = event.arguments;
        if (!Commands || !Arguments) {
            console.error("[Claim Vouchers] command API missing");
            return;
        }

        event.register(
            Commands.literal("claimvoucher")
                .requires(function (src) {
                    return src.hasPermission(2);
                })
                .then(
                    Commands.literal("give").then(
                        Commands.argument(
                            "player",
                            Arguments.PLAYER.create(event)
                        ).then(
                            Commands.argument(
                                "blocks",
                                Arguments.INTEGER.create(event)
                            )
                                .executes(function (ctx) {
                                    return cmdGiveFromCtx(ctx, Arguments, 1);
                                })
                                .then(
                                    Commands.argument(
                                        "count",
                                        Arguments.INTEGER.create(event)
                                    ).executes(function (ctx) {
                                        var count =
                                            Arguments.INTEGER.getResult(
                                                ctx,
                                                "count"
                                            );
                                        return cmdGiveFromCtx(
                                            ctx,
                                            Arguments,
                                            count
                                        );
                                    })
                                )
                        )
                    )
                )
                .then(
                    Commands.literal("me")
                        .then(
                            Commands.argument(
                                "blocks",
                                Arguments.INTEGER.create(event)
                            )
                                .executes(function (ctx) {
                                    var self = ctx.source.player;
                                    var blocks =
                                        Arguments.INTEGER.getResult(
                                            ctx,
                                            "blocks"
                                        );
                                    var err = giveVoucherToPlayer(
                                        self,
                                        blocks,
                                        1
                                    );
                                    if (err) {
                                        cmdMsg(ctx.source, err, true);
                                        return 0;
                                    }
                                    cmdMsg(
                                        ctx.source,
                                        "Gave yourself 1x claim voucher (" +
                                            blocks +
                                            " blocks)",
                                        false
                                    );
                                    return 1;
                                })
                                .then(
                                    Commands.argument(
                                        "count",
                                        Arguments.INTEGER.create(event)
                                    ).executes(function (ctx) {
                                        var self = ctx.source.player;
                                        var blocks =
                                            Arguments.INTEGER.getResult(
                                                ctx,
                                                "blocks"
                                            );
                                        var count =
                                            Arguments.INTEGER.getResult(
                                                ctx,
                                                "count"
                                            );
                                        var err = giveVoucherToPlayer(
                                            self,
                                            blocks,
                                            count
                                        );
                                        if (err) {
                                            cmdMsg(ctx.source, err, true);
                                            return 0;
                                        }
                                        cmdMsg(
                                            ctx.source,
                                            "Gave yourself " +
                                                count +
                                                "x claim voucher (" +
                                                blocks +
                                                " blocks)",
                                            false
                                        );
                                        return 1;
                                    })
                                )
                        )
                )
                .then(
                    Commands.literal("denoms").executes(function (ctx) {
                        cmdMsg(
                            ctx.source,
                            "Suggested denoms: " + DEFAULT_DENOMS.join(", "),
                            false
                        );
                        cmdMsg(
                            ctx.source,
                            "Mint: /claimvoucher me <blocks> [count]",
                            false
                        );
                        return 1;
                    })
                )
        );
        CLAIMVOUCHER_REGISTERED = true;
        console.info(
            "[Claim Vouchers] /claimvoucher registered via commandRegistry"
        );
    } catch (err) {
        console.error("[Claim Vouchers] commandRegistry failed: " + err);
    }
});

ServerEvents.loaded(function (event) {
    tryRegisterLive();
});

/* On /kubejs reload server_scripts the server is already up — register now. */
tryRegisterLive();

/*
 * Mohist/Bukkit fallback: op chat command works immediately after reload even
 * when Brigadier registration is not visible to the Bukkit command map.
 *   !claimvoucher me 100 64
 *   !claimvoucher give PlayerName 100 64
 *   !claimvoucher denoms
 */
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

function findOnlinePlayer(server, name) {
    if (!server || !name) return null;
    var want = String(name).toLowerCase();
    try {
        var list = server.players;
        for (var i = 0; i < list.length; i++) {
            if (playerName(list[i]).toLowerCase() === want) return list[i];
        }
    } catch (e1) {}
    try {
        return server.getPlayerList().getPlayerByName(name);
    } catch (e2) {}
    return null;
}

function handleClaimVoucherArgs(player, server, parts) {
    if (!parts || parts.length < 1) {
        tell(
            player,
            "\u00A7eUsage: !claimvoucher me <blocks> [count]  |  give <player> <blocks> [count]  |  denoms"
        );
        return;
    }
    var sub = String(parts[0]).toLowerCase();
    if (sub === "denoms") {
        tell(
            player,
            "\u00A7aSuggested denoms: \u00A7e" + DEFAULT_DENOMS.join(", ")
        );
        return;
    }
    if (sub === "me") {
        var blocksMe = parseInt(parts[1], 10);
        var countMe = parts.length >= 3 ? parseInt(parts[2], 10) : 1;
        var errMe = giveVoucherToPlayer(player, blocksMe, countMe);
        if (errMe) tell(player, "\u00A7c" + errMe);
        else {
            tell(
                player,
                "\u00A7aMinted \u00A7e" +
                    countMe +
                    "x \u00A7avoucher (\u00A7e" +
                    blocksMe +
                    " \u00A7ablocks)."
            );
        }
        return;
    }
    if (sub === "give") {
        if (parts.length < 3) {
            tell(
                player,
                "\u00A7eUsage: !claimvoucher give <player> <blocks> [count]"
            );
            return;
        }
        var target = findOnlinePlayer(server, parts[1]);
        if (target == null) {
            tell(player, "\u00A7cPlayer not found: " + parts[1]);
            return;
        }
        var blocksG = parseInt(parts[2], 10);
        var countG = parts.length >= 4 ? parseInt(parts[3], 10) : 1;
        var errG = giveVoucherToPlayer(target, blocksG, countG);
        if (errG) tell(player, "\u00A7c" + errG);
        else {
            tell(
                player,
                "\u00A7aGave \u00A7e" +
                    countG +
                    "x \u00A7avoucher (\u00A7e" +
                    blocksG +
                    " \u00A7ablocks) to \u00A7e" +
                    playerName(target)
            );
        }
        return;
    }
    tell(player, "\u00A7cUnknown subcommand. Try me / give / denoms");
}

PlayerEvents.chat(function (event) {
    try {
        var msg = String(event.message);
        if (msg.length < 2) return;
        if (msg.charAt(0) !== "!") return;
        var body = msg.substring(1).trim();
        if (body.toLowerCase().indexOf("claimvoucher") !== 0) return;

        var player = event.player;
        if (!playerIsOp(player)) {
            tell(player, "\u00A7cOps only.");
            event.cancel();
            return;
        }

        var rest = body.substring("claimvoucher".length).trim();
        var parts = rest.length ? rest.split(/\s+/) : [];
        handleClaimVoucherArgs(player, event.server, parts);
        event.cancel();
    } catch (err) {
        console.error("[Claim Vouchers] chat fallback error: " + err);
    }
});

console.info(
    "[Claim Vouchers] ready — /claimvoucher me 100  OR  !claimvoucher me 100"
);
