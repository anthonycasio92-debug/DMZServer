# Fabled dynamic skill fixes (purchase / level-up)

## Cause
1. **Ancient Saiyan** had `needs-permission: true` — Fabled **silently ignores** skill-tree clicks without `fabled.skill.ancient-saiyan` (no error message).
2. **Farming** had `level-base`/`level-scale` set to `10000` (looks like cost fields were swapped) — required Jobs class level 10000 to buy.
3. **Spiritualist.yml** was invalid YAML (missing `:` on the root key) and failed to load.

## Fix
| Skill | Change |
|-------|--------|
| Ancient Saiyan | `needs-permission: false`, `level-base: 11` (Prestige 10) |
| Sento Saiyan | keep `level-base: 2` (Prestige 1), lore clarified |
| Farming | `level-base: 1`, `cost-base/scale: 10000` (like Building) |
| Spiritualist | valid YAML matching other class skills |

## Building TP (silent)
`Building.yml` awards via kubejs `/silentdmztp {TPB}` (not `/dmzpoints`).
Requires `kubejs/server_scripts/silent_dmz_tp.js`. Restart once so the
command registers; then `/fabled reload` after Building.yml updates.

## Deploy
Copy into `plugins/Fabled/dynamic/skill/` then `/fabled reload` (or restart).

TP costs for race unlocks are unchanged (paid with DMZ TP via TP↔SP sync).
