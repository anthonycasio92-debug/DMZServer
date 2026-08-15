/*
 * DBZ Legacy Reborn - Disable Tinkers' Construct Silky + Silky Cloth.
 *
 * Pure ASCII so KubeJS UTF-8 reader never hits MalformedInputException.
 *
 * 1) Removes Silky ability + salvage recipes (cannot apply / crystal).
 * 2) Removes Silky Cloth casting + melting recipes (cannot craft / melt).
 * 3) Datapack override zeros Silky modules (no silk touch effect).
 * 4) Strips Silky from existing tools (tool kept, ability slot freed).
 * 5) Converts existing Silky Cloth / Silky modifier crystals to cobwebs
 *    (slot kept, banned item replaced — never clears to empty wipe).
 *
 * Reload: /reload  or  /kubejs reload server_scripts then /reload
 */

console.info(
    "[Tinkers Silky] Disabling Silky / Silky Cloth (recipes + strip)..."
);

var PLAYER_SCAN_INTERVAL = 100; /* 5s at 20 tps */
var DEBUG_SILKY = false;

var ToolStack = null;
var ModifierId = null;
var ModifierCrystalItem = null;
var SilkyId = null;
var JAVA_READY = false;

function initJava() {
    if (JAVA_READY) return true;
    try {
        ToolStack = Java.loadClass(
            "slimeknights.tconstruct.library.tools.nbt.ToolStack"
        );
        ModifierId = Java.loadClass(
            "slimeknights.tconstruct.library.modifiers.ModifierId"
        );
        try {
            ModifierCrystalItem = Java.loadClass(
                "slimeknights.tconstruct.tools.item.ModifierCrystalItem"
            );
        } catch (eCrystal) {
            ModifierCrystalItem = null;
        }
        try {
            SilkyId = new ModifierId("tconstruct", "silky");
        } catch (eId1) {
            try {
                SilkyId = new ModifierId("tconstruct:silky");
            } catch (eId2) {
                SilkyId = null;
            }
        }
        JAVA_READY = ToolStack != null && SilkyId != null;
        if (JAVA_READY) {
            console.info("[Tinkers Silky] ToolStack + ModifierId ready.");
        } else {
            console.error(
                "[Tinkers Silky] Java init incomplete (ToolStack/SilkyId)."
            );
        }
    } catch (err) {
        JAVA_READY = false;
        console.error("[Tinkers Silky] Java init failed: " + err);
    }
    return JAVA_READY;
}

function isEmptyStack(stack) {
    if (stack == null) return true;
    try {
        if (stack.isEmpty()) return true;
    } catch (e) {}
    try {
        if (String(stack.id) === "minecraft:air") return true;
    } catch (e2) {}
    return false;
}

function stackId(stack) {
    try {
        return String(stack.id);
    } catch (e) {
        return "";
    }
}

function getMcItemStack(stack) {
    if (stack == null) return null;
    try {
        if (stack.itemStack != null) return stack.itemStack;
    } catch (e1) {}
    try {
        if (typeof stack.getItemStack === "function") {
            return stack.getItemStack();
        }
    } catch (e2) {}
    return stack;
}

function nbtMentionsSilky(stack) {
    if (isEmptyStack(stack)) return false;
    try {
        var snbt = "";
        if (stack.nbtString) snbt = String(stack.nbtString);
        else if (stack.nbt) snbt = String(stack.nbt);
        if (snbt === "") return false;
        var lower = snbt.toLowerCase();
        return (
            lower.indexOf("tconstruct:silky") >= 0 ||
            lower.indexOf('"name":"tconstruct:silky"') >= 0 ||
            lower.indexOf("silky") >= 0
        );
    } catch (e) {
        return false;
    }
}

/*
 * Strip Silky upgrade from a Tinkers tool. Keeps the tool; frees the slot.
 * Returns true if Silky was removed.
 */
function stripSilkyFromTool(stack) {
    if (!initJava()) return false;
    if (isEmptyStack(stack)) return false;

    var mc = getMcItemStack(stack);
    if (mc == null) return false;

    try {
        if (!ToolStack.isInitialized(mc)) return false;
    } catch (eInit) {
        return false;
    }

    try {
        var tool = ToolStack.from(mc);
        if (tool == null) return false;
        var level = 0;
        try {
            level = Number(tool.getUpgrades().getLevel(SilkyId));
        } catch (eLvl) {
            level = 0;
        }
        if (isNaN(level) || level <= 0) return false;

        tool.removeModifier(SilkyId, level);
        try {
            tool.updateStack(mc);
        } catch (eUpd) {
            try {
                tool.updateStack(mc, true);
            } catch (eUpd2) {}
        }
        if (DEBUG_SILKY) {
            console.info(
                "[Tinkers Silky] Stripped Silky lvl " +
                    level +
                    " from " +
                    stackId(stack)
            );
        }
        return true;
    } catch (err) {
        if (DEBUG_SILKY) {
            console.info("[Tinkers Silky] strip tool failed: " + err);
        }
        return stripSilkyViaRawNbt(mc) || stripSilkyViaRawNbt(stack);
    }
}

/* Fallback: remove silky entries from tic_upgrades list in NBT. */
function stripSilkyViaRawNbt(stack) {
    if (stack == null) return false;
    var tag = null;
    try {
        tag = stack.nbt;
    } catch (e1) {
        try {
            tag = stack.getOrCreateTag ? stack.getOrCreateTag() : null;
        } catch (e2) {
            tag = null;
        }
    }
    if (tag == null) return false;

    var changed = filterModifierList(tag, "tic_upgrades");
    changed = filterModifierList(tag, "tic_modifiers") || changed;
    return changed;
}

function filterModifierList(tag, key) {
    if (tag == null || key == null) return false;
    try {
        if (!(tag.contains && tag.contains(key))) return false;
    } catch (eHas) {
        return false;
    }
    try {
        var list = tag.getList(key, 10); /* 10 = compound */
        if (list == null) return false;
        var removed = false;
        for (var i = list.size() - 1; i >= 0; i--) {
            var compound = list.getCompound(i);
            var name = "";
            try {
                name = String(compound.getString("name"));
            } catch (eName) {}
            if (name.toLowerCase().indexOf("silky") >= 0) {
                list.remove(i);
                removed = true;
            }
        }
        if (removed) {
            if (list.size() <= 0) {
                try {
                    tag.remove(key);
                } catch (eRem) {}
            } else {
                try {
                    tag.put(key, list);
                } catch (ePut) {}
            }
        }
        return removed;
    } catch (err) {
        return false;
    }
}

function stackCount(stack) {
    try {
        var c = Number(stack.count);
        if (!isNaN(c) && c > 0) return c;
    } catch (e) {}
    return 1;
}

/*
 * Convert banned silky cloth / silky crystals to cobwebs (same count).
 * Never wipes the slot to empty without a replacement.
 */
function convertBannedSilkyItem(stack) {
    if (isEmptyStack(stack)) return null;
    var id = stackId(stack);
    var count = stackCount(stack);

    if (id === "tconstruct:silky_cloth") {
        return Item.of(count + "x minecraft:cobweb");
    }

    if (id === "tconstruct:modifier_crystal") {
        var isSilkyCrystal = false;
        try {
            if (initJava() && ModifierCrystalItem != null) {
                var mid = ModifierCrystalItem.getModifier(getMcItemStack(stack));
                if (mid != null && String(mid).toLowerCase().indexOf("silky") >= 0) {
                    isSilkyCrystal = true;
                }
            }
        } catch (eMid) {}
        if (!isSilkyCrystal && nbtMentionsSilky(stack)) {
            isSilkyCrystal = true;
        }
        if (isSilkyCrystal) {
            return Item.of(count + "x minecraft:cobweb");
        }
    }
    return null;
}

function containerSize(container) {
    try {
        if (typeof container.getContainerSize === "function") {
            return Number(container.getContainerSize());
        }
    } catch (e1) {}
    try {
        if (container.size != null) return Number(container.size);
    } catch (e2) {}
    try {
        if (typeof container.getSlots === "function") {
            return Number(container.getSlots());
        }
    } catch (e3) {}
    return 41;
}

function readSlot(container, slot) {
    try {
        if (typeof container.getItem === "function") return container.getItem(slot);
    } catch (e1) {}
    try {
        if (typeof container.getStackInSlot === "function") {
            return container.getStackInSlot(slot);
        }
    } catch (e2) {}
    return null;
}

function writeSlot(container, slot, stack) {
    try {
        if (typeof container.setItem === "function") {
            container.setItem(slot, stack);
            return true;
        }
    } catch (e1) {}
    try {
        if (typeof container.setStackInSlot === "function") {
            container.setStackInSlot(slot, stack);
            return true;
        }
    } catch (e2) {}
    return false;
}

function processStackInSlot(container, slot, label) {
    var stack = readSlot(container, slot);
    if (isEmptyStack(stack)) return 0;
    var changed = 0;

    var replacement = convertBannedSilkyItem(stack);
    if (replacement != null) {
        if (writeSlot(container, slot, replacement)) {
            if (DEBUG_SILKY) {
                console.info(
                    "[Tinkers Silky] Converted " +
                        stackId(stack) +
                        " -> cobweb in " +
                        label +
                        " slot " +
                        slot
                );
            }
            return 1;
        }
    }

    if (nbtMentionsSilky(stack) || stackId(stack).indexOf("tconstruct:") === 0) {
        if (stripSilkyFromTool(stack)) {
            writeSlot(container, slot, stack);
            changed = 1;
            if (DEBUG_SILKY) {
                console.info(
                    "[Tinkers Silky] Stripped Silky from tool in " +
                        label +
                        " slot " +
                        slot
                );
            }
        }
    }
    return changed;
}

function purgeContainer(container, label) {
    var changed = 0;
    if (container == null) return 0;
    var size = containerSize(container);
    for (var slot = 0; slot < size; slot++) {
        try {
            changed += processStackInSlot(container, slot, label);
        } catch (eSlot) {}
    }
    return changed;
}

function purgePlayerSilky(player, announce) {
    if (player == null) return;
    if (!initJava()) return;

    var changed = 0;
    try {
        changed += purgeContainer(player.getInventory(), "inventory");
    } catch (eInv) {
        try {
            changed += purgeContainer(player.inventory, "inventoryKJS");
        } catch (eInv2) {}
    }
    try {
        changed += purgeContainer(player.getEnderChestInventory(), "ender");
    } catch (eEnder) {
        try {
            changed += purgeContainer(player.enderChestInventory, "enderKJS");
        } catch (eEnder2) {}
    }

    try {
        if (player.mainHandItem && stripSilkyFromTool(player.mainHandItem)) {
            changed += 1;
        } else if (player.mainHandItem) {
            var rep = convertBannedSilkyItem(player.mainHandItem);
            if (rep != null) {
                try {
                    player.mainHandItem = rep;
                    changed += 1;
                } catch (eHand) {}
            }
        }
    } catch (eMain) {}
    try {
        if (player.offHandItem && stripSilkyFromTool(player.offHandItem)) {
            changed += 1;
        } else if (player.offHandItem) {
            var repOff = convertBannedSilkyItem(player.offHandItem);
            if (repOff != null) {
                try {
                    player.offHandItem = repOff;
                    changed += 1;
                } catch (eOff) {}
            }
        }
    } catch (eOff2) {}

    if (changed > 0 && announce) {
        try {
            player.tell(
                "\u00A77Silky was removed from " +
                    changed +
                    " item(s). Tools were kept; Silky Cloth became cobwebs."
            );
        } catch (eTell) {}
        console.info(
            "[Tinkers Silky] Purged Silky from " +
                changed +
                " stack(s) for " +
                player.username
        );
    }
}

ServerEvents.recipes(function (event) {
    var ids = [
        "tconstruct:tools/modifiers/ability/silky",
        "tconstruct:tools/modifiers/salvage/ability/silky",
        "tconstruct:tools/modifiers/silky_cloth",
        "tconstruct:smeltery/melting/metal/rose_gold/silky_cloth"
    ];
    var removed = 0;
    for (var i = 0; i < ids.length; i++) {
        try {
            event.remove({ id: ids[i] });
            removed++;
            console.info("[Tinkers Silky] Removed recipe " + ids[i]);
        } catch (eRem) {
            console.info(
                "[Tinkers Silky] Could not remove " + ids[i] + ": " + eRem
            );
        }
    }

    try {
        event.remove({ output: "tconstruct:silky_cloth" });
        removed++;
        console.info(
            "[Tinkers Silky] Removed recipes with output tconstruct:silky_cloth"
        );
    } catch (eOut) {
        console.info("[Tinkers Silky] output remove silky_cloth: " + eOut);
    }

    try {
        event.forEachRecipe({ type: "tconstruct:modifier" }, function (recipe) {
            var keep = true;
            try {
                var rid = String(recipe.getId()).toLowerCase();
                if (rid.indexOf("silky") >= 0) keep = false;
            } catch (eId) {}
            try {
                var json = recipe.json;
                var result = null;
                try {
                    result = json.get("result");
                } catch (e1) {
                    try {
                        result = json.result;
                    } catch (e2) {}
                }
                if (result != null && String(result).indexOf("silky") >= 0) {
                    keep = false;
                }
            } catch (eJ) {}
            if (!keep) {
                try {
                    event.remove({ id: recipe.getId() });
                    removed++;
                    console.info(
                        "[Tinkers Silky] Removed modifier recipe " +
                            recipe.getId()
                    );
                } catch (eR2) {}
            }
        });
    } catch (eForEach) {}

    console.info(
        "[DBZ Legacy Reborn] Tinkers Silky / Silky Cloth recipes removed (" +
            removed +
            ")."
    );
});

ServerEvents.highPriorityData(function (event) {
    event.addJson("tconstruct:tinkering/modifiers/silky", {
        level_display: "tconstruct:no_levels",
        modules: [],
        tooltip_display: "never"
    });
    console.info("[Tinkers Silky] highPriorityData applied (modifier empty).");
});

PlayerEvents.loggedIn(function (event) {
    try {
        purgePlayerSilky(event.player, true);
    } catch (err) {
        console.error("[Tinkers Silky] loggedIn error: " + err);
    }
});

PlayerEvents.tick(function (event) {
    try {
        var player = event.player;
        if (player == null) return;
        if (player.age % PLAYER_SCAN_INTERVAL !== 0) return;
        purgePlayerSilky(player, true);
    } catch (err) {
        console.error("[Tinkers Silky] tick error: " + err);
    }
});

EntityEvents.spawned("minecraft:item", function (event) {
    try {
        var entity = event.entity;
        var stack = entity.item;
        if (isEmptyStack(stack)) return;

        var replacement = convertBannedSilkyItem(stack);
        if (replacement != null) {
            try {
                entity.item = replacement;
            } catch (eSet) {}
            return;
        }

        if (nbtMentionsSilky(stack) || stackId(stack).indexOf("tconstruct:") === 0) {
            stripSilkyFromTool(stack);
        }
    } catch (err) {}
});

console.info(
    "[DBZ Legacy Reborn] Tinkers Silky strip handlers registered (tools kept)."
);
