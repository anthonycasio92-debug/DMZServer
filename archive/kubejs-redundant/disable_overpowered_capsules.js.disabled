// kubejs/server_scripts/disable_overpowered_capsules.js
// Recipe disable only. Inventory/ground purge is handled by capsule_disable.js
// (no @e[nbt=...] / clear commands — those caused severe Overworld MSPT spikes).

ServerEvents.recipes((event) => {
    event.remove({ id: "capsule:capsule_op" });
    console.info(
        "[DBZ Legacy Reborn] Overpowered capsule recipe disabled (purge via capsule_disable.js)."
    );
});
