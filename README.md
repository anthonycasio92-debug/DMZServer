# DMZServer

Fresh Minecraft **Forge 1.20.1** server pack for **DragonMineZ 2.1.3** (CurseForge file `8469416`).

## Mods (verified)

| Jar | Role |
|-----|------|
| `mods/dragonminez-2.1.3.jar` | DragonMineZ 2.1.3 (SHA-256 matches Modrinth) |
| `mods/geckolib-forge-1.20.1-4.8.4.jar` | required |
| `mods/TerraBlender-forge-1.20.1-3.0.1.10.jar` | required |
| `mods/curios-forge-5.14.1+1.20.1.jar` | required |

## Run

```bash
./run.sh nogui
```

Tune RAM in `user_jvm_args.txt` (recommend at least `-Xmx4G` for DMZ).

## Scripts / API work

- API + event/method reference: [`docs/DMZ_API_REFERENCE.md`](docs/DMZ_API_REFERENCE.md)
- Drop your existing scripts/jars: [`uploads/`](uploads/)
- **Upload to this agent:** https://cursor.com/agents/bc-65345b36-ca27-45d9-b51d-6a16f547c766

DMZ scripting is primarily **Java Forge event hooks** on `com.dragonminez.common.events.DMZEvent` plus the `StatsProvider` / `StatsData` capability API. There is no built-in KubeJS/CraftTweaker bridge.
