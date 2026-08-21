# Script & jar inventory

**Current layout:** see `tools/SCRIPT_LAYOUT.md` and `tools/SCRIPT_PACK_AUDIT.md`.

| Path | Contents |
|------|----------|
| `customnpcs/scripts/*.js` | Live CustomNPCs scripts + `player_scripts.json` |
| `kubejs/` | Live KubeJS (startup / server / client / data) |
| `forge_scripts.json` | Empty + disabled (required) |
| `uploads/scripts/` | CMI aliases + JSON configs (no `.js` mirrors) |
| `archive/` | Superseded / duplicate dumps (not loaded) |
| `mods/`, `plugins/` | Jars (CustomNPCs, KubeJS, DMZ, CMI, Fabled, …) |

Hybrid server (Mohist / Arclight) is required for CNPC scripts that call Bukkit (Fabled, CMI).

Do **not** put loose `.js` at the repo root — CNPC only loads from `customnpcs/scripts/`.
