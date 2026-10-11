# Legacy Mechanics — Ultra GUI via Kai Path shared bridge

Ultra look-and-feel comes from **DMZUltra** (`dmzultra`). The
**LegacyMechanics** jar stays on the **dedicated server only**.

Kai Path hosts a **shared Ultra UI API** that **any mod** can call
(`UltraUiAccess` / `UltraScreen`). LegacyMechanics is only one optional
consumer of that API (network channel + hub).

→ Drop-in + docs: **`tools/kaipath-ultra-ui-bridge/`** (`INTEGRATE.md`)

## Jars

| Jar | Where | Role |
|-----|--------|------|
| `mods/LegacyMechanics-*.jar` | **Server only** | Gameplay; opens screens over `legacymechanics:gui` |
| `kaipath-*.jar` (with Ultra bridge) | **Client modpack** | Shared `UltraUiAccess` for all mods + optional LM consumer |
| `dmzultra-*.jar` | **Client modpack** | Ultra toolkit / textures (optional soft dep) |

Other client mods: soft-depend on `kaipath` and call `UltraUiAccess` — do not
ship their own dmzultra reflective copy if kaipath already provides it.

Do **not** also ship `LegacyMechanicsUltra` once kaipath includes the LM consumer
(channel conflict).

## Server config

```json
"guiBackend": "ultra",
"useNativeTestGui": true
```

## Protocol (LM consumer only)

- Channel: `legacymechanics:gui`
- Protocol: `"1"`
- S2C open: `screenId` · C2S action: `screenId`, `actionId`, `argsJson`

## Build (server LM)

```bash
bash tools/dmz-adaptive-difficulty/build.sh
```
