// Consolidated item spawn. One handler for the former per-item listeners:
// apotheosis spawner strip, capsule ban, silk touch strip, tinkers silky convert.
//
// Each original file keeps its helpers and publishes them on global. One
// listener means one proxy call per spawned item.
//
// LegacyMechanics LmHotPath owns this listener once the class is loaded.

function dmzLmOwnsHotPath() {
    try {
        return Java.loadClass("com.dbzlegacy.adaptivedifficulty.kube.LmHotPath").ENABLED === true;
    } catch (e) {
        return false;
    }
}

if (dmzLmOwnsHotPath()) {
    console.info("[DMZ] Item spawn hot path is native LegacyMechanics; KubeJS listener idle.");
} else {
EntityEvents.spawned("minecraft:item", function (event) {
    try {
        var entity = event.entity;
        if (entity == null) return;
        var stack = entity.item;
        if (stack == null) return;

        try {
            var apoth = global.dmzApothItemSpawn;
            if (apoth) apoth(entity, stack, event);
        } catch (err) {}

        try {
            var cap = global.dmzCapsuleItemSpawn;
            if (cap) cap(entity, stack, event);
        } catch (err) {}

        try {
            var silk = global.dmzSilkTouchItemSpawn;
            if (silk) silk(entity, stack, event);
        } catch (err) {}

        try {
            var silky = global.dmzSilkyItemSpawn;
            if (silky) silky(entity, stack, event);
        } catch (err) {}
    } catch (err) {}
});
}

console.info("[DMZ] Consolidated item spawn loaded (spawner, capsule, silk touch, silky).");
