// kubejs/server_scripts/disable_overpowered_capsules.js
// Mirrors the blueprint disable/purge pattern for Capsule "Overpowered" capsules.
// OP capsules use NBT {overpowered:1} and CapsuleItem.isOverpowered(stack).

const CapsuleItem = Java.loadClass('capsule.items.CapsuleItem')

let opCapsulePurgeTimer = 0

// Disable Overpowered capsule crafting (nether star recipe)
ServerEvents.recipes(event => {
    event.remove({ id: 'capsule:capsule_op' })
    console.log('[DBZ Legacy Reborn] Overpowered capsule recipe disabled.')
})

function isOverpoweredCapsule(stack) {
    if (stack.isEmpty() || stack.id !== 'capsule:capsule') {
        return false
    }
    try {
        if (CapsuleItem.isOverpowered(stack)) {
            return true
        }
    } catch (e) {}
    // Fallback NBT check
    try {
        const nbt = stack.nbt
        if (nbt && (nbt.overpowered === 1 || nbt.overpowered === true)) {
            return true
        }
    } catch (e2) {}
    return false
}

function purgePlayerCapsules(player) {
    let removed = 0
    const inventory = player.getInventory()

    for (let slot = 0; slot < inventory.getContainerSize(); slot++) {
        const stack = inventory.getItem(slot)
        if (isOverpoweredCapsule(stack)) {
            removed += stack.count
            inventory.setItem(slot, Item.empty)
        }
    }

    const enderChest = player.getEnderChestInventory()
    for (let slot = 0; slot < enderChest.getContainerSize(); slot++) {
        const stack = enderChest.getItem(slot)
        if (isOverpoweredCapsule(stack)) {
            removed += stack.count
            enderChest.setItem(slot, Item.empty)
        }
    }

    if (removed > 0) {
        player.tell('\u00A7cOverpowered capsules have been disabled and removed.')
        console.log(
            '[OP Capsule Removal] Removed ' +
                removed +
                ' overpowered capsule(s) from ' +
                player.username
        )
    }
}

// Inventory + ender chest purge (same cadence as blueprint script)
PlayerEvents.tick(event => {
    const player = event.player
    if (!player || player.level.clientSide) {
        return
    }
    if (player.age % 20 !== 0) {
        return
    }
    purgePlayerCapsules(player)
})

// Ground item purge via Capsule API
EntityEvents.spawned(event => {
    const entity = event.entity
    if (entity.type !== 'minecraft:item') {
        return
    }
    const stack = entity.item
    if (isOverpoweredCapsule(stack)) {
        entity.discard()
    }
})

// Backup clear/kill commands (NBT), like blueprint purge
ServerEvents.tick(event => {
    opCapsulePurgeTimer++
    if (opCapsulePurgeTimer < 20) {
        return
    }
    opCapsulePurgeTimer = 0

    const server = event.server
    server.runCommandSilent('minecraft:clear @a capsule:capsule{overpowered:1}')
    server.runCommandSilent('minecraft:clear @a capsule:capsule{overpowered:1b}')
    server.runCommandSilent(
        'minecraft:kill @e[type=minecraft:item,nbt={Item:{id:"capsule:capsule",tag:{overpowered:1}}}]'
    )
    server.runCommandSilent(
        'minecraft:kill @e[type=minecraft:item,nbt={Item:{id:"capsule:capsule",tag:{overpowered:1b}}}]'
    )
})

ServerEvents.loaded(event => {
    console.log('[DBZ Legacy Reborn] Permanent Overpowered capsule purge enabled.')
})
