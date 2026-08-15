/*
 * DBZ Legacy Reborn - Disable Tinkers' Construct Silky + Silky Cloth.
 *
 * Pure ASCII so KubeJS UTF-8 reader never hits MalformedInputException.
 *
 * 1) Removes Silky ability + salvage recipes (cannot apply / crystal).
 * 2) Removes Silky Cloth casting + melting recipes (cannot craft / melt).
 * 3) Datapack override zeros Silky modules (no silk touch effect).
 * 4) Strips Silky from existing tools (tool kept, ability slot freed).
 * 5) Converts Silky Cloth / Silky modifier crystals to cobwebs
 *    (uses Forge item IDs so Mohist raw ItemStacks are detected).
 *
 * JEI hide: kubejs/client_scripts/tinkers_silky_hide.js (clients need that file).
 * Reload: /kubejs reload server_scripts then /reload
 * Client JEI: reconnect or /jei reload after client_scripts update.
 */

console.info(
    "[Tinkers Silky] Disabling Silky / Silky Cloth (recipes + strip)..."
);

var CLOTH_SCAN_INTERVAL = 20; /* 1s — match ItemBan tick */
var TOOL_SCAN_INTERVAL = 100; /* 5s */
var DEBUG_SILKY = false;

var ToolStack = null;
var ModifierId = null;
var ModifierCrystalItem = null;
var ForgeRegistries = null;
var ServerPlayer = null;
var SilkyId = null;
var JAVA_READY = false;
var FORGE_ID_READY = false;

function initForgeId() {
    if (FORGE_ID_READY) return true;
    try {
        ForgeRegistries = Java.loadClass(
            "net.minecraftforge.registries.ForgeRegistries"
        );
        FORGE_ID_READY = ForgeRegistries != null;
    } catch (err) {
        FORGE_ID_READY = false;
        console.error("[Tinkers Silky] ForgeRegistries load failed: " + err);
    }
    return FORGE_ID_READY;
}

function initJava() {
    if (JAVA_READY) return true;
    try {
        initForgeId();
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
            ServerPlayer = Java.loadClass(
                "net.minecraft.server.level.ServerPlayer"
            );
        } catch (eSp) {
            ServerPlayer = null;
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

function asServerPlayer(player) {
    if (player == null) return null;
    var p = player;
    try {
        if (p.minecraftPlayer) p = p.minecraftPlayer;
    } catch (e1) {}
    try {
        if (p.player) p = p.player;
    } catch (e2) {}
    try {
        if (ServerPlayer != null && ServerPlayer.class.isInstance(p)) return p;
    } catch (e3) {}
    try {
        if (ServerPlayer != null && p instanceof ServerPlayer) return p;
    } catch (e4) {}
    return p;
}

function isEmptyStack(stack) {
    if (stack == null) return true;
    try {
        if (stack.isEmpty()) return true;
    } catch (e) {}
    try {
        if (stack.empty === true) return true;
    } catch (e0) {}
    try {
        if (String(stack.id) === "minecraft:air") return true;
    } catch (e2) {}
    var id = stackId(stack);
    return id === "" || id === "minecraft:air";
}

/*
 * Resolve item id for KubeJS wrappers AND raw Forge/Bukkit ItemStacks.
 * Previous bug: container.getItem() returns vanilla stacks with no .id,
 * so silky_cloth was never detected / converted.
 */
function stackId(stack) {
    if (stack == null) return "";

    try {
        if (stack.id != null && String(stack.id) !== "" && String(stack.id) !== "undefined") {
            return String(stack.id);
        }
    } catch (e0) {}

    try {
        if (typeof stack.getId === "function") {
            var gid = String(stack.getId());
            if (gid && gid !== "undefined") return gid;
        }
    } catch (e1) {}

    try {
        if (typeof stack.is === "function") {
            if (stack.is("tconstruct:silky_cloth")) return "tconstruct:silky_cloth";
            if (stack.is("tconstruct:modifier_crystal")) {
                return "tconstruct:modifier_crystal";
            }
        }
    } catch (e2) {}

    try {
        initForgeId();
        if (ForgeRegistries != null) {
            var mc = getMcItemStack(stack);
            if (mc != null) {
                var item = null;
                try {
                    item = mc.getItem();
                } catch (eGet) {
                    try {
                        item = mc.item;
                    } catch (eItem) {}
                }
                if (item != null) {
                    var key = ForgeRegistries.ITEMS.getKey(item);
                    if (key != null) return String(key);
                }
            }
        }
    } catch (e3) {}

    try {
        var desc = "";
        if (typeof stack.getDescriptionId === "function") {
            desc = String(stack.getDescriptionId());
        } else if (typeof stack.getItem === "function") {
            desc = String(stack.getItem().getDescriptionId());
        }
        /* item.tconstruct.silky_cloth */
        if (desc.indexOf("silky_cloth") >= 0) return "tconstruct:silky_cloth";
        if (desc.indexOf("modifier_crystal") >= 0) {
            return "tconstruct:modifier_crystal";
        }
    } catch (e4) {}

    try {
        var s = String(stack);
        if (s.indexOf("silky_cloth") >= 0) return "tconstruct:silky_cloth";
    } catch (e5) {}

    return "";
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
        try {
            if (stack.nbtString) snbt = String(stack.nbtString);
        } catch (eA) {}
        try {
            if (!snbt && stack.nbt) snbt = String(stack.nbt);
        } catch (eB) {}
        try {
            if (!snbt && typeof stack.getTag === "function" && stack.getTag()) {
                snbt = String(stack.getTag());
            }
        } catch (eC) {}
        if (snbt === "") return false;
        var lower = snbt.toLowerCase();
        return (
            lower.indexOf("tconstruct:silky") >= 0 ||
            lower.indexOf("silky_cloth") >= 0 ||
            (lower.indexOf('"name":"tconstruct:silky"') >= 0)
        );
    } catch (e) {
        return false;
    }
}

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
        return stripSilkyViaRawNbt(mc) || stripSilkyViaRawNbt(stack);
    }
}

function stripSilkyViaRawNbt(stack) {
    if (stack == null) return false;
    var tag = null;
    try {
        tag = stack.nbt;
    } catch (e1) {
        try {
            tag = stack.getTag ? stack.getTag() : null;
        } catch (e2) {
            try {
                tag = stack.getOrCreateTag ? stack.getOrCreateTag() : null;
            } catch (e3) {
                tag = null;
            }
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
        var list = tag.getList(key, 10);
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
    try {
        var c2 = Number(stack.getCount());
        if (!isNaN(c2) && c2 > 0) return c2;
    } catch (e2) {}
    return 1;
}

function isSilkyCloth(stack) {
    return stackId(stack) === "tconstruct:silky_cloth";
}

function isSilkyCrystal(stack) {
    if (stackId(stack) !== "tconstruct:modifier_crystal") return false;
    try {
        if (initJava() && ModifierCrystalItem != null) {
            var mid = ModifierCrystalItem.getModifier(getMcItemStack(stack));
            if (mid != null && String(mid).toLowerCase().indexOf("silky") >= 0) {
                return true;
            }
        }
    } catch (eMid) {}
    return nbtMentionsSilky(stack);
}

function cobwebReplacement(count) {
    var c = count;
    if (isNaN(c) || c < 1) c = 1;
    try {
        return Item.of(c + "x minecraft:cobweb");
    } catch (e1) {
        try {
            var stack = Item.of("minecraft:cobweb");
            stack.count = c;
            return stack;
        } catch (e2) {
            return Item.of("minecraft:cobweb");
        }
    }
}

function convertBannedSilkyItem(stack) {
    if (isEmptyStack(stack)) return null;
    if (isSilkyCloth(stack) || isSilkyCrystal(stack)) {
        return cobwebReplacement(stackCount(stack));
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
    try {
        if (typeof container.get === "function") return container.get(slot);
    } catch (e3) {}
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
    try {
        if (typeof container.set === "function") {
            container.set(slot, stack);
            return true;
        }
    } catch (e3) {}
    return false;
}

function processStackInSlot(container, slot, label, doTools) {
    var stack = readSlot(container, slot);
    if (isEmptyStack(stack)) return 0;

    var replacement = convertBannedSilkyItem(stack);
    if (replacement != null) {
        if (writeSlot(container, slot, replacement)) {
            console.info(
                "[Tinkers Silky] Converted " +
                    stackId(stack) +
                    " x" +
                    stackCount(stack) +
                    " -> cobweb in " +
                    label +
                    " slot " +
                    slot
            );
            return 1;
        }
        console.info(
            "[Tinkers Silky] FAILED to write cobweb over " +
                stackId(stack) +
                " in " +
                label +
                " slot " +
                slot
        );
    }

    if (doTools) {
        var id = stackId(stack);
        if (nbtMentionsSilky(stack) || id.indexOf("tconstruct:") === 0) {
            if (stripSilkyFromTool(stack)) {
                writeSlot(container, slot, stack);
                if (DEBUG_SILKY) {
                    console.info(
                        "[Tinkers Silky] Stripped Silky from tool in " +
                            label +
                            " slot " +
                            slot
                    );
                }
                return 1;
            }
        }
    }
    return 0;
}

function purgeContainer(container, label, doTools) {
    var changed = 0;
    if (container == null) return 0;
    var size = containerSize(container);
    for (var slot = 0; slot < size; slot++) {
        try {
            changed += processStackInSlot(container, slot, label, doTools);
        } catch (eSlot) {}
    }
    return changed;
}

function purgePlayerSilky(player, announce, doTools) {
    if (player == null) return;
    initJava();
    initForgeId();

    var changed = 0;
    var sp = asServerPlayer(player);

    /* Prefer Forge ServerPlayer inventory (raw ItemStacks). */
    try {
        if (sp != null && typeof sp.getInventory === "function") {
            changed += purgeContainer(sp.getInventory(), "forgeInv", doTools);
        }
    } catch (eForge) {}

    try {
        changed += purgeContainer(player.getInventory(), "inventory", doTools);
    } catch (eInv) {
        try {
            changed += purgeContainer(player.inventory, "inventoryKJS", doTools);
        } catch (eInv2) {}
    }

    try {
        if (sp != null && typeof sp.getEnderChestInventory === "function") {
            changed += purgeContainer(
                sp.getEnderChestInventory(),
                "forgeEnder",
                doTools
            );
        }
    } catch (eFe) {}

    try {
        changed += purgeContainer(
            player.getEnderChestInventory(),
            "ender",
            doTools
        );
    } catch (eEnder) {
        try {
            changed += purgeContainer(
                player.enderChestInventory,
                "enderKJS",
                doTools
            );
        } catch (eEnder2) {}
    }

    try {
        var main = player.mainHandItem;
        if (main) {
            var rep = convertBannedSilkyItem(main);
            if (rep != null) {
                try {
                    player.setMainHandItem(rep);
                    changed += 1;
                } catch (eSetM) {
                    try {
                        player.mainHandItem = rep;
                        changed += 1;
                    } catch (eSetM2) {}
                }
            } else if (doTools && stripSilkyFromTool(main)) {
                changed += 1;
            }
        }
    } catch (eMain) {}

    try {
        var off = player.offHandItem;
        if (off) {
            var repOff = convertBannedSilkyItem(off);
            if (repOff != null) {
                try {
                    player.setOffHandItem(repOff);
                    changed += 1;
                } catch (eSetO) {
                    try {
                        player.offHandItem = repOff;
                        changed += 1;
                    } catch (eSetO2) {}
                }
            } else if (doTools && stripSilkyFromTool(off)) {
                changed += 1;
            }
        }
    } catch (eOff) {}

    if (changed > 0 && announce) {
        try {
            player.tell(
                "\u00A77Silky Cloth removed (" +
                    changed +
                    "). Replaced with cobwebs; tools were kept."
            );
        } catch (eTell) {}
        console.info(
            "[Tinkers Silky] Purged " +
                changed +
                " stack(s) for " +
                (player.username || player.name || "?")
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
        event.forEachRecipe({}, function (recipe) {
            var rid = "";
            try {
                rid = String(recipe.getId()).toLowerCase();
            } catch (eId) {
                return;
            }
            if (rid.indexOf("silky_cloth") < 0 && rid.indexOf("/silky") < 0) {
                return;
            }
            if (rid.indexOf("silky") < 0) return;
            try {
                event.remove({ id: recipe.getId() });
                removed++;
                console.info("[Tinkers Silky] Removed recipe scan " + rid);
            } catch (eR2) {}
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

/* Block using / picking up silky cloth if somehow present. */
ItemEvents.rightClicked("tconstruct:silky_cloth", function (event) {
    try {
        event.cancel();
    } catch (e) {}
    try {
        var rep = cobwebReplacement(stackCount(event.item));
        event.player.setMainHandItem(rep);
        event.player.tell("\u00A77Silky Cloth is disabled and was replaced with cobwebs.");
    } catch (e2) {}
});

PlayerEvents.loggedIn(function (event) {
    try {
        purgePlayerSilky(event.player, true, true);
    } catch (err) {
        console.error("[Tinkers Silky] loggedIn error: " + err);
    }
});

PlayerEvents.tick(function (event) {
    try {
        var player = event.player;
        if (player == null) return;
        var age = 0;
        try {
            age = Number(player.age);
        } catch (eAge) {
            return;
        }
        if (age % CLOTH_SCAN_INTERVAL === 0) {
            /* Cloth every 1s; tools only on the slower cadence. */
            purgePlayerSilky(player, true, age % TOOL_SCAN_INTERVAL === 0);
        }
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
                console.info(
                    "[Tinkers Silky] Ground silky item converted to cobweb."
                );
            } catch (eSet) {}
            return;
        }

        if (nbtMentionsSilky(stack) || stackId(stack).indexOf("tconstruct:") === 0) {
            stripSilkyFromTool(stack);
        }
    } catch (err) {}
});

console.info(
    "[DBZ Legacy Reborn] Tinkers Silky strip handlers registered (Forge ID + cobweb convert)."
);
