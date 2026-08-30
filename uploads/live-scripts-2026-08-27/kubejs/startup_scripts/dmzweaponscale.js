// ============================================================================
// DMZ Legacy - Actual Base Weapon Damage
// Minecraft 1.20.1 Forge
// KubeJS 2001.6.5-build.26
//
// Put in:
// kubejs/startup_scripts/dmzweaponscale.js
//
// PURPOSE:
// - Replaces the item's NORMAL main-hand attack damage modifier.
// - Does NOT scan players.
// - Does NOT scan entities.
// - Does NOT scan inventories.
// - Does NOT modify ItemStack NBT.
// - Does NOT run commands.
// - Does NOT use scheduled functions.
// - Preserves attack speed.
// - Preserves Apotheosis and other unrelated modifiers.
// - Works with existing items as attributes are calculated.
//
// IMPORTANT:
// Forge 1.20.1 ItemAttributeModifierEvent.removeModifier() expects:
//
//     removeModifier(Attribute, AttributeModifier)
//
// NOT:
//
//     removeModifier(Attribute, UUID)
//
// DEBUG defaults to false.
//
// FULL SERVER RESTART REQUIRED.
// ============================================================================

var DEBUG = false;

// Prevent a scripting mistake from ever crashing the server.
// We log only a few errors rather than flooding console.
var ERROR_LOGS_LEFT = 5;


// ============================================================================
// JAVA
// ============================================================================

var Attributes = Java.loadClass(
    "net.minecraft.world.entity.ai.attributes.Attributes"
);

var AttributeModifier = Java.loadClass(
    "net.minecraft.world.entity.ai.attributes.AttributeModifier"
);

var EquipmentSlot = Java.loadClass(
    "net.minecraft.world.entity.EquipmentSlot"
);

var UUID = Java.loadClass(
    "java.util.UUID"
);

var ForgeRegistries = Java.loadClass(
    "net.minecraftforge.registries.ForgeRegistries"
);


// ============================================================================
// NORMAL VANILLA WEAPON ATTACK-DAMAGE UUID
//
// Minecraft 1.20.1 Item.BASE_ATTACK_DAMAGE_UUID:
//
// cb3f55d3-645c-4f38-a497-9c13a33db5cf
//
// SwordItem and many SwordItem-derived modded weapons use this UUID for their
// normal/base main-hand attack-damage modifier.
// ============================================================================

var BASE_ATTACK_DAMAGE_UUID_TEXT =
    "cb3f55d3-645c-4f38-a497-9c13a33db5cf";

var BASE_ATTACK_DAMAGE_UUID =
    UUID.fromString(BASE_ATTACK_DAMAGE_UUID_TEXT);


// ============================================================================
// DAMAGE VALUES
//
// These are the ADDITION modifier amounts.
//
// Minecraft players inherently have:
//
//     generic.attack_damage = 1.0
//
// Therefore:
//
//     modifier 89
//     + player base 1
//     = 90 normal attack damage
//
// Apotheosis / other modifiers can then affect the resulting weapon normally.
// ============================================================================

var BASE_DAMAGE = {

    // ========================================================================
    // DragonMineZ
    // ========================================================================

    "dragonminez:brave_sword": 17.75,
    "dragonminez:dimensional_sword": 61.5,

    "dragonminez:gete_axe": 2,
    "dragonminez:gete_hoe": -0.25,
    "dragonminez:gete_pickaxe": 1,
    "dragonminez:gete_shovel": 1.125,

    "dragonminez:power_pole": 15,
    "dragonminez:yajirobe_katana": 2,
    "dragonminez:z_sword": 74,


    // ========================================================================
    // Simply More
    // ========================================================================

    "simplymore:ascended_idol": 99,
    "simplymore:black_pearl": 89,
    "simplymore:blade_of_the_grotesque": 119,
    "simplymore:boas_fang": 69,
    "simplymore:brassturn": 79,
    "simplymore:cindergorge": 109,
    "simplymore:culterex": 79,
    "simplymore:darksent": 99,
    "simplymore:deaths_eyrie": 119,
    "simplymore:earthshatter": 119,
    "simplymore:exedrill": 89,
    "simplymore:glimmerstep": 89,
    "simplymore:grandfrost": 139,
    "simplymore:great_slither": 109,
    "simplymore:holylight": 99,
    "simplymore:lustrous_moxie": 99,
    "simplymore:matterbane": 89,

    "simplymore:mimicry_backhand_blade": 79,
    "simplymore:mimicry_chakram": 89,
    "simplymore:mimicry_claymore": 119,
    "simplymore:mimicry_cutlass": 99,
    "simplymore:mimicry_dagger": 79,
    "simplymore:mimicry_deer_horns": 89,
    "simplymore:mimicry_glaive": 99,
    "simplymore:mimicry_grandsword": 159,
    "simplymore:mimicry_great_katana": 109,
    "simplymore:mimicry_great_spear": 129,
    "simplymore:mimicry_greataxe": 129,
    "simplymore:mimicry_greathammer": 139,
    "simplymore:mimicry_halberd": 129,
    "simplymore:mimicry_katana": 99,
    "simplymore:mimicry_khopesh": 89,
    "simplymore:mimicry_lance": 99,
    "simplymore:mimicry_longsword": 99,
    "simplymore:mimicry_pernach": 109,
    "simplymore:mimicry_quarterstaff": 79,
    "simplymore:mimicry_rapier": 89,
    "simplymore:mimicry_sai": 69,
    "simplymore:mimicry_scythe": 109,
    "simplymore:mimicry_spear": 99,
    "simplymore:mimicry_twinblade": 99,
    "simplymore:mimicry_warglaive": 99,

    "simplymore:molten_flare": 129,
    "simplymore:myrmedge": 79,
    "simplymore:perforiscus": 99,
    "simplymore:revvengine": 79,

    "simplymore:runic_backhand_blade": 69,
    "simplymore:runic_dagger": 69,
    "simplymore:runic_deer_horns": 79,
    "simplymore:runic_grandsword": 149,
    "simplymore:runic_great_katana": 99,
    "simplymore:runic_great_spear": 119,
    "simplymore:runic_khopesh": 79,
    "simplymore:runic_lance": 89,
    "simplymore:runic_pernach": 99,
    "simplymore:runic_quarterstaff": 69,

    "simplymore:ruptured_idol": 99,
    "simplymore:ruyi_jingu_bang": 89,
    "simplymore:serpentine_valour": 109,
    "simplymore:smouldering_ruin": 69,
    "simplymore:soul_foreseer": 89,
    "simplymore:stasis": 79,
    "simplymore:tarnished_idol": 99,
    "simplymore:the_blood_harvester": 79,
    "simplymore:the_vessel_breach": 59,
    "simplymore:tidebreaker": 89,
    "simplymore:timekeeper": 79,
    "simplymore:vipers_call": 59,


    // ========================================================================
    // Simply Swords
    // ========================================================================

    "simplyswords:arcanethyst": 129,
    "simplyswords:awakened_lichblade": 129,
    "simplyswords:bramblethorn": 89,
    "simplyswords:brimstone_claymore": 119,
    "simplyswords:caelestis": 119,
    "simplyswords:decaying_relic": 99,
    "simplyswords:dormant_relic": 89,
    "simplyswords:dreadtide": 89,
    "simplyswords:emberblade": 89,
    "simplyswords:emberlash": 59,
    "simplyswords:enigma": 129,
    "simplyswords:flamewind": 89,
    "simplyswords:frostfall": 109,
    "simplyswords:harbinger": 89,
    "simplyswords:hearthflame": 139,
    "simplyswords:hiveheart": 129,
    "simplyswords:icewhisper": 129,
    "simplyswords:livyatan": 99,
    "simplyswords:magiblade": 89,
    "simplyswords:magiscythe": 99,
    "simplyswords:magispear": 99,
    "simplyswords:mjolnir": 89,
    "simplyswords:molten_edge": 99,
    "simplyswords:ribboncleaver": 129,
    "simplyswords:righteous_relic": 89,

    "simplyswords:runic_chakram": 79,
    "simplyswords:runic_claymore": 109,
    "simplyswords:runic_cutlass": 89,
    "simplyswords:runic_glaive": 89,
    "simplyswords:runic_greataxe": 119,
    "simplyswords:runic_greathammer": 129,
    "simplyswords:runic_halberd": 119,
    "simplyswords:runic_katana": 89,
    "simplyswords:runic_longsword": 89,
    "simplyswords:runic_rapier": 79,
    "simplyswords:runic_sai": 59,
    "simplyswords:runic_scythe": 99,
    "simplyswords:runic_spear": 89,
    "simplyswords:runic_twinblade": 89,
    "simplyswords:runic_warglaive": 89,

    "simplyswords:shadowsting": 39,
    "simplyswords:slumbering_lichblade": 129,
    "simplyswords:soulkeeper": 139,
    "simplyswords:soulpyre": 129,
    "simplyswords:soulrender": 99,
    "simplyswords:soulstealer": 59,
    "simplyswords:stars_edge": 89,
    "simplyswords:stormbringer": 89,
    "simplyswords:storms_edge": 89,
    "simplyswords:sunfire": 89,
    "simplyswords:tainted_relic": 89,
    "simplyswords:tempest": 59,
    "simplyswords:thunderbrand": 129,
    "simplyswords:toxic_longsword": 89,
    "simplyswords:twisted_blade": 99,
    "simplyswords:waking_lichblade": 129,
    "simplyswords:watcher_claymore": 119,
    "simplyswords:watching_warglaive": 89,
    "simplyswords:waxweaver": 119,
    "simplyswords:whisperwind": 89,
    "simplyswords:wickpiercer": 99
};


// ============================================================================
// ATTRIBUTE EVENT
// ============================================================================

ForgeEvents.onEvent(
    "net.minecraftforge.event.ItemAttributeModifierEvent",
    function (event) {

        // IMPORTANT:
        //
        // Never allow an error inside this Forge event to bubble into the
        // player's equipment tick. The previous version did exactly that and
        // could crash the server when a player's attributes were evaluated.
        try {

            // Base weapon attributes are relevant to MAINHAND only.
            if (event.getSlotType() != EquipmentSlot.MAINHAND) {
                return;
            }

            var stack = event.getItemStack();

            if (stack == null) {
                return;
            }

            if (stack.isEmpty()) {
                return;
            }


            // ----------------------------------------------------------------
            // GET ITEM ID
            // ----------------------------------------------------------------

            var key = ForgeRegistries.ITEMS.getKey(
                stack.getItem()
            );

            if (key == null) {
                return;
            }

            var id = String(key);


            // ----------------------------------------------------------------
            // QUICK LOOKUP
            //
            // For virtually every item in the game, the script ends here.
            // ----------------------------------------------------------------

            var damage = BASE_DAMAGE[id];

            if (damage === undefined || damage === null) {
                return;
            }


            // ----------------------------------------------------------------
            // FIND THE EXISTING BASE WEAPON MODIFIER
            //
            // Forge 1.20.1 DOES NOT accept:
            //
            // event.removeModifier(attribute, UUID)
            //
            // It accepts:
            //
            // event.removeModifier(attribute, AttributeModifier)
            //
            // So we inspect the current ATTACK_DAMAGE modifiers and locate the
            // one using Minecraft's normal base weapon-damage UUID.
            // ----------------------------------------------------------------

            var currentModifiers =
                event.getModifiers().get(
                    Attributes.ATTACK_DAMAGE
                );

            var baseModifier = null;

            if (currentModifiers != null) {

                var iterator =
                    currentModifiers.iterator();

                while (iterator.hasNext()) {

                    var modifier =
                        iterator.next();

                    if (modifier == null) {
                        continue;
                    }

                    var modifierId = "";

                    try {
                        modifierId =
                            String(
                                modifier.getId()
                            ).toLowerCase();
                    } catch (eId) {
                        modifierId = "";
                    }

                    if (
                        modifierId ===
                        BASE_ATTACK_DAMAGE_UUID_TEXT
                    ) {
                        baseModifier = modifier;
                        break;
                    }
                }
            }


            // ----------------------------------------------------------------
            // REMOVE ONLY THE NORMAL BASE DAMAGE MODIFIER
            //
            // This is the corrected Forge 1.20.1 call:
            //
            // removeModifier(Attribute, AttributeModifier)
            //
            // We do NOT remove:
            // - attack speed
            // - Apotheosis affixes
            // - Apotheosis gems
            // - other UUID-based damage modifiers
            // - other attributes
            // ----------------------------------------------------------------

            if (baseModifier != null) {

                event.removeModifier(
                    Attributes.ATTACK_DAMAGE,
                    baseModifier
                );
            }


            // ----------------------------------------------------------------
            // ADD OUR REPLACEMENT BASE DAMAGE
            //
            // Same vanilla UUID.
            // Same ADDITION operation.
            // ----------------------------------------------------------------

            var replacement =
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_UUID,
                    "DMZ Legacy base weapon damage",
                    damage,
                    AttributeModifier.Operation.ADDITION
                );

            var added =
                event.addModifier(
                    Attributes.ATTACK_DAMAGE,
                    replacement
                );


            // ----------------------------------------------------------------
            // DEBUG
            // ----------------------------------------------------------------

            if (DEBUG) {

                console.info(
                    "[DMZ Weapon Damage] " +
                    id +
                    " old=" +
                    (
                        baseModifier != null
                            ? baseModifier.getAmount()
                            : "(none)"
                    ) +
                    " new=" +
                    damage +
                    " finalVanillaBase=" +
                    (damage + 1) +
                    " added=" +
                    added
                );
            }

        } catch (err) {

            if (ERROR_LOGS_LEFT > 0) {

                ERROR_LOGS_LEFT--;

                console.error(
                    "[DMZ Weapon Damage ERROR] " +
                    err
                );
            }

            // DO NOT rethrow.
            //
            // A failure in ItemAttributeModifierEvent can otherwise propagate
            // through Forge's equipment calculation and crash a player/server.
        }
    }
);