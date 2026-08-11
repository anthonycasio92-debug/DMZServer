// kubejs/server_scripts/disable_capsule_blueprints.js

let blueprintPurgeTimer = 0;

// Disable all Capsule blueprint crafting recipes
ServerEvents.recipes(event => {
    event.remove({ id: 'capsule:blueprint' });
    event.remove({ type: 'capsule:blueprint_capsule' });

    event.remove({ id: 'capsule:blueprint_change' });
    event.remove({ type: 'capsule:blueprint_change' });

    event.remove({ id: 'capsule:aggregate_all_prefabs' });
    event.remove({ type: 'capsule:aggregate_all_prefabs' });

    console.log('[DBZ Legacy Reborn] Capsule blueprint recipes disabled.');
});

// Continuously remove existing blueprints
ServerEvents.tick(event => {
    blueprintPurgeTimer++;

    // Run once every second
    if (blueprintPurgeTimer < 20) {
        return;
    }

    blueprintPurgeTimer = 0;

    const server = event.server;

    // Remove blueprint capsules from every online player's inventory
    server.runCommandSilent(
        'minecraft:clear @a capsule:capsule{state:7}'
    );

    // Backup check using Capsule's actual blueprint marker
    server.runCommandSilent(
        'minecraft:clear @a capsule:capsule{sourceInventory:{}}'
    );

    // Remove blueprint capsules dropped in loaded worlds
    server.runCommandSilent(
        'minecraft:kill @e[type=minecraft:item,nbt={Item:{id:"capsule:capsule",tag:{state:7}}}]'
    );

    // Backup check for dropped blueprints using sourceInventory
    server.runCommandSilent(
        'minecraft:kill @e[type=minecraft:item,nbt={Item:{id:"capsule:capsule",tag:{sourceInventory:{}}}}]'
    );
});

ServerEvents.loaded(event => {
    console.log(
        '[DBZ Legacy Reborn] Permanent Capsule blueprint purge enabled.'
    );
});