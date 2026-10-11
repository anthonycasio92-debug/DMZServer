// ============================================================================
// DMZ POTHALA PROTECTION
// Minecraft 1.20.1 Forge / KubeJS
//
// FILE:
// kubejs/server_scripts/dmz_pothala_protection.js
//
// PURPOSE:
// - Prevents ALL DMZ Pothalas from retaining enchantments.
// - Removes ALL Apotheosis affix data.
// - Removes Apotheosis rarity.
// - Removes Apotheosis sockets.
// - Removes socketed Apotheosis gems.
// - Preserves DMZ-specific Pothala NBT such as PothalaPairId.
// - Scans normal player inventory.
// - Scans equipped Curios.
// - Cleans existing Pothalas.
// - Continually cleans future modified Pothalas.
//
// DEBUG defaults to false.
// ============================================================================


// ============================================================================
// CONFIG
// ============================================================================

var DEBUG = false;

// Backup scan interval.
// 20 ticks = 1 second.
var CHECK_INTERVAL = 20;


// ============================================================================
// CURIOS
// ============================================================================

var CuriosApi = null;

try {

    CuriosApi = Java.loadClass(
        "top.theillusivec4.curios.api.CuriosApi"
    );

} catch (e) {

    console.warn(
        "[DMZ Pothala Protection] Curios API could not be loaded. " +
        "Normal inventory protection will still work."
    );
}


// ============================================================================
// PROTECTED POTHALAS
// ============================================================================

var PROTECTED_POTHALAS = {

    "dragonminez:pothala_left": true,
    "dragonminez:pothala_right": true,
    "dragonminez:pothala_pair": true,

    "dragonminez:green_pothala_left": true,
    "dragonminez:green_pothala_right": true,
    "dragonminez:green_pothala_pair": true
};


// ============================================================================
// GET ITEM ID
// ============================================================================

function getItemId(stack) {

    try {

        if (stack == null) {
            return "";
        }


        try {

            if (stack.empty) {
                return "";
            }

        } catch (ignored1) {
        }


        try {

            if (stack.isEmpty()) {
                return "";
            }

        } catch (ignored2) {
        }


        // KubeJS ItemStack registry ID.
        var id = stack.id;

        if (id == null) {
            return "";
        }


        return String(id);

    } catch (e) {

        if (DEBUG) {

            console.error(
                "[DMZ Pothala Protection] Failed to read item ID: " +
                e
            );
        }


        return "";
    }
}


// ============================================================================
// CHECK PROTECTED ITEM
// ============================================================================

function isProtectedPothala(stack) {

    var id = getItemId(stack);

    if (id == "") {
        return false;
    }


    return PROTECTED_POTHALAS[id] === true;
}


// ============================================================================
// PLAYER NAME
// ============================================================================

function getPlayerName(player) {

    try {

        return String(
            player.getGameProfile().getName()
        );

    } catch (e) {

        try {
            return String(player.username);
        } catch (ignored) {
            return "unknown";
        }
    }
}


// ============================================================================
// CLEAN POTHALA
// ============================================================================
//
// IMPORTANT:
//
// KubeJS exposes NBT using:
//
//     stack.nbt
//
// and writes NBT using:
//
//     stack.setNbt(...)
//
// We intentionally DO NOT use:
//
//     stack.getTag()
//     stack.setTag()
//
// because those are native Minecraft ItemStack methods and are not exposed
// on the ItemStack objects being passed to this KubeJS server script.
// ============================================================================

function cleanPothala(stack, player, location) {

    try {

        if (stack == null) {
            return false;
        }


        if (!isProtectedPothala(stack)) {
            return false;
        }


        var itemId = getItemId(stack);


        // ====================================================================
        // READ KUBEJS NBT
        // ====================================================================

        var nbt = null;


        try {

            nbt = stack.nbt;

        } catch (nbtError) {

            console.error(
                "[DMZ Pothala Protection] Could not read NBT from " +
                itemId +
                ": " +
                nbtError
            );

            return false;
        }


        // No NBT = nothing to remove.
        if (nbt == null) {

            if (DEBUG) {

                console.info(
                    "[DMZ Pothala Protection] " +
                    itemId +
                    " has no NBT."
                );
            }


            return false;
        }


        var changed = false;


        if (DEBUG) {

            console.info(
                "[DMZ Pothala Protection] Checking " +
                itemId +
                " at " +
                location +
                " | NBT=" +
                String(nbt)
            );
        }


        // ====================================================================
        // VANILLA ENCHANTMENTS
        // ====================================================================

        try {

            if (nbt.contains("Enchantments")) {

                nbt.remove("Enchantments");

                changed = true;


                if (DEBUG) {

                    console.info(
                        "[DMZ Pothala Protection] Removed Enchantments from " +
                        itemId
                    );
                }
            }

        } catch (e1) {

            console.error(
                "[DMZ Pothala Protection] Failed checking Enchantments on " +
                itemId +
                ": " +
                e1
            );
        }


        // ====================================================================
        // STORED ENCHANTMENTS
        // ====================================================================

        try {

            if (nbt.contains("StoredEnchantments")) {

                nbt.remove("StoredEnchantments");

                changed = true;


                if (DEBUG) {

                    console.info(
                        "[DMZ Pothala Protection] Removed StoredEnchantments from " +
                        itemId
                    );
                }
            }

        } catch (e2) {

            console.error(
                "[DMZ Pothala Protection] Failed checking StoredEnchantments on " +
                itemId +
                ": " +
                e2
            );
        }


        // ====================================================================
        // APOTHEOSIS AFFIX DATA
        // ====================================================================
        //
        // Apotheosis 1.20.1 stores affixed gear information under:
        //
        // affix_data
        //
        // Example:
        //
        // affix_data: {
        //     rarity: "apotheosis:mythic",
        //     sockets: 3,
        //     affixes: {...},
        //     uuids: [...]
        // }
        //
        // Removing the entire affix_data compound removes:
        //
        // - rarity
        // - affixes
        // - sockets
        // - affix UUIDs
        // - generated affix name data
        //
        // while leaving unrelated DMZ data alone.
        // ====================================================================

        try {

            if (nbt.contains("affix_data")) {

                nbt.remove("affix_data");

                changed = true;


                if (DEBUG) {

                    console.info(
                        "[DMZ Pothala Protection] Removed affix_data from " +
                        itemId
                    );
                }
            }

        } catch (e3) {

            console.error(
                "[DMZ Pothala Protection] Failed checking affix_data on " +
                itemId +
                ": " +
                e3
            );
        }


        // ====================================================================
        // APOTHEOSIS GENERATED LOOT FLAGS
        // ====================================================================
        //
        // These do not normally give the item affixes by themselves, but
        // clearing them prevents a Pothala that has gone through Apotheosis
        // loot generation from continuing to be marked as an Apotheosis item.
        //
        // They are NOT DMZ Pothala data.
        // ====================================================================

        try {

            if (nbt.contains("apoth_boss")) {

                nbt.remove("apoth_boss");

                changed = true;
            }

        } catch (ignored3) {
        }


        try {

            if (nbt.contains("apoth_rspawn")) {

                nbt.remove("apoth_rspawn");

                changed = true;
            }

        } catch (ignored4) {
        }


        try {

            if (nbt.contains("apoth_rchest")) {

                nbt.remove("apoth_rchest");

                changed = true;
            }

        } catch (ignored5) {
        }


        // ====================================================================
        // POSSIBLE COMPATIBILITY RARITY TAG
        // ====================================================================

        try {

            if (nbt.contains("apothic_rarity")) {

                nbt.remove("apothic_rarity");

                changed = true;
            }

        } catch (ignored6) {
        }


        // ====================================================================
        // NOTHING CHANGED
        // ====================================================================

        if (!changed) {

            if (DEBUG) {

                console.info(
                    "[DMZ Pothala Protection] No prohibited NBT found on " +
                    itemId
                );
            }


            return false;
        }


        // ====================================================================
        // WRITE NBT BACK USING KUBEJS
        // ====================================================================

        try {

            stack.setNbt(nbt);

        } catch (writeError) {

            console.error(
                "[DMZ Pothala Protection] Failed writing cleaned NBT to " +
                itemId +
                ": " +
                writeError
            );

            return false;
        }


        // ====================================================================
        // SUCCESS
        // ====================================================================

        if (DEBUG) {

            console.info(
                "[DMZ Pothala Protection] Sanitized " +
                itemId +
                " owned by " +
                getPlayerName(player) +
                " at " +
                location +
                " | Remaining NBT=" +
                String(stack.nbt)
            );
        }


        return true;

    } catch (e) {

        console.error(
            "[DMZ Pothala Protection] Error cleaning item: " +
            e
        );


        return false;
    }
}


// ============================================================================
// NORMAL PLAYER INVENTORY
// ============================================================================

function cleanPlayerInventory(player) {

    try {

        var inventory = player.getInventory();


        if (inventory == null) {
            return 0;
        }


        var cleaned = 0;

        var size = inventory.getContainerSize();


        for (var i = 0; i < size; i++) {

            var stack = inventory.getItem(i);


            if (
                cleanPothala(
                    stack,
                    player,
                    "inventory slot " + i
                )
            ) {

                cleaned++;
            }
        }


        if (cleaned > 0) {

            try {
                inventory.setChanged();
            } catch (ignored1) {
            }


            // Force server -> client inventory update.
            try {
                player.inventoryMenu.broadcastChanges();
            } catch (ignored2) {
            }


            try {
                player.containerMenu.broadcastChanges();
            } catch (ignored3) {
            }
        }


        return cleaned;

    } catch (e) {

        console.error(
            "[DMZ Pothala Protection] Inventory scan failed: " +
            e
        );


        return 0;
    }
}


// ============================================================================
// CURIOS INVENTORY
// ============================================================================

function cleanPlayerCurios(player) {

    if (CuriosApi == null) {
        return 0;
    }


    var result = {
        cleaned: 0
    };


    try {

        var optional = CuriosApi.getCuriosInventory(player);


        if (optional == null) {
            return 0;
        }


        optional.ifPresent(function(handler) {

            try {

                var curios = handler.getEquippedCurios();


                if (curios == null) {
                    return;
                }


                var slots = curios.getSlots();


                for (var i = 0; i < slots; i++) {

                    var stack = curios.getStackInSlot(i);


                    if (
                        cleanPothala(
                            stack,
                            player,
                            "Curios slot " + i
                        )
                    ) {

                        result.cleaned++;


                        // The ItemStack itself has already been modified.
                        // Re-setting it helps force Curios to notice the
                        // changed NBT.
                        try {

                            curios.setStackInSlot(
                                i,
                                stack
                            );

                        } catch (ignored1) {
                        }
                    }
                }

            } catch (inner) {

                console.error(
                    "[DMZ Pothala Protection] Curios handler scan failed: " +
                    inner
                );
            }
        });


    } catch (e) {

        console.error(
            "[DMZ Pothala Protection] Curios API scan failed: " +
            e
        );
    }


    if (result.cleaned > 0) {

        try {
            player.inventoryMenu.broadcastChanges();
        } catch (ignored2) {
        }


        try {
            player.containerMenu.broadcastChanges();
        } catch (ignored3) {
        }
    }


    return result.cleaned;
}


// ============================================================================
// COMPLETE PLAYER SCAN
// ============================================================================

function cleanAllPothalas(player) {

    if (player == null) {
        return;
    }


    var normal = cleanPlayerInventory(player);

    var curios = cleanPlayerCurios(player);


    if (
        DEBUG &&
        (normal > 0 || curios > 0)
    ) {

        console.info(
            "[DMZ Pothala Protection] Cleaned " +
            normal +
            " inventory Pothala(s) and " +
            curios +
            " Curios Pothala(s) for " +
            getPlayerName(player)
        );
    }
}


// ============================================================================
// LOGIN
// ============================================================================

PlayerEvents.loggedIn(function(event) {

    cleanAllPothalas(
        event.player
    );

});


// ============================================================================
// INVENTORY CHANGE
// ============================================================================

PlayerEvents.inventoryChanged(function(event) {

    cleanAllPothalas(
        event.player
    );

});


// ============================================================================
// BACKUP TICK
// ============================================================================
//
// This catches:
//
// - commands modifying the item
// - another mod adding enchantments
// - Apotheosis reforging
// - moving the item directly into Curios
//
// Maximum normal delay: ~1 second.
// ============================================================================

// Shared tick in player_tick_consolidated.js calls this. No listener here.
global.dmzPothalaTick = {
    CHECK_INTERVAL: CHECK_INTERVAL,
    cleanAllPothalas: cleanAllPothalas
};


// ============================================================================
// LOAD
// ============================================================================

console.info(
    "[DMZ Pothala Protection] Loaded. DEBUG=" +
    DEBUG +
    " | Protected Pothalas=6"
);