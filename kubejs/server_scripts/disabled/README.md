# Disabled KubeJS scripts (TPS)

These files were moved out of the active `server_scripts/` load path because they
duplicated work already handled by `../capsule_disable.js` / `../apotheosis_balance.js`
and used expensive every-second `clear @a` / `kill @e` command purges.

| File | Why disabled |
|------|----------------|
| `disable_capsule_blueprints.js.disabled` | `ServerEvents.tick` → 4 NBT `clear`/`kill` commands every second |
| `disable_overpowered_capsules.js.disabled` | Player tick inv scan + 4 more `clear`/`kill` commands every second |
| `remove_existing_blueprints.js.disabled` | Full inv/ender scan every second per player (overlaps capsule_disable) |
| `Apotheoisis_nerf.js.disabled` | Duplicate of `apotheosis_balance.js` data writers |
| `apotheosis_armor_health_nerf.js.disabled` | Covered by `apotheosis_balance.js` |
| `dmz_bridge_example.js.disabled` | Example `PlayerEvents.tick` probe; not gameplay |

**Keep active:**
- `capsule_disable.js` — recipe remove + slot-safe purge (no `minecraft:clear`)
- `apotheosis_balance.js` — data balance
- `apotheosis_spawner_disable.js` — recipe remove only
