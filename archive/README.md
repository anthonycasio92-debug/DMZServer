# Archive

Superseded files kept for reference only. **Nothing here is loaded by the server.**

Canonical live paths are documented in `tools/SCRIPT_LAYOUT.md`:

- `customnpcs/scripts/` — CNPC Global Player / NPC scripts
- `kubejs/` — Forge-side scripting
- `uploads/scripts/` — CMI aliases + DMZ JSON configs

## root-scripts/

Legacy root-level copies of the CNPC `.js` scripts, moved out of the repo root
(2026-10-15). These were the original full scripts; in `customnpcs/scripts/`
most have since been replaced by newer versions or by "DISABLED STUB"
placeholders whose systems are now owned by the LegacyMechanics Forge mod
(`tools/dmz-adaptive-difficulty/`).

The copies are **not** byte-identical to their `customnpcs/scripts/` twins —
some root files predate a system being ported to Java, and three exist only
here (`Fabled Prestige Faction Sync.js`, `Prestige Sync Fabled.js`,
`Universal Fabled Value Cleaner.js` — superseded by the combined
`customnpcs/scripts/Fabled Sync.js`).

Do not copy anything from this folder back onto the live server without
checking `docs/DEPLOY.md` and `tools/SCRIPT_LAYOUT.md` first.
