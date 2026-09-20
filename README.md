# DMZServer

Client modpack: **[DMZ Legacy Reborn 2.1.3 Testing Phase](https://www.curseforge.com/minecraft/modpacks/testing-dmzlegacy)** (`docs/MODPACK.md`).

Forge **1.20.1** server pack for **DragonMineZ 2.1.3** + your CustomNPCs script pack, with **KubeJS** installed for Forge-side scripting.

## Deploy policy

Production deploys go to the **live** server only. The former test host is no longer used. See [`docs/DEPLOY.md`](docs/DEPLOY.md).

## Important: hybrid runtime

Most uploaded scripts call `org.bukkit.Bukkit` and Fabled/CMI. They need a **hybrid** jar (Arclight / Mohist / similar) that loads both `mods/` and `plugins/`. Plain Forge will run DMZ + CustomNPCs + KubeJS, but Fabled/CMI bridges will no-op or error.

## Mods (`mods/`)

- DragonMineZ 2.1.3 + GeckoLib + TerraBlender + Curios
- CustomNPCs (GBPort Unofficial 1.20.1)
- KubeJS + Rhino + Architectury

## Plugins (`plugins/`) — hybrid only

- CMI 9.8.9.4 (**needs CMILib** — not uploaded yet)
- Fabled 1.0.4 (**needs CodexCore** — not uploaded yet)
