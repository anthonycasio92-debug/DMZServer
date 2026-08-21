// kubejs/server_scripts/disable_capsule_blueprints.js
// Recipe disable only. Inventory/ground purge is handled by capsule_disable.js
// (no @e[nbt=...] / clear commands — those caused severe Overworld MSPT spikes).

ServerEvents.recipes((event) => {
    event.remove({ id: "capsule:blueprint" });
    event.remove({ type: "capsule:blueprint_capsule" });
    event.remove({ id: "capsule:blueprint_change" });
    event.remove({ type: "capsule:blueprint_change" });
    event.remove({ id: "capsule:aggregate_all_prefabs" });
    event.remove({ type: "capsule:aggregate_all_prefabs" });
    console.info(
        "[DBZ Legacy Reborn] Capsule blueprint recipes disabled (purge via capsule_disable.js)."
    );
});
