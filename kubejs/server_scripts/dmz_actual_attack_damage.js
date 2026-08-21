// kubejs/server_scripts/dmz_actual_attack_damage.js
// Port of DMZ_Actual_Attack_Damage_ItemStack_Datapack_v1 → KubeJS.
//
// Same behavior: once a listed weapon is held in mainhand/offhand, write
// AttributeModifiers + DMZActualDamageV1 marker onto that ItemStack.
//
// WHY KUBEJS: the datapack loop used execute @a with nbt= selectors every
// 10 ticks, which forced full player NBT saves (~28% Server thread in Spark).
// This script only reads mainhand/offhand ItemStacks via Java — no commands.
//
// INSTALL:
// 1. Deploy this file
// 2. /schedule clear dmz_actual_damage:loop
// 3. /datapack disable "file/DMZ_Actual_Attack_Damage_ItemStack_Datapack_v1.zip"
//    (or remove the zip from AdventureWorld/datapacks/)
// 4. /kubejs reload server_scripts
//
// Do NOT leave the datapack enabled or values may double-apply races.

var MARKER = "DMZActualDamageV1";
var UPDATE_INTERVAL = 10; // match datapack: every 10 ticks
var DEBUG = false;

// Vanilla tool AttributeModifier UUIDs (same as datapack item_modifiers).
var DAMAGE_UUID = Java.loadClass("java.util.UUID").fromString(
    "cb3f55d3-645c-4f38-a497-9c13a33db5cf"
);
var SPEED_UUID = Java.loadClass("java.util.UUID").fromString(
    "fa233e1c-4180-4865-b01b-bcce9785aca3"
);

var CompoundTag = Java.loadClass("net.minecraft.nbt.CompoundTag");
var ListTag = Java.loadClass("net.minecraft.nbt.ListTag");

// amount = displayed target Attack Damage minus the player's base 1.0 fist damage
// (matches datapack set_attributes addition values).
var TARGETS = {
    "dragonminez:brave_sword": { amount: 17.75, speed: -2.4, target: 18.75, category: "DMZ x0.25" },
    "dragonminez:dimensional_sword": { amount: 61.5, speed: -2.4, target: 62.5, category: "DMZ x0.25" },
    "dragonminez:gete_axe": { amount: 2.0, speed: -3.0, target: 3.0, category: "DMZ x0.25" },
    "dragonminez:gete_hoe": { amount: -0.25, speed: 0.0, target: 0.75, category: "DMZ x0.25" },
    "dragonminez:gete_pickaxe": { amount: 1.0, speed: -2.8, target: 2.0, category: "DMZ x0.25" },
    "dragonminez:gete_shovel": { amount: 1.125, speed: -3.0, target: 2.125, category: "DMZ x0.25" },
    "dragonminez:power_pole": { amount: 15.0, speed: -2.6, target: 16.0, category: "DMZ x0.25" },
    "dragonminez:yajirobe_katana": { amount: 2.0, speed: -2.6, target: 3.0, category: "DMZ x0.25" },
    "dragonminez:z_sword": { amount: 74.0, speed: -2.8, target: 75.0, category: "DMZ x0.25" },
    "simplymore:ascended_idol": { amount: 99.0, speed: -2.7, target: 100.0, category: "Simply More unique x10" },
    "simplymore:black_pearl": { amount: 89.0, speed: -2.0, target: 90.0, category: "Simply More unique x10" },
    "simplymore:blade_of_the_grotesque": { amount: 119.0, speed: -2.8, target: 120.0, category: "Simply More unique x10" },
    "simplymore:boas_fang": { amount: 69.0, speed: -2.2, target: 70.0, category: "Simply More unique x10" },
    "simplymore:brassturn": { amount: 79.0, speed: -0.2, target: 80.0, category: "Simply More unique x10" },
    "simplymore:cindergorge": { amount: 109.0, speed: -2.7, target: 110.0, category: "Simply More unique x10" },
    "simplymore:culterex": { amount: 79.0, speed: -1.8, target: 80.0, category: "Simply More unique x10" },
    "simplymore:darksent": { amount: 99.0, speed: -2.7, target: 100.0, category: "Simply More unique x10" },
    "simplymore:deaths_eyrie": { amount: 119.0, speed: -3.2, target: 120.0, category: "Simply More unique x10" },
    "simplymore:earthshatter": { amount: 119.0, speed: -3.4, target: 120.0, category: "Simply More unique x10" },
    "simplymore:exedrill": { amount: 89.0, speed: -3.0, target: 90.0, category: "Simply More unique x10" },
    "simplymore:glimmerstep": { amount: 89.0, speed: -3.0, target: 90.0, category: "Simply More unique x10" },
    "simplymore:grandfrost": { amount: 139.0, speed: -3.4, target: 140.0, category: "Simply More unique x10" },
    "simplymore:great_slither": { amount: 109.0, speed: -2.6, target: 110.0, category: "Simply More unique x10" },
    "simplymore:holylight": { amount: 99.0, speed: -2.7, target: 100.0, category: "Simply More unique x10" },
    "simplymore:lustrous_moxie": { amount: 99.0, speed: -2.6, target: 100.0, category: "Simply More unique x10" },
    "simplymore:matterbane": { amount: 89.0, speed: -2.4, target: 90.0, category: "Simply More unique x10" },
    "simplymore:mimicry_backhand_blade": { amount: 79.0, speed: -1.7, target: 80.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_chakram": { amount: 89.0, speed: -3.1, target: 90.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_claymore": { amount: 119.0, speed: -2.8, target: 120.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_cutlass": { amount: 99.0, speed: -2.0, target: 100.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_dagger": { amount: 79.0, speed: -1.8, target: 80.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_deer_horns": { amount: 89.0, speed: -1.9, target: 90.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_glaive": { amount: 99.0, speed: -2.6, target: 100.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_grandsword": { amount: 159.0, speed: -3.4, target: 160.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_great_katana": { amount: 109.0, speed: -2.6, target: 110.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_great_spear": { amount: 129.0, speed: -2.7, target: 130.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_greataxe": { amount: 129.0, speed: -3.1, target: 130.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_greathammer": { amount: 139.0, speed: -3.2, target: 140.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_halberd": { amount: 129.0, speed: -2.8, target: 130.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_katana": { amount: 99.0, speed: -2.0, target: 100.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_khopesh": { amount: 89.0, speed: -2.1, target: 90.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_lance": { amount: 99.0, speed: -1.7, target: 100.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_longsword": { amount: 99.0, speed: -2.4, target: 100.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_pernach": { amount: 109.0, speed: -2.7, target: 110.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_quarterstaff": { amount: 79.0, speed: -2.0, target: 80.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_rapier": { amount: 89.0, speed: -1.8, target: 90.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_sai": { amount: 69.0, speed: -1.5, target: 70.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_scythe": { amount: 109.0, speed: -2.7, target: 110.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_spear": { amount: 99.0, speed: -2.7, target: 100.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_twinblade": { amount: 99.0, speed: -2.0, target: 100.0, category: "Simply More Mimicry x10" },
    "simplymore:mimicry_warglaive": { amount: 99.0, speed: -2.2, target: 100.0, category: "Simply More Mimicry x10" },
    "simplymore:molten_flare": { amount: 129.0, speed: -3.4, target: 130.0, category: "Simply More unique x10" },
    "simplymore:myrmedge": { amount: 79.0, speed: -2.1, target: 80.0, category: "Simply More unique x10" },
    "simplymore:perforiscus": { amount: 99.0, speed: -3.3, target: 100.0, category: "Simply More unique x10" },
    "simplymore:revvengine": { amount: 79.0, speed: -2.0, target: 80.0, category: "Simply More unique x10" },
    "simplymore:runic_backhand_blade": { amount: 69.0, speed: -1.7, target: 70.0, category: "Simply More runic x10" },
    "simplymore:runic_dagger": { amount: 69.0, speed: -1.8, target: 70.0, category: "Simply More runic x10" },
    "simplymore:runic_deer_horns": { amount: 79.0, speed: -1.9, target: 80.0, category: "Simply More runic x10" },
    "simplymore:runic_grandsword": { amount: 149.0, speed: -3.4, target: 150.0, category: "Simply More runic x10" },
    "simplymore:runic_great_katana": { amount: 99.0, speed: -2.6, target: 100.0, category: "Simply More runic x10" },
    "simplymore:runic_great_spear": { amount: 119.0, speed: -3.0, target: 120.0, category: "Simply More runic x10" },
    "simplymore:runic_khopesh": { amount: 79.0, speed: -2.1, target: 80.0, category: "Simply More runic x10" },
    "simplymore:runic_lance": { amount: 89.0, speed: -3.0, target: 90.0, category: "Simply More runic x10" },
    "simplymore:runic_pernach": { amount: 99.0, speed: -2.7, target: 100.0, category: "Simply More runic x10" },
    "simplymore:runic_quarterstaff": { amount: 69.0, speed: -2.0, target: 70.0, category: "Simply More runic x10" },
    "simplymore:ruptured_idol": { amount: 99.0, speed: -2.7, target: 100.0, category: "Simply More unique x10" },
    "simplymore:ruyi_jingu_bang": { amount: 89.0, speed: -2.0, target: 90.0, category: "Simply More unique x10" },
    "simplymore:serpentine_valour": { amount: 109.0, speed: -3.3, target: 110.0, category: "Simply More unique x10" },
    "simplymore:smouldering_ruin": { amount: 69.0, speed: -1.7, target: 70.0, category: "Simply More unique x10" },
    "simplymore:soul_foreseer": { amount: 89.0, speed: -2.6, target: 90.0, category: "Simply More unique x10" },
    "simplymore:stasis": { amount: 79.0, speed: -2.0, target: 80.0, category: "Simply More unique x10" },
    "simplymore:tarnished_idol": { amount: 99.0, speed: -2.7, target: 100.0, category: "Simply More unique x10" },
    "simplymore:the_blood_harvester": { amount: 79.0, speed: -2.4, target: 80.0, category: "Simply More unique x10" },
    "simplymore:the_vessel_breach": { amount: 59.0, speed: -1.8, target: 60.0, category: "Simply More unique x10" },
    "simplymore:tidebreaker": { amount: 89.0, speed: -1.9, target: 90.0, category: "Simply More unique x10" },
    "simplymore:timekeeper": { amount: 79.0, speed: -2.0, target: 80.0, category: "Simply More unique x10" },
    "simplymore:vipers_call": { amount: 59.0, speed: -3.0, target: 60.0, category: "Simply More unique x10" },
    "simplyswords:arcanethyst": { amount: 129.0, speed: -2.7, target: 130.0, category: "Simply Swords unique x10" },
    "simplyswords:awakened_lichblade": { amount: 129.0, speed: -3.1, target: 130.0, category: "Simply Swords unique x10" },
    "simplyswords:bramblethorn": { amount: 89.0, speed: -1.8, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:brimstone_claymore": { amount: 119.0, speed: -2.8, target: 120.0, category: "Simply Swords unique x10" },
    "simplyswords:caelestis": { amount: 119.0, speed: -2.9, target: 120.0, category: "Simply Swords unique x10" },
    "simplyswords:decaying_relic": { amount: 99.0, speed: -2.4, target: 100.0, category: "Simply Swords unique x10" },
    "simplyswords:dormant_relic": { amount: 89.0, speed: -2.4, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:dreadtide": { amount: 89.0, speed: -2.0, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:emberblade": { amount: 89.0, speed: -2.4, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:emberlash": { amount: 59.0, speed: -1.5, target: 60.0, category: "Simply Swords unique x10" },
    "simplyswords:enigma": { amount: 129.0, speed: -3.2, target: 130.0, category: "Simply Swords unique x10" },
    "simplyswords:flamewind": { amount: 89.0, speed: -2.6, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:frostfall": { amount: 109.0, speed: -2.5, target: 110.0, category: "Simply Swords unique x10" },
    "simplyswords:harbinger": { amount: 89.0, speed: -2.4, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:hearthflame": { amount: 139.0, speed: -3.2, target: 140.0, category: "Simply Swords unique x10" },
    "simplyswords:hiveheart": { amount: 129.0, speed: -3.0, target: 130.0, category: "Simply Swords unique x10" },
    "simplyswords:icewhisper": { amount: 129.0, speed: -2.7, target: 130.0, category: "Simply Swords unique x10" },
    "simplyswords:livyatan": { amount: 99.0, speed: -2.1, target: 100.0, category: "Simply Swords unique x10" },
    "simplyswords:magiblade": { amount: 89.0, speed: -2.0, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:magiscythe": { amount: 99.0, speed: -2.4, target: 100.0, category: "Simply Swords unique x10" },
    "simplyswords:magispear": { amount: 99.0, speed: -2.5, target: 100.0, category: "Simply Swords unique x10" },
    "simplyswords:mjolnir": { amount: 89.0, speed: -3.0, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:molten_edge": { amount: 99.0, speed: -2.1, target: 100.0, category: "Simply Swords unique x10" },
    "simplyswords:ribboncleaver": { amount: 129.0, speed: -3.2, target: 130.0, category: "Simply Swords unique x10" },
    "simplyswords:righteous_relic": { amount: 89.0, speed: -2.4, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:runic_chakram": { amount: 79.0, speed: -3.0, target: 80.0, category: "Simply Swords runic x10" },
    "simplyswords:runic_claymore": { amount: 109.0, speed: -2.8, target: 110.0, category: "Simply Swords runic x10" },
    "simplyswords:runic_cutlass": { amount: 89.0, speed: -2.0, target: 90.0, category: "Simply Swords runic x10" },
    "simplyswords:runic_glaive": { amount: 89.0, speed: -2.6, target: 90.0, category: "Simply Swords runic x10" },
    "simplyswords:runic_greataxe": { amount: 119.0, speed: -3.1, target: 120.0, category: "Simply Swords runic x10" },
    "simplyswords:runic_greathammer": { amount: 129.0, speed: -3.2, target: 130.0, category: "Simply Swords runic x10" },
    "simplyswords:runic_halberd": { amount: 119.0, speed: -2.8, target: 120.0, category: "Simply Swords runic x10" },
    "simplyswords:runic_katana": { amount: 89.0, speed: -2.0, target: 90.0, category: "Simply Swords runic x10" },
    "simplyswords:runic_longsword": { amount: 89.0, speed: -2.4, target: 90.0, category: "Simply Swords runic x10" },
    "simplyswords:runic_rapier": { amount: 79.0, speed: -1.8, target: 80.0, category: "Simply Swords runic x10" },
    "simplyswords:runic_sai": { amount: 59.0, speed: -1.5, target: 60.0, category: "Simply Swords runic x10" },
    "simplyswords:runic_scythe": { amount: 99.0, speed: -2.7, target: 100.0, category: "Simply Swords runic x10" },
    "simplyswords:runic_spear": { amount: 89.0, speed: -2.7, target: 90.0, category: "Simply Swords runic x10" },
    "simplyswords:runic_twinblade": { amount: 89.0, speed: -2.0, target: 90.0, category: "Simply Swords runic x10" },
    "simplyswords:runic_warglaive": { amount: 89.0, speed: -2.2, target: 90.0, category: "Simply Swords runic x10" },
    "simplyswords:shadowsting": { amount: 39.0, speed: -1.7, target: 40.0, category: "Simply Swords unique x10" },
    "simplyswords:slumbering_lichblade": { amount: 129.0, speed: -3.1, target: 130.0, category: "Simply Swords unique x10" },
    "simplyswords:soulkeeper": { amount: 139.0, speed: -2.9, target: 140.0, category: "Simply Swords unique x10" },
    "simplyswords:soulpyre": { amount: 129.0, speed: -3.0, target: 130.0, category: "Simply Swords unique x10" },
    "simplyswords:soulrender": { amount: 99.0, speed: -2.4, target: 100.0, category: "Simply Swords unique x10" },
    "simplyswords:soulstealer": { amount: 59.0, speed: -1.5, target: 60.0, category: "Simply Swords unique x10" },
    "simplyswords:stars_edge": { amount: 89.0, speed: -2.0, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:stormbringer": { amount: 89.0, speed: -2.4, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:storms_edge": { amount: 89.0, speed: -2.0, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:sunfire": { amount: 89.0, speed: -2.4, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:tainted_relic": { amount: 89.0, speed: -2.4, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:tempest": { amount: 59.0, speed: -2.5, target: 60.0, category: "Simply Swords unique x10" },
    "simplyswords:thunderbrand": { amount: 129.0, speed: -2.7, target: 130.0, category: "Simply Swords unique x10" },
    "simplyswords:toxic_longsword": { amount: 89.0, speed: -2.4, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:twisted_blade": { amount: 99.0, speed: -2.6, target: 100.0, category: "Simply Swords unique x10" },
    "simplyswords:waking_lichblade": { amount: 129.0, speed: -3.1, target: 130.0, category: "Simply Swords unique x10" },
    "simplyswords:watcher_claymore": { amount: 119.0, speed: -2.8, target: 120.0, category: "Simply Swords unique x10" },
    "simplyswords:watching_warglaive": { amount: 89.0, speed: -2.2, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:waxweaver": { amount: 119.0, speed: -2.9, target: 120.0, category: "Simply Swords unique x10" },
    "simplyswords:whisperwind": { amount: 89.0, speed: -2.0, target: 90.0, category: "Simply Swords unique x10" },
    "simplyswords:wickpiercer": { amount: 99.0, speed: -2.1, target: 100.0, category: "Simply Swords unique x10" }
};

function stackId(stack) {
    try {
        return String(stack.id);
    } catch (e) {
        return "";
    }
}

function isEmpty(stack) {
    if (stack == null) return true;
    try {
        return !!stack.isEmpty();
    } catch (e) {
        return true;
    }
}

function hasMarker(stack) {
    try {
        var tag = stack.getTag ? stack.getTag() : stack.nbt;
        if (tag == null) return false;
        if (tag.contains && tag.contains(MARKER)) return true;
        if (tag[MARKER] !== undefined && tag[MARKER] !== null) return true;
    } catch (e) {}
    return false;
}

function setMarker(stack) {
    try {
        var tag = stack.getOrCreateTag ? stack.getOrCreateTag() : null;
        if (tag != null && tag.putByte) {
            tag.putByte(MARKER, 1);
            return;
        }
    } catch (e1) {}
    try {
        stack.nbt = stack.nbt || {};
        stack.nbt[MARKER] = 1;
    } catch (e2) {}
}

function modifierTag(attributeId, amount, uuid) {
    var tag = new CompoundTag();
    tag.putString("AttributeName", attributeId);
    tag.putString("Name", "Weapon modifier");
    tag.putDouble("Amount", amount);
    tag.putInt("Operation", 0); // addition
    tag.putUUID("UUID", uuid);
    tag.putString("Slot", "mainhand");
    return tag;
}

function applyToStack(stack, entry) {
    if (isEmpty(stack) || entry == null) return false;
    if (hasMarker(stack)) return false;

    try {
        var tag = stack.getOrCreateTag();
        var list = new ListTag();
        list.add(
            modifierTag(
                "minecraft:generic.attack_damage",
                entry.amount,
                DAMAGE_UUID
            )
        );
        list.add(
            modifierTag(
                "minecraft:generic.attack_speed",
                entry.speed,
                SPEED_UUID
            )
        );
        tag.put("AttributeModifiers", list);
        tag.putByte(MARKER, 1);
        if (DEBUG) {
            console.info(
                "[DMZ Actual Damage] patched " +
                    stackId(stack) +
                    " -> AD " +
                    entry.target +
                    " (amount " +
                    entry.amount +
                    ") speed " +
                    entry.speed
            );
        }
        return true;
    } catch (err) {
        console.error(
            "[DMZ Actual Damage] failed to patch " +
                stackId(stack) +
                ": " +
                err
        );
        return false;
    }
}

function patchPlayer(player) {
    if (player == null) return;
    try {
        var main = player.getMainHandItem
            ? player.getMainHandItem()
            : player.mainHandItem;
        var off = player.getOffhandItem
            ? player.getOffhandItem()
            : player.offHandItem;

        var mainId = stackId(main);
        if (TARGETS[mainId]) applyToStack(main, TARGETS[mainId]);

        var offId = stackId(off);
        if (TARGETS[offId]) applyToStack(off, TARGETS[offId]);
    } catch (err) {
        if (DEBUG) console.error("[DMZ Actual Damage] patchPlayer: " + err);
    }
}

PlayerEvents.loggedIn(function (event) {
    patchPlayer(event.player);
});

PlayerEvents.tick(function (event) {
    var player = event.player;
    if (player == null) return;
    try {
        if (player.level && player.level.isClientSide) return;
    } catch (e) {}
    try {
        var age = player.tickCount !== undefined ? player.tickCount : player.age;
        if (age % UPDATE_INTERVAL !== 0) return;
    } catch (e2) {
        return;
    }
    patchPlayer(player);
});

console.info(
    "[DMZ Actual Damage] loaded " +
        Object.keys(TARGETS).length +
        " weapon targets (KubeJS port of datapack v1)"
);
