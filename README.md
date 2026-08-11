# DMZServer

Client modpack: **[DMZ Legacy Reborn 2.1.3 Testing Phase](https://www.curseforge.com/minecraft/modpacks/testing-dmzlegacy)** (`docs/MODPACK.md`).

Forge **1.20.1** server pack for **DragonMineZ 2.1.3** + your CustomNPCs script pack, with **KubeJS** installed for Forge-side scripting.

## Important: hybrid runtime

Most uploaded scripts call `org.bukkit.Bukkit` and Fabled/CMI. They need a **hybrid** jar (Arclight / Mohist / similar) that loads both `mods/` and `plugins/`. Plain Forge will run DMZ + CustomNPCs + KubeJS, but Fabled/CMI bridges will no-op or error.

## Mods (`mods/`)

- DragonMineZ 2.1.3 + GeckoLib + TerraBlender + Curios
- CustomNPCs (GBPort Unofficial 1.20.1)
- KubeJS + Rhino + Architectury

## Plugins (`plugins/`) — hybrid only

- CMI 9.8.9.4 (**needs CMILib** — not uploaded yet)
- Fabled 1.0.4 (**needs CodexCore** — not uploaded yet)

## Scripts

| Location | Engine |
|----------|--------|
| `customnpcs/scripts/` | CustomNPCs Nashorn (your uploaded pack) |
| `kubejs/` | KubeJS (starter example only) |

Inventory + DMZ call patterns: [`docs/SCRIPT_INVENTORY.md`](docs/SCRIPT_INVENTORY.md)  
Java event/API map: [`docs/DMZ_API_REFERENCE.md`](docs/DMZ_API_REFERENCE.md)

## Run

```bash
./run.sh nogui   # plain Forge — CNPC/DMZ/KubeJS only
# or your hybrid server start script for full Fabled/CMI support
```

## Still needed from you

1. Hybrid server jar (if not already elsewhere)
2. `CMILib` + `CodexCore` jars → `plugins/`
3. Apotheosis AttributesLib if you use KiWeapons/Piercing Apoth hooks
4. Any existing `kubejs/` scripts you still have (these uploads were CNPC-only)
