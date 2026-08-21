const CapsuleItem = Java.loadClass('capsule.items.CapsuleItem');

PlayerEvents.tick(event => {
    const player = event.player;

    if (player.age % 20 !== 0) {
        return;
    }

    let removed = 0;
    const inventory = player.getInventory();

    for (let slot = 0; slot < inventory.getContainerSize(); slot++) {
        const stack = inventory.getItem(slot);

        if (
            !stack.isEmpty() &&
            stack.id === 'capsule:capsule' &&
            CapsuleItem.isBlueprint(stack)
        ) {
            removed += stack.count;
            inventory.setItem(slot, Item.empty);
        }
    }

    const enderChest = player.getEnderChestInventory();

    for (let slot = 0; slot < enderChest.getContainerSize(); slot++) {
        const stack = enderChest.getItem(slot);

        if (
            !stack.isEmpty() &&
            stack.id === 'capsule:capsule' &&
            CapsuleItem.isBlueprint(stack)
        ) {
            removed += stack.count;
            enderChest.setItem(slot, Item.empty);
        }
    }

    if (removed > 0) {
        player.tell('\u00A7cBlueprint capsules have been disabled and removed.');
        console.log(
            '[Blueprint Removal] Removed ' +
            removed +
            ' blueprint(s) from ' +
            player.username
        );
    }
});

EntityEvents.spawned(event => {
    const entity = event.entity;

    if (entity.type !== 'minecraft:item') {
        return;
    }

    const stack = entity.item;

    if (
        !stack.isEmpty() &&
        stack.id === 'capsule:capsule' &&
        CapsuleItem.isBlueprint(stack)
    ) {
        entity.discard();
    }
});