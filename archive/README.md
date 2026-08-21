# Archive — superseded / duplicate scripts

These files are **not loaded**. Live scripts live only in:

| Engine | Path |
|--------|------|
| KubeJS | `kubejs/` (`startup_scripts`, `server_scripts`, `client_scripts`, `data`) |
| CustomNPCs | `customnpcs/scripts/` |
| CMI aliases | `uploads/scripts/*.yml` |
| Forge scripts | `forge_scripts.json` (must stay empty, `ScriptEnabled: 0`) |

## Folders

| Folder | Contents |
|--------|----------|
| `cnpc-superseded/` | Old CNPC scripts replaced by combined versions or KubeJS |
| `root-script-dumps/` | Loose root `.js` copies (never a CNPC load path) |
| `uploads-script-mirrors/` | Duplicate uploads mirrors of cnpc/kubejs |
| `kubejs-redundant/` | KubeJS files that duplicated live scripts |
| `reference/` | Datapack zip — **do not enable** (use `kubejs/.../dmz_actual_attack_damage.js`) |

## Do not re-enable (conflicts)

| Archived / removed | Use instead |
|--------------------|-------------|
| Fly / ViltrumiteFly / flight suppression | `Flight.js` |
| Jump / Sprint / JumpSprint | `SprintJump.js` |
| YardratRace / YardratSkills | `Yardrat.js` |
| DMZ Energy / DMZ Stat Screen | `DMZ Fabled Bridge.js` |
| Prestige Sync / Faction Sync / Value Cleaner | `Fabled Sync.js` |
| Attr Fabled bonus stats | `Attr Fabled Multi bonus.js` |
| EndDragon / Dungeon / Shadow forge CNPC | KubeJS + CMI `asOp!` aliases |
| `dmzweaponbonusscale.js` | `kubejs/.../dmz_weapon_bonus_scale.js` |
| Attack-damage datapack zip | `kubejs/.../dmz_actual_attack_damage.js` |
| `apotheosis_armor_health_nerf.js` | already in `apotheosis_balance.js` |
| capsule blueprint helpers / tick purge | `capsule_disable.js` |

See `tools/SCRIPT_PACK_AUDIT.md`.
