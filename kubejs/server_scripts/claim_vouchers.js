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
 * Staff mint stock:
 *   /claimvoucher give <player> <blocks> [count]
 *   /claimvoucher give <player> 100
 *   /claimvoucher give <player> 500 16
 *
 * Reload: /kubejs reload server_scripts
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
        if (src.player) tell(src.player, (failure ? "\u00A7c" : "\u00A7a") + msg);
    } catch (e2) {}
    console.info("[Claim Vouchers] " + msg);
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
                    Commands.literal("give")
                        .then(
                            Commands.argument(
                                "player",
                                Arguments.PLAYER.create(event)
                            ).then(
                                Commands.argument(
                                    "blocks",
                                    Arguments.INTEGER.create(event)
                                )
                                    .executes(function (ctx) {
                                        return cmdGive(ctx, Arguments, 1);
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
                                            return cmdGive(
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
                    Commands.literal("denoms").executes(function (ctx) {
                        cmdMsg(
                            ctx.source,
                            "Suggested denoms: " + DEFAULT_DENOMS.join(", "),
                            false
                        );
                        cmdMsg(
                            ctx.source,
                            "Mint: /claimvoucher give <player> <blocks> [count]",
                            false
                        );
                        return 1;
                    })
                )
        );
        console.info("[Claim Vouchers] /claimvoucher registered");
    } catch (err) {
        console.error("[Claim Vouchers] commandRegistry failed: " + err);
    }
});

function cmdGive(ctx, Arguments, count) {
    var target = Arguments.PLAYER.getResult(ctx, "player");
    var blocks = Arguments.INTEGER.getResult(ctx, "blocks");
    if (blocks <= 0) {
        cmdMsg(ctx.source, "blocks must be > 0", true);
        return 0;
    }
    if (blocks > MAX_BLOCKS_PER_VOUCHER) {
        cmdMsg(
            ctx.source,
            "blocks max is " + MAX_BLOCKS_PER_VOUCHER,
            true
        );
        return 0;
    }
    if (count <= 0) count = 1;
    if (count > 64) count = 64;

    var item = makeVoucherItem(blocks, count);
    try {
        target.give(item);
    } catch (eGive) {
        cmdMsg(ctx.source, "give failed: " + eGive, true);
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
    tell(
        target,
        "\u00A7aReceived \u00A7e" +
            count +
            "x \u00A7aclaim voucher (\u00A7e" +
            blocks +
            " \u00A7ablocks each)."
    );
    return 1;
}

console.info(
    "[Claim Vouchers] ready — right-click paper with dmzClaimVoucher NBT; /claimvoucher give"
);
